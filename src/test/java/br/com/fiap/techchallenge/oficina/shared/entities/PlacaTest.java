package br.com.fiap.techchallenge.oficina.shared.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PlacaTest {

    @ParameterizedTest
    @CsvSource({
            "ABC-1234, ABC1234",
            "abc1234, ABC1234",
            "ABC 1234, ABC1234",
            "  XYZ-9876  , XYZ9876"
    })
    void deveAceitarFormatoAntigoNormalizando(String entrada, String esperada) {
        Placa p = Placa.de(entrada);
        assertEquals(esperada, p.valor());
        assertFalse(p.isMercosul());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1D23", "abc1d23", "BRA0S17"})
    void deveAceitarFormatoMercosul(String entrada) {
        Placa p = Placa.de(entrada);
        assertTrue(p.isMercosul());
        assertEquals(entrada.toUpperCase(), p.valor());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "AB-1234",      // só 2 letras
            "ABCD1234",     // 4 letras
            "ABC12345",     // 5 dígitos
            "12C-3456",     // começa com número
            "ABC1A2",       // mercosul mal formado
            "ABC1AA2",      // mercosul mal formado
            ""
    })
    void deveRejeitarPlacaInvalida(String entrada) {
        assertThrows(PlacaInvalidaException.class, () -> Placa.de(entrada));
    }

    @Test
    void deveRejeitarNulo() {
        assertThrows(PlacaInvalidaException.class, () -> Placa.de(null));
    }

    @Test
    void equalsEHashCodeBaseiamSeNoValor() {
        Placa a = Placa.de("ABC-1234");
        Placa b = Placa.de("abc1234");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Placa.de("XYZ9876"));
        assertNotEquals(a, "ABC1234");
        assertEquals(a, a);
    }

    @Test
    void toStringRetornaValorNormalizado() {
        assertEquals("ABC1234", Placa.de("abc-1234").toString());
    }
}
