package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.estoque.usecases.BaixarPecaUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemPeca;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;

/**
 * Aprova o orçamento <i>e</i> dá baixa, no BC Estoque, em todas as reservas das peças.
 *
 * <p>Transação única: se uma baixa falhar, a aprovação inteira reverte (status volta
 * a AGUARDANDO_APROVACAO, reservas anteriores não são consumidas). É a contrapartida
 * da política POL "Efetivar baixa" do Event Storming.
 */
public class AprovarOrcamentoUseCase {

    private final OrdemServicoGateway osRepository;
    private final BaixarPecaUseCase baixarPeca;

    public AprovarOrcamentoUseCase(OrdemServicoGateway osRepository,
                                   BaixarPecaUseCase baixarPeca) {
        this.osRepository = osRepository;
        this.baixarPeca = baixarPeca;
    }

    public OrdemServico executar(OrdemServicoId id) {
        OrdemServico os = osRepository.buscarPorIdComLock(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));

        os.aprovarOrcamento();

        for (ItemPeca item : os.itensPeca()) {
            if (item.reservaId() == null) {
                throw new IllegalStateException(
                        "Item de peça " + item.id() + " não possui reserva — orçamento inconsistente");
            }
            baixarPeca.executar(new BaixarPecaUseCase.Input(item.pecaId(), item.reservaId()));
        }

        return osRepository.salvar(os);
    }
}
