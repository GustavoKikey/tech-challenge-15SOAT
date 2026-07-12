package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;

public class RemoverVeiculoUseCase {

    private final VeiculoGateway repository;

    public RemoverVeiculoUseCase(VeiculoGateway repository) {
        this.repository = repository;
    }

    public void executar(VeiculoId id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new VeiculoNaoEncontradoException(id);
        }
        repository.remover(id);
    }
}
