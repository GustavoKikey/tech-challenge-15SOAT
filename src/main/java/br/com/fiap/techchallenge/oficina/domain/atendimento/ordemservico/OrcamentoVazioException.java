package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class OrcamentoVazioException extends DomainException {

    public OrcamentoVazioException() {
        super("Não é possível gerar orçamento sem itens de serviço ou peça");
    }
}
