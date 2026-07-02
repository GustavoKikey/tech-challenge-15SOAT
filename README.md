# Oficina MVP — Tech Challenge Fase 1 (15SOAT / FIAP)

Sistema de gestão de uma oficina mecânica: cadastro de clientes, veículos e serviços; controle de estoque de peças; e fluxo de Ordem de Serviço (orçamento → execução → entrega) com indicador de tempo médio de execução.

---

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Quarkus 3.15.x |
| Build | Maven |
| Banco de dados | PostgreSQL 16 |
| Migrations | Flyway |
| Persistência | Hibernate ORM + Panache |
| API Docs | OpenAPI / Swagger UI (SmallRye) |
| Auth | JWT (SmallRye JWT)|
| Testes | JUnit 5 + Mockito + REST-Assured + Testcontainers |
| Cobertura | JaCoCo (≥ 80% em `domain` e `application`) |
| Análise estática | SonarQube |
| Containers | Docker + docker-compose |

---

## Por que Postgres?

- **Relacional maduro com ACID forte** — essencial para os fluxos de orçamento e baixa de estoque, que precisam de consistência transacional (uma reserva de peça que falha não pode deixar saldo "corrompido").
- **Open source, sem custo de licença** e fácil de containerizar.

---

## Arquitetura — Clean Architecture (canônica, por bounded context)

```
external (api/persistence/security/config)   → Frameworks & Drivers (Quarkus, JPA, JWT)
   ↓
controllers · presenters · gateways · dtos   → Interface Adapters (por BC)
   ↓
usecases                                      → Application (1 classe por caso de uso)
   ↓
entities                                      → Enterprise (agregados, VOs, regras puras)
```

A dependência aponta **sempre para dentro**. `entities` e `usecases` **não** importam
Quarkus, JPA, Jackson nem `jakarta.transaction` — verificado por build (grep + gate JaCoCo).
Detalhe completo, diagramas e o de→para da refatoração em **[docs/ARQUITETURA.md](docs/ARQUITETURA.md)**.

### Bounded Contexts

- **Atendimento** (Customer/Downstream) — Cliente, Veículo, Serviço, Ordem de Serviço.
- **Estoque** (Supplier/Upstream) — Peça, Saldo, Reserva, Baixa.
- Relação **Customer-Supplier**: Atendimento consome Estoque para reservar/baixar peças durante a Ordem de Serviço.

---

## Como rodar localmente (modo dev)

Pré-requisitos: Docker, JDK 21 e Maven 3.9+.

```bash
# 1) Sobe só o Postgres em container
docker compose up -d postgres

# 2) Roda a app em modo dev (hot reload)
./mvnw quarkus:dev
# (ou: mvn quarkus:dev)
```

A app sobe em `http://localhost:8080`. Endpoints úteis:

- `GET http://localhost:8080/health` → `{"status":"UP"}`
- `http://localhost:8080/swagger` → Swagger UI
- `http://localhost:8080/openapi` → especificação OpenAPI (JSON/YAML)
- `http://localhost:8080/q/health` → health check do SmallRye

---

## Como rodar tudo via Docker

```bash
docker compose up --build
```

Isso sobe o Postgres + app, com Flyway aplicando as migrations no startup.
Verifique:

```bash
curl http://localhost:8080/health
# {"status":"UP"}
```

Para subir também o **SonarQube**:

```bash
docker compose --profile tools up -d
# Sonar disponível em http://localhost:9000  (login default: admin / admin123)
```

---

## Build e testes

```bash
# Build sem rodar JaCoCo:
mvn -B verify -Djacoco.skip=true

# Build com cobertura:
mvn -B verify
```

Relatório JaCoCo em `target/site/jacoco/index.html`.

---

## Endpoints principais

> _Atualizado conforme o projeto evolui._

