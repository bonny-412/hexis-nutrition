# Lista pazienti paginata — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sostituire la lista pazienti minimale con una tabella paginata/filtrata lato server (fedele nella struttura al mockup fornito), aggiungere l'archiviazione logica dei pazienti e il relativo menu azioni riga.

**Architecture:** Nuovo endpoint `GET /pazienti/ricerca` (Specification JPA dinamiche + `Pageable`) separato dal `GET /pazienti` esistente (che resta invariato, usato da `DashboardView.vue`). Nuovo flag `archiviato` su `Paziente`, con endpoint azione `POST /pazienti/{id}/archivia` e `/de-archivia`. Frontend: `PazientiListView.vue` riscritta per pilotare ricerca/filtri/ordinamento/paginazione lato server via una nuova funzione `cerca()`; azioni riga (invita, apri cartella, archivia/de-archivia, menu) estratte nel componente `PazienteRigaAzioni.vue`.

**Tech Stack:** Spring Boot 3.3.4 / Java 21 / PostgreSQL (Spring Data JPA Specifications) per il backend; Vue 3 + TypeScript + Tailwind CSS + shadcn-vue (Reka UI) per il frontend.

**Spec:** [`docs/superpowers/specs/2026-09-02-lista-pazienti-paginata-design.md`](../specs/2026-09-02-lista-pazienti-paginata-design.md)

## Global Constraints

- **Mai `git commit`** — questo progetto vieta commit automatici (vedi `CLAUDE.md` radice del workspace). Ogni step "Commit" di questo piano è in realtà uno step **"Stage"**: `git add` sui file toccati, mai `git commit`.
- `GET /pazienti` esistente **non va modificato** — `DashboardView.vue` lo usa per contare i pazienti attivi su tutta la lista, non paginata.
- Nessuna selezione multipla / azioni di massa, nessun command palette (⌘K) — esplicitamente fuori scope.
- La voce di menu "Nuova visita" resta **disabilitata** con etichetta secondaria "Presto disponibile" — nessun flusso da costruire per questo piano.
- Parametri query e nomi di campo in italiano, coerenti col resto dell'API (`ricerca`, `pagina`, `dimensione`, `ordinaPer`, `direzione`, `archiviato`, `dataNascitaDa`/`dataNascitaA`).
- `dimensione` va sempre clampata lato server (1–100) prima di costruire il `Pageable`.
- Ogni cambio di ricerca/filtro/ordinamento lato frontend riporta `pagina` a `0`.
- Backend: dopo ogni modifica, eseguire `mvn test` da `backend/` con `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` impostato (JDK di sistema di default è Java 8), richiede `postgresql-x64-13` attivo.
- Frontend: dopo ogni modifica, eseguire `npx vitest run` e `npx tsc --noEmit` da `frontend-professionisti/`.

---

## Task 1: Flag `archiviato` — migrazione, entità, risposta

**Files:**
- Create: `backend/src/main/resources/db/migration/V15__paziente_archiviato.sql`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Produces: `Paziente.isArchiviato(): boolean`, `Paziente.setArchiviato(boolean)` (default `false`); `PazienteResponse.archiviato(): boolean` (nuovo componente record, ultimo).

- [ ] **Step 1: Scrivi il test che fallisce (default `archiviato = false`)**

Aggiungi in fondo a `PazienteRepositoryTest.java` (dentro la classe, prima dell'ultima `}`):

```java
    @Test
    void unNuovoPazienteNonEArchiviatoPerDefault() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-archivio@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca.archivio@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null);
        pazienteRepository.save(paziente);

        Paziente ritrovato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(ritrovato.isArchiviato()).isFalse();
    }
```

- [ ] **Step 2: Esegui il test e verifica che fallisca**

Run: `mvn test -Dtest=PazienteRepositoryTest -pl . ` da `backend/` (con `JAVA_HOME` impostato come da Global Constraints)
Expected: FAIL — `isArchiviato()` non esiste su `Paziente` (errore di compilazione).

- [ ] **Step 3: Migrazione SQL**

`backend/src/main/resources/db/migration/V15__paziente_archiviato.sql`:

```sql
ALTER TABLE pazienti ADD COLUMN archiviato BOOLEAN NOT NULL DEFAULT false;
```

- [ ] **Step 4: Campo entità**

In `Paziente.java`, aggiungi il campo dopo `statoAccount` (circa riga 59, dopo `private StatoAccountPaziente statoAccount = StatoAccountPaziente.MAI_INVITATO;`):

```java
    @Column(nullable = false)
    private boolean archiviato = false;
```

E i due metodi dopo `setStatoAccount(...)` (fine classe, prima dell'ultima `}`):

```java
    public boolean isArchiviato() {
        return archiviato;
    }

    public void setArchiviato(boolean archiviato) {
        this.archiviato = archiviato;
    }
```

- [ ] **Step 5: Esegui il test e verifica che passi**

Run: `mvn test -Dtest=PazienteRepositoryTest` da `backend/`
Expected: PASS

- [ ] **Step 6: Esponi il campo in `PazienteResponse` e aggiorna il test del controller**

`PazienteResponse.java` — aggiungi il componente `archiviato` e passalo nella factory:

```java
public record PazienteResponse(
        UUID id,
        String nome,
        String cognome,
        String codiceFiscale,
        String email,
        String telefono,
        LocalDate dataNascita,
        String sesso,
        String lavoro,
        String tipoLavoro,
        String statoAccount,
        boolean archiviato
) {
    public static PazienteResponse da(Paziente paziente) {
        return new PazienteResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getCodiceFiscale(), paziente.getEmail(), paziente.getTelefono(), paziente.getDataNascita(),
                paziente.getSesso().name(), paziente.getLavoro(),
                paziente.getTipoLavoro() != null ? paziente.getTipoLavoro().name() : null,
                paziente.getStatoAccount().name(), paziente.isArchiviato());
    }
}
```

In `PazienteControllerTest.java`, nel test `creaPazienteRestituisce201EPersisteLaPrimaVisita`, aggiungi un'asserzione dopo `.andExpect(jsonPath("$.statoAccount").value("MAI_INVITATO"));`:

```java
                .andExpect(jsonPath("$.archiviato").value(false));
```

(nota: questo diventa l'ultima riga della catena `.andExpect(...)`, prima del `;` — sposta il `;` di conseguenza).

- [ ] **Step 7: Esegui l'intera suite backend**

Run: `mvn test` da `backend/`
Expected: PASS (tutti i test, inclusi quelli esistenti)

- [ ] **Step 8: Stage**

```bash
git add backend/src/main/resources/db/migration/V15__paziente_archiviato.sql backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Task 2: Blocco invito su paziente archiviato

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteArchiviatoException.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: `Paziente.isArchiviato()` (Task 1).
- Produces: `PazienteArchiviatoException` (RuntimeException, `@ResponseStatus(HttpStatus.BAD_REQUEST)`).

- [ ] **Step 1: Scrivi il test che fallisce**

Aggiungi in `PazienteControllerTest.java`, subito dopo il test `invitoAPazienteGiaAttivoRestituisce409`:

```java
    @Test
    void invitoAPazienteArchiviatoRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-arch-invito@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-arch-invito@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isBadRequest());
    }
```

- [ ] **Step 2: Esegui il test e verifica che fallisca**

Run: `mvn test -Dtest=PazienteControllerTest#invitoAPazienteArchiviatoRestituisce400` da `backend/`
Expected: FAIL — oggi l'invito procede normalmente (nessun blocco), restituisce 204 invece di 400.

- [ ] **Step 3: Eccezione**

`backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteArchiviatoException.java`:

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PazienteArchiviatoException extends RuntimeException {
}
```

- [ ] **Step 4: Blocco in `PazienteService.invita(...)`**

In `PazienteService.java`, dentro `invita(...)`, subito dopo `Paziente paziente = dettaglio(professionistaId, pazienteId);` e **prima** del controllo `StatoAccountPaziente.ATTIVO`:

```java
        if (paziente.isArchiviato()) {
            throw new PazienteArchiviatoException();
        }
```

- [ ] **Step 5: Esegui il test e verifica che passi**

Run: `mvn test -Dtest=PazienteControllerTest` da `backend/`
Expected: PASS (incluso il nuovo test e tutti quelli esistenti sull'invito)

- [ ] **Step 6: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteArchiviatoException.java backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Task 3: Endpoint `archivia` / `de-archivia`

**Files:**
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: `PazienteService.dettaglio(UUID, UUID)` (esistente, controllo ownership).
- Produces: `PazienteService.archivia(UUID professionistaId, UUID pazienteId)`, `PazienteService.deArchivia(UUID professionistaId, UUID pazienteId)`; endpoint `POST /pazienti/{id}/archivia` e `POST /pazienti/{id}/de-archivia` (204).

- [ ] **Step 1: Scrivi i test che falliscono**

Aggiungi in `PazienteControllerTest.java`, dopo il test `invitoAPazienteArchiviatoRestituisce400` (Task 2):

```java
    @Test
    void archiviaImpostaIlFlagArchiviato() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-archivia@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-archivia@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.isArchiviato()).isTrue();
    }

    @Test
    void deArchiviaRimuoveIlFlagArchiviato() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-dearchivia@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-dearchivia@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/de-archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.isArchiviato()).isFalse();
    }

    @Test
    void nonSiPuoArchiviareUnPazienteDiUnAltroProfessionista() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-a-arch@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-b-arch@example.com", "hash", "B", "B"));
        Paziente pazienteDiB = pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "RSSMRA80A01H501U", "diB-arch@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(post("/pazienti/" + pazienteDiB.getId() + "/archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiviareUnPazienteGiaArchiviatoNonProduceErrori() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-arch-idem@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-arch-idem@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        assertThat(pazienteRepository.findById(paziente.getId()).orElseThrow().isArchiviato()).isTrue();
    }

    @Test
    void deArchiviareUnPazienteNonArchiviatoNonProduceErrori() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-dearch-idem@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-dearch-idem@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/de-archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        assertThat(pazienteRepository.findById(paziente.getId()).orElseThrow().isArchiviato()).isFalse();
    }
