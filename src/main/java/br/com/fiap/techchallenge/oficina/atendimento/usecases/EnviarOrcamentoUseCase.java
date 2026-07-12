package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;

/**
 * Transição EM_DIAGNOSTICO → AGUARDANDO_APROVACAO.
 *
 * <p>No MVP "envio" é puramente uma mudança de status — não há canal externo
 * (e-mail/SMS) acionado.
 */
public class EnviarOrcamentoUseCase {

    private final OrdemServicoGateway repository;

    public EnviarOrcamentoUseCase(OrdemServicoGateway repository) {
        this.repository = repository;
    }

    public OrdemServico executar(OrdemServicoId id) {
        OrdemServico os = repository.buscarPorId(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        os.enviarOrcamento();
        return repository.salvar(os);
    }
}
