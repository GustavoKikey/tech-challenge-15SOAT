package br.com.fiap.techchallenge.oficina.external.security;

import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Implementação de {@link PasswordHasher} usando BCrypt (cost padrão do Quarkus
 * Elytron — 10). Hash gerado é compatível com o formato {@code $2a$10$...} e
 * já carrega o salt embutido, então o repositório só precisa persistir a string.
 */
@ApplicationScoped
public class BCryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String plainText) {
        return BcryptUtil.bcryptHash(plainText);
    }

    @Override
    public boolean matches(String plainText, String hashed) {
        if (plainText == null || hashed == null) return false;
        return BcryptUtil.matches(plainText, hashed);
    }
}
