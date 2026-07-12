package br.com.fiap.techchallenge.oficina.atendimento.gateways;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;

import br.com.fiap.techchallenge.oficina.shared.entities.Documento;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência do agregado {@link Cliente}. Sem dependência de framework —
 * impl em {@code infrastructure/persistence}.
 */
public interface ClienteGateway {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarPorId(ClienteId id);

    Optional<Cliente> buscarPorDocumento(Documento documento);

    List<Cliente> listar();

    void remover(ClienteId id);
}
