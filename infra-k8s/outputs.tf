output "cluster_name" {
  description = "Nome do cluster EKS."
  value       = aws_eks_cluster.este.name
}

output "cluster_endpoint" {
  description = "Endpoint da API do cluster."
  value       = aws_eks_cluster.este.endpoint
}

output "sg_lambda_id" {
  description = "Security group da Lambda — consumido pelo repo 1."
  value       = aws_security_group.lambda.id
}

output "sg_banco_id" {
  description = "Security group do RDS — consumido pelo repo 3."
  value       = aws_security_group.banco.id
}

output "subnet_ids" {
  description = "Subnets da VPC default."
  value       = data.aws_subnets.default.ids
}

output "kubeconfig" {
  description = "Comando para apontar o kubectl para este cluster."
  value       = "aws eks update-kubeconfig --region ${var.regiao} --name ${aws_eks_cluster.este.name}"
}
