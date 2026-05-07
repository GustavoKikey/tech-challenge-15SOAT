package br.com.fiap.techchallenge.oficina.infrastructure.persistence.servico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ServicoRepositoryImpl implements ServicoRepository,
        PanacheRepositoryBase<ServicoJpaEntity, UUID> {

    @Override
    public Servico salvar(Servico servico) {
        UUID id = servico.id().valor();
        ServicoJpaEntity existente = findById(id);
        ServicoJpaEntity entity = ServicoJpaMapper.toEntity(servico, existente);
        if (existente == null) {
            persist(entity);
        }
        return ServicoJpaMapper.toDomain(entity);
    }

    @Override
    public Optional<Servico> buscarPorId(ServicoId id) {
        ServicoJpaEntity entity = findById(id.valor());
        return Optional.ofNullable(entity).map(ServicoJpaMapper::toDomain);
    }

    @Override
    public List<Servico> listar() {
        return listAll().stream().map(ServicoJpaMapper::toDomain).toList();
    }

    @Override
    public void remover(ServicoId id) {
        deleteById(id.valor());
    }
}
