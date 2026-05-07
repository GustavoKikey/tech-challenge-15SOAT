package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

/**
 * Falha de autenticação — username inexistente, senha errada ou usuário inativo.
 * A mensagem é deliberadamente genérica para não revelar qual desses casos ocorreu.
 */
public class CredenciaisInvalidasException extends DomainException {

    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}
