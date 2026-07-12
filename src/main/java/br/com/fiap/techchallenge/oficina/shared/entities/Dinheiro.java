package br.com.fiap.techchallenge.oficina.shared.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Valor monetário em BRL. Imutável, escala fixa 2, não permite negativos.
 *
 * <p>Operações retornam novas instâncias.
 */
public final class Dinheiro {

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

    private final BigDecimal valor;

    private Dinheiro(BigDecimal valor) {
        this.valor = valor;
    }

    public static Dinheiro de(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Dinheiro não pode ser nulo");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("Dinheiro não pode ser negativo: " + valor);
        }
        return new Dinheiro(valor.setScale(2, RoundingMode.HALF_UP));
    }

    public static Dinheiro de(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Dinheiro não pode ser vazio");
        }
        return de(new BigDecimal(valor));
    }

    public static Dinheiro de(double valor) {
        return de(BigDecimal.valueOf(valor));
    }

    public Dinheiro somar(Dinheiro outro) {
        Objects.requireNonNull(outro, "outro");
        return new Dinheiro(this.valor.add(outro.valor));
    }

    public Dinheiro multiplicar(int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa: " + quantidade);
        }
        return new Dinheiro(this.valor.multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP));
    }

    public BigDecimal valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dinheiro that)) return false;
        return valor.compareTo(that.valor) == 0;
    }

    @Override
    public int hashCode() {
        return valor.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return "BRL " + valor.toPlainString();
    }
}
