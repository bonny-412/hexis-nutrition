import type { Visita } from '@/api/pazienti'

/** Numero massimo di visite disegnate nei grafici di andamento (le più recenti). */
export const MAX_PUNTI_GRAFICO = 5

export interface PuntoAndamento {
  data: string
  valore: number
}

export interface Andamento {
  punti: PuntoAndamento[]
  ultimo: number | null
  delta: number | null
}

export interface AndamentoPaziente {
  peso: Andamento
  bmi: Andamento
  percentualeGrassoCorporeo: Andamento
  massaMagra: Andamento
}

function costruisciAndamento(visite: Visita[], estraiValore: (visita: Visita) => number | null): Andamento {
  const punti = visite
    .map((v) => ({ data: v.dataVisita, valore: estraiValore(v) }))
    .filter((p): p is PuntoAndamento => p.valore !== null)

  const ultimo = punti.length > 0 ? punti[punti.length - 1].valore : null
  const delta = punti.length >= 2 ? punti[punti.length - 1].valore - punti[punti.length - 2].valore : null

  return { punti, ultimo, delta }
}

export function prepareAndamento(visite: Visita[]): AndamentoPaziente {
  return {
    peso: costruisciAndamento(visite, (v) => v.pesoKg),
    bmi: costruisciAndamento(visite, (v) => v.bmi),
    percentualeGrassoCorporeo: costruisciAndamento(visite, (v) => v.plicometria?.percentualeGrassoCorporeo ?? null),
    massaMagra: costruisciAndamento(visite, (v) => v.plicometria?.massaMagraKg ?? null),  }
}

/**
 * Tick dell'asse x dei grafici: uno per visita, ma senza etichette che si sovrappongono. Le visite troppo
 * vicine a quella precedente (meno di `distanzaMinima` dell'intervallo mostrato) perdono il tick; il punto
 * resta. Prima e ultima visita hanno sempre il loro tick.
 */
export function diradaTick(timestamp: number[], distanzaMinima = 0.22): number[] {
  if (timestamp.length <= 2) return timestamp
  const primo = timestamp[0]
  const ultimo = timestamp[timestamp.length - 1]
  const soglia = (ultimo - primo) * distanzaMinima
  const tenuti = [primo]
  for (const t of timestamp.slice(1, -1)) {
    if (t - tenuti[tenuti.length - 1] >= soglia && ultimo - t >= soglia) tenuti.push(t)
  }
  tenuti.push(ultimo)
  return tenuti
}

/** Vero se le date mostrate cadono in anni diversi: allora le etichette dell'asse riportano l'anno. */
export function suAnniDiversi(timestamp: number[]): boolean {
  if (timestamp.length < 2) return false
  return new Date(timestamp[0]).getUTCFullYear() !== new Date(timestamp[timestamp.length - 1]).getUTCFullYear()
}
