package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.atendimento.controllers.ServicoController;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.AtualizarServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.CadastrarServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.ServicoResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/** Driver HTTP do agregado Serviço — delega ao {@link ServicoController}. */
@Path("/servicos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@Tag(name = "Serviços", description = "Catálogo de serviços de mão-de-obra")
public class ServicoResource {

    private final ServicoController controller;

    public ServicoResource(ServicoController controller) {
        this.controller = controller;
    }

    @POST
    @Operation(summary = "Cadastra um novo serviço")
    @APIResponse(responseCode = "201", description = "Serviço criado")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    public Response cadastrar(@Valid CadastrarServicoRequest req) {
        ServicoResponse resp = controller.cadastrar(req);
        return Response.created(UriBuilder.fromResource(ServicoResource.class)
                        .path("{id}").build(resp.id()))
                .entity(resp)
                .build();
    }

    @GET
    @Operation(summary = "Lista todos os serviços")
    public List<ServicoResponse> listar() {
        return controller.listar();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca um serviço pelo ID")
    @APIResponse(responseCode = "404", description = "Serviço não encontrado")
    public ServicoResponse buscar(@PathParam("id") UUID id) {
        return controller.buscar(id);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza um serviço")
    @APIResponse(responseCode = "404", description = "Serviço não encontrado")
    public ServicoResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarServicoRequest req) {
        return controller.atualizar(id, req);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove um serviço")
    @APIResponse(responseCode = "204", description = "Removido")
    @APIResponse(responseCode = "404", description = "Serviço não encontrado")
    public Response remover(@PathParam("id") UUID id) {
        controller.remover(id);
        return Response.noContent().build();
    }
}
