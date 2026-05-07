package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class OrdemServicoNaoEncontradaException extends DomainException {

    public OrdemServicoNaoEncontradaException(OrdemServicoId id) {
        super("Ordem de Serviço não encontrada: " + id);
    }
}
