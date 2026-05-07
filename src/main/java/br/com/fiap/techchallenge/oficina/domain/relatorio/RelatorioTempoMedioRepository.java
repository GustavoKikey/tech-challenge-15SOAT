package br.com.fiap.techchallenge.oficina.domain.relatorio;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Read repository do indicador "tempo médio de execução". Implementação em
 * {@code infrastructure/persistence/relatorio} usa SQL nativo agregado — não
 * passa pelo agregado OrdemServico (read model puro).
 */
public interface RelatorioTempoMedioRepository {

    /**
     * @param desde filtro inclusivo pelo {@code criada_em} (opcional)
     * @param ate   filtro inclusivo pelo {@code criada_em} (opcional)
     */
    TempoMedioExecucao calcular(Optional<LocalDate> desde, Optional<LocalDate> ate);
}
