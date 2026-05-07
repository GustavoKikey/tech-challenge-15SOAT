package br.com.fiap.techchallenge.oficina.application.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AtualizarClienteUseCase {

    private final ClienteRepository repository;

    public AtualizarClienteUseCase(ClienteRepository repository) {
        this.repository = repository;
    }

    public record Input(ClienteId id, String nome, String email, String telefone) {}

    @Transactional
    public Cliente executar(Input input) {
        Cliente cliente = repository.buscarPorId(input.id())
                .orElseThrow(() -> new ClienteNaoEncontradoException(input.id()));
        cliente.renomear(input.nome());
        cliente.alterarContato(input.email(), input.telefone());
        return repository.salvar(cliente);
    }
}
