package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Documento;
import br.com.fiap.techchallenge.oficina.domain.shared.Placa;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Cria uma OS no status RECEBIDA.
 *
 * <p>Cliente e veículo <b>devem existir</b> — fluxo de cadastro é separado.
 * Resolve por documento (cliente) e placa (veículo).
 */
@ApplicationScoped
public class CriarOrdemServicoUseCase {

    private final OrdemServicoRepository osRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;

    public CriarOrdemServicoUseCase(OrdemServicoRepository osRepository,
                                    ClienteRepository clienteRepository,
                                    VeiculoRepository veiculoRepository) {
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
    }

    public record Input(String documentoCliente, String placaVeiculo) {}

    @Transactional
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
