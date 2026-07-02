package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.entities.ReservaId;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;

/**
 * Cancela uma reserva ATIVA, devolvendo o saldo ao estoque.
 *
 * <p><b>Não é endpoint REST público.</b> Acionado pelo BC Atendimento quando o
 * cliente recusa o orçamento: as peças reservadas na geração do orçamento
 * voltam a ficar disponíveis.
 */
public class LiberarReservaUseCase {

    private final PecaGateway repository;

    public LiberarReservaUseCase(PecaGateway repository) {
        this.repository = repository;
    }

    public record Input(PecaId pecaId, ReservaId reservaId) {}

    public Peca executar(Input input) {
        Peca peca = repository.buscarPorIdComLock(input.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.pecaId()));
        peca.cancelarReserva(input.reservaId());
        return repository.salvar(peca);
    }
}
