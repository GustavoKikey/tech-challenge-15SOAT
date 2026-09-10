# RFC 003 — Estratégia de autenticação

| | |
| --- | --- |
| **Status** | Aceito — implementado e verificado em 2026-09-08 |
| **Autor** | Gustavo Kikey |
| **Data** | 2026-09-08 |
| **Decisão** | Dois emissores (app e Lambda), um validador, autorização por propriedade do recurso |

---

## 1. Contexto

O enunciado exige:

> Proteger rotas sensíveis da aplicação com autenticação via CPF.
> Criar uma Function Serverless para validar o CPF do cliente, consultar a existência e
> o status do cliente na base de dados, e gerar e devolver um token (JWT) válido.

Duas condições herdadas da fase 2 complicam isso:

**Já existe autenticação.** Usuários administrativos (`ATENDENTE`, `MECANICO`,
`ADMINISTRADOR`) fazem login com usuário e senha em `POST /auth/login` e recebem um JWT
RS256 emitido pela própria aplicação. Esse fluxo não pode quebrar.

**Existe um furo aberto.** `POST /publico/ordens-servico/{id}/orcamento/decisao` é
`@PermitAll`. Qualquer portador do UUID de uma OS **aprova o orçamento dela** — e UUID
de OS circula em link de e-mail. Essa é a "rota sensível" que o requisito descreve.

## 2. Proposta

### 2.1 Dois emissores, um validador

| | Emite | Valida |
| --- | --- | --- |
| Token administrativo | App Quarkus (`JwtTokenService`) | App Quarkus |
| Token de cliente | **Lambda** | App Quarkus |

Mesmo par de chaves RSA e mesmo issuer. A aplicação valida os dois com um único
`mp.jwt.verify.publickey.location`; o que distingue os públicos é o claim `groups`.

A aplicação **não consulta o banco** para validar token — a verificação é
criptográfica e local.

### 2.2 Contrato do token de cliente

| Claim | Valor |
| --- | --- |
| `iss` | `oficina-mvp` |
| `sub` / `upn` | **UUID do cliente** |
| `groups` | `["CLIENTE"]` |
| `cpf` | 11 dígitos, sem máscara |
| `exp` | emissão + **30 min** |

Contrato completo e justificativas: [ADR 001](../adr/adr-001-contrato-jwt-cliente.md).

**`sub` é o UUID, não o CPF.** O subject aparece em logs e traces; CPF é dado pessoal.
O id é estável, opaco e já é a chave da tabela.

**30 min, não 8h.** O token administrativo dura o turno porque vive num terminal
controlado. O token de cliente circula em celular e link de e-mail, e dá acesso a
decidir orçamento. É também a janela máxima entre desativar um cliente e o acesso
cessar — consequência de não consultar o banco a cada requisição.

### 2.3 Autenticar não basta: autorização por propriedade

Toda rota de cliente compara o `sub` do token com o dono do recurso:

```
Bruno (token válido) → GET  /cliente/ordens-servico/{OS-da-Alice}       → 403
Bruno (token válido) → POST /cliente/.../{OS-da-Alice}/orcamento/decisao → 403
Alice                → POST /cliente/.../{OS-dela}/orcamento/decisao     → 200
```

Sem isso, a fase 3 apenas trocaria *"qualquer um com o UUID"* por *"qualquer cliente
logado"* — não seria correção nenhuma. O id do cliente vem do `sub`, **nunca** de
parâmetro da requisição.

### 2.4 Cliente inativo não autentica

A Lambda recusa emissão quando `clientes.ativo = false` (coluna criada em `V7`).
Inexistente e inativo devolvem **resposta idêntica**: distinguir transformaria o
endpoint num verificador de cadastro, permitindo descobrir quais CPFs são clientes.

## 3. Alternativas avaliadas

| Alternativa | Por que não |
| --- | --- |
| **Amazon Cognito como emissor** | O enunciado pede explicitamente que a **Lambda** gere o token. Cognito também traria um modelo de usuário próprio, duplicando a identidade que já vive em `clientes`. |
| **CPF + senha** | O enunciado pede autenticação *via CPF*. Adicionar senha criaria cadastro, recuperação e política de senha — escopo não pedido. |
| **Token opaco com introspecção** | Revogação imediata, mas exige consulta a cada requisição: acopla toda chamada ao banco e adiciona latência. JWT curto resolve o mesmo problema com folga aceitável. |
| **Chaves separadas por emissor (JWKS)** | Isolaria comprometimento de chave, mas exigiria endpoint JWKS e dois issuers. Complexidade sem ganho no escopo. |
| **`CLIENTE` dentro do enum `Role`** | Cliente não é usuário da oficina: não tem senha, nem registro em `usuarios`, e vive em outro bounded context. Acoplaria Segurança a Atendimento. |
| **Remover as rotas `/publico/*`** | Quebraria o contrato entregue na fase 2. Depreciadas e protegidas por API key no gateway — [ADR 002](../adr/adr-002-descontinuar-rotas-publicas.md). |

## 4. Implementação e verificação

| Componente | Onde |
| --- | --- |
| Validação de CPF | `lambda-auth/src/cpf.js` — espelha `Documento.java`, com teste de equivalência |
| Consulta de existência e status | `lambda-auth/src/cliente-repo.js` |
| Emissão do JWT | `lambda-auth/src/token.js` |
| Rotas protegidas | `external/api/AreaClienteResource.java` |
| Verificação de propriedade | `OrdemServicoController.buscarDoCliente(...)`, dentro da transação |

**Verificado em 2026-09-08**, com a aplicação rodando em Kubernetes:

```
GET /cliente/ordens-servico  + token emitido pela Lambda  → 200
GET /cliente/ordens-servico  sem token                    → 401
GET /cliente/ordens-servico  token adulterado             → 401
```

Cobertura: 14 testes na Lambda, 7 ITs de isolamento na aplicação, 238 unitários + 54
ITs no total.

## 5. Riscos e débitos

| Item | Situação |
| --- | --- |
| **Chave privada versionada** em `src/main/resources/privateKey.pem` | Deve ser considerada **comprometida** (esteve em repositório). Rotacionar e mover para Secrets Manager antes da entrega. |
| Revogação não é imediata | Limitada à janela de 30 min. Aceito conscientemente. |
| Chave privada em dois lugares | App (tokens administrativos) e Lambda (tokens de cliente). Mitigação: distribuição por Secrets Manager. |
| Segredo em variável de ambiente da Lambda | Visível a quem tem acesso ao console. A alternativa (Secrets Manager em runtime) exigiria NAT ou VPC endpoint pago — [ADR 004](../adr/adr-004-padrao-de-comunicacao.md), §3. |

## 6. Questões em aberto

- **Renovação do token.** Hoje o cliente reautentica com o CPF quando expira. Refresh
  token traria complexidade de armazenamento e revogação; avaliar só se a fricção de
  30 min se mostrar real.
- **Rate limiting no `/auth/cliente`.** Sem limite, o endpoint aceita força bruta de
  CPFs. Resolver no API Gateway (throttling por IP) no repositório 1.
