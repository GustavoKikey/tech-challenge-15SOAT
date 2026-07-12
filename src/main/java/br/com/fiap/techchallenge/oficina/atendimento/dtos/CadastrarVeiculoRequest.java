package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

public record CadastrarVeiculoRequest(
        @Schema(example = "ABC1234", description = "Placa do veículo")
        @NotBlank @Size(max = 10)  String placa,
        
        @Schema(example = "Toyota", description = "Marca do veículo")
        @NotBlank @Size(max = 80)  String marca,
        
        @Schema(example = "Corolla", description = "Modelo do veículo")
        @NotBlank @Size(max = 120) String modelo,
        
        @Schema(example = "2020", description = "Ano de fabricação do veículo")
        @Min(1900)                 int ano,
        
        @Schema(example = "123e4567-e89b-12d3-a456-426614174000", description = "ID do cliente proprietário")
        @NotNull                   UUID clienteId
) {}
