package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;

public class AdicionarSaldoUseCase {

    private final PecaGateway repository;

    public AdicionarSaldoUseCase(PecaGateway repository) {
        this.repository = repository;
    }

    public record Input(PecaId id, int quantidade) {}

    public Peca executar(Input input) {
        Peca peca = repository.buscarPorId(input.id())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.id()));
        peca.adicionarSaldo(input.quantidade());
        return repository.salvar(peca);
    }
}
