/**
 * Nome di un alimento in forma leggibile. La Banca Dati Alimenti scrive i nomi in maiuscolo
 * ("ACCIUGHE o ALICI SOTT'OLIO"): se il nome è quasi tutto maiuscolo lo porta in minuscolo con
 * l'iniziale maiuscola ("Acciughe o alici sott'olio"). Vale anche quando la BDA aggiunge una specifica
 * in minuscolo dopo un nome maiuscolo ("PANE A FETTE, BIANCO, confezionato", "AGLIO, fresco"): basta
 * che la prima parola, di almeno 4 lettere, sia tutta maiuscola (le sigle brevi come "DHA" restano).
 * I nomi scritti dal professionista restano come sono.
 */
export function nomeAlimentoLeggibile(nome: string): string {
  const lettere = nome.replace(/[^\p{L}]/gu, '')
  if (lettere.length === 0) return nome
  const maiuscole = lettere.replace(/[^\p{Lu}]/gu, '').length
  const primaParola = nome.trim().split(/[\s,]+/)[0] ?? ''
  const apertoInMaiuscolo =
    primaParola.replace(/[^\p{L}]/gu, '').length >= 4 && primaParola === primaParola.toLocaleUpperCase('it-IT')
  if (maiuscole / lettere.length < 0.6 && !apertoInMaiuscolo) return nome
  const minuscolo = nome.toLocaleLowerCase('it-IT')
  return minuscolo.charAt(0).toLocaleUpperCase('it-IT') + minuscolo.slice(1)
}
