package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Abertura de OS (fase 2): dados do cliente, veículo, serviços e peças em uma única
 * chamada. Cliente/veículo inexistentes são cadastrados na hora (get-or-create) —
 * por isso nome, marca, modelo e ano só são obrigatórios no primeiro atendimento.
 */
public record AbrirOrdemServicoRequest(
        @NotNull @Valid ClienteInput cliente,

        @NotNull @Valid VeiculoInput veiculo,

        @Valid List<ItemServicoInput> servicos,

        @Valid List<ItemPecaInput> pecas
) {

    public record ClienteInput(
            @Schema(example = "12345678900", description = "Documento (CPF/CNPJ) do cliente")
            @NotBlank String documento,

            @Schema(example = "Ana Souza", description = "Nome — obrigatório se o cliente ainda não existir")
            String nome,

            @Schema(example = "ana@email.com", description = "E-mail para notificações de status da OS")
            String email,

            @Schema(example = "11999990000")
            String telefone
    ) {}

    public record VeiculoInput(
            @Schema(example = "ABC1234", description = "Placa do veículo")
            @NotBlank String placa,

            @Schema(example = "VW", description = "Marca — obrigatória se o veículo ainda não existir")
            String marca,

            @Schema(example = "Golf", description = "Modelo — obrigatório se o veículo ainda não existir")
            String modelo,

            @Schema(example = "2020", description = "Ano — obrigatório se o veículo ainda não existir")
            Integer ano
    ) {}

    public record ItemServicoInput(
            @Schema(example = "123e4567-e89b-12d3-a456-426614174000", description = "ID do serviço no catálogo")
            @NotNull UUID servicoId,

            @Schema(example = "350.00", description = "Valor cobrado — se omitido, usa o valor base do catálogo")
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
            BigDecimal valorCobrado
    ) {}

    public record ItemPecaInput(
            @Schema(example = "123e4567-e89b-12d3-a456-426614174000", description = "ID da peça no estoque")
            @NotNull UUID pecaId,

            @Schema(example = "2")
            @NotNull @Positive Integer quantidade
    ) {}
}
