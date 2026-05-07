package br.com.fiap.techchallenge.oficina.domain.shared;

/** Raiz da hierarquia de exceções de domínio. Cada BC pode estender com erros específicos. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
