package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class OrcamentoJaGeradoException extends DomainException {

    public OrcamentoJaGeradoException() {
        super("Orçamento já foi gerado para esta Ordem de Serviço");
    }
}
