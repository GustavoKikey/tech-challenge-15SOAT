package br.com.fiap.techchallenge.oficina.relatorio.controllers;

import br.com.fiap.techchallenge.oficina.relatorio.dtos.TempoMedioResponse;
import br.com.fiap.techchallenge.oficina.relatorio.gateways.RelatorioTempoMedioGateway;
import br.com.fiap.techchallenge.oficina.relatorio.presenters.RelatorioPresenter;
import br.com.fiap.techchallenge.oficina.relatorio.usecases.MonitorarTempoMedioUseCase;

import java.time.LocalDate;

/**
 * Controller do BC Relatório. O caso de uso é um read model puro (SQL agregado),
 * portanto não demarca transação de escrita.
 */
public class RelatorioController {

    private final MonitorarTempoMedioUseCase monitorar;
    private final RelatorioPresenter presenter = new RelatorioPresenter();

    public RelatorioController(RelatorioTempoMedioGateway gateway) {
        this.monitorar = new MonitorarTempoMedioUseCase(gateway);
    }

    public TempoMedioResponse tempoMedioExecucao(LocalDate desde, LocalDate ate) {
        return presenter.apresentar(monitorar.executar(desde, ate));
    }
}
