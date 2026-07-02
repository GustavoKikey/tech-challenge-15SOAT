package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record CriarOrdemServicoRequest(
        @Schema(example = "12345678900", description = "Documento (CPF/CNPJ) do cliente")
        @NotBlank String documentoCliente,
        
        @Schema(example = "ABC1234", description = "Placa do veículo")
        @NotBlank String placaVeiculo
) {}
