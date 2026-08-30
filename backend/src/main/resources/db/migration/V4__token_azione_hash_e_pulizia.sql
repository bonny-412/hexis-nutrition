ALTER TABLE token_azione RENAME COLUMN token TO token_hash;
ALTER TABLE token_azione DROP COLUMN usato;
