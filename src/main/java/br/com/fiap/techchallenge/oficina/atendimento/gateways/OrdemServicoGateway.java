package br.com.fiap.techchallenge.oficina.atendimento.gateways;

import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.StatusOS;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;

import java.util.List;
import java.util.Optional;

public interface OrdemServicoGateway {

    OrdemServico salvar(OrdemServico os);

    Optional<OrdemServico> buscarPorId(OrdemServicoId id);

    /**
     * Busca com lock pessimista — usado pelos use cases que mutam estado da OS
     * em conjunto com efeitos no BC Estoque (geração de orçamento, aprovação),
     * para evitar leituras stale em concorrência.
     */
    Optional<OrdemServico> buscarPorIdComLock(OrdemServicoId id);

    List<OrdemServico> listar(Filtro filtro);

    List<OrdemServico> listarPorCliente(ClienteId clienteId);

    /** Filtros opcionais para listagem. Campos null são ignorados. */
    record Filtro(StatusOS status, ClienteId clienteId, VeiculoId veiculoId) {
        public static Filtro vazio() {
            return new Filtro(null, null, null);
        }
    }
}
