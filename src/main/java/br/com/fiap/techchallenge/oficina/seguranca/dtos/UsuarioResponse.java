package br.com.fiap.techchallenge.oficina.seguranca.dtos;

import java.util.UUID;

/** Resposta segura — não expõe senha. Record puro; a montagem vive no presenter. */
public record UsuarioResponse(UUID id, String username, String role, boolean ativo) {
}
