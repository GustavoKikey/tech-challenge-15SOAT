package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;

public class BuscarVeiculoPorIdUseCase {

    private final VeiculoGateway repository;

    public BuscarVeiculoPorIdUseCase(VeiculoGateway repository) {
        this.repository = repository;
    }

    public Veiculo executar(VeiculoId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(id));
    }
}
