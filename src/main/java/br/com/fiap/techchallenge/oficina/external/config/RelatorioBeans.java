package br.com.fiap.techchallenge.oficina.external.config;

import br.com.fiap.techchallenge.oficina.relatorio.controllers.RelatorioController;
import br.com.fiap.techchallenge.oficina.relatorio.gateways.RelatorioTempoMedioGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/** Composition root do BC Relatório. */
@ApplicationScoped
public class RelatorioBeans {

    @Produces
    @ApplicationScoped
    RelatorioController relatorioController(RelatorioTempoMedioGateway gateway) {
        return new RelatorioController(gateway);
    }
}
