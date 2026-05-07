package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.servico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponse(
        UUID id,
        String descricao,
        BigDecimal valorBase
) {
    public static ServicoResponse from(Servico s) {
        return new ServicoResponse(
                s.id().valor(),
                s.descricao(),
                s.valorBase().valor()
        );
    }
}
