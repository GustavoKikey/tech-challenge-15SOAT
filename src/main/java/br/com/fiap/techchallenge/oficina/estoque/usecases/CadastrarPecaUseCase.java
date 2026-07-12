package br.com.fiap.techchallenge.oficina.estoque.usecases;

import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.math.BigDecimal;

public class CadastrarPecaUseCase {

    private final PecaGateway repository;

    public CadastrarPecaUseCase(PecaGateway repository) {
        this.repository = repository;
    }

    public record Input(String descricao, BigDecimal valorUnitario, int quantidadeInicial) {}

    public Peca executar(Input input) {
        Peca peca = Peca.novo(
                input.descricao(),
                Dinheiro.de(input.valorUnitario()),
                input.quantidadeInicial()
        );
        return repository.salvar(peca);
    }
}
