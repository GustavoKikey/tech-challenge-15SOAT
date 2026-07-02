package br.com.fiap.techchallenge.oficina.atendimento.presenters;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.ServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Servico;

import java.util.List;

/** Presenter do agregado Serviço. */
public class ServicoPresenter {

    public ServicoResponse apresentar(Servico s) {
        return new ServicoResponse(
                s.id().valor(),
                s.descricao(),
                s.valorBase().valor());
    }

    public List<ServicoResponse> apresentar(List<Servico> servicos) {
        return servicos.stream().map(this::apresentar).toList();
    }
}
