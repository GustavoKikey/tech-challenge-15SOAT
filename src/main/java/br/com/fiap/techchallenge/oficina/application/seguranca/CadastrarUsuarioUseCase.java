package br.com.fiap.techchallenge.oficina.application.seguranca;

import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.PasswordHasher;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Role;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsernameJaCadastradoException;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Usuario;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Cadastra um novo usuário administrativo. A proteção de quem pode chamar
 * (apenas {@code ADMINISTRADOR}) é feita no resource via {@code @RolesAllowed}.
 */
@ApplicationScoped
public class CadastrarUsuarioUseCase {

    private final UsuarioRepository repository;
    private final PasswordHasher hasher;

    public CadastrarUsuarioUseCase(UsuarioRepository repository, PasswordHasher hasher) {
        this.repository = repository;
        this.hasher = hasher;
    }

    @Transactional
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
