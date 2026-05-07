package br.com.fiap.techchallenge.oficina.domain.estoque.peca;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class PecaNaoEncontradaException extends DomainException {

    public PecaNaoEncontradaException(PecaId id) {
        super("Peça não encontrada: " + id);
    }
}
