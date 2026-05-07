package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaPossuiReservasAtivasException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoverPecaUseCase {

    private final PecaRepository repository;

    public RemoverPecaUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(PecaId id) {
        Peca peca = repository.buscarPorId(id)
                .orElseThrow(() -> new PecaNaoEncontradaException(id));
        if (peca.possuiReservasAtivas()) {
            throw new PecaPossuiReservasAtivasException(id);
        }
        repository.remover(id);
    }
}
