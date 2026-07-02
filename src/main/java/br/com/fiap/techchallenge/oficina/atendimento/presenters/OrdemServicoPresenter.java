package br.com.fiap.techchallenge.oficina.atendimento.presenters;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.AberturaOrdemServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoPublicaResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.StatusOSResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemPeca;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Orcamento;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;

import java.util.List;

/**
 * Presenter do agregado Ordem de Serviço. Concentra a montagem do detalhe completo
 * (com itens/orçamento) e da visão pública resumida — antes espalhada nos
 * {@code from(...)} dos DTOs e dos records aninhados.
 */
public class OrdemServicoPresenter {

    public OrdemServicoResponse apresentar(OrdemServico os) {
        return new OrdemServicoResponse(
                os.id().valor(),
                os.clienteId().valor(),
                os.veiculoId().valor(),
                os.status().name(),
                os.itensServico().stream().map(this::view).toList(),
                os.itensPeca().stream().map(this::view).toList(),
                os.orcamento() == null ? null : view(os.orcamento()),
                os.criadaEm(),
                os.diagnosticoIniciadoEm(),
                os.execucaoIniciadaEm(),
                os.finalizadaEm(),
                os.entregueEm(),
                os.canceladaEm());
    }

    /** Retorno enxuto da abertura — destaca a identificação única da OS. */
    public AberturaOrdemServicoResponse apresentarAbertura(OrdemServico os) {
        return new AberturaOrdemServicoResponse(
                os.id().valor(),
                os.status().name(),
                os.status().descricao(),
                os.criadaEm());
    }

    /** Consulta de status: situação atual com descrição amigável. */
    public StatusOSResponse apresentarStatus(OrdemServico os) {
        return new StatusOSResponse(
                os.id().valor(),
                os.status().name(),
                os.status().descricao(),
                os.atualizadaEm());
    }

    public List<OrdemServicoResponse> apresentar(List<OrdemServico> ordens) {
        return ordens.stream().map(this::apresentar).toList();
    }

    public OrdemServicoPublicaResponse apresentarPublico(OrdemServico os) {
        Orcamento orcamento = os.orcamento();
        return new OrdemServicoPublicaResponse(
                os.id().valor(),
                os.status().name(),
                orcamento == null ? null : orcamento.valorTotal().valor(),
                os.criadaEm(),
                orcamento == null ? null : orcamento.geradoEm(),
                orcamento == null ? null : orcamento.aprovadoEm(),
                os.finalizadaEm(),
                os.entregueEm());
    }

    private OrdemServicoResponse.ItemServicoView view(ItemServico i) {
        return new OrdemServicoResponse.ItemServicoView(
                i.id(), i.servicoId().valor(), i.valorCobrado().valor());
    }

    private OrdemServicoResponse.ItemPecaView view(ItemPeca i) {
        return new OrdemServicoResponse.ItemPecaView(
                i.id(),
                i.pecaId().valor(),
                i.quantidade(),
                i.valorUnitario().valor(),
                i.reservaId() == null ? null : i.reservaId().valor());
    }

    private OrdemServicoResponse.OrcamentoView view(Orcamento o) {
        return new OrdemServicoResponse.OrcamentoView(
                o.valorTotal().valor(), o.geradoEm(), o.aprovadoEm());
    }
}
