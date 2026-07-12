# Infraestrutura como Código — Terraform

Provisiona toda a infraestrutura do projeto **localmente**, usando
[kind](https://kind.sigs.k8s.io/) (Kubernetes in Docker): os nós do cluster
rodam como containers no Docker Desktop. A opção por cluster local é
gratuita, reproduzível em qualquer máquina com Docker e sem dependência de
conta em cloud. A seção final explica o caminho de migração para cloud.

## Recursos criados

| # | Recurso Terraform | Arquivo | O que é |
| --- | --- | --- | --- |
| 1 | `kind_cluster.oficina` | `cluster.tf` | Cluster Kubernetes local com 2 nós (control-plane + worker). Publica o NodePort da app em `http://localhost:8080`. |
| 2 | `helm_release.metrics_server` | `metrics-server.tf` | metrics-server no `kube-system` — fornece as métricas de CPU/memória que o HPA consome. |
| 3 | `kubernetes_namespace_v1.oficina` | `database.tf` | Namespace `oficina`, onde app e banco vivem. |
| 4 | `kubernetes_secret_v1.db_credentials` | `database.tf` | Secret `oficina-db-credentials` — fonte única das credenciais do banco (lido pelo Postgres **e** pelo Deployment da app). |
| 5 | `kubernetes_service_v1.db` | `database.tf` | Service ClusterIP `oficina-db` — DNS estável do banco dentro do cluster. |
| 6 | `kubernetes_stateful_set_v1.db` | `database.tf` | **Banco de dados** PostgreSQL 16 com volume persistente de 1Gi (PVC) — os dados sobrevivem a reinícios do pod. |

Divisão de responsabilidade proposital:

- **Terraform (`infra/`)** = infraestrutura: cluster, banco, credenciais, métricas.
- **Manifestos (`k8s/`)** = aplicação: Deployment, Service, ConfigMap, Secrets, HPA.
- A **pipeline de CD** (`.github/workflows/cd.yml`) usa exatamente esta divisão a cada
  push na `main`: `terraform apply` desta pasta (cluster + banco) e, em seguida,
  `kubectl apply -f k8s/` com a imagem recém-publicada.

## Pré-requisitos

| Ferramenta | Instalação (Windows) | Para quê |
| --- | --- | --- |
| Docker Desktop | já instalado | roda os nós do kind e o build da imagem |
| Terraform ≥ 1.5 | `winget install Hashicorp.Terraform` | aplicar esta pasta |
| kubectl | já vem com o Docker Desktop | interagir com o cluster |
| kind | `winget install Kubernetes.kind` | `kind load` da imagem da app (o Terraform embute o kind; o CLI é usado pelos scripts de deploy) |

> O binário `helm` **não** é necessário — o provider Terraform embute o SDK do Helm.

## Como aplicar

```bash
cd infra

# 1) Baixa os providers (primeira vez apenas)
terraform init

# 2) Mostra o que será criado (revisão)
terraform plan

# 3) Cria tudo (~2 a 4 minutos)
terraform apply
```

Ao final, os outputs mostram o contexto kubectl, a URL JDBC interna do banco e
o próximo passo (deploy da aplicação com `scripts/deploy-app.ps1` ou `.sh`).

Verificação rápida:

```bash
kubectl config use-context kind-oficina
kubectl get nodes                     # 2 nós Ready
kubectl -n oficina get pods           # oficina-db-0 Running (1/1)
kubectl top nodes                     # metrics-server respondendo (aguarde ~1 min)
```

## Variáveis

Todas têm default funcional (ver `variables.tf`). Para sobrescrever, use
`-var` ou um arquivo `*.tfvars` (ignorado pelo git):

```bash
terraform apply -var="db_password=senha-forte"
```

| Variável | Default | Descrição |
| --- | --- | --- |
| `cluster_name` | `oficina` | Nome do cluster kind (contexto = `kind-oficina`). |
| `namespace` | `oficina` | Namespace da aplicação. |
| `app_host_port` | `8080` | Porta no localhost mapeada para a app. |
| `app_node_port` | `30080` | NodePort do Service (deve casar com `k8s/40-service.yaml`). |
| `db_name` / `db_user` | `oficina` | Database e usuário do Postgres. |
| `db_password` | `oficina` | Senha do banco (**sensível** — trocar fora do ambiente local). |
| `db_storage_size` | `1Gi` | Tamanho do volume persistente. |
| `postgres_image` | `postgres:16-alpine` | Mesma major do docker-compose e dos testes. |
| `metrics_server_chart_version` | `3.12.2` | Versão do chart Helm do metrics-server. |

## Como destruir

```bash
cd infra
terraform destroy
```

Remove o cluster inteiro (nós, banco, volume, app — tudo que vivia nele).
Fallback caso o estado do Terraform se perca: `kind delete cluster --name oficina`.

## Caminho para a cloud (evolução)

A separação em arquivos torna a migração pontual:

- `cluster.tf` → módulo EKS/GKE/AKS (ex.: `terraform-aws-modules/eks`);
- `database.tf` → banco gerenciado (ex.: `aws_db_instance` / RDS Postgres) —
  o Secret `oficina-db-credentials` passa a receber o endpoint/senha do RDS,
  e **nada muda nos manifestos da aplicação**, que continuam lendo o mesmo Secret;
- `metrics-server.tf` → já vem instalado na maioria dos clusters gerenciados;
- estado local (`terraform.tfstate`) → backend remoto (S3 + lock, Terraform Cloud).
