package br.com.fiap.techchallenge.oficina.seguranca.dtos;

import br.com.fiap.techchallenge.oficina.seguranca.entities.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record CadastrarUsuarioRequest(
        @Schema(example = "joao.silva", description = "Nome de usuário")
        @NotBlank @Size(min = 3, max = 80) String username,
        
        @Schema(example = "SenhaForte123!", description = "Senha do usuário")
        @NotBlank @Size(min = 8, max = 100) String password,
        
        @Schema(example = "ATENDENTE", description = "Papel do usuário no sistema")
        @NotNull Role role
) {}
