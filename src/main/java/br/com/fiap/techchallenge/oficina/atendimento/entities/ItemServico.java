package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.util.Objects;
import java.util.UUID;

/**
 * Item de serviço cobrado em uma OS. Entidade interna do agregado {@link OrdemServico}.
 *
 * <p>Carrega seu próprio id (para permitir remoção pelo endpoint
 * {@code DELETE /ordens-servico/{id}/servicos/{itemId}}) e o valor cobrado, que
 * pode divergir do valor base do catálogo de serviço (negociação).
 */
public final class ItemServico {

    private final UUID id;
    private final ServicoId servicoId;
    private final Dinheiro valorCobrado;

    private ItemServico(UUID id, ServicoId servicoId, Dinheiro valorCobrado) {
        this.id = Objects.requireNonNull(id, "id");
        this.servicoId = Objects.requireNonNull(servicoId, "servicoId");
        this.valorCobrado = Objects.requireNonNull(valorCobrado, "valorCobrado");
    }

    public static ItemServico novo(ServicoId servicoId, Dinheiro valorCobrado) {
        return new ItemServico(UUID.randomUUID(), servicoId, valorCobrado);
    }

    public static ItemServico reconstituir(UUID id, ServicoId servicoId, Dinheiro valorCobrado) {
        return new ItemServico(id, servicoId, valorCobrado);
    }

    public UUID id()                { return id; }
    public ServicoId servicoId()    { return servicoId; }
    public Dinheiro valorCobrado()  { return valorCobrado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemServico that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
