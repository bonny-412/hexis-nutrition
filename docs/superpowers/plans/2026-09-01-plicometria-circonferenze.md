# Plicometria e Circonferenze — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aggiungere il modulo Plicometria (6 protocolli: Jackson-Pollock 3/7, Durnin-Womersley 4, Faulkner 4, Slaughter pediatrico, Evans atleti) e ridisegnare il modulo Circonferenze sulla visita, con calcolo e persistenza backend dei valori derivati (BMI, WHR, WHtR, MAMC, %BF, FM, FFM, FMI, FFMI).

**Architecture:** Backend Spring Boot: nuove colonne su `visite`, nuova tabella `plicometrie` (1:1 opzionale con `visite`) e tabella di riferimento `durnin_womersley_coefficienti`. Motore di calcolo a strategy pattern (`CalcolatorePlicometria`, un'implementazione per protocollo). `PlicometriaService` valida, orchestra, applica il limite di sicurezza biologico e salva. Frontend Vue: `Select` sesso a 3 valori, redesign dell'accordion circonferenze in `DatiVisitaForm.vue`, nuovo `PlicometriaForm.vue` con campi condizionati dal protocollo e tripla misurazione con media live.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Flyway, PostgreSQL 13, Vue 3 + TypeScript + Vite + Tailwind + shadcn-vue, Vitest.

**Spec:** [`docs/superpowers/specs/2026-09-01-plicometria-circonferenze-design.md`](../specs/2026-09-01-plicometria-circonferenze-design.md)

## Global Constraints

- BIA resta esclusa (placeholder invariato in `DatiVisitaForm.vue`).
- Nessuna anteprima live dei valori derivati nel form prima del salvataggio — solo backend calcola e persiste; l'unico calcolo frontend è la media delle 3 sotto-misurazioni di una plica.
- Nessuna nuova UI di visualizzazione risultati in questo giro (nessun dettaglio paziente/visita esiste ancora).
- Tutte le pliche in mm, circonferenze in cm, altezza in cm convertita in metri solo nei calcoli che lo richiedono.
- Precisione: circonferenze/peso `NUMERIC(6,2)`, pliche `NUMERIC(5,2)`, indici (%BF, FMI, FFMI, BMI...) `NUMERIC(5,2)`, densità corporea `NUMERIC(6,4)`, WHR/WHtR `NUMERIC(4,2)`.
- Arrotondamento: `RoundingMode.HALF_UP` ovunque nel backend.
- Limite di sicurezza biologico su `%BF`: `max(3.0, %BF)` per sesso M, `max(10.0, %BF)` per sesso F — applicato a tutti e 6 i protocolli, dopo il calcolo del calcolatore, prima di derivare FM/FFM/FMI/FFMI (interpretazione dello spec, vedi nota lì).
- Età per le formule plicometriche: sempre calcolata backend da `paziente.dataNascita` + `visita.dataVisita` (mai un valore manuale, mai "oggi").
- Storicità: ogni riga `plicometrie` salva `formula_versione` (stringa costante per protocollo) e, per Durnin-Womersley, lo snapshot dei coefficienti `c`/`m` effettivamente usati.
- `git commit` non va mai eseguito dall'agente: solo `git add` a fine task, poi segnalare ad Andrea.
- Dopo ogni modifica backend: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` poi `mvn test` da `backend/`.
- Dopo ogni modifica frontend: `npm run test` (Vitest) e `npx tsc --noEmit` da `frontend-professionisti/`.
- Se aggiungi una tabella, aggiungila al `TRUNCATE` in `AbstractIntegrationTest` — **eccetto** `durnin_womersley_coefficienti`, che è dato di riferimento seminato da migrazione e deve sopravvivere tra un test e l'altro.

---

## Task 1: Sesso obbligatorio a 3 valori (backend)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Sesso.java`
- Create: `backend/src/main/resources/db/migration/V9__paziente_sesso_obbligatorio.sql`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/inviti/TokenAzioneRepositoryTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java`

**Interfaces:**
- Produces: `enum Sesso { M, F, ALTRO }` — usato da `Paziente.getSesso(): Sesso`, `CreaPazienteRequest.sesso(): Sesso`, e da tutti i task successivi (Plicometria, VisitaCalcoli).

- [ ] **Step 1: Crea l'enum `Sesso`**

```java
package com.hexisnutrition.backend.pazienti;

public enum Sesso {
    M,
    F,
    ALTRO
}
```

- [ ] **Step 2: Scrivi la migrazione V9**

```sql
ALTER TABLE pazienti ALTER COLUMN sesso SET NOT NULL;
ALTER TABLE pazienti ADD CONSTRAINT chk_pazienti_sesso CHECK (sesso IN ('M', 'F', 'ALTRO'));
```

Sicura: il database `hexis` (produzione) è vuoto, confermato da Andrea l'1 settembre 2026; `hexis_test` viene ricreato dai test.

- [ ] **Step 3: Aggiorna `Paziente.java`**

Cambia il campo, il getter e il parametro del costruttore da `String sesso` a `Sesso sesso`:

```java
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sesso sesso;
```

Aggiungi import `jakarta.persistence.EnumType` (già presente, usato da `tipoLavoro`) e cambia:

```java
    public Paziente(UUID professionistaId, String nome, String cognome, String email,
                     String telefono, LocalDate dataNascita, Sesso sesso, String lavoro, TipoLavoro tipoLavoro) {
```

e

```java
    public Sesso getSesso() {
        return sesso;
    }
```

- [ ] **Step 4: Aggiorna `CreaPazienteRequest.java`**

```java
public record CreaPazienteRequest(
        @NotBlank String nome,
        @NotBlank String cognome,
        @NotBlank @Email String email,
        String telefono,
        @NotNull LocalDate dataNascita,
        @NotNull Sesso sesso,
        String lavoro,
        TipoLavoro tipoLavoro,
        @NotNull @Valid VisitaRequest visita
) {
}
```

- [ ] **Step 5: Aggiorna `PazienteResponse.java`**

```java
public record PazienteResponse(
        UUID id,
        String nome,
        String cognome,
        String email,
        String telefono,
        LocalDate dataNascita,
        String sesso,
        String lavoro,
        String tipoLavoro,
        String statoAccount
) {
    public static PazienteResponse da(Paziente paziente) {
        return new PazienteResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getEmail(), paziente.getTelefono(), paziente.getDataNascita(),
                paziente.getSesso().name(), paziente.getLavoro(),
                paziente.getTipoLavoro() != null ? paziente.getTipoLavoro().name() : null,
                paziente.getStatoAccount().name());
    }
}
```

(`sesso` non è mai `null` ora, quindi niente più controllo condizionale come per `tipoLavoro`.)

`PazienteService.crea()` non richiede modifiche in questo task: già passa `request.sesso()` posizionalmente al costruttore di `Paziente`, il cambio di tipo si propaga da solo.

- [ ] **Step 6: Aggiorna i fixture di test esistenti — pattern `null, null, null`**

In questi 4 file, gli argomenti posizionali 7/8/9 del costruttore `Paziente` (`sesso`, `lavoro`, `tipoLavoro`) sono `null, null, null` dopo un `LocalDate.of(...)`. Sostituisci ogni occorrenza di `LocalDate.of(1990, 1, 1), null, null, null` con `LocalDate.of(1990, 1, 1), Sesso.M, null, null` (usa `replace_all` dove il file ha più occorrenze):

- `TokenAzioneRepositoryTest.java` (1 occorrenza, riga 42) — aggiungi anche `import com.hexisnutrition.backend.pazienti.Sesso;`
- `VisitaRepositoryTest.java` (1 occorrenza, riga 39) — stesso package di `Sesso`, nessun import da aggiungere
- `PazienteControllerTest.java` (11 occorrenze: righe 308, 310, 326, 338, 353, 385, 404, 424, 439, 454, e anche 370 che ha lo stesso testo ma senza il doppio `)` finale — usa `replace_all` con la stringa esatta `LocalDate.of(1990, 1, 1), null, null, null` che cattura entrambe le varianti) — stesso package, nessun import
- `AuthControllerTest.java` (3 occorrenze: righe 83, 171, 235) — aggiungi `import com.hexisnutrition.backend.pazienti.Sesso;`

- [ ] **Step 7: Aggiorna il fixture con sesso già valorizzato**

In `PazienteRepositoryTest.java` riga 36, sostituisci `"3331234567", LocalDate.of(1990, 5, 20), "M", "Impiegato", TipoLavoro.ATTIVO)` con `"3331234567", LocalDate.of(1990, 5, 20), Sesso.M, "Impiegato", TipoLavoro.ATTIVO)`.

- [ ] **Step 8: Aggiungi `"sesso":"M"` ai body JSON di successo che ne sono privi**

In `PazienteControllerTest.java`, questi 4 test si aspettano `status().isCreated()` ma il body JSON non contiene `"sesso"` — aggiungilo (tra `"dataNascita"` e il resto), altrimenti falliscono con 400 dopo questo task:

- `creaPazienteConDataVisitaLaPersisteEsattamente` (riga 143-144): dopo `"dataNascita":"1990-05-20",` aggiungi `"sesso":"M",`
- `creaPazienteSenzaDataVisitaUsaLaDataOdierna` (riga 163-164): stesso pattern
- `creaPazienteConPesoECirconferenzeADueDecimaliLiPersisteEsattamente` (riga 183-184): stesso pattern
- `creaPazienteConLavoroETipoLavoroLiRestituisceNellaRisposta` (riga 291-293): dopo `"dataNascita":"1990-05-20",` aggiungi `"sesso":"M",` prima di `"lavoro":"Impiegato",`

Non toccare `creaPazienteConTutteLe14MisurazioniDellaVisitaLePersisteNeiCampiCorretti`: viene riscritto interamente nel Task 2 (usa i vecchi campi circonferenza).

- [ ] **Step 9: Esegui la suite backend**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn test
```

Expected: BUILD SUCCESS, tutti i test verdi (nessuna nuova funzionalità aggiunta in questo task, solo un campo reso obbligatorio — se qualcosa fallisce è un fixture non aggiornato, cercalo con `grep -rn "new Paziente(" backend/src/test`).

- [ ] **Step 10: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/Sesso.java
git add backend/src/main/resources/db/migration/V9__paziente_sesso_obbligatorio.sql
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java
git add backend/src/test/java/com/hexisnutrition/backend/inviti/TokenAzioneRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
git add backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java
```

Non eseguire `git commit` — è responsabilità di Andrea.

---

## Task 2: Redesign circonferenze su `Visita` (backend)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/ProtocolloVita.java`
- Create: `backend/src/main/resources/db/migration/V10__redesign_circonferenze_visita.sql`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Visita.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: `Sesso` (Task 1, non usato direttamente qui ma nello stesso file `PazienteService`).
- Produces: `Visita` con i nuovi getter/setter (`getCirconferenzaAddomeCm()`, `getCirconferenzaBraccioRilassatoCm()`, `getCirconferenzaCosciaCm()`, `getCirconferenzaPolpaccioCm()`, `getCirconferenzaColloCm()`, `getCirconferenzaToraceCm()`, `getCirconferenzaBraccioContrattoCm()`, `getCirconferenzaAvambraccioCm()`, `getCirconferenzaCavigliaCm()`, `getProtocolloVita()`, più `setBmi`/`getBmi`, `setWhr`/`getWhr`, `setWhtr`/`getWhtr`, `setMamcCm`/`getMamcCm` usati dal Task 3 e dal Task 7); `ProtocolloVita { OMS, OMBELICALE, ALTRO }`.

- [ ] **Step 1: Crea l'enum `ProtocolloVita`**

```java
package com.hexisnutrition.backend.pazienti;

public enum ProtocolloVita {
    OMS,
    OMBELICALE,
    ALTRO
}
```

- [ ] **Step 2: Scrivi la migrazione V10**

```sql
ALTER TABLE visite DROP COLUMN circonferenza_ombelico_cm;
ALTER TABLE visite DROP COLUMN circonferenza_petto_cm;
ALTER TABLE visite DROP COLUMN circonferenza_coscia_dx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_coscia_sx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_polpaccio_dx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_polpaccio_sx_cm;
ALTER TABLE visite DROP COLUMN larghezza_spalle_cm;
ALTER TABLE visite DROP COLUMN circonferenza_spalle_cm;
ALTER TABLE visite DROP COLUMN circonferenza_bicipite_dx_cm;
ALTER TABLE visite DROP COLUMN circonferenza_bicipite_sx_cm;

ALTER TABLE visite ADD COLUMN circonferenza_addome_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_braccio_rilassato_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_coscia_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_polpaccio_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_collo_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_torace_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_braccio_contratto_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_avambraccio_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_caviglia_cm NUMERIC(6,2);

ALTER TABLE visite ADD COLUMN protocollo_vita VARCHAR(20) NOT NULL DEFAULT 'OMS';
ALTER TABLE visite ADD COLUMN bmi NUMERIC(5,2);
ALTER TABLE visite ADD COLUMN whr NUMERIC(4,2);
ALTER TABLE visite ADD COLUMN whtr NUMERIC(4,2);
ALTER TABLE visite ADD COLUMN mamc_cm NUMERIC(5,2);
```

- [ ] **Step 3: Riscrivi `Visita.java`**

Sostituisci i campi/getter delle 10 vecchie circonferenze con i nuovi, mantenendo `circonferenzaVitaCm` e `circonferenzaFianchiCm` invariati. Il file completo diventa:

```java
package com.hexisnutrition.backend.pazienti;

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
@Table(name = "visite")
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "paziente_id", nullable = false)
    private UUID pazienteId;

    @Column(name = "data_visita", nullable = false)
    private LocalDate dataVisita = LocalDate.now();

    @Column(name = "altezza_cm", nullable = false)
    private Integer altezzaCm;

    @Column(name = "peso_kg", nullable = false)
    private BigDecimal pesoKg;

    @Column(name = "circonferenza_vita_cm")
    private BigDecimal circonferenzaVitaCm;

    @Column(name = "circonferenza_fianchi_cm")
    private BigDecimal circonferenzaFianchiCm;

    @Column(name = "circonferenza_addome_cm")
    private BigDecimal circonferenzaAddomeCm;

    @Column(name = "circonferenza_braccio_rilassato_cm")
    private BigDecimal circonferenzaBraccioRilassatoCm;

    @Column(name = "circonferenza_coscia_cm")
    private BigDecimal circonferenzaCosciaCm;

    @Column(name = "circonferenza_polpaccio_cm")
    private BigDecimal circonferenzaPolpaccioCm;

    @Column(name = "circonferenza_collo_cm")
    private BigDecimal circonferenzaColloCm;

    @Column(name = "circonferenza_torace_cm")
    private BigDecimal circonferenzaToraceCm;

    @Column(name = "circonferenza_braccio_contratto_cm")
    private BigDecimal circonferenzaBraccioContrattoCm;

    @Column(name = "circonferenza_avambraccio_cm")
    private BigDecimal circonferenzaAvambraccioCm;

    @Column(name = "circonferenza_caviglia_cm")
    private BigDecimal circonferenzaCavigliaCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocollo_vita", nullable = false)
    private ProtocolloVita protocolloVita = ProtocolloVita.OMS;

    @Column(name = "bmi")
    private BigDecimal bmi;

    @Column(name = "whr")
    private BigDecimal whr;

    @Column(name = "whtr")
    private BigDecimal whtr;

    @Column(name = "mamc_cm")
    private BigDecimal mamcCm;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Visita() {
    }

    public Visita(UUID pazienteId, LocalDate dataVisita, Integer altezzaCm, BigDecimal pesoKg,
                  BigDecimal circonferenzaVitaCm, BigDecimal circonferenzaFianchiCm,
                  BigDecimal circonferenzaAddomeCm, BigDecimal circonferenzaBraccioRilassatoCm,
                  BigDecimal circonferenzaCosciaCm, BigDecimal circonferenzaPolpaccioCm,
                  BigDecimal circonferenzaColloCm, BigDecimal circonferenzaToraceCm,
                  BigDecimal circonferenzaBraccioContrattoCm, BigDecimal circonferenzaAvambraccioCm,
                  BigDecimal circonferenzaCavigliaCm, ProtocolloVita protocolloVita) {
        this.pazienteId = pazienteId;
        if (dataVisita != null) {
            this.dataVisita = dataVisita;
        }
        this.altezzaCm = altezzaCm;
        this.pesoKg = pesoKg;
        this.circonferenzaVitaCm = circonferenzaVitaCm;
        this.circonferenzaFianchiCm = circonferenzaFianchiCm;
        this.circonferenzaAddomeCm = circonferenzaAddomeCm;
        this.circonferenzaBraccioRilassatoCm = circonferenzaBraccioRilassatoCm;
        this.circonferenzaCosciaCm = circonferenzaCosciaCm;
        this.circonferenzaPolpaccioCm = circonferenzaPolpaccioCm;
        this.circonferenzaColloCm = circonferenzaColloCm;
        this.circonferenzaToraceCm = circonferenzaToraceCm;
        this.circonferenzaBraccioContrattoCm = circonferenzaBraccioContrattoCm;
        this.circonferenzaAvambraccioCm = circonferenzaAvambraccioCm;
        this.circonferenzaCavigliaCm = circonferenzaCavigliaCm;
        if (protocolloVita != null) {
            this.protocolloVita = protocolloVita;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPazienteId() {
        return pazienteId;
    }

    public LocalDate getDataVisita() {
        return dataVisita;
    }

    public Integer getAltezzaCm() {
        return altezzaCm;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public BigDecimal getCirconferenzaVitaCm() {
        return circonferenzaVitaCm;
    }

    public BigDecimal getCirconferenzaFianchiCm() {
        return circonferenzaFianchiCm;
    }

    public BigDecimal getCirconferenzaAddomeCm() {
        return circonferenzaAddomeCm;
    }

    public BigDecimal getCirconferenzaBraccioRilassatoCm() {
        return circonferenzaBraccioRilassatoCm;
    }

    public BigDecimal getCirconferenzaCosciaCm() {
        return circonferenzaCosciaCm;
    }

    public BigDecimal getCirconferenzaPolpaccioCm() {
        return circonferenzaPolpaccioCm;
    }

    public BigDecimal getCirconferenzaColloCm() {
        return circonferenzaColloCm;
    }

    public BigDecimal getCirconferenzaToraceCm() {
        return circonferenzaToraceCm;
    }

    public BigDecimal getCirconferenzaBraccioContrattoCm() {
        return circonferenzaBraccioContrattoCm;
    }

    public BigDecimal getCirconferenzaAvambraccioCm() {
        return circonferenzaAvambraccioCm;
    }

    public BigDecimal getCirconferenzaCavigliaCm() {
        return circonferenzaCavigliaCm;
    }

    public ProtocolloVita getProtocolloVita() {
        return protocolloVita;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    public BigDecimal getWhr() {
        return whr;
    }

    public void setWhr(BigDecimal whr) {
        this.whr = whr;
    }

    public BigDecimal getWhtr() {
        return whtr;
    }

    public void setWhtr(BigDecimal whtr) {
        this.whtr = whtr;
    }

    public BigDecimal getMamcCm() {
        return mamcCm;
    }

    public void setMamcCm(BigDecimal mamcCm) {
        this.mamcCm = mamcCm;
    }
}
```

