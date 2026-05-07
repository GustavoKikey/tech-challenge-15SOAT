package br.com.fiap.techchallenge.oficina.domain.relatorio;

/**
 * Resultado do <i>read model</i> "Tempo médio de execução" do Event Storming
 * (PDF DDD, pág. 2). Calculado direto no banco — não materializa OS em memória.
 *
 * @param amostra                       quantidade de OS consideradas (ambas datas preenchidas)
 * @param tempoMedioExecucaoSegundos    média aritmética em segundos; {@code 0} quando amostra == 0
 */
public record TempoMedioExecucao(long amostra, long tempoMedioExecucaoSegundos) {

    public static TempoMedioExecucao vazio() {
        return new TempoMedioExecucao(0L, 0L);
    }
}
