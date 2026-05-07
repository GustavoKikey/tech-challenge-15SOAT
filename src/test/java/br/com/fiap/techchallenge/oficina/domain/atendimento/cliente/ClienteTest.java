package br.com.fiap.techchallenge.oficina.domain.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.shared.Documento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    private static final Documento DOC = Documento.de("52998224725");

    @Test
    void deveCriarComIdGeradoENomeNormalizado() {
        Cliente c = Cliente.novo("  João Silva  ", DOC, "joao@x.com", "11999990000");
        assertNotNull(c.id());
        assertEquals("João Silva", c.nome());
        assertEquals(DOC, c.documento());
    }

    @Test
    void naoDeveCriarComNomeVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> Cliente.novo("", DOC, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> Cliente.novo("   ", DOC, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> Cliente.novo(null, DOC, null, null));
    }

    @Test
    void naoDeveCriarComDocumentoNulo() {
        assertThrows(NullPointerException.class,
                () -> Cliente.novo("João", null, null, null));
    }

    @Test
    void renomearAtualizaNome() {
        Cliente c = Cliente.novo("João", DOC, null, null);
        c.renomear("Maria");
        assertEquals("Maria", c.nome());
    }

    @Test
    void renomearComVazioFalha() {
        Cliente c = Cliente.novo("João", DOC, null, null);
        assertThrows(IllegalArgumentException.class, () -> c.renomear(""));
    }

    @Test
    void alterarContatoSubstituiCampos() {
        Cliente c = Cliente.novo("João", DOC, "a@x.com", "111");
        c.alterarContato("b@y.com", "222");
        assertEquals("b@y.com", c.email());
        assertEquals("222", c.telefone());
    }

    @Test
    void reconstituirNaoRegeraId() {
        ClienteId id = ClienteId.novo();
        Cliente c = Cliente.reconstituir(id, "João", DOC, null, null);
        assertSame(id, c.id());
    }

    @Test
    void equalsEHashCodePorId() {
        ClienteId id = ClienteId.novo();
        Cliente a = Cliente.reconstituir(id, "João", DOC, null, null);
        Cliente b = Cliente.reconstituir(id, "Outro", DOC, null, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Cliente.novo("João", DOC, null, null));
        assertNotEquals(a, "string");
        assertEquals(a, a);
    }

    @Test
    void clienteIdRejeitaNuloEAceitaUuid() {
        assertThrows(NullPointerException.class, () -> new ClienteId(null));
        assertNotNull(ClienteId.de("00000000-0000-0000-0000-000000000001"));
    }
}
