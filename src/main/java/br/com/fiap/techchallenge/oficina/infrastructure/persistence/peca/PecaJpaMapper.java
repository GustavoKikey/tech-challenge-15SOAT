package br.com.fiap.techchallenge.oficina.infrastructure.persistence.peca;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Reserva;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.ReservaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.StatusReserva;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class PecaJpaMapper {

    private PecaJpaMapper() {}

    static PecaJpaEntity toEntity(Peca peca, PecaJpaEntity existente) {
        PecaJpaEntity entity = existente != null ? existente : new PecaJpaEntity();
        entity.id = peca.id().valor();
        entity.descricao = peca.descricao();
        entity.valorUnitario = peca.valorUnitario().valor();
        entity.quantidadeTotal = peca.quantidadeTotal();

        Map<UUID, ReservaJpaEntity> existentes = new HashMap<>();
        for (ReservaJpaEntity r : entity.reservas) {
            existentes.put(r.id, r);
        }
        for (Reserva reserva : peca.reservas()) {
            ReservaJpaEntity rEntity = existentes.get(reserva.id().valor());
            if (rEntity == null) {
                rEntity = new ReservaJpaEntity();
                rEntity.id = reserva.id().valor();
                rEntity.criadaEm = reserva.criadaEm();
                rEntity.quantidade = reserva.quantidade();
                rEntity.ordemServicoId = reserva.ordemServicoId().valor();
                rEntity.peca = entity;
                rEntity.status = reserva.status().name();
                entity.reservas.add(rEntity);
            } else {
                rEntity.status = reserva.status().name();
            }
        }
        return entity;
    }

    static Peca toDomain(PecaJpaEntity entity) {
        List<Reserva> reservas = entity.reservas.stream()
                .map(PecaJpaMapper::toDomainReserva)
                .toList();
        return Peca.reconstituir(
                PecaId.de(entity.id),
                entity.descricao,
                Dinheiro.de(entity.valorUnitario),
                entity.quantidadeTotal,
                reservas
        );
    }

    private static Reserva toDomainReserva(ReservaJpaEntity entity) {
        return Reserva.reconstituir(
                ReservaId.de(entity.id),
                OrdemServicoId.de(entity.ordemServicoId),
                entity.quantidade,
                StatusReserva.valueOf(entity.status),
                entity.criadaEm
        );
    }
}
