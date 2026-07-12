package br.com.fiap.techchallenge.oficina.seguranca.usecases;

import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Role;
import br.com.fiap.techchallenge.oficina.seguranca.entities.UsernameJaCadastradoException;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.UsuarioGateway;

/**
 * Cadastra um novo usuário administrativo. A proteção de quem pode chamar
 * (apenas {@code ADMINISTRADOR}) é feita no resource via {@code @RolesAllowed}.
 */
public class CadastrarUsuarioUseCase {

    private final UsuarioGateway repository;
    private final PasswordHasher hasher;

    public CadastrarUsuarioUseCase(UsuarioGateway repository, PasswordHasher hasher) {
        this.repository = repository;
        this.hasher = hasher;
    }

    public Usuario executar(Input input) {
        repository.buscarPorUsername(input.username()).ifPresent(u -> {
            throw new UsernameJaCadastradoException(input.username());
        });
        Usuario usuario = Usuario.novo(
                input.username(),
                hasher.hash(input.senha()),
                input.role());
        return repository.salvar(usuario);
    }

    public record Input(String username, String senha, Role role) {}
}
