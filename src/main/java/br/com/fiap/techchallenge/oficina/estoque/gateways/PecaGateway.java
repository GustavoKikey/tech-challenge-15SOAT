package br.com.fiap.techchallenge.oficina.estoque.gateways;

import br.com.fiap.techchallenge.oficina.estoque.entities.Peca;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;

import java.util.List;
import java.util.Optional;

public interface PecaGateway {

    /** Persiste a raiz inteira (cobre criação e atualização, incluindo reservas). */
    Peca salvar(Peca peca);

    Optional<Peca> buscarPorId(PecaId id);

    /**
     * Busca com lock pessimista (PESSIMISTIC_WRITE). Usado pelos use cases de
     * <i>reservar</i> e <i>baixar</i> para evitar race condition em concorrência —
     * dois pedidos simultâneos não podem ler o mesmo saldo disponível e ambos
     * passar por validação.
     */
    Optional<Peca> buscarPorIdComLock(PecaId id);

    List<Peca> listar();

    void remover(PecaId id);
}
