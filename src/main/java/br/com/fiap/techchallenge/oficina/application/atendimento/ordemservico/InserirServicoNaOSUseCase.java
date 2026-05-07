package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class InserirServicoNaOSUseCase {

    private final OrdemServicoRepository osRepository;
    private final ServicoRepository servicoRepository;

    public InserirServicoNaOSUseCase(OrdemServicoRepository osRepository,
                                     ServicoRepository servicoRepository) {
        this.osRepository = osRepository;
        this.servicoRepository = servicoRepository;
    }

    public record Input(OrdemServicoId osId, ServicoId servicoId, BigDecimal valorCobrado) {}

    public record Output(OrdemServico ordemServico, ItemServico item) {}

    @Transactional
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
