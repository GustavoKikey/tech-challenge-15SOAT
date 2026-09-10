package br.com.fiap.techchallenge.oficina.external.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ConfiguracaoDemoResourceIT {

    /**
     * Este teste existe por causa de uma falha concreta: injetar a URL do
     * Gateway como {@code String} com valor padrão vazio faz o SmallRye
     * converter a string vazia em {@code null} e abortar o <em>startup</em>
     * com SRCFG00040 — a aplicação inteira não sobe.
     *
     * <p>O ambiente de teste é exatamente o cenário sem Gateway, então basta
     * a aplicação responder aqui para o contrato estar preservado.
     */
    @Test
    @DisplayName("responde mesmo sem API Gateway configurado")
    void respondeSemGatewayConfigurado() {
        given()
            .when().get("/config-demo")
            .then()
                .statusCode(200)
                .body("apiGatewayUrl", equalTo(""))
                .body("ambiente", notNullValue());
    }

    /**
     * São endereços de serviços públicos, não segredo — e o painel precisa
     * deles antes de qualquer login. Com a aplicação negando por padrão toda
     * rota não anotada, a ausência de {@code @PermitAll} transformaria isto
     * num 401 que só apareceria no navegador.
     */
    @Test
    @DisplayName("é público — o painel consulta antes de autenticar")
    void naoExigeAutenticacao() {
        given()
            .when().get("/config-demo")
            .then()
                .statusCode(200);
    }
}
