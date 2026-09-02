# Dettaglio paziente — sezione "Andamento" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aggiungere a `PazienteDettaglioView.vue` una sezione "Andamento" con 3 grafici a linee (peso, BMI, % grasso corporeo) sullo storico delle visite del paziente, ciascuno con un indicatore di variazione rispetto alla visita precedente.

**Architecture:** Nuovo endpoint di sola lettura `GET /pazienti/{id}/visite` (backend) che espone lo storico visite ordinato cronologicamente. Nel frontend, una funzione pura `prepareAndamento` trasforma la lista di visite in dati pronti per il grafico (testabile senza montare componenti), consumata da un componente `AndamentoChart.vue` riutilizzabile (una card per metrica) basato sul componente `ChartLine` di shadcn-vue (Unovis), a sua volta wired in `PazienteDettaglioView.vue`.

**Tech Stack:** Spring Boot 3.3.4 / Java 21 (backend), Vue 3 + TypeScript + Vite + Tailwind CSS v4 + shadcn-vue/Unovis (frontend).

**Spec:** [`docs/superpowers/specs/2026-09-02-dettaglio-paziente-andamento-design.md`](../specs/2026-09-02-dettaglio-paziente-andamento-design.md)

## Global Constraints

- **Mai `git commit`, in nessun task.** La regola di questo workspace è che i commit li fa sempre e solo Andrea. Ogni step "Commit" in questo piano significa `git add` sui file del task **seguito da un messaggio all'utente**, non `git commit`. Se la skill di esecuzione (`subagent-driven-development`) normalmente fa commit per calcolare i diff tra task, va adattata come già fatto nella sessione Plicometria del 1° settembre 2026: solo `git add`, pacchetti di revisione via `git diff --cached` sui file del task.
- Backend: dopo ogni modifica, eseguire `mvn test` da `backend/` con `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` impostato prima — riportare l'esito reale (vedi `backend/CLAUDE.md`).
- Frontend: dopo ogni modifica, eseguire `npx vitest run` e `npx tsc --noEmit` da `frontend-professionisti/` — riportare l'esito reale.
- Non avviare l'applicazione per verifiche manuali (backend o frontend): è responsabilità di Andrea, per convenzione di questo progetto.
- Se un endpoint o il modello dati cambia, aggiornare `wiki/api-contracts.md` nello stesso task (richiesto da `backend/CLAUDE.md`).

---

