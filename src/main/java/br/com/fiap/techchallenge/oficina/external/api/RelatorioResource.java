package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.relatorio.controllers.RelatorioController;
import br.com.fiap.techchallenge.oficina.relatorio.dtos.TempoMedioResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;

/**
 * Driver HTTP de relatórios. Faz só o parsing dos query params (concern de
 * protocolo) e delega ao {@link RelatorioController}. Sempre exige ADMINISTRADOR.
 */
@Path("/relatorios")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
@Tag(name = "Relatórios", description = "Indicadores de gestão (read models)")
public class RelatorioResource {

    private final RelatorioController controller;

    public RelatorioResource(RelatorioController controller) {
        this.controller = controller;
    }

    @GET
    @Path("/tempo-medio-execucao")
    @Operation(summary = "Tempo médio de execução das OS (em segundos)")
    @APIResponse(responseCode = "200", description = "Indicador calculado")
    @APIResponse(responseCode = "401", description = "Não autenticado")
    @APIResponse(responseCode = "403", description = "Sem permissão")
    public TempoMedioResponse tempoMedioExecucao(
            @QueryParam("desde") String desde,
            @QueryParam("ate") String ate) {
        LocalDate desdeDate = desde == null || desde.isBlank() ? null : LocalDate.parse(desde);
        LocalDate ateDate = ate == null || ate.isBlank() ? null : LocalDate.parse(ate);
        return controller.tempoMedioExecucao(desdeDate, ateDate);
    }
}