```

- [ ] **Step 2: Esegui i test e verifica che falliscano**

Run: `mvn test -Dtest=PazienteControllerTest` da `backend/`
Expected: FAIL — compilazione fallisce (nessun endpoint `/archivia` o `/de-archivia`), oppure 404 sulle richieste POST.

- [ ] **Step 3: Metodi di servizio**

In `PazienteService.java`, aggiungi dopo il metodo `invita(...)`:

```java
    @Transactional
    public void archivia(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);
    }

    @Transactional
    public void deArchivia(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        paziente.setArchiviato(false);
        pazienteRepository.save(paziente);
    }
```

- [ ] **Step 4: Endpoint controller**

In `PazienteController.java`, aggiungi dopo il metodo `invita(...)`:

```java
    @PostMapping("/{id}/archivia")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archivia(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pazienteService.archivia(professionistaId, id);
    }

    @PostMapping("/{id}/de-archivia")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deArchivia(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pazienteService.deArchivia(professionistaId, id);
    }
```

- [ ] **Step 5: Esegui i test e verifica che passino**

Run: `mvn test -Dtest=PazienteControllerTest` da `backend/`
Expected: PASS

- [ ] **Step 6: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Task 4: `PazienteSpecifications` — filtri dinamici

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteSpecifications.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteRepository.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteSpecificationsTest.java`

**Interfaces:**
- Produces: `PazienteSpecifications.delProfessionista(UUID)`, `.conArchiviato(boolean)`, `.conRicerca(String)`, `.conStatoAccount(StatoAccountPaziente)`, `.conSesso(Sesso)`, `.conDataNascitaTra(LocalDate da, LocalDate a)` — tutti `Specification<Paziente>`. `PazienteRepository` guadagna `JpaSpecificationExecutor<Paziente>`.

- [ ] **Step 1: Scrivi i test che falliscono**

`backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteSpecificationsTest.java`:

```java
package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PazienteSpecificationsTest extends AbstractIntegrationTest {

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    private Paziente creaPaziente(UUID professionistaId, String nome, String cognome, String email,
                                   String codiceFiscale, LocalDate dataNascita, Sesso sesso) {
        Paziente paziente = new Paziente(professionistaId, nome, cognome, codiceFiscale, email,
                null, dataNascita, sesso, null, null);
        return pazienteRepository.save(paziente);
    }

    @Test
    void conRicercaTrovaPerNomeCognomeEmailOCodiceFiscale() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec1@example.com", "hash", "Anna", "Bianchi"));
        creaPaziente(professionista.getId(), "Marco", "Rossi", "marco.rossi@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia.verdi@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conRicerca("giulia"));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getNome()).isEqualTo("Giulia");
    }

    @Test
    void conStatoAccountFiltraPerStato() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec2@example.com", "hash", "Anna", "Bianchi"));
        Paziente attivo = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-attivo@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        attivo.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(attivo);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-non-invitata@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conStatoAccount(StatoAccountPaziente.ATTIVO));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("marco-attivo@example.com");
    }

    @Test
    void conSessoFiltraPerSesso() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec3@example.com", "hash", "Anna", "Bianchi"));
        creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-m@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-f@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conSesso(Sesso.F));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getSesso()).isEqualTo(Sesso.F);
    }

    @Test
    void conDataNascitaTraFiltraPerIntervallo() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec4@example.com", "hash", "Anna", "Bianchi"));
        creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-1970@example.com",
                "RSSMRC70A01H501U", LocalDate.of(1970, 1, 1), Sesso.M);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-1995@example.com",
                "VRDGLI95A41H501U", LocalDate.of(1995, 3, 10), Sesso.F);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conDataNascitaTra(LocalDate.of(1990, 1, 1), LocalDate.of(2000, 1, 1)));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("giulia-1995@example.com");
    }

    @Test
    void conArchiviatoEscludeGliArchiviatiPerDefaultEIsolaGliArchiviati() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec5@example.com", "hash", "Anna", "Bianchi"));
        Paziente archiviato = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-archiviato@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        archiviato.setArchiviato(true);
        pazienteRepository.save(archiviato);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-attiva@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        List<Paziente> attivi = pazienteRepository.findAll(Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false)));
        List<Paziente> archiviati = pazienteRepository.findAll(Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(true)));

        assertThat(attivi).extracting(Paziente::getEmail).containsExactly("giulia-attiva@example.com");
        assertThat(archiviati).extracting(Paziente::getEmail).containsExactly("marco-archiviato@example.com");
    }

    @Test
    void combinaPiuFiltriConAnd() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec6@example.com", "hash", "Anna", "Bianchi"));
        Paziente match = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-combo@example.com",
                "RSSMRC90A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        match.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(match);
        creaPaziente(professionista.getId(), "Marco", "Bianchi", "marco-non-attivo@example.com",
                "BNCMRC90A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conRicerca("marco"),
                PazienteSpecifications.conStatoAccount(StatoAccountPaziente.ATTIVO));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("marco-combo@example.com");
    }

    @Test
    void delProfessionistaIsolaIPazientiPerProfessionista() {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-spec7a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-spec7b@example.com", "hash", "B", "B"));
        creaPaziente(professionistaA.getId(), "Marco", "Rossi", "marco-a@example.com",
                "RSSMRC90A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        creaPaziente(professionistaB.getId(), "Giulia", "Verdi", "giulia-b@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        List<Paziente> risultato = pazienteRepository.findAll(Specification.allOf(
                PazienteSpecifications.delProfessionista(professionistaA.getId()),
                PazienteSpecifications.conArchiviato(false)));

        assertThat(risultato).extracting(Paziente::getEmail).containsExactly("marco-a@example.com");
    }
}
```

- [ ] **Step 2: Esegui i test e verifica che falliscano**

Run: `mvn test -Dtest=PazienteSpecificationsTest` da `backend/`
Expected: FAIL — compilazione fallisce, `PazienteSpecifications` non esiste e `pazienteRepository.findAll(Specification)` non è disponibile.

- [ ] **Step 3: `PazienteRepository` guadagna `JpaSpecificationExecutor`**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PazienteRepository extends JpaRepository<Paziente, UUID>, JpaSpecificationExecutor<Paziente> {
    List<Paziente> findAllByProfessionistaId(UUID professionistaId);

    Optional<Paziente> findByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);

    boolean existsByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);
}
```

- [ ] **Step 4: `PazienteSpecifications`**

`backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteSpecifications.java`:

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class PazienteSpecifications {

    private PazienteSpecifications() {
    }

    public static Specification<Paziente> delProfessionista(UUID professionistaId) {
        return (root, query, cb) -> cb.equal(root.get("professionistaId"), professionistaId);
    }

    public static Specification<Paziente> conArchiviato(boolean archiviato) {
        return (root, query, cb) -> cb.equal(root.get("archiviato"), archiviato);
    }

    public static Specification<Paziente> conRicerca(String ricerca) {
        String pattern = "%" + ricerca.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nome")), pattern),
                cb.like(cb.lower(root.get("cognome")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("codiceFiscale")), pattern));
    }

    public static Specification<Paziente> conStatoAccount(StatoAccountPaziente statoAccount) {
        return (root, query, cb) -> cb.equal(root.get("statoAccount"), statoAccount);
    }

    public static Specification<Paziente> conSesso(Sesso sesso) {
        return (root, query, cb) -> cb.equal(root.get("sesso"), sesso);
    }

    public static Specification<Paziente> conDataNascitaTra(LocalDate da, LocalDate a) {
        return (root, query, cb) -> {
            if (da != null && a != null) {
                return cb.between(root.get("dataNascita"), da, a);
            }
            if (da != null) {
                return cb.greaterThanOrEqualTo(root.get("dataNascita"), da);
            }
            return cb.lessThanOrEqualTo(root.get("dataNascita"), a);
        };
    }
}
```

