package br.com.fiap.techchallenge.oficina.seguranca.gateways;

import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.entities.UsuarioId;

import java.util.Optional;

/**
 * Contrato de persistência do agregado {@link Usuario}. Sem dependência de framework —
 * impl em {@code infrastructure/persistence}.
 */
public interface UsuarioGateway {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UsuarioId id);

    Optional<Usuario> buscarPorUsername(String username);
}
