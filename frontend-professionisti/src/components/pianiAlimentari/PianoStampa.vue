<script setup lang="ts">
import { computed } from 'vue'
import type { GiornoSettimana, ModalitaPiano, TipoPasto } from '@/api/pianiAlimentari'
import { formattaDataItalianaConMese } from '@/utils/data'
import { nomeAlimentoLeggibile } from '@/utils/testo'

// Versione stampabile del piano (bug 04 del collaudo, 27/11/2026). Prima "Stampa PDF" stampava la pagina
// dell'editor: comandi di modifica, niente intestazione e solo il primo foglio, perché il contenuto stava
// nel riquadro scorrevole dell'AppShell. Questo documento è montato fuori da #app (Teleport sul body):
// a schermo è nascosto, in stampa è l'unica cosa visibile (regole @media print in main.css).
// Colori fissi e non i token del tema: si stampa sempre su carta bianca, anche col tema scuro.

interface RigaStampa {
  clientId: string
  nome: string
  grammi: number
  kcal100g: number
}
interface PastoStampa {
  clientId: string
  giornoSettimana: GiornoSettimana
  nome: string
  nota: string | null
  righe: RigaStampa[]
}
interface GiornoMacroStampa {
  giornoSettimana: GiornoSettimana
  kcalTarget: string
  proteineTarget: string
  carboidratiTarget: string
  grassiTarget: string
  nota: string | null
}
interface EsempioStampa {
  clientId: string
  tipoPasto: TipoPasto
  nome: string
  nota: string | null
  righe: RigaStampa[]
}

const props = defineProps<{
  nome: string
  paziente: string
  professionista: string | null
  dataInizio: string
  dataFine: string | null
  obiettivoKcal: string
  modalita: ModalitaPiano
  pasti: PastoStampa[]
  giorniMacro: GiornoMacroStampa[]
  esempi: EsempioStampa[]
}>()

const GIORNI: { chiave: GiornoSettimana; etichetta: string }[] = [
  { chiave: 'LUNEDI', etichetta: 'Lunedì' },
  { chiave: 'MARTEDI', etichetta: 'Martedì' },
  { chiave: 'MERCOLEDI', etichetta: 'Mercoledì' },
  { chiave: 'GIOVEDI', etichetta: 'Giovedì' },
  { chiave: 'VENERDI', etichetta: 'Venerdì' },
  { chiave: 'SABATO', etichetta: 'Sabato' },
  { chiave: 'DOMENICA', etichetta: 'Domenica' },
]
const CATEGORIE: { tipo: TipoPasto; etichetta: string }[] = [
  { tipo: 'COLAZIONE', etichetta: 'Colazione' },
  { tipo: 'SPUNTINO_MATTINA', etichetta: 'Spuntino mattina' },
  { tipo: 'PRANZO', etichetta: 'Pranzo' },
  { tipo: 'SPUNTINO_POMERIGGIO', etichetta: 'Spuntino pomeriggio' },
  { tipo: 'CENA', etichetta: 'Cena' },
]

const numero = new Intl.NumberFormat('it-IT', { maximumFractionDigits: 1 })
const intero = new Intl.NumberFormat('it-IT', { maximumFractionDigits: 0 })

function kcal(righe: RigaStampa[]): number {
  return righe.reduce((somma, r) => somma + (r.kcal100g * r.grammi) / 100, 0)
}

// Si stampa solo ciò che ha contenuto: un pasto senza alimenti né nota, o un giorno vuoto, non occupa carta.
const giorniPasti = computed(() =>
  GIORNI.map((g) => {
    const pasti = props.pasti.filter((p) => p.giornoSettimana === g.chiave && (p.righe.length > 0 || p.nota))
    return { ...g, pasti, kcal: pasti.reduce((s, p) => s + kcal(p.righe), 0) }
  }).filter((g) => g.pasti.length > 0),
)

const giorniMacro = computed(() =>
  GIORNI.map((g) => ({ ...g, target: props.giorniMacro.find((t) => t.giornoSettimana === g.chiave) }))
    .filter((g) => g.target && (g.target.kcalTarget || g.target.proteineTarget || g.target.carboidratiTarget
      || g.target.grassiTarget || g.target.nota)),
)

