---
title: Glossario
tags: [glossario]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-08-08
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md]
---

# Glossario — hexis-nutrition

- **Professionista**: utente dietista/nutrizionista, opera in `frontend-professionisti`; gestisce pazienti, piani alimentari e monitoraggio. Un account SaaS = un professionista (nessun team/studio condiviso).
- **Paziente**: persona seguita da un professionista, opera in `frontend-cliente`. Ha sempre un'anagrafica creata dal professionista; l'account di accesso è opzionale e attivato solo tramite invito.
- **Cliente**: termine *business*, non di dominio — indica il professionista in quanto abbonato pagante del SaaS. **Correzione rispetto alla versione precedente di questa pagina**: "paziente" e "cliente" non sono sinonimi intercambiabili, sono due concetti distinti riferiti a persone diverse. Il nome della cartella `frontend-cliente` resta invariato ma è fuorviante rispetto a questa distinzione (l'app serve i pazienti, non i "clienti" in questo senso) — nota per chi legge il codice.
- **Piano alimentare**: entità centrale assegnata dal professionista a un paziente; strutturato in giorni/pasti/alimenti con calcolo automatico di calorie/macro (dettagli in [modello-dati](modello-dati.md), sotto-progetto "Piano alimentare").
- **Invito**: meccanismo a token+email con cui il professionista attiva l'accesso di un paziente all'app.
- **Fondamenta**: primo sotto-progetto della roadmap di prodotto — autenticazione, anagrafica paziente, invito/onboarding. Vedi [stato](stato.md).
