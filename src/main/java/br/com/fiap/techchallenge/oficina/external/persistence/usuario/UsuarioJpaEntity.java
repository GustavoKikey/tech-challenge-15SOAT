package br.com.fiap.techchallenge.oficina.external.persistence.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class UsuarioJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "username", nullable = false, length = 80, unique = true)
    public String username;

    @Column(name = "senha_hash", nullable = false, length = 100)
    public String senhaHash;

    @Column(name = "role", nullable = false, length = 16)
    public String role;

    @Column(name = "ativo", nullable = false)
    public boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    public OffsetDateTime criadoEm;

    @PrePersist
    void onCreate() {
        if (criadoEm == null) criadoEm = OffsetDateTime.now();
    }
}
