package br.com.fiap.techchallenge.oficina.domain.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class ClienteNaoEncontradoException extends DomainException {

    public ClienteNaoEncontradoException(ClienteId id) {
        super("Cliente não encontrado: " + id);
    }

    public ClienteNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
