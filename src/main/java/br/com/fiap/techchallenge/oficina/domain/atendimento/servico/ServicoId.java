package br.com.fiap.techchallenge.oficina.domain.atendimento.servico;

import java.util.Objects;
import java.util.UUID;

public record ServicoId(UUID valor) {

    public ServicoId {
        Objects.requireNonNull(valor, "ServicoId não pode ser nulo");
    }

    public static ServicoId novo() {
        return new ServicoId(UUID.randomUUID());
    }

    public static ServicoId de(UUID valor) {
        return new ServicoId(valor);
    }

    public static ServicoId de(String valor) {
        return new ServicoId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
