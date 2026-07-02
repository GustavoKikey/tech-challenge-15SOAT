package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class ClienteNaoEncontradoException extends DomainException {

    public ClienteNaoEncontradoException(ClienteId id) {
        super("Cliente não encontrado: " + id);
    }

    public ClienteNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
