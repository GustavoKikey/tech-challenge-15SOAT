package br.com.fiap.techchallenge.oficina.external.persistence.veiculo;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;

final class VeiculoJpaMapper {

    private VeiculoJpaMapper() {}

    static VeiculoJpaEntity toEntity(Veiculo veiculo, VeiculoJpaEntity existente) {
        VeiculoJpaEntity entity = existente != null ? existente : new VeiculoJpaEntity();
        entity.id = veiculo.id().valor();
        entity.placa = veiculo.placa().valor();
        entity.marca = veiculo.marca();
        entity.modelo = veiculo.modelo();
        entity.ano = veiculo.ano();
        entity.clienteId = veiculo.clienteId().valor();
        return entity;
    }

    static Veiculo toDomain(VeiculoJpaEntity entity) {
        return Veiculo.reconstituir(
                VeiculoId.de(entity.id),
                Placa.de(entity.placa),
                entity.marca,
                entity.modelo,
                entity.ano,
                ClienteId.de(entity.clienteId)
        );
    }
}
