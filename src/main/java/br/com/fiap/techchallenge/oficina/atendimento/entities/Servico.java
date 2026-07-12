package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.Dinheiro;

import java.util.Objects;

/**
 * Agregado raiz <b>Serviço</b> — item de catálogo de mão-de-obra.
 *
 * <p>Identidade: {@link ServicoId}. Descrição e valor base podem ser alterados.
 */
public class Servico {

    private final ServicoId id;
    private String descricao;
    private Dinheiro valorBase;

    private Servico(ServicoId id, String descricao, Dinheiro valorBase) {
        this.id = Objects.requireNonNull(id, "id");
        this.descricao = exigirDescricao(descricao);
        this.valorBase = Objects.requireNonNull(valorBase, "valorBase");
    }

    public static Servico novo(String descricao, Dinheiro valorBase) {
        return new Servico(ServicoId.novo(), descricao, valorBase);
    }

    public static Servico reconstituir(ServicoId id, String descricao, Dinheiro valorBase) {
        return new Servico(id, descricao, valorBase);
    }

    public void alterar(String descricao, Dinheiro valorBase) {
        this.descricao = exigirDescricao(descricao);
        this.valorBase = Objects.requireNonNull(valorBase, "valorBase");
    }

    private static String exigirDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição do serviço é obrigatória");
        }
        return descricao.trim();
    }

    public ServicoId id()           { return id; }
    public String descricao()       { return descricao; }
    public Dinheiro valorBase()     { return valorBase; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Servico that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
