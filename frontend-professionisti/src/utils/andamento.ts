import type { Visita } from '@/api/pazienti'

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
    massaMagra: costruisciAndamento(visite, (v) => v.plicometria?.massaMagraKg ?? null),
  }
}
