/**
 * Pasqua (calendario gregoriano) come data ISO `YYYY-MM-DD`, con l'algoritmo
 * anonimo gregoriano (Meeus/Jones/Butcher). Solo aritmetica intera, nessun `Date`.
 */
export function pasqua(anno: number): string {
  const a = anno % 19
  const b = Math.floor(anno / 100)
  const c = anno % 100
  const d = Math.floor(b / 4)
  const e = b % 4
  const f = Math.floor((b + 8) / 25)
  const g = Math.floor((b - f + 1) / 3)
  const h = (19 * a + b - d - g + 15) % 30
  const i = Math.floor(c / 4)
  const k = c % 4
  const l = (32 + 2 * e + 2 * i - h - k) % 7
  const m = Math.floor((a + 11 * h + 22 * l) / 451)
  const mese = Math.floor((h + l - 7 * m + 114) / 31)
  const giorno = ((h + l - 7 * m + 114) % 31) + 1
  return `${String(anno).padStart(4, '0')}-${String(mese).padStart(2, '0')}-${String(giorno).padStart(2, '0')}`
}

const FESTIVITA_FISSE: Record<string, string> = {
  '01-01': 'Capodanno',
  '01-06': 'Epifania',
  '04-25': 'Liberazione',
  '05-01': 'Festa dei lavoratori',
  '06-02': 'Festa della Repubblica',
  '08-15': 'Ferragosto',
  '11-01': 'Ognissanti',
  '12-08': 'Immacolata',
  '12-25': 'Natale',
  '12-26': 'Santo Stefano',
}

/** Giorno successivo a una data ISO, con componenti locali (mai `new Date('YYYY-MM-DD')`). */
function giornoDopo(iso: string): string {
  const [anno, mese, giorno] = iso.split('-').map(Number)
  const d = new Date(anno, mese - 1, giorno + 1)
  return `${String(d.getFullYear()).padStart(4, '0')}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Nome della festività italiana del giorno `YYYY-MM-DD`, o `null` se non è festivo. */
export function festivitaDelGiorno(iso: string): string | null {
  const fissa = FESTIVITA_FISSE[iso.slice(5)]
  if (fissa) return fissa
  const easter = pasqua(Number(iso.slice(0, 4)))
  if (iso === easter) return 'Pasqua'
  if (iso === giornoDopo(easter)) return "Lunedì dell'Angelo"
  return null
}
