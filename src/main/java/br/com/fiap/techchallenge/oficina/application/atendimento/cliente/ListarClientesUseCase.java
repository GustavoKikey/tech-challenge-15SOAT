package br.com.fiap.techchallenge.oficina.application.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ListarClientesUseCase {

    private final ClienteRepository repository;

    public ListarClientesUseCase(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Cliente> executar() {
        return repository.listar();
    }
}
