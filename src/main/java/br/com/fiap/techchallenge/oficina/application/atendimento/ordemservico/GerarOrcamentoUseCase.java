package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.application.estoque.ReservarPecaUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemPeca;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Reserva;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Gera o orçamento <i>e</i> reserva, no BC Estoque, todas as peças do orçamento.
 *
 * <p>Tudo em uma única transação JTA: se uma reserva falhar (por
 * {@link br.com.fiap.techchallenge.oficina.domain.estoque.peca.EstoqueInsuficienteException},
 * por exemplo), a transação reverte e nem a OS nem as reservas anteriores ficam
 * persistidas. Esse é o ponto da política POL "Reservar peça" do Event Storming.
 */
@ApplicationScoped
public class GerarOrcamentoUseCase {

    private final OrdemServicoRepository osRepository;
    private final ReservarPecaUseCase reservarPeca;

    public GerarOrcamentoUseCase(OrdemServicoRepository osRepository,
                                 ReservarPecaUseCase reservarPeca) {
        this.osRepository = osRepository;
        this.reservarPeca = reservarPeca;
    }

    @Transactional
    public OrdemServico executar(OrdemServicoId id) {
        OrdemServico os = osRepository.buscarPorIdComLock(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));

        os.gerarOrcamento();

        for (ItemPeca item : os.itensPeca()) {
            Reserva reserva = reservarPeca.executar(new ReservarPecaUseCase.Input(
                    item.pecaId(), os.id(), item.quantidade()));
            os.registrarReserva(item.id(), reserva.id());
        }

        return osRepository.salvar(os);
    }
}
