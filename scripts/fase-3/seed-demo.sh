#!/usr/bin/env bash
# =====================================================================
# Dados de demonstração — catálogo, ordens de serviço e transições
# ---------------------------------------------------------------------
# Popula um ambiente recém-provisionado com o mínimo que a demonstração
# precisa para não aparecer vazia:
#
#   - dois clientes, para provar que um não enxerga a OS do outro;
#   - catálogo de serviços;
#   - ordens de serviço levadas por TODAS as fases do ciclo de vida.
#
# A última parte é a que importa para o painel "tempo médio por status":
# a métrica oficina.os.fase.duracao só existe quando há transição. Sem
# rodar isto, o painel exigido pelo enunciado aparece em branco.
#
# Uso:
#   kubectl -n oficina port-forward svc/oficina-app 8080:80 &
#   bash scripts/fase-3/seed-demo.sh
# =====================================================================
set -euo pipefail

API="${API:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASSWORD:-admin123}"

# --retry cobre o caso mais comum de rodar o seed logo após um deploy: o
# rolling update ainda troca pods, e uma requisição cai num que está
# terminando. Sem isso o seed falha pela metade e deixa OS órfãs.
CURL=(curl -s --retry 5 --retry-delay 2 --retry-all-errors --max-time 30)

campo() { python -c "import sys,json;print(json.load(sys.stdin)[sys.argv[1]])" "$1" 2>/dev/null || echo ""; }

echo "==> autenticando como $ADMIN_USER"
TOKEN=$("${CURL[@]}" -X POST "$API/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | campo accessToken)

if [ -z "$TOKEN" ]; then
  echo "Falha no login. A aplicação está no ar em $API?"
  exit 1
fi
AUTH=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')

# ---------------------------------------------------------------------
# Catálogo
# ---------------------------------------------------------------------
# Reaproveita o que já existe: rodar o seed duas vezes não deve encher o
# catálogo de duplicatas na frente da câmera.
echo "==> catálogo de serviços"
CATALOGO=$("${CURL[@]}" "$API/servicos" "${AUTH[@]}")

servico() {
  local id
  id=$(DESCRICAO="$1" printf '%s' "$CATALOGO" | DESCRICAO="$1" python -c "
import os, sys, json
alvo = os.environ['DESCRICAO']
try:
    for s in json.load(sys.stdin):
        if s.get('descricao') == alvo:
            print(s['id'])
            break
except Exception:
    pass
" 2>/dev/null)

  if [ -n "$id" ]; then
    echo "$id"
    return
  fi
  "${CURL[@]}" -X POST "$API/servicos" "${AUTH[@]}" \
    -d "{\"descricao\":\"$1\",\"valorBase\":$2}" | campo id
}

SRV_OLEO=$(servico "Troca de oleo e filtro" 189.90)
SRV_FREIO=$(servico "Revisao do sistema de freios" 420.00)
SRV_ALINHA=$(servico "Alinhamento e balanceamento" 160.00)
echo "    catálogo pronto"

# ---------------------------------------------------------------------
# Ordens de serviço
# ---------------------------------------------------------------------
# Cada OS percorre o ciclo inteiro. As pausas existem para que as fases
# tenham duração distinguível no gráfico — sem elas, todas as barras do
# painel "tempo médio por status" saem do mesmo tamanho.
abrir_os() {
  local doc="$1" nome="$2" email="$3" placa="$4" marca="$5" modelo="$6" ano="$7" servico="$8"
  local id
  id=$("${CURL[@]}" -X POST "$API/ordens-servico" "${AUTH[@]}" -d "{
    \"cliente\": {\"documento\":\"$doc\",\"nome\":\"$nome\",\"email\":\"$email\",\"telefone\":\"11999990000\"},
    \"veiculo\": {\"placa\":\"$placa\",\"marca\":\"$marca\",\"modelo\":\"$modelo\",\"ano\":$ano},
    \"servicos\": [{\"servicoId\":\"$servico\"}]
  }" | campo id)

  # Sem o id, todo passo seguinte montaria "/ordens-servico//diagnostico"
  # e colheria 405 — um seed pela metade, que só aparece na gravação.
  if [ -z "$id" ]; then
    echo "    ERRO: a API não devolveu id ao abrir a OS de $nome." >&2
    exit 1
  fi
  echo "$id"
}

passo() {
  local os="$1" caminho="$2"
  local codigo
  codigo=$("${CURL[@]}" -o /dev/null -w '%{http_code}' -X POST "$API/ordens-servico/$os$caminho" "${AUTH[@]}")
  printf '    %-22s %s\n' "$caminho" "$codigo"
}

ciclo_completo() {
  local os="$1" pausa="$2"
  passo "$os" "/diagnostico"
  sleep "$pausa"
  passo "$os" "/orcamento"
  passo "$os" "/orcamento/enviar"
  passo "$os" "/orcamento/aprovar"
  sleep "$pausa"
  passo "$os" "/finalizar"
  sleep 1
  passo "$os" "/entregar"
}

echo "==> OS 1 — Ana Souza (ciclo completo)"
OS1=$(abrir_os "529.982.247-25" "Ana Souza" "ana@exemplo.com" "ABC1D23" "VW" "Golf" 2020 "$SRV_OLEO")
ciclo_completo "$OS1" 2

echo "==> OS 2 — Ana Souza (segunda OS, aguardando aprovação)"
OS2=$(abrir_os "529.982.247-25" "Ana Souza" "ana@exemplo.com" "ABC1D23" "VW" "Golf" 2020 "$SRV_FREIO")
passo "$OS2" "/diagnostico"
sleep 3
passo "$OS2" "/orcamento"
passo "$OS2" "/orcamento/enviar"

echo "==> OS 3 — Bruno Lima (a OS que a Ana NÃO pode ver)"
OS3=$(abrir_os "390.533.447-05" "Bruno Lima" "bruno@exemplo.com" "XYZ4E56" "Fiat" "Argo" 2022 "$SRV_ALINHA")
ciclo_completo "$OS3" 1

echo
echo "======================================================================"
echo "  Pronto"
echo "======================================================================"
echo "  OS da Ana (entregue) ......: $OS1"
echo "  OS da Ana (aguardando) ....: $OS2"
echo "  OS do Bruno ...............: $OS3"
echo
echo "  Para demonstrar o isolamento, use o id do Bruno com o token da Ana."
echo "  A resposta tem que ser 403:"
echo
echo "    curl -H \"Authorization: Bearer \$TOKEN_ANA\" \\"
echo "      $API/cliente/ordens-servico/$OS3"
