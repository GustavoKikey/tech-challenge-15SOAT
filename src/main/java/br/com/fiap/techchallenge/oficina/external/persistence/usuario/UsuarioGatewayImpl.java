package br.com.fiap.techchallenge.oficina.external.persistence.usuario;

import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.entities.UsuarioId;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.UsuarioGateway;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UsuarioGatewayImpl implements UsuarioGateway,
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
