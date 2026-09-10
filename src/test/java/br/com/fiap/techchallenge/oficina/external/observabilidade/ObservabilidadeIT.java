package br.com.fiap.techchallenge.oficina.external.observabilidade;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova ponta a ponta da telemetria de negócio: percorre o ciclo de vida real de
 * uma OS via HTTP e verifica que as métricas exigidas pela fase 3 aparecem no
 * endpoint {@code /q/metrics} — com os valores certos.
 *
 * <p>Diferente de {@code MicrometerMetricasGatewayTest} (que testa o adapter
 * isolado), aqui a aplicação inteira está no ar, contra Postgres real via
 * Testcontainers. Se a instrumentação não estiver plugada no fluxo, este teste
 * falha.
 */
@QuarkusTest
@TestSecurity(user = "admin", roles = "ADMINISTRADOR")
class ObservabilidadeIT {

    @Test
    @DisplayName("o ciclo de vida da OS publica as três métricas do enunciado em /q/metrics")
    void cicloDeVidaPublicaMetricasDeNegocio() {
        double abertasAntes = valorDaMetrica(metricas(), "oficina_os_abertas_total");

        percorrerCicloCompletoDeUmaOS();

        String metricas = metricas();

        // 1) Volume de OS — o contador subiu exatamente uma unidade.
        assertEquals(abertasAntes + 1, valorDaMetrica(metricas, "oficina_os_abertas_total"), 0.001,
                "abrir uma OS deve incrementar oficina_os_abertas_total. Linhas publicadas:"
                        + trecho(metricas, "oficina_os_abertas"));

        // 2) Tempo por status — as três fases do enunciado foram medidas.
        //    Cada transição fecha uma fase: aprovar → Diagnóstico, finalizar →
        //    Execução, entregar → Finalizada.
        for (String fase : new String[]{"Diagnóstico", "Execução", "Finalizada"}) {
            assertTrue(metricas.contains("fase=\"" + fase + "\""),
                    "faltou o timer da fase '" + fase + "' em /q/metrics:\n" + trecho(metricas, "oficina_os_fase"));
        }

        // 3) Latência das APIs — o binder HTTP do Micrometer está ativo.
        assertTrue(metricas.contains("http_server_requests_seconds"),
                "métricas de latência HTTP ausentes");

        // 4) Correlação métrica ↔ trace: o contador sai com exemplar OpenMetrics
        //    carregando o trace_id do span que o incrementou. É o que permite
        //    saltar do gráfico para o trace da requisição no New Relic.
        assertTrue(metricas.lines()
                        .filter(l -> l.startsWith("oficina_os_abertas_total"))
                        .anyMatch(l -> l.contains("trace_id=")),
                "contador sem exemplar de trace — correlação métrica/trace quebrada:"
                        + trecho(metricas, "oficina_os_abertas"));
    }

    @Test
    @DisplayName("uma operação inválida incrementa o contador que alimenta o alerta de falhas")
    void falhaDeProcessamentoIncrementaContador() {
        String osId = abrirOrdemServico("888.999.000-78", "OBV2B22");
        double falhasAntes = valorDaMetrica(metricas(), "oficina_os_falhas_total");

        // Finalizar uma OS recém-aberta viola a máquina de estados: a operação é
        // recusada com 409 (conflito de estado) e a falha precisa ser contabilizada.
        given().when().post("/ordens-servico/" + osId + "/finalizar")
                .then().statusCode(409);

        assertEquals(falhasAntes + 1, valorDaMetrica(metricas(), "oficina_os_falhas_total"), 0.001,
                "transição inválida deve incrementar oficina_os_falhas_total");
    }

    // ------------------------------------------------------------------
    // Fluxo
    // ------------------------------------------------------------------

