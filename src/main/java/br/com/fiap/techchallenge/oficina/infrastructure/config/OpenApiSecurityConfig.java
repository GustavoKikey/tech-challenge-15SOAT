package br.com.fiap.techchallenge.oficina.infrastructure.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Configura o esquema "BearerAuth" no OpenAPI/Swagger UI — habilita o botão
 * <i>Authorize</i> no Swagger para colar o JWT obtido via {@code POST /auth/login}.
 *
 * <p>O {@code SecurityRequirement} aqui é apenas o <i>default</i> mostrado pelo
 * Swagger; a proteção real é feita por {@code @RolesAllowed} / {@code @PermitAll}
 * em cada resource.
 */
@SecurityScheme(
        securitySchemeName = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT emitido pelo POST /auth/login (cole apenas o token, sem prefixo)."
)
@SecurityRequirement(name = "BearerAuth")
public class OpenApiSecurityConfig extends Application {
}
