import type { Paziente } from '@/api/pazienti'
import { TONI_STATO } from '@/utils/toniStato'
import { AREA_PAZIENTE_ATTIVA } from '@/config/funzioni'

/** Iniziali dell'avatar: prima lettera del nome e del cognome ("Beatrice", "De Luca" → "BD"). */
export function inizialiPaziente(nome: string, cognome: string): string {
  return `${nome.trim()[0] ?? ''}${cognome.trim()[0] ?? ''}`.toUpperCase()
}

/** Colori del badge di stato account; gli avatar delle liste usano gli stessi (codice in `toniStato.ts`). */
export const CLASSI_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: TONI_STATO.daFare,
  INVITATO: TONI_STATO.neutro,
  ATTIVO: TONI_STATO.attivo,
}

/**
 * Classi dell'avatar di un paziente: archiviato in neutro, altrimenti il colore del suo stato account.
 * Senza area paziente lo stato account non si vede, quindi il colore non avrebbe spiegazione: tutti `attivo`.
 */
export function classiAvatarPaziente(paziente: Pick<Paziente, 'statoAccount' | 'archiviato'>): string {
  if (paziente.archiviato) return TONI_STATO.neutro
  return AREA_PAZIENTE_ATTIVA ? CLASSI_STATO_ACCOUNT[paziente.statoAccount] : TONI_STATO.attivo
}
