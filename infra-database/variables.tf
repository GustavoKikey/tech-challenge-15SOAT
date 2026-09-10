variable "ambiente" {
  description = "Ambiente lógico (hom | prod). Prefixa os parâmetros no SSM."
  type        = string

  validation {
    condition     = contains(["hom", "prod"], var.ambiente)
    error_message = "Ambiente deve ser 'hom' ou 'prod'."
  }
}

variable "regiao" {
  description = "Região AWS. O Learner Lab só libera us-east-1."
  type        = string
  default     = "us-east-1"
}

variable "projeto" {
  description = "Nome do projeto, usado em nomes de recurso e no path do SSM."
  type        = string
  default     = "oficina"
}

variable "versao_postgres" {
  description = "Versão do PostgreSQL. Mesma dos testes (Testcontainers) e da fase 2."
  type        = string
  default     = "16.4"
}

variable "classe_instancia" {
  description = "Classe da instância. db.t3.micro está no free tier."
  type        = string
  default     = "db.t3.micro"
}

variable "armazenamento_gb" {
  description = "Armazenamento inicial em GB. 20 é o mínimo do free tier."
  type        = number
  default     = 20
}

variable "nome_banco" {
  description = "Nome do banco criado na instância."
  type        = string
  default     = "oficina"
}

variable "usuario_master" {
  description = "Usuário master. 'admin' e 'postgres' são reservados pelo RDS."
  type        = string
  default     = "oficina"
}

variable "dias_backup" {
  description = "Retenção de backup automático, em dias."
  type        = number
  default     = 7
}
