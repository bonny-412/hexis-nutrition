# Piano alimentare (editor + lista) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Costruire il modello dati, il calcolo del suggerimento calorico (TDEE) e le due viste principali (editor + lista) del sotto-progetto "Piano alimentare", nelle tre modalità (Piano con pasti / Solo target macro / Esempi intercambiabili) decise in brainstorming.

**Architecture:** Nuovo package backend `pianialimentari` (entità piatte senza relazioni JPA, stesso stile di `pazienti`/`alimenti`), 5 nuove tabelle, un servizio di calcolo puro (`CalcolatoreTdee`), un controller REST con 6 endpoint. Frontend: nuovo `api/pianiAlimentari.ts`, editor condiviso (`PianoAlimentareFormView.vue`) con tre rendering distinti per modalità, lista paginata (`PianiAlimentariListView.vue`), stesso pattern di `PazientiListView.vue`/`AlimentiListView.vue`.

**Tech Stack:** Spring Boot 3.3.4 / Java 21 / PostgreSQL (Flyway) lato backend; Vue 3 + TypeScript + Tailwind v4 + shadcn-vue lato frontend.

**Spec:** [`docs/superpowers/specs/2026-09-12-piano-alimentare-design.md`](../specs/2026-09-12-piano-alimentare-design.md) — leggerlo per intero prima di iniziare, questo piano ne assume tutte le decisioni.

## Global Constraints

- **Nessun `git commit`**: fare sempre e solo `git add` alla fine di ogni task (mai `git commit`), regola assoluta del workspace — vedi `CLAUDE.md` alla radice del progetto.
- **Nessun autosave**: l'editor accumula modifiche in stato locale Vue, la persistenza avviene solo su azione esplicita "Salva" (`PUT`, sostituzione totale della struttura figlia).
- **Modalità fissata alla creazione**, mai cambiata dopo.
- **Un solo piano `ATTIVO` per paziente**: attivarne uno nuovo porta il precedente a `TERMINATO`.
- **Percentuale di aggiustamento sempre 0%** per tutti gli obiettivi visita non esclusi; `PATOLOGIA_CLINICA` e `GRAVIDANZA_ALLATTAMENTO` restano esclusi dal calcolo (nessun numero suggerito).
- **Snapshot nutrizionale**: i valori per 100g di un alimento vengono copiati nella riga al momento dell'inserimento, mai referenziati a runtime.
- **Niente `style="..."` inline nei componenti Vue**: sempre classi Tailwind, variabili CSS custom con la sintassi v4 `bg-(--surf)`/`text-(--fg)` (non `bg-[var(--surf)]`) — verificato nel codice esistente (`SelezionaPazienteCombobox.vue`).
- **Campi numerici lato frontend**: mai `type="number"` nativo — `type="text" inputmode="decimal"`, filtro live `filtraDecimaleItaliano` (virgola italiana), errore sotto al campo che sparisce alla correzione — convenzione stabilita da Andrea per ogni form futura.
- **Dopo ogni modifica backend**: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` poi `mvn test` da `backend/`, riportare l'esito reale.
- **Nuove tabelle**: vanno aggiunte al `TRUNCATE` di `AbstractIntegrationTest` (non sono tabelle seminate/di riferimento come `alimenti`).
- **Verifica manuale in browser**: non è responsabilità dell'agente (la fa Andrea) — non trattarla come condizione di completamento.

---

## File Structure

**Backend** (`backend/src/main/java/com/hexisnutrition/backend/pianialimentari/`, nuovo package):
- `ModalitaPiano.java`, `StatoPiano.java`, `StatoPianoVisualizzato.java`, `FormulaBmr.java`, `GiornoSettimana.java`, `TipoPasto.java` — enum
- `PianoAlimentare.java`, `Pasto.java`, `PianoGiornoMacroTarget.java`, `PianoEsempio.java`, `PianoAlimentoRiga.java` — entità
- `PianoAlimentareRepository.java`, `PastoRepository.java`, `PianoGiornoMacroTargetRepository.java`, `PianoEsempioRepository.java`, `PianoAlimentoRigaRepository.java`
- `CalcolatoreTdee.java`, `SuggerimentoTdee.java` (record)
- DTO richiesta: `CreaPianoAlimentareRequest.java`, `AggiornaPianoAlimentareRequest.java`, `PastoRequest.java`, `GiornoMacroTargetRequest.java`, `EsempioRequest.java`, `RigaAlimentoRequest.java`
- DTO risposta: `PianoAlimentareResponse.java`, `PastoResponse.java`, `GiornoMacroTargetResponse.java`, `EsempioResponse.java`, `RigaAlimentoResponse.java`, `PianoAlimentareListaPaginataResponse.java`, `PianoAlimentareRigaListaResponse.java`
- `CriteriRicercaPianiAlimentari.java`, `CampoOrdinamentoPianiAlimentari.java`, `DirezioneOrdinamento.java` (proprio del package, stessa scelta già presa per `alimenti`)
- `PianoAlimentareSpecifications.java`
- `PianoAlimentareNonTrovatoException.java`, `PianoAlimentareNonEliminabileException.java`
- `PianoAlimentareService.java`, `PianoAlimentareController.java`
- Migrazioni: `V21__crea_tabelle_piani_alimentari.sql` (tutte e 5 le tabelle in un'unica migrazione, sono un unico sotto-dominio)
- Modifica: `backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java` (TRUNCATE)

**Frontend** (`frontend-professionisti/src/`):
- `api/pianiAlimentari.ts` (+ `.spec.ts`)
- `views/pianiAlimentari/PianoAlimentareFormView.vue` (+ `.spec.ts`) — editor condiviso, tre rendering
- `views/pianiAlimentari/PianiAlimentariListView.vue` (+ `.spec.ts`)
- `components/pianiAlimentari/AggiungiAlimentoDialog.vue` (+ `.spec.ts`) — modal condiviso tra le modalità PASTI/ESEMPI
- Modifica: `router/index.ts`, `components/AppSidebar.vue`, `views/DashboardView.vue`, `views/pazienti/PazienteDettaglioView.vue`

---

### Task 1: Migrazione, enum ed entità

**Files:**
- Create: `backend/src/main/resources/db/migration/V21__crea_tabelle_piani_alimentari.sql`
- Create: `backend/.../pianialimentari/ModalitaPiano.java`, `StatoPiano.java`, `StatoPianoVisualizzato.java`, `FormulaBmr.java`, `GiornoSettimana.java`, `TipoPasto.java`
- Create: `backend/.../pianialimentari/PianoAlimentare.java`, `Pasto.java`, `PianoGiornoMacroTarget.java`, `PianoEsempio.java`, `PianoAlimentoRiga.java`
- Create: `backend/.../pianialimentari/PianoAlimentareRepository.java`, `PastoRepository.java`, `PianoGiornoMacroTargetRepository.java`, `PianoEsempioRepository.java`, `PianoAlimentoRigaRepository.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareRepositoryTest.java`

**Interfaces:**
- Produces: le 5 entità con i getter/setter elencati sotto, i 5 repository (`JpaRepository<T, UUID>`, più i metodi di query elencati), i 6 enum. Ogni task successivo dipende da questi nomi esatti.

- [ ] **Step 1: Scrivere la migrazione**

```sql
-- Modello dati del sotto-progetto "Piano alimentare" (editor + lista), vedi
-- docs/superpowers/specs/2026-09-12-piano-alimentare-design.md.
CREATE TABLE piani_alimentari (
    id UUID PRIMARY KEY,
    paziente_id UUID NOT NULL REFERENCES pazienti(id),
    professionista_id UUID NOT NULL REFERENCES professionisti(id),
    visita_id UUID REFERENCES visite(id),
    nome VARCHAR(200) NOT NULL,
    modalita VARCHAR(10) NOT NULL,
    stato VARCHAR(10) NOT NULL DEFAULT 'BOZZA',
    data_inizio DATE NOT NULL,
    data_fine DATE,
    obiettivo_kcal NUMERIC(7,2),
    obiettivo_kcal_suggerito NUMERIC(7,2),
    bmr_calcolato NUMERIC(7,2),
    tdee_calcolato NUMERIC(7,2),
    formula_bmr_usata VARCHAR(20),
    sotto_soglia_sicurezza BOOLEAN NOT NULL DEFAULT false,
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now(),
    aggiornato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_piani_alimentari_paziente_id ON piani_alimentari(paziente_id);
CREATE INDEX idx_piani_alimentari_professionista_id ON piani_alimentari(professionista_id);

CREATE TABLE pasti (
    id UUID PRIMARY KEY,
    piano_id UUID NOT NULL REFERENCES piani_alimentari(id) ON DELETE CASCADE,
    giorno_settimana VARCHAR(10) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    nota TEXT,
    ordine INTEGER NOT NULL
);
CREATE INDEX idx_pasti_piano_id ON pasti(piano_id);

CREATE TABLE piano_giorno_macro_target (
    id UUID PRIMARY KEY,
    piano_id UUID NOT NULL REFERENCES piani_alimentari(id) ON DELETE CASCADE,
    giorno_settimana VARCHAR(10) NOT NULL,
    kcal_target NUMERIC(7,2),
    proteine_target NUMERIC(6,2),
    carboidrati_target NUMERIC(6,2),
    grassi_target NUMERIC(6,2),
    UNIQUE (piano_id, giorno_settimana)
);

CREATE TABLE piano_esempi (
    id UUID PRIMARY KEY,
    piano_id UUID NOT NULL REFERENCES piani_alimentari(id) ON DELETE CASCADE,
    tipo_pasto VARCHAR(20) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    ordine INTEGER NOT NULL
);
CREATE INDEX idx_piano_esempi_piano_id ON piano_esempi(piano_id);

-- Righe di alimenti condivise tra pasti ed esempi (esattamente uno dei due FK
-- valorizzato), stesso pattern di esclusività già usato da token_azione per
-- professionista_id/paziente_id.
CREATE TABLE piano_alimento_righe (
    id UUID PRIMARY KEY,
    pasto_id UUID REFERENCES pasti(id) ON DELETE CASCADE,
    esempio_id UUID REFERENCES piano_esempi(id) ON DELETE CASCADE,
    alimento_id UUID,
    nome VARCHAR(200) NOT NULL,
    kcal_100g NUMERIC(7,2) NOT NULL,
    proteine_100g NUMERIC(6,2) NOT NULL,
    carboidrati_100g NUMERIC(6,2) NOT NULL,
    grassi_100g NUMERIC(6,2) NOT NULL,
    zuccheri_100g NUMERIC(6,2),
    grammi NUMERIC(7,2) NOT NULL,
    ordine INTEGER NOT NULL,
    CONSTRAINT chk_piano_alimento_riga_padre CHECK (
        (pasto_id IS NOT NULL AND esempio_id IS NULL) OR
        (pasto_id IS NULL AND esempio_id IS NOT NULL)
    )
);
CREATE INDEX idx_piano_alimento_righe_pasto_id ON piano_alimento_righe(pasto_id);
CREATE INDEX idx_piano_alimento_righe_esempio_id ON piano_alimento_righe(esempio_id);
```

- [ ] **Step 2: Aggiungere le nuove tabelle al `TRUNCATE` di test**

In `AbstractIntegrationTest.java:33`, sostituire la riga esistente con:

```java
jdbcTemplate.execute("TRUNCATE TABLE piano_alimento_righe, piano_esempi, piano_giorno_macro_target, pasti, " +
        "piani_alimentari, token_azione, plicometrie, visite, pazienti, professionisti RESTART IDENTITY CASCADE");
```

(le tabelle figlie dei piani vanno elencate prima per chiarezza, anche se `CASCADE` le coprirebbe comunque tramite `piani_alimentari` — stesso stile esplicito già usato per le altre tabelle in questo file).

- [ ] **Step 3: Enum**

```java
package com.hexisnutrition.backend.pianialimentari;

public enum ModalitaPiano { PASTI, MACRO, ESEMPI }
```

```java
package com.hexisnutrition.backend.pianialimentari;

public enum StatoPiano { BOZZA, ATTIVO, TERMINATO }
```

```java
package com.hexisnutrition.backend.pianialimentari;

/** Stato esposto in API: include SCADUTO, calcolato da ATTIVO + dataFine passata, mai persistito. */
public enum StatoPianoVisualizzato { BOZZA, ATTIVO, SCADUTO, TERMINATO }
```

```java
package com.hexisnutrition.backend.pianialimentari;

public enum FormulaBmr { KATCH_MCARDLE, MIFFLIN_ST_JEOR }
```

```java
package com.hexisnutrition.backend.pianialimentari;

public enum GiornoSettimana { LUNEDI, MARTEDI, MERCOLEDI, GIOVEDI, VENERDI, SABATO, DOMENICA }
```

```java
package com.hexisnutrition.backend.pianialimentari;

public enum TipoPasto { COLAZIONE, SPUNTINO_MATTINA, PRANZO, SPUNTINO_POMERIGGIO, CENA, ALTRO }
```

- [ ] **Step 4: Entità `PianoAlimentare`**

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "piani_alimentari")
public class PianoAlimentare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "paziente_id", nullable = false)
    private UUID pazienteId;

    @Column(name = "professionista_id", nullable = false)
    private UUID professionistaId;

    @Column(name = "visita_id")
    private UUID visitaId;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalitaPiano modalita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoPiano stato = StatoPiano.BOZZA;

    @Column(name = "data_inizio", nullable = false)
    private LocalDate dataInizio = LocalDate.now();

    @Column(name = "data_fine")
    private LocalDate dataFine;

    @Column(name = "obiettivo_kcal")
    private BigDecimal obiettivoKcal;

    @Column(name = "obiettivo_kcal_suggerito")
    private BigDecimal obiettivoKcalSuggerito;

    @Column(name = "bmr_calcolato")
    private BigDecimal bmrCalcolato;

    @Column(name = "tdee_calcolato")
    private BigDecimal tdeeCalcolato;

    @Enumerated(EnumType.STRING)
    @Column(name = "formula_bmr_usata")
    private FormulaBmr formulaBmrUsata;

    @Column(name = "sotto_soglia_sicurezza", nullable = false)
    private boolean sottoSogliaSicurezza;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    @Column(name = "aggiornato_il", nullable = false)
    private Instant aggiornatoIl = Instant.now();

    protected PianoAlimentare() {
    }

    public PianoAlimentare(UUID pazienteId, UUID professionistaId, UUID visitaId, String nome,
                            ModalitaPiano modalita) {
        this.pazienteId = pazienteId;
        this.professionistaId = professionistaId;
        this.visitaId = visitaId;
        this.nome = nome;
        this.modalita = modalita;
    }

    /** BOZZA/TERMINATO restano invariati; ATTIVO con dataFine passata diventa SCADUTO solo in lettura. */
    public StatoPianoVisualizzato statoEffettivo() {
        if (stato == StatoPiano.ATTIVO && dataFine != null && dataFine.isBefore(LocalDate.now())) {
            return StatoPianoVisualizzato.SCADUTO;
        }
        return StatoPianoVisualizzato.valueOf(stato.name());
    }

    public UUID getId() {
        return id;
    }

    public UUID getPazienteId() {
        return pazienteId;
    }

    public UUID getProfessionistaId() {
        return professionistaId;
    }

    public UUID getVisitaId() {
        return visitaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public ModalitaPiano getModalita() {
        return modalita;
    }

    public StatoPiano getStato() {
        return stato;
    }

    public void setStato(StatoPiano stato) {
        this.stato = stato;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public BigDecimal getObiettivoKcal() {
        return obiettivoKcal;
    }

    public void setObiettivoKcal(BigDecimal obiettivoKcal) {
        this.obiettivoKcal = obiettivoKcal;
    }

    public BigDecimal getObiettivoKcalSuggerito() {
        return obiettivoKcalSuggerito;
    }

    public void setObiettivoKcalSuggerito(BigDecimal obiettivoKcalSuggerito) {
        this.obiettivoKcalSuggerito = obiettivoKcalSuggerito;
    }

    public BigDecimal getBmrCalcolato() {
        return bmrCalcolato;
    }

    public void setBmrCalcolato(BigDecimal bmrCalcolato) {
        this.bmrCalcolato = bmrCalcolato;
    }

    public BigDecimal getTdeeCalcolato() {
        return tdeeCalcolato;
    }

    public void setTdeeCalcolato(BigDecimal tdeeCalcolato) {
        this.tdeeCalcolato = tdeeCalcolato;
    }

    public FormulaBmr getFormulaBmrUsata() {
        return formulaBmrUsata;
    }

    public void setFormulaBmrUsata(FormulaBmr formulaBmrUsata) {
        this.formulaBmrUsata = formulaBmrUsata;
    }

    public boolean isSottoSogliaSicurezza() {
        return sottoSogliaSicurezza;
    }

    public void setSottoSogliaSicurezza(boolean sottoSogliaSicurezza) {
        this.sottoSogliaSicurezza = sottoSogliaSicurezza;
    }
}
```

- [ ] **Step 5: Entità figlie**

`Pasto.java` — stesso stile piatto (`@Entity`/`@Table(name = "pasti")`, id generato, niente relazioni JPA verso `PianoAlimentare`):

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "pasti")
public class Pasto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "piano_id", nullable = false)
    private UUID pianoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "giorno_settimana", nullable = false)
    private GiornoSettimana giornoSettimana;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPasto tipo;

    @Column(columnDefinition = "TEXT")
    private String nota;

    @Column(nullable = false)
    private int ordine;

    protected Pasto() {
    }

    public Pasto(UUID pianoId, GiornoSettimana giornoSettimana, String nome, TipoPasto tipo, String nota, int ordine) {
        this.pianoId = pianoId;
        this.giornoSettimana = giornoSettimana;
        this.nome = nome;
        this.tipo = tipo;
        this.nota = nota;
        this.ordine = ordine;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPianoId() {
        return pianoId;
    }

    public GiornoSettimana getGiornoSettimana() {
        return giornoSettimana;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoPasto getTipo() {
        return tipo;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public int getOrdine() {
        return ordine;
    }
}
```

`PianoGiornoMacroTarget.java` (tabella `piano_giorno_macro_target`): campi `id`, `pianoId` (UUID), `giornoSettimana` (`GiornoSettimana`), `kcalTarget`/`proteineTarget`/`carboidratiTarget`/`grassiTarget` (tutti `BigDecimal`, nullable). Costruttore `(UUID pianoId, GiornoSettimana giornoSettimana, BigDecimal kcalTarget, BigDecimal proteineTarget, BigDecimal carboidratiTarget, BigDecimal grassiTarget)`, getter per tutti i campi — stesso stile di `Pasto` sopra.

`PianoEsempio.java` (tabella `piano_esempi`): campi `id`, `pianoId`, `tipoPasto` (`TipoPasto`), `nome`, `ordine` (int). Costruttore `(UUID pianoId, TipoPasto tipoPasto, String nome, int ordine)`, stessi getter di `Pasto` (senza `nota`).

`PianoAlimentoRiga.java` (tabella `piano_alimento_righe`): campi `id`, `pastoId` (UUID, nullable), `esempioId` (UUID, nullable), `alimentoId` (UUID, nullable, **senza FK** — stesso motivo di `alimenti.professionista_id`), `nome`, `kcal100g`/`proteine100g`/`carboidrati100g`/`grassi100g` (`BigDecimal`, NOT NULL), `zuccheri100g` (`BigDecimal`, nullable), `grammi` (`BigDecimal`), `ordine` (int). Due costruttori factory statici invece di uno con troppi parametri posizionali ambigui:

```java
public static PianoAlimentoRiga perPasto(UUID pastoId, UUID alimentoId, String nome, BigDecimal kcal100g,
        BigDecimal proteine100g, BigDecimal carboidrati100g, BigDecimal grassi100g, BigDecimal zuccheri100g,
        BigDecimal grammi, int ordine) {
    PianoAlimentoRiga riga = new PianoAlimentoRiga();
    riga.pastoId = pastoId;
    riga.alimentoId = alimentoId;
    riga.nome = nome;
    riga.kcal100g = kcal100g;
    riga.proteine100g = proteine100g;
    riga.carboidrati100g = carboidrati100g;
    riga.grassi100g = grassi100g;
    riga.zuccheri100g = zuccheri100g;
    riga.grammi = grammi;
    riga.ordine = ordine;
    return riga;
}

public static PianoAlimentoRiga perEsempio(UUID esempioId, UUID alimentoId, String nome, BigDecimal kcal100g,
        BigDecimal proteine100g, BigDecimal carboidrati100g, BigDecimal grassi100g, BigDecimal zuccheri100g,
        BigDecimal grammi, int ordine) {
    PianoAlimentoRiga riga = new PianoAlimentoRiga();
    riga.esempioId = esempioId;
    riga.alimentoId = alimentoId;
    riga.nome = nome;
    riga.kcal100g = kcal100g;
    riga.proteine100g = proteine100g;
    riga.carboidrati100g = carboidrati100g;
    riga.grassi100g = grassi100g;
    riga.zuccheri100g = zuccheri100g;
    riga.grammi = grammi;
    riga.ordine = ordine;
    return riga;
}
```

più il costruttore protetto vuoto richiesto da JPA e i getter per tutti i campi (nessun setter: una riga modificata si cancella e si ricrea, mai in place, coerente con "sostituzione totale" del `PUT`).

- [ ] **Step 6: Repository**

```java
package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PianoAlimentareRepository extends JpaRepository<PianoAlimentare, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<PianoAlimentare> {
    List<PianoAlimentare> findAllByPazienteIdAndStato(UUID pazienteId, StatoPiano stato);
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PastoRepository extends JpaRepository<Pasto, UUID> {
    List<Pasto> findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(UUID pianoId);

    void deleteAllByPianoId(UUID pianoId);
}
```

`PianoGiornoMacroTargetRepository` (stesso stile): `List<PianoGiornoMacroTarget> findAllByPianoId(UUID pianoId)`, `void deleteAllByPianoId(UUID pianoId)`.

`PianoEsempioRepository`: `List<PianoEsempio> findAllByPianoIdOrderByTipoPastoAscOrdineAsc(UUID pianoId)`, `void deleteAllByPianoId(UUID pianoId)`.

`PianoAlimentoRigaRepository`: `List<PianoAlimentoRiga> findAllByPastoIdInOrderByOrdineAsc(List<UUID> pastoIds)`, `List<PianoAlimentoRiga> findAllByEsempioIdInOrderByOrdineAsc(List<UUID> esempioIds)`, `void deleteAllByPastoIdIn(List<UUID> pastoIds)`, `void deleteAllByEsempioIdIn(List<UUID> esempioIds)`.

- [ ] **Step 7: Test di persistenza minimo**

```java
package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PianoAlimentareRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PianoAlimentareRepository pianoAlimentareRepository;
    @Autowired
    private PastoRepository pastoRepository;
    @Autowired
    private PianoAlimentoRigaRepository pianoAlimentoRigaRepository;
    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Test
    void salvaPianoConPastoERigaEIndiciziaPerPazienteEStato() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@test.it", "hash", "Anna", "Rossi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", "333", LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        PianoAlimentare piano = pianoAlimentareRepository.save(
                new PianoAlimentare(paziente.getId(), professionista.getId(), null, "Ipertrofia · fase 1",
                        ModalitaPiano.PASTI));

        Pasto pasto = pastoRepository.save(
                new Pasto(piano.getId(), GiornoSettimana.LUNEDI, "Colazione", TipoPasto.COLAZIONE, null, 0));
        pianoAlimentoRigaRepository.save(PianoAlimentoRiga.perPasto(pasto.getId(), null, "Avena", null,
                java.math.BigDecimal.valueOf(372), java.math.BigDecimal.valueOf(12.9), java.math.BigDecimal.valueOf(65),
                java.math.BigDecimal.valueOf(6.5), java.math.BigDecimal.valueOf(1.1), java.math.BigDecimal.valueOf(60), 0));

        assertThat(pianoAlimentareRepository.findAllByPazienteIdAndStato(paziente.getId(), StatoPiano.BOZZA))
                .hasSize(1);
        assertThat(pastoRepository.findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(piano.getId())).hasSize(1);
        assertThat(pianoAlimentoRigaRepository.findAllByPastoIdInOrderByOrdineAsc(java.util.List.of(pasto.getId())))
                .hasSize(1);
    }
}
```

Verificare le firme reali di `Professionista`/`ProfessionistaRepository`/`Paziente` (costruttore, package) prima di scrivere questo test — se il costruttore di `Paziente` non corrisponde esattamente (es. ordine parametri), adattare la chiamata, non il modello dati.

- [ ] **Step 8: Eseguire i test**

Run: `mvn test -Dtest=PianoAlimentareRepositoryTest` da `backend/` (con `JAVA_HOME` impostato)
Expected: PASS, 1 test verde.

- [ ] **Step 9: Eseguire l'intera suite backend** (per verificare che il nuovo `TRUNCATE` non rompa nessun test esistente)

Run: `mvn test` da `backend/`
Expected: tutti i test verdi, nessuna regressione.

- [ ] **Step 10: Staging (nessun commit)**

```bash
git add backend/src/main/resources/db/migration/V21__crea_tabelle_piani_alimentari.sql
git add backend/src/main/java/com/hexisnutrition/backend/pianialimentari/
git add backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java
```

---

### Task 2: `CalcolatoreTdee` (motore di calcolo, puro)

**Files:**
- Create: `backend/.../pianialimentari/SuggerimentoTdee.java`
- Create: `backend/.../pianialimentari/CalcolatoreTdee.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/pianialimentari/CalcolatoreTdeeTest.java`

**Interfaces:**
- Consumes: `Paziente` (`getDataNascita()`, `getSesso()`, `getStileDiVita()`) e `Visita` (`getDataVisita()`, `getPesoKg()`, `getAltezzaCm()`, `getObiettivo()`) da `com.hexisnutrition.backend.pazienti`; `Plicometria.getMassaMagraKg()` (nullable, package `pazienti`).
- Produces: `CalcolatoreTdee.calcola(Paziente, Visita, Plicometria)` → `Optional<SuggerimentoTdee>`, usato da `PianoAlimentareService` in Task 3.

- [ ] **Step 1: Scrivere il record risultato**

```java
package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;