- [ ] **Step 4: Riscrivi `VisitaRequest.java`**

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VisitaRequest(
        LocalDate dataVisita,
        @NotNull @Positive @Max(300) Integer altezzaCm,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal pesoKg,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaVitaCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaFianchiCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaAddomeCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBraccioRilassatoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCosciaCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaPolpaccioCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaColloCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaToraceCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBraccioContrattoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaAvambraccioCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCavigliaCm,
        ProtocolloVita protocolloVita
) {
}
```

(Il campo `plicometria` verrà aggiunto in coda a questo record nel Task 6 — non aggiungerlo ora.)

- [ ] **Step 5: Aggiorna `PazienteService.crea()`**

Sostituisci il blocco di costruzione della `Visita`:

```java
        VisitaRequest v = request.visita();
        Visita visita = new Visita(paziente.getId(), v.dataVisita(), v.altezzaCm(), v.pesoKg(),
                v.circonferenzaVitaCm(), v.circonferenzaFianchiCm(), v.circonferenzaAddomeCm(),
                v.circonferenzaBraccioRilassatoCm(), v.circonferenzaCosciaCm(), v.circonferenzaPolpaccioCm(),
                v.circonferenzaColloCm(), v.circonferenzaToraceCm(), v.circonferenzaBraccioContrattoCm(),
                v.circonferenzaAvambraccioCm(), v.circonferenzaCavigliaCm(), v.protocolloVita());
        visitaRepository.save(visita);

        return paziente;
```

(Il calcolo di BMI/WHR/WHtR via `VisitaCalcoli.applica(visita)` viene inserito nel Task 3 prima di `visitaRepository.save(visita)`; il wiring di `PlicometriaService` nel Task 6.)

- [ ] **Step 6: Riscrivi `VisitaRepositoryTest.salvaERitrovaPerPaziente`**

```java
    @Test
    void salvaERitrovaPerPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("visite-prof@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "visite-luca@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        Visita visita = new Visita(paziente.getId(), null, 178, new BigDecimal("82.5"),
                new BigDecimal("95.0"), new BigDecimal("100.0"), null, new BigDecimal("32.0"),
                new BigDecimal("58.0"), new BigDecimal("38.0"), null, null, null, null, null,
                ProtocolloVita.OMS);
        visitaRepository.save(visita);

        List<Visita> visite = visitaRepository.findAllByPazienteId(paziente.getId());

        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getAltezzaCm()).isEqualTo(178);
        assertThat(visite.get(0).getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visite.get(0).getCirconferenzaFianchiCm()).isEqualByComparingTo("100.0");
        assertThat(visite.get(0).getCirconferenzaAddomeCm()).isNull();
        assertThat(visite.get(0).getCirconferenzaColloCm()).isNull();
        assertThat(visite.get(0).getProtocolloVita()).isEqualTo(ProtocolloVita.OMS);
        assertThat(visite.get(0).getDataVisita()).isNotNull();
    }
```

- [ ] **Step 7: Riscrivi il test "14 misurazioni" in `PazienteControllerTest.java`**

Sostituisci l'intero metodo `creaPazienteConTutteLe14MisurazioniDellaVisitaLePersisteNeiCampiCorretti` (righe 94-132) con:

```java
    @Test
    void creaPazienteConTutteLeCirconferenzeLePersisteNeiCampiCorretti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-11-misure@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-11-misure@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,
                                 "circonferenzaVitaCm":90.1,"circonferenzaFianchiCm":91.2,
                                 "circonferenzaAddomeCm":92.3,"circonferenzaBraccioRilassatoCm":93.4,
                                 "circonferenzaCosciaCm":94.5,"circonferenzaPolpaccioCm":95.6,
                                 "circonferenzaColloCm":96.7,"circonferenzaToraceCm":97.8,
                                 "circonferenzaBraccioContrattoCm":98.9,"circonferenzaAvambraccioCm":99.0,
                                 "circonferenzaCavigliaCm":100.1,"protocolloVita":"OMBELICALE"}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        Visita visita = visite.get(0);
        assertThat(visita.getAltezzaCm()).isEqualTo(178);
        assertThat(visita.getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visita.getCirconferenzaVitaCm()).isEqualByComparingTo("90.1");
        assertThat(visita.getCirconferenzaFianchiCm()).isEqualByComparingTo("91.2");
        assertThat(visita.getCirconferenzaAddomeCm()).isEqualByComparingTo("92.3");
        assertThat(visita.getCirconferenzaBraccioRilassatoCm()).isEqualByComparingTo("93.4");
        assertThat(visita.getCirconferenzaCosciaCm()).isEqualByComparingTo("94.5");
        assertThat(visita.getCirconferenzaPolpaccioCm()).isEqualByComparingTo("95.6");
        assertThat(visita.getCirconferenzaColloCm()).isEqualByComparingTo("96.7");
        assertThat(visita.getCirconferenzaToraceCm()).isEqualByComparingTo("97.8");
        assertThat(visita.getCirconferenzaBraccioContrattoCm()).isEqualByComparingTo("98.9");
        assertThat(visita.getCirconferenzaAvambraccioCm()).isEqualByComparingTo("99.0");
        assertThat(visita.getCirconferenzaCavigliaCm()).isEqualByComparingTo("100.1");
        assertThat(visita.getProtocolloVita()).isEqualTo(ProtocolloVita.OMBELICALE);
    }

    @Test
    void creaPazienteSenzaProtocolloVitaUsaOmsPerDefault() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-protocollo-default@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-protocollo-default@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite.get(0).getProtocolloVita()).isEqualTo(ProtocolloVita.OMS);
    }
```

Aggiungi `import com.hexisnutrition.backend.pazienti.ProtocolloVita;` non serve (stesso package). Nessun altro test in questo file referenzia i vecchi campi circonferenza rimossi (verificato con grep prima di scrivere questo piano).

- [ ] **Step 8: Esegui la suite backend**

```powershell
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 9: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/ProtocolloVita.java
git add backend/src/main/resources/db/migration/V10__redesign_circonferenze_visita.sql
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/Visita.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Task 3: `VisitaCalcoli` — BMI, WHR, WHtR (backend)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaCalcoli.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaCalcoliTest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: `Visita` (Task 2) — legge `altezzaCm`, `pesoKg`, `circonferenzaVitaCm`, `circonferenzaFianchiCm`; scrive via `setBmi`/`setWhr`/`setWhtr`.
- Produces: `VisitaCalcoli.applica(Visita visita): void` — chiamato da `PazienteService.crea()` prima del salvataggio. `VisitaCalcoli.applicaMamc(Visita visita, BigDecimal plicaTricipitaleMm): void` — usato dal Task 7, implementato già qui.

- [ ] **Step 1: Scrivi il test di `VisitaCalcoli`**

```java
package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VisitaCalcoliTest {

    private Visita visitaConMisure(BigDecimal vita, BigDecimal fianchi) {
        return new Visita(UUID.randomUUID(), null, 180, new BigDecimal("82.50"),
                vita, fianchi, null, null, null, null, null, null, null, null, null,
                ProtocolloVita.OMS);
    }

    @Test
    void calcolaBmiSempre() {
        Visita visita = visitaConMisure(null, null);

        VisitaCalcoli.applica(visita);

        assertThat(visita.getBmi()).isEqualByComparingTo("25.46");
    }

    @Test
    void calcolaWhrSoloSeVitaEFianchiPresenti() {
        Visita conEntrambe = visitaConMisure(new BigDecimal("95.00"), new BigDecimal("100.00"));
        Visita senzaFianchi = visitaConMisure(new BigDecimal("95.00"), null);

        VisitaCalcoli.applica(conEntrambe);
        VisitaCalcoli.applica(senzaFianchi);

        assertThat(conEntrambe.getWhr()).isEqualByComparingTo("0.95");
        assertThat(senzaFianchi.getWhr()).isNull();
    }

    @Test
    void calcolaWhtrSoloSeVitaPresente() {
        Visita conVita = visitaConMisure(new BigDecimal("95.00"), null);
        Visita senzaVita = visitaConMisure(null, new BigDecimal("100.00"));

        VisitaCalcoli.applica(conVita);
        VisitaCalcoli.applica(senzaVita);

        assertThat(conVita.getWhtr()).isEqualByComparingTo("0.53");
        assertThat(senzaVita.getWhtr()).isNull();
    }

    @Test
    void applicaMamcSoloSeBraccioRilassatoPresente() {
        Visita conBraccio = new Visita(UUID.randomUUID(), null, 180, new BigDecimal("82.50"),
                null, null, null, new BigDecimal("32.00"), null, null, null, null, null, null, null,
                ProtocolloVita.OMS);
        Visita senzaBraccio = visitaConMisure(null, null);

        VisitaCalcoli.applicaMamc(conBraccio, new BigDecimal("16.00"));
        VisitaCalcoli.applicaMamc(senzaBraccio, new BigDecimal("16.00"));

        assertThat(conBraccio.getMamcCm()).isEqualByComparingTo("26.97");
        assertThat(senzaBraccio.getMamcCm()).isNull();
    }
}
```

Valori attesi verificati a mano: altezza 180cm→1,80m, altezza²=3,24; BMI=82,50/3,24=25,4629...→25,46. WHR=95/100=0,95. WHtR=95/180=0,5277...→0,53. MAMC: 32−(π×16/10)=32−5,02655...=26,97345→26,97 (stesso esempio numerico del PDF "specifiche_gestionale_dietista.pdf").

- [ ] **Step 2: Esegui il test per vederlo fallire**

```powershell
mvn test -Dtest=VisitaCalcoliTest
```

Expected: FAIL, `VisitaCalcoli` non esiste.

- [ ] **Step 3: Implementa `VisitaCalcoli`**

```java
package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class VisitaCalcoli {

    private VisitaCalcoli() {
    }

    public static void applica(Visita visita) {
        BigDecimal altezzaCm = BigDecimal.valueOf(visita.getAltezzaCm());
        BigDecimal altezzaM = altezzaCm.divide(BigDecimal.valueOf(100));
        BigDecimal altezzaM2 = altezzaM.multiply(altezzaM);

        visita.setBmi(visita.getPesoKg().divide(altezzaM2, 2, RoundingMode.HALF_UP));

        BigDecimal vita = visita.getCirconferenzaVitaCm();
        BigDecimal fianchi = visita.getCirconferenzaFianchiCm();

        if (vita != null && fianchi != null) {
            visita.setWhr(vita.divide(fianchi, 2, RoundingMode.HALF_UP));
        }
        if (vita != null) {
            visita.setWhtr(vita.divide(altezzaCm, 2, RoundingMode.HALF_UP));
        }
    }

    public static void applicaMamc(Visita visita, BigDecimal plicaTricipitaleMm) {
        BigDecimal braccio = visita.getCirconferenzaBraccioRilassatoCm();
        if (braccio == null || plicaTricipitaleMm == null) {
            return;
        }
        BigDecimal termineCm = BigDecimal.valueOf(Math.PI)
                .multiply(plicaTricipitaleMm)
                .divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
        visita.setMamcCm(braccio.subtract(termineCm).setScale(2, RoundingMode.HALF_UP));
    }
}
```

- [ ] **Step 4: Esegui di nuovo il test**

```powershell
mvn test -Dtest=VisitaCalcoliTest
```

Expected: PASS, 4 test verdi.

- [ ] **Step 5: Chiama `VisitaCalcoli.applica` da `PazienteService.crea()`**

Nel blocco scritto nel Task 2, prima di `visitaRepository.save(visita);`, inserisci:

```java
        VisitaCalcoli.applica(visita);
        visitaRepository.save(visita);
```

- [ ] **Step 6: Aggiungi un test di integrazione in `PazienteControllerTest.java`**

```java
    @Test
    void creaPazienteConVitaEFianchiCalcolaBmiWhrWhtr() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-bmi-whr@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-bmi-whr@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":180,"pesoKg":82.50,
                                 "circonferenzaVitaCm":95.00,"circonferenzaFianchiCm":100.00}}
                                """))
                .andExpect(status().isCreated());

        Visita visita = visitaRepository.findAll().get(0);
        assertThat(visita.getBmi()).isEqualByComparingTo("25.46");
        assertThat(visita.getWhr()).isEqualByComparingTo("0.95");
        assertThat(visita.getWhtr()).isEqualByComparingTo("0.53");
    }
```

- [ ] **Step 7: Esegui l'intera suite backend**

