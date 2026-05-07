package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BuscarPecaPorIdUseCase {

    private final PecaRepository repository;

    public BuscarPecaPorIdUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Peca executar(PecaId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new PecaNaoEncontradaException(id));
    }
}
