package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

/**
 * Porta de domínio para hashing/verificação de senhas. A implementação concreta
 * (BCrypt) vive em {@code infrastructure/security} — assim o agregado
 * {@link Usuario} permanece livre de framework e de dependências de criptografia.
 */
public interface PasswordHasher {

    /** Gera o hash da senha em texto puro. Hash deve ser único por chamada (salt aleatório). */
    String hash(String plainText);

    /** Compara senha em texto puro com hash armazenado, em tempo constante. */
    boolean matches(String plainText, String hashed);
}