- [ ] **Step 5: Esegui i test e verifica che passino**

Run: `mvn test -Dtest=PazienteSpecificationsTest` da `backend/`
Expected: PASS

- [ ] **Step 6: Esegui l'intera suite backend**

Run: `mvn test` da `backend/`
Expected: PASS

- [ ] **Step 7: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteSpecifications.java backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteRepository.java backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteSpecificationsTest.java
```

---

## Task 5: Endpoint `GET /pazienti/ricerca`

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CampoOrdinamentoPazienti.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/DirezioneOrdinamento.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CriteriRicercaPazienti.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteListaPaginataResponse.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java`
- Modify: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: `PazienteSpecifications.*` (Task 4), `PazienteRepository` come `JpaSpecificationExecutor` (Task 4).
- Produces: `GET /pazienti/ricerca` → `PazienteListaPaginataResponse { contenuto, paginaCorrente, dimensionePagina, totaleElementi, totalePagine }`.

- [ ] **Step 1: Scrivi i test che falliscono**

Aggiungi in `PazienteControllerTest.java`, dopo il test `nonSiPuoArchiviareUnPazienteDiUnAltroProfessionista` (Task 3):

```java
    @Test
    void ricercaSenzaParametriRestituiscePaginaDefaultEscludendoArchiviati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca1@example.com", "hash", "Anna", "Bianchi"));
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca1@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        Paziente archiviato = new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI85A41H501U", "giulia-ricerca1@example.com", null, LocalDate.of(1985, 3, 10), Sesso.F, null, null);
        archiviato.setArchiviato(true);
        pazienteRepository.save(archiviato);

        mockMvc.perform(get("/pazienti/ricerca")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].email").value("marco-ricerca1@example.com"))
                .andExpect(jsonPath("$.paginaCorrente").value(0))
                .andExpect(jsonPath("$.dimensionePagina").value(20))
                .andExpect(jsonPath("$.totaleElementi").value(1))
                .andExpect(jsonPath("$.totalePagine").value(1));
    }

    @Test
    void ricercaConArchiviatoTrueRestituisceSoloGliArchiviati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca2@example.com", "hash", "Anna", "Bianchi"));
        Paziente archiviato = new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI85A41H501U", "giulia-ricerca2@example.com", null, LocalDate.of(1985, 3, 10), Sesso.F, null, null);
        archiviato.setArchiviato(true);
        pazienteRepository.save(archiviato);
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca2@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(get("/pazienti/ricerca?archiviato=true")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].email").value("giulia-ricerca2@example.com"));
    }

    @Test
    void ricercaConTestoLibero() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca3@example.com", "hash", "Anna", "Bianchi"));
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca3@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        pazienteRepository.save(new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI85A41H501U", "giulia-ricerca3@example.com", null, LocalDate.of(1985, 3, 10), Sesso.F, null, null));

        mockMvc.perform(get("/pazienti/ricerca?ricerca=giulia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Giulia"));
    }

    @Test
    void ricercaConStatoAccountSessoEIntervalloDataNascita() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca4@example.com", "hash", "Anna", "Bianchi"));
        Paziente match = new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca4@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null);
        match.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(match);
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Bianchi",
                "BNCMRC70A01H501U", "marco-vecchio-ricerca4@example.com", null, LocalDate.of(1970, 1, 1), Sesso.M, null, null));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("statoAccount", "ATTIVO")
                        .param("sesso", "M")
                        .param("dataNascitaDa", "1985-01-01")
                        .param("dataNascitaA", "1995-01-01")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].email").value("marco-ricerca4@example.com"));
    }

    @Test
    void ricercaOrdinaEPaginaIRisultati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca5@example.com", "hash", "Anna", "Bianchi"));
        pazienteRepository.save(new Paziente(professionista.getId(), "Carlo", "Neri",
                "NRICRL90A01H501U", "carlo-ricerca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        pazienteRepository.save(new Paziente(professionista.getId(), "Anna", "Bruni",
                "BRNANN90A01H501U", "anna-ricerca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.F, null, null));
        pazienteRepository.save(new Paziente(professionista.getId(), "Bruno", "Villa",
                "VLLBRN90A01H501U", "bruno-ricerca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("dimensione", "2")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(2))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Anna"))
                .andExpect(jsonPath("$.contenuto[1].nome").value("Bruno"))
                .andExpect(jsonPath("$.totaleElementi").value(3))
                .andExpect(jsonPath("$.totalePagine").value(2));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("dimensione", "2")
                        .param("pagina", "1")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Carlo"))
                .andExpect(jsonPath("$.paginaCorrente").value(1));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("ordinaPer", "nome")
                        .param("direzione", "desc")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].nome").value("Carlo"))
                .andExpect(jsonPath("$.contenuto[2].nome").value("Anna"));
    }

    @Test
    void ricercaSenzaAutenticazioneRestituisce401() throws Exception {
        mockMvc.perform(get("/pazienti/ricerca"))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 2: Esegui i test e verifica che falliscano**

Run: `mvn test -Dtest=PazienteControllerTest` da `backend/`
Expected: FAIL — compilazione fallisce, `GET /pazienti/ricerca` non esiste.

- [ ] **Step 3: Enum di ordinamento**

`backend/src/main/java/com/hexisnutrition/backend/pazienti/CampoOrdinamentoPazienti.java`:

```java
package com.hexisnutrition.backend.pazienti;

public enum CampoOrdinamentoPazienti {
    nome,
    cognome,
    dataNascita,
    statoAccount
}
```

`backend/src/main/java/com/hexisnutrition/backend/pazienti/DirezioneOrdinamento.java`:

```java
package com.hexisnutrition.backend.pazienti;

public enum DirezioneOrdinamento {
    asc,
    desc
}
```

(valori Spring convertono automaticamente il parametro query nell'enum corrispondente; un valore non ammesso produce già 400 tramite la gestione errori di default di Spring Boot).

- [ ] **Step 4: DTO criteri e risposta paginata**

`backend/src/main/java/com/hexisnutrition/backend/pazienti/CriteriRicercaPazienti.java`:

```java
package com.hexisnutrition.backend.pazienti;

import java.time.LocalDate;

public record CriteriRicercaPazienti(
        String ricerca,
        StatoAccountPaziente statoAccount,
        Sesso sesso,
        LocalDate dataNascitaDa,
        LocalDate dataNascitaA,
        boolean archiviato
) {
}
```

`backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteListaPaginataResponse.java`:

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.domain.Page;

import java.util.List;

public record PazienteListaPaginataResponse(
        List<PazienteResponse> contenuto,
        int paginaCorrente,
        int dimensionePagina,
        long totaleElementi,
        int totalePagine
) {
    public static PazienteListaPaginataResponse da(Page<Paziente> pagina) {
        return new PazienteListaPaginataResponse(
                pagina.getContent().stream().map(PazienteResponse::da).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
```

- [ ] **Step 5: `PazienteService.cerca(...)`**

In `PazienteService.java`, aggiungi (import da aggiungere: `org.springframework.data.domain.Page`, `org.springframework.data.domain.Pageable`, `org.springframework.data.jpa.domain.Specification`, `java.util.ArrayList`):

```java
    public Page<Paziente> cerca(UUID professionistaId, CriteriRicercaPazienti criteri, Pageable pageable) {
        List<Specification<Paziente>> specifiche = new ArrayList<>();
        specifiche.add(PazienteSpecifications.delProfessionista(professionistaId));
        specifiche.add(PazienteSpecifications.conArchiviato(criteri.archiviato()));
        if (criteri.ricerca() != null && !criteri.ricerca().isBlank()) {
            specifiche.add(PazienteSpecifications.conRicerca(criteri.ricerca()));
        }
        if (criteri.statoAccount() != null) {
            specifiche.add(PazienteSpecifications.conStatoAccount(criteri.statoAccount()));
        }
        if (criteri.sesso() != null) {
            specifiche.add(PazienteSpecifications.conSesso(criteri.sesso()));
        }
        if (criteri.dataNascitaDa() != null || criteri.dataNascitaA() != null) {
            specifiche.add(PazienteSpecifications.conDataNascitaTra(criteri.dataNascitaDa(), criteri.dataNascitaA()));
        }
        return pazienteRepository.findAll(Specification.allOf(specifiche), pageable);
    }
```

