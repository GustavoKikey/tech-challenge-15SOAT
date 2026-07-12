# =====================================================================
# Variáveis — todos os valores têm default funcional para o ambiente local.
# Para sobrescrever: 'terraform apply -var="db_password=outra"' ou um
# arquivo *.tfvars (não versionado, ver .gitignore).
# =====================================================================

variable "cluster_name" {
  description = "Nome do cluster kind (contexto kubectl vira 'kind-<nome>')."
  type        = string
  default     = "oficina"
}

variable "namespace" {
  description = "Namespace Kubernetes da aplicação (o mesmo usado nos manifestos em k8s/)."
  type        = string
  default     = "oficina"
}

variable "app_host_port" {
  description = "Porta no localhost mapeada para o NodePort da aplicação."
  type        = number
  default     = 8080
}

variable "app_node_port" {
  description = "NodePort do Service da aplicação — deve casar com k8s/40-service.yaml."
  type        = number
  default     = 30080
}

variable "db_name" {
  description = "Nome do database Postgres."
  type        = string
  default     = "oficina"
}

variable "db_user" {
  description = "Usuário do Postgres."
  type        = string
  default     = "oficina"
}

variable "db_password" {
  description = "Senha do Postgres. Default apenas para o ambiente local — em produção, injetar via tfvars/cofre de segredos."
  type        = string
  default     = "oficina"
  sensitive   = true
}

variable "db_storage_size" {
  description = "Tamanho do volume persistente do banco."
  type        = string
  default     = "1Gi"
}

variable "postgres_image" {
  description = "Imagem do Postgres (mesma major usada no docker-compose e nos testes)."
  type        = string
  default     = "postgres:16-alpine"
}

variable "metrics_server_chart_version" {
  description = "Versão do chart Helm do metrics-server (dependência do HPA)."
  type        = string
  default     = "3.12.2"
}
