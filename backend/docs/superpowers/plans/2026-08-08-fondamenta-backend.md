# Fondamenta (backend) Implementation Plan

> **⚠️ Documento storico — piano già eseguito (8 agosto 2026).** Non seguirlo alla lettera per il setup dei test: la parte su Testcontainers/Docker è superata da [`wiki/decisioni/0004`](../../../../wiki/decisioni/0004-test-su-postgres-locale.md) — i test di integrazione girano ora su un PostgreSQL locale (`hexis_test`), le dipendenze Testcontainers sono state rimosse.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Nota su commit e git:** questo workspace vieta all'agente di eseguire `git commit` di propria iniziativa (vedi `CLAUDE.md` radice del workspace). Ogni task termina quindi con uno step "Stage delle modifiche" (`git add`), **non** con un commit eseguito automaticamente. Segnala ad Andrea quando un task è pronto per essere committato; sarà lui a eseguire `git commit`.

**Goal:** Costruire il backend Spring Boot del sotto-progetto "Fondamenta": autenticazione JWT con ruoli, anagrafica paziente, invito paziente via email, reset password self-service.

**Architecture:** Applicazione Spring Boot modulare per package-di-dominio (`auth`, `professionisti`, `pazienti`, `inviti`, `email`, `support`), un solo sistema di autenticazione JWT stateless condiviso tra i due frontend, PostgreSQL con migrazioni Flyway, invio email tramite Resend dietro l'interfaccia `EmailSender`. Nessun endpoint di self-signup: gli account professionista sono creati direttamente nel database, fuori da questo codice.

**Tech Stack:** Java 21, Spring Boot 3.3.4 (Maven), Spring Web, Spring Data JPA, Spring Security 6, Flyway, PostgreSQL, jjwt 0.12.6, JUnit 5 + Testcontainers (postgres:16-alpine), AssertJ.

## Global Constraints

- Package base: `com.hexisnutrition.backend`.
- Build tool: Maven. Java 21. Spring Boot 3.3.4 (vedi [ADR 0001](../../../wiki/decisioni/0001-stack-tecnologico.md)).
- Autenticazione: un solo sistema JWT con claim `ruolo` (`PROFESSIONISTA` / `PAZIENTE`), vedi [ADR 0002](../../../wiki/decisioni/0002-autenticazione-e-onboarding.md). Nessun endpoint di registrazione/self-signup per i professionisti in questo piano.
- Email: Resend via HTTP, sempre dietro l'interfaccia `EmailSender` — nessuna dipendenza diretta a Resend fuori dal package `email`.
- Tutte le migrazioni Flyway vivono in `src/main/resources/db/migration/`, naming `V{n}__descrizione.sql`.
- Tutti i test che toccano il database estendono `AbstractIntegrationTest` (Testcontainers PostgreSQL) — nessun test deve dipendere da un database installato localmente. Docker deve essere in esecuzione per lanciare i test.
- Nessun test chiama la vera API di Resend: nei test che esercitano l'invio email si usa `FakeEmailSender` (via `TestEmailConfig`); la logica HTTP di `ResendEmailSender` è verificata separatamente con `MockRestServiceServer`.
- Ogni task termina con `git add` (stage), mai con `git commit` — vedi nota in cima al documento.

---

### Task 1: Bootstrap del progetto e contesto Spring con Testcontainers

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/hexisnutrition/backend/BackendApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/BackendApplicationTest.java`

**Interfaces:**
- Produces: `AbstractIntegrationTest` (classe astratta) — sottoclassi ereditano `protected MockMvc mockMvc` e `protected ObjectMapper objectMapper` già autowired, e un container PostgreSQL condiviso già avviato. Ogni task successivo che ha bisogno di un test con database reale estende questa classe.

- [ ] **Step 1: Scrivere `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.hexisnutrition</groupId>
    <artifactId>backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>backend</name>
    <description>Backend Spring Boot di hexis-nutrition</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>1.20.1</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Scrivere `BackendApplication.java`**

```java
package com.hexisnutrition.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

- [ ] **Step 3: Scrivere `application.yml`**

```yaml
spring:
  application:
    name: backend
  datasource:
    url: jdbc:postgresql://localhost:5432/hexis
    username: hexis
    password: hexis
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
```

- [ ] **Step 4: Scrivere `AbstractIntegrationTest`**

```java
package com.hexisnutrition.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
```

- [ ] **Step 5: Scrivere il test che verifica l'avvio del contesto**

```java
package com.hexisnutrition.backend;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendApplicationTest extends AbstractIntegrationTest {

    @Test
    void ilContestoSiAvviaCorrettamente() {
        assertThat(mockMvc).isNotNull();
    }
}
```

- [ ] **Step 6: Eseguire il test e verificare che passi**

Run: `mvn test -Dtest=BackendApplicationTest` (dalla cartella `backend/`)
Expected: `BUILD SUCCESS`, 1 test eseguito, 0 fallimenti. Richiede Docker in esecuzione (Testcontainers avvia un container Postgres reale).

- [ ] **Step 7: Stage delle modifiche**

```bash
git add pom.xml src/main/java/com/hexisnutrition/backend/BackendApplication.java \
        src/main/resources/application.yml \
        src/test/java/com/hexisnutrition/backend/support/AbstractIntegrationTest.java \
        src/test/java/com/hexisnutrition/backend/BackendApplicationTest.java
```

---

### Task 2: Entità Professionista

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_professionisti.sql`
- Create: `backend/src/main/java/com/hexisnutrition/backend/professionisti/Professionista.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/professionisti/ProfessionistaRepository.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/professionisti/ProfessionistaRepositoryTest.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest` (Task 1).
- Produces: `Professionista(String email, String passwordHash, String nome, String cognome)` costruttore pubblico; `getId(): UUID`, `getEmail(): String`, `getPasswordHash(): String`, `setPasswordHash(String): void`, `getNome(): String`, `getCognome(): String`. `ProfessionistaRepository extends JpaRepository<Professionista, UUID>` con `findByEmail(String): Optional<Professionista>`.

- [ ] **Step 1: Scrivere il test (fallirà per assenza delle classi)**

```java
package com.hexisnutrition.backend.professionisti;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionistaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerEmail() {
        Professionista professionista = new Professionista(
                "mario.rossi@example.com", "hash-fittizio", "Mario", "Rossi");

        professionistaRepository.save(professionista);

        Optional<Professionista> trovato = professionistaRepository.findByEmail("mario.rossi@example.com");

        assertThat(trovato).isPresent();
        assertThat(trovato.get().getNome()).isEqualTo("Mario");
        assertThat(trovato.get().getCognome()).isEqualTo("Rossi");
    }

    @Test
    void nonTrovaUnaEmailInesistente() {
        Optional<Professionista> trovato = professionistaRepository.findByEmail("non-esiste@example.com");

        assertThat(trovato).isEmpty();
    }
}
```

