import { inject, onScopeDispose, type InjectionKey } from 'vue'
import { eErroreConnessione } from '@/api/client'

export const segnalaErroreConnessione: InjectionKey<() => void> = Symbol('segnalaErroreConnessione')

export function useErroreConnessione() {
  const segnala = inject(segnalaErroreConnessione, undefined)
  let attiva = true
  onScopeDispose(() => { attiva = false })

  return (errore: unknown) => {
    if (attiva && eErroreConnessione(errore)) segnala?.()
  }
}
