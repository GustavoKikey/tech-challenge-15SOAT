package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;

public class BuscarPecaPorIdUseCase {

    private final PecaGateway repository;

    public BuscarPecaPorIdUseCase(PecaGateway repository) {
        this.repository = repository;
    }

    public Peca executar(PecaId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new PecaNaoEncontradaException(id));
    }
}
