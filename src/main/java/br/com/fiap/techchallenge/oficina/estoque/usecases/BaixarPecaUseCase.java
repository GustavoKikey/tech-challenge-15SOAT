package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.estoque.entities.ReservaId;

/**
 * Marca a reserva como BAIXADA e decrementa o saldo total.
 *
 * <p><b>Não é endpoint REST público.</b> É a contrapartida da política
 * "Efetivar baixa" do Event Storming, acionada pelo BC Atendimento
 * após o cliente aprovar o orçamento.
 */
public class BaixarPecaUseCase {

    private final PecaGateway repository;

    public BaixarPecaUseCase(PecaGateway repository) {
        this.repository = repository;
    }

    public record Input(PecaId pecaId, ReservaId reservaId) {}

    public Peca executar(Input input) {
        Peca peca = repository.buscarPorIdComLock(input.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.pecaId()));
        peca.baixar(input.reservaId());
        return repository.salvar(peca);
    }
}
