package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;

import java.util.Comparator;
import java.util.List;

/**
 * Listagem operacional de OS (fase 2):
 *
 * <ul>
 *   <li>Ordenação por status: Em Execução &gt; Aguardando Aprovação &gt; Diagnóstico &gt; Recebida;</li>
 *   <li>dentro do mesmo status, mais antigas primeiro (criadaEm asc);</li>
 *   <li>OS finalizadas, entregues e canceladas saem da listagem padrão — exclusão
 *       <b>lógica</b>: continuam no banco e podem ser consultadas com o filtro
 *       explícito de status.</li>
 * </ul>
 */
public class ListarOrdensServicoUseCase {

    private static final Comparator<OrdemServico> ORDENACAO_OPERACIONAL =
            Comparator.comparingInt((OrdemServico os) -> os.status().prioridadeListagem())
                    .thenComparing(OrdemServico::criadaEm);

    private final OrdemServicoGateway repository;

    public ListarOrdensServicoUseCase(OrdemServicoGateway repository) {
        this.repository = repository;
    }

    public List<OrdemServico> executar(OrdemServicoGateway.Filtro filtro) {
        boolean semFiltroDeStatus = filtro.status() == null;
        return repository.listar(filtro).stream()
                .filter(os -> !semFiltroDeStatus || os.status().visivelNaListagemPadrao())
                .sorted(ORDENACAO_OPERACIONAL)
                .toList();
    }
}
