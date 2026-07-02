package br.com.fiap.techchallenge.oficina.relatorio.presenters;

import br.com.fiap.techchallenge.oficina.relatorio.dtos.TempoMedioResponse;
import br.com.fiap.techchallenge.oficina.relatorio.entities.TempoMedioExecucao;

/** Presenter do BC Relatório: read model {@link TempoMedioExecucao} → DTO. */
public class RelatorioPresenter {

    public TempoMedioResponse apresentar(TempoMedioExecucao t) {
        return new TempoMedioResponse(t.amostra(), t.tempoMedioExecucaoSegundos());
    }
}
