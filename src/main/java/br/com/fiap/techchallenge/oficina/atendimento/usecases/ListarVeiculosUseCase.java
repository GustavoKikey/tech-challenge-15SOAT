package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;

import java.util.List;

public class ListarVeiculosUseCase {

    private final VeiculoGateway repository;

    public ListarVeiculosUseCase(VeiculoGateway repository) {
        this.repository = repository;
    }

    public List<Veiculo> executar() {
        return repository.listar();
    }

    public List<Veiculo> porCliente(ClienteId clienteId) {
        return repository.listarPorCliente(clienteId);
    }
}
