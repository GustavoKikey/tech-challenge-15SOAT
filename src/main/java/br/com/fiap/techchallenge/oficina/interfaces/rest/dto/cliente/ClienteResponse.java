package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.cliente;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;

import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nome,
        String documento,
        String tipoDocumento,
        String email,
        String telefone
) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
                c.id().valor(),
                c.nome(),
                c.documento().numero(),
                c.documento().tipo().name(),
                c.email(),
                c.telefone()
        );
    }
}