| Recurso | Método | Path | Roles |
| --- | --- | --- | --- |
| Health | GET | `/health` | público |
| Swagger UI | GET | `/swagger` | público |
| Painel de demonstração | GET | `/` | público |
| Login JWT | POST | `/auth/login` | público |
| Cadastrar usuário | POST | `/auth/usuarios` | ADMINISTRADOR |
| Clientes | CRUD | `/clientes` | ATENDENTE/ADMIN (escrita) · todos (leitura) |
| Veículos | CRUD | `/veiculos` | ATENDENTE/ADMIN (escrita) · todos (leitura) |
| Serviços | CRUD | `/servicos` | ADMINISTRADOR |
| Peças / Estoque | CRUD + saldo | `/pecas` | ADMINISTRADOR |
| Ordens de Serviço | Abertura + fluxo | `/ordens-servico` | varia por endpoint|
| Status da OS | GET | `/ordens-servico/{id}/status` | autenticado |
| Consulta pública | GET | `/publico/ordens-servico/{id}` | **público** |
| Status público da OS | GET | `/publico/ordens-servico/{id}/status` | **público** |
| Decisão do orçamento (webhook) | POST | `/publico/ordens-servico/{id}/orcamento/decisao` | **público** |
| Tempo médio | GET | `/relatorios/tempo-medio-execucao` | ADMINISTRADOR |

---

## Como autenticar

Todos os endpoints administrativos exigem **JWT Bearer**. O token é emitido pelo `POST /auth/login` e válido por 8 horas (configurável em `oficina.jwt.expiration`).

### 1) Faz login com o admin inicial

O usuário `admin` é provisionado automaticamente no startup pelo `AdminBootstrap`, com a senha definida em `ADMIN_PASSWORD` (default `admin123` em dev — **rotacionar em produção**).

```bash
curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "expiresIn": 28800,
  "role": "ADMINISTRADOR"
}
```

### 2) Usa o token nos endpoints

```bash
TOKEN="eyJhbGciOiJSUzI1NiJ9..."
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/clientes
```

No **Swagger UI** (`/swagger`), clique em **Authorize**, cole apenas o token (sem o prefixo `Bearer`) e dispare as requisições.

### 3) Cadastra novos usuários (somente ADMINISTRADOR)

```bash
curl -X POST http://localhost:8080/auth/usuarios \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"username":"mecanico1","password":"senha-segura","role":"MECANICO"}'
```

### Chaves RSA de assinatura

Em `src/main/resources/` há um par de chaves **só para dev** (`privateKey.pem` / `publicKey.pem`). Em produção, monte os arquivos via secret manager ou aponte `mp.jwt.verify.publickey.location` / `smallrye.jwt.sign.key.location` para caminhos absolutos.

---

## Estrutura de pastas

```
src/main/java/br/com/fiap/techchallenge/oficina
├── atendimento/             # bounded context (mesmo padrão em estoque/, seguranca/, relatorio/)
│   ├── entities/            # agregados + VOs + exceções    (framework-free)
│   ├── usecases/            # 1 classe por caso de uso       (framework-free)
│   ├── gateways/            # ports (ex-*Repository → *Gateway)
│   ├── controllers/         # orquestram use cases + transação + presenter
│   ├── presenters/          # domínio → *Response DTO
│   └── dtos/                # Request/Response (records)
├── shared/
│   ├── entities/            # Documento, Placa, Dinheiro, DomainException
│   └── usecases/            # ExecutorTransacional (porta de transação)
└── external/                # Frameworks & Drivers
    ├── api/                 # *Resource (JAX-RS) + exception mappers
    ├── persistence/         # JPA/Panache, mappers, *GatewayImpl, ExecutorTransacionalJta
    ├── security/            # JWT, BCrypt, AdminBootstrap
    └── config/              # composition root (CDI @Produces) + OpenAPI

src/main/resources
├── application.properties
├── privateKey.pem            # chaves RSA dev — em produção, montar via secret
├── publicKey.pem
└── db/migration/V*.sql       # Flyway
```

