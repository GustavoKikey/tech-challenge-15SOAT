package br.com.fiap.techchallenge.oficina.infrastructure.persistence.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Placa;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VeiculoRepositoryImpl implements VeiculoRepository,
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
