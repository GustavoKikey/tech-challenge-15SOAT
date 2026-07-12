package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class OrcamentoVazioException extends DomainException {

    public OrcamentoVazioException() {
        super("Não é possível gerar orçamento sem itens de serviço ou peça");
    }
}
