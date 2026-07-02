package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteJaCadastradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Documento;

public class CadastrarClienteUseCase {

    private final ClienteGateway repository;

    public CadastrarClienteUseCase(ClienteGateway repository) {
        this.repository = repository;
    }

    public record Input(String nome, String documento, String email, String telefone) {}

    public Cliente executar(Input input) {
        Documento documento = Documento.de(input.documento());
        repository.buscarPorDocumento(documento).ifPresent(c -> {
            throw new ClienteJaCadastradoException(documento.numero());
        });
        Cliente cliente = Cliente.novo(input.nome(), documento, input.email(), input.telefone());
        return repository.salvar(cliente);
    }
}
