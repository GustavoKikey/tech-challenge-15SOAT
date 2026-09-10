package br.com.fiap.techchallenge.oficina.external.config;

import br.com.fiap.techchallenge.oficina.atendimento.controllers.ClienteController;
import br.com.fiap.techchallenge.oficina.atendimento.controllers.OrdemServicoController;
import br.com.fiap.techchallenge.oficina.atendimento.controllers.ServicoController;
import br.com.fiap.techchallenge.oficina.atendimento.controllers.VeiculoController;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.MetricasGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.NotificacaoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Composition root do BC Atendimento. Monta os controllers (POJOs do núcleo) a
 * partir dos gateways da infra — único ponto que conhece CDI e o grafo de objetos.
 */
@ApplicationScoped
public class AtendimentoBeans {

    @Produces
    @ApplicationScoped
    ClienteController clienteController(ClienteGateway clienteGateway, ExecutorTransacional tx) {
        return new ClienteController(clienteGateway, tx);
    }

    @Produces
    @ApplicationScoped
    VeiculoController veiculoController(VeiculoGateway veiculoGateway,
                                        ClienteGateway clienteGateway,
                                        ExecutorTransacional tx) {
        return new VeiculoController(veiculoGateway, clienteGateway, tx);
    }

    @Produces
    @ApplicationScoped
    ServicoController servicoController(ServicoGateway servicoGateway, ExecutorTransacional tx) {
        return new ServicoController(servicoGateway, tx);
    }

    @Produces
    @ApplicationScoped
    OrdemServicoController ordemServicoController(OrdemServicoGateway osGateway,
                                                  ClienteGateway clienteGateway,
                                                  VeiculoGateway veiculoGateway,
                                                  ServicoGateway servicoGateway,
                                                  PecaGateway pecaGateway,
                                                  NotificacaoGateway notificacaoGateway,
                                                  ExecutorTransacional tx,
                                                  MetricasGateway metricas) {
        return new OrdemServicoController(osGateway, clienteGateway, veiculoGateway,
                servicoGateway, pecaGateway, notificacaoGateway, tx, metricas);
    }
}
