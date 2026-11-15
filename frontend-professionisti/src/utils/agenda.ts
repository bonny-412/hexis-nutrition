export type VistaAgenda = 'MESE' | 'SETTIMANA' | 'GIORNO'
export type FiltroAgenda = 'TUTTI' | 'VISITE' | 'PROMEMORIA'

/** Pixel per ora della griglia oraria e ora di origine (08:00). */
export const PX_PER_ORA = 58
export const ORA_ORIGINE_MIN = 8 * 60

const MESI = ['gennaio', 'febbraio', 'marzo', 'aprile', 'maggio', 'giugno', 'luglio', 'agosto', 'settembre', 'ottobre', 'novembre', 'dicembre']
const GIORNI_BREVI = ['Dom', 'Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab']

// Tutta l'aritmetica usa Date con componenti locali: mai new Date('YYYY-MM-DD') (UTC).
function daIso(iso: string): Date {
  const [anno, mese, giorno] = iso.split('-').map(Number)
  return new Date(anno, mese - 1, giorno)
}

function aIso(d: Date): string {
  return `${String(d.getFullYear()).padStart(4, '0')}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export function aggiungiGiorni(iso: string, n: number): string {
  const [anno, mese, giorno] = iso.split('-').map(Number)
  return aIso(new Date(anno, mese - 1, giorno + n))
}

/** Lunedì della settimana che contiene la data. */
export function inizioSettimana(iso: string): string {
  const giornoSettimana = daIso(iso).getDay() // 0 = domenica
  return aggiungiGiorni(iso, -((giornoSettimana + 6) % 7))
}

export function giorniDellaSettimana(iso: string): string[] {
  const lunedi = inizioSettimana(iso)
  return Array.from({ length: 7 }, (_, i) => aggiungiGiorni(lunedi, i))
}

/** Celle del mese: multiplo di 7, da lunedì, con i giorni dei mesi adiacenti. */
export function celleDelMese(iso: string): string[] {
  const [anno, mese] = iso.split('-').map(Number)
  const prefisso = `${String(anno).padStart(4, '0')}-${String(mese).padStart(2, '0')}`
  const ultimoGiorno = new Date(anno, mese, 0).getDate()
  const inizio = inizioSettimana(`${prefisso}-01`)
  const fine = aggiungiGiorni(inizioSettimana(`${prefisso}-${String(ultimoGiorno).padStart(2, '0')}`), 6)
  const celle: string[] = []
  for (let g = inizio; g <= fine; g = aggiungiGiorni(g, 1)) celle.push(g)
  return celle
}

export function intervalloVisibile(vista: VistaAgenda, riferimento: string): { da: string; a: string } {
  if (vista === 'GIORNO') return { da: riferimento, a: riferimento }
  if (vista === 'SETTIMANA') {
    const giorni = giorniDellaSettimana(riferimento)
    return { da: giorni[0], a: giorni[6] }
  }
  const celle = celleDelMese(riferimento)
  return { da: celle[0], a: celle[celle.length - 1] }
}

/** `'09:30'` -> 570. */
export function minutiDaOra(ora: string): number {
  const [h, m] = ora.split(':').map(Number)
  return h * 60 + m
}

export function posizioneEvento(ora: string, durataMinuti: number): { topPx: number; altezzaPx: number } {
  return {
    topPx: ((minutiDaOra(ora) - ORA_ORIGINE_MIN) * PX_PER_ORA) / 60,
    altezzaPx: (durataMinuti * PX_PER_ORA) / 60,
  }
}

/**
 * Assegna una corsia a ogni evento. Gli eventi formano gruppi di sovrapposizione
 * transitiva (intervalli semiaperti [inizio, fine)); dentro il gruppo ciascuno
 * prende la prima corsia libera; `corsie` è il numero di corsie del gruppo.
 */
export function impaginaSovrapposizioni<T extends { inizioMin: number; fineMin: number }>(
  eventi: T[],
): Array<T & { corsia: number; corsie: number }> {
  const ordinati = [...eventi].sort((a, b) => a.inizioMin - b.inizioMin || a.fineMin - b.fineMin)
  const risultato: Array<T & { corsia: number; corsie: number }> = []

  let gruppo: Array<T & { corsia: number; corsie: number }> = []
  let fineGruppo = -Infinity
  let fineCorsie: number[] = []

  const chiudiGruppo = () => {
    for (const e of gruppo) e.corsie = fineCorsie.length
    risultato.push(...gruppo)
    gruppo = []
    fineCorsie = []
  }

  for (const evento of ordinati) {
    if (gruppo.length > 0 && evento.inizioMin >= fineGruppo) chiudiGruppo()
    let corsia = fineCorsie.findIndex((fine) => fine <= evento.inizioMin)
    if (corsia === -1) {
      corsia = fineCorsie.length
      fineCorsie.push(evento.fineMin)
    } else {
      fineCorsie[corsia] = evento.fineMin
    }
    fineGruppo = gruppo.length === 0 ? evento.fineMin : Math.max(fineGruppo, evento.fineMin)
    gruppo.push({ ...evento, corsia, corsie: 1 })
  }
  if (gruppo.length > 0) chiudiGruppo()
  return risultato
}

export function etichettaPeriodo(vista: VistaAgenda, riferimento: string): string {
  const [anno, mese, giorno] = riferimento.split('-').map(Number)
  if (vista === 'MESE') {
    const nome = MESI[mese - 1]
    return `${nome.charAt(0).toUpperCase()}${nome.slice(1)} ${anno}`
  }
  if (vista === 'GIORNO') {
    return `${GIORNI_BREVI[daIso(riferimento).getDay()]} ${giorno} ${MESI[mese - 1]} ${anno}`
  }
  const giorni = giorniDellaSettimana(riferimento)
  const [aA, aM, aG] = giorni[0].split('-').map(Number)
  const [bA, bM, bG] = giorni[6].split('-').map(Number)
  if (aA !== bA) return `${aG} ${MESI[aM - 1]} ${aA} – ${bG} ${MESI[bM - 1]} ${bA}`
  if (aM !== bM) return `${aG} ${MESI[aM - 1]} – ${bG} ${MESI[bM - 1]} ${bA}`
  return `${aG} – ${bG} ${MESI[bM - 1]} ${bA}`
}

/** Data odierna ISO con componenti locali. */
export function oggiIso(): string {
  return aIso(new Date())
}

/** Sposta una data di `n` mesi (negativo = indietro), riducendo il giorno se il mese di arrivo è più corto. */
export function aggiungiMesi(iso: string, n: number): string {
  const [anno, mese, giorno] = iso.split('-').map(Number)
  const ultimoGiorno = new Date(anno, mese - 1 + n + 1, 0).getDate()
  return aIso(new Date(anno, mese - 1 + n, Math.min(giorno, ultimoGiorno)))
}
