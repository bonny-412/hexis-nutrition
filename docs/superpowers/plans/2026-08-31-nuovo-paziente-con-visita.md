# Nuovo paziente con anagrafica + prima visita — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Estendere la pagina "Nuovo paziente" di `frontend-professionisti` per raccogliere anagrafica completa + dati della prima visita (altezza, peso, circonferenze, placeholder BIA), introducendo una nuova entità `Visita` nel backend e un unico endpoint di creazione transazionale.

**Architecture:** Nuova tabella `visite` (FK verso `pazienti`) creata additivamente (Task 1); poi `Paziente` guadagna `lavoro`/`tipoLavoro` e perde `altezzaCm`, spostata su `Visita` (Task 2); poi `POST /pazienti` viene esteso per creare `Paziente`+`Visita` nella stessa transazione (Task 3). Sul frontend, il tipo di risposta `Paziente` viene aggiornato prima (Task 4, non rompe nulla perché `altezzaCm` non è mostrata in nessuna view), poi il form viene riscritto insieme al tipo di richiesta esteso (Task 5, coppia accoppiata). Infine la wiki di progetto viene allineata (Task 6).

**Tech Stack:** Spring Boot 3.3.4 / Java 21 / PostgreSQL + Flyway (backend); Vue 3 + TypeScript + shadcn-vue + Tailwind (frontend-professionisti).

**Spec:** [docs/superpowers/specs/2026-08-31-nuovo-paziente-con-visita-design.md](../specs/2026-08-31-nuovo-paziente-con-visita-design.md)

## Global Constraints

- **Mai `git commit`**: i commit li fa sempre e solo Andrea. Ogni task termina con `git add` (staging), mai con `git commit` — questo sostituisce lo step "Commit" del template standard della skill.
- Backend: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` prima di ogni `mvn test` (il JDK di sistema di default è Java 8). Richiede il servizio `postgresql-x64-13` attivo e il database `hexis_test`.
- Se una migrazione aggiunge una tabella, va aggiunta al `TRUNCATE` in `AbstractIntegrationTest` (`backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java`), altrimenti i dati sopravvivono tra un test e l'altro.
- Frontend: mai `style="..."` inline — sempre classi Tailwind (incluse le variabili CSS custom via sintassi arbitraria `text-[var(--fg3)]` / `text-(--fg3)`).
- Nessuna verifica manuale in browser da parte dell'agente (convenzione di progetto, sia backend sia frontend): riportare sempre l'esito reale dei comandi di test, mai una previsione.

---

### Task 1: Backend — entità `Visita` (tabella additiva)

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__create_visite.sql`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Visita.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRepository.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java`

**Interfaces:**
- Consumes: `Paziente` (esistente, 8 argomenti — **non ancora modificato in questo task**, verrà cambiato nel Task 2), `PazienteRepository` (esistente).
- Produces: `Visita` (entità JPA con getter per tutti i campi), `VisitaRepository extends JpaRepository<Visita, UUID>` con `findAllByPazienteId(UUID pazienteId): List<Visita>`. Usati dal Task 3.

- [ ] **Step 1: Scrivi il test che fallisce**

Crea `backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java`:

```java
package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisitaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private VisitaRepository visitaRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        visitaRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("visite-prof@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "visite-luca@example.com", null, null, null, null));

        Visita visita = new Visita(paziente.getId(), 178, new BigDecimal("82.5"),
                new BigDecimal("95.0"), null, new BigDecimal("102.0"), new BigDecimal("100.0"),
                new BigDecimal("58.0"), new BigDecimal("58.0"), new BigDecimal("38.0"), new BigDecimal("38.0"),
                new BigDecimal("45.0"), new BigDecimal("110.0"), new BigDecimal("32.0"), new BigDecimal("32.0"));
        visitaRepository.save(visita);

        List<Visita> visite = visitaRepository.findAllByPazienteId(paziente.getId());

        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getAltezzaCm()).isEqualTo(178);
        assertThat(visite.get(0).getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visite.get(0).getCirconferenzaOmbelicoCm()).isNull();
        assertThat(visite.get(0).getDataVisita()).isNotNull();
    }
}
```

Nota: `new Paziente(...)` usa qui il costruttore **attuale** a 8 argomenti (non ancora cambiato) — verrà aggiornato nel Task 2.

- [ ] **Step 2: Esegui il test e verifica che fallisca**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn test -Dtest=VisitaRepositoryTest -pl . 2>&1 | Select-String -Pattern "ERROR|BUILD"
```

Atteso: FAIL — `Visita`/`VisitaRepository` non esistono, errore di compilazione.

- [ ] **Step 3: Crea la migrazione Flyway**

Crea `backend/src/main/resources/db/migration/V5__create_visite.sql`:

```sql
CREATE TABLE visite (
    id UUID PRIMARY KEY,
    paziente_id UUID NOT NULL REFERENCES pazienti(id),
    data_visita DATE NOT NULL DEFAULT CURRENT_DATE,
    altezza_cm INTEGER NOT NULL,
    peso_kg NUMERIC(5,1) NOT NULL,
    circonferenza_vita_cm NUMERIC(5,1),
    circonferenza_ombelico_cm NUMERIC(5,1),
    circonferenza_fianchi_cm NUMERIC(5,1),
    circonferenza_petto_cm NUMERIC(5,1),
    circonferenza_coscia_dx_cm NUMERIC(5,1),
    circonferenza_coscia_sx_cm NUMERIC(5,1),
    circonferenza_polpaccio_dx_cm NUMERIC(5,1),
    circonferenza_polpaccio_sx_cm NUMERIC(5,1),
    larghezza_spalle_cm NUMERIC(5,1),
    circonferenza_spalle_cm NUMERIC(5,1),
    circonferenza_bicipite_dx_cm NUMERIC(5,1),
    circonferenza_bicipite_sx_cm NUMERIC(5,1),
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_visite_paziente_id ON visite(paziente_id);
```

- [ ] **Step 4: Crea l'entità `Visita`**

