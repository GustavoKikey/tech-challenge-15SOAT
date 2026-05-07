package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemPeca;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

/**
 * Adiciona um item de peça à OS.
 *
 * <p>Valida que a peça existe e que há saldo disponível <i>no momento da inserção</i>
 * (consulta sem reservar — a reserva acontece só ao gerar o orçamento, conforme POL
 * do Event Storming). Não há lock aqui, é apenas um pré-aviso de saldo: a verificação
 * autoritativa rola na geração do orçamento sob {@code PESSIMISTIC_WRITE}.
 */
@ApplicationScoped
public class InserirPecaNaOSUseCase {

    private final OrdemServicoRepository osRepository;
    private final PecaRepository pecaRepository;

    public InserirPecaNaOSUseCase(OrdemServicoRepository osRepository,
                                  PecaRepository pecaRepository) {
        this.osRepository = osRepository;
        this.pecaRepository = pecaRepository;
    }

    public record Input(OrdemServicoId osId, PecaId pecaId, int quantidade,
                        BigDecimal valorUnitario) {}

    public record Output(OrdemServico ordemServico, ItemPeca item) {}

    @Transactional
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
