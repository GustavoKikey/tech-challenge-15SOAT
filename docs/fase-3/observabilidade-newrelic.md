# Observabilidade no New Relic — dashboards e alertas

Consultas NRQL dos painéis exigidos pela fase 3 e das condições de alerta.

---

## Como os dados chegam

| Sinal | Origem | Caminho |
| --- | --- | --- |
| Traces | Aplicação Quarkus | OpenTelemetry → OTLP `http/protobuf` |
| Métricas de negócio | `MetricasGateway` (Micrometer) | OTLP, junto com os traces |
| Métricas técnicas | JVM, HTTP, Vert.x | idem |
| CPU, memória, pods | `nri-bundle` no cluster | agente do New Relic |
| Eventos do Kubernetes | `nri-kube-events` | idem |
| Logs | stdout em JSON | agente do cluster |

O cluster é identificado como `oficina-hom`; a aplicação, como `oficina-app`.

---

## Dashboards exigidos

### 1. Volume diário de ordens de serviço

```sql
SELECT count(oficina.os.abertas)
FROM Metric
WHERE service.name = 'oficina-app'
FACET dateOf(timestamp)
SINCE 7 days ago
```

Versão em série temporal, para acompanhar o dia corrente:

```sql
SELECT rate(sum(oficina.os.abertas), 1 hour)
FROM Metric
WHERE service.name = 'oficina-app'
TIMESERIES SINCE 24 hours ago
```

### 2. Tempo médio de execução por status

```sql
SELECT average(oficina.os.fase.duracao)
FROM Metric
WHERE service.name = 'oficina-app'
FACET fase
SINCE 24 hours ago
```

As três fases correspondem aos estados do ciclo de vida: **Diagnóstico** (do início do
diagnóstico até a aprovação do orçamento), **Execução** (da aprovação até a
finalização) e **Finalizada** (da finalização até a entrega).

Distribuição, para enxergar cauda longa:

```sql
SELECT histogram(oficina.os.fase.duracao, 20, 20)
FROM Metric
WHERE service.name = 'oficina-app'
FACET fase
SINCE 7 days ago
```

### 3. Erros e falhas nas integrações

```sql
SELECT sum(oficina.integracao.falhas)
FROM Metric
WHERE service.name = 'oficina-app'
FACET integracao
TIMESERIES SINCE 24 hours ago
```

Junto das falhas de processamento da própria OS:

```sql
SELECT sum(oficina.os.falhas)
FROM Metric
WHERE service.name = 'oficina-app'
FACET operacao
TIMESERIES SINCE 24 hours ago
```

---

## Painéis complementares

### Latência das APIs

```sql
SELECT percentile(http.server.requests, 50, 95, 99)
FROM Metric
WHERE service.name = 'oficina-app'
FACET uri
SINCE 1 hour ago TIMESERIES
```

### Consumo de recursos do cluster

```sql
SELECT average(cpuUsedCores), average(memoryWorkingSetBytes / 1024 / 1024)
FROM K8sContainerSample
WHERE clusterName = 'oficina-hom' AND namespaceName = 'oficina'
FACET podName
TIMESERIES SINCE 1 hour ago
```

### Réplicas e escala automática

```sql
SELECT latest(podsDesired), latest(podsReady)
FROM K8sReplicasetSample
WHERE clusterName = 'oficina-hom' AND namespaceName = 'oficina'
TIMESERIES SINCE 3 hours ago
```

### Saúde dos pods

```sql
SELECT latest(status)
FROM K8sPodSample
WHERE clusterName = 'oficina-hom' AND namespaceName = 'oficina'
FACET podName
SINCE 30 minutes ago
```

---

## Condições de alerta

### Falha no processamento de ordens de serviço

O alerta exigido pelo enunciado. Dispara quando aparecem falhas em transições de OS.

```sql
SELECT sum(oficina.os.falhas)
FROM Metric
WHERE service.name = 'oficina-app'
```

| Parâmetro | Valor |
| --- | --- |
| Threshold | acima de `0` |
| Janela | 5 minutos |
| Tipo | estático, `above` |

Uma falha isolada pode ser uma transição inválida legítima — cliente tentando aprovar
uma OS já aprovada, por exemplo. O sinal que importa é o **volume**: várias no mesmo
intervalo indicam problema real.

### Falha de integração externa

```sql
SELECT sum(oficina.integracao.falhas)
FROM Metric
WHERE service.name = 'oficina-app'
FACET integracao
```

Threshold acima de `3` em 5 minutos. O envio de e-mail é *best-effort* e falhas
esporádicas não derrubam a operação; sequência sustentada indica o SMTP fora.

### Aplicação sem réplica saudável

```sql
SELECT uniqueCount(podName)
FROM K8sPodSample
WHERE clusterName = 'oficina-hom'
  AND namespaceName = 'oficina'
  AND status = 'Running'
```

Threshold **abaixo de 2** por 5 minutos — o mínimo configurado no HPA.

### Latência acima do aceitável

```sql
SELECT percentile(http.server.requests, 95)
FROM Metric
WHERE service.name = 'oficina-app'
```

Threshold acima de `2` segundos por 5 minutos.

---

## Correlação entre métrica, trace e log

Os contadores saem com **exemplar** OpenMetrics carregando o `trace_id` da requisição
que os incrementou:

```
oficina_os_abertas_total 3.0 # {span_id="6b7c4175...",trace_id="10ff99c9..."} 1.0
```

Isso permite partir de um pico no gráfico e chegar ao trace exato. Os logs da aplicação
carregam os mesmos `traceId` e `spanId`, o que fecha o caminho métrica → trace → log:

```sql
SELECT * FROM Log
WHERE service.name = 'oficina-app' AND trace.id = '<trace_id>'
SINCE 1 hour ago
```

---

## Nota sobre múltiplas réplicas

Contador do Micrometer vive na memória de cada pod. O New Relic coleta de cada réplica
separadamente e soma na consulta — por isso as agregações acima usam `sum` e `count`
em vez de `latest`.

Ler `/q/metrics` pelo Service devolve o valor de **um** pod apenas, e não o total.
