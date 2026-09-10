package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;

/**
 * Um cliente autenticado tentou acessar uma OS que não é dele.
 *
 * <p>Mapeada para <b>403 Forbidden</b>, não 404: o token é válido e o recurso existe —
 * o que falta é autorização. Também não expõe se a OS existe ou não, evitando que o
 * endpoint vire um oráculo para descobrir ids válidos por tentativa.
 */
public class AcessoNegadoAOrdemServicoException extends DomainException {

    public AcessoNegadoAOrdemServicoException(OrdemServicoId id) {
        super("Ordem de Serviço não pertence ao cliente autenticado: " + id);
    }
}
