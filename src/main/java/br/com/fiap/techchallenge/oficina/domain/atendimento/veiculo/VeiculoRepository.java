package br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.shared.Placa;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository {

    Veiculo salvar(Veiculo veiculo);

    Optional<Veiculo> buscarPorId(VeiculoId id);

    Optional<Veiculo> buscarPorPlaca(Placa placa);

    List<Veiculo> listarPorCliente(ClienteId clienteId);

    List<Veiculo> listar();

    void remover(VeiculoId id);
}
