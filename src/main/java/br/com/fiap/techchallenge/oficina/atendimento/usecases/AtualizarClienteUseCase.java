package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;

public class AtualizarClienteUseCase {

    private final ClienteGateway repository;

    public AtualizarClienteUseCase(ClienteGateway repository) {
        this.repository = repository;
    }

    public record Input(ClienteId id, String nome, String email, String telefone) {}

    public Cliente executar(Input input) {
        Cliente cliente = repository.buscarPorId(input.id())
                .orElseThrow(() -> new ClienteNaoEncontradoException(input.id()));
        cliente.renomear(input.nome());
        cliente.alterarContato(input.email(), input.telefone());
        return repository.salvar(cliente);
    }
}
