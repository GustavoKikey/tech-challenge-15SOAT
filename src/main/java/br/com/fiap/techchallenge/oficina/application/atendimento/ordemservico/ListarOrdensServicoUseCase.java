package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ListarOrdensServicoUseCase {

    private final OrdemServicoRepository repository;

    public ListarOrdensServicoUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<OrdemServico> executar(OrdemServicoRepository.Filtro filtro) {
        return repository.listar(filtro);
    }
}
