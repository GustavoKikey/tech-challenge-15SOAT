package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

public class VeiculoJaCadastradoException extends DomainException {

    public VeiculoJaCadastradoException(String placa) {
        super("Já existe veículo cadastrado com a placa: " + placa);
    }
}
