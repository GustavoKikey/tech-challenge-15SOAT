package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class ServicoNaoEncontradoException extends DomainException {

    public ServicoNaoEncontradoException(ServicoId id) {
        super("Serviço não encontrado: " + id);
    }
}
