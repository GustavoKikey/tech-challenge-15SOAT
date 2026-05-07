package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Value Object — orçamento gerado para uma OS.
 *
 * <p>Imutável após criação, exceto pela marcação de aprovação ({@link #aprovar()}).
 * Vive como atributo do agregado {@link OrdemServico}; não é raiz própria.
 */
public final class Orcamento {

    private final Dinheiro valorTotal;
    private final OffsetDateTime geradoEm;
    private OffsetDateTime aprovadoEm;

    private Orcamento(Dinheiro valorTotal, OffsetDateTime geradoEm, OffsetDateTime aprovadoEm) {
        this.valorTotal = Objects.requireNonNull(valorTotal, "valorTotal");
        this.geradoEm = Objects.requireNonNull(geradoEm, "geradoEm");
        this.aprovadoEm = aprovadoEm;
    }

    public static Orcamento gerar(Dinheiro valorTotal) {
        return new Orcamento(valorTotal, OffsetDateTime.now(), null);
    }

    public static Orcamento reconstituir(Dinheiro valorTotal, OffsetDateTime geradoEm,
                                         OffsetDateTime aprovadoEm) {
        return new Orcamento(valorTotal, geradoEm, aprovadoEm);
    }

    void aprovar() {
        this.aprovadoEm = OffsetDateTime.now();
    }

    public Dinheiro valorTotal()        { return valorTotal; }
    public OffsetDateTime geradoEm()    { return geradoEm; }
    public OffsetDateTime aprovadoEm()  { return aprovadoEm; }
    public boolean aprovado()           { return aprovadoEm != null; }
}
