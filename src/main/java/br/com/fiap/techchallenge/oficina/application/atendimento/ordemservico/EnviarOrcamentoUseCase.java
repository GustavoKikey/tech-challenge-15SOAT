package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Transição EM_DIAGNOSTICO → AGUARDANDO_APROVACAO.
 *
 * <p>No MVP "envio" é puramente uma mudança de status — não há canal externo
 * (e-mail/SMS) acionado.
 */
@ApplicationScoped
public class EnviarOrcamentoUseCase {

    private final OrdemServicoRepository repository;

    public EnviarOrcamentoUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OrdemServico executar(OrdemServicoId id) {
        OrdemServico os = repository.buscarPorId(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        os.enviarOrcamento();
        return repository.salvar(os);
    }
}
