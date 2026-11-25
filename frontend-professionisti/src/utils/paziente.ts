import type { Paziente } from '@/api/pazienti'

/** Iniziali dell'avatar: prima lettera del nome e del cognome ("Beatrice", "De Luca" → "BD"). */
export function inizialiPaziente(nome: string, cognome: string): string {
  return `${nome.trim()[0] ?? ''}${cognome.trim()[0] ?? ''}`.toUpperCase()
}

/** Colori del badge di stato account; gli avatar delle liste usano gli stessi (preferenza di Andrea). */
export const CLASSI_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: 'bg-(--danger)/10 text-(--danger)',
  INVITATO: 'bg-(--warn-bg) text-(--warn-fg)',
  ATTIVO: 'bg-(--mint) text-(--green-d)',
}

/** Classi dell'avatar di un paziente: archiviato in rosso, altrimenti il colore del suo stato account. */
export function classiAvatarPaziente(paziente: Pick<Paziente, 'statoAccount' | 'archiviato'>): string {
  return paziente.archiviato ? 'bg-(--danger)/10 text-(--danger)' : CLASSI_STATO_ACCOUNT[paziente.statoAccount]
}
