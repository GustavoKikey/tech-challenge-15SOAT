package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.atendimento.controllers.OrdemServicoController;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.CriarOrdemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemPecaRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * Driver HTTP do ciclo de vida da OS. Cuida só de rotas/status/segurança e delega
 * toda a orquestração ao {@link OrdemServicoController}.
 */
@Path("/ordens-servico")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Ordens de Serviço", description = "Ciclo de vida da OS — orçamento, aprovação, execução e entrega")
public class OrdemServicoResource {

    private final OrdemServicoController controller;

    public OrdemServicoResource(OrdemServicoController controller) {
        this.controller = controller;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Cria uma nova Ordem de Serviço (status RECEBIDA)")
    @APIResponse(responseCode = "201", description = "OS criada")
    @APIResponse(responseCode = "404", description = "Cliente ou veículo não encontrado")
    public Response criar(@Valid CriarOrdemServicoRequest req) {
        OrdemServicoResponse resp = controller.criar(req);
        return Response.created(UriBuilder.fromResource(OrdemServicoResource.class)
                        .path("{id}").build(resp.id()))
                .entity(resp)
                .build();
    }

    @GET
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista Ordens de Serviço com filtros opcionais")
    public List<OrdemServicoResponse> listar(@QueryParam("status") String status,
                                             @QueryParam("clienteId") UUID clienteId,
                                             @QueryParam("veiculoId") UUID veiculoId) {
        return controller.listar(status, clienteId, veiculoId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Detalhe completo da OS (itens, orçamento, datas)")
    @APIResponse(responseCode = "404", description = "OS não encontrada")
    public OrdemServicoResponse buscar(@PathParam("id") UUID id) {
        return controller.buscar(id);
    }

    @POST
    @Path("/{id}/diagnostico")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Inicia diagnóstico (RECEBIDA → EM_DIAGNOSTICO)")
    @APIResponse(responseCode = "409", description = "Transição inválida")
    public OrdemServicoResponse iniciarDiagnostico(@PathParam("id") UUID id) {
        return controller.iniciarDiagnostico(id);
    }

    @POST
    @Path("/{id}/servicos")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Adiciona item de serviço (somente em EM_DIAGNOSTICO, sem orçamento)")
    @APIResponse(responseCode = "404", description = "Serviço ou OS não encontrado")
    @APIResponse(responseCode = "409", description = "Status incompatível ou orçamento já gerado")
    public OrdemServicoResponse inserirServico(@PathParam("id") UUID id,
                                               @Valid InserirItemServicoRequest req) {
        return controller.inserirServico(id, req);
    }

    @DELETE
    @Path("/{id}/servicos/{itemId}")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Remove item de serviço (somente antes de gerar orçamento)")
    public OrdemServicoResponse removerServico(@PathParam("id") UUID id,
                                               @PathParam("itemId") UUID itemId) {
        return controller.removerServico(id, itemId);
    }

    @POST
    @Path("/{id}/pecas")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Adiciona item de peça (valida saldo disponível, sem reservar)")
    @APIResponse(responseCode = "404", description = "Peça ou OS não encontrada")
    @APIResponse(responseCode = "422", description = "Estoque insuficiente")
    @APIResponse(responseCode = "409", description = "Status incompatível ou orçamento já gerado")
    public OrdemServicoResponse inserirPeca(@PathParam("id") UUID id,
                                            @Valid InserirItemPecaRequest req) {
        return controller.inserirPeca(id, req);
    }

    @DELETE
    @Path("/{id}/pecas/{itemId}")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Remove item de peça (somente antes de gerar orçamento)")
    public OrdemServicoResponse removerPeca(@PathParam("id") UUID id,
                                            @PathParam("itemId") UUID itemId) {
        return controller.removerPeca(id, itemId);
    }

    @POST
    @Path("/{id}/orcamento")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Gera orçamento e reserva peças no estoque")
    @APIResponse(responseCode = "422", description = "Estoque insuficiente em alguma peça")
    @APIResponse(responseCode = "409", description = "Transição inválida ou orçamento já gerado")
    public OrdemServicoResponse gerarOrcamento(@PathParam("id") UUID id) {
        return controller.gerarOrcamento(id);
    }

    @POST
    @Path("/{id}/orcamento/enviar")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Envia orçamento para aprovação (EM_DIAGNOSTICO → AGUARDANDO_APROVACAO)")
    @APIResponse(responseCode = "409", description = "Transição inválida ou orçamento ainda não gerado")
    public OrdemServicoResponse enviarOrcamento(@PathParam("id") UUID id) {
        return controller.enviarOrcamento(id);
    }

    @POST
    @Path("/{id}/orcamento/aprovar")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Aprova orçamento e dá baixa nas reservas (representa o cliente no MVP)")
    @APIResponse(responseCode = "409", description = "Transição inválida ou orçamento inexistente")
    public OrdemServicoResponse aprovarOrcamento(@PathParam("id") UUID id) {
        return controller.aprovarOrcamento(id);
    }

    @POST
    @Path("/{id}/finalizar")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Mecânico finaliza serviços (EM_EXECUCAO → FINALIZADA)")
    @APIResponse(responseCode = "409", description = "Transição inválida")
    public OrdemServicoResponse finalizar(@PathParam("id") UUID id) {
        return controller.finalizar(id);
    }

    @POST
    @Path("/{id}/entregar")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Atendente entrega o veículo (FINALIZADA → ENTREGUE)")
    @APIResponse(responseCode = "409", description = "Transição inválida")
    public OrdemServicoResponse entregar(@PathParam("id") UUID id) {
        return controller.entregar(id);
    }
}
