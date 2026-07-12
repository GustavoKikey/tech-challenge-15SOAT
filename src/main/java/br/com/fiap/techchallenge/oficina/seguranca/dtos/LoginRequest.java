package br.com.fiap.techchallenge.oficina.seguranca.dtos;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record LoginRequest(
        @Schema(example = "joao.silva", description = "Nome de usuário")
        @NotBlank(message = "username é obrigatório") String username,
        
        @Schema(example = "SenhaForte123!", description = "Senha do usuário")
        @NotBlank(message = "password é obrigatório") String password
) {}