```powershell
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaCalcoli.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaCalcoliTest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Task 4: Tabelle `plicometrie` e `durnin_womersley_coefficienti` (backend)

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__create_plicometrie.sql`
- Create: `backend/src/main/resources/db/migration/V12__create_durnin_womersley_coefficienti.sql`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/ProtocolloPlicometrico.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/EtniaAtleta.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Plicometria.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaRepository.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/DurninWomersleyCoefficiente.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/DurninWomersleyCoefficienteRepository.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/DurninWomersleyCoefficienteRepositoryTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java`

**Interfaces:**
- Produces: `ProtocolloPlicometrico { JACKSON_POLLOCK_3, JACKSON_POLLOCK_7, DURNIN_WOMERSLEY_4, FAULKNER_4, SLAUGHTER_PEDIATRICO, EVANS_ATLETI }`; `EtniaAtleta { CAUCASICO, AFROAMERICANO }`; entità `Plicometria` con costruttore/getter completi (usati dal Task 6); `DurninWomersleyCoefficienteRepository.trovaCandidati(Sesso sesso, Integer eta): List<DurninWomersleyCoefficiente>` (usato dal Task 5).

- [ ] **Step 1: Scrivi la migrazione V11**

```sql
CREATE TABLE plicometrie (
    id UUID PRIMARY KEY,
    visita_id UUID NOT NULL UNIQUE REFERENCES visite(id),
    protocollo VARCHAR(30) NOT NULL,
    formula_versione VARCHAR(40) NOT NULL,
    eta_anni INTEGER NOT NULL,
    coefficiente_c NUMERIC(6,4),
    coefficiente_m NUMERIC(6,4),
    etnia_atleta VARCHAR(20),
    plica_pettorale_mm NUMERIC(5,2),
    plica_ascellare_mm NUMERIC(5,2),
    plica_tricipitale_mm NUMERIC(5,2),
    plica_bicipitale_mm NUMERIC(5,2),
    plica_sottoscapolare_mm NUMERIC(5,2),
    plica_soprailiaca_mm NUMERIC(5,2),
    plica_addominale_mm NUMERIC(5,2),
    plica_coscia_mm NUMERIC(5,2),
    plica_polpaccio_mm NUMERIC(5,2),
    somma_pliche_mm NUMERIC(6,2) NOT NULL,
    densita_corporea NUMERIC(6,4),
    percentuale_grasso NUMERIC(5,2) NOT NULL,
    massa_grassa_kg NUMERIC(6,2) NOT NULL,
    massa_magra_kg NUMERIC(6,2) NOT NULL,
    fmi NUMERIC(5,2) NOT NULL,
    ffmi NUMERIC(5,2) NOT NULL,
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 2: Scrivi la migrazione V12 (crea + semina la tabella di riferimento)**

```sql
CREATE TABLE durnin_womersley_coefficienti (
    id UUID PRIMARY KEY,
    sesso VARCHAR(1) NOT NULL,
    eta_min INTEGER NOT NULL,
    eta_max INTEGER,
    c NUMERIC(6,4) NOT NULL,
    m NUMERIC(6,4) NOT NULL
);

INSERT INTO durnin_womersley_coefficienti (id, sesso, eta_min, eta_max, c, m) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'M', 17, 19, 1.1620, 0.0630),
    ('a0000000-0000-4000-8000-000000000002', 'M', 20, 29, 1.1631, 0.0632),
    ('a0000000-0000-4000-8000-000000000003', 'M', 30, 39, 1.1422, 0.0544),
    ('a0000000-0000-4000-8000-000000000004', 'M', 40, 49, 1.1620, 0.0700),
    ('a0000000-0000-4000-8000-000000000005', 'M', 50, NULL, 1.1715, 0.0779),
    ('a0000000-0000-4000-8000-000000000006', 'F', 16, 19, 1.1549, 0.0678),
    ('a0000000-0000-4000-8000-000000000007', 'F', 20, 29, 1.1599, 0.0717),
    ('a0000000-0000-4000-8000-000000000008', 'F', 30, 39, 1.1423, 0.0632),
    ('a0000000-0000-4000-8000-000000000009', 'F', 40, 49, 1.1333, 0.0612),
    ('a0000000-0000-4000-8000-000000000010', 'F', 50, NULL, 1.1339, 0.0645);
```

- [ ] **Step 3: Crea gli enum**

```java
package com.hexisnutrition.backend.pazienti;

public enum ProtocolloPlicometrico {
    JACKSON_POLLOCK_3,
    JACKSON_POLLOCK_7,
    DURNIN_WOMERSLEY_4,
    FAULKNER_4,
    SLAUGHTER_PEDIATRICO,
    EVANS_ATLETI
}
```

```java
package com.hexisnutrition.backend.pazienti;

public enum EtniaAtleta {
    CAUCASICO,
    AFROAMERICANO
}
```

- [ ] **Step 4: Crea l'entità `Plicometria`**

```java
package com.hexisnutrition.backend.pazienti;

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
import java.util.UUID;

@Entity
@Table(name = "plicometrie")
public class Plicometria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "visita_id", nullable = false, unique = true)
    private UUID visitaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtocolloPlicometrico protocollo;

    @Column(name = "formula_versione", nullable = false)
    private String formulaVersione;

    @Column(name = "eta_anni", nullable = false)
    private Integer etaAnni;

    @Column(name = "coefficiente_c")
    private BigDecimal coefficienteC;

    @Column(name = "coefficiente_m")
    private BigDecimal coefficienteM;

    @Enumerated(EnumType.STRING)
    @Column(name = "etnia_atleta")
    private EtniaAtleta etniaAtleta;

    @Column(name = "plica_pettorale_mm")
    private BigDecimal plicaPettoraleMm;

    @Column(name = "plica_ascellare_mm")
    private BigDecimal plicaAscellareMm;

    @Column(name = "plica_tricipitale_mm")
    private BigDecimal plicaTricipitaleMm;

    @Column(name = "plica_bicipitale_mm")
    private BigDecimal plicaBicipitaleMm;

    @Column(name = "plica_sottoscapolare_mm")
    private BigDecimal plicaSottoscapolareMm;

    @Column(name = "plica_soprailiaca_mm")
    private BigDecimal plicaSoprailiacaMm;

    @Column(name = "plica_addominale_mm")
    private BigDecimal plicaAddominaleMm;

    @Column(name = "plica_coscia_mm")
    private BigDecimal plicaCosciaMm;

    @Column(name = "plica_polpaccio_mm")
    private BigDecimal plicaPolpaccioMm;

    @Column(name = "somma_pliche_mm", nullable = false)
    private BigDecimal sommaPlicheMm;

    @Column(name = "densita_corporea")
    private BigDecimal densitaCorporea;

    @Column(name = "percentuale_grasso", nullable = false)
    private BigDecimal percentualeGrasso;

    @Column(name = "massa_grassa_kg", nullable = false)
    private BigDecimal massaGrassaKg;

    @Column(name = "massa_magra_kg", nullable = false)
    private BigDecimal massaMagraKg;

    @Column(nullable = false)
    private BigDecimal fmi;

    @Column(nullable = false)
    private BigDecimal ffmi;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Plicometria() {
    }

    public Plicometria(UUID visitaId, ProtocolloPlicometrico protocollo, String formulaVersione, Integer etaAnni,
                        BigDecimal coefficienteC, BigDecimal coefficienteM, EtniaAtleta etniaAtleta,
                        BigDecimal plicaPettoraleMm, BigDecimal plicaAscellareMm, BigDecimal plicaTricipitaleMm,
                        BigDecimal plicaBicipitaleMm, BigDecimal plicaSottoscapolareMm, BigDecimal plicaSoprailiacaMm,
                        BigDecimal plicaAddominaleMm, BigDecimal plicaCosciaMm, BigDecimal plicaPolpaccioMm,
                        BigDecimal sommaPlicheMm, BigDecimal densitaCorporea, BigDecimal percentualeGrasso,
                        BigDecimal massaGrassaKg, BigDecimal massaMagraKg, BigDecimal fmi, BigDecimal ffmi) {
        this.visitaId = visitaId;
        this.protocollo = protocollo;
        this.formulaVersione = formulaVersione;
        this.etaAnni = etaAnni;
        this.coefficienteC = coefficienteC;
        this.coefficienteM = coefficienteM;
        this.etniaAtleta = etniaAtleta;
        this.plicaPettoraleMm = plicaPettoraleMm;
        this.plicaAscellareMm = plicaAscellareMm;
        this.plicaTricipitaleMm = plicaTricipitaleMm;
        this.plicaBicipitaleMm = plicaBicipitaleMm;
        this.plicaSottoscapolareMm = plicaSottoscapolareMm;
        this.plicaSoprailiacaMm = plicaSoprailiacaMm;
        this.plicaAddominaleMm = plicaAddominaleMm;
        this.plicaCosciaMm = plicaCosciaMm;
        this.plicaPolpaccioMm = plicaPolpaccioMm;
        this.sommaPlicheMm = sommaPlicheMm;
        this.densitaCorporea = densitaCorporea;
        this.percentualeGrasso = percentualeGrasso;
        this.massaGrassaKg = massaGrassaKg;
        this.massaMagraKg = massaMagraKg;
        this.fmi = fmi;
        this.ffmi = ffmi;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVisitaId() {
        return visitaId;
    }

    public ProtocolloPlicometrico getProtocollo() {
        return protocollo;
    }

    public String getFormulaVersione() {
        return formulaVersione;
    }

    public Integer getEtaAnni() {
        return etaAnni;
    }

    public BigDecimal getCoefficienteC() {
        return coefficienteC;
    }

    public BigDecimal getCoefficienteM() {
        return coefficienteM;
    }

    public EtniaAtleta getEtniaAtleta() {
        return etniaAtleta;
    }

    public BigDecimal getPlicaPettoraleMm() {
        return plicaPettoraleMm;
    }

    public BigDecimal getPlicaAscellareMm() {
        return plicaAscellareMm;
    }

    public BigDecimal getPlicaTricipitaleMm() {
        return plicaTricipitaleMm;
    }

    public BigDecimal getPlicaBicipitaleMm() {
        return plicaBicipitaleMm;
    }

    public BigDecimal getPlicaSottoscapolareMm() {
        return plicaSottoscapolareMm;
    }

    public BigDecimal getPlicaSoprailiacaMm() {
        return plicaSoprailiacaMm;
    }

    public BigDecimal getPlicaAddominaleMm() {
        return plicaAddominaleMm;
    }

    public BigDecimal getPlicaCosciaMm() {
        return plicaCosciaMm;
    }

    public BigDecimal getPlicaPolpaccioMm() {
        return plicaPolpaccioMm;
    }

    public BigDecimal getSommaPlicheMm() {
        return sommaPlicheMm;
    }

    public BigDecimal getDensitaCorporea() {
        return densitaCorporea;
    }

    public BigDecimal getPercentualeGrasso() {
        return percentualeGrasso;
    }

    public BigDecimal getMassaGrassaKg() {
        return massaGrassaKg;
    }

    public BigDecimal getMassaMagraKg() {
        return massaMagraKg;
    }

    public BigDecimal getFmi() {
        return fmi;
    }

    public BigDecimal getFfmi() {
        return ffmi;
    }
}
```

- [ ] **Step 5: Crea `PlicometriaRepository`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlicometriaRepository extends JpaRepository<Plicometria, UUID> {
}
```

- [ ] **Step 6: Crea l'entità `DurninWomersleyCoefficiente`**

Costruttore pubblico incluso apposta per costruire istanze nei test (Task 5), non solo per idratazione JPA da riga di seed:

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "durnin_womersley_coefficienti")
public class DurninWomersleyCoefficiente {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sesso sesso;

    @Column(name = "eta_min", nullable = false)
    private Integer etaMin;

    @Column(name = "eta_max")
    private Integer etaMax;

    @Column(nullable = false)
    private BigDecimal c;

    @Column(nullable = false)
    private BigDecimal m;

    protected DurninWomersleyCoefficiente() {
    }

    public DurninWomersleyCoefficiente(Sesso sesso, Integer etaMin, Integer etaMax, BigDecimal c, BigDecimal m) {
        this.id = UUID.randomUUID();
        this.sesso = sesso;
        this.etaMin = etaMin;
        this.etaMax = etaMax;
        this.c = c;
        this.m = m;
    }

    public Sesso getSesso() {
        return sesso;
    }

    public Integer getEtaMin() {
        return etaMin;
    }

    public Integer getEtaMax() {
        return etaMax;
    }

    public BigDecimal getC() {
        return c;
    }

    public BigDecimal getM() {
        return m;
    }
}
```

- [ ] **Step 7: Crea `DurninWomersleyCoefficienteRepository`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DurninWomersleyCoefficienteRepository extends JpaRepository<DurninWomersleyCoefficiente, UUID> {

    @Query("""
            SELECT c FROM DurninWomersleyCoefficiente c
            WHERE c.sesso = :sesso
              AND c.etaMin <= :eta
              AND (c.etaMax IS NULL OR c.etaMax >= :eta)
            ORDER BY c.etaMin DESC
            """)
    List<DurninWomersleyCoefficiente> trovaCandidati(@Param("sesso") Sesso sesso, @Param("eta") Integer eta);
}
```

- [ ] **Step 8: Scrivi un test di integrazione per la lookup**

Verifica che la migrazione V12 abbia seminato correttamente i dati e che la query funzioni (fascia 50+, fascia intermedia, età sotto la più bassa):

```java
package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DurninWomersleyCoefficienteRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private DurninWomersleyCoefficienteRepository repository;

    @Test
    void trovaLaFasciaCorrettaPerEtaIntermedia() {
        List<DurninWomersleyCoefficiente> candidati = repository.trovaCandidati(Sesso.M, 25);

        assertThat(candidati).hasSize(1);
        assertThat(candidati.get(0).getC()).isEqualByComparingTo("1.1631");
        assertThat(candidati.get(0).getM()).isEqualByComparingTo("0.0632");
    }

    @Test
    void usaSempreLUltimaFasciaOltreI50Anni() {
        List<DurninWomersleyCoefficiente> candidati = repository.trovaCandidati(Sesso.F, 70);

        assertThat(candidati).hasSize(1);
        assertThat(candidati.get(0).getC()).isEqualByComparingTo("1.1339");
        assertThat(candidati.get(0).getM()).isEqualByComparingTo("0.0645");
    }

    @Test
    void nessunaRigaSottoLaFasciaMinima() {
        List<DurninWomersleyCoefficiente> candidati = repository.trovaCandidati(Sesso.M, 10);

        assertThat(candidati).isEmpty();
    }
}
```

Expected: PASS senza bisogno di seminare dati nel test — i 10 coefficienti sono già in `hexis_test` grazie alla migrazione V12 (eseguita una sola volta all'avvio, non ripetuta ad ogni test).

- [ ] **Step 9: Aggiorna il `TRUNCATE` in `AbstractIntegrationTest.java`**

```java
        jdbcTemplate.execute("TRUNCATE TABLE token_azione, plicometrie, visite, pazienti, professionisti RESTART IDENTITY CASCADE");
```

**Non aggiungere `durnin_womersley_coefficienti`**: è dato di riferimento seminato dalla migrazione, deve restare popolato tra un test e l'altro — se lo tronchi, `DurninWomersleyCoefficienteRepositoryTest` e ogni test del Task 6 che usa Durnin-Womersley falliscono dal secondo test in poi.

- [ ] **Step 10: Esegui l'intera suite backend**

```powershell
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 11: Stage**

```bash
git add backend/src/main/resources/db/migration/V11__create_plicometrie.sql
git add backend/src/main/resources/db/migration/V12__create_durnin_womersley_coefficienti.sql
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/ProtocolloPlicometrico.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/EtniaAtleta.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/Plicometria.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaRepository.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/DurninWomersleyCoefficiente.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/DurninWomersleyCoefficienteRepository.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/DurninWomersleyCoefficienteRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java
```

---

## Task 5: Motore di calcolo — 6 calcolatori plicometrici (backend)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CampoPlica.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicheInput.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/ContestoPlicometria.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/RisultatoDensita.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CalcolatorePlicometria.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CalcoliPlicometria.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/JacksonPollock3Calcolatore.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/JacksonPollock7Calcolatore.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/DurninWomersley4Calcolatore.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Faulkner4Calcolatore.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/SlaughterPediatricoCalcolatore.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/EvansAtletiCalcolatore.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/CalcoliPlicometriaTest.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/JacksonPollock3CalcolatoreTest.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/JacksonPollock7CalcolatoreTest.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/DurninWomersley4CalcolatoreTest.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/Faulkner4CalcolatoreTest.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/SlaughterPediatricoCalcolatoreTest.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/EvansAtletiCalcolatoreTest.java`

**Interfaces:**
- Consumes: `Sesso`, `EtniaAtleta`, `ProtocolloPlicometrico` (Task 1, Task 4); `DurninWomersleyCoefficienteRepository` (Task 4).
- Produces: `CalcolatorePlicometria` (interfaccia, 6 implementazioni `@Component`) usato dal Task 6 come `List<CalcolatorePlicometria>` iniettato in Spring; `CalcoliPlicometria.applicaLimiteSicurezza(double percentualeGrasso, Sesso sesso): double` usato dal Task 6.

- [ ] **Step 1: Crea i tipi di supporto**

```java
package com.hexisnutrition.backend.pazienti;

public enum CampoPlica {
    PETTORALE, ASCELLARE, TRICIPITALE, BICIPITALE, SOTTOSCAPOLARE, SOPRAILIACA, ADDOMINALE, COSCIA, POLPACCIO
}
```

```java
package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;

public record PlicheInput(
        BigDecimal pettoraleMm,
        BigDecimal ascellareMm,
        BigDecimal tricipitaleMm,
        BigDecimal bicipitaleMm,
        BigDecimal sottoscapolareMm,
        BigDecimal soprailiacaMm,
        BigDecimal addominaleMm,
        BigDecimal cosciaMm,
        BigDecimal polpaccioMm
) {
}
```

```java
package com.hexisnutrition.backend.pazienti;

public record ContestoPlicometria(Sesso sesso, int etaAnni, EtniaAtleta etniaAtleta) {
}
```

```java
package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;

public record RisultatoDensita(
        BigDecimal sommaPlicheMm,
        BigDecimal densitaCorporea,
        BigDecimal percentualeGrasso,
        BigDecimal coefficienteC,
        BigDecimal coefficienteM,
        String formulaVersione
) {
}
```

- [ ] **Step 2: Crea l'interfaccia `CalcolatorePlicometria`**

```java
package com.hexisnutrition.backend.pazienti;

import java.util.Set;

public interface CalcolatorePlicometria {

    ProtocolloPlicometrico protocollo();

    Set<CampoPlica> plicheRichieste(Sesso sesso);

    RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto);
}
```

- [ ] **Step 3: Scrivi il test di `CalcoliPlicometria` (helper condiviso + limite di sicurezza)**

```java
package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalcoliPlicometriaTest {

    @Test
    void sommaValoriBigDecimal() {
        double somma = CalcoliPlicometria.somma(new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));

        assertThat(somma).isEqualTo(30.0);
    }

    @Test
    void arrotondaConHalfUp() {
        assertThat(CalcoliPlicometria.arrotonda(8.5097, 2)).isEqualByComparingTo("8.51");
        assertThat(CalcoliPlicometria.arrotonda(11.903, 2)).isEqualByComparingTo("11.90");
    }

    @Test
    void limiteSicurezzaNonAlteraValoriSopraSoglia() {
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(5.0, Sesso.M)).isEqualTo(5.0);
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(15.0, Sesso.F)).isEqualTo(15.0);
    }

    @Test
    void limiteSicurezzaAlzaValoriSottoSoglia() {
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(2.9, Sesso.M)).isEqualTo(3.0);
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(8.0, Sesso.F)).isEqualTo(10.0);
    }
}
```

- [ ] **Step 4: Esegui il test per vederlo fallire**

```powershell
mvn test -Dtest=CalcoliPlicometriaTest
```

Expected: FAIL, `CalcoliPlicometria` non esiste.

- [ ] **Step 5: Implementa `CalcoliPlicometria`**

