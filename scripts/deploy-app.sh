#!/usr/bin/env bash
# =====================================================================
# Deploy da aplicação no cluster kind (Linux/macOS/Git Bash).
#
# Pré-requisito: cluster provisionado com Terraform (pasta infra/).
# Passos: build da imagem -> kind load -> kubectl apply -> rollout.
#
# Uso: ./scripts/deploy-app.sh
# =====================================================================
set -euo pipefail

CLUSTER="${CLUSTER:-oficina}"
NAMESPACE="${NAMESPACE:-oficina}"
IMAGE="${IMAGE:-oficina-mvp:latest}"

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> [1/4] Build da imagem ${IMAGE}"
docker build -t "${IMAGE}" "${RAIZ}"

echo "==> [2/4] Carregando a imagem no cluster kind '${CLUSTER}'"
kind load docker-image "${IMAGE}" --name "${CLUSTER}"

echo "==> [3/4] Aplicando manifestos k8s/"
kubectl apply -f "${RAIZ}/k8s"

echo "==> [4/4] Aguardando rollout do Deployment"
kubectl -n "${NAMESPACE}" rollout status deployment/oficina-app --timeout=300s

echo
echo "Aplicação no ar:"
echo "  Painel de demonstração: http://localhost:8080/"
echo "  Swagger UI ...........: http://localhost:8080/swagger"
echo "  Health ...............: http://localhost:8080/health"
