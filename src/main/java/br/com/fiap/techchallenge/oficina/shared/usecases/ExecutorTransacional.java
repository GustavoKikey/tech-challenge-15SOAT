package br.com.fiap.techchallenge.oficina.shared.usecases;

import java.util.function.Supplier;

/**
 * Porta de transação (Clean Architecture: a aplicação declara <i>que</i> uma
 * interação é atômica, sem saber <i>como</i> — JTA, JDBC, etc.).
 *
 * <p>Definida no núcleo (sem dependência de framework) e implementada na camada
 * {@code external}. Permite que Controllers/Use Cases demarquem transações sem
 * importar {@code jakarta.transaction} — invertendo a dependência do detalhe
 * técnico (Dependency Inversion Principle).
 */
public interface ExecutorTransacional {

    /**
     * Executa a ação dentro de uma transação e devolve seu resultado. Ações sem
     * retorno devolvem {@code null} (mantemos um único método para evitar a
     * ambiguidade clássica entre {@code Supplier} e {@code Runnable} em lambdas).
     */
    <T> T emTransacao(Supplier<T> acao);
}
