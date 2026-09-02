/**
 * Converte una data ISO `YYYY-MM-DD` nel timestamp (ms) della mezzanotte UTC
 * di quel giorno. Per una stringa data-only lo spec ECMAScript impone il
 * parsing come UTC, quindi il valore è indipendente dal fuso dell'utente:
 * usalo solo come coordinata numerica (es. asse x di un grafico), mai per
 * derivarne testo con i getter locali di `Date`.
 */
export function isoATimestamp(dataIso: string): number {
    return new Date(dataIso).getTime()
}

/**
 * Converte un timestamp (ms) nella data ISO `YYYY-MM-DD` del giorno UTC
 * corrispondente. Usa solo getter UTC: i getter locali applicherebbero
 * l'offset del fuso del browser, spostando la data al giorno sbagliato.
 */
export function timestampAIso(timestamp: number): string {
    const d = new Date(timestamp)
    const anno = String(d.getUTCFullYear()).padStart(4, '0')
    const mese = String(d.getUTCMonth() + 1).padStart(2, '0')
    const giorno = String(d.getUTCDate()).padStart(2, '0')
    return `${anno}-${mese}-${giorno}`
}

/**
 * Formatta una data come `GG/MM/AA` per la visualizzazione.
 *
 * Accetta una data ISO `YYYY-MM-DD` (formattata per pura manipolazione di
 * stringhe) oppure un timestamp in millisecondi (normalizzato al giorno UTC).
 * Non usa mai `toLocaleDateString`, `Intl.DateTimeFormat` senza `timeZone`
 * o getter locali di `Date`: applicherebbero l'offset del fuso dell'utente
 * e potrebbero mostrare il giorno di calendario sbagliato.
 */
export function formattaDataItaliana(data: string | number): string {
    const iso = typeof data === 'number' ? timestampAIso(data) : data
    const [anno, mese, giorno] = iso.split('-')
    return `${giorno}/${mese}/${anno.slice(2)}`
}

/**
 * Formatta una data ISO `YYYY-MM-DD` come `GG/MM/AAAA` (anno completo) per
 * la visualizzazione — a differenza di `formattaDataItaliana` (pensata per
 * le etichette compatte dei grafici, anno a 2 cifre). Pura manipolazione di
 * stringhe, nessun uso di `Date`: evita lo stesso bug di fuso orario.
 */
export function formattaDataItalianaEstesa(dataIso: string): string {
    const [anno, mese, giorno] = dataIso.split('-')
    return `${giorno}/${mese}/${anno}`
}

const MESI_BREVI_ITALIANI = ['gen', 'feb', 'mar', 'apr', 'mag', 'giu', 'lug', 'ago', 'set', 'ott', 'nov', 'dic']

/**
 * Formatta una data ISO `YYYY-MM-DD` come `GG mmm AAAA` (es. "01 ago 2026"),
 * con il mese abbreviato a 3 lettere in italiano. Pura manipolazione di
 * stringhe, nessun uso di `Date`: evita lo stesso bug di fuso orario.
 */
export function formattaDataItalianaConMese(dataIso: string): string {
    const [anno, mese, giorno] = dataIso.split('-')
    const nomeMese = MESI_BREVI_ITALIANI[Number(mese) - 1]
    return `${giorno} ${nomeMese} ${anno}`
}

export function calcolaEta(dataNascita: string): number | null {
    if (!dataNascita) return null

    const nascita = new Date(dataNascita)
    if (Number.isNaN(nascita.getTime())) return null

    const oggi = new Date()
    if (nascita > oggi) return null

    let eta = oggi.getFullYear() - nascita.getFullYear()
    const compleannoNonAncoraArrivato =
        oggi.getMonth() < nascita.getMonth() ||
        (oggi.getMonth() === nascita.getMonth() && oggi.getDate() < nascita.getDate())

    if (compleannoNonAncoraArrivato) eta--

    return eta
}
