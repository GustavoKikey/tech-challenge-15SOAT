package br.com.fiap.techchallenge.oficina.domain.estoque.peca;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class PecaPossuiReservasAtivasException extends DomainException {

    public PecaPossuiReservasAtivasException(PecaId id) {
        super("Peça " + id + " possui reservas ativas e não pode ser removida");
    }
}
