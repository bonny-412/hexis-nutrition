import { chiavePush, registraPush, rimuoviPush } from '@/api/notifiche'

export type StatoPush = 'non-supportato' | 'da-attivare' | 'attivo' | 'negato' | 'non-configurato'

const SERVICE_WORKER = '/sw.js'

function supportato(): boolean {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

// La chiave VAPID arriva in base64url: il browser la vuole come byte.
function chiaveInByte(base64Url: string): Uint8Array<ArrayBuffer> {
  const base64 = (base64Url + '='.repeat((4 - (base64Url.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/')
  const binario = atob(base64)
  const byte = new Uint8Array(new ArrayBuffer(binario.length))
  for (let i = 0; i < binario.length; i++) byte[i] = binario.charCodeAt(i)
  return byte
}

async function inviaIscrizione(sub: PushSubscription): Promise<void> {
  const json = sub.toJSON()
  const p256dh = json.keys?.p256dh
  const auth = json.keys?.auth
  if (!json.endpoint || !p256dh || !auth) return
  await registraPush({ endpoint: json.endpoint, p256dh, auth })
}

/** Stato delle notifiche push su questo dispositivo. Non lancia mai. */
export async function statoPush(): Promise<StatoPush> {
  if (!supportato()) return 'non-supportato'
  try {
    if (!(await chiavePush())) return 'non-configurato'
    if (Notification.permission === 'denied') return 'negato'
    const registrazione = await navigator.serviceWorker.getRegistration(SERVICE_WORKER)
    const sub = await registrazione?.pushManager.getSubscription()
    return sub && Notification.permission === 'granted' ? 'attivo' : 'da-attivare'
  } catch {
    return 'non-configurato'
  }
}

/** Da chiamare in un gesto dell'utente (i browser, Safari compreso, lo richiedono). Non lancia mai. */
export async function attivaPush(): Promise<StatoPush> {
  if (!supportato()) return 'non-supportato'
  try {
    // Prima di qualsiasi `await`: Safari accetta la richiesta solo se parte direttamente dal gesto dell'utente.
    const permesso = await Notification.requestPermission()
    if (permesso === 'denied') return 'negato'
    if (permesso !== 'granted') return 'da-attivare'
    const chiave = await chiavePush()
    if (!chiave) return 'non-configurato'
    const registrazione = await navigator.serviceWorker.register(SERVICE_WORKER)
    await navigator.serviceWorker.ready
    const sub =
      (await registrazione.pushManager.getSubscription()) ??
      (await registrazione.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: chiaveInByte(chiave) }))
    await inviaIscrizione(sub)
    return 'attivo'
  } catch {
    return 'da-attivare'
  }
}

/** All'avvio: se il permesso c'è già, riallinea l'iscrizione del dispositivo al backend (senza chiedere nulla). */
export async function sincronizzaPush(): Promise<void> {
  if (!supportato() || Notification.permission !== 'granted') return
  try {
    const registrazione = await navigator.serviceWorker.register(SERVICE_WORKER)
    const sub = await registrazione.pushManager.getSubscription()
    if (sub) await inviaIscrizione(sub)
  } catch {
    // Best-effort: le notifiche in app continuano a funzionare.
  }
}

/** Al logout: toglie l'iscrizione, così un computer condiviso non riceve gli avvisi del professionista precedente. */
export async function disattivaPush(): Promise<void> {
  if (!supportato()) return
  try {
    const registrazione = await navigator.serviceWorker.getRegistration(SERVICE_WORKER)
    const sub = await registrazione?.pushManager.getSubscription()
    if (!sub) return
    await rimuoviPush(sub.endpoint).catch(() => undefined)
    await sub.unsubscribe()
  } catch {
    // Best-effort.
  }
}
