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
