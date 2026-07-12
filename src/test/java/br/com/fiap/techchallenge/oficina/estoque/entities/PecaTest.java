package br.com.fiap.techchallenge.oficina.estoque.entities;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class PecaTest {

    @Test
    void deveCriarPecaValida() {
        Peca p = Peca.novo("  Filtro de óleo  ", Dinheiro.de("35.50"), 10);
        assertNotNull(p.id());
        assertEquals("Filtro de óleo", p.descricao());
        assertEquals(Dinheiro.de("35.50"), p.valorUnitario());
        assertEquals(10, p.quantidadeTotal());
        assertEquals(10, p.saldoDisponivel());
        assertTrue(p.reservas().isEmpty());
    }

    @Test
    void deveCriarComSaldoZeroPorDefault() {
        Peca p = Peca.novo("Pastilha", Dinheiro.de("80.00"));
        assertEquals(0, p.quantidadeTotal());
        assertEquals(0, p.saldoDisponivel());
    }

    @Test
    void naoDeveCriarSemDescricao() {
        assertThrows(IllegalArgumentException.class,
                () -> Peca.novo("", Dinheiro.de("10.00"), 5));
        assertThrows(IllegalArgumentException.class,
                () -> Peca.novo(null, Dinheiro.de("10.00"), 5));
    }

    @Test
    void naoDeveCriarComSaldoNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> Peca.novo("x", Dinheiro.de("10.00"), -1));
    }

    @Test
    void adicionarSaldoSomaQuantidade() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 3);
        p.adicionarSaldo(7);
        assertEquals(10, p.quantidadeTotal());
        assertEquals(10, p.saldoDisponivel());
    }

    @Test
    void adicionarSaldoNaoAceitaZeroOuNegativo() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 3);
        assertThrows(IllegalArgumentException.class, () -> p.adicionarSaldo(0));
        assertThrows(IllegalArgumentException.class, () -> p.adicionarSaldo(-5));
    }

    @Test
    void reservarComSaldoSuficiente() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        OrdemServicoId os = OrdemServicoId.novo();
        Reserva r = p.reservar(os, 4);
        assertNotNull(r);
        assertEquals(StatusReserva.ATIVA, r.status());
        assertEquals(os, r.ordemServicoId());
        assertEquals(4, r.quantidade());
        assertEquals(10, p.quantidadeTotal());
        assertEquals(6, p.saldoDisponivel());
    }

    @Test
    void reservarComSaldoInsuficienteLanca() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 5);
        assertThrows(EstoqueInsuficienteException.class,
                () -> p.reservar(OrdemServicoId.novo(), 6));
    }

    @Test
    void multiplasReservasReduzemSaldoDisponivel() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        p.reservar(OrdemServicoId.novo(), 3);
        p.reservar(OrdemServicoId.novo(), 4);
        assertEquals(3, p.saldoDisponivel());
        assertEquals(10, p.quantidadeTotal());
        assertThrows(EstoqueInsuficienteException.class,
                () -> p.reservar(OrdemServicoId.novo(), 4));
    }

    @Test
    void reservarRejeitaQuantidadeNaoPositiva() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        assertThrows(IllegalArgumentException.class,
                () -> p.reservar(OrdemServicoId.novo(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> p.reservar(OrdemServicoId.novo(), -1));
    }

    @Test
    void baixarReservaAtivaDecrementaTotal() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        Reserva r = p.reservar(OrdemServicoId.novo(), 4);
        p.baixar(r.id());
        assertEquals(6, p.quantidadeTotal());
        assertEquals(6, p.saldoDisponivel());
        assertEquals(StatusReserva.BAIXADA, r.status());
    }

    @Test
    void baixarReservaInexistenteLanca() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        assertThrows(NoSuchElementException.class, () -> p.baixar(ReservaId.novo()));
    }

    @Test
    void baixarDuasVezesLanca() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        Reserva r = p.reservar(OrdemServicoId.novo(), 2);
        p.baixar(r.id());
        assertThrows(ReservaInvalidaException.class, () -> p.baixar(r.id()));
    }

    @Test
    void cancelarReservaLiberaSaldo() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        Reserva r = p.reservar(OrdemServicoId.novo(), 4);
        assertEquals(6, p.saldoDisponivel());
        p.cancelarReserva(r.id());
        assertEquals(10, p.saldoDisponivel());
        assertEquals(10, p.quantidadeTotal());
        assertEquals(StatusReserva.CANCELADA, r.status());
    }

    @Test
    void cancelarReservaJaBaixadaLanca() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        Reserva r = p.reservar(OrdemServicoId.novo(), 2);
        p.baixar(r.id());
        assertThrows(ReservaInvalidaException.class, () -> p.cancelarReserva(r.id()));
    }

    @Test
    void alterarMudaDescricaoEValor() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 0);
        p.alterar("Filtro premium", Dinheiro.de("99.90"));
        assertEquals("Filtro premium", p.descricao());
        assertEquals(Dinheiro.de("99.90"), p.valorUnitario());
    }

    @Test
    void alterarValidaCampos() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 0);
        assertThrows(IllegalArgumentException.class, () -> p.alterar("", Dinheiro.ZERO));
        assertThrows(NullPointerException.class, () -> p.alterar("ok", null));
    }

    @Test
    void possuiReservasAtivasReflete() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        assertFalse(p.possuiReservasAtivas());
        Reserva r = p.reservar(OrdemServicoId.novo(), 1);
        assertTrue(p.possuiReservasAtivas());
        p.cancelarReserva(r.id());
        assertFalse(p.possuiReservasAtivas());
    }

    @Test
    void reservasIsListaImutavel() {
        Peca p = Peca.novo("x", Dinheiro.de("1.00"), 10);
        assertThrows(UnsupportedOperationException.class,
                () -> p.reservas().add(Reserva.nova(OrdemServicoId.novo(), 1)));
    }

    @Test
    void reconstituirRestauraEstado() {
        PecaId id = PecaId.novo();
        Peca p = Peca.reconstituir(id, "x", Dinheiro.de("1.00"), 5, java.util.List.of());
        assertEquals(id, p.id());
        assertEquals(5, p.quantidadeTotal());
    }

    @Test
    void equalsEHashCodePorId() {
        PecaId id = PecaId.novo();
        Peca a = Peca.reconstituir(id, "x", Dinheiro.ZERO, 0, java.util.List.of());
        Peca b = Peca.reconstituir(id, "y", Dinheiro.de("9"), 99, java.util.List.of());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Peca.novo("x", Dinheiro.ZERO));
        assertNotEquals(a, "string");
        assertEquals(a, a);
    }

    @Test
    void pecaIdEReservaIdRejeitamNulo() {
        assertThrows(NullPointerException.class, () -> new PecaId(null));
        assertThrows(NullPointerException.class, () -> new ReservaId(null));
    }
}
