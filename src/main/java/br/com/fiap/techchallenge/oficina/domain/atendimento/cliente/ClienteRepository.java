package br.com.fiap.techchallenge.oficina.domain.atendimento.cliente;

import br.com.fiap.techchallenge.oficina.domain.shared.Documento;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência do agregado {@link Cliente}. Sem dependência de framework —
 * impl em {@code infrastructure/persistence}.
 */
public interface ClienteRepository {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarPorId(ClienteId id);

    Optional<Cliente> buscarPorDocumento(Documento documento);

    List<Cliente> listar();

    void remover(ClienteId id);
}
