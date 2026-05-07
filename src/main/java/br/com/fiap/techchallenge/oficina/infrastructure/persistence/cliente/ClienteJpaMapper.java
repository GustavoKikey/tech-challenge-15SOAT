package br.com.fiap.techchallenge.oficina.infrastructure.persistence.cliente;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.shared.Documento;

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
