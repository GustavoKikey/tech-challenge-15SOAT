package br.com.fiap.techchallenge.oficina.domain.estoque.peca;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidade interna do agregado {@link Peca}.
 *
 * <p>Não é raiz: criada e mutada apenas por {@link Peca}. Métodos que alteram estado
 * são package-private para impedir manipulação direta de fora do agregado.
 */
public class Reserva {

    private final ReservaId id;
    private final OrdemServicoId ordemServicoId;
    private final int quantidade;
    private StatusReserva status;
    private final OffsetDateTime criadaEm;

    private Reserva(ReservaId id, OrdemServicoId ordemServicoId, int quantidade,
                    StatusReserva status, OffsetDateTime criadaEm) {
        this.id = Objects.requireNonNull(id, "id");
        this.ordemServicoId = Objects.requireNonNull(ordemServicoId, "ordemServicoId");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade da reserva deve ser positiva: " + quantidade);
        }
        this.quantidade = quantidade;
        this.status = Objects.requireNonNull(status, "status");
        this.criadaEm = Objects.requireNonNull(criadaEm, "criadaEm");
    }

    static Reserva nova(OrdemServicoId ordemServicoId, int quantidade) {
        return new Reserva(ReservaId.novo(), ordemServicoId, quantidade,
                StatusReserva.ATIVA, OffsetDateTime.now());
    }

    public static Reserva reconstituir(ReservaId id, OrdemServicoId ordemServicoId, int quantidade,
                                       StatusReserva status, OffsetDateTime criadaEm) {
        return new Reserva(id, ordemServicoId, quantidade, status, criadaEm);
    }

    void marcarBaixada() {
        exigirStatus(StatusReserva.ATIVA, "baixada");
        this.status = StatusReserva.BAIXADA;
    }

    void cancelar() {
        exigirStatus(StatusReserva.ATIVA, "cancelada");
        this.status = StatusReserva.CANCELADA;
    }

    private void exigirStatus(StatusReserva esperado, String acao) {
        if (this.status != esperado) {
            throw new ReservaInvalidaException(
                    "Reserva " + id + " não pode ser " + acao + ": status atual " + this.status);
        }
    }

    public ReservaId id()                   { return id; }
    public OrdemServicoId ordemServicoId()  { return ordemServicoId; }
    public int quantidade()                 { return quantidade; }
    public StatusReserva status()           { return status; }
    public OffsetDateTime criadaEm()        { return criadaEm; }

    public boolean ativa() {
        return status == StatusReserva.ATIVA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
