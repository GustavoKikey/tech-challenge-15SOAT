package br.com.fiap.techchallenge.oficina.interfaces.rest;

import br.com.fiap.techchallenge.oficina.application.atendimento.cliente.AtualizarClienteUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.cliente.BuscarClientePorIdUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.cliente.CadastrarClienteUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.cliente.ListarClientesUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.cliente.RemoverClienteUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.cliente.AtualizarClienteRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.cliente.CadastrarClienteRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.cliente.ClienteResponse;
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

@Path("/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Clientes", description = "Cadastro de clientes (PF ou PJ)")
public class ClienteResource {

    private final CadastrarClienteUseCase cadastrar;
    private final AtualizarClienteUseCase atualizar;
    private final RemoverClienteUseCase remover;
    private final BuscarClientePorIdUseCase buscar;
    private final ListarClientesUseCase listar;

    public ClienteResource(CadastrarClienteUseCase cadastrar,
                           AtualizarClienteUseCase atualizar,
                           RemoverClienteUseCase remover,
                           BuscarClientePorIdUseCase buscar,
                           ListarClientesUseCase listar) {
        this.cadastrar = cadastrar;
        this.atualizar = atualizar;
        this.remover = remover;
        this.buscar = buscar;
        this.listar = listar;
    }

    @POST
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Cadastra um novo cliente")
    @APIResponse(responseCode = "201", description = "Cliente criado")
    @APIResponse(responseCode = "400", description = "Documento ou payload inválido")
    @APIResponse(responseCode = "409", description = "Documento já cadastrado")
    public Response cadastrar(@Valid CadastrarClienteRequest request) {
        Cliente cliente = cadastrar.executar(new CadastrarClienteUseCase.Input(
                request.nome(), request.documento(), request.email(), request.telefone()));
        return Response.created(UriBuilder.fromResource(ClienteResource.class)
                        .path("{id}").build(cliente.id().valor()))
                .entity(ClienteResponse.from(cliente))
                .build();
    }

    @GET
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista todos os clientes")
    public List<ClienteResponse> listar() {
        return listar.executar().stream().map(ClienteResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Busca um cliente pelo ID")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public ClienteResponse buscar(@PathParam("id") UUID id) {
        return ClienteResponse.from(buscar.executar(ClienteId.de(id)));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Atualiza nome/contato de um cliente")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public ClienteResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarClienteRequest request) {
        Cliente atualizado = atualizar.executar(new AtualizarClienteUseCase.Input(
                ClienteId.de(id), request.nome(), request.email(), request.telefone()));
        return ClienteResponse.from(atualizado);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Remove um cliente")
    @APIResponse(responseCode = "204", description = "Removido")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public Response remover(@PathParam("id") UUID id) {
        remover.executar(ClienteId.de(id));
        return Response.noContent().build();
    }
}
