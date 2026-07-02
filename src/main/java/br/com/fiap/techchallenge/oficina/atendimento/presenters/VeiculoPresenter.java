package br.com.fiap.techchallenge.oficina.atendimento.presenters;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.VeiculoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;

import java.util.List;

/** Presenter do agregado Veículo. */
public class VeiculoPresenter {

    public VeiculoResponse apresentar(Veiculo v) {
        return new VeiculoResponse(
                v.id().valor(),
                v.placa().valor(),
                v.marca(),
                v.modelo(),
                v.ano(),
                v.clienteId().valor());
    }

    public List<VeiculoResponse> apresentar(List<Veiculo> veiculos) {
        return veiculos.stream().map(this::apresentar).toList();
    }
}
