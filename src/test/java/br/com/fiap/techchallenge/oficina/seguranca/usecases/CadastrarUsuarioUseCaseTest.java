package br.com.fiap.techchallenge.oficina.seguranca.usecases;

import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Role;
import br.com.fiap.techchallenge.oficina.seguranca.entities.UsernameJaCadastradoException;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.UsuarioGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadastrarUsuarioUseCaseTest {

    @Mock UsuarioGateway repository;
    @Mock PasswordHasher hasher;

    @Test
    void cadastraNovoUsuarioHasheandoSenha() {
        when(repository.buscarPorUsername("ana")).thenReturn(Optional.empty());
        when(hasher.hash("senha-123")).thenReturn("$2a$10$hash");
        when(repository.salvar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario salvo = new CadastrarUsuarioUseCase(repository, hasher).executar(
                new CadastrarUsuarioUseCase.Input("ana", "senha-123", Role.ATENDENTE));

        assertEquals("ana", salvo.username());
        assertEquals(Role.ATENDENTE, salvo.role());
        assertTrue(salvo.ativo());
        assertEquals("$2a$10$hash", salvo.senhaHash());
        verify(hasher).hash("senha-123");
    }

    @Test
    void usernameJaCadastradoLanca() {
        when(repository.buscarPorUsername("ana"))
                .thenReturn(Optional.of(Usuario.novo("ana", "h:x", Role.ATENDENTE)));
        CadastrarUsuarioUseCase uc = new CadastrarUsuarioUseCase(repository, hasher);
        assertThrows(UsernameJaCadastradoException.class,
                () -> uc.executar(new CadastrarUsuarioUseCase.Input("ana", "x", Role.ATENDENTE)));
        verify(hasher, never()).hash(any());
        verify(repository, never()).salvar(any());
    }
}
