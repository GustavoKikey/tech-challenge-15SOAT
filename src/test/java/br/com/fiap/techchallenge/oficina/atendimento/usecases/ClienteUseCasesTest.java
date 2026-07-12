package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteJaCadastradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteUseCasesTest {

    @Mock ClienteGateway repository;

    @InjectMocks CadastrarClienteUseCase cadastrar;
    @InjectMocks AtualizarClienteUseCase atualizar;
    @InjectMocks RemoverClienteUseCase remover;
    @InjectMocks BuscarClientePorIdUseCase buscar;
    @InjectMocks ListarClientesUseCase listar;

    private final Documento doc = Documento.de("52998224725");

    @BeforeEach
    void noOp() { /* MockitoExtension cuida da injeção */ }

    @Test
    void cadastrarNovoCliente() {
        when(repository.buscarPorDocumento(doc)).thenReturn(Optional.empty());
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Cliente c = cadastrar.executar(new CadastrarClienteUseCase.Input(
                "João", "52998224725", "j@x.com", "111"));

        assertEquals("João", c.nome());
        assertEquals(doc, c.documento());
        verify(repository).salvar(any());
    }

    @Test
    void cadastrarFalhaSeDocumentoExiste() {
        Cliente existente = Cliente.novo("Ana", doc, null, null);
        when(repository.buscarPorDocumento(doc)).thenReturn(Optional.of(existente));

        var input = new CadastrarClienteUseCase.Input("João", "52998224725", null, null);
        assertThrows(ClienteJaCadastradoException.class, () -> cadastrar.executar(input));
        verify(repository, never()).salvar(any());
    }

    @Test
    void atualizarClienteExistente() {
        Cliente existente = Cliente.novo("Ana", doc, "a@x.com", "111");
        when(repository.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Cliente atualizado = atualizar.executar(new AtualizarClienteUseCase.Input(
                existente.id(), "Ana Maria", "am@x.com", "222"));

        assertEquals("Ana Maria", atualizado.nome());
        assertEquals("am@x.com", atualizado.email());
    }

    @Test
    void atualizarFalhaSeNaoEncontrado() {
        ClienteId id = ClienteId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        var input = new AtualizarClienteUseCase.Input(id, "Ana", null, null);
        assertThrows(ClienteNaoEncontradoException.class, () -> atualizar.executar(input));
    }

    @Test
    void removerClienteExistente() {
        Cliente existente = Cliente.novo("Ana", doc, null, null);
        when(repository.buscarPorId(existente.id())).thenReturn(Optional.of(existente));

        remover.executar(existente.id());
        verify(repository).remover(existente.id());
    }

    @Test
    void removerFalhaSeNaoEncontrado() {
        ClienteId id = ClienteId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(ClienteNaoEncontradoException.class, () -> remover.executar(id));
        verify(repository, never()).remover(any());
    }

    @Test
    void buscarPorId() {
        Cliente existente = Cliente.novo("Ana", doc, null, null);
        when(repository.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        assertSame(existente, buscar.executar(existente.id()));
    }

    @Test
    void buscarPorIdFalhaSeNaoEncontrado() {
        ClienteId id = ClienteId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(ClienteNaoEncontradoException.class, () -> buscar.executar(id));
    }

    @Test
    void listarRetornaTodos() {
        Cliente a = Cliente.novo("A", doc, null, null);
        when(repository.listar()).thenReturn(List.of(a));
        assertEquals(List.of(a), listar.executar());
    }
}
