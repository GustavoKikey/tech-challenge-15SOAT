package br.com.fiap.techchallenge.oficina.external.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Prova da autenticação por CPF (fase 3): o cliente autenticado só enxerga e só
 * decide sobre <b>as próprias</b> ordens de serviço.
 *
 * <p>O cenário central é o do "cliente curioso": Bruno tem token válido e tenta
 * mexer na OS de Alice. Antes da fase 3 isso era possível para qualquer um, sem
 * token nenhum, via {@code POST /publico/ordens-servico/{id}/orcamento/decisao}.
 */
@QuarkusTest
class AreaClienteResourceIT {

    private static final AtomicInteger PLACAS = new AtomicInteger(1);

    // ------------------------------------------------------------------
    // Autorização de entrada
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sem token, a área do cliente responde 401")
    void semTokenNaoEntra() {
        given().when().get("/cliente/ordens-servico").then().statusCode(401);
    }

    @Test
    @DisplayName("token administrativo não abre a área do cliente (403)")
    @TestSecurity(user = "admin", roles = "ADMINISTRADOR")
    void tokenAdministrativoNaoEntra() {
        given().when().get("/cliente/ordens-servico").then().statusCode(403);
    }

    @Test
    @DisplayName("token com 'sub' fora do contrato do ADR 001 é rejeitado (400)")
    @TestSecurity(user = "nao-e-uuid", roles = "CLIENTE")
    void subInvalidoRejeitado() {
        given().when().get("/cliente/ordens-servico").then().statusCode(400);
    }

    // ------------------------------------------------------------------
    // Isolamento entre clientes — o coração do requisito
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cliente só lista as próprias OS")
    void listaApenasAsProprias() {
        String alice = criarClienteComOS("Alice");
        criarClienteComOS("Bruno");

        comoCliente(alice).when().get("/cliente/ordens-servico")
                .then().statusCode(200).body("", hasSize(1));
    }

    @Test
    @DisplayName("cliente NÃO acessa a OS de outro cliente (403)")
    void naoAcessaOSDeOutro() {
        String alice = criarClienteComOS("Alice");
        String osDeAlice = idDaPrimeiraOS(alice);
        String bruno = criarClienteComOS("Bruno");

        comoCliente(bruno).when().get("/cliente/ordens-servico/" + osDeAlice)
                .then().statusCode(403);
        comoCliente(bruno).when().get("/cliente/ordens-servico/" + osDeAlice + "/status")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("cliente NÃO aprova orçamento de OS alheia — o furo que a fase 3 fecha")
    void naoAprovaOrcamentoDeOutro() {
        String alice = criarClienteComOS("Alice");
        String osDeAlice = idDaPrimeiraOS(alice);
        String bruno = criarClienteComOS("Bruno");

        comoCliente(bruno).contentType("application/json")
                .body(Map.of("aprovado", true))
                .when().post("/cliente/ordens-servico/" + osDeAlice + "/orcamento/decisao")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("cliente aprova o orçamento da PRÓPRIA OS")
    void aprovaOOrcamentoDaPropriaOS() {
        String alice = criarClienteComOS("Alice");
        String os = idDaPrimeiraOS(alice);
        levarAteAguardandoAprovacao(os);

        comoCliente(alice).contentType("application/json")
                .body(Map.of("aprovado", true))
                .when().post("/cliente/ordens-servico/" + os + "/orcamento/decisao")
                .then().statusCode(200).body("status", equalTo("EM_EXECUCAO"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private RequestSpecification comoCliente(String clienteId) {
        return given().auth().oauth2(TokenDeTeste.paraCliente(clienteId));
    }

    private RequestSpecification comoAdmin() {
        return given().auth().oauth2(TokenDeTeste.paraAdministrador());
    }

    /** Cria cliente + veículo + uma OS aberta, e devolve o id do cliente. */
    private String criarClienteComOS(String nome) {
        String documento = TokenDeTeste.cpfValido();
        String placa = "ACL%04d".formatted(PLACAS.getAndIncrement());

        String clienteId = comoAdmin().contentType("application/json")
                .body(Map.of("nome", nome, "documento", documento))
                .when().post("/clientes")
                .then().statusCode(201)
                .extract().path("id");

        comoAdmin().contentType("application/json")
                .body(Map.of("placa", placa, "marca", "Fiat", "modelo", "Uno",
                        "ano", 2015, "clienteId", clienteId))
                .when().post("/veiculos")
                .then().statusCode(201);

        comoAdmin().contentType("application/json")
                .body(Map.of("cliente", Map.of("documento", documento),
                        "veiculo", Map.of("placa", placa)))
                .when().post("/ordens-servico")
                .then().statusCode(201);

        return clienteId;
    }

    private String idDaPrimeiraOS(String clienteId) {
        return comoCliente(clienteId).when().get("/cliente/ordens-servico")
                .then().statusCode(200)
                .extract().path("[0].id");
    }

    /** Fluxo interno (atendente) até a OS ficar pendente de decisão do cliente. */
    private void levarAteAguardandoAprovacao(String os) {
        comoAdmin().when().post("/ordens-servico/" + os + "/diagnostico").then().statusCode(200);
        String servicoId = comoAdmin().contentType("application/json")
                .body(Map.of("descricao", "Revisao area cliente", "valorBase", "150.00"))
                .when().post("/servicos").then().statusCode(201).extract().path("id");
        comoAdmin().contentType("application/json")
                .body(Map.of("servicoId", servicoId, "valorCobrado", "150.00"))
                .when().post("/ordens-servico/" + os + "/servicos").then().statusCode(200);
        comoAdmin().when().post("/ordens-servico/" + os + "/orcamento").then().statusCode(200);
        comoAdmin().when().post("/ordens-servico/" + os + "/orcamento/enviar")
                .then().statusCode(200).body("status", equalTo("AGUARDANDO_APROVACAO"));
    }
}
