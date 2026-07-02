package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Documento;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;

/**
 * Cria uma OS no status RECEBIDA.
 *
 * <p>Cliente e veículo <b>devem existir</b> — fluxo de cadastro é separado.
 * Resolve por documento (cliente) e placa (veículo).
 */
public class CriarOrdemServicoUseCase {

    private final OrdemServicoGateway osRepository;
    private final ClienteGateway clienteRepository;
    private final VeiculoGateway veiculoRepository;

    public CriarOrdemServicoUseCase(OrdemServicoGateway osRepository,
                                    ClienteGateway clienteRepository,
                                    VeiculoGateway veiculoRepository) {
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
    }

    public record Input(String documentoCliente, String placaVeiculo) {}

    public OrdemServico executar(Input input) {
        Documento documento = Documento.de(input.documentoCliente());
        Cliente cliente = clienteRepository.buscarPorDocumento(documento)
                .orElseThrow(() -> new ClienteNaoEncontradoException(
                        "Cliente não encontrado para o documento " + documento.numero()));

        Placa placa = Placa.de(input.placaVeiculo());
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(placa)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(
                        "Veículo não encontrado para a placa " + placa.valor()));

        OrdemServico os = OrdemServico.abrir(cliente.id(), veiculo.id());
        return osRepository.salvar(os);
    }
}
