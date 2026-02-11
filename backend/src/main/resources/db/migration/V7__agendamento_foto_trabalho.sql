-- Foto do trabalho anexada pelo prestador no checkout
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS foto_trabalho_url VARCHAR(512);
