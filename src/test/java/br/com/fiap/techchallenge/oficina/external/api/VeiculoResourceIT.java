package br.com.fiap.techchallenge.oficina.external.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestSecurity(user = "admin", roles = "ADMINISTRADOR")
class VeiculoResourceIT {

    private String criarCliente(String nome, String documento) {
        return given()
                .contentType("application/json")
                .body(Map.of("nome", nome, "documento", documento))
                .when().post("/clientes")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    void cadastraBuscaListaEAtualizaVeiculo() {
        String clienteId = criarCliente("Carlos", "500.000.005-66");

        String veiculoId = given().contentType("application/json")
                .body(Map.of(
                        "placa", "ABC-1234",
                        "marca", "Fiat",
                        "modelo", "Uno",
                        "ano", 2010,
                        "clienteId", clienteId))
                .when().post("/veiculos")
                .then().statusCode(201)
                    .body("placa", equalTo("ABC1234"))
                    .body("id", notNullValue())
                .extract().path("id");

        given().when().get("/veiculos/" + veiculoId)
                .then().statusCode(200).body("modelo", equalTo("Uno"));

        given().when().get("/veiculos/cliente/" + clienteId)
                .then().statusCode(200).body("size()", greaterThanOrEqualTo(1));

        given().contentType("application/json")
                .body(Map.of("marca", "VW", "modelo", "Gol", "ano", 2018))
                .when().put("/veiculos/" + veiculoId)
                .then().statusCode(200).body("marca", equalTo("VW"));

        given().when().delete("/veiculos/" + veiculoId).then().statusCode(204);
    }

    @Test
    void placaInvalidaRetorna400() {
        String clienteId = criarCliente("Diana", "11.222.333/0001-81");
        given().contentType("application/json")
                .body(Map.of("placa", "AB1234", "marca", "X", "modelo", "Y", "ano", 2010, "clienteId", clienteId))
                .when().post("/veiculos")
                .then().statusCode(400);
    }

    @Test
    void clienteInexistenteRetorna404() {
        given().contentType("application/json")
                .body(Map.of("placa", "XYZ-9876", "marca", "X", "modelo", "Y", "ano", 2010,
                        "clienteId", "00000000-0000-0000-0000-000000000001"))
                .when().post("/veiculos")
                .then().statusCode(404);
    }

    @Test
    void placaDuplicadaRetorna409() {
        String clienteId = criarCliente("Edu", "22.222.222/0001-91");
        Map<String, Object> body = Map.of("placa", "MNO1A23", "marca", "X", "modelo", "Y",
                "ano", 2020, "clienteId", clienteId);
        given().contentType("application/json").body(body)
                .when().post("/veiculos").then().statusCode(201);
        given().contentType("application/json").body(body)
                .when().post("/veiculos").then().statusCode(409);
    }
}
