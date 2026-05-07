package br.com.fiap.techchallenge.oficina.domain.shared;

import java.util.Objects;

/**
 * Documento fiscal — CPF (11 dígitos) ou CNPJ (14 dígitos).
 *
 * <p>Imutável. Normaliza removendo máscaras (".", "-", "/", " "), valida tamanho e
 * dígitos verificadores. {@link DocumentoInvalidoException} é lançada para qualquer
 * entrada que não satisfaça as regras.
 */
public final class Documento {

    public enum Tipo { CPF, CNPJ }

    private final String numero; // só dígitos
    private final Tipo tipo;

    private Documento(String numero, Tipo tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    public static Documento de(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new DocumentoInvalidoException("Documento não pode ser vazio");
        }
        String digitos = entrada.replaceAll("[\\.\\-/\\s]", "");
        if (!digitos.chars().allMatch(Character::isDigit)) {
            throw new DocumentoInvalidoException("Documento contém caracteres inválidos: " + entrada);
        }
        return switch (digitos.length()) {
            case 11 -> validarCpf(digitos);
            case 14 -> validarCnpj(digitos);
            default -> throw new DocumentoInvalidoException(
                    "Documento deve ter 11 (CPF) ou 14 (CNPJ) dígitos: " + entrada);
        };
    }

    private static Documento validarCpf(String d) {
        if (todosDigitosIguais(d)) {
            throw new DocumentoInvalidoException("CPF inválido: " + d);
        }
        int dv1 = calcularDv(d, 9, 10);
        int dv2 = calcularDv(d, 10, 11);
        if (dv1 != Character.getNumericValue(d.charAt(9)) ||
            dv2 != Character.getNumericValue(d.charAt(10))) {
            throw new DocumentoInvalidoException("CPF com dígito verificador inválido: " + d);
        }
        return new Documento(d, Tipo.CPF);
    }

    private static Documento validarCnpj(String d) {
        if (todosDigitosIguais(d)) {
            throw new DocumentoInvalidoException("CNPJ inválido: " + d);
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int dv1 = calcularDvCnpj(d, pesos1);
        int dv2 = calcularDvCnpj(d, pesos2);
        if (dv1 != Character.getNumericValue(d.charAt(12)) ||
            dv2 != Character.getNumericValue(d.charAt(13))) {
            throw new DocumentoInvalidoException("CNPJ com dígito verificador inválido: " + d);
        }
        return new Documento(d, Tipo.CNPJ);
    }

    private static int calcularDv(String digitos, int tamanho, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int calcularDvCnpj(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosDigitosIguais(String d) {
        char primeiro = d.charAt(0);
        for (int i = 1; i < d.length(); i++) {
            if (d.charAt(i) != primeiro) return false;
        }
        return true;
    }

    public String numero() {
        return numero;
    }

    public Tipo tipo() {
        return tipo;
    }

    public boolean isCpf() {
        return tipo == Tipo.CPF;
    }

    public boolean isCnpj() {
        return tipo == Tipo.CNPJ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Documento that)) return false;
        return numero.equals(that.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return tipo + ":" + numero;
    }
}
