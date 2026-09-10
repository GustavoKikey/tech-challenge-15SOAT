# O invoke_url do stage $default termina com barra. Concatenar "/auth/cliente"
# direto produziria ".../com//auth/cliente" — que funciona, mas aparece em toda
# demonstração e em todo comando copiado do README.
locals {
  url_base = trimsuffix(aws_apigatewayv2_stage.default.invoke_url, "/")
}

output "invoke_url" {
  description = "URL base do API Gateway, sem barra no fim."
  value       = local.url_base
}

output "endpoint_autenticacao" {
  description = "Endpoint completo de autenticação por CPF."
  value       = "${local.url_base}/auth/cliente"
}

output "lambda_name" {
  description = "Nome da função — para ver logs com aws logs tail."
  value       = aws_lambda_function.auth.function_name
}
