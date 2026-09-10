terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.6"
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

provider "aws" {
  region = var.regiao

  default_tags {
    tags = {
      Projeto   = var.projeto
      Ambiente  = var.ambiente
      ManagedBy = "terraform"
    }
  }
}
