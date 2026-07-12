package br.com.fiap.techchallenge.oficina.estoque.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class ReservaInvalidaException extends DomainException {

    public ReservaInvalidaException(String message) {
        super(message);
    }
}
