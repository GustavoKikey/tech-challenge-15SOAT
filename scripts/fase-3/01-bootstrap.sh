#!/usr/bin/env bash
# =====================================================================
# Passo 1 — backend remoto do Terraform (bucket S3 + trava DynamoDB)
# ---------------------------------------------------------------------
# Roda UMA VEZ por conta. Sem isso, o state nasce e morre dentro do runner
# do GitHub Actions, e não há trava entre o CI e quem aplica da máquina.
#
# Feito com AWS CLI, e não com Terraform, por dois motivos:
#
#   1. Ovo e galinha — é este passo que cria o backend onde o state dos
#      outros módulos vai morar. Um módulo Terraform aqui precisaria de
#      state local, que ninguém compartilha e que se perde.
#
#   2. O AWS Academy Learner Lab tem uma Service Control Policy que nega
#      s3:GetBucketObjectLockConfiguration. O provider AWS faz essa leitura
#      sempre que gerencia um aws_s3_bucket, então o apply falha logo após
#      criar o bucket — mesmo tendo criado com sucesso.
#
# É idempotente: rodar de novo não quebra nada.
#
# Uso:  bash scripts/fase-3/01-bootstrap.sh SEU-SUFIXO-UNICO
#       (nome de bucket S3 é global na AWS inteira — use algo seu)
# =====================================================================
set -euo pipefail

SUFIXO="${1:-}"
if [ -z "$SUFIXO" ]; then
  echo "Uso: bash scripts/fase-3/01-bootstrap.sh SEU-SUFIXO-UNICO"
  echo "Ex.: bash scripts/fase-3/01-bootstrap.sh gustavokikey"
  exit 1
fi

REGIAO="${AWS_REGION:-us-east-1}"
BUCKET="oficina-tfstate-${SUFIXO}"
TABELA="oficina-tflock-${SUFIXO}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> Bucket de state: ${BUCKET}"
if aws s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; then
  echo "    ja existe"
else
  aws s3api create-bucket --bucket "$BUCKET" --region "$REGIAO" >/dev/null
  echo "    criado"
fi

# Versionamento é a rede de segurança do state: um apply que corrompe
# permite voltar para a versão anterior.
aws s3api put-bucket-versioning --bucket "$BUCKET" \
  --versioning-configuration Status=Enabled >/dev/null
echo "    versionamento habilitado"

aws s3api put-bucket-encryption --bucket "$BUCKET" \
  --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}' >/dev/null
echo "    criptografia habilitada"

# O state guarda dados sensíveis — inclusive a senha do RDS em texto claro.
aws s3api put-public-access-block --bucket "$BUCKET" \
  --public-access-block-configuration \
  'BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true' >/dev/null
echo "    acesso publico bloqueado"

echo
echo "==> Tabela de trava: ${TABELA}"
if aws dynamodb describe-table --table-name "$TABELA" >/dev/null 2>&1; then
  echo "    ja existe"
else
  aws dynamodb create-table --table-name "$TABELA" \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST >/dev/null
  echo "    criada, aguardando ficar ativa..."
  aws dynamodb wait table-exists --table-name "$TABELA"
  echo "    ativa"
fi

printf '# Gerado por 01-bootstrap.sh — usado pelos scripts 02 e 99.\nTF_STATE_BUCKET=%s\nTF_LOCK_TABLE=%s\n' \
  "$BUCKET" "$TABELA" > "$RAIZ/scripts/fase-3/.backend.env"

echo
echo "======================================================================"
echo "  Backend pronto"
echo "======================================================================"
echo "  bucket: ${BUCKET}"
echo "  tabela: ${TABELA}"
echo
echo "  Salvo em scripts/fase-3/.backend.env (fora do Git)."
echo
echo "  Cadastrar como secrets nos repositorios de infraestrutura:"
echo "    for r in oficina-auth-lambda oficina-infra-k8s oficina-infra-database; do"
echo "      gh secret set TF_STATE_BUCKET --repo SEU-USUARIO/\$r --body '${BUCKET}'"
echo "      gh secret set TF_LOCK_TABLE   --repo SEU-USUARIO/\$r --body '${TABELA}'"
echo "    done"
echo
echo "  Proximo passo:  bash scripts/fase-3/02-aplicar.sh hom"
