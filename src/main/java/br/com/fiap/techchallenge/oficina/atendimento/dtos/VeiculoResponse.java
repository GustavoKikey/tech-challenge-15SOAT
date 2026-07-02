package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import java.util.UUID;

/** Record puro de resposta; a montagem vive no {@code VeiculoPresenter}. */
public record VeiculoResponse(
        UUID id,
        String placa,
        String marca,
        String modelo,
        int ano,
        UUID clienteId
) {
}
