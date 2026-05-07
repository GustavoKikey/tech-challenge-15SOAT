package br.com.fiap.techchallenge.oficina.infrastructure.persistence.ordemservico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "os_itens_peca")
public class OsItemPecaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false)
    public OrdemServicoJpaEntity ordemServico;

    @Column(name = "peca_id", nullable = false)
    public UUID pecaId;

    @Column(name = "quantidade", nullable = false)
    public int quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    public BigDecimal valorUnitario;

    @Column(name = "reserva_id")
    public UUID reservaId;
}
