#!/usr/bin/env python3
"""
Dashboard do New Relic como código.

Cria (ou atualiza) o painel "Oficina — Operação" com os gráficos que o
enunciado da fase 3 exige, mais os complementares de infraestrutura.

Por que existe
--------------
Painel montado na interface é invisível para a revisão e se perde quando a
conta muda. Aqui as consultas ficam versionadas junto do código que produz
as métricas — quando um nome de métrica muda, o diff aparece no mesmo Pull
Request.

Sobre os nomes das métricas
---------------------------
Elas nascem no Micrometer com nome pontuado ("oficina.os.abertas"), mas
NÃO chegam ao New Relic assim. O caminho é o endpoint Prometheus do pod,
raspado pelo agente do cluster — e a convenção do Prometheus troca ponto
por underscore e acrescenta sufixo por tipo:

    oficina.os.abertas       ->  oficina_os_abertas_total
    oficina.os.fase.duracao  ->  oficina_os_fase_duracao_seconds_{sum,count,max}

Pelo mesmo motivo o filtro de serviço NÃO é `service.name`: esse atributo
existe nos spans (OTLP), não nas métricas raspadas. Nelas o que identifica
a aplicação é o label vindo do Kubernetes, `app_kubernetes_io_name`.

Uso
---
    export NEW_RELIC_API_KEY=NRAK-...        # chave de usuário, não a de ingestão
    export NEW_RELIC_ACCOUNT_ID=1234567
    python scripts/fase-3/dashboard-newrelic.py prod
"""
import json
import os
import sys
import urllib.request

API = "https://api.newrelic.com/graphql"
NOME = "Oficina — Operação"


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
        raise SystemExit("GraphQL: " + json.dumps(resposta["errors"])[:600])
    return resposta["data"]


def widget(titulo, viz, consulta, coluna, linha, largura=4, altura=3):
    return {
        "title": titulo,
        "layout": {"column": coluna, "row": linha, "width": largura, "height": altura},
        "visualization": {"id": viz},
        "rawConfiguration": {
            "nrqlQueries": [{"accountId": CONTA, "query": consulta}],
            "platformOptions": {"ignoreTimeRange": False},
        },
    }


def markdown(texto, coluna, linha, largura=12, altura=1):
    return {
        "title": "",
        "layout": {"column": coluna, "row": linha, "width": largura, "height": altura},
        "visualization": {"id": "viz.markdown"},
        "rawConfiguration": {"text": texto},
    }