const categorieEsempi = computed(() =>
  CATEGORIE.map((c) => ({
    ...c,
    esempi: props.esempi.filter((e) => e.tipoPasto === c.tipo && (e.righe.length > 0 || e.nota)),
  })).filter((c) => c.esempi.length > 0),
)

const vuoto = computed(() =>
  (props.modalita === 'PASTI' && giorniPasti.value.length === 0)
  || (props.modalita === 'MACRO' && giorniMacro.value.length === 0)
  || (props.modalita === 'ESEMPI' && categorieEsempi.value.length === 0),
)

const periodo = computed(() => {
  const dal = formattaDataItalianaConMese(props.dataInizio)
  return props.dataFine ? `Dal ${dal} al ${formattaDataItalianaConMese(props.dataFine)}` : `Dal ${dal}`
})

const stampatoIl = formattaDataItalianaConMese(new Date().toISOString().slice(0, 10))

function grammi(r: RigaStampa) {
  return `${numero.format(r.grammi)} g`
}
function valoreMacro(testo: string, unita: string) {
  return testo ? `${testo} ${unita}` : '—'
}
</script>

<template>
  <Teleport to="body">
    <article class="stampa-piano" data-test="stampa-piano" aria-hidden="true">
      <header class="sp-testata">
        <span class="sp-marchio">
          <img src="@/assets/hexis-logo.svg" alt="" class="sp-logo" />
          Hexis
        </span>
        <span v-if="professionista" class="sp-studio">{{ professionista }}</span>
      </header>

      <h1 class="sp-titolo">{{ nome || 'Piano alimentare' }}</h1>
      <dl class="sp-dati">
        <div><dt>Per</dt><dd>{{ paziente }}</dd></div>
        <div><dt>Periodo</dt><dd>{{ periodo }}</dd></div>
        <div v-if="obiettivoKcal"><dt>Obiettivo</dt><dd>{{ obiettivoKcal }} kcal al giorno</dd></div>
      </dl>

      <p v-if="vuoto" class="sp-vuoto">Il piano non contiene ancora alimenti.</p>

      <!-- Pasti: un blocco per giorno, alimenti con la quantità allineata a destra su una linea di puntini.
           Il blocco è una tabella solo per la stampa: il titolo del giorno sta nel <thead>, che il browser
           ripete in cima a ogni foglio su cui il giorno continua (vedi .sp-blocco nello stile). -->
      <template v-else-if="modalita === 'PASTI'">
        <table v-for="giorno in giorniPasti" :key="giorno.chiave" class="sp-sezione sp-blocco">
          <thead>
            <tr>
              <th>
                <h2 class="sp-giorno">
                  <span>{{ giorno.etichetta }}</span>
                  <span class="sp-kcal">{{ intero.format(giorno.kcal) }} kcal</span>
                </h2>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="pasto in giorno.pasti" :key="pasto.clientId">
              <td class="sp-pasto">
                <h3 class="sp-pasto-nome">
                  <span>{{ pasto.nome }}</span>
                  <span v-if="pasto.righe.length" class="sp-kcal">{{ intero.format(kcal(pasto.righe)) }} kcal</span>
                </h3>
                <ul class="sp-righe">
                  <li v-for="riga in pasto.righe" :key="riga.clientId" class="sp-riga">
                    <span class="sp-alimento">{{ nomeAlimentoLeggibile(riga.nome) }}</span>
                    <span class="sp-puntini" aria-hidden="true" />
                    <span class="sp-grammi">{{ grammi(riga) }}</span>
                  </li>
                </ul>
                <p v-if="pasto.nota" class="sp-nota">{{ pasto.nota }}</p>
              </td>
            </tr>
          </tbody>
        </table>
      </template>

      <!-- Macronutrienti: obiettivi del giorno in tabella, le note sotto. -->
      <template v-else-if="modalita === 'MACRO'">
        <table class="sp-tabella">
          <thead>
            <tr><th>Giorno</th><th>Energia</th><th>Proteine</th><th>Carboidrati</th><th>Grassi</th></tr>
          </thead>
          <tbody>
            <tr v-for="giorno in giorniMacro" :key="giorno.chiave">
              <th scope="row">{{ giorno.etichetta }}</th>
              <td>{{ valoreMacro(giorno.target!.kcalTarget, 'kcal') }}</td>
              <td>{{ valoreMacro(giorno.target!.proteineTarget, 'g') }}</td>
              <td>{{ valoreMacro(giorno.target!.carboidratiTarget, 'g') }}</td>
              <td>{{ valoreMacro(giorno.target!.grassiTarget, 'g') }}</td>
            </tr>
          </tbody>
        </table>
        <div v-for="giorno in giorniMacro.filter((g) => g.target!.nota)" :key="`nota-${giorno.chiave}`" class="sp-pasto">
          <h3 class="sp-pasto-nome"><span>{{ giorno.etichetta }}</span></h3>
          <p class="sp-nota">{{ giorno.target!.nota }}</p>
        </div>
      </template>

      <!-- Esempi intercambiabili: per ogni pasto della giornata, gli esempi tra cui scegliere.
           Stessa tabella dei giorni: il nome del pasto si ripete sul foglio dopo se gli esempi continuano. -->
      <template v-else>
        <table v-for="categoria in categorieEsempi" :key="categoria.tipo" class="sp-sezione sp-blocco">
          <thead>
            <tr>
              <th>
                <h2 class="sp-giorno"><span>{{ categoria.etichetta }}</span></h2>
                <p v-if="categoria.esempi.length > 1" class="sp-scelta">Scegli uno degli esempi.</p>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="esempio in categoria.esempi" :key="esempio.clientId">
              <td class="sp-pasto">
                <h3 class="sp-pasto-nome">
                  <span>{{ esempio.nome }}</span>
                  <span v-if="esempio.righe.length" class="sp-kcal">{{ intero.format(kcal(esempio.righe)) }} kcal</span>
                </h3>
                <ul class="sp-righe">
                  <li v-for="riga in esempio.righe" :key="riga.clientId" class="sp-riga">
                    <span class="sp-alimento">{{ nomeAlimentoLeggibile(riga.nome) }}</span>
                    <span class="sp-puntini" aria-hidden="true" />
                    <span class="sp-grammi">{{ grammi(riga) }}</span>
                  </li>
                </ul>
                <p v-if="esempio.nota" class="sp-nota">{{ esempio.nota }}</p>
              </td>
            </tr>
          </tbody>
        </table>
      </template>

      <footer class="sp-piede">Stampato il {{ stampatoIl }}</footer>
    </article>
  </Teleport>
