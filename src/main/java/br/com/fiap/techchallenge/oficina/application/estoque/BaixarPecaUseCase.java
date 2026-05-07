package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.ReservaId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Marca a reserva como BAIXADA e decrementa o saldo total.
 *
 * <p><b>Não é endpoint REST público.</b> É a contrapartida da política
 * "Efetivar baixa" do Event Storming, acionada pelo BC Atendimento
 * após o cliente aprovar o orçamento.
 */
@ApplicationScoped
public class BaixarPecaUseCase {

    private final PecaRepository repository;

    public BaixarPecaUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    public record Input(PecaId pecaId, ReservaId reservaId) {}

    @Transactional
    public Peca executar(Input input) {
        Peca peca = repository.buscarPorIdComLock(input.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.pecaId()));
        peca.baixar(input.reservaId());
        return repository.salvar(peca);
    }
}
