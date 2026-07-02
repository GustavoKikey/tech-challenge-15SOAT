package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;

import java.util.List;

public class ListarOrdensServicoUseCase {

    private final OrdemServicoGateway repository;

    public ListarOrdensServicoUseCase(OrdemServicoGateway repository) {
        this.repository = repository;
    }

    public List<OrdemServico> executar(OrdemServicoGateway.Filtro filtro) {
        return repository.listar(filtro);
    }
}
