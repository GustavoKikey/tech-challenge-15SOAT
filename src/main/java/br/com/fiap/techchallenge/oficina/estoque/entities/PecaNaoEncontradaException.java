package br.com.fiap.techchallenge.oficina.estoque.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class PecaNaoEncontradaException extends DomainException {

    public PecaNaoEncontradaException(PecaId id) {
        super("Peça não encontrada: " + id);
    }
}
