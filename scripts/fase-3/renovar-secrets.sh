#!/usr/bin/env bash
# =====================================================================
# Renova as credenciais do Learner Lab nos 4 repositórios do GitHub
# ---------------------------------------------------------------------
# A sessão do lab dura 4h e as credenciais mudam a cada Start Lab. Os
# workflows dependem delas, então os secrets precisam ser reenviados —
# senão o deploy automático quebra no dia seguinte.
#
# Uso:
#   1. Cole as credenciais novas em ~/.aws/credentials
#      (no lab: AWS Details -> AWS CLI -> Show)
#   2. bash scripts/fase-3/renovar-secrets.sh SEU-USUARIO-GITHUB
# =====================================================================
set -euo pipefail

USUARIO="${1:-}"
if [ -z "$USUARIO" ]; then
  echo "Uso: bash scripts/fase-3/renovar-secrets.sh SEU-USUARIO-GITHUB"
  exit 1
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "gh não instalado:  winget install --id GitHub.cli -e"
  exit 1
fi

# Lê direto do ~/.aws/credentials para não depender de copiar e colar
# valor por valor (é onde o erro humano acontece).
ler() { awk -F' *= *' "/^${1}/ {print \$2; exit}" ~/.aws/credentials; }
CHAVE=$(ler aws_access_key_id)
SEGREDO=$(ler aws_secret_access_key)
TOKEN=$(ler aws_session_token)

if [ -z "$CHAVE" ]; then
  echo "Não achei aws_access_key_id em ~/.aws/credentials."
  echo "Copie o bloco de AWS Details -> AWS CLI -> Show para lá."
  exit 1
fi

# Valida ANTES de distribuir: enviar credencial vencida para 4 repos só
# adia a descoberta do problema para o meio de um deploy.
echo "Validando as credenciais..."
if ! aws sts get-caller-identity >/dev/null 2>&1; then
  echo "Credenciais inválidas ou expiradas. Recopie do lab e tente de novo."
  exit 1
fi
echo "OK."
echo

# O repositório da aplicação mantém o nome do desafio; os outros três
# nasceram na fase 3 com o nome do que provisionam.
REPOS="oficina-auth-lambda oficina-infra-k8s oficina-infra-database tech-challenge-15SOAT"
for repo in $REPOS; do
  echo "==> ${USUARIO}/${repo}"
  if ! gh secret set AWS_ACCESS_KEY_ID --repo "$USUARIO/$repo" --body "$CHAVE" 2>/dev/null; then
    echo "    (repositório não existe ainda — pulando)"
    continue
  fi
  gh secret set AWS_SECRET_ACCESS_KEY --repo "$USUARIO/$repo" --body "$SEGREDO" 2>/dev/null || true
  if [ -n "$TOKEN" ]; then
    gh secret set AWS_SESSION_TOKEN --repo "$USUARIO/$repo" --body "$TOKEN" 2>/dev/null || true
  fi
  echo "    secrets atualizados"
done

echo
echo "Pronto. Os workflows voltam a funcionar até a sessão expirar (~4h)."
