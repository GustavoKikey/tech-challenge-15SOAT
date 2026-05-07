package br.com.fiap.techchallenge.oficina.application.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BuscarClientePorIdUseCase {

    private final ClienteRepository repository;

    public BuscarClientePorIdUseCase(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Cliente executar(ClienteId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ClienteNaoEncontradoException(id));
    }
}
