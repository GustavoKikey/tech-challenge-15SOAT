package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class TransicaoStatusInvalidaException extends DomainException {

    public TransicaoStatusInvalidaException(StatusOS atual, StatusOS desejado) {
        super("Transição de status inválida: " + atual + " → " + desejado);
    }

    public TransicaoStatusInvalidaException(String message) {
        super(message);
    }
}
