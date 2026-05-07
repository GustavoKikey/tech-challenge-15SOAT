package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador da Ordem de Serviço (raiz do agregado).
 *
 * <p>Vive aqui antes do agregado existir porque o BC <b>Estoque</b> precisa referenciar
 * a OS dona de uma reserva (relação Customer-Supplier do Context Map). Cross-BC sempre
 * por ID, nunca por referência ao agregado.
 */
public record OrdemServicoId(UUID valor) {

    public OrdemServicoId {
        Objects.requireNonNull(valor, "OrdemServicoId não pode ser nulo");
    }

    public static OrdemServicoId novo() {
        return new OrdemServicoId(UUID.randomUUID());
    }

    public static OrdemServicoId de(UUID valor) {
        return new OrdemServicoId(valor);
    }

    public static OrdemServicoId de(String valor) {
        return new OrdemServicoId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
