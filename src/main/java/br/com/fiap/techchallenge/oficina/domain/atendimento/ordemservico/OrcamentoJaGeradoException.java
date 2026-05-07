package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class OrcamentoJaGeradoException extends DomainException {

    public OrcamentoJaGeradoException() {
        super("Orçamento já foi gerado para esta Ordem de Serviço");
    }
}
