-- =============================================================
-- BC Estoque — schema do agregado Peça (raiz) + Reserva (entidade interna)
-- =============================================================

CREATE TABLE pecas (
    id                UUID            PRIMARY KEY,
    descricao         VARCHAR(200)    NOT NULL,
    valor_unitario    NUMERIC(12, 2)  NOT NULL CHECK (valor_unitario >= 0),
    quantidade_total  INTEGER         NOT NULL CHECK (quantidade_total >= 0),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE reservas (
    id                UUID         PRIMARY KEY,
    peca_id           UUID         NOT NULL REFERENCES pecas(id) ON DELETE CASCADE,
    ordem_servico_id  UUID         NOT NULL,
    quantidade        INTEGER      NOT NULL CHECK (quantidade > 0),
    status            VARCHAR(16)  NOT NULL CHECK (status IN ('ATIVA', 'BAIXADA', 'CANCELADA')),
    criada_em         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_reservas_peca_id ON reservas (peca_id);
CREATE INDEX idx_reservas_os_id   ON reservas (ordem_servico_id);
CREATE INDEX idx_reservas_status  ON reservas (status);
