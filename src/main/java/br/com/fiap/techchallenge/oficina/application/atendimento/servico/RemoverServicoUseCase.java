package br.com.fiap.techchallenge.oficina.application.atendimento.servico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoverServicoUseCase {

    private final ServicoRepository repository;

    public RemoverServicoUseCase(ServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(ServicoId id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new ServicoNaoEncontradoException(id);
        }
        repository.remover(id);
    }
}
