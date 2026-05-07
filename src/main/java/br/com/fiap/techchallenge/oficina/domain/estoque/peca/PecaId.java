package br.com.fiap.techchallenge.oficina.domain.estoque.peca;

import java.util.Objects;
import java.util.UUID;

public record PecaId(UUID valor) {

    public PecaId {
        Objects.requireNonNull(valor, "PecaId não pode ser nulo");
    }

    public static PecaId novo() {
        return new PecaId(UUID.randomUUID());
    }

    public static PecaId de(UUID valor) {
        return new PecaId(valor);
    }

    public static PecaId de(String valor) {
        return new PecaId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
