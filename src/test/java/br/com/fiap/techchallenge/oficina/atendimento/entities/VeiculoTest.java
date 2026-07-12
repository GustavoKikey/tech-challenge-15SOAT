package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class VeiculoTest {

    private static final Placa PLACA = Placa.de("ABC1D23");
    private final ClienteId dono = ClienteId.novo();

    @Test
    void deveCriarVeiculoValido() {
        Veiculo v = Veiculo.novo(PLACA, "Fiat", "Uno", 2010, dono);
        assertNotNull(v.id());
        assertEquals(PLACA, v.placa());
        assertEquals("Fiat", v.marca());
        assertEquals(2010, v.ano());
        assertSame(dono, v.clienteId());
    }

    @Test
    void naoDeveCriarComCamposVazios() {
        assertThrows(IllegalArgumentException.class,
                () -> Veiculo.novo(PLACA, "", "Uno", 2010, dono));
        assertThrows(IllegalArgumentException.class,
                () -> Veiculo.novo(PLACA, "Fiat", "  ", 2010, dono));
    }

    @Test
    void naoDeveCriarComAnoForaDoIntervalo() {
        int futuro = Year.now().getValue() + 5;
        assertThrows(IllegalArgumentException.class,
                () -> Veiculo.novo(PLACA, "Fiat", "Uno", 1899, dono));
        assertThrows(IllegalArgumentException.class,
                () -> Veiculo.novo(PLACA, "Fiat", "Uno", futuro, dono));
    }

    @Test
    void aceitaAnoLimiteSuperior() {
        int proximo = Year.now().getValue() + 1;
        Veiculo v = Veiculo.novo(PLACA, "Fiat", "Uno", proximo, dono);
        assertEquals(proximo, v.ano());
    }

    @Test
    void naoDeveCriarComPlacaOuClienteNulos() {
        assertThrows(NullPointerException.class,
                () -> Veiculo.novo(null, "Fiat", "Uno", 2010, dono));
        assertThrows(NullPointerException.class,
                () -> Veiculo.novo(PLACA, "Fiat", "Uno", 2010, null));
    }

    @Test
    void atualizarFichaTecnica() {
        Veiculo v = Veiculo.novo(PLACA, "Fiat", "Uno", 2010, dono);
        v.atualizarFichaTecnica("Volkswagen", "Gol", 2018);
        assertEquals("Volkswagen", v.marca());
        assertEquals("Gol", v.modelo());
        assertEquals(2018, v.ano());
    }

    @Test
    void transferirParaNovoDono() {
        Veiculo v = Veiculo.novo(PLACA, "Fiat", "Uno", 2010, dono);
        ClienteId novo = ClienteId.novo();
        v.transferirPara(novo);
        assertSame(novo, v.clienteId());
        assertThrows(NullPointerException.class, () -> v.transferirPara(null));
    }

    @Test
    void reconstituirNaoRegeraId() {
        VeiculoId id = VeiculoId.novo();
        Veiculo v = Veiculo.reconstituir(id, PLACA, "Fiat", "Uno", 2010, dono);
        assertSame(id, v.id());
    }

    @Test
    void equalsEHashCodePorId() {
        VeiculoId id = VeiculoId.novo();
        Veiculo a = Veiculo.reconstituir(id, PLACA, "Fiat", "Uno", 2010, dono);
        Veiculo b = Veiculo.reconstituir(id, PLACA, "VW", "Gol", 2020, dono);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Veiculo.novo(PLACA, "Fiat", "Uno", 2010, dono));
        assertNotEquals(a, "string");
        assertEquals(a, a);
    }

    @Test
    void veiculoIdRejeitaNulo() {
        assertThrows(NullPointerException.class, () -> new VeiculoId(null));
    }
}
