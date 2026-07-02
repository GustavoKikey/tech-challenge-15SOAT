package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemPeca;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.estoque.usecases.LiberarReservaUseCase;

/**
 * Recusa o orçamento: a OS vai para CANCELADA (exclusão lógica) <i>e</i> todas as
 * reservas de peças feitas na geração do orçamento são liberadas no BC Estoque,
 * devolvendo o saldo.
 *
 * <p>Transação única, espelho do {@link AprovarOrcamentoUseCase}: se a liberação
 * de alguma reserva falhar, a recusa inteira reverte.
 */
public class RecusarOrcamentoUseCase {

    private final OrdemServicoGateway osRepository;
    private final LiberarReservaUseCase liberarReserva;

    public RecusarOrcamentoUseCase(OrdemServicoGateway osRepository,
                                   LiberarReservaUseCase liberarReserva) {
        this.osRepository = osRepository;
        this.liberarReserva = liberarReserva;
    }

    public OrdemServico executar(OrdemServicoId id) {
        OrdemServico os = osRepository.buscarPorIdComLock(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));

        os.recusarOrcamento();

        for (ItemPeca item : os.itensPeca()) {
            if (item.reservaId() != null) {
                liberarReserva.executar(new LiberarReservaUseCase.Input(
                        item.pecaId(), item.reservaId()));
            }
        }

        return osRepository.salvar(os);
    }
}
