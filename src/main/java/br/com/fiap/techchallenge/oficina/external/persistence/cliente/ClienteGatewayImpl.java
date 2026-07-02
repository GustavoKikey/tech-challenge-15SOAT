package br.com.fiap.techchallenge.oficina.external.persistence.cliente;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Documento;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ClienteGatewayImpl implements ClienteGateway,
        PanacheRepositoryBase<ClienteJpaEntity, UUID> {

    @Override
    public Cliente salvar(Cliente cliente) {
        UUID id = cliente.id().valor();
        ClienteJpaEntity existente = findById(id);
        ClienteJpaEntity entity = ClienteJpaMapper.toEntity(cliente, existente);
        if (existente == null) {
            persist(entity);
        }
        return ClienteJpaMapper.toDomain(entity);
    }

    @Override
    public Optional<Cliente> buscarPorId(ClienteId id) {
        ClienteJpaEntity entity = findById(id.valor());
        return Optional.ofNullable(entity).map(ClienteJpaMapper::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(Documento documento) {
        return find("documento", documento.numero())
                .firstResultOptional()
                .map(ClienteJpaMapper::toDomain);
    }

    @Override
    public List<Cliente> listar() {
        return listAll().stream().map(ClienteJpaMapper::toDomain).toList();
    }

    @Override
    public void remover(ClienteId id) {
        deleteById(id.valor());
    }
}