public record SuggerimentoTdee(
        BigDecimal bmr,
        BigDecimal tdee,
        FormulaBmr formulaUsata,
        BigDecimal calorieSuggerite,
        boolean sottoSogliaSicurezza
) {
}
```

- [ ] **Step 2: Scrivere il test (ordine dell'algoritmo, §4-6 di `specifica-tdee.docx`)**

```java
package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.ObiettivoVisita;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.Plicometria;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.pazienti.StileDiVita;
import com.hexisnutrition.backend.pazienti.Visita;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalcolatoreTdeeTest {

    private final CalcolatoreTdee calcolatore = new CalcolatoreTdee();

    private Paziente paziente(LocalDate dataNascita, Sesso sesso, StileDiVita stileDiVita) {
        return new Paziente(UUID.randomUUID(), "Nome", "Cognome", "AAAAAA00A00A000A", "test@test.it", null,
                dataNascita, sesso, null, stileDiVita, null);
    }

    private Visita visita(LocalDate dataVisita, BigDecimal pesoKg, Integer altezzaCm, ObiettivoVisita obiettivo) {
        // 18 parametri posizionali: pazienteId, dataVisita, altezzaCm, pesoKg, le 11 circonferenze (null,
        // non servono per il calcolo TDEE), protocolloVita (null → default OMS), note (null), obiettivo.
        return new Visita(UUID.randomUUID(), dataVisita, altezzaCm, pesoKg,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, obiettivo);
    }

    @Test
    void esempioNumericoCompletoDellaSpecifica() {
        // Donna 35 anni, 68kg, 165cm, moderatamente attiva, Dimagrimento, nessuna plicometria.
        // NOTA: `specifica-tdee.docx` §7 riporta come risultato 1300.25/2015.4/1612.3 kcal, ma applicando
        // la sua stessa formula (§4.2/4.3) ai suoi stessi input si ottiene 1375.25/2131.64 — il documento
        // sorgente ha un errore aritmetico nel proprio esempio numerico (probabile refuso, i passaggi
        // successivi del documento sono comunque coerenti tra loro: 1300.25×1.55=2015.4 torna, ma
        // 10×68+6.25×165−5×35−161 non fa 1300.25). Qui si segue la formula (autorevole, non l'esempio),
        // scoperto e verificato in fase di implementazione — vedi ledger SDD Task 2.
        Paziente paziente = paziente(LocalDate.of(1991, 9, 5), Sesso.F, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 9, 5), BigDecimal.valueOf(68), 165, ObiettivoVisita.DIMAGRIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        assertThat(risultato).isPresent();
        assertThat(risultato.get().formulaUsata()).isEqualTo(FormulaBmr.MIFFLIN_ST_JEOR);
        // BMR = 10*68 + 6.25*165 - 5*35 - 161 = 680 + 1031.25 - 175 - 161 = 1375.25
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1375.25, org.assertj.core.data.Offset.offset(0.01));
        // TDEE = 1375.25 * 1.55 = 2131.6375
        assertThat(risultato.get().tdee().doubleValue()).isCloseTo(2131.64, org.assertj.core.data.Offset.offset(0.5));
        // Aggiustamento sempre 0%: le calorie suggerite coincidono col TDEE puro (decisione di Andrea,
        // diversa dal -20% proposto nel documento originale per Dimagrimento).
        assertThat(risultato.get().calorieSuggerite()).isEqualByComparingTo(risultato.get().tdee());
        assertThat(risultato.get().sottoSogliaSicurezza()).isFalse();
    }

    @Test
    void usaKatchMcArdleQuandoLaMassaMagraEDisponibile() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(80), 180, ObiettivoVisita.MANTENIMENTO);
        Plicometria plicometria = plicometriaConMassaMagra(BigDecimal.valueOf(65));

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, plicometria);

        assertThat(risultato).isPresent();
        assertThat(risultato.get().formulaUsata()).isEqualTo(FormulaBmr.KATCH_MCARDLE);
        // BMR = 370 + 21.6 * 65 = 1774
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1774.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void etaCalcolataSullaDataDellaVisitaNonSuOggi() {
        // Nato il 10 settembre 1990, visita il 1 settembre 2026 (9 giorni prima del compleanno, in cui
        // compirebbe 36 anni): ha ancora 35 anni.
        Paziente paziente = paziente(LocalDate.of(1990, 9, 10), Sesso.M, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 9, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.MANTENIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        // BMR Mifflin M = 10*70 + 6.25*175 - 5*35 + 5 = 700 + 1093.75 - 175 + 5 = 1623.75
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1623.75, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void coefficienteSessoAltroEMediaTraMEF() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.ALTRO, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.MANTENIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        // BMR = 10*70 + 6.25*175 - 5*36 - 78 = 700 + 1093.75 - 180 - 78 = 1535.75
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1535.75, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void fattoreDiAttivitaDiDefaultQuandoStileDiVitaNonCompilato() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, null);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.MANTENIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        BigDecimal bmrAtteso = BigDecimal.valueOf(10.0 * 70 + 6.25 * 175 - 5 * 36 + 5);
        BigDecimal tdeeAtteso = bmrAtteso.multiply(BigDecimal.valueOf(1.375));
        assertThat(risultato.get().tdee().doubleValue()).isCloseTo(tdeeAtteso.doubleValue(),
                org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void nessunCalcoloPerPatologiaClinica() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.PATOLOGIA_CLINICA);

        assertThat(calcolatore.calcola(paziente, visita, null)).isEmpty();
    }

    @Test
    void nessunCalcoloPerGravidanzaAllattamento() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.F, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 165, ObiettivoVisita.GRAVIDANZA_ALLATTAMENTO);

        assertThat(calcolatore.calcola(paziente, visita, null)).isEmpty();
    }

    @Test
    void mostraIlTdeePuroPerObiettivoEducativo() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.EDUCATIVO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        assertThat(risultato).isPresent();
        assertThat(risultato.get().calorieSuggerite()).isEqualByComparingTo(risultato.get().tdee());
    }

    @Test
    void segnalaSottoSogliaDiSicurezzaPerCalorieMoltoBasse() {
        // Donna molto minuta e sedentaria, 25 anni: BMR = 10*35 + 6.25*140 - 5*25 - 161 = 939,
        // TDEE = 939 * 1.2 = 1126.8, sotto la soglia di 1200 kcal (F).
        Paziente paziente = paziente(LocalDate.of(2001, 1, 1), Sesso.F, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(35), 140, ObiettivoVisita.DIMAGRIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        assertThat(risultato.get().sottoSogliaSicurezza()).isTrue();
    }

    private Plicometria plicometriaConMassaMagra(BigDecimal massaMagraKg) {
        // 24 parametri posizionali: visitaId, protocollo, formulaVersione, etaAnni, coefficienteC,
        // coefficienteM, etniaAtleta, le 9 pliche (tutte null, non servono per questo test), sommaPlicheMm,
        // densitaCorporea, percentualeGrasso, massaGrassaKg, massaMagraKg, fmi, ffmi, limiteSicurezzaApplicato.
        return new Plicometria(UUID.randomUUID(), com.hexisnutrition.backend.pazienti.ProtocolloPlicometrico.JACKSON_POLLOCK_3,
                "v1", 30, null, null, null,
                null, null, null, null, null, null, null, null, null,
                BigDecimal.valueOf(10), BigDecimal.valueOf(1.06), BigDecimal.valueOf(15), BigDecimal.valueOf(10),
                massaMagraKg, BigDecimal.ONE, BigDecimal.ONE, false);
    }
}
```

Prima di eseguire questo test, verificare le firme reali di `Visita`/`Plicometria`/`Paziente` (i costruttori completi sono in `backend/.../pazienti/Visita.java`, `Plicometria.java`, `Paziente.java`, letti durante il brainstorming) — il numero e l'ordine esatto dei parametri `null` nei costruttori sopra deve combaciare con quelle classi, non è stato ricopiato qui carattere per carattere per brevità del test di esempio.

- [ ] **Step 3: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=CalcolatoreTdeeTest` da `backend/`
Expected: FAIL (compilazione, `CalcolatoreTdee` non esiste ancora)

- [ ] **Step 4: Implementare `CalcolatoreTdee`**

```java
package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.ObiettivoVisita;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.Plicometria;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.pazienti.StileDiVita;
import com.hexisnutrition.backend.pazienti.Visita;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class CalcolatoreTdee {

    private static final Set<ObiettivoVisita> OBIETTIVI_ESCLUSI =
            Set.of(ObiettivoVisita.PATOLOGIA_CLINICA, ObiettivoVisita.GRAVIDANZA_ALLATTAMENTO);

    private static final Map<StileDiVita, BigDecimal> FATTORI_ATTIVITA = new EnumMap<>(StileDiVita.class);
    static {
        FATTORI_ATTIVITA.put(StileDiVita.SEDENTARIO, BigDecimal.valueOf(1.2));
        FATTORI_ATTIVITA.put(StileDiVita.POCO_ATTIVO, BigDecimal.valueOf(1.375));
        FATTORI_ATTIVITA.put(StileDiVita.ATTIVO, BigDecimal.valueOf(1.55));
        FATTORI_ATTIVITA.put(StileDiVita.MOLTO_ATTIVO, BigDecimal.valueOf(1.725));
        FATTORI_ATTIVITA.put(StileDiVita.ESTREMAMENTE_ATTIVO, BigDecimal.valueOf(1.9));
    }
    private static final BigDecimal FATTORE_ATTIVITA_DEFAULT = BigDecimal.valueOf(1.375);

    private static final BigDecimal SOGLIA_MINIMA_F = BigDecimal.valueOf(1200);
    private static final BigDecimal SOGLIA_MINIMA_ALTRI = BigDecimal.valueOf(1500);

    public Optional<SuggerimentoTdee> calcola(Paziente paziente, Visita visita, Plicometria plicometriaOpzionale) {
        if (OBIETTIVI_ESCLUSI.contains(visita.getObiettivo())) {
            return Optional.empty();
        }

        int eta = Period.between(paziente.getDataNascita(), visita.getDataVisita()).getYears();

        BigDecimal bmr;
        FormulaBmr formulaUsata;
        if (plicometriaOpzionale != null && plicometriaOpzionale.getMassaMagraKg() != null) {
            // Katch-McArdle: BMR = 370 + 21.6 * massa magra (kg)
            bmr = BigDecimal.valueOf(370).add(
                    BigDecimal.valueOf(21.6).multiply(plicometriaOpzionale.getMassaMagraKg()));
            formulaUsata = FormulaBmr.KATCH_MCARDLE;
        } else {
            // Mifflin-St Jeor: BMR = 10*peso + 6.25*altezza - 5*eta + s
            BigDecimal base = BigDecimal.valueOf(10).multiply(visita.getPesoKg())
                    .add(BigDecimal.valueOf(6.25).multiply(BigDecimal.valueOf(visita.getAltezzaCm())))
                    .subtract(BigDecimal.valueOf(5L * eta));
            BigDecimal s = switch (paziente.getSesso()) {
                case M -> BigDecimal.valueOf(5);
                case F -> BigDecimal.valueOf(-161);
                case ALTRO -> BigDecimal.valueOf(-78);
            };
            bmr = base.add(s);
            formulaUsata = FormulaBmr.MIFFLIN_ST_JEOR;
        }

        BigDecimal fattoreAttivita = paziente.getStileDiVita() != null
                ? FATTORI_ATTIVITA.get(paziente.getStileDiVita())
                : FATTORE_ATTIVITA_DEFAULT;
        BigDecimal tdee = bmr.multiply(fattoreAttivita);

        // Aggiustamento sempre 0% per decisione di prodotto (vedi spec): le calorie suggerite
        // coincidono sempre col TDEE puro, per ogni obiettivo non escluso.
        BigDecimal calorieSuggerite = tdee.setScale(2, RoundingMode.HALF_UP);

        BigDecimal sogliaMinima = paziente.getSesso() == Sesso.F ? SOGLIA_MINIMA_F : SOGLIA_MINIMA_ALTRI;
        boolean sottoSoglia = calorieSuggerite.compareTo(sogliaMinima) < 0;

        return Optional.of(new SuggerimentoTdee(bmr.setScale(2, RoundingMode.HALF_UP),
                tdee.setScale(2, RoundingMode.HALF_UP), formulaUsata, calorieSuggerite, sottoSoglia));
    }
}
```

- [ ] **Step 5: Eseguire i test e verificare che passino**

Run: `mvn test -Dtest=CalcolatoreTdeeTest` da `backend/`
Expected: PASS, tutti gli 8 test verdi.

- [ ] **Step 6: Staging**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pianialimentari/SuggerimentoTdee.java
git add backend/src/main/java/com/hexisnutrition/backend/pianialimentari/CalcolatoreTdee.java
git add backend/src/test/java/com/hexisnutrition/backend/pianialimentari/CalcolatoreTdeeTest.java
```

---

### Task 3: Creazione bozza, dettaglio e DTO di risposta

**Files:**
- Create: `backend/.../pianialimentari/CreaPianoAlimentareRequest.java`
- Create: `backend/.../pianialimentari/RigaAlimentoResponse.java`, `PastoResponse.java`, `GiornoMacroTargetResponse.java`, `EsempioResponse.java`, `PianoAlimentareResponse.java`
- Create: `backend/.../pianialimentari/PianoAlimentareNonTrovatoException.java`
- Create: `backend/.../pianialimentari/PianoAlimentareService.java`
- Create: `backend/.../pianialimentari/PianoAlimentareController.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareControllerTest.java`

**Interfaces:**
- Consumes: tutto da Task 1 (entità/repository) e Task 2 (`CalcolatoreTdee`/`SuggerimentoTdee`); `PazienteRepository`, `VisitaRepository`, `PlicometriaRepository` da `com.hexisnutrition.backend.pazienti` (già esistenti).
- Produces: `PianoAlimentareService.creaBozza(UUID professionistaId, CreaPianoAlimentareRequest request): PianoAlimentareResponse`, `PianoAlimentareService.dettaglio(UUID professionistaId, UUID id): PianoAlimentareResponse`, `POST /piani-alimentari`, `GET /piani-alimentari/{id}`. Il campo `pianoAlimentareRepository`/i repository figli e la forma di `PianoAlimentareResponse` sono usati identici dai Task 4-7.

- [ ] **Step 1: DTO richiesta**

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreaPianoAlimentareRequest(
        @NotNull UUID pazienteId,
        @NotBlank String nome,
        @NotNull ModalitaPiano modalita
) {
}
```

- [ ] **Step 2: DTO risposta**

