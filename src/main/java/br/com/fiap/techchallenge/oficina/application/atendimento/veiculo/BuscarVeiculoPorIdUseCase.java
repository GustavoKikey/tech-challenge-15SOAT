package br.com.fiap.techchallenge.oficina.application.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BuscarVeiculoPorIdUseCase {

    private final VeiculoRepository repository;

    public BuscarVeiculoPorIdUseCase(VeiculoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Veiculo executar(VeiculoId id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(id));
    }
}
