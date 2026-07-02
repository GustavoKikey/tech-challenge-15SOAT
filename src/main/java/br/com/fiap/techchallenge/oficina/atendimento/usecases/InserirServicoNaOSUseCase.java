package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.math.BigDecimal;

public class InserirServicoNaOSUseCase {

    private final OrdemServicoGateway osRepository;
    private final ServicoGateway servicoRepository;

    public InserirServicoNaOSUseCase(OrdemServicoGateway osRepository,
                                     ServicoGateway servicoRepository) {
        this.osRepository = osRepository;
        this.servicoRepository = servicoRepository;
    }

    public record Input(OrdemServicoId osId, ServicoId servicoId, BigDecimal valorCobrado) {}

    public record Output(OrdemServico ordemServico, ItemServico item) {}

    public Output executar(Input input) {
        if (servicoRepository.buscarPorId(input.servicoId()).isEmpty()) {
            throw new ServicoNaoEncontradoException(input.servicoId());
        }

        OrdemServico os = osRepository.buscarPorId(input.osId())
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(input.osId()));

        ItemServico item = os.inserirServico(input.servicoId(), Dinheiro.de(input.valorCobrado()));
        OrdemServico salvo = osRepository.salvar(os);
        return new Output(salvo, item);
    }
}