- [ ] **Step 2: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=ProfessionistaRepositoryTest`
Expected: FAIL (compilazione fallita: `Professionista` e `ProfessionistaRepository` non esistono).

- [ ] **Step 3: Scrivere la migrazione**

```sql
CREATE TABLE professionisti (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    creato_il TIMESTAMP NOT NULL DEFAULT now()
);
```

- [ ] **Step 4: Scrivere l'entità**

```java
package com.hexisnutrition.backend.professionisti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "professionisti")
public class Professionista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected Professionista() {
    }

    public Professionista(String email, String passwordHash, String nome, String cognome) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.cognome = cognome;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }
}
```

- [ ] **Step 5: Scrivere il repository**

```java
package com.hexisnutrition.backend.professionisti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfessionistaRepository extends JpaRepository<Professionista, UUID> {
    Optional<Professionista> findByEmail(String email);
}
```

- [ ] **Step 6: Eseguire il test e verificare che passi**

Run: `mvn test -Dtest=ProfessionistaRepositoryTest`
Expected: `BUILD SUCCESS`, 2 test eseguiti, 0 fallimenti.

- [ ] **Step 7: Stage delle modifiche**

```bash
git add src/main/resources/db/migration/V1__create_professionisti.sql \
        src/main/java/com/hexisnutrition/backend/professionisti/Professionista.java \
        src/main/java/com/hexisnutrition/backend/professionisti/ProfessionistaRepository.java \
        src/test/java/com/hexisnutrition/backend/professionisti/ProfessionistaRepositoryTest.java
```

---

### Task 3: Entità Paziente

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__create_pazienti.sql`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/StatoAccountPaziente.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteRepository.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java`

**Interfaces:**
- Consumes: `Professionista`, `ProfessionistaRepository` (Task 2), `AbstractIntegrationTest` (Task 1).
- Produces: `StatoAccountPaziente` enum (`MAI_INVITATO`, `INVITATO`, `ATTIVO`). `Paziente(UUID professionistaId, String nome, String cognome, String email, String telefono, LocalDate dataNascita, String sesso, Integer altezzaCm)` costruttore; `getId(): UUID`, `getProfessionistaId(): UUID`, `getNome/getCognome/getEmail/getTelefono(): String`, `getDataNascita(): LocalDate`, `getSesso(): String`, `getAltezzaCm(): Integer`, `getPasswordHash(): String`, `setPasswordHash(String): void`, `getStatoAccount(): StatoAccountPaziente`, `setStatoAccount(StatoAccountPaziente): void`. `PazienteRepository extends JpaRepository<Paziente, UUID>` con `findAllByProfessionistaId(UUID): List<Paziente>`, `findByEmailAndStatoAccount(String, StatoAccountPaziente): Optional<Paziente>`, `existsByEmailAndStatoAccount(String, StatoAccountPaziente): boolean`.

- [ ] **Step 1: Scrivere il test**

```java
package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PazienteRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerProfessionista() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));

        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca.verdi@example.com", "3331234567", LocalDate.of(1990, 5, 20), "M", 178);
        pazienteRepository.save(paziente);

        List<Paziente> pazienti = pazienteRepository.findAllByProfessionistaId(professionista.getId());

        assertThat(pazienti).hasSize(1);
        assertThat(pazienti.get(0).getEmail()).isEqualTo("luca.verdi@example.com");
        assertThat(pazienti.get(0).getStatoAccount()).isEqualTo(StatoAccountPaziente.MAI_INVITATO);
    }

    @Test
    void trovaSoloIlPazienteAttivoPerEmail() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof2@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "attivo@example.com", null, null, null, null);
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        paziente.setPasswordHash("hash-paziente");
        pazienteRepository.save(paziente);

        Optional<Paziente> trovato = pazienteRepository.findByEmailAndStatoAccount(
                "attivo@example.com", StatoAccountPaziente.ATTIVO);
        assertThat(trovato).isPresent();

        boolean esisteNonAttivo = pazienteRepository.existsByEmailAndStatoAccount(
                "attivo@example.com", StatoAccountPaziente.INVITATO);
        assertThat(esisteNonAttivo).isFalse();
    }
}
```

- [ ] **Step 2: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=PazienteRepositoryTest`
Expected: FAIL (compilazione fallita: le classi non esistono).

- [ ] **Step 3: Scrivere la migrazione**

```sql
CREATE TABLE pazienti (
    id UUID PRIMARY KEY,
    professionista_id UUID NOT NULL REFERENCES professionisti(id),
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefono VARCHAR(30),
    data_nascita DATE,
    sesso VARCHAR(10),
    altezza_cm INTEGER,
    password_hash VARCHAR(255),
    stato_account VARCHAR(20) NOT NULL DEFAULT 'MAI_INVITATO',
    creato_il TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_pazienti_professionista_id ON pazienti(professionista_id);

-- Un'email può comparire più volte come anagrafica (es. presso professionisti diversi),
-- ma può appartenere a un solo account ATTIVO per volta: vedi ADR 0002.
CREATE UNIQUE INDEX uq_pazienti_email_attivo ON pazienti(email) WHERE stato_account = 'ATTIVO';
```

- [ ] **Step 4: Scrivere l'enum**

```java
package com.hexisnutrition.backend.pazienti;

public enum StatoAccountPaziente {
    MAI_INVITATO,
    INVITATO,
    ATTIVO
}
```

