package br.com.fiap.techchallenge.oficina.external.observabilidade;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testa o adapter de métricas contra um registry real em memória
 * ({@link SimpleMeterRegistry}) — sem Quarkus, sem rede.
 */
class MicrometerMetricasGatewayTest {

    private MeterRegistry registry;
    private MicrometerMetricasGateway gateway;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        gateway = new MicrometerMetricasGateway(registry);
    }

    @Test
    @DisplayName("conta cada OS aberta (base do volume diário)")
    void contaOrdensAbertas() {
        gateway.ordemServicoAberta();
        gateway.ordemServicoAberta();

        assertEquals(2.0, registry.counter("oficina.os.abertas").count());
    }

    @Test
    @DisplayName("registra a duração de cada fase separadamente, por tag")
    void registraDuracaoPorFase() {
        gateway.faseConcluida("Diagnóstico", Duration.ofMinutes(30));
        gateway.faseConcluida("Execução", Duration.ofHours(2));
        gateway.faseConcluida("Execução", Duration.ofHours(4));

        var diagnostico = registry.timer("oficina.os.fase.duracao", "fase", "Diagnóstico");
        var execucao = registry.timer("oficina.os.fase.duracao", "fase", "Execução");

        assertEquals(1, diagnostico.count());
        assertEquals(30.0, diagnostico.totalTime(TimeUnit.MINUTES), 0.01);

        assertEquals(2, execucao.count());
        assertEquals(3.0, execucao.mean(TimeUnit.HOURS), 0.01, "média de 2h e 4h");
    }

    @Test
    @DisplayName("ignora duração ausente ou negativa em vez de poluir a média")
    void ignoraDuracaoInvalida() {
        gateway.faseConcluida("Execução", null);
        gateway.faseConcluida("Execução", Duration.ofMinutes(-5));
        gateway.faseConcluida(null, Duration.ofMinutes(10));

        assertTrue(registry.find("oficina.os.fase.duracao").timers().isEmpty());
    }

    @Test
    @DisplayName("separa falhas de processamento por operação e integrações por sistema")
    void contaFalhas() {
        gateway.falhaProcessamento("finalizar");
        gateway.falhaIntegracao("email");

        assertEquals(1.0, registry.counter("oficina.os.falhas", "operacao", "finalizar").count());
        assertEquals(1.0, registry.counter("oficina.integracao.falhas", "integracao", "email").count());
    }

    @Test
    @DisplayName("tag nula vira 'desconhecida' em vez de estourar")
    void toleraTagNula() {
        assertDoesNotThrow(() -> gateway.falhaProcessamento(null));

        assertEquals(1.0, registry.counter("oficina.os.falhas", "operacao", "desconhecida").count());
    }

    @Test
    @DisplayName("falha do registry não derruba a operação de negócio")
    void engoleErroDeTelemetria() {
        MeterRegistry quebrado = mock(MeterRegistry.class);
        when(quebrado.counter(anyString())).thenThrow(new IllegalStateException("registry fora do ar"));
        MicrometerMetricasGateway comFalha = new MicrometerMetricasGateway(quebrado);

        assertDoesNotThrow(comFalha::ordemServicoAberta);
    }
}
