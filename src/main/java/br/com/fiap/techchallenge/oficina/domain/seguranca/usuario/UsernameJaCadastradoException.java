package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class UsernameJaCadastradoException extends DomainException {

    public UsernameJaCadastradoException(String username) {
        super("Username já cadastrado: " + username);
    }
}
