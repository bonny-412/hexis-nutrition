/**
 * Nome di un alimento in forma leggibile. La Banca Dati Alimenti scrive i nomi in maiuscolo
 * ("ACCIUGHE o ALICI SOTT'OLIO"): se il nome è quasi tutto maiuscolo lo porta in minuscolo con
 * l'iniziale maiuscola ("Acciughe o alici sott'olio"). I nomi scritti dal professionista restano come sono.
 */
export function nomeAlimentoLeggibile(nome: string): string {
  const lettere = nome.replace(/[^\p{L}]/gu, '')
  if (lettere.length === 0) return nome
  const maiuscole = lettere.replace(/[^\p{Lu}]/gu, '').length
  if (maiuscole / lettere.length < 0.6) return nome
  const minuscolo = nome.toLocaleLowerCase('it-IT')
  return minuscolo.charAt(0).toLocaleUpperCase('it-IT') + minuscolo.slice(1)
}
