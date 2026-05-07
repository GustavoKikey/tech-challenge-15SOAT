package br.com.fiap.techchallenge.oficina.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Teste de integração ponta a ponta do fluxo da Ordem de Serviço via REST.
 *
 * <p>Cobre os três cenários:
 * <ul>
 *   <li>fluxo feliz completo (criação → diagnóstico → orçamento → aprovação → entrega);</li>
 *   <li>orçamento com saldo insuficiente (422, sem efeito colateral no estoque);</li>
 *   <li>endpoint público com payload reduzido (sem ids de cliente/veículo, sem itens).</li>
 * </ul>
 */
@QuarkusTest
@TestSecurity(user = "admin", roles = "ADMINISTRADOR")
class OrdemServicoResourceIT {

    @Test
    void fluxoFelizCompletoAtualizaStatusEMovimentaEstoque() {
        String clienteId = criarCliente("José da Silva", "100.000.001-08");
        criarVeiculo(clienteId, "OSF-1A23");
        String pecaId = criarPeca("Filtro de óleo", "35.50", 5);
        String servicoId = criarServico("Troca de óleo", "120.00");

        // 1) Cria OS — status RECEBIDA
        String osId = given().contentType("application/json")
                .body(Map.of("documentoCliente", "10000000108", "placaVeiculo", "OSF1A23"))
                .when().post("/ordens-servico")
                .then().statusCode(201)
                    .body("id", notNullValue())
                    .body("status", equalTo("RECEBIDA"))
                    .body("criadaEm", notNullValue())
                .extract().path("id");

        // 2) Inicia diagnóstico — RECEBIDA → EM_DIAGNOSTICO
        given().when().post("/ordens-servico/" + osId + "/diagnostico")
                .then().statusCode(200)
                    .body("status", equalTo("EM_DIAGNOSTICO"))
                    .body("diagnosticoIniciadoEm", notNullValue());

        // 3) Adiciona um serviço
        given().contentType("application/json")
                .body(Map.of("servicoId", servicoId, "valorCobrado", "120.00"))
                .when().post("/ordens-servico/" + osId + "/servicos")
                .then().statusCode(200)
                    .body("itensServico.size()", equalTo(1));

        // 4) Adiciona uma peça (quantidade 2)
        given().contentType("application/json")
                .body(Map.of("pecaId", pecaId, "quantidade", 2, "valorUnitario", "35.50"))
                .when().post("/ordens-servico/" + osId + "/pecas")
                .then().statusCode(200)
                    .body("itensPeca.size()", equalTo(1))
                    .body("itensPeca[0].reservaId", nullValue()); // ainda não reservou

        // Sanidade: saldo da peça intacto antes do orçamento
        given().when().get("/pecas/" + pecaId)
                .then().statusCode(200)
                    .body("quantidadeTotal", equalTo(5))
                    .body("saldoDisponivel", equalTo(5));

        // 5) Gera orçamento — reserva peças no estoque (saldo disponível cai)
        given().when().post("/ordens-servico/" + osId + "/orcamento")
                .then().statusCode(200)
                    .body("orcamento.valorTotal", equalTo(191.00f)) // 120 + 2*35.50
                    .body("orcamento.geradoEm", notNullValue())
                    .body("itensPeca[0].reservaId", notNullValue());

        given().when().get("/pecas/" + pecaId)
                .then().statusCode(200)
                    .body("quantidadeTotal", equalTo(5))
                    .body("saldoDisponivel", equalTo(3)); // 5 - 2 reservadas

        // 6) Envia orçamento — EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
        given().when().post("/ordens-servico/" + osId + "/orcamento/enviar")
                .then().statusCode(200)
                    .body("status", equalTo("AGUARDANDO_APROVACAO"));

        // 7) Aprova orçamento — AGUARDANDO_APROVACAO → EM_EXECUCAO + baixa do saldo total
        given().when().post("/ordens-servico/" + osId + "/orcamento/aprovar")
                .then().statusCode(200)
                    .body("status", equalTo("EM_EXECUCAO"))
                    .body("orcamento.aprovadoEm", notNullValue())
                    .body("execucaoIniciadaEm", notNullValue());

        given().when().get("/pecas/" + pecaId)
                .then().statusCode(200)
                    .body("quantidadeTotal", equalTo(3))   // 5 - 2 baixadas
                    .body("saldoDisponivel", equalTo(3));

        // 8) Finaliza serviços — EM_EXECUCAO → FINALIZADA
        given().when().post("/ordens-servico/" + osId + "/finalizar")
                .then().statusCode(200)
                    .body("status", equalTo("FINALIZADA"))
                    .body("finalizadaEm", notNullValue());

        // 9) Entrega o veículo — FINALIZADA → ENTREGUE
        given().when().post("/ordens-servico/" + osId + "/entregar")
                .then().statusCode(200)
                    .body("status", equalTo("ENTREGUE"))
                    .body("entregueEm", notNullValue());
    }

