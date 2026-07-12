package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.entities.ReservaId;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz <b>Ordem de Serviço</b> — coração do sistema (Event Storming, pág. 2 do DDD).
 *
 * <p>Encapsula a máquina de estados da OS, a coleção de itens (serviço/peça) e o
 * orçamento. Cliente e veículo são referenciados <i>por id</i> — mantém o agregado
 * pequeno e respeita o limite com os outros agregados do BC Atendimento.
 *
 * <p>Reservas no BC Estoque são identificadas pelo {@link ReservaId} guardado em
 * cada {@link ItemPeca}. O agregado <b>não chama o estoque</b>: quem orquestra a
 * conversa entre BCs é o use case (relação Customer-Supplier).
 */
public class OrdemServico {

    private final OrdemServicoId id;
    private final ClienteId clienteId;
    private final VeiculoId veiculoId;
    private StatusOS status;
    private final List<ItemServico> itensServico;
    private final List<ItemPeca> itensPeca;
    private Orcamento orcamento;

    private final OffsetDateTime criadaEm;
    private OffsetDateTime diagnosticoIniciadoEm;
    private OffsetDateTime execucaoIniciadaEm;
    private OffsetDateTime finalizadaEm;
    private OffsetDateTime entregueEm;
    private OffsetDateTime canceladaEm;

    private OrdemServico(OrdemServicoId id, ClienteId clienteId, VeiculoId veiculoId,
                         StatusOS status, List<ItemServico> itensServico, List<ItemPeca> itensPeca,
                         Orcamento orcamento, OffsetDateTime criadaEm,
                         OffsetDateTime diagnosticoIniciadoEm, OffsetDateTime execucaoIniciadaEm,
                         OffsetDateTime finalizadaEm, OffsetDateTime entregueEm,
                         OffsetDateTime canceladaEm) {
        this.id = Objects.requireNonNull(id, "id");
        this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
        this.veiculoId = Objects.requireNonNull(veiculoId, "veiculoId");
        this.status = Objects.requireNonNull(status, "status");
        this.itensServico = new ArrayList<>(itensServico);
        this.itensPeca = new ArrayList<>(itensPeca);
        this.orcamento = orcamento;
        this.criadaEm = Objects.requireNonNull(criadaEm, "criadaEm");
        this.diagnosticoIniciadoEm = diagnosticoIniciadoEm;
        this.execucaoIniciadaEm = execucaoIniciadaEm;
        this.finalizadaEm = finalizadaEm;
        this.entregueEm = entregueEm;
        this.canceladaEm = canceladaEm;
    }

    public static OrdemServico abrir(ClienteId clienteId, VeiculoId veiculoId) {
        return new OrdemServico(
                OrdemServicoId.novo(), clienteId, veiculoId, StatusOS.RECEBIDA,
                List.of(), List.of(), null, OffsetDateTime.now(),
                null, null, null, null, null);
    }

    public static OrdemServico reconstituir(OrdemServicoId id, ClienteId clienteId, VeiculoId veiculoId,
                                            StatusOS status, List<ItemServico> itensServico,
                                            List<ItemPeca> itensPeca, Orcamento orcamento,
                                            OffsetDateTime criadaEm, OffsetDateTime diagnosticoIniciadoEm,
                                            OffsetDateTime execucaoIniciadaEm, OffsetDateTime finalizadaEm,
                                            OffsetDateTime entregueEm, OffsetDateTime canceladaEm) {
        return new OrdemServico(id, clienteId, veiculoId, status, itensServico, itensPeca,
                orcamento, criadaEm, diagnosticoIniciadoEm, execucaoIniciadaEm,
                finalizadaEm, entregueEm, canceladaEm);
    }

    // ---------- Transições de status ----------

    public void iniciarDiagnostico() {
        exigirStatus(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO);
        this.status = StatusOS.EM_DIAGNOSTICO;
        this.diagnosticoIniciadoEm = OffsetDateTime.now();
    }