```java
package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CalcoliPlicometria {

    private CalcoliPlicometria() {
    }

    static double somma(BigDecimal... valori) {
        double totale = 0;
        for (BigDecimal valore : valori) {
            totale += valore.doubleValue();
        }
        return totale;
    }

    static BigDecimal arrotonda(double valore, int scala) {
        return BigDecimal.valueOf(valore).setScale(scala, RoundingMode.HALF_UP);
    }

    static double percentualeGrassoSiri(double densitaCorporea) {
        return 495 / densitaCorporea - 450;
    }

    static double applicaLimiteSicurezza(double percentualeGrasso, Sesso sesso) {
        double soglia = sesso == Sesso.M ? 3.0 : 10.0;
        return Math.max(soglia, percentualeGrasso);
    }
}
```

- [ ] **Step 6: Esegui di nuovo il test**

```powershell
mvn test -Dtest=CalcoliPlicometriaTest
```

Expected: PASS, 4 test verdi.

- [ ] **Step 7: Scrivi il test di `JacksonPollock3Calcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonPollock3CalcolatoreTest {

    private final JacksonPollock3Calcolatore calcolatore = new JacksonPollock3Calcolatore();

    @Test
    void plicheRichiesteDipendonoDalSesso() {
        assertThat(calcolatore.plicheRichieste(Sesso.M))
                .containsExactlyInAnyOrder(CampoPlica.PETTORALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA);
        assertThat(calcolatore.plicheRichieste(Sesso.F))
                .containsExactlyInAnyOrder(CampoPlica.TRICIPITALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
    }

    @Test
    void calcolaDensitaEPercentualeGrassoUomo() {
        PlicheInput pliche = new PlicheInput(new BigDecimal("10.00"), null, null, null, null, null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 25, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("30.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0796, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(8.51, Offset.offset(0.05));
        assertThat(risultato.formulaVersione()).isEqualTo("jackson-pollock-1978-3siti");
    }

    @Test
    void calcolaDensitaEPercentualeGrassoDonna() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("15.00"), null, null,
                new BigDecimal("10.00"), null, new BigDecimal("15.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 25, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("40.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0600, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(16.99, Offset.offset(0.05));
    }
}
```

- [ ] **Step 8: Implementa `JacksonPollock3Calcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class JacksonPollock3Calcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.JACKSON_POLLOCK_3;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return sesso == Sesso.M
                ? EnumSet.of(CampoPlica.PETTORALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA)
                : EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s3 = contesto.sesso() == Sesso.M
                ? CalcoliPlicometria.somma(pliche.pettoraleMm(), pliche.addominaleMm(), pliche.cosciaMm())
                : CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.soprailiacaMm(), pliche.cosciaMm());

        double eta = contesto.etaAnni();
        double d = contesto.sesso() == Sesso.M
                ? 1.109380 - 0.0008267 * s3 + 0.0000016 * s3 * s3 - 0.0002574 * eta
                : 1.0994921 - 0.0009929 * s3 + 0.0000023 * s3 * s3 - 0.0001392 * eta;

        double percentualeGrasso = CalcoliPlicometria.percentualeGrassoSiri(d);

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s3, 2), CalcoliPlicometria.arrotonda(d, 4),
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null,
                "jackson-pollock-1978-3siti");
    }
}
```

- [ ] **Step 9: Esegui il test per vederlo passare**

```powershell
mvn test -Dtest=JacksonPollock3CalcolatoreTest
```

Expected: PASS, 3 test verdi.

- [ ] **Step 10: Scrivi il test di `JacksonPollock7Calcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonPollock7CalcolatoreTest {

    private final JacksonPollock7Calcolatore calcolatore = new JacksonPollock7Calcolatore();

    @Test
    void plicheRichiesteSonoLeStesseSetteSediIndipendentementeDalSesso() {
        assertThat(calcolatore.plicheRichieste(Sesso.M)).containsExactlyInAnyOrder(
                CampoPlica.PETTORALE, CampoPlica.ASCELLARE, CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE,
                CampoPlica.ADDOMINALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
        assertThat(calcolatore.plicheRichieste(Sesso.F)).isEqualTo(calcolatore.plicheRichieste(Sesso.M));
    }

    @Test
    void calcolaDensitaEPercentualeGrassoUomo() {
        PlicheInput pliche = new PlicheInput(new BigDecimal("10.00"), new BigDecimal("12.00"),
                new BigDecimal("8.00"), null, new BigDecimal("14.00"), new BigDecimal("10.00"),
                new BigDecimal("16.00"), new BigDecimal("12.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 30, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("82.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0714, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(12.02, Offset.offset(0.05));
    }

    @Test
    void calcolaDensitaEPercentualeGrassoDonna() {
        PlicheInput pliche = new PlicheInput(new BigDecimal("8.00"), new BigDecimal("10.00"),
                new BigDecimal("15.00"), null, new BigDecimal("12.00"), new BigDecimal("14.00"),
                new BigDecimal("18.00"), new BigDecimal("16.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 28, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("93.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0546, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(19.39, Offset.offset(0.05));
    }
}
```

- [ ] **Step 11: Implementa `JacksonPollock7Calcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class JacksonPollock7Calcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.JACKSON_POLLOCK_7;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.PETTORALE, CampoPlica.ASCELLARE, CampoPlica.TRICIPITALE,
                CampoPlica.SOTTOSCAPOLARE, CampoPlica.ADDOMINALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s7 = CalcoliPlicometria.somma(pliche.pettoraleMm(), pliche.ascellareMm(), pliche.tricipitaleMm(),
                pliche.sottoscapolareMm(), pliche.addominaleMm(), pliche.soprailiacaMm(), pliche.cosciaMm());

        double eta = contesto.etaAnni();
        double d = contesto.sesso() == Sesso.M
                ? 1.112 - 0.00043499 * s7 + 0.00000055 * s7 * s7 - 0.00028826 * eta
                : 1.097 - 0.00046971 * s7 + 0.00000056 * s7 * s7 - 0.00012828 * eta;

        double percentualeGrasso = CalcoliPlicometria.percentualeGrassoSiri(d);

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s7, 2), CalcoliPlicometria.arrotonda(d, 4),
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null,
                "jackson-pollock-1978-7siti");
    }
}
```

- [ ] **Step 12: Esegui il test**

```powershell
mvn test -Dtest=JacksonPollock7CalcolatoreTest
```

Expected: PASS, 3 test verdi.

- [ ] **Step 13: Scrivi il test di `DurninWomersley4Calcolatore`**

Usa `Mockito.mock` per il repository, nessun contesto Spring necessario:

```java
package com.hexisnutrition.backend.pazienti;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DurninWomersley4CalcolatoreTest {

    @Test
    void plicheRichiesteSonoLeQuattroStandard() {
        DurninWomersley4Calcolatore calcolatore = new DurninWomersley4Calcolatore(mock(DurninWomersleyCoefficienteRepository.class));

        assertThat(calcolatore.plicheRichieste(Sesso.M)).containsExactlyInAnyOrder(
                CampoPlica.BICIPITALE, CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA);
    }

    @Test
    void calcolaDensitaEPercentualeGrassoConCoefficientiTrovati() {
        DurninWomersleyCoefficienteRepository repository = mock(DurninWomersleyCoefficienteRepository.class);
        DurninWomersleyCoefficiente coefficiente = new DurninWomersleyCoefficiente(
                Sesso.M, 20, 29, new BigDecimal("1.1631"), new BigDecimal("0.0632"));
        when(repository.trovaCandidati(Sesso.M, 25)).thenReturn(List.of(coefficiente));
        DurninWomersley4Calcolatore calcolatore = new DurninWomersley4Calcolatore(repository);

        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("15.00"), new BigDecimal("8.00"),
                new BigDecimal("14.00"), new BigDecimal("12.00"), null, null, null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 25, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("49.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0563, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(18.63, Offset.offset(0.05));
        assertThat(risultato.coefficienteC()).isEqualByComparingTo("1.1631");
        assertThat(risultato.coefficienteM()).isEqualByComparingTo("0.0632");
    }

    @Test
    void lanciaEccezioneSeNessunCoefficienteApplicabile() {
        DurninWomersleyCoefficienteRepository repository = mock(DurninWomersleyCoefficienteRepository.class);
        when(repository.trovaCandidati(any(), anyInt())).thenReturn(List.of());
        DurninWomersley4Calcolatore calcolatore = new DurninWomersley4Calcolatore(repository);

        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("15.00"), new BigDecimal("8.00"),
                new BigDecimal("14.00"), new BigDecimal("12.00"), null, null, null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 10, EtniaAtleta.CAUCASICO);

        assertThatThrownBy(() -> calcolatore.calcola(pliche, contesto))
                .isInstanceOf(CoefficientiDurninMancantiException.class);
    }
}
```

- [ ] **Step 14: Crea `CoefficientiDurninMancantiException`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CoefficientiDurninMancantiException extends RuntimeException {
    public CoefficientiDurninMancantiException(String messaggio) {
        super(messaggio);
    }
}
```

- [ ] **Step 15: Implementa `DurninWomersley4Calcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class DurninWomersley4Calcolatore implements CalcolatorePlicometria {

    private final DurninWomersleyCoefficienteRepository coefficienti;

    public DurninWomersley4Calcolatore(DurninWomersleyCoefficienteRepository coefficienti) {
        this.coefficienti = coefficienti;
    }

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.DURNIN_WOMERSLEY_4;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.BICIPITALE, CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s4 = CalcoliPlicometria.somma(pliche.bicipitaleMm(), pliche.tricipitaleMm(),
                pliche.sottoscapolareMm(), pliche.soprailiacaMm());

        DurninWomersleyCoefficiente coefficiente = coefficienti.trovaCandidati(contesto.sesso(), contesto.etaAnni())
                .stream()
                .findFirst()
                .orElseThrow(() -> new CoefficientiDurninMancantiException(
                        "Nessun coefficiente Durnin-Womersley per sesso " + contesto.sesso()
                                + " ed età " + contesto.etaAnni()));

        double d = coefficiente.getC().doubleValue() - coefficiente.getM().doubleValue() * Math.log10(s4);
        double percentualeGrasso = CalcoliPlicometria.percentualeGrassoSiri(d);

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s4, 2), CalcoliPlicometria.arrotonda(d, 4),
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2),
                coefficiente.getC(), coefficiente.getM(), "durnin-womersley-1974");
    }
}
```

- [ ] **Step 16: Esegui il test**

```powershell
mvn test -Dtest=DurninWomersley4CalcolatoreTest
```

Expected: PASS, 3 test verdi.

- [ ] **Step 17: Scrivi il test di `Faulkner4Calcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class Faulkner4CalcolatoreTest {

    private final Faulkner4Calcolatore calcolatore = new Faulkner4Calcolatore();

    @Test
    void plicheRichiesteSonoLeQuattroStandard() {
        assertThat(calcolatore.plicheRichieste(Sesso.M)).containsExactlyInAnyOrder(
                CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA, CampoPlica.ADDOMINALE);
    }

    @Test
    void calcolaPercentualeGrassoDirettaSenzaDensita() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), null, null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 30, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("40.00");
        assertThat(risultato.densitaCorporea()).isNull();
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("11.90");
    }
}
```

(`%BF = 40×0,153+5,783 = 6,12+5,783 = 11,903` → arrotondato a 11,90.)

- [ ] **Step 18: Implementa `Faulkner4Calcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class Faulkner4Calcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.FAULKNER_4;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA, CampoPlica.ADDOMINALE);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s4 = CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.sottoscapolareMm(),
                pliche.soprailiacaMm(), pliche.addominaleMm());

        double percentualeGrasso = s4 * 0.153 + 5.783;

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s4, 2), null,
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null, "faulkner-1968");
    }
}
```

- [ ] **Step 19: Esegui il test**

```powershell
mvn test -Dtest=Faulkner4CalcolatoreTest
```

Expected: PASS, 2 test verdi.

- [ ] **Step 20: Scrivi il test di `SlaughterPediatricoCalcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SlaughterPediatricoCalcolatoreTest {

    private final SlaughterPediatricoCalcolatore calcolatore = new SlaughterPediatricoCalcolatore();

    @Test
    void plicheRichiesteSonoTricipitaleEPolpaccio() {
        assertThat(calcolatore.plicheRichieste(Sesso.M))
                .containsExactlyInAnyOrder(CampoPlica.TRICIPITALE, CampoPlica.POLPACCIO);
    }

    @Test
    void maschioSottoI35MmUsaLaFormulaLineare() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null, null, null, null, null,
                new BigDecimal("10.00"));
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 12, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("20.00");
        assertThat(risultato.densitaCorporea()).isNull();
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("15.70");
    }

    @Test
    void femminaSopraI35MmUsaLaFormulaQuadratica() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("20.00"), null, null, null, null, null,
                new BigDecimal("20.00"));
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 14, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("40.00");
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("32.20");
    }

    @Test
    void maschioEsattamenteA35MmUsaLaFormulaQuadratica() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("17.50"), null, null, null, null, null,
                new BigDecimal("17.50"));
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 15, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("35.00");
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("30.85");
    }
}
```

Valori attesi: M, S2=20<35 → 0,735×20+1,0=15,70. F, S2=40≥35 → 1,21×40−0,008×1600−3,4=48,4−12,8−3,4=32,20. M, S2=35 (bordo, usa il ramo ≥35) → 1,21×35−0,008×1225−1,7=42,35−9,8−1,7=30,85.

- [ ] **Step 21: Implementa `SlaughterPediatricoCalcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class SlaughterPediatricoCalcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.SLAUGHTER_PEDIATRICO;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.POLPACCIO);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double s2 = CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.polpaccioMm());

        double percentualeGrasso;
        if (contesto.sesso() == Sesso.M) {
            percentualeGrasso = s2 < 35
                    ? 0.735 * s2 + 1.0
                    : 1.21 * s2 - 0.008 * s2 * s2 - 1.7;
        } else {
            percentualeGrasso = s2 < 35
                    ? 0.610 * s2 + 5.1
                    : 1.21 * s2 - 0.008 * s2 * s2 - 3.4;
        }

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(s2, 2), null,
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null, "slaughter-1988");
    }
}
```

- [ ] **Step 22: Esegui il test**

```powershell
mvn test -Dtest=SlaughterPediatricoCalcolatoreTest
```

Expected: PASS, 4 test verdi.

- [ ] **Step 23: Scrivi il test di `EvansAtletiCalcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvansAtletiCalcolatoreTest {

    private final EvansAtletiCalcolatore calcolatore = new EvansAtletiCalcolatore();

    @Test
    void plicheRichiesteSonoTricipitaleAddominaleCoscia() {
        assertThat(calcolatore.plicheRichieste(Sesso.M))
                .containsExactlyInAnyOrder(CampoPlica.TRICIPITALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA);
    }

    @Test
    void uomoCaucasico() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null, null, null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 24, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("30.00");
        assertThat(risultato.densitaCorporea()).isNull();
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("10.05");
    }

    @Test
    void donnaAfroamericana() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null, null, null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 24, EtniaAtleta.AFROAMERICANO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("14.40");
    }
}
```

Valori attesi: `3SKF=30`. Uomo caucasico: `8,997+0,24658×30−6,343×1−1,998×0 = 8,997+7,3974−6,343 = 10,0514` → 10,05. Donna afroamericana: `8,997+7,3974−6,343×0−1,998×1 = 16,3944−1,998 = 14,3964` → 14,40.

- [ ] **Step 24: Implementa `EvansAtletiCalcolatore`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class EvansAtletiCalcolatore implements CalcolatorePlicometria {

    @Override
    public ProtocolloPlicometrico protocollo() {
        return ProtocolloPlicometrico.EVANS_ATLETI;
    }

    @Override
    public Set<CampoPlica> plicheRichieste(Sesso sesso) {
        return EnumSet.of(CampoPlica.TRICIPITALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA);
    }

    @Override
    public RisultatoDensita calcola(PlicheInput pliche, ContestoPlicometria contesto) {
        double skf3 = CalcoliPlicometria.somma(pliche.tricipitaleMm(), pliche.addominaleMm(), pliche.cosciaMm());

        double sesso = contesto.sesso() == Sesso.M ? 1 : 0;
        double etnia = contesto.etniaAtleta() == EtniaAtleta.AFROAMERICANO ? 1 : 0;

        double percentualeGrasso = 8.997 + 0.24658 * skf3 - 6.343 * sesso - 1.998 * etnia;

        return new RisultatoDensita(
                CalcoliPlicometria.arrotonda(skf3, 2), null,
                CalcoliPlicometria.arrotonda(percentualeGrasso, 2), null, null, "evans-2005");
    }
}
```

- [ ] **Step 25: Esegui il test**

```powershell
mvn test -Dtest=EvansAtletiCalcolatoreTest
```

Expected: PASS, 3 test verdi.

- [ ] **Step 26: Esegui l'intera suite backend**

