# Observabilidade no New Relic — dashboards e alertas

Consultas NRQL dos painéis exigidos pela fase 3 e das condições de alerta.

O painel está versionado: [`scripts/fase-3/dashboard-newrelic.py`](../../scripts/fase-3/dashboard-newrelic.py)
cria ou atualiza o dashboard "Oficina — Operação" a partir destas mesmas consultas.
Painel montado na interface é invisível para revisão e some quando a conta muda; assim,
quando um nome de métrica mudar, a diferença aparece no mesmo Pull Request que a causou.

---

## Como os dados chegam

| Sinal | Origem | Caminho |
| --- | --- | --- |
| Traces | Aplicação Quarkus | OpenTelemetry → OTLP `http/protobuf` |
| Métricas de negócio | `MetricasGateway` (Micrometer) | `/q/metrics` → scrape do agente |
| Métricas técnicas | JVM, HTTP, Vert.x | idem |
| CPU, memória, pods | `nri-bundle` no cluster | agente do New Relic |
| Eventos do Kubernetes | `nri-kube-events` | idem |
| Logs | stdout em JSON | agente do cluster |

São **dois caminhos distintos**, e essa separação explica quase toda dúvida que aparece
depois. Traces saem da aplicação direto para o New Relic, por OTLP. Métricas não: elas
ficam no registry do Micrometer, expostas no endpoint Prometheus do pod, e dependem do
agente do cluster raspá-las.

Por isso o agente não é opcional. Sem ele os traces continuam chegando — o que dá a
impressão de que a observabilidade está de pé — enquanto os três painéis exigidos pelo
enunciado ficam vazios. Instalação em [`scripts/fase-3/03-observabilidade.sh`](../../scripts/fase-3/03-observabilidade.sh).

### Os nomes mudam no caminho

As métricas nascem no Micrometer com nome pontuado, mas **não chegam ao New Relic
assim**. A convenção do Prometheus troca ponto por underscore e acrescenta sufixo por
tipo de métrica:

| No código | No New Relic |
| --- | --- |
| `oficina.os.abertas` | `oficina_os_abertas_total` |
| `oficina.os.fase.duracao` | `oficina_os_fase_duracao_seconds_sum` / `_count` / `_max` |
| `oficina.os.falhas` | `oficina_os_falhas_total` |
| `oficina.integracao.falhas` | `oficina_integracao_falhas_total` |

Pelo mesmo motivo o filtro de serviço **não** é `service.name`: esse atributo existe nos
spans, que vêm por OTLP. Nas métricas raspadas, quem identifica a aplicação é o rótulo
que o Kubernetes injeta — `app_kubernetes_io_name`.

Consultar `oficina.os.abertas` filtrando por `service.name` devolve vazio sem erro
nenhum. É a forma mais fácil de concluir que a observabilidade não funciona quando o
problema é só o nome.

---

## Dashboards exigidos

### 1. Volume diário de ordens de serviço

```sql
SELECT sum(oficina_os_abertas_total) AS 'OS abertas'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET dateOf(timestamp)
SINCE 7 days ago
```

Total do dia corrente, para o cabeçalho:

```sql
SELECT sum(oficina_os_abertas_total) AS 'OS abertas'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
SINCE today
```

`sum` e não `latest`: o contador chega como cumulativo, e o New Relic guarda em cada
ponto o incremento desde a coleta anterior. Somar os incrementos da janela dá o volume
do período; pegar o último ponto daria só o que aconteceu nos últimos trinta segundos.

Consequência prática, que aparece em ambiente recém-provisionado: a **primeira** coleta
apenas estabelece a linha de base. Ordens de serviço criadas antes dela não entram na
conta — o painel só passa a contar o que acontece depois de o agente subir.

### 2. Tempo médio de execução por status

```sql
SELECT sum(oficina_os_fase_duracao_seconds_sum)
     / sum(oficina_os_fase_duracao_seconds_count) AS 'Segundos'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET fase
SINCE 24 hours ago
```

A divisão de somas é o que dá a média correta entre vários pods: cada réplica tem seu
próprio histograma, e `average` sobre os pontos daria a média das médias, ponderada
errado.

