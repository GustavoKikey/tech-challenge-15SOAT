package br.com.fiap.techchallenge.oficina.external.config;

import br.com.fiap.techchallenge.oficina.estoque.controllers.PecaController;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/** Composition root do BC Estoque. */
@ApplicationScoped
public class EstoqueBeans {

    @Produces
    @ApplicationScoped
    PecaController pecaController(PecaGateway gateway, ExecutorTransacional tx) {
        return new PecaController(gateway, tx);
    }
}