- [ ] **Step 6: Endpoint controller**

In `PazienteController.java`, aggiungi gli import necessari (`org.springframework.data.domain.Page`, `org.springframework.data.domain.PageRequest`, `org.springframework.data.domain.Pageable`, `org.springframework.data.domain.Sort`, `org.springframework.format.annotation.DateTimeFormat`, `java.time.LocalDate`) e il metodo, dopo `lista(...)`:

```java
    @GetMapping("/ricerca")
    public PazienteListaPaginataResponse ricerca(
            @AuthenticationPrincipal UUID professionistaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int dimensione,
            @RequestParam(defaultValue = "nome") CampoOrdinamentoPazienti ordinaPer,
            @RequestParam(defaultValue = "asc") DirezioneOrdinamento direzione,
            @RequestParam(required = false) String ricerca,
            @RequestParam(required = false) StatoAccountPaziente statoAccount,
            @RequestParam(required = false) Sesso sesso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascitaDa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascitaA,
            @RequestParam(defaultValue = "false") boolean archiviato) {
        int paginaEffettiva = Math.max(pagina, 0);
        int dimensioneEffettiva = Math.min(Math.max(dimensione, 1), 100);
        Sort.Direction direzioneSort = direzione == DirezioneOrdinamento.desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(paginaEffettiva, dimensioneEffettiva, Sort.by(direzioneSort, ordinaPer.name()));
        CriteriRicercaPazienti criteri = new CriteriRicercaPazienti(ricerca, statoAccount, sesso, dataNascitaDa, dataNascitaA, archiviato);
        Page<Paziente> pagina1 = pazienteService.cerca(professionistaId, criteri, pageable);
        return PazienteListaPaginataResponse.da(pagina1);
    }
```

- [ ] **Step 7: Esegui i test e verifica che passino**

Run: `mvn test -Dtest=PazienteControllerTest` da `backend/`
Expected: PASS

- [ ] **Step 8: Esegui l'intera suite backend**

Run: `mvn test` da `backend/`
Expected: PASS

- [ ] **Step 9: Stage**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/pazienti/CampoOrdinamentoPazienti.java backend/src/main/java/com/hexisnutrition/backend/pazienti/DirezioneOrdinamento.java backend/src/main/java/com/hexisnutrition/backend/pazienti/CriteriRicercaPazienti.java backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteListaPaginataResponse.java backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Task 6: API client — `archiviato`, `archivia()`, `deArchivia()`

**Files:**
- Modify: `frontend-professionisti/src/api/pazienti.ts`
- Modify: `frontend-professionisti/src/api/pazienti.spec.ts`

**Interfaces:**
- Produces: `Paziente.archiviato: boolean`; `archivia(id: string): Promise<void>`; `deArchivia(id: string): Promise<void>`.

- [ ] **Step 1: Scrivi i test che falliscono**

In `pazienti.spec.ts`, aggiorna l'import e la fixture, poi aggiungi i due test.

```ts
import { lista, dettaglio, crea, invita, archivia, deArchivia } from './pazienti'
```

```ts
const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
  archiviato: false,
}
```

Aggiungi in fondo al `describe`, dopo il test `invita`:

```ts
  it('archivia chiama POST /pazienti/{id}/archivia', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await archivia('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/archivia', { method: 'POST' })
  })

  it('deArchivia chiama POST /pazienti/{id}/de-archivia', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await deArchivia('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/de-archivia', { method: 'POST' })
  })
```

- [ ] **Step 2: Esegui i test e verifica che falliscano**

Run: `npx vitest run src/api/pazienti.spec.ts` da `frontend-professionisti/`
Expected: FAIL — `archivia`/`deArchivia` non esportati da `./pazienti`.

- [ ] **Step 3: Implementa**

In `pazienti.ts`, aggiungi `archiviato: boolean` all'interfaccia `Paziente` (dopo `statoAccount`):

```ts
export interface Paziente {
  id: string
  nome: string
  cognome: string
  codiceFiscale: string
  email: string
  telefono: string | null
  dataNascita: string | null
  sesso: string
  lavoro: string | null
  tipoLavoro: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO' | null
  statoAccount: 'MAI_INVITATO' | 'INVITATO' | 'ATTIVO'
  archiviato: boolean
}
```

E aggiungi in fondo al file, dopo `invita(...)`:

```ts
export function archivia(id: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${id}/archivia`, { method: 'POST' })
}