As três fases correspondem aos estados do ciclo de vida: **Diagnóstico** (do início do
diagnóstico até a aprovação do orçamento), **Execução** (da aprovação até a finalização)
e **Finalizada** (da finalização até a entrega).

Evolução no tempo, para enxergar tendência:

```sql
SELECT sum(oficina_os_fase_duracao_seconds_sum)
     / sum(oficina_os_fase_duracao_seconds_count) AS 'Segundos'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET fase
TIMESERIES SINCE 24 hours ago
```

### 3. Erros e falhas nas integrações

```sql
SELECT sum(oficina_integracao_falhas_total) AS 'Falhas'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET integracao
TIMESERIES SINCE 24 hours ago
```

Junto das falhas de processamento da própria OS:

```sql
SELECT sum(oficina_os_falhas_total) AS 'Falhas'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET operacao
TIMESERIES SINCE 24 hours ago
```

Painel vazio aqui é o estado saudável: a série só passa a existir quando há a primeira
falha. Em ambiente com o mailer em modo mock — o padrão fora de produção real — não há
integração externa que possa falhar, e `oficina_integracao_falhas_total` legitimamente
não existe.

---

## Painéis complementares

### Latência das APIs

```sql
SELECT sum(http_server_requests_seconds_sum)
     / sum(http_server_requests_seconds_count) AS 'Segundos'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET uri
TIMESERIES SINCE 1 hour ago
```

O `uri` vem normalizado pelo Micrometer (`/ordens-servico/{id}/finalizar`), e não com o
identificador concreto — sem isso cada requisição viraria uma série própria.

### Requisições por status

```sql
SELECT sum(http_server_requests_seconds_count) AS 'Requisições'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET status
SINCE 1 hour ago
```

### Consumo de recursos do cluster

```sql
SELECT average(cpuUsedCores) AS 'Cores'
FROM K8sContainerSample
WHERE clusterName = 'oficina-prod' AND namespaceName = 'oficina'
FACET podName
TIMESERIES SINCE 1 hour ago
```

```sql
SELECT average(memoryWorkingSetBytes) / 1e6 AS 'MB'
FROM K8sContainerSample
WHERE clusterName = 'oficina-prod' AND namespaceName = 'oficina'
FACET podName
TIMESERIES SINCE 1 hour ago
```

O `clusterName` é `oficina-<ambiente>`. Trocar de ambiente exige trocá-lo nas consultas —
o script do dashboard recebe o ambiente como argumento justamente por isso.

### Healthcheck e disponibilidade

O enunciado pede healthchecks e uptime. Eles existem em duas camadas, e vale distinguir
o que cada uma responde.

**Dentro do cluster**, o Kubernetes executa três sondas contra a aplicação — as do
SmallRye Health, declaradas em `k8s/30-deployment.yaml`:

| Sonda | Endpoint | O que decide |
| --- | --- | --- |
| `startupProbe` | `/q/health/started` | segura as outras até o boot e as migrações terminarem |
| `readinessProbe` | `/q/health/ready` | tira o pod do Service quando ele não pode atender |
| `livenessProbe` | `/q/health/live` | reinicia o container quando a aplicação trava |

A distinção entre *ready* e *live* é o que evita o pior caso: um pod que perdeu o banco
sai do balanceamento (readiness falha) mas **não** é reiniciado, porque reiniciar não
traria o banco de volta — só transformaria uma indisponibilidade parcial em um ciclo de
reinícios.

**Disponibilidade observada**, medida pelo que o cliente de fato recebeu:

```sql
SELECT percentage(sum(http_server_requests_seconds_count), WHERE status NOT LIKE '5%')
       AS 'Disponibilidade'
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
TIMESERIES SINCE 24 hours ago
```

Erro 4xx não entra na conta: cliente mandando CPF inválido não é indisponibilidade do
serviço. O que conta é `5xx`, onde a culpa é da aplicação.

Réplicas prontas ao longo do tempo, que é o sinal antecedente — a disponibilidade cai
*depois* que as réplicas caem:

```sql
SELECT uniqueCount(podName) AS 'Pods prontos'
FROM K8sPodSample
WHERE clusterName = 'oficina-prod'
  AND namespaceName = 'oficina'
  AND status = 'Running'
TIMESERIES SINCE 24 hours ago
```

