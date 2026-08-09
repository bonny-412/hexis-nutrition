# Fonte — Migrazione a un repo git unico per progetto (9 agosto 2026)

Tipo: conversazione con Andrea (sessione Claude Code aperta su `progetti/hexis-nutrition/`).

## Richiesta iniziale

> Voglio mettere su github l'intero progetto, ma per ora su github sono stati messi 4 progetti singoli "wiki", "frontend-professionisti", "frontend-cliente", "backend". Invece io vorrei mettere direttamente su gitub il progetto "hexis-nutrition"

## Situazione rilevata prima di intervenire

- Repo radice `workspace2.0` già su GitHub (`bonny-412/workspace2.0`), tracciava solo lo scheletro del workspace più `progetti/hexis-nutrition/CLAUDE.md`. Il suo `.gitignore` escludeva `progetti/*/frontend*/`, `progetti/*/backend*/`, `progetti/*/wiki/`.
- Quattro repo indipendenti su GitHub, tutti allineati con `origin`, un solo branch (`master`), nessuno stash, niente di non pushato:
  - `backend` — 3 commit (`41100ca` Setup iniziale, `4e21133` "b", `1f40db9` "b"), codice reale di "Fondamenta";
  - `wiki` — 2 commit (`0f6082d` Ingest iniziale, `7919076` "v");
  - `frontend-professionisti` — 1 commit, solo `CLAUDE.md`;
  - `frontend-cliente` — 1 commit, solo `CLAUDE.md`.
- `gh` CLI non installato sulla macchina.

## Decisioni prese durante la conversazione

Tre domande poste ad Andrea, con le sue risposte:

1. **Storia git** → *ripartire pulito*: un unico commit iniziale, senza `git subtree`. Motivo indicato nell'opzione scelta: 7 commit totali con messaggi poco informativi ("a", "b", "v"), valore storico basso rispetto alla complessità dei merge subtree.
2. **Vecchi repo GitHub** → *li archivia Andrea dopo* aver verificato che il monorepo è a posto (fatto in giornata).
3. **Chi esegue i commit** → preparazione fatta da Claude fino allo staging, commit e push eseguiti manualmente da Andrea (regola del workspace: i commit li fa sempre e solo Andrea).

Successiva conferma esplicita di Andrea sulla portata della regola:

> Si, salva come decisione che ogni progetto sotto progetti/ sarà una repo di git unica a prescindere da quello che c'è dentro

## Operazioni eseguite

- I quattro `.git` delle sottocartelle spostati in backup (scratchpad di sessione), non cancellati.
- `git init -b master` in `progetti/hexis-nutrition/`, nuovo `.gitignore` di root (`.claude/settings.local.json`, `.superpowers/`, `node_modules/`, `dist/`, `.env.local`, IDE, OS). `backend/.gitignore` lasciato dov'era: continua a valere come ignore di sottocartella.
- 69 file in staging; `backend/target/` e `backend/.superpowers/` correttamente esclusi.
- `workspace2.0/.gitignore`: le tre righe specifiche sostituite da `progetti/*/`.
- `progetti/hexis-nutrition/CLAUDE.md` rimosso dal tracking del repo radice (`git rm --cached`) — altrimenti sarebbe stato tracciato da due repo insieme.
- `CLAUDE.md` del workspace aggiornato (sezioni *Struttura*, *Nuovo progetto*, *Come aprire una sessione*) e `CLAUDE.md` di progetto (sezione *Repo* → *Struttura*).
- Remote impostato su `https://github.com/bonny-412/hexis-nutrition.git`.

## Esito verificato

- Monorepo: commit `70e2141` ("Primo Commit"), locale e `origin/master` sullo stesso SHA.
- Repo workspace: commit `2e0ebf7` ("Update struttura git"), locale e remoto allineati.
- I quattro repo vecchi archiviati su GitHub da Andrea.
