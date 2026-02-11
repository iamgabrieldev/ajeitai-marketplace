-- Integração AbacatePay: armazenar id da cobrança
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS billing_id VARCHAR(100);
