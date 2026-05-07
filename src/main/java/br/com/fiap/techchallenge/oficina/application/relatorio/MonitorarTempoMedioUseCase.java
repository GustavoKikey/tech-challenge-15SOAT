package br.com.fiap.techchallenge.oficina.application.relatorio;

import br.com.fiap.techchallenge.oficina.domain.relatorio.RelatorioTempoMedioRepository;
import br.com.fiap.techchallenge.oficina.domain.relatorio.TempoMedioExecucao;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Caso de uso "Monitorar Tempo Médio de Execução" — nome retirado do diagrama do DDD.
 * Apenas delega ao read repository; nenhum agregado é carregado em memória.
 */
@ApplicationScoped
public class MonitorarTempoMedioUseCase {

    private final RelatorioTempoMedioRepository repository;

    public MonitorarTempoMedioUseCase(RelatorioTempoMedioRepository repository) {
        this.repository = repository;
    }

    public TempoMedioExecucao executar(LocalDate desde, LocalDate ate) {
        return repository.calcular(Optional.ofNullable(desde), Optional.ofNullable(ate));
    }
}
