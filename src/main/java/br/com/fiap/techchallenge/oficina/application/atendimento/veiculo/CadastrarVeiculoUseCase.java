package br.com.fiap.techchallenge.oficina.application.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoJaCadastradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Placa;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CadastrarVeiculoUseCase {

    private final VeiculoRepository veiculoRepo;
    private final ClienteRepository clienteRepo;

    public CadastrarVeiculoUseCase(VeiculoRepository veiculoRepo, ClienteRepository clienteRepo) {
        this.veiculoRepo = veiculoRepo;
        this.clienteRepo = clienteRepo;
    }

    public record Input(String placa, String marca, String modelo, int ano, ClienteId clienteId) {}

    @Transactional
    public Veiculo executar(Input input) {
        if (clienteRepo.buscarPorId(input.clienteId()).isEmpty()) {
            throw new ClienteNaoEncontradoException(input.clienteId());
        }

        Placa placa = Placa.de(input.placa());
        veiculoRepo.buscarPorPlaca(placa).ifPresent(v -> {
            throw new VeiculoJaCadastradoException(placa.valor());
        });

        Veiculo veiculo = Veiculo.novo(placa, input.marca(), input.modelo(), input.ano(), input.clienteId());
        return veiculoRepo.salvar(veiculo);
    }
}
