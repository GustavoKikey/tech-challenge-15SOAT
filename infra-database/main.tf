# =====================================================================
# RDS PostgreSQL — banco de dados gerenciado
# ---------------------------------------------------------------------
# Justificativa da escolha: RFC 002. Schema e relacionamentos:
# modelagem-dados.md. As migrations são aplicadas pela própria aplicação
# no startup (Flyway), não aqui — o Terraform cria a instância vazia.
# =====================================================================

locals {
  prefixo_ssm = "/${var.projeto}/${var.ambiente}"
  identifier  = "${var.projeto}-${var.ambiente}"

  tags = {
    Projeto   = var.projeto
    Ambiente  = var.ambiente
    ManagedBy = "terraform"
    Repo      = "infra-database"
  }
}

# ---------------------------------------------------------------------
# Rede — vinda do repo 2 via SSM
# ---------------------------------------------------------------------
# Ler daqui em vez de terraform_remote_state: o nome do parâmetro é um
# contrato explícito, e não exige permissão de leitura no state alheio
# (que carrega segredos em texto claro). ADR 004, §3.

data "aws_ssm_parameter" "subnet_ids" {
  name = "${local.prefixo_ssm}/rede/subnet-ids"
}

data "aws_ssm_parameter" "sg_banco_id" {
  name = "${local.prefixo_ssm}/rede/sg-banco-id"
}

# ---------------------------------------------------------------------
# Credenciais
# ---------------------------------------------------------------------
# A senha é gerada pelo Terraform e nunca digitada por ninguém. Vai para
# o Secrets Manager; a aplicação e a Lambda a recebem de lá.
#
# Ela também fica em texto claro no state — é por isso que o bucket do
# backend é privado e cifrado (bootstrap/main.tf).

resource "random_password" "banco" {
  length = 32
  # Alguns caracteres especiais quebram a URL JDBC ou o parsing do
  # cliente pg; este conjunto é seguro nos dois.
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_secretsmanager_secret" "banco" {
  name        = "${local.identifier}-db-credentials"
  description = "Credenciais do RDS PostgreSQL da oficina"

  # O Learner Lab não permite reaproveitar nome de secret excluído antes
  # da janela de recuperação. Zero dias evita travar o re-apply.
  recovery_window_in_days = 0

  tags = local.tags
}

resource "aws_secretsmanager_secret_version" "banco" {
  secret_id = aws_secretsmanager_secret.banco.id
  secret_string = jsonencode({
    username = var.usuario_master
    password = random_password.banco.result
    dbname   = var.nome_banco
    host     = aws_db_instance.este.address
    port     = aws_db_instance.este.port
  })
}

# ---------------------------------------------------------------------
# Instância
# ---------------------------------------------------------------------

resource "aws_db_subnet_group" "este" {
  name       = "${local.identifier}-subnets"
  subnet_ids = split(",", nonsensitive(data.aws_ssm_parameter.subnet_ids.value))
  tags       = local.tags
}

resource "aws_db_instance" "este" {
  identifier = local.identifier
  engine     = "postgres"
  # Só o major: a AWS resolve a minor mais recente disponível.
  engine_version             = var.versao_postgres
  auto_minor_version_upgrade = true
  instance_class             = var.classe_instancia

  db_name  = var.nome_banco
  username = var.usuario_master
  password = random_password.banco.result

  allocated_storage     = var.armazenamento_gb
  max_allocated_storage = var.armazenamento_gb * 2 # autoscaling de disco
  storage_type          = "gp3"
  storage_encrypted     = true

  db_subnet_group_name   = aws_db_subnet_group.este.name
  vpc_security_group_ids = [nonsensitive(data.aws_ssm_parameter.sg_banco_id.value)]

  # NÃO acessível pela internet. O único caminho é pelo security group,
  # que só aceita 5432 vindo do cluster e da Lambda (repo 2, network.tf).
  publicly_accessible = false

  backup_retention_period = var.dias_backup
  skip_final_snapshot     = var.ambiente != "prod"
  deletion_protection     = var.ambiente == "prod"

  # Multi-AZ desligado: dobraria o custo e estoura o crédito de US$ 50 do
  # Learner Lab. Débito conhecido e declarado — RFC 002, §4.
  multi_az = false

  # Envia os logs do Postgres para o CloudWatch. Sem isso, erro de
  # conexão da Lambda fica invisível.
  enabled_cloudwatch_logs_exports = ["postgresql"]

  apply_immediately = var.ambiente != "prod"

  tags = local.tags
}
