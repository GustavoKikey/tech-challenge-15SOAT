package br.com.fiap.techchallenge.oficina.estoque.controllers;

import br.com.fiap.techchallenge.oficina.estoque.dtos.AdicionarSaldoRequest;
import br.com.fiap.techchallenge.oficina.estoque.dtos.AtualizarPecaRequest;
import br.com.fiap.techchallenge.oficina.estoque.dtos.CadastrarPecaRequest;
import br.com.fiap.techchallenge.oficina.estoque.dtos.PecaResponse;
import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.estoque.presenters.EstoquePresenter;
import br.com.fiap.techchallenge.oficina.estoque.usecases.AdicionarSaldoUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.AtualizarPecaUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.BuscarPecaPorIdUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.CadastrarPecaUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.ListarPecasUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.RemoverPecaUseCase;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;

import java.util.List;
import java.util.UUID;

/**
 * Controller do BC Estoque. Compõe os use cases a partir do {@link PecaGateway},
 * demarca a transação pela porta {@link ExecutorTransacional} e formata a saída
 * pelo {@link EstoquePresenter}. Livre de qualquer dependência web.
 */
public class PecaController {

    private final CadastrarPecaUseCase cadastrar;
    private final AtualizarPecaUseCase atualizar;
    private final RemoverPecaUseCase remover;
    private final BuscarPecaPorIdUseCase buscar;
    private final ListarPecasUseCase listar;
    private final AdicionarSaldoUseCase adicionarSaldo;
    private final ExecutorTransacional tx;
    private final EstoquePresenter presenter = new EstoquePresenter();

    public PecaController(PecaGateway gateway, ExecutorTransacional tx) {
        this.cadastrar = new CadastrarPecaUseCase(gateway);
        this.atualizar = new AtualizarPecaUseCase(gateway);
        this.remover = new RemoverPecaUseCase(gateway);
        this.buscar = new BuscarPecaPorIdUseCase(gateway);
        this.listar = new ListarPecasUseCase(gateway);
        this.adicionarSaldo = new AdicionarSaldoUseCase(gateway);
        this.tx = tx;
    }

    public PecaResponse cadastrar(CadastrarPecaRequest req) {
        Peca p = tx.emTransacao(() -> cadastrar.executar(new CadastrarPecaUseCase.Input(
                req.descricao(), req.valorUnitario(), req.quantidadeInicialOuZero())));
        return presenter.apresentar(p);
    }

    public List<PecaResponse> listar() {
        return presenter.apresentar(tx.emTransacao(listar::executar));
    }

    public PecaResponse buscar(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> buscar.executar(PecaId.de(id))));
    }

    public PecaResponse atualizar(UUID id, AtualizarPecaRequest req) {
        Peca p = tx.emTransacao(() -> atualizar.executar(new AtualizarPecaUseCase.Input(
                PecaId.de(id), req.descricao(), req.valorUnitario())));
        return presenter.apresentar(p);
    }

    public void remover(UUID id) {
        tx.emTransacao(() -> {
            remover.executar(PecaId.de(id));
            return null;
        });
    }

    public PecaResponse adicionarSaldo(UUID id, AdicionarSaldoRequest req) {
        Peca p = tx.emTransacao(() -> adicionarSaldo.executar(new AdicionarSaldoUseCase.Input(
                PecaId.de(id), req.quantidade())));
        return presenter.apresentar(p);
    }
}
