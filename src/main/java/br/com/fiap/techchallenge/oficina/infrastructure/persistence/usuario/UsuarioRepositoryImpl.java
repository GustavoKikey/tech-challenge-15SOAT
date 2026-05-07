package br.com.fiap.techchallenge.oficina.infrastructure.persistence.usuario;

import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Usuario;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsuarioId;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsuarioRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UsuarioRepositoryImpl implements UsuarioRepository,
        PanacheRepositoryBase<UsuarioJpaEntity, UUID> {

    @Override
    public Usuario salvar(Usuario usuario) {
        UUID id = usuario.id().valor();
        UsuarioJpaEntity existente = findById(id);
        UsuarioJpaEntity entity = UsuarioJpaMapper.toEntity(usuario, existente);
        if (existente == null) {
            persist(entity);
        }
        return UsuarioJpaMapper.toDomain(entity);
    }

    @Override
    public Optional<Usuario> buscarPorId(UsuarioId id) {
        return Optional.ofNullable(findById(id.valor())).map(UsuarioJpaMapper::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return find("username", username).firstResultOptional()
                .map(UsuarioJpaMapper::toDomain);
    }
}
