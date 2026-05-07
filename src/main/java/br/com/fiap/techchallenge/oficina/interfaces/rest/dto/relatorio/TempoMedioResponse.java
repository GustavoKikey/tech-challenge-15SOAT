package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.relatorio;

import br.com.fiap.techchallenge.oficina.domain.relatorio.TempoMedioExecucao;

public record TempoMedioResponse(long amostra, long tempoMedioExecucaoSegundos) {

    public static TempoMedioResponse from(TempoMedioExecucao t) {
        return new TempoMedioResponse(t.amostra(), t.tempoMedioExecucaoSegundos());
    }
}
