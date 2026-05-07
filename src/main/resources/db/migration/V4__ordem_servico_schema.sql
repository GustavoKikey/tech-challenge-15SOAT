-- =============================================================
-- BC Atendimento — schema do agregado Ordem de Serviço
-- =============================================================

CREATE TABLE ordens_servico (
    id                       UUID            PRIMARY KEY,
    cliente_id               UUID            NOT NULL REFERENCES clientes(id),
    veiculo_id               UUID            NOT NULL REFERENCES veiculos(id),
    status                   VARCHAR(32)     NOT NULL CHECK (status IN
        ('RECEBIDA','EM_DIAGNOSTICO','AGUARDANDO_APROVACAO','EM_EXECUCAO','FINALIZADA','ENTREGUE')),
    valor_total              NUMERIC(12, 2),
    criada_em                TIMESTAMPTZ     NOT NULL,
    diagnostico_iniciado_em  TIMESTAMPTZ,
    execucao_iniciada_em     TIMESTAMPTZ,
    finalizada_em            TIMESTAMPTZ,
    entregue_em              TIMESTAMPTZ,
    orcamento_gerado_em      TIMESTAMPTZ,
    orcamento_aprovado_em    TIMESTAMPTZ
);

CREATE INDEX idx_ordens_servico_status     ON ordens_servico (status);
CREATE INDEX idx_ordens_servico_cliente_id ON ordens_servico (cliente_id);
CREATE INDEX idx_ordens_servico_veiculo_id ON ordens_servico (veiculo_id);

CREATE TABLE os_itens_servico (
    id              UUID            PRIMARY KEY,
    os_id           UUID            NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    servico_id      UUID            NOT NULL REFERENCES servicos(id),
    valor_cobrado   NUMERIC(12, 2)  NOT NULL CHECK (valor_cobrado >= 0)
);

CREATE INDEX idx_os_itens_servico_os_id ON os_itens_servico (os_id);

CREATE TABLE os_itens_peca (
    id              UUID            PRIMARY KEY,
    os_id           UUID            NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    peca_id         UUID            NOT NULL REFERENCES pecas(id),
    quantidade      INTEGER         NOT NULL CHECK (quantidade > 0),
    valor_unitario  NUMERIC(12, 2)  NOT NULL CHECK (valor_unitario >= 0),
    reserva_id      UUID
);

CREATE INDEX idx_os_itens_peca_os_id ON os_itens_peca (os_id);