- [ ] **Step 5: Scrivere l'entità**

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

    @Column(name = "altezza_cm")
    private Integer altezzaCm;

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
                     String telefono, LocalDate dataNascita, String sesso, Integer altezzaCm) {
        this.professionistaId = professionistaId;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.telefono = telefono;
        this.dataNascita = dataNascita;
        this.sesso = sesso;
        this.altezzaCm = altezzaCm;
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

    public Integer getAltezzaCm() {
        return altezzaCm;
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

- [ ] **Step 6: Scrivere il repository**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PazienteRepository extends JpaRepository<Paziente, UUID> {
    List<Paziente> findAllByProfessionistaId(UUID professionistaId);

    Optional<Paziente> findByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);

    boolean existsByEmailAndStatoAccount(String email, StatoAccountPaziente statoAccount);
}
```

- [ ] **Step 7: Eseguire il test e verificare che passi**

Run: `mvn test -Dtest=PazienteRepositoryTest`
Expected: `BUILD SUCCESS`, 2 test eseguiti, 0 fallimenti.

- [ ] **Step 8: Stage delle modifiche**

```bash
git add src/main/resources/db/migration/V2__create_pazienti.sql \
        src/main/java/com/hexisnutrition/backend/pazienti/StatoAccountPaziente.java \
        src/main/java/com/hexisnutrition/backend/pazienti/Paziente.java \
        src/main/java/com/hexisnutrition/backend/pazienti/PazienteRepository.java \
        src/test/java/com/hexisnutrition/backend/pazienti/PazienteRepositoryTest.java
```

---

### Task 4: Token di invito/reset (TokenAzione)

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__create_token_azione.sql`
- Create: `backend/src/main/java/com/hexisnutrition/backend/inviti/TipoToken.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/inviti/TokenAzione.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/inviti/TokenAzioneRepository.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/inviti/TokenAzioneRepositoryTest.java`

**Interfaces:**
- Consumes: `Professionista`/`ProfessionistaRepository` (Task 2), `Paziente`/`PazienteRepository` (Task 3), `AbstractIntegrationTest` (Task 1).
- Produces: `TipoToken` enum (`INVITO`, `RESET_PASSWORD`). `TokenAzione.perPaziente(TipoToken, UUID pazienteId, Duration validita): TokenAzione` e `TokenAzione.perProfessionista(TipoToken, UUID professionistaId, Duration validita): TokenAzione` (factory statici); `getId/getToken/getTipo/getProfessionistaId/getPazienteId/getScadenza(): ...`, `isValido(): boolean`, `isUsato(): boolean`, `segnaUsato(): void`. `TokenAzioneRepository extends JpaRepository<TokenAzione, UUID>` con `findByToken(String): Optional<TokenAzione>`.

- [ ] **Step 1: Scrivere il test**

```java
package com.hexisnutrition.backend.inviti;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAzioneRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        tokenAzioneRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerToken() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca@example.com", null, null, null, null));

        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        Optional<TokenAzione> trovato = tokenAzioneRepository.findByToken(token.getToken());

        assertThat(trovato).isPresent();
        assertThat(trovato.get().getTipo()).isEqualTo(TipoToken.INVITO);
        assertThat(trovato.get().isValido()).isTrue();
    }

    @Test
    void unTokenScadutoNonEValido() {
        TokenAzione token = TokenAzione.perProfessionista(
                TipoToken.RESET_PASSWORD, UUID.randomUUID(), Duration.ofSeconds(-1));

        assertThat(token.isValido()).isFalse();
    }

    @Test
    void unTokenUsatoNonEValido() {
        TokenAzione token = TokenAzione.perProfessionista(
                TipoToken.RESET_PASSWORD, UUID.randomUUID(), Duration.ofHours(1));

        token.segnaUsato();

        assertThat(token.isValido()).isFalse();
    }
}
```

- [ ] **Step 2: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=TokenAzioneRepositoryTest`
Expected: FAIL (compilazione fallita: le classi non esistono).

- [ ] **Step 3: Scrivere la migrazione**

```sql
CREATE TABLE token_azione (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    professionista_id UUID REFERENCES professionisti(id),
    paziente_id UUID REFERENCES pazienti(id),
    scadenza TIMESTAMP NOT NULL,
    usato BOOLEAN NOT NULL DEFAULT false,
    creato_il TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_token_azione_target CHECK (
        (professionista_id IS NOT NULL AND paziente_id IS NULL) OR
        (professionista_id IS NULL AND paziente_id IS NOT NULL)
    )
);
```

- [ ] **Step 4: Scrivere l'enum**

```java
package com.hexisnutrition.backend.inviti;

public enum TipoToken {
    INVITO,
    RESET_PASSWORD
}
```

- [ ] **Step 5: Scrivere l'entità**

```java
package com.hexisnutrition.backend.inviti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_azione")
public class TokenAzione {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoToken tipo;

    @Column(name = "professionista_id")
    private UUID professionistaId;

    @Column(name = "paziente_id")
    private UUID pazienteId;

    @Column(nullable = false)
    private Instant scadenza;

    @Column(nullable = false)
    private boolean usato = false;

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl = Instant.now();

    protected TokenAzione() {
    }

    public static TokenAzione perPaziente(TipoToken tipo, UUID pazienteId, Duration validita) {
        TokenAzione token = new TokenAzione();
        token.token = UUID.randomUUID().toString();
        token.tipo = tipo;
        token.pazienteId = pazienteId;
        token.scadenza = Instant.now().plus(validita);
        return token;
    }

    public static TokenAzione perProfessionista(TipoToken tipo, UUID professionistaId, Duration validita) {
        TokenAzione token = new TokenAzione();
        token.token = UUID.randomUUID().toString();
        token.tipo = tipo;
        token.professionistaId = professionistaId;
        token.scadenza = Instant.now().plus(validita);
        return token;
    }

    public boolean isValido() {
        return !usato && Instant.now().isBefore(scadenza);
    }

    public void segnaUsato() {
        this.usato = true;
    }

    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public UUID getProfessionistaId() {
        return professionistaId;
    }

    public UUID getPazienteId() {
        return pazienteId;
    }

    public Instant getScadenza() {
        return scadenza;
    }

    public boolean isUsato() {
        return usato;
    }
}
```

- [ ] **Step 6: Scrivere il repository**

```java
package com.hexisnutrition.backend.inviti;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenAzioneRepository extends JpaRepository<TokenAzione, UUID> {
    Optional<TokenAzione> findByToken(String token);
}
```

- [ ] **Step 7: Eseguire il test e verificare che passi**

Run: `mvn test -Dtest=TokenAzioneRepositoryTest`
Expected: `BUILD SUCCESS`, 3 test eseguiti, 0 fallimenti.

- [ ] **Step 8: Stage delle modifiche**

```bash
git add src/main/resources/db/migration/V3__create_token_azione.sql \
        src/main/java/com/hexisnutrition/backend/inviti/TipoToken.java \
        src/main/java/com/hexisnutrition/backend/inviti/TokenAzione.java \
        src/main/java/com/hexisnutrition/backend/inviti/TokenAzioneRepository.java \
        src/test/java/com/hexisnutrition/backend/inviti/TokenAzioneRepositoryTest.java
```

---

### Task 5: JwtService

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/Ruolo.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/JwtService.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/auth/JwtServiceTest.java`

**Interfaces:**
- Produces: `Ruolo` enum (`PROFESSIONISTA`, `PAZIENTE`). `JwtService(String secret, long expirationMinutes)` costruttore; `generateToken(UUID userId, Ruolo ruolo): String`; `parseToken(String token): Jws<Claims>` (lancia `io.jsonwebtoken.JwtException` se il token non è valido/scaduto/firmato male).

- [ ] **Step 1: Scrivere il test**

```java
package com.hexisnutrition.backend.auth;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String CHIAVE_UNO = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String CHIAVE_DUE = "fedcba9876543210fedcba9876543210fedcba9876543210";

    @Test
    void generaEValidaUnTokenConRuolo() {
        JwtService jwtService = new JwtService(CHIAVE_UNO, 60);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, Ruolo.PROFESSIONISTA);
        var claims = jwtService.parseToken(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("ruolo", String.class)).isEqualTo("PROFESSIONISTA");
    }

    @Test
    void rifiutaUnTokenFirmatoConUnaChiaveDiversa() {
        JwtService emittente = new JwtService(CHIAVE_UNO, 60);
        JwtService verificatore = new JwtService(CHIAVE_DUE, 60);

        String token = emittente.generateToken(UUID.randomUUID(), Ruolo.PAZIENTE);

        assertThatThrownBy(() -> verificatore.parseToken(token))
                .isInstanceOf(SignatureException.class);
    }
}
```

- [ ] **Step 2: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=JwtServiceTest`
Expected: FAIL (compilazione fallita: `JwtService` e `Ruolo` non esistono).

- [ ] **Step 3: Scrivere l'enum**

```java
package com.hexisnutrition.backend.auth;

public enum Ruolo {
    PROFESSIONISTA,
    PAZIENTE
}
```

- [ ] **Step 4: Scrivere `JwtService`**

```java
package com.hexisnutrition.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(String secret, long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UUID userId, Ruolo ruolo) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("ruolo", ruolo.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
```

- [ ] **Step 5: Eseguire il test e verificare che passi**

Run: `mvn test -Dtest=JwtServiceTest`
Expected: `BUILD SUCCESS`, 2 test eseguiti, 0 fallimenti.

- [ ] **Step 6: Stage delle modifiche**

```bash
git add src/main/java/com/hexisnutrition/backend/auth/Ruolo.java \
        src/main/java/com/hexisnutrition/backend/auth/JwtService.java \
        src/test/java/com/hexisnutrition/backend/auth/JwtServiceTest.java
```

---

### Task 6: EmailSender e integrazione Resend

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/email/EmailSender.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/email/ResendEmailSender.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/email/FakeEmailSender.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/email/ResendEmailSenderTest.java`
- Modify: `backend/src/main/resources/application.yml`

**Interfaces:**
- Produces: `EmailSender` interfaccia con `invia(String destinatario, String oggetto, String corpoHtml): void`. `ResendEmailSender implements EmailSender`, bean Spring (`@Component`), costruttore `(RestClient.Builder, String baseUrl, String apiKey, String fromEmail)`. `FakeEmailSender implements EmailSender` (solo test) con `getInviate(): List<EmailInviata>` dove `EmailInviata` è un record `(String destinatario, String oggetto, String corpoHtml)`.

- [ ] **Step 1: Scrivere il test di `ResendEmailSender`**

```java
package com.hexisnutrition.backend.email;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {

    @Test
    void inviaChiamaLApiResendConIDatiCorretti() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chiave-di-test"))
                .andExpect(jsonPath("$.from").value("no-reply@hexisnutrition.example"))
                .andExpect(jsonPath("$.to[0]").value("paziente@example.com"))
                .andExpect(jsonPath("$.subject").value("Oggetto di test"))
                .andRespond(withSuccess("{\"id\":\"abc123\"}", MediaType.APPLICATION_JSON));

        ResendEmailSender sender = new ResendEmailSender(builder, "https://api.resend.com",
                "chiave-di-test", "no-reply@hexisnutrition.example");

        sender.invia("paziente@example.com", "Oggetto di test", "<p>Corpo</p>");

        server.verify();
    }
}
```

- [ ] **Step 2: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=ResendEmailSenderTest`
Expected: FAIL (compilazione fallita: `ResendEmailSender` non esiste).

- [ ] **Step 3: Scrivere l'interfaccia `EmailSender`**

```java
package com.hexisnutrition.backend.email;

public interface EmailSender {
    void invia(String destinatario, String oggetto, String corpoHtml);
}
```

- [ ] **Step 4: Scrivere `ResendEmailSender`**

```java
package com.hexisnutrition.backend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ResendEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String fromEmail;

    public ResendEmailSender(RestClient.Builder restClientBuilder,
                              @Value("${resend.base-url}") String baseUrl,
                              @Value("${resend.api-key}") String apiKey,
                              @Value("${resend.from-email}") String fromEmail) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.fromEmail = fromEmail;
    }

    @Override
    public void invia(String destinatario, String oggetto, String corpoHtml) {
        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RichiestaEmailResend(fromEmail, List.of(destinatario), oggetto, corpoHtml))
                .retrieve()
                .toBodilessEntity();
    }

    private record RichiestaEmailResend(String from, List<String> to, String subject, String html) {
    }
}
```

- [ ] **Step 5: Aggiungere la configurazione Resend ad `application.yml`**

Aggiungere in coda al file esistente:

```yaml
resend:
  base-url: https://api.resend.com
  api-key: ${RESEND_API_KEY:}
  from-email: ${RESEND_FROM_EMAIL:no-reply@hexisnutrition.example}
