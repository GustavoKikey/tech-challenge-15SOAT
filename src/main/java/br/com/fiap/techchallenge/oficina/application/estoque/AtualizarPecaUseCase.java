package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class AtualizarPecaUseCase {

    private final PecaRepository repository;

    public AtualizarPecaUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    public record Input(PecaId id, String descricao, BigDecimal valorUnitario) {}

    @Transactional
    public Peca executar(Input input) {
        Peca peca = repository.buscarPorId(input.id())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.id()));
        peca.alterar(input.descricao(), Dinheiro.de(input.valorUnitario()));
        return repository.salvar(peca);
    }
}
