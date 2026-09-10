# oficina-infra-k8s

Infraestrutura de **rede e cluster Kubernetes** da oficina — repositório 2 dos 4 do
Tech Challenge Fase 3 (15SOAT).

> Esta pasta é o conteúdo do repositório `oficina-infra-k8s`. Vive junto da aplicação
> enquanto a separação dos 4 repositórios não acontece.

## Propósito

**Primeiro da cadeia.** Cria os security groups sobre a VPC default e o cluster EKS, e
publica no SSM Parameter Store o que os outros três repositórios precisam para se
conectar.

```
[2] infra-k8s  ->  [3] infra-database  ->  [1] lambda-auth  ->  [4] oficina-app
```

## Tecnologias

Terraform ~> 1.9 · AWS provider ~> 5.70 · Amazon EKS · GitHub Actions

## O que provisiona

| Recurso | Observação |
| --- | --- |
| Security group do cluster | Saída liberada (pull de imagem, OTLP) |
| Security group da Lambda | **Sem saída para a internet** — só 5432 para o banco |
| Security group do banco | Só aceita 5432 do cluster e da Lambda; sem saída |
| Cluster EKS | Endpoint público e privado; logs de `api` e `audit` no CloudWatch |
| Node group | `t3.medium`, 2 nós, teto de 4 (o HPA escala pods, mas precisa de nó livre) |
| Addon metrics-server | Pré-requisito do HPA — sem ele o HPA fica em `unknown` |
| 6 parâmetros no SSM | O contrato com os outros repos |

**A VPC não é criada.** Usamos a default. Uma VPC própria com subnets privadas exigiria
NAT Gateway (~US$ 32/mês) ou VPC endpoints (~US$ 7/mês cada), o que consome o crédito de
US$ 50 do Learner Lab sem ganho pedagógico — o isolamento real vem dos security groups.
Ver [ADR 004](../docs/fase-3/adr/adr-004-padrao-de-comunicacao.md), §3.

## Contrato publicado no SSM

| Parâmetro | Consumido por |
| --- | --- |
| `/oficina/{env}/rede/vpc-id` | repos 1, 3 |
| `/oficina/{env}/rede/subnet-ids` | repos 1, 3 |
| `/oficina/{env}/rede/sg-lambda-id` | repo 1 |
| `/oficina/{env}/rede/sg-banco-id` | repo 3 |
| `/oficina/{env}/eks/cluster-name` | repo 4 |
| `/oficina/{env}/eks/endpoint` | repo 4 |

## Passo zero — bootstrap do backend

O bucket S3 e a tabela DynamoDB do state precisam existir **antes** do primeiro
`terraform init`. Aplicado uma única vez:

```bash
cd bootstrap
terraform init
terraform apply -var="sufixo=SEU-IDENTIFICADOR-UNICO"
```

O output devolve o comando de `init` pronto para os três repos de infraestrutura.

## Execução

```bash
terraform init \
  -backend-config="bucket=oficina-tfstate-SEU-SUFIXO" \
  -backend-config="dynamodb_table=oficina-tflock-SEU-SUFIXO"

terraform plan  -var="ambiente=hom"
terraform apply -var="ambiente=hom"
```

Depois, para usar o cluster:

```bash
aws eks update-kubeconfig --region us-east-1 --name oficina-hom
```

## CI/CD

`.github/workflows/terraform.yml`

| Gatilho | O que faz |
| --- | --- |
| Pull Request | `fmt`, `validate`, `tflint` e **`plan` comentado no PR** |
| push em `homolog` | `apply` no ambiente `hom` |
| push em `main` | `apply` no ambiente `prod` |

Secrets necessários: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`,
`TF_STATE_BUCKET`, `TF_LOCK_TABLE`.

> As credenciais do AWS Academy são **temporárias** (sessão de 4h). O
> `AWS_SESSION_TOKEN` precisa ser renovado nos secrets a cada sessão. Numa conta
> própria, trocar por OIDC e remover as três chaves.

## ⚠️ Risco conhecido — EKS no Learner Lab

O EKS exige uma IAM role de cluster, e o Learner Lab **não permite criar roles**. Este
código reusa a `LabRole`. Se ela não tiver trust policy para `eks.amazonaws.com`, o
apply falha.

Verificar antes, com a sessão do lab ativa:

```bash
aws iam list-roles --query "Roles[?contains(RoleName,'Lab')].[RoleName,AssumeRolePolicyDocument]" --output json
```

Plano B: cluster k3s em EC2 provisionado por Terraform. Ver
[RFC 001](../docs/fase-3/rfc/rfc-001-escolha-da-nuvem.md), §5.

## Diagrama

Ver `docs/fase-3/diagrama-componentes.md` — este repositório provisiona a caixa
**Amazon EKS** e os security groups dentro de **VPC default**.
