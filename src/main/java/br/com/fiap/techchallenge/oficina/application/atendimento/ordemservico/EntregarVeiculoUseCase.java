package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EntregarVeiculoUseCase {

    private final OrdemServicoRepository repository;

    public EntregarVeiculoUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OrdemServico executar(OrdemServicoId id) {
        OrdemServico os = repository.buscarPorId(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        os.entregar();
        return repository.salvar(os);
    }
}
