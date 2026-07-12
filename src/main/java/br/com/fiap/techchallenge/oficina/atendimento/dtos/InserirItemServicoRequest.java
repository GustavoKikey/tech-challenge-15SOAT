package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record InserirItemServicoRequest(
        @Schema(example = "123e4567-e89b-12d3-a456-426614174001", description = "ID do serviço")
        @NotNull UUID servicoId,
        
        @Schema(example = "250.00", description = "Valor cobrado pelo serviço")
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal valorCobrado
) {}
