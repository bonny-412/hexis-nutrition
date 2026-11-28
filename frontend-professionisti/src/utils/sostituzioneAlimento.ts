import type { Alimento } from '@/api/alimenti'

/** Valore nutrizionale che la sostituzione deve mantenere uguale. */
export type ValoreMantenuto = 'kcal' | 'proteine' | 'carboidrati' | 'grassi'

/** Valori mantenibili, nell'ordine in cui vengono proposti. */
export const VALORI_MANTENIBILI: readonly {
  valore: ValoreMantenuto
  etichetta: string
  unita: 'kcal' | 'g'
  nomeAssente: string
}[] = [
  { valore: 'kcal', etichetta: 'Kcal', unita: 'kcal', nomeAssente: 'kcal' },
  { valore: 'proteine', etichetta: 'Proteine', unita: 'g', nomeAssente: 'proteine' },
  { valore: 'carboidrati', etichetta: 'Carboidrati', unita: 'g', nomeAssente: 'carboidrati' },
  { valore: 'grassi', etichetta: 'Grassi', unita: 'g', nomeAssente: 'grassi' },
]

/** Dati di un alimento in una riga del piano, valori per 100 g (stessa forma dell'evento `aggiunto` di AggiungiAlimentoDialog). */
export interface DatiAlimentoRiga {
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  fibre100g: number | null
  ferro100mg: number | null
  calcio100mg: number | null
  acqua100g: number | null
}

export interface RigaAlimento extends DatiAlimentoRiga {
  grammi: number
}

/** Converte un alimento del database (valori per `quantitaG`) nei dati per 100 g di una riga; i facoltativi assenti restano `null`. */
export function datiRigaDaAlimento(alimento: Alimento): DatiAlimentoRiga {
  const fattore = 100 / alimento.quantitaG
  return {
    alimentoId: alimento.id,
    nome: alimento.nome,
    kcal100g: alimento.kcal * fattore,
    proteine100g: alimento.proteineG * fattore,
    carboidrati100g: alimento.carboidratiG * fattore,
    grassi100g: alimento.grassiG * fattore,
    zuccheri100g: alimento.zuccheriG !== null ? alimento.zuccheriG * fattore : null,
    fibre100g: alimento.fibreG !== null ? alimento.fibreG * fattore : null,
    ferro100mg: alimento.ferroMg !== null ? alimento.ferroMg * fattore : null,
    calcio100mg: alimento.calcioMg !== null ? alimento.calcioMg * fattore : null,
    acqua100g: alimento.acquaG !== null ? alimento.acquaG * fattore : null,
  }
}

/** I quattro valori della riga ai suoi grammi. */
export function valoriRiga(riga: RigaAlimento): Record<ValoreMantenuto, number> {
  const rapporto = riga.grammi / 100
  return {
    kcal: riga.kcal100g * rapporto,
    proteine: riga.proteine100g * rapporto,
    carboidrati: riga.carboidrati100g * rapporto,
    grassi: riga.grassi100g * rapporto,
  }
}

/** Valore per 100 g di un alimento per la chiave indicata. */
export function valorePer100g(dati: DatiAlimentoRiga, chiave: ValoreMantenuto): number {
  switch (chiave) {
    case 'kcal': return dati.kcal100g
    case 'proteine': return dati.proteine100g
    case 'carboidrati': return dati.carboidrati100g
    case 'grassi': return dati.grassi100g
  }
}

/** Grammi del sostituto che portano lo stesso valore; `null` se uno dei due valori non è positivo o finito. */
export function grammiEquivalenti(valoreDaMantenere: number, valorePer100gSostituto: number): number | null {
  if (!Number.isFinite(valoreDaMantenere) || !Number.isFinite(valorePer100gSostituto)) return null
  if (valoreDaMantenere <= 0 || valorePer100gSostituto <= 0) return null
  return Math.max(1, Math.round((valoreDaMantenere / valorePer100gSostituto) * 100))
}

/** Primo valore (kcal, proteine, carboidrati, grassi) maggiore di zero nella riga, `null` se nessuno. */
export function primoValoreDisponibile(riga: RigaAlimento): ValoreMantenuto | null {
  const valori = valoriRiga(riga)
  return VALORI_MANTENIBILI.find((v) => valori[v.valore] > 0)?.valore ?? null
}

/** Differenze (sostituto meno partenza, arrotondate come in tabella) sui valori non mantenuti, escluse quelle nulle. */
export function differenze(
  partenza: Record<ValoreMantenuto, number>,
  sostituto: Record<ValoreMantenuto, number>,
  mantenuto: ValoreMantenuto,
): { chiave: ValoreMantenuto; delta: number }[] {
  return VALORI_MANTENIBILI
    .map((v) => v.valore)
    .filter((chiave) => chiave !== mantenuto)
    .map((chiave) => ({ chiave, delta: Math.round(sostituto[chiave]) - Math.round(partenza[chiave]) }))
    .filter((d) => d.delta !== 0)
}

/** Vero se i due dati descrivono lo stesso alimento: stesso id, o stesso nome se entrambi manuali. */
export function stessoAlimento(a: DatiAlimentoRiga, b: DatiAlimentoRiga): boolean {
  if (a.alimentoId !== null && b.alimentoId !== null) return a.alimentoId === b.alimentoId
  if (a.alimentoId === null && b.alimentoId === null) return a.nome === b.nome
  return false
}

/**
 * Sostituisce l'alimento della riga mantenendo il valore scelto. Muta la riga in place
 * (tiene `clientId` e posizione). Restituisce `false`, senza toccare la riga, se i grammi non sono calcolabili.
 */
export function applicaSostituzione(riga: RigaAlimento, sostituto: DatiAlimentoRiga, mantenuto: ValoreMantenuto): boolean {
  const grammi = grammiEquivalenti(valoriRiga(riga)[mantenuto], valorePer100g(sostituto, mantenuto))
  if (grammi === null) return false
  Object.assign(riga, sostituto, { grammi })
  return true
}
