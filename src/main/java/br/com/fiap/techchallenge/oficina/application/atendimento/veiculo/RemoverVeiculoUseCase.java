package br.com.fiap.techchallenge.oficina.application.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoverVeiculoUseCase {

    private final VeiculoRepository repository;

    public RemoverVeiculoUseCase(VeiculoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(VeiculoId id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new VeiculoNaoEncontradoException(id);
        }
        repository.remover(id);
    }
}