```powershell
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 27: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CampoPlica.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicheInput.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/ContestoPlicometria.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/RisultatoDensita.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CalcolatorePlicometria.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CalcoliPlicometria.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CoefficientiDurninMancantiException.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/JacksonPollock3Calcolatore.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/JacksonPollock7Calcolatore.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/DurninWomersley4Calcolatore.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/Faulkner4Calcolatore.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/SlaughterPediatricoCalcolatore.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/EvansAtletiCalcolatore.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/CalcoliPlicometriaTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/JacksonPollock3CalcolatoreTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/JacksonPollock7CalcolatoreTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/DurninWomersley4CalcolatoreTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/Faulkner4CalcolatoreTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/SlaughterPediatricoCalcolatoreTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/EvansAtletiCalcolatoreTest.java
```

---

## Task 6: `PlicometriaService` — validazione, orchestrazione, persistenza (backend)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicheMancantiException.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaNonDisponibilePerSessoException.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaRequest.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaService.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: `List<CalcolatorePlicometria>` (Task 5, iniettato da Spring — 6 `@Component` trovati automaticamente), `PlicometriaRepository` (Task 4), `Paziente`/`Sesso` (Task 1), `Visita`/`VisitaCalcoli.applicaMamc` (Task 2/3).
- Produces: `PlicometriaService.elabora(Paziente paziente, Visita visita, PlicometriaRequest request): void` — chiamato da `PazienteService.crea()`.

- [ ] **Step 1: Crea le eccezioni**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PlicheMancantiException extends RuntimeException {
    public PlicheMancantiException(String messaggio) {
        super(messaggio);
    }
}
```

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PlicometriaNonDisponibilePerSessoException extends RuntimeException {
    public PlicometriaNonDisponibilePerSessoException() {
        super("La plicometria non è disponibile per sesso ALTRO: le equazioni richiedono M o F.");
    }
}
```

- [ ] **Step 2: Crea `PlicometriaRequest`**

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlicometriaRequest(
        ProtocolloPlicometrico protocollo,
        EtniaAtleta etniaAtleta,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaPettoraleMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaAscellareMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaTricipitaleMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaBicipitaleMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaSottoscapolareMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaSoprailiacaMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaAddominaleMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaCosciaMm,
        @Positive @Digits(integer = 2, fraction = 2) @DecimalMax("100") BigDecimal plicaPolpaccioMm
) {
}
```

- [ ] **Step 3: Aggiungi il campo `plicometria` a `VisitaRequest`**

Aggiungi come ultimo parametro del record scritto nel Task 2:

```java
public record VisitaRequest(
        LocalDate dataVisita,
        @NotNull @Positive @Max(300) Integer altezzaCm,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal pesoKg,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaVitaCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaFianchiCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaAddomeCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBraccioRilassatoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCosciaCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaPolpaccioCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaColloCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaToraceCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBraccioContrattoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaAvambraccioCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCavigliaCm,
        ProtocolloVita protocolloVita,
        @Valid PlicometriaRequest plicometria
) {
}
```

Aggiungi `import jakarta.validation.Valid;`.

- [ ] **Step 4: Implementa `PlicometriaService`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PlicometriaService {

    private final List<CalcolatorePlicometria> calcolatori;
    private final PlicometriaRepository plicometriaRepository;

    public PlicometriaService(List<CalcolatorePlicometria> calcolatori, PlicometriaRepository plicometriaRepository) {
        this.calcolatori = calcolatori;
        this.plicometriaRepository = plicometriaRepository;
    }

    public void elabora(Paziente paziente, Visita visita, PlicometriaRequest request) {
        if (request == null || request.protocollo() == null) {
            return;
        }
        if (paziente.getSesso() == Sesso.ALTRO) {
            throw new PlicometriaNonDisponibilePerSessoException();
        }

        CalcolatorePlicometria calcolatore = calcolatori.stream()
                .filter(c -> c.protocollo() == request.protocollo())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nessun calcolatore per " + request.protocollo()));

        Map<CampoPlica, BigDecimal> valori = valoriPerCampo(request);
        for (CampoPlica campo : calcolatore.plicheRichieste(paziente.getSesso())) {
            if (valori.get(campo) == null) {
                throw new PlicheMancantiException(
                        "Plica " + campo + " obbligatoria per il protocollo " + request.protocollo());
            }
        }

        int etaAnni = Period.between(paziente.getDataNascita(), visita.getDataVisita()).getYears();
        EtniaAtleta etnia = request.etniaAtleta() != null ? request.etniaAtleta() : EtniaAtleta.CAUCASICO;
        ContestoPlicometria contesto = new ContestoPlicometria(paziente.getSesso(), etaAnni, etnia);

        PlicheInput pliche = new PlicheInput(request.plicaPettoraleMm(), request.plicaAscellareMm(),
                request.plicaTricipitaleMm(), request.plicaBicipitaleMm(), request.plicaSottoscapolareMm(),
                request.plicaSoprailiacaMm(), request.plicaAddominaleMm(), request.plicaCosciaMm(),
                request.plicaPolpaccioMm());

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        double percentualeGrassoConLimite = CalcoliPlicometria.applicaLimiteSicurezza(
                risultato.percentualeGrasso().doubleValue(), paziente.getSesso());
        BigDecimal percentualeGrasso = CalcoliPlicometria.arrotonda(percentualeGrassoConLimite, 2);

        BigDecimal massaGrassaKg = visita.getPesoKg().multiply(percentualeGrasso)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal massaMagraKg = visita.getPesoKg().subtract(massaGrassaKg);

        BigDecimal altezzaM = BigDecimal.valueOf(visita.getAltezzaCm()).divide(BigDecimal.valueOf(100));
        BigDecimal altezzaM2 = altezzaM.multiply(altezzaM);
        BigDecimal fmi = massaGrassaKg.divide(altezzaM2, 2, RoundingMode.HALF_UP);
        BigDecimal ffmi = massaMagraKg.divide(altezzaM2, 2, RoundingMode.HALF_UP);

        Plicometria plicometria = new Plicometria(visita.getId(), request.protocollo(), risultato.formulaVersione(),
                etaAnni, risultato.coefficienteC(), risultato.coefficienteM(),
                request.protocollo() == ProtocolloPlicometrico.EVANS_ATLETI ? etnia : null,
                request.plicaPettoraleMm(), request.plicaAscellareMm(), request.plicaTricipitaleMm(),
                request.plicaBicipitaleMm(), request.plicaSottoscapolareMm(), request.plicaSoprailiacaMm(),
                request.plicaAddominaleMm(), request.plicaCosciaMm(), request.plicaPolpaccioMm(),
                risultato.sommaPlicheMm(), risultato.densitaCorporea(), percentualeGrasso,
                massaGrassaKg, massaMagraKg, fmi, ffmi);

        plicometriaRepository.save(plicometria);

        if (request.plicaTricipitaleMm() != null) {
            VisitaCalcoli.applicaMamc(visita, request.plicaTricipitaleMm());
        }
    }

    private Map<CampoPlica, BigDecimal> valoriPerCampo(PlicometriaRequest request) {
        Map<CampoPlica, BigDecimal> valori = new EnumMap<>(CampoPlica.class);
        valori.put(CampoPlica.PETTORALE, request.plicaPettoraleMm());
        valori.put(CampoPlica.ASCELLARE, request.plicaAscellareMm());
        valori.put(CampoPlica.TRICIPITALE, request.plicaTricipitaleMm());
        valori.put(CampoPlica.BICIPITALE, request.plicaBicipitaleMm());
        valori.put(CampoPlica.SOTTOSCAPOLARE, request.plicaSottoscapolareMm());
        valori.put(CampoPlica.SOPRAILIACA, request.plicaSoprailiacaMm());
        valori.put(CampoPlica.ADDOMINALE, request.plicaAddominaleMm());
        valori.put(CampoPlica.COSCIA, request.plicaCosciaMm());
        valori.put(CampoPlica.POLPACCIO, request.plicaPolpaccioMm());
        return valori;
    }
}
```

`CalcoliPlicometria` (Task 5) è package-private (`final class` senza modificatore `public`, metodi `static` senza modificatore): siccome `PlicometriaService` vive nello stesso package `com.hexisnutrition.backend.pazienti`, può chiamare `CalcoliPlicometria.applicaLimiteSicurezza(...)` e `CalcoliPlicometria.arrotonda(...)` direttamente, come nel codice sopra. L'import `java.math.RoundingMode` nel file resta usato dalle chiamate a `divide(...)` con scala esplicita più sotto nello stesso metodo.

- [ ] **Step 5: Wiring in `PazienteService`**

Aggiungi il campo e il parametro del costruttore:

```java
    private final PlicometriaService plicometriaService;
```

nel costruttore, aggiungi il parametro `PlicometriaService plicometriaService` e `this.plicometriaService = plicometriaService;`.

Poi, nel blocco scritto nei Task 2/3, dopo `visitaRepository.save(visita);` aggiungi la chiamata al servizio — **l'ordine è importante**: `visita.getId()` deve essere valorizzato (disponibile solo dopo il salvataggio) prima di creare la riga `Plicometria` che referenzia `visita_id`; la successiva mutazione di `visita` (MAMC) dentro `elabora()` viene tracciata dal dirty-checking di JPA sulla stessa entità gestita nella stessa transazione, senza bisogno di un secondo salvataggio esplicito:

```java
        VisitaCalcoli.applica(visita);
        visitaRepository.save(visita);
        plicometriaService.elabora(paziente, visita, v.plicometria());

        return paziente;
```

- [ ] **Step 6: Aggiungi i test di integrazione in `PazienteControllerTest.java`**

```java
    @Test
    void creaPazienteConPlicometriaJackson3CalcolaEPersisteIRisultati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-jp3@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-jp3@example.com",
                                 "dataNascita":"2001-01-01","sesso":"M",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":180,"pesoKg":80.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaPettoraleMm":10.00,"plicaAddominaleMm":10.00,"plicaCosciaMm":10.00}}}
                                """))
                .andExpect(status().isCreated());

        List<Plicometria> plicometrie = plicometriaRepository.findAll();
        assertThat(plicometrie).hasSize(1);
        Plicometria plicometria = plicometrie.get(0);
        assertThat(plicometria.getEtaAnni()).isEqualTo(25);
        assertThat(plicometria.getPercentualeGrasso().doubleValue()).isCloseTo(8.51, org.assertj.core.data.Offset.offset(0.1));
        assertThat(plicometria.getMassaGrassaKg().doubleValue()).isCloseTo(6.81, org.assertj.core.data.Offset.offset(0.1));
        assertThat(plicometria.getMassaMagraKg().doubleValue()).isCloseTo(73.19, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void creaPazienteConPlicometriaESessoAltroRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-altro@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-altro@example.com",
                                 "dataNascita":"1990-05-20","sesso":"ALTRO",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,
                                 "plicometria":{"protocollo":"FAULKNER_4",
                                 "plicaTricipitaleMm":10.00,"plicaSottoscapolareMm":10.00,
                                 "plicaSoprailiacaMm":10.00,"plicaAddominaleMm":10.00}}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConProtocolloJackson3EPlicaMancanteRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-mancante@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-mancante@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3","plicaPettoraleMm":10.00}}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConPlicaTricipitaleEBraccioRilassatoCalcolaMamc() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-mamc@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-mamc@example.com",
                                 "dataNascita":"2001-01-01","sesso":"F",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":165,"pesoKg":65.00,
                                 "circonferenzaBraccioRilassatoCm":32.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaTricipitaleMm":16.00,"plicaSoprailiacaMm":10.00,"plicaCosciaMm":15.00}}}
                                """))
                .andExpect(status().isCreated());

        Visita visita = visitaRepository.findAll().get(0);
        assertThat(visita.getMamcCm()).isEqualByComparingTo("26.97");
    }

    @Test
    void creaPazienteSenzaBraccioRilassatoNonCalcolaMamcAncheConPlicaTricipitale() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-mamc-senza-braccio@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-mamc-senza-braccio@example.com",
                                 "dataNascita":"2001-01-01","sesso":"F",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":165,"pesoKg":65.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaTricipitaleMm":16.00,"plicaSoprailiacaMm":10.00,"plicaCosciaMm":15.00}}}
                                """))
                .andExpect(status().isCreated());

        Visita visita = visitaRepository.findAll().get(0);
        assertThat(visita.getMamcCm()).isNull();
    }

    @Test
    void creaPazienteConDurninWomersleyEEtaSottoLaFasciaMinimaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-durnin-eta@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-durnin-eta@example.com",
                                 "dataNascita":"2020-01-01","sesso":"M",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":120,"pesoKg":25.0,
                                 "plicometria":{"protocollo":"DURNIN_WOMERSLEY_4",
                                 "plicaBicipitaleMm":8.00,"plicaTricipitaleMm":15.00,
                                 "plicaSottoscapolareMm":14.00,"plicaSoprailiacaMm":12.00}}}
                                """))
                .andExpect(status().isBadRequest());
    }
```

Aggiungi `@Autowired private PlicometriaRepository plicometriaRepository;` tra gli altri `@Autowired` del file, e in `pulisci()` (metodo `@AfterEach`) aggiungi `plicometriaRepository.deleteAll();` **prima** di `visitaRepository.deleteAll();` (la FK `visita_id` blocca la cancellazione della visita se la riga plicometria non è già stata rimossa).

- [ ] **Step 7: Esegui l'intera suite backend**

```powershell
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicheMancantiException.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaNonDisponibilePerSessoException.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaRequest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PlicometriaService.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Task 7: Sesso a 3 valori obbligatorio (frontend)

**Files:**
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.vue`
- Modify: `frontend-professionisti/src/utils/validators.ts`
- Modify: `frontend-professionisti/src/api/pazienti.ts`
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.spec.ts`

**Interfaces:**
- Produces: `erroreSesso(valore: string): string | undefined` in `validators.ts`, usato anche dal Task 9 (nessun dipendente diretto in questo task).

- [ ] **Step 1: Aggiungi `erroreSesso` a `validators.ts`**

Vicino a `erroreDataNascita`:

```ts
export function erroreSesso(valore: string): string | undefined {
    if (!valore) return 'Il sesso è obbligatorio.'
    return undefined
}
```

- [ ] **Step 2: Aggiorna i tipi in `api/pazienti.ts`**

```ts
export interface Paziente {
  id: string
  nome: string
  cognome: string
  email: string
  telefono: string | null
  dataNascita: string | null
  sesso: string
  lavoro: string | null
  tipoLavoro: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO' | null
  statoAccount: 'MAI_INVITATO' | 'INVITATO' | 'ATTIVO'
}
```

e in `CreaPazienteRequest`:

```ts
export interface CreaPazienteRequest {
  nome: string
  cognome: string
  email: string
  telefono?: string
  dataNascita?: string
  sesso: 'M' | 'F' | 'ALTRO'
  lavoro?: string
  tipoLavoro?: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO'
  visita: CreaVisitaRequest
}
```

- [ ] **Step 3: Aggiorna `PazienteNuovoView.vue`**

Nel `<script setup>`, importa `erroreSesso` insieme agli altri validatori e aggiungi la validazione:

```ts
  erroreDataNascita,
  erroreSesso,
```

in `validaCampi()`, dopo `assegna('dataNascita', erroreDataNascita(dataNascita.value))`:

```ts
  assegna('sesso', erroreSesso(sesso.value))
```

Nel submit, cambia `sesso: sesso.value || undefined,` in `sesso: sesso.value as 'M' | 'F' | 'ALTRO',` (validato prima del submit, non può essere vuoto a questo punto).

Nel template, aggiorna il blocco Sesso:

```html
          <div class="flex flex-col gap-1.5">
            <Label for="sesso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Sesso*</Label>
            <Select v-model="sesso">
              <SelectTrigger id="sesso" class="w-full" :aria-invalid="!!errori.sesso">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="M">Maschio</SelectItem>
                <SelectItem value="F">Femmina</SelectItem>
                <SelectItem value="ALTRO">Altro</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errori.sesso" class="text-xs font-medium text-(--danger)">{{ errori.sesso }}</p>
          </div>
```

- [ ] **Step 4: Aggiorna `PazienteNuovoView.spec.ts` — mock e import**

Aggiungi l'import in cima al file:

```ts
import { Select, SelectTrigger } from '@/components/ui/select'
```

Aggiungi l'helper accanto a `selezionaDataNascita`:

```ts
async function selezionaSelect(wrapper: ReturnType<typeof mount>, triggerId: string, valore: string) {
  const select = wrapper.findAllComponents(Select).find((s) => s.findComponent(SelectTrigger).props('id') === triggerId)
  await select?.vm.$emit('update:modelValue', valore)
  await wrapper.vm.$nextTick()
}
```

Nei due mock di risposta (righe con `sesso: null`), cambia in `sesso: 'M'`:

```ts
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
```

(due occorrenze, una per test — usa `replace_all` se identiche).

- [ ] **Step 5: Aggiorna i due test di submit esistenti**

In `'crea il paziente con i dati anagrafici e della visita, poi naviga al suo dettaglio'`, dopo `await selezionaDataNascita(wrapper, '1990-05-20')` aggiungi `await selezionaSelect(wrapper, 'sesso', 'M')`, e nell'assert del payload cambia `sesso: undefined,` in `sesso: 'M',`.

In `'invia tutte le 14 misurazioni della visita con i valori corretti nei rispettivi campi'`, stesso trattamento: aggiungi la selezione del sesso e cambia `sesso: undefined,` in `sesso: 'M',` nell'assert (il resto del payload circonferenze non cambia in questo task — verrà aggiornato nel Task 8).

- [ ] **Step 6: Aggiorna il test degli errori obbligatori**

In `'non invia la richiesta e mostra gli errori sotto i campi obbligatori vuoti'`, aggiungi dopo l'ultima riga `expect`:

```ts
    expect(wrapper.text()).toContain('Il sesso è obbligatorio.')
