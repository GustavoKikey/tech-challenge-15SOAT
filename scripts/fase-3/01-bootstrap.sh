#!/usr/bin/env bash
# =====================================================================
# Passo 1 — backend remoto do Terraform (bucket S3 + trava DynamoDB)
# ---------------------------------------------------------------------
# Roda UMA VEZ por conta. Sem isso, o state do Terraform nasce e morre
# dentro do runner do GitHub Actions, e não há trava entre o CI e você.
#
# Uso:  bash scripts/fase-3/01-bootstrap.sh SEU-SUFIXO-UNICO
#       (nome de bucket S3 é global na AWS inteira — use algo seu,
#        por exemplo seu usuário do GitHub)
# =====================================================================
set -euo pipefail

SUFIXO="${1:-}"
if [ -z "$SUFIXO" ]; then
  echo "Uso: bash scripts/fase-3/01-bootstrap.sh SEU-SUFIXO-UNICO"
  echo "Ex.: bash scripts/fase-3/01-bootstrap.sh gustavokikey"
  exit 1
fi

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ/infra-k8s/bootstrap"

echo "==> Provisionando backend com sufixo '${SUFIXO}'"
terraform init -input=false
terraform apply -auto-approve -input=false -var="sufixo=${SUFIXO}"

BUCKET="oficina-tfstate-${SUFIXO}"
TABELA="oficina-tflock-${SUFIXO}"

printf '# Gerado por 01-bootstrap.sh — usado pelos scripts 02 e 99.\nTF_STATE_BUCKET=%s\nTF_LOCK_TABLE=%s\n' \
  "$BUCKET" "$TABELA" > "$RAIZ/scripts/fase-3/.backend.env"

echo
echo "Backend pronto:"
echo "   bucket: ${BUCKET}"
echo "   tabela: ${TABELA}"
echo
echo "Salvo em scripts/fase-3/.backend.env (fora do Git)."
echo
echo "Estes dois valores também viram secrets no GitHub:"
echo "   gh secret set TF_STATE_BUCKET --body '${BUCKET}'"
echo "   gh secret set TF_LOCK_TABLE   --body '${TABELA}'"
echo
echo "Próximo passo:  bash scripts/fase-3/02-aplicar.sh hom"
