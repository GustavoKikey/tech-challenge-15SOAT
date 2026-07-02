package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record InserirItemPecaRequest(
        @Schema(example = "123e4567-e89b-12d3-a456-426614174000", description = "ID da peça")
        @NotNull UUID pecaId,
        
        @Schema(example = "2", description = "Quantidade de peças")
        @NotNull @Positive Integer quantidade,
        
        @Schema(example = "150.50", description = "Valor unitário cobrado pela peça")
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal valorUnitario
) {}
