package br.com.fiap.techchallenge.oficina.relatorio.gateways;

import br.com.fiap.techchallenge.oficina.relatorio.entities.TempoMedioExecucao;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Read repository do indicador "tempo médio de execução". Implementação em
 * {@code infrastructure/persistence/relatorio} usa SQL nativo agregado — não
 * passa pelo agregado OrdemServico (read model puro).
 */
public interface RelatorioTempoMedioGateway {

    /**
     * @param desde filtro inclusivo pelo {@code criada_em} (opcional)
     * @param ate   filtro inclusivo pelo {@code criada_em} (opcional)
     */
    TempoMedioExecucao calcular(Optional<LocalDate> desde, Optional<LocalDate> ate);
}
