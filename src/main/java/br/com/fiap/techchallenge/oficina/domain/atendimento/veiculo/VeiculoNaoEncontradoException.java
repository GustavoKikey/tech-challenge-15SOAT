package br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class VeiculoNaoEncontradoException extends DomainException {

    public VeiculoNaoEncontradoException(VeiculoId id) {
        super("Veículo não encontrado: " + id);
    }

    public VeiculoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
