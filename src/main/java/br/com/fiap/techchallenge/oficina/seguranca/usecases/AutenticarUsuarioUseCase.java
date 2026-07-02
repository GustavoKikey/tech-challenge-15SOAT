package br.com.fiap.techchallenge.oficina.seguranca.usecases;

import br.com.fiap.techchallenge.oficina.seguranca.entities.CredenciaisInvalidasException;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.TokenService;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.UsuarioGateway;

/**
 * Caso de uso de login. Sempre lança {@link CredenciaisInvalidasException} ao
 * falhar — independente de o motivo ser usuário inexistente, senha errada ou
 * usuário inativo —, para não vazar qual dos casos ocorreu.
 */
public class AutenticarUsuarioUseCase {

    private final UsuarioGateway repository;
    private final PasswordHasher hasher;
    private final TokenService tokenService;

    public AutenticarUsuarioUseCase(UsuarioGateway repository,
                                    PasswordHasher hasher,
                                    TokenService tokenService) {
        this.repository = repository;
        this.hasher = hasher;
        this.tokenService = tokenService;
    }

    public Output executar(Input input) {
        Usuario usuario = repository.buscarPorUsername(input.username())
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!usuario.verificarSenha(input.senha(), hasher)) {
            throw new CredenciaisInvalidasException();
        }
        TokenService.Token token = tokenService.gerar(usuario);
        return new Output(token.accessToken(), token.expiresInSeconds(), usuario.role().name());
    }

    public record Input(String username, String senha) {}
    public record Output(String accessToken, long expiresIn, String role) {}
}
