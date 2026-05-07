package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class UsuarioNaoEncontradoException extends DomainException {

    public UsuarioNaoEncontradoException(String username) {
        super("Usuário não encontrado: " + username);
    }

    public UsuarioNaoEncontradoException(UsuarioId id) {
        super("Usuário não encontrado: " + id);
    }
}
