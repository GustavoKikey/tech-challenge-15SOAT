package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.application.estoque.BaixarPecaUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemPeca;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Aprova o orçamento <i>e</i> dá baixa, no BC Estoque, em todas as reservas das peças.
 *
 * <p>Transação única: se uma baixa falhar, a aprovação inteira reverte (status volta
 * a AGUARDANDO_APROVACAO, reservas anteriores não são consumidas). É a contrapartida
 * da política POL "Efetivar baixa" do Event Storming.
 */
@ApplicationScoped
public class AprovarOrcamentoUseCase {

    private final OrdemServicoRepository osRepository;
    private final BaixarPecaUseCase baixarPeca;

    public AprovarOrcamentoUseCase(OrdemServicoRepository osRepository,
                                   BaixarPecaUseCase baixarPeca) {
        this.osRepository = osRepository;
        this.baixarPeca = baixarPeca;
    }

    @Transactional
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
