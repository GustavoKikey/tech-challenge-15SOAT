# =====================================================================
# Banco de dados — PostgreSQL 16 dentro do cluster
# ---------------------------------------------------------------------
# O banco faz parte da infraestrutura (Terraform), separado do ciclo de
# deploy da aplicação (manifestos em k8s/).
#
# Composição:
# - Namespace 'oficina' ........ isolamento dos recursos da aplicação
# - Secret de credenciais ...... fonte ÚNICA de usuário/senha do banco
#                                (o Deployment da app lê deste mesmo
#                                Secret — nada de senha duplicada)
# - StatefulSet + PVC .......... Postgres com volume persistente: o pod
#                                pode morrer/reiniciar que os dados ficam
# - Service ClusterIP .......... DNS estável 'oficina-db' para a app
#
# Em cloud, este arquivo seria substituído por um banco gerenciado
# (ex.: aws_db_instance / RDS) — ver infra/README.md.
# =====================================================================

resource "kubernetes_namespace_v1" "oficina" {
  metadata {
    name = var.namespace

    labels = {
      "app.kubernetes.io/part-of" = "oficina"
    }
  }

  depends_on = [kind_cluster.oficina]
}

resource "kubernetes_secret_v1" "db_credentials" {
  metadata {
    name      = "oficina-db-credentials"
    namespace = kubernetes_namespace_v1.oficina.metadata[0].name

    labels = {
      "app.kubernetes.io/name"    = "oficina-db"
      "app.kubernetes.io/part-of" = "oficina"
    }
  }

  data = {
    username = var.db_user
    password = var.db_password
  }
}

resource "kubernetes_service_v1" "db" {
  metadata {
    name      = "oficina-db"
    namespace = kubernetes_namespace_v1.oficina.metadata[0].name

    labels = {
      "app.kubernetes.io/name"    = "oficina-db"
      "app.kubernetes.io/part-of" = "oficina"
    }
  }

  spec {
    selector = {
      "app.kubernetes.io/name" = "oficina-db"
    }

    port {
      name        = "postgres"
      port        = 5432
      target_port = 5432
    }
  }
}

resource "kubernetes_stateful_set_v1" "db" {
  metadata {
    name      = "oficina-db"
    namespace = kubernetes_namespace_v1.oficina.metadata[0].name

    labels = {
      "app.kubernetes.io/name"      = "oficina-db"
      "app.kubernetes.io/component" = "database"
      "app.kubernetes.io/part-of"   = "oficina"
    }
  }

  spec {
    service_name = kubernetes_service_v1.db.metadata[0].name
    replicas     = 1

    selector {
      match_labels = {
        "app.kubernetes.io/name" = "oficina-db"
      }
    }

    template {
      metadata {
        labels = {
          "app.kubernetes.io/name"      = "oficina-db"
          "app.kubernetes.io/component" = "database"
          "app.kubernetes.io/part-of"   = "oficina"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = var.postgres_image

          port {
            name           = "postgres"
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = var.db_name
          }

          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.db_credentials.metadata[0].name
                key  = "username"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.db_credentials.metadata[0].name
                key  = "password"
              }
            }
          }

          # Subdiretório evita conflito do initdb com o lost+found
          # criado pelo provisionador de volumes.
          env {
            name  = "PGDATA"
            value = "/var/lib/postgresql/data/pgdata"
          }

          volume_mount {
            name       = "dados"
            mount_path = "/var/lib/postgresql/data"
          }

          resources {
            requests = {
              cpu    = "100m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }

          readiness_probe {
            exec {
              command = ["/bin/sh", "-c", "pg_isready -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\""]
            }
            initial_delay_seconds = 5
            period_seconds        = 5
            timeout_seconds       = 3
          }

          liveness_probe {
            exec {
              command = ["/bin/sh", "-c", "pg_isready -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\""]
            }
            initial_delay_seconds = 30
            period_seconds        = 20
            timeout_seconds       = 3
          }
        }
      }
    }

    # PVC gerado automaticamente para o pod: usa a StorageClass default
    # do kind (rancher local-path). Os dados sobrevivem a reinícios do pod.
    volume_claim_template {
      metadata {
        name = "dados"
      }

      spec {
        access_modes = ["ReadWriteOnce"]

        resources {
          requests = {
            storage = var.db_storage_size
          }
        }
      }
    }
  }
}
