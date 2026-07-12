package br.com.fiap.techchallenge.oficina.external.persistence.relatorio;

import br.com.fiap.techchallenge.oficina.relatorio.gateways.RelatorioTempoMedioGateway;
import br.com.fiap.techchallenge.oficina.relatorio.entities.TempoMedioExecucao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Read repository do indicador "tempo médio de execução".
 *
 * <p>Cálculo feito direto no banco com SQL agregado: o tempo de execução de uma OS
 * é {@code finalizada_em - execucao_iniciada_em}, calculado em segundos via
 * {@code EXTRACT(EPOCH FROM ...)}. A média é tirada apenas sobre as OS que têm
 * ambas as datas preenchidas — OS que ainda não saíram da execução não contam.
 *
 * <p>Os filtros opcionais {@code desde} / {@code ate} aplicam-se à
 * {@code criada_em}, conforme spec.
 */
@ApplicationScoped
public class RelatorioTempoMedioGatewayImpl implements RelatorioTempoMedioGateway {

    private static final String SQL = """
        SELECT
            COUNT(*) AS amostra,
            COALESCE(AVG(EXTRACT(EPOCH FROM (finalizada_em - execucao_iniciada_em))), 0)
                AS media_segundos
        FROM ordens_servico
        WHERE finalizada_em IS NOT NULL
          AND execucao_iniciada_em IS NOT NULL
          AND (CAST(?1 AS timestamptz) IS NULL OR criada_em >= ?1)
          AND (CAST(?2 AS timestamptz) IS NULL OR criada_em <= ?2)
        """;

    private final EntityManager em;

    public RelatorioTempoMedioGatewayImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public TempoMedioExecucao calcular(Optional<LocalDate> desde, Optional<LocalDate> ate) {
        Object[] row = (Object[]) em.createNativeQuery(SQL)
                .setParameter(1, desde.map(d -> Timestamp.from(
                        d.atStartOfDay().toInstant(ZoneOffset.UTC))).orElse(null))
                .setParameter(2, ate.map(d -> Timestamp.from(
                        d.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))).orElse(null))
                .getSingleResult();

        long amostra = ((Number) row[0]).longValue();
        long mediaSegundos = row[1] == null ? 0L : ((BigDecimal) row[1]).longValue();
        return new TempoMedioExecucao(amostra, mediaSegundos);
    }

    /** Util para testes — não usado em runtime. */
    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
