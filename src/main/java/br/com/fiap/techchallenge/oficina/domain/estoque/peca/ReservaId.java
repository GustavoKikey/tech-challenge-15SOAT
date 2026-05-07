package br.com.fiap.techchallenge.oficina.domain.estoque.peca;

import java.util.Objects;
import java.util.UUID;

public record ReservaId(UUID valor) {

    public ReservaId {
        Objects.requireNonNull(valor, "ReservaId não pode ser nulo");
    }

    public static ReservaId novo() {
        return new ReservaId(UUID.randomUUID());
    }

    public static ReservaId de(UUID valor) {
        return new ReservaId(valor);
    }

    public static ReservaId de(String valor) {
        return new ReservaId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
