package br.com.fiap.techchallenge.oficina.domain.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class ClienteJaCadastradoException extends DomainException {

    public ClienteJaCadastradoException(String documento) {
        super("Já existe cliente cadastrado com o documento: " + documento);
    }
}
