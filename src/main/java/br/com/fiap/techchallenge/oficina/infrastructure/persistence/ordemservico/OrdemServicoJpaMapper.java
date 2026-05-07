package br.com.fiap.techchallenge.oficina.infrastructure.persistence.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemPeca;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.Orcamento;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.StatusOS;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.ReservaId;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class OrdemServicoJpaMapper {

    private OrdemServicoJpaMapper() {}

    static OrdemServicoJpaEntity toEntity(OrdemServico os, OrdemServicoJpaEntity existente) {
        OrdemServicoJpaEntity entity = existente != null ? existente : new OrdemServicoJpaEntity();
        entity.id = os.id().valor();
        entity.clienteId = os.clienteId().valor();
        entity.veiculoId = os.veiculoId().valor();
        entity.status = os.status().name();
        entity.criadaEm = os.criadaEm();
        entity.diagnosticoIniciadoEm = os.diagnosticoIniciadoEm();
        entity.execucaoIniciadaEm = os.execucaoIniciadaEm();
        entity.finalizadaEm = os.finalizadaEm();
        entity.entregueEm = os.entregueEm();

        Orcamento orcamento = os.orcamento();
        if (orcamento != null) {
            entity.valorTotal = orcamento.valorTotal().valor();
            entity.orcamentoGeradoEm = orcamento.geradoEm();
            entity.orcamentoAprovadoEm = orcamento.aprovadoEm();
        } else {
            entity.valorTotal = null;
            entity.orcamentoGeradoEm = null;
            entity.orcamentoAprovadoEm = null;
        }

        sincronizarItensServico(os.itensServico(), entity);
        sincronizarItensPeca(os.itensPeca(), entity);
        return entity;
    }

    private static void sincronizarItensServico(List<ItemServico> itens,
                                                OrdemServicoJpaEntity entity) {
        Map<UUID, OsItemServicoJpaEntity> existentes = new HashMap<>();
        for (OsItemServicoJpaEntity it : entity.itensServico) {
            existentes.put(it.id, it);
        }
        entity.itensServico.clear();
        for (ItemServico item : itens) {
            OsItemServicoJpaEntity it = existentes.get(item.id());
            if (it == null) {
                it = new OsItemServicoJpaEntity();
                it.id = item.id();
                it.servicoId = item.servicoId().valor();
            }
            it.ordemServico = entity;
            it.valorCobrado = item.valorCobrado().valor();
            entity.itensServico.add(it);
        }
    }

    private static void sincronizarItensPeca(List<ItemPeca> itens,
                                             OrdemServicoJpaEntity entity) {
        Map<UUID, OsItemPecaJpaEntity> existentes = new HashMap<>();
        for (OsItemPecaJpaEntity it : entity.itensPeca) {
            existentes.put(it.id, it);
        }
        entity.itensPeca.clear();
        for (ItemPeca item : itens) {
            OsItemPecaJpaEntity it = existentes.get(item.id());
            if (it == null) {
                it = new OsItemPecaJpaEntity();
                it.id = item.id();
                it.pecaId = item.pecaId().valor();
                it.quantidade = item.quantidade();
                it.valorUnitario = item.valorUnitario().valor();
            }
            it.ordemServico = entity;
            it.reservaId = item.reservaId() == null ? null : item.reservaId().valor();
            entity.itensPeca.add(it);
        }
    }

    static OrdemServico toDomain(OrdemServicoJpaEntity entity) {
        List<ItemServico> servicos = entity.itensServico.stream()
                .map(OrdemServicoJpaMapper::toDomainItemServico)
                .toList();
        List<ItemPeca> pecas = entity.itensPeca.stream()
                .map(OrdemServicoJpaMapper::toDomainItemPeca)
                .toList();
        Orcamento orcamento = null;
        if (entity.orcamentoGeradoEm != null && entity.valorTotal != null) {
            orcamento = Orcamento.reconstituir(
                    Dinheiro.de(entity.valorTotal),
                    entity.orcamentoGeradoEm,
                    entity.orcamentoAprovadoEm);
        }
        return OrdemServico.reconstituir(
                OrdemServicoId.de(entity.id),
                ClienteId.de(entity.clienteId),
                VeiculoId.de(entity.veiculoId),
                StatusOS.valueOf(entity.status),
                servicos,
                pecas,
                orcamento,
                entity.criadaEm,
                entity.diagnosticoIniciadoEm,
                entity.execucaoIniciadaEm,
                entity.finalizadaEm,
                entity.entregueEm
        );
    }

    private static ItemServico toDomainItemServico(OsItemServicoJpaEntity it) {
        return ItemServico.reconstituir(
                it.id,
                ServicoId.de(it.servicoId),
                Dinheiro.de(it.valorCobrado));
    }

    private static ItemPeca toDomainItemPeca(OsItemPecaJpaEntity it) {
        ReservaId reserva = it.reservaId == null ? null : ReservaId.de(it.reservaId);
        return ItemPeca.reconstituir(
                it.id,
                PecaId.de(it.pecaId),
                it.quantidade,
                Dinheiro.de(it.valorUnitario),
                reserva);
    }
}
