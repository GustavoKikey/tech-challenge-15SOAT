package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;

public class RemoverServicoUseCase {

    private final ServicoGateway repository;

    public RemoverServicoUseCase(ServicoGateway repository) {
        this.repository = repository;
    }

    public void executar(ServicoId id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new ServicoNaoEncontradoException(id);
        }
        repository.remover(id);
    }
}
