package br.com.fiap.techchallenge.oficina.external.persistence.veiculo;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VeiculoGatewayImpl implements VeiculoGateway,
        PanacheRepositoryBase<VeiculoJpaEntity, UUID> {

    @Override
    public Veiculo salvar(Veiculo veiculo) {
        UUID id = veiculo.id().valor();
        VeiculoJpaEntity existente = findById(id);
        VeiculoJpaEntity entity = VeiculoJpaMapper.toEntity(veiculo, existente);
        if (existente == null) {
            persist(entity);
        }
        return VeiculoJpaMapper.toDomain(entity);
    }

    @Override
    public Optional<Veiculo> buscarPorId(VeiculoId id) {
        VeiculoJpaEntity entity = findById(id.valor());
        return Optional.ofNullable(entity).map(VeiculoJpaMapper::toDomain);
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(Placa placa) {
        return find("placa", placa.valor())
                .firstResultOptional()
                .map(VeiculoJpaMapper::toDomain);
    }

    @Override
    public List<Veiculo> listarPorCliente(ClienteId clienteId) {
        return list("clienteId", clienteId.valor()).stream()
                .map(VeiculoJpaMapper::toDomain).toList();
    }

    @Override
    public List<Veiculo> listar() {
        return listAll().stream().map(VeiculoJpaMapper::toDomain).toList();
    }

    @Override
    public void remover(VeiculoId id) {
        deleteById(id.valor());
    }
}
