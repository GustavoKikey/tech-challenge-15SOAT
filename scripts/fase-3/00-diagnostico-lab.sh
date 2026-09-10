#!/usr/bin/env bash
# =====================================================================
# Diagnóstico do AWS Academy Learner Lab
# ---------------------------------------------------------------------
# RODE ISTO PRIMEIRO, assim que a bolinha do lab ficar verde.
#
# Responde de uma vez tudo que decide a arquitetura da fase 3, em vez
# de você descobrir cada limitação no meio de um terraform apply:
#
#   1. as credenciais estão válidas?
#   2. a LabRole serve para o EKS?          <- decide plano A ou plano B
#   3. a LabRole serve para Lambda e RDS?
#   4. os serviços necessários respondem?
#   5. quanto tempo de sessão ainda resta?
#
# É 100% somente leitura: não cria, não altera e não gasta crédito.
#
# Uso:  bash scripts/fase-3/00-diagnostico-lab.sh
# =====================================================================
set -uo pipefail

VERDE=$'\033[0;32m'; VERMELHO=$'\033[0;31m'; AMARELO=$'\033[0;33m'; NEGRITO=$'\033[1m'; FIM=$'\033[0m'
ok()    { echo "  ${VERDE}[ OK ]${FIM} $*"; }
falha() { echo "  ${VERMELHO}[FALHA]${FIM} $*"; }
aviso() { echo "  ${AMARELO}[ ! ]${FIM} $*"; }
titulo(){ echo; echo "${NEGRITO}$*${FIM}"; echo "${NEGRITO}$(printf '%.0s-' {1..66})${FIM}"; }

EKS_LIBERADO=0
PROBLEMAS=0

titulo "1. Ferramentas locais"

for cmd in aws terraform kubectl; do
  if command -v "$cmd" >/dev/null 2>&1; then
    ok "$cmd instalado"
  else
    falha "$cmd NÃO instalado"
    PROBLEMAS=$((PROBLEMAS + 1))
  fi
done
command -v gh >/dev/null 2>&1 && ok "gh instalado" || aviso "gh não instalado (só necessário para criar os repositórios)"

if ! command -v aws >/dev/null 2>&1; then
  echo
  falha "Sem AWS CLI não dá para continuar. Instale com:"
  echo "        winget install --id Amazon.AWSCLI -e"
  exit 1
fi

titulo "2. Credenciais e identidade"

IDENTIDADE=$(aws sts get-caller-identity --output json 2>&1)
if [ $? -ne 0 ]; then
  falha "Credenciais inválidas ou expiradas."
  echo "        $(echo "$IDENTIDADE" | head -1)"
  echo
  echo "        A sessão do lab dura 4h. Recopie as credenciais em"
  echo "        AWS Details -> AWS CLI -> Show  para  ~/.aws/credentials"
  exit 1
fi

ARN=$(echo "$IDENTIDADE"   | python -c 'import sys,json;print(json.load(sys.stdin)["Arn"])' 2>/dev/null)
CONTA=$(echo "$IDENTIDADE" | python -c 'import sys,json;print(json.load(sys.stdin)["Account"])' 2>/dev/null)
ok "Autenticado — conta ${CONTA}"
echo "        ${ARN}"

case "$ARN" in
  *voclabs*|*LabRole*) ok "Confirmado: AWS Academy Learner Lab" ;;
  *)                   aviso "Não parece Learner Lab — talvez conta própria (melhor ainda)" ;;
esac

REGIAO=$(aws configure get region 2>/dev/null || echo "")
[ "$REGIAO" = "us-east-1" ] && ok "Região us-east-1" \
  || aviso "Região atual: '${REGIAO:-nao definida}'. O Learner Lab só libera us-east-1."

titulo "3. LabRole — o teste que decide a arquitetura"

ROLES=$(aws iam list-roles --query "Roles[?contains(RoleName,'Lab')].[RoleName]" --output text 2>/dev/null)
if [ -z "$ROLES" ]; then
  aviso "Nenhuma role com 'Lab' no nome. Se for conta própria, tudo bem —"
  aviso "informe sua role em role_arn_cluster / role_arn_lambda."
else
  ok "Roles encontradas: $(echo "$ROLES" | tr '\n' ' ')"
fi

TRUST=$(aws iam get-role --role-name LabRole \
          --query 'Role.AssumeRolePolicyDocument' --output json 2>/dev/null)

if [ -z "$TRUST" ]; then
  falha "LabRole não encontrada ou sem permissão de leitura."
  PROBLEMAS=$((PROBLEMAS + 1))
