# =====================================================================
# Bootstrap do backend remoto — aplicado UMA ÚNICA VEZ, à mão
# ---------------------------------------------------------------------
# Cria o bucket S3 e a tabela DynamoDB que guardam e travam o state dos
# três repositórios de infraestrutura.
#
# Problema do ovo e da galinha: este módulo não pode usar backend remoto,
# porque é ele que cria o backend. Roda com state local e o resultado é
# descartável — se o state local sumir, basta importar os dois recursos.
#
# Uso:
#   cd bootstrap
#   terraform init
#   terraform apply -var="sufixo=SEU-IDENTIFICADOR-UNICO"
#
# Depois, nos três repos de infra:
#   terraform init \
#     -backend-config="bucket=oficina-tfstate-SEU-SUFIXO" \
#     -backend-config="dynamodb_table=oficina-tflock-SEU-SUFIXO"
# =====================================================================

terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
  }
}

provider "aws" {
  region = var.regiao
}

variable "regiao" {
  type    = string
  default = "us-east-1"
}

variable "sufixo" {
  description = <<-EOT
    Sufixo único do bucket. Nome de bucket S3 é global em toda a AWS —
    "oficina-tfstate" certamente já existe. Use algo como seu usuário
    do GitHub ou o número da conta.
  EOT
  type        = string
}

resource "aws_s3_bucket" "state" {
  bucket = "oficina-tfstate-${var.sufixo}"

  # O state guarda dados sensíveis (inclusive senha do RDS em texto
  # claro). Destruir por acidente perderia o rastro de tudo que existe.
  lifecycle {
    prevent_destroy = true
  }
}

# Versionamento é a rede de segurança do state: um apply que corrompe
# permite voltar para a versão anterior.
resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Trava de concorrência: impede que o CI e alguém do grupo apliquem ao
# mesmo tempo e corrompam o state.
resource "aws_dynamodb_table" "lock" {
  name         = "oficina-tflock-${var.sufixo}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  lifecycle {
    prevent_destroy = true
  }
}

output "backend_config" {
  description = "Cole no terraform init dos repos de infraestrutura."
  value       = <<-EOT
    terraform init \
      -backend-config="bucket=${aws_s3_bucket.state.id}" \
      -backend-config="dynamodb_table=${aws_dynamodb_table.lock.name}"
  EOT
}
