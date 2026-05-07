package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

import java.util.Objects;

/**
 * Agregado raiz <b>Usuário Administrativo</b>.
 *
 * <p>Representa um operador interno do sistema (Atendente, Mecânico ou Administrador)
 * — a única identidade autenticável. O cliente final não tem usuário; consulta a OS
 * pelo endpoint público.
 *
 * <p>A senha é guardada apenas como hash (BCrypt). O agregado nunca aceita nem expõe
 * a senha em texto puro: o {@link #verificarSenha} delega o trabalho ao
 * {@link PasswordHasher} (porta de domínio, implementada em {@code infrastructure}).
 */
public class Usuario {

    private final UsuarioId id;
    private final String username;
    private String senhaHash;
    private Role role;
    private boolean ativo;

    private Usuario(UsuarioId id, String username, String senhaHash, Role role, boolean ativo) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = exigirUsername(username);
        this.senhaHash = exigirSenhaHash(senhaHash);
        this.role = Objects.requireNonNull(role, "role");
        this.ativo = ativo;
    }

    public static Usuario novo(String username, String senhaHash, Role role) {
        return new Usuario(UsuarioId.novo(), username, senhaHash, role, true);
    }

    public static Usuario reconstituir(UsuarioId id, String username, String senhaHash,
                                       Role role, boolean ativo) {
        return new Usuario(id, username, senhaHash, role, ativo);
    }

    /**
     * Compara a senha em texto puro com o hash persistido. Usuário inativo nunca
     * autentica, mesmo com senha correta.
     */
    public boolean verificarSenha(String plainText, PasswordHasher hasher) {
        Objects.requireNonNull(hasher, "hasher");
        if (!ativo || plainText == null || plainText.isEmpty()) {
            return false;
        }
        return hasher.matches(plainText, senhaHash);
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = exigirSenhaHash(novoHash);
    }

    public void alterarRole(Role novoRole) {
        this.role = Objects.requireNonNull(novoRole, "role");
    }

    public void desativar() { this.ativo = false; }
    public void ativar()    { this.ativo = true;  }

    private static String exigirUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username é obrigatório");
        }
        return username.trim();
    }

    private static String exigirSenhaHash(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("Senha (hash) é obrigatória");
        }
        return senhaHash;
    }

    public UsuarioId id()      { return id; }
    public String username()   { return username; }
    public String senhaHash()  { return senhaHash; }
    public Role role()         { return role; }
    public boolean ativo()     { return ativo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
