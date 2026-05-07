package br.com.fiap.techchallenge.oficina.application.seguranca;

import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.CredenciaisInvalidasException;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.PasswordHasher;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.TokenService;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Usuario;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Caso de uso de login. Sempre lança {@link CredenciaisInvalidasException} ao
 * falhar — independente de o motivo ser usuário inexistente, senha errada ou
 * usuário inativo —, para não vazar qual dos casos ocorreu.
 */
@ApplicationScoped
public class AutenticarUsuarioUseCase {

    private final UsuarioRepository repository;
    private final PasswordHasher hasher;
    private final TokenService tokenService;

    public AutenticarUsuarioUseCase(UsuarioRepository repository,
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
