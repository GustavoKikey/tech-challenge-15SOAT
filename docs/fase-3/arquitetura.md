# Documentação da arquitetura — Fase 3

> Índice do entregável *"Documentação da Arquitetura"* do Tech Challenge Fase 3.
> Atualizado em **2026-09-08**.

---

## O que o enunciado pede e onde está

| Entregável exigido | Documento |
| --- | --- |
| **Diagrama de Componentes** (visão de nuvem, APIs, banco e monitoramento) | [diagrama-componentes.md](diagrama-componentes.md) |
| **Diagrama de Sequência** — autenticação | [diagramas-sequencia.md](diagramas-sequencia.md) §1 |
| **Diagrama de Sequência** — abertura de ordens de serviço | [diagramas-sequencia.md](diagramas-sequencia.md) §2 |
| **RFC** — escolha da nuvem | [rfc/rfc-001-escolha-da-nuvem.md](rfc/rfc-001-escolha-da-nuvem.md) |
| **RFC** — escolha do banco | [rfc/rfc-002-escolha-do-banco.md](rfc/rfc-002-escolha-do-banco.md) |
| **RFC** — estratégia de autenticação | [rfc/rfc-003-estrategia-de-autenticacao.md](rfc/rfc-003-estrategia-de-autenticacao.md) |
| **ADR** — padrão de comunicação | [adr/adr-004-padrao-de-comunicacao.md](adr/adr-004-padrao-de-comunicacao.md) |
| **ADR** — uso de HPA | [adr/adr-003-uso-de-hpa.md](adr/adr-003-uso-de-hpa.md) |
| **Justificativa formal do banco + ER + relacionamentos** | [modelagem-dados.md](modelagem-dados.md) |

## RFC ou ADR?

Os dois formatos aparecem porque servem a coisas diferentes, e o enunciado pede ambos:

- **RFC** — decisão que teve alternativas reais em disputa, com trade-offs a discutir.
  Traz o que foi avaliado, por que perdeu, e o que ficou em aberto.
- **ADR** — decisão arquitetural que passa a valer como regra do projeto. Mais curto,
  mais assertivo, e citado pelo código.

## Todos os registros

### RFCs

| # | Título | Decisão |
| --- | --- | --- |
| [001](rfc/rfc-001-escolha-da-nuvem.md) | Escolha do provedor de nuvem | AWS via Learner Lab — e as três restrições que moldaram a arquitetura |
| [002](rfc/rfc-002-escolha-do-banco.md) | Escolha do banco de dados | PostgreSQL 16 no RDS |
| [003](rfc/rfc-003-estrategia-de-autenticacao.md) | Estratégia de autenticação | Dois emissores, um validador, autorização por propriedade |

### ADRs

| # | Título | Decisão |
| --- | --- | --- |
| [001](adr/adr-001-contrato-jwt-cliente.md) | Contrato do JWT de cliente | `sub` = UUID, `groups: [CLIENTE]`, 30 min |
| [002](adr/adr-002-descontinuar-rotas-publicas.md) | Rotas públicas da fase 2 | Depreciar, não remover |
| [003](adr/adr-003-uso-de-hpa.md) | Escalabilidade | HPA nativo, 2 a 5 réplicas |
| [004](adr/adr-004-padrao-de-comunicacao.md) | Padrão de comunicação | REST síncrono entre processos; transação única entre bounded contexts |

## Documentos de apoio

| Documento | Conteúdo |
| --- | --- |
| [RFC 001](rfc/rfc-001-escolha-da-nuvem.md) | Direção da fase: escolha da nuvem, restrições e riscos |
| [../ARQUITETURA.md](../ARQUITETURA.md) | Clean Architecture da aplicação (fase 2, ainda vigente) |

## Estado da arquitetura documentada

Nem tudo aqui está implementado — os documentos marcam a diferença, mas vale o resumo:

| Componente | Estado |
| --- | --- |
| Autenticação por CPF (Lambda + app) | ✅ implementado, testado, interoperabilidade verificada |
| Autorização por propriedade da OS | ✅ 7 ITs |
| Modelo de dados (`V7`) | ✅ aplicado e validado contra Postgres real |
| Observabilidade da aplicação | ✅ validada em cluster |
| API Gateway | ❌ depende da AWS |
| EKS, RDS, Terraform de nuvem | ❌ depende da AWS e da [restrição de IAM do Learner Lab](rfc/rfc-001-escolha-da-nuvem.md) |
| New Relic conectado, dashboards, alertas | ❌ depende da license key |

> ⚠️ O [diagrama de componentes](diagrama-componentes.md) mostra **EKS**. O AWS Academy
> Learner Lab não permite criar IAM roles, e o EKS exige uma role de cluster
> ([RFC 001](rfc/rfc-001-escolha-da-nuvem.md), §5). Se a role existente não servir, a
> alternativa é k3s em EC2 provisionado por Terraform — só a caixa do cluster muda.

## Escopo desta pasta

Os documentos listados acima são os **entregáveis avaliados** e estão versionados.
O material de trabalho interno da fase — planejamento, checklist de pendências e guias
operacionais — fica fora do repositório.
