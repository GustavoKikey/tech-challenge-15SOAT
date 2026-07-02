package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.estoque.entities.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.Reserva;
import br.com.fiap.techchallenge.oficina.estoque.entities.StatusReserva;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IT do fluxo Reservar → Baixar via use cases (não há endpoint público — é o
 * BC Atendimento que vai injetar isto).
 */
@QuarkusTest
class EstoqueFluxoReservaBaixaIT {

    @Inject CadastrarPecaUseCase cadastrar;
    @Inject AdicionarSaldoUseCase adicionarSaldo;
    @Inject ReservarPecaUseCase reservar;
    @Inject BaixarPecaUseCase baixar;
    @Inject BuscarPecaPorIdUseCase buscar;

    @Test
    void reservarEBaixarPeca() {
        Peca peca = cadastrar.executar(new CadastrarPecaUseCase.Input(
                "Disco de freio", new BigDecimal("250.00"), 0));
        adicionarSaldo.executar(new AdicionarSaldoUseCase.Input(peca.id(), 5));

        OrdemServicoId os = OrdemServicoId.novo();
        Reserva reserva = reservar.executar(new ReservarPecaUseCase.Input(peca.id(), os, 2));

        Peca apos = buscar.executar(peca.id());
        assertEquals(5, apos.quantidadeTotal());
        assertEquals(3, apos.saldoDisponivel());

        baixar.executar(new BaixarPecaUseCase.Input(peca.id(), reserva.id()));

        Peca depoisBaixa = buscar.executar(peca.id());
        assertEquals(3, depoisBaixa.quantidadeTotal());
        assertEquals(3, depoisBaixa.saldoDisponivel());
        assertTrue(depoisBaixa.reservas().stream()
                .anyMatch(r -> r.id().equals(reserva.id()) && r.status() == StatusReserva.BAIXADA));
    }

    @Test
    void reservarSemSaldoLanca() {
        Peca peca = cadastrar.executar(new CadastrarPecaUseCase.Input(
                "Lâmpada H4", new BigDecimal("19.90"), 1));
        var input = new ReservarPecaUseCase.Input(peca.id(), OrdemServicoId.novo(), 5);
        assertThrows(EstoqueInsuficienteException.class, () -> reservar.executar(input));
    }
}
