package br.com.fiap.techchallenge.oficina.atendimento.gateways;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;

import java.util.List;
import java.util.Optional;

public interface VeiculoGateway {

    Veiculo salvar(Veiculo veiculo);

    Optional<Veiculo> buscarPorId(VeiculoId id);

    Optional<Veiculo> buscarPorPlaca(Placa placa);

    List<Veiculo> listarPorCliente(ClienteId clienteId);

    List<Veiculo> listar();

    void remover(VeiculoId id);
}
