package br.com.fiap.techchallenge.oficina.relatorio.dtos;

/** Record puro de resposta; a montagem vive no {@code RelatorioPresenter}. */
public record TempoMedioResponse(long amostra, long tempoMedioExecucaoSegundos) {
}