```java
package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;
import java.util.UUID;

public record RigaAlimentoResponse(
        UUID id,
        UUID alimentoId,
        String nome,
        BigDecimal kcal100g,
        BigDecimal proteine100g,
        BigDecimal carboidrati100g,
        BigDecimal grassi100g,
        BigDecimal zuccheri100g,
        BigDecimal grammi
) {
    public static RigaAlimentoResponse da(PianoAlimentoRiga riga) {
        return new RigaAlimentoResponse(riga.getId(), riga.getAlimentoId(), riga.getNome(), riga.getKcal100g(),
                riga.getProteine100g(), riga.getCarboidrati100g(), riga.getGrassi100g(), riga.getZuccheri100g(),
                riga.getGrammi());
    }
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import java.util.List;
import java.util.UUID;

public record PastoResponse(
        UUID id,
        GiornoSettimana giornoSettimana,
        String nome,
        TipoPasto tipo,
        String nota,
        int ordine,
        List<RigaAlimentoResponse> righe
) {
    public static PastoResponse da(Pasto pasto, List<PianoAlimentoRiga> righe) {
        return new PastoResponse(pasto.getId(), pasto.getGiornoSettimana(), pasto.getNome(), pasto.getTipo(),
                pasto.getNota(), pasto.getOrdine(), righe.stream().map(RigaAlimentoResponse::da).toList());
    }
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;

public record GiornoMacroTargetResponse(
        GiornoSettimana giornoSettimana,
        BigDecimal kcalTarget,
        BigDecimal proteineTarget,
        BigDecimal carboidratiTarget,
        BigDecimal grassiTarget
) {
    public static GiornoMacroTargetResponse da(PianoGiornoMacroTarget target) {
        return new GiornoMacroTargetResponse(target.getGiornoSettimana(), target.getKcalTarget(),
                target.getProteineTarget(), target.getCarboidratiTarget(), target.getGrassiTarget());
    }
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import java.util.List;
import java.util.UUID;

public record EsempioResponse(
        UUID id,
        TipoPasto tipoPasto,
        String nome,
        int ordine,
        List<RigaAlimentoResponse> righe
) {
    public static EsempioResponse da(PianoEsempio esempio, List<PianoAlimentoRiga> righe) {
        return new EsempioResponse(esempio.getId(), esempio.getTipoPasto(), esempio.getNome(), esempio.getOrdine(),
                righe.stream().map(RigaAlimentoResponse::da).toList());
    }
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PianoAlimentareResponse(
        UUID id,
        UUID pazienteId,
        String pazienteNomeCompleto,
        String nome,
        ModalitaPiano modalita,
        StatoPianoVisualizzato stato,
        LocalDate dataInizio,
        LocalDate dataFine,
        BigDecimal obiettivoKcal,
        BigDecimal obiettivoKcalSuggerito,
        BigDecimal bmrCalcolato,
        BigDecimal tdeeCalcolato,
        FormulaBmr formulaBmrUsata,
        boolean sottoSogliaSicurezza,
        List<PastoResponse> pasti,
        List<GiornoMacroTargetResponse> giorniMacroTarget,
        List<EsempioResponse> esempi
) {
}
```

- [ ] **Step 3: Eccezione**

```java
package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PianoAlimentareNonTrovatoException extends RuntimeException {
}
```

- [ ] **Step 4: Scrivere il test del controller (creazione + dettaglio) e verificare che fallisca**

```java
package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.pazienti.ObiettivoVisita;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.pazienti.StileDiVita;
import com.hexisnutrition.backend.pazienti.Visita;
import com.hexisnutrition.backend.pazienti.VisitaRepository;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PianoAlimentareControllerTest extends AbstractIntegrationTest {

    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private VisitaRepository visitaRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;
    @Autowired
    private JwtService jwtService;

    private Professionista professionista;
    private String token;
    private Paziente paziente;

    // Stesso pattern di autenticazione già usato da AlimentoControllerTest/PazienteControllerTest:
    // un JWT generato direttamente da JwtService, nessun vero giro su /auth/login.
    private String tokenPer(Professionista professionista) {
        return jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
    }

    @BeforeEach
    void creaProfessionistaEPaziente() {
        professionista = professionistaRepository.save(
                new Professionista("prof@test.it", "$2a$10$hash", "Anna", "Rossi"));
        token = tokenPer(professionista);
        paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", "333", LocalDate.of(1990, 1, 1), Sesso.M,
                null, StileDiVita.ATTIVO, null));
        visitaRepository.save(new Visita(paziente.getId(), LocalDate.now(), 178, BigDecimal.valueOf(78),
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                ObiettivoVisita.IPERTROFIA));
    }

    @Test
    void creaBozzaPianoConPastiCreaSetteGiorniConICinquePastiTemplateENessunaRiga() throws Exception {
        String body = """
                {"pazienteId":"%s","nome":"Ipertrofia · fase 1","modalita":"PASTI"}
                """.formatted(paziente.getId());

        mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato", is("BOZZA")))
                .andExpect(jsonPath("$.modalita", is("PASTI")))
                .andExpect(jsonPath("$.pasti.length()", is(35))) // 7 giorni * 5 pasti template
                .andExpect(jsonPath("$.pasti[0].righe.length()", is(0)))
                .andExpect(jsonPath("$.obiettivoKcalSuggerito").isNotEmpty());
    }
}
```

Verificare comunque `JwtService.generateToken(...)` e l'enum `Ruolo` con `Grep` prima di eseguire (firma confermata identica a `AlimentoControllerTest.java:53-55` durante la scrittura di questo piano).

- [ ] **Step 5: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=PianoAlimentareControllerTest` da `backend/`
Expected: FAIL (classi non ancora esistenti / 404)

- [ ] **Step 6: Implementare `PianoAlimentareService` (creazione + dettaglio)**

```java
package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteNonTrovatoException;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Plicometria;
import com.hexisnutrition.backend.pazienti.PlicometriaRepository;
import com.hexisnutrition.backend.pazienti.Visita;
import com.hexisnutrition.backend.pazienti.VisitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PianoAlimentareService {

    private static final List<TipoPasto> PASTI_TEMPLATE = List.of(
            TipoPasto.COLAZIONE, TipoPasto.SPUNTINO_MATTINA, TipoPasto.PRANZO,
            TipoPasto.SPUNTINO_POMERIGGIO, TipoPasto.CENA);
    private static final Map<TipoPasto, String> NOMI_PASTI_TEMPLATE = Map.of(
            TipoPasto.COLAZIONE, "Colazione",
            TipoPasto.SPUNTINO_MATTINA, "Spuntino mattina",
            TipoPasto.PRANZO, "Pranzo",
            TipoPasto.SPUNTINO_POMERIGGIO, "Spuntino pomeriggio",
            TipoPasto.CENA, "Cena");

    private final PianoAlimentareRepository pianoAlimentareRepository;
    private final PastoRepository pastoRepository;
    private final PianoGiornoMacroTargetRepository pianoGiornoMacroTargetRepository;
    private final PianoEsempioRepository pianoEsempioRepository;
    private final PianoAlimentoRigaRepository pianoAlimentoRigaRepository;
    private final PazienteRepository pazienteRepository;
    private final VisitaRepository visitaRepository;
    private final PlicometriaRepository plicometriaRepository;
    private final CalcolatoreTdee calcolatoreTdee;

    public PianoAlimentareService(PianoAlimentareRepository pianoAlimentareRepository, PastoRepository pastoRepository,
            PianoGiornoMacroTargetRepository pianoGiornoMacroTargetRepository,
            PianoEsempioRepository pianoEsempioRepository, PianoAlimentoRigaRepository pianoAlimentoRigaRepository,
            PazienteRepository pazienteRepository, VisitaRepository visitaRepository,
            PlicometriaRepository plicometriaRepository, CalcolatoreTdee calcolatoreTdee) {
        this.pianoAlimentareRepository = pianoAlimentareRepository;
        this.pastoRepository = pastoRepository;
        this.pianoGiornoMacroTargetRepository = pianoGiornoMacroTargetRepository;
        this.pianoEsempioRepository = pianoEsempioRepository;
        this.pianoAlimentoRigaRepository = pianoAlimentoRigaRepository;
        this.pazienteRepository = pazienteRepository;
        this.visitaRepository = visitaRepository;
        this.plicometriaRepository = plicometriaRepository;
        this.calcolatoreTdee = calcolatoreTdee;
    }

    private Paziente pazienteDiProprieta(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = pazienteRepository.findById(pazienteId).orElseThrow(PazienteNonTrovatoException::new);
        if (!paziente.getProfessionistaId().equals(professionistaId)) {
            throw new PazienteNonTrovatoException();
        }
        return paziente;
    }

    private Optional<Visita> ultimaVisita(UUID pazienteId) {
        return visitaRepository.findAllByPazienteIdOrderByDataVisitaAsc(pazienteId).stream()
                .max(Comparator.comparing(Visita::getDataVisita));
    }

    @Transactional
    public PianoAlimentareResponse creaBozza(UUID professionistaId, CreaPianoAlimentareRequest request) {
        Paziente paziente = pazienteDiProprieta(professionistaId, request.pazienteId());

        Optional<Visita> visitaOpt = ultimaVisita(paziente.getId());
        PianoAlimentare piano = new PianoAlimentare(paziente.getId(), professionistaId,
                visitaOpt.map(Visita::getId).orElse(null), request.nome(), request.modalita());

        if (visitaOpt.isPresent()) {
            Visita visita = visitaOpt.get();
            Plicometria plicometria = plicometriaRepository.findByVisitaId(visita.getId()).orElse(null);
            Optional<SuggerimentoTdee> suggerimento = calcolatoreTdee.calcola(paziente, visita, plicometria);
            suggerimento.ifPresent(s -> {
                piano.setObiettivoKcalSuggerito(s.calorieSuggerite());
                piano.setBmrCalcolato(s.bmr());
                piano.setTdeeCalcolato(s.tdee());
                piano.setFormulaBmrUsata(s.formulaUsata());
                piano.setSottoSogliaSicurezza(s.sottoSogliaSicurezza());
                piano.setObiettivoKcal(s.calorieSuggerite());
            });
        }
        pianoAlimentareRepository.save(piano);

        switch (request.modalita()) {
            case PASTI -> creaPastiTemplate(piano.getId());
            case MACRO -> creaGiorniMacroTemplate(piano.getId());
            case ESEMPI -> creaEsempiTemplate(piano.getId());
        }

        return dettaglio(professionistaId, piano.getId());
    }

    private void creaPastiTemplate(UUID pianoId) {
        for (GiornoSettimana giorno : GiornoSettimana.values()) {
            int ordine = 0;
            for (TipoPasto tipo : PASTI_TEMPLATE) {
                pastoRepository.save(new Pasto(pianoId, giorno, NOMI_PASTI_TEMPLATE.get(tipo), tipo, null, ordine++));
            }
        }
    }

    private void creaGiorniMacroTemplate(UUID pianoId) {
        for (GiornoSettimana giorno : GiornoSettimana.values()) {
            pianoGiornoMacroTargetRepository.save(
                    new PianoGiornoMacroTarget(pianoId, giorno, null, null, null, null));
        }
    }

    private void creaEsempiTemplate(UUID pianoId) {
        int ordine = 0;
        for (TipoPasto tipo : PASTI_TEMPLATE) {
            pianoEsempioRepository.save(new PianoEsempio(pianoId, tipo, NOMI_PASTI_TEMPLATE.get(tipo), ordine++));
        }
    }

    private PianoAlimentare pianoDiProprieta(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoAlimentareRepository.findById(pianoId)
                .orElseThrow(PianoAlimentareNonTrovatoException::new);
        if (!piano.getProfessionistaId().equals(professionistaId)) {
            throw new PianoAlimentareNonTrovatoException();
        }
        return piano;
    }

    public PianoAlimentareResponse dettaglio(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        Paziente paziente = pazienteRepository.findById(piano.getPazienteId()).orElseThrow(PazienteNonTrovatoException::new);

        List<PastoResponse> pasti = List.of();
        List<GiornoMacroTargetResponse> giorniMacroTarget = List.of();
        List<EsempioResponse> esempi = List.of();

        if (piano.getModalita() == ModalitaPiano.PASTI) {
            List<Pasto> listaPasti = pastoRepository.findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(pianoId);
            List<UUID> pastoIds = listaPasti.stream().map(Pasto::getId).toList();
            Map<UUID, List<PianoAlimentoRiga>> righePerPasto = pianoAlimentoRigaRepository
                    .findAllByPastoIdInOrderByOrdineAsc(pastoIds).stream()
                    .collect(Collectors.groupingBy(PianoAlimentoRiga::getPastoId));
            pasti = listaPasti.stream()
                    .map(p -> PastoResponse.da(p, righePerPasto.getOrDefault(p.getId(), List.of())))
                    .toList();
        } else if (piano.getModalita() == ModalitaPiano.MACRO) {
            giorniMacroTarget = pianoGiornoMacroTargetRepository.findAllByPianoId(pianoId).stream()
                    .map(GiornoMacroTargetResponse::da).toList();
        } else {
            List<PianoEsempio> listaEsempi = pianoEsempioRepository.findAllByPianoIdOrderByTipoPastoAscOrdineAsc(pianoId);
            List<UUID> esempioIds = listaEsempi.stream().map(PianoEsempio::getId).toList();
            Map<UUID, List<PianoAlimentoRiga>> righePerEsempio = pianoAlimentoRigaRepository
                    .findAllByEsempioIdInOrderByOrdineAsc(esempioIds).stream()
                    .collect(Collectors.groupingBy(PianoAlimentoRiga::getEsempioId));
            esempi = listaEsempi.stream()
                    .map(e -> EsempioResponse.da(e, righePerEsempio.getOrDefault(e.getId(), List.of())))
                    .toList();
        }

        return new PianoAlimentareResponse(piano.getId(), piano.getPazienteId(),
                paziente.getNome() + " " + paziente.getCognome(), piano.getNome(), piano.getModalita(),
                piano.statoEffettivo(), piano.getDataInizio(), piano.getDataFine(), piano.getObiettivoKcal(),
                piano.getObiettivoKcalSuggerito(), piano.getBmrCalcolato(), piano.getTdeeCalcolato(),
                piano.getFormulaBmrUsata(), piano.isSottoSogliaSicurezza(), pasti, giorniMacroTarget, esempi);
    }
}
```

Verificare che `PazienteNonTrovatoException` sia pubblica e importabile da un altro package (dovrebbe già esserlo, stesso modificatore delle altre eccezioni viste in Task 1-2).

- [ ] **Step 7: Implementare il controller (solo `POST`/`GET` per ora)**

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/piani-alimentari")
public class PianoAlimentareController {

    private final PianoAlimentareService pianoAlimentareService;

    public PianoAlimentareController(PianoAlimentareService pianoAlimentareService) {
        this.pianoAlimentareService = pianoAlimentareService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PianoAlimentareResponse crea(@AuthenticationPrincipal UUID professionistaId,
                                          @Valid @RequestBody CreaPianoAlimentareRequest request) {
        return pianoAlimentareService.creaBozza(professionistaId, request);
    }

    @GetMapping("/{id}")
    public PianoAlimentareResponse dettaglio(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return pianoAlimentareService.dettaglio(professionistaId, id);
    }
}
```

- [ ] **Step 8: Eseguire i test e verificare che passino**

Run: `mvn test -Dtest=PianoAlimentareControllerTest` da `backend/`
Expected: PASS

- [ ] **Step 9: Eseguire l'intera suite backend**

Run: `mvn test` da `backend/`
Expected: tutti i test verdi.

- [ ] **Step 10: Staging**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pianialimentari/
git add backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareControllerTest.java
```

---

### Task 4: Salvataggio (`PUT`) — sostituzione totale della struttura per tutte e tre le modalità

**Files:**
- Create: `backend/.../pianialimentari/RigaAlimentoRequest.java`, `PastoRequest.java`, `GiornoMacroTargetRequest.java`, `EsempioRequest.java`, `AggiornaPianoAlimentareRequest.java`
- Modify: `backend/.../pianialimentari/PianoAlimentareService.java` (metodo `aggiorna`)
- Modify: `backend/.../pianialimentari/PianoAlimentareController.java` (`PUT /piani-alimentari/{id}`)
- Test: `backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareControllerTest.java` (estende il file di Task 3)

**Interfaces:**
- Consumes: repository e entità di Task 1, `PianoAlimentareResponse`/`dettaglio(...)` di Task 3.
- Produces: `PianoAlimentareService.aggiorna(UUID professionistaId, UUID pianoId, AggiornaPianoAlimentareRequest request): PianoAlimentareResponse`, `PUT /piani-alimentari/{id}` — usato dal frontend (Task 11) per "Salva".

- [ ] **Step 1: DTO richiesta**

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record RigaAlimentoRequest(
        UUID alimentoId,
        @NotBlank String nome,
        @NotNull @PositiveOrZero BigDecimal kcal100g,
        @NotNull @PositiveOrZero BigDecimal proteine100g,
        @NotNull @PositiveOrZero BigDecimal carboidrati100g,
        @NotNull @PositiveOrZero BigDecimal grassi100g,
        @PositiveOrZero BigDecimal zuccheri100g,
        @NotNull @PositiveOrZero BigDecimal grammi
) {
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PastoRequest(
        @NotNull GiornoSettimana giornoSettimana,
        @NotBlank String nome,
        @NotNull TipoPasto tipo,
        String nota,
        @Valid List<RigaAlimentoRequest> righe
) {
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GiornoMacroTargetRequest(
        @NotNull GiornoSettimana giornoSettimana,
        BigDecimal kcalTarget,
        BigDecimal proteineTarget,
        BigDecimal carboidratiTarget,
        BigDecimal grassiTarget
) {
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EsempioRequest(
        @NotNull TipoPasto tipoPasto,
        @NotBlank String nome,
        @Valid List<RigaAlimentoRequest> righe
) {
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AggiornaPianoAlimentareRequest(
        @NotBlank String nome,
        LocalDate dataFine,
        BigDecimal obiettivoKcal,
        @Valid List<PastoRequest> pasti,
        @Valid List<GiornoMacroTargetRequest> giorniMacroTarget,
        @Valid List<EsempioRequest> esempi
) {
}
```

Le tre liste sono opzionali (`null`/assenti nel JSON per le due modalità non pertinenti): il frontend invia sempre e solo quella corrispondente a `modalita`, coerente con "un piano ha una sola modalità, fissata alla creazione".

- [ ] **Step 2: Estendere il test del controller**

Aggiungere al file `PianoAlimentareControllerTest.java` di Task 3:

```java
    @Test
    void aggiornaSostituisceLaStrutturaPastiEIRicalcoliDiTotaleRestanoLatoClient() throws Exception {
        String bodyCreazione = """
                {"pazienteId":"%s","nome":"Piano","modalita":"PASTI"}
                """.formatted(paziente.getId());
        String rispostaCreazione = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyCreazione))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(rispostaCreazione).get("id").asText());

        String bodyAggiornamento = """
                {
                  "nome": "Piano aggiornato",
                  "dataFine": "2026-10-10",
                  "obiettivoKcal": 2500,
                  "pasti": [
                    {
                      "giornoSettimana": "LUNEDI",
                      "nome": "Colazione",
                      "tipo": "COLAZIONE",
                      "nota": null,
                      "righe": [
                        {"alimentoId": null, "nome": "Avena in fiocchi", "kcal100g": 372, "proteine100g": 12.9,
                         "carboidrati100g": 65, "grassi100g": 6.5, "zuccheri100g": 1.1, "grammi": 60}
                      ]
                    }
                  ],
                  "giorniMacroTarget": null,
                  "esempi": null
                }
                """;

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyAggiornamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", is("Piano aggiornato")))
                .andExpect(jsonPath("$.obiettivoKcal", is(2500)))
                // I 35 pasti template sono sostituiti da un solo pasto: sostituzione totale, non merge.
                .andExpect(jsonPath("$.pasti.length()", is(1)))
                .andExpect(jsonPath("$.pasti[0].righe.length()", is(1)))
                .andExpect(jsonPath("$.pasti[0].righe[0].nome", is("Avena in fiocchi")));
    }

    @Test
    void nonPuoAggiornareUnPianoDiUnAltroProfessionista() throws Exception {
        Professionista altroProfessionista = professionistaRepository.save(
                new Professionista("altro@test.it", "$2a$10$hash", "Luca", "Verdi"));
        String bodyCreazione = """
                {"pazienteId":"%s","nome":"Piano","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String risposta = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyCreazione))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(risposta).get("id").asText());

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + tokenPer(altroProfessionista))
                        .contentType("application/json")
                        .content("""
                                {"nome":"x","dataFine":null,"obiettivoKcal":null,"pasti":null,
                                 "giorniMacroTarget":[],"esempi":null}
                                """))
                .andExpect(status().isNotFound());
    }
```

