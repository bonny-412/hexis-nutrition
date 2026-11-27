import { describe, expect, it } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { useConfermaUscita } from './useConfermaUscita'

// Bug 13 del collaudo (27/11/2026): la sidebar usciva dalla bozza del piano senza "Uscire senza salvare?".
// La guardia vale per qualunque navigazione, non solo per il pulsante della pagina.

function creaRouter() {
  const vuota = { render: () => null }
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/form', component: vuota },
      { path: '/agenda', component: vuota },
    ],
  })
}

async function montaPagina(modificato: boolean) {
  const router = creaRouter()
  router.push('/form')
  await router.isReady()
  const stato = ref(modificato)
  let api!: ReturnType<typeof useConfermaUscita>
  const Pagina = defineComponent({
    setup() {
      api = useConfermaUscita(() => stato.value)
      return () => h('div')
    },
  })
  const wrapper = mount(Pagina, { global: { plugins: [router] } })
  return { router, stato, api: () => api, wrapper }
}

describe('useConfermaUscita', () => {
  it('senza modifiche si esce subito', async () => {
    const { router, api } = await montaPagina(false)

    await router.push('/agenda')

    expect(router.currentRoute.value.path).toBe('/agenda')
    expect(api().aperta.value).toBe(false)
  })

  it('con modifiche una navigazione qualunque (es. la sidebar) si ferma e chiede conferma', async () => {
    const { router, api } = await montaPagina(true)

    await router.push('/agenda')

    expect(router.currentRoute.value.path).toBe('/form')
    expect(api().aperta.value).toBe(true)
  })

  it('"Esci senza salvare" porta dove si voleva andare', async () => {
    const { router, api } = await montaPagina(true)
    await router.push('/agenda')

    api().esciSenzaSalvare()
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/agenda')
  })

  it('dopo un salvataggio riuscito la prossima uscita non chiede, e solo quella', async () => {
    const { router, api } = await montaPagina(true)

    api().consentiUscita()
    await router.push('/agenda')
    expect(router.currentRoute.value.path).toBe('/agenda')

    await router.push('/form')
    expect(router.currentRoute.value.path).toBe('/agenda')
    expect(api().aperta.value).toBe(true)
  })

  it('smontata la pagina la guardia non vale più', async () => {
    const { router, wrapper } = await montaPagina(true)

    wrapper.unmount()
    await router.push('/agenda')

    expect(router.currentRoute.value.path).toBe('/agenda')
  })
})
