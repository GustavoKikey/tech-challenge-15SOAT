package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;

public class BuscarServicoPorIdUseCase {

    private final ServicoGateway repository;

    public BuscarServicoPorIdUseCase(ServicoGateway repository) {
        this.repository = repository;
    }

    public Servico executar(ServicoId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ServicoNaoEncontradoException(id));
    }
}
