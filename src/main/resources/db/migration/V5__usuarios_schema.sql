-- =============================================================
-- Segurança: usuários administrativos (Atendente / Mecânico / Administrador)
-- =============================================================

CREATE TABLE usuarios (
    id          UUID            PRIMARY KEY,
    username    VARCHAR(80)     NOT NULL UNIQUE,
    senha_hash  VARCHAR(100)    NOT NULL,
    role        VARCHAR(16)     NOT NULL CHECK (role IN ('ATENDENTE','MECANICO','ADMINISTRADOR')),
    ativo       BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuarios_username ON usuarios (username);

-- O usuário 'admin' inicial é provisionado pelo AdminBootstrap no startup da app
-- (BCrypt requer cálculo runtime do hash). Em produção, definir senha via
-- variável de ambiente ADMIN_PASSWORD e rotacionar imediatamente após o primeiro login.
