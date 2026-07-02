package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resposta resumida para o endpoint público de acompanhamento. Record puro — a
 * montagem vive no {@code OrdemServicoPresenter}.
 *
 * <p>Sem ids de cliente/veículo, sem itens, sem reservas — apenas o que o cliente
 * precisa para acompanhar o status da OS.
 */
public record OrdemServicoPublicaResponse(
        UUID id,
        String status,
        BigDecimal valorTotal,
        OffsetDateTime criadaEm,
        OffsetDateTime orcamentoGeradoEm,
        OffsetDateTime orcamentoAprovadoEm,
        OffsetDateTime finalizadaEm,
        OffsetDateTime entregueEm
) {
}