### Task 1: Backend — `GET /pazienti/{id}/visite`

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaResponse.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRepository.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaRepository.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`
- Modify: `wiki/api-contracts.md`

**Interfaces:**
- Produces: `VisitaResponse` (record) con factory statica `VisitaResponse.da(Visita visita, Plicometria plicometriaONull)`; campi `id, dataVisita, altezzaCm, pesoKg, bmi, whr, whtr, mamcCm, circonferenze (VisitaResponse.Circonferenze), plicometria (VisitaResponse.PlicometriaResponse, nullable)`.
- Produces: `PazienteService.visite(UUID professionistaId, UUID pazienteId): List<VisitaResponse>` — 404 (`PazienteNonTrovatoException`) se il paziente non esiste o appartiene a un altro professionista, altrimenti lista ordinata per `dataVisita` crescente.
- Produces: `GET /pazienti/{id}/visite` → `200` con `List<VisitaResponse>` (ruolo PROFESSIONISTA, stesso controllo di ownership di `GET /pazienti/{id}`).

- [ ] **Step 1: Scrivere i test falliti per il repository (ordinamento cronologico)**

Aggiungere a `backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java` (dopo il test esistente `salvaERitrovaPerPaziente`):

```java
    @Test
    void findAllByPazienteIdOrderByDataVisitaAscRestituisceInOrdineCronologico() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("visite-ordine-prof@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "visite-ordine-luca@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        Visita piuRecente = new Visita(paziente.getId(), LocalDate.of(2026, 8, 1), 178, new BigDecimal("77.5"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS);
        Visita piuVecchia = new Visita(paziente.getId(), LocalDate.of(2026, 6, 1), 178, new BigDecimal("80.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS);
        // Salvate in ordine inverso rispetto alla data, per verificare che sia la query a ordinare, non l'inserimento.
        visitaRepository.save(piuRecente);
        visitaRepository.save(piuVecchia);

        List<Visita> visite = visitaRepository.findAllByPazienteIdOrderByDataVisitaAsc(paziente.getId());

        assertThat(visite).hasSize(2);
        assertThat(visite.get(0).getDataVisita()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(visite.get(1).getDataVisita()).isEqualTo(LocalDate.of(2026, 8, 1));
    }
```

- [ ] **Step 2: Verificare che il test fallisca**

Run (da `backend/`, con `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` impostato):
```powershell
mvn test -Dtest=VisitaRepositoryTest
```
Expected: FAIL — compilazione fallita, `findAllByPazienteIdOrderByDataVisitaAsc` non esiste su `VisitaRepository`.

- [ ] **Step 3: Aggiungere il metodo di query al repository**

In `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRepository.java`, aggiungere accanto a `findAllByPazienteId`:

```java
    List<Visita> findAllByPazienteIdOrderByDataVisitaAsc(UUID pazienteId);
```

- [ ] **Step 4: Rieseguire il test e verificare che passi**

```powershell
mvn test -Dtest=VisitaRepositoryTest
```
Expected: PASS.

- [ ] **Step 5: Aggiungere `findByVisitaId` a `PlicometriaRepository`**

Sostituire il contenuto di `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaRepository.java`:

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlicometriaRepository extends JpaRepository<Plicometria, UUID> {
    Optional<Plicometria> findByVisitaId(UUID visitaId);
}
```

Nessun test dedicato per questo singolo metodo derivato (semplice query Spring Data) — è coperto dai test dell'endpoint allo Step 8, coerente con l'assenza di un `PlicometriaRepositoryTest` dedicato nel progetto oggi.

- [ ] **Step 6: Creare `VisitaResponse`**

Creare `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaResponse.java`:

```java
package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VisitaResponse(
        UUID id,
        LocalDate dataVisita,
        Integer altezzaCm,
        BigDecimal pesoKg,
        BigDecimal bmi,
        BigDecimal whr,
        BigDecimal whtr,
        BigDecimal mamcCm,
        Circonferenze circonferenze,
        PlicometriaResponse plicometria
) {
    public static VisitaResponse da(Visita visita, Plicometria plicometria) {
        return new VisitaResponse(
                visita.getId(),
                visita.getDataVisita(),
                visita.getAltezzaCm(),
                visita.getPesoKg(),
                visita.getBmi(),
                visita.getWhr(),
                visita.getWhtr(),
                visita.getMamcCm(),
                Circonferenze.da(visita),
                plicometria != null ? PlicometriaResponse.da(plicometria) : null);
    }

    public record Circonferenze(
            BigDecimal vitaCm,
            BigDecimal fianchiCm,
            BigDecimal addomeCm,
            BigDecimal braccioRilassatoCm,
            BigDecimal cosciaCm,
            BigDecimal polpaccioCm,
            BigDecimal colloCm,
            BigDecimal toraceCm,
            BigDecimal braccioContrattoCm,
            BigDecimal avambraccioCm,
            BigDecimal cavigliaCm
    ) {
        public static Circonferenze da(Visita visita) {
            return new Circonferenze(
                    visita.getCirconferenzaVitaCm(),
                    visita.getCirconferenzaFianchiCm(),
                    visita.getCirconferenzaAddomeCm(),
                    visita.getCirconferenzaBraccioRilassatoCm(),
                    visita.getCirconferenzaCosciaCm(),
                    visita.getCirconferenzaPolpaccioCm(),
                    visita.getCirconferenzaColloCm(),
                    visita.getCirconferenzaToraceCm(),
                    visita.getCirconferenzaBraccioContrattoCm(),
                    visita.getCirconferenzaAvambraccioCm(),
                    visita.getCirconferenzaCavigliaCm());
        }
    }

    public record PlicometriaResponse(
            BigDecimal percentualeGrassoCorporeo,
            BigDecimal massaGrassaKg,
            BigDecimal massaMagraKg,
            BigDecimal fmi,
            BigDecimal ffmi
    ) {
        public static PlicometriaResponse da(Plicometria plicometria) {
            return new PlicometriaResponse(
                    plicometria.getPercentualeGrasso(),
                    plicometria.getMassaGrassaKg(),
                    plicometria.getMassaMagraKg(),
                    plicometria.getFmi(),
                    plicometria.getFfmi());
        }
    }
}
```

- [ ] **Step 7: Scrivere i test falliti per l'endpoint**

Aggiungere a `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java` (in fondo alla classe, prima dell'ultima `}`).

Questo file non costruisce oggi nessun `Visita`/`Plicometria` direttamente (le visite esistenti nei test passano sempre per il body JSON di `POST /pazienti`) — servono quindi import non ancora presenti. Aggiungere in cima al file, vicino agli import esistenti:
- `import java.math.BigDecimal;` (vicino a `import java.time.Duration;`)
- `import java.util.UUID;` (vicino a `import java.util.List;`)
- `import org.hamcrest.Matchers;` (vicino a `import org.junit.jupiter.api.Test;`)

Poi aggiungere i test:

```java
    @Test
    void visiteRestituisceListaVuotaSeIlPazienteNonHaVisite() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-visite-vuote@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-visite-vuote@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(get("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void visiteRestituisceInOrdineCronologicoConPlicometriaSoloDoveApplicata() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-visite-ordine@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-visite-ordine@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        Visita piuRecente = new Visita(paziente.getId(), LocalDate.of(2026, 8, 1), 178, new BigDecimal("77.5"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS);
        piuRecente.setBmi(new BigDecimal("24.4"));
        Visita piuVecchia = new Visita(paziente.getId(), LocalDate.of(2026, 6, 1), 178, new BigDecimal("80.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS);
        piuVecchia.setBmi(new BigDecimal("25.2"));
        visitaRepository.save(piuRecente);
        visitaRepository.save(piuVecchia);

        plicometriaRepository.save(new Plicometria(piuRecente.getId(), ProtocolloPlicometrico.JACKSON_POLLOCK_3, "v1", 36,
                null, null, null,
                null, null, new BigDecimal("12.5"),
                null, null, null,
                new BigDecimal("15.0"), new BigDecimal("14.0"), null,
                new BigDecimal("41.5"), new BigDecimal("1.06"), new BigDecimal("18.2"),
                new BigDecimal("14.1"), new BigDecimal("63.4"), new BigDecimal("4.4"), new BigDecimal("20.1"),
                false));

        mockMvc.perform(get("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dataVisita").value("2026-06-01"))
                .andExpect(jsonPath("$[0].bmi").value(25.2))
                .andExpect(jsonPath("$[0].plicometria").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[1].dataVisita").value("2026-08-01"))
                .andExpect(jsonPath("$[1].bmi").value(24.4))
                .andExpect(jsonPath("$[1].plicometria.percentualeGrassoCorporeo").value(18.2));
    }

    @Test
    void visiteDiPazienteDiAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-visite-a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-visite-b@example.com", "hash", "B", "B"));
        Paziente pazienteDiB = pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "RSSMRA80A01H501U", "diB-visite@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(get("/pazienti/" + pazienteDiB.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void visiteSenzaAutenticazioneRestituisce401() throws Exception {
        mockMvc.perform(get("/pazienti/" + UUID.randomUUID() + "/visite"))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 8: Verificare che i test falliscano**

```powershell
mvn test -Dtest=PazienteControllerTest
```
Expected: FAIL — compilazione fallita, nessun endpoint `GET /pazienti/{id}/visite`.

- [ ] **Step 9: Implementare il metodo di servizio e l'endpoint**

In `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`:

1. Sostituire il blocco dei campi e il costruttore esistenti (Spring inietta `PlicometriaRepository` automaticamente, è già un bean usato da `PlicometriaService`):

```java
    private final PazienteRepository pazienteRepository;
    private final VisitaRepository visitaRepository;
    private final ProfessionistaRepository professionistaRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final PlicometriaService plicometriaService;
    private final PlicometriaRepository plicometriaRepository;

    public PazienteService(PazienteRepository pazienteRepository,
                            VisitaRepository visitaRepository,
                            ProfessionistaRepository professionistaRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender,
                            PasswordEncoder passwordEncoder,
                            PlicometriaService plicometriaService,
                            PlicometriaRepository plicometriaRepository) {
        this.pazienteRepository = pazienteRepository;
        this.visitaRepository = visitaRepository;
        this.professionistaRepository = professionistaRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.plicometriaService = plicometriaService;
        this.plicometriaRepository = plicometriaRepository;
    }
```

2. Aggiungere il metodo (accanto a `dettaglio(...)`):

```java
    public List<VisitaResponse> visite(UUID professionistaId, UUID pazienteId) {
        dettaglio(professionistaId, pazienteId);
        return visitaRepository.findAllByPazienteIdOrderByDataVisitaAsc(pazienteId).stream()
                .map(v -> VisitaResponse.da(v, plicometriaRepository.findByVisitaId(v.getId()).orElse(null)))
                .toList();
    }
```

In `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java`, aggiungere accanto a `dettaglio(...)`:

```java
    @GetMapping("/{id}/visite")
    public List<VisitaResponse> visite(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return pazienteService.visite(professionistaId, id);
    }
```

- [ ] **Step 10: Rieseguire i test e verificare che passino**

```powershell
mvn test
```
Expected: PASS, tutti i test verdi (nessuna regressione sugli altri test di `PazienteControllerTest`/`VisitaRepositoryTest`).

- [ ] **Step 11: Aggiornare `wiki/api-contracts.md`**

Aggiungere una riga alla tabella degli endpoint, dopo la riga di `GET /pazienti/{id}`:

```
| GET | `/pazienti/{id}/visite` | PROFESSIONISTA | Storico visite del paziente, ordinato per `dataVisita` crescente; ogni voce include `plicometria` annidata (nullable, presente solo se la plicometria è stata eseguita per quella visita); 404 se il paziente appartiene a un altro professionista |
```

- [ ] **Step 12: Staging (nessun commit)**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaResponse.java \
        backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRepository.java \
        backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaRepository.java \
        backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java \
        backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java \
        backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java \
        backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java \
        wiki/api-contracts.md
```

Non eseguire `git commit` (vedi Global Constraints).

---

### Task 2: Frontend — livello dati e `prepareAndamento`

**Files:**
- Modify: `frontend-professionisti/src/api/pazienti.ts`
- Modify: `frontend-professionisti/src/api/pazienti.spec.ts`
- Create: `frontend-professionisti/src/utils/andamento.ts`
- Create: `frontend-professionisti/src/utils/andamento.spec.ts`

**Interfaces:**
- Consumes: contratto JSON di `GET /pazienti/{id}/visite` prodotto dal Task 1 (`VisitaResponse` → stesso shape lato TypeScript).
- Produces: `Visita`, `Circonferenze`, `Plicometria` (interfacce TS), `visite(id: string): Promise<Visita[]>` in `api/pazienti.ts`.
- Produces: `PuntoAndamento { data: string; valore: number }`, `Andamento { punti: PuntoAndamento[]; ultimo: number | null; delta: number | null }`, `AndamentoPaziente { peso: Andamento; bmi: Andamento; percentualeGrassoCorporeo: Andamento }`, `prepareAndamento(visite: Visita[]): AndamentoPaziente` in `utils/andamento.ts` — usati dal Task 3/4.

- [ ] **Step 1: Scrivere i test falliti per `api/pazienti.ts`**

Aggiungere a `frontend-professionisti/src/api/pazienti.spec.ts`, dentro `describe('api/pazienti', ...)`, dopo il test `cerca costruisce la query string...`:

```ts
  it('visite chiama GET /pazienti/{id}/visite', async () => {
    const visiteEsempio = [
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ]
    vi.mocked(apiRequest).mockResolvedValue(visiteEsempio)

    const risultato = await visite('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/visite')
    expect(risultato).toEqual(visiteEsempio)
  })
```

Aggiornare l'import in cima al file: `import { lista, dettaglio, crea, invita, archivia, deArchivia, cerca, visite } from './pazienti'`.

- [ ] **Step 2: Verificare che il test fallisca**

```powershell
npx vitest run pazienti.spec.ts
```
Expected: FAIL — `visite` non esportato da `./pazienti`.

- [ ] **Step 3: Implementare tipi e funzione in `api/pazienti.ts`**

Aggiungere, dopo l'interfaccia `PaginaPazienti` (o in un punto simile vicino alle altre interfacce):

```ts
export interface Circonferenze {
  vitaCm: number | null
  fianchiCm: number | null
  addomeCm: number | null
  braccioRilassatoCm: number | null
  cosciaCm: number | null
  polpaccioCm: number | null
  colloCm: number | null
  toraceCm: number | null
  braccioContrattoCm: number | null
  avambraccioCm: number | null
  cavigliaCm: number | null
}

export interface Plicometria {
  percentualeGrassoCorporeo: number
  massaGrassaKg: number
  massaMagraKg: number
  fmi: number
  ffmi: number
}

export interface Visita {
  id: string
  dataVisita: string
  altezzaCm: number
  pesoKg: number
  bmi: number | null
  whr: number | null
  whtr: number | null
  mamcCm: number | null
  circonferenze: Circonferenze
  plicometria: Plicometria | null
}
```

Aggiungere, dopo la funzione `cerca(...)`:

```ts
export function visite(id: string): Promise<Visita[]> {
  return apiRequest<Visita[]>(`/pazienti/${id}/visite`)
}
```

- [ ] **Step 4: Rieseguire il test e verificare che passi**

```powershell
npx vitest run pazienti.spec.ts
```
Expected: PASS.

- [ ] **Step 5: Scrivere i test falliti per `prepareAndamento`**

Creare `frontend-professionisti/src/utils/andamento.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { prepareAndamento } from './andamento'
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
    plicometria: null,
    ...overrides,
  }
}

describe('prepareAndamento', () => {
  it('con nessuna visita restituisce punti vuoti e ultimo/delta null per tutte le metriche', () => {
    const risultato = prepareAndamento([])

    expect(risultato.peso).toEqual({ punti: [], ultimo: null, delta: null })
    expect(risultato.bmi).toEqual({ punti: [], ultimo: null, delta: null })
    expect(risultato.percentualeGrassoCorporeo).toEqual({ punti: [], ultimo: null, delta: null })
  })

  it('con una sola visita valorizza ultimo ma non il delta', () => {
    const risultato = prepareAndamento([visita({ pesoKg: 80, bmi: 25.2 })])

    expect(risultato.peso).toEqual({ punti: [{ data: '2026-01-01', valore: 80 }], ultimo: 80, delta: null })
    expect(risultato.bmi.ultimo).toBe(25.2)
    expect(risultato.bmi.delta).toBeNull()
  })

  it('con più visite calcola il delta tra le ultime due (in ordine di arrivo)', () => {
    const risultato = prepareAndamento([
      visita({ dataVisita: '2026-06-01', pesoKg: 80 }),
      visita({ dataVisita: '2026-08-01', pesoKg: 77.5 }),
    ])

    expect(risultato.peso.ultimo).toBe(77.5)
    expect(risultato.peso.delta).toBeCloseTo(-2.5)
  })

  it('percentualeGrassoCorporeo ignora le visite senza plicometria', () => {
    const risultato = prepareAndamento([
      visita({ dataVisita: '2026-06-01', plicometria: null }),
      visita({
        dataVisita: '2026-08-01',
        plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
      }),
    ])

    expect(risultato.percentualeGrassoCorporeo.punti).toEqual([{ data: '2026-08-01', valore: 18.2 }])
    expect(risultato.percentualeGrassoCorporeo.ultimo).toBe(18.2)
    expect(risultato.percentualeGrassoCorporeo.delta).toBeNull()
  })

  it('percentualeGrassoCorporeo è vuoto se nessuna visita ha la plicometria', () => {
    const risultato = prepareAndamento([visita({ plicometria: null }), visita({ dataVisita: '2026-02-01', plicometria: null })])

    expect(risultato.percentualeGrassoCorporeo).toEqual({ punti: [], ultimo: null, delta: null })
  })

  it('ignora visite senza bmi valorizzato nel calcolo dei punti per BMI', () => {
    const risultato = prepareAndamento([
      visita({ dataVisita: '2026-01-01', bmi: null }),
      visita({ dataVisita: '2026-02-01', bmi: 24.0 }),
    ])

    expect(risultato.bmi.punti).toEqual([{ data: '2026-02-01', valore: 24.0 }])
    expect(risultato.bmi.delta).toBeNull()
  })
})
```

- [ ] **Step 6: Verificare che il test fallisca**

```powershell
npx vitest run andamento.spec.ts
```
Expected: FAIL — il modulo `./andamento` non esiste.

- [ ] **Step 7: Implementare `utils/andamento.ts`**

Creare `frontend-professionisti/src/utils/andamento.ts`:

```ts
import type { Visita } from '@/api/pazienti'

export interface PuntoAndamento {
  data: string
  valore: number
}

export interface Andamento {
  punti: PuntoAndamento[]
  ultimo: number | null
  delta: number | null
}

export interface AndamentoPaziente {
  peso: Andamento
  bmi: Andamento
  percentualeGrassoCorporeo: Andamento
}

function costruisciAndamento(visite: Visita[], estraiValore: (visita: Visita) => number | null): Andamento {
  const punti = visite
    .map((v) => ({ data: v.dataVisita, valore: estraiValore(v) }))
    .filter((p): p is PuntoAndamento => p.valore !== null)

  const ultimo = punti.length > 0 ? punti[punti.length - 1].valore : null
  const delta = punti.length >= 2 ? punti[punti.length - 1].valore - punti[punti.length - 2].valore : null

  return { punti, ultimo, delta }
}

export function prepareAndamento(visite: Visita[]): AndamentoPaziente {
  return {
    peso: costruisciAndamento(visite, (v) => v.pesoKg),
    bmi: costruisciAndamento(visite, (v) => v.bmi),
    percentualeGrassoCorporeo: costruisciAndamento(visite, (v) => v.plicometria?.percentualeGrassoCorporeo ?? null),
  }
}
```

- [ ] **Step 8: Rieseguire i test e verificare che passino**

```powershell
npx vitest run andamento.spec.ts
npx tsc --noEmit
```
Expected: PASS, nessun errore TypeScript.

- [ ] **Step 9: Staging (nessun commit)**

```bash
git add frontend-professionisti/src/api/pazienti.ts \
        frontend-professionisti/src/api/pazienti.spec.ts \
        frontend-professionisti/src/utils/andamento.ts \
        frontend-professionisti/src/utils/andamento.spec.ts
```

---

### Task 3: Frontend — componente `AndamentoChart.vue` (shadcn-vue Chart/Unovis)

**Files:**
- Modify: `frontend-professionisti/src/assets/main.css`
- Create (via CLI, poi ispezionato): `frontend-professionisti/src/components/ui/chart-line/` (generato da shadcn-vue)
- Create: `frontend-professionisti/src/components/pazienti/AndamentoChart.vue`
- Create: `frontend-professionisti/src/components/pazienti/AndamentoChart.spec.ts`

**Interfaces:**
- Consumes: `Andamento` da `utils/andamento.ts` (Task 2).
- Produces: `AndamentoChart.vue`, props `{ titolo: string; unita: string; andamento: Andamento; colore: string; decimali?: number }` — consumato dal Task 4.

- [ ] **Step 1: Installare il componente chart-line di shadcn-vue**

Da `frontend-professionisti/`:

```powershell
npx shadcn-vue@latest add chart-line
```

Questo genera file sotto `src/components/ui/chart-line/` (nome esatto e struttura decisi dal CLI). **Prima di procedere**, aprire il file del componente generato e annotare i nomi esatti delle prop esportate (tipicamente `data`, `index`, `categories`, `colors`, più eventuali prop di visibilità come `showLegend`/`showTooltip`/`showGridLine`). Se i nomi differiscono da quelli usati negli step successivi di questo task, adattare il codice di `AndamentoChart.vue` di conseguenza — quanto scritto qui sotto è il contratto atteso in base alla documentazione pubblica, non verificato contro il codice sorgente effettivo.

- [ ] **Step 2: Aggiungere i token colore per i grafici**

In `frontend-professionisti/src/assets/main.css`, dentro il blocco `:root { ... }` esistente, aggiungere subito dopo la riga `--warn-fg: #A8641F;`:

```css
  --chart-1: var(--green);
  --chart-2: var(--sage);
  --chart-3: var(--warn-fg);
```

(Riuso di colori brand già esistenti — nessun nuovo colore inventato: verde per il peso, verde chiaro/sage per il BMI, ambra per la % di grasso corporeo, così i tre grafici restano distinguibili senza uscire dalla palette Hexis.)

- [ ] **Step 3: Scrivere i test falliti per `AndamentoChart.vue`**

Creare `frontend-professionisti/src/components/pazienti/AndamentoChart.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AndamentoChart from './AndamentoChart.vue'
import type { Andamento } from '@/utils/andamento'

function montaConAndamento(andamento: Andamento) {
  return mount(AndamentoChart, {
    props: { titolo: 'Peso', unita: 'kg', andamento, colore: 'var(--chart-1)' },
    global: { stubs: { ChartLine: true } },
  })
}

describe('AndamentoChart', () => {
  it('mostra il titolo passato', () => {
    const wrapper = montaConAndamento({ punti: [], ultimo: null, delta: null })

    expect(wrapper.text()).toContain('Peso')
  })

  it('mostra "Nessun dato disponibile" senza punti', () => {
    const wrapper = montaConAndamento({ punti: [], ultimo: null, delta: null })

    expect(wrapper.text()).toContain('Nessun dato disponibile')
  })

  it('mostra l\'ultimo valore e "Prima visita" quando il delta è null', () => {
    const wrapper = montaConAndamento({ punti: [{ data: '2026-01-01', valore: 80 }], ultimo: 80, delta: null })

    expect(wrapper.text()).toContain('80,0')
    expect(wrapper.text()).toContain('Prima visita')
  })

  it('mostra il delta negativo con il segno e senza "Prima visita"', () => {
    const wrapper = montaConAndamento({
      punti: [{ data: '2026-06-01', valore: 80 }, { data: '2026-08-01', valore: 77.5 }],
      ultimo: 77.5,
      delta: -2.5,
    })

    expect(wrapper.text()).toContain('77,5')
    expect(wrapper.text()).toContain('2,5')
    expect(wrapper.text()).not.toContain('Prima visita')
  })
})
```

- [ ] **Step 4: Verificare che il test fallisca**

```powershell
npx vitest run AndamentoChart.spec.ts
```
Expected: FAIL — il componente `./AndamentoChart.vue` non esiste.

- [ ] **Step 5: Implementare `AndamentoChart.vue`**

Creare `frontend-professionisti/src/components/pazienti/AndamentoChart.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { ChartLine } from '@/components/ui/chart-line'
import { ArrowDown, ArrowUp } from '@lucide/vue'
import type { Andamento } from '@/utils/andamento'

const props = withDefaults(
  defineProps<{
    titolo: string
    unita: string
    andamento: Andamento
    colore: string
    decimali?: number
  }>(),
  { decimali: 1 },
)

function formattaDataItaliana(dataIso: string): string {
  const [anno, mese, giorno] = dataIso.split('-')
  return `${giorno}/${mese}/${anno.slice(2)}`
}

const datiGrafico = computed(() =>
  props.andamento.punti.map((p) => ({
    data: formattaDataItaliana(p.data),
    valore: p.valore,
  })),
)

function formatta(valore: number): string {
  return valore.toLocaleString('it-IT', { minimumFractionDigits: props.decimali, maximumFractionDigits: props.decimali })
}
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle class="text-xs font-bold uppercase tracking-wide text-(--fg3)">{{ titolo }}</CardTitle>
    </CardHeader>
    <CardContent>
      <div v-if="andamento.punti.length === 0" class="text-sm text-(--fg4)">Nessun dato disponibile</div>
      <template v-else>
        <div class="flex items-baseline gap-2">
          <span class="text-3xl font-semibold text-(--fg)">{{ formatta(andamento.ultimo as number) }}</span>
          <span v-if="unita" class="text-sm text-(--fg3)">{{ unita }}</span>
          <span
            v-if="andamento.delta !== null"
            class="flex items-center gap-0.5 text-sm font-medium"
            :class="andamento.delta < 0 ? 'text-(--green)' : andamento.delta > 0 ? 'text-(--danger)' : 'text-(--fg3)'"
          >
            <ArrowDown v-if="andamento.delta < 0" :size="14" />
            <ArrowUp v-else-if="andamento.delta > 0" :size="14" />
            {{ formatta(Math.abs(andamento.delta)) }}{{ unita ? ' ' + unita : '' }}
          </span>
          <span v-else class="text-sm text-(--fg3)">Prima visita</span>
        </div>
        <ChartLine
          class="mt-4 h-40"
          :data="datiGrafico"
          index="data"
          :categories="['valore']"
          :colors="[colore]"
          :show-legend="false"
        />
      </template>
    </CardContent>
  </Card>
</template>
```

- [ ] **Step 6: Rieseguire i test e verificare che passino**

```powershell
npx vitest run AndamentoChart.spec.ts
npx tsc --noEmit
```
Expected: PASS. Se `tsc --noEmit` segnala che le prop di `ChartLine` non corrispondono (nomi diversi da `data`/`index`/`categories`/`colors`/`show-legend`), correggerle in base al file reale generato allo Step 1 e rieseguire.

- [ ] **Step 7: Staging (nessun commit)**

```bash
git add frontend-professionisti/src/assets/main.css \
        frontend-professionisti/src/components/ui/chart-line \
        frontend-professionisti/src/components/pazienti/AndamentoChart.vue \
        frontend-professionisti/src/components/pazienti/AndamentoChart.spec.ts \
        frontend-professionisti/package.json \
        frontend-professionisti/package-lock.json
```

(Il comando `npx shadcn-vue@latest add chart-line` può modificare `package.json`/`package-lock.json` se aggiunge dipendenze — es. `@unovis/vue` — includerli nello staging se effettivamente modificati.)

---

### Task 4: Frontend — wiring in `PazienteDettaglioView.vue`

**Files:**
- Modify: `frontend-professionisti/src/views/pazienti/PazienteDettaglioView.vue`
- Modify: `frontend-professionisti/src/views/pazienti/PazienteDettaglioView.spec.ts`

**Interfaces:**
- Consumes: `visite(id)` da `api/pazienti.ts` (Task 2), `prepareAndamento` da `utils/andamento.ts` (Task 2), `AndamentoChart.vue` (Task 3).

- [ ] **Step 1: Scrivere i test falliti per la sezione "Andamento"**

In `frontend-professionisti/src/views/pazienti/PazienteDettaglioView.spec.ts`:

1. Aggiungere `beforeEach` all'import da `vitest` (riga 1): `import { describe, expect, it, vi, beforeEach } from 'vitest'`.
2. Subito dentro `describe('PazienteDettaglioView', () => { ... })`, prima del primo `it(...)`, aggiungere:

```ts
  beforeEach(() => {
    vi.mocked(pazientiApi.visite).mockResolvedValue([])
  })
```

(Necessario perché la view ora chiama anche `visite(id)` al mount: senza un default, i test esistenti — che non lo mockano esplicitamente — riceverebbero `undefined` dalla funzione auto-mockata e andrebbero in errore. I test esistenti restano altrimenti invariati.)

3. Aggiungere, in fondo al file, prima dell'ultima chiusura `})` di `describe`:

```ts
  it('mostra la sezione Andamento con i dati delle visite', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
      {
        id: 'v2', dataVisita: '2026-08-01', altezzaCm: 178, pesoKg: 77.5, bmi: 24.4, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartLine: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Andamento')
    expect(wrapper.text()).toContain('Peso')
    expect(wrapper.text()).toContain('77,5')
    expect(wrapper.text()).toContain('BMI')
  })

  it('nasconde la card % Grasso corporeo se nessuna visita ha la plicometria', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartLine: true } },
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('Grasso corporeo')
  })

  it('mostra la card % Grasso corporeo quando almeno una visita ha la plicometria', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartLine: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Grasso corporeo')
  })

  it('mostra un errore se lo storico visite non si carica, senza bloccare l\'anagrafica', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockRejectedValue(new Error('500'))
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('Non è stato possibile caricare lo storico visite.')
  })
```

- [ ] **Step 2: Verificare che i nuovi test falliscano**

```powershell
npx vitest run PazienteDettaglioView.spec.ts
```
Expected: FAIL — nessuna sezione "Andamento" nel markup attuale, `pazientiApi.visite` non ancora chiamata dalla view.

- [ ] **Step 3: Implementare la sezione "Andamento" in `PazienteDettaglioView.vue`**

Sostituire l'intero contenuto di `frontend-professionisti/src/views/pazienti/PazienteDettaglioView.vue`:

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, visite as caricaVisite, type Paziente, type Visita } from '@/api/pazienti'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import AndamentoChart from '@/components/pazienti/AndamentoChart.vue'
import { prepareAndamento } from '@/utils/andamento'

const route = useRoute()
const paziente = ref<Paziente | null>(null)
const erroreCaricamento = ref<string | null>(null)
const invitoInCorso = ref(false)
const visite = ref<Visita[]>([])
const erroreVisite = ref(false)

const andamento = computed(() => prepareAndamento(visite.value))

async function carica() {
  try {
    paziente.value = await dettaglio(route.params.id as string)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      erroreCaricamento.value = 'Paziente non trovato.'
    } else {
      erroreCaricamento.value = 'Non è stato possibile caricare il paziente.'
    }
  }
}

async function caricaAndamento() {
  try {
    visite.value = await caricaVisite(route.params.id as string)
  } catch {
    erroreVisite.value = true
  }
}

async function onInvita() {
  if (!paziente.value) return
  invitoInCorso.value = true
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
    toast.success('Invito inviato.')
  } catch {
    toast.error('Non è stato possibile inviare l\'invito.')
  } finally {
    invitoInCorso.value = false
  }
}

onMounted(() => {
  carica()
  caricaAndamento()
})
</script>

<template>
  <AppShell>
    <p v-if="erroreCaricamento" class="text-(--danger)">{{ erroreCaricamento }}</p>
    <div v-else-if="paziente">
      <h1 class="font-heading text-3xl italic text-(--fg)">
        {{ paziente.nome }} {{ paziente.cognome }}
      </h1>
      <p class="text-(--fg2)">{{ paziente.email }}</p>
      <p class="text-(--fg3)">Codice fiscale: {{ paziente.codiceFiscale }}</p>
      <p class="mt-1 flex items-center gap-1.5 text-(--fg3)">Stato account: <Badge variant="secondary">{{ paziente.statoAccount }}</Badge></p>

      <Button
        v-if="paziente.statoAccount !== 'ATTIVO'"
        type="button"
        :disabled="invitoInCorso"
        class="mt-4"
        @click="onInvita"
      >
        {{ paziente.statoAccount === 'MAI_INVITATO' ? 'Invita' : 'Reinvia invito' }}
      </Button>

      <div class="mt-8">
        <h2 class="font-heading text-xl italic text-(--fg)">Andamento</h2>
        <p v-if="erroreVisite" class="mt-2 text-(--danger)">Non è stato possibile caricare lo storico visite.</p>
        <div v-else class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
          <AndamentoChart titolo="Peso" unita="kg" :andamento="andamento.peso" colore="var(--chart-1)" />
          <AndamentoChart titolo="BMI" unita="" :andamento="andamento.bmi" colore="var(--chart-2)" />
          <AndamentoChart
            v-if="andamento.percentualeGrassoCorporeo.punti.length > 0"
            titolo="% Grasso corporeo"
            unita="%"
            :andamento="andamento.percentualeGrassoCorporeo"
            colore="var(--chart-3)"
          />
        </div>
      </div>
    </div>
  </AppShell>
</template>
```

- [ ] **Step 4: Rieseguire i test e verificare che passino**

```powershell
npx vitest run PazienteDettaglioView.spec.ts
npx tsc --noEmit
```
Expected: PASS, nessun errore TypeScript.

- [ ] **Step 5: Eseguire l'intera suite frontend**

```powershell
npx vitest run
```
Expected: PASS su tutti i test toccati da questo piano; eventuali fallimenti preesistenti e scollegati (es. `LoginView.spec.ts`, vedi `wiki/stato.md`) restano tali, da non confondere con regressioni introdotte qui.

- [ ] **Step 6: Staging (nessun commit)**

```bash
git add frontend-professionisti/src/views/pazienti/PazienteDettaglioView.vue \
        frontend-professionisti/src/views/pazienti/PazienteDettaglioView.spec.ts
```

---

## Note finali per chi esegue

- Il numero di visite per paziente è oggi quasi sempre 1 (il flusso "Nuova visita" su paziente esistente non esiste ancora, fuori scope qui — vedi spec). Per una verifica manuale con più punti nel grafico, Andrea inserirà a mano righe aggiuntive in `visite` sul database `hexis` — non è compito di questo piano automatizzarlo.
- Nessuna verifica manuale in browser va fatta dall'agente (convenzione di progetto, vedi `backend/CLAUDE.md`/`wiki/stato.md`): il criterio di completamento è la suite automatica verde più `tsc --noEmit` pulito.
