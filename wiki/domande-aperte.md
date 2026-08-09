---
title: Domande aperte
tags: [domande-aperte]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-08-08
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md]
---

# Domande aperte — hexis-nutrition

- Perché Spring Boot (vs alternative Node/altro)? Motivazione non raccolta, potrebbe non servire documentarla se ovvia per Andrea.
- Quale dataset pubblico di alimenti usare per il catalogo base (es. CREA, USDA, altro)? Da decidere nel sotto-progetto "Piano alimentare".
- Deploy target (cloud, on-prem, provider) non ancora discusso.
- Modello dati per i sotto-progetti "Piano alimentare", "Monitoraggio" e "Chat" non ancora dettagliato (solo "Fondamenta" è stato progettato, vedi [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md)).

## Risolte

- ~~Perché due frontend Vue separati invece di una singola app con routing/permessi per ruolo?~~ Risolto in [architettura](architettura.md).
- ~~Terminologia "paziente" vs "cliente"~~: sono due concetti distinti, non sinonimi. Risolto in [glossario](glossario.md).
