package br.com.fiap.techchallenge.oficina.atendimento.controllers;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.AtualizarVeiculoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.CadastrarVeiculoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.VeiculoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.presenters.VeiculoPresenter;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.AtualizarVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.BuscarVeiculoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.CadastrarVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.ListarVeiculosUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RemoverVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;

import java.util.List;
import java.util.UUID;

/**
 * Controller do agregado Veículo. Cadastro e atualização cruzam o agregado Cliente
 * (validação de dono), por isso compõem use cases com os dois gateways.
 */
public class VeiculoController {

    private final CadastrarVeiculoUseCase cadastrar;
    private final AtualizarVeiculoUseCase atualizar;
    private final RemoverVeiculoUseCase remover;
    private final BuscarVeiculoPorIdUseCase buscar;
    private final ListarVeiculosUseCase listar;
    private final ExecutorTransacional tx;
    private final VeiculoPresenter presenter = new VeiculoPresenter();

    public VeiculoController(VeiculoGateway veiculoGateway, ClienteGateway clienteGateway,
                             ExecutorTransacional tx) {
        this.cadastrar = new CadastrarVeiculoUseCase(veiculoGateway, clienteGateway);
        this.atualizar = new AtualizarVeiculoUseCase(veiculoGateway, clienteGateway);
        this.remover = new RemoverVeiculoUseCase(veiculoGateway);
        this.buscar = new BuscarVeiculoPorIdUseCase(veiculoGateway);
        this.listar = new ListarVeiculosUseCase(veiculoGateway);
        this.tx = tx;
    }

    public VeiculoResponse cadastrar(CadastrarVeiculoRequest req) {
        Veiculo v = tx.emTransacao(() -> cadastrar.executar(new CadastrarVeiculoUseCase.Input(
                req.placa(), req.marca(), req.modelo(), req.ano(), ClienteId.de(req.clienteId()))));
        return presenter.apresentar(v);
    }

    public List<VeiculoResponse> listar() {
        return presenter.apresentar(tx.emTransacao(listar::executar));
    }

    public VeiculoResponse buscar(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> buscar.executar(VeiculoId.de(id))));
    }

    public List<VeiculoResponse> porCliente(UUID clienteId) {
        return presenter.apresentar(tx.emTransacao(() -> listar.porCliente(ClienteId.de(clienteId))));
    }

    public VeiculoResponse atualizar(UUID id, AtualizarVeiculoRequest req) {
        ClienteId novoDono = req.clienteId() == null ? null : ClienteId.de(req.clienteId());
        Veiculo v = tx.emTransacao(() -> atualizar.executar(new AtualizarVeiculoUseCase.Input(
                VeiculoId.de(id), req.marca(), req.modelo(), req.ano(), novoDono)));
        return presenter.apresentar(v);
    }

    public void remover(UUID id) {
        tx.emTransacao(() -> {
            remover.executar(VeiculoId.de(id));
            return null;
        });
    }
}
