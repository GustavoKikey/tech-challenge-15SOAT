package br.com.fiap.techchallenge.oficina.atendimento.entities;

import java.util.Objects;
import java.util.UUID;

public record VeiculoId(UUID valor) {

    public VeiculoId {
        Objects.requireNonNull(valor, "VeiculoId não pode ser nulo");
    }

    public static VeiculoId novo() {
        return new VeiculoId(UUID.randomUUID());
    }

    public static VeiculoId de(UUID valor) {
        return new VeiculoId(valor);
    }

    public static VeiculoId de(String valor) {
        return new VeiculoId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
