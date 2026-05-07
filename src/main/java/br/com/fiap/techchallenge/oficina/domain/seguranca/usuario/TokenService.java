package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

/**
 * Porta de domínio para emissão de tokens de autenticação. A implementação concreta
 * (JWT assinado em RSA) vive em {@code infrastructure/security}.
 */
public interface TokenService {

    /** Emite um token para o usuário, com {@code sub=username} e {@code groups=[role]}. */
    Token gerar(Usuario usuario);

    /** Token emitido — string + duração de validade em segundos. */
    record Token(String accessToken, long expiresInSeconds) {}
}
