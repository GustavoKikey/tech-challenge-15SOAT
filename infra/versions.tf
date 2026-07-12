# =====================================================================
# Versões — Terraform e providers utilizados
# ---------------------------------------------------------------------
# - tehcyx/kind ........ cria o cluster Kubernetes local (kind = nós do
#                        cluster rodando como containers no Docker)
# - hashicorp/kubernetes provisiona recursos DENTRO do cluster
#                        (namespace, banco, secret de credenciais)
# - hashicorp/helm ..... instala o metrics-server (dependência do HPA);
#                        usa o SDK do Helm embutido — não requer o
#                        binário 'helm' instalado na máquina
# =====================================================================
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kind = {
      source  = "tehcyx/kind"
      version = "~> 0.6"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.16"
    }
  }
}