export function deArchivia(id: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${id}/de-archivia`, { method: 'POST' })
}
```

- [ ] **Step 4: Esegui i test e verifica che passino**

Run: `npx vitest run src/api/pazienti.spec.ts` da `frontend-professionisti/`
Expected: PASS

- [ ] **Step 5: Stage**

```bash
git add frontend-professionisti/src/api/pazienti.ts frontend-professionisti/src/api/pazienti.spec.ts
```

---

## Task 7: API client — `cerca()` paginata/filtrata

**Files:**
- Modify: `frontend-professionisti/src/api/pazienti.ts`
- Modify: `frontend-professionisti/src/api/pazienti.spec.ts`

**Interfaces:**
- Consumes: `Paziente` (Task 6).
- Produces: `CriteriRicercaPazienti` (interfaccia TS), `PaginaPazienti` (interfaccia TS), `cerca(criteri?: CriteriRicercaPazienti): Promise<PaginaPazienti>`.

- [ ] **Step 1: Scrivi i test che falliscono**

In `pazienti.spec.ts`, aggiorna l'import:

```ts
import { lista, dettaglio, crea, invita, archivia, deArchivia, cerca } from './pazienti'
```

Aggiungi in fondo al `describe`:

```ts
  it('cerca chiama GET /pazienti/ricerca senza parametri se non specificati', async () => {
    const paginaEsempio = { contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1 }
    vi.mocked(apiRequest).mockResolvedValue(paginaEsempio)

    const risultato = await cerca()

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/ricerca')
    expect(risultato).toEqual(paginaEsempio)
  })

  it('cerca costruisce la query string con tutti i filtri passati', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ contenuto: [], paginaCorrente: 1, dimensionePagina: 10, totaleElementi: 0, totalePagine: 0 })

    await cerca({
      pagina: 1, dimensione: 10, ordinaPer: 'dataNascita', direzione: 'desc',
      ricerca: 'marco', statoAccount: 'ATTIVO', sesso: 'M',
      dataNascitaDa: '1990-01-01', dataNascitaA: '2000-01-01', archiviato: true,
    })

    expect(apiRequest).toHaveBeenCalledWith(
      '/pazienti/ricerca?pagina=1&dimensione=10&ordinaPer=dataNascita&direzione=desc&ricerca=marco' +
      '&statoAccount=ATTIVO&sesso=M&dataNascitaDa=1990-01-01&dataNascitaA=2000-01-01&archiviato=true',
    )
  })
```

- [ ] **Step 2: Esegui i test e verifica che falliscano**

Run: `npx vitest run src/api/pazienti.spec.ts` da `frontend-professionisti/`
Expected: FAIL — `cerca` non esportata da `./pazienti`.

- [ ] **Step 3: Implementa**

In `pazienti.ts`, aggiungi dopo l'interfaccia `Paziente`:

```ts
export interface CriteriRicercaPazienti {
  pagina?: number
  dimensione?: number
  ordinaPer?: 'nome' | 'cognome' | 'dataNascita' | 'statoAccount'
  direzione?: 'asc' | 'desc'
  ricerca?: string
  statoAccount?: Paziente['statoAccount']
  sesso?: 'M' | 'F' | 'ALTRO'
  dataNascitaDa?: string
  dataNascitaA?: string
  archiviato?: boolean
}

export interface PaginaPazienti {
  contenuto: Paziente[]
  paginaCorrente: number
  dimensionePagina: number
  totaleElementi: number
  totalePagine: number
}
```

E in fondo al file:

```ts
export function cerca(criteri: CriteriRicercaPazienti = {}): Promise<PaginaPazienti> {
  const parametri = new URLSearchParams()
  if (criteri.pagina !== undefined) parametri.set('pagina', String(criteri.pagina))
  if (criteri.dimensione !== undefined) parametri.set('dimensione', String(criteri.dimensione))
  if (criteri.ordinaPer) parametri.set('ordinaPer', criteri.ordinaPer)
  if (criteri.direzione) parametri.set('direzione', criteri.direzione)
  if (criteri.ricerca) parametri.set('ricerca', criteri.ricerca)
  if (criteri.statoAccount) parametri.set('statoAccount', criteri.statoAccount)
  if (criteri.sesso) parametri.set('sesso', criteri.sesso)
  if (criteri.dataNascitaDa) parametri.set('dataNascitaDa', criteri.dataNascitaDa)
  if (criteri.dataNascitaA) parametri.set('dataNascitaA', criteri.dataNascitaA)
  if (criteri.archiviato !== undefined) parametri.set('archiviato', String(criteri.archiviato))

  const query = parametri.toString()
  return apiRequest<PaginaPazienti>(`/pazienti/ricerca${query ? `?${query}` : ''}`)
}
```

- [ ] **Step 4: Esegui i test e verifica che passino**

Run: `npx vitest run src/api/pazienti.spec.ts` da `frontend-professionisti/`
Expected: PASS

- [ ] **Step 5: `tsc` pulito**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: nessun errore

- [ ] **Step 6: Stage**

```bash
git add frontend-professionisti/src/api/pazienti.ts frontend-professionisti/src/api/pazienti.spec.ts
```

---

## Task 8: `PazienteRigaAzioni.vue` — azioni riga con menu e conferma

**Files:**
- Create: `frontend-professionisti/src/components/pazienti/PazienteRigaAzioni.vue`
- Create: `frontend-professionisti/src/components/pazienti/PazienteRigaAzioni.spec.ts`
- Create (via CLI): `frontend-professionisti/src/components/ui/alert-dialog/*`

**Interfaces:**
- Consumes: `Paziente` (Task 6).
- Produces: componente `PazienteRigaAzioni` — props `{ paziente: Paziente; mostraArchiviati: boolean }`, emits `invita(paziente)`, `archivia(paziente)`, `deArchivia(paziente)`.

- [ ] **Step 1: Genera il componente `alert-dialog` di shadcn-vue**

Run (da `frontend-professionisti/`): `npx shadcn-vue@latest add alert-dialog`

Verifica che siano stati creati file sotto `src/components/ui/alert-dialog/` con un `index.ts` che esporta almeno: `AlertDialog`, `AlertDialogContent`, `AlertDialogHeader`, `AlertDialogTitle`, `AlertDialogDescription`, `AlertDialogFooter`, `AlertDialogCancel`, `AlertDialogAction` (stessi nomi del pattern React di shadcn/ui, mantenuti identici nel port Vue). Se un nome esportato differisse, adatta gli import del componente allo Step 3 di conseguenza.

- [ ] **Step 2: Scrivi i test che falliscono**

`frontend-professionisti/src/components/pazienti/PazienteRigaAzioni.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteRigaAzioni from './PazienteRigaAzioni.vue'
import type { Paziente } from '@/api/pazienti'

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } }],
  })
}

const pazienteEsempio: Paziente = {
  id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null,
  statoAccount: 'MAI_INVITATO', archiviato: false,
}

async function montaComponente(paziente: Paziente, mostraArchiviati = false) {
  const router = creaRouter()
  router.push('/')
  await router.isReady()
  return mount(PazienteRigaAzioni, {
    props: { paziente, mostraArchiviati },
    attachTo: document.body,
    global: { plugins: [router] },
  })
}

async function apriMenu(wrapper: Awaited<ReturnType<typeof montaComponente>>) {
  await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
  await flushPromises()
}

describe('PazienteRigaAzioni', () => {
  it('mostra il bottone Invita per un paziente mai invitato', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    expect(wrapper.text()).toContain('Invita')
    wrapper.unmount()
  })

  it('emette invita al click del bottone', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    const pulsante = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsante?.trigger('click')
    expect(wrapper.emitted('invita')).toEqual([[pazienteEsempio]])
    wrapper.unmount()
  })

  it('non mostra il bottone Invita in vista archiviati', async () => {
    const wrapper = await montaComponente({ ...pazienteEsempio, archiviato: true }, true)
    expect(wrapper.text()).not.toContain('Invita')
    wrapper.unmount()
  })

  it('apre il menu e mostra "Nuova visita" disabilitata', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    await apriMenu(wrapper)

    expect(document.body.textContent).toContain('Nuova visita')
    expect(document.body.textContent).toContain('Presto disponibile')
    wrapper.unmount()
  })

  it('chiede conferma prima di archiviare e non emette nulla finché non si conferma', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    await apriMenu(wrapper)
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()

    expect(wrapper.emitted('archivia')).toBeUndefined()
    expect(document.body.textContent).toContain('Archiviare Luca Verdi?')
    wrapper.unmount()
  })

  it('emette archivia dopo la conferma', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    await apriMenu(wrapper)
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(wrapper.emitted('archivia')).toEqual([[pazienteEsempio]])
    wrapper.unmount()
  })

  it('mostra "De-archivia paziente" ed emette deArchivia in vista archiviati', async () => {
    const pazienteArchiviato = { ...pazienteEsempio, archiviato: true }
    const wrapper = await montaComponente(pazienteArchiviato, true)
    await apriMenu(wrapper)
    document.querySelector<HTMLElement>('[data-test="menu-de-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(wrapper.emitted('deArchivia')).toEqual([[pazienteArchiviato]])
    wrapper.unmount()
  })
})
```

- [ ] **Step 3: Esegui i test e verifica che falliscano**

Run: `npx vitest run src/components/pazienti/PazienteRigaAzioni.spec.ts` da `frontend-professionisti/`
Expected: FAIL — il file `PazienteRigaAzioni.vue` non esiste ancora.

- [ ] **Step 4: Implementa il componente**

`frontend-professionisti/src/components/pazienti/PazienteRigaAzioni.vue`:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import type { Paziente } from '@/api/pazienti'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from '@/components/ui/alert-dialog'
import { FolderOpen, MoreHorizontal } from '@lucide/vue'

const props = defineProps<{
  paziente: Paziente
  mostraArchiviati: boolean
}>()

const emit = defineEmits<{
  invita: [paziente: Paziente]
  archivia: [paziente: Paziente]
  deArchivia: [paziente: Paziente]
}>()

const confermaAperta = ref(false)

function apriConferma() {
  confermaAperta.value = true
}

function confermaAzione() {
  if (props.mostraArchiviati) {
    emit('deArchivia', props.paziente)
  } else {
    emit('archivia', props.paziente)
  }
  confermaAperta.value = false
}

function etichettaAzione(paziente: Paziente) {
  if (paziente.statoAccount === 'MAI_INVITATO') return 'Invita'
  if (paziente.statoAccount === 'INVITATO') return 'Reinvia invito'
  return null
}
</script>

<template>
  <div class="flex items-center justify-end gap-1">
    <Button as-child variant="ghost" size="icon" aria-label="Apri cartella" title="Apri cartella">
      <router-link :to="`/pazienti/${paziente.id}`">
        <FolderOpen :size="15" />
      </router-link>
    </Button>

    <Button
      v-if="!mostraArchiviati && etichettaAzione(paziente)"
      type="button"
      variant="link"
      size="sm"
      @click="emit('invita', paziente)"
    >
      {{ etichettaAzione(paziente) }}
    </Button>

    <DropdownMenu>
      <DropdownMenuTrigger as-child>
        <Button type="button" variant="ghost" size="icon" aria-label="Altre opzioni" title="Altre opzioni">
          <MoreHorizontal :size="15" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" class="w-52">
        <DropdownMenuItem disabled data-test="menu-nuova-visita" class="flex flex-col items-start gap-0">
          <span>Nuova visita</span>
          <span class="text-xs text-(--fg4)">Presto disponibile</span>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          v-if="!mostraArchiviati"
          data-test="menu-archivia"
          class="text-(--danger) focus:text-(--danger)"
          @click="apriConferma"
        >
          Archivia paziente
        </DropdownMenuItem>
        <DropdownMenuItem v-else data-test="menu-de-archivia" @click="apriConferma">
          De-archivia paziente
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>

    <AlertDialog v-model:open="confermaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {{ mostraArchiviati ? 'De-archiviare' : 'Archiviare' }} {{ paziente.nome }} {{ paziente.cognome }}?
          </AlertDialogTitle>
          <AlertDialogDescription>
            {{
              mostraArchiviati
                ? 'Il paziente tornerà visibile nella lista pazienti attivi.'
                : 'Il paziente non comparirà più nella lista pazienti attivi. Potrai de-archiviarlo in qualsiasi momento.'
            }}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel data-test="conferma-annulla">Annulla</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-conferma" @click="confermaAzione">Conferma</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
```

- [ ] **Step 5: Esegui i test e verifica che passino**

Run: `npx vitest run src/components/pazienti/PazienteRigaAzioni.spec.ts` da `frontend-professionisti/`
Expected: PASS

Se un test fallisce per un nome di componente `alert-dialog` diverso da quello atteso, apri `src/components/ui/alert-dialog/index.ts` generato allo Step 1 e correggi gli import nel componente (Step 4) di conseguenza, poi rilancia i test.

- [ ] **Step 6: `tsc` pulito**

Run: `npx tsc --noEmit` da `frontend-professionisti/`
Expected: nessun errore

- [ ] **Step 7: Stage**

```bash
git add frontend-professionisti/src/components/pazienti/PazienteRigaAzioni.vue frontend-professionisti/src/components/pazienti/PazienteRigaAzioni.spec.ts frontend-professionisti/src/components/ui/alert-dialog frontend-professionisti/components.json
```

(l'ultimo percorso copre eventuali file di configurazione toccati dal generatore shadcn-vue — verifica con `git status` cosa è stato effettivamente modificato/creato e aggiungi solo quello).

---

## Task 9: Riscrittura `PazientiListView.vue`

**Files:**
- Modify: `frontend-professionisti/src/views/pazienti/PazientiListView.vue`
- Modify: `frontend-professionisti/src/views/pazienti/PazientiListView.spec.ts`

**Interfaces:**
- Consumes: `cerca`, `invita`, `archivia`, `deArchivia`, `Paziente`, `PaginaPazienti`, `CriteriRicercaPazienti` (Task 6-7); `PazienteRigaAzioni` (Task 8); `calcolaEta` (`@/utils/data`, esistente).

- [ ] **Step 1: Scrivi i test che falliscono (riscrittura completa dello spec)**

Sostituisci interamente `frontend-professionisti/src/views/pazienti/PazientiListView.spec.ts` con:

```ts
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazientiListView from './PazientiListView.vue'
import { DatePicker } from '@/components/ui/date-picker'
import * as pazientiApi from '@/api/pazienti'
import type { PaginaPazienti } from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: PazientiListView },
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: { template: '<div/>' } },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
  telefono: '3331234567', dataNascita: '1990-01-01', sesso: 'M', lavoro: 'Impiegato', tipoLavoro: 'ATTIVO' as const,
  statoAccount: 'MAI_INVITATO' as const, archiviato: false,
}

