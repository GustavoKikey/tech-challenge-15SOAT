package br.com.fiap.techchallenge.oficina.application.estoque;

import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ListarPecasUseCase {

    private final PecaRepository repository;

    public ListarPecasUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Peca> executar() {
        return repository.listar();
    }
}
