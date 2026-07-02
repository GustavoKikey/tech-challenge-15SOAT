package br.com.fiap.techchallenge.oficina.estoque.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record AdicionarSaldoRequest(
        @Schema(example = "10", description = "Quantidade de peças a adicionar ao saldo")
        @NotNull @Positive Integer quantidade
) {}
