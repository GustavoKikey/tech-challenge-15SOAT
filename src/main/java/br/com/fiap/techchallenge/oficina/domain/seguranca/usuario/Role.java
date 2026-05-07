package br.com.fiap.techchallenge.oficina.domain.seguranca.usuario;

/**
 * Papéis administrativos do MVP. Mapeiam diretamente os atores administrativos do
 * PDF DDD (pág. 1) — Atendente, Mecânico e Administrador. O cliente final não tem
 * usuário no sistema; ele consulta a OS pelo endpoint público.
 *
 * <p>Os nomes em maiúsculo são propagados como <i>groups</i> dentro do JWT.
 */
public enum Role {
    ATENDENTE,
    MECANICO,
    ADMINISTRADOR
}
