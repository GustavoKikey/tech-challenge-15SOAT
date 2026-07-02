package br.com.fiap.techchallenge.oficina.external.api;

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
class ClienteResourceIT {

    @Test
    void cadastraBuscaAtualizaERemove() {
        // Cadastra
        String id = given()
                .contentType("application/json")
                .body(Map.of(
                        "nome", "Ana Souza",
                        "documento", "111.444.777-35",
                        "email", "ana@x.com",
                        "telefone", "11999990000"))
                .when().post("/clientes")
                .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("documento", equalTo("11144477735"))
                    .body("tipoDocumento", equalTo("CPF"))
                .extract().path("id");

        // Busca
        given().when().get("/clientes/" + id)
                .then().statusCode(200).body("nome", equalTo("Ana Souza"));

        // Atualiza
        given().contentType("application/json")
                .body(Map.of("nome", "Ana Maria", "email", "am@x.com", "telefone", "11"))
                .when().put("/clientes/" + id)
                .then().statusCode(200).body("nome", equalTo("Ana Maria"));

        // Remove
        given().when().delete("/clientes/" + id).then().statusCode(204);
        given().when().get("/clientes/" + id).then().statusCode(404);
    }

    @Test
    void documentoDuplicadoRetorna409() {
        Map<String, String> body = Map.of(
                "nome", "Beto",
                "documento", "390.533.447-05",
                "email", "b@x.com",
                "telefone", "");
        given().contentType("application/json").body(body)
                .when().post("/clientes").then().statusCode(201);
        given().contentType("application/json").body(body)
                .when().post("/clientes")
                .then().statusCode(409)
                    .body("error", equalTo("Conflict"));
    }

    @Test
    void documentoInvalidoRetorna400() {
        given().contentType("application/json")
                .body(Map.of("nome", "X", "documento", "111.111.111-11"))
                .when().post("/clientes")
                .then().statusCode(400);
    }

    @Test
    void payloadSemNomeRetorna400() {
        given().contentType("application/json")
                .body(Map.of("documento", "52998224725"))
                .when().post("/clientes")
                .then().statusCode(400)
                    .body("fields.field", org.hamcrest.Matchers.hasItem("nome"));
    }

    @Test
    void buscarInexistenteRetorna404() {
        given().when().get("/clientes/" + UUID.randomUUID())
                .then().statusCode(404);
    }
}
