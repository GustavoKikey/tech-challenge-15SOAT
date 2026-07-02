package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoJaCadastradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;

public class CadastrarVeiculoUseCase {

    private final VeiculoGateway veiculoRepo;
    private final ClienteGateway clienteRepo;

    public CadastrarVeiculoUseCase(VeiculoGateway veiculoRepo, ClienteGateway clienteRepo) {
        this.veiculoRepo = veiculoRepo;
        this.clienteRepo = clienteRepo;
    }

    public record Input(String placa, String marca, String modelo, int ano, ClienteId clienteId) {}

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