Crea `backend/src/main/java/com/hexisnutrition/backend/pazienti/Visita.java`:

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "circonferenza_ombelico_cm")
    private BigDecimal circonferenzaOmbelicoCm;

    @Column(name = "circonferenza_fianchi_cm")
    private BigDecimal circonferenzaFianchiCm;

    @Column(name = "circonferenza_petto_cm")
    private BigDecimal circonferenzaPettoCm;

    @Column(name = "circonferenza_coscia_dx_cm")
    private BigDecimal circonferenzaCosciaDxCm;

    @Column(name = "circonferenza_coscia_sx_cm")
    private BigDecimal circonferenzaCosciaSxCm;

    @Column(name = "circonferenza_polpaccio_dx_cm")
    private BigDecimal circonferenzaPolpaccioDxCm;

    @Column(name = "circonferenza_polpaccio_sx_cm")
    private BigDecimal circonferenzaPolpaccioSxCm;

    @Column(name = "larghezza_spalle_cm")
    private BigDecimal larghezzaSpalleCm;

    @Column(name = "circonferenza_spalle_cm")
    private BigDecimal circonferenzaSpalleCm;

    @Column(name = "circonferenza_bicipite_dx_cm")
    private BigDecimal circonferenzaBicipiteDxCm;

    @Column(name = "circonferenza_bicipite_sx_cm")
    private BigDecimal circonferenzaBicipiteSxCm;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Visita() {
    }

    public Visita(UUID pazienteId, Integer altezzaCm, BigDecimal pesoKg,
                  BigDecimal circonferenzaVitaCm, BigDecimal circonferenzaOmbelicoCm,
                  BigDecimal circonferenzaFianchiCm, BigDecimal circonferenzaPettoCm,
                  BigDecimal circonferenzaCosciaDxCm, BigDecimal circonferenzaCosciaSxCm,
                  BigDecimal circonferenzaPolpaccioDxCm, BigDecimal circonferenzaPolpaccioSxCm,
                  BigDecimal larghezzaSpalleCm, BigDecimal circonferenzaSpalleCm,
                  BigDecimal circonferenzaBicipiteDxCm, BigDecimal circonferenzaBicipiteSxCm) {
        this.pazienteId = pazienteId;
        this.altezzaCm = altezzaCm;
        this.pesoKg = pesoKg;
        this.circonferenzaVitaCm = circonferenzaVitaCm;
        this.circonferenzaOmbelicoCm = circonferenzaOmbelicoCm;
        this.circonferenzaFianchiCm = circonferenzaFianchiCm;
        this.circonferenzaPettoCm = circonferenzaPettoCm;
        this.circonferenzaCosciaDxCm = circonferenzaCosciaDxCm;
        this.circonferenzaCosciaSxCm = circonferenzaCosciaSxCm;
        this.circonferenzaPolpaccioDxCm = circonferenzaPolpaccioDxCm;
        this.circonferenzaPolpaccioSxCm = circonferenzaPolpaccioSxCm;
        this.larghezzaSpalleCm = larghezzaSpalleCm;
        this.circonferenzaSpalleCm = circonferenzaSpalleCm;
        this.circonferenzaBicipiteDxCm = circonferenzaBicipiteDxCm;
        this.circonferenzaBicipiteSxCm = circonferenzaBicipiteSxCm;
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

    public BigDecimal getCirconferenzaOmbelicoCm() {
        return circonferenzaOmbelicoCm;
    }

    public BigDecimal getCirconferenzaFianchiCm() {
        return circonferenzaFianchiCm;
    }

    public BigDecimal getCirconferenzaPettoCm() {
        return circonferenzaPettoCm;
    }

    public BigDecimal getCirconferenzaCosciaDxCm() {
        return circonferenzaCosciaDxCm;
    }

    public BigDecimal getCirconferenzaCosciaSxCm() {
        return circonferenzaCosciaSxCm;
    }

    public BigDecimal getCirconferenzaPolpaccioDxCm() {
        return circonferenzaPolpaccioDxCm;
    }

    public BigDecimal getCirconferenzaPolpaccioSxCm() {
        return circonferenzaPolpaccioSxCm;
    }

    public BigDecimal getLarghezzaSpalleCm() {
        return larghezzaSpalleCm;
    }

    public BigDecimal getCirconferenzaSpalleCm() {
        return circonferenzaSpalleCm;
    }

    public BigDecimal getCirconferenzaBicipiteDxCm() {
        return circonferenzaBicipiteDxCm;
    }

    public BigDecimal getCirconferenzaBicipiteSxCm() {
        return circonferenzaBicipiteSxCm;
    }
}
```

- [ ] **Step 5: Crea il repository**

Crea `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRepository.java`:

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VisitaRepository extends JpaRepository<Visita, UUID> {
    List<Visita> findAllByPazienteId(UUID pazienteId);
}
```

- [ ] **Step 6: Aggiungi `visite` al TRUNCATE dei test**

In `backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java`, sostituisci:

```java
        jdbcTemplate.execute("TRUNCATE TABLE token_azione, pazienti, professionisti RESTART IDENTITY CASCADE");
```

con:

```java
        jdbcTemplate.execute("TRUNCATE TABLE token_azione, visite, pazienti, professionisti RESTART IDENTITY CASCADE");
```

- [ ] **Step 7: Esegui i test e verifica che passino**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn test
```

Atteso: PASS, tutti i test verdi (nessuna regressione sugli esistenti).

- [ ] **Step 8: Metti in staging (niente commit)**

```powershell
git add backend/src/main/resources/db/migration/V5__create_visite.sql
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/Visita.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRepository.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/VisitaRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java
```

---

### Task 2: Backend — `Paziente`: `lavoro`/`tipoLavoro` al posto di `altezzaCm`

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__pazienti_lavoro_e_rimozione_altezza.sql`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/TipoLavoro.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java:39-43`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java` (11 costruttori `new Paziente(...)` + il test `creaPazienteRestituisce201`)
- Modify: `backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java` (3 costruttori `new Paziente(...)`)
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java` (2 costruttori `new Paziente(...)`)
- Modify: `backend/src/test/java/com/hexisnutrition/backend/inviti/TokenAzioneRepositoryTest.java` (1 costruttore `new Paziente(...)`)

**Interfaces:**
- Consumes: nessuna dal Task 1 (task indipendente, tocca solo `pazienti`).
- Produces: `Paziente` con costruttore a 9 argomenti `(UUID professionistaId, String nome, String cognome, String email, String telefono, LocalDate dataNascita, String sesso, String lavoro, TipoLavoro tipoLavoro)` — **rompe tutte le chiamate esistenti a 8 argomenti**, da correggere nello stesso task. `PazienteResponse` con campi `lavoro: String`, `tipoLavoro: String` al posto di `altezzaCm`. Usati dal Task 3 e dal Task 4 (frontend).

- [ ] **Step 1: Scrivi il test che fallisce**

In `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`, aggiungi (dopo `creaPazienteRestituisce201`, che verrà sostituito nel Task 3 — per ora resta invariato):

```java
    @Test
    void creaPazienteConLavoroETipoLavoroLiRestituisceNellaRisposta() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-lavoro@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-lavoro@example.com",
                                 "lavoro":"Impiegato","tipoLavoro":"ATTIVO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lavoro").value("Impiegato"))
                .andExpect(jsonPath("$.tipoLavoro").value("ATTIVO"));
    }
