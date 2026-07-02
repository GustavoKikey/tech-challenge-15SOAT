package br.com.fiap.techchallenge.oficina.atendimento.presenters;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.ClienteResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;

import java.util.List;

/** Presenter do agregado Cliente. */
public class ClientePresenter {

    public ClienteResponse apresentar(Cliente c) {
        return new ClienteResponse(
                c.id().valor(),
                c.nome(),
                c.documento().numero(),
                c.documento().tipo().name(),
                c.email(),
                c.telefone());
    }

    public List<ClienteResponse> apresentar(List<Cliente> clientes) {
        return clientes.stream().map(this::apresentar).toList();
    }
}
