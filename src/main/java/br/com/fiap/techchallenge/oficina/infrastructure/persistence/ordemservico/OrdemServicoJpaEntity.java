package br.com.fiap.techchallenge.oficina.infrastructure.persistence.ordemservico;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico")
public class OrdemServicoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public UUID id;

    @Column(name = "cliente_id", nullable = false)
    public UUID clienteId;

    @Column(name = "veiculo_id", nullable = false)
    public UUID veiculoId;

    @Column(name = "status", nullable = false, length = 32)
    public String status;

    @Column(name = "valor_total", precision = 12, scale = 2)
    public BigDecimal valorTotal;

    @Column(name = "criada_em", nullable = false)
    public OffsetDateTime criadaEm;

    @Column(name = "diagnostico_iniciado_em")
    public OffsetDateTime diagnosticoIniciadoEm;

    @Column(name = "execucao_iniciada_em")
    public OffsetDateTime execucaoIniciadaEm;

    @Column(name = "finalizada_em")
    public OffsetDateTime finalizadaEm;

    @Column(name = "entregue_em")
    public OffsetDateTime entregueEm;

    @Column(name = "orcamento_gerado_em")
    public OffsetDateTime orcamentoGeradoEm;

    @Column(name = "orcamento_aprovado_em")
    public OffsetDateTime orcamentoAprovadoEm;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    public List<OsItemServicoJpaEntity> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    public List<OsItemPecaJpaEntity> itensPeca = new ArrayList<>();
}
