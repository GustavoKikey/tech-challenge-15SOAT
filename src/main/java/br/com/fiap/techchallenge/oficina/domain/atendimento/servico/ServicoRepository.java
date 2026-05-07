package br.com.fiap.techchallenge.oficina.domain.atendimento.servico;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository {

    Servico salvar(Servico servico);

    Optional<Servico> buscarPorId(ServicoId id);

    List<Servico> listar();

    void remover(ServicoId id);
}
