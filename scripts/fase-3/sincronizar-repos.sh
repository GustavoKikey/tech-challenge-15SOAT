#!/usr/bin/env bash
# =====================================================================
# Sincroniza os repositórios separados com o código deste monorepo
# ---------------------------------------------------------------------
# O desafio pede quatro repositórios independentes, mas o código nasce e
# evolui aqui — os outros três foram criados por 'git subtree split' e
# ficam parados no momento do corte.
#
# Isso é pior do que parece. Repositório de infraestrutura desatualizado
# não fica só velho: ele fica ERRADO de um jeito perigoso. Um deles ainda
# apontava para a chave de state anterior à separação por ambiente — a
# mesma que fez um apply de 'prod' destruir o cluster de 'hom'. A
# pipeline dele vinha falhando por outro motivo, e essa falha, por
# acidente, era a única coisa impedindo o estrago.
#
# Este script copia o estado atual de cada diretório para o clone do
# repositório correspondente e abre um Pull Request. NÃO faz merge: o
# job de plan roda no PR justamente para que o efeito seja lido antes de
# ser aplicado.
#
# Uso:
#   bash scripts/fase-3/sincronizar-repos.sh
#   bash scripts/fase-3/sincronizar-repos.sh --so-diferencas   # só compara
# =====================================================================
set -euo pipefail

USUARIO="${USUARIO_GITHUB:-GustavoKikey}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TRABALHO="${TRABALHO:-$(mktemp -d)}"
BRANCH="${BRANCH:-sync/monorepo}"
SO_DIFERENCAS=false
[ "${1:-}" = "--so-diferencas" ] && SO_DIFERENCAS=true

PARES="infra-k8s:oficina-infra-k8s infra-database:oficina-infra-database lambda-auth:oficina-auth-lambda"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh não encontrado — necessário para abrir os Pull Requests."
  exit 1
fi

for par in $PARES; do
  DIR="${par%%:*}"
  REPO="${par##*:}"

  echo
  echo "══════════════════════════════════════════════════════════════════════"
  echo "  $DIR  →  $USUARIO/$REPO"
  echo "══════════════════════════════════════════════════════════════════════"

  CLONE="$TRABALHO/$REPO"
  rm -rf "$CLONE"
  git clone -q "https://github.com/$USUARIO/$REPO.git" "$CLONE"

  # Remove o conteúdo versionado antes de copiar: sem isso, arquivo
  # apagado no monorepo sobreviveria para sempre no repositório separado.
  ( cd "$CLONE" && git ls-files -z | xargs -0 rm -f 2>/dev/null || true )

  # Copia exatamente o que o Git rastreia — nada de .terraform/, state
  # local ou node_modules.
  ( cd "$RAIZ" && git ls-files "$DIR" ) | while read -r arquivo; do
    destino="$CLONE/${arquivo#"$DIR"/}"
    mkdir -p "$(dirname "$destino")"
    cp "$RAIZ/$arquivo" "$destino"
  done

  cd "$CLONE"
  if git diff --quiet && [ -z "$(git status --porcelain)" ]; then
    echo "  já está sincronizado"
    continue
  fi

  echo "  diferenças:"
  git add -A
  git --no-pager diff --cached --stat | sed 's/^/    /'

  if [ "$SO_DIFERENCAS" = true ]; then
    continue
  fi

  git checkout -q -b "$BRANCH"
  git commit -q -m "chore: sincroniza com a evolução do código de origem

Traz as correções aplicadas depois da separação dos repositórios. A mais
importante é a chave do state do Terraform, que passa a incluir o ambiente
e chega por -backend-config no init: com a chave fixa por módulo, aplicar
'prod' sobre o state de 'hom' não cria um segundo ambiente — o Terraform lê
como renomeação e destrói o que existe."

  git push -q -u origin "$BRANCH" --force
  gh pr create --repo "$USUARIO/$REPO" --base main --head "$BRANCH" \
    --title "chore: sincroniza com a evolução do código de origem" \
    --body "Traz as correções aplicadas depois da separação dos repositórios.

A mais importante é a **chave do state do Terraform**: passa a incluir o
ambiente e a chegar por \`-backend-config\` no \`init\`. Com a chave fixa por
módulo, aplicar \`prod\` sobre o state de \`hom\` não cria um segundo
ambiente — o Terraform lê a mudança como renomeação dos recursos que
gerencia e destrói o que existe para recriar com o nome novo.

Confira o **plano** antes de aprovar: como a infraestrutura já está no ar, o
esperado é um plano sem alterações. Qualquer coisa além disso é divergência
entre o que está aplicado e o que está descrito, e merece leitura." 2>&1 | tail -1 | sed 's/^/  PR: /'
done

echo
echo "Pull Requests abertos. Leia o plano de cada um antes de aprovar."
echo "Diretório de trabalho: $TRABALHO"
