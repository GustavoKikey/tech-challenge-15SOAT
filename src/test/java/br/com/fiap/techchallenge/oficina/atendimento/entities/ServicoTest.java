package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServicoTest {

    @Test
    void deveCriarServicoValido() {
        Servico s = Servico.novo("  Troca de óleo  ", Dinheiro.de("99.90"));
        assertNotNull(s.id());
        assertEquals("Troca de óleo", s.descricao());
        assertEquals(Dinheiro.de("99.90"), s.valorBase());
    }

    @Test
    void naoDeveCriarSemDescricao() {
        assertThrows(IllegalArgumentException.class,
                () -> Servico.novo("", Dinheiro.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> Servico.novo(null, Dinheiro.ZERO));
    }

    @Test
    void naoDeveCriarSemValorBase() {
        assertThrows(NullPointerException.class,
                () -> Servico.novo("Troca de óleo", null));
    }

    @Test
    void alterarSubstituiDescricaoEValor() {
        Servico s = Servico.novo("Troca de óleo", Dinheiro.de("99.90"));
        s.alterar("Troca de óleo + filtro", Dinheiro.de("149.90"));
        assertEquals("Troca de óleo + filtro", s.descricao());
        assertEquals(Dinheiro.de("149.90"), s.valorBase());
    }

    @Test
    void alterarValidaCampos() {
        Servico s = Servico.novo("Troca de óleo", Dinheiro.de("99.90"));
        assertThrows(IllegalArgumentException.class, () -> s.alterar("", Dinheiro.ZERO));
        assertThrows(NullPointerException.class, () -> s.alterar("ok", null));
    }

    @Test
    void reconstituirNaoRegeraId() {
        ServicoId id = ServicoId.novo();
        Servico s = Servico.reconstituir(id, "Troca de óleo", Dinheiro.de("99.90"));
        assertSame(id, s.id());
    }

    @Test
    void equalsEHashCodePorId() {
        ServicoId id = ServicoId.novo();
        Servico a = Servico.reconstituir(id, "x", Dinheiro.ZERO);
        Servico b = Servico.reconstituir(id, "y", Dinheiro.de("10"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Servico.novo("x", Dinheiro.ZERO));
        assertNotEquals(a, "string");
        assertEquals(a, a);
    }

    @Test
    void servicoIdRejeitaNulo() {
        assertThrows(NullPointerException.class, () -> new ServicoId(null));
    }
}
