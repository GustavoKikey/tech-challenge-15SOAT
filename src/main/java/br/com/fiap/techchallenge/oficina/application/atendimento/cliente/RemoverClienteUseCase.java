package br.com.fiap.techchallenge.oficina.application.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoverClienteUseCase {

    private final ClienteRepository repository;

    public RemoverClienteUseCase(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(ClienteId id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new ClienteNaoEncontradoException(id);
        }
        repository.remover(id);
    }
}
