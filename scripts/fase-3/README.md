# Scripts de operação da infraestrutura

Sequência para provisionar, verificar e remover a infraestrutura da fase 3 na AWS.

---

## A sequência

```
liga o lab  →  00-diagnostico  →  01-bootstrap (1x)  →  02-aplicar  →  testa  →  99-destruir
```

| Script | Quando | O que faz |
| --- | --- | --- |
| `00-diagnostico-lab.sh` | **Sempre primeiro** | Verifica credenciais, `LabRole`, serviços, VPC e o que já está ligado. Diz **PLANO A ou B**. Somente leitura. |
| `01-bootstrap.sh SUFIXO` | Uma vez por conta | Cria o bucket S3 + tabela DynamoDB do state do Terraform |
| `02-aplicar.sh hom` | Quando a infraestrutura **não existe** | Aplica `infra-k8s` → `infra-database` → `lambda-auth` na ordem |
| `03-observabilidade.sh hom` | Depois de aplicar | Instala o agente do New Relic no cluster |
| `04-retomar.sh hom` | Quando a infraestrutura **existe** e a sessão reiniciou | Refaz tudo que depende das credenciais e espera o ambiente voltar |
| `seed-demo.sh` | Antes de demonstrar | Catálogo, ordens de serviço e transições de fase |
| `liberar-meu-ip.sh` | Ao trocar de rede | Abre o balanceador para o seu IP (`--fechar` tranca) |
| `99-destruir.sh hom` | **Antes de fechar o lab** | Derruba tudo na ordem inversa |
| `renovar-secrets.sh USUARIO` | Cada sessão, se usar CI | Reenvia as credenciais de 4h para os 4 repos do GitHub |

### 02-aplicar ou 04-retomar?

Encerrar a sessão do lab **não** apaga a infraestrutura — o cluster, o banco e a
Function continuam existindo. O que expira são as credenciais.

- Se você **não** rodou `99-destruir.sh`: `04-retomar.sh`. Ele valida as credenciais
  novas, espera os nós e o banco voltarem, renova os secrets do GitHub, religa a
  aplicação se ela caiu enquanto o banco subia, e devolve a URL.
- Se você **rodou** `99-destruir.sh`: `02-aplicar.sh` + `03-observabilidade.sh`. Aí a
  infraestrutura nasce de novo — e **os endereços mudam**: outro API Gateway, outro
  balanceador. Qualquer link anotado antes deixa de valer.

O `04-retomar.sh` detecta o segundo caso e avisa em vez de falhar pela metade.

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
