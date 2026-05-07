package br.com.fiap.techchallenge.oficina.infrastructure.persistence.peca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class ReservaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id", nullable = false)
    public PecaJpaEntity peca;

    @Column(name = "ordem_servico_id", nullable = false)
    public UUID ordemServicoId;

    @Column(name = "quantidade", nullable = false)
    public int quantidade;

    @Column(name = "status", nullable = false, length = 16)
    public String status;

    @Column(name = "criada_em", nullable = false)
    public OffsetDateTime criadaEm;
}
