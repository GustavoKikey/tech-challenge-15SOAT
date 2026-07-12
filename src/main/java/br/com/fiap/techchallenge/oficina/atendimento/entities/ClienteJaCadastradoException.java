package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class ClienteJaCadastradoException extends DomainException {

    public ClienteJaCadastradoException(String documento) {
        super("Já existe cliente cadastrado com o documento: " + documento);
    }
}
