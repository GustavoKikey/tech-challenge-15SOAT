package br.com.fiap.techchallenge.oficina.application.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AtualizarVeiculoUseCase {

    private final VeiculoRepository veiculoRepo;
    private final ClienteRepository clienteRepo;

    public AtualizarVeiculoUseCase(VeiculoRepository veiculoRepo, ClienteRepository clienteRepo) {
        this.veiculoRepo = veiculoRepo;
        this.clienteRepo = clienteRepo;
    }

    public record Input(VeiculoId id, String marca, String modelo, int ano, ClienteId clienteId) {}

    @Transactional
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
