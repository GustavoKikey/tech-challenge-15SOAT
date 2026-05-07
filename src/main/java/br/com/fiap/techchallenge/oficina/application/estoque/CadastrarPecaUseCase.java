package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class CadastrarPecaUseCase {

    private final PecaRepository repository;

    public CadastrarPecaUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    public record Input(String descricao, BigDecimal valorUnitario, int quantidadeInicial) {}

    @Transactional
    public Peca executar(Input input) {
        Peca peca = Peca.novo(
                input.descricao(),
                Dinheiro.de(input.valorUnitario()),
                input.quantidadeInicial()
        );
        return repository.salvar(peca);
    }
}