```

- [ ] **Step 6: Eseguire il test e verificare che passi**

Run: `mvn test -Dtest=ResendEmailSenderTest`
Expected: `BUILD SUCCESS`, 1 test eseguito, 0 fallimenti.

- [ ] **Step 7: Scrivere `FakeEmailSender`** (test double riusato dai task successivi)

```java
package com.hexisnutrition.backend.email;

import java.util.ArrayList;
import java.util.List;

public class FakeEmailSender implements EmailSender {

    private final List<EmailInviata> inviate = new ArrayList<>();

    @Override
    public void invia(String destinatario, String oggetto, String corpoHtml) {
        inviate.add(new EmailInviata(destinatario, oggetto, corpoHtml));
    }

    public List<EmailInviata> getInviate() {
        return inviate;
    }

    public record EmailInviata(String destinatario, String oggetto, String corpoHtml) {
    }
}
```

- [ ] **Step 8: Eseguire `mvn test` completo per verificare che nulla si sia rotto**

Run: `mvn test`
Expected: `BUILD SUCCESS`, tutti i test dei task precedenti + questo passano.

- [ ] **Step 9: Stage delle modifiche**

```bash
git add src/main/java/com/hexisnutrition/backend/email/EmailSender.java \
        src/main/java/com/hexisnutrition/backend/email/ResendEmailSender.java \
        src/main/resources/application.yml \
        src/test/java/com/hexisnutrition/backend/email/FakeEmailSender.java \
        src/test/java/com/hexisnutrition/backend/email/ResendEmailSenderTest.java
```

---

### Task 7: Configurazione di sicurezza (JWT + ruoli)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/SecurityConfig.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/auth/SecurityConfigTest.java`
- Modify: `backend/src/main/resources/application.yml`

**Interfaces:**
- Consumes: `JwtService`, `Ruolo` (Task 5), `AbstractIntegrationTest` (Task 1).
- Produces: bean `PasswordEncoder` (BCrypt), bean `JwtService` (letto da proprietà `app.jwt.secret` / `app.jwt.expiration-minutes`), `SecurityFilterChain` che: permette senza autenticazione `POST /auth/login`, `POST /auth/password-dimenticata`, `POST /auth/reset-password`, `POST /inviti/*/attiva`; richiede ruolo `PROFESSIONISTA` su `/pazienti/**`; richiede autenticazione su tutto il resto. Ogni richiesta autenticata ha come `Authentication.getPrincipal()` lo `UUID` dell'utente (professionista o paziente) e come authority `ROLE_PROFESSIONISTA` o `ROLE_PAZIENTE`.

