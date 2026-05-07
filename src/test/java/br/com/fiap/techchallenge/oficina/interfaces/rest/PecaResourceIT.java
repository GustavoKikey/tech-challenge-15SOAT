package br.com.fiap.techchallenge.oficina.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "ADMINISTRADOR")
class PecaResourceIT {

    @Test
    void crudCompletoPeca() {
        String id = given().contentType("application/json")
                .body(Map.of(
                        "descricao", "Filtro de óleo",
                        "valorUnitario", "35.50",
                        "quantidadeInicial", 5))
                .when().post("/pecas")
                .then().statusCode(201)
                    .body("id", notNullValue())
                    .body("quantidadeTotal", equalTo(5))
                    .body("saldoDisponivel", equalTo(5))
                .extract().path("id");

        given().when().get("/pecas/" + id)
                .then().statusCode(200)
                    .body("descricao", equalTo("Filtro de óleo"))
                    .body("saldoDisponivel", equalTo(5));

        given().contentType("application/json")
                .body(Map.of("descricao", "Filtro premium", "valorUnitario", "99.90"))
                .when().put("/pecas/" + id)
                .then().statusCode(200)
                    .body("descricao", equalTo("Filtro premium"))
                    .body("valorUnitario", equalTo(99.90f));

        given().when().get("/pecas")
                .then().statusCode(200);

        given().when().delete("/pecas/" + id).then().statusCode(204);
    }

    @Test
    void cadastrarSemQuantidadeInicialAssumeZero() {
        Map<String, Object> body = new HashMap<>();
        body.put("descricao", "Pastilha");
        body.put("valorUnitario", "80.00");

        given().contentType("application/json").body(body)
                .when().post("/pecas")
                .then().statusCode(201)
                    .body("quantidadeTotal", equalTo(0));
    }

    @Test
    void valorNegativoRetorna400() {
        given().contentType("application/json")
                .body(Map.of("descricao", "Inválido",
                        "valorUnitario", "-1.00",
                        "quantidadeInicial", 1))
                .when().post("/pecas")
                .then().statusCode(400);
    }

    @Test
    void buscarInexistenteRetorna404() {
        given().when().get("/pecas/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void adicionarSaldoAtualizaQuantidade() {
        String id = given().contentType("application/json")
                .body(Map.of("descricao", "Vela", "valorUnitario", "20.00", "quantidadeInicial", 0))
                .when().post("/pecas")
                .then().statusCode(201)
                .extract().path("id");

        given().contentType("application/json")
                .body(Map.of("quantidade", 8))
                .when().post("/pecas/" + id + "/saldo")
                .then().statusCode(200)
                    .body("quantidadeTotal", equalTo(8))
                    .body("saldoDisponivel", equalTo(8));
    }

    @Test
    void adicionarSaldoZeroRetorna400() {
        String id = given().contentType("application/json")
                .body(Map.of("descricao", "Bomba", "valorUnitario", "120.00", "quantidadeInicial", 0))
                .when().post("/pecas")
                .then().statusCode(201)
                .extract().path("id");

        given().contentType("application/json")
                .body(Map.of("quantidade", 0))
                .when().post("/pecas/" + id + "/saldo")
                .then().statusCode(400);
    }
}
