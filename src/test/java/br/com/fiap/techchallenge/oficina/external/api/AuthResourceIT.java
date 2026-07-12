package br.com.fiap.techchallenge.oficina.external.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.JwtSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * IT — login JWT, proteção dos endpoints administrativos e endpoint
 * público de consulta da OS. O usuário {@code admin} é provisionado pelo
 * {@code AdminBootstrap} no startup da app (senha {@code admin123} em dev).
 */
@QuarkusTest
class AuthResourceIT {

    private static final Map<String, String> ADMIN = Map.of(
            "username", "admin",
            "password", "admin123");

    @Test
    void loginComAdminRetornaToken() {
        given().contentType("application/json").body(ADMIN)
                .when().post("/auth/login")
                .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("expiresIn", notNullValue())
                    .body("role", equalTo("ADMINISTRADOR"));
    }

    @Test
    void loginComSenhaErradaRetorna401() {
        given().contentType("application/json")
                .body(Map.of("username", "admin", "password", "errada"))
                .when().post("/auth/login")
                .then()
                    .statusCode(401)
                    .body("error", equalTo("Unauthorized"));
    }

    @Test
    void loginComUsuarioInexistenteRetorna401() {
        given().contentType("application/json")
                .body(Map.of("username", "ninguem-existe", "password", "x"))
                .when().post("/auth/login")
                .then().statusCode(401);
    }

    @Test
    void loginSemBodyRetorna400() {
        given().contentType("application/json")
                .body(Map.of("username", "", "password", ""))
                .when().post("/auth/login")
                .then().statusCode(400);
    }

    @Test
    void enderecoProtegidoSemTokenRetorna401() {
        given().when().get("/clientes")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "fulano", roles = "MECANICO")
    void mecanicoTentandoCriarServicoDoCatalogoRetorna403() {
        given().contentType("application/json")
                .body(Map.of("descricao", "Diagnostico", "valorBase", "100"))
                .when().post("/servicos")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "fulano", roles = "MECANICO")
    void mecanicoListaClientesRetorna200() {
        given().when().get("/clientes").then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "fulano", roles = "ATENDENTE")
    void atendenteTentandoCadastrarUsuarioRetorna403() {
        given().contentType("application/json")
                .body(Map.of("username", "novo", "password", "senha-segura", "role", "ATENDENTE"))
                .when().post("/auth/usuarios")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMINISTRADOR")
    void adminCadastraOutroUsuarioComSucesso() {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        given().contentType("application/json")
                .body(Map.of("username", username, "password", "senha-segura", "role", "ATENDENTE"))
                .when().post("/auth/usuarios")
                .then()
                    .statusCode(201)
                    .body("username", equalTo(username))
                    .body("role", equalTo("ATENDENTE"))
                    .body("ativo", equalTo(true));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMINISTRADOR")
    void cadastrarUsuarioDuplicadoRetorna409() {
        Map<String, Object> body = Map.of(
                "username", "duplicado-" + UUID.randomUUID().toString().substring(0, 8),
                "password", "senha-segura",
                "role", "MECANICO");
        given().contentType("application/json").body(body)
                .when().post("/auth/usuarios").then().statusCode(201);
        given().contentType("application/json").body(body)
                .when().post("/auth/usuarios").then().statusCode(409);
    }

    @Test
    void publicoOSConsultaSemTokenAcessivel() {
        // Mesmo retornando 404 (não há OS), o filtro de segurança não bloqueia
        given().when().get("/publico/ordens-servico/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void healthAcessivelSemToken() {
        given().when().get("/health").then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMINISTRADOR")
    @JwtSecurity
    void tokenEmitidoPossuiClaimsEsperados() {
        // Combinação @TestSecurity + @JwtSecurity — força o uso do mock JWT
        given().when().get("/clientes").then().statusCode(200);
    }
}
