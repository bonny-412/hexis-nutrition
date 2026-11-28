// Impostazioni comuni a tutti i test (vitest, jsdom).

// jsdom non implementa scrollIntoView: scorriAlPrimoErrore (utils/form.ts) lo chiama dopo un nextTick, e nei test
// dei form non validi l'eccezione arrivava a test già finito, come errore non gestito che faceva uscire vitest
// con codice 1 anche con tutti i test verdi (trovato il 28/11/2026).
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {}
}
