package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

import java.util.Objects;
import java.util.UUID;

public record UsuarioId(UUID valor) {

    public UsuarioId {
        Objects.requireNonNull(valor, "UsuarioId não pode ser nulo");
    }

    public static UsuarioId novo() {
        return new UsuarioId(UUID.randomUUID());
    }

    public static UsuarioId de(UUID valor) {
        return new UsuarioId(valor);
    }

    public static UsuarioId de(String valor) {
        return new UsuarioId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
