package br.com.fiap.techchallenge.oficina.external.security;

import br.com.fiap.techchallenge.oficina.seguranca.gateways.TokenService;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;

/**
 * Emissor de JWT assinado com a chave RSA configurada em
 * {@code smallrye.jwt.sign.key.location}. O token transporta:
 *
 * <ul>
 *   <li>{@code iss} — issuer fixo ({@code mp.jwt.verify.issuer})</li>
 *   <li>{@code sub} — username</li>
 *   <li>{@code upn} — username (alias usado pelo SmallRye para principal)</li>
 *   <li>{@code groups} — uma role (ex.: {@code [ADMINISTRADOR]}) — formato exigido pelo MP-JWT</li>
 *   <li>{@code uid} — claim privado com o id do usuário (opcional, para auditoria)</li>
 *   <li>{@code exp} — agora + duração configurada (default 8h)</li>
 * </ul>
 */
@ApplicationScoped
public class JwtTokenService implements TokenService {

    private final String issuer;
    private final long expirationSeconds;

    public JwtTokenService(
            @ConfigProperty(name = "mp.jwt.verify.issuer") String issuer,
            @ConfigProperty(name = "oficina.jwt.expiration", defaultValue = "PT8H") String expiration) {
        this.issuer = issuer;
        this.expirationSeconds = Duration.parse(expiration).toSeconds();
    }

    @Override
    public Token gerar(Usuario usuario) {
        String jwt = Jwt.issuer(issuer)
                .subject(usuario.username())
                .upn(usuario.username())
                .groups(Set.of(usuario.role().name()))
                .claim("uid", usuario.id().valor().toString())
                .expiresIn(Duration.ofSeconds(expirationSeconds))
                .sign();
        return new Token(jwt, expirationSeconds);
    }
}
