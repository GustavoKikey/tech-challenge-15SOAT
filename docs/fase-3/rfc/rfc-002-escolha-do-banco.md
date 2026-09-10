# RFC 002 — Escolha do banco de dados

| | |
| --- | --- |
| **Status** | Aceito |
| **Autor** | Gustavo Kikey |
| **Data** | 2026-09-08 |
| **Decisão** | PostgreSQL 16 gerenciado, via Amazon RDS |

> Este RFC trata da **decisão** e dos trade-offs. O schema, o diagrama ER e a
> explicação dos relacionamentos estão em [modelagem-dados.md](../modelagem-dados.md).

---

## 1. Contexto

O enunciado exige *"Banco de Dados Gerenciado (PostgreSQL, MySQL, SQL Server, etc.)"* e
*"melhorar e documentar a modelagem do banco de dados, garantindo consistência e
performance"*.

A fase 2 usava PostgreSQL 16 em StatefulSet dentro do cluster kind, provisionado pelo
próprio Terraform. Isso satisfazia "banco", não "gerenciado": backup, patching e
disponibilidade eram responsabilidade nossa.

## 2. Proposta

Manter **PostgreSQL**, migrando para **Amazon RDS**. Sem mudança de motor, sem
reescrita de schema: as sete migrations Flyway aplicam iguais.

## 3. Duas perguntas, em ordem

### 3.1 O domínio pede um banco relacional?

| Característica | Implicação |
| --- | --- |
| Aprovar orçamento **baixa estoque**; recusar **libera reserva**. As duas operações cruzam bounded contexts e precisam ser atômicas. | ACID com transação multi-tabela é requisito, não conforto. |
| Cliente → Veículo → OS → Itens → Peça → Reserva formam um grafo consultado por vários caminhos. | Integridade referencial no banco; junções sem duplicar dados. |
| Schema estável — o conceito de "ordem de serviço" não muda a cada sprint. | Rigidez de schema vira vantagem: erro de forma aparece no `INSERT`. |
| Milhares de OS por mês, não bilhões de eventos. | Sem pressão para trocar consistência por escala horizontal. |

**Alternativas não relacionais avaliadas:**

- **DynamoDB** — modelar um grafo consultado por cliente, por veículo, por status e por
  período exigiria duplicar dados em vários índices e reimplementar na aplicação a
  integridade que o relacional dá pronta. Transação entre itens existe, mas é limitada
  e mais cara de raciocinar.
- **MongoDB** — OS e Peça são agregados distintos que se cruzam: a mesma peça é
  reservada por várias OS. Como documento, ou se duplica a peça em cada OS (e o saldo
  diverge), ou se referencia entre coleções (e se perde a transação).

### 3.2 Qual banco relacional?

| Opção | Avaliação |
| --- | --- |
| **PostgreSQL** ✅ | `NUMERIC` decimal exato para dinheiro; `TIMESTAMPTZ` com instante absoluto (múltiplas unidades, fusos diferentes); `UUID` nativo; `CHECK` constraints; **índice parcial**, usado em `V7` para indexar só clientes ativos. |
| **MySQL** | Atenderia. Perde em: sem índice parcial; `CHECK` só passou a ser aplicado no 8.0.16; `UUID` sem tipo nativo. |
| **SQL Server** | Tecnicamente capaz, custo de licença no RDS bem superior, sem ganho para este domínio. |
| **Aurora Serverless v2** | Escala automática elegante, mas custo mínimo maior e complexidade desnecessária para o volume real. Vale reconsiderar se o volume crescer uma ordem de grandeza. |

**Custo de mudança = zero.** O código já é PostgreSQL: sete migrations, `pgcrypto`,
tipos nativos, e 54 testes de integração rodando contra Postgres real via
Testcontainers. Trocar de motor jogaria fora essa cobertura sem nenhum ganho.

## 4. Configuração proposta

| Parâmetro | Valor | Motivo |
| --- | --- | --- |
| Engine | PostgreSQL 16 | Mesma versão dos testes e da fase 2 |
| Instância | `db.t3.micro` | Free tier; suficiente para o volume real |
| Armazenamento | 20 GB gp3 | Mínimo do free tier |
| `publicly_accessible` | **false** | Só a VPC alcança |
| Security Group | 5432 apenas dos SGs da Lambda e do cluster | Menor superfície possível |
| Backup | 7 dias | Padrão do RDS |
| Multi-AZ | **não** | Dobraria o custo; fora do orçamento de US$ 50 do Learner Lab |
| Credenciais | Secrets Manager | Nunca no state nem no repositório |

**Multi-AZ desligado é um débito conhecido.** O enunciado pede alta disponibilidade;
a aplicação a tem (HPA, múltiplas réplicas, PDB), o banco não. Numa operação real com
orçamento, Multi-AZ seria obrigatório. Aqui, é restrição de crédito, declarada.

## 5. Consistência e performance

**Consistência** — garantida por constraint, não por código: `UNIQUE` em
`clientes.documento` e `veiculos.placa` (duas requisições simultâneas são barradas pelo
banco, e a aplicação traduz para `409`); `CHECK` em status e quantidades; `FK` com
`ON DELETE CASCADE` nos itens.

**Performance** — doze índices cobrindo os caminhos reais de consulta, incluindo o
parcial de `V7`. Detalhamento na §6 de [modelagem-dados.md](../modelagem-dados.md).

**Saldo de peça é calculado, não armazenado** (`quantidade_total` menos reservas
`ATIVA`). Um contador materializado divergiria do estado real na primeira falha
parcial.

## 6. Impactos da migração

| Item | Impacto |
| --- | --- |
| Schema e migrations | Nenhum — aplicam iguais |
| `DB_URL` no ConfigMap | Deixa de ser fixo; passa a vir do SSM no deploy |
| Lambda | Ganha acesso direto ao RDS pela VPC |
| Testes | Nenhum — Testcontainers continua subindo Postgres local |
| Latência | Aumenta: sai de `localhost` no cluster para RDS na mesma VPC. Milissegundos, aceitável |

## 7. Questões em aberto

- **`db.t3.micro` aguenta a demonstração de HPA?** Cinco réplicas abrindo conexões
  simultâneas contra a menor instância. Se aparecer saturação, subir para
  `db.t3.small` ou colocar RDS Proxy — mas só com evidência, não por precaução.
- **Retenção de backup** de 7 dias é suficiente para o escopo acadêmico; revisar se
  o projeto continuar após a entrega.

## 8. Referências

- [modelagem-dados.md](../modelagem-dados.md) — ER, relacionamentos, índices
- [RFC 001](rfc-001-escolha-da-nuvem.md) — provedor e restrições de crédito
