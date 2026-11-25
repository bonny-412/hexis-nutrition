# Barra azioni fissa nei form lunghi — design breve

Data: 2026-11-25. Approvato da Andrea in chat. Origine: §4 e §6 punto 1 di [`docs/ui-review-2026-11-25.md`](../../ui-review-2026-11-25.md).

## Perché

"Nuovo paziente", "Nuova visita" ed editor del piano sono lunghi 2-3 schermate e hanno "Annulla"/"Salva" solo in fondo. Il salvataggio deve essere sempre a portata e la pagina deve dire se ci sono modifiche non salvate.

## Obiettivi / non obiettivi

- Obiettivo: una fascia in fondo all'area di contenuto, sempre visibile mentre si scorre, con stato + "Annulla" + "Salva", identica nei tre form.
- Non obiettivi: cambiare la logica di validazione, di salvataggio o di conferma "Uscire senza salvare?"; toccare altri form (dialog, agenda).
- Anti-obiettivo: la barra non deve coprire l'ultimo campo né i pulsanti dei menu, e da mobile non deve mangiare troppo spazio insieme al riepilogo macro fisso del piano.

## Approcci considerati

1. **`sticky bottom-0` dentro il `<main>` che scorre (scelto)**: nessun calcolo della larghezza della sidebar, la barra resta nel flusso e a fine pagina si appoggia sotto l'ultimo contenuto.
2. `position: fixed` in fondo alla finestra: va allineata a mano alla sidebar e copre il contenuto senza spazio riservato.
3. Barra che compare solo dopo la prima modifica, o pulsanti nell'intestazione: scartati, il primo lascia la pagina senza "Annulla" visibile, il secondo allontana "Salva" dall'ultimo campo compilato.

## Design

- Componente condiviso `components/form/BarraAzioniForm.vue`. Props: `modificato` (bool), `inCorso` (bool), `etichettaSalva` (stringa), `salvaDisabilitato` (bool), `tipoSalva` (`submit` | `button`), `dataTestAnnulla`. Emette `annulla` e `salva` (solo quando `tipoSalva` è `button`). Slot opzionale non necessario.
- Aspetto: `sticky bottom-0`, sfondo `--surf` con bordo superiore `--bd`, estesa ai bordi orizzontali del `<main>` (margini negativi pari al padding), `print:hidden`, `z-20` sopra le card.
- Sinistra: "Modifiche non salvate" in `--fg3` con un punto lime (`--lime`, segnale come da ADR 0009) quando `modificato`; altrimenti vuoto. Destra: "Annulla" (`neutral`, icona `X`) e "Salva" (primario, icona `Save`), etichette e `data-test` di oggi.
- Mobile (< 640 px): indicatore sopra in piccolo, i due pulsanti a metà ciascuno.
- Collegamenti: nuovo paziente → `formValorizzata()`; visita → `datiVisitaForm.modificato()`; piano → `modificato` (non mostrata in sola lettura).

## Modifica dopo la prima prova in Chrome (richiesta di Andrea)

La fascia a tutta larghezza con bordo superiore è diventata una **pillola flottante con angoli arrotondati** (`rounded-2xl`, bordo, `shadow-lg`, `sticky bottom-0` staccata di 16 px dal fondo grazie al padding del `<main>`), larga quanto il contenuto e allineata a destra su desktop, a tutta larghezza da mobile. Pulsanti `rounded-xl` per stare dentro la curva.

## Lasciato a chi implementa

Spaziatura esatta, eventuale ombra sottile verso l'alto solo quando la barra si stacca dal fondo pagina (facoltativa), adeguamento dei test esistenti che cercano i pulsanti. Nessun test nuovo di default (li decide Andrea).
