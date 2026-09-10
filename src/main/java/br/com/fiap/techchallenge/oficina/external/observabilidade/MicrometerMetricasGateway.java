package br.com.fiap.techchallenge.oficina.external.observabilidade;

import br.com.fiap.techchallenge.oficina.atendimento.gateways.MetricasGateway;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Adapter de {@link MetricasGateway} sobre Micrometer. As métricas saem no
 * formato Prometheus em {@code /q/metrics} e são coletadas pelo agente do
 * New Relic no cluster.
 *
 * <p>Todo registro é <i>best-effort</i>: um erro de telemetria é logado em
 * {@code WARN} e engolido, nunca propagado — a operação de negócio já
 * aconteceu e não pode ser desfeita por causa de um contador.
 *
 * <p>Nomes usam pontos (convenção do Micrometer); o registry Prometheus os
 * converte para {@code snake_case} — {@code oficina.os.abertas} vira
 * {@code oficina_os_abertas_total}.
 */
@ApplicationScoped
public class MicrometerMetricasGateway implements MetricasGateway {

    private static final Logger LOG = Logger.getLogger(MicrometerMetricasGateway.class);

    private final MeterRegistry registry;

    public MicrometerMetricasGateway(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void ordemServicoAberta() {
        seguro("oficina.os.abertas", () -> registry
                .counter("oficina.os.abertas")
                .increment());
    }

    @Override
    public void faseConcluida(String fase, Duration duracao) {
        // Duração ausente ou negativa indica timestamps inconsistentes (ex.: OS
        // migrada de outro sistema). Ignorar é melhor que poluir a média.
        if (fase == null || duracao == null || duracao.isNegative()) {
            return;
        }
        seguro("oficina.os.fase.duracao", () -> Timer.builder("oficina.os.fase.duracao")
                .description("Tempo que a OS permaneceu em cada fase do ciclo de vida")
                .tag("fase", fase)
                .register(registry)
                .record(duracao));
    }

    @Override
    public void falhaProcessamento(String operacao) {
        seguro("oficina.os.falhas", () -> registry
                .counter("oficina.os.falhas", "operacao", nuloVira(operacao))
                .increment());
    }

    @Override
    public void falhaIntegracao(String integracao) {
        seguro("oficina.integracao.falhas", () -> registry
                .counter("oficina.integracao.falhas", "integracao", nuloVira(integracao))
                .increment());
    }

    private void seguro(String metrica, Runnable registro) {
        try {
            registro.run();
        } catch (RuntimeException e) {
            LOG.warnf(e, "Falha ao registrar a métrica '%s' — operação de negócio não afetada.", metrica);
        }
    }

    /** Tag nula quebra o Micrometer; "desconhecida" mantém a série válida. */
    private static String nuloVira(String valor) {
        return valor == null ? "desconhecida" : valor;
    }
}
