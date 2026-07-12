package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.atendimento.controllers.VeiculoController;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.AtualizarVeiculoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.CadastrarVeiculoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.VeiculoResponse;
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

/** Driver HTTP do agregado Veículo — delega ao {@link VeiculoController}. */
@Path("/veiculos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Veículos", description = "Cadastro de veículos vinculados a clientes")
public class VeiculoResource {

    private final VeiculoController controller;

    public VeiculoResource(VeiculoController controller) {
        this.controller = controller;
    }

    @POST
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Cadastra um novo veículo")
    @APIResponse(responseCode = "201", description = "Veículo criado")
    @APIResponse(responseCode = "400", description = "Placa ou payload inválido")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    @APIResponse(responseCode = "409", description = "Placa já cadastrada")
    public Response cadastrar(@Valid CadastrarVeiculoRequest req) {
        VeiculoResponse resp = controller.cadastrar(req);
        return Response.created(UriBuilder.fromResource(VeiculoResource.class)
                        .path("{id}").build(resp.id()))
                .entity(resp)
                .build();
    }

    @GET
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista todos os veículos")
    public List<VeiculoResponse> listar() {
        return controller.listar();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Busca um veículo pelo ID")
    @APIResponse(responseCode = "404", description = "Veículo não encontrado")
    public VeiculoResponse buscar(@PathParam("id") UUID id) {
        return controller.buscar(id);
    }

    @GET
    @Path("/cliente/{clienteId}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista veículos de um cliente")
    public List<VeiculoResponse> porCliente(@PathParam("clienteId") UUID clienteId) {
        return controller.porCliente(clienteId);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Atualiza ficha técnica e/ou dono do veículo")
    @APIResponse(responseCode = "404", description = "Veículo ou cliente não encontrado")
    public VeiculoResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarVeiculoRequest req) {
        return controller.atualizar(id, req);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Remove um veículo")
    @APIResponse(responseCode = "204", description = "Removido")
    @APIResponse(responseCode = "404", description = "Veículo não encontrado")
    public Response remover(@PathParam("id") UUID id) {
        controller.remover(id);
        return Response.noContent().build();
    }
}
