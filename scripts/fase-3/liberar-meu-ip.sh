#!/usr/bin/env bash
# =====================================================================
# Libera o seu IP público no balanceador da aplicação
# ---------------------------------------------------------------------
# O Service nasce fechado (127.0.0.1/32 em loadBalancerSourceRanges), e
# isso é proposital: a senha do administrador está versionada num
# repositório público, então um balanceador aberto entrega acesso
# administrativo a quem achar a URL.
#
# Rode este script quando trocar de rede — trabalho, casa, celular. O IP
# muda, e o acesso anterior deixa de valer.
#
# Uso:
#   bash scripts/fase-3/liberar-meu-ip.sh                    # detecta sozinho
#   bash scripts/fase-3/liberar-meu-ip.sh 203.0.113.7        # informa um
#   bash scripts/fase-3/liberar-meu-ip.sh 203.0.113.7 198.51.100.4
#   bash scripts/fase-3/liberar-meu-ip.sh --fechar           # tranca de novo
# =====================================================================
set -euo pipefail

NAMESPACE="${NAMESPACE:-oficina}"
SERVICO="${SERVICO:-oficina-app}"
AMOSTRAS="${AMOSTRAS:-4}"

meu_ip() {
    # Dois provedores: o primeiro que responder resolve. Rede corporativa
    # costuma bloquear um ou outro.
    local ip
    ip=$(curl -s --max-time 8 https://checkip.amazonaws.com 2>/dev/null || true)
    ip=$(printf '%s' "$ip" | tr -d '[:space:]')
    if [ -z "$ip" ]; then
        ip=$(curl -s --max-time 8 https://api.ipify.org 2>/dev/null || true)
        ip=$(printf '%s' "$ip" | tr -d '[:space:]')
    fi
    printf '%s' "$ip"
}

if [ "${1:-}" = "--fechar" ]; then
    IPS="127.0.0.1"
    echo "==> fechando o acesso externo"
elif [ $# -gt 0 ]; then
    IPS="$*"
else
    # Consulta mais de uma vez de propósito. Saída corporativa costuma ter
    # vários endereços em rodízio: liberar só o primeiro faz parte das
    # requisições ser descartada em silêncio — e o sintoma, requisições que
    # ora respondem ora expiram, parece instabilidade da aplicação.
    echo "==> descobrindo o(s) IP(s) público(s) desta máquina (${AMOSTRAS} amostras)"
    IPS=""
    for _ in $(seq 1 "$AMOSTRAS"); do
        ip=$(meu_ip)
        case " $IPS " in
            *" $ip "*) ;;
            *) [ -n "$ip" ] && IPS="${IPS:+$IPS }$ip" ;;
        esac
    done
fi

FAIXAS=""
for ip in $IPS; do
    if ! printf '%s' "$ip" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'; then
        echo "Ignorando valor que não é IPv4: '${ip}'"
        continue
    fi
    FAIXAS="${FAIXAS:+$FAIXAS,}\"${ip}/32\""
    echo "    ${ip}/32"
done

if [ -z "$FAIXAS" ]; then
    echo "Nenhum IPv4 válido. Informe manualmente:"
    echo "   bash scripts/fase-3/liberar-meu-ip.sh SEU.IP.AQUI"
    exit 1
fi

echo "==> aplicando no balanceador"
kubectl -n "$NAMESPACE" patch service "$SERVICO" --type merge \
    -p "{\"spec\":{\"loadBalancerSourceRanges\":[${FAIXAS}]}}" >/dev/null

echo "==> aguardando o endereço do balanceador"
ENDERECO=""
for _ in $(seq 1 40); do
    ENDERECO=$(kubectl -n "$NAMESPACE" get svc "$SERVICO" \
        -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)
    [ -n "$ENDERECO" ] && break
    sleep 10
done

if [ -z "$ENDERECO" ]; then
    echo
    echo "O balanceador ainda não recebeu endereço."
    echo "Em cluster kind isso é o esperado e nunca vai acontecer — lá o acesso"
    echo "é pelo nodePort 30080, mapeado para http://localhost:8080."
    echo "No EKS, acompanhe com:  kubectl -n $NAMESPACE get svc $SERVICO -w"
    exit 0
fi

cat <<FIM

======================================================================
  Aplicação acessível
======================================================================
  http://${ENDERECO}

  Liberado para: ${IPS}

  Um balanceador recém-criado leva 2 a 3 minutos para começar a responder,
  mesmo já tendo nome DNS — os health checks precisam passar primeiro.

  Trocou de rede? Rode este script de novo.
  Terminou? bash scripts/fase-3/liberar-meu-ip.sh --fechar
FIM