</template>

<style scoped>
.stampa-piano {
  --sp-inchiostro: #17302e;
  --sp-tenue: #5a6e6b;
  --sp-filetto: #cfdcd8;
  --sp-accento: #087f74;
  --sp-puntini: #a9bab6;
  color: var(--sp-inchiostro);
  background: #fff;
  font-family: 'Figtree', system-ui, sans-serif;
  font-size: 10.5pt;
  line-height: 1.45;
}

.sp-testata {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 6pt;
  border-bottom: 0.75pt solid var(--sp-filetto);
  color: var(--sp-tenue);
  font-size: 9pt;
}
.sp-marchio {
  display: inline-flex;
  align-items: center;
  gap: 6pt;
  font-family: 'Fraunces', Georgia, serif;
  font-weight: 600;
  font-size: 12pt;
  color: var(--sp-accento);
}

.sp-logo {
  width: 18pt;
  height: 18pt;
  border-radius: 3pt;
}

.sp-titolo {
  margin: 18pt 0 8pt;
  font-family: 'Fraunces', Georgia, serif;
  font-style: italic;
  font-weight: 500;
  font-size: 24pt;
  line-height: 1.15;
}

.sp-dati {
  display: flex;
  flex-wrap: wrap;
  gap: 4pt 24pt;
  margin: 0 0 18pt;
  padding-bottom: 12pt;
  border-bottom: 0.75pt solid var(--sp-filetto);
}
.sp-dati dt {
  font-size: 8.5pt;
  color: var(--sp-tenue);
}
.sp-dati dd {
  margin: 0;
  font-weight: 600;
}

