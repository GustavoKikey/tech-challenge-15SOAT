package br.com.fiap.techchallenge.oficina.seguranca.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class UsernameJaCadastradoException extends DomainException {

    public UsernameJaCadastradoException(String username) {
        super("Username já cadastrado: " + username);
    }
}
