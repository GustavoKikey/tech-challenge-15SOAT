package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemPeca;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.estoque.entities.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.math.BigDecimal;

/**
 * Adiciona um item de peça à OS.
 *
 * <p>Valida que a peça existe e que há saldo disponível <i>no momento da inserção</i>
 * (consulta sem reservar — a reserva acontece só ao gerar o orçamento, conforme POL
 * do Event Storming). Não há lock aqui, é apenas um pré-aviso de saldo: a verificação
 * autoritativa rola na geração do orçamento sob {@code PESSIMISTIC_WRITE}.
 */
public class InserirPecaNaOSUseCase {

    private final OrdemServicoGateway osRepository;
    private final PecaGateway pecaRepository;

    public InserirPecaNaOSUseCase(OrdemServicoGateway osRepository,
                                  PecaGateway pecaRepository) {
        this.osRepository = osRepository;
        this.pecaRepository = pecaRepository;
    }

    public record Input(OrdemServicoId osId, PecaId pecaId, int quantidade,
                        BigDecimal valorUnitario) {}

    public record Output(OrdemServico ordemServico, ItemPeca item) {}

    public Output executar(Input input) {
        Peca peca = pecaRepository.buscarPorId(input.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.pecaId()));

        if (input.quantidade() > peca.saldoDisponivel()) {
            throw new EstoqueInsuficienteException(
                    peca.id(), input.quantidade(), peca.saldoDisponivel());
        }

        OrdemServico os = osRepository.buscarPorId(input.osId())
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(input.osId()));

        ItemPeca item = os.inserirPeca(
                input.pecaId(), input.quantidade(), Dinheiro.de(input.valorUnitario()));
        OrdemServico salvo = osRepository.salvar(os);
        return new Output(salvo, item);
    }
}