```

- [ ] **Step 7: Aggiungi un test dedicato per il sesso obbligatorio**

Dopo il test `'non invia la richiesta se manca la data di nascita...'`:

```ts
  it('non invia la richiesta se manca il sesso, e l\'errore sparisce non appena viene selezionato', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Mario')
    await wrapper.find('#cognome').setValue('Rossi')
    await wrapper.find('#email').setValue('mario@example.com')
    await selezionaDataNascita(wrapper, '1990-05-20')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Il sesso è obbligatorio.')

    await selezionaSelect(wrapper, 'sesso', 'ALTRO')

    expect(wrapper.text()).not.toContain('Il sesso è obbligatorio.')
  })
```

- [ ] **Step 8: Esegui la suite frontend**

```powershell
npm run test
npx tsc --noEmit
```

Expected: tutti i test verdi, `tsc` pulito.

- [ ] **Step 9: Stage**

```bash
git add frontend-professionisti/src/views/PazienteNuovoView.vue
git add frontend-professionisti/src/utils/validators.ts
git add frontend-professionisti/src/api/pazienti.ts
git add frontend-professionisti/src/views/PazienteNuovoView.spec.ts
```

---

## Task 8: Redesign circonferenze (frontend)

**Files:**
- Modify: `frontend-professionisti/src/components/pazienti/DatiVisitaForm.vue`
- Modify: `frontend-professionisti/src/api/pazienti.ts`
- Modify: `frontend-professionisti/src/components/pazienti/DatiVisitaForm.spec.ts`
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.spec.ts`

**Interfaces:**
- Produces: `CreaVisitaRequest` con i nuovi campi circonferenza + `protocolloVita`; `DatiVisitaForm.ottieniDati()` li restituisce. Il Task 9 aggiunge il campo `plicometria` a questa stessa interfaccia e al valore restituito da `ottieniDati()`.

- [ ] **Step 1: Aggiorna `CreaVisitaRequest` in `api/pazienti.ts`**

```ts
export interface CreaVisitaRequest {
  dataVisita?: string
  altezzaCm: number
  pesoKg: number
  circonferenzaVitaCm?: number
  circonferenzaFianchiCm?: number
  circonferenzaAddomeCm?: number
  circonferenzaBraccioRilassatoCm?: number
  circonferenzaCosciaCm?: number
  circonferenzaPolpaccioCm?: number
  circonferenzaColloCm?: number
  circonferenzaToraceCm?: number
  circonferenzaBraccioContrattoCm?: number
  circonferenzaAvambraccioCm?: number
  circonferenzaCavigliaCm?: number
  protocolloVita?: 'OMS' | 'OMBELICALE' | 'ALTRO'
}
```

- [ ] **Step 2: Riscrivi `DatiVisitaForm.vue`**

File completo (la sezione Plicometria e la prop `sesso` arrivano nel Task 9 — non aggiungerle ora):

```vue
<script setup lang="ts">
import { nextTick, ref, type Ref } from 'vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { DatePicker } from '@/components/ui/date-picker'
import { Ruler } from '@lucide/vue'
import type { CreaVisitaRequest } from '@/api/pazienti'

import {
  numeroItaliano,
  numeroItalianoOpzionale,
  filtraSoloCifre,
  filtraDecimaleItaliano,
  erroreAltezza,
  errorePeso,
  erroreCirconferenza,
} from '@/utils/validators'

const dataVisita = ref(new Date().toISOString().slice(0, 10))
const altezzaCm = ref('')
const pesoKg = ref('')
const circonferenzaVita = ref('')
const circonferenzaFianchi = ref('')
const circonferenzaAddome = ref('')
const circonferenzaBraccioRilassato = ref('')
const circonferenzaCoscia = ref('')
const circonferenzaPolpaccio = ref('')
const circonferenzaCollo = ref('')
const circonferenzaTorace = ref('')
const circonferenzaBraccioContratto = ref('')
const circonferenzaAvambraccio = ref('')
const circonferenzaCaviglia = ref('')
const protocolloVita = ref<'OMS' | 'OMBELICALE' | 'ALTRO'>('OMS')

const errori = ref<Record<string, string>>({})
const accordionAperto = ref('')

// --- GESTIONE FILTRI SU INPUT (LOGICA VUE UI) ---
const MARCATORE_INVISIBILE = '​'

function pulisciErroreSeCorretto(chiave: string, valida: (valore: string) => string | undefined, valore: string) {
  if (errori.value[chiave] && !valida(valore)) {
    const nuovi = { ...errori.value }
    delete nuovi[chiave]
    errori.value = nuovi
  }
}

function conFiltro(
  rif: Ref<string>,
  filtro: (valore: string) => string,
  chiave?: string,
  valida?: (valore: string) => string | undefined,
) {
  return async (valore: string | number) => {
    const filtrato = filtro(String(valore))
    if (filtrato === rif.value) {
      rif.value = `${filtrato}${MARCATORE_INVISIBILE}`
      await nextTick()
    }
    rif.value = filtrato
    if (chiave && valida) pulisciErroreSeCorretto(chiave, valida, filtrato)
  }
}

const onAltezzaInput = conFiltro(altezzaCm, (v) => filtraSoloCifre(v, 3), 'altezzaCm', erroreAltezza)
const onPesoInput = conFiltro(pesoKg, filtraDecimaleItaliano, 'pesoKg', errorePeso)

const creaHandlerCirconferenza = (rif: Ref<string>, chiave: string) =>
  conFiltro(rif, filtraDecimaleItaliano, chiave, erroreCirconferenza)

const onCirconferenzaVitaInput = creaHandlerCirconferenza(circonferenzaVita, 'circonferenzaVita')
const onCirconferenzaFianchiInput = creaHandlerCirconferenza(circonferenzaFianchi, 'circonferenzaFianchi')
const onCirconferenzaAddomeInput = creaHandlerCirconferenza(circonferenzaAddome, 'circonferenzaAddome')
const onCirconferenzaBraccioRilassatoInput = creaHandlerCirconferenza(circonferenzaBraccioRilassato, 'circonferenzaBraccioRilassato')
const onCirconferenzaCosciaInput = creaHandlerCirconferenza(circonferenzaCoscia, 'circonferenzaCoscia')
const onCirconferenzaPolpaccioInput = creaHandlerCirconferenza(circonferenzaPolpaccio, 'circonferenzaPolpaccio')
const onCirconferenzaColloInput = creaHandlerCirconferenza(circonferenzaCollo, 'circonferenzaCollo')
const onCirconferenzaToraceInput = creaHandlerCirconferenza(circonferenzaTorace, 'circonferenzaTorace')
const onCirconferenzaBraccioContrattoInput = creaHandlerCirconferenza(circonferenzaBraccioContratto, 'circonferenzaBraccioContratto')
const onCirconferenzaAvambraccioInput = creaHandlerCirconferenza(circonferenzaAvambraccio, 'circonferenzaAvambraccio')
const onCirconferenzaCavigliaInput = creaHandlerCirconferenza(circonferenzaCaviglia, 'circonferenzaCaviglia')

// --- VALIDAZIONE ---
function valida(): boolean {
  const nuoviErrori: Record<string, string> = {}

  const assegna = (chiave: string, messaggio: string | undefined) => {
    if (messaggio) nuoviErrori[chiave] = messaggio
  }

  assegna('altezzaCm', erroreAltezza(altezzaCm.value))
  assegna('pesoKg', errorePeso(pesoKg.value))

  const circonferenze: Array<[string, string]> = [
    ['circonferenzaVita', circonferenzaVita.value],
    ['circonferenzaFianchi', circonferenzaFianchi.value],
    ['circonferenzaAddome', circonferenzaAddome.value],
    ['circonferenzaBraccioRilassato', circonferenzaBraccioRilassato.value],
    ['circonferenzaCoscia', circonferenzaCoscia.value],
    ['circonferenzaPolpaccio', circonferenzaPolpaccio.value],
    ['circonferenzaCollo', circonferenzaCollo.value],
    ['circonferenzaTorace', circonferenzaTorace.value],
    ['circonferenzaBraccioContratto', circonferenzaBraccioContratto.value],
    ['circonferenzaAvambraccio', circonferenzaAvambraccio.value],
    ['circonferenzaCaviglia', circonferenzaCaviglia.value],
  ]
  for (const [chiave, valore] of circonferenze) {
    assegna(chiave, erroreCirconferenza(valore))
  }

  errori.value = nuoviErrori

  if (circonferenze.some(([chiave]) => nuoviErrori[chiave])) {
    accordionAperto.value = 'circonferenze'
  }

  return Object.keys(nuoviErrori).length === 0
}

function ottieniDati(): CreaVisitaRequest {
  return {
    dataVisita: dataVisita.value || undefined,
    altezzaCm: numeroItaliano(altezzaCm.value),
    pesoKg: numeroItaliano(pesoKg.value),
    circonferenzaVitaCm: numeroItalianoOpzionale(circonferenzaVita.value),
    circonferenzaFianchiCm: numeroItalianoOpzionale(circonferenzaFianchi.value),
    circonferenzaAddomeCm: numeroItalianoOpzionale(circonferenzaAddome.value),
    circonferenzaBraccioRilassatoCm: numeroItalianoOpzionale(circonferenzaBraccioRilassato.value),
    circonferenzaCosciaCm: numeroItalianoOpzionale(circonferenzaCoscia.value),
    circonferenzaPolpaccioCm: numeroItalianoOpzionale(circonferenzaPolpaccio.value),
    circonferenzaColloCm: numeroItalianoOpzionale(circonferenzaCollo.value),
    circonferenzaToraceCm: numeroItalianoOpzionale(circonferenzaTorace.value),
    circonferenzaBraccioContrattoCm: numeroItalianoOpzionale(circonferenzaBraccioContratto.value),
    circonferenzaAvambraccioCm: numeroItalianoOpzionale(circonferenzaAvambraccio.value),
    circonferenzaCavigliaCm: numeroItalianoOpzionale(circonferenzaCaviglia.value),
    protocolloVita: protocolloVita.value,
  }
}

defineExpose({
  valida,
  ottieniDati,
})
</script>

<template>
  <div>
    <h2 class="font-heading text-xl italic text-(--fg)">Dati della visita</h2>

    <div class="mt-5 grid gap-5 sm:grid-cols-2">
      <div class="flex flex-col gap-1.5">
        <Label for="data-visita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Data visita</Label>
        <DatePicker id="data-visita" v-model="dataVisita" />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="altezza" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Altezza (cm)*</Label>
        <Input id="altezza" :model-value="altezzaCm" @update:model-value="onAltezzaInput" type="text" inputmode="numeric" :aria-invalid="!!errori.altezzaCm" placeholder="Es. 178" />
        <p v-if="errori.altezzaCm" class="text-xs font-medium text-(--danger)">{{ errori.altezzaCm }}</p>
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="peso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Peso (kg)*</Label>
        <Input id="peso" :model-value="pesoKg" @update:model-value="onPesoInput" type="text" inputmode="decimal" :aria-invalid="!!errori.pesoKg" placeholder="Es. 78,50" />
        <p v-if="errori.pesoKg" class="text-xs font-medium text-(--danger)">{{ errori.pesoKg }}</p>
      </div>
    </div>

    <div class="mt-6 border-t border-(--bd) pt-5">
      <h3 class="text-sm font-bold uppercase tracking-wide text-(--fg3)">Misurazione BIA</h3>
      <p class="mt-1.5 text-sm text-(--fg3)">Sarà disponibile a breve.</p>
    </div>

    <Accordion v-model="accordionAperto" type="single" collapsible class="mt-6">
      <AccordionItem value="circonferenze" class="overflow-hidden rounded-xl border border-(--bd)">
        <AccordionTrigger class="group px-4 py-3.5 hover:no-underline sm:px-5">
          <div class="flex items-center gap-3">
            <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-(--mint) text-(--green)">
              <Ruler :size="18" />
            </div>

            <div class="flex flex-col items-start">
              <span class="text-sm font-bold text-(--fg)">Circonferenze</span>
              <span class="mt-0.5 text-xs text-(--fg3)">Misure corporee in cm</span>
            </div>
          </div>
        </AccordionTrigger>
        <AccordionContent>
          <div class="mx-2 border-t-2 border-t-(--bd)"></div>
          <div class="grid gap-5 py-4 px-6 sm:grid-cols-2">
            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-vita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza vita</Label>
              <Input id="circonferenza-vita" :model-value="circonferenzaVita" @update:model-value="onCirconferenzaVitaInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaVita" />
              <p v-if="errori.circonferenzaVita" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaVita }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="protocollo-vita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Protocollo vita</Label>
              <Select v-model="protocolloVita">
                <SelectTrigger id="protocollo-vita" class="w-full">
                  <SelectValue placeholder="OMS" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="OMS">OMS</SelectItem>
                  <SelectItem value="OMBELICALE">Ombelicale</SelectItem>
                  <SelectItem value="ALTRO">Altro</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-fianchi" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza fianchi</Label>
              <Input id="circonferenza-fianchi" :model-value="circonferenzaFianchi" @update:model-value="onCirconferenzaFianchiInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaFianchi" />
              <p v-if="errori.circonferenzaFianchi" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaFianchi }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-addome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza addome</Label>
              <Input id="circonferenza-addome" :model-value="circonferenzaAddome" @update:model-value="onCirconferenzaAddomeInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaAddome" />
              <p v-if="errori.circonferenzaAddome" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaAddome }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-braccio-rilassato" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio rilassato</Label>
              <Input id="circonferenza-braccio-rilassato" :model-value="circonferenzaBraccioRilassato" @update:model-value="onCirconferenzaBraccioRilassatoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioRilassato" />
              <p v-if="errori.circonferenzaBraccioRilassato" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioRilassato }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-coscia" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza coscia</Label>
              <Input id="circonferenza-coscia" :model-value="circonferenzaCoscia" @update:model-value="onCirconferenzaCosciaInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCoscia" />
              <p v-if="errori.circonferenzaCoscia" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCoscia }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-polpaccio" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza polpaccio</Label>
              <Input id="circonferenza-polpaccio" :model-value="circonferenzaPolpaccio" @update:model-value="onCirconferenzaPolpaccioInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaPolpaccio" />
              <p v-if="errori.circonferenzaPolpaccio" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaPolpaccio }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-collo" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza collo</Label>
              <Input id="circonferenza-collo" :model-value="circonferenzaCollo" @update:model-value="onCirconferenzaColloInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCollo" />
              <p v-if="errori.circonferenzaCollo" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCollo }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-torace" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza torace</Label>
              <Input id="circonferenza-torace" :model-value="circonferenzaTorace" @update:model-value="onCirconferenzaToraceInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaTorace" />
              <p v-if="errori.circonferenzaTorace" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaTorace }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-braccio-contratto" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio contratto</Label>
              <Input id="circonferenza-braccio-contratto" :model-value="circonferenzaBraccioContratto" @update:model-value="onCirconferenzaBraccioContrattoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioContratto" />
              <p v-if="errori.circonferenzaBraccioContratto" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioContratto }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-avambraccio" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza avambraccio</Label>
              <Input id="circonferenza-avambraccio" :model-value="circonferenzaAvambraccio" @update:model-value="onCirconferenzaAvambraccioInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaAvambraccio" />
              <p v-if="errori.circonferenzaAvambraccio" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaAvambraccio }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-caviglia" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza caviglia</Label>
              <Input id="circonferenza-caviglia" :model-value="circonferenzaCaviglia" @update:model-value="onCirconferenzaCavigliaInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCaviglia" />
              <p v-if="errori.circonferenzaCaviglia" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCaviglia }}</p>
            </div>
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  </div>
</template>
```

- [ ] **Step 3: Riscrivi `DatiVisitaForm.spec.ts`**

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DatiVisitaForm from './DatiVisitaForm.vue'

function oggiIso(): string {
  return new Date().toISOString().slice(0, 10)
}

interface DatiVisitaFormExposed {
  valida(): boolean
  ottieniDati(): Record<string, unknown>
}

function esposti(wrapper: ReturnType<typeof mount>): DatiVisitaFormExposed {
  return wrapper.vm as unknown as DatiVisitaFormExposed
}

