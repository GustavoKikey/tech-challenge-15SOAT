# ADR 004 — Padrão de comunicação entre componentes

- **Status:** aceito
- **Data:** 2026-09-08

---

## Contexto

A fase 3 quebra o que era um processo único em quatro componentes com ciclos de deploy
independentes: API Gateway, Lambda de autenticação, aplicação no cluster e banco
gerenciado. É preciso decidir **como eles conversam** — e, igualmente importante, o que
deliberadamente **não** se torna comunicação entre processos.

## Decisão

### 1. Externo: REST/JSON síncrono sobre HTTPS

Toda comunicação que cruza processo é **síncrona, REST, JSON**, entrando pelo API
Gateway.

| Origem → Destino | Protocolo |
| --- | --- |
| Cliente → API Gateway | HTTPS / REST |
| API Gateway → Lambda | invoke (proxy integration) |
| API Gateway → App (EKS) | HTTPS / REST |
| Lambda → RDS | PostgreSQL wire protocol |
| App → RDS | JDBC |
| App / Lambda → New Relic | OTLP `http/protobuf` |

**Por que síncrono.** Os dois fluxos que cruzam processos — autenticar e consultar OS —
são **pergunta e resposta com o usuário esperando**. Autenticação assíncrona não faz
sentido: não há o que fazer com um token que chega depois.

### 2. Interno: chamada direta entre bounded contexts, na mesma transação

Dentro da aplicação, Atendimento chama Estoque **em processo**, pelos use cases
(`ReservarPeca`, `BaixarPeca`, `LiberarReserva`), com a composição feita no
`OrdemServicoController` — nunca dentro do agregado.

**Por que não mensageria aqui.** Aprovar um orçamento baixa estoque. As duas coisas
precisam acontecer juntas ou não acontecer. Com fila entre elas, aprovar a OS e falhar
a baixa deixaria a OS aprovada e o estoque errado, exigindo saga com compensação —
complexidade grande para resolver um problema que a transação do banco já resolve. Com
o volume real (milhares de OS/mês), não há pressão que justifique o custo.

**A porta está aberta para mudar.** `reservas.ordem_servico_id` **não tem FK** para
`ordens_servico` justamente para que Estoque possa ser extraído mais tarde sem migração
de schema ([modelagem-dados.md](../modelagem-dados.md), §4.3).

### 3. Integração entre repositórios: SSM Parameter Store, não `terraform_remote_state`

Componentes provisionados em repositórios diferentes precisam descobrir endereços uns
dos outros (endpoint do RDS, subnets, nome do cluster). Quem cria **publica** num
parâmetro nomeado; quem consome **lê**.

| Preferido a | Por quê |
| --- | --- |
| `terraform_remote_state` | Acoplaria ao layout interno do state alheio, e exigiria permissão de leitura no bucket inteiro — inclusive senhas, que ficam em texto claro no state. |
| Copiar valores à mão | É o que se faz às 23h do dia da entrega, e o que quebra silenciosamente na semana seguinte. |

Os nomes dos parâmetros estão no README de cada repositório de infraestrutura.

### 4. Notificação ao cliente: e-mail, best-effort, após o commit

O envio acontece **fora** da transação, depois do commit: não há rollback de e-mail
entregue. Falha de envio é capturada, logada e vira
`oficina_integracao_falhas_total{integracao="email"}` — nunca derruba a operação de
negócio que já aconteceu.

## Alternativas consideradas

| Alternativa | Por que não |
| --- | --- |
| **SQS/SNS entre Atendimento e Estoque** | Trocaria uma transação ACID por uma saga com compensação, sem ganho de escala perceptível neste volume. |
| **EventBridge para eventos de OS** | Interessante para integrar terceiros no futuro; hoje não há consumidor externo. Adicionar o barramento antes do consumidor é infraestrutura sem propósito. |
| **gRPC entre gateway e aplicação** | Ganho de desempenho irrelevante aqui, e perderia a compatibilidade REST/OpenAPI que o enunciado pede documentar (Swagger/Postman). |
| **Lambda chamando a aplicação em vez do banco** | Colocaria a autenticação na dependência da disponibilidade da app, e o enunciado pede que a função *"consulte a existência e o status do cliente na base de dados"*. |

## Consequências

**Positivas**

- Uma única transação garante consistência entre OS e estoque; sem código de
  compensação.
- REST/JSON mantém o OpenAPI como contrato único, que já é entregável.
- Contrato entre repositórios explícito e versionado.

**Negativas / custos assumidos**

- **Acoplamento temporal**: se o RDS estiver fora, autenticação e aplicação caem
  juntas. Aceito — sem banco não há o que responder de qualquer forma.
- Chamada síncrona propaga latência ao usuário. Mitigado pela ausência de I/O externo
  no caminho crítico (o e-mail sai depois do commit).
- Extrair Estoque para outro serviço no futuro exigirá reintroduzir a saga que hoje
  evitamos. A ausência de FK entre os contextos é o que mantém esse caminho aberto.