- [ ] **Step 1: Aggiungere la configurazione JWT ad `application.yml`**

Aggiungere in coda al file esistente:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:cambia-questo-valore-in-produzione-almeno-32-byte}
    expiration-minutes: 480
```

- [ ] **Step 2: Scrivere il test**

```java
package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest extends AbstractIntegrationTest {

    @Test
    void richiestaSenzaTokenSuEndpointProtettoRestituisce401() throws Exception {
        mockMvc.perform(get("/pazienti"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void richiestaSuEndpointPubblicoNonVieneBloccataDallaSicurezza() throws Exception {
        var risultato = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertThat(risultato.getResponse().getStatus()).isNotEqualTo(401);
    }
}
```

- [ ] **Step 3: Eseguire il test e verificare che fallisca**

Run: `mvn test -Dtest=SecurityConfigTest`
Expected: FAIL. Senza configurazione di sicurezza esplicita, Spring Boot applica la sicurezza di default (utente generato con password casuale), che restituisce 401/403 anche su `/auth/login`, quindi il secondo assert fallisce.

- [ ] **Step 4: Scrivere `JwtAuthenticationFilter`**

```java
package com.hexisnutrition.backend.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var claims = jwtService.parseToken(token).getPayload();
                UUID userId = UUID.fromString(claims.getSubject());
                String ruolo = claims.get("ruolo", String.class);
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + ruolo));
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 5: Scrivere `SecurityConfig`**

```java
package com.hexisnutrition.backend.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtService jwtService(@Value("${app.jwt.secret}") String secret,
                                  @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        return new JwtService(secret, expirationMinutes);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/password-dimenticata", "/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/inviti/*/attiva").permitAll()
                        .requestMatchers("/pazienti/**").hasRole("PROFESSIONISTA")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 6: Eseguire il test e verificare che passi**

Run: `mvn test -Dtest=SecurityConfigTest`
Expected: `BUILD SUCCESS`, 2 test eseguiti, 0 fallimenti.

- [ ] **Step 7: Eseguire `mvn test` completo**

Run: `mvn test`
Expected: `BUILD SUCCESS`, tutti i test passano.

- [ ] **Step 8: Stage delle modifiche**

```bash
git add src/main/java/com/hexisnutrition/backend/auth/JwtAuthenticationFilter.java \
        src/main/java/com/hexisnutrition/backend/auth/SecurityConfig.java \
        src/main/resources/application.yml \
        src/test/java/com/hexisnutrition/backend/auth/SecurityConfigTest.java
```

---

### Task 8: Endpoint di autenticazione (login, password dimenticata, reset)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/LoginRequest.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/LoginResponse.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/PasswordDimenticataRequest.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/ResetPasswordRequest.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/CredenzialiNonValideException.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/inviti/TokenNonValidoException.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/AuthService.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/AuthController.java`
- Create: `backend/src/test/java/com/hexisnutrition/backend/support/TestEmailConfig.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java`

**Interfaces:**
- Consumes: `Professionista`/`ProfessionistaRepository` (Task 2), `Paziente`/`PazienteRepository`/`StatoAccountPaziente` (Task 3), `TokenAzione`/`TokenAzioneRepository`/`TipoToken` (Task 4), `JwtService`/`Ruolo` (Task 5), `EmailSender`/`FakeEmailSender` (Task 6), `PasswordEncoder` (Task 7).
- Produces: `POST /auth/login` → `LoginResponse(String token, String ruolo)`, 401 se credenziali errate o paziente non `ATTIVO`. `POST /auth/password-dimenticata` → 204 sempre (non rivela se l'email esiste). `POST /auth/reset-password` → 204 se il token è valido, 400 se scaduto/usato/inesistente. `TokenNonValidoException` (package `inviti`, `@ResponseStatus(BAD_REQUEST)`) — riusata dal Task 9. `TestEmailConfig` (`@TestConfiguration`, bean `FakeEmailSender` `@Primary`) — riusata dal Task 9.

- [ ] **Step 1: Scrivere `TestEmailConfig`**

```java
package com.hexisnutrition.backend.support;

import com.hexisnutrition.backend.email.FakeEmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    public FakeEmailSender fakeEmailSender() {
        return new FakeEmailSender();
    }
}
```

- [ ] **Step 2: Scrivere i test di login (falliranno per assenza delle classi)**

```java
package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.StatoAccountPaziente;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import com.hexisnutrition.backend.support.TestEmailConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestEmailConfig.class)
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void pulisci() {
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void loginProfessionistaConCredenzialiCorretteRestituisceToken() throws Exception {
        professionistaRepository.save(new Professionista(
                "mario@example.com", passwordEncoder.encode("password123"), "Mario", "Rossi"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mario@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("PROFESSIONISTA"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginConPasswordErrataRestituisce401() throws Exception {
        professionistaRepository.save(new Professionista(
                "mario2@example.com", passwordEncoder.encode("password123"), "Mario", "Rossi"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mario2@example.com\",\"password\":\"sbagliata\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pazienteNonAttivoNonPuoFareLogin() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "prof@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca@example.com", null, null, null, null);
        paziente.setPasswordHash(passwordEncoder.encode("password123"));
        paziente.setStatoAccount(StatoAccountPaziente.INVITATO);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"luca@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=AuthControllerTest`
Expected: FAIL (compilazione fallita: `AuthController` e le classi collegate non esistono).

- [ ] **Step 4: Scrivere i DTO e l'eccezione**

```java
package com.hexisnutrition.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
}
```

```java
package com.hexisnutrition.backend.auth;

public record LoginResponse(String token, String ruolo) {
}
```

```java
package com.hexisnutrition.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordDimenticataRequest(@NotBlank @Email String email) {
}
```

```java
package com.hexisnutrition.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8) String nuovaPassword) {
}
```

```java
package com.hexisnutrition.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class CredenzialiNonValideException extends RuntimeException {
}
```

```java
package com.hexisnutrition.backend.inviti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TokenNonValidoException extends RuntimeException {
}
```

- [ ] **Step 5: Scrivere `AuthService` (solo login, per ora)**

```java
package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.StatoAccountPaziente;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final ProfessionistaRepository professionistaRepository;
    private final PazienteRepository pazienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(ProfessionistaRepository professionistaRepository,
                        PazienteRepository pazienteRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.professionistaRepository = professionistaRepository;
        this.pazienteRepository = pazienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String email, String password) {
        var professionista = professionistaRepository.findByEmail(email);
        if (professionista.isPresent()
                && passwordEncoder.matches(password, professionista.get().getPasswordHash())) {
            String token = jwtService.generateToken(professionista.get().getId(), Ruolo.PROFESSIONISTA);
            return new LoginResponse(token, Ruolo.PROFESSIONISTA.name());
        }

        var paziente = pazienteRepository.findByEmailAndStatoAccount(email, StatoAccountPaziente.ATTIVO);
        if (paziente.isPresent() && paziente.get().getPasswordHash() != null
                && passwordEncoder.matches(password, paziente.get().getPasswordHash())) {
            String token = jwtService.generateToken(paziente.get().getId(), Ruolo.PAZIENTE);
            return new LoginResponse(token, Ruolo.PAZIENTE.name());
        }

        throw new CredenzialiNonValideException();
    }
}
```

- [ ] **Step 6: Scrivere `AuthController` (solo login, per ora)**

```java
package com.hexisnutrition.backend.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }
}
```

- [ ] **Step 7: Eseguire i test di login e verificare che passino**

Run: `mvn test -Dtest=AuthControllerTest`
Expected: `BUILD SUCCESS`, 3 test eseguiti, 0 fallimenti.

- [ ] **Step 8: Aggiungere i test di reset password**

Aggiungere alla classe `AuthControllerTest` (dopo `pazienteNonAttivoNonPuoFareLogin`):

```java
    @Autowired
    private com.hexisnutrition.backend.inviti.TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private com.hexisnutrition.backend.email.FakeEmailSender fakeEmailSender;

    @Test
    void richiestaResetPasswordInviaEmailSeProfessionistaEsiste() throws Exception {
        professionistaRepository.save(new Professionista(
                "reset@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));

        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isNoContent());

        assertThat(fakeEmailSender.getInviate()).hasSize(1);
        assertThat(fakeEmailSender.getInviate().get(0).destinatario()).isEqualTo("reset@example.com");
    }

    @Test
    void richiestaResetPasswordNonRivelaSeEmailNonEsiste() throws Exception {
        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"non-esiste@example.com\"}"))
                .andExpect(status().isNoContent());

        assertThat(fakeEmailSender.getInviate()).isEmpty();
    }

    @Test
    void resetPasswordConTokenValidoCambiaLaPassword() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset2@example.com", passwordEncoder.encode("vecchia"), "Anna", "Bianchi"));
        var token = com.hexisnutrition.backend.inviti.TokenAzione.perProfessionista(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                professionista.getId(), java.time.Duration.ofHours(1));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isNoContent());

        Professionista aggiornato = professionistaRepository.findById(professionista.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("nuovaPassword1", aggiornato.getPasswordHash())).isTrue();
    }

    @Test
    void resetPasswordConTokenScadutoRestituisceErrore() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset3@example.com", passwordEncoder.encode("vecchia"), "Anna", "Bianchi"));
        var token = com.hexisnutrition.backend.inviti.TokenAzione.perProfessionista(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                professionista.getId(), java.time.Duration.ofSeconds(-1));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isBadRequest());
    }
```

Aggiungere in cima al file anche l'import statico mancante:

```java
import static org.assertj.core.api.Assertions.assertThat;
```

E aggiornare il metodo `pulisci()` esistente per includere anche i token e le email finte:

```java
    @AfterEach
    void pulisci() {
        tokenAzioneRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
        fakeEmailSender.getInviate().clear();
    }
```

- [ ] **Step 9: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=AuthControllerTest`
Expected: FAIL (compilazione fallita: `AuthService` non ha ancora i metodi `richiediResetPassword`/`resetPassword`, `AuthController` non ha gli endpoint corrispondenti).

- [ ] **Step 10: Estendere `AuthService` con reset password**

Sostituire il costruttore e aggiungere i metodi (file completo aggiornato):

```java
package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.email.EmailSender;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.inviti.TokenNonValidoException;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.StatoAccountPaziente;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {

    private final ProfessionistaRepository professionistaRepository;
    private final PazienteRepository pazienteRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSender emailSender;

    public AuthService(ProfessionistaRepository professionistaRepository,
                        PazienteRepository pazienteRepository,
                        TokenAzioneRepository tokenAzioneRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        EmailSender emailSender) {
        this.professionistaRepository = professionistaRepository;
        this.pazienteRepository = pazienteRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailSender = emailSender;
    }

    public LoginResponse login(String email, String password) {
        var professionista = professionistaRepository.findByEmail(email);
        if (professionista.isPresent()
                && passwordEncoder.matches(password, professionista.get().getPasswordHash())) {
            String token = jwtService.generateToken(professionista.get().getId(), Ruolo.PROFESSIONISTA);
            return new LoginResponse(token, Ruolo.PROFESSIONISTA.name());
        }

        var paziente = pazienteRepository.findByEmailAndStatoAccount(email, StatoAccountPaziente.ATTIVO);
        if (paziente.isPresent() && paziente.get().getPasswordHash() != null
                && passwordEncoder.matches(password, paziente.get().getPasswordHash())) {
            String token = jwtService.generateToken(paziente.get().getId(), Ruolo.PAZIENTE);
            return new LoginResponse(token, Ruolo.PAZIENTE.name());
        }

        throw new CredenzialiNonValideException();
    }

    public void richiediResetPassword(String email) {
        professionistaRepository.findByEmail(email).ifPresent(professionista -> {
            TokenAzione token = TokenAzione.perProfessionista(
                    TipoToken.RESET_PASSWORD, professionista.getId(), Duration.ofHours(1));
            tokenAzioneRepository.save(token);
            emailSender.invia(professionista.getEmail(), "Reimposta la tua password",
                    corpoResetPassword(token.getToken()));
        });

        pazienteRepository.findByEmailAndStatoAccount(email, StatoAccountPaziente.ATTIVO).ifPresent(paziente -> {
            TokenAzione token = TokenAzione.perPaziente(
                    TipoToken.RESET_PASSWORD, paziente.getId(), Duration.ofHours(1));
            tokenAzioneRepository.save(token);
            emailSender.invia(paziente.getEmail(), "Reimposta la tua password",
                    corpoResetPassword(token.getToken()));
        });
        // Nessuna eccezione se l'email non esiste: evita di rivelare quali indirizzi sono registrati.
    }

    public void resetPassword(String token, String nuovaPassword) {
        TokenAzione tokenAzione = tokenAzioneRepository.findByToken(token)
                .filter(TokenAzione::isValido)
                .filter(t -> t.getTipo() == TipoToken.RESET_PASSWORD)
                .orElseThrow(TokenNonValidoException::new);

        String hash = passwordEncoder.encode(nuovaPassword);
        if (tokenAzione.getProfessionistaId() != null) {
            Professionista professionista = professionistaRepository.findById(tokenAzione.getProfessionistaId())
                    .orElseThrow(TokenNonValidoException::new);
            professionista.setPasswordHash(hash);
            professionistaRepository.save(professionista);
        } else {
            Paziente paziente = pazienteRepository.findById(tokenAzione.getPazienteId())
                    .orElseThrow(TokenNonValidoException::new);
            paziente.setPasswordHash(hash);
            pazienteRepository.save(paziente);
        }

        tokenAzione.segnaUsato();
        tokenAzioneRepository.save(tokenAzione);
    }

    private String corpoResetPassword(String token) {
        return "<p>Reimposta la password: <a href=\"https://app.hexisnutrition.example/reset-password?token="
                + token + "\">Reimposta password</a></p>";
    }
}
```

- [ ] **Step 11: Estendere `AuthController` con gli endpoint di reset password**

```java
package com.hexisnutrition.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/password-dimenticata")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void passwordDimenticata(@Valid @RequestBody PasswordDimenticataRequest request) {
        authService.richiediResetPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.nuovaPassword());
    }
}
```

- [ ] **Step 12: Eseguire tutti i test di `AuthControllerTest` e verificare che passino**

Run: `mvn test -Dtest=AuthControllerTest`
Expected: `BUILD SUCCESS`, 7 test eseguiti, 0 fallimenti.

- [ ] **Step 13: Eseguire `mvn test` completo**

Run: `mvn test`
Expected: `BUILD SUCCESS`, tutti i test passano.

- [ ] **Step 14: Stage delle modifiche**

```bash
git add src/main/java/com/hexisnutrition/backend/auth/ \
        src/main/java/com/hexisnutrition/backend/inviti/TokenNonValidoException.java \
        src/test/java/com/hexisnutrition/backend/support/TestEmailConfig.java \
        src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java
