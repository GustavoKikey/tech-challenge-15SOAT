package br.com.fiap.techchallenge.oficina.external.persistence.cliente;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.shared.entities.Documento;

final class ClienteJpaMapper {

    private ClienteJpaMapper() {}

    static ClienteJpaEntity toEntity(Cliente cliente, ClienteJpaEntity existente) {
        ClienteJpaEntity entity = existente != null ? existente : new ClienteJpaEntity();
        entity.id = cliente.id().valor();
        entity.nome = cliente.nome();
        entity.documento = cliente.documento().numero();
        entity.email = cliente.email();
        entity.telefone = cliente.telefone();
        return entity;
    }

    static Cliente toDomain(ClienteJpaEntity entity) {
        return Cliente.reconstituir(
                ClienteId.de(entity.id),
                entity.nome,
                Documento.de(entity.documento),
                entity.email,
                entity.telefone
        );
    }
}
