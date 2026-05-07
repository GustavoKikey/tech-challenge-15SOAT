package br.com.fiap.techchallenge.oficina.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DinheiroTest {

    @Test
    void deveCriarComBigDecimalNormalizandoEscala() {
        Dinheiro d = Dinheiro.de(new BigDecimal("10.5"));
        assertEquals(new BigDecimal("10.50"), d.valor());
    }

    @Test
    void deveCriarAPartirDeStringEDouble() {
        assertEquals(new BigDecimal("12.34"), Dinheiro.de("12.34").valor());
        assertEquals(new BigDecimal("9.99"), Dinheiro.de(9.99d).valor());
    }

    @Test
    void deveArredondarParaCima() {
        assertEquals(new BigDecimal("10.46"), Dinheiro.de("10.455").valor());
    }

    @Test
    void zeroEUmValorValido() {
        assertEquals(new BigDecimal("0.00"), Dinheiro.de("0").valor());
        assertEquals(Dinheiro.ZERO, Dinheiro.de("0.00"));
    }

    @Test
    void naoDeveAceitarNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Dinheiro.de("-0.01"));
        assertThrows(IllegalArgumentException.class, () -> Dinheiro.de(new BigDecimal("-10")));
    }

    @Test
    void naoDeveAceitarNuloNemVazio() {
        assertThrows(IllegalArgumentException.class, () -> Dinheiro.de((BigDecimal) null));
        assertThrows(IllegalArgumentException.class, () -> Dinheiro.de((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dinheiro.de("   "));
    }

    @Test
    void somarDeveAcumular() {
        Dinheiro a = Dinheiro.de("10.00");
        Dinheiro b = Dinheiro.de("0.50");
        assertEquals(Dinheiro.de("10.50"), a.somar(b));
    }

    @Test
    void somarComNuloLanca() {
        assertThrows(NullPointerException.class, () -> Dinheiro.de("1").somar(null));
    }

    @Test
    void multiplicarPorQuantidade() {
        Dinheiro d = Dinheiro.de("12.50");
        assertEquals(Dinheiro.de("37.50"), d.multiplicar(3));
        assertEquals(Dinheiro.ZERO, d.multiplicar(0));
    }

    @Test
    void multiplicarPorNegativoLanca() {
        assertThrows(IllegalArgumentException.class, () -> Dinheiro.de("1").multiplicar(-1));
    }

    @Test
    void equalsEHashCodeIgnoramTrailingZeros() {
        Dinheiro a = Dinheiro.de("10");
        Dinheiro b = Dinheiro.de("10.00");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Dinheiro.de("10.01"));
        assertNotEquals(a, "10.00");
        assertEquals(a, a);
    }

    @Test
    void toStringContemMoedaEValor() {
        assertEquals("BRL 12.34", Dinheiro.de("12.34").toString());
    }
}