- [ ] **Step 3: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=PianoAlimentareControllerTest` da `backend/`
Expected: FAIL (metodo `aggiorna`/endpoint `PUT` non esistono ancora)

- [ ] **Step 4: Implementare `aggiorna` in `PianoAlimentareService`**

Aggiungere al file esistente (creato in Task 3):

```java
    @Transactional
    public PianoAlimentareResponse aggiorna(UUID professionistaId, UUID pianoId, AggiornaPianoAlimentareRequest request) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        piano.setNome(request.nome());
        piano.setDataFine(request.dataFine());
        piano.setObiettivoKcal(request.obiettivoKcal());
        pianoAlimentareRepository.save(piano);

        switch (piano.getModalita()) {
            case PASTI -> sostituisciPasti(pianoId, request.pasti() != null ? request.pasti() : List.of());
            case MACRO -> sostituisciGiorniMacro(pianoId,
                    request.giorniMacroTarget() != null ? request.giorniMacroTarget() : List.of());
            case ESEMPI -> sostituisciEsempi(pianoId, request.esempi() != null ? request.esempi() : List.of());
        }

        return dettaglio(professionistaId, pianoId);
    }

    private void sostituisciPasti(UUID pianoId, List<PastoRequest> richieste) {
        List<Pasto> esistenti = pastoRepository.findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(pianoId);
        List<UUID> idEsistenti = esistenti.stream().map(Pasto::getId).toList();
        pianoAlimentoRigaRepository.deleteAllByPastoIdIn(idEsistenti);
        pastoRepository.deleteAllByPianoId(pianoId);

        int ordine = 0;
        for (PastoRequest richiesta : richieste) {
            Pasto pasto = pastoRepository.save(new Pasto(pianoId, richiesta.giornoSettimana(), richiesta.nome(),
                    richiesta.tipo(), richiesta.nota(), ordine++));
            salvaRighePerPasto(pasto.getId(), richiesta.righe());
        }
    }

    private void salvaRighePerPasto(UUID pastoId, List<RigaAlimentoRequest> righe) {
        if (righe == null) {
            return;
        }
        int ordineRiga = 0;
        for (RigaAlimentoRequest riga : righe) {
            pianoAlimentoRigaRepository.save(PianoAlimentoRiga.perPasto(pastoId, riga.alimentoId(), riga.nome(),
                    riga.kcal100g(), riga.proteine100g(), riga.carboidrati100g(), riga.grassi100g(),
                    riga.zuccheri100g(), riga.grammi(), ordineRiga++));
        }
    }

    private void sostituisciGiorniMacro(UUID pianoId, List<GiornoMacroTargetRequest> richieste) {
        pianoGiornoMacroTargetRepository.deleteAllByPianoId(pianoId);
        for (GiornoMacroTargetRequest richiesta : richieste) {
            pianoGiornoMacroTargetRepository.save(new PianoGiornoMacroTarget(pianoId, richiesta.giornoSettimana(),
                    richiesta.kcalTarget(), richiesta.proteineTarget(), richiesta.carboidratiTarget(),
                    richiesta.grassiTarget()));
        }
    }

    private void sostituisciEsempi(UUID pianoId, List<EsempioRequest> richieste) {
        List<PianoEsempio> esistenti = pianoEsempioRepository.findAllByPianoIdOrderByTipoPastoAscOrdineAsc(pianoId);
        List<UUID> idEsistenti = esistenti.stream().map(PianoEsempio::getId).toList();
        pianoAlimentoRigaRepository.deleteAllByEsempioIdIn(idEsistenti);
        pianoEsempioRepository.deleteAllByPianoId(pianoId);

        int ordine = 0;
        for (EsempioRequest richiesta : richieste) {
            PianoEsempio esempio = pianoEsempioRepository.save(
                    new PianoEsempio(pianoId, richiesta.tipoPasto(), richiesta.nome(), ordine++));
            if (richiesta.righe() != null) {
                int ordineRiga = 0;
                for (RigaAlimentoRequest riga : richiesta.righe()) {
                    pianoAlimentoRigaRepository.save(PianoAlimentoRiga.perEsempio(esempio.getId(), riga.alimentoId(),
                            riga.nome(), riga.kcal100g(), riga.proteine100g(), riga.carboidrati100g(),
                            riga.grassi100g(), riga.zuccheri100g(), riga.grammi(), ordineRiga++));
                }
            }
        }
    }
```

Aggiungere `deleteAllByPianoId(UUID pianoId)` a `PianoEsempioRepository` (menzionato nell'interfaccia di Task 1 ma non ancora scritto esplicitamente lì — verificare che esista, altrimenti aggiungerlo ora).

- [ ] **Step 5: Aggiungere l'endpoint al controller**

Aggiungere a `PianoAlimentareController.java`:

```java
    @PutMapping("/{id}")
    public PianoAlimentareResponse aggiorna(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id,
                                              @Valid @RequestBody AggiornaPianoAlimentareRequest request) {
        return pianoAlimentareService.aggiorna(professionistaId, id, request);
    }
```

- [ ] **Step 6: Eseguire i test e verificare che passino**

Run: `mvn test -Dtest=PianoAlimentareControllerTest` da `backend/`
Expected: PASS

- [ ] **Step 7: Eseguire l'intera suite backend**

Run: `mvn test` da `backend/`
Expected: tutti i test verdi.

- [ ] **Step 8: Staging**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pianialimentari/
git add backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareControllerTest.java
```

---

### Task 5: Attivazione ed eliminazione

**Files:**
- Create: `backend/.../pianialimentari/PianoAlimentareNonEliminabileException.java`
- Modify: `backend/.../pianialimentari/PianoAlimentareService.java` (metodi `attiva`, `elimina`)
- Modify: `backend/.../pianialimentari/PianoAlimentareController.java` (`POST /{id}/attiva`, `DELETE /{id}`)
- Test: `backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareControllerTest.java` (estende il file di Task 3-4)

**Interfaces:**
- Consumes: `PianoAlimentareRepository.findAllByPazienteIdAndStato(...)` (Task 1), `pianoDiProprieta(...)` (Task 3).
- Produces: `PianoAlimentareService.attiva(UUID professionistaId, UUID pianoId): void`, `PianoAlimentareService.elimina(UUID professionistaId, UUID pianoId): void`.

- [ ] **Step 1: Eccezione**

```java
package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PianoAlimentareNonEliminabileException extends RuntimeException {
}
```

- [ ] **Step 2: Estendere il test del controller**

```java
    @Test
    void attivaDisattivaIlPianoPrecedenteDelloStessoPazienteEDiventaTerminato() throws Exception {
        String bodyPrimoPiano = """
                {"pazienteId":"%s","nome":"Fase 1","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaPrimoPiano = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(bodyPrimoPiano))
                .andReturn().getResponse().getContentAsString();
        UUID primoPianoId = UUID.fromString(objectMapper.readTree(rispostaPrimoPiano).get("id").asText());
        mockMvc.perform(post("/piani-alimentari/" + primoPianoId + "/attiva")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        String bodySecondoPiano = """
                {"pazienteId":"%s","nome":"Fase 2","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaSecondoPiano = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(bodySecondoPiano))
                .andReturn().getResponse().getContentAsString();
        UUID secondoPianoId = UUID.fromString(objectMapper.readTree(rispostaSecondoPiano).get("id").asText());
        mockMvc.perform(post("/piani-alimentari/" + secondoPianoId + "/attiva")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/piani-alimentari/" + primoPianoId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stato", is("TERMINATO")));
        mockMvc.perform(get("/piani-alimentari/" + secondoPianoId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stato", is("ATTIVO")));
    }

    @Test
    void eliminaUnaBozzaFunzionaMaUnPianoAttivoRestituisce409() throws Exception {
        String body = """
                {"pazienteId":"%s","nome":"Da eliminare","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String risposta = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json").content(body))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(risposta).get("id").asText());

        mockMvc.perform(post("/piani-alimentari/" + pianoId + "/attiva").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/piani-alimentari/" + pianoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        String bodyBozza = """
                {"pazienteId":"%s","nome":"Bozza","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaBozza = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json").content(bodyBozza))
                .andReturn().getResponse().getContentAsString();
        UUID bozzaId = UUID.fromString(objectMapper.readTree(rispostaBozza).get("id").asText());
        mockMvc.perform(delete("/piani-alimentari/" + bozzaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
```

(Import mancante da aggiungere in cima al file: `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;` — se non già coperto dalla `*` esistente.)

- [ ] **Step 3: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=PianoAlimentareControllerTest` da `backend/`
Expected: FAIL

- [ ] **Step 4: Implementare `attiva`/`elimina`**

Aggiungere a `PianoAlimentareService.java`:

```java
    @Transactional
    public void attiva(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        pianoAlimentareRepository.findAllByPazienteIdAndStato(piano.getPazienteId(), StatoPiano.ATTIVO)
                .forEach(precedente -> {
                    precedente.setStato(StatoPiano.TERMINATO);
                    pianoAlimentareRepository.save(precedente);
                });
        piano.setStato(StatoPiano.ATTIVO);
        pianoAlimentareRepository.save(piano);
    }

    @Transactional
    public void elimina(UUID professionistaId, UUID pianoId) {
        PianoAlimentare piano = pianoDiProprieta(professionistaId, pianoId);
        if (piano.getStato() != StatoPiano.BOZZA) {
            throw new PianoAlimentareNonEliminabileException();
        }
        switch (piano.getModalita()) {
            case PASTI -> sostituisciPasti(pianoId, List.of());
            case MACRO -> sostituisciGiorniMacro(pianoId, List.of());
            case ESEMPI -> sostituisciEsempi(pianoId, List.of());
        }
        pianoAlimentareRepository.delete(piano);
    }
