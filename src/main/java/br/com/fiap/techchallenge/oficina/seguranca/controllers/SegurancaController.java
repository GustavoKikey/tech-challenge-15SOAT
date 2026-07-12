package br.com.fiap.techchallenge.oficina.seguranca.controllers;

import br.com.fiap.techchallenge.oficina.seguranca.dtos.CadastrarUsuarioRequest;
import br.com.fiap.techchallenge.oficina.seguranca.dtos.LoginRequest;
import br.com.fiap.techchallenge.oficina.seguranca.dtos.LoginResponse;
import br.com.fiap.techchallenge.oficina.seguranca.dtos.UsuarioResponse;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.TokenService;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.UsuarioGateway;
import br.com.fiap.techchallenge.oficina.seguranca.presenters.SegurancaPresenter;
import br.com.fiap.techchallenge.oficina.seguranca.usecases.AutenticarUsuarioUseCase;
import br.com.fiap.techchallenge.oficina.seguranca.usecases.CadastrarUsuarioUseCase;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;

/**
 * Controller (Clean Architecture) do BC Segurança — orquestrador <b>livre de
 * framework web</b>. Recebe DTOs de request, monta os {@code Input} dos use cases,
 * demarca a transação pela porta {@link ExecutorTransacional} e devolve DTOs via
 * {@link SegurancaPresenter}. Quem fala HTTP é o {@code AuthResource} (external/api).
 */
public class SegurancaController {

    private final AutenticarUsuarioUseCase autenticar;
    private final CadastrarUsuarioUseCase cadastrar;
    private final ExecutorTransacional tx;
    private final SegurancaPresenter presenter = new SegurancaPresenter();

    public SegurancaController(UsuarioGateway usuarioGateway, PasswordHasher hasher,
                               TokenService tokenService, ExecutorTransacional tx) {
        this.autenticar = new AutenticarUsuarioUseCase(usuarioGateway, hasher, tokenService);
        this.cadastrar = new CadastrarUsuarioUseCase(usuarioGateway, hasher);
        this.tx = tx;
    }

    /** Login é leitura pura — não demarca transação (mantém o comportamento original). */
    public LoginResponse login(LoginRequest req) {
        AutenticarUsuarioUseCase.Output out = autenticar.executar(
                new AutenticarUsuarioUseCase.Input(req.username(), req.password()));
        return presenter.apresentar(out);
    }

    public UsuarioResponse cadastrar(CadastrarUsuarioRequest req) {
        Usuario usuario = tx.emTransacao(() -> cadastrar.executar(
                new CadastrarUsuarioUseCase.Input(req.username(), req.password(), req.role())));
        return presenter.apresentar(usuario);
    }
}
