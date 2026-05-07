package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.seguranca;

import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Usuario;

import java.util.UUID;

/** Resposta segura — não expõe senha (hash incluso). */
public record UsuarioResponse(UUID id, String username, String role, boolean ativo) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.id().valor(),
                usuario.username(),
                usuario.role().name(),
                usuario.ativo());
    }
}