else
  for servico in eks ec2 lambda rds; do
    if echo "$TRUST" | grep -q "${servico}.amazonaws.com"; then
      ok "LabRole confia em ${servico}.amazonaws.com"
      [ "$servico" = "eks" ] && EKS_LIBERADO=1
    else
      if [ "$servico" = "eks" ]; then
        falha "LabRole NÃO confia em eks.amazonaws.com  <<< PLANO B"
      else
        aviso "LabRole não confia em ${servico}.amazonaws.com"
      fi
    fi
  done
fi

titulo "4. Serviços respondem?"

verificar() {
  local nome="$1"; shift
  if "$@" >/dev/null 2>&1; then
    ok "$nome"
  else
    falha "$nome — sem permissão ou serviço bloqueado"
    PROBLEMAS=$((PROBLEMAS + 1))
  fi
}

verificar "EC2 / VPC"        aws ec2 describe-vpcs --max-items 1
verificar "EKS"              aws eks list-clusters
verificar "RDS"              aws rds describe-db-instances --max-items 1
verificar "Lambda"           aws lambda list-functions --max-items 1
verificar "API Gateway"      aws apigatewayv2 get-apis --max-results 1
verificar "SSM Parameter"    aws ssm describe-parameters --max-results 1
verificar "Secrets Manager"  aws secretsmanager list-secrets --max-results 1
verificar "S3"               aws s3api list-buckets
verificar "DynamoDB"         aws dynamodb list-tables --max-items 1
verificar "ECR"              aws ecr describe-repositories --max-results 1

titulo "5. VPC default (a arquitetura depende dela)"

VPC=$(aws ec2 describe-vpcs --filters Name=is-default,Values=true \
        --query 'Vpcs[0].VpcId' --output text 2>/dev/null)
if [ -n "$VPC" ] && [ "$VPC" != "None" ]; then
  SUBNETS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC" \
              --query 'length(Subnets)' --output text 2>/dev/null)
  ok "VPC default: $VPC  (${SUBNETS} subnets)"
  [ "${SUBNETS:-0}" -ge 2 ] && ok "Subnets suficientes (RDS exige 2+ em AZs diferentes)" \
    || { falha "Menos de 2 subnets — o RDS subnet group vai falhar"; PROBLEMAS=$((PROBLEMAS+1)); }
else
  falha "VPC default não encontrada. O Terraform assume que ela existe."
  PROBLEMAS=$((PROBLEMAS + 1))
fi

titulo "6. Recursos já existentes (crédito sendo consumido?)"

for par in "EKS:aws eks list-clusters --query length(clusters)" \
           "RDS:aws rds describe-db-instances --query length(DBInstances)" \
           "EC2 rodando:aws ec2 describe-instances --filters Name=instance-state-name,Values=running --query length(Reservations)"; do
  nome="${par%%:*}"; cmd="${par#*:}"
  qtd=$($cmd --output text 2>/dev/null || echo "?")
  if [ "$qtd" = "0" ]; then
    ok "$nome: nenhum"
  else
    aviso "$nome: $qtd  <<< consumindo crédito"
  fi
done

# =====================================================================
titulo "VEREDITO"

if [ "$EKS_LIBERADO" -eq 1 ]; then
  echo "  ${VERDE}${NEGRITO}PLANO A — EKS liberado.${FIM}"
  echo
  echo "  A LabRole confia em eks.amazonaws.com. Siga o plano principal:"
  echo
  echo "    bash scripts/fase-3/01-bootstrap.sh SEU-SUFIXO"
  echo "    bash scripts/fase-3/02-aplicar.sh hom"
else
  echo "  ${VERMELHO}${NEGRITO}PLANO B — EKS provavelmente bloqueado.${FIM}"
  echo
  echo "  A trust policy da LabRole não menciona eks.amazonaws.com."
  echo "  ${NEGRITO}Antes de desistir do EKS${FIM}, confirme na prática — a trust policy"
  echo "  pode estar incompleta na leitura, mas o serviço funcionar:"
  echo
  echo "    cd infra-k8s && terraform init -backend=false && terraform plan -var=ambiente=hom"
  echo
  echo "  Se o apply falhar mesmo, o plano B é k3s em EC2 (docs/fase-3/plano.md, Risco 2)."
fi

echo
if [ "$PROBLEMAS" -eq 0 ]; then
  echo "  Nenhum bloqueio além do acima. ${VERDE}Pode seguir.${FIM}"
else
  echo "  ${AMARELO}${PROBLEMAS} verificação(ões) falharam${FIM} — reveja acima antes de aplicar."
fi
echo
echo "  Lembrete: a sessão do lab dura 4h. Rode 99-destruir.sh antes de sair."
echo