describe('DatiVisitaForm', () => {
  it('valida() restituisce false e mostra gli errori se altezza e peso sono vuoti', async () => {
    const wrapper = mount(DatiVisitaForm)

    const valido = esposti(wrapper).valida()
    await wrapper.vm.$nextTick()

    expect(valido).toBe(false)
    expect(wrapper.text()).toContain("L'altezza è obbligatoria.")
    expect(wrapper.text()).toContain('Il peso è obbligatorio.')
  })

  it('ottieniDati() restituisce il payload della visita con la data di oggi e OMS di default', async () => {
    const wrapper = mount(DatiVisitaForm)

    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')

    expect(esposti(wrapper).valida()).toBe(true)
    expect(esposti(wrapper).ottieniDati()).toEqual({
      dataVisita: oggiIso(),
      altezzaCm: 178,
      pesoKg: 82.5,
      circonferenzaVitaCm: undefined,
      circonferenzaFianchiCm: undefined,
      circonferenzaAddomeCm: undefined,
      circonferenzaBraccioRilassatoCm: undefined,
      circonferenzaCosciaCm: undefined,
      circonferenzaPolpaccioCm: undefined,
      circonferenzaColloCm: undefined,
      circonferenzaToraceCm: undefined,
      circonferenzaBraccioContrattoCm: undefined,
      circonferenzaAvambraccioCm: undefined,
      circonferenzaCavigliaCm: undefined,
      protocolloVita: 'OMS',
    })
  })

  it('ottieniDati() include le misurazioni compilate nell\'accordion circonferenze', async () => {
    const wrapper = mount(DatiVisitaForm)

    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')

    const accordionCirconferenze = wrapper.findAll('button').find((b) => b.text().includes('Circonferenze'))
    await accordionCirconferenze?.trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#circonferenza-vita').setValue('90,10')
    await wrapper.find('#circonferenza-caviglia').setValue('22,90')

    const dati = esposti(wrapper).ottieniDati()
    expect(dati.circonferenzaVitaCm).toBe(90.1)
    expect(dati.circonferenzaCavigliaCm).toBe(22.9)
  })

  it('mostra un errore di formato per una circonferenza non valida', async () => {
    const wrapper = mount(DatiVisitaForm)

    const accordionCirconferenze = wrapper.findAll('button').find((b) => b.text().includes('Circonferenze'))
    await accordionCirconferenze?.trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#circonferenza-vita').setValue(',50')
    esposti(wrapper).valida()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Inserisci un numero valido (es. 95,50).')
  })
})
```

- [ ] **Step 4: Riscrivi il test "14 misurazioni" in `PazienteNuovoView.spec.ts`**

Rinomina il test in `'invia tutte le circonferenze della visita con i valori corretti nei rispettivi campi'` e sostituisci il blocco di compilazione/assert:

```ts
    await wrapper.find('#circonferenza-vita').setValue('90,10')
    await wrapper.find('#circonferenza-fianchi').setValue('91,2')
    await wrapper.find('#circonferenza-addome').setValue('92,3')
    await wrapper.find('#circonferenza-braccio-rilassato').setValue('93,4')
    await wrapper.find('#circonferenza-coscia').setValue('94,5')
    await wrapper.find('#circonferenza-polpaccio').setValue('95,6')
    await wrapper.find('#circonferenza-collo').setValue('96,7')
    await wrapper.find('#circonferenza-torace').setValue('97,8')
    await wrapper.find('#circonferenza-braccio-contratto').setValue('98,9')
    await wrapper.find('#circonferenza-avambraccio').setValue('99,0')
    await wrapper.find('#circonferenza-caviglia').setValue('100,1')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: undefined, dataNascita: '1990-05-20', sesso: 'M', lavoro: undefined, tipoLavoro: undefined,
      visita: {
        dataVisita: oggiIso(), altezzaCm: 178, pesoKg: 82.5,
        circonferenzaVitaCm: 90.1, circonferenzaFianchiCm: 91.2, circonferenzaAddomeCm: 92.3,
        circonferenzaBraccioRilassatoCm: 93.4, circonferenzaCosciaCm: 94.5, circonferenzaPolpaccioCm: 95.6,
        circonferenzaColloCm: 96.7, circonferenzaToraceCm: 97.8, circonferenzaBraccioContrattoCm: 98.9,
        circonferenzaAvambraccioCm: 99.0, circonferenzaCavigliaCm: 100.1, protocolloVita: 'OMS',
      },
    })
```

- [ ] **Step 5: Esegui la suite frontend**

```powershell
npm run test
npx tsc --noEmit
```

Expected: tutti i test verdi, `tsc` pulito.

- [ ] **Step 6: Stage**

```bash
git add frontend-professionisti/src/components/pazienti/DatiVisitaForm.vue
git add frontend-professionisti/src/api/pazienti.ts
git add frontend-professionisti/src/components/pazienti/DatiVisitaForm.spec.ts
git add frontend-professionisti/src/views/PazienteNuovoView.spec.ts
```

---

## Task 9: Plicometria — `PlicaInput.vue`, `PlicometriaForm.vue` e wiring (frontend)

**Files:**
- Create: `frontend-professionisti/src/components/pazienti/PlicaInput.vue`
- Create: `frontend-professionisti/src/components/pazienti/PlicaInput.spec.ts`
- Create: `frontend-professionisti/src/components/pazienti/PlicometriaForm.vue`
- Create: `frontend-professionisti/src/components/pazienti/PlicometriaForm.spec.ts`
- Modify: `frontend-professionisti/src/api/pazienti.ts`
- Modify: `frontend-professionisti/src/components/pazienti/DatiVisitaForm.vue`
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.vue`
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.spec.ts`

**Interfaces:**
- Consumes: `Checkbox` (`@/components/ui/checkbox`), `Select`/`SelectTrigger` (`@/components/ui/select`), `numeroItalianoOpzionale`/`filtraDecimaleItaliano`/`erroreCirconferenza` (`validators.ts`).
- Produces: `PlicaInput` — prop `id`/`label`/`modelValue`/`errore?`, evento `update:modelValue`. `PlicometriaForm` — prop `sesso: string`, espone `valida(): boolean` e `ottieniDati(): CreaPlicometriaRequest | undefined` via `defineExpose`. `DatiVisitaForm` guadagna la prop `sesso: string` e integra `PlicometriaForm` in `valida()`/`ottieniDati()`.

- [ ] **Step 1: Aggiungi `CreaPlicometriaRequest` a `api/pazienti.ts`**

```ts
export interface CreaPlicometriaRequest {
  protocollo: 'JACKSON_POLLOCK_3' | 'JACKSON_POLLOCK_7' | 'DURNIN_WOMERSLEY_4' | 'FAULKNER_4' | 'SLAUGHTER_PEDIATRICO' | 'EVANS_ATLETI'
  etniaAtleta?: 'CAUCASICO' | 'AFROAMERICANO'
  plicaPettoraleMm?: number
  plicaAscellareMm?: number
  plicaTricipitaleMm?: number
  plicaBicipitaleMm?: number
  plicaSottoscapolareMm?: number
  plicaSoprailiacaMm?: number
  plicaAddominaleMm?: number
  plicaCosciaMm?: number
  plicaPolpaccioMm?: number
}
```

E aggiungi il campo a `CreaVisitaRequest` (scritta nel Task 8), come ultimo campo:

```ts
  protocolloVita?: 'OMS' | 'OMBELICALE' | 'ALTRO'
  plicometria?: CreaPlicometriaRequest
}
```

- [ ] **Step 2: Crea `PlicaInput.vue`**

```vue
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { filtraDecimaleItaliano, numeroItalianoOpzionale } from '@/utils/validators'

const props = defineProps<{
  id: string
  label: string
  modelValue: string
  errore?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [valore: string]
}>()

const tripla = ref(false)
const misura1 = ref('')
const misura2 = ref('')
const misura3 = ref('')

const mediaCalcolata = computed(() => {
  const n1 = numeroItalianoOpzionale(misura1.value)
  const n2 = numeroItalianoOpzionale(misura2.value)
  const n3 = numeroItalianoOpzionale(misura3.value)
  if (n1 === undefined || n2 === undefined || n3 === undefined) return null
  return (n1 + n2 + n3) / 3
})

watch(mediaCalcolata, (media) => {
  if (media !== null) emit('update:modelValue', media.toFixed(2).replace('.', ','))
})

function onSingoloInput(valore: string | number) {
  emit('update:modelValue', filtraDecimaleItaliano(String(valore)))
}

function onMisura1Input(valore: string | number) {
  misura1.value = filtraDecimaleItaliano(String(valore))
}

function onMisura2Input(valore: string | number) {
  misura2.value = filtraDecimaleItaliano(String(valore))
}

function onMisura3Input(valore: string | number) {
  misura3.value = filtraDecimaleItaliano(String(valore))
}

function onTriplaChange(valore: boolean) {
  tripla.value = valore
  if (!valore) {
    misura1.value = ''
    misura2.value = ''
    misura3.value = ''
  } else {
    emit('update:modelValue', '')
  }
}
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <div class="flex items-center justify-between gap-2">
      <Label :for="id" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">{{ label }} (mm)</Label>
      <label class="flex items-center gap-1.5 text-xs text-(--fg3)">
        <Checkbox :id="`${id}-tripla`" :model-value="tripla" @update:model-value="onTriplaChange" />
        Tripla misurazione
      </label>
    </div>

    <Input v-if="!tripla" :id="id" :model-value="modelValue" @update:model-value="onSingoloInput" type="text" inputmode="decimal" placeholder="Es. 12,50" :aria-invalid="!!errore" />

    <div v-else class="grid grid-cols-3 gap-2">
      <Input :id="`${id}-m1`" :model-value="misura1" @update:model-value="onMisura1Input" type="text" inputmode="decimal" placeholder="Misura 1" />
      <Input :id="`${id}-m2`" :model-value="misura2" @update:model-value="onMisura2Input" type="text" inputmode="decimal" placeholder="Misura 2" />
      <Input :id="`${id}-m3`" :model-value="misura3" @update:model-value="onMisura3Input" type="text" inputmode="decimal" placeholder="Misura 3" />
    </div>
    <p v-if="tripla && mediaCalcolata !== null" class="text-xs text-(--fg3)">Media calcolata: {{ mediaCalcolata.toFixed(2) }} mm</p>
    <p v-if="errore" class="text-xs font-medium text-(--danger)">{{ errore }}</p>
  </div>
</template>
```

- [ ] **Step 3: Scrivi `PlicaInput.spec.ts`**

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PlicaInput from './PlicaInput.vue'
import { Checkbox } from '@/components/ui/checkbox'

async function attivaTripla(wrapper: ReturnType<typeof mount>, attiva: boolean) {
  await wrapper.findComponent(Checkbox).vm.$emit('update:modelValue', attiva)
  await wrapper.vm.$nextTick()
}

describe('PlicaInput', () => {
  it('in modalità singola emette il valore filtrato al variare dell\'input', async () => {
    const wrapper = mount(PlicaInput, { props: { id: 'plica-test', label: 'Plica test', modelValue: '' } })

    await wrapper.find('#plica-test').setValue('12,50')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['12,50'])
  })

  it('attivando la tripla misurazione mostra 3 campi e calcola la media', async () => {
    const wrapper = mount(PlicaInput, { props: { id: 'plica-test', label: 'Plica test', modelValue: '' } })

    await attivaTripla(wrapper, true)
    await wrapper.find('#plica-test-m1').setValue('12,5')
    await wrapper.find('#plica-test-m2').setValue('13,0')
    await wrapper.find('#plica-test-m3').setValue('12,2')

    expect(wrapper.text()).toContain('Media calcolata: 12.57 mm')
    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['12,57'])
  })

  it('disattivando la tripla misurazione svuota le sotto-misurazioni', async () => {
    const wrapper = mount(PlicaInput, { props: { id: 'plica-test', label: 'Plica test', modelValue: '' } })

    await attivaTripla(wrapper, true)
    await wrapper.find('#plica-test-m1').setValue('12,5')
    await attivaTripla(wrapper, false)
    await attivaTripla(wrapper, true)

    expect((wrapper.find('#plica-test-m1').element as HTMLInputElement).value).toBe('')
  })
})
```

(Valori 12,5 / 13,0 / 12,2 → media 12,57 mm: stesso esempio del terzo PDF di specifica.)

- [ ] **Step 4: Crea `PlicometriaForm.vue`**

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import PlicaInput from './PlicaInput.vue'
import { numeroItalianoOpzionale, erroreCirconferenza } from '@/utils/validators'
import type { CreaPlicometriaRequest } from '@/api/pazienti'

const props = defineProps<{ sesso: string }>()

type Protocollo = '' | 'JACKSON_POLLOCK_3' | 'JACKSON_POLLOCK_7' | 'DURNIN_WOMERSLEY_4' | 'FAULKNER_4' | 'SLAUGHTER_PEDIATRICO' | 'EVANS_ATLETI'
type Campo = 'pettorale' | 'ascellare' | 'tricipitale' | 'bicipitale' | 'sottoscapolare' | 'soprailiaca' | 'addominale' | 'coscia' | 'polpaccio'

const protocollo = ref<Protocollo>('')
const etniaAtleta = ref<'CAUCASICO' | 'AFROAMERICANO'>('CAUCASICO')

const plicaPettorale = ref('')
const plicaAscellare = ref('')
const plicaTricipitale = ref('')
const plicaBicipitale = ref('')
const plicaSottoscapolare = ref('')
const plicaSoprailiaca = ref('')
const plicaAddominale = ref('')
const plicaCoscia = ref('')
const plicaPolpaccio = ref('')

const valoriPerCampo: Record<Campo, ReturnType<typeof ref<string>>> = {
  pettorale: plicaPettorale,
  ascellare: plicaAscellare,
  tricipitale: plicaTricipitale,
  bicipitale: plicaBicipitale,
  sottoscapolare: plicaSottoscapolare,
  soprailiaca: plicaSoprailiaca,
  addominale: plicaAddominale,
  coscia: plicaCoscia,
  polpaccio: plicaPolpaccio,
}

const campiPerProtocollo: Record<Exclude<Protocollo, ''>, Campo[]> = {
  JACKSON_POLLOCK_3: [],
  JACKSON_POLLOCK_7: ['pettorale', 'ascellare', 'tricipitale', 'sottoscapolare', 'addominale', 'soprailiaca', 'coscia'],
  DURNIN_WOMERSLEY_4: ['bicipitale', 'tricipitale', 'sottoscapolare', 'soprailiaca'],
  FAULKNER_4: ['tricipitale', 'sottoscapolare', 'soprailiaca', 'addominale'],
  SLAUGHTER_PEDIATRICO: ['tricipitale', 'polpaccio'],
  EVANS_ATLETI: ['tricipitale', 'addominale', 'coscia'],
}

const etichette: Record<Campo, string> = {
  pettorale: 'Plica pettorale',
  ascellare: 'Plica ascellare media',
  tricipitale: 'Plica tricipitale',
  bicipitale: 'Plica bicipitale',
  sottoscapolare: 'Plica sottoscapolare',
  soprailiaca: 'Plica soprailiaca',
  addominale: 'Plica addominale',
  coscia: 'Plica coscia anteriore',
  polpaccio: 'Plica polpaccio mediale',
}

const campiVisibili = computed<Campo[]>(() => {
  if (!protocollo.value) return []
  if (protocollo.value === 'JACKSON_POLLOCK_3') {
    return props.sesso === 'F' ? ['tricipitale', 'soprailiaca', 'coscia'] : ['pettorale', 'addominale', 'coscia']
  }
  return campiPerProtocollo[protocollo.value]
})

const disabilitato = computed(() => props.sesso === 'ALTRO')

const errori = ref<Record<string, string>>({})

function valida(): boolean {
  if (disabilitato.value || !protocollo.value) {
    errori.value = {}
    return true
  }
  const nuoviErrori: Record<string, string> = {}
  for (const campo of campiVisibili.value) {
    const valore = valoriPerCampo[campo].value
    if (!valore.trim()) {
      nuoviErrori[campo] = 'Questa plica è obbligatoria per il protocollo scelto.'
      continue
    }
    const messaggio = erroreCirconferenza(valore)
    if (messaggio) nuoviErrori[campo] = messaggio
  }
  errori.value = nuoviErrori
  return Object.keys(nuoviErrori).length === 0
}

function ottieniDati(): CreaPlicometriaRequest | undefined {
  if (disabilitato.value || !protocollo.value) return undefined
  return {
    protocollo: protocollo.value,
    etniaAtleta: protocollo.value === 'EVANS_ATLETI' ? etniaAtleta.value : undefined,
    plicaPettoraleMm: numeroItalianoOpzionale(plicaPettorale.value),
    plicaAscellareMm: numeroItalianoOpzionale(plicaAscellare.value),
    plicaTricipitaleMm: numeroItalianoOpzionale(plicaTricipitale.value),
    plicaBicipitaleMm: numeroItalianoOpzionale(plicaBicipitale.value),
    plicaSottoscapolareMm: numeroItalianoOpzionale(plicaSottoscapolare.value),
    plicaSoprailiacaMm: numeroItalianoOpzionale(plicaSoprailiaca.value),
    plicaAddominaleMm: numeroItalianoOpzionale(plicaAddominale.value),
    plicaCosciaMm: numeroItalianoOpzionale(plicaCoscia.value),
    plicaPolpaccioMm: numeroItalianoOpzionale(plicaPolpaccio.value),
  }
}

defineExpose({ valida, ottieniDati })
</script>

