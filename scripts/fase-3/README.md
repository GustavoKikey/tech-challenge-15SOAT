# Scripts da fase 3 — kit de pouso no Learner Lab

Sequência para ligar o lab e provisionar tudo, sem descobrir limitação no meio de um
`terraform apply` pela metade.

---

## A sequência

```
liga o lab  →  00-diagnostico  →  01-bootstrap (1x)  →  02-aplicar  →  testa  →  99-destruir
```

| Script | Quando | O que faz |
| --- | --- | --- |
| `00-diagnostico-lab.sh` | **Sempre primeiro** | Verifica credenciais, `LabRole`, serviços, VPC e o que já está ligado. Diz **PLANO A ou B**. Somente leitura. |
| `01-bootstrap.sh SUFIXO` | Uma vez por conta | Cria o bucket S3 + tabela DynamoDB do state do Terraform |
| `02-aplicar.sh hom` | Cada sessão | Aplica `infra-k8s` → `infra-database` → `lambda-auth` na ordem |
| `99-destruir.sh hom` | **Antes de fechar o lab** | Derruba tudo na ordem inversa |
| `renovar-secrets.sh USUARIO` | Cada sessão, se usar CI | Reenvia as credenciais de 4h para os 4 repos do GitHub |

## Passo a passo da primeira vez

```bash
# 1. Ligue o lab e cole as credenciais em ~/.aws/credentials
bash scripts/fase-3/00-diagnostico-lab.sh
```

Leia o **VEREDITO** no fim. Se disser PLANO A, siga. Se disser PLANO B, o EKS
provavelmente está bloqueado — confirme na prática antes de mudar de rota
(o próprio script mostra como).

```bash
# 2. Backend do Terraform (uma vez só na vida)
bash scripts/fase-3/01-bootstrap.sh gustavokikey

# 3. A chave que a Lambda usa para assinar os tokens
export JWT_PRIVATE_KEY_BASE64=$(base64 -w0 src/main/resources/privateKey.pem)

# 4. Sobe tudo
bash scripts/fase-3/02-aplicar.sh hom
```

No fim, o script imprime a URL do API Gateway e o nome do cluster.

```bash
# 5. Ao terminar — NÃO PULE ESTE PASSO
bash scripts/fase-3/99-destruir.sh hom
```

## Nas sessões seguintes

O bootstrap já existe. É só:

```bash
bash scripts/fase-3/00-diagnostico-lab.sh
export JWT_PRIVATE_KEY_BASE64=$(base64 -w0 src/main/resources/privateKey.pem)
bash scripts/fase-3/02-aplicar.sh hom
```

E, se os workflows estiverem em uso:

```bash
bash scripts/fase-3/renovar-secrets.sh SEU-USUARIO-GITHUB
```

## Por que destruir toda vez

O EKS cobra **~US$ 0,10/h de control plane**, mesmo sem nada rodando dentro — cerca de
**US$ 2,40 por dia** esquecido ligado. Com US$ 50 de crédito para a fase inteira, um fim
de semana esquecido custa 10% do orçamento.

O bucket de state e a tabela de trava **não** são destruídos: são baratos, têm
`prevent_destroy`, e você os reusa na sessão seguinte.

## Arquivos gerados

`01-bootstrap.sh` grava `.backend.env` nesta pasta com o nome do bucket e da tabela.
Os scripts `02` e `99` leem de lá. Está no `.gitignore` — não vai para o repositório.

## ⚠️ A chave privada

`src/main/resources/privateKey.pem` está **versionada** e deve ser considerada
comprometida. Rotacione antes de usar em qualquer ambiente que importe
(`docs/fase-3/plano.md`, decisão ②).
