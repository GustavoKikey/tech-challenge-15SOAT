#!/usr/bin/env bash
# =====================================================================
# Proteção das branches main dos quatro repositórios
# ---------------------------------------------------------------------
# O desafio pede a main protegida, sem commits diretos e com merge por
# Pull Request. Marcar "Require a pull request" na interface não basta:
#
#   - sem 'enforce_admins', a regra não vale para quem administra o
#     repositório — que costuma ser exatamente quem vai empurrar direto
#     na pressa. O push passa, e ninguém percebe;
#
#   - sem 'required_status_checks', a CI roda, fica vermelha, e o botão
#     de merge continua verde. A pipeline vira decoração.
#
# Este script fecha os dois buracos. É idempotente.
#
# Aprovação de revisor fica em ZERO de propósito: num trabalho individual
# exigir aprovação de terceiro trava tudo, porque o GitHub não deixa
# ninguém aprovar o próprio Pull Request.
#
# Uso:
#   bash scripts/fase-3/proteger-branches.sh
#   bash scripts/fase-3/proteger-branches.sh --conferir   # só mostra
# =====================================================================
set -euo pipefail

USUARIO="${USUARIO_GITHUB:-GustavoKikey}"
CONFERIR=false
[ "${1:-}" = "--conferir" ] && CONFERIR=true

if ! command -v gh >/dev/null 2>&1; then
  echo "gh não encontrado."
  exit 1
fi

# Repositório e os checks que a CI dele publica. Os nomes precisam bater
# com o campo 'name' do job renderizado — se um job muda de nome, o check
# exigido deixa de existir e NENHUM Pull Request consegue mais mergear.
checks_de() {
  case "$1" in
    tech-challenge-15SOAT)
      echo "Build + testes (unitários e integração)|Validação do Terraform e dos manifestos K8s|Build da imagem Docker" ;;
    oficina-infra-k8s|oficina-infra-database)
      # "Plan (prod)" é o nome renderizado quando o Pull Request tem a
      # main como base — que é o único caso protegido aqui.
      echo "Formato, sintaxe e lint|Plan (prod)" ;;
    oficina-auth-lambda)
      echo "Testes (Vitest)|Terraform — formato e sintaxe" ;;
  esac
}

for repo in tech-challenge-15SOAT oficina-infra-k8s oficina-infra-database oficina-auth-lambda; do
  echo
  echo "══ $USUARIO/$repo"

  if [ "$CONFERIR" = true ]; then
    gh api "repos/$USUARIO/$repo/branches/main/protection" --jq \
      '"   admins=\(.enforce_admins.enabled)  PR=\(.required_pull_request_reviews != null)  checks=\((.required_status_checks.contexts // []) | join(", "))"' \
      2>/dev/null || echo "   sem proteção"
    continue
  fi

  CONTEXTOS=$(checks_de "$repo" | tr '|' '\n' | python -c "import sys,json;print(json.dumps([l.rstrip('\n') for l in sys.stdin if l.strip()]))")

  python - "$CONTEXTOS" <<'PYEOF' > /tmp/protecao.json
import json, sys
print(json.dumps({
    # strict=false de propósito: exigir a branch atualizada com a main
    # obriga a rebase a cada merge alheio, e o ganho não paga o atrito
    # num repositório com um autor só.
    "required_status_checks": {"strict": False, "contexts": json.loads(sys.argv[1])},
    "enforce_admins": True,
    "required_pull_request_reviews": {
        "dismiss_stale_reviews": False,
        "require_code_owner_reviews": False,
        "required_approving_review_count": 0,
    },
    "restrictions": None,
}))
PYEOF

  gh api -X PUT "repos/$USUARIO/$repo/branches/main/protection" \
    --input /tmp/protecao.json >/dev/null
  gh api "repos/$USUARIO/$repo/branches/main/protection" --jq \
    '"   admins=\(.enforce_admins.enabled)  checks=\((.required_status_checks.contexts // []) | join(", "))"'
done

echo
echo "Pronto. A partir daqui, commit direto na main é recusado — inclusive o seu."
echo "Toda mudança passa por Pull Request com a CI verde."
