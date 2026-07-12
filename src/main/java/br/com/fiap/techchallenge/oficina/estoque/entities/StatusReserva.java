package br.com.fiap.techchallenge.oficina.estoque.entities;

/**
 * Estados de uma {@link Reserva}.
 *
 * <ul>
 *   <li>{@code ATIVA} — peça comprometida com a OS, ainda conta no saldo reservado.</li>
 *   <li>{@code BAIXADA} — saldo já decrementado após aprovação do orçamento.</li>
 *   <li>{@code CANCELADA} — reserva liberada (rollback ou desistência), saldo volta.</li>
 * </ul>
 */
public enum StatusReserva {
    ATIVA, BAIXADA, CANCELADA
}
