# =====================================================================
# Repo 2 — Infraestrutura Kubernetes
# ---------------------------------------------------------------------
# Primeiro da cadeia: cria a rede (security groups sobre a VPC default) e
# o cluster, publicando no SSM o que os repos 3 (banco) e 1 (lambda)
# precisam para se conectar.
#
# Ordem de aplicação: 2 -> 3 -> 1 -> 4
# =====================================================================
terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
  }

  # State remoto: sem isso o state nasce e morre dentro do runner do
  # GitHub Actions, e não há trava entre o CI e quem aplica da máquina.
  # O bucket e a tabela são criados uma única vez por bootstrap/.
  backend "s3" {
    key     = "infra-k8s/terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
    # bucket e dynamodb_table vêm de -backend-config no init
    # (variam por aluno/turma e não devem ser fixados no código).
  }
}
