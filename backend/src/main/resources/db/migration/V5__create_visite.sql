CREATE TABLE visite (
    id UUID PRIMARY KEY,
    paziente_id UUID NOT NULL REFERENCES pazienti(id),
    data_visita DATE NOT NULL DEFAULT CURRENT_DATE,
    altezza_cm INTEGER NOT NULL,
    peso_kg NUMERIC(5,1) NOT NULL,
    circonferenza_vita_cm NUMERIC(5,1),
    circonferenza_ombelico_cm NUMERIC(5,1),
    circonferenza_fianchi_cm NUMERIC(5,1),
    circonferenza_petto_cm NUMERIC(5,1),
    circonferenza_coscia_dx_cm NUMERIC(5,1),
    circonferenza_coscia_sx_cm NUMERIC(5,1),
    circonferenza_polpaccio_dx_cm NUMERIC(5,1),
    circonferenza_polpaccio_sx_cm NUMERIC(5,1),
    larghezza_spalle_cm NUMERIC(5,1),
    circonferenza_spalle_cm NUMERIC(5,1),
    circonferenza_bicipite_dx_cm NUMERIC(5,1),
    circonferenza_bicipite_sx_cm NUMERIC(5,1),
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_visite_paziente_id ON visite(paziente_id);
