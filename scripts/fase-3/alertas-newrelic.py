#!/usr/bin/env python3
"""
Condições de alerta do New Relic como código.

Reconcilia a policy "Oficina — Operação" com as quatro condições descritas em
docs/fase-3/observabilidade-newrelic.md, deixando exatamente esse conjunto: o
que estiver lá é removido, e as quatro são criadas.

A remoção importa mais do que parece. Condição criada à mão e esquecida vira
alerta que dispara por motivo errado — ou, pior, duplicata que avisa duas
vezes o mesmo incidente e treina quem recebe a ignorar. Era o caso aqui: havia
duas condições de falha de processamento, e a de réplicas apontava para um
cluster que não existe mais.

Uso:
    export NEW_RELIC_API_KEY=NRAK-...
    export NEW_RELIC_ACCOUNT_ID=1234567
    python scripts/fase-3/alertas-newrelic.py prod
"""
import json
import os
import sys
import urllib.request

API = "https://api.newrelic.com/graphql"
POLICY = "Oficina — Operação"


def graphql(consulta, variaveis=None):
    corpo = {"query": consulta}
    if variaveis:
        corpo["variables"] = variaveis
    req = urllib.request.Request(
        API,
        data=json.dumps(corpo).encode(),
        headers={"Content-Type": "application/json", "API-Key": CHAVE},
    )
    with urllib.request.urlopen(req, timeout=90) as r:
        resposta = json.load(r)
    if resposta.get("errors"):
        raise SystemExit("GraphQL: " + json.dumps(resposta["errors"], ensure_ascii=False)[:800])
    return resposta["data"]


def condicoes(cluster):
    app = "WHERE app_kubernetes_io_name = 'oficina-app'"
    return [
        {
            "name": "Falha no processamento de ordens de servico",
            # O alerta nomeado pelo enunciado. O limiar não é uma falha, é o
            # volume: transição inválida isolada costuma ser o cliente
            # tentando aprovar uma OS já aprovada.
            "nrql": f"SELECT sum(oficina_os_falhas_total) FROM Metric {app}",
            "threshold": 3.0,
            "duracao": 300,
        },
        {
            "name": "Falha em integracao externa",
            # Notificação é best-effort: falha esporádica não derruba a
            # operação. Sequência sustentada indica o SMTP fora.
            "nrql": f"SELECT sum(oficina_integracao_falhas_total) FROM Metric {app}",
            "threshold": 3.0,
            "duracao": 300,
        },
        {
            "name": "Latencia acima do aceitavel",
            # Divisão de somas, não média de razões: cada réplica tem seu
            # próprio histograma, e a média das médias pondera errado.
            "nrql": (
                "SELECT sum(http_server_requests_seconds_sum) / "
                f"sum(http_server_requests_seconds_count) FROM Metric {app}"
            ),
            "threshold": 2.0,
            "duracao": 300,
        },
        {
            "name": "Aplicacao sem replicas suficientes",
            "nrql": (
                "SELECT uniqueCount(podName) FROM K8sPodSample "
                f"WHERE clusterName = '{cluster}' AND namespaceName = 'oficina' "
                "AND status = 'Running'"
            ),
            "threshold": 2.0,
            "duracao": 300,
            "operador": "BELOW",
        },
    ]


def corpo_condicao(c):
    return {
        "name": c["name"],
        "enabled": True,
        "nrql": {"query": c["nrql"]},
        "signal": {"aggregationWindow": 60, "aggregationMethod": "EVENT_FLOW", "aggregationDelay": 120},
        "terms": [{
            "threshold": c["threshold"],
            "thresholdOccurrences": "ALL",
            "thresholdDuration": c["duracao"],
            "operator": c.get("operador", "ABOVE"),
            "priority": "CRITICAL",
        }],
        # Sem sinal por uma hora, o incidente fecha sozinho: métrica que
        # deixou de existir não é o mesmo que problema resolvido, mas manter
        # incidente aberto para sempre também não ajuda ninguém.
        "expiration": {"closeViolationsOnExpiration": True, "expirationDuration": 3600},
        "violationTimeLimitSeconds": 86400,
    }


if __name__ == "__main__":
    ambiente = sys.argv[1] if len(sys.argv) > 1 else "hom"
    cluster = os.environ.get("CLUSTER", f"oficina-{ambiente}")

    CHAVE = os.environ.get("NEW_RELIC_API_KEY", "")
    CONTA = os.environ.get("NEW_RELIC_ACCOUNT_ID", "")
    if not CHAVE or not CONTA:
        raise SystemExit("Defina NEW_RELIC_API_KEY e NEW_RELIC_ACCOUNT_ID.")
    CONTA = int(CONTA)

    dados = graphql(
        "{ actor { account(id: %d) { alerts { policiesSearch "
        "{ policies { id name } } } } } }" % CONTA
    )
    politicas = dados["actor"]["account"]["alerts"]["policiesSearch"]["policies"]
    politica = next((p for p in politicas if p["name"] == POLICY), None)

    if politica is None:
        r = graphql(
            "mutation($c: Int!, $p: AlertsPolicyInput!) { "
            "alertsPolicyCreate(accountId: $c, policy: $p) { id } }",
            {"c": CONTA, "p": {"name": POLICY, "incidentPreference": "PER_CONDITION"}},
        )
        policy_id = r["alertsPolicyCreate"]["id"]
        print(f"policy criada: {policy_id}")
        existentes = []
    else:
        policy_id = politica["id"]
        print(f"policy existente: {policy_id}")
        r = graphql(
            '{ actor { account(id: %d) { alerts { nrqlConditionsSearch'
            '(searchCriteria: {policyId: "%s"}) { nrqlConditions { id name } } } } } }'
            % (CONTA, policy_id)
        )
        existentes = r["actor"]["account"]["alerts"]["nrqlConditionsSearch"]["nrqlConditions"]

    desejadas = condicoes(cluster)

    # Recriar em vez de atualizar. Condição criada por outra via da API não
    # aceita alertsNrqlConditionStaticUpdate — a mutação devolve SERVER_ERROR
    # sem dizer por quê, inclusive com o corpo mínimo. Create e delete
    # funcionam em qualquer caso, e recriar mantém o script idempotente sem
    # depender de como a condição nasceu.
    #
    # O custo é uma janela de alguns segundos sem alerta configurado, entre a
    # remoção e a criação. Aceitável para um script de provisionamento;
    # não seria, se rodasse em resposta a incidente.
    for c in existentes:
        graphql(
            "mutation($c: Int!, $id: ID!) { alertsConditionDelete(accountId: $c, id: $id) { id } }",
            {"c": CONTA, "id": c["id"]},
        )
        print(f"  removida    {c['name']}")

    for d in desejadas:
        graphql(
            "mutation($c: Int!, $p: ID!, $cond: AlertsNrqlConditionStaticInput!) { "
            "alertsNrqlConditionStaticCreate(accountId: $c, policyId: $p, condition: $cond) "
            "{ id } }",
            {"c": CONTA, "p": policy_id, "cond": corpo_condicao(d)},
        )
        print(f"  criada      {d['name']}")

    print(f"\nOK — policy '{POLICY}' com {len(desejadas)} condições, cluster {cluster}")
