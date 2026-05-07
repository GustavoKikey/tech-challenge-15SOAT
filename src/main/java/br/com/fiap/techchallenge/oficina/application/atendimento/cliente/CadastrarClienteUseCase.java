package br.com.fiap.techchallenge.oficina.application.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteJaCadastradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Documento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CadastrarClienteUseCase {

    private final ClienteRepository repository;

    public CadastrarClienteUseCase(ClienteRepository repository) {
        this.repository = repository;
    }

    public record Input(String nome, String documento, String email, String telefone) {}

    @Transactional
    public Cliente executar(Input input) {
        Documento documento = Documento.de(input.documento());
        repository.buscarPorDocumento(documento).ifPresent(c -> {
            throw new ClienteJaCadastradoException(documento.numero());
        });
        Cliente cliente = Cliente.novo(input.nome(), documento, input.email(), input.telefone());
        return repository.salvar(cliente);
    }
}
