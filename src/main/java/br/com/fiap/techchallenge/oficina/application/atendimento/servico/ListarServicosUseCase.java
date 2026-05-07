package br.com.fiap.techchallenge.oficina.application.atendimento.servico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ListarServicosUseCase {

    private final ServicoRepository repository;

    public ListarServicosUseCase(ServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Servico> executar() {
        return repository.listar();
    }
}
