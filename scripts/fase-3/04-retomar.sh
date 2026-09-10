#!/usr/bin/env bash
# =====================================================================
# Retomar o ambiente depois de reiniciar a sessão do Learner Lab
# ---------------------------------------------------------------------
# Encerrar a sessão NÃO apaga a infraestrutura: o cluster, o banco, a
# Function e o balanceador continuam existindo. O que muda são as
# credenciais — e é isso que quebra tudo que dependia delas.
#
# Este script refaz a sequência inteira e espera pelo que precisa voltar:
#
#   1. confere as credenciais novas;
#   2. reaponta o kubectl para o cluster;
#   3. espera os nós ficarem prontos (as instâncias reiniciam com o lab);
#   4. espera o banco voltar a aceitar conexão;
#   5. reenvia as credenciais aos 4 repositórios do GitHub;
#   6. religa a aplicação, se ela ficou de molho enquanto o banco subia;
#   7. libera o seu IP no balanceador e devolve a URL.
#
# Se você tinha rodado 99-destruir.sh, não é este o script: aí a
# infraestrutura não existe mais e o caminho é 02-aplicar.sh. O script
# detecta e avisa.
#
# Uso:
#   1. Cole as credenciais novas em ~/.aws/credentials
#   2. bash scripts/fase-3/04-retomar.sh prod
# =====================================================================
set -euo pipefail

AMBIENTE="${1:-prod}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CLUSTER="oficina-${AMBIENTE}"
REGIAO="${AWS_REGION:-us-east-1}"
NAMESPACE=oficina
USUARIO_GITHUB="${USUARIO_GITHUB:-GustavoKikey}"

case "$AMBIENTE" in
  hom|prod) ;;
  *) echo "Ambiente deve ser 'hom' ou 'prod'."; exit 1 ;;
esac

titulo() {
  echo
  echo "──────────────────────────────────────────────────────────────────────"
  echo "  $1"
  echo "──────────────────────────────────────────────────────────────────────"
}

# ---------------------------------------------------------------------
titulo "1. Credenciais"
if ! aws sts get-caller-identity >/dev/null 2>&1; then
  echo "Credenciais inválidas ou expiradas."
  echo
  echo "No lab: AWS Details -> AWS CLI -> Show, e cole o bloco inteiro em"
  echo "   ~/.aws/credentials"
  exit 1
fi
CONTA=$(aws sts get-caller-identity --query Account --output text)
echo "OK — conta $CONTA"

# ---------------------------------------------------------------------
titulo "2. O cluster ainda existe?"
if ! aws eks describe-cluster --name "$CLUSTER" --region "$REGIAO" >/dev/null 2>&1; then
  echo "O cluster '$CLUSTER' não existe."
  echo
  echo "Isso é o esperado se você rodou 99-destruir.sh. Para recriar tudo:"
  echo
  echo "   export JWT_PRIVATE_KEY_BASE64=\$(base64 -w0 src/main/resources/privateKey.pem)"
  echo "   bash scripts/fase-3/02-aplicar.sh $AMBIENTE"
  echo "   export NEW_RELIC_LICENSE_KEY=..."
  echo "   bash scripts/fase-3/03-observabilidade.sh $AMBIENTE"
  echo
  echo "ATENÇÃO: recriar gera endereços NOVOS — outro API Gateway, outro"
  echo "balanceador. Todo link anotado antes deixa de valer."
  exit 1
fi
echo "OK — $CLUSTER existe"

aws eks update-kubeconfig --name "$CLUSTER" --region "$REGIAO" >/dev/null
echo "OK — kubeconfig atualizado"

# ---------------------------------------------------------------------
titulo "3. Nós do cluster"
# As instâncias são reiniciadas junto com a sessão do lab e levam alguns
# minutos para voltar a se registrar no cluster.
for _ in $(seq 1 60); do
  PRONTOS=$(kubectl get nodes --no-headers 2>/dev/null | grep -c " Ready " || true)
  [ "${PRONTOS:-0}" -ge 1 ] && break
  printf '.'
  sleep 10