```

---

### Task 9: Anagrafica pazienti, invito e attivazione

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/CreaPazienteRequest.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteResponse.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteNonTrovatoException.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteGiaAttivoException.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/inviti/EmailGiaInUsoException.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/inviti/AttivaInvitoRequest.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteController.java`
- Create: `backend/src/main/java/com/hexisnutrition/backend/inviti/InvitoController.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java`

**Interfaces:**
- Consumes: tutto quanto prodotto dai Task 2–8 (`Professionista`/Repository, `Paziente`/Repository/`StatoAccountPaziente`, `TokenAzione`/Repository/`TipoToken`, `JwtService`/`Ruolo`, `EmailSender`/`FakeEmailSender`, `PasswordEncoder`, `TestEmailConfig`, `TokenNonValidoException`).
- Produces: `POST /pazienti` (ruolo `PROFESSIONISTA`) → crea, 201 + `PazienteResponse`. `GET /pazienti` → lista dei pazienti del professionista autenticato. `GET /pazienti/{id}` → dettaglio, 404 se il paziente non appartiene al professionista autenticato. `POST /pazienti/{id}/invito` → genera invito e invia email, 204; 409 se il paziente è già `ATTIVO`. `POST /inviti/{token}/attiva` (pubblico) → imposta password e attiva l'account, 204; 400 se il token non è valido, 409 se l'email è già in uso da un account attivo (professionista o altro paziente).