function paginaCon(contenuto: typeof pazienteEsempio[]): PaginaPazienti {
  return { contenuto, paginaCorrente: 0, dimensionePagina: 20, totaleElementi: contenuto.length, totalePagine: 1 }
}

async function montaView(attachToBody = false) {
  const router = creaRouter()
  router.push('/pazienti')
  await router.isReady()
  const wrapper = mount(PazientiListView, {
    attachTo: attachToBody ? document.body : undefined,
    global: { plugins: [router, createTestingPinia()] },
  })
  await flushPromises()
  return wrapper
}

afterEach(() => {
  vi.useRealTimers()
})

describe('PazientiListView', () => {
  it('mostra i pazienti caricati dal backend con le colonne reali', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('luca@example.com · RSSMRA80A01H501U')
    expect(wrapper.text()).toContain('3331234567')
    expect(wrapper.text()).toContain('Impiegato')
    expect(wrapper.text()).toContain('Invita')
  })

  it('chiama cerca() invece di lista() e non richiede lista completa', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    await montaView()

    expect(pazientiApi.cerca).toHaveBeenCalled()
    expect(pazientiApi.lista).not.toHaveBeenCalled()
  })

  it('la ricerca testuale è debounced e richiama cerca() con il testo digitato', async () => {
    vi.useFakeTimers()
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()

    await wrapper.find('input[type="search"]').setValue('marco')
    expect(pazientiApi.cerca).not.toHaveBeenCalled()

    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ ricerca: 'marco', pagina: 0 }))
  })

  it('il click su un chip di stato richiama cerca() con statoAccount filtrato', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()

    const chipAttivo = wrapper.findAll('button').find((b) => b.text() === 'Attivo')
    await chipAttivo?.trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ statoAccount: 'ATTIVO', pagina: 0 }))
  })

  it('i filtri avanzati (sesso, intervallo date) richiamano cerca() con i parametri', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()

    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()
    vi.mocked(pazientiApi.cerca).mockClear()

    const pickerDataDa = wrapper.findAllComponents(DatePicker).find((c) => c.props('id') === 'data-nascita-da')
    await pickerDataDa?.vm.$emit('update:modelValue', '1990-01-01')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ dataNascitaDa: '1990-01-01', pagina: 0 }))
  })

  it('il toggle "Mostra pazienti archiviati" richiama cerca() con archiviato:true e nasconde Invita', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()

    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([{ ...pazienteEsempio, archiviato: true }]))
    await wrapper.find('#filtro-mostra-archiviati').trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ archiviato: true, pagina: 0 }))
    expect(wrapper.text()).not.toContain('Invita')
  })

  it('"Pulisci filtri" resetta ricerca/filtri e richiama cerca() senza criteri attivi', async () => {
    vi.useFakeTimers()
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    await wrapper.find('input[type="search"]').setValue('marco')
    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()

    const pulisci = wrapper.findAll('button').find((b) => b.text() === 'Pulisci filtri')
    await pulisci?.trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({
      ricerca: undefined, statoAccount: undefined, sesso: undefined,
      dataNascitaDa: undefined, dataNascitaA: undefined, archiviato: false, pagina: 0,
    }))
  })

  it('il click su un header ordinabile richiama cerca() con ordinaPer, un secondo click inverte la direzione', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()

    const headerPaziente = wrapper.findAll('th button').find((b) => b.text().includes('Paziente'))
    await headerPaziente?.trigger('click')
    await flushPromises()
    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({ ordinaPer: 'nome', direzione: 'asc' }))

    await headerPaziente?.trigger('click')
    await flushPromises()
    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({ ordinaPer: 'nome', direzione: 'desc' }))
  })

  it('mostra lo stato vuoto "primo paziente" se non ci sono pazienti e nessun filtro attivo', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([]))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Nessun paziente, per ora')
  })

  it('mostra lo stato vuoto "per filtro" se la ricerca non trova risultati', async () => {
    vi.useFakeTimers()
    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()

    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([]))
    await wrapper.find('input[type="search"]').setValue('nessuna-corrispondenza-xyz')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.text()).toContain('Nessun paziente con questi filtri')
  })

  it('mostra un errore con bottone Riprova se il caricamento fallisce, e Riprova richiama cerca()', async () => {
    vi.mocked(pazientiApi.cerca).mockRejectedValue(new Error('500'))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Non è stato possibile caricare i pazienti.')

    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([pazienteEsempio]))
    const riprova = wrapper.findAll('button').find((b) => b.text() === 'Riprova')
    await riprova?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
  })

  it('invita un paziente e ne aggiorna lo stato in tabella', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const wrapper = await montaView()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('INVITATO')
  })

  it('mostra un errore se l\'invito fallisce e non aggiorna lo stato del paziente', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([{ ...pazienteEsempio, statoAccount: 'MAI_INVITATO' }]))
    vi.mocked(pazientiApi.invita).mockRejectedValue(new Error('409'))
    const wrapper = await montaView()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile inviare l\'invito')
    expect(wrapper.findAll('button').some((b) => b.text() === 'Invita')).toBe(true)
    expect(wrapper.text()).not.toContain('Reinvia invito')
  })

  it('archivia un paziente dal menu riga e ricarica la lista', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    vi.mocked(pazientiApi.archivia).mockResolvedValue(undefined)
    const wrapper = await montaView(true)

    await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(pazientiApi.archivia).toHaveBeenCalledWith('1')
    expect(pazientiApi.cerca).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('de-archivia un paziente dal menu riga in vista archiviati e ricarica la lista', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([{ ...pazienteEsempio, archiviato: true }]))
    vi.mocked(pazientiApi.deArchivia).mockResolvedValue(undefined)
    const wrapper = await montaView(true)
    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()
    await wrapper.find('#filtro-mostra-archiviati').trigger('click')
    await flushPromises()

    await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="menu-de-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(pazientiApi.deArchivia).toHaveBeenCalledWith('1')
    wrapper.unmount()
  })

  it('i bottoni di paginazione richiamano cerca() con la pagina aggiornata', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue({
      contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 40, totalePagine: 2,
    })
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()
    vi.mocked(pazientiApi.cerca).mockResolvedValue({
      contenuto: [pazienteEsempio], paginaCorrente: 1, dimensionePagina: 20, totaleElementi: 40, totalePagine: 2,
    })

    const successivo = wrapper.findAll('button').find((b) => b.text() === 'Successivo')
    await successivo?.trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ pagina: 1 }))
  })
})
```

- [ ] **Step 2: Esegui i test e verifica che falliscano**

Run: `npx vitest run src/views/pazienti/PazientiListView.spec.ts` da `frontend-professionisti/`
Expected: FAIL — la view attuale usa ancora `lista()` e non ha filtri/paginazione/menu riga.

- [ ] **Step 3: Riscrivi `PazientiListView.vue`**

Sostituisci interamente il file con:

```vue
<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import {
  cerca,
  invita,
  archivia,
  deArchivia,
  type Paziente,
  type PaginaPazienti,
  type CriteriRicercaPazienti,
} from '@/api/pazienti'
import PazienteRigaAzioni from '@/components/pazienti/PazienteRigaAzioni.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { DatePicker } from '@/components/ui/date-picker'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Plus, ChevronRight, ArrowUp, ArrowDown } from '@lucide/vue'
import { calcolaEta } from '@/utils/data'

