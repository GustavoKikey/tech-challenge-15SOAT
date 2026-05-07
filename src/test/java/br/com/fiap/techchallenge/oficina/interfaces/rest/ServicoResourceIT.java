package br.com.fiap.techchallenge.oficina.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "ADMINISTRADOR")
class ServicoResourceIT {

    @Test
    void crudCompletoServico() {
        String id = given().contentType("application/json")
                .body(Map.of("descricao", "Troca de óleo", "valorBase", "99.90"))
                .when().post("/servicos")
                .then().statusCode(201)
                    .body("id", notNullValue())
                    .body("valorBase", equalTo(99.90f))
                .extract().path("id");

        given().when().get("/servicos/" + id)
                .then().statusCode(200).body("descricao", equalTo("Troca de óleo"));

        given().contentType("application/json")
                .body(Map.of("descricao", "Troca de óleo + filtro", "valorBase", "149.90"))
                .when().put("/servicos/" + id)
                .then().statusCode(200).body("valorBase", equalTo(149.90f));

        given().when().delete("/servicos/" + id).then().statusCode(204);
    }

    @Test
    void valorNegativoRetorna400() {
        given().contentType("application/json")
                .body(Map.of("descricao", "Inválido", "valorBase", "-1.00"))
                .when().post("/servicos")
                .then().statusCode(400);
    }

    @Test
    void buscarInexistenteRetorna404() {
        given().when().get("/servicos/" + UUID.randomUUID())
                .then().statusCode(404);
    }
}
