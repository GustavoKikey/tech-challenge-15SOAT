-- =============================================================
-- BC Atendimento — schema dos agregados Cliente, Veiculo, Servico
-- =============================================================

CREATE TABLE clientes (
    id          UUID         PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    documento   VARCHAR(14)  NOT NULL UNIQUE,
    email       VARCHAR(150),
    telefone    VARCHAR(30),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_clientes_documento ON clientes (documento);

CREATE TABLE veiculos (
    id          UUID         PRIMARY KEY,
    placa       VARCHAR(7)   NOT NULL UNIQUE,
    marca       VARCHAR(80)  NOT NULL,
    modelo      VARCHAR(120) NOT NULL,
    ano         INTEGER      NOT NULL,
    cliente_id  UUID         NOT NULL REFERENCES clientes(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_veiculos_cliente_id ON veiculos (cliente_id);

CREATE TABLE servicos (
    id          UUID            PRIMARY KEY,
    descricao   VARCHAR(200)    NOT NULL,
    valor_base  NUMERIC(12, 2)  NOT NULL CHECK (valor_base >= 0),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);
