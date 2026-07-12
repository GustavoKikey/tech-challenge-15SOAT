package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;

public class AtualizarVeiculoUseCase {

    private final VeiculoGateway veiculoRepo;
    private final ClienteGateway clienteRepo;

    public AtualizarVeiculoUseCase(VeiculoGateway veiculoRepo, ClienteGateway clienteRepo) {
        this.veiculoRepo = veiculoRepo;
        this.clienteRepo = clienteRepo;
    }

    public record Input(VeiculoId id, String marca, String modelo, int ano, ClienteId clienteId) {}

    public Veiculo executar(Input input) {
        Veiculo veiculo = veiculoRepo.buscarPorId(input.id())
                .orElseThrow(() -> new VeiculoNaoEncontradoException(input.id()));

        if (input.clienteId() != null && !input.clienteId().equals(veiculo.clienteId())) {
            if (clienteRepo.buscarPorId(input.clienteId()).isEmpty()) {
                throw new ClienteNaoEncontradoException(input.clienteId());
            }
            veiculo.transferirPara(input.clienteId());
        }
        veiculo.atualizarFichaTecnica(input.marca(), input.modelo(), input.ano());
        return veiculoRepo.salvar(veiculo);
    }
}
