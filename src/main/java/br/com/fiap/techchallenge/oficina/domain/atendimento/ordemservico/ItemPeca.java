package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.ReservaId;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;

import java.util.Objects;
import java.util.UUID;

/**
 * Item de peça cobrado em uma OS. Entidade interna do agregado {@link OrdemServico}.
 *
 * <p>Carrega o id da reserva criada no BC Estoque ({@code reservaId}) — preenchido
 * pelo use case que orquestra a reserva no momento da geração do orçamento.
 * Antes disso, fica nulo. Mantém o {@code valorUnitario} cobrado para registro
 * histórico (o valor de catálogo da peça pode mudar depois).
 */
public final class ItemPeca {

    private final UUID id;
    private final PecaId pecaId;
    private final int quantidade;
    private final Dinheiro valorUnitario;
    private ReservaId reservaId;

    private ItemPeca(UUID id, PecaId pecaId, int quantidade, Dinheiro valorUnitario,
                     ReservaId reservaId) {
        this.id = Objects.requireNonNull(id, "id");
        this.pecaId = Objects.requireNonNull(pecaId, "pecaId");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva: " + quantidade);
        }
        this.quantidade = quantidade;
        this.valorUnitario = Objects.requireNonNull(valorUnitario, "valorUnitario");
        this.reservaId = reservaId;
    }

    public static ItemPeca novo(PecaId pecaId, int quantidade, Dinheiro valorUnitario) {
        return new ItemPeca(UUID.randomUUID(), pecaId, quantidade, valorUnitario, null);
    }

    public static ItemPeca reconstituir(UUID id, PecaId pecaId, int quantidade,
                                        Dinheiro valorUnitario, ReservaId reservaId) {
        return new ItemPeca(id, pecaId, quantidade, valorUnitario, reservaId);
    }

    void registrarReserva(ReservaId reservaId) {
        if (this.reservaId != null) {
            throw new IllegalStateException("Item já possui reserva registrada.");
        }
        this.reservaId = Objects.requireNonNull(reservaId, "reservaId");
    }

    public Dinheiro subtotal() {
        return valorUnitario.multiplicar(quantidade);
    }

    public boolean possuiReserva() {
        return reservaId != null;
    }

    public UUID id()                { return id; }
    public PecaId pecaId()          { return pecaId; }
    public int quantidade()         { return quantidade; }
    public Dinheiro valorUnitario() { return valorUnitario; }
    public ReservaId reservaId()    { return reservaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemPeca that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
