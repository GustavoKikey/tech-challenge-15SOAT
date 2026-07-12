package br.com.fiap.techchallenge.oficina.estoque.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class PecaPossuiReservasAtivasException extends DomainException {

    public PecaPossuiReservasAtivasException(PecaId id) {
        super("Peça " + id + " possui reservas ativas e não pode ser removida");
    }
}
