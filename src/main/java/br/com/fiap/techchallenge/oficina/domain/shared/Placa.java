package br.com.fiap.techchallenge.oficina.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Placa veicular brasileira.
 *
 * <p>Aceita os dois formatos vigentes:
 * <ul>
 *     <li><b>Antigo</b>: {@code AAA-9999} (hífen opcional) — 3 letras, 4 dígitos.</li>
 *     <li><b>Mercosul</b>: {@code AAA9A99} — 3 letras, 1 dígito, 1 letra, 2 dígitos.</li>
 * </ul>
 * Normaliza removendo hífens/espaços e fazendo uppercase.
 */
public final class Placa {

    private static final Pattern ANTIGA   = Pattern.compile("^[A-Z]{3}\\d{4}$");
    private static final Pattern MERCOSUL = Pattern.compile("^[A-Z]{3}\\d[A-Z]\\d{2}$");

    private final String valor;

    private Placa(String valor) {
        this.valor = valor;
    }

    public static Placa de(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new PlacaInvalidaException("Placa não pode ser vazia");
        }
        String normalizada = entrada.replaceAll("[\\-\\s]", "").toUpperCase();
        if (!ANTIGA.matcher(normalizada).matches() && !MERCOSUL.matcher(normalizada).matches()) {
            throw new PlacaInvalidaException("Placa em formato inválido: " + entrada);
        }
        return new Placa(normalizada);
    }

    public String valor() {
        return valor;
    }

    public boolean isMercosul() {
        return MERCOSUL.matcher(valor).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Placa that)) return false;
        return valor.equals(that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
