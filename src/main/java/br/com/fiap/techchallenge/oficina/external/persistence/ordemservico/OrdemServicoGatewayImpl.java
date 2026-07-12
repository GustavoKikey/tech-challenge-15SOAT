package br.com.fiap.techchallenge.oficina.external.persistence.ordemservico;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrdemServicoGatewayImpl implements OrdemServicoGateway,
        PanacheRepositoryBase<OrdemServicoJpaEntity, UUID> {

    @Override
    public OrdemServico salvar(OrdemServico os) {
        UUID id = os.id().valor();
        OrdemServicoJpaEntity existente = findById(id);
        OrdemServicoJpaEntity entity = OrdemServicoJpaMapper.toEntity(os, existente);
        if (existente == null) {
            persist(entity);
        }
        return OrdemServicoJpaMapper.toDomain(entity);
    }

    @Override
    public Optional<OrdemServico> buscarPorId(OrdemServicoId id) {
        OrdemServicoJpaEntity entity = findById(id.valor());
        return Optional.ofNullable(entity).map(OrdemServicoJpaMapper::toDomain);
    }

    @Override
    public Optional<OrdemServico> buscarPorIdComLock(OrdemServicoId id) {
        OrdemServicoJpaEntity entity = findById(id.valor(), LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(entity).map(OrdemServicoJpaMapper::toDomain);
    }

    @Override
    public List<OrdemServico> listar(Filtro filtro) {
        StringBuilder query = new StringBuilder("1=1");
        Parameters params = new Parameters();
        if (filtro.status() != null) {
            query.append(" and status = :status");
            params.and("status", filtro.status().name());
        }
        if (filtro.clienteId() != null) {
            query.append(" and clienteId = :clienteId");
            params.and("clienteId", filtro.clienteId().valor());
        }
        if (filtro.veiculoId() != null) {
            query.append(" and veiculoId = :veiculoId");
            params.and("veiculoId", filtro.veiculoId().valor());
        }
        List<OrdemServicoJpaEntity> entities = find(query.toString(), params).list();
        List<OrdemServico> result = new ArrayList<>(entities.size());
        for (OrdemServicoJpaEntity e : entities) {
            result.add(OrdemServicoJpaMapper.toDomain(e));
        }
        return result;
    }

    @Override
    public List<OrdemServico> listarPorCliente(ClienteId clienteId) {
        return list("clienteId", clienteId.valor()).stream()
                .map(OrdemServicoJpaMapper::toDomain)
                .toList();
    }
}
