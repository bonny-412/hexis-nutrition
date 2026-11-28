/**
 * Chiave per l'header Idempotency-Key di una creazione (bug S4 della verifica 3): la stessa chiave in un nuovo
 * tentativo dice al backend che è la stessa richiesta, e lui restituisce la risposta della prima invece di creare
 * un doppione. Si tiene la stessa chiave finché la creazione non riesce, poi se ne prende una nuova.
 *
 * `crypto.randomUUID` esiste solo in un contesto sicuro (https o localhost): altrove si ripiega su
 * `getRandomValues`, che c'è sempre.
 */
export function nuovaChiaveIdempotenza(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  const byte = crypto.getRandomValues(new Uint8Array(16))
  return Array.from(byte, (b) => b.toString(16).padStart(2, '0')).join('')
}
