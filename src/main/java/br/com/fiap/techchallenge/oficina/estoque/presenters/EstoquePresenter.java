package br.com.fiap.techchallenge.oficina.estoque.presenters;

import br.com.fiap.techchallenge.oficina.estoque.dtos.PecaResponse;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;

import java.util.List;

/** Presenter do BC Estoque: agregado {@link Peca} → {@link PecaResponse}. */
public class EstoquePresenter {

    public PecaResponse apresentar(Peca peca) {
        return new PecaResponse(
                peca.id().valor(),
                peca.descricao(),
                peca.valorUnitario().valor(),
                peca.quantidadeTotal(),
                peca.saldoDisponivel());
    }

    public List<PecaResponse> apresentar(List<Peca> pecas) {
        return pecas.stream().map(this::apresentar).toList();
    }
}
