package br.com.fiap.techchallenge.oficina.atendimento.entities;

/**
 * Status do ciclo de vida de uma Ordem de Serviço.
 *
 * <p>Transições válidas (Event Storming):
 * <pre>
 *   (criação) → RECEBIDA
 *   RECEBIDA              → EM_DIAGNOSTICO
 *   EM_DIAGNOSTICO        → AGUARDANDO_APROVACAO
 *   AGUARDANDO_APROVACAO  → EM_EXECUCAO   (orçamento aprovado)
 *   AGUARDANDO_APROVACAO  → CANCELADA     (orçamento recusado)
 *   EM_EXECUCAO           → FINALIZADA
 *   FINALIZADA            → ENTREGUE
 * </pre>
 *
 * <p>Cada status carrega a descrição amigável exibida ao cliente e a prioridade
 * usada na listagem operacional (menor = mais urgente). Status encerrados
 * (FINALIZADA, ENTREGUE, CANCELADA) ficam fora da listagem padrão — exclusão
 * lógica, nunca física.
 */
public enum StatusOS {
    RECEBIDA("Recebida", 4, true),
    EM_DIAGNOSTICO("Diagnóstico", 3, true),
    AGUARDANDO_APROVACAO("Aguardando Aprovação", 2, true),
    EM_EXECUCAO("Execução", 1, true),
    FINALIZADA("Finalizada", 9, false),
    ENTREGUE("Entregue", 9, false),
    CANCELADA("Cancelada", 9, false);

    private final String descricao;
    private final int prioridadeListagem;
    private final boolean visivelNaListagemPadrao;

    StatusOS(String descricao, int prioridadeListagem, boolean visivelNaListagemPadrao) {
        this.descricao = descricao;
        this.prioridadeListagem = prioridadeListagem;
        this.visivelNaListagemPadrao = visivelNaListagemPadrao;
    }

    /** Descrição amigável exigida pela consulta de status (ex.: "Aguardando Aprovação"). */
    public String descricao() {
        return descricao;
    }

    /** Prioridade na listagem: Em Execução &gt; Aguardando Aprovação &gt; Diagnóstico &gt; Recebida. */
    public int prioridadeListagem() {
        return prioridadeListagem;
    }

    /** Encerradas (finalizada/entregue/cancelada) saem da listagem padrão (exclusão lógica). */
    public boolean visivelNaListagemPadrao() {
        return visivelNaListagemPadrao;
    }
}
