-- Collaudo 27/11/2026, bug 21: al massimo un piano ATTIVO per paziente, garantito dal database
-- anche quando due attivazioni arrivano nello stesso istante.
-- Se per qualche paziente ci sono già più piani attivi, resta attivo il più recente
-- (data di inizio, poi creazione) e gli altri diventano TERMINATO.
UPDATE piani_alimentari p
SET stato = 'TERMINATO'
WHERE p.stato = 'ATTIVO'
  AND EXISTS (
    SELECT 1 FROM piani_alimentari altro
    WHERE altro.paziente_id = p.paziente_id
      AND altro.stato = 'ATTIVO'
      AND altro.id <> p.id
      AND (altro.data_inizio, altro.creato_il) > (p.data_inizio, p.creato_il)
  );

CREATE UNIQUE INDEX uq_piani_alimentari_un_attivo_per_paziente
    ON piani_alimentari(paziente_id)
    WHERE stato = 'ATTIVO';

-- Bug 18: versione per il blocco ottimistico, così un salvataggio basato su una copia
-- vecchia del piano (un'altra scheda) viene respinto invece di sovrascrivere.
ALTER TABLE piani_alimentari
    ADD COLUMN versione BIGINT NOT NULL DEFAULT 0;
