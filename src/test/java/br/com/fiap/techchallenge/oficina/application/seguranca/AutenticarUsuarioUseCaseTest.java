package br.com.fiap.techchallenge.oficina.application.seguranca;

import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.CredenciaisInvalidasException;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.PasswordHasher;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Role;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.TokenService;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Usuario;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioUseCaseTest {

    @Mock UsuarioRepository repository;
    @Mock PasswordHasher hasher;
    @Mock TokenService tokenService;

    @Test
    void autenticaComCredenciaisCorretas() {
        Usuario admin = Usuario.novo("admin", "$2a$10$hash", Role.ADMINISTRADOR);
        when(repository.buscarPorUsername("admin")).thenReturn(Optional.of(admin));
        when(hasher.matches("segredo", "$2a$10$hash")).thenReturn(true);
        when(tokenService.gerar(any())).thenReturn(new TokenService.Token("token-aaa", 28800L));

        AutenticarUsuarioUseCase.Output out = new AutenticarUsuarioUseCase(repository, hasher, tokenService)
                .executar(new AutenticarUsuarioUseCase.Input("admin", "segredo"));

        assertEquals("token-aaa", out.accessToken());
        assertEquals(28800L, out.expiresIn());
        assertEquals("ADMINISTRADOR", out.role());
    }

    @Test
    void usuarioInexistenteRetornaCredenciaisInvalidas() {
        when(repository.buscarPorUsername("ninguem")).thenReturn(Optional.empty());
        AutenticarUsuarioUseCase uc = new AutenticarUsuarioUseCase(repository, hasher, tokenService);
        assertThrows(CredenciaisInvalidasException.class,
                () -> uc.executar(new AutenticarUsuarioUseCase.Input("ninguem", "x")));
        verifyNoInteractions(tokenService);
    }

    @Test
    void senhaErradaRetornaCredenciaisInvalidas() {
        Usuario u = Usuario.novo("admin", "$2a$10$hash", Role.ATENDENTE);
        when(repository.buscarPorUsername("admin")).thenReturn(Optional.of(u));
        when(hasher.matches(any(), any())).thenReturn(false);

        AutenticarUsuarioUseCase uc = new AutenticarUsuarioUseCase(repository, hasher, tokenService);
        assertThrows(CredenciaisInvalidasException.class,
                () -> uc.executar(new AutenticarUsuarioUseCase.Input("admin", "errada")));
        verifyNoInteractions(tokenService);
    }

    @Test
    void usuarioInativoRetornaCredenciaisInvalidasMesmoComSenhaCorreta() {
        Usuario u = Usuario.novo("admin", "$2a$10$hash", Role.ATENDENTE);
        u.desativar();
        when(repository.buscarPorUsername("admin")).thenReturn(Optional.of(u));
        // hasher.matches nem deve ser chamado se inativo (Usuario curto-circuita)

        AutenticarUsuarioUseCase uc = new AutenticarUsuarioUseCase(repository, hasher, tokenService);
        assertThrows(CredenciaisInvalidasException.class,
                () -> uc.executar(new AutenticarUsuarioUseCase.Input("admin", "segredo")));
        verifyNoInteractions(tokenService);
    }
}
