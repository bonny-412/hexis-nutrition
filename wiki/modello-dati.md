---
title: Modello dati
tags: [dati]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-09-01
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md]
---

# Modello dati — hexis-nutrition

Database: PostgreSQL, migrazioni gestite con Flyway (in `backend/`).

## Entità implementate (sotto-progetto "Fondamenta", vedi [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md))

Migrazioni Flyway V1-V7 scritte e committate in `backend/` (`70e2141`), **applicate con successo** al database di test `hexis_test` (PostgreSQL 13.23) il 2026-08-09 — vedi [stato](stato.md):

- **Professionista** (V1): email (univoca), password (hash), nome, cognome. Un account SaaS = un professionista (no team/studio condiviso).
- **Paziente** (V2, esteso in V6, V8, V9): nome, cognome, email, telefono, data di nascita (**obbligatoria da V8**, 2026-09-01), sesso (**obbligatorio da V9**, 2026-09-01, valori M/F/ALTRO), lavoro, tipo di lavoro (`SEDENTARIO`/`POCO_ATTIVO`/`ATTIVO`/`MOLTO_ATTIVO`); collegato a un `Professionista`; stato account (`MAI_INVITATO` / `INVITATO` / `ATTIVO`). Non richiede un account attivo per esistere. Email univoca solo tra account `ATTIVO` (indice parziale), non a livello di anagrafica. L'altezza **non** è più qui: è stata spostata su `Visita` (V5), storicizzata per ogni visita anziché unica per paziente. Nessun campo età: si calcola da `dataNascita` al volo (frontend), mai persistito, per non disallinearsi nel tempo — scelta di Andrea il 2026-09-01.
- **Visita** (V5, estesa in V7, V10): data visita (scelta dal professionista, default oggi), altezza, peso, 11 circonferenze a misura singola (vita, fianchi, addome, braccio rilassato, coscia, polpaccio, collo, torace, braccio contratto, avambraccio, caviglia) + protocollo vita usato (`OMS`/`OMBELICALE`/`ALTRO`, default `OMS`); collegata a un `Paziente`. Solo altezza e peso sono obbligatori. Peso e circonferenze hanno precisione `NUMERIC(6,2)`; altezza resta intera (cm). Dal 2026-09-01 (V10) sostituisce lo schema precedente con coppie dx/sx (coscia, polpaccio, bicipite) e i campi ombelico/petto/spalle, mai popolati in produzione. Valori derivati calcolati e persistiti dal backend al salvataggio: BMI (sempre), WHR (se vita e fianchi presenti), WHtR (se vita presente), MAMC (se braccio rilassato e plica tricipitale presenti, incrocio con `Plicometria`). Creata contestualmente alla prima visita in `POST /pazienti`; visite successive (storico, endpoint dedicato) restano fuori scope — anticipa il sotto-progetto "Monitoraggio". Nessun campo per la misurazione BIA per ora (da definire).
- **Plicometria** (V11, nuova entità, 2026-09-01): 1:1 opzionale con `Visita` (creata solo se il professionista sceglie un protocollo plicometrico). Protocollo (`JACKSON_POLLOCK_3` / `JACKSON_POLLOCK_7` / `DURNIN_WOMERSLEY_4` / `FAULKNER_4` / `SLAUGHTER_PEDIATRICO` / `EVANS_ATLETI`), fino a 9 pliche in mm (già mediate se il professionista ha usato la tripla misurazione), età alla visita (calcolata da `Paziente.dataNascita` + `Visita.dataVisita`, mai un valore manuale), e i valori derivati persistiti al momento del calcolo per riproducibilità storica: somma pliche, densità corporea (nulla per i protocolli a `%BF` diretta), `%BF` (con limite di sicurezza biologico `max(3, %BF)` per M / `max(10, %BF)` per F applicato a tutti i protocolli), massa grassa/magra in kg, FMI, FFMI, versione della formula usata, e — solo per Durnin-Womersley — lo snapshot dei coefficienti `c`/`m` effettivamente applicati (non solo il riferimento). Se `Paziente.sesso = ALTRO`, la creazione è bloccata (400): tutte le equazioni richiedono M/F. Contiene anche `limite_sicurezza_applicato` (booleano, V13, 2026-09-01): `true` quando il `%BF` grezzo calcolato dalla formula era sotto la soglia minima (3% uomo / 10% donna) ed è stato corretto — in quel caso `densita_corporea` resta il valore grezzo non corretto (mai falsificato per farlo tornare con Siri), mentre `percentuale_grasso` è già il valore floorato: il flag segnala esplicitamente quando i due numeri non si riconciliano tra loro via l'equazione di Siri, invece di lasciarlo implicito. Vedi [domande-aperte](domande-aperte.md) per il ragionamento.
- **DurninWomersleyCoefficiente** (V12, tabella di riferimento, 2026-09-01): 10 righe seminate da migrazione (5 fasce d'età × 2 sessi, valori Durnin & Womersley 1974), non modificabile da applicativo. Lookup per (sesso, età) usata solo dal calcolatore Durnin-Womersley.
- **TokenAzione** (V3): token, scadenza (`TIMESTAMPTZ`), usato, tipo (`INVITO` / `RESET_PASSWORD`); collegato a un `Professionista` **oppure** un `Paziente` (mai entrambi, vincolo CHECK).

## Entità previste ma non ancora progettate (sotto-progetti successivi)

- **Piano alimentare**, **Alimento**, **Pasto** — sotto-progetto "Piano alimentare". Piano strutturato (giorni/pasti/alimenti) con calcolo automatico di calorie/macro; alimenti da dataset pubblico importato + alimenti custom per professionista (dataset specifico da scegliere, vedi [domande-aperte](domande-aperte.md)).
- **Misurazione**, **Appuntamento** — sotto-progetto "Monitoraggio". Appuntamenti con flusso di richiesta (paziente) + conferma (professionista); misurazioni inseribili sia dal professionista sia dal paziente.
- **Messaggio** (chat) — sotto-progetto "Chat".

Vedi anche: [domande-aperte](domande-aperte.md), [stato](stato.md) per la roadmap.
