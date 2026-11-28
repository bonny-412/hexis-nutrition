import { ApiError, ErroreRete, ErroreTimeout } from '@/api/client'

/**
 * Messaggio del toast quando un salvataggio non riesce (bug S7 della verifica 3): un problema di rete o del server
 * non è un errore nei dati, e dirlo evita di cercare un campo sbagliato che non c'è. I dati restano nel form in
 * ogni caso, quindi i messaggi di rete invitano solo a riprovare.
 *
 * `messaggioDati` è il messaggio della pagina per una richiesta rifiutata (400, 404, 409…).
 */
export function messaggioErroreSalvataggio(errore: unknown, messaggioDati: string): string {
  if (errore instanceof ErroreTimeout) {
    return 'Il server non ha risposto in tempo. Le modifiche sono ancora qui: riprova tra poco.'
  }
  if (errore instanceof ErroreRete) {
    return 'Connessione al server non riuscita. Le modifiche sono ancora qui: riprova tra poco.'
  }
  if (errore instanceof ApiError) {
    // La finestra "Sessione scaduta" spiega già cosa fare.
    if (errore.status === 401) return 'Sessione scaduta: accedi di nuovo, poi salva.'
    if (errore.status === 409 && errore.message.includes('RICHIESTA_IN_CORSO')) {
      return 'Il salvataggio precedente è ancora in corso. Attendi qualche secondo e riprova.'
    }
    if (errore.status >= 500) return 'Errore del server. Le modifiche sono ancora qui: riprova tra poco.'
  }
  return messaggioDati
}
