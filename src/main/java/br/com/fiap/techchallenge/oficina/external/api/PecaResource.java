package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.estoque.controllers.PecaController;
import br.com.fiap.techchallenge.oficina.estoque.dtos.AdicionarSaldoRequest;
import br.com.fiap.techchallenge.oficina.estoque.dtos.AtualizarPecaRequest;
import br.com.fiap.techchallenge.oficina.estoque.dtos.CadastrarPecaRequest;
import br.com.fiap.techchallenge.oficina.estoque.dtos.PecaResponse;
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

/**
 * Driver HTTP do BC Estoque. Cuida apenas de protocolo (rotas, status, segurança)
 * e delega ao {@link PecaController}.
 */
@Path("/pecas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@Tag(name = "Peças / Estoque", description = "Catálogo de peças e insumos com controle de estoque")
public class PecaResource {

    private final PecaController controller;

    public PecaResource(PecaController controller) {
        this.controller = controller;
    }

    @POST
    @Operation(summary = "Cadastra uma nova peça")
    @APIResponse(responseCode = "201", description = "Peça criada")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    public Response cadastrar(@Valid CadastrarPecaRequest req) {
        PecaResponse resp = controller.cadastrar(req);
        return Response.created(UriBuilder.fromResource(PecaResource.class)
                        .path("{id}").build(resp.id()))
                .entity(resp)
                .build();
    }

    @GET
    @Operation(summary = "Lista todas as peças")
    public List<PecaResponse> listar() {
        return controller.listar();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma peça pelo ID (inclui saldo disponível)")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    public PecaResponse buscar(@PathParam("id") UUID id) {
        return controller.buscar(id);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza dados de catálogo da peça (descrição e valor unitário)")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    public PecaResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarPecaRequest req) {
        return controller.atualizar(id, req);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove uma peça (bloqueado se houver reservas ativas)")
    @APIResponse(responseCode = "204", description = "Removida")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    @APIResponse(responseCode = "409", description = "Peça possui reservas ativas")
    public Response remover(@PathParam("id") UUID id) {
        controller.remover(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/saldo")
    @Operation(summary = "Adiciona saldo (compra/devolução) à peça")
    @APIResponse(responseCode = "200", description = "Saldo atualizado")
    @APIResponse(responseCode = "400", description = "Quantidade inválida")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    public PecaResponse adicionarSaldo(@PathParam("id") UUID id, @Valid AdicionarSaldoRequest req) {
        return controller.adicionarSaldo(id, req);
    }
}
