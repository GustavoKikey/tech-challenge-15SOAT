package br.com.fiap.techchallenge.oficina.application.relatorio;

import br.com.fiap.techchallenge.oficina.domain.relatorio.RelatorioTempoMedioRepository;
import br.com.fiap.techchallenge.oficina.domain.relatorio.TempoMedioExecucao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorarTempoMedioUseCaseTest {

    @Mock RelatorioTempoMedioRepository repository;
    @Captor ArgumentCaptor<Optional<LocalDate>> from;
    @Captor ArgumentCaptor<Optional<LocalDate>> to;

    @Test
    void semOSRetornaVazio() {
        when(repository.calcular(Optional.empty(), Optional.empty()))
                .thenReturn(TempoMedioExecucao.vazio());

        TempoMedioExecucao out = new MonitorarTempoMedioUseCase(repository).executar(null, null);

        assertEquals(0L, out.amostra());
        assertEquals(0L, out.tempoMedioExecucaoSegundos());
    }

    @Test
    void delegaParametrosOpcionaisDeData() {
        when(repository.calcular(any(), any())).thenReturn(new TempoMedioExecucao(5L, 3600L));
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate ate   = LocalDate.of(2026, 12, 31);

        TempoMedioExecucao out = new MonitorarTempoMedioUseCase(repository).executar(desde, ate);
        assertEquals(5L, out.amostra());
        assertEquals(3600L, out.tempoMedioExecucaoSegundos());

        verify(repository).calcular(from.capture(), to.capture());
        assertEquals(Optional.of(desde), from.getValue());
        assertEquals(Optional.of(ate), to.getValue());
    }
}
