package br.com.fiap.techchallenge.oficina.domain.estoque.peca;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class ReservaInvalidaException extends DomainException {

    public ReservaInvalidaException(String message) {
        super(message);
    }
}