type CampoOrdinamento = NonNullable<CriteriRicercaPazienti['ordinaPer']>

const chipStatoAccount: { valore: 'TUTTI' | Paziente['statoAccount']; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'MAI_INVITATO', etichetta: 'Mai invitato' },
  { valore: 'INVITATO', etichetta: 'Invitato' },
  { valore: 'ATTIVO', etichetta: 'Attivo' },
]

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const statoAccount = ref<'TUTTI' | Paziente['statoAccount']>('TUTTI')
const sesso = ref<'TUTTI' | 'M' | 'F' | 'ALTRO'>('TUTTI')
const dataNascitaDa = ref('')
const dataNascitaA = ref('')
const mostraArchiviati = ref(false)
const filtriAvanzatiAperti = ref(false)
const pagina = ref(0)
const ordinaPer = ref<CampoOrdinamento>('nome')
const direzione = ref<'asc' | 'desc'>('asc')

const paginaDati = ref<PaginaPazienti | null>(null)
const caricamentoIniziale = ref(true)
const aggiornamentoInCorso = ref(false)
const errore = ref(false)
const invitoInCorsoId = ref<string | null>(null)
const erroreInvito = ref(false)

let debounceHandle: ReturnType<typeof setTimeout> | undefined

watch(ricercaInput, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => {
    ricercaEffettiva.value = valore
    pagina.value = 0
  }, 300)
})

onUnmounted(() => clearTimeout(debounceHandle))

const filtriAvanzatiAttivi = computed(() =>
  [sesso.value !== 'TUTTI', !!dataNascitaDa.value, !!dataNascitaA.value, mostraArchiviati.value].filter(Boolean).length,
)

const filtriAttivi = computed(() =>
  ricercaEffettiva.value.trim() !== '' || statoAccount.value !== 'TUTTI' || filtriAvanzatiAttivi.value > 0,
)

function criteriCorrenti(): CriteriRicercaPazienti {
  return {
    pagina: pagina.value,
    dimensione: 20,
    ordinaPer: ordinaPer.value,
    direzione: direzione.value,
    ricerca: ricercaEffettiva.value.trim() || undefined,
    statoAccount: statoAccount.value === 'TUTTI' ? undefined : statoAccount.value,
    sesso: sesso.value === 'TUTTI' ? undefined : sesso.value,
    dataNascitaDa: dataNascitaDa.value || undefined,
    dataNascitaA: dataNascitaA.value || undefined,
    archiviato: mostraArchiviati.value,
  }
}

async function carica() {
  if (paginaDati.value === null) {
    caricamentoIniziale.value = true
  } else {
    aggiornamentoInCorso.value = true
  }
  errore.value = false
  try {
    paginaDati.value = await cerca(criteriCorrenti())
  } catch {
    errore.value = true
  } finally {
    caricamentoIniziale.value = false
    aggiornamentoInCorso.value = false
  }
}

watch(
  [ricercaEffettiva, statoAccount, sesso, dataNascitaDa, dataNascitaA, mostraArchiviati, pagina, ordinaPer, direzione],
  carica,
)

onMounted(carica)

function selezionaStato(valore: typeof statoAccount.value) {
  statoAccount.value = valore
  pagina.value = 0
}

function onSessoChange(valore: string) {
  sesso.value = valore as typeof sesso.value
  pagina.value = 0
}

function onDataNascitaDaChange(valore: string) {
  dataNascitaDa.value = valore
  pagina.value = 0
}

function onDataNascitaAChange(valore: string) {
  dataNascitaA.value = valore
  pagina.value = 0
}

function onMostraArchiviatiChange(valore: boolean | 'indeterminate') {
  mostraArchiviati.value = valore === true
  pagina.value = 0
}

function ordina(campo: CampoOrdinamento) {
  if (ordinaPer.value === campo) {
    direzione.value = direzione.value === 'asc' ? 'desc' : 'asc'
  } else {
    ordinaPer.value = campo
    direzione.value = 'asc'
  }
  pagina.value = 0
}

function pulisciFiltri() {
  clearTimeout(debounceHandle)
  ricercaInput.value = ''
  ricercaEffettiva.value = ''
  statoAccount.value = 'TUTTI'
  sesso.value = 'TUTTI'
  dataNascitaDa.value = ''
  dataNascitaA.value = ''
  mostraArchiviati.value = false
  pagina.value = 0
}

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}

function paginaSuccessiva() {
  if (paginaDati.value && pagina.value < paginaDati.value.totalePagine - 1) pagina.value += 1
}

async function onInvita(paziente: Paziente) {
  invitoInCorsoId.value = paziente.id
  erroreInvito.value = false
  try {
    await invita(paziente.id)
    paziente.statoAccount = 'INVITATO'
  } catch {
    erroreInvito.value = true
  } finally {
    invitoInCorsoId.value = null
  }
}

async function onArchivia(paziente: Paziente) {
  await archivia(paziente.id)
  await carica()
}

async function onDeArchivia(paziente: Paziente) {
  await deArchivia(paziente.id)
  await carica()
}

