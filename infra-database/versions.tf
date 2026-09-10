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

  # A chave do state NÃO fica fixa aqui: ela carrega o ambiente e chega por
  # -backend-config no init (ver scripts/fase-3/02-aplicar.sh).
  #
  # Com uma chave única por módulo, "terraform apply -var=ambiente=prod" não
  # cria um segundo ambiente: ele reescreve o único state existente, e o
  # Terraform destrói os recursos de hom para recriá-los como prod.
  # Ver docs/fase-3/adr/adr-005-state-por-ambiente.md.
  backend "s3" {
    region  = "us-east-1"
    encrypt = true
  }
}
