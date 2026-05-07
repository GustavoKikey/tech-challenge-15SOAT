package br.com.fiap.techchallenge.oficina.interfaces.rest;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/health")
@PermitAll
@Tag(name = "Health", description = "Verificação de saúde da aplicação")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Status da aplicação",
               description = "Retorna {\"status\":\"UP\"} quando a aplicação está no ar.")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
