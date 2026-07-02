package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;

public class BuscarOrdemServicoPorIdUseCase {

    private final OrdemServicoGateway repository;

    public BuscarOrdemServicoPorIdUseCase(OrdemServicoGateway repository) {
        this.repository = repository;
    }

    public OrdemServico executar(OrdemServicoId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
    }
}
