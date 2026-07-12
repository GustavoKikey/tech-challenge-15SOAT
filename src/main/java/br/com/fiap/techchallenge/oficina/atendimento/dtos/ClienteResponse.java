package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import java.util.UUID;

/** Record puro de resposta; a montagem vive no {@code ClientePresenter}. */
public record ClienteResponse(
        UUID id,
        String nome,
        String documento,
        String tipoDocumento,
        String email,
        String telefone
) {
}
