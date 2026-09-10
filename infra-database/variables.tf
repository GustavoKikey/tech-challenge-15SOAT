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
  description = <<-EOT
    Versão do PostgreSQL. Fixada no MAJOR ("16"), não no minor: a AWS
    remove versões menores do catálogo conforme lança correções, e um
    minor fixo faz o apply falhar meses depois com
    "Cannot find version X.Y for postgres". Com apenas o major, o RDS
    escolhe a minor disponível mais recente.

    O major 16 é o mesmo dos testes (Testcontainers) e da fase 2.
  EOT
  type        = string
  default     = "16"
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
