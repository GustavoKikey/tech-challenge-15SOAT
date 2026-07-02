package br.com.fiap.techchallenge.oficina.atendimento.controllers;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.AtualizarServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.CadastrarServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.ServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.presenters.ServicoPresenter;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.AtualizarServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.BuscarServicoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.CadastrarServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.ListarServicosUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RemoverServicoUseCase;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;

import java.util.List;
import java.util.UUID;

/** Controller do agregado Serviço (catálogo de mão-de-obra). */
public class ServicoController {

    private final CadastrarServicoUseCase cadastrar;
    private final AtualizarServicoUseCase atualizar;
    private final RemoverServicoUseCase remover;
    private final BuscarServicoPorIdUseCase buscar;
    private final ListarServicosUseCase listar;
    private final ExecutorTransacional tx;
    private final ServicoPresenter presenter = new ServicoPresenter();

    public ServicoController(ServicoGateway gateway, ExecutorTransacional tx) {
        this.cadastrar = new CadastrarServicoUseCase(gateway);
        this.atualizar = new AtualizarServicoUseCase(gateway);
        this.remover = new RemoverServicoUseCase(gateway);
        this.buscar = new BuscarServicoPorIdUseCase(gateway);
        this.listar = new ListarServicosUseCase(gateway);
        this.tx = tx;
    }

    public ServicoResponse cadastrar(CadastrarServicoRequest req) {
        Servico s = tx.emTransacao(() -> cadastrar.executar(
                new CadastrarServicoUseCase.Input(req.descricao(), req.valorBase())));
        return presenter.apresentar(s);
    }

    public List<ServicoResponse> listar() {
        return presenter.apresentar(tx.emTransacao(listar::executar));
    }

    public ServicoResponse buscar(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> buscar.executar(ServicoId.de(id))));
    }

    public ServicoResponse atualizar(UUID id, AtualizarServicoRequest req) {
        Servico s = tx.emTransacao(() -> atualizar.executar(new AtualizarServicoUseCase.Input(
                ServicoId.de(id), req.descricao(), req.valorBase())));
        return presenter.apresentar(s);
    }

    public void remover(UUID id) {
        tx.emTransacao(() -> {
            remover.executar(ServicoId.de(id));
            return null;
        });
    }
}
