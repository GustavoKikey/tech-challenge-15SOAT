package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.estoque.entities.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.Reserva;
import br.com.fiap.techchallenge.oficina.estoque.entities.StatusReserva;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IT do fluxo Reservar → Baixar via use cases (não há endpoint público — é o
 * BC Atendimento que vai injetar isto).
 *
 * <p>Use cases não são beans CDI (são POJOs compostos pelos controllers no
 * composition root); o teste os monta a partir do {@link PecaGateway} real e
 * demarca cada interação pela mesma porta {@link ExecutorTransacional} que os
 * controllers usam.
 */
@QuarkusTest
class EstoqueFluxoReservaBaixaIT {

    @Inject PecaGateway gateway;
    @Inject ExecutorTransacional tx;

    CadastrarPecaUseCase cadastrar;
    AdicionarSaldoUseCase adicionarSaldo;
    ReservarPecaUseCase reservar;
    BaixarPecaUseCase baixar;
    BuscarPecaPorIdUseCase buscar;

    @BeforeEach
    void montaUseCases() {
        cadastrar = new CadastrarPecaUseCase(gateway);
        adicionarSaldo = new AdicionarSaldoUseCase(gateway);
        reservar = new ReservarPecaUseCase(gateway);
        baixar = new BaixarPecaUseCase(gateway);
        buscar = new BuscarPecaPorIdUseCase(gateway);
    }

    @Test
    void reservarEBaixarPeca() {
        Peca peca = tx.emTransacao(() -> cadastrar.executar(new CadastrarPecaUseCase.Input(
                "Disco de freio", new BigDecimal("250.00"), 0)));
        tx.emTransacao(() -> adicionarSaldo.executar(
                new AdicionarSaldoUseCase.Input(peca.id(), 5)));

        OrdemServicoId os = OrdemServicoId.novo();
        Reserva reserva = tx.emTransacao(() -> reservar.executar(
                new ReservarPecaUseCase.Input(peca.id(), os, 2)));

        Peca apos = tx.emTransacao(() -> buscar.executar(peca.id()));
        assertEquals(5, apos.quantidadeTotal());
        assertEquals(3, apos.saldoDisponivel());

        tx.emTransacao(() -> baixar.executar(
                new BaixarPecaUseCase.Input(peca.id(), reserva.id())));

        Peca depoisBaixa = tx.emTransacao(() -> buscar.executar(peca.id()));
        assertEquals(3, depoisBaixa.quantidadeTotal());
        assertEquals(3, depoisBaixa.saldoDisponivel());
        assertTrue(depoisBaixa.reservas().stream()
                .anyMatch(r -> r.id().equals(reserva.id()) && r.status() == StatusReserva.BAIXADA));
    }

    @Test
    void reservarSemSaldoLanca() {
        Peca peca = tx.emTransacao(() -> cadastrar.executar(new CadastrarPecaUseCase.Input(
                "Lâmpada H4", new BigDecimal("19.90"), 1)));
        var input = new ReservarPecaUseCase.Input(peca.id(), OrdemServicoId.novo(), 5);
        assertThrows(EstoqueInsuficienteException.class,
                () -> tx.emTransacao(() -> reservar.executar(input)));
    }
}
