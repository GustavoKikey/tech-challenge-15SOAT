package br.com.fiap.techchallenge.oficina.relatorio.usecases;

import br.com.fiap.techchallenge.oficina.relatorio.gateways.RelatorioTempoMedioGateway;
import br.com.fiap.techchallenge.oficina.relatorio.entities.TempoMedioExecucao;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Caso de uso "Monitorar Tempo Médio de Execução" — nome retirado do diagrama do DDD.
 * Apenas delega ao read repository; nenhum agregado é carregado em memória.
 */
public class MonitorarTempoMedioUseCase {

    private final RelatorioTempoMedioGateway repository;

    public MonitorarTempoMedioUseCase(RelatorioTempoMedioGateway repository) {
        this.repository = repository;
    }

    public TempoMedioExecucao executar(LocalDate desde, LocalDate ate) {
        return repository.calcular(Optional.ofNullable(desde), Optional.ofNullable(ate));
    }
}
