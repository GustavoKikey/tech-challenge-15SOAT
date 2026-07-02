package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.estoque.usecases.ReservarPecaUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemPeca;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.estoque.entities.Reserva;

/**
 * Gera o orçamento <i>e</i> reserva, no BC Estoque, todas as peças do orçamento.
 *
 * <p>Tudo em uma única transação JTA: se uma reserva falhar (por
 * {@link br.com.fiap.techchallenge.oficina.estoque.entities.EstoqueInsuficienteException},
 * por exemplo), a transação reverte e nem a OS nem as reservas anteriores ficam
 * persistidas. Esse é o ponto da política POL "Reservar peça" do Event Storming.
 */
public class GerarOrcamentoUseCase {

    private final OrdemServicoGateway osRepository;
    private final ReservarPecaUseCase reservarPeca;

    public GerarOrcamentoUseCase(OrdemServicoGateway osRepository,
                                 ReservarPecaUseCase reservarPeca) {
        this.osRepository = osRepository;
        this.reservarPeca = reservarPeca;
    }

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
