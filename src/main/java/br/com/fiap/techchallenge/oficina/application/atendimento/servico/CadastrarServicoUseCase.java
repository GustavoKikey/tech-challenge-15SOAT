package br.com.fiap.techchallenge.oficina.application.atendimento.servico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class CadastrarServicoUseCase {

    private final ServicoRepository repository;

    public CadastrarServicoUseCase(ServicoRepository repository) {
        this.repository = repository;
    }

    public record Input(String descricao, BigDecimal valorBase) {}

    @Transactional
    public Servico executar(Input input) {
        Servico servico = Servico.novo(input.descricao(), Dinheiro.de(input.valorBase()));
        return repository.salvar(servico);
    }
}
