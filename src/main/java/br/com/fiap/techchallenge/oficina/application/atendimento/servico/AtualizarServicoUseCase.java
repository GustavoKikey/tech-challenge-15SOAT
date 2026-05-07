package br.com.fiap.techchallenge.oficina.application.atendimento.servico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class AtualizarServicoUseCase {

    private final ServicoRepository repository;

    public AtualizarServicoUseCase(ServicoRepository repository) {
        this.repository = repository;
    }

    public record Input(ServicoId id, String descricao, BigDecimal valorBase) {}

    @Transactional
    public Servico executar(Input input) {
        Servico servico = repository.buscarPorId(input.id())
                .orElseThrow(() -> new ServicoNaoEncontradoException(input.id()));
        servico.alterar(input.descricao(), Dinheiro.de(input.valorBase()));
        return repository.salvar(servico);
    }
}
