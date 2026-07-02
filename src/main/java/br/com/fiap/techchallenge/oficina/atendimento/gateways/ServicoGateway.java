package br.com.fiap.techchallenge.oficina.atendimento.gateways;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;

import java.util.List;
import java.util.Optional;

public interface ServicoGateway {

    Servico salvar(Servico servico);

    Optional<Servico> buscarPorId(ServicoId id);

    List<Servico> listar();

    void remover(ServicoId id);
}
