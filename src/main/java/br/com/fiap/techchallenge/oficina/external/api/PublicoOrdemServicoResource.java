package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.atendimento.controllers.OrdemServicoController;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoPublicaResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/**
 * Endpoint público de acompanhamento da OS. Reaproveita o {@link OrdemServicoController}
 * (método {@code consultarPublico}), que devolve apenas dados resumidos (status,
 * valor total, datas-chave) — sem itens, reservas ou ids de cliente/veículo.
 */
@Path("/publico/ordens-servico")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "Público", description = "Consulta pública e resumida de Ordens de Serviço")
public class PublicoOrdemServicoResource {

    private final OrdemServicoController controller;

    public PublicoOrdemServicoResource(OrdemServicoController controller) {
        this.controller = controller;
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta resumida de uma OS para acompanhamento do cliente")
    @APIResponse(responseCode = "404", description = "OS não encontrada")
    public OrdemServicoPublicaResponse buscar(@PathParam("id") UUID id) {
        return controller.consultarPublico(id);
    }
}
