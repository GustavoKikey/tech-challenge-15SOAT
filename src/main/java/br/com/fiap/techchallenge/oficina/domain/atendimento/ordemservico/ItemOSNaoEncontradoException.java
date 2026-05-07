package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;

import java.util.UUID;

public class ItemOSNaoEncontradoException extends DomainException {

    public ItemOSNaoEncontradoException(UUID itemId) {
        super("Item da Ordem de Serviço não encontrado: " + itemId);
    }
}
