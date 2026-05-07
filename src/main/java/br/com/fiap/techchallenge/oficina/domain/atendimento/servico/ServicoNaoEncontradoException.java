package br.com.fiap.techchallenge.oficina.domain.atendimento.servico;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class ServicoNaoEncontradoException extends DomainException {

    public ServicoNaoEncontradoException(ServicoId id) {
        super("Serviço não encontrado: " + id);
    }
}
