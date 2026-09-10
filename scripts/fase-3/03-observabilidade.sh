#!/usr/bin/env bash
# =====================================================================
# Agente do New Relic no cluster (nri-bundle)
# ---------------------------------------------------------------------
# A aplicação exporta traces sozinha, por OTLP. Todo o resto depende
# deste agente:
#
#   - CPU, memória e estado dos pods           (K8sContainerSample, ...)
#   - eventos do Kubernetes                    (nri-kube-events)
#   - logs dos contêineres                     (newrelic-logging)
#   - as MÉTRICAS DE NEGÓCIO expostas em /q/metrics
#
# A última é a que costuma passar despercebida: os contadores oficina_*
# vivem no registry do Micrometer e saem pelo endpoint Prometheus do pod.
# Sem alguém raspando esse endpoint, os três painéis exigidos pelo
# enunciado ficam vazios — mesmo com os traces chegando normalmente.
#
# Por que isto é um script e não Terraform: o cluster é recriado a cada
# sessão do Learner Lab, e instalar o agente à mão significa descobrir
# que o dashboard está vazio no meio da demonstração.
#
# Uso:
#   export NEW_RELIC_LICENSE_KEY=...        # chave de INGESTÃO, sufixo NRAL
#   bash scripts/fase-3/03-observabilidade.sh prod
# =====================================================================
set -euo pipefail

AMBIENTE="${1:-hom}"
CLUSTER="${CLUSTER:-oficina-${AMBIENTE}}"
HELM="${HELM:-helm}"

case "$AMBIENTE" in
  hom|prod) ;;
  *) echo "Ambiente deve ser 'hom' ou 'prod'."; exit 1 ;;
esac

if [ -z "${NEW_RELIC_LICENSE_KEY:-}" ]; then
  echo "Exporte a license key antes:"
  echo "   export NEW_RELIC_LICENSE_KEY=sua-chave-NRAL"
  echo
  echo "É a chave de INGESTÃO (termina em NRAL), não a de API (NRAK)."
  exit 1
fi

if ! command -v "$HELM" >/dev/null 2>&1; then
  echo "helm não encontrado. Aponte o binário:"
  echo "   HELM=/caminho/para/helm.exe bash scripts/fase-3/03-observabilidade.sh $AMBIENTE"
  exit 1
fi

echo "==> cluster alvo: $CLUSTER"
kubectl config current-context >/dev/null || {
  echo "kubectl sem contexto. Rode antes:"
  echo "   aws eks update-kubeconfig --region us-east-1 --name $CLUSTER"
  exit 1
}

echo "==> repositório do chart"
"$HELM" repo add newrelic https://helm-charts.newrelic.com >/dev/null 2>&1 || true
"$HELM" repo update newrelic >/dev/null

echo "==> instalando nri-bundle (pode levar alguns minutos)"
# integrations_filter.enabled=false é o detalhe que faz a diferença: com
# ele ligado (o padrão), o agente só raspa alvos que reconhece como
# integrações conhecidas — redis, nginx, coredns — e ignora aplicações
# próprias, mesmo anotadas com prometheus.io/scrape. O Deployment da
# aplicação também carrega as anotações newrelic.io/*, que passam pelo
# job sem filtro; desligar o filtro faz os dois caminhos funcionarem.
"$HELM" upgrade --install newrelic-bundle newrelic/nri-bundle \
  --namespace newrelic --create-namespace \
  --set global.licenseKey="$NEW_RELIC_LICENSE_KEY" \
  --set global.cluster="$CLUSTER" \
  --set global.lowDataMode=true \
  --set kube-state-metrics.enabled=true \
  --set newrelic-prometheus-agent.enabled=true \
  --set newrelic-prometheus-agent.config.kubernetes.integrations_filter.enabled=false \
  --set kubeEvents.enabled=true \
  --set newrelic-logging.enabled=true \
  --set nri-metadata-injection.enabled=true \
  --wait --timeout 10m

echo
echo "==> pods do agente"
kubectl -n newrelic get pods

cat <<FIM

======================================================================
  Agente instalado — cluster $CLUSTER
======================================================================

  A coleta leva 1 a 3 minutos para aparecer. Confira no New Relic com:

    SELECT count(*) FROM K8sPodSample
    WHERE clusterName = '$CLUSTER' SINCE 10 minutes ago

    SELECT count(*) FROM Metric
    WHERE service.name = 'oficina-app' SINCE 10 minutes ago

  Os painéis e as condições de alerta estão em
  docs/fase-3/observabilidade-newrelic.md.
FIM
