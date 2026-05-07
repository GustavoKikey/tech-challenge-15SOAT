package br.com.fiap.techchallenge.oficina.interfaces.rest;

import br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico.BuscarOrdemServicoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.ordemservico.OrdemServicoPublicaResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/**
 * Endpoint público de acompanhamento da OS.
 *
 * <p>Retorna apenas dados resumidos (status, valor total, datas-chave) — sem
 * itens, sem reservas, sem ids de cliente/veículo. A proteção formal das demais
 * APIs (JWT); este endpoint fica preparado para ser o ponto
 * sem autenticação que o cliente usa para acompanhar.
 */
@Path("/publico/ordens-servico")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "Público", description = "Consulta pública e resumida de Ordens de Serviço")
public class PublicoOrdemServicoResource {

    private final BuscarOrdemServicoPorIdUseCase buscar;

    public PublicoOrdemServicoResource(BuscarOrdemServicoPorIdUseCase buscar) {
        this.buscar = buscar;
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta resumida de uma OS para acompanhamento do cliente")
    @APIResponse(responseCode = "404", description = "OS não encontrada")
    public OrdemServicoPublicaResponse buscar(@PathParam("id") UUID id) {
        return OrdemServicoPublicaResponse.from(buscar.executar(OrdemServicoId.de(id)));
    }
}
