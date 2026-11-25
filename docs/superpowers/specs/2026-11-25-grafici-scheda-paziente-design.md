# Grafici della scheda paziente — design breve

Data: 2026-11-25. Approvato da Andrea in chat (tabella delle direzioni e proposta rivista con frontend-design). Origine: §4 e §6 punto 2 di [`docs/ui-review-2026-11-25.md`](../../ui-review-2026-11-25.md).

## Perché

- Il Confronto visite mette kg, % e indici sullo stesso grafico a barre: le altezze non si confrontano.
- Le variazioni sono colorate senza tenere conto dell'obiettivo: in tabella sempre ocra (sembrano avvisi), negli indicatori "peso giù = verde" fisso, sbagliato per chi deve aumentare di peso.
- I grafici della Panoramica sono linee senza valori.

## Obiettivi / non obiettivi

- Obiettivi: variazione A→B confrontabile tra parametri; verde/rosso solo quando l'obiettivo indica una direzione; valori leggibili sui grafici.
- Non obiettivi: backend (l'obiettivo è già su ogni visita), BIA, banda dell'obiettivo/peso target, nuovi parametri.
- Anti-obiettivo: grafici rumorosi (etichette sovrapposte, colori decorativi, gradienti).

## Approcci considerati

1. **Barre divergenti in HTML/CSS, una riga per parametro (scelto)**: lunghezza = variazione % rispetto ad A, etichetta = variazione reale con unità. Accessibile, responsive, nessun tooltip di Unovis da stilare a mano.
2. Barre divergenti con `VisStackedBar` di Unovis: assi e tooltip da personalizzare, etichette lunghe delle circonferenze difficili da gestire da mobile.
3. Piccoli grafici per unità (kg, %, cm, indici): più grafici da leggere, non risponde a "cosa è migliorato?".

## Regole delle direzioni (approvate)

| Obiettivo | Verde se… | Neutro |
|---|---|---|
| Dimagrimento | peso, BMI, % grasso, massa grassa, FMI, vita, addome ↓; massa magra, FFMI ↑ | resto |
| Aumento peso | peso, BMI, massa magra, FFMI ↑ | resto |
| Ipertrofia | massa magra, FFMI ↑; % grasso ↓ | resto |
| Ricomposizione, Preparazione sportiva | % grasso, massa grassa, FMI ↓; massa magra, FFMI ↑ | resto |
| Mantenimento, Educativo, Patologia clinica, Gravidanza/allattamento | — | tutto |

Verso opposto = rosso (`--danger`), verso giusto = verde (`--green`), neutro o variazione nulla = `--fg3`. Nel Confronto vale l'obiettivo della visita B, negli indicatori quello dell'ultima visita. FMI segue la massa grassa e FFMI la massa magra; "addome" segue "vita" (scelta di Claude, coerente con la tabella).

## Design

- `utils/obiettivo.ts`: `direzioneDesiderata(obiettivo, parametro)` e `esitoVariazione(obiettivo, parametro, delta)` → `'positivo' | 'negativo' | 'neutro'`, più `CLASSE_ESITO`.
- Indicatori della scheda paziente: colore della riga "vs prec." da `esitoVariazione` con l'obiettivo dell'ultima visita; frecce ↑/↓ invariate.
- Confronto visite: un'unica card "Variazioni" con `VariazioniChart.vue` al posto dei due `ConfrontoBarChart` (rimosso con il suo spec, già rotto). Gruppi Peso / Plicometria / Circonferenze; per riga etichetta, barra divergente attorno allo zero (scala comune = massima variazione % assoluta), variazione reale con unità. Righe con un solo lato valorizzato escluse. Tabella sotto invariata, con la colonna Variazione colorata per esito.
- Panoramica: su `AndamentoChart` etichetta su ogni punto (`--fg3`) e ultimo punto in evidenza (`--fg`, grassetto con unità); su `ComposizioneChart` etichetta solo sull'ultimo punto di ogni serie, con il valore reale.

## Lasciato a chi implementa

Spaziature, gestione delle etichette sovrapposte, adeguamento dei test esistenti. Nessun test nuovo di default.

## Cambio di rotta durante l'implementazione (richiesta di Andrea)

Il grafico a barre divergenti "Variazioni" è stato provato e **scartato**: Andrea preferisce il confronto A/B a barre affiancate, senza colori legati all'obiettivo. Restano invece i colori secondo l'obiettivo nella **tabella** del Confronto e negli **indicatori** in alto.

`ConfrontoBarChart.vue` è stato ridisegnato in HTML/CSS (stesse props): visita A in tinta chiara del colore di B (`color-mix` al 32% con `--surf`), B pieno; valore su ogni barra (B in grassetto); ogni parametro scalato sul massimo della propria coppia; verticale per la plicometria (unità solo nell'etichetta), orizzontale per le circonferenze (A sopra, B sotto, valore con unità a fine barra). `ConfrontoBarChart.spec.ts` riscritto per la versione HTML (9 test, richiesto da Andrea).