    @Test
    void gerarOrcamentoComSaldoInsuficienteRetorna422EMantemEstoqueIntacto() {
        // Peça com saldo total = 1
        String pecaId = criarPeca("Vela única", "50.00", 1);

        // OS A reserva tudo
        String clienteAId = criarCliente("Cliente A", "200.000.002-99");
        criarVeiculo(clienteAId, "AAA-1A01");
        String osA = criarOSEAvancarParaDiagnostico("20000000299", "AAA1A01");

        given().contentType("application/json")
                .body(Map.of("pecaId", pecaId, "quantidade", 1, "valorUnitario", "50.00"))
                .when().post("/ordens-servico/" + osA + "/pecas")
                .then().statusCode(200);

        // OS B é montada antes de A reservar — saldo no momento ainda é 1
        String clienteBId = criarCliente("Cliente B", "300.000.003-88");
        criarVeiculo(clienteBId, "BBB-1A02");
        String osB = criarOSEAvancarParaDiagnostico("30000000388", "BBB1A02");

        given().contentType("application/json")
                .body(Map.of("pecaId", pecaId, "quantidade", 1, "valorUnitario", "50.00"))
                .when().post("/ordens-servico/" + osB + "/pecas")
                .then().statusCode(200);

        // A gera orçamento e reserva a única unidade
        given().when().post("/ordens-servico/" + osA + "/orcamento")
                .then().statusCode(200);

        given().when().get("/pecas/" + pecaId)
                .then().statusCode(200)
                    .body("saldoDisponivel", equalTo(0));

        // B tenta gerar orçamento — falha 422 e nada muda no estoque
        given().when().post("/ordens-servico/" + osB + "/orcamento")
                .then().statusCode(422)
                    .body("error", equalTo("Unprocessable Entity"));

        given().when().get("/pecas/" + pecaId)
                .then().statusCode(200)
                    .body("quantidadeTotal", equalTo(1))
                    .body("saldoDisponivel", equalTo(0)); // a reserva de A continua

        // OS B continua sem reserva associada
        given().when().get("/ordens-servico/" + osB)
                .then().statusCode(200)
                    .body("status", equalTo("EM_DIAGNOSTICO"))
                    .body("itensPeca[0].reservaId", nullValue());
    }

    @Test
    void endpointPublicoExpoeApenasResumoSemDadosSensiveis() {
        String clienteId = criarCliente("Maria Pública", "11.111.111/0001-91");
        criarVeiculo(clienteId, "PUB-9Z99");
        String osId = criarOSEAvancarParaDiagnostico("11111111000191", "PUB9Z99");

        given().when().get("/publico/ordens-servico/" + osId)
                .then().statusCode(200)
                    .body("id", equalTo(osId))
                    .body("status", equalTo("EM_DIAGNOSTICO"))
                    .body("criadaEm", notNullValue())
                    // Campos sensíveis não estão presentes no payload público
                    .body("clienteId", nullValue())
                    .body("veiculoId", nullValue())
                    .body("itensServico", nullValue())
                    .body("itensPeca", nullValue());
    }

    @Test
    void buscarOrdemInexistenteRetorna404() {
        given().when().get("/ordens-servico/00000000-0000-0000-0000-000000000999")
                .then().statusCode(404);
    }

    @Test
    void criarOSComClienteInexistenteRetorna404() {
        given().contentType("application/json")
                .body(Map.of("documentoCliente", "400.000.004-77", "placaVeiculo", "ZZZ9X99"))
                .when().post("/ordens-servico")
                .then().statusCode(404);
    }

    // ---------------------------------------------------------------- helpers

    private String criarCliente(String nome, String documento) {
        return given().contentType("application/json")
                .body(Map.of("nome", nome, "documento", documento))
                .when().post("/clientes")
                .then().statusCode(201)
                .extract().path("id");
    }

    private void criarVeiculo(String clienteId, String placa) {
        given().contentType("application/json")
                .body(Map.of("placa", placa, "marca", "Fiat", "modelo", "Uno",
                        "ano", 2015, "clienteId", clienteId))
                .when().post("/veiculos")
                .then().statusCode(201);
    }

    private String criarPeca(String descricao, String valor, int quantidadeInicial) {
        return given().contentType("application/json")
                .body(Map.of("descricao", descricao,
                        "valorUnitario", valor,
                        "quantidadeInicial", quantidadeInicial))
                .when().post("/pecas")
                .then().statusCode(201)
                .extract().path("id");
    }

    private String criarServico(String descricao, String valor) {
        return given().contentType("application/json")
                .body(Map.of("descricao", descricao, "valorBase", valor))
                .when().post("/servicos")
                .then().statusCode(201)
                .extract().path("id");
    }

    private String criarOSEAvancarParaDiagnostico(String documento, String placa) {
        String id = given().contentType("application/json")
                .body(Map.of("documentoCliente", documento, "placaVeiculo", placa))
                .when().post("/ordens-servico")
                .then().statusCode(201)
                .extract().path("id");
        given().when().post("/ordens-servico/" + id + "/diagnostico")
                .then().statusCode(200);
        return id;
    }

}
