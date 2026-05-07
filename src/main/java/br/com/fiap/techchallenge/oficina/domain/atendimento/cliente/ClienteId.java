package br.com.fiap.techchallenge.oficina.domain.atendimento.cliente;

import java.util.Objects;
import java.util.UUID;

public record ClienteId(UUID valor) {

    public ClienteId {
        Objects.requireNonNull(valor, "ClienteId não pode ser nulo");
    }

    public static ClienteId novo() {
        return new ClienteId(UUID.randomUUID());
    }

    public static ClienteId de(UUID valor) {
        return new ClienteId(valor);
    }

    public static ClienteId de(String valor) {
        return new ClienteId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
