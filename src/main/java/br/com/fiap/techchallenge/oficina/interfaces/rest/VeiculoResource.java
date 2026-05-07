package br.com.fiap.techchallenge.oficina.interfaces.rest;

import br.com.fiap.techchallenge.oficina.application.atendimento.veiculo.AtualizarVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.veiculo.BuscarVeiculoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.veiculo.CadastrarVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.veiculo.ListarVeiculosUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.veiculo.RemoverVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.veiculo.AtualizarVeiculoRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.veiculo.CadastrarVeiculoRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.veiculo.VeiculoResponse;
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

@Path("/veiculos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Veículos", description = "Cadastro de veículos vinculados a clientes")
public class VeiculoResource {

    private final CadastrarVeiculoUseCase cadastrar;
    private final AtualizarVeiculoUseCase atualizar;
    private final RemoverVeiculoUseCase remover;
    private final BuscarVeiculoPorIdUseCase buscar;
    private final ListarVeiculosUseCase listar;

    public VeiculoResource(CadastrarVeiculoUseCase cadastrar,
                           AtualizarVeiculoUseCase atualizar,
                           RemoverVeiculoUseCase remover,
                           BuscarVeiculoPorIdUseCase buscar,
                           ListarVeiculosUseCase listar) {
        this.cadastrar = cadastrar;
        this.atualizar = atualizar;
        this.remover = remover;
        this.buscar = buscar;
        this.listar = listar;
    }

    @POST
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Cadastra um novo veículo")
    @APIResponse(responseCode = "201", description = "Veículo criado")
    @APIResponse(responseCode = "400", description = "Placa ou payload inválido")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    @APIResponse(responseCode = "409", description = "Placa já cadastrada")
    public Response cadastrar(@Valid CadastrarVeiculoRequest req) {
        Veiculo veiculo = cadastrar.executar(new CadastrarVeiculoUseCase.Input(
                req.placa(), req.marca(), req.modelo(), req.ano(), ClienteId.de(req.clienteId())));
        return Response.created(UriBuilder.fromResource(VeiculoResource.class)
                        .path("{id}").build(veiculo.id().valor()))
                .entity(VeiculoResponse.from(veiculo))
                .build();
    }

    @GET
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista todos os veículos")
    public List<VeiculoResponse> listar() {
        return listar.executar().stream().map(VeiculoResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Busca um veículo pelo ID")
    @APIResponse(responseCode = "404", description = "Veículo não encontrado")
    public VeiculoResponse buscar(@PathParam("id") UUID id) {
        return VeiculoResponse.from(buscar.executar(VeiculoId.de(id)));
    }

    @GET
    @Path("/cliente/{clienteId}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista veículos de um cliente")
    public List<VeiculoResponse> porCliente(@PathParam("clienteId") UUID clienteId) {
        return listar.porCliente(ClienteId.de(clienteId)).stream()
                .map(VeiculoResponse::from).toList();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Atualiza ficha técnica e/ou dono do veículo")
    @APIResponse(responseCode = "404", description = "Veículo ou cliente não encontrado")
    public VeiculoResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarVeiculoRequest req) {
        Veiculo atualizado = atualizar.executar(new AtualizarVeiculoUseCase.Input(
                VeiculoId.de(id), req.marca(), req.modelo(), req.ano(),
                req.clienteId() == null ? null : ClienteId.de(req.clienteId())));
        return VeiculoResponse.from(atualizado);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Remove um veículo")
    @APIResponse(responseCode = "204", description = "Removido")
    @APIResponse(responseCode = "404", description = "Veículo não encontrado")
    public Response remover(@PathParam("id") UUID id) {
        remover.executar(VeiculoId.de(id));
        return Response.noContent().build();
    }
}
