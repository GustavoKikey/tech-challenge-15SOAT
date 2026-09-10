# =====================================================================
# Cluster Kubernetes (EKS)
# ---------------------------------------------------------------------
# O AWS Academy Learner Lab não permite CRIAR IAM roles, mas provisiona
# roles prontas para o EKS — que é o que este código descobre e reusa.
# Confirmado na conta do projeto em 2026-09-10 pelo
# scripts/fase-3/00-diagnostico-lab.sh.
# =====================================================================

# ---------------------------------------------------------------------
# Descoberta das roles
# ---------------------------------------------------------------------
# O Learner Lab provisiona roles DEDICADAS ao EKS, com nomes que carregam
# um sufixo aleatório por conta — daí a busca por regex em vez de nome fixo:
#
#   ...-LabEksClusterRole-XXXX  -> control plane
#   ...-LabEksNodeRole-XXXX     -> nós
#
# Usar a LabRole genérica para os nós PARECE funcionar (ela tem
# AmazonEKSWorkerNodePolicy), mas falta a AmazonEKS_CNI_Policy: sem ela o
# VPC CNI não atribui IP aos pods e os nós ficam NotReady, com um erro que
# não menciona IAM em lugar nenhum. Por isso a role dedicada tem prioridade.
#
# Verificado na conta do projeto em 2026-09-10.

data "aws_iam_roles" "eks_cluster" {
  name_regex = ".*LabEksClusterRole.*"
}

data "aws_iam_roles" "eks_nos" {
  name_regex = ".*LabEksNodeRole.*"
}

# Fallback para contas sem as roles dedicadas do lab.
data "aws_iam_role" "lab" {
  count = var.role_arn_cluster == "" ? 1 : 0
  name  = "LabRole"
}

locals {
  arns_cluster_lab = tolist(data.aws_iam_roles.eks_cluster.arns)
  arns_nos_lab     = tolist(data.aws_iam_roles.eks_nos.arns)
  arn_lab_generica = one(data.aws_iam_role.lab[*].arn)

  # Ordem de preferência: variável explícita > role dedicada do lab > LabRole.
  role_cluster = coalesce(
    var.role_arn_cluster != "" ? var.role_arn_cluster : null,
    length(local.arns_cluster_lab) > 0 ? local.arns_cluster_lab[0] : null,
    local.arn_lab_generica,
  )

  role_nos = coalesce(
    var.role_arn_nos != "" ? var.role_arn_nos : null,
    length(local.arns_nos_lab) > 0 ? local.arns_nos_lab[0] : null,
    local.role_cluster,
  )

  nome_cluster = "${var.projeto}-${var.ambiente}"

  tags = {
    Projeto   = var.projeto
    Ambiente  = var.ambiente
    ManagedBy = "terraform"
    Repo      = "infra-k8s"
  }
}

resource "aws_eks_cluster" "este" {
  name     = local.nome_cluster
  role_arn = local.role_cluster
  version  = var.versao_kubernetes

  vpc_config {
    subnet_ids         = local.subnets
    security_group_ids = [aws_security_group.cluster.id]
    # Endpoint público: o runner do GitHub Actions precisa alcançar a API
    # do cluster para o kubectl apply. Numa operação real, isso viria
    # restrito por public_access_cidrs ou por runner dentro da VPC.
    endpoint_public_access  = true
    endpoint_private_access = true
  }

  # Logs do control plane no CloudWatch — útil para depurar o que o
  # New Relic não enxerga (falha de autenticação na própria API do K8s).
  enabled_cluster_log_types = ["api", "audit"]

  tags = local.tags
}

resource "aws_eks_node_group" "principal" {
  cluster_name    = aws_eks_cluster.este.name
  node_group_name = "${local.nome_cluster}-nos"
  node_role_arn   = local.role_nos
  subnet_ids      = local.subnets

  instance_types = [var.tipo_instancia_no]

  scaling_config {
    desired_size = var.nos_desejados
    min_size     = var.nos_desejados
    # Teto de nós acima do desejado: o HPA escala PODS, mas sem nó livre
    # os pods novos ficam Pending. Dois nós extras cobrem as 5 réplicas.
    max_size = var.nos_desejados + 2
  }

  update_config {
    max_unavailable = 1
  }

  tags = local.tags

  # Sem isso o node group pode ser criado antes de o cluster aceitar nós.
  depends_on = [aws_eks_cluster.este]
}

# ---------------------------------------------------------------------
# metrics-server — pré-requisito do HPA
# ---------------------------------------------------------------------
# Sem ele o HPA fica com "unknown" nas métricas e nunca escala. É a mesma
# dependência que a fase 2 resolvia no cluster kind (infra/metrics-server.tf).
resource "aws_eks_addon" "metrics_server" {
  cluster_name  = aws_eks_cluster.este.name
  addon_name    = "metrics-server"
  addon_version = null # deixa a AWS escolher a versão compatível

  depends_on = [aws_eks_node_group.principal]
}
