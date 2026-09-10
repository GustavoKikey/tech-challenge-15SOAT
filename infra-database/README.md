# oficina-infra-database

Infraestrutura do **banco de dados gerenciado** — repositório 3 dos 4 do Tech Challenge
Fase 3 (15SOAT).

## Propósito

**Segundo da cadeia.** Consome a rede publicada pelo repo 2 e provisiona o RDS
PostgreSQL, publicando o endpoint que a Lambda (repo 1) e a aplicação (repo 4) usam.

```
[2] infra-k8s  ->  [3] infra-database  ->  [1] lambda-auth  ->  [4] oficina-app
```

## Tecnologias

Terraform ~> 1.9 · AWS provider ~> 5.70 · Amazon RDS for PostgreSQL 16 ·
AWS Secrets Manager · GitHub Actions

## O que provisiona

| Recurso | Configuração |
| --- | --- |
| Instância RDS | PostgreSQL 16.4, `db.t3.micro`, 20 GB gp3 cifrado |
| Subnet group | Subnets vindas do SSM (repo 2) |
| Secret | Usuário e senha gerados pelo Terraform, nunca digitados |
| Logs | `postgresql` exportado para o CloudWatch |
| 5 parâmetros no SSM | Endpoint, porta, nome, ARN do secret e URL JDBC pronta |

**Acesso:** `publicly_accessible = false`. O único caminho é pelo security group criado
no repo 2, que aceita 5432 apenas do cluster e da Lambda.

**Migrations não são aplicadas aqui.** O Terraform cria a instância vazia; o schema
(`V1`–`V7`) é aplicado pelo Flyway no startup da aplicação.

## Justificativa da escolha

PostgreSQL gerenciado, com o raciocínio completo em [RFC 002](https://github.com/GustavoKikey/tech-challenge-15SOAT/blob/main/docs/fase-3/rfc/rfc-002-escolha-do-banco.md)
e o modelo em [modelagem de dados](https://github.com/GustavoKikey/tech-challenge-15SOAT/blob/main/docs/fase-3/modelagem-dados.md). Em resumo: o domínio exige transação
ACID cruzando tabelas (aprovar orçamento baixa estoque), é fortemente relacional, e o
código já usa recursos específicos do Postgres — `NUMERIC` exato para dinheiro,
`TIMESTAMPTZ`, `UUID` nativo, `CHECK` constraints e índice parcial.

**Multi-AZ está desligado.** Dobraria o custo e estoura o crédito de US$ 50 do Learner
Lab. É um débito conhecido e declarado, não um esquecimento.

## Contrato publicado no SSM

| Parâmetro | Consumido por |
| --- | --- |
| `/oficina/{env}/db/endpoint` | repos 1, 4 |
| `/oficina/{env}/db/port` | repo 1 |
| `/oficina/{env}/db/name` | repo 1 |
| `/oficina/{env}/db/secret-arn` | repos 1, 4 |
| `/oficina/{env}/db/jdbc-url` | repo 4 (vai direto para o ConfigMap) |

A **senha nunca é publicada no SSM** — só o ARN do secret que a contém.

## Execução

Exige que o repo 2 já tenha sido aplicado no mesmo ambiente.

```bash
terraform init \
  -backend-config="bucket=oficina-tfstate-SEU-SUFIXO" \
  -backend-config="dynamodb_table=oficina-tflock-SEU-SUFIXO"

terraform plan  -var="ambiente=hom"
terraform apply -var="ambiente=hom"
```

Recuperar a senha:

```bash
aws secretsmanager get-secret-value \
  --secret-id oficina-hom-db-credentials --query SecretString --output text
```

## CI/CD

`.github/workflows/terraform.yml` — mesmo padrão do repo 2: PR faz `plan` comentado,
`homolog` aplica em `hom`, `main` aplica em `prod`.

## Diagrama

Ver [diagrama de componentes](https://github.com/GustavoKikey/tech-challenge-15SOAT/blob/main/docs/fase-3/diagrama-componentes.md) — este repositório provisiona a caixa
**RDS PostgreSQL**. O ER está em [modelagem de dados](https://github.com/GustavoKikey/tech-challenge-15SOAT/blob/main/docs/fase-3/modelagem-dados.md).
