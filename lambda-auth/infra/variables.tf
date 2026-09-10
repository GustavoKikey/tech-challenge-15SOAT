variable "ambiente" {
  description = "Ambiente lógico (hom | prod)."
  type        = string

  validation {
    condition     = contains(["hom", "prod"], var.ambiente)
    error_message = "Ambiente deve ser 'hom' ou 'prod'."
  }
}

variable "regiao" {
  type    = string
  default = "us-east-1"
}

variable "projeto" {
  type    = string
  default = "oficina"
}

variable "jwt_private_key_base64" {
  description = <<-EOT
    Chave privada RSA em base64, para assinar os tokens de cliente.

    Vem do secret JWT_PRIVATE_KEY do GitHub Actions. Base64 evita o
    problema de quebras de linha em variável de ambiente.

    ATENÇÃO: a chave hoje versionada no repositório da aplicação deve ser
    considerada comprometida e ROTACIONADA antes do primeiro deploy real
    (plano.md, decisão 2).
  EOT
  type        = string
  sensitive   = true
}

variable "jwt_issuer" {
  description = "Issuer do token. Precisa bater com mp.jwt.verify.issuer da aplicação."
  type        = string
  default     = "oficina-mvp"
}

variable "jwt_expiracao" {
  description = "Validade do token de cliente. 30 min conforme ADR 001."
  type        = string
  default     = "30m"
}

variable "role_arn_lambda" {
  description = <<-EOT
    ARN da role de execução da função. Vazio faz o Terraform descobrir a
    LabRole do AWS Academy, que é a única utilizável lá.
  EOT
  type        = string
  default     = ""
}

variable "origens_cors" {
  description = "Origens permitidas no CORS do API Gateway."
  type        = list(string)
  default     = ["*"]
}

variable "throttle_burst" {
  description = "Rajada máxima de requisições no gateway."
  type        = number
  default     = 20
}

variable "throttle_rate" {
  description = "Requisições por segundo sustentadas no gateway."
  type        = number
  default     = 10
}