def paginas(cluster):
    app = "WHERE app_kubernetes_io_name = 'oficina-app'"
    k8s = f"WHERE clusterName = '{cluster}' AND namespaceName = 'oficina'"

    negocio = [
        markdown(
            "## Negócio\n"
            "Os três painéis exigidos pelo enunciado. As métricas nascem no caso de "
            "uso, não na infraestrutura: quem as incrementa é a regra de negócio.",
            1, 1,
        ),
        widget(
            "Ordens de serviço abertas — hoje", "viz.billboard",
            f"SELECT sum(oficina_os_abertas_total) AS 'OS abertas' FROM Metric {app} SINCE today",
            1, 2, largura=3,
        ),
        widget(
            "Volume diário de ordens de serviço", "viz.bar",
            f"SELECT sum(oficina_os_abertas_total) AS 'OS abertas' FROM Metric {app} "
            "FACET dateOf(timestamp) SINCE 7 days ago",
            4, 2, largura=5,
        ),
        widget(
            "Abertura ao longo do dia", "viz.line",
            f"SELECT sum(oficina_os_abertas_total) AS 'OS abertas' FROM Metric {app} "
            "TIMESERIES SINCE 24 hours ago",
            9, 2, largura=4,
        ),
        widget(
            "Tempo médio de execução por status", "viz.bar",
            "SELECT sum(oficina_os_fase_duracao_seconds_sum) / "
            "sum(oficina_os_fase_duracao_seconds_count) AS 'Segundos' "
            f"FROM Metric {app} FACET fase SINCE 24 hours ago",
            1, 5, largura=6,
        ),
        widget(
            "Duração das fases ao longo do tempo", "viz.line",
            "SELECT sum(oficina_os_fase_duracao_seconds_sum) / "
            "sum(oficina_os_fase_duracao_seconds_count) AS 'Segundos' "
            f"FROM Metric {app} FACET fase TIMESERIES SINCE 24 hours ago",
            7, 5, largura=6,
        ),
        widget(
            "Falhas no processamento de ordens de serviço", "viz.line",
            f"SELECT sum(oficina_os_falhas_total) AS 'Falhas' FROM Metric {app} "
            "FACET operacao TIMESERIES SINCE 24 hours ago",
            1, 8, largura=6,
        ),
        widget(
            "Falhas nas integrações externas", "viz.line",
            f"SELECT sum(oficina_integracao_falhas_total) AS 'Falhas' FROM Metric {app} "
            "FACET integracao TIMESERIES SINCE 24 hours ago",
            7, 8, largura=6,
        ),
    ]

    infra = [
        markdown(
            "## Infraestrutura\n"
            "Latência, recursos do cluster e saúde dos pods. O cluster monitorado é "
            f"`{cluster}`.",
            1, 1,
        ),
        widget(
            "Latência média por rota", "viz.line",
            "SELECT sum(http_server_requests_seconds_sum) / "
            "sum(http_server_requests_seconds_count) AS 'Segundos' "
            f"FROM Metric {app} FACET uri TIMESERIES SINCE 1 hour ago",
            1, 2, largura=8,
        ),
        widget(
            "Requisições por status", "viz.pie",
            f"SELECT sum(http_server_requests_seconds_count) AS 'Requisições' FROM Metric {app} "
            "FACET status SINCE 1 hour ago",
            9, 2, largura=4,
        ),
        widget(
            "CPU por pod", "viz.line",
            f"SELECT average(cpuUsedCores) AS 'Cores' FROM K8sContainerSample {k8s} "
            "FACET podName TIMESERIES SINCE 1 hour ago",
            1, 5, largura=6,
        ),
        widget(
            "Memória por pod", "viz.line",
            "SELECT average(memoryWorkingSetBytes) / 1e6 AS 'MB' "
            f"FROM K8sContainerSample {k8s} FACET podName TIMESERIES SINCE 1 hour ago",
            7, 5, largura=6,
        ),
        widget(
            "Pods em execução", "viz.billboard",
            f"SELECT uniqueCount(podName) AS 'Pods' FROM K8sPodSample {k8s} "
            "AND status = 'Running' SINCE 5 minutes ago",
            1, 8, largura=3,
        ),
        widget(
            "Saúde dos pods", "viz.table",
            f"SELECT latest(status) AS 'Status', latest(createdAt) AS 'Criado' "
            f"FROM K8sPodSample {k8s} FACET podName SINCE 30 minutes ago",
            4, 8, largura=5,
        ),
        widget(
            "Traces recebidos", "viz.line",
            "SELECT count(*) AS 'Spans' FROM Span WHERE service.name = 'oficina-app' "
            "TIMESERIES SINCE 1 hour ago",
            9, 8, largura=4,
        ),
    ]

    return [
        {"name": "Negócio", "description": "Painéis exigidos pelo enunciado", "widgets": negocio},
        {"name": "Infraestrutura", "description": "Latência, recursos e saúde", "widgets": infra},
    ]


def encontrar():
    dados = graphql(
        '{ actor { entitySearch(query: "type = \'DASHBOARD\'") '
        "{ results { entities { guid name } } } } }"
    )
    for e in dados["actor"]["entitySearch"]["results"]["entities"]:
        # O parent tem o nome exato; as páginas vêm como "Nome / Página".
        if e["name"] == NOME:
            return e["guid"]
    return None


if __name__ == "__main__":
    ambiente = sys.argv[1] if len(sys.argv) > 1 else "hom"
    cluster = os.environ.get("CLUSTER", f"oficina-{ambiente}")

    CHAVE = os.environ.get("NEW_RELIC_API_KEY", "")
    CONTA = os.environ.get("NEW_RELIC_ACCOUNT_ID", "")
    if not CHAVE or not CONTA:
        raise SystemExit(
            "Defina NEW_RELIC_API_KEY (chave de usuário, prefixo NRAK) e "
            "NEW_RELIC_ACCOUNT_ID."
        )
    CONTA = int(CONTA)

    corpo = {
        "name": NOME,
        "description": f"Tech Challenge fase 3 — cluster {cluster}",
        "permissions": "PUBLIC_READ_WRITE",
        "pages": paginas(cluster),
    }

    guid = encontrar()
    if guid:
        print(f"atualizando dashboard existente ({guid})")
        r = graphql(
            "mutation($guid: EntityGuid!, $d: DashboardInput!) { "
            "dashboardUpdate(guid: $guid, dashboard: $d) "
            "{ errors { description type } } }",
            {"guid": guid, "d": corpo},
        )
        erros = r["dashboardUpdate"]["errors"]
    else:
        print("criando dashboard")
        r = graphql(
            "mutation($conta: Int!, $d: DashboardInput!) { "
            "dashboardCreate(accountId: $conta, dashboard: $d) "
            "{ entityResult { guid } errors { description type } } }",
            {"conta": CONTA, "d": corpo},
        )
        erros = r["dashboardCreate"]["errors"]
        guid = (r["dashboardCreate"].get("entityResult") or {}).get("guid")

    if erros:
        raise SystemExit("Falhou: " + json.dumps(erros, ensure_ascii=False)[:800])

    print(f"OK — {NOME}")
    print(f"https://one.newrelic.com/redirect/entity/{guid}")
