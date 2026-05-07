package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

import java.util.Optional;

/**
 * Contrato de persistência do agregado {@link Usuario}. Sem dependência de framework —
 * impl em {@code infrastructure/persistence}.
 */
public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UsuarioId id);

    Optional<Usuario> buscarPorUsername(String username);
}
