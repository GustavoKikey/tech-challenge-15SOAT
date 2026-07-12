# Manifestos Kubernetes — aplicação Oficina

Esta pasta contém **apenas a aplicação**. A infraestrutura de que ela depende
(cluster kind, banco Postgres, Secret `oficina-db-credentials`, metrics-server)
é provisionada pelo Terraform em [`infra/`](../infra).

## Arquivos (na ordem de aplicação)

| Arquivo | Recurso | O que faz |
| --- | --- | --- |
| `00-namespace.yaml` | Namespace | Isola os recursos da oficina no namespace `oficina`. |
| `10-configmap.yaml` | ConfigMap | Configuração **não sensível** (URL do banco, mailer, caminhos das chaves JWT, opções da JVM). |
| `20-secret-app.yaml` | Secret | Variáveis **sensíveis** da app (`ADMIN_PASSWORD`, credenciais SMTP). Valores de desenvolvimento — ver aviso no arquivo. |
| `21-secret-jwt.yaml` | Secret | Par de chaves RSA do JWT, montado como arquivos em `/etc/oficina/jwt`. |
| `30-deployment.yaml` | Deployment | Os pods da API Quarkus: probes de health, requests/limits, rolling update sem downtime, container hardening. |
| `40-service.yaml` | Service (NodePort) | DNS estável `oficina-app` + balanceamento entre os pods; NodePort 30080 → `http://localhost:8080` via kind. |
| `50-hpa.yaml` | HorizontalPodAutoscaler | Escala de 2 a 5 réplicas conforme CPU (70%) e memória (80%). |
| `60-pdb.yaml` | PodDisruptionBudget | Garante ≥1 pod no ar durante manutenções do cluster. |

## Como aplicar

```bash
# Pré-requisito: cluster provisionado (cd infra && terraform apply)

# Caminho feliz — script que faz build da imagem, kind load e apply:
./scripts/deploy-app.sh          # Linux/macOS/Git Bash
.\scripts\deploy-app.ps1         # Windows PowerShell

# Ou manualmente:
docker build -t oficina-mvp:latest .
kind load docker-image oficina-mvp:latest --name oficina
kubectl apply -f k8s/
kubectl -n oficina rollout status deployment/oficina-app
```

A aplicação fica em **http://localhost:8080** (NodePort mapeado pelo kind).

## Teste da escala automática (HPA)

```bash
# Terminais de acompanhamento:
kubectl -n oficina get hpa -w      # decisões do autoscaler
kubectl -n oficina get pods -w     # pods nascendo/morrendo

# Liga a carga: 3 pods martelando POST /auth/login (BCrypt = CPU cara)
kubectl apply -f scripts/gerador-carga.yaml

# Em 1–3 min a CPU passa do alvo (70%) e as réplicas sobem de 2 até 5.
# Consumo por pod: kubectl -n oficina top pods

# Desliga a carga (scale-down de volta a 2 leva alguns minutos — proposital):
kubectl delete -f scripts/gerador-carga.yaml
```

Comportamento observado em teste: CPU `4% → 317%` do request e réplicas
`2 → 5` em ~30 segundos após o estouro do alvo.

## Usando esta pasta sem o Terraform (ex.: minikube)

Os manifestos assumem que o Secret `oficina-db-credentials` e o Service
`oficina-db` (Postgres) existem — o Terraform os cria. Sem ele, crie o
banco por conta própria e o Secret manualmente:

```bash
kubectl -n oficina create secret generic oficina-db-credentials \
  --from-literal=username=oficina --from-literal=password=oficina
```

O HPA também exige o **metrics-server** instalado no cluster.
