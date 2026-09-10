# =====================================================================
# Contrato com os outros repositórios
# ---------------------------------------------------------------------
# Este repo é o primeiro da cadeia: cria rede e cluster, e PUBLICA aqui
# o que os repos 3 (banco), 1 (lambda) e 4 (app) precisam saber.
#
# Preferido a terraform_remote_state porque o nome do parâmetro é um
# contrato explícito e versionado, o IAM é granular por path, e o valor
# também é legível em runtime (ADR 004, §3).
#
# Quem lê o quê: plano.md, decisão 4.
# =====================================================================

locals {
  prefixo_ssm = "/${var.projeto}/${var.ambiente}"
}

resource "aws_ssm_parameter" "vpc_id" {
  name        = "${local.prefixo_ssm}/rede/vpc-id"
  description = "VPC onde vivem cluster, lambda e banco"
  type        = "String"
  value       = data.aws_vpc.default.id
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "subnet_ids" {
  name        = "${local.prefixo_ssm}/rede/subnet-ids"
  description = "Subnets para RDS subnet group e para a Lambda na VPC"
  type        = "StringList"
  value       = join(",", local.subnets)
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "sg_lambda_id" {
  name        = "${local.prefixo_ssm}/rede/sg-lambda-id"
  description = "Security group da Function de autenticacao"
  type        = "String"
  value       = aws_security_group.lambda.id
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "sg_banco_id" {
  name        = "${local.prefixo_ssm}/rede/sg-banco-id"
  description = "Security group do RDS - consumido pelo repo do banco"
  type        = "String"
  value       = aws_security_group.banco.id
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "cluster_name" {
  name        = "${local.prefixo_ssm}/eks/cluster-name"
  description = "Nome do cluster - o repo da app usa no update-kubeconfig"
  type        = "String"
  value       = aws_eks_cluster.este.name
  overwrite   = true
  tags        = local.tags
}

resource "aws_ssm_parameter" "cluster_endpoint" {
  name        = "${local.prefixo_ssm}/eks/endpoint"
  description = "Endpoint da API do cluster"
  type        = "String"
  value       = aws_eks_cluster.este.endpoint
  overwrite   = true
  tags        = local.tags
}
