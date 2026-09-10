# ADR 003 — Escalabilidade com Horizontal Pod Autoscaler

- **Status:** aceito (fase 2, revalidado na fase 3)
- **Data:** 2026-09-08

---

## Contexto

O enunciado da fase 2 pedia escalar *"conforme consumo de CPU/memória"*; o da fase 3
pede *"cluster Kubernetes com escalabilidade"* e monitorar *"consumo de recursos do
Kubernetes (CPU, memória)"*.

A carga da oficina é irregular: concentra-se na abertura da manhã e no fim da tarde,
com longos períodos de baixa. Dimensionar para o pico desperdiça recurso o dia inteiro;
dimensionar para a média derruba o serviço no pico.

## Decisão

**HPA nativo do Kubernetes**, escalando de **2 a 5 réplicas** por CPU e memória.

| Parâmetro | Valor | Justificativa |
| --- | --- | --- |
| `minReplicas` | 2 | Uma réplica só significa indisponibilidade durante qualquer rollout ou queda de nó. Dois é o mínimo para o PDB fazer sentido. |
| `maxReplicas` | 5 | Teto compatível com o tamanho dos nós e com o limite de conexões do banco. |
| Métrica | CPU e memória | Métricas que o `metrics-server` entrega sem instrumentação adicional. |
| Alvo de memória | 80% | Casado com `-XX:MaxRAMPercentage=50.0` no container: o heap fica abaixo do alvo, evitando escalar por memória que a JVM simplesmente reservou. |

Acompanham o HPA:

- **PodDisruptionBudget** — impede que manutenção do cluster derrube as duas réplicas
  ao mesmo tempo;
- **Probes** `started`/`ready`/`live` sobre o SmallRye Health — réplica nova só recebe
  tráfego quando está pronta;
- **`requests` e `limits`** definidos — sem `requests`, o HPA não tem base de cálculo.

## Alternativas consideradas

| Alternativa | Por que não |
| --- | --- |
| **Réplicas fixas** | Não atende ao requisito e desperdiça ou falta recurso, conforme a hora. |
| **KEDA** (escala por evento) | Faz sentido para fila ou métrica externa. Aqui a carga é HTTP síncrona; CPU e memória descrevem bem a pressão. Traria um operador a mais para manter. |
| **HPA por métrica customizada** (ex.: OS abertas por minuto) | Atraente agora que existe `oficina_os_abertas_total`, mas exige Prometheus Adapter e acopla a escala a uma métrica de negócio que pode variar sem relação com saturação. Reavaliar se CPU se mostrar mau indicador. |
| **Vertical Pod Autoscaler** | Escala o pod, não a quantidade. Não dá alta disponibilidade e exige reinício para aplicar. |

## Consequências

**Positivas**

- Absorve pico sem intervenção; devolve recurso na baixa.
- `minReplicas: 2` dá disponibilidade durante rollout.
- Demonstrável ao vivo: `scripts/gerador-carga.yaml` + `kubectl get hpa -w`.

**Negativas / custos assumidos**

- **A aplicação precisa ser stateless** — vale hoje, e passa a valer com mais força na
  fase 3: contador do Micrometer vive na memória de cada pod, então cada réplica tem o
  seu. Não é defeito (o New Relic faz scrape por pod e soma), mas quebra a leitura
  manual de `/q/metrics` via Service. Registrado no README do repositório da aplicação.
- Escalar pods multiplica conexões ao banco. Com `db.t3.micro` e 5 réplicas, vale
  observar saturação — [RFC 002](../rfc/rfc-002-escolha-do-banco.md), §7.
- Cold start da JVM (segundos) faz a réplica nova demorar a ajudar. Mitigado pelas
  probes e pelo `minReplicas: 2`.

## Verificação

Fase 2: HPA lendo métricas reais em cluster kind, escalando de 2 para 5 sob carga
gerada. Fase 3: mantido sem alteração; o alvo passa a ser EKS (ou k3s em EC2, se o
o Learner Lab não permitir o EKS — o manifesto do HPA não muda em nenhum dos casos).
