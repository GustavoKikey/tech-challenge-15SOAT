# =====================================================================
# Cluster Kubernetes local (kind — Kubernetes in Docker)
# ---------------------------------------------------------------------
# Por que kind?
# - Os nós do cluster rodam como containers no Docker já instalado —
#   zero custo, sem dependência de conta em cloud e reproduzível em
#   qualquer máquina.
# - A topologia (control-plane + worker) e os manifestos são os mesmos
#   de um cluster gerenciado; migrar para EKS/GKE/AKS trocaria apenas
#   este arquivo (ver infra/README.md).
#
# Topologia:
# - 1 control-plane: componentes de controle (API server, scheduler...)
# - 1 worker: onde os pods da aplicação e do banco são agendados
#
# O extra_port_mappings publica o NodePort 30080 do Service da app
# (k8s/40-service.yaml) em http://localhost:8080 na sua máquina.
# =====================================================================

resource "kind_cluster" "oficina" {
  name           = var.cluster_name
  wait_for_ready = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"

      extra_port_mappings {
        container_port = var.app_node_port
        host_port      = var.app_host_port
        protocol       = "TCP"
      }
    }

    node {
      role = "worker"
    }
  }
}
