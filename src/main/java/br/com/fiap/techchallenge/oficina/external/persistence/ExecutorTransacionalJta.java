package br.com.fiap.techchallenge.oficina.external.persistence;

import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.function.Supplier;

/**
 * Adapter JTA da porta {@link ExecutorTransacional}. É o ÚNICO ponto que conhece
 * {@code jakarta.transaction} — o detalhe transacional fica confinado na camada
 * mais externa, exatamente onde a Regra de Dependência manda.
 *
 * <p>Semântica {@code REQUIRED} (default de {@link Transactional}): se já houver
 * transação ativa, junta-se a ela; senão, abre uma nova. É isso que mantém a
 * atomicidade aninhada — quando {@code AprovarOrcamento} (transacional) chama
 * {@code BaixarPeca}, a baixa entra na MESMA transação e reverte junto em caso de
 * falha.
 */
@ApplicationScoped
public class ExecutorTransacionalJta implements ExecutorTransacional {

    @Override
    @Transactional
    public <T> T emTransacao(Supplier<T> acao) {
        return acao.get();
    }
}
