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
echo "  - RDS PostgreSQL (com os dados, e SEM snapshot final)"
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

# ---------------------------------------------------------------------
# ANTES do Terraform: o que o Kubernetes criou fora dele
# ---------------------------------------------------------------------
# O balanceador da aplicação não pertence ao Terraform. Quem o criou foi o
# controlador do Kubernetes, ao ver um Service do tipo LoadBalancer — e por
# isso destruir o cluster não o remove: ele fica órfão na conta, cobrando
# por hora, sem aparecer em nenhum state.
#
# É um vazamento silencioso: o 'terraform destroy' termina dizendo
# "Destroy complete", e a fatura continua andando.
#
# Apagar o namespace faz o controlador desfazer o que criou, na ordem
# certa. Sem cluster acessível — porque já foi destruído antes, por
# exemplo — não há o que fazer aqui, e seguimos adiante.
echo
echo "==> removendo o que o Kubernetes criou fora do Terraform"
if kubectl cluster-info >/dev/null 2>&1; then
  for ns in oficina newrelic; do
    if kubectl get namespace "$ns" >/dev/null 2>&1; then
      echo "    namespace $ns"
      kubectl delete namespace "$ns" --timeout=300s >/dev/null 2>&1 \
        || echo "    (não saiu no tempo; verifique balanceadores órfãos no fim)"
    fi
  done

  # O controlador apaga o balanceador de forma assíncrona. Destruir o
  # cluster antes disso deixa o recurso pendurado.
  echo "    aguardando o balanceador sumir"
  for _ in $(seq 1 18); do
    restantes=$(aws elb describe-load-balancers \
      --query 'length(LoadBalancerDescriptions)' --output text 2>/dev/null || echo 0)
    [ "${restantes:-0}" = "0" ] && break
    sleep 10
  done
else
  echo "    cluster inacessível — nada a remover"
fi

# ---------------------------------------------------------------------
# A proteção contra exclusão precisa sair antes
# ---------------------------------------------------------------------
# Em 'prod' o banco nasce com deletion_protection ligada — o que está certo
# para produção de verdade, e é justamente o que impede este script de
# funcionar num ambiente que só se chama prod.
#
# O 'terraform destroy' NÃO desliga a trava: ele tenta apagar e a AWS
# recusa. E o erro anterior, sobre snapshot final, aparece primeiro e
# esconde este — some o segundo motivo e o destroy falha de novo, pelo
# primeiro. Desligar aqui, antes, resolve os dois de uma vez, junto com
# -var="ambiente_efemero=true".
BANCO="oficina-${AMBIENTE}"
if aws rds describe-db-instances --db-instance-identifier "$BANCO" >/dev/null 2>&1; then
  PROTEGIDO=$(aws rds describe-db-instances --db-instance-identifier "$BANCO" \
    --query 'DBInstances[0].DeletionProtection' --output text 2>/dev/null || echo "False")
  if [ "$PROTEGIDO" = "True" ]; then
    echo
    echo "==> desligando a proteção contra exclusão de $BANCO"
    aws rds modify-db-instance --db-instance-identifier "$BANCO" \
      --no-deletion-protection --apply-immediately >/dev/null 2>&1 || true
    sleep 15
  fi
fi

destruir lambda-auth/infra lambda-auth -var="jwt_private_key_base64=${JWT_PRIVATE_KEY_BASE64:-x}"
destruir infra-database   infra-database -var="ambiente_efemero=true"
destruir infra-k8s        infra-k8s

echo
echo "Confirmando que não sobrou nada cobrando:"
echo "  EKS ....: $(aws eks list-clusters --query 'length(clusters)' --output text 2>/dev/null || echo '?')"
echo "  RDS ....: $(aws rds describe-db-instances --query 'length(DBInstances)' --output text 2>/dev/null || echo '?')"
echo "  EC2 on .: $(aws ec2 describe-instances --filters Name=instance-state-name,Values=running --query 'length(Reservations)' --output text 2>/dev/null || echo '?')"
echo
echo "O bucket de state e a tabela de trava foram mantidos (reuso na próxima sessão)."
