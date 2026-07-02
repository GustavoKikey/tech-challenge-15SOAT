package br.com.fiap.techchallenge.oficina.estoque.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

public record CadastrarPecaRequest(
        @Schema(example = "Filtro de Óleo", description = "Descrição da peça")
        @NotBlank @Size(max = 200)                            String descricao,
        
        @Schema(example = "45.90", description = "Valor unitário da peça")
        @NotNull  @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
                                                              BigDecimal valorUnitario,
                                                              
        @Schema(example = "10", description = "Quantidade inicial em estoque")
        @PositiveOrZero                                       Integer quantidadeInicial
) {
    public int quantidadeInicialOuZero() {
        return quantidadeInicial == null ? 0 : quantidadeInicial;
    }
}
