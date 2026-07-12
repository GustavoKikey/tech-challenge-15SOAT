package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class OrdemServicoNaoEncontradaException extends DomainException {

    public OrdemServicoNaoEncontradaException(OrdemServicoId id) {
        super("Ordem de Serviço não encontrada: " + id);
    }
}
