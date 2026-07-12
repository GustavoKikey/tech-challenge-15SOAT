package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoUseCasesTest {

    @Mock ServicoGateway repository;

    @InjectMocks CadastrarServicoUseCase cadastrar;
    @InjectMocks AtualizarServicoUseCase atualizar;
    @InjectMocks RemoverServicoUseCase remover;
    @InjectMocks BuscarServicoPorIdUseCase buscar;
    @InjectMocks ListarServicosUseCase listar;

    @Test
    void cadastrarServico() {
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        Servico s = cadastrar.executar(new CadastrarServicoUseCase.Input(
                "Troca de óleo", new BigDecimal("99.90")));
        assertEquals("Troca de óleo", s.descricao());
        assertEquals(Dinheiro.de("99.90"), s.valorBase());
    }

    @Test
    void atualizarServico() {
        Servico existente = Servico.novo("Troca de óleo", Dinheiro.de("99.90"));
        when(repository.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Servico res = atualizar.executar(new AtualizarServicoUseCase.Input(
                existente.id(), "Troca completa", new BigDecimal("149.90")));
        assertEquals("Troca completa", res.descricao());
        assertEquals(Dinheiro.de("149.90"), res.valorBase());
    }

    @Test
    void atualizarFalhaSeNaoEncontrado() {
        ServicoId id = ServicoId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        var input = new AtualizarServicoUseCase.Input(id, "x", BigDecimal.ZERO);
        assertThrows(ServicoNaoEncontradoException.class, () -> atualizar.executar(input));
    }

    @Test
    void removerServicoExistente() {
        Servico s = Servico.novo("x", Dinheiro.ZERO);
        when(repository.buscarPorId(s.id())).thenReturn(Optional.of(s));
        remover.executar(s.id());
        verify(repository).remover(s.id());
    }

    @Test
    void removerFalhaSeNaoExiste() {
        ServicoId id = ServicoId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(ServicoNaoEncontradoException.class, () -> remover.executar(id));
    }

    @Test
    void buscarPorId() {
        Servico s = Servico.novo("x", Dinheiro.ZERO);
        when(repository.buscarPorId(s.id())).thenReturn(Optional.of(s));
        assertSame(s, buscar.executar(s.id()));
    }

    @Test
    void buscarPorIdFalha() {
        ServicoId id = ServicoId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(ServicoNaoEncontradoException.class, () -> buscar.executar(id));
    }

    @Test
    void listar() {
        Servico s = Servico.novo("x", Dinheiro.ZERO);
        when(repository.listar()).thenReturn(List.of(s));
        assertEquals(List.of(s), listar.executar());
    }
}
