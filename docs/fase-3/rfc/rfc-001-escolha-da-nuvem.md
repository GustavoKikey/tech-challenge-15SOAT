# RFC 001 — Escolha do provedor de nuvem

| | |
| --- | --- |
| **Status** | Aceito |
| **Autor** | Gustavo Kikey |
| **Data** | 2026-09-08 |
| **Decisão** | Amazon Web Services (AWS), região `us-east-1`, via AWS Academy Learner Lab |

---

## 1. Contexto

O enunciado da fase 3 diz textualmente **"Infraestrutura obrigatória (livre escolha de
nuvem)"** e, ao citar o API Gateway, oferece exemplos de dentro e de fora da AWS:
*"AWS API Gateway, Kong, Traefik ou outro"*. Não há obrigação de provedor.

Cinco itens precisam existir, seja onde for:

1. API Gateway para controle e roteamento
2. Function Serverless para autenticação
3. Banco de dados gerenciado
4. Cluster Kubernetes com escalabilidade
5. Terraform para provisionamento

A fase 2 rodava em cluster **kind local** com Postgres em StatefulSet. Nada disso
sobrevive à fase 3: é preciso escolher um provedor real.

## 2. Proposta

Adotar **AWS**, com o mapeamento direto:

| Requisito | Serviço |
| --- | --- |
| API Gateway | Amazon API Gateway (HTTP API) |
| Function Serverless | AWS Lambda (Node.js 20) |
| Banco gerenciado | Amazon RDS for PostgreSQL |
| Cluster Kubernetes | Amazon EKS |
| IaC | Terraform, provider `hashicorp/aws` |

## 3. Alternativas avaliadas

| Provedor | A favor | Contra |
| --- | --- | --- |
| **AWS** | Mapeamento 1:1 sem adaptação; provider Terraform mais maduro; é a referência da trilha SOAT — mesmo vocabulário do avaliador; a turma já tem crédito no Learner Lab | EKS não tem free tier (~US$ 0,10/h de control plane); o Learner Lab impõe restrições severas (§5) |
| **Google Cloud** | GKE com operação mais simples; Cloud Run seria elegante para a função | Exigiria conta e cartão próprios; afasta do material do curso; sem crédito disponível |
| **Azure** | AKS sem custo de control plane — vantagem real de custo | Menos material na trilha; provider Terraform com mais arestas; sem crédito disponível |
| **Oracle Cloud** | OKE com control plane gratuito e free tier generoso | Ecossistema pouco usado no mercado-alvo; documentação e comunidade menores; nada do curso cobre |
| **Multi-cloud** (ex.: cluster em um, serverless em outro) | Otimizaria custo por serviço | Multiplica complexidade de rede, IAM e CI/CD por dois, sem ganho pedagógico |

## 4. Justificativa

**Alinhamento com a avaliação.** O enunciado dá liberdade, mas os exemplos são AWS e o
curso é construído sobre ela. Uma escolha exótica gastaria parte da defesa explicando
o provedor em vez da arquitetura.

**Custo de aprendizado.** Os cinco requisitos mapeiam sem tradução. Em GCP, "cluster
Kubernetes com escalabilidade" continua direto (GKE), mas "Function Serverless"
convidaria a Cloud Run — que é container, não function — e abriria discussão sobre se
atende ao requisito.

**Crédito disponível.** O Learner Lab elimina o cartão de crédito, que é barreira real
para um trabalho acadêmico em grupo.

## 5. Impactos e restrições assumidas

O Learner Lab não é uma conta AWS comum. Três restrições moldam a arquitetura inteira:

| Restrição | Consequência |
| --- | --- |
| **Credenciais temporárias** (sessão de 4h, com `aws_session_token`) | Os secrets do GitHub Actions expiram diariamente, colidindo com o requisito de *deploy automático*. Mitigação na §6 deste documento. |
| **Proibido criar IAM roles** — só a `LabRole` existente | **Ameaça direta ao EKS**, que exige uma cluster role. Se a `LabRole` não tiver trust policy para `eks.amazonaws.com`, o cluster não sobe. Plano B: k3s em EC2 via Terraform. §6. |
| **Crédito ~US$ 50, região travada** | Sem NAT Gateway (~US$ 32/mês) e sem VPC endpoints (~US$ 7/mês cada). A configuração da Lambda passa a ser injetada como variável de ambiente no `terraform apply`, em vez de lida do Secrets Manager em runtime. |

Essas restrições **não são preferência técnica** — são o que de fato desenhou as
decisões de rede e de configuração da fase.

## 6. Verificação

O ambiente foi diagnosticado antes de qualquer provisionamento, com a sessão do lab
ativa. Resultado:

| Verificação | Resultado |
| --- | --- |
| Serviços exigidos respondem (EKS, RDS, Lambda, API Gateway, SSM, Secrets Manager, S3, DynamoDB, ECR) | todos |
| VPC default | 6 subnets em AZs distintas |
| IAM para EKS | roles dedicadas disponíveis — `LabEksClusterRole` e `LabEksNodeRole` |
| `terraform plan` da infraestrutura de rede e cluster | 15 recursos, sem erro |

A restrição de IAM se confirmou (não é possível criar roles), mas o lab provisiona as
roles necessárias — o que valida a escolha da AWS sem exigir plano alternativo.

Sobre as credenciais temporárias: o `AWS_SESSION_TOKEN` precisa ser renovado nos
secrets do CI a cada sessão de 4h. Numa conta com IAM próprio, a troca por OIDC elimina
o problema sem mudar nada na arquitetura.

## 7. Referências

- [diagrama-componentes.md](../diagrama-componentes.md) — arquitetura resultante
