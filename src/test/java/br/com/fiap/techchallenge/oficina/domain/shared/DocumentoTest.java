package br.com.fiap.techchallenge.oficina.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoTest {

    // ====== CPFs válidos (gerados/conhecidos) ======
    @ParameterizedTest
    @ValueSource(strings = {
            "529.982.247-25",      // CPF válido com máscara
            "52998224725",         // mesmo, sem máscara
            "111.444.777-35",      // outro válido
            "390.533.447-05"       // outro válido
    })
    void deveAceitarCpfValido(String entrada) {
        Documento doc = Documento.de(entrada);
        assertTrue(doc.isCpf());
        assertFalse(doc.isCnpj());
        assertEquals(11, doc.numero().length());
        assertTrue(doc.numero().chars().allMatch(Character::isDigit));
    }

    // ====== CNPJs válidos ======
    @ParameterizedTest
    @ValueSource(strings = {
            "11.222.333/0001-81",  // CNPJ válido com máscara
            "11222333000181",
            "04.252.011/0001-10"
    })
    void deveAceitarCnpjValido(String entrada) {
        Documento doc = Documento.de(entrada);
        assertTrue(doc.isCnpj());
        assertFalse(doc.isCpf());
        assertEquals(14, doc.numero().length());
    }

    // ====== Inválidos ======
    @ParameterizedTest
    @ValueSource(strings = {
            "111.111.111-11",     // todos iguais
            "00000000000",
            "12345678900",        // DV errado
            "123",                // tamanho inválido
            "abcdefghijk",        // não-dígitos
            "11.222.333/0001-00"  // CNPJ DV errado
    })
    void deveRejeitarDocumentosInvalidos(String entrada) {
        assertThrows(DocumentoInvalidoException.class, () -> Documento.de(entrada));
    }

    @Test
    void deveRejeitarNuloOuVazio() {
        assertThrows(DocumentoInvalidoException.class, () -> Documento.de(null));
        assertThrows(DocumentoInvalidoException.class, () -> Documento.de(""));
        assertThrows(DocumentoInvalidoException.class, () -> Documento.de("   "));
    }

    @Test
    void equalsEHashCodeBaseiamSeNoNumero() {
        Documento a = Documento.de("529.982.247-25");
        Documento b = Documento.de("52998224725");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Documento.de("11.222.333/0001-81"));
        assertNotEquals(a, "string-qualquer");
        assertEquals(a, a);
    }

    @Test
    void toStringIncluiTipoENumero() {
        assertTrue(Documento.de("52998224725").toString().contains("CPF"));
        assertTrue(Documento.de("11222333000181").toString().contains("CNPJ"));
    }
}
