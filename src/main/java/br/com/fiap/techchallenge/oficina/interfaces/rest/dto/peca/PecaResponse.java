package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.peca;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;

import java.math.BigDecimal;
import java.util.UUID;

public record PecaResponse(
        UUID id,
        String descricao,
        BigDecimal valorUnitario,
        int quantidadeTotal,
        int saldoDisponivel
) {
    public static PecaResponse from(Peca p) {
        return new PecaResponse(
                p.id().valor(),
                p.descricao(),
                p.valorUnitario().valor(),
                p.quantidadeTotal(),
                p.saldoDisponivel()
        );
    }
}
