package br.com.fiap.techchallenge.oficina.infrastructure.persistence.veiculo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "veiculos")
public class VeiculoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "placa", nullable = false, length = 7, unique = true)
    public String placa;

    @Column(name = "marca", nullable = false, length = 80)
    public String marca;

    @Column(name = "modelo", nullable = false, length = 120)
    public String modelo;

    @Column(name = "ano", nullable = false)
    public int ano;

    @Column(name = "cliente_id", nullable = false)
    public UUID clienteId;

    @Column(name = "created_at", nullable = false, updatable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
