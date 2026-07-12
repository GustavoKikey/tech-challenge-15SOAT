package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Payload do endpoint de notificação externa da decisão do cliente sobre o orçamento.
 */
public record DecisaoOrcamentoRequest(
        @Schema(example = "true", description = "true = orçamento aprovado; false = recusado")
        @NotNull Boolean aprovado
) {}
