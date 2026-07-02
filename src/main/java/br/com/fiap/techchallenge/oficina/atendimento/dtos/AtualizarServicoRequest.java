package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

public record AtualizarServicoRequest(
        @Schema(example = "Troca de Óleo", description = "Descrição do serviço")
        @NotBlank @Size(max = 200)                            String descricao,
        
        @Schema(example = "120.00", description = "Valor base do serviço")
        @NotNull  @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
                                                              BigDecimal valorBase
) {}
