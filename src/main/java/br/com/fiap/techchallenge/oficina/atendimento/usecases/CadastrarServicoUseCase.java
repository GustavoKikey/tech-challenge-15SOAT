package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.math.BigDecimal;

public class CadastrarServicoUseCase {

    private final ServicoGateway repository;

    public CadastrarServicoUseCase(ServicoGateway repository) {
        this.repository = repository;
    }

    public record Input(String descricao, BigDecimal valorBase) {}

    public Servico executar(Input input) {
        Servico servico = Servico.novo(input.descricao(), Dinheiro.de(input.valorBase()));
        return repository.salvar(servico);
    }
}