Reinícios de container, que denunciam pod instável mesmo quando a disponibilidade ainda
está boa — o cluster está absorvendo o problema, e isso não dura para sempre:

```sql
SELECT sum(restartCount) AS 'Reinícios'
FROM K8sContainerSample
WHERE clusterName = 'oficina-prod' AND namespaceName = 'oficina'
FACET podName
SINCE 24 hours ago
```

### Saúde dos pods

```sql
SELECT latest(status) AS 'Status', latest(createdAt) AS 'Criado'
FROM K8sPodSample
WHERE clusterName = 'oficina-prod' AND namespaceName = 'oficina'
FACET podName
SINCE 30 minutes ago
```

### Traces recebidos

```sql
SELECT count(*) AS 'Spans'
FROM Span
WHERE service.name = 'oficina-app'
TIMESERIES SINCE 1 hour ago
```

Aqui `service.name` **é** o filtro certo: span vem por OTLP, com o atributo de recurso
que a aplicação declara.

---

## Condições de alerta

### Falha no processamento de ordens de serviço

O alerta exigido pelo enunciado. Dispara quando aparecem falhas em transições de OS.

```sql
SELECT sum(oficina_os_falhas_total)
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
```

| Parâmetro | Valor |
| --- | --- |
| Threshold | acima de `3` |
| Janela | 5 minutos |
| Tipo | estático, `above` |

Uma falha isolada pode ser uma transição inválida legítima — cliente tentando aprovar
uma OS já aprovada, por exemplo. O sinal que importa é o **volume**: várias no mesmo
intervalo indicam problema real.

### Falha de integração externa

```sql
SELECT sum(oficina_integracao_falhas_total)
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
FACET integracao
```

Threshold acima de `3` em 5 minutos. O envio de e-mail é *best-effort* e falhas
esporádicas não derrubam a operação; sequência sustentada indica o SMTP fora.

### Aplicação sem réplica saudável

```sql
SELECT uniqueCount(podName)
FROM K8sPodSample
WHERE clusterName = 'oficina-prod'
  AND namespaceName = 'oficina'
  AND status = 'Running'
```

Threshold **abaixo de 2** por 5 minutos — o mínimo configurado no HPA.

### Latência acima do aceitável

```sql
SELECT sum(http_server_requests_seconds_sum)
     / sum(http_server_requests_seconds_count)
FROM Metric
WHERE app_kubernetes_io_name = 'oficina-app'
```

Threshold acima de `2` segundos por 5 minutos.

---

## Correlação entre métrica, trace e log

Os contadores saem com **exemplar** OpenMetrics carregando o `trace_id` da requisição
que os incrementou:

```
oficina_os_falhas_total{operacao="finalizar"} 1.0 # {span_id="d38e65a3...",trace_id="76f10e99..."} 1.0
```

Isso permite partir de um pico no gráfico e chegar ao trace exato. Os logs da aplicação
carregam os mesmos identificadores no MDC:

```json
{"level":"INFO","message":"Notificação de status enviada — OS 29426713… → EM_EXECUCAO",
 "mdc":{"spanId":"9d54fa28e30359d4","traceId":"09ccc36463d59ea1f6f3b43bf5925d6f","sampled":"true"},
 "service.name":"oficina-app"}
```

O que fecha o caminho métrica → trace → log:

```sql
SELECT * FROM Log
WHERE trace.id = '<trace_id>'
SINCE 1 hour ago
```

O exemplar depende do **SDK do OpenTelemetry estar ativo** — não do exportador. É o SDK
que abre o span de onde sai o `trace_id`. Com `OTEL_SDK_DISABLED=true`, os contadores
continuam funcionando e simplesmente perdem o exemplar, junto com o `traceId` no log: a
correlação some sem nenhum erro aparecer.

---

## Nota sobre múltiplas réplicas

Contador do Micrometer vive na memória de cada pod. O agente coleta de cada réplica
separadamente e o New Relic soma na consulta — por isso as agregações acima usam `sum`,
e as médias dividem soma por contagem em vez de usar `average`.

Ler `/q/metrics` pelo Service devolve o valor de **um** pod apenas, e não o total.
