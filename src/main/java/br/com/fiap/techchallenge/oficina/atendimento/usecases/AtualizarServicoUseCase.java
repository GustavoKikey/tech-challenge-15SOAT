package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.math.BigDecimal;

public class AtualizarServicoUseCase {

    private final ServicoGateway repository;

    public AtualizarServicoUseCase(ServicoGateway repository) {
        this.repository = repository;
    }

    public record Input(ServicoId id, String descricao, BigDecimal valorBase) {}

    public Servico executar(Input input) {
        Servico servico = repository.buscarPorId(input.id())
                .orElseThrow(() -> new ServicoNaoEncontradoException(input.id()));
        servico.alterar(input.descricao(), Dinheiro.de(input.valorBase()));
        return repository.salvar(servico);
    }
}
