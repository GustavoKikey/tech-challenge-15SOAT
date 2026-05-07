package br.com.fiap.techchallenge.oficina.infrastructure.persistence.peca;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PecaRepositoryImpl implements PecaRepository,
        PanacheRepositoryBase<PecaJpaEntity, UUID> {

    @Override
    public Peca salvar(Peca peca) {
        UUID id = peca.id().valor();
        PecaJpaEntity existente = findById(id);
        PecaJpaEntity entity = PecaJpaMapper.toEntity(peca, existente);
        if (existente == null) {
            persist(entity);
        }
        return PecaJpaMapper.toDomain(entity);
    }

    @Override
    public Optional<Peca> buscarPorId(PecaId id) {
        PecaJpaEntity entity = findById(id.valor());
        return Optional.ofNullable(entity).map(PecaJpaMapper::toDomain);
    }

    @Override
    public Optional<Peca> buscarPorIdComLock(PecaId id) {
        PecaJpaEntity entity = findById(id.valor(), LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(entity).map(PecaJpaMapper::toDomain);
    }

    @Override
    public List<Peca> listar() {
        return listAll().stream().map(PecaJpaMapper::toDomain).toList();
    }

    @Override
    public void remover(PecaId id) {
        deleteById(id.valor());
    }
}