const conteggioTesto = computed(() => {
  if (!paginaDati.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = paginaDati.value
  if (totaleElementi === 0) return mostraArchiviati.value ? 'Nessun paziente archiviato' : 'Nessun paziente'
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} pazienti`
})
</script>

<template>
  <AppShell>
    <div class="mb-6 flex items-center justify-between">
      <h1 class="font-heading text-3xl italic text-(--fg)">Pazienti</h1>
      <Button as-child size="lg" class="hover:bg-primary/80">
        <router-link to="/pazienti/nuovo"><Plus :size="16" /> Nuovo paziente</router-link>
      </Button>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <div class="flex flex-wrap items-center gap-2.5">
        <Input
          v-model="ricercaInput"
          type="search"
          placeholder="Filtra per nome, email o codice fiscale…"
          class="min-w-70 flex-1"
        />
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="chip in chipStatoAccount"
            :key="chip.valore"
            type="button"
            class="rounded-full border px-3 py-1.5 text-xs font-bold transition-colors"
            :class="statoAccount === chip.valore
              ? 'border-(--green) bg-(--green) text-white'
              : 'border-(--bd2) bg-(--surf) text-(--fg2) hover:border-(--sage)'"
            @click="selezionaStato(chip.valore)"
          >
            {{ chip.etichetta }}
          </button>
        </div>
      </div>

      <div class="my-3 h-px bg-(--div)" />

      <button
        type="button"
        class="flex items-center gap-2 text-xs font-bold text-(--fg2) hover:text-(--fg)"
        @click="filtriAvanzatiAperti = !filtriAvanzatiAperti"
      >
        <ChevronRight :size="12" class="transition-transform" :class="{ 'rotate-90': filtriAvanzatiAperti }" />
        Filtri avanzati
        <Badge :variant="filtriAvanzatiAttivi > 0 ? 'default' : 'secondary'">
          {{ filtriAvanzatiAttivi > 0 ? `${filtriAvanzatiAttivi} attivi` : 'nessuno attivo' }}
        </Badge>
      </button>

      <div v-if="filtriAvanzatiAperti" class="mt-3 grid grid-cols-[repeat(auto-fit,minmax(170px,1fr))] items-end gap-2.5">
        <div class="flex flex-col gap-1.5">
          <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Sesso</Label>
          <Select :model-value="sesso" @update:model-value="onSessoChange">
            <SelectTrigger class="w-full"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="TUTTI">Tutti</SelectItem>
              <SelectItem value="M">Maschio</SelectItem>
              <SelectItem value="F">Femmina</SelectItem>
              <SelectItem value="ALTRO">Altro</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Data di nascita da</Label>
          <DatePicker id="data-nascita-da" :model-value="dataNascitaDa" @update:model-value="onDataNascitaDaChange" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Data di nascita a</Label>
          <DatePicker id="data-nascita-a" :model-value="dataNascitaA" @update:model-value="onDataNascitaAChange" />
        </div>
        <label class="flex items-center gap-2 pb-1.5">
          <Checkbox
            id="filtro-mostra-archiviati"
            :model-value="mostraArchiviati"
            @update:model-value="onMostraArchiviatiChange"
          />
          <span class="text-sm text-(--fg2)">Mostra pazienti archiviati</span>
        </label>
        <button
          type="button"
          class="rounded-lg border border-dashed border-(--dash) px-3 py-2 text-xs font-bold text-(--fg3) hover:border-(--fg4) hover:text-(--fg)"
          @click="pulisciFiltri"
        >
          Pulisci filtri
        </button>
      </div>
    </section>

    <p v-if="erroreInvito" class="mb-3.5 text-sm text-(--danger)">Non è stato possibile inviare l'invito.</p>

    <section class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf)">
      <div v-if="errore" class="flex flex-col items-center gap-3 p-14 text-center">
        <p class="text-(--danger)">Non è stato possibile caricare i pazienti.</p>
        <Button type="button" variant="outline" @click="carica">Riprova</Button>
      </div>

      <div v-else-if="caricamentoIniziale" class="flex flex-col gap-2 p-4">
        <div v-for="n in 6" :key="n" class="h-9 animate-pulse rounded-lg bg-(--hover)" />
      </div>

      <template v-else-if="paginaDati && paginaDati.contenuto.length > 0">
        <div class="overflow-x-auto" :class="{ 'pointer-events-none opacity-60': aggiornamentoInCorso }">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 font-bold" @click="ordina('nome')">
                    Paziente
                    <component :is="direzione === 'asc' ? ArrowUp : ArrowDown" v-if="ordinaPer === 'nome'" :size="12" />
                  </button>
                </TableHead>
                <TableHead>Contatto</TableHead>
                <TableHead>Attività</TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 font-bold" @click="ordina('dataNascita')">
                    Età
                    <component :is="direzione === 'asc' ? ArrowUp : ArrowDown" v-if="ordinaPer === 'dataNascita'" :size="12" />
                  </button>
                </TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 font-bold" @click="ordina('statoAccount')">
                    Stato account
                    <component :is="direzione === 'asc' ? ArrowUp : ArrowDown" v-if="ordinaPer === 'statoAccount'" :size="12" />
                  </button>
                </TableHead>
                <TableHead class="text-right">Azioni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="paziente in paginaDati.contenuto" :key="paziente.id">
                <TableCell>
                  <div class="flex items-center gap-2.5">
                    <span class="flex h-9 w-9 items-center justify-center rounded-full bg-(--mint) font-heading font-semibold text-(--green)">
                      {{ paziente.nome[0] }}{{ paziente.cognome[0] }}
                    </span>
                    <div>
                      <router-link :to="`/pazienti/${paziente.id}`" class="font-heading font-semibold text-(--fg)">
                        {{ paziente.nome }} {{ paziente.cognome }}
                      </router-link>
                      <div class="text-xs text-(--fg4)">{{ paziente.email }} · {{ paziente.codiceFiscale }}</div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>{{ paziente.telefono ?? '—' }}</TableCell>
                <TableCell>
                  {{ paziente.lavoro ?? '—' }}
                  <span v-if="paziente.tipoLavoro" class="ml-1.5 text-xs text-(--fg4)">({{ paziente.tipoLavoro }})</span>
                </TableCell>
                <TableCell>{{ paziente.dataNascita ? (calcolaEta(paziente.dataNascita) ?? '—') : '—' }}</TableCell>
                <TableCell><Badge variant="secondary">{{ paziente.statoAccount }}</Badge></TableCell>
                <TableCell class="text-right">
                  <PazienteRigaAzioni
                    :paziente="paziente"
                    :mostra-archiviati="mostraArchiviati"
                    @invita="onInvita"
                    @archivia="onArchivia"
                    @de-archivia="onDeArchivia"
                  />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </template>

      <div v-else-if="!filtriAttivi" class="flex flex-col items-center gap-2 p-16 text-center">
        <p class="font-heading text-lg italic">Nessun paziente, per ora</p>
        <Button as-child><router-link to="/pazienti/nuovo">Nuovo paziente</router-link></Button>
      </div>
      <div v-else class="flex flex-col items-center gap-2 p-16 text-center">
        <p class="font-bold">Nessun paziente con questi filtri</p>
        <Button type="button" variant="outline" @click="pulisciFiltri">Pulisci filtri</Button>
      </div>

      <div v-if="paginaDati && !errore" class="flex items-center justify-between gap-3 border-t border-(--div) bg-(--soft) px-4.5 py-3">
        <span class="text-xs text-(--fg3)">{{ conteggioTesto }}</span>
        <div class="flex gap-2">
          <Button type="button" variant="outline" size="sm" :disabled="pagina === 0" @click="paginaPrecedente">Precedente</Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            :disabled="!paginaDati || pagina >= paginaDati.totalePagine - 1"
            @click="paginaSuccessiva"
          >
            Successivo
          </Button>
        </div>
      </div>
    </section>
  </AppShell>
</template>
```

- [ ] **Step 4: Esegui i test e verifica che passino**

Run: `npx vitest run src/views/pazienti/PazientiListView.spec.ts` da `frontend-professionisti/`
Expected: PASS

Se un test fallisce per un selettore (es. `#filtro-mostra-archiviati` sul contenitore invece che sull'input reale del `Checkbox`), ispeziona l'HTML effettivamente renderizzato dal `Checkbox`/`DatePicker` di shadcn-vue e aggiusta il selettore nel test — non il comportamento del componente.

- [ ] **Step 5: Esegui l'intera suite frontend e `tsc`**

Run: `npx vitest run` e `npx tsc --noEmit` da `frontend-professionisti/`
Expected: PASS, nessun errore

- [ ] **Step 6: Stage**

```bash
git add frontend-professionisti/src/views/pazienti/PazientiListView.vue frontend-professionisti/src/views/pazienti/PazientiListView.spec.ts
```

---

## Task 10: Documentazione — `wiki/api-contracts.md`

**Files:**
- Modify: `wiki/api-contracts.md`

**Interfaces:**
- Nessuna (task di documentazione).

- [ ] **Step 1: Aggiungi le tre nuove righe alla tabella degli endpoint**

Nella tabella di `wiki/api-contracts.md`, dopo la riga `| GET | \`/pazienti\` | ... |`, aggiungi:

```markdown
| GET | `/pazienti/ricerca` | PROFESSIONISTA | Lista pazienti paginata/filtrata/ordinata (`pagina`, `dimensione`, `ordinaPer`, `direzione`, `ricerca`, `statoAccount`, `sesso`, `dataNascitaDa`/`dataNascitaA`, `archiviato`) — esclude i pazienti archiviati per default, `archiviato=true` mostra solo quelli |
```

E dopo la riga `| POST | \`/pazienti/{id}/invito\` | ... |`, aggiungi:

```markdown
| POST | `/pazienti/{id}/archivia` | PROFESSIONISTA | Archivia (soft-delete logico) il paziente, 204; idempotente |
| POST | `/pazienti/{id}/de-archivia` | PROFESSIONISTA | De-archivia il paziente, 204; idempotente |
```

Aggiorna inoltre la riga descrittiva di `POST /pazienti/{id}/invito` aggiungendo la nuova condizione di errore, e aggiorna il paragrafo dopo la tabella con una nota sulla paginazione. Sostituisci la riga:

```markdown
| POST | `/pazienti/{id}/invito` | PROFESSIONISTA | Genera token invito e invia email; 409 se il paziente è già ATTIVO |
```

con:

```markdown
| POST | `/pazienti/{id}/invito` | PROFESSIONISTA | Genera token invito e invia email; 409 se il paziente è già ATTIVO, 400 se il paziente è archiviato |
```

Aggiungi infine, come nuovo paragrafo dopo quello che descrive `POST /pazienti`:

```markdown
`GET /pazienti/ricerca` è separato da `GET /pazienti` (che resta la lista completa non paginata, usata dalla dashboard): risponde con `{contenuto, paginaCorrente, dimensionePagina, totaleElementi, totalePagine}`, tutti i filtri sono combinati in AND, `dimensione` è clampata lato server tra 1 e 100.
```

Aggiorna anche il campo `aggiornato:` nel frontmatter in cima al file alla data odierna.

- [ ] **Step 2: Stage**

```bash
git add wiki/api-contracts.md
```

---

## Note per l'esecutore

- I task 1-5 (backend) sono sequenziali (ognuno costruisce sul precedente sullo stesso file `PazienteController.java`/`PazienteService.java`).
- I task 6-9 (frontend) sono sequenziali (Task 9 consuma Task 6-8).
- Il Task 10 (doc) può girare in qualunque momento dopo il Task 5, in parallelo al lavoro frontend.
- Eseguire sempre la suite completa (`mvn test` / `npx vitest run` + `npx tsc --noEmit`) all'ultimo step di ogni task prima di segnalarlo come concluso, non solo il singolo file di test appena scritto.