- [ ] **Step 1: Scrivere i test di creazione/lista/dettaglio (falliranno per assenza delle classi)**

```java
package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import com.hexisnutrition.backend.support.TestEmailConfig;
import com.hexisnutrition.backend.email.FakeEmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestEmailConfig.class)
class PazienteControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender fakeEmailSender;

    @AfterEach
    void pulisci() {
        tokenAzioneRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
        fakeEmailSender.getInviate().clear();
    }

    private String tokenPer(Professionista professionista) {
        return jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
    }

    @Test
    void creaPazienteRestituisce201() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca@example.com",
                                 "telefono":"333123456","dataNascita":"1990-05-20","sesso":"M","altezzaCm":178}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("luca@example.com"))
                .andExpect(jsonPath("$.statoAccount").value("MAI_INVITATO"));
    }

    @Test
    void listaRestituisceSoloIPazientiDelProfessionistaAutenticato() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("b@example.com", "hash", "B", "B"));
        pazienteRepository.save(new Paziente(professionistaA.getId(), "Paziente", "DiA",
                "diA@example.com", null, null, null, null));
        pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "diB@example.com", null, null, null, null));

        mockMvc.perform(get("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("diA@example.com"));
    }

    @Test
    void dettaglioDiPazienteDiAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("a2@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("b2@example.com", "hash", "B", "B"));
        Paziente pazienteDiB = pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "diB2@example.com", null, null, null, null));

        mockMvc.perform(get("/pazienti/" + pazienteDiB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unPazienteAutenticatoNonPuoCreareAltriPazienti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof3@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca3@example.com", null, null, null, null));
        String tokenPaziente = jwtService.generateToken(paziente.getId(), Ruolo.PAZIENTE);

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPaziente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"cognome\":\"Y\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=PazienteControllerTest`
Expected: FAIL (compilazione fallita: `CreaPazienteRequest`, `PazienteResponse`, `PazienteService`, `PazienteController` non esistono).

- [ ] **Step 3: Scrivere i DTO e le eccezioni**

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
        Integer altezzaCm
) {
}
```

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
        Integer altezzaCm,
        String statoAccount
) {
    public static PazienteResponse da(Paziente paziente) {
        return new PazienteResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getEmail(), paziente.getTelefono(), paziente.getDataNascita(),
                paziente.getSesso(), paziente.getAltezzaCm(), paziente.getStatoAccount().name());
    }
}
```

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PazienteNonTrovatoException extends RuntimeException {
}
```

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PazienteGiaAttivoException extends RuntimeException {
}
```

```java
package com.hexisnutrition.backend.inviti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailGiaInUsoException extends RuntimeException {
}
```

```java
package com.hexisnutrition.backend.inviti;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttivaInvitoRequest(@NotBlank @Size(min = 8) String nuovaPassword) {
}
```

- [ ] **Step 4: Scrivere `PazienteService` (crea/lista/dettaglio, per ora)**

```java
package com.hexisnutrition.backend.pazienti;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PazienteService {

    private final PazienteRepository pazienteRepository;

    public PazienteService(PazienteRepository pazienteRepository) {
        this.pazienteRepository = pazienteRepository;
    }

    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.altezzaCm());
        return pazienteRepository.save(paziente);
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
}
```

- [ ] **Step 5: Scrivere `PazienteController`**

```java
package com.hexisnutrition.backend.pazienti;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pazienti")
public class PazienteController {

    private final PazienteService pazienteService;

    public PazienteController(PazienteService pazienteService) {
        this.pazienteService = pazienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PazienteResponse crea(@AuthenticationPrincipal UUID professionistaId,
                                  @Valid @RequestBody CreaPazienteRequest request) {
        return PazienteResponse.da(pazienteService.crea(professionistaId, request));
    }

    @GetMapping
    public List<PazienteResponse> lista(@AuthenticationPrincipal UUID professionistaId) {
        return pazienteService.listaPerProfessionista(professionistaId).stream()
                .map(PazienteResponse::da)
                .toList();
    }

    @GetMapping("/{id}")
    public PazienteResponse dettaglio(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        return PazienteResponse.da(pazienteService.dettaglio(professionistaId, id));
    }
}
```

