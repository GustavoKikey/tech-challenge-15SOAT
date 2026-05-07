package br.com.fiap.techchallenge.oficina.application.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ListarVeiculosUseCase {

    private final VeiculoRepository repository;

    public ListarVeiculosUseCase(VeiculoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Veiculo> executar() {
        return repository.listar();
    }

    @Transactional
    public List<Veiculo> porCliente(ClienteId clienteId) {
        return repository.listarPorCliente(clienteId);
    }
}
