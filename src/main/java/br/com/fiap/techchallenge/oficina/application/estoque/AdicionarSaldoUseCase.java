package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AdicionarSaldoUseCase {

    private final PecaRepository repository;

    public AdicionarSaldoUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    public record Input(PecaId id, int quantidade) {}

    @Transactional
    public Peca executar(Input input) {
        Peca peca = repository.buscarPorId(input.id())
                .orElseThrow(() -> new PecaNaoEncontradaException(input.id()));
        peca.adicionarSaldo(input.quantidade());
        return repository.salvar(peca);
    }
}
