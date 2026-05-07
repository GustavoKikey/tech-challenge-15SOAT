package br.com.fiap.techchallenge.oficina.interfaces.rest;

import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.AprovarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.BuscarOrdemServicoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.CriarOrdemServicoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.EnviarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.EntregarVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.FinalizarServicoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.GerarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.IniciarDiagnosticoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.InserirPecaNaOSUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.InserirServicoNaOSUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.ListarOrdensServicoUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.RemoverPecaDaOSUseCase;
import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.RemoverServicoDaOSUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.StatusOS;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.ordemservico.CriarOrdemServicoRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.ordemservico.InserirItemPecaRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.ordemservico.InserirItemServicoRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.ordemservico.OrdemServicoResponse;
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

@Path("/ordens-servico")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Ordens de Serviço", description = "Ciclo de vida da OS — orçamento, aprovação, execução e entrega")
public class OrdemServicoResource {

    private final CriarOrdemServicoUseCase criar;
    private final BuscarOrdemServicoPorIdUseCase buscar;
    private final ListarOrdensServicoUseCase listar;
    private final IniciarDiagnosticoUseCase iniciarDiagnostico;
    private final InserirServicoNaOSUseCase inserirServico;
    private final RemoverServicoDaOSUseCase removerServico;
    private final InserirPecaNaOSUseCase inserirPeca;
    private final RemoverPecaDaOSUseCase removerPeca;
    private final GerarOrcamentoUseCase gerarOrcamento;
    private final EnviarOrcamentoUseCase enviarOrcamento;
    private final AprovarOrcamentoUseCase aprovarOrcamento;
    private final FinalizarServicoUseCase finalizar;
    private final EntregarVeiculoUseCase entregar;

