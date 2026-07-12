package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;

import java.util.UUID;

public class RemoverServicoDaOSUseCase {

    private final OrdemServicoGateway repository;

    public RemoverServicoDaOSUseCase(OrdemServicoGateway repository) {
        this.repository = repository;
    }

    public record Input(OrdemServicoId osId, UUID itemId) {}

    public OrdemServico executar(Input input) {
        OrdemServico os = repository.buscarPorId(input.osId())
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(input.osId()));
        os.removerItemServico(input.itemId());
        return repository.salvar(os);
    }
}