<template>
  <div class="mt-6 border-t border-(--bd) pt-5">
    <h3 class="text-sm font-bold uppercase tracking-wide text-(--fg3)">Plicometria</h3>

    <p v-if="disabilitato" class="mt-1.5 text-sm text-(--fg3)">
      Non disponibile per sesso "Altro": le equazioni plicometriche richiedono Maschio o Femmina.
    </p>

    <template v-else>
      <div class="mt-3 grid gap-5 sm:grid-cols-2">
        <div class="flex flex-col gap-1.5">
          <Label for="protocollo-plico" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Protocollo</Label>
          <Select v-model="protocollo">
            <SelectTrigger id="protocollo-plico" class="w-full">
              <SelectValue placeholder="Nessuno" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="JACKSON_POLLOCK_3">Jackson-Pollock 3 pliche</SelectItem>
              <SelectItem value="JACKSON_POLLOCK_7">Jackson-Pollock 7 pliche</SelectItem>
              <SelectItem value="DURNIN_WOMERSLEY_4">Durnin-Womersley 4 pliche</SelectItem>
              <SelectItem value="FAULKNER_4">Faulkner 4 pliche</SelectItem>
              <SelectItem value="SLAUGHTER_PEDIATRICO">Slaughter (pediatrico)</SelectItem>
              <SelectItem value="EVANS_ATLETI">Evans (atleti)</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div v-if="protocollo === 'EVANS_ATLETI'" class="flex flex-col gap-1.5">
          <Label for="etnia-atleta" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Etnia</Label>
          <Select v-model="etniaAtleta">
            <SelectTrigger id="etnia-atleta" class="w-full">
              <SelectValue placeholder="Caucasico" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="CAUCASICO">Caucasico</SelectItem>
              <SelectItem value="AFROAMERICANO">Afroamericano</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div v-if="campiVisibili.length" class="mt-5 grid gap-5 sm:grid-cols-2">
        <PlicaInput
          v-for="campo in campiVisibili"
          :key="campo"
          :id="`plica-${campo}`"
          :label="etichette[campo]"
          v-model="valoriPerCampo[campo].value"
          :errore="errori[campo]"
        />
      </div>
    </template>
  </div>
</template>
```

- [ ] **Step 5: Scrivi `PlicometriaForm.spec.ts`**

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PlicometriaForm from './PlicometriaForm.vue'
import { Select, SelectTrigger } from '@/components/ui/select'

interface PlicometriaFormExposed {
  valida(): boolean
  ottieniDati(): Record<string, unknown> | undefined
}

function esposti(wrapper: ReturnType<typeof mount>): PlicometriaFormExposed {
  return wrapper.vm as unknown as PlicometriaFormExposed
}

async function selezionaSelect(wrapper: ReturnType<typeof mount>, triggerId: string, valore: string) {
  const select = wrapper.findAllComponents(Select).find((s) => s.findComponent(SelectTrigger).attributes('id') === triggerId)
  await select?.vm.$emit('update:modelValue', valore)
  await wrapper.vm.$nextTick()
}

describe('PlicometriaForm', () => {
  it('è disabilitato per sesso ALTRO: valida() torna true e ottieniDati() torna undefined', () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'ALTRO' } })

    expect(wrapper.text()).toContain('Non disponibile per sesso "Altro"')
    expect(esposti(wrapper).valida()).toBe(true)
    expect(esposti(wrapper).ottieniDati()).toBeUndefined()
  })

  it('senza protocollo selezionato, ottieniDati() torna undefined', () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    expect(esposti(wrapper).ottieniDati()).toBeUndefined()
  })

  it('Jackson-Pollock 3 mostra pettorale/addominale/coscia per sesso M', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'JACKSON_POLLOCK_3')

    expect(wrapper.find('#plica-pettorale').exists()).toBe(true)
    expect(wrapper.find('#plica-addominale').exists()).toBe(true)
    expect(wrapper.find('#plica-coscia').exists()).toBe(true)
    expect(wrapper.find('#plica-tricipitale').exists()).toBe(false)
  })

  it('Jackson-Pollock 3 mostra tricipitale/soprailiaca/coscia per sesso F', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'F' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'JACKSON_POLLOCK_3')

    expect(wrapper.find('#plica-tricipitale').exists()).toBe(true)
    expect(wrapper.find('#plica-soprailiaca').exists()).toBe(true)
    expect(wrapper.find('#plica-coscia').exists()).toBe(true)
    expect(wrapper.find('#plica-pettorale').exists()).toBe(false)
  })

  it('Evans mostra anche il campo etnia', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'EVANS_ATLETI')

    expect(wrapper.find('#etnia-atleta').exists()).toBe(true)
  })

  it('valida() fallisce e mostra un errore se manca una plica obbligatoria', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'FAULKNER_4')
    await wrapper.find('#plica-tricipitale').setValue('10,00')

    const valido = esposti(wrapper).valida()
    await wrapper.vm.$nextTick()

    expect(valido).toBe(false)
    expect(wrapper.text()).toContain('Questa plica è obbligatoria per il protocollo scelto.')
  })

  it('ottieniDati() restituisce protocollo, etnia e pliche compilate', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'EVANS_ATLETI')
    await selezionaSelect(wrapper, 'etnia-atleta', 'AFROAMERICANO')
    await wrapper.find('#plica-tricipitale').setValue('10,00')
    await wrapper.find('#plica-addominale').setValue('10,00')
    await wrapper.find('#plica-coscia').setValue('10,00')

    expect(esposti(wrapper).ottieniDati()).toMatchObject({
      protocollo: 'EVANS_ATLETI',
      etniaAtleta: 'AFROAMERICANO',
      plicaTricipitaleMm: 10,
      plicaAddominaleMm: 10,
      plicaCosciaMm: 10,
    })
  })
})
```

- [ ] **Step 6: Esegui i test dei nuovi componenti**

```powershell
npx vitest run PlicaInput PlicometriaForm
```

Expected: PASS, 3 + 7 test verdi.

- [ ] **Step 7: Aggiungi la prop `sesso` e integra `PlicometriaForm` in `DatiVisitaForm.vue`**

Nel `<script setup>`, aggiungi in cima (dopo gli import):

```ts
import PlicometriaForm from './PlicometriaForm.vue'

const props = defineProps<{ sesso: string }>()
```

Aggiungi il ref accanto agli altri stati:

```ts
const plicometriaForm = ref<InstanceType<typeof PlicometriaForm>>()
```

In `valida()`, sostituisci la riga `return Object.keys(nuoviErrori).length === 0` con:

```ts
  const plicometriaValida = plicometriaForm.value?.valida() ?? true

  return Object.keys(nuoviErrori).length === 0 && plicometriaValida
```

In `ottieniDati()`, aggiungi come ultima proprietà dell'oggetto restituito:

```ts
    protocolloVita: protocolloVita.value,
    plicometria: plicometriaForm.value?.ottieniDati(),
```

(sostituisce la singola riga `protocolloVita: protocolloVita.value,` scritta nel Task 8.)

Nel template, subito dopo il blocco "Misurazione BIA" e prima dell'`Accordion` delle circonferenze:

```html
    <PlicometriaForm ref="plicometriaForm" :sesso="props.sesso" />
```

- [ ] **Step 8: Passa `sesso` da `PazienteNuovoView.vue`**

Cambia:

```html
      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <DatiVisitaForm ref="datiVisitaForm" :sesso="sesso" />
      </div>
```

- [ ] **Step 9: Aggiungi un test end-to-end del wiring in `PazienteNuovoView.spec.ts`**

Aggiungi l'helper `selezionaSelect` già scritto nel Task 7 (già presente nel file), poi:

```ts
  it('include la plicometria nel payload quando protocollo e pliche sono compilati', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '44', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await selezionaDataNascita(wrapper, '1990-05-20')
    await selezionaSelect(wrapper, 'sesso', 'M')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await selezionaSelect(wrapper, 'protocollo-plico', 'FAULKNER_4')
    await wrapper.find('#plica-tricipitale').setValue('10,00')
    await wrapper.find('#plica-sottoscapolare').setValue('10,00')
    await wrapper.find('#plica-soprailiaca').setValue('10,00')
    await wrapper.find('#plica-addominale').setValue('10,00')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalled()
    const richiesta = vi.mocked(pazientiApi.crea).mock.calls[0][0]
    expect(richiesta.visita.plicometria).toMatchObject({
      protocollo: 'FAULKNER_4',
      plicaTricipitaleMm: 10,
      plicaSottoscapolareMm: 10,
      plicaSoprailiacaMm: 10,
      plicaAddominaleMm: 10,
    })
  })
```

- [ ] **Step 10: Esegui l'intera suite frontend**

```powershell
npm run test
npx tsc --noEmit
```

Expected: tutti i test verdi, `tsc` pulito.

- [ ] **Step 11: Stage**

```bash
git add frontend-professionisti/src/components/pazienti/PlicaInput.vue
git add frontend-professionisti/src/components/pazienti/PlicaInput.spec.ts
git add frontend-professionisti/src/components/pazienti/PlicometriaForm.vue
git add frontend-professionisti/src/components/pazienti/PlicometriaForm.spec.ts
git add frontend-professionisti/src/api/pazienti.ts
git add frontend-professionisti/src/components/pazienti/DatiVisitaForm.vue
git add frontend-professionisti/src/views/PazienteNuovoView.vue
git add frontend-professionisti/src/views/PazienteNuovoView.spec.ts
```

---

## Task 10: Aggiornamento wiki (`modello-dati.md`, `api-contracts.md`)

**Files:**
- Modify: `wiki/modello-dati.md`
- Modify: `wiki/api-contracts.md`

**Interfaces:** nessuna — solo documentazione, richiesta esplicitamente da `backend/CLAUDE.md` ("Se aggiungi o modifichi un endpoint o il modello dati, aggiorna nello stesso passaggio `../wiki/api-contracts.md` o `../wiki/modello-dati.md`"). `wiki/stato.md` e `wiki/log.md` restano fuori da questo task: si aggiornano a fine sessione con l'operazione di handoff descritta nel `CLAUDE.md` di radice del workspace, non task per task.

- [ ] **Step 1: Aggiorna il paragrafo `Paziente` in `modello-dati.md`**

Nel punto elenco `**Paziente** (V2, esteso in V6, V8): ...`, dopo `sesso` aggiungi `(V9, obbligatorio: M/F/ALTRO)` e aggiorna la frase sull'assenza di validazione se necessario. Il testo aggiornato diventa:

```
- **Paziente** (V2, esteso in V6, V8, V9): nome, cognome, email, telefono, data di nascita (**obbligatoria da V8**, 2026-09-01), sesso (**obbligatorio da V9**, 2026-09-01, valori M/F/ALTRO), lavoro, tipo di lavoro (`SEDENTARIO`/`POCO_ATTIVO`/`ATTIVO`/`MOLTO_ATTIVO`); collegato a un `Professionista`; stato account (`MAI_INVITATO` / `INVITATO` / `ATTIVO`). Non richiede un account attivo per esistere. Email univoca solo tra account `ATTIVO` (indice parziale), non a livello di anagrafica. L'altezza **non** è più qui: è stata spostata su `Visita` (V5), storicizzata per ogni visita anziché unica per paziente. Nessun campo età: si calcola da `dataNascita` al volo (frontend), mai persistito, per non disallinearsi nel tempo — scelta di Andrea il 2026-09-01.
```

- [ ] **Step 2: Aggiorna il paragrafo `Visita` in `modello-dati.md`**

Sostituisci il testo esistente con:

```
- **Visita** (V5, estesa in V7, V10): data visita (scelta dal professionista, default oggi), altezza, peso, 11 circonferenze a misura singola (vita, fianchi, addome, braccio rilassato, coscia, polpaccio, collo, torace, braccio contratto, avambraccio, caviglia) + protocollo vita usato (`OMS`/`OMBELICALE`/`ALTRO`, default `OMS`); collegata a un `Paziente`. Solo altezza e peso sono obbligatori. Peso e circonferenze hanno precisione `NUMERIC(6,2)`; altezza resta intera (cm). Dal 2026-09-01 (V10) sostituisce lo schema precedente con coppie dx/sx (coscia, polpaccio, bicipite) e i campi ombelico/petto/spalle, mai popolati in produzione. Valori derivati calcolati e persistiti dal backend al salvataggio: BMI (sempre), WHR (se vita e fianchi presenti), WHtR (se vita presente), MAMC (se braccio rilassato e plica tricipitale presenti, incrocio con `Plicometria`). Creata contestualmente alla prima visita in `POST /pazienti`; visite successive (storico, endpoint dedicato) restano fuori scope — anticipa il sotto-progetto "Monitoraggio". Nessun campo per la misurazione BIA per ora (da definire).
- **Plicometria** (V11, nuova entità, 2026-09-01): 1:1 opzionale con `Visita` (creata solo se il professionista sceglie un protocollo plicometrico). Protocollo (`JACKSON_POLLOCK_3` / `JACKSON_POLLOCK_7` / `DURNIN_WOMERSLEY_4` / `FAULKNER_4` / `SLAUGHTER_PEDIATRICO` / `EVANS_ATLETI`), fino a 9 pliche in mm (già mediate se il professionista ha usato la tripla misurazione), età alla visita (calcolata da `Paziente.dataNascita` + `Visita.dataVisita`, mai un valore manuale), e i valori derivati persistiti al momento del calcolo per riproducibilità storica: somma pliche, densità corporea (nulla per i protocolli a `%BF` diretta), `%BF` (con limite di sicurezza biologico `max(3, %BF)` per M / `max(10, %BF)` per F applicato a tutti i protocolli), massa grassa/magra in kg, FMI, FFMI, versione della formula usata, e — solo per Durnin-Womersley — lo snapshot dei coefficienti `c`/`m` effettivamente applicati (non solo il riferimento). Se `Paziente.sesso = ALTRO`, la creazione è bloccata (400): tutte le equazioni richiedono M/F.
- **DurninWomersleyCoefficiente** (V12, tabella di riferimento, 2026-09-01): 10 righe seminate da migrazione (5 fasce d'età × 2 sessi, valori Durnin & Womersley 1974), non modificabile da applicativo. Lookup per (sesso, età) usata solo dal calcolatore Durnin-Womersley.
```

- [ ] **Step 3: Aggiorna `api-contracts.md`**

Nella riga della tabella per `POST /pazienti`, sostituisci la descrizione con:

```
| POST | `/pazienti` | PROFESSIONISTA | Crea anagrafica paziente **e prima visita** (dati antropometrici, circonferenze, plicometria opzionale), 201; 400 se la visita manca, se altezza/peso al suo interno sono nulli, se `dataNascita` o `sesso` sono nulli (entrambi obbligatori dal 2026-09-01), se la plicometria è richiesta con `sesso: ALTRO`, se mancano pliche obbligatorie per il protocollo scelto, o se manca una riga di coefficienti Durnin-Womersley applicabile all'età |
```

E aggiorna il paragrafo sotto la tabella:

```
`POST /pazienti` richiede un oggetto `visita` obbligatorio nel body (altezza e peso obbligatori, le 11 circonferenze sono opzionali, il blocco `plicometria` è opzionale e annidato dentro `visita`) — creato in transazione con l'anagrafica. `sesso` del paziente è obbligatorio (`M`/`F`/`ALTRO`) dal 2026-09-01; la plicometria è disponibile solo per `M`/`F`. Dettagli campo per campo in `VisitaRequest` e `PlicometriaRequest` (`backend/src/main/java/com/hexisnutrition/backend/pazienti/`).
```

- [ ] **Step 4: Stage**

```bash
git add wiki/modello-dati.md
git add wiki/api-contracts.md
```

---

## Self-review

- **Copertura spec**: sesso 3 valori + blocco plicometria (Task 1, 6) ✓; redesign circonferenze (Task 2, 8) ✓; BMI/WHR/WHtR (Task 3) ✓; MAMC (Task 3, integrato in Task 6) ✓; tabella `plicometrie` + `durnin_womersley_coefficienti` con snapshot coefficienti (Task 4) ✓; 6 calcolatori con formule esatte dai PDF (Task 5) ✓; limite di sicurezza biologico universale (Task 5, applicato in Task 6) ✓; validazione bloccante pliche mancanti/sesso ALTRO/coefficienti Durnin mancanti (Task 6) ✓; tripla misurazione con media live solo sulle pliche (Task 9) ✓; nessuna anteprima live dei valori derivati, nessuna nuova UI risultati (rispettato in tutti i task frontend) ✓; wiki (Task 10) ✓.
- **Placeholder**: nessun "TODO"/"implementa dopo" nei passi — verificato scorrendo tutti i 10 task.
- **Coerenza dei tipi**: `Sesso` (Task 1) usato identico in `Paziente`, `ContestoPlicometria`, `DurninWomersleyCoefficiente`, `PlicometriaService` — nomi verificati. `ProtocolloPlicometrico` (Task 4) ha esattamente gli stessi 6 valori usati nei calcolatori (Task 5), in `PlicometriaRequest`/`Plicometria` (Task 6) e nel `Select` frontend (Task 9). `CampoPlica` (Task 5) ha gli stessi 9 valori usati in `PlicometriaService.valoriPerCampo` (Task 6) e nelle chiavi `Campo` lato frontend (Task 9, nomi minuscoli equivalenti).
- **Ordine delle dipendenze**: rispettato — `Sesso` (T1) prima di tutto ciò che lo usa; `Visita`/`ProtocolloVita` (T2) prima di `VisitaCalcoli` (T3) e di `PlicometriaService` (T6); `Plicometria`/tabelle (T4) prima dei calcolatori (T5) e del servizio (T6); backend completo (T1-T6) prima del frontend equivalente (T7-T9), dato che il frontend consuma i contratti DTO definiti nel backend.