```

- [ ] **Step 2: Esegui il test e verifica che fallisca**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn test -Dtest=PazienteControllerTest
```

Atteso: FAIL — `$.lavoro`/`$.tipoLavoro` non esistono nella risposta (i campi vengono ignorati silenziosamente in request, non sono in `PazienteResponse`).

- [ ] **Step 3: Crea la migrazione Flyway**

Crea `backend/src/main/resources/db/migration/V6__pazienti_lavoro_e_rimozione_altezza.sql`:

```sql
ALTER TABLE pazienti ADD COLUMN lavoro VARCHAR(150);
ALTER TABLE pazienti ADD COLUMN tipo_lavoro VARCHAR(20);
ALTER TABLE pazienti DROP COLUMN altezza_cm;
```

- [ ] **Step 4: Crea l'enum `TipoLavoro`**

Crea `backend/src/main/java/com/hexisnutrition/backend/pazienti/TipoLavoro.java`:

```java
package com.hexisnutrition.backend.pazienti;

public enum TipoLavoro {
    SEDENTARIO,
    POCO_ATTIVO,
    ATTIVO,
    MOLTO_ATTIVO
}
```

- [ ] **Step 5: Aggiorna l'entità `Paziente`**

Sostituisci l'intero contenuto di `backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java` con:

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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pazienti")
public class Paziente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professionista_id", nullable = false)
    private UUID professionistaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(nullable = false)
    private String email;

    private String telefono;

    @Column(name = "data_nascita")
    private LocalDate dataNascita;

    private String sesso;

    private String lavoro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lavoro")
    private TipoLavoro tipoLavoro;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato_account", nullable = false)
    private StatoAccountPaziente statoAccount = StatoAccountPaziente.MAI_INVITATO;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Paziente() {
    }

    public Paziente(UUID professionistaId, String nome, String cognome, String email,
                     String telefono, LocalDate dataNascita, String sesso, String lavoro, TipoLavoro tipoLavoro) {
        this.professionistaId = professionistaId;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.telefono = telefono;
        this.dataNascita = dataNascita;
        this.sesso = sesso;
        this.lavoro = lavoro;
        this.tipoLavoro = tipoLavoro;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfessionistaId() {
        return professionistaId;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public String getSesso() {
        return sesso;
    }

    public String getLavoro() {
        return lavoro;
    }

    public TipoLavoro getTipoLavoro() {
        return tipoLavoro;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public StatoAccountPaziente getStatoAccount() {
        return statoAccount;
    }

    public void setStatoAccount(StatoAccountPaziente statoAccount) {
        this.statoAccount = statoAccount;
    }
}
```

- [ ] **Step 6: Aggiorna `PazienteResponse`**

Sostituisci l'intero contenuto di `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java` con:

```java
package com.hexisnutrition.backend.pazienti;

import java.time.LocalDate;
import java.util.UUID;

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
                paziente.getSesso(), paziente.getLavoro(),
                paziente.getTipoLavoro() != null ? paziente.getTipoLavoro().name() : null,
                paziente.getStatoAccount().name());
    }
}
```

- [ ] **Step 7: Aggiorna `CreaPazienteRequest`**

Sostituisci l'intero contenuto di `backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java` con:

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreaPazienteRequest(
        @NotBlank String nome,
        @NotBlank String cognome,
        @NotBlank @Email String email,
        String telefono,
        LocalDate dataNascita,
        String sesso,
        String lavoro,
        TipoLavoro tipoLavoro
) {
}
```

(Il campo `visita` viene aggiunto nel Task 3, non qui.)

- [ ] **Step 8: Aggiorna `PazienteService.crea(...)`**

In `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`, sostituisci:

```java
    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.altezzaCm());
        return pazienteRepository.save(paziente);
    }
```

con:

```java
    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.lavoro(), request.tipoLavoro());
        return pazienteRepository.save(paziente);
    }
```

- [ ] **Step 9: Correggi tutte le chiamate esistenti a `new Paziente(...)`**

Il vecchio costruttore aveva 4 argomenti finali `(telefono, dataNascita, sesso, altezzaCm)`; il nuovo ne ha 5 `(telefono, dataNascita, sesso, lavoro, tipoLavoro)`. Nella maggior parte dei test questi 4 argomenti erano tutti `null` — basta aggiungere un `null` in coda.

In **`backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`**: usa una sostituzione globale (find-and-replace su tutto il file, non solo la prima occorrenza) di

```
null, null, null, null)
```

con

```
null, null, null, null, null)
```

Ci sono **11 occorrenze** in questo file (una per ogni `new Paziente(...)`). Verifica dopo la sostituzione che siano diventate 11 anche le occorrenze di `null, null, null, null, null)`.

In **`backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java`**: stessa sostituzione globale, `null, null, null, null)` → `null, null, null, null, null)`. **3 occorrenze**.

In **`backend/src/test/java/com/hexisnutrition/backend/inviti/TokenAzioneRepositoryTest.java`**: stessa sostituzione, **1 occorrenza**.

In **`backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java`**:
- Il metodo `trovaSoloIlPazienteAttivoPerEmail` ha la stessa sostituzione globale, **1 occorrenza** di `null, null, null, null)` → `null, null, null, null, null)`.
- Il metodo `salvaERitrovaPerProfessionista` ha valori reali, non null — sostituisci:

```java
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca.verdi@example.com", "3331234567", LocalDate.of(1990, 5, 20), "M", 178);
```

con:

