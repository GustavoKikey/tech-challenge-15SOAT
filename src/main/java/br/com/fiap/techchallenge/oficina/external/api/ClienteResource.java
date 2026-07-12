package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.atendimento.controllers.ClienteController;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.AtualizarClienteRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.CadastrarClienteRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.ClienteResponse;
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

/** Driver HTTP do agregado Cliente — delega ao {@link ClienteController}. */
@Path("/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Clientes", description = "Cadastro de clientes (PF ou PJ)")
public class ClienteResource {

    private final ClienteController controller;

    public ClienteResource(ClienteController controller) {
        this.controller = controller;
    }

    @POST
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Cadastra um novo cliente")
    @APIResponse(responseCode = "201", description = "Cliente criado")
    @APIResponse(responseCode = "400", description = "Documento ou payload inválido")
    @APIResponse(responseCode = "409", description = "Documento já cadastrado")
    public Response cadastrar(@Valid CadastrarClienteRequest request) {
        ClienteResponse resp = controller.cadastrar(request);
        return Response.created(UriBuilder.fromResource(ClienteResource.class)
                        .path("{id}").build(resp.id()))
                .entity(resp)
                .build();
    }

    @GET
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista todos os clientes")
    public List<ClienteResponse> listar() {
        return controller.listar();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Busca um cliente pelo ID")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public ClienteResponse buscar(@PathParam("id") UUID id) {
        return controller.buscar(id);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Atualiza nome/contato de um cliente")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public ClienteResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarClienteRequest request) {
        return controller.atualizar(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Remove um cliente")
    @APIResponse(responseCode = "204", description = "Removido")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public Response remover(@PathParam("id") UUID id) {
        controller.remover(id);
        return Response.noContent().build();
    }
}
