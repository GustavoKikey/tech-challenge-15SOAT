package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

import java.util.UUID;

public class ItemOSNaoEncontradoException extends DomainException {

    public ItemOSNaoEncontradoException(UUID itemId) {
        super("Item da Ordem de Serviço não encontrado: " + itemId);
    }
}
