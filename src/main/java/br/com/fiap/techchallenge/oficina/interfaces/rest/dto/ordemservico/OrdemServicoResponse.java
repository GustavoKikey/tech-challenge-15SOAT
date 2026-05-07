package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemPeca;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.Orcamento;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
        OffsetDateTime entregueEm
) {

    public record ItemServicoView(UUID id, UUID servicoId, BigDecimal valorCobrado) {
        static ItemServicoView from(ItemServico i) {
            return new ItemServicoView(i.id(), i.servicoId().valor(), i.valorCobrado().valor());
        }
    }

    public record ItemPecaView(UUID id, UUID pecaId, int quantidade,
                               BigDecimal valorUnitario, UUID reservaId) {
        static ItemPecaView from(ItemPeca i) {
            return new ItemPecaView(
                    i.id(),
                    i.pecaId().valor(),
                    i.quantidade(),
                    i.valorUnitario().valor(),
                    i.reservaId() == null ? null : i.reservaId().valor());
        }
    }

    public record OrcamentoView(BigDecimal valorTotal, OffsetDateTime geradoEm,
                                OffsetDateTime aprovadoEm) {
        static OrcamentoView from(Orcamento o) {
            return new OrcamentoView(o.valorTotal().valor(), o.geradoEm(), o.aprovadoEm());
        }
    }

    public static OrdemServicoResponse from(OrdemServico os) {
        return new OrdemServicoResponse(
                os.id().valor(),
                os.clienteId().valor(),
                os.veiculoId().valor(),
                os.status().name(),
                os.itensServico().stream().map(ItemServicoView::from).toList(),
                os.itensPeca().stream().map(ItemPecaView::from).toList(),
                os.orcamento() == null ? null : OrcamentoView.from(os.orcamento()),
                os.criadaEm(),
                os.diagnosticoIniciadoEm(),
                os.execucaoIniciadaEm(),
                os.finalizadaEm(),
                os.entregueEm()
        );
    }
}
