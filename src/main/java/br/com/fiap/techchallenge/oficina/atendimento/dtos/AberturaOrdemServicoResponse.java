package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Retorno da abertura de OS — destaca a identificação única exigida pelo enunciado.
 */
public record AberturaOrdemServicoResponse(
        UUID id,
        String status,
        String descricaoStatus,
        OffsetDateTime criadaEm
) {}
