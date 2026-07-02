package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detalhe completo da OS. Record puro (e records aninhados puros) — a montagem a
 * partir do agregado vive no {@code OrdemServicoPresenter}.
 */
public record OrdemServicoResponse(
        UUID id,
        UUID clienteId,
        UUID veiculoId,
        String status,
        List<ItemServicoView> itensServico,
        List<ItemPecaView> itensPeca,
        OrcamentoView orcamento,
        OffsetDateTime criadaEm,
        OffsetDateTime diagnosticoIniciadoEm,
        OffsetDateTime execucaoIniciadaEm,
        OffsetDateTime finalizadaEm,
        OffsetDateTime entregueEm,
        OffsetDateTime canceladaEm
) {

    public record ItemServicoView(UUID id, UUID servicoId, BigDecimal valorCobrado) {}

    public record ItemPecaView(UUID id, UUID pecaId, int quantidade,
                               BigDecimal valorUnitario, UUID reservaId) {}

    public record OrcamentoView(BigDecimal valorTotal, OffsetDateTime geradoEm,
                                OffsetDateTime aprovadoEm) {}
}
