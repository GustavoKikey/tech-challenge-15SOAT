# Publicado para o repo 4 (aplicação) e para o README/vídeo.
resource "aws_ssm_parameter" "apigw_invoke_url" {
  name        = "${local.prefixo_ssm}/apigw/invoke-url"
  description = "URL base do API Gateway"
  type        = "String"
  value       = local.url_base
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "apigw_id" {
  name        = "${local.prefixo_ssm}/apigw/api-id"
  description = "ID do HTTP API - o repo da app adiciona as rotas da aplicacao"
  type        = "String"
  value       = aws_apigatewayv2_api.gateway.id
  overwrite   = true
  tags        = local.tags
}
