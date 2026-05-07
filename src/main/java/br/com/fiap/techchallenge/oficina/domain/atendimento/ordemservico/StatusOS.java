package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

/**
 * Status do ciclo de vida de uma Ordem de Serviço.
 *
 * <p>Transições válidas (Event Storming):
 * <pre>
 *   (criação) → RECEBIDA
 *   RECEBIDA              → EM_DIAGNOSTICO
 *   EM_DIAGNOSTICO        → AGUARDANDO_APROVACAO
 *   AGUARDANDO_APROVACAO  → EM_EXECUCAO
 *   EM_EXECUCAO           → FINALIZADA
 *   FINALIZADA            → ENTREGUE
 * </pre>
 */
public enum StatusOS {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE
}
