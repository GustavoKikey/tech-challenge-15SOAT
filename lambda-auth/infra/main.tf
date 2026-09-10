# =====================================================================
# Repo 1 — Function Serverless de autenticação + API Gateway
# ---------------------------------------------------------------------
# Terceiro da cadeia: consome rede (repo 2) e banco (repo 3) via SSM.
#
# Ordem de aplicação: 2 -> 3 -> [1] -> 4
# =====================================================================

locals {
  prefixo_ssm = "/${var.projeto}/${var.ambiente}"
  nome        = "${var.projeto}-${var.ambiente}-auth"

  tags = {
    Projeto   = var.projeto
    Ambiente  = var.ambiente
    ManagedBy = "terraform"
    Repo      = "lambda-auth"
  }
}

# ---------------------------------------------------------------------
# Contrato: o que os outros repos publicaram
# ---------------------------------------------------------------------

data "aws_ssm_parameter" "subnet_ids" { name = "${local.prefixo_ssm}/rede/subnet-ids" }
data "aws_ssm_parameter" "sg_lambda" { name = "${local.prefixo_ssm}/rede/sg-lambda-id" }
data "aws_ssm_parameter" "db_endpoint" { name = "${local.prefixo_ssm}/db/endpoint" }
data "aws_ssm_parameter" "db_port" { name = "${local.prefixo_ssm}/db/port" }
data "aws_ssm_parameter" "db_name" { name = "${local.prefixo_ssm}/db/name" }
data "aws_ssm_parameter" "db_secret_arn" { name = "${local.prefixo_ssm}/db/secret-arn" }

# Credenciais lidas AQUI, no apply — não em runtime pela função.
# A Lambda vive na VPC sem NAT: não alcançaria o Secrets Manager sozinha,
# e abrir esse caminho custaria NAT Gateway ou VPC endpoint pago.
# Trade-off registrado em plano.md, decisão 4.
data "aws_secretsmanager_secret_version" "banco" {
  secret_id = nonsensitive(data.aws_ssm_parameter.db_secret_arn.value)
}

data "aws_iam_role" "lab" {
  count = var.role_arn_lambda == "" ? 1 : 0
  name  = "LabRole"
}

locals {
  credenciais   = jsondecode(data.aws_secretsmanager_secret_version.banco.secret_string)
  role_execucao = var.role_arn_lambda != "" ? var.role_arn_lambda : one(data.aws_iam_role.lab[*].arn)
}

# ---------------------------------------------------------------------
# Empacotamento
# ---------------------------------------------------------------------
# Zipa src/ + node_modules já instalados pelo CI (npm ci --omit=dev).
# Sem layer: as duas dependências (jsonwebtoken, pg) somam poucos MB, e
# layer adicionaria um artefato a versionar sem ganho real.

data "archive_file" "pacote" {
  type        = "zip"
  source_dir  = "${path.module}/.."
  output_path = "${path.module}/.terraform/lambda-auth.zip"

  excludes = [
    "infra",
    "test",
    ".github",
    "README.md",
    ".gitignore",
    "package-lock.json",
  ]
}

resource "aws_lambda_function" "auth" {
  function_name = local.nome
  role          = local.role_execucao
  handler       = "src/index.handler"
  runtime       = "nodejs20.x"

  filename         = data.archive_file.pacote.output_path
  source_code_hash = data.archive_file.pacote.output_base64sha256

  # 512 MB dá CPU suficiente para a assinatura RSA sem encarecer: na
  # Lambda, CPU é proporcional à memória, e RS256 é sensível a isso.
  memory_size = 512
  timeout     = 15

  vpc_config {
    subnet_ids         = split(",", nonsensitive(data.aws_ssm_parameter.subnet_ids.value))
    security_group_ids = [nonsensitive(data.aws_ssm_parameter.sg_lambda.value)]
  }

  environment {
    variables = {
      DB_HOST     = nonsensitive(data.aws_ssm_parameter.db_endpoint.value)
      DB_PORT     = nonsensitive(data.aws_ssm_parameter.db_port.value)
      DB_NAME     = nonsensitive(data.aws_ssm_parameter.db_name.value)
      DB_USER     = local.credenciais.username
      DB_PASSWORD = local.credenciais.password

      JWT_PRIVATE_KEY = var.jwt_private_key_base64
      JWT_ISSUER      = var.jwt_issuer
      JWT_EXPIRACAO   = var.jwt_expiracao
    }
  }

  tags = local.tags
}

# 14 dias: suficiente para investigar um incidente, sem acumular custo.
resource "aws_cloudwatch_log_group" "auth" {
  name              = "/aws/lambda/${local.nome}"
  retention_in_days = 14
  tags              = local.tags
}

# ---------------------------------------------------------------------
# API Gateway
# ---------------------------------------------------------------------
# HTTP API (v2), não REST API (v1): mais barato, menor latência, e o que
# precisamos aqui é roteamento simples — não usage plans nem transformação
# de payload.

resource "aws_apigatewayv2_api" "gateway" {
  name          = "${var.projeto}-${var.ambiente}"
  protocol_type = "HTTP"
  description   = "Entrada unica da oficina - autenticacao e aplicacao"

  cors_configuration {
    allow_origins = var.origens_cors
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_headers = ["Content-Type", "Authorization"]
    max_age       = 300
  }

  tags = local.tags
}

resource "aws_apigatewayv2_integration" "auth" {
  api_id                 = aws_apigatewayv2_api.gateway.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.auth.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "auth" {
  api_id    = aws_apigatewayv2_api.gateway.id
  route_key = "POST /auth/cliente"
  target    = "integrations/${aws_apigatewayv2_integration.auth.id}"
}

resource "aws_lambda_permission" "gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.gateway.execution_arn}/*/*"
}

resource "aws_cloudwatch_log_group" "gateway" {
  name              = "/aws/apigateway/${var.projeto}-${var.ambiente}"
  retention_in_days = 14
  tags              = local.tags
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.gateway.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.gateway.arn
    # Log em JSON para casar com o formato estruturado da aplicação e da
    # função — o mesmo requestId aparece nos três.
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      routeKey       = "$context.routeKey"
      status         = "$context.status"
      responseLength = "$context.responseLength"
      latencia       = "$context.responseLatency"
    })
  }

  # Throttling protege o /auth/cliente de força bruta de CPF — a lacuna
  # apontada em RFC 003, §6.
  default_route_settings {
    throttling_burst_limit = var.throttle_burst
    throttling_rate_limit  = var.throttle_rate
  }

  tags = local.tags
}
