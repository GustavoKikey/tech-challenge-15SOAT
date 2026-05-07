package br.com.fiap.techchallenge.oficina.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class HealthResourceIT {

    @Test
    void healthEndpointReturnsUp() {
        given()
            .when().get("/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
