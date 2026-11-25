import type { Visita } from '@/api/pazienti'

/** Parametri clinici di cui si mostra una variazione tra due visite. */
export type ParametroClinico =
  | 'peso'
  | 'bmi'
  | 'percentualeGrasso'
  | 'massaGrassa'
  | 'massaMagra'
  | 'fmi'
  | 'ffmi'
  | keyof Visita['circonferenze']

export type EsitoVariazione = 'positivo' | 'negativo' | 'neutro'

type Direzione = 1 | -1

/**
 * Verso "buono" di ciascun parametro per obiettivo (decisione del 2026-11-25, vedi
 * docs/superpowers/specs/2026-11-25-grafici-scheda-paziente-design.md). Un parametro assente è neutro:
 * per quell'obiettivo una sua variazione non è né un progresso né un passo indietro.
 * FMI segue la massa grassa, FFMI la massa magra, l'addome segue la vita.
 */
const DIREZIONI: Record<Visita['obiettivo'], Partial<Record<ParametroClinico, Direzione>>> = {
  DIMAGRIMENTO: {
    peso: -1, bmi: -1, percentualeGrasso: -1, massaGrassa: -1, fmi: -1, vitaCm: -1, addomeCm: -1,
    massaMagra: 1, ffmi: 1,
  },
  AUMENTO_PESO: { peso: 1, bmi: 1, massaMagra: 1, ffmi: 1 },
  IPERTROFIA: { massaMagra: 1, ffmi: 1, percentualeGrasso: -1 },
  RICOMPOSIZIONE: { percentualeGrasso: -1, massaGrassa: -1, fmi: -1, massaMagra: 1, ffmi: 1 },
  PREPARAZIONE_SPORTIVA: { percentualeGrasso: -1, massaGrassa: -1, fmi: -1, massaMagra: 1, ffmi: 1 },
  MANTENIMENTO: {},
  EDUCATIVO: {},
  PATOLOGIA_CLINICA: {},
  GRAVIDANZA_ALLATTAMENTO: {},
}

/** 1 se è bene che il parametro salga, -1 se è bene che scenda, 0 se per l'obiettivo è indifferente. */
export function direzioneDesiderata(
  obiettivo: Visita['obiettivo'] | null | undefined,
  parametro: ParametroClinico,
): 1 | -1 | 0 {
  if (!obiettivo) return 0
  return DIREZIONI[obiettivo][parametro] ?? 0
}

export function esitoVariazione(
  obiettivo: Visita['obiettivo'] | null | undefined,
  parametro: ParametroClinico,
  delta: number | null,
): EsitoVariazione {
  const direzione = direzioneDesiderata(obiettivo, parametro)
  if (delta === null || delta === 0 || direzione === 0) return 'neutro'
  return Math.sign(delta) === direzione ? 'positivo' : 'negativo'
}

/** Colore del testo di una variazione: verde solo se va verso l'obiettivo, rosso se va nel verso opposto. */
export const CLASSE_ESITO: Record<EsitoVariazione, string> = {
  positivo: 'text-(--green)',
  negativo: 'text-(--danger)',
  neutro: 'text-(--fg3)',
}

/** Colore di riempimento di una barra di variazione, con gli stessi significati di `CLASSE_ESITO`. */
export const SFONDO_ESITO: Record<EsitoVariazione, string> = {
  positivo: 'bg-(--green)',
  negativo: 'bg-(--danger)',
  neutro: 'bg-(--fg4)',
}
