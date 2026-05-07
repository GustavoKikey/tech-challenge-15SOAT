package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resposta resumida para o endpoint público de acompanhamento.
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
    public static OrdemServicoPublicaResponse from(OrdemServico os) {
        return new OrdemServicoPublicaResponse(
                os.id().valor(),
                os.status().name(),
                os.orcamento() == null ? null : os.orcamento().valorTotal().valor(),
                os.criadaEm(),
                os.orcamento() == null ? null : os.orcamento().geradoEm(),
                os.orcamento() == null ? null : os.orcamento().aprovadoEm(),
                os.finalizadaEm(),
                os.entregueEm()
        );
    }
}
