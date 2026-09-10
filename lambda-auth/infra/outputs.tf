output "invoke_url" {
  description = "URL base do API Gateway."
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "endpoint_autenticacao" {
  description = "Endpoint completo de autenticação por CPF."
  value       = "${aws_apigatewayv2_stage.default.invoke_url}/auth/cliente"
}

output "lambda_name" {
  description = "Nome da função — para ver logs com aws logs tail."
  value       = aws_lambda_function.auth.function_name
}
