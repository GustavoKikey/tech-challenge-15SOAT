package br.com.fiap.techchallenge.oficina.atendimento.gateways;

import java.time.Duration;

/**
 * Porta de saída para telemetria de negócio da Ordem de Serviço
 * (fase 3: "dashboards com volume diário de ordens de serviço, tempo médio de
 * execução por status e erros/falhas nas integrações").
 *
 * <p>Definida no núcleo, sem qualquer tipo de framework: o domínio declara
 * <i>o que</i> vale medir, sem saber <i>como</i> (Micrometer, OpenTelemetry,
 * StatsD…). Implementação em {@code external/observabilidade}.
 *
 * <p>Registro de métrica é <i>best-effort</i>, exatamente como
 * {@link NotificacaoGateway}: falha de telemetria nunca pode derrubar uma
 * operação de negócio, então implementações devem capturar e logar erros.
 */
public interface MetricasGateway {

    /**
     * Uma OS foi aberta. Alimenta o dashboard de <b>volume diário</b> —
     * a agregação por dia é responsabilidade da ferramenta de observabilidade,
     * não da aplicação.
     */
    void ordemServicoAberta();

    /**
     * Uma fase do ciclo de vida da OS terminou, com quanto tempo durou.
     * Alimenta o dashboard de <b>tempo médio de execução por status</b>.
     *
     * @param fase    valor de {@code StatusOS.descricao()} da fase encerrada
     *                (ex.: {@code "Diagnóstico"}, {@code "Execução"}).
     * @param duracao tempo decorrido entre a entrada e a saída daquela fase.
     */
    void faseConcluida(String fase, Duration duracao);

    /**
     * Uma operação sobre a OS falhou. Base para o alerta de
     * <b>falhas no processamento de ordens de serviço</b>.
     *
     * @param operacao nome curto e de baixa cardinalidade da operação
     *                 (ex.: {@code "abrir"}, {@code "finalizar"}) — nunca use
     *                 identificadores aqui: cada valor distinto vira uma série
     *                 temporal na ferramenta de métricas.
     */
    void falhaProcessamento(String operacao);

    /**
     * Uma integração externa falhou. Alimenta o painel de
     * <b>erros e falhas nas integrações</b>.
     *
     * @param integracao nome do sistema externo (ex.: {@code "email"}).
     */
    void falhaIntegracao(String integracao);
}
