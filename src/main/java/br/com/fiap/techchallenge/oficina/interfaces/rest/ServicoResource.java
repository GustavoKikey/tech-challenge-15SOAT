package br.com.fiap.techchallenge.oficina.interfaces.rest;

import br.com.fiap.techchallenge.oficina.application.atendimento.servico.AtualizarServicoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.servico.BuscarServicoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.servico.CadastrarServicoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.servico.ListarServicosUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.servico.RemoverServicoUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.servico.AtualizarServicoRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.servico.CadastrarServicoRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.servico.ServicoResponse;
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

@Path("/servicos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@Tag(name = "Serviços", description = "Catálogo de serviços de mão-de-obra")
public class ServicoResource {

    private final CadastrarServicoUseCase cadastrar;
    private final AtualizarServicoUseCase atualizar;
    private final RemoverServicoUseCase remover;
    private final BuscarServicoPorIdUseCase buscar;
    private final ListarServicosUseCase listar;

    public ServicoResource(CadastrarServicoUseCase cadastrar,
                           AtualizarServicoUseCase atualizar,
                           RemoverServicoUseCase remover,
                           BuscarServicoPorIdUseCase buscar,
                           ListarServicosUseCase listar) {
        this.cadastrar = cadastrar;
        this.atualizar = atualizar;
        this.remover = remover;
        this.buscar = buscar;
        this.listar = listar;
    }

    @POST
    @Operation(summary = "Cadastra um novo serviço")
    @APIResponse(responseCode = "201", description = "Serviço criado")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    public Response cadastrar(@Valid CadastrarServicoRequest req) {
        Servico s = cadastrar.executar(new CadastrarServicoUseCase.Input(req.descricao(), req.valorBase()));
        return Response.created(UriBuilder.fromResource(ServicoResource.class)
                        .path("{id}").build(s.id().valor()))
                .entity(ServicoResponse.from(s))
                .build();
    }

    @GET
    @Operation(summary = "Lista todos os serviços")
    public List<ServicoResponse> listar() {
        return listar.executar().stream().map(ServicoResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca um serviço pelo ID")
    @APIResponse(responseCode = "404", description = "Serviço não encontrado")
    public ServicoResponse buscar(@PathParam("id") UUID id) {
        return ServicoResponse.from(buscar.executar(ServicoId.de(id)));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza um serviço")
    @APIResponse(responseCode = "404", description = "Serviço não encontrado")
    public ServicoResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarServicoRequest req) {
        Servico s = atualizar.executar(new AtualizarServicoUseCase.Input(
                ServicoId.de(id), req.descricao(), req.valorBase()));
        return ServicoResponse.from(s);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove um serviço")
    @APIResponse(responseCode = "204", description = "Removido")
    @APIResponse(responseCode = "404", description = "Serviço não encontrado")
    public Response remover(@PathParam("id") UUID id) {
        remover.executar(ServicoId.de(id));
        return Response.noContent().build();
    }
}
