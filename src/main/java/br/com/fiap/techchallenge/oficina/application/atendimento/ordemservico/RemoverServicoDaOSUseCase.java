package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class RemoverServicoDaOSUseCase {

    private final OrdemServicoRepository repository;

    public RemoverServicoDaOSUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    public record Input(OrdemServicoId osId, UUID itemId) {}

    @Transactional
    public OrdemServico executar(Input input) {
        OrdemServico os = repository.buscarPorId(input.osId())
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(input.osId()));
        os.removerItemServico(input.itemId());
        return repository.salvar(os);
    }
}
