package br.com.fiap.techchallenge.oficina.atendimento.controllers;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.AtualizarClienteRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.CadastrarClienteRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.ClienteResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.presenters.ClientePresenter;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.AtualizarClienteUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.BuscarClientePorIdUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.CadastrarClienteUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.ListarClientesUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RemoverClienteUseCase;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;

import java.util.List;
import java.util.UUID;

/** Controller do agregado Cliente — orquestra os use cases sob a porta de transação. */
public class ClienteController {

    private final CadastrarClienteUseCase cadastrar;
    private final AtualizarClienteUseCase atualizar;
    private final RemoverClienteUseCase remover;
    private final BuscarClientePorIdUseCase buscar;
    private final ListarClientesUseCase listar;
    private final ExecutorTransacional tx;
    private final ClientePresenter presenter = new ClientePresenter();

    public ClienteController(ClienteGateway gateway, ExecutorTransacional tx) {
        this.cadastrar = new CadastrarClienteUseCase(gateway);
        this.atualizar = new AtualizarClienteUseCase(gateway);
        this.remover = new RemoverClienteUseCase(gateway);
        this.buscar = new BuscarClientePorIdUseCase(gateway);
        this.listar = new ListarClientesUseCase(gateway);
        this.tx = tx;
    }

    public ClienteResponse cadastrar(CadastrarClienteRequest req) {
        Cliente c = tx.emTransacao(() -> cadastrar.executar(new CadastrarClienteUseCase.Input(
                req.nome(), req.documento(), req.email(), req.telefone())));
        return presenter.apresentar(c);
    }

    public List<ClienteResponse> listar() {
        return presenter.apresentar(tx.emTransacao(listar::executar));
    }

    public ClienteResponse buscar(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> buscar.executar(ClienteId.de(id))));
    }

    public ClienteResponse atualizar(UUID id, AtualizarClienteRequest req) {
        Cliente c = tx.emTransacao(() -> atualizar.executar(new AtualizarClienteUseCase.Input(
                ClienteId.de(id), req.nome(), req.email(), req.telefone())));
        return presenter.apresentar(c);
    }

    public void remover(UUID id) {
        tx.emTransacao(() -> {
            remover.executar(ClienteId.de(id));
            return null;
        });
    }
}
