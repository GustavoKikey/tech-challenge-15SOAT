# =====================================================================
# Rede
# ---------------------------------------------------------------------
# A VPC NÃO é criada: usamos a default da conta.
#
# Motivo (plano.md, decisão 4): uma VPC própria com subnets privadas
# exigiria NAT Gateway (~US$ 32/mês) para a Lambda alcançar qualquer
# serviço AWS, ou VPC endpoints (~US$ 7/mês cada). Com US$ 50 de crédito
# no Learner Lab, isso consome o orçamento da fase inteira sem nenhum
# ganho pedagógico — a topologia que interessa (RDS privado, acesso
# controlado por security group) é demonstrável na VPC default.
#
# O isolamento real vem dos SECURITY GROUPS abaixo, não da VPC.
# =====================================================================

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# ---------------------------------------------------------------------
# Nem toda AZ aceita control plane de EKS
# ---------------------------------------------------------------------
# A VPC default cobre todas as AZs da região, mas o EKS recusa criar o
# control plane em algumas delas — em us-east-1, a us-east-1e. O erro é
# explícito (UnsupportedAvailabilityZoneException), mas só aparece depois
# de os security groups já terem sido criados.
#
# Por isso as subnets são resolvidas uma a uma, para filtrar por AZ.

data "aws_subnet" "cada" {
  for_each = toset(data.aws_subnets.default.ids)
  id       = each.value
}

locals {
  # Subnets utilizáveis pelo cluster, pelo RDS e pela Lambda.
  subnets = sort([
    for s in data.aws_subnet.cada : s.id
    if !contains(var.azs_sem_eks, s.availability_zone)
  ])
}

# ---------------------------------------------------------------------
# Security groups — três papéis, três grupos
# ---------------------------------------------------------------------

resource "aws_security_group" "cluster" {
  name        = "${var.projeto}-${var.ambiente}-cluster"
  description = "Nos do cluster Kubernetes"
  vpc_id      = data.aws_vpc.default.id

  egress {
    description = "Saida liberada: pull de imagem, OTLP para o New Relic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "lambda" {
  name        = "${var.projeto}-${var.ambiente}-lambda"
  description = "Function de autenticacao por CPF"
  vpc_id      = data.aws_vpc.default.id

  # Sem regra de egress para a internet de propósito: a função só precisa
  # falar com o RDS. Endpoint, senha e chave privada chegam como variáveis
  # de ambiente, injetadas no terraform apply — não são lidas do Secrets
  # Manager em runtime (ADR 004, §3).
  egress {
    description     = "Somente PostgreSQL, e somente para o SG do banco"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.banco.id]
  }

  tags = local.tags
}

resource "aws_security_group" "banco" {
  name        = "${var.projeto}-${var.ambiente}-banco"
  description = "RDS PostgreSQL - aceita apenas cluster e lambda"
  vpc_id      = data.aws_vpc.default.id

  tags = local.tags
}

# Regras separadas do bloco do SG para evitar dependência circular:
# lambda -> banco e banco <- lambda se referenciam mutuamente.

resource "aws_vpc_security_group_ingress_rule" "banco_do_cluster" {
  security_group_id            = aws_security_group.banco.id
  description                  = "PostgreSQL a partir dos nos do cluster"
  referenced_security_group_id = aws_security_group.cluster.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "banco_da_lambda" {
  security_group_id            = aws_security_group.banco.id
  description                  = "PostgreSQL a partir da funcao de autenticacao"
  referenced_security_group_id = aws_security_group.lambda.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

# O banco não inicia conexão com ninguém.
resource "aws_vpc_security_group_egress_rule" "banco_sem_saida" {
  security_group_id = aws_security_group.banco.id
  description       = "Sem saida - o banco nunca inicia conexao"
  cidr_ipv4         = "127.0.0.1/32"
  ip_protocol       = "-1"
}
