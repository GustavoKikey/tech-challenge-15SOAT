# ADR 002 — Descontinuar `/publico/ordens-servico` sem removê-lo

- **Status:** aceito
- **Data:** 2026-09-08
- **Relacionado:** [ADR 001](adr-001-contrato-jwt-cliente.md)

---

## Contexto

A fase 2 entregou três endpoints sem autenticação em `/publico/ordens-servico`, para
atender ao requisito *"endpoint para receber notificações externas de aprovação ou
recusa do orçamento"*:

| Endpoint | Efeito |
| --- | --- |
| `GET /publico/ordens-servico/{id}` | Lê dados da OS |
| `GET /publico/ordens-servico/{id}/status` | Lê o status |
| `POST /publico/ordens-servico/{id}/orcamento/decisao` | **Aprova ou recusa o orçamento** |

O terceiro muda estado de negócio e libera baixa de estoque. Estando `@PermitAll`,
qualquer portador do UUID da OS o executa — e UUID de OS circula em link de e-mail.

A fase 3 exige *"proteger rotas sensíveis da aplicação com autenticação via CPF"*, e o
caminho autenticado equivalente já existe em `/cliente/ordens-servico` (ADR 001).

## Decisão

**Marcar como `@Deprecated`, manter funcionando, e proteger na borda.**

1. A classe recebe `@Deprecated(since = "fase-3")` e as três operações saem no
   OpenAPI com `deprecated = true` — aparecem riscadas no Swagger UI.
2. O `@Tag` passa a se chamar "Público (descontinuado)" apontando o substituto.
3. No repositório do API Gateway, essas rotas ficam atrás de **API key**, tratadas
   como canal de sistema parceiro — não de cliente final.
4. O caminho recomendado para o cliente passa a ser `/cliente/ordens-servico`.

## Alternativas consideradas

| Alternativa | Por que não |
| --- | --- |
| **Remover os endpoints** | Quebra o contrato entregue e avaliado na fase 2. O enunciado da fase 2 pedia explicitamente esse canal de notificação externa; retirá-lo agora pode ser lido como regressão de requisito. |
| **Deixar como está, sem sinalizar** | Mantém uma rota que aprova orçamento sem autenticação, sem registro da intenção. Numa arguição, "não percebemos" e "decidimos manter por compatibilidade" têm pesos muito diferentes. |
| **Exigir JWT nesses mesmos paths** | Quebraria os clientes da fase 2 do mesmo jeito que remover, só que com 401 em vez de 404. |

## Consequências

**Positivas**

- Contrato da fase 2 preservado; nada que funcionava para de funcionar.
- A intenção fica explícita no código, no Swagger e aqui.
- A proteção é resolvida onde o enunciado pede que exista controle de acesso: o
  **API Gateway**.

**Negativas / custos assumidos**

- Enquanto o API Gateway não estiver no ar, a rota segue aberta. É uma exposição
  conhecida e datada, não um descuido.
- Passam a existir dois caminhos para a mesma operação até a remoção definitiva
  (prevista para depois da fase 3).

## Verificação

Os ITs da fase 2 (`OrdemServicoResourceIT`) continuam exercitando os endpoints
públicos e passando — é o que comprova que a compatibilidade foi mantida. Os ITs
novos (`AreaClienteResourceIT`) cobrem o caminho autenticado.
