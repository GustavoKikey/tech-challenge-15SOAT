package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record AtualizarClienteRequest(
        @Schema(example = "João da Silva", description = "Nome completo do cliente")
        @NotBlank @Size(max = 150) String nome,
        
        @Schema(example = "joao.silva@example.com", description = "Email do cliente")
        @Email    @Size(max = 150) String email,
        
        @Schema(example = "11999999999", description = "Telefone do cliente")
        @Size(max = 30)            String telefone
) {}
