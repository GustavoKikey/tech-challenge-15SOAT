package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.estoque.entities.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;
import br.com.fiap.techchallenge.oficina.shared.entities.Documento;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;

import java.math.BigDecimal;
import java.util.List;

/**
 * Abertura de OS (fase 2): recebe os dados do cliente, do veículo e as listas de
 * serviços/peças em uma única chamada, devolvendo a OS criada (status RECEBIDA)
 * com sua identificação única.
 *
 * <p>Cliente e veículo são resolvidos por documento/placa; se ainda não existirem,
 * são cadastrados com os dados recebidos (get-or-create). Serviços e peças precisam
 * existir no catálogo/estoque: valor cobrado do serviço usa o valor base do catálogo
 * quando não informado, e o valor unitário da peça vem sempre do estoque.
 *
 * <p>O saldo de peças é apenas conferido aqui (pré-aviso); a reserva autoritativa
 * acontece na geração do orçamento, como na fase 1.
 */
public class AbrirOrdemServicoUseCase {

    private final OrdemServicoGateway osRepository;
    private final ClienteGateway clienteRepository;
    private final VeiculoGateway veiculoRepository;
    private final ServicoGateway servicoRepository;
    private final PecaGateway pecaRepository;

    public AbrirOrdemServicoUseCase(OrdemServicoGateway osRepository,
                                    ClienteGateway clienteRepository,
                                    VeiculoGateway veiculoRepository,
                                    ServicoGateway servicoRepository,
                                    PecaGateway pecaRepository) {
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.pecaRepository = pecaRepository;
    }

    public record DadosCliente(String documento, String nome, String email, String telefone) {}

    public record DadosVeiculo(String placa, String marca, String modelo, Integer ano) {}

    /** {@code valorCobrado} null → usa o valor base do catálogo. */
    public record ItemServicoInput(ServicoId servicoId, BigDecimal valorCobrado) {}

    public record ItemPecaInput(PecaId pecaId, int quantidade) {}

    public record Input(DadosCliente cliente, DadosVeiculo veiculo,
                        List<ItemServicoInput> servicos, List<ItemPecaInput> pecas) {}

    public OrdemServico executar(Input input) {
        Cliente cliente = resolverCliente(input.cliente());
        Veiculo veiculo = resolverVeiculo(input.veiculo(), cliente);

        OrdemServico os = OrdemServico.abrir(cliente.id(), veiculo.id());

        for (ItemServicoInput item : listaSegura(input.servicos())) {
            Servico servico = servicoRepository.buscarPorId(item.servicoId())
                    .orElseThrow(() -> new ServicoNaoEncontradoException(item.servicoId()));
            Dinheiro valor = item.valorCobrado() != null
                    ? Dinheiro.de(item.valorCobrado())
                    : servico.valorBase();
            os.inserirServico(servico.id(), valor);
        }

        for (ItemPecaInput item : listaSegura(input.pecas())) {
            Peca peca = pecaRepository.buscarPorId(item.pecaId())
                    .orElseThrow(() -> new PecaNaoEncontradaException(item.pecaId()));
            if (item.quantidade() > peca.saldoDisponivel()) {
                throw new EstoqueInsuficienteException(
                        peca.id(), item.quantidade(), peca.saldoDisponivel());
            }
            os.inserirPeca(peca.id(), item.quantidade(), peca.valorUnitario());
        }

        return osRepository.salvar(os);
    }

    private Cliente resolverCliente(DadosCliente dados) {
        Documento documento = Documento.de(dados.documento());
        return clienteRepository.buscarPorDocumento(documento).orElseGet(() -> {
            if (dados.nome() == null || dados.nome().isBlank()) {
                throw new ClienteNaoEncontradoException(
                        "Cliente não encontrado para o documento " + documento.numero()
                                + " — informe o nome para cadastrá-lo na abertura da OS");
            }
            return clienteRepository.salvar(Cliente.novo(
                    dados.nome(), documento, dados.email(), dados.telefone()));
        });
    }

    private Veiculo resolverVeiculo(DadosVeiculo dados, Cliente cliente) {
        Placa placa = Placa.de(dados.placa());
        return veiculoRepository.buscarPorPlaca(placa).orElseGet(() -> {
            if (dados.marca() == null || dados.modelo() == null || dados.ano() == null) {
                throw new VeiculoNaoEncontradoException(
                        "Veículo não encontrado para a placa " + placa.valor()
                                + " — informe marca, modelo e ano para cadastrá-lo na abertura da OS");
            }
            return veiculoRepository.salvar(Veiculo.novo(
                    placa, dados.marca(), dados.modelo(), dados.ano(), cliente.id()));
        });
    }

    private static <T> List<T> listaSegura(List<T> lista) {
        return lista == null ? List.of() : lista;
    }
}
