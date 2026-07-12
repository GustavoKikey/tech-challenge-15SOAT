package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaPossuiReservasAtivasException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;

public class RemoverPecaUseCase {

    private final PecaGateway repository;

    public RemoverPecaUseCase(PecaGateway repository) {
        this.repository = repository;
    }

    public void executar(PecaId id) {
        Peca peca = repository.buscarPorId(id)
                .orElseThrow(() -> new PecaNaoEncontradaException(id));
        if (peca.possuiReservasAtivas()) {
            throw new PecaPossuiReservasAtivasException(id);
        }
        repository.remover(id);
    }
}