```java
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca.verdi@example.com", "3331234567", LocalDate.of(1990, 5, 20), "M", "Impiegato", TipoLavoro.ATTIVO);
```

- [ ] **Step 10: Aggiorna il body JSON del test `creaPazienteRestituisce201` esistente**

In `PazienteControllerTest.java`, il test `creaPazienteRestituisce201` invia ancora `"altezzaCm":178` nel JSON — Jackson lo ignorerebbe silenziosamente (nessun errore), ma va ripulito. Sostituisci il suo `content("""...""")`:

```java
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca@example.com",
                                 "telefono":"333123456","dataNascita":"1990-05-20","sesso":"M","altezzaCm":178}
                                """))
```

con:

```java
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca@example.com",
                                 "telefono":"333123456","dataNascita":"1990-05-20","sesso":"M",
                                 "lavoro":"Impiegato","tipoLavoro":"ATTIVO"}
                                """))
```

- [ ] **Step 11: Esegui i test e verifica che passino**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn test
```

Atteso: PASS, tutti i test verdi (inclusi `VisitaRepositoryTest` del Task 1 e il nuovo `creaPazienteConLavoroETipoLavoroLiRestituisceNellaRisposta`).

- [ ] **Step 12: Metti in staging (niente commit)**

```powershell
git add backend/src/main/resources/db/migration/V6__pazienti_lavoro_e_rimozione_altezza.sql
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/TipoLavoro.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
git add backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java
git add backend/src/test/java/com/hexisnutrition/backend/inviti/TokenAzioneRepositoryTest.java
```

---

### Task 3: Backend — `POST /pazienti` crea anagrafica + prima visita in transazione

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: `Visita` e `VisitaRepository` (Task 1), `Paziente`/`CreaPazienteRequest`/`PazienteResponse` a 9 campi (Task 2).
- Produces: `POST /pazienti` con `visita` obbligatoria nel payload, che crea `Paziente`+`Visita` in un'unica transazione. Nessun consumer successivo in questo piano (il frontend chiama l'endpoint via HTTP, non consuma le classi Java direttamente).

- [ ] **Step 1: Scrivi i test che falliscono**

In `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`:

1. Sostituisci l'intero test `creaPazienteRestituisce201` (nella versione aggiornata al Task 2, Step 10) con:

```java
    @Test
    void creaPazienteRestituisce201EPersisteLaPrimaVisita() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca@example.com",
                                 "telefono":"333123456","dataNascita":"1990-05-20","sesso":"M",
                                 "lavoro":"Impiegato","tipoLavoro":"ATTIVO",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,"circonferenzaVitaCm":95.0}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("luca@example.com"))
                .andExpect(jsonPath("$.lavoro").value("Impiegato"))
                .andExpect(jsonPath("$.tipoLavoro").value("ATTIVO"))
                .andExpect(jsonPath("$.statoAccount").value("MAI_INVITATO"));

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getAltezzaCm()).isEqualTo(178);
        assertThat(visite.get(0).getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visite.get(0).getCirconferenzaVitaCm()).isEqualByComparingTo("95.0");
    }

    @Test
    void creaPazienteSenzaVisitaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-senza-visita@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Luca\",\"cognome\":\"Verdi\",\"email\":\"luca-senza-visita@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConVisitaSenzaAltezzaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-visita-incompleta@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-visita-incompleta@example.com",
                                 "visita":{"pesoKg":82.5}}
                                """))
                .andExpect(status().isBadRequest());
    }
```

2. Aggiungi il campo autowired e includilo nel cleanup, subito dopo `pazienteRepository`:

```java
    @Autowired
    private VisitaRepository visitaRepository;
```

e in `pulisci()` aggiungi `visitaRepository.deleteAll();` come prima riga (prima di `tokenAzioneRepository.deleteAll();`).

3. Aggiungi in cima al file l'import mancante:

```java
import java.util.List;
```

(vicino a `import java.util.UUID;`, già presente).

- [ ] **Step 2: Esegui i test e verifica che falliscano**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn test -Dtest=PazienteControllerTest
```

Atteso: FAIL — `creaPazienteRestituisce201EPersisteLaPrimaVisita` fallisce perché nessuna `Visita` viene persistita (il campo `visita` del payload viene ignorato); `creaPazienteSenzaVisitaRestituisce400` e `creaPazienteConVisitaSenzaAltezzaRestituisce400` falliscono perché l'endpoint risponde comunque 201 (nessuna validazione su `visita`, che non esiste ancora come campo).

- [ ] **Step 3: Crea `VisitaRequest`**

Crea `backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java`:

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record VisitaRequest(
        @NotNull Integer altezzaCm,
        @NotNull BigDecimal pesoKg,
        BigDecimal circonferenzaVitaCm,
        BigDecimal circonferenzaOmbelicoCm,
        BigDecimal circonferenzaFianchiCm,
        BigDecimal circonferenzaPettoCm,
        BigDecimal circonferenzaCosciaDxCm,
        BigDecimal circonferenzaCosciaSxCm,
        BigDecimal circonferenzaPolpaccioDxCm,
        BigDecimal circonferenzaPolpaccioSxCm,
        BigDecimal larghezzaSpalleCm,
        BigDecimal circonferenzaSpalleCm,
        BigDecimal circonferenzaBicipiteDxCm,
        BigDecimal circonferenzaBicipiteSxCm
) {
}
```

- [ ] **Step 4: Aggiungi `visita` a `CreaPazienteRequest`**

Sostituisci l'intero contenuto di `backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java` con:

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreaPazienteRequest(
        @NotBlank String nome,
        @NotBlank String cognome,
        @NotBlank @Email String email,
        String telefono,
        LocalDate dataNascita,
        String sesso,
        String lavoro,
        TipoLavoro tipoLavoro,
        @NotNull @Valid VisitaRequest visita
) {
}
```

- [ ] **Step 5: Rendi `PazienteService.crea(...)` transazionale e persisti la `Visita`**

Sostituisci l'intero contenuto di `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java` con:

```java
package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.email.EmailSender;
import com.hexisnutrition.backend.inviti.EmailGiaInUsoException;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.inviti.TokenNonValidoException;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PazienteService {

    private final PazienteRepository pazienteRepository;
    private final VisitaRepository visitaRepository;
    private final ProfessionistaRepository professionistaRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    public PazienteService(PazienteRepository pazienteRepository,
                            VisitaRepository visitaRepository,
                            ProfessionistaRepository professionistaRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender,
                            PasswordEncoder passwordEncoder) {
        this.pazienteRepository = pazienteRepository;
        this.visitaRepository = visitaRepository;
        this.professionistaRepository = professionistaRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.lavoro(), request.tipoLavoro());
        pazienteRepository.save(paziente);

        VisitaRequest v = request.visita();
        Visita visita = new Visita(paziente.getId(), v.altezzaCm(), v.pesoKg(),
                v.circonferenzaVitaCm(), v.circonferenzaOmbelicoCm(), v.circonferenzaFianchiCm(),
                v.circonferenzaPettoCm(), v.circonferenzaCosciaDxCm(), v.circonferenzaCosciaSxCm(),
                v.circonferenzaPolpaccioDxCm(), v.circonferenzaPolpaccioSxCm(),
                v.larghezzaSpalleCm(), v.circonferenzaSpalleCm(),
                v.circonferenzaBicipiteDxCm(), v.circonferenzaBicipiteSxCm());
        visitaRepository.save(visita);

        return paziente;
    }

    public List<Paziente> listaPerProfessionista(UUID professionistaId) {
        return pazienteRepository.findAllByProfessionistaId(professionistaId);
    }

    public Paziente dettaglio(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = pazienteRepository.findById(pazienteId)
                .orElseThrow(PazienteNonTrovatoException::new);
        if (!paziente.getProfessionistaId().equals(professionistaId)) {
            throw new PazienteNonTrovatoException();
        }
        return paziente;
    }

    @Transactional
    public void invita(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        if (paziente.getStatoAccount() == StatoAccountPaziente.ATTIVO) {
            throw new PazienteGiaAttivoException();
        }
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);
        paziente.setStatoAccount(StatoAccountPaziente.INVITATO);
        pazienteRepository.save(paziente);
        emailSender.invia(paziente.getEmail(), "Sei stato invitato su Hexis Nutrition",
                "<p>Attiva il tuo account: <a href=\"https://app.hexisnutrition.example/attiva?token="
                        + token.getToken() + "\">Attiva account</a></p>");
    }

    @Transactional
    public void attiva(String token, String nuovaPassword) {
        TokenAzione tokenAzione = tokenAzioneRepository.findByTokenHash(TokenAzione.hash(token))
                .filter(TokenAzione::isValido)
                .filter(t -> t.getTipo() == TipoToken.INVITO)
                .orElseThrow(TokenNonValidoException::new);

        Paziente paziente = pazienteRepository.findById(tokenAzione.getPazienteId())
                .orElseThrow(PazienteNonTrovatoException::new);

        boolean emailUsataDaProfessionista = professionistaRepository.findByEmail(paziente.getEmail()).isPresent();
        boolean emailUsataDaAltroPazienteAttivo = pazienteRepository
                .existsByEmailAndStatoAccount(paziente.getEmail(), StatoAccountPaziente.ATTIVO);
        if (emailUsataDaProfessionista || emailUsataDaAltroPazienteAttivo) {
            throw new EmailGiaInUsoException();
        }

        paziente.setPasswordHash(passwordEncoder.encode(nuovaPassword));
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(paziente);

        tokenAzioneRepository.delete(tokenAzione);
    }
}
```

- [ ] **Step 6: Esegui i test e verifica che passino**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn test
```

Atteso: PASS, tutti i test verdi (backend completo).

- [ ] **Step 7: Metti in staging (niente commit)**

```powershell
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java
git add backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

### Task 4: Frontend — tipo `Paziente` (risposta): `lavoro`/`tipoLavoro` al posto di `altezzaCm`

**Files:**
- Modify: `frontend-professionisti/src/api/pazienti.ts`
- Modify: `frontend-professionisti/src/views/PazienteDettaglioView.spec.ts`
- Modify: `frontend-professionisti/src/views/PazientiListView.spec.ts`
- Modify: `frontend-professionisti/src/views/DashboardView.spec.ts`
- Modify: `frontend-professionisti/src/api/pazienti.spec.ts`

**Interfaces:**
- Consumes: risposta JSON di `GET /pazienti` / `GET /pazienti/{id}` come definita da `PazienteResponse` (Task 2) — `lavoro`/`tipoLavoro` al posto di `altezzaCm`.
- Produces: `Paziente` (tipo TS aggiornato). **Non tocca `CreaPazienteRequest`** — quello è del Task 5, accoppiato alla riscrittura del form.

- [ ] **Step 1: Scrivi il test che fallisce**

In `frontend-professionisti/src/api/pazienti.spec.ts`, sostituisci la fixture:

```ts
const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO',
}
```

con:

```ts
const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: null, lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
}
```

- [ ] **Step 2: Esegui il typecheck e verifica che fallisca**

```powershell
cd frontend-professionisti
npx tsc --noEmit
```

Atteso: FAIL — `altezzaCm` non è più (o non ancora, a seconda dell'ordine) un campo coerente con `Paziente`; in ogni caso il tipo `Paziente` corrente (con `altezzaCm`) non corrisponde più alla fixture appena cambiata finché non si esegue lo Step 3. Se il tool di partenza non lo segnala per via del tipo strutturale, procedi comunque: la verifica reale è lo Step 5 dopo la modifica del tipo.

- [ ] **Step 3: Aggiorna il tipo `Paziente` in `api/pazienti.ts`**

In `frontend-professionisti/src/api/pazienti.ts`, sostituisci:

```ts
export interface Paziente {
  id: string
  nome: string
  cognome: string
  email: string
  telefono: string | null
  dataNascita: string | null
  sesso: string | null
  altezzaCm: number | null
  statoAccount: 'MAI_INVITATO' | 'INVITATO' | 'ATTIVO'
}
```

con:

```ts
export interface Paziente {
  id: string
  nome: string
  cognome: string
  email: string
  telefono: string | null
  dataNascita: string | null
  sesso: string | null
  lavoro: string | null
  tipoLavoro: string | null
  statoAccount: 'MAI_INVITATO' | 'INVITATO' | 'ATTIVO'
}
```

- [ ] **Step 4: Aggiorna le altre fixture di test**

In ciascuno dei seguenti file, sostituisci **tutte** le occorrenze di

```
sesso: null, altezzaCm: null, statoAccount:
```

con

```
sesso: null, lavoro: null, tipoLavoro: null, statoAccount:
```

- `frontend-professionisti/src/views/PazienteDettaglioView.spec.ts` (3 occorrenze)
- `frontend-professionisti/src/views/PazientiListView.spec.ts` (1 occorrenza)
- `frontend-professionisti/src/views/DashboardView.spec.ts` (3 occorrenze)

- [ ] **Step 5: Esegui i test e il typecheck, verifica che passino**

```powershell
cd frontend-professionisti
npm run test
npx tsc --noEmit
```

Atteso: PASS su entrambi.

- [ ] **Step 6: Metti in staging (niente commit)**

```powershell
git add frontend-professionisti/src/api/pazienti.ts
git add frontend-professionisti/src/api/pazienti.spec.ts
git add frontend-professionisti/src/views/PazienteDettaglioView.spec.ts
git add frontend-professionisti/src/views/PazientiListView.spec.ts
git add frontend-professionisti/src/views/DashboardView.spec.ts
```

---

### Task 5: Frontend — form "Nuovo paziente" con anagrafica + dati della visita

**Files:**
- Modify: `frontend-professionisti/src/api/pazienti.ts` (aggiunta `Visita`/`CreaVisitaRequest`, estensione `CreaPazienteRequest`)
- Create (via CLI): `frontend-professionisti/src/components/ui/select/*` (shadcn-vue)
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.vue`
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.spec.ts`

**Interfaces:**
- Consumes: `crea(request: CreaPazienteRequest): Promise<Paziente>` (esistente, firma invariata — cambia solo la forma di `CreaPazienteRequest`); `Paziente` (Task 4).
- Produces: nessun consumer successivo in questo piano — è l'ultimo anello della catena UI→API.

- [ ] **Step 1: Scrivi il test che fallisce**

Sostituisci l'intero contenuto di `frontend-professionisti/src/views/PazienteNuovoView.spec.ts` con:

```ts
import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteNuovoView from './PazienteNuovoView.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: PazienteNuovoView },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('PazienteNuovoView', () => {
  it('crea il paziente con i dati anagrafici e della visita, poi naviga al suo dettaglio', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '42', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: null, lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82.5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: undefined, dataNascita: undefined, sesso: undefined, lavoro: undefined, tipoLavoro: undefined,
      visita: {
        altezzaCm: 178, pesoKg: 82.5,
        circonferenzaVitaCm: undefined, circonferenzaOmbelicoCm: undefined, circonferenzaFianchiCm: undefined,
        circonferenzaPettoCm: undefined, circonferenzaCosciaDxCm: undefined, circonferenzaCosciaSxCm: undefined,
        circonferenzaPolpaccioDxCm: undefined, circonferenzaPolpaccioSxCm: undefined,
        larghezzaSpalleCm: undefined, circonferenzaSpalleCm: undefined,
        circonferenzaBicipiteDxCm: undefined, circonferenzaBicipiteSxCm: undefined,
      },
    })
    expect(router.currentRoute.value.path).toBe('/pazienti/42')
  })

  it('mostra un errore se la creazione fallisce', async () => {
    vi.mocked(pazientiApi.crea).mockRejectedValue(new Error('email duplicata'))
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82.5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile creare il paziente')
  })
})
```

- [ ] **Step 2: Esegui il test e verifica che fallisca**

```powershell
cd frontend-professionisti
npm run test -- PazienteNuovoView
```

Atteso: FAIL — `PazienteNuovoView.vue` non ha ancora i campi `#altezza`/`#peso`, e `crea()` viene ancora chiamata con il vecchio payload senza `visita`.

- [ ] **Step 3: Aggiungi il componente `select` di shadcn-vue**

```powershell
cd frontend-professionisti
npx shadcn-vue@latest add select
```

Verifica che sia stato creato `frontend-professionisti/src/components/ui/select/` con un `index.ts` che esporta (tra gli altri) `Select`, `SelectTrigger`, `SelectValue`, `SelectContent`, `SelectItem` — stesso pattern già in uso per `frontend-professionisti/src/components/ui/dropdown-menu/index.ts`.

- [ ] **Step 4: Estendi `api/pazienti.ts` con `Visita`/`CreaVisitaRequest` e il nuovo `CreaPazienteRequest`**

In `frontend-professionisti/src/api/pazienti.ts`, sostituisci:

```ts
export interface CreaPazienteRequest {
  nome: string
  cognome: string
  email: string
  telefono?: string
  dataNascita?: string
  sesso?: string
  altezzaCm?: number
}
```

con:

```ts
export interface CreaVisitaRequest {
  altezzaCm: number
  pesoKg: number
  circonferenzaVitaCm?: number
  circonferenzaOmbelicoCm?: number
  circonferenzaFianchiCm?: number
  circonferenzaPettoCm?: number
  circonferenzaCosciaDxCm?: number
  circonferenzaCosciaSxCm?: number
  circonferenzaPolpaccioDxCm?: number
  circonferenzaPolpaccioSxCm?: number
  larghezzaSpalleCm?: number
  circonferenzaSpalleCm?: number
  circonferenzaBicipiteDxCm?: number
  circonferenzaBicipiteSxCm?: number
}

export interface CreaPazienteRequest {
  nome: string
  cognome: string
  email: string
  telefono?: string
  dataNascita?: string
  sesso?: string
  lavoro?: string
  tipoLavoro?: string
  visita: CreaVisitaRequest
}
```

- [ ] **Step 5: Riscrivi `PazienteNuovoView.vue`**

Sostituisci l'intero contenuto di `frontend-professionisti/src/views/PazienteNuovoView.vue` con:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { crea } from '@/api/pazienti'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { AlertCircle, ArrowLeft } from '@lucide/vue'

const nome = ref('')
const cognome = ref('')
const sesso = ref('')
const email = ref('')
const telefono = ref('')
const dataNascita = ref('')
const lavoro = ref('')
const tipoLavoro = ref('')

const altezzaCm = ref('')
const pesoKg = ref('')
const circonferenzaVita = ref('')
const circonferenzaOmbelico = ref('')
const circonferenzaFianchi = ref('')
const circonferenzaPetto = ref('')
const circonferenzaCosciaDx = ref('')
const circonferenzaCosciaSx = ref('')
const circonferenzaPolpaccioDx = ref('')
const circonferenzaPolpaccioSx = ref('')
const larghezzaSpalle = ref('')
const circonferenzaSpalle = ref('')
const circonferenzaBicipiteDx = ref('')
const circonferenzaBicipiteSx = ref('')

const inCorso = ref(false)
const errore = ref('')

const router = useRouter()

function numeroOpzionale(valore: string): number | undefined {
  return valore ? Number(valore) : undefined
}

async function onSubmit() {
  inCorso.value = true
  errore.value = ''
  try {
    const paziente = await crea({
      nome: nome.value,
      cognome: cognome.value,
      email: email.value,
      telefono: telefono.value || undefined,
      dataNascita: dataNascita.value || undefined,
      sesso: sesso.value || undefined,
      lavoro: lavoro.value || undefined,
      tipoLavoro: tipoLavoro.value || undefined,
      visita: {
        altezzaCm: Number(altezzaCm.value),
        pesoKg: Number(pesoKg.value),
        circonferenzaVitaCm: numeroOpzionale(circonferenzaVita.value),
        circonferenzaOmbelicoCm: numeroOpzionale(circonferenzaOmbelico.value),
        circonferenzaFianchiCm: numeroOpzionale(circonferenzaFianchi.value),
        circonferenzaPettoCm: numeroOpzionale(circonferenzaPetto.value),
        circonferenzaCosciaDxCm: numeroOpzionale(circonferenzaCosciaDx.value),
        circonferenzaCosciaSxCm: numeroOpzionale(circonferenzaCosciaSx.value),
        circonferenzaPolpaccioDxCm: numeroOpzionale(circonferenzaPolpaccioDx.value),
        circonferenzaPolpaccioSxCm: numeroOpzionale(circonferenzaPolpaccioSx.value),
        larghezzaSpalleCm: numeroOpzionale(larghezzaSpalle.value),
        circonferenzaSpalleCm: numeroOpzionale(circonferenzaSpalle.value),
        circonferenzaBicipiteDxCm: numeroOpzionale(circonferenzaBicipiteDx.value),
        circonferenzaBicipiteSxCm: numeroOpzionale(circonferenzaBicipiteSx.value),
      },
    })
    router.push(`/pazienti/${paziente.id}`)
  } catch {
    errore.value = 'Non è stato possibile creare il paziente. Controlla i dati e riprova.'
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="mb-6">
      <router-link to="/pazienti"
          class="inline-flex items-center gap-2 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)"
        >
          <ArrowLeft :size="16" />
          <span>Torna alla lista pazienti</span>
        </router-link>
        <h1 class="font-heading text-3xl italic text-(--fg)">Nuovo paziente</h1>
        <p class="mt-1 text-sm text-(--fg3)">
          Inserisci le informazioni personali e i dati della prima visita per registrare una nuova scheda clinica.
        </p>
    </div>

    <div v-if="errore" class="mb-6 flex items-start gap-3 rounded-xl border border-(--danger)/20 bg-(--warn-bg) p-3.5 text-xs font-medium text-(--danger)">
      <AlertCircle :size="16" class="mt-0.5 shrink-0" />
      <span>{{ errore }}</span>
    </div>

    <form class="space-y-6" @submit.prevent="onSubmit">
      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <h2 class="font-heading text-xl italic text-(--fg)">Dati anagrafici</h2>

        <div class="mt-5 grid gap-5 sm:grid-cols-2">
          <div class="flex flex-col gap-1.5">
            <Label for="nome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Nome*</Label>
            <Input id="nome" v-model="nome" required placeholder="Es. Mario" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="cognome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Cognome*</Label>
            <Input id="cognome" v-model="cognome" required placeholder="Es. Rossi" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="sesso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Sesso</Label>
            <Select v-model="sesso">
              <SelectTrigger id="sesso" class="w-full">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="M">Maschio</SelectItem>
                <SelectItem value="F">Femmina</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="data-nascita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Data di nascita</Label>
            <Input id="data-nascita" v-model="dataNascita" type="date" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="email" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Email*</Label>
            <Input id="email" v-model="email" type="email" required placeholder="Es. mariorossi@gmail.com" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="telefono" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Telefono</Label>
            <Input id="telefono" v-model="telefono" placeholder="Es. 3325676543" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="lavoro" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Lavoro</Label>
            <Input id="lavoro" v-model="lavoro" placeholder="Es. Impiegato" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="tipo-lavoro" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Tipo lavoro</Label>
            <Select v-model="tipoLavoro">
              <SelectTrigger id="tipo-lavoro" class="w-full">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="SEDENTARIO">Sedentario</SelectItem>
                <SelectItem value="POCO_ATTIVO">Poco attivo</SelectItem>
                <SelectItem value="ATTIVO">Attivo</SelectItem>
                <SelectItem value="MOLTO_ATTIVO">Molto attivo</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <h2 class="font-heading text-xl italic text-(--fg)">Dati della visita</h2>

        <div class="mt-5 grid gap-5 sm:grid-cols-2">
          <div class="flex flex-col gap-1.5">
            <Label for="altezza" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Altezza (cm)*</Label>
            <Input id="altezza" v-model="altezzaCm" type="number" step="1" required placeholder="Es. 178" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="peso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Peso (kg)*</Label>
            <Input id="peso" v-model="pesoKg" type="number" step="0.1" required placeholder="Es. 78.5" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-vita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza vita (cm)</Label>
            <Input id="circonferenza-vita" v-model="circonferenzaVita" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-ombelico" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza ombelico (cm)</Label>
            <Input id="circonferenza-ombelico" v-model="circonferenzaOmbelico" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-fianchi" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza fianchi (cm)</Label>
            <Input id="circonferenza-fianchi" v-model="circonferenzaFianchi" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-petto" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza petto (cm)</Label>
            <Input id="circonferenza-petto" v-model="circonferenzaPetto" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-coscia-dx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza coscia dx (cm)</Label>
            <Input id="circonferenza-coscia-dx" v-model="circonferenzaCosciaDx" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-coscia-sx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza coscia sx (cm)</Label>
            <Input id="circonferenza-coscia-sx" v-model="circonferenzaCosciaSx" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-polpaccio-dx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza polpaccio dx (cm)</Label>
            <Input id="circonferenza-polpaccio-dx" v-model="circonferenzaPolpaccioDx" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-polpaccio-sx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza polpaccio sx (cm)</Label>
            <Input id="circonferenza-polpaccio-sx" v-model="circonferenzaPolpaccioSx" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="larghezza-spalle" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Larghezza spalle (cm)</Label>
            <Input id="larghezza-spalle" v-model="larghezzaSpalle" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-spalle" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza spalle (cm)</Label>
            <Input id="circonferenza-spalle" v-model="circonferenzaSpalle" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-bicipite-dx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza bicipite dx (cm)</Label>
            <Input id="circonferenza-bicipite-dx" v-model="circonferenzaBicipiteDx" type="number" step="0.1" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="circonferenza-bicipite-sx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza bicipite sx (cm)</Label>
            <Input id="circonferenza-bicipite-sx" v-model="circonferenzaBicipiteSx" type="number" step="0.1" />
          </div>
        </div>

        <div class="mt-6 border-t border-(--bd) pt-5">
          <h3 class="text-sm font-bold uppercase tracking-wide text-(--fg3)">Misurazione BIA</h3>
          <p class="mt-1.5 text-sm text-(--fg3)">Sarà disponibile a breve.</p>
        </div>
      </div>

      <Button type="submit" :disabled="inCorso">
        {{ inCorso ? 'Salvataggio…' : 'Crea paziente' }}
      </Button>
    </form>
  </AppShell>
</template>
```

- [ ] **Step 6: Esegui i test e verifica che passino**

```powershell
cd frontend-professionisti
npm run test
npx tsc --noEmit
```

Atteso: PASS su entrambi (l'intera suite, non solo `PazienteNuovoView`).

- [ ] **Step 7: Metti in staging (niente commit)**

```powershell
git add frontend-professionisti/src/api/pazienti.ts
git add frontend-professionisti/src/components/ui/select
git add frontend-professionisti/src/views/PazienteNuovoView.vue
git add frontend-professionisti/src/views/PazienteNuovoView.spec.ts
git add frontend-professionisti/package.json frontend-professionisti/package-lock.json
```

---

### Task 6: Documentazione — allinea la wiki di progetto

**Files:**
- Modify: `wiki/api-contracts.md`
- Modify: `wiki/modello-dati.md`

**Interfaces:**
- Consumes: il contratto reale implementato nei Task 1-3 (nessuna interfaccia di codice).
- Produces: pagine wiki allineate al codice — nessun consumer nel codice.

- [ ] **Step 1: Aggiorna `wiki/api-contracts.md`**

Nella riga della tabella per `POST /pazienti`, aggiorna la descrizione per menzionare la creazione contestuale della prima visita:

```
| POST | `/pazienti` | PROFESSIONISTA | Crea anagrafica paziente **e prima visita** (dati antropometrici), 201; 400 se la visita manca o se altezza/peso al suo interno sono nulli |
```

Aggiungi una riga di nota dopo la tabella (prima del paragrafo "Richiesta/risposta dettagliate..."):

```
`POST /pazienti` richiede un oggetto `visita` obbligatorio nel body (altezza e peso obbligatori, le circonferenze sono opzionali) — creato in transazione con l'anagrafica. Dettagli campo per campo in `VisitaRequest` (`backend/src/main/java/com/hexisnutrition/backend/pazienti/VisitaRequest.java`).
```

- [ ] **Step 2: Aggiorna `wiki/modello-dati.md`**

Nella sezione "Entità implementate", dopo la voce **Paziente**, aggiorna la sua descrizione e aggiungi la voce **Visita**:

Sostituisci:

```
- **Paziente** (V2): nome, cognome, email, telefono, data di nascita, sesso, altezza; collegato a un `Professionista`; stato account (`MAI_INVITATO` / `INVITATO` / `ATTIVO`). Non richiede un account attivo per esistere. Email univoca solo tra account `ATTIVO` (indice parziale), non a livello di anagrafica.
```

con:

```
- **Paziente** (V2, esteso in V6): nome, cognome, email, telefono, data di nascita, sesso, lavoro, tipo di lavoro (`SEDENTARIO`/`POCO_ATTIVO`/`ATTIVO`/`MOLTO_ATTIVO`); collegato a un `Professionista`; stato account (`MAI_INVITATO` / `INVITATO` / `ATTIVO`). Non richiede un account attivo per esistere. Email univoca solo tra account `ATTIVO` (indice parziale), non a livello di anagrafica. L'altezza **non** è più qui: è stata spostata su `Visita` (V5), storicizzata per ogni visita anziché unica per paziente.
- **Visita** (V5): altezza, peso, 12 circonferenze (vita, ombelico, fianchi, petto, coscia dx/sx, polpaccio dx/sx, larghezza spalle, circonferenza spalle, bicipite dx/sx), data visita; collegata a un `Paziente`. Solo altezza e peso sono obbligatori. Creata contestualmente alla prima visita in `POST /pazienti`; visite successive (storico, endpoint dedicato) restano fuori scope — anticipa il sotto-progetto "Monitoraggio". Nessun campo per la misurazione BIA per ora (da definire).
```

- [ ] **Step 3: Metti in staging (niente commit)**

```powershell
git add wiki/api-contracts.md wiki/modello-dati.md
```

---

## Self-Review

**Copertura spec**: ogni sezione della spec (`docs/superpowers/specs/2026-08-31-nuovo-paziente-con-visita-design.md`) è coperta — modello dati → Task 1+2, contratto API → Task 3, frontend → Task 4+5, testing → step di ogni task, wiki → Task 6. BIA placeholder → Task 5, Step 5 (sotto-sezione nella card "Dati della visita"). Fuori scope esplicitamente non implementato: campi BIA reali, storico visite, endpoint PATCH anagrafica.

**Scansione placeholder**: nessun TBD/TODO; ogni step ha codice reale, non descrizioni generiche.

**Coerenza dei tipi**: verificato che `Visita`/`VisitaRepository` (Task 1) → consumati identici in Task 3; `Paziente` a 9 argomenti (Task 2) → stessa firma usata in Task 3 e nei fix dei call site; `CreaVisitaRequest`/`CreaPazienteRequest` (Task 5) → stessi nomi di campo di `VisitaRequest`/`CreaPazienteRequest` lato backend (Task 3), incluso l'ordine dei campi nell'assert del test frontend.
