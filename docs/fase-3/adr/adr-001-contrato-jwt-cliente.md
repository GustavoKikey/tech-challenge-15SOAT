# ADR 001 — Contrato do JWT de cliente

- **Status:** aceito
- **Data:** 2026-09-08
- **Contexto da fase:** Tech Challenge Fase 3 — autenticação por CPF via Function Serverless

---

## Contexto

A fase 3 exige uma Lambda que valide o CPF, consulte o cliente na base e **emita um
JWT** para consumo das APIs protegidas. A aplicação Quarkus, que hoje **emite** tokens
para usuários administrativos, passa a também **consumir** tokens emitidos por outro
serviço.

São dois componentes em repositórios diferentes, com ciclos de deploy independentes.
Sem um contrato escrito, qualquer divergência de claim (`groups` vs `roles`, `sub` com
CPF vs com id) só aparece em runtime, como 401 sem explicação.

Existe também um segundo público já em produção: os usuários administrativos
(`ATENDENTE`, `MECANICO`, `ADMINISTRADOR`), autenticados por usuário e senha em
`POST /auth/login`. Esse fluxo **não pode quebrar**.

## Decisão

### 1. Dois emissores, um validador

| | Emite | Valida |
| --- | --- | --- |
| Token administrativo | App Quarkus (`JwtTokenService`) | App Quarkus |
| Token de cliente | **Lambda** | App Quarkus |

A aplicação valida ambos com a **mesma chave pública** e o **mesmo issuer**. Um único
`mp.jwt.verify.publickey.location` continua atendendo, porque a Lambda assina com a
chave privada correspondente.

### 2. Claims do token de cliente

| Claim | Valor | Origem |
| --- | --- | --- |
| `iss` | `oficina-mvp` | mesmo issuer dos tokens administrativos |
| `sub` | UUID do cliente (`clientes.id`) | **não é o CPF** — ver justificativa |
| `upn` | UUID do cliente | exigido pelo SmallRye como principal |
| `groups` | `["CLIENTE"]` | array, formato MP-JWT |
| `cpf` | CPF com 11 dígitos, sem máscara | claim privado, para auditoria |
| `exp` | emissão + 30 min | ver justificativa |

**Por que `sub` é o id e não o CPF:** o `sub` identifica o principal e aparece em logs
e traces. CPF é dado pessoal (LGPD) e não deve circular como identificador primário.
O id é estável, opaco e já é a chave da tabela. O CPF vai num claim próprio, para
quando a auditoria realmente precisar dele.

**Por que 30 minutos e não 8 horas:** o token administrativo dura 8h porque um
atendente fica logado o turno inteiro num terminal controlado. O token de cliente
circula em contexto não confiável (celular, link de e-mail) e dá acesso a decidir
orçamento. Janela curta reduz a exposição; renovar é uma chamada barata à Lambda.

### 3. `CLIENTE` não entra no enum `Role`

O enum `seguranca.entities.Role` modela **usuários da oficina**, que têm registro na
tabela `usuarios`, senha com hash e ciclo de vida próprio. Um cliente não é um usuário:
não tem senha, não é cadastrado por um administrador, e vive em outro bounded context
(`atendimento`).

`CLIENTE` é, portanto, apenas um valor de `groups` no token — usado em
`@RolesAllowed("CLIENTE")` — e não um membro do enum. Misturar os dois acoplaria o BC
Segurança ao BC Atendimento sem necessidade.

### 4. Autorização não termina no token

Ter um token de cliente válido prova **quem** é o portador, não **a que** ele tem
direito. Toda rota de cliente compara o `sub` do token com o dono do recurso e devolve
**403** quando divergem.

Sem isso a proteção seria cosmética: hoje qualquer pessoa com o UUID de uma OS aprova
o orçamento dela em `POST /publico/ordens-servico/{id}/orcamento/decisao`
(`@PermitAll`). Trocar "qualquer um com o UUID" por "qualquer cliente logado" não
corrigiria nada.

### 5. Cliente inativo não autentica

A Lambda recusa a emissão quando `clientes.ativo = false` (coluna criada em
`V7__cliente_status.sql`). A verificação é **na emissão**, não na validação: a
aplicação confia no token porque ele é curto — 30 min é o tempo máximo que uma
desativação leva para surtir efeito, o que é aceitável e evita consultar a base a
cada requisição.

## Consequências

**Positivas**

- Contrato explícito e versionado: a Lambda pode ser escrita em qualquer linguagem.
- Nenhuma mudança no fluxo administrativo — `JwtTokenService` segue como está.
- CPF fora do identificador primário reduz exposição de dado pessoal.

**Negativas / custos assumidos**

- Chave privada precisa existir em dois lugares (app, para tokens administrativos; e
  Lambda, para tokens de cliente). Mitigação: rotacionar a chave atual, que está
  versionada no repositório, e distribuir por Secrets Manager.
- Revogação não é imediata — limitada pela janela de 30 min.
- Token curto exige que o cliente reautentique com mais frequência.

**Alternativas descartadas**

| Alternativa | Por que não |
| --- | --- |
| CPF no `sub` | Espalha dado pessoal por logs e traces |
| Cognito como emissor | O enunciado pede explicitamente que a **Lambda** gere o token |
| Chaves separadas por emissor | Exigiria JWKS e dois issuers; complexidade sem ganho no escopo |
| `CLIENTE` no enum `Role` | Acopla BC Segurança a BC Atendimento |

## Referências

- [RFC 003](../rfc/rfc-003-estrategia-de-autenticacao.md) — a estratégia de autenticação completa
- `V7__cliente_status.sql` — coluna `ativo`
- `external/security/JwtTokenService.java` — emissor administrativo
