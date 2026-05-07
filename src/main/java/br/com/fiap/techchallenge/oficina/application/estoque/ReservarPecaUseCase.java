package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Reserva;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Cria reserva ATIVA de uma peça para uma OS.
 *
 * <p><b>Não é endpoint REST público.</b> É a contrapartida da política
 * "Reservar peça" do Event Storming, acionada pelo BC Atendimento
 * durante a geração do orçamento. Lê com lock pessimista para evitar dois
 * pedidos concorrentes passarem na verificação de saldo disponível.
 */
@ApplicationScoped
public class ReservarPecaUseCase {

    private final PecaRepository repository;

    public ReservarPecaUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    public record Input(PecaId pecaId, OrdemServicoId ordemServicoId, int quantidade) {}

    @Transactional
    public Reserva executar(Input input) {
        Peca peca = repository.buscarPorIdComLock(input.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.pecaId()));
        Reserva reserva = peca.reservar(input.ordemServicoId(), input.quantidade());
        repository.salvar(peca);
        return reserva;
    }
}
