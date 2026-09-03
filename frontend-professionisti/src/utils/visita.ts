import type { Visita } from '@/api/pazienti'

export const ETICHETTE_OBIETTIVO: Record<Visita['obiettivo'], string> = {
  DIMAGRIMENTO: 'Dimagrimento',
  AUMENTO_PESO: 'Aumento peso',
  IPERTROFIA: 'Ipertrofia',
  RICOMPOSIZIONE: 'Ricomposizione',
  MANTENIMENTO: 'Mantenimento',
  EDUCATIVO: 'Educativo',
  PREPARAZIONE_SPORTIVA: 'Preparazione sportiva',
}

/** Etichette dei campi di `Circonferenze`, nell'ordine in cui vanno mostrate. */
export const ETICHETTE_CIRCONFERENZE: Record<keyof Visita['circonferenze'], string> = {
  vitaCm: 'Vita',
  fianchiCm: 'Fianchi',
  addomeCm: 'Addome',
  braccioRilassatoCm: 'Braccio rilassato',
  braccioContrattoCm: 'Braccio contratto',
  cosciaCm: 'Coscia',
  polpaccioCm: 'Polpaccio',
  colloCm: 'Collo',
  toraceCm: 'Torace',
  avambraccioCm: 'Avambraccio',
  cavigliaCm: 'Caviglia',
}

/** Classificazione OMS del BMI. Restituisce `null` se il bmi non è disponibile. */
export function categoriaBmi(bmi: number | null): string | null {
  if (bmi === null) return null
  if (bmi < 18.5) return 'sottopeso'
  if (bmi < 25) return 'normopeso'
  if (bmi < 30) return 'sovrappeso'
  return 'obesità'
}

/** Formatta un numero con la notazione italiana (virgola decimale). */
export function formattaNumero(valore: number, decimali = 1): string {
  return valore.toLocaleString('it-IT', { minimumFractionDigits: decimali, maximumFractionDigits: decimali })
}
