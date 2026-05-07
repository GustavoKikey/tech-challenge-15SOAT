package br.com.fiap.techchallenge.oficina.domain.estoque.peca;

import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Agregado raiz <b>Peça</b> (BC Estoque).
 *
 * <p>Encapsula descrição, valor unitário, quantidade total em estoque e a coleção de
 * {@link Reserva}s vinculadas a Ordens de Serviço. Toda invariante (saldo &gt;= 0,
 * reserva nunca excede saldo disponível, transições de status da reserva) é checada
 * aqui dentro: quem está fora <i>só</i> fala com a raiz.
 *
 * <p><b>Saldo disponível</b> = {@code quantidadeTotal} − Σ reservas com status ATIVA.
 */
public class Peca {

    private final PecaId id;
    private String descricao;
    private Dinheiro valorUnitario;
    private int quantidadeTotal;
    private final List<Reserva> reservas;

    private Peca(PecaId id, String descricao, Dinheiro valorUnitario,
                 int quantidadeTotal, List<Reserva> reservas) {
        this.id = Objects.requireNonNull(id, "id");
        this.descricao = exigirDescricao(descricao);
        this.valorUnitario = Objects.requireNonNull(valorUnitario, "valorUnitario");
        if (quantidadeTotal < 0) {
            throw new IllegalArgumentException("Quantidade total não pode ser negativa: " + quantidadeTotal);
        }
        this.quantidadeTotal = quantidadeTotal;
        this.reservas = new ArrayList<>(reservas);
    }

    public static Peca novo(String descricao, Dinheiro valorUnitario, int quantidadeInicial) {
        return new Peca(PecaId.novo(), descricao, valorUnitario, quantidadeInicial, List.of());
    }

    public static Peca novo(String descricao, Dinheiro valorUnitario) {
        return novo(descricao, valorUnitario, 0);
    }

    public static Peca reconstituir(PecaId id, String descricao, Dinheiro valorUnitario,
                                    int quantidadeTotal, List<Reserva> reservas) {
        return new Peca(id, descricao, valorUnitario, quantidadeTotal, reservas);
    }

    /** Atualiza dados de catálogo (não mexe em saldo nem em reservas). */
    public void alterar(String descricao, Dinheiro valorUnitario) {
        this.descricao = exigirDescricao(descricao);
        this.valorUnitario = Objects.requireNonNull(valorUnitario, "valorUnitario");
    }

    /** Adiciona saldo (compra, devolução). Quantidade deve ser estritamente positiva. */
    public void adicionarSaldo(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade a adicionar deve ser positiva: " + quantidade);
        }
        this.quantidadeTotal += quantidade;
    }

    public int saldoDisponivel() {
        int reservado = reservas.stream()
                .filter(Reserva::ativa)
                .mapToInt(Reserva::quantidade)
                .sum();
        return quantidadeTotal - reservado;
    }

    /**
     * Cria uma nova reserva ATIVA vinculada à OS. Acionada pela política
     * "Reservar peça" do Event Storming, no momento da geração do orçamento.
     *
     * @throws EstoqueInsuficienteException se {@code quantidade > saldoDisponivel()}
     */
    public Reserva reservar(OrdemServicoId ordemServicoId, int quantidade) {
        Objects.requireNonNull(ordemServicoId, "ordemServicoId");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade a reservar deve ser positiva: " + quantidade);
        }
        int disponivel = saldoDisponivel();
        if (quantidade > disponivel) {
            throw new EstoqueInsuficienteException(id, quantidade, disponivel);
        }
        Reserva nova = Reserva.nova(ordemServicoId, quantidade);
        reservas.add(nova);
        return nova;
    }

    /**
     * Marca a reserva como BAIXADA e decrementa o total. Acionada pela política
     * "Efetivar baixa" do Event Storming, após aprovação do orçamento.
     */
    public void baixar(ReservaId reservaId) {
        Reserva reserva = exigirReserva(reservaId);
        reserva.marcarBaixada();
        this.quantidadeTotal -= reserva.quantidade();
    }

    /**
     * Cancela uma reserva ATIVA, devolvendo o saldo. Não é endpoint nem use case
     * isolado: serve para rollback transacional do fluxo de geração de orçamento
     * se uma reserva falhar no meio, as anteriores precisam voltar.
     */
    public void cancelarReserva(ReservaId reservaId) {
        exigirReserva(reservaId).cancelar();
    }

    private Reserva exigirReserva(ReservaId reservaId) {
        Objects.requireNonNull(reservaId, "reservaId");
        return reservas.stream()
                .filter(r -> r.id().equals(reservaId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Reserva " + reservaId + " não encontrada na peça " + id));
    }

    public boolean possuiReservasAtivas() {
        return reservas.stream().anyMatch(Reserva::ativa);
    }

    private static String exigirDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição da peça é obrigatória");
        }
        return descricao.trim();
    }

    public PecaId id()                  { return id; }
    public String descricao()           { return descricao; }
    public Dinheiro valorUnitario()     { return valorUnitario; }
    public int quantidadeTotal()        { return quantidadeTotal; }
    public List<Reserva> reservas()     { return Collections.unmodifiableList(reservas); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peca that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
