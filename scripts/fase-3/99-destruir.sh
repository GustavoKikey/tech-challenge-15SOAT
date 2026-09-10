#!/usr/bin/env bash
# =====================================================================
# Higiene de crédito — destrói na ordem INVERSA da criação
# ---------------------------------------------------------------------
#   1 lambda-auth  ->  3 infra-database  ->  2 infra-k8s
#
# RODE ANTES DE FECHAR O LAB. O EKS cobra ~US$ 0,10/h de control plane
# mesmo sem nada rodando dentro: são ~US$ 2,40 por dia esquecido.
#
# O bucket de state e a tabela de trava NÃO são destruídos (têm
# prevent_destroy) — são baratos e você reusa na próxima sessão.
#
# Uso:  bash scripts/fase-3/99-destruir.sh hom
# =====================================================================
set -uo pipefail

AMBIENTE="${1:-hom}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

case "$AMBIENTE" in
  hom|prod) ;;
  *) echo "Ambiente deve ser 'hom' ou 'prod'."; exit 1 ;;
esac

if [ ! -f "$RAIZ/scripts/fase-3/.backend.env" ]; then
  echo "Backend não configurado (scripts/fase-3/.backend.env ausente)."
  exit 1
fi
# shellcheck disable=SC1091
source "$RAIZ/scripts/fase-3/.backend.env"

echo "Isto vai DESTRUIR a infraestrutura de '${AMBIENTE}':"
echo "  - Lambda e API Gateway"
echo "  - RDS PostgreSQL (com os dados)"
echo "  - Cluster EKS e security groups"
echo
read -r -p "Digite ${AMBIENTE} para confirmar: " confirma
if [ "$confirma" != "$AMBIENTE" ]; then
  echo "Cancelado."
  exit 1
fi

# Argumentos: <diretório> <prefixo do state> [extras do terraform destroy].
# A chave inclui o ambiente — destruir hom não pode alcançar prod (adr-005).
destruir() {
  local dir="$1"
  local modulo="$2"
  shift 2
  echo
  echo "==> destruindo $dir  (state: $modulo/$AMBIENTE)"
  cd "$RAIZ/$dir" || return
  terraform init -input=false -reconfigure \
    -backend-config="bucket=${TF_STATE_BUCKET}" \
    -backend-config="dynamodb_table=${TF_LOCK_TABLE}" \
    -backend-config="key=${modulo}/${AMBIENTE}/terraform.tfstate" >/dev/null 2>&1
  terraform destroy -auto-approve -input=false -var="ambiente=${AMBIENTE}" "$@" \
    || echo "   (falhou — pode já não existir; seguindo adiante)"
}

destruir lambda-auth/infra lambda-auth -var="jwt_private_key_base64=${JWT_PRIVATE_KEY_BASE64:-x}"
destruir infra-database   infra-database
destruir infra-k8s        infra-k8s

echo
echo "Confirmando que não sobrou nada cobrando:"
echo "  EKS ....: $(aws eks list-clusters --query 'length(clusters)' --output text 2>/dev/null || echo '?')"
echo "  RDS ....: $(aws rds describe-db-instances --query 'length(DBInstances)' --output text 2>/dev/null || echo '?')"
echo "  EC2 on .: $(aws ec2 describe-instances --filters Name=instance-state-name,Values=running --query 'length(Reservations)' --output text 2>/dev/null || echo '?')"
echo
echo "O bucket de state e a tabela de trava foram mantidos (reuso na próxima sessão)."
