ALTER TABLE visite DROP COLUMN circonferenza_ombelico_cm;
ALTER TABLE visite DROP COLUMN circonferenza_petto_cm;
ALTER TABLE visite DROP COLUMN circonferenza_coscia_dx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_coscia_sx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_polpaccio_dx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_polpaccio_sx_cm;
ALTER TABLE visite DROP COLUMN larghezza_spalle_cm;
ALTER TABLE visite DROP COLUMN circonferenza_spalle_cm;
ALTER TABLE visite DROP COLUMN circonferenza_bicipite_dx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_bicipite_sx_cm;

ALTER TABLE visite ADD COLUMN circonferenza_addome_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_braccio_rilassato_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_coscia_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_polpaccio_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_collo_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_torace_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_braccio_contratto_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_avambraccio_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_caviglia_cm NUMERIC(6,2);

ALTER TABLE visite ADD COLUMN protocollo_vita VARCHAR(20) NOT NULL DEFAULT 'OMS';
ALTER TABLE visite ADD COLUMN bmi NUMERIC(5,2);
ALTER TABLE visite ADD COLUMN whr NUMERIC(4,2);
ALTER TABLE visite ADD COLUMN whtr NUMERIC(4,2);
ALTER TABLE visite ADD COLUMN mamc_cm NUMERIC(5,2);
