# Diagramas de sequência — autenticação e abertura de OS

> Entregável da fase 3: *"Diagrama de Sequência para o fluxo de autenticação e
> abertura de ordens de serviço"*. Atualizado em **2026-09-08**.
>
> Os fluxos abaixo descrevem código que existe e foi verificado ponta a ponta —
> não intenção de projeto. As exceções são os componentes de nuvem (API Gateway,
> Lambda em execução na AWS), marcados onde aparecem.

---

## 1. Autenticação do cliente por CPF

O fluxo que a fase 3 introduz. O cliente não tem senha: prova identidade pelo CPF, e
a Function Serverless decide se emite o token.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant L as Lambda auth<br/>(Node.js)
    participant DB as RDS PostgreSQL
    participant APP as App Quarkus<br/>(EKS)

    C->>GW: POST /auth/cliente<br/>{ "cpf": "529.982.247-25" }
    GW->>L: invoke (proxy integration)

    Note over L: 1. valida o CPF<br/>formato + dígitos verificadores
    alt CPF inválido
        L-->>GW: 400 { erro: "CPF inválido." }
        GW-->>C: 400
    end

    Note over L: 2. normaliza para 11 dígitos<br/>(mesma regra de Documento.java)
    L->>DB: SELECT id, nome, ativo<br/>FROM clientes WHERE documento = $1
    DB-->>L: linha ou vazio

    alt cliente inexistente OU ativo = false
        Note over L: resposta idêntica nos dois casos —<br/>distinguir viraria verificador de cadastro
        L-->>GW: 401 { erro: "Cliente não encontrado ou inativo." }
        GW-->>C: 401
    end

    Note over L: 3. assina JWT RS256 (ADR 001)<br/>sub = UUID · groups = [CLIENTE]<br/>cpf · exp = +30 min
    L-->>GW: 200 { accessToken, expiresIn: 1800, cliente }
    GW-->>C: 200

    Note over C,APP: A partir daqui o token vale para as APIs protegidas

    C->>GW: GET /cliente/ordens-servico<br/>Authorization: Bearer <token>
    GW->>APP: encaminha
    Note over APP: valida assinatura com a CHAVE PÚBLICA<br/>confere issuer e expiração
    APP->>DB: SELECT ... WHERE cliente_id = sub
    DB-->>APP: OS do cliente
    APP-->>GW: 200 [ ... ]
    GW-->>C: 200
```

**Pontos que o diagrama não mostra e importam:**

- A Lambda **assina** com a chave privada; a aplicação apenas **valida** com a
  pública. São processos diferentes, em repositórios diferentes, unidos só pelo
  contrato do [ADR 001](adr/adr-001-contrato-jwt-cliente.md).
- A aplicação **não consulta o banco** para validar o token. A verificação é
  criptográfica e local. É por isso que o token dura 30 min: essa é a janela máxima
  entre desativar um cliente e o acesso dele efetivamente cessar.
- A Lambda roda **dentro da VPC**, sem saída para a internet. Endpoint e credenciais
  chegam como variáveis de ambiente, injetadas pelo Terraform no `apply`
  ([ADR 004](adr/adr-004-padrao-de-comunicacao.md), §3).

### Autorização: por que autenticar não basta

```mermaid
sequenceDiagram
    autonumber
    actor B as Bruno<br/>(cliente autenticado)
    participant APP as App Quarkus
    participant DB as RDS

    B->>APP: POST /cliente/ordens-servico/{OS-da-Alice}/orcamento/decisao<br/>Bearer <token válido do Bruno>
    Note over APP: token OK: assinatura válida, role CLIENTE
    APP->>DB: SELECT * FROM ordens_servico WHERE id = {OS-da-Alice}
    DB-->>APP: OS (cliente_id = Alice)
    Note over APP: sub do token (Bruno) ≠ os.clienteId (Alice)
    APP-->>B: 403 Forbidden

    Note over B,DB: Nenhuma escrita aconteceu.<br/>A checagem roda DENTRO da transação da decisão.
