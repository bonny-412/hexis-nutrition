import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { disattivaPush } from '@/utils/push'

/** Uscita dall'account, condivisa tra il menu profilo dell'header (desktop) e il menu laterale (mobile). */
export function useEsci() {
  const auth = useAuthStore()
  const router = useRouter()

  return async function esci() {
    // Prima del logout: serve ancora il token per togliere l'iscrizione push di questo dispositivo.
    await disattivaPush()
    auth.logout()
    router.push({ name: 'login' })
  }
}