---

## Linguagem ubíqua

Termos do domínio usados de forma consistente em código, testes e documentação. Detalhamento completo em (Glossário).

| Termo | Significado no contexto da oficina |
| --- | --- |
| **Ordem de Serviço (OS)** | Documento central que registra cliente, veículo, problema, diagnóstico, serviços executados e peças utilizadas. |
| **Diagnóstico** | Ação do mecânico para identificar o problema do veículo antes de gerar o orçamento. |
| **Peça / Insumo** | Item físico mantido em estoque, consumido durante a execução de um serviço. |
| **Cliente** | Pessoa física ou jurídica (CPF/CNPJ) dona do veículo e quem aprova o orçamento. |
| **Veículo** | Automóvel atrelado a um cliente, identificado unicamente pela placa. |
| **Serviço** | Mão de obra executada pelo mecânico (ex.: troca de óleo, alinhamento). |
| **Orçamento** | Cálculo automático somando serviços + peças, requer validação do cliente. |
| **Status da OS** | Recebida → Em diagnóstico → Aguardando aprovação → Em execução → Finalizada → Entregue. |
| **Reserva** | Comprometimento temporário de uma peça com uma OS, antes da baixa. |
| **Baixa de estoque** | Redução efetiva do saldo, acionada pela aprovação do orçamento. |
| **Saldo disponível** | Saldo total da peça menos as reservas ativas. |
| **Atendente** | Funcionário que abre a OS no recebimento e realiza a entrega ao cliente. |
| **Mecânico** | Profissional que executa diagnóstico, insere itens na OS e finaliza os reparos. |
| **Administrador** | Responsável pelos cadastros e atualização do saldo do estoque. |
| **Token** | Credencial JWT emitida após autenticação, usada para acessar APIs administrativas. |

---

## Como rodar testes e gerar cobertura

```bash
# Roda testes unitários + cobertura JaCoCo (regra ≥80% nos pacotes críticos)
mvn -B clean verify

# Relatório HTML
open target/site/jacoco/index.html      # macOS
start target\site\jacoco\index.html     # Windows
xdg-open target/site/jacoco/index.html  # Linux
```

A regra do JaCoCo (`<rule>`) **falha o build** se a cobertura média de instruções ou branches em `domain.*` ou `application.*` cair abaixo de 80%.

---

## Como rodar análise SonarQube

```bash
# 1) Sobe Sonar local (uma vez)
docker compose --profile tools up -d sonarqube sonar-postgres
# Aguarda http://localhost:9000 ficar disponível (admin / admin → trocar senha)
# Cria projeto "oficina-mvp" e gera token

# 2) Roda análise apontando para o Sonar local
mvn -B clean verify sonar:sonar \
  -Dsonar.projectKey=oficina-mvp \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```
---

## Limitações conhecidas

Decisões conscientes para manter o MVP enxuto:

- **Sem CORS habilitado** — o backend é consumido por um cliente confiável (Swagger UI no MVP). Habilitar quando o front-end web entrar em escopo.
- **Decisão do orçamento por webhook público sem assinatura** — a fase 2 introduziu `POST /publico/ordens-servico/{id}/orcamento/decisao` para receber a aprovação/recusa externa do cliente (o UUID da OS funciona como capability token). Evolução futura: link único assinado / OTP / verificação de origem. A rota interna `/orcamento/aprovar|recusar` (ATENDENTE/ADMIN) segue disponível para registro presencial.
- **Chaves RSA do JWT versionadas** — `privateKey.pem`/`publicKey.pem` em `src/main/resources/` são **só para dev**. Em produção, montar via secret manager (env `MP_JWT_VERIFY_PUBLICKEY_LOCATION` / `SMALLRYE_JWT_SIGN_KEY_LOCATION`).
- **Senha admin default** — `admin123` no profile `dev`. Em produção, sobrescrever via env `ADMIN_PASSWORD`.
