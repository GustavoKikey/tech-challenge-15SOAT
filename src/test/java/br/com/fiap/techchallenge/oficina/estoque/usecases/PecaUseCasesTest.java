package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.estoque.entities.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaPossuiReservasAtivasException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.estoque.entities.Reserva;
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
class PecaUseCasesTest {

    @Mock PecaGateway repository;

    @InjectMocks CadastrarPecaUseCase cadastrar;
    @InjectMocks AtualizarPecaUseCase atualizar;
    @InjectMocks RemoverPecaUseCase remover;
    @InjectMocks BuscarPecaPorIdUseCase buscar;
    @InjectMocks ListarPecasUseCase listar;
    @InjectMocks AdicionarSaldoUseCase adicionarSaldo;
    @InjectMocks ReservarPecaUseCase reservar;
    @InjectMocks BaixarPecaUseCase baixar;

    @Test
    void cadastrar() {
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        Peca p = cadastrar.executar(new CadastrarPecaUseCase.Input(
                "Filtro", new BigDecimal("35.50"), 5));
        assertEquals("Filtro", p.descricao());
        assertEquals(5, p.quantidadeTotal());
    }

    @Test
    void atualizar() {
        Peca existente = Peca.novo("Filtro", Dinheiro.de("35.50"), 5);
        when(repository.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Peca p = atualizar.executar(new AtualizarPecaUseCase.Input(
                existente.id(), "Filtro Premium", new BigDecimal("99.90")));
        assertEquals("Filtro Premium", p.descricao());
        assertEquals(Dinheiro.de("99.90"), p.valorUnitario());
    }

    @Test
    void atualizarFalhaSeNaoExiste() {
        PecaId id = PecaId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        var input = new AtualizarPecaUseCase.Input(id, "x", BigDecimal.ZERO);
        assertThrows(PecaNaoEncontradaException.class, () -> atualizar.executar(input));
    }

    @Test
    void buscar() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 1);
        when(repository.buscarPorId(p.id())).thenReturn(Optional.of(p));
        assertSame(p, buscar.executar(p.id()));
    }

    @Test
    void buscarFalha() {
        PecaId id = PecaId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(PecaNaoEncontradaException.class, () -> buscar.executar(id));
    }

    @Test
    void listar() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 1);
        when(repository.listar()).thenReturn(List.of(p));
        assertEquals(List.of(p), listar.executar());
    }

    @Test
    void removerPecaSemReservasAtivas() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 1);
        when(repository.buscarPorId(p.id())).thenReturn(Optional.of(p));
        remover.executar(p.id());
        verify(repository).remover(p.id());
    }

    @Test
    void removerFalhaSeNaoExiste() {
        PecaId id = PecaId.novo();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(PecaNaoEncontradaException.class, () -> remover.executar(id));
    }

    @Test
    void removerFalhaSeReservasAtivas() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        p.reservar(OrdemServicoId.novo(), 1);
        when(repository.buscarPorId(p.id())).thenReturn(Optional.of(p));
        assertThrows(PecaPossuiReservasAtivasException.class, () -> remover.executar(p.id()));
        verify(repository, never()).remover(any());
    }

    @Test
    void adicionarSaldoFluxo() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 5);
        when(repository.buscarPorId(p.id())).thenReturn(Optional.of(p));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Peca res = adicionarSaldo.executar(new AdicionarSaldoUseCase.Input(p.id(), 7));
        assertEquals(12, res.quantidadeTotal());
    }

    @Test
    void reservarFluxoUsaLockESalva() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        when(repository.buscarPorIdComLock(p.id())).thenReturn(Optional.of(p));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServicoId os = OrdemServicoId.novo();
        Reserva r = reservar.executar(new ReservarPecaUseCase.Input(p.id(), os, 4));
        assertNotNull(r);
        assertEquals(4, r.quantidade());
        verify(repository).buscarPorIdComLock(p.id());
        verify(repository).salvar(p);
    }

    @Test
    void reservarFalhaSeSemSaldo() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 1);
        when(repository.buscarPorIdComLock(p.id())).thenReturn(Optional.of(p));
        var input = new ReservarPecaUseCase.Input(p.id(), OrdemServicoId.novo(), 5);
        assertThrows(EstoqueInsuficienteException.class, () -> reservar.executar(input));
        verify(repository, never()).salvar(any());
    }

    @Test
    void reservarFalhaSePecaNaoExiste() {
        PecaId id = PecaId.novo();
        when(repository.buscarPorIdComLock(id)).thenReturn(Optional.empty());
        var input = new ReservarPecaUseCase.Input(id, OrdemServicoId.novo(), 1);
        assertThrows(PecaNaoEncontradaException.class, () -> reservar.executar(input));
    }

    @Test
    void baixarFluxoUsaLockEDecrementa() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        Reserva r = p.reservar(OrdemServicoId.novo(), 3);
        when(repository.buscarPorIdComLock(p.id())).thenReturn(Optional.of(p));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Peca res = baixar.executar(new BaixarPecaUseCase.Input(p.id(), r.id()));
        assertEquals(7, res.quantidadeTotal());
        verify(repository).buscarPorIdComLock(p.id());
    }

    @Test
    void baixarFalhaSePecaNaoExiste() {
        PecaId id = PecaId.novo();
        when(repository.buscarPorIdComLock(id)).thenReturn(Optional.empty());
        var input = new BaixarPecaUseCase.Input(id, br.com.fiap.techchallenge.oficina.estoque.entities.ReservaId.novo());
        assertThrows(PecaNaoEncontradaException.class, () -> baixar.executar(input));
    }
}
