package br.com.fiap.techchallenge.oficina.interfaces.rest;

import br.com.fiap.techchallenge.oficina.application.estoque.AdicionarSaldoUseCase;
import br.com.fiap.techchallenge.oficina.application.estoque.AtualizarPecaUseCase;
import br.com.fiap.techchallenge.oficina.application.estoque.BuscarPecaPorIdUseCase;
import br.com.fiap.techchallenge.oficina.application.estoque.CadastrarPecaUseCase;
import br.com.fiap.techchallenge.oficina.application.estoque.ListarPecasUseCase;
import br.com.fiap.techchallenge.oficina.application.estoque.RemoverPecaUseCase;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.peca.AdicionarSaldoRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.peca.AtualizarPecaRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.peca.CadastrarPecaRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.peca.PecaResponse;
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

@Path("/pecas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@Tag(name = "Peças / Estoque", description = "Catálogo de peças e insumos com controle de estoque")
public class PecaResource {

    private final CadastrarPecaUseCase cadastrar;
    private final AtualizarPecaUseCase atualizar;
    private final RemoverPecaUseCase remover;
    private final BuscarPecaPorIdUseCase buscar;
    private final ListarPecasUseCase listar;
    private final AdicionarSaldoUseCase adicionarSaldo;

    public PecaResource(CadastrarPecaUseCase cadastrar,
                        AtualizarPecaUseCase atualizar,
                        RemoverPecaUseCase remover,
                        BuscarPecaPorIdUseCase buscar,
                        ListarPecasUseCase listar,
                        AdicionarSaldoUseCase adicionarSaldo) {
        this.cadastrar = cadastrar;
        this.atualizar = atualizar;
        this.remover = remover;
        this.buscar = buscar;
        this.listar = listar;
        this.adicionarSaldo = adicionarSaldo;
    }

    @POST
    @Operation(summary = "Cadastra uma nova peça")
    @APIResponse(responseCode = "201", description = "Peça criada")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    public Response cadastrar(@Valid CadastrarPecaRequest req) {
        Peca p = cadastrar.executar(new CadastrarPecaUseCase.Input(
                req.descricao(), req.valorUnitario(), req.quantidadeInicialOuZero()));
        return Response.created(UriBuilder.fromResource(PecaResource.class)
                        .path("{id}").build(p.id().valor()))
                .entity(PecaResponse.from(p))
                .build();
    }

    @GET
    @Operation(summary = "Lista todas as peças")
    public List<PecaResponse> listar() {
        return listar.executar().stream().map(PecaResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma peça pelo ID (inclui saldo disponível)")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    public PecaResponse buscar(@PathParam("id") UUID id) {
        return PecaResponse.from(buscar.executar(PecaId.de(id)));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualiza dados de catálogo da peça (descrição e valor unitário)")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    public PecaResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarPecaRequest req) {
        Peca p = atualizar.executar(new AtualizarPecaUseCase.Input(
                PecaId.de(id), req.descricao(), req.valorUnitario()));
        return PecaResponse.from(p);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove uma peça (bloqueado se houver reservas ativas)")
    @APIResponse(responseCode = "204", description = "Removida")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    @APIResponse(responseCode = "409", description = "Peça possui reservas ativas")
    public Response remover(@PathParam("id") UUID id) {
        remover.executar(PecaId.de(id));
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/saldo")
    @Operation(summary = "Adiciona saldo (compra/devolução) à peça")
    @APIResponse(responseCode = "200", description = "Saldo atualizado")
    @APIResponse(responseCode = "400", description = "Quantidade inválida")
    @APIResponse(responseCode = "404", description = "Peça não encontrada")
    public PecaResponse adicionarSaldo(@PathParam("id") UUID id, @Valid AdicionarSaldoRequest req) {
        Peca p = adicionarSaldo.executar(new AdicionarSaldoUseCase.Input(
                PecaId.de(id), req.quantidade()));
        return PecaResponse.from(p);
    }
}