done
echo
if [ "${PRONTOS:-0}" -lt 1 ]; then
  echo "Nenhum nó pronto depois de 10 minutos."
  echo "Verifique o node group:  aws eks describe-nodegroup --cluster-name $CLUSTER --nodegroup-name ${CLUSTER}-nos"
  exit 1
fi
echo "OK — $PRONTOS nó(s) pronto(s)"

# ---------------------------------------------------------------------
titulo "4. Banco de dados"
for _ in $(seq 1 60); do
  ESTADO=$(aws rds describe-db-instances --db-instance-identifier "oficina-${AMBIENTE}" \
    --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "ausente")
  [ "$ESTADO" = "available" ] && break
  # O lab pode devolver o banco parado; nesse caso, ligue-o.
  if [ "$ESTADO" = "stopped" ]; then
    echo "banco parado — iniciando"
    aws rds start-db-instance --db-instance-identifier "oficina-${AMBIENTE}" >/dev/null 2>&1 || true
  fi
  printf '.'
  sleep 15
done
echo
if [ "$ESTADO" != "available" ]; then
  echo "O banco está '$ESTADO' e não ficou disponível a tempo."
  exit 1
fi
echo "OK — banco disponível"

# ---------------------------------------------------------------------
titulo "5. Secrets dos repositórios"
# Sem isso o deploy automático falha com "Credentials could not be loaded"
# — e só se descobre no meio de uma demonstração.
if command -v gh >/dev/null 2>&1; then
  bash "$RAIZ/scripts/fase-3/renovar-secrets.sh" "$USUARIO_GITHUB" 2>&1 | sed 's/^/    /'
else
  echo "gh não encontrado — os secrets dos repositórios NÃO foram renovados."
  echo "Sem eles a pipeline falha na autenticação com a AWS. Depois de"
  echo "instalar o gh:  bash scripts/fase-3/renovar-secrets.sh $USUARIO_GITHUB"
fi

# ---------------------------------------------------------------------
titulo "6. Aplicação"
# Pods que tentaram subir enquanto o banco estava fora entram em
# CrashLoopBackOff e ficam esperando um backoff cada vez maior. Reiniciar
# é mais rápido — e mais previsível — do que torcer pela próxima tentativa.
NAO_PRONTOS=$(kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null \
  | grep -v "Running" | grep -vc "^$" || true)
if [ "${NAO_PRONTOS:-0}" -gt 0 ]; then
  echo "Há pods fora do ar — reiniciando a aplicação"
  kubectl -n "$NAMESPACE" rollout restart deployment/oficina-app >/dev/null
fi
kubectl -n "$NAMESPACE" rollout status deployment/oficina-app --timeout=420s
kubectl -n "$NAMESPACE" get pods

# ---------------------------------------------------------------------
titulo "7. Acesso externo"
bash "$RAIZ/scripts/fase-3/liberar-meu-ip.sh" 2>&1 | sed 's/^/    /'

# ---------------------------------------------------------------------
# --cli-input-json em vez de --name: no Git Bash do Windows, argumento que
# comece com barra é lido como caminho POSIX e convertido para caminho do
# Windows antes de chegar na CLI. O parâmetro vira algo como
# "C:/Program Files/Git/oficina/..." e a AWS responde ParameterNotFound —
# sem nenhuma pista de que quem mexeu foi o shell. Dentro do JSON o valor
# não parece caminho e passa intacto. No Linux dá no mesmo.
GATEWAY=$(aws ssm get-parameter \
  --cli-input-json "{\"Name\":\"/oficina/${AMBIENTE}/apigw/invoke-url\"}" \
  --query Parameter.Value --output text 2>/dev/null || echo "?")

cat <<FIM

══════════════════════════════════════════════════════════════════════
  Ambiente $AMBIENTE retomado
══════════════════════════════════════════════════════════════════════
  API Gateway ...: $GATEWAY
  Cluster .......: $CLUSTER

  Os dados de demonstração continuam no banco — ele não foi recriado.
  Mas o New Relic só conta o que acontece DEPOIS da coleta recomeçar,
  então para o painel de volume não aparecer zerado:

    bash scripts/fase-3/seed-demo.sh

  E antes de fechar o lab, para não queimar crédito:

    bash scripts/fase-3/99-destruir.sh $AMBIENTE
FIM
