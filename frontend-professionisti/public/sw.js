// Service worker di Hexis: mostra le notifiche push e porta all'agenda al clic.
// Il testo dell'avviso è generico (nessun dato del paziente): può comparire a schermo bloccato.

self.addEventListener('install', () => self.skipWaiting())
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()))

self.addEventListener('push', (event) => {
  let dati = {}
  try {
    dati = event.data ? event.data.json() : {}
  } catch {
    dati = {}
  }
  const titolo = dati.titolo || 'Hexis'
  const opzioni = {
    body: dati.corpo || '',
    icon: '/favicon.svg',
    data: { url: dati.url || '/agenda' },
  }

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((finestre) => {
      // Se Hexis è già visibile in primo piano il professionista vede il campanello: niente avviso di sistema.
      if (finestre.some((f) => f.visibilityState === 'visible')) return undefined
      return self.registration.showNotification(titolo, opzioni)
    }),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = (event.notification.data && event.notification.data.url) || '/agenda'
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((finestre) => {
      for (const finestra of finestre) {
        if ('focus' in finestra) {
          return finestra.focus().then((f) => ('navigate' in f ? f.navigate(url) : undefined))
        }
      }
      return self.clients.openWindow(url)
    }),
  )
})
