package br.com.fiap.techchallenge.oficina.atendimento.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Consulta de status da OS (fase 2): situação atual com descrição amigável
 * (Recebida, Diagnóstico, Aguardando Aprovação, Execução, Finalizada, Entregue).
 */
public record StatusOSResponse(
        UUID id,
        String status,
        String descricao,
        OffsetDateTime atualizadoEm
) {}