```

Antes da fase 3, essa mesma operação era `@PermitAll` em
`/publico/ordens-servico/{id}/orcamento/decisao`: qualquer pessoa com o UUID da OS
aprovava o orçamento. Exigir apenas login trocaria *"qualquer um com o UUID"* por
*"qualquer cliente logado"* — por isso a comparação de propriedade.

Coberto por `AreaClienteResourceIT`.

---

## 2. Abertura de ordem de serviço

Fluxo do atendente, com o cruzamento entre os bounded contexts Atendimento e Estoque.

```mermaid
sequenceDiagram
    autonumber
    actor A as Atendente
    participant R as OrdemServicoResource<br/>(external/api)
    participant CT as OrdemServicoController
    participant TX as ExecutorTransacional
    participant UC as AbrirOrdemServicoUseCase
    participant G as Gateways<br/>(Cliente/Veículo/OS)
    participant DB as RDS PostgreSQL
    participant M as MetricasGateway
    participant N as NotificacaoGateway

    A->>R: POST /ordens-servico<br/>{ cliente, veiculo, servicos, pecas }
    Note over R: JWT administrativo validado<br/>(@RolesAllowed)
    R->>CT: abrir(request)

    CT->>TX: emTransacao( ... )
    activate TX

    TX->>UC: executar(input)
    UC->>G: buscar/criar cliente por documento
    G->>DB: SELECT / INSERT clientes
    UC->>G: buscar/criar veículo por placa
    G->>DB: SELECT / INSERT veiculos
    Note over UC: OrdemServico.abrir(clienteId, veiculoId)<br/>nasce com status RECEBIDA
    UC->>G: salvar OS + itens
    G->>DB: INSERT ordens_servico, os_itens_*
    UC-->>TX: OrdemServico

    TX-->>CT: commit
    deactivate TX

    Note over CT,N: Só APÓS o commit — efeitos externos não<br/>podem acontecer numa transação que pode abortar
    CT->>N: notificarMudancaStatus(os, cliente)
    Note over N: e-mail best-effort —<br/>falha vira métrica, não erro HTTP
    CT->>M: ordemServicoAberta()
    Note over M: oficina_os_abertas_total++<br/>com exemplar de trace_id

    CT-->>R: AberturaOrdemServicoResponse
    R-->>A: 201 { id, status: "RECEBIDA", criadaEm }
```

### Ciclo de vida completo, com estoque e telemetria

```mermaid
sequenceDiagram
    autonumber
    actor A as Atendente
    participant CT as OrdemServicoController
    participant OS as Agregado<br/>OrdemServico
    participant E as Estoque<br/>(Reservar/Baixar/Liberar)
    participant M as MetricasGateway

    A->>CT: POST /{id}/diagnostico
    CT->>OS: iniciarDiagnostico()
    Note over OS: RECEBIDA → EM_DIAGNOSTICO<br/>marca diagnosticoIniciadoEm

    A->>CT: POST /{id}/orcamento
    CT->>E: ReservarPecaUseCase
    Note over E: reserva ATIVA<br/>saldo disponível cai, total intacto

    A->>CT: POST /{id}/orcamento/enviar
    CT->>OS: enviarOrcamento()
    Note over OS: → AGUARDANDO_APROVACAO

    alt Cliente aprova (área autenticada por CPF)
        CT->>E: BaixarPecaUseCase
        Note over E: reserva BAIXADA · quantidade_total cai
        CT->>OS: aprovarOrcamento()
        Note over OS: → EM_EXECUCAO<br/>marca execucaoIniciadaEm
        CT->>M: faseConcluida("Diagnóstico", duração)
    else Cliente recusa
        CT->>E: LiberarReservaUseCase
        Note over E: reserva CANCELADA · saldo volta
        CT->>OS: recusarOrcamento()
        Note over OS: → CANCELADA
    end

    A->>CT: POST /{id}/finalizar
    CT->>M: faseConcluida("Execução", duração)
    A->>CT: POST /{id}/entregar
    CT->>M: faseConcluida("Finalizada", duração)
```

**Decisões visíveis no diagrama:**

- **Transação no Controller, não no Use Case.** Aprovar orçamento chama a baixa de
  estoque; recusar chama a liberação. As duas cruzam bounded contexts e precisam ser
  atômicas. Centralizar a demarcação no controller mantém o use case como orquestração
  pura de domínio (`ARQUITETURA.md`, §5).
- **Notificação e métrica após o commit.** E-mail não pode ser enviado dentro de uma
  transação que ainda pode abortar — não há rollback de e-mail entregue.
- **A duração de cada fase sai dos timestamps do próprio agregado**, não de um
  cronômetro paralelo. Cada transição fecha exatamente uma fase, então não há risco de
  contar a mesma duração duas vezes.
- **Falha em qualquer transição incrementa `oficina_os_falhas_total{operacao}`**, que
  alimenta o alerta de "falhas no processamento de ordens de serviço".

---

## 3. Onde cada fluxo está coberto por teste

| Fluxo | Prova |
| --- | --- |
| Autenticação por CPF (validação, status, emissão) | `lambda-auth/test/handler.test.js` — 14 casos |
| Token da Lambda aceito pela aplicação | Verificado no cluster: token Node → `200` na API Java |
| Autorização por propriedade (403) | `AreaClienteResourceIT` — 7 ITs |
| Abertura de OS e ciclo completo | `OrdemServicoResourceIT` |
| Métricas do ciclo de vida | `ObservabilidadeIT` — lê `/q/metrics` da app no ar |
