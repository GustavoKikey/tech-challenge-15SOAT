output "endpoint" {
  description = "Host do RDS."
  value       = aws_db_instance.este.address
}

output "jdbc_url" {
  description = "URL JDBC completa."
  value       = "jdbc:postgresql://${aws_db_instance.este.address}:${aws_db_instance.este.port}/${var.nome_banco}"
}

output "secret_arn" {
  description = "ARN do secret com as credenciais."
  value       = aws_secretsmanager_secret.banco.arn
}

output "identifier" {
  description = "Identificador da instância RDS."
  value       = aws_db_instance.este.identifier
}