    public void enviarOrcamento() {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO);
        if (orcamento == null) {
            throw new TransicaoStatusInvalidaException(
                    "Orçamento precisa ser gerado antes de enviar para aprovação");
        }
        this.status = StatusOS.AGUARDANDO_APROVACAO;
    }

    public void aprovarOrcamento() {
        exigirStatus(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_EXECUCAO);
        if (orcamento == null) {
            throw new TransicaoStatusInvalidaException(
                    "Não há orçamento para aprovar");
        }
        orcamento.aprovar();
        this.status = StatusOS.EM_EXECUCAO;
        this.execucaoIniciadaEm = OffsetDateTime.now();
    }

    /**
     * Cliente recusou o orçamento — a OS é cancelada (exclusão lógica: continua
     * persistida, mas sai da listagem padrão). O use case que orquestra a recusa
     * é responsável por liberar as reservas de peças no BC Estoque.
     */
    public void recusarOrcamento() {
        exigirStatus(StatusOS.AGUARDANDO_APROVACAO, StatusOS.CANCELADA);
        if (orcamento == null) {
            throw new TransicaoStatusInvalidaException(
                    "Não há orçamento para recusar");
        }
        this.status = StatusOS.CANCELADA;
        this.canceladaEm = OffsetDateTime.now();
    }

    public void finalizar() {
        exigirStatus(StatusOS.EM_EXECUCAO, StatusOS.FINALIZADA);
        this.status = StatusOS.FINALIZADA;
        this.finalizadaEm = OffsetDateTime.now();
    }

    public void entregar() {
        exigirStatus(StatusOS.FINALIZADA, StatusOS.ENTREGUE);
        this.status = StatusOS.ENTREGUE;
        this.entregueEm = OffsetDateTime.now();
    }

    private void exigirStatus(StatusOS esperado, StatusOS desejado) {
        if (this.status != esperado) {
            throw new TransicaoStatusInvalidaException(this.status, desejado);
        }
    }

    // ---------- Itens ----------

    public ItemServico inserirServico(ServicoId servicoId, Dinheiro valorCobrado) {
        exigirEdicaoDeItens();
        ItemServico item = ItemServico.novo(servicoId, valorCobrado);
        itensServico.add(item);
        return item;
    }

    public ItemPeca inserirPeca(PecaId pecaId, int quantidade, Dinheiro valorUnitario) {
        exigirEdicaoDeItens();
        ItemPeca item = ItemPeca.novo(pecaId, quantidade, valorUnitario);
        itensPeca.add(item);
        return item;
    }

    public void removerItemServico(UUID itemId) {
        exigirEdicaoDeItens();
        boolean removido = itensServico.removeIf(i -> i.id().equals(itemId));
        if (!removido) {
            throw new ItemOSNaoEncontradoException(itemId);
        }
    }

    public void removerItemPeca(UUID itemId) {
        exigirEdicaoDeItens();
        boolean removido = itensPeca.removeIf(i -> i.id().equals(itemId));
        if (!removido) {
            throw new ItemOSNaoEncontradoException(itemId);
        }
    }

    private void exigirEdicaoDeItens() {
        if (status != StatusOS.RECEBIDA && status != StatusOS.EM_DIAGNOSTICO) {
            throw new TransicaoStatusInvalidaException(
                    "Itens só podem ser editados em RECEBIDA ou EM_DIAGNOSTICO; status atual: " + status);
        }
        if (orcamento != null) {
            throw new OrcamentoJaGeradoException();
        }
    }

    // ---------- Orçamento ----------

    public Orcamento gerarOrcamento() {
        if (status != StatusOS.EM_DIAGNOSTICO) {
            throw new TransicaoStatusInvalidaException(
                    "Orçamento só pode ser gerado em EM_DIAGNOSTICO; status atual: " + status);
        }
        if (orcamento != null) {
            throw new OrcamentoJaGeradoException();
        }
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new OrcamentoVazioException();
        }
        Dinheiro total = Dinheiro.ZERO;
        for (ItemServico s : itensServico) {
            total = total.somar(s.valorCobrado());
        }
        for (ItemPeca p : itensPeca) {
            total = total.somar(p.subtotal());
        }
        this.orcamento = Orcamento.gerar(total);
        return this.orcamento;
    }

    /**
     * Preenche o {@link ReservaId} no item de peça correspondente. Chamado pelo use case
     * que orquestra a reserva no BC Estoque, logo após gerar o orçamento.
     */
    public void registrarReserva(UUID itemPecaId, ReservaId reservaId) {
        ItemPeca item = itensPeca.stream()
                .filter(i -> i.id().equals(itemPecaId))
                .findFirst()
                .orElseThrow(() -> new ItemOSNaoEncontradoException(itemPecaId));
        item.registrarReserva(reservaId);
    }

    // ---------- Acessores ----------

    public OrdemServicoId id()                  { return id; }
    public ClienteId clienteId()                { return clienteId; }
    public VeiculoId veiculoId()                { return veiculoId; }
    public StatusOS status()                    { return status; }
    public List<ItemServico> itensServico()     { return Collections.unmodifiableList(itensServico); }
    public List<ItemPeca> itensPeca()           { return Collections.unmodifiableList(itensPeca); }
    public Orcamento orcamento()                { return orcamento; }
    public OffsetDateTime criadaEm()            { return criadaEm; }
    public OffsetDateTime diagnosticoIniciadoEm() { return diagnosticoIniciadoEm; }
    public OffsetDateTime execucaoIniciadaEm()  { return execucaoIniciadaEm; }
    public OffsetDateTime finalizadaEm()        { return finalizadaEm; }
    public OffsetDateTime entregueEm()          { return entregueEm; }
    public OffsetDateTime canceladaEm()         { return canceladaEm; }

    /** Momento da última mudança de estado — usado na consulta de status. */
    public OffsetDateTime atualizadaEm() {
        OffsetDateTime ultima = criadaEm;
        OffsetDateTime[] marcos = {
                diagnosticoIniciadoEm,
                orcamento == null ? null : orcamento.geradoEm(),
                orcamento == null ? null : orcamento.aprovadoEm(),
                execucaoIniciadaEm, finalizadaEm, entregueEm, canceladaEm};
        for (OffsetDateTime marco : marcos) {
            if (marco != null && marco.isAfter(ultima)) {
                ultima = marco;
            }
        }
        return ultima;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdemServico that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