.sp-sezione + .sp-sezione {
  margin-top: 16pt;
}
/* Giorno (o pasto degli esempi) come tabella: il <thead> col titolo si ripete in cima a ogni foglio su cui il
   blocco continua, così un foglio non parte mai da un pasto senza dire di che giorno è (verifica del 28/11/2026).
   Si spezza solo tra un pasto e l'altro. Tenere il giorno intero con break-inside: avoid non bastava: un giorno
   più lungo di un foglio veniva spinto al foglio dopo, lasciando vuoto il primo, e poi si spezzava lo stesso. */
.sp-blocco {
  width: 100%;
  table-layout: fixed;
  /* Bordi separati: con "collapse" il filetto del pasto mandato al foglio dopo restava anche in fondo al foglio prima. */
  border-collapse: separate;
  border-spacing: 0;
}
/* Solo il th: il padding delle celle dei pasti lo dà .sp-pasto, che un ".sp-blocco td" scavalcherebbe. */
.sp-blocco th {
  padding: 0;
  text-align: left;
  font-weight: inherit;
}
.sp-blocco td {
  text-align: left;
  vertical-align: top;
}
.sp-blocco tr {
  break-inside: avoid;
}
.sp-giorno {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 0 0 6pt;
  font-family: 'Fraunces', Georgia, serif;
  font-weight: 600;
  font-size: 14pt;
  color: var(--sp-accento);
  break-after: avoid;
}
.sp-kcal {
  font-family: 'Figtree', system-ui, sans-serif;
  font-weight: 500;
  font-size: 9pt;
  color: var(--sp-tenue);
  font-variant-numeric: tabular-nums;
}

.sp-pasto {
  padding: 6pt 0 7pt;
  border-top: 0.5pt solid var(--sp-filetto);
  break-inside: avoid;
}
.sp-pasto-nome {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 0 0 3pt;
  font-size: 10.5pt;
  font-weight: 700;
}

.sp-righe {
  margin: 0;
  padding: 0;
  list-style: none;
}
.sp-riga {
  display: flex;
  align-items: baseline;
  gap: 4pt;
}
/* Puntini di guida fatti di caratteri, tagliati alla larghezza disponibile. Non un bordo "dotted", che la stampa
   dalla finestra di Chrome trasformava in trattini (prova di Andrea del 28/11/2026), né uno sfondo a gradiente o
   SVG, che Chrome mette nel PDF come immagine su ogni riga (file quattro volte più pesante). */
.sp-puntini {
  flex: 1;
  min-width: 12pt;
  /* La fila di caratteri non deve contare nella larghezza: allargherebbe la tabella e Chrome rimpicciolirebbe il foglio. */
  contain: inline-size;
  overflow: hidden;
  white-space: nowrap;
  color: var(--sp-puntini);
  font-size: 8pt;
  letter-spacing: 1.6pt;
}
.sp-puntini::before {
  content: '................................................................................................................................................................................................................................';
}
.sp-grammi {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.sp-nota {
  margin: 4pt 0 0;
  font-style: italic;
  color: var(--sp-tenue);
  white-space: pre-line;
}
.sp-scelta {
  margin: -2pt 0 4pt;
  font-size: 9pt;
  color: var(--sp-tenue);
}
.sp-vuoto {
  color: var(--sp-tenue);
}

.sp-tabella {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 14pt;
  font-variant-numeric: tabular-nums;
}
.sp-tabella th,
.sp-tabella td {
  padding: 5pt 6pt;
  border-bottom: 0.5pt solid var(--sp-filetto);
  text-align: right;
}
.sp-tabella th:first-child {
  text-align: left;
}
.sp-tabella thead th {
  font-size: 8.5pt;
  font-weight: 600;
  color: var(--sp-tenue);
}
.sp-tabella tbody th {
  font-weight: 700;
}

.sp-piede {
  margin-top: 20pt;
  padding-top: 6pt;
  border-top: 0.75pt solid var(--sp-filetto);
  font-size: 8.5pt;
  color: var(--sp-tenue);
}
</style>
