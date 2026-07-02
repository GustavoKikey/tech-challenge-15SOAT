package br.com.fiap.techchallenge.oficina.seguranca.entities;

import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    /** Hasher fake reversível — só pra testar a lógica do agregado, sem BCrypt real. */
    private static final PasswordHasher FAKE_HASHER = new PasswordHasher() {
        @Override public String hash(String plain) { return "h:" + plain; }
        @Override public boolean matches(String plain, String hashed) {
            return hashed != null && hashed.equals("h:" + plain);
        }
    };

    @Test
    void criaUsuarioAtivoComCamposObrigatorios() {
        Usuario u = Usuario.novo("admin", "h:s3nha", Role.ADMINISTRADOR);
        assertNotNull(u.id());
        assertEquals("admin", u.username());
        assertEquals(Role.ADMINISTRADOR, u.role());
        assertTrue(u.ativo());
    }

    @Test
    void usernameVazioOuNuloFalha() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.novo("", "h:x", Role.ATENDENTE));
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.novo(null, "h:x", Role.ATENDENTE));
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.novo("   ", "h:x", Role.ATENDENTE));
    }

    @Test
    void senhaHashVaziaOuNulaFalha() {
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.novo("u", "", Role.ATENDENTE));
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.novo("u", null, Role.ATENDENTE));
    }

    @Test
    void roleNuloFalha() {
        assertThrows(NullPointerException.class,
                () -> Usuario.novo("u", "h:x", null));
    }

    @Test
    void verificarSenhaComSenhaCorreta() {
        Usuario u = Usuario.novo("admin", FAKE_HASHER.hash("segredo"), Role.ATENDENTE);
        assertTrue(u.verificarSenha("segredo", FAKE_HASHER));
    }

    @Test
    void verificarSenhaComSenhaErrada() {
        Usuario u = Usuario.novo("admin", FAKE_HASHER.hash("segredo"), Role.ATENDENTE);
        assertFalse(u.verificarSenha("errada", FAKE_HASHER));
    }

    @Test
    void verificarSenhaUsuarioInativoSempreFalha() {
        Usuario u = Usuario.novo("admin", FAKE_HASHER.hash("segredo"), Role.ATENDENTE);
        u.desativar();
        assertFalse(u.verificarSenha("segredo", FAKE_HASHER));
    }

    @Test
    void verificarSenhaPlainTextNuloOuVazioFalhaSemBater() {
        Usuario u = Usuario.novo("admin", FAKE_HASHER.hash("segredo"), Role.ATENDENTE);
        assertFalse(u.verificarSenha(null, FAKE_HASHER));
        assertFalse(u.verificarSenha("", FAKE_HASHER));
    }

    @Test
    void verificarSenhaSemHasherLanca() {
        Usuario u = Usuario.novo("admin", "h:x", Role.ATENDENTE);
        assertThrows(NullPointerException.class, () -> u.verificarSenha("x", null));
    }

    @Test
    void trocarSenhaAtualizaHash() {
        Usuario u = Usuario.novo("admin", FAKE_HASHER.hash("velha"), Role.ATENDENTE);
        u.trocarSenha(FAKE_HASHER.hash("nova"));
        assertTrue(u.verificarSenha("nova", FAKE_HASHER));
        assertFalse(u.verificarSenha("velha", FAKE_HASHER));
    }

    @Test
    void alterarRoleEAtivacao() {
        Usuario u = Usuario.novo("admin", "h:x", Role.ATENDENTE);
        u.alterarRole(Role.ADMINISTRADOR);
        assertEquals(Role.ADMINISTRADOR, u.role());
        u.desativar();
        assertFalse(u.ativo());
        u.ativar();
        assertTrue(u.ativo());
    }

    @Test
    void equalsEHashCodePorId() {
        UsuarioId id = UsuarioId.novo();
        Usuario a = Usuario.reconstituir(id, "u1", "h:x", Role.ATENDENTE, true);
        Usuario b = Usuario.reconstituir(id, "u2", "h:y", Role.ADMINISTRADOR, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Usuario.novo("u3", "h:z", Role.ATENDENTE));
        assertNotEquals(a, "x");
        assertEquals(a, a);
    }

    @Test
    void usuarioIdDeStringEUuidFunciona() {
        UUID raw = UUID.randomUUID();
        UsuarioId fromString = UsuarioId.de(raw.toString());
        UsuarioId fromUuid = UsuarioId.de(raw);
        assertEquals(fromString, fromUuid);
        assertEquals(raw.toString(), fromUuid.toString());
        assertThrows(NullPointerException.class, () -> new UsuarioId(null));
    }
}
