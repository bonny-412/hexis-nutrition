import type { Visita } from '@/api/pazienti'

export const ETICHETTE_OBIETTIVO: Record<Visita['obiettivo'], string> = {
  DIMAGRIMENTO: 'Dimagrimento',
  AUMENTO_PESO: 'Aumento peso',
  IPERTROFIA: 'Ipertrofia',
  RICOMPOSIZIONE: 'Ricomposizione',
  MANTENIMENTO: 'Mantenimento',
  PREPARAZIONE_SPORTIVA: 'Preparazione sportiva',
  EDUCATIVO: 'Educativo',
  PATOLOGIA_CLINICA: 'Patologia clinica',
  GRAVIDANZA_ALLATTAMENTO: 'Gravidanza/Allattamento',
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

export interface FasciaBmi {
  chiave: string
  etichetta: string
  sogliaMin: number
  sogliaMax: number
  /** Grado di scostamento dal normopeso, usato per lo stile del badge. */
  livello: 'normale' | 'moderato' | 'severo'
}

/** Le 8 fasce ufficiali OMS del BMI, in ordine crescente. */
export const FASCE_BMI: FasciaBmi[] = [
  { chiave: 'sottopeso-grave', etichetta: 'Sottopeso grave', sogliaMin: -Infinity, sogliaMax: 16, livello: 'severo' },
  { chiave: 'sottopeso-moderato', etichetta: 'Sottopeso moderato', sogliaMin: 16, sogliaMax: 17, livello: 'severo' },
  { chiave: 'sottopeso-lieve', etichetta: 'Sottopeso lieve', sogliaMin: 17, sogliaMax: 18.5, livello: 'moderato' },
  { chiave: 'normopeso', etichetta: 'Normopeso', sogliaMin: 18.5, sogliaMax: 25, livello: 'normale' },
  { chiave: 'sovrappeso', etichetta: 'Sovrappeso', sogliaMin: 25, sogliaMax: 30, livello: 'moderato' },
  { chiave: 'obesita-1', etichetta: 'Obesità classe I', sogliaMin: 30, sogliaMax: 35, livello: 'moderato' },
  { chiave: 'obesita-2', etichetta: 'Obesità classe II', sogliaMin: 35, sogliaMax: 40, livello: 'severo' },
  { chiave: 'obesita-3', etichetta: 'Obesità classe III', sogliaMin: 40, sogliaMax: Infinity, livello: 'severo' },
]

/** Individua la fascia OMS (tra le 8 ufficiali) a cui appartiene il bmi. Restituisce `null` se il bmi non è disponibile. */
export function fasciaBmi(bmi: number | null): FasciaBmi | null {
  if (bmi === null) return null
  return FASCE_BMI.find((fascia) => bmi >= fascia.sogliaMin && bmi < fascia.sogliaMax) ?? null
}

/** Calcola il BMI da altezza (cm) e peso (kg). Restituisce `null` se uno dei due valori non è disponibile o non è positivo. */
export function calcolaBmi(altezzaCm: number | null, pesoKg: number | null): number | null {
  if (altezzaCm === null || pesoKg === null || altezzaCm <= 0 || pesoKg <= 0) return null
  const altezzaM = altezzaCm / 100
  return pesoKg / (altezzaM * altezzaM)
}

/** Formatta un numero con la notazione italiana (virgola decimale). Default a 2 cifre: usato per peso, plicometria e circonferenze; il BMI passa esplicitamente 1. */
export function formattaNumero(valore: number, decimali = 2): string {
  return valore.toLocaleString('it-IT', { minimumFractionDigits: decimali, maximumFractionDigits: decimali })
}
