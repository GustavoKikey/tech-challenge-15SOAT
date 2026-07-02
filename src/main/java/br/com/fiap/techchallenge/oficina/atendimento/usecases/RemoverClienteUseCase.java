package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;

public class RemoverClienteUseCase {

    private final ClienteGateway repository;

    public RemoverClienteUseCase(ClienteGateway repository) {
        this.repository = repository;
    }

    public void executar(ClienteId id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new ClienteNaoEncontradoException(id);
        }
        repository.remover(id);
    }
}
