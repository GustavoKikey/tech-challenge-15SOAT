-- =============================================================
-- Fase 3 — status do cliente
--
-- A Function Serverless de autenticação precisa "consultar a existência
-- E O STATUS do cliente na base de dados" antes de emitir o token. A tabela
-- criada na fase 1 (V2) só respondia à existência: não havia como recusar
-- login de um cliente desativado.
--
-- Modelado como BOOLEAN, não VARCHAR: hoje só existem dois estados de fato
-- (pode ou não pode autenticar). Um VARCHAR sem CHECK convidaria a valores
-- livres divergindo entre a Lambda e a aplicação; quando surgir um terceiro
-- estado real (ex.: BLOQUEADO por inadimplência), a migração para enum é
-- trivial e explícita.
-- =============================================================

ALTER TABLE clientes
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN clientes.ativo IS
    'Cliente habilitado a autenticar e abrir OS. FALSE bloqueia a emissão de token pela Lambda.';

-- A Lambda busca por documento e filtra por ativo em toda autenticação. O
-- índice de V2 (idx_clientes_documento) já cobre a busca, mas o índice
-- parcial abaixo mantém a leitura eficiente conforme a base cresce e a
-- proporção de inativos aumenta.
CREATE INDEX idx_clientes_documento_ativo
    ON clientes (documento)
    WHERE ativo;
