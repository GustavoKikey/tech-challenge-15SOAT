package br.com.fiap.techchallenge.oficina.interfaces.rest.dto.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;

import java.util.UUID;

public record VeiculoResponse(
        UUID id,
        String placa,
        String marca,
        String modelo,
        int ano,
        UUID clienteId
) {
    public static VeiculoResponse from(Veiculo v) {
        return new VeiculoResponse(
                v.id().valor(),
                v.placa().valor(),
                v.marca(),
                v.modelo(),
                v.ano(),
                v.clienteId().valor()
        );
    }
}
