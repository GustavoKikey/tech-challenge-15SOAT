-- =============================================================
-- Fase 2 — recusa de orçamento: status CANCELADA + data de cancelamento
-- =============================================================

ALTER TABLE ordens_servico ADD COLUMN cancelada_em TIMESTAMPTZ;

ALTER TABLE ordens_servico DROP CONSTRAINT ordens_servico_status_check;
ALTER TABLE ordens_servico ADD CONSTRAINT ordens_servico_status_check CHECK (status IN
    ('RECEBIDA','EM_DIAGNOSTICO','AGUARDANDO_APROVACAO','EM_EXECUCAO','FINALIZADA','ENTREGUE','CANCELADA'));