    public OrdemServicoResource(CriarOrdemServicoUseCase criar,
                                BuscarOrdemServicoPorIdUseCase buscar,
                                ListarOrdensServicoUseCase listar,
                                IniciarDiagnosticoUseCase iniciarDiagnostico,
                                InserirServicoNaOSUseCase inserirServico,
                                RemoverServicoDaOSUseCase removerServico,
                                InserirPecaNaOSUseCase inserirPeca,
                                RemoverPecaDaOSUseCase removerPeca,
                                GerarOrcamentoUseCase gerarOrcamento,
                                EnviarOrcamentoUseCase enviarOrcamento,
                                AprovarOrcamentoUseCase aprovarOrcamento,
                                FinalizarServicoUseCase finalizar,
                                EntregarVeiculoUseCase entregar) {
        this.criar = criar;
        this.buscar = buscar;
        this.listar = listar;
        this.iniciarDiagnostico = iniciarDiagnostico;
        this.inserirServico = inserirServico;
        this.removerServico = removerServico;
        this.inserirPeca = inserirPeca;
        this.removerPeca = removerPeca;
        this.gerarOrcamento = gerarOrcamento;
        this.enviarOrcamento = enviarOrcamento;
        this.aprovarOrcamento = aprovarOrcamento;
        this.finalizar = finalizar;
        this.entregar = entregar;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Cria uma nova Ordem de Serviço (status RECEBIDA)")
    @APIResponse(responseCode = "201", description = "OS criada")
    @APIResponse(responseCode = "404", description = "Cliente ou veículo não encontrado")
    public Response criar(@Valid CriarOrdemServicoRequest req) {
        OrdemServico os = criar.executar(new CriarOrdemServicoUseCase.Input(
                req.documentoCliente(), req.placaVeiculo()));
        return Response.created(UriBuilder.fromResource(OrdemServicoResource.class)
                        .path("{id}").build(os.id().valor()))
                .entity(OrdemServicoResponse.from(os))
                .build();
    }

    @GET
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Lista Ordens de Serviço com filtros opcionais")
    public List<OrdemServicoResponse> listar(@QueryParam("status") String status,
                                             @QueryParam("clienteId") UUID clienteId,
                                             @QueryParam("veiculoId") UUID veiculoId) {
        OrdemServicoRepository.Filtro filtro = new OrdemServicoRepository.Filtro(
                status == null ? null : StatusOS.valueOf(status),
                clienteId == null ? null : ClienteId.de(clienteId),
                veiculoId == null ? null : VeiculoId.de(veiculoId));
        return listar.executar(filtro).stream().map(OrdemServicoResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Detalhe completo da OS (itens, orçamento, datas)")
    @APIResponse(responseCode = "404", description = "OS não encontrada")
    public OrdemServicoResponse buscar(@PathParam("id") UUID id) {
        return OrdemServicoResponse.from(buscar.executar(OrdemServicoId.de(id)));
    }

    @POST
    @Path("/{id}/diagnostico")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Inicia diagnóstico (RECEBIDA → EM_DIAGNOSTICO)")
    @APIResponse(responseCode = "409", description = "Transição inválida")
    public OrdemServicoResponse iniciarDiagnostico(@PathParam("id") UUID id) {
        return OrdemServicoResponse.from(iniciarDiagnostico.executar(OrdemServicoId.de(id)));
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
        InserirServicoNaOSUseCase.Output out = inserirServico.executar(
                new InserirServicoNaOSUseCase.Input(
                        OrdemServicoId.de(id),
                        ServicoId.de(req.servicoId()),
                        req.valorCobrado()));
        return OrdemServicoResponse.from(out.ordemServico());
    }

    @DELETE
    @Path("/{id}/servicos/{itemId}")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Remove item de serviço (somente antes de gerar orçamento)")
    public OrdemServicoResponse removerServico(@PathParam("id") UUID id,
                                               @PathParam("itemId") UUID itemId) {
        return OrdemServicoResponse.from(removerServico.executar(
                new RemoverServicoDaOSUseCase.Input(OrdemServicoId.de(id), itemId)));
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
        InserirPecaNaOSUseCase.Output out = inserirPeca.executar(
                new InserirPecaNaOSUseCase.Input(
                        OrdemServicoId.de(id),
                        PecaId.de(req.pecaId()),
                        req.quantidade(),
                        req.valorUnitario()));
        return OrdemServicoResponse.from(out.ordemServico());
    }

    @DELETE
    @Path("/{id}/pecas/{itemId}")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Remove item de peça (somente antes de gerar orçamento)")
    public OrdemServicoResponse removerPeca(@PathParam("id") UUID id,
                                            @PathParam("itemId") UUID itemId) {
        return OrdemServicoResponse.from(removerPeca.executar(
                new RemoverPecaDaOSUseCase.Input(OrdemServicoId.de(id), itemId)));
    }

    @POST
    @Path("/{id}/orcamento")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Gera orçamento e reserva peças no estoque")
    @APIResponse(responseCode = "422", description = "Estoque insuficiente em alguma peça")
    @APIResponse(responseCode = "409", description = "Transição inválida ou orçamento já gerado")
    public OrdemServicoResponse gerarOrcamento(@PathParam("id") UUID id) {
        return OrdemServicoResponse.from(gerarOrcamento.executar(OrdemServicoId.de(id)));
    }

    @POST
    @Path("/{id}/orcamento/enviar")
    @RolesAllowed({"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Envia orçamento para aprovação (EM_DIAGNOSTICO → AGUARDANDO_APROVACAO)")
    @APIResponse(responseCode = "409", description = "Transição inválida ou orçamento ainda não gerado")
    public OrdemServicoResponse enviarOrcamento(@PathParam("id") UUID id) {
        return OrdemServicoResponse.from(enviarOrcamento.executar(OrdemServicoId.de(id)));
    }

    @POST
    @Path("/{id}/orcamento/aprovar")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Aprova orçamento e dá baixa nas reservas (representa o cliente no MVP)")
    @APIResponse(responseCode = "409", description = "Transição inválida ou orçamento inexistente")
    public OrdemServicoResponse aprovarOrcamento(@PathParam("id") UUID id) {
        return OrdemServicoResponse.from(aprovarOrcamento.executar(OrdemServicoId.de(id)));
    }

    @POST
    @Path("/{id}/finalizar")
    @RolesAllowed({"MECANICO", "ADMINISTRADOR"})
    @Operation(summary = "Mecânico finaliza serviços (EM_EXECUCAO → FINALIZADA)")
    @APIResponse(responseCode = "409", description = "Transição inválida")
    public OrdemServicoResponse finalizar(@PathParam("id") UUID id) {
        return OrdemServicoResponse.from(finalizar.executar(OrdemServicoId.de(id)));
    }

    @POST
    @Path("/{id}/entregar")
    @RolesAllowed({"ATENDENTE", "ADMINISTRADOR"})
    @Operation(summary = "Atendente entrega o veículo (FINALIZADA → ENTREGUE)")
    @APIResponse(responseCode = "409", description = "Transição inválida")
    public OrdemServicoResponse entregar(@PathParam("id") UUID id) {
        return OrdemServicoResponse.from(entregar.executar(OrdemServicoId.de(id)));
    }
}
