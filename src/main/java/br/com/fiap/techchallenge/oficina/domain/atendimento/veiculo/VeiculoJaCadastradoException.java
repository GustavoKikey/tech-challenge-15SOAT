package br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

public class VeiculoJaCadastradoException extends DomainException {

    public VeiculoJaCadastradoException(String placa) {
        super("Já existe veículo cadastrado com a placa: " + placa);
    }
}
