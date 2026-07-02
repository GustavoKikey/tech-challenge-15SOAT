package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.atendimento.controllers.OrdemServicoController;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.DecisaoOrcamentoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoPublicaResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.StatusOSResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/**
 * Endpoints públicos de acompanhamento da OS. Reaproveitam o
 * {@link OrdemServicoController}: consulta resumida, consulta de status e o
 * canal de <b>notificação externa</b> da decisão do cliente sobre o orçamento
 * (aprovação/recusa vinda de link de e-mail, portal ou sistema parceiro).
 */
@Path("/publico/ordens-servico")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "Público", description = "Consulta pública de OS e decisão de orçamento do cliente")
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

    @GET
    @Path("/{id}/status")
    @Operation(summary = "Consulta de status da OS: situação atual com descrição amigável")
    @APIResponse(responseCode = "404", description = "OS não encontrada")
    public StatusOSResponse consultarStatus(@PathParam("id") UUID id) {
        return controller.consultarStatus(id);
    }

    @POST
    @Path("/{id}/orcamento/decisao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Recebe notificação externa de aprovação ou recusa do orçamento pelo cliente")
    @APIResponse(responseCode = "200", description = "Decisão registrada")
    @APIResponse(responseCode = "404", description = "OS não encontrada")
    @APIResponse(responseCode = "409", description = "OS não está aguardando aprovação")
    public StatusOSResponse decidirOrcamento(@PathParam("id") UUID id,
                                             @Valid DecisaoOrcamentoRequest req) {
        controller.decidirOrcamento(id, req.aprovado());
        return controller.consultarStatus(id);
    }
}
