# =====================================================================
# Outputs — informações úteis após o 'terraform apply'
# =====================================================================

output "kubectl_context" {
  description = "Contexto kubectl do cluster criado (kind registra no ~/.kube/config)."
  value       = "kind-${var.cluster_name}"
}

output "cluster_endpoint" {
  description = "Endpoint do API server do cluster."
  value       = kind_cluster.oficina.endpoint
}

output "namespace" {
  description = "Namespace onde app e banco vivem."
  value       = var.namespace
}

output "db_jdbc_url_interno" {
  description = "URL JDBC do banco vista de DENTRO do cluster (usada pela aplicação)."
  value       = "jdbc:postgresql://oficina-db.${var.namespace}.svc.cluster.local:5432/${var.db_name}"
}

output "app_url_local" {
  description = "URL da aplicação no navegador, após o deploy dos manifestos k8s/."
  value       = "http://localhost:${var.app_host_port}"
}

output "proximo_passo" {
  description = "Como colocar a aplicação no ar depois do provisionamento."
  value       = "Execute scripts/deploy-app.ps1 (Windows) ou scripts/deploy-app.sh (Linux/macOS) na raiz do repositório."
}
