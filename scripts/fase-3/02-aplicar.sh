#!/usr/bin/env bash
# =====================================================================
# Passo 2 — aplica a infraestrutura na ordem da cadeia de dependências
# ---------------------------------------------------------------------
#   2 infra-k8s  ->  3 infra-database  ->  1 lambda-auth
#
# Fora dessa ordem, o apply falha com "parâmetro não encontrado no SSM"
# — que é o contrato entre repositórios funcionando como deveria.
#
# A aplicação (repo 4) NÃO entra aqui: ela sobe pelo workflow de CD ou
# pelo scripts/deploy-app.sh.
#
# Uso:  bash scripts/fase-3/02-aplicar.sh hom
#       bash scripts/fase-3/02-aplicar.sh prod
# =====================================================================
set -euo pipefail

AMBIENTE="${1:-hom}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

case "$AMBIENTE" in
  hom|prod) ;;
  *) echo "Ambiente deve ser 'hom' ou 'prod'."; exit 1 ;;
esac

if [ ! -f "$RAIZ/scripts/fase-3/.backend.env" ]; then
  echo "Backend não configurado. Rode antes:"
  echo "   bash scripts/fase-3/01-bootstrap.sh SEU-SUFIXO"
  exit 1
fi
# shellcheck disable=SC1091
source "$RAIZ/scripts/fase-3/.backend.env"

if [ -z "${JWT_PRIVATE_KEY_BASE64:-}" ]; then
  echo "Exporte a chave privada antes de aplicar (a Lambda assina com ela):"
  echo '   export JWT_PRIVATE_KEY_BASE64=$(base64 -w0 src/main/resources/privateKey.pem)'
  echo
  echo "ATENÇÃO: a chave versionada no repositório deve ser ROTACIONADA"
  echo "antes de ir para produção (plano.md, decisão 2)."
  exit 1
fi

aplicar() {
  local dir="$1"
  shift
  echo
  echo "======================================================================"
  echo "  $dir  ->  ambiente $AMBIENTE"
  echo "======================================================================"
  cd "$RAIZ/$dir"
  terraform init -input=false -reconfigure \
    -backend-config="bucket=${TF_STATE_BUCKET}" \
    -backend-config="dynamodb_table=${TF_LOCK_TABLE}"
  terraform apply -auto-approve -input=false -var="ambiente=${AMBIENTE}" "$@"
}

aplicar infra-k8s
aplicar infra-database
aplicar lambda-auth/infra -var="jwt_private_key_base64=${JWT_PRIVATE_KEY_BASE64}"

cd "$RAIZ/lambda-auth/infra"
URL=$(terraform output -raw invoke_url 2>/dev/null || echo "?")
cd "$RAIZ/infra-k8s"
CLUSTER=$(terraform output -raw cluster_name 2>/dev/null || echo "?")

echo
echo "======================================================================"
echo "  Infraestrutura de $AMBIENTE no ar"
echo "======================================================================"
echo "  API Gateway ..: $URL"
echo "  Autenticação .: $URL/auth/cliente"
echo "  Cluster ......: $CLUSTER"
echo
echo "  Apontar o kubectl:"
echo "    aws eks update-kubeconfig --region us-east-1 --name $CLUSTER"
echo
echo "  Subir a aplicação: push na branch 'homolog' (workflow cd-aws.yml)"
echo
echo "  QUANDO TERMINAR:  bash scripts/fase-3/99-destruir.sh $AMBIENTE"
