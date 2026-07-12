package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.estoque.entities.Reserva;

/**
 * Cria reserva ATIVA de uma peça para uma OS.
 *
 * <p><b>Não é endpoint REST público.</b> É a contrapartida da política
 * "Reservar peça" do Event Storming, acionada pelo BC Atendimento
 * durante a geração do orçamento. Lê com lock pessimista para evitar dois
 * pedidos concorrentes passarem na verificação de saldo disponível.
 */
public class ReservarPecaUseCase {

    private final PecaGateway repository;

    public ReservarPecaUseCase(PecaGateway repository) {
        this.repository = repository;
    }

    public record Input(PecaId pecaId, OrdemServicoId ordemServicoId, int quantidade) {}

    public Reserva executar(Input input) {
        Peca peca = repository.buscarPorIdComLock(input.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.pecaId()));
        Reserva reserva = peca.reservar(input.ordemServicoId(), input.quantidade());
        repository.salvar(peca);
        return reserva;
    }
}