```

(riusa `sostituisciPasti`/`sostituisciGiorniMacro`/`sostituisciEsempi` di Task 4 per ripulire la struttura figlia prima di cancellare il piano, invece di duplicare la logica di cancellazione.)

- [ ] **Step 5: Aggiungere gli endpoint al controller**

```java
    @PostMapping("/{id}/attiva")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attiva(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pianoAlimentareService.attiva(professionistaId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pianoAlimentareService.elimina(professionistaId, id);
    }
```

- [ ] **Step 6: Eseguire i test e verificare che passino**

Run: `mvn test -Dtest=PianoAlimentareControllerTest` da `backend/`
Expected: PASS

- [ ] **Step 7: Eseguire l'intera suite backend**

Run: `mvn test` da `backend/`
Expected: tutti i test verdi.

- [ ] **Step 8: Staging**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pianialimentari/
git add backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareControllerTest.java
```

---

### Task 6: Ricerca paginata (`GET /piani-alimentari/ricerca`)

**Files:**
- Create: `backend/.../pianialimentari/CampoOrdinamentoPianiAlimentari.java`, `DirezioneOrdinamento.java`
- Create: `backend/.../pianialimentari/CriteriRicercaPianiAlimentari.java`
- Create: `backend/.../pianialimentari/PianoAlimentareSpecifications.java`
- Create: `backend/.../pianialimentari/PianoAlimentareRigaListaResponse.java`, `PianoAlimentareListaPaginataResponse.java`
- Modify: `backend/.../pianialimentari/PianoAlimentareService.java` (metodo `cerca`)
- Modify: `backend/.../pianialimentari/PianoAlimentareController.java` (`GET /ricerca`)
- Test: `backend/src/test/java/com/hexisnutrition/backend/pianialimentari/PianoAlimentareSpecificationsTest.java`, estensione di `PianoAlimentareControllerTest.java`

**Interfaces:**
- Consumes: `PianoAlimentareRepository` (già `JpaSpecificationExecutor`, Task 1), `Paziente`/`PazienteRepository` (`pazienti`).
- Produces: `PianoAlimentareService.cerca(UUID professionistaId, CriteriRicercaPianiAlimentari criteri, Pageable pageable): Page<PianoAlimentare>`, `GET /piani-alimentari/ricerca` — consumato dalla lista frontend (Task 12).

- [ ] **Step 1: Enum ed criteri**

```java
package com.hexisnutrition.backend.pianialimentari;

public enum CampoOrdinamentoPianiAlimentari { nome, dataFine }
```

```java
package com.hexisnutrition.backend.pianialimentari;

public enum DirezioneOrdinamento { asc, desc }
```

```java
package com.hexisnutrition.backend.pianialimentari;

public record CriteriRicercaPianiAlimentari(
        String ricerca,
        StatoPianoVisualizzato stato
) {
}
```

- [ ] **Step 2: Specifications**

```java
package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class PianoAlimentareSpecifications {

    private PianoAlimentareSpecifications() {
    }

    public static Specification<PianoAlimentare> delProfessionista(UUID professionistaId) {
        return (root, query, cb) -> cb.equal(root.get("professionistaId"), professionistaId);
    }

    /** Match sul nome del piano o su nome/cognome del paziente collegato (nessuna relazione JPA: subquery correlata). */
    public static Specification<PianoAlimentare> conRicerca(String ricerca) {
        String pattern = "%" + ricerca.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<UUID> pazienteConNome = query.subquery(UUID.class);
            var pazienteRoot = pazienteConNome.from(Paziente.class);
            pazienteConNome.select(pazienteRoot.get("id"))
                    .where(cb.and(
                            cb.equal(pazienteRoot.get("id"), root.get("pazienteId")),
                            cb.or(
                                    cb.like(cb.lower(pazienteRoot.get("nome")), pattern),
                                    cb.like(cb.lower(pazienteRoot.get("cognome")), pattern))));
            return cb.or(cb.like(cb.lower(root.get("nome")), pattern), cb.exists(pazienteConNome));
        };
    }

    public static Specification<PianoAlimentare> conStato(StatoPianoVisualizzato stato) {
        LocalDate oggi = LocalDate.now();
        return switch (stato) {
            case BOZZA -> (root, query, cb) -> cb.equal(root.get("stato"), StatoPiano.BOZZA);
            case TERMINATO -> (root, query, cb) -> cb.equal(root.get("stato"), StatoPiano.TERMINATO);
            case ATTIVO -> (root, query, cb) -> cb.and(
                    cb.equal(root.get("stato"), StatoPiano.ATTIVO),
                    cb.or(cb.isNull(root.get("dataFine")), cb.greaterThanOrEqualTo(root.get("dataFine"), oggi)));
            case SCADUTO -> (root, query, cb) -> cb.and(
                    cb.equal(root.get("stato"), StatoPiano.ATTIVO),
                    cb.isNotNull(root.get("dataFine")),
                    cb.lessThan(root.get("dataFine"), oggi));
        };
    }
}
```

- [ ] **Step 3: Test delle specifications**

```java
package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PianoAlimentareSpecificationsTest extends AbstractIntegrationTest {

    @Autowired
    private PianoAlimentareRepository pianoAlimentareRepository;
    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Test
    void conStatoScadutoTrovaSoloAttiviConDataFinePassata() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@test.it", "hash", "Anna", "Rossi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        PianoAlimentare scaduto = new PianoAlimentare(paziente.getId(), professionista.getId(), null, "Scaduto",
                ModalitaPiano.MACRO);
        scaduto.setStato(StatoPiano.ATTIVO);
        scaduto.setDataFine(LocalDate.now().minusDays(1));
        pianoAlimentareRepository.save(scaduto);

        PianoAlimentare ancoraAttivo = new PianoAlimentare(paziente.getId(), professionista.getId(), null, "Attivo",
                ModalitaPiano.MACRO);
        ancoraAttivo.setStato(StatoPiano.ATTIVO);
        ancoraAttivo.setDataFine(LocalDate.now().plusDays(10));
        pianoAlimentareRepository.save(ancoraAttivo);

        var risultato = pianoAlimentareRepository.findAll(
                Specification.allOf(
                        PianoAlimentareSpecifications.delProfessionista(professionista.getId()),
                        PianoAlimentareSpecifications.conStato(StatoPianoVisualizzato.SCADUTO)),
                PageRequest.of(0, 10));

        assertThat(risultato.getContent()).extracting("nome").containsExactly("Scaduto");
    }

    @Test
    void conRicercaTrovaAncheSulNomeDelPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof2@test.it", "hash", "Anna", "Rossi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI80A01H501U", "giulia@test.it", null, LocalDate.of(1990, 1, 1), Sesso.F, null, null, null));
        pianoAlimentareRepository.save(new PianoAlimentare(paziente.getId(), professionista.getId(), null,
                "Piano X", ModalitaPiano.MACRO));

        var risultato = pianoAlimentareRepository.findAll(
                Specification.allOf(
                        PianoAlimentareSpecifications.delProfessionista(professionista.getId()),
                        PianoAlimentareSpecifications.conRicerca("giulia")),
                PageRequest.of(0, 10));

        assertThat(risultato.getContent()).hasSize(1);
    }
}
```

- [ ] **Step 4: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=PianoAlimentareSpecificationsTest` da `backend/`
Expected: FAIL (classe non esiste)

- [ ] **Step 5: DTO di risposta lista**

```java
package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PianoAlimentareRigaListaResponse(
        UUID id,
        String pazienteNomeCompleto,
        String nome,
        StatoPianoVisualizzato stato,
        BigDecimal obiettivoKcal,
        LocalDate dataFine
) {
}
```

```java
package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.domain.Page;

import java.util.List;

public record PianoAlimentareListaPaginataResponse(
        List<PianoAlimentareRigaListaResponse> contenuto,
        int paginaCorrente,
        int dimensionePagina,
        long totaleElementi,
        int totalePagine
) {
}
```

- [ ] **Step 6: Implementare `cerca` nel servizio**

Aggiungere a `PianoAlimentareService.java` (nuova dipendenza `PazienteRepository` già iniettata da Task 3):

```java
    public org.springframework.data.domain.Page<PianoAlimentare> cerca(UUID professionistaId,
            CriteriRicercaPianiAlimentari criteri, org.springframework.data.domain.Pageable pageable) {
        List<org.springframework.data.jpa.domain.Specification<PianoAlimentare>> specifiche = new java.util.ArrayList<>();
        specifiche.add(PianoAlimentareSpecifications.delProfessionista(professionistaId));
        if (criteri.ricerca() != null && !criteri.ricerca().isBlank()) {
            specifiche.add(PianoAlimentareSpecifications.conRicerca(criteri.ricerca()));
        }
        if (criteri.stato() != null) {
            specifiche.add(PianoAlimentareSpecifications.conStato(criteri.stato()));
        }
        return pianoAlimentareRepository.findAll(
                org.springframework.data.jpa.domain.Specification.allOf(specifiche), pageable);
    }
```

- [ ] **Step 7: Aggiungere l'endpoint al controller** (con la mappatura verso `PianoAlimentareRigaListaResponse`, batch-fetch dei pazienti per evitare N+1)

```java
    @GetMapping("/ricerca")
    public PianoAlimentareListaPaginataResponse ricerca(
            @AuthenticationPrincipal UUID professionistaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int dimensione,
            @RequestParam(defaultValue = "nome") CampoOrdinamentoPianiAlimentari ordinaPer,
            @RequestParam(defaultValue = "asc") DirezioneOrdinamento direzione,
            @RequestParam(required = false) String ricerca,
            @RequestParam(required = false) StatoPianoVisualizzato stato) {
        int paginaEffettiva = Math.max(pagina, 0);
        int dimensioneEffettiva = Math.min(Math.max(dimensione, 1), 100);
        org.springframework.data.domain.Sort.Direction direzioneSort =
                direzione == DirezioneOrdinamento.desc
                        ? org.springframework.data.domain.Sort.Direction.DESC
                        : org.springframework.data.domain.Sort.Direction.ASC;
        var pageable = org.springframework.data.domain.PageRequest.of(paginaEffettiva, dimensioneEffettiva,
                org.springframework.data.domain.Sort.by(direzioneSort, ordinaPer.name()));
        var pagina2 = pianoAlimentareService.cerca(professionistaId,
                new CriteriRicercaPianiAlimentari(ricerca, stato), pageable);

        var pazientiPerId = pazienteRepository.findAllById(
                pagina2.getContent().stream().map(PianoAlimentare::getPazienteId).distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(com.hexisnutrition.backend.pazienti.Paziente::getId,
                        p -> p.getNome() + " " + p.getCognome()));

        var contenuto = pagina2.getContent().stream()
                .map(p -> new PianoAlimentareRigaListaResponse(p.getId(),
                        pazientiPerId.getOrDefault(p.getPazienteId(), "—"), p.getNome(), p.statoEffettivo(),
                        p.getObiettivoKcal(), p.getDataFine()))
                .toList();

        return new PianoAlimentareListaPaginataResponse(contenuto, pagina2.getNumber(), pagina2.getSize(),
                pagina2.getTotalElements(), pagina2.getTotalPages());
    }
```

Il controller ha bisogno di `PazienteRepository` iniettato (nuovo parametro nel costruttore) — aggiungerlo lì, non nel servizio, dato che è solo per arricchire la risposta della lista.

- [ ] **Step 8: Eseguire i test e verificare che passino**

Run: `mvn test -Dtest=PianoAlimentareSpecificationsTest,PianoAlimentareControllerTest` da `backend/`
Expected: PASS

- [ ] **Step 9: Eseguire l'intera suite backend**

Run: `mvn test` da `backend/`
Expected: tutti i test verdi. Aggiornare `wiki/api-contracts.md` e `wiki/modello-dati.md` con i 6 endpoint e le 5 nuove tabelle, come richiesto da `backend/CLAUDE.md`.

- [ ] **Step 10: Staging**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pianialimentari/
git add backend/src/test/java/com/hexisnutrition/backend/pianialimentari/
git add wiki/api-contracts.md wiki/modello-dati.md
```

---

### Task 7: Client API frontend (`api/pianiAlimentari.ts`)

**Files:**
- Create: `frontend-professionisti/src/api/pianiAlimentari.ts`
- Test: `frontend-professionisti/src/api/pianiAlimentari.spec.ts`

**Interfaces:**
- Consumes: `apiRequest` da `./client` (esistente).
- Produces: tutti i tipi TypeScript e le funzioni `crea`/`dettaglio`/`aggiorna`/`attiva`/`elimina`/`cerca`, usati da ogni view successiva (Task 8-12).

- [ ] **Step 1: Scrivere il test (guida i tipi e le firme)**

```typescript
import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { crea, dettaglio, aggiorna, attiva, elimina, cerca } from './pianiAlimentari'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

const pianoEsempio = {
  id: '1', pazienteId: 'p1', pazienteNomeCompleto: 'Mario Bianchi', nome: 'Fase 1', modalita: 'PASTI',
  stato: 'BOZZA', dataInizio: '2026-09-12', dataFine: null, obiettivoKcal: 2400, obiettivoKcalSuggerito: 2400,
  bmrCalcolato: 1700, tdeeCalcolato: 2400, formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: false,
  pasti: [], giorniMacroTarget: [], esempi: [],
}

describe('api/pianiAlimentari', () => {
  it('crea chiama POST /piani-alimentari', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pianoEsempio)

    await crea({ pazienteId: 'p1', nome: 'Fase 1', modalita: 'PASTI' })

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari', {
      method: 'POST',
      body: { pazienteId: 'p1', nome: 'Fase 1', modalita: 'PASTI' },
    })
  })

  it('dettaglio chiama GET /piani-alimentari/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pianoEsempio)

    await dettaglio('1')

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1')
  })

  it('aggiorna chiama PUT /piani-alimentari/{id} con la struttura completa', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pianoEsempio)

    await aggiorna('1', {
      nome: 'Fase 1', dataFine: '2026-10-10', obiettivoKcal: 2400,
      pasti: [{ giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, righe: [] }],
      giorniMacroTarget: null, esempi: null,
    })

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1', {
      method: 'PUT',
      body: {
        nome: 'Fase 1', dataFine: '2026-10-10', obiettivoKcal: 2400,
        pasti: [{ giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, righe: [] }],
        giorniMacroTarget: null, esempi: null,
      },
    })
  })

  it('attiva chiama POST /piani-alimentari/{id}/attiva', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await attiva('1')

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1/attiva', { method: 'POST' })
  })

  it('elimina chiama DELETE /piani-alimentari/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await elimina('1')

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1', { method: 'DELETE' })
  })

  it('cerca chiama GET /piani-alimentari/ricerca senza parametri se non specificati', async () => {
    const pagina = { contenuto: [], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 0, totalePagine: 0 }
    vi.mocked(apiRequest).mockResolvedValue(pagina)

    await cerca()

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/ricerca')
  })

  it('cerca costruisce la query string con tutti i filtri passati', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ contenuto: [], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 0, totalePagine: 0 })

    await cerca({ pagina: 1, dimensione: 10, ricerca: 'mario', stato: 'ATTIVO', ordinaPer: 'dataFine', direzione: 'desc' })

    expect(apiRequest).toHaveBeenCalledWith(
      '/piani-alimentari/ricerca?pagina=1&dimensione=10&ordinaPer=dataFine&direzione=desc&ricerca=mario&stato=ATTIVO',
    )
  })
})
```

- [ ] **Step 2: Eseguire il test e verificare che fallisca**

Run: `npx vitest run src/api/pianiAlimentari.spec.ts` da `frontend-professionisti/`
Expected: FAIL (modulo non esiste)

- [ ] **Step 3: Implementare `api/pianiAlimentari.ts`**

```typescript
import { apiRequest } from './client'

export type ModalitaPiano = 'PASTI' | 'MACRO' | 'ESEMPI'
export type StatoPiano = 'BOZZA' | 'ATTIVO' | 'SCADUTO' | 'TERMINATO'
export type FormulaBmr = 'KATCH_MCARDLE' | 'MIFFLIN_ST_JEOR'
export type GiornoSettimana = 'LUNEDI' | 'MARTEDI' | 'MERCOLEDI' | 'GIOVEDI' | 'VENERDI' | 'SABATO' | 'DOMENICA'
export type TipoPasto = 'COLAZIONE' | 'SPUNTINO_MATTINA' | 'PRANZO' | 'SPUNTINO_POMERIGGIO' | 'CENA' | 'ALTRO'

export interface RigaAlimento {
  id: string
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  grammi: number
}

export interface Pasto {
  id: string
  giornoSettimana: GiornoSettimana
  nome: string
  tipo: TipoPasto
  nota: string | null
  ordine: number
  righe: RigaAlimento[]
}

export interface GiornoMacroTarget {
  giornoSettimana: GiornoSettimana
  kcalTarget: number | null
  proteineTarget: number | null
  carboidratiTarget: number | null
  grassiTarget: number | null
}

export interface Esempio {
  id: string
  tipoPasto: TipoPasto
  nome: string
  ordine: number
  righe: RigaAlimento[]
}

export interface PianoAlimentare {
  id: string
  pazienteId: string
  pazienteNomeCompleto: string
  nome: string
  modalita: ModalitaPiano
  stato: StatoPiano
  dataInizio: string
  dataFine: string | null
  obiettivoKcal: number | null
  obiettivoKcalSuggerito: number | null
  bmrCalcolato: number | null
  tdeeCalcolato: number | null
  formulaBmrUsata: FormulaBmr | null
  sottoSogliaSicurezza: boolean
  pasti: Pasto[]
  giorniMacroTarget: GiornoMacroTarget[]
  esempi: Esempio[]
}

export interface CreaPianoAlimentareRequest {
  pazienteId: string
  nome: string
  modalita: ModalitaPiano
}

export interface RigaAlimentoRequest {
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  grammi: number
}

export interface PastoRequest {
  giornoSettimana: GiornoSettimana
  nome: string
  tipo: TipoPasto
  nota: string | null
  righe: RigaAlimentoRequest[]
}

export interface GiornoMacroTargetRequest {
  giornoSettimana: GiornoSettimana
  kcalTarget: number | null
  proteineTarget: number | null
  carboidratiTarget: number | null
  grassiTarget: number | null
}

export interface EsempioRequest {
  tipoPasto: TipoPasto
  nome: string
  righe: RigaAlimentoRequest[]
}

export interface AggiornaPianoAlimentareRequest {
  nome: string
  dataFine: string | null
  obiettivoKcal: number | null
  pasti: PastoRequest[] | null
  giorniMacroTarget: GiornoMacroTargetRequest[] | null
  esempi: EsempioRequest[] | null
}

export interface CriteriRicercaPianiAlimentari {
  pagina?: number
  dimensione?: number
  ordinaPer?: 'nome' | 'dataFine'
  direzione?: 'asc' | 'desc'
  ricerca?: string
  stato?: StatoPiano
}

export interface PianoAlimentareRigaLista {
  id: string
  pazienteNomeCompleto: string
  nome: string
  stato: StatoPiano
  obiettivoKcal: number | null
  dataFine: string | null
}

export interface PaginaPianiAlimentari {
  contenuto: PianoAlimentareRigaLista[]
  paginaCorrente: number
  dimensionePagina: number
  totaleElementi: number
  totalePagine: number
}

export function crea(request: CreaPianoAlimentareRequest): Promise<PianoAlimentare> {
  return apiRequest<PianoAlimentare>('/piani-alimentari', { method: 'POST', body: request })
}

export function dettaglio(id: string): Promise<PianoAlimentare> {
  return apiRequest<PianoAlimentare>(`/piani-alimentari/${id}`)
}

export function aggiorna(id: string, request: AggiornaPianoAlimentareRequest): Promise<PianoAlimentare> {
  return apiRequest<PianoAlimentare>(`/piani-alimentari/${id}`, { method: 'PUT', body: request })
}

export function attiva(id: string): Promise<void> {
  return apiRequest<void>(`/piani-alimentari/${id}/attiva`, { method: 'POST' })
}

export function elimina(id: string): Promise<void> {
  return apiRequest<void>(`/piani-alimentari/${id}`, { method: 'DELETE' })
}

export function cerca(criteri: CriteriRicercaPianiAlimentari = {}): Promise<PaginaPianiAlimentari> {
  const parametri = new URLSearchParams()
  if (criteri.pagina !== undefined) parametri.set('pagina', String(criteri.pagina))
  if (criteri.dimensione !== undefined) parametri.set('dimensione', String(criteri.dimensione))
  if (criteri.ordinaPer) parametri.set('ordinaPer', criteri.ordinaPer)
  if (criteri.direzione) parametri.set('direzione', criteri.direzione)
  if (criteri.ricerca) parametri.set('ricerca', criteri.ricerca)
  if (criteri.stato) parametri.set('stato', criteri.stato)

  const query = parametri.toString()
  return apiRequest<PaginaPianiAlimentari>(`/piani-alimentari/ricerca${query ? `?${query}` : ''}`)
}
```

- [ ] **Step 4: Eseguire il test e verificare che passi**

Run: `npx vitest run src/api/pianiAlimentari.spec.ts` da `frontend-professionisti/`
Expected: PASS, 7 test verdi.

- [ ] **Step 5: `tsc --noEmit`**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: pulito.

- [ ] **Step 6: Staging**

```bash
git add frontend-professionisti/src/api/pianiAlimentari.ts frontend-professionisti/src/api/pianiAlimentari.spec.ts
```

---

### Task 8: `AggiungiAlimentoDialog.vue` — modal condiviso (ricerca dal catalogo o manuale)

**Files:**
- Create: `frontend-professionisti/src/components/pianiAlimentari/AggiungiAlimentoDialog.vue`
- Test: `frontend-professionisti/src/components/pianiAlimentari/AggiungiAlimentoDialog.spec.ts`

**Interfaces:**
- Consumes: `cerca` da `@/api/alimenti` (esistente), `filtraDecimaleItaliano`/`erroreNumeroDecimale`/`erroreNumeroDecimaleObbligatorio`/`erroreNomeAlimento` da `@/utils/validators` (esistenti), componenti `Dialog`/`DialogContent`/`DialogHeader`/`DialogTitle` di shadcn-vue.
- Produces: componente `<AggiungiAlimentoDialog v-model:open="..." @aggiunto="..." />`, evento `aggiunto` con payload `{ alimentoId: string | null; nome: string; kcal100g: number; proteine100g: number; carboidrati100g: number; grassi100g: number; zuccheri100g: number | null; grammi: number }` — consumato da Task 9 (pasti) e Task 10 (esempi).

- [ ] **Step 1: Scrivere il test**

```typescript
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import AggiungiAlimentoDialog from './AggiungiAlimentoDialog.vue'
import { cerca } from '@/api/alimenti'

vi.mock('@/api/alimenti', () => ({ cerca: vi.fn() }))

describe('AggiungiAlimentoDialog', () => {
  beforeEach(() => {
    vi.mocked(cerca).mockResolvedValue({
      contenuto: [{ id: 'a1', nome: 'Avena in fiocchi', categoria: 'Cereali', quantitaG: 100, kcal: 372,
        proteineG: 12.9, grassiG: 6.5, carboidratiG: 65, acquaG: null, fibreG: null, zuccheriG: 1.1,
        ferroMg: null, calcioMg: null, sodioMg: null, bda: true }],
      paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1,
    })
  })

  it('selezionando un risultato di ricerca emette aggiunto con grammi 100 e chiude il dialog', async () => {
    const wrapper = mount(AggiungiAlimentoDialog, { props: { open: true } })
    await wrapper.find('input[placeholder="Cerca per nome o categoria…"]').setValue('avena')
    await vi.waitFor(() => expect(cerca).toHaveBeenCalled())
    await nextTick()

    await wrapper.find('[data-test="risultato-ricerca"]').trigger('click')

    expect(wrapper.emitted('aggiunto')?.[0]?.[0]).toEqual({
      alimentoId: 'a1', nome: 'Avena in fiocchi', kcal100g: 372, proteine100g: 12.9, carboidrati100g: 65,
      grassi100g: 6.5, zuccheri100g: 1.1, grammi: 100,
    })
    expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
  })

  it('richiede nome e i 4 macro obbligatori nella modalità manuale', async () => {
    const wrapper = mount(AggiungiAlimentoDialog, { props: { open: true } })
    await wrapper.find('[data-test="tab-manuale"]').trigger('click')

    await wrapper.find('[data-test="aggiungi-manuale"]').trigger('click')

    expect(wrapper.emitted('aggiunto')).toBeUndefined()
    expect(wrapper.text()).toContain('obbligatorio')
  })

  it('in modalità manuale emette aggiunto con i valori inseriti e alimentoId nullo', async () => {
    const wrapper = mount(AggiungiAlimentoDialog, { props: { open: true } })
    await wrapper.find('[data-test="tab-manuale"]').trigger('click')
    await wrapper.find('[data-test="manuale-nome"]').setValue('Insalata mista')
    await wrapper.find('[data-test="manuale-kcal"]').setValue('50')
    await wrapper.find('[data-test="manuale-proteine"]').setValue('2')
    await wrapper.find('[data-test="manuale-carboidrati"]').setValue('5')
    await wrapper.find('[data-test="manuale-grassi"]').setValue('1')

    await wrapper.find('[data-test="aggiungi-manuale"]').trigger('click')

    expect(wrapper.emitted('aggiunto')?.[0]?.[0]).toEqual({
      alimentoId: null, nome: 'Insalata mista', kcal100g: 50, proteine100g: 2, carboidrati100g: 5,
      grassi100g: 1, zuccheri100g: null, grammi: 100,
    })
  })
})
```

- [ ] **Step 2: Eseguire il test e verificare che fallisca**

Run: `npx vitest run src/components/pianiAlimentari/AggiungiAlimentoDialog.spec.ts` da `frontend-professionisti/`
Expected: FAIL (componente non esiste)

- [ ] **Step 3: Implementare il componente**

Layout ripreso dal modal "Aggiungi alimento" del mockup (`Hexis Piano Alimentare.dc.html`, righe 359-422 della copia estratta fornita da Andrea): due tab manuali ("Cerca dal database" / "Alimento manuale", bottoni semplici con bordo inferiore colorato sull'attivo — **non** un componente `Tabs` di shadcn, non installato in questo progetto), lista risultati con nome/categoria/kcal, form manuale con `Nome`, `Grammi`, e i 4 campi macro obbligatori + zuccheri opzionale.

```vue
<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { cerca as cercaAlimenti, type Alimento } from '@/api/alimenti'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { filtraDecimaleItaliano, numeroItaliano, numeroItalianoOpzionale, erroreNomeAlimento, erroreNumeroDecimaleObbligatorio, erroreNumeroDecimale } from '@/utils/validators'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [valore: boolean]
  aggiunto: [riga: {
    alimentoId: string | null
    nome: string
    kcal100g: number
    proteine100g: number
    carboidrati100g: number
    grassi100g: number
    zuccheri100g: number | null
    grammi: number
  }]
}>()

const modo = ref<'cerca' | 'manuale'>('cerca')
const query = ref('')
const risultati = ref<Alimento[]>([])
let debounceHandle: ReturnType<typeof setTimeout> | undefined

async function eseguiRicerca(testo: string) {
  if (!testo.trim()) {
    risultati.value = []
    return
  }
  const pagina = await cercaAlimenti({ ricerca: testo.trim(), dimensione: 20 })
  risultati.value = pagina.contenuto
}

watch(query, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => eseguiRicerca(valore), 300)
})
onUnmounted(() => clearTimeout(debounceHandle))

const manualeNome = ref('')
const manualeGrammi = ref('100')
const manualeKcal = ref('')
const manualeProteine = ref('')
const manualeCarboidrati = ref('')
const manualeGrassi = ref('')
const manualeZuccheri = ref('')
const errori = ref<Record<string, string | undefined>>({})

function resetStato() {
  modo.value = 'cerca'
  query.value = ''
  risultati.value = []
  manualeNome.value = ''
  manualeGrammi.value = '100'
  manualeKcal.value = ''
  manualeProteine.value = ''
  manualeCarboidrati.value = ''
  manualeGrassi.value = ''
  manualeZuccheri.value = ''
  errori.value = {}
}

function chiudi() {
  emit('update:open', false)
  resetStato()
}

function selezionaRisultato(alimento: Alimento) {
  emit('aggiunto', {
    alimentoId: alimento.id,
    nome: alimento.nome,
    kcal100g: alimento.kcal,
    proteine100g: alimento.proteineG,
    carboidrati100g: alimento.carboidratiG,
    grassi100g: alimento.grassiG,
    zuccheri100g: alimento.zuccheriG,
    grammi: 100,
  })
  chiudi()
}

function aggiungiManuale() {
  const nuoviErrori: Record<string, string | undefined> = {
    nome: erroreNomeAlimento(manualeNome.value),
    kcal: erroreNumeroDecimaleObbligatorio(manualeKcal.value),
    proteine: erroreNumeroDecimaleObbligatorio(manualeProteine.value),
    carboidrati: erroreNumeroDecimaleObbligatorio(manualeCarboidrati.value),
    grassi: erroreNumeroDecimaleObbligatorio(manualeGrassi.value),
    zuccheri: erroreNumeroDecimale(manualeZuccheri.value),
  }
  errori.value = nuoviErrori
  if (Object.values(nuoviErrori).some((e) => e !== undefined)) return

  emit('aggiunto', {
    alimentoId: null,
    nome: manualeNome.value.trim(),
    kcal100g: numeroItaliano(manualeKcal.value),
    proteine100g: numeroItaliano(manualeProteine.value),
    carboidrati100g: numeroItaliano(manualeCarboidrati.value),
    grassi100g: numeroItaliano(manualeGrassi.value),
    zuccheri100g: numeroItalianoOpzionale(manualeZuccheri.value) ?? null,
    grammi: numeroItalianoOpzionale(manualeGrammi.value) ?? 100,
  })
  chiudi()
}
</script>

<template>
  <Dialog :open="props.open" @update:open="(v) => !v && chiudi()">
    <DialogContent class="w-[480px] max-w-full">
      <DialogHeader>
        <DialogTitle class="font-heading italic">Aggiungi alimento</DialogTitle>
      </DialogHeader>

      <div class="flex gap-1 border-b border-(--div) px-1">
        <button
          data-test="tab-cerca"
          class="border-b-2 px-1 py-2 text-sm font-bold"
          :class="modo === 'cerca' ? 'border-(--green) text-(--green)' : 'border-transparent text-(--fg3)'"
          @click="modo = 'cerca'"
        >
          Cerca dal database
        </button>
        <button
          data-test="tab-manuale"
          class="border-b-2 px-1 py-2 text-sm font-bold"
          :class="modo === 'manuale' ? 'border-(--green) text-(--green)' : 'border-transparent text-(--fg3)'"
          @click="modo = 'manuale'"
        >
          Alimento manuale
        </button>
      </div>

      <div v-if="modo === 'cerca'" class="flex flex-col gap-3">
        <Input v-model="query" placeholder="Cerca per nome o categoria…" />
        <div class="flex max-h-70 flex-col gap-1 overflow-y-auto">
          <button
            v-for="risultato in risultati"
            :key="risultato.id"
            data-test="risultato-ricerca"
            class="flex items-center justify-between gap-3 rounded-lg px-2 py-2 text-left hover:bg-(--soft)"
            @click="selezionaRisultato(risultato)"
          >
            <span class="flex flex-col">
              <span class="text-sm font-semibold text-(--fg)">{{ risultato.nome }}</span>
              <span class="text-xs text-(--fg3)">{{ risultato.categoria }}</span>
            </span>
            <span class="whitespace-nowrap text-xs font-bold text-(--fg3)">{{ risultato.kcal }} kcal/100g</span>
          </button>
          <p v-if="query.trim() && risultati.length === 0" class="p-4 text-center text-sm text-(--fg3)">
            Nessun alimento trovato.
          </p>
        </div>
      </div>

      <div v-else class="flex flex-col gap-3">
        <label class="flex flex-col gap-1">
          <span class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Nome alimento</span>
          <Input data-test="manuale-nome" v-model="manualeNome" placeholder="Es. Insalata mista condita" />
          <span v-if="errori.nome" class="text-xs text-(--danger)">{{ errori.nome }}</span>
        </label>
        <div class="grid grid-cols-2 gap-3">
          <label class="flex flex-col gap-1">
            <span class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Grammi</span>
            <Input
              type="text" inputmode="decimal"
              :model-value="manualeGrammi"
              @update:model-value="(v) => (manualeGrammi = filtraDecimaleItaliano(String(v)))"
            />
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Kcal</span>
            <Input
              data-test="manuale-kcal" type="text" inputmode="decimal"
              :model-value="manualeKcal"
              @update:model-value="(v) => { manualeKcal = filtraDecimaleItaliano(String(v)); errori.kcal = undefined }"
            />
            <span v-if="errori.kcal" class="text-xs text-(--danger)">{{ errori.kcal }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Proteine (g)</span>
            <Input
              data-test="manuale-proteine" type="text" inputmode="decimal"
              :model-value="manualeProteine"
              @update:model-value="(v) => { manualeProteine = filtraDecimaleItaliano(String(v)); errori.proteine = undefined }"
            />
            <span v-if="errori.proteine" class="text-xs text-(--danger)">{{ errori.proteine }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Carboidrati (g)</span>
            <Input
              data-test="manuale-carboidrati" type="text" inputmode="decimal"
              :model-value="manualeCarboidrati"
              @update:model-value="(v) => { manualeCarboidrati = filtraDecimaleItaliano(String(v)); errori.carboidrati = undefined }"
            />
            <span v-if="errori.carboidrati" class="text-xs text-(--danger)">{{ errori.carboidrati }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Grassi (g)</span>
            <Input
              data-test="manuale-grassi" type="text" inputmode="decimal"
              :model-value="manualeGrassi"
              @update:model-value="(v) => { manualeGrassi = filtraDecimaleItaliano(String(v)); errori.grassi = undefined }"
            />
            <span v-if="errori.grassi" class="text-xs text-(--danger)">{{ errori.grassi }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Zuccheri (g)</span>
            <Input
              type="text" inputmode="decimal"
              :model-value="manualeZuccheri"
              @update:model-value="(v) => (manualeZuccheri = filtraDecimaleItaliano(String(v)))"
            />
          </label>
        </div>
        <p class="text-xs text-(--fg4)">Valori per 100 g.</p>
        <Button data-test="aggiungi-manuale" class="w-full" @click="aggiungiManuale">Aggiungi al pasto</Button>
      </div>
    </DialogContent>
  </Dialog>
</template>
```

- [ ] **Step 4: Eseguire il test e verificare che passi**

Run: `npx vitest run src/components/pianiAlimentari/AggiungiAlimentoDialog.spec.ts` da `frontend-professionisti/`
Expected: PASS. Se il selettore `input[placeholder="Cerca per nome o categoria…"]` non trova l'input dentro `Input` di shadcn-vue (wrapper che inoltra gli attributi), verificare come lo fa `SelezionaPazienteCombobox.spec.ts` (se esiste) o `AlimentoFormDialog.spec.ts` per il pattern corretto di query nei test.

- [ ] **Step 5: `tsc --noEmit`**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: pulito.

- [ ] **Step 6: Staging**

```bash
git add frontend-professionisti/src/components/pianiAlimentari/
```

---

### Task 9: `PianoAlimentareFormView.vue` — creazione, shell dell'editor, modalità PASTI

**Files:**
- Create: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.vue`
- Create: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts`
- Modify: `frontend-professionisti/src/router/index.ts` (nuove rotte `piano-alimentare-nuovo`, `piano-alimentare-modifica`)

**Interfaces:**
- Consumes: `api/pianiAlimentari.ts` (Task 7, tutte le funzioni e i tipi), `AggiungiAlimentoDialog.vue` (Task 8, evento `aggiunto`), `SelezionaPazienteCombobox.vue` (esistente), `filtraDecimaleItaliano`/`erroreNumeroDecimale` (esistenti).
- Produces: rotte `/piani-alimentari/nuovo` (+ `?pazienteId=`) e `/piani-alimentari/:id`; struttura dati locale `PastoLocale`/`RigaLocale` (con `clientId` per il `:key` di Vue, dato che le righe nuove non hanno ancora un `id` dal server) — riusata identica nel rendering MACRO/ESEMPI dei Task 10-11 e dalle azioni di Task 11.

**Nota di riferimento visivo**: questo task ricalca il mockup `Hexis Piano Alimentare.dc.html` (estratto da Andrea in una cartella locale fuori dal repo) per la modalità "Piano con pasti": intestazione con nome piano editabile inline, riquadro obiettivo/data fine, sidebar dei 7 giorni, card per pasto con tabella alimenti. Usare quel file come riferimento per rifiniture di spaziatura/colore non specificate esplicitamente sotto, adattando sempre a classi Tailwind (mai `style="..."` inline) e ai componenti shadcn-vue già presenti in `components/ui/`.

- [ ] **Step 1: Aggiungere le rotte**

In `router/index.ts`, aggiungere dopo la riga delle rotte `alimenti`:

```typescript
    { path: '/piani-alimentari/nuovo', name: 'piano-alimentare-nuovo', component: () => import('@/views/pianiAlimentari/PianoAlimentareFormView.vue'), meta: { requiresAuth: true } },
    { path: '/piani-alimentari/:id', name: 'piano-alimentare-modifica', component: () => import('@/views/pianiAlimentari/PianoAlimentareFormView.vue'), meta: { requiresAuth: true } },
```

(la rotta lista, `/piani-alimentari`, arriva con Task 12 — se questo task viene eseguito da solo prima, il link "Piani alimentari"/"← Piani alimentari" punta temporaneamente a una rotta non ancora registrata: va bene, non blocca i test di questo componente).

- [ ] **Step 2: Scrivere il test (creazione + caricamento esistente + aggiunta/rimozione pasto e alimento)**

```typescript
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import PianoAlimentareFormView from './PianoAlimentareFormView.vue'
import * as api from '@/api/pianiAlimentari'

vi.mock('@/api/pianiAlimentari')

const pianoVuoto = {
  id: 'piano1', pazienteId: 'p1', pazienteNomeCompleto: 'Mario Bianchi', nome: 'Ipertrofia · fase 1',
  modalita: 'PASTI', stato: 'BOZZA', dataInizio: '2026-09-12', dataFine: null,
  obiettivoKcal: 2800, obiettivoKcalSuggerito: 2800, bmrCalcolato: 1806.45, tdeeCalcolato: 2800,
  formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: false,
  pasti: [
    { id: 'pasto1', giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, ordine: 0, righe: [] },
  ],
  giorniMacroTarget: [], esempi: [],
}

async function creaRouter(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/piani-alimentari/nuovo', name: 'piano-alimentare-nuovo', component: PianoAlimentareFormView },
      { path: '/piani-alimentari/:id', name: 'piano-alimentare-modifica', component: PianoAlimentareFormView },
    ],
  })
  router.push(path)
  await router.isReady()
  return router
}

describe('PianoAlimentareFormView', () => {
  beforeEach(() => {
    vi.mocked(api.dettaglio).mockResolvedValue(pianoVuoto as never)
    vi.mocked(api.crea).mockResolvedValue(pianoVuoto as never)
    vi.mocked(api.aggiorna).mockResolvedValue(pianoVuoto as never)
  })

  it('carica un piano esistente da /piani-alimentari/:id e mostra il pasto già presente', async () => {
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalledWith('piano1'))
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Colazione')
    expect(wrapper.text()).toContain('Mario Bianchi')
  })

  it('aggiungere un pasto lo mostra nel giorno selezionato', async () => {
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="aggiungi-pasto"]').trigger('click')

    expect(wrapper.findAll('[data-test="card-pasto"]')).toHaveLength(2)
  })

  it('aggiungere un alimento tramite il dialog lo mostra nella tabella del pasto', async () => {
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="apri-aggiungi-alimento"]').trigger('click')
    await wrapper.findComponent({ name: 'AggiungiAlimentoDialog' }).vm.$emit('aggiunto', {
      alimentoId: 'a1', nome: 'Avena in fiocchi', kcal100g: 372, proteine100g: 12.9, carboidrati100g: 65,
      grassi100g: 6.5, zuccheri100g: 1.1, grammi: 60,
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Avena in fiocchi')
  })
})
```

- [ ] **Step 3: Eseguire il test e verificare che fallisca**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: FAIL (componente non esiste)

- [ ] **Step 4: Implementare `PianoAlimentareFormView.vue`**

```vue
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import SelezionaPazienteCombobox from '@/components/pazienti/SelezionaPazienteCombobox.vue'
import AggiungiAlimentoDialog from '@/components/pianiAlimentari/AggiungiAlimentoDialog.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { type Paziente } from '@/api/pazienti'
import {
  crea, dettaglio, aggiorna, attiva as attivaApi, elimina as eliminaApi,
  type PianoAlimentare, type ModalitaPiano, type GiornoSettimana, type TipoPasto,
} from '@/api/pianiAlimentari'
import { filtraDecimaleItaliano, numeroItalianoOpzionale } from '@/utils/validators'

const route = useRoute()
const router = useRouter()

const pianoIdRoute = route.params.id as string | undefined
const pazienteIdQuery = route.query.pazienteId as string | undefined
const modalitaCreazione = computed(() => !pianoIdRoute)

const caricamento = ref(!modalitaCreazione.value)
const erroreCaricamento = ref<string | null>(null)

// --- Flusso di creazione (nessun piano esiste ancora) ---
const pazienteSelezionato = ref<Paziente | null>(null)
const modalitaScelta = ref<ModalitaPiano>('PASTI')
const nomeCreazione = ref('Nuovo piano')
const creazioneInCorso = ref(false)

async function confermaCreazione() {
  if (!pazienteSelezionato.value || !nomeCreazione.value.trim()) return
  creazioneInCorso.value = true
  try {
    const creato = await crea({
      pazienteId: pazienteSelezionato.value.id,
      nome: nomeCreazione.value.trim(),
      modalita: modalitaScelta.value,
    })
    router.replace(`/piani-alimentari/${creato.id}`)
    applicaPiano(creato)
  } catch {
    toast.error('Non è stato possibile creare il piano. Riprova.')
  } finally {
    creazioneInCorso.value = false
  }
}

// --- Piano caricato/creato: stato locale modificabile ---
const piano = ref<PianoAlimentare | null>(null)
const nome = ref('')
const obiettivoKcalInput = ref('')
const dataFineInput = ref('')

interface RigaLocale {
  clientId: string
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  grammi: number
}

interface PastoLocale {
  clientId: string
  giornoSettimana: GiornoSettimana
  nome: string
  tipo: TipoPasto
  nota: string | null
  righe: RigaLocale[]
}

const pastiLocali = ref<PastoLocale[]>([])

const GIORNI: { chiave: GiornoSettimana; etichetta: string }[] = [
  { chiave: 'LUNEDI', etichetta: 'Lunedì' },
  { chiave: 'MARTEDI', etichetta: 'Martedì' },
  { chiave: 'MERCOLEDI', etichetta: 'Mercoledì' },
  { chiave: 'GIOVEDI', etichetta: 'Giovedì' },
  { chiave: 'VENERDI', etichetta: 'Venerdì' },
  { chiave: 'SABATO', etichetta: 'Sabato' },
  { chiave: 'DOMENICA', etichetta: 'Domenica' },
]
const giornoSelezionato = ref<GiornoSettimana>('LUNEDI')

function applicaPiano(risultato: PianoAlimentare) {
  piano.value = risultato
  nome.value = risultato.nome
  obiettivoKcalInput.value = risultato.obiettivoKcal !== null ? String(risultato.obiettivoKcal) : ''
  dataFineInput.value = risultato.dataFine ?? ''
  pastiLocali.value = risultato.pasti.map((p) => ({
    clientId: p.id,
    giornoSettimana: p.giornoSettimana,
    nome: p.nome,
    tipo: p.tipo,
    nota: p.nota,
    righe: p.righe.map((r) => ({ clientId: r.id, ...r })),
  }))
}

async function caricaPiano(id: string) {
  caricamento.value = true
  try {
    applicaPiano(await dettaglio(id))
  } catch {
    erroreCaricamento.value = 'Non è stato possibile caricare il piano.'
  } finally {
    caricamento.value = false
  }
}

onMounted(() => {
  if (pianoIdRoute) {
    caricaPiano(pianoIdRoute)
  }
})

const pastiDelGiorno = computed(() =>
  pastiLocali.value.filter((p) => p.giornoSettimana === giornoSelezionato.value),
)

function macroRiga(riga: RigaLocale) {
  const rapporto = riga.grammi / 100
  return {
    kcal: riga.kcal100g * rapporto,
    proteine: riga.proteine100g * rapporto,
    carboidrati: riga.carboidrati100g * rapporto,
    grassi: riga.grassi100g * rapporto,
  }
}
function kcalPasto(pasto: PastoLocale) {
  return Math.round(pasto.righe.reduce((somma, r) => somma + macroRiga(r).kcal, 0))
}
function kcalGiorno(giorno: GiornoSettimana) {
  return Math.round(
    pastiLocali.value.filter((p) => p.giornoSettimana === giorno)
      .reduce((somma, p) => somma + kcalPasto(p), 0),
  )
}

function aggiungiPasto() {
  pastiLocali.value.push({
    clientId: crypto.randomUUID(),
    giornoSettimana: giornoSelezionato.value,
    nome: 'Pasto extra',
    tipo: 'ALTRO',
    nota: null,
    righe: [],
  })
}
function rimuoviPasto(clientId: string) {
  pastiLocali.value = pastiLocali.value.filter((p) => p.clientId !== clientId)
}

const addModalAperto = ref(false)
const pastoTargetClientId = ref<string | null>(null)

function apriAggiungiAlimento(pastoClientId: string) {
  pastoTargetClientId.value = pastoClientId
  addModalAperto.value = true
}
function onAlimentoAggiunto(riga: Omit<RigaLocale, 'clientId'>) {
  const pasto = pastiLocali.value.find((p) => p.clientId === pastoTargetClientId.value)
  if (!pasto) return
  pasto.righe.push({ clientId: crypto.randomUUID(), ...riga })
}
function rimuoviRiga(pastoClientId: string, rigaClientId: string) {
  const pasto = pastiLocali.value.find((p) => p.clientId === pastoClientId)
  if (!pasto) return
  pasto.righe = pasto.righe.filter((r) => r.clientId !== rigaClientId)
}
function onGrammiInput(pastoClientId: string, rigaClientId: string, valore: string | number) {
  const pasto = pastiLocali.value.find((p) => p.clientId === pastoClientId)
  const riga = pasto?.righe.find((r) => r.clientId === rigaClientId)
  if (riga) riga.grammi = numeroItalianoOpzionale(filtraDecimaleItaliano(String(valore))) ?? riga.grammi
}

const salvataggioInCorso = ref(false)

async function salva() {
  if (!piano.value) return
  salvataggioInCorso.value = true
  try {
    const aggiornato = await aggiorna(piano.value.id, {
      nome: nome.value.trim(),
      dataFine: dataFineInput.value || null,
      obiettivoKcal: numeroItalianoOpzionale(obiettivoKcalInput.value) ?? null,
      pasti: pastiLocali.value.map((p) => ({
        giornoSettimana: p.giornoSettimana,
        nome: p.nome,
        tipo: p.tipo,
        nota: p.nota,
        righe: p.righe.map(({ clientId: _clientId, ...resto }) => resto),
      })),
      giorniMacroTarget: null,
      esempi: null,
    })
    applicaPiano(aggiornato)
    toast.success('Piano salvato.')
  } catch {
    toast.error('Non è stato possibile salvare il piano.')
  } finally {
    salvataggioInCorso.value = false
  }
}
</script>

<template>
  <AppShell>
    <div v-if="modalitaCreazione && !piano" class="mx-auto flex max-w-lg flex-col gap-4 p-6">
      <h1 class="font-heading text-2xl italic text-(--fg)">Nuovo piano alimentare</h1>

      <div v-if="!pazienteIdQuery">
        <SelezionaPazienteCombobox v-model="pazienteSelezionato" />
      </div>

      <div class="flex gap-2 rounded-xl border border-(--bd2) bg-(--soft) p-1">
        <button
          v-for="opzione in (['PASTI', 'MACRO', 'ESEMPI'] as ModalitaPiano[])"
          :key="opzione"
          class="flex-1 rounded-lg px-3 py-2 text-sm font-bold"
          :class="modalitaScelta === opzione ? 'bg-(--surf) text-(--green)' : 'text-(--fg3)'"
          @click="modalitaScelta = opzione"
        >
          {{ opzione === 'PASTI' ? 'Piano con pasti' : opzione === 'MACRO' ? 'Solo target macro' : 'Esempi intercambiabili' }}
        </button>
      </div>

      <Input v-model="nomeCreazione" placeholder="Nome del piano" />

      <Button :disabled="creazioneInCorso || (!pazienteSelezionato && !pazienteIdQuery)" @click="confermaCreazione">
        Crea piano
      </Button>
    </div>

    <div v-else-if="caricamento" class="p-6 text-sm text-(--fg3)">Caricamento…</div>
    <div v-else-if="erroreCaricamento" class="p-6 text-sm text-(--danger)">{{ erroreCaricamento }}</div>

    <div v-else-if="piano" class="flex flex-col gap-4 p-6">
      <div class="flex items-end justify-between gap-4">
        <div>
          <Input v-model="nome" class="font-heading border-0 p-0 text-2xl italic" />
          <p class="text-sm text-(--fg2)">{{ piano.pazienteNomeCompleto }}</p>
        </div>
        <Button :disabled="salvataggioInCorso" @click="salva">Salva</Button>
      </div>

      <div v-if="piano.modalita === 'PASTI'" class="grid grid-cols-[180px_1fr] gap-4">
        <div class="rounded-2xl border border-(--bd) bg-(--surf)">
          <button
            v-for="giorno in GIORNI" :key="giorno.chiave"
            class="flex w-full items-center justify-between border-b border-(--div2) px-3 py-3 text-left"
            :class="giornoSelezionato === giorno.chiave ? 'bg-(--mint) text-(--green)' : 'text-(--fg2)'"
            @click="giornoSelezionato = giorno.chiave"
          >
            <span class="text-sm font-bold">{{ giorno.etichetta }}</span>
            <span class="text-xs">{{ kcalGiorno(giorno.chiave) || '—' }}</span>
          </button>
        </div>

        <div class="flex flex-col gap-3">
          <div
            v-for="pasto in pastiDelGiorno" :key="pasto.clientId"
            data-test="card-pasto"
            class="rounded-2xl border border-(--bd) bg-(--surf)"
          >
            <div class="flex items-center justify-between gap-3 bg-(--soft) px-4 py-3">
              <Input v-model="pasto.nome" class="border-0 bg-transparent p-0 font-heading text-sm font-semibold" />
              <div class="flex items-center gap-3">
                <span class="text-xs text-(--fg3)">{{ kcalPasto(pasto) }} kcal</span>
                <button class="text-(--fg4)" @click="rimuoviPasto(pasto.clientId)">Elimina</button>
              </div>
            </div>

            <table v-if="pasto.righe.length" class="w-full">
              <tbody>
                <tr v-for="riga in pasto.righe" :key="riga.clientId" class="border-t border-(--div2)">
                  <td class="p-2 text-sm font-semibold">{{ riga.nome }}</td>
                  <td class="p-2 text-right">
                    <Input
                      type="text" inputmode="decimal" class="w-16 text-right"
                      :model-value="riga.grammi"
                      @update:model-value="(v) => onGrammiInput(pasto.clientId, riga.clientId, v)"
                    />
                  </td>
                  <td class="p-2 text-right text-sm font-bold">{{ Math.round(macroRiga(riga).kcal) }}</td>
                  <td class="p-2 text-center">
                    <button class="text-(--fg4)" @click="rimuoviRiga(pasto.clientId, riga.clientId)">✕</button>
                  </td>
                </tr>
              </tbody>
            </table>

            <div class="p-3">
              <button
                data-test="apri-aggiungi-alimento"
                class="rounded-lg border border-dashed border-(--bd2) px-3 py-2 text-xs font-bold text-(--fg3)"
                @click="apriAggiungiAlimento(pasto.clientId)"
              >
                + Aggiungi alimento
              </button>
            </div>
          </div>

          <button
            data-test="aggiungi-pasto"
            class="rounded-xl border border-dashed border-(--bd2) bg-(--surf) px-4 py-3 text-sm font-bold text-(--fg2)"
            @click="aggiungiPasto"
          >
            + Aggiungi pasto
          </button>
        </div>
      </div>
    </div>

    <AggiungiAlimentoDialog v-model:open="addModalAperto" @aggiunto="onAlimentoAggiunto" />
  </AppShell>
</template>
```

**Nota**: il rendering per `piano.modalita === 'MACRO'`/`'ESEMPI'` arriva nei Task 10-11 (per ora, con un piano non-PASTI, l'editor mostra solo l'intestazione — accettabile perché questo task consegna esplicitamente solo la modalità PASTI).

- [ ] **Step 5: Eseguire il test e verificare che passi**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: PASS. Se `SelezionaPazienteCombobox`/`AppShell`/`Input` richiedono stub o provider aggiuntivi nei test (es. plugin Pinia per lo store `auth` usato da `AppShell`), replicare l'impostazione già usata in `VisitaFormView.spec.ts` (stesso genere di dipendenze).

- [ ] **Step 6: `tsc --noEmit`**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: pulito.

- [ ] **Step 7: Staging**

```bash
git add frontend-professionisti/src/views/pianiAlimentari/ frontend-professionisti/src/router/index.ts
```

---

### Task 10: Modalità "Solo target macro"

**Files:**
- Modify: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.vue`
- Modify: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts`

**Interfaces:**
- Consumes: `GiornoMacroTarget`/`GiornoMacroTargetRequest` da `api/pianiAlimentari.ts` (Task 7), `GIORNI`/`giornoSelezionato` già definiti in Task 9.
- Produces: `giorniMacroLocali` (stato locale analogo a `pastiLocali`), incluso nel payload di `salva()` quando `piano.modalita === 'MACRO'`.

- [ ] **Step 1: Estendere il test**

```typescript
const pianoMacro = {
  ...pianoVuoto,
  id: 'piano2', modalita: 'MACRO', pasti: [],
  giorniMacroTarget: [{ giornoSettimana: 'LUNEDI', kcalTarget: 2400, proteineTarget: 150, carboidratiTarget: 300, grassiTarget: 67 }],
}

it('mostra e permette di modificare i target macro del giorno selezionato', async () => {
  vi.mocked(api.dettaglio).mockResolvedValue(pianoMacro as never)
  const router = await creaRouter('/piani-alimentari/piano2')
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
  await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
  await wrapper.vm.$nextTick()

  const campoKcal = wrapper.find('[data-test="macro-target-kcal"]')
  expect((campoKcal.element as HTMLInputElement).value).toBe('2400')
})

it('applica a tutti i giorni copia i target del giorno corrente sugli altri 6', async () => {
  vi.mocked(api.dettaglio).mockResolvedValue(pianoMacro as never)
  const router = await creaRouter('/piani-alimentari/piano2')
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
  await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
  await wrapper.vm.$nextTick()

  await wrapper.find('[data-test="applica-a-tutti"]').trigger('click')
  await wrapper.findAll('[data-test="giorno-tab"]')[1].trigger('click') // Martedì
  await wrapper.vm.$nextTick()

  const campoKcal = wrapper.find('[data-test="macro-target-kcal"]')
  expect((campoKcal.element as HTMLInputElement).value).toBe('2400')
})
```

(Il test `'aggiungere un pasto lo mostra nel giorno selezionato'` esistente resta invariato; aggiungere anche `data-test="giorno-tab"` ai bottoni giorno già scritti in Task 9, e `data-test="macro-target-kcal"` al nuovo campo qui sotto.)

- [ ] **Step 2: Eseguire i nuovi test e verificare che falliscano**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: FAIL (non c'è ancora rendering per MACRO)

- [ ] **Step 3: Aggiungere lo stato e il rendering MACRO**

Nello `<script setup>`, aggiungere accanto a `pastiLocali`:

```typescript
interface GiornoMacroLocale {
  giornoSettimana: GiornoSettimana
  kcalTarget: string
  proteineTarget: string
  carboidratiTarget: string
  grassiTarget: string
}
const giorniMacroLocali = ref<GiornoMacroLocale[]>([])

function applicaGiorniMacroDaRisultato(risultato: PianoAlimentare) {
  giorniMacroLocali.value = GIORNI.map((g) => {
    const esistente = risultato.giorniMacroTarget.find((t) => t.giornoSettimana === g.chiave)
    return {
      giornoSettimana: g.chiave,
      kcalTarget: esistente?.kcalTarget != null ? String(esistente.kcalTarget) : '',
      proteineTarget: esistente?.proteineTarget != null ? String(esistente.proteineTarget) : '',
      carboidratiTarget: esistente?.carboidratiTarget != null ? String(esistente.carboidratiTarget) : '',
      grassiTarget: esistente?.grassiTarget != null ? String(esistente.grassiTarget) : '',
    }
  })
}

const macroDelGiornoSelezionato = computed(() =>
  giorniMacroLocali.value.find((g) => g.giornoSettimana === giornoSelezionato.value),
)

function applicaMacroATuttiIGiorni() {
  const corrente = macroDelGiornoSelezionato.value
  if (!corrente) return
  giorniMacroLocali.value = giorniMacroLocali.value.map((g) => ({ ...corrente, giornoSettimana: g.giornoSettimana }))
}
```

Chiamare `applicaGiorniMacroDaRisultato(risultato)` dentro `applicaPiano(risultato)` (aggiungere questa riga alla funzione esistente di Task 9, subito dopo `pastiLocali.value = ...`).

Aggiungere `data-test="giorno-tab"` al bottone giorno della sidebar (già scritto in Task 9): `<button data-test="giorno-tab" v-for="giorno in GIORNI" ...>`.

Nel template, subito prima del blocco `<div v-if="piano.modalita === 'PASTI'" ...>` scritto in Task 9, aggiungere:

```vue
      <div v-if="piano.modalita === 'MACRO'" class="grid grid-cols-[180px_1fr] gap-4">
        <div class="rounded-2xl border border-(--bd) bg-(--surf)">
          <button
            v-for="giorno in GIORNI" :key="giorno.chiave" data-test="giorno-tab"
            class="flex w-full items-center justify-between border-b border-(--div2) px-3 py-3 text-left"
            :class="giornoSelezionato === giorno.chiave ? 'bg-(--mint) text-(--green)' : 'text-(--fg2)'"
            @click="giornoSelezionato = giorno.chiave"
          >
            <span class="text-sm font-bold">{{ giorno.etichetta }}</span>
          </button>
        </div>

        <div v-if="macroDelGiornoSelezionato" class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
          <div class="mb-3 flex items-center justify-between">
            <p class="font-heading text-base font-semibold">Target macro · {{ GIORNI.find(g => g.chiave === giornoSelezionato)?.etichetta }}</p>
            <button data-test="applica-a-tutti" class="text-xs font-bold text-(--green)" @click="applicaMacroATuttiIGiorni">
              Applica a tutti i giorni
            </button>
          </div>
          <div class="grid grid-cols-4 gap-3">
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Kcal</span>
              <Input
                data-test="macro-target-kcal" type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.kcalTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.kcalTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Proteine (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.proteineTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.proteineTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Carboidrati (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.carboidratiTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.carboidratiTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Grassi (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.grassiTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.grassiTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
          </div>
        </div>
      </div>
```

- [ ] **Step 4: Includere i target macro nel payload di `salva()`**

In `salva()` (Task 9), sostituire `giorniMacroTarget: null,` con:

```typescript
      giorniMacroTarget: piano.value.modalita === 'MACRO'
        ? giorniMacroLocali.value.map((g) => ({
            giornoSettimana: g.giornoSettimana,
            kcalTarget: numeroItalianoOpzionale(g.kcalTarget) ?? null,
            proteineTarget: numeroItalianoOpzionale(g.proteineTarget) ?? null,
            carboidratiTarget: numeroItalianoOpzionale(g.carboidratiTarget) ?? null,
            grassiTarget: numeroItalianoOpzionale(g.grassiTarget) ?? null,
          }))
        : null,
```

(e analogamente `pasti: piano.value.modalita === 'PASTI' ? ... : null,` — la mappatura di Task 9 va condizionata alla modalità, dato che ora il payload può riguardare due modalità diverse.)

- [ ] **Step 5: Eseguire i test e verificare che passino**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: PASS

- [ ] **Step 6: `tsc --noEmit`**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: pulito.

- [ ] **Step 7: Staging**

```bash
git add frontend-professionisti/src/views/pianiAlimentari/
```

---

### Task 11: Modalità "Esempi intercambiabili"

**Files:**
- Modify: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.vue`
- Modify: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts`

**Interfaces:**
- Consumes: `Esempio`/`EsempioRequest` da `api/pianiAlimentari.ts` (Task 7), `RigaLocale`/`onAlimentoAggiunto`-style logic di Task 9 (qui duplicata per gli esempi, dato che il "genitore" della riga è un esempio non un pasto).
- Produces: `esempiLocali`, incluso nel payload di `salva()` quando `piano.modalita === 'ESEMPI'`.

- [ ] **Step 1: Estendere il test**

```typescript
const pianoEsempi = {
  ...pianoVuoto,
  id: 'piano3', modalita: 'ESEMPI', pasti: [], giorniMacroTarget: [],
  esempi: [{ id: 'es1', tipoPasto: 'COLAZIONE', nome: 'Colazione 1', ordine: 0, righe: [] }],
}

it('mostra le categorie di esempi e permette di aggiungerne uno nuovo', async () => {
  vi.mocked(api.dettaglio).mockResolvedValue(pianoEsempi as never)
  const router = await creaRouter('/piani-alimentari/piano3')
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
  await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
  await wrapper.vm.$nextTick()

  expect(wrapper.text()).toContain('Colazione 1')

  await wrapper.find('[data-test="nuovo-esempio-COLAZIONE"]').trigger('click')

  expect(wrapper.findAll('[data-test="card-esempio"]').length).toBeGreaterThanOrEqual(2)
})
```

- [ ] **Step 2: Eseguire il nuovo test e verificare che fallisca**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: FAIL

- [ ] **Step 3: Aggiungere stato e rendering ESEMPI**

Nello `<script setup>`:

```typescript
interface EsempioLocale {
  clientId: string
  tipoPasto: TipoPasto
  nome: string
  righe: RigaLocale[]
}
const esempiLocali = ref<EsempioLocale[]>([])

const CATEGORIE_ESEMPI: { tipo: TipoPasto; etichetta: string }[] = [
  { tipo: 'COLAZIONE', etichetta: 'Colazione' },
  { tipo: 'SPUNTINO_MATTINA', etichetta: 'Spuntino mattina' },
  { tipo: 'PRANZO', etichetta: 'Pranzo' },
  { tipo: 'SPUNTINO_POMERIGGIO', etichetta: 'Spuntino pomeriggio' },
  { tipo: 'CENA', etichetta: 'Cena' },
]

function esempiPerCategoria(tipo: TipoPasto) {
  return esempiLocali.value.filter((e) => e.tipoPasto === tipo)
}

function aggiungiEsempio(tipo: TipoPasto, etichetta: string) {
  const numero = esempiPerCategoria(tipo).length + 1
  esempiLocali.value.push({ clientId: crypto.randomUUID(), tipoPasto: tipo, nome: `${etichetta} ${numero}`, righe: [] })
}
function rimuoviEsempio(clientId: string) {
  esempiLocali.value = esempiLocali.value.filter((e) => e.clientId !== clientId)
}

// L'"apertura" del modal aggiungi-alimento ora deve sapere se il bersaglio è un pasto o un esempio.
const esempioTargetClientId = ref<string | null>(null)
function apriAggiungiAlimentoEsempio(esempioClientId: string) {
  pastoTargetClientId.value = null
  esempioTargetClientId.value = esempioClientId
  addModalAperto.value = true
}
function rimuoviRigaEsempio(esempioClientId: string, rigaClientId: string) {
  const esempio = esempiLocali.value.find((e) => e.clientId === esempioClientId)
  if (!esempio) return
  esempio.righe = esempio.righe.filter((r) => r.clientId !== rigaClientId)
}
```

Modificare `apriAggiungiAlimento` (Task 9) per azzerare `esempioTargetClientId.value = null` all'apertura per un pasto (simmetrico a quanto fa `apriAggiungiAlimentoEsempio`), e modificare `onAlimentoAggiunto` (Task 9) così:

```typescript
function onAlimentoAggiunto(riga: Omit<RigaLocale, 'clientId'>) {
  if (esempioTargetClientId.value) {
    const esempio = esempiLocali.value.find((e) => e.clientId === esempioTargetClientId.value)
    esempio?.righe.push({ clientId: crypto.randomUUID(), ...riga })
    return
  }
  const pasto = pastiLocali.value.find((p) => p.clientId === pastoTargetClientId.value)
  pasto?.righe.push({ clientId: crypto.randomUUID(), ...riga })
}
```

Aggiungere `applicaEsempiDaRisultato(risultato)` chiamato da `applicaPiano(risultato)` (Task 9), analogo a `applicaGiorniMacroDaRisultato`:

```typescript
function applicaEsempiDaRisultato(risultato: PianoAlimentare) {
  esempiLocali.value = risultato.esempi.map((e) => ({
    clientId: e.id,
    tipoPasto: e.tipoPasto,
    nome: e.nome,
    righe: e.righe.map((r) => ({ clientId: r.id, ...r })),
  }))
}
```

Nel template, prima del blocco `MACRO` (Task 10):

```vue
      <div v-if="piano.modalita === 'ESEMPI'" class="flex flex-col gap-4">
        <div class="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-4">
          <div v-for="categoria in CATEGORIE_ESEMPI" :key="categoria.tipo" class="rounded-2xl border border-(--bd) bg-(--surf)">
            <div class="bg-(--soft) px-3 py-2">
              <p class="font-heading text-sm font-semibold">{{ categoria.etichetta }}</p>
            </div>
            <div class="flex flex-col gap-2 p-2">
              <div
                v-for="esempio in esempiPerCategoria(categoria.tipo)" :key="esempio.clientId"
                data-test="card-esempio"
                class="rounded-xl border border-(--bd2) p-2"
              >
                <div class="mb-1 flex items-center justify-between">
                  <Input v-model="esempio.nome" class="border-0 bg-transparent p-0 text-xs font-bold" />
                  <button class="text-(--fg4)" @click="rimuoviEsempio(esempio.clientId)">✕</button>
                </div>
                <div v-for="riga in esempio.righe" :key="riga.clientId" class="flex items-center justify-between gap-1 text-xs">
                  <span class="min-w-0 flex-1 truncate">{{ riga.nome }}</span>
                  <span>{{ riga.grammi }}g</span>
                  <button class="text-(--fg4)" @click="rimuoviRigaEsempio(esempio.clientId, riga.clientId)">✕</button>
                </div>
                <button class="mt-1 text-xs font-bold text-(--fg3)" @click="apriAggiungiAlimentoEsempio(esempio.clientId)">
                  + Aggiungi alimento
                </button>
              </div>
              <button
                :data-test="`nuovo-esempio-${categoria.tipo}`"
                class="rounded-lg border border-dashed border-(--bd2) py-2 text-xs font-bold text-(--fg3)"
                @click="aggiungiEsempio(categoria.tipo, categoria.etichetta)"
              >
                + Nuovo esempio
              </button>
            </div>
          </div>
        </div>
      </div>
```

- [ ] **Step 4: Includere gli esempi nel payload di `salva()`**

Sostituire `esempi: null,` (Task 9/10) con:

```typescript
      esempi: piano.value.modalita === 'ESEMPI'
        ? esempiLocali.value.map((e) => ({
            tipoPasto: e.tipoPasto,
            nome: e.nome,
            righe: e.righe.map(({ clientId: _clientId, ...resto }) => resto),
          }))
        : null,
```

Chiamare anche `applicaEsempiDaRisultato(aggiornato)` (o più semplicemente lasciare che `applicaPiano(aggiornato)`, già chiamato in `salva()`, lo faccia dato che ora invoca anche questa funzione).

- [ ] **Step 5: Eseguire i test e verificare che passino**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: PASS

- [ ] **Step 6: `tsc --noEmit`**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: pulito.

- [ ] **Step 7: Staging**

```bash
git add frontend-professionisti/src/views/pianiAlimentari/
```

---

### Task 12: Obiettivo/data fine, Attiva/Elimina/Stampa PDF

**Files:**
- Modify: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.vue`
- Modify: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts`

**Interfaces:**
- Consumes: `attiva`/`elimina` da `api/pianiAlimentari.ts` (Task 7), `AlertDialog*` di shadcn-vue (esistente, già usato da `PazientiListView.vue`).
- Produces: azioni complete dell'editor — nessuna interfaccia consumata da altri task.

- [ ] **Step 1: Estendere il test**

```typescript
it('mostra il suggerimento TDEE e un avviso se sotto la soglia di sicurezza', async () => {
  vi.mocked(api.dettaglio).mockResolvedValue({
    ...pianoVuoto, obiettivoKcalSuggerito: 1100, tdeeCalcolato: 1100, bmrCalcolato: 900,
    formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: true,
  } as never)
  const router = await creaRouter('/piani-alimentari/piano1')
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
  await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
  await wrapper.vm.$nextTick()

  expect(wrapper.text()).toContain('1100')
  expect(wrapper.text()).toContain('sotto la soglia')
})

it('attiva salva e poi chiama attiva, senza mostrare più il bottone su un piano già attivo', async () => {
  vi.mocked(api.attiva).mockResolvedValue(undefined as never)
  const pianoAttivato = { ...pianoVuoto, stato: 'ATTIVO' }
  vi.mocked(api.aggiorna).mockResolvedValue(pianoVuoto as never)
  vi.mocked(api.dettaglio)
    .mockResolvedValueOnce(pianoVuoto as never)
    .mockResolvedValueOnce(pianoAttivato as never)
  const router = await creaRouter('/piani-alimentari/piano1')
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
  await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
  await wrapper.vm.$nextTick()

  await wrapper.find('[data-test="attiva-piano"]').trigger('click')
  await vi.waitFor(() => expect(api.attiva).toHaveBeenCalledWith('piano1'))

  expect(api.aggiorna).toHaveBeenCalled()
})

it('elimina richiede conferma e poi chiama elimina', async () => {
  vi.mocked(api.elimina).mockResolvedValue(undefined as never)
  const router = await creaRouter('/piani-alimentari/piano1')
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
  await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
  await wrapper.vm.$nextTick()

  await wrapper.find('[data-test="elimina-piano"]').trigger('click')
  await wrapper.find('[data-test="conferma-elimina-piano"]').trigger('click')

  await vi.waitFor(() => expect(api.elimina).toHaveBeenCalledWith('piano1'))
})
```

- [ ] **Step 2: Eseguire i nuovi test e verificare che falliscano**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: FAIL

- [ ] **Step 3: Implementare le azioni**

Aggiungere all'`<script setup>` (import aggiuntivi: `AlertDialog`, `AlertDialogContent`, `AlertDialogHeader`, `AlertDialogTitle`, `AlertDialogDescription`, `AlertDialogFooter`, `AlertDialogCancel`, `AlertDialogAction` da `@/components/ui/alert-dialog`):

```typescript
const attivazioneInCorso = ref(false)
async function attivaPiano() {
  if (!piano.value) return
  attivazioneInCorso.value = true
  try {
    await salva()
    await attivaApi(piano.value.id)
    applicaPiano(await dettaglio(piano.value.id))
    toast.success('Piano attivato.')
  } catch {
    toast.error('Non è stato possibile attivare il piano.')
  } finally {
    attivazioneInCorso.value = false
  }
}

const confermaEliminaAperta = ref(false)
const eliminazioneInCorso = ref(false)
async function confermaElimina() {
  if (!piano.value) return
  eliminazioneInCorso.value = true
  try {
    await eliminaApi(piano.value.id)
    toast.success('Piano eliminato.')
    router.push('/piani-alimentari')
  } catch {
    toast.error('Non è stato possibile eliminare il piano.')
  } finally {
    eliminazioneInCorso.value = false
    confermaEliminaAperta.value = false
  }
}

function stampaPdf() {
  window.print()
}
```

Nel template, sostituire il blocco intestazione scritto in Task 9 (`<div class="flex items-end justify-between gap-4">...</div>`) con:

```vue
      <div class="flex items-end justify-between gap-4 print:hidden">
        <div>
          <Input v-model="nome" class="font-heading border-0 p-0 text-2xl italic" />
          <p class="text-sm text-(--fg2)">{{ piano.pazienteNomeCompleto }}</p>
        </div>
        <div class="flex gap-2">
          <Button variant="outline" @click="stampaPdf">Stampa PDF</Button>
          <Button v-if="piano.stato === 'BOZZA'" data-test="elimina-piano" variant="outline"
                  @click="confermaEliminaAperta = true">Elimina</Button>
          <Button v-if="piano.stato === 'BOZZA'" data-test="attiva-piano" :disabled="attivazioneInCorso"
                  @click="attivaPiano">Attiva piano</Button>
          <Button :disabled="salvataggioInCorso" @click="salva">Salva</Button>
        </div>
      </div>

      <div class="flex items-center justify-center gap-6 rounded-2xl border border-(--sage-l) bg-(--mint) p-4 print:hidden">
        <div>
          <p class="text-xs font-bold uppercase text-(--green)">Obiettivo giornaliero</p>
          <Input
            type="text" inputmode="decimal" class="w-24 font-heading text-base font-semibold"
            :model-value="obiettivoKcalInput"
            @update:model-value="(v) => (obiettivoKcalInput = filtraDecimaleItaliano(String(v)))"
          />
          <p v-if="piano.obiettivoKcalSuggerito !== null" class="text-xs text-(--green)">
            Suggerito: {{ piano.obiettivoKcalSuggerito }} kcal
            ({{ piano.formulaBmrUsata === 'KATCH_MCARDLE' ? 'Katch-McArdle' : 'Mifflin-St Jeor' }})
          </p>
          <p v-else class="text-xs text-(--fg3)">
            Nessun suggerimento automatico disponibile per l'obiettivo di questa visita: imposta il valore a mano.
          </p>
          <p v-if="piano.sottoSogliaSicurezza" class="text-xs text-(--danger)">
            Il valore suggerito è sotto la soglia minima indicativa: valutare con attenzione.
          </p>
        </div>
        <div>
          <p class="text-xs font-bold uppercase text-(--green)">Fine piano</p>
          <input type="date" v-model="dataFineInput" class="rounded-md border border-(--sage) bg-(--surf) px-2 py-1 text-sm" />
        </div>
      </div>
```

(spostare il blocco "Salva" preesistente dentro il nuovo gruppo di bottoni sopra, non duplicarlo.)

Aggiungere in coda al template, prima della chiusura di `</AppShell>`:

```vue
    <AlertDialog v-model:open="confermaEliminaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Eliminare questo piano?</AlertDialogTitle>
          <AlertDialogDescription>L'operazione non è reversibile.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Annulla</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-elimina-piano" variant="destructive" @click="confermaElimina">
            Elimina
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
```

Aggiungere, in coda al file (dopo `</template>`), uno stile di stampa che nasconde la shell applicativa (sidebar/header di `AppShell`) e i controlli marcati `print:hidden`, lasciando solo il contenuto del piano:

```vue
<style scoped>
@media print {
  :deep(aside),
  :deep(header) {
    display: none !important;
  }
}
</style>
```

- [ ] **Step 4: Eseguire i test e verificare che passino**

Run: `npx vitest run src/views/pianiAlimentari/PianoAlimentareFormView.spec.ts` da `frontend-professionisti/`
Expected: PASS

- [ ] **Step 5: `tsc --noEmit` e build**

Run: `npx tsc --noEmit` poi `npm run build` da `frontend-professionisti/`
Expected: entrambi puliti.

- [ ] **Step 6: Staging**

```bash
git add frontend-professionisti/src/views/pianiAlimentari/
```

---

### Task 13: `PianiAlimentariListView.vue`

**Files:**
- Create: `frontend-professionisti/src/views/pianiAlimentari/PianiAlimentariListView.vue`
- Create: `frontend-professionisti/src/views/pianiAlimentari/PianiAlimentariListView.spec.ts`
- Modify: `frontend-professionisti/src/router/index.ts` (rotta `/piani-alimentari`)

**Interfaces:**
- Consumes: `cerca` da `api/pianiAlimentari.ts` (Task 7).
- Produces: rotta `/piani-alimentari` — collegata dal Task 14 (nav) e dal link "← Piani alimentari"/"Crea nuovo piano" verso `/piani-alimentari/nuovo` (Task 9).

- [ ] **Step 1: Aggiungere la rotta**

```typescript
    { path: '/piani-alimentari', name: 'piani-alimentari', component: () => import('@/views/pianiAlimentari/PianiAlimentariListView.vue'), meta: { requiresAuth: true } },
```

- [ ] **Step 2: Scrivere il test**

```typescript
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import PianiAlimentariListView from './PianiAlimentariListView.vue'
import * as api from '@/api/pianiAlimentari'

vi.mock('@/api/pianiAlimentari')

async function creaRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/piani-alimentari', name: 'piani-alimentari', component: PianiAlimentariListView },
      { path: '/piani-alimentari/nuovo', name: 'piano-alimentare-nuovo', component: { template: '<div/>' } },
    ],
  })
  router.push('/piani-alimentari')
  await router.isReady()
  return router
}

describe('PianiAlimentariListView', () => {
  beforeEach(() => {
    vi.mocked(api.cerca).mockResolvedValue({
      contenuto: [
        { id: '1', pazienteNomeCompleto: 'Mario Bianchi', nome: 'Ipertrofia · fase 2', stato: 'ATTIVO', obiettivoKcal: 2800, dataFine: '2026-09-18' },
      ],
      paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1,
    })
  })

  it('carica e mostra i piani alla creazione del componente', async () => {
    const router = await creaRouter()
    const wrapper = mount(PianiAlimentariListView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.cerca).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Mario Bianchi')
    expect(wrapper.text()).toContain('Ipertrofia · fase 2')
  })

  it('il chip di stato filtra la ricerca e riporta la pagina a 0', async () => {
    const router = await creaRouter()
    const wrapper = mount(PianiAlimentariListView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.cerca).toHaveBeenCalled())

    await wrapper.find('[data-test="chip-stato-ATTIVO"]').trigger('click')

    await vi.waitFor(() =>
      expect(api.cerca).toHaveBeenCalledWith(expect.objectContaining({ stato: 'ATTIVO', pagina: 0 })),
    )
  })

  it('mostra lo stato vuoto quando non ci sono risultati', async () => {
    vi.mocked(api.cerca).mockResolvedValue({ contenuto: [], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 0, totalePagine: 0 })
    const router = await creaRouter()
    const wrapper = mount(PianiAlimentariListView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.cerca).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Nessun piano trovato')
  })
})
```

- [ ] **Step 3: Eseguire il test e verificare che fallisca**

Run: `npx vitest run src/views/pianiAlimentari/PianiAlimentariListView.spec.ts` da `frontend-professionisti/`
Expected: FAIL (componente non esiste)

- [ ] **Step 4: Implementare la view**

Stesso pattern di `AlimentiListView.vue` (ricerca con debounce 300ms, chip di stato, paginazione, stati vuoto/caricamento/errore), semplificato (nessun dialog: ogni riga naviga alla rotta dell'editor):

```vue
<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { cerca, type PaginaPianiAlimentari, type StatoPiano } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Plus } from '@lucide/vue'

const router = useRouter()

const CHIP_STATI: { valore: StatoPiano | 'TUTTI'; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'BOZZA', etichetta: 'Bozza' },
  { valore: 'ATTIVO', etichetta: 'Attivo' },
  { valore: 'SCADUTO', etichetta: 'Scaduto' },
  { valore: 'TERMINATO', etichetta: 'Terminato' },
]

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const statoFiltro = ref<StatoPiano | 'TUTTI'>('TUTTI')
const pagina = ref(0)

const paginaDati = ref<PaginaPianiAlimentari | null>(null)
const caricamentoIniziale = ref(true)
const errore = ref(false)

let debounceHandle: ReturnType<typeof setTimeout> | undefined
watch(ricercaInput, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => {
    ricercaEffettiva.value = valore
    pagina.value = 0
  }, 300)
})
onUnmounted(() => clearTimeout(debounceHandle))

function selezionaStato(valore: StatoPiano | 'TUTTI') {
  statoFiltro.value = valore
  pagina.value = 0
}

async function carica() {
  caricamentoIniziale.value = paginaDati.value === null
  errore.value = false
  try {
    paginaDati.value = await cerca({
      pagina: pagina.value,
      dimensione: 20,
      ricerca: ricercaEffettiva.value.trim() || undefined,
      stato: statoFiltro.value === 'TUTTI' ? undefined : statoFiltro.value,
    })
  } catch {
    errore.value = true
  } finally {
    caricamentoIniziale.value = false
  }
}
watch([ricercaEffettiva, statoFiltro, pagina], carica)
onMounted(carica)

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}
function paginaSuccessiva() {
  if (paginaDati.value && pagina.value < paginaDati.value.totalePagine - 1) pagina.value += 1
}
function apriPiano(id: string) {
  router.push(`/piani-alimentari/${id}`)
}

const conteggioTesto = computed(() => {
  if (!paginaDati.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = paginaDati.value
  if (totaleElementi === 0) return ''
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} piani`
})
</script>

<template>
  <AppShell>
    <div class="flex flex-col gap-4 p-6">
      <div class="flex items-end justify-between gap-4">
        <h1 class="font-heading text-2xl italic text-(--fg)">Piani alimentari</h1>
        <Button @click="router.push('/piani-alimentari/nuovo')">
          <Plus :size="14" /> Crea nuovo piano
        </Button>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <Input v-model="ricercaInput" placeholder="Cerca paziente o piano…" class="min-w-60" />
        <button
          v-for="chip in CHIP_STATI" :key="chip.valore" :data-test="`chip-stato-${chip.valore}`"
          class="rounded-full border px-3 py-1 text-xs font-bold"
          :class="statoFiltro === chip.valore ? 'border-(--green) bg-(--green) text-(--on-green)' : 'border-(--bd2) bg-(--surf) text-(--fg2)'"
          @click="selezionaStato(chip.valore)"
        >
          {{ chip.etichetta }}
        </button>
        <span class="ml-auto text-xs text-(--fg3)">{{ conteggioTesto }}</span>
      </div>

      <div v-if="caricamentoIniziale" class="text-sm text-(--fg3)">Caricamento…</div>
      <div v-else-if="errore" class="text-sm text-(--danger)">Non è stato possibile caricare i piani.</div>
      <div v-else class="rounded-2xl border border-(--bd) bg-(--surf)">
        <Table v-if="paginaDati && paginaDati.contenuto.length > 0">
          <TableHeader>
            <TableRow>
              <TableHead>Paziente</TableHead>
              <TableHead>Piano</TableHead>
              <TableHead>Stato</TableHead>
              <TableHead class="text-right">Obiettivo</TableHead>
              <TableHead>Scadenza</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow
              v-for="riga in paginaDati.contenuto" :key="riga.id"
              class="cursor-pointer" @click="apriPiano(riga.id)"
            >
              <TableCell class="font-heading font-semibold">{{ riga.pazienteNomeCompleto }}</TableCell>
              <TableCell>{{ riga.nome }}</TableCell>
              <TableCell>{{ riga.stato }}</TableCell>
              <TableCell class="text-right">{{ riga.obiettivoKcal ?? '—' }} kcal</TableCell>
              <TableCell>{{ riga.dataFine ?? '—' }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <div v-else class="flex flex-col items-center gap-2 p-10 text-center">
          <p class="text-sm font-bold">Nessun piano trovato</p>
          <p class="text-xs text-(--fg3)">Prova a modificare la ricerca o i filtri.</p>
        </div>
      </div>

      <div v-if="paginaDati && paginaDati.totalePagine > 1" class="flex items-center justify-end gap-2">
        <Button variant="outline" size="sm" :disabled="pagina === 0" @click="paginaPrecedente">Precedente</Button>
        <Button variant="outline" size="sm" :disabled="pagina >= paginaDati.totalePagine - 1" @click="paginaSuccessiva">Successivo</Button>
      </div>
    </div>
  </AppShell>
</template>
```

- [ ] **Step 5: Eseguire il test e verificare che passi**

Run: `npx vitest run src/views/pianiAlimentari/PianiAlimentariListView.spec.ts` da `frontend-professionisti/`
Expected: PASS

- [ ] **Step 6: `tsc --noEmit`**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: pulito.

- [ ] **Step 7: Staging**

```bash
git add frontend-professionisti/src/views/pianiAlimentari/ frontend-professionisti/src/router/index.ts
```

---

### Task 14: Abilitare i punti di ingresso già predisposti (sidebar, dashboard, command palette, scheda paziente)

**Files:**
- Modify: `frontend-professionisti/src/components/AppSidebar.vue`
- Modify: `frontend-professionisti/src/views/DashboardView.vue` (+ `.spec.ts`)
- Modify: `frontend-professionisti/src/components/CommandPalette.vue` (+ `.spec.ts`)
- Modify: `frontend-professionisti/src/views/pazienti/PazienteDettaglioView.vue` (+ `.spec.ts`)

**Interfaces:**
- Consumes: rotte `piani-alimentari`/`piano-alimentare-nuovo` (Task 9 e 13).
- Produces: nessuna — task terminale, chiude i 4 punti di ingresso già disegnati ma disabilitati in sessioni precedenti (vedi `wiki/stato.md`, sessione dell'8 settembre 2026 e precedenti).

- [ ] **Step 1: Sidebar** — in `AppSidebar.vue:18`, sostituire

```typescript
  { nome: 'Piani alimentari', icona: Utensils },
```

con

```typescript
  { nome: 'Piani alimentari', routeName: 'piani-alimentari', routeNamesAttivi: ['piani-alimentari', 'piano-alimentare-nuovo', 'piano-alimentare-modifica'], icona: Utensils },
```

(diventa un `RouterLink` automaticamente, per via del rendering condizionale già esistente su `voce.routeName` — vedi `AppSidebar.vue:40-53`.)

- [ ] **Step 2: Dashboard** — in `DashboardView.vue:67-70`, sostituire

```vue
          <DropdownMenuItem disabled class="gap-2 px-2.5 py-1.5 cursor-pointer">
            <FileText :size="16" />
            Nuovo piano alimentare
          </DropdownMenuItem>
```

con

```vue
          <DropdownMenuItem as-child class="gap-2 px-2.5 py-1.5 cursor-pointer">
            <router-link to="/piani-alimentari/nuovo">
              <FileText :size="16" />
              Nuovo piano alimentare
            </router-link>
          </DropdownMenuItem>
```

Aggiornare il test esistente che verifica questa voce come disabilitata (cercare `disabled` in `DashboardView.spec.ts` relativo a "Nuovo piano alimentare") per aspettarsi invece un link funzionante verso `/piani-alimentari/nuovo`.

- [ ] **Step 3: Command palette** — in `CommandPalette.vue:20`, sostituire

```typescript
  { id: 'nuovo-piano-alimentare', etichetta: 'Nuovo piano alimentare', icona: FileText, disabilitata: true },
```

con

```typescript
  { id: 'nuovo-piano-alimentare', etichetta: 'Nuovo piano alimentare', icona: FileText, href: '/piani-alimentari/nuovo' },
```

Aggiornare l'eventuale test che verifica questa voce come disabilitata in `CommandPalette.spec.ts`.

- [ ] **Step 4: Scheda paziente** — in `PazienteDettaglioView.vue:256-259`, sostituire

```vue
          <Button variant="outline" disabled>
            <Utensils :size="15" />
            <span>Nuovo piano</span>
          </Button>
```

con

```vue
          <Button variant="outline" as-child>
            <router-link :to="`/piani-alimentari/nuovo?pazienteId=${paziente.id}`">
              <Utensils :size="15" />
              <span>Nuovo piano</span>
            </router-link>
          </Button>
```

Verificare che `Utensils` sia già importato in questo file (dovrebbe esserlo, dato che l'icona era già usata sul bottone disabilitato) e aggiornare l'eventuale test che verifica questo bottone come disabilitato.

- [ ] **Step 5: Aggiornare i test esistenti**

Cercare con `Grep -i "disabled" DashboardView.spec.ts CommandPalette.spec.ts PazienteDettaglioView.spec.ts` (relativi a "piano alimentare"/"Nuovo piano") e sostituire le asserzioni "è disabilitato" con "naviga/è un link verso `/piani-alimentari/nuovo`", stesso pattern già usato per "Nuova visita" quando è stata abilitata in una sessione precedente.

- [ ] **Step 6: Eseguire l'intera suite frontend**

Run: `npx vitest run` da `frontend-professionisti/`
Expected: tutti i test verdi (inclusi quelli aggiornati in questo task).

- [ ] **Step 7: `tsc --noEmit` e build**

Run: `npx tsc --noEmit` poi `npm run build` da `frontend-professionisti/`
Expected: entrambi puliti.

- [ ] **Step 8: Staging**

```bash
git add frontend-professionisti/src/components/AppSidebar.vue
git add frontend-professionisti/src/components/CommandPalette.vue frontend-professionisti/src/components/CommandPalette.spec.ts
git add frontend-professionisti/src/views/DashboardView.vue frontend-professionisti/src/views/DashboardView.spec.ts
git add frontend-professionisti/src/views/pazienti/PazienteDettaglioView.vue frontend-professionisti/src/views/pazienti/PazienteDettaglioView.spec.ts
```

Aggiornare `wiki/stato.md` con un handoff di sessione e `wiki/domande-aperte.md` (rimuovere/aggiornare le voci ora risolte: snapshot vs riferimento runtime, stato "Inviato" vs "Attivo").

---

## Self-Review

**Copertura spec**: modello dati (Task 1), calcolo TDEE con tutte le decisioni di Andrea incluso "sempre 0%" (Task 2), creazione/dettaglio (Task 3), salvataggio/sostituzione totale per le 3 modalità (Task 4), attivazione con disattivazione del piano precedente ed eliminazione ristretta a BOZZA (Task 5), ricerca paginata con SCADUTO calcolato (Task 6), client API (Task 7), modal alimenti condiviso (Task 8), editor PASTI (Task 9), MACRO (Task 10), ESEMPI (Task 11), obiettivo/attiva/elimina/stampa (Task 12), lista (Task 13), 4 punti di ingresso (Task 14) — nessuna sezione della spec priva di un task corrispondente.

**Placeholder**: nessun "TBD"/"implementa dopo"; le uniche note di verifica esplicite (firme di `Visita`/`Plicometria`/`Paziente` in Task 2, pattern JWT in Task 3, eventuale adattamento dei selettori nei test in Task 8-9) rimandano a codice reale già letto durante questo piano, non a decisioni rimandate.

**Coerenza dei tipi**: `PianoAlimentareResponse`/`PianoAlimentare` (TS) hanno gli stessi 17 campi in Task 3 e Task 7; `RigaAlimentoRequest`/`RigaLocale` restano allineati tra Task 7 e Task 9 (stessi 7 campi + `grammi`); `pianoDiProprieta`/`pazienteDiProprieta` (Task 3) sono riusati identici nei Task 4-6, mai ridefiniti.

<!-- END -->