- [ ] **Step 6: Eseguire i test e verificare che passino**

Run: `mvn test -Dtest=PazienteControllerTest`
Expected: `BUILD SUCCESS`, 4 test eseguiti, 0 fallimenti.

- [ ] **Step 7: Aggiungere i test di invito**

Aggiungere alla classe `PazienteControllerTest`:

```java
    @Test
    void invitoGeneraTokenCambiaStatoEInviaEmail() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof4@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca4@example.com", null, null, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.getStatoAccount()).isEqualTo(StatoAccountPaziente.INVITATO);
        assertThat(fakeEmailSender.getInviate()).hasSize(1);
        assertThat(fakeEmailSender.getInviate().get(0).destinatario()).isEqualTo("luca4@example.com");
    }

    @Test
    void invitoAPazienteGiaAttivoRestituisce409() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof5@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca5@example.com", null, null, null, null);
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        paziente.setPasswordHash("hash");
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isConflict());
    }
```

- [ ] **Step 8: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=PazienteControllerTest`
Expected: FAIL (404/405: l'endpoint `POST /pazienti/{id}/invito` non esiste ancora).

- [ ] **Step 9: Estendere `PazienteService` con `invita`**

```java
package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.email.EmailSender;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PazienteService {

    private final PazienteRepository pazienteRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final EmailSender emailSender;

    public PazienteService(PazienteRepository pazienteRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender) {
        this.pazienteRepository = pazienteRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.emailSender = emailSender;
    }

    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.altezzaCm());
        return pazienteRepository.save(paziente);
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
}
```

- [ ] **Step 10: Aggiungere l'endpoint di invito a `PazienteController`**

Aggiungere al controller esistente:

```java
    @PostMapping("/{id}/invito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invita(@AuthenticationPrincipal UUID professionistaId, @PathVariable UUID id) {
        pazienteService.invita(professionistaId, id);
    }
```

- [ ] **Step 11: Eseguire i test e verificare che passino**

Run: `mvn test -Dtest=PazienteControllerTest`
Expected: `BUILD SUCCESS`, 6 test eseguiti, 0 fallimenti.

- [ ] **Step 12: Aggiungere i test di attivazione invito**

Aggiungere alla classe `PazienteControllerTest`:

```java
    @Test
    void attivaConTokenValidoImpostaPasswordEAttivaAccount() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof6@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca6@example.com", null, null, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.getStatoAccount()).isEqualTo(StatoAccountPaziente.ATTIVO);
        assertThat(passwordEncoder.matches("password123", aggiornato.getPasswordHash())).isTrue();
    }

    @Test
    void attivaConTokenGiaUsatoRestituisceErrore() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof7@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca7@example.com", null, null, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        token.segnaUsato();
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attivaConEmailGiaInUsoDaUnProfessionistaRestituisce409() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("collisione@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "collisione@example.com", null, null, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isConflict());
    }
```

Nessun nuovo import necessario: `TipoToken`, `TokenAzione` e `java.time.Duration` sono già importati dallo Step 1 di questo task.

- [ ] **Step 13: Eseguire i test e verificare che falliscano**

Run: `mvn test -Dtest=PazienteControllerTest`
Expected: FAIL (404: l'endpoint `POST /inviti/{token}/attiva` non esiste ancora).

- [ ] **Step 14: Estendere `PazienteService` con `attiva` (aggiungere `ProfessionistaRepository` e `PasswordEncoder`)**

Sostituire costruttore e campi, e aggiungere il metodo (file completo aggiornato):

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

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PazienteService {

    private final PazienteRepository pazienteRepository;
    private final ProfessionistaRepository professionistaRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    public PazienteService(PazienteRepository pazienteRepository,
                            ProfessionistaRepository professionistaRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender,
                            PasswordEncoder passwordEncoder) {
        this.pazienteRepository = pazienteRepository;
        this.professionistaRepository = professionistaRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.altezzaCm());
        return pazienteRepository.save(paziente);
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

    public void attiva(String token, String nuovaPassword) {
        TokenAzione tokenAzione = tokenAzioneRepository.findByToken(token)
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

        tokenAzione.segnaUsato();
        tokenAzioneRepository.save(tokenAzione);
    }
}
```

- [ ] **Step 15: Scrivere `InvitoController`**

```java
package com.hexisnutrition.backend.inviti;

import com.hexisnutrition.backend.pazienti.PazienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inviti")
public class InvitoController {

    private final PazienteService pazienteService;

    public InvitoController(PazienteService pazienteService) {
        this.pazienteService = pazienteService;
    }

    @PostMapping("/{token}/attiva")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attiva(@PathVariable String token, @Valid @RequestBody AttivaInvitoRequest request) {
        pazienteService.attiva(token, request.nuovaPassword());
    }
}
```

- [ ] **Step 16: Eseguire tutti i test di `PazienteControllerTest` e verificare che passino**

Run: `mvn test -Dtest=PazienteControllerTest`
Expected: `BUILD SUCCESS`, 9 test eseguiti, 0 fallimenti.

- [ ] **Step 17: Eseguire la suite completa**

Run: `mvn test`
Expected: `BUILD SUCCESS`, tutti i test di tutti i task passano.

- [ ] **Step 18: Stage delle modifiche**

```bash
git add src/main/java/com/hexisnutrition/backend/pazienti/ \
        src/main/java/com/hexisnutrition/backend/inviti/ \
        src/test/java/com/hexisnutrition/backend/pazienti/PazienteControllerTest.java
```

---

## Note per chi esegue il piano

- La creazione dell'account professionista **non è un endpoint** in questo piano (per scelta, vedi [ADR 0002](../../../wiki/decisioni/0002-autenticazione-e-onboarding.md)): per i test manuali end-to-end, inserisci una riga nella tabella `professionisti` a mano (email + hash BCrypt di una password nota, generabile con un piccolo `main` temporaneo o con un tool online di hashing BCrypt) oppure tramite un test/script ad-hoc — non aggiungere un endpoint di creazione per questo, sarebbe fuori scope.
- Al termine del Task 9, l'intero sotto-progetto "Fondamenta" lato backend è funzionalmente completo secondo il design in [decisioni/0002](../../../wiki/decisioni/0002-autenticazione-e-onboarding.md). Aggiorna `wiki/api-contracts.md` con richiesta/risposta reali (già abbozzate nella tabella esistente) e `wiki/stato.md` per segnare "Fondamenta" come implementato lato backend, prima di passare ai piani dei due frontend.