    private void percorrerCicloCompletoDeUmaOS() {
        String osId = abrirOrdemServico("555.666.777-20", "OBV1A11");

        given().when().post("/ordens-servico/" + osId + "/diagnostico")
                .then().statusCode(200).body("status", equalTo("EM_DIAGNOSTICO"));

        // Orçamento exige ao menos um item — OS vazia é recusada com 422.
        String servicoId = given().contentType("application/json")
                .body(Map.of("descricao", "Revisão observabilidade", "valorBase", "150.00"))
                .when().post("/servicos")
                .then().statusCode(201)
                .extract().path("id");
        given().contentType("application/json")
                .body(Map.of("servicoId", servicoId, "valorCobrado", "150.00"))
                .when().post("/ordens-servico/" + osId + "/servicos")
                .then().statusCode(200);

        given().when().post("/ordens-servico/" + osId + "/orcamento")
                .then().statusCode(200);
        given().when().post("/ordens-servico/" + osId + "/orcamento/enviar")
                .then().statusCode(200).body("status", equalTo("AGUARDANDO_APROVACAO"));
        given().when().post("/ordens-servico/" + osId + "/orcamento/aprovar")
                .then().statusCode(200).body("status", equalTo("EM_EXECUCAO"));
        given().when().post("/ordens-servico/" + osId + "/finalizar")
                .then().statusCode(200).body("status", equalTo("FINALIZADA"));
        given().when().post("/ordens-servico/" + osId + "/entregar")
                .then().statusCode(200).body("status", equalTo("ENTREGUE"));
    }

    private String abrirOrdemServico(String documento, String placa) {
        String clienteId = given().contentType("application/json")
                .body(Map.of("nome", "Cliente Observabilidade", "documento", documento))
                .when().post("/clientes")
                .then().statusCode(201)
                .extract().path("id");

        given().contentType("application/json")
                .body(Map.of("placa", placa, "marca", "Fiat", "modelo", "Uno",
                        "ano", 2015, "clienteId", clienteId))
                .when().post("/veiculos")
                .then().statusCode(201);

        return given().contentType("application/json")
                .body(Map.of(
                        "cliente", Map.of("documento", documento.replaceAll("\\D", "")),
                        "veiculo", Map.of("placa", placa)))
                .when().post("/ordens-servico")
                .then().statusCode(201).body("id", notNullValue())
                .extract().path("id");
    }

    // ------------------------------------------------------------------
    // Leitura do /q/metrics (formato texto do Prometheus)
    // ------------------------------------------------------------------

    private String metricas() {
        return given().when().get("/q/metrics").then().statusCode(200).extract().asString();
    }

    /**
     * Soma todas as séries de um contador, ignorando metadados ({@code # HELP} /
     * {@code # TYPE}). Devolve 0 se a métrica ainda não existe — um contador só
     * aparece depois do primeiro incremento.
     *
     * <p>Duas armadilhas do formato:
     * <ul>
     *   <li>o nome precisa casar até o delimitador ({@code " "} ou <code>"{"</code>),
     *       senão séries de nome parecido entram na conta;</li>
     *   <li>cada linha pode trazer um <b>exemplar</b> depois de {@code " # "}
     *       — {@code {trace_id=…} valor timestamp} — e o timestamp dele é o
     *       último campo da linha. Ler o fim da linha crua devolveria o epoch.</li>
     * </ul>
     */
    private static double valorDaMetrica(String corpo, String nome) {
        return corpo.lines()
                .filter(l -> !l.startsWith("#"))
                .filter(l -> l.startsWith(nome + " ") || l.startsWith(nome + "{"))
                .map(l -> l.contains(" # ") ? l.substring(0, l.indexOf(" # ")) : l)
                .mapToDouble(l -> Double.parseDouble(l.substring(l.lastIndexOf(' ') + 1).trim()))
                .sum();
    }

    /** Recorte do corpo para a mensagem de erro ficar legível quando algo falha. */
    private static String trecho(String corpo, String prefixo) {
        String encontrado = corpo.lines()
                .filter(l -> l.startsWith(prefixo))
                .reduce("", (a, b) -> a + "\n" + b);
        return encontrado.isBlank() ? "(nenhuma linha começando com '" + prefixo + "')" : encontrado;
    }
}
