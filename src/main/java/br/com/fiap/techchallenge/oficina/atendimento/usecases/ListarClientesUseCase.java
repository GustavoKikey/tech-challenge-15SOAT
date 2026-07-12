package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;

import java.util.List;

public class ListarClientesUseCase {

    private final ClienteGateway repository;

    public ListarClientesUseCase(ClienteGateway repository) {
        this.repository = repository;
    }

    public List<Cliente> executar() {
        return repository.listar();
    }
}
