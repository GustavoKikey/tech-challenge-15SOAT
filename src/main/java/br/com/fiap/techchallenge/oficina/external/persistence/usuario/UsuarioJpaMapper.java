package br.com.fiap.techchallenge.oficina.external.persistence.usuario;

import br.com.fiap.techchallenge.oficina.seguranca.entities.Role;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.entities.UsuarioId;

final class UsuarioJpaMapper {

    private UsuarioJpaMapper() {}

    static UsuarioJpaEntity toEntity(Usuario usuario, UsuarioJpaEntity existente) {
        UsuarioJpaEntity entity = existente != null ? existente : new UsuarioJpaEntity();
        entity.id = usuario.id().valor();
        entity.username = usuario.username();
        entity.senhaHash = usuario.senhaHash();
        entity.role = usuario.role().name();
        entity.ativo = usuario.ativo();
        return entity;
    }

    static Usuario toDomain(UsuarioJpaEntity entity) {
        return Usuario.reconstituir(
                UsuarioId.de(entity.id),
                entity.username,
                entity.senhaHash,
                Role.valueOf(entity.role),
                entity.ativo);
    }
}
