package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class VeiculoNaoEncontradoException extends DomainException {

    public VeiculoNaoEncontradoException(VeiculoId id) {
        super("Veículo não encontrado: " + id);
    }

    public VeiculoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
