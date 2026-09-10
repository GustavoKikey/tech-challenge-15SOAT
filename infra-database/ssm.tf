# =====================================================================
# Contrato publicado para os repos 1 (lambda) e 4 (aplicação)
# ---------------------------------------------------------------------
# A senha NÃO é publicada aqui — só o ARN do secret que a contém. Quem
# precisar do valor pede ao Secrets Manager com a própria permissão.
# =====================================================================

resource "aws_ssm_parameter" "db_endpoint" {
  name        = "${local.prefixo_ssm}/db/endpoint"
  description = "Host do RDS (sem porta)"
  type        = "String"
  value       = aws_db_instance.este.address
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "db_port" {
  name        = "${local.prefixo_ssm}/db/port"
  description = "Porta do RDS"
  type        = "String"
  value       = tostring(aws_db_instance.este.port)
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "db_name" {
  name        = "${local.prefixo_ssm}/db/name"
  description = "Nome do banco"
  type        = "String"
  value       = var.nome_banco
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "db_secret_arn" {
  name        = "${local.prefixo_ssm}/db/secret-arn"
  description = "ARN do secret com usuario e senha"
  type        = "String"
  value       = aws_secretsmanager_secret.banco.arn
  overwrite   = true
  tags        = local.tags
}

# URL JDBC pronta — evita que cada consumidor a monte à mão e erre o
# formato. É o valor que vai direto para o ConfigMap da aplicação.
resource "aws_ssm_parameter" "db_jdbc_url" {
  name        = "${local.prefixo_ssm}/db/jdbc-url"
  description = "URL JDBC completa para a aplicacao"
  type        = "String"
  value       = "jdbc:postgresql://${aws_db_instance.este.address}:${aws_db_instance.este.port}/${var.nome_banco}"
  overwrite   = true
  tags        = local.tags
}
