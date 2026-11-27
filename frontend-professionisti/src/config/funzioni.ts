/**
 * Funzioni accese o spente nel rilascio corrente.
 *
 * `AREA_PAZIENTE_ATTIVA`: il primo rilascio è solo per il professionista (wiki, decisioni/0012).
 * Senza area paziente l'invito e lo stato account non servono, quindi sono nascosti: pulsante "Invita",
 * badge e colonna dello stato account, filtri per stato, linguetta "Inviti" in Dashboard, voce "Chat".
 * Il backend resta invariato. Per riaccendere tutto basta mettere `true`.
 */
export const AREA_PAZIENTE_ATTIVA = false
