package br.com.fiap.techchallenge.oficina.external.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class RelatorioResourceIT {

    @Inject EntityManager em;

    @Test
    void enderecoSemTokenRetorna401() {
        given().when().get("/relatorios/tempo-medio-execucao").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "fulano", roles = "MECANICO")
    void roleInsuficienteRetorna403() {
        given().when().get("/relatorios/tempo-medio-execucao").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMINISTRADOR")
    void semOSRelatorioRetornaMediaZero() {
        // Filtra por uma janela vazia para evitar interferência com OS criadas em outros ITs.
        given().when()
                .get("/relatorios/tempo-medio-execucao?desde=1900-01-01&ate=1900-01-02")
                .then()
                    .statusCode(200)
                    .body("amostra", equalTo(0))
                    .body("tempoMedioExecucaoSegundos", equalTo(0));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMINISTRADOR")
    void calculaMediaCorretaQuandoExistemOSFinalizadas() {
        UUID cliente = inserirCliente("Ana Relatorio", "11144477735");
        UUID veiculo = inserirVeiculo(cliente, "REL1A99");
        OffsetDateTime base = OffsetDateTime.of(2030, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        // 3 OS: durações 1h, 2h, 3h (média = 2h = 7200s)
        inserirOSFinalizada(cliente, veiculo, base, 3600);
        inserirOSFinalizada(cliente, veiculo, base.plusDays(1), 7200);
        inserirOSFinalizada(cliente, veiculo, base.plusDays(2), 10800);

        given().when()
                .get("/relatorios/tempo-medio-execucao?desde=2030-05-30&ate=2030-06-05")
                .then()
                    .statusCode(200)
                    .body("amostra", equalTo(3))
                    .body("tempoMedioExecucaoSegundos", greaterThanOrEqualTo(7200));
    }

    // ---------- fixtures via SQL nativo (mais barato que percorrer todos os use cases) ----------

    @Transactional
    UUID inserirCliente(String nome, String documento) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO clientes (id, nome, documento, email, telefone, created_at, updated_at)
            VALUES (?1, ?2, ?3, NULL, NULL, NOW(), NOW())
            """)
                .setParameter(1, id)
                .setParameter(2, nome)
                .setParameter(3, documento)
                .executeUpdate();
        return id;
    }

    @Transactional
    UUID inserirVeiculo(UUID cliente, String placa) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO veiculos (id, placa, marca, modelo, ano, cliente_id, created_at, updated_at)
            VALUES (?1, ?2, 'X', 'Y', 2020, ?3, NOW(), NOW())
            """)
                .setParameter(1, id)
                .setParameter(2, placa)
                .setParameter(3, cliente)
                .executeUpdate();
        return id;
    }

    @Transactional
    void inserirOSFinalizada(UUID cliente, UUID veiculo, OffsetDateTime criadaEm, long duracaoSegundos) {
        UUID id = UUID.randomUUID();
        OffsetDateTime execIni = criadaEm.plusMinutes(30);
        OffsetDateTime fim = execIni.plusSeconds(duracaoSegundos);
        em.createNativeQuery("""
            INSERT INTO ordens_servico (id, cliente_id, veiculo_id, status, valor_total,
                criada_em, diagnostico_iniciado_em, execucao_iniciada_em, finalizada_em,
                entregue_em, orcamento_gerado_em, orcamento_aprovado_em)
            VALUES (?1, ?2, ?3, 'FINALIZADA', 0.00, ?4, ?5, ?5, ?6, NULL, ?5, ?5)
            """)
                .setParameter(1, id)
                .setParameter(2, cliente)
                .setParameter(3, veiculo)
                .setParameter(4, Timestamp.from(criadaEm.toInstant()))
                .setParameter(5, Timestamp.from(execIni.toInstant()))
                .setParameter(6, Timestamp.from(fim.toInstant()))
                .executeUpdate();
    }
}
