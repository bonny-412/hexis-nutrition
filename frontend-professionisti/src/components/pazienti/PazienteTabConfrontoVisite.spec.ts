import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PazienteTabConfrontoVisite from './PazienteTabConfrontoVisite.vue'
import type { Visita } from '@/api/pazienti'

function visita(overrides: Partial<Visita> = {}): Visita {
  return {
    id: '1',
    dataVisita: '2026-01-01',
    altezzaCm: 178,
    pesoKg: 80,
    bmi: 25.2,
    whr: null,
    whtr: null,
    mamcCm: null,
    circonferenze: {
      vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
      polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
    },
    note: null,
    obiettivo: 'MANTENIMENTO',
    plicometria: null,
    ...overrides,
  }
}

describe('PazienteTabConfrontoVisite', () => {
  it('mostra un messaggio se ci sono meno di due visite', () => {
    const wrapper = mount(PazienteTabConfrontoVisite, { props: { visite: [visita()] } })
    expect(wrapper.text()).toContain('Servono almeno due visite')
  })

  it('confronta di default la prima e l\'ultima visita, con la tabella divisa per sezioni', () => {
    const wrapper = mount(PazienteTabConfrontoVisite, {
      props: {
        visite: [
          visita({ id: 'v1', dataVisita: '2026-01-01', pesoKg: 84.5, bmi: 26.0 }),
          visita({ id: 'v2', dataVisita: '2026-06-01', pesoKg: 80.9, bmi: 24.9 }),
          visita({ id: 'v3', dataVisita: '2026-08-01', pesoKg: 78.4, bmi: 24.1 }),
        ],
      },
    })

    expect(wrapper.text()).toContain('Peso')
    expect(wrapper.text()).toContain('84,5')
    expect(wrapper.text()).toContain('78,4')
    expect(wrapper.text()).toContain('-6,1')
    expect(wrapper.text()).toContain('BIA')
    expect(wrapper.text()).toContain('Dati non disponibili')
    expect(wrapper.text()).toContain('Circonferenze')
  })

  it('mostra un messaggio nella sezione plicometria quando nessuna delle due visite la ha registrata', () => {
    const wrapper = mount(PazienteTabConfrontoVisite, {
      props: { visite: [visita({ id: 'v1' }), visita({ id: 'v2', dataVisita: '2026-02-01' })] },
    })
    expect(wrapper.text()).toContain('Nessuna plicometria registrata per queste visite.')
  })

  it('mostra i dati di plicometria quando almeno una visita li ha registrati', () => {
    const wrapper = mount(PazienteTabConfrontoVisite, {
      props: {
        visite: [
          visita({ id: 'v1', dataVisita: '2026-01-01', plicometria: null }),
          visita({
            id: 'v2',
            dataVisita: '2026-02-01',
            plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
          }),
        ],
      },
    })

    expect(wrapper.text()).toContain('% Grasso corporeo')
    expect(wrapper.text()).toContain('18,2')
  })

  it('aggiorna il confronto quando si cambia la visita selezionata', async () => {
    const wrapper = mount(PazienteTabConfrontoVisite, {
      props: {
        visite: [
          visita({ id: 'v1', dataVisita: '2026-01-01', pesoKg: 84.5 }),
          visita({ id: 'v2', dataVisita: '2026-06-01', pesoKg: 80.9 }),
          visita({ id: 'v3', dataVisita: '2026-08-01', pesoKg: 78.4 }),
        ],
      },
    })

    const selects = wrapper.findAll('select')
    await selects[1].setValue('1')

    expect(wrapper.text()).toContain('80,9')
    expect(wrapper.text()).not.toContain('78,4')
  })
})
