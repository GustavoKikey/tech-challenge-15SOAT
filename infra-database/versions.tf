# =====================================================================
# Repo 3 — Infraestrutura do Banco de Dados Gerenciado
# ---------------------------------------------------------------------
# Segundo da cadeia: consome a rede publicada pelo repo 2 (infra-k8s) e
# publica no SSM o endpoint que a Lambda (repo 1) e a aplicação (repo 4)
# usam para conectar.
#
# Ordem de aplicação: 2 -> [3] -> 1 -> 4
# =====================================================================
terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  backend "s3" {
    key     = "infra-database/terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
  }
}
