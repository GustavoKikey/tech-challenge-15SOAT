package br.com.fiap.techchallenge.oficina.application.atendimento.servico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BuscarServicoPorIdUseCase {

    private final ServicoRepository repository;

    public BuscarServicoPorIdUseCase(ServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Servico executar(ServicoId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ServicoNaoEncontradoException(id));
    }
}
