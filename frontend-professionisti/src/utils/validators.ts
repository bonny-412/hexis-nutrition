// REGEX
export const REGEX_NUMERO_INTERO = /^\d{1,3}$/
export const REGEX_TELEFONO = /^\d{10}$/
export const REGEX_NOME = /^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$/
export const REGEX_NUMERO_DECIMALE_ITALIANO = /^\d{1,4}(,\d{1,2})?$/
export const REGEX_EMAIL = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
export const REGEX_CODICE_FISCALE = /^[A-Z0-9]{16}$/

// PARSERS
export function numeroItaliano(valore: string): number {
    return Number(valore.replace(',', '.'))
}

export function numeroItalianoOpzionale(valore: string): number | undefined {
    return valore ? numeroItaliano(valore) : undefined
}

// FILTRI SULL'INPUT
export function filtraLettere(valore: string): string {
    return valore.replace(/[^A-Za-zÀ-ÖØ-öø-ÿ' -]/g, '')
}

export function filtraSoloCifre(valore: string, maxCifre: number): string {
    return valore.replace(/\D/g, '').slice(0, maxCifre)
}

export function filtraDecimaleItaliano(valore: string): string {
    const pulito = valore.replace(/[^\d,]/g, '')
    const indiceVirgola = pulito.indexOf(',')
    if (indiceVirgola === -1) {
        return pulito.slice(0, 4)
    }
    const intero = pulito.slice(0, indiceVirgola).slice(0, 4)
    const decimali = pulito.slice(indiceVirgola + 1).replace(/,/g, '').slice(0, 2)
    return `${intero},${decimali}`
}

export function filtraEmail(valore: string): string {
    return valore.replace(/[^a-zA-Z0-9._%+\-@]/g, '')
}

export function filtraCodiceFiscale(valore: string): string {
    return valore.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, 16)
}

export function capitalizzaPrimaLettera(valore: string): string {
    return valore.charAt(0).toUpperCase() + valore.slice(1)
}

export function componiFiltri(...filtri: Array<(valore: string) => string>): (valore: string) => string {
    return (valore) => filtri.reduce((risultato, filtro) => filtro(risultato), valore)
}

export const filtraNome = componiFiltri(filtraLettere, capitalizzaPrimaLettera)

// REGOLATORI ERRORE / VALIDATORI
export function erroreNome(valore: string): string | undefined {
    if (!valore.trim()) return 'Il nome è obbligatorio.'
    if (!REGEX_NOME.test(valore)) return 'Il nome può contenere solo lettere.'
    return undefined
}

export function erroreCognome(valore: string): string | undefined {
    if (!valore.trim()) return 'Il cognome è obbligatorio.'
    if (!REGEX_NOME.test(valore)) return 'Il cognome può contenere solo lettere.'
    return undefined
}

export function erroreCodiceFiscale(valore: string): string | undefined {
    if (!valore.trim()) return 'Il codice fiscale è obbligatorio.'
    if (!REGEX_CODICE_FISCALE.test(valore)) return 'Il codice fiscale deve avere 16 caratteri alfanumerici.'
    return undefined
}

export function erroreEmail(valore: string): string | undefined {
    if (!valore.trim()) return "L'email è obbligatoria."
    if (!REGEX_EMAIL.test(valore)) return "Inserisci un'email valida."
    return undefined
}

export function erroreTelefono(valore: string): string | undefined {
    if (valore && !REGEX_TELEFONO.test(valore)) return 'Il telefono deve contenere 10 cifre numeriche.'
    return undefined
}

export function erroreDataNascita(valore: string): string | undefined {
    if (!valore.trim()) return 'La data di nascita è obbligatoria.'
    return undefined
}

export function erroreSesso(valore: string): string | undefined {
    if (!valore) return 'Il sesso è obbligatorio.'
    return undefined
}

export function erroreAltezza(valore: string): string | undefined {
    if (!valore.trim()) return "L'altezza è obbligatoria."
    if (!REGEX_NUMERO_INTERO.test(valore)) return 'Inserisci un numero intero (es. 178).'
    if (Number(valore) < 50) return "L'altezza deve essere almeno 50 cm."
    return undefined
}

export function errorePeso(valore: string): string | undefined {
    if (!valore.trim()) return 'Il peso è obbligatorio.'
    if (!REGEX_NUMERO_DECIMALE_ITALIANO.test(valore)) return 'Inserisci un numero valido (es. 78,50).'
    return undefined
}

export function erroreCirconferenza(valore: string): string | undefined {
    if (valore && !REGEX_NUMERO_DECIMALE_ITALIANO.test(valore)) return 'Inserisci un numero valido (es. 95,50).'
    return undefined
}

export function erroreNumeroDecimale(valore: string): string | undefined {
    if (valore && !REGEX_NUMERO_DECIMALE_ITALIANO.test(valore)) return 'Inserisci un numero valido (es. 12,50).'
    return undefined
}

export function erroreNumeroDecimaleObbligatorio(valore: string): string | undefined {
    if (!valore.trim()) return 'Il valore è obbligatorio.'
    return erroreNumeroDecimale(valore)
}

export function erroreNomeAlimento(valore: string): string | undefined {
    if (!valore.trim()) return 'Il nome è obbligatorio.'
    return undefined
}

export function erroreCategoriaAlimento(valore: string): string | undefined {
    if (!valore.trim()) return 'La categoria è obbligatoria.'
    return undefined
}