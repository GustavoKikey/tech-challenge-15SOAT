package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class TransicaoStatusInvalidaException extends DomainException {

    public TransicaoStatusInvalidaException(StatusOS atual, StatusOS desejado) {
        super("Transição de status inválida: " + atual + " → " + desejado);
    }

    public TransicaoStatusInvalidaException(String message) {
        super(message);
    }
}
