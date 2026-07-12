package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;

import java.util.List;

public class ListarServicosUseCase {

    private final ServicoGateway repository;

    public ListarServicosUseCase(ServicoGateway repository) {
        this.repository = repository;
    }

    public List<Servico> executar() {
        return repository.listar();
    }
}
