package br.com.fiap.techchallenge.oficina.shared.entities;

public class DocumentoInvalidoException extends DomainException {

    public DocumentoInvalidoException(String message) {
        super(message);
    }
}
