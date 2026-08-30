# Frontend Professionisti — Fondamenta (login, sessione, shell, pazienti) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un professionista può accedere all'app Vue `frontend-professionisti`, avere la sessione ripristinata dopo un refresh, recuperare la password, vedere la shell di navigazione definitiva e gestire l'anagrafica dei propri pazienti (crea/lista/dettaglio/invito) — tutto contro il backend reale già esistente.

**Architecture:** SPA Vue 3 + TypeScript con `vue-router` e uno store Pinia dedicato solo alla sessione; un client HTTP minimo condiviso (niente libreria di data-fetching) da cui due moduli tipizzati (`api/auth.ts`, `api/pazienti.ts`) chiamano gli endpoint del backend Spring Boot esistente. Tre piccole aggiunte lato backend (CORS, `GET /auth/me`, URL configurabile del link di reset password) sbloccano l'integrazione reale.

**Tech Stack:** Vite, Vue 3 (`<script setup lang="ts">`), TypeScript, `vue-router`, Pinia, Tailwind CSS v4, Vitest + `@vue/test-utils` + `@pinia/testing` (frontend). Spring Boot 3.3.4, Spring Security, Spring Data JPA (backend, invariato). `shadcn-vue` (previsto dallo stack di progetto, [decisioni/0001](../../../wiki/decisioni/0001-stack-tecnologico.md)) è rimandato a quando una pagina di questa app richiederà davvero un suo componente — vedi la nota finale del piano.

**Spec:** [docs/superpowers/specs/2026-08-30-frontend-professionisti-fondamenta-design.md](../specs/2026-08-30-frontend-professionisti-fondamenta-design.md)

## Global Constraints

- Tutta la UI e i nomi di dominio sono in italiano, coerenti con il backend (`nome`, `cognome`, `email`, `ruolo`, `statoAccount`).
- TypeScript ovunque nel frontend; niente `any` se non esplicitamente motivato in un test.
- Pinia è usato **solo** per lo stato di sessione (`stores/auth.ts`). Nessuna libreria di data-fetching aggiuntiva (niente TanStack Query o simili) in questa fase.
- Struttura a cartelle piatta: `src/{api,stores,router,views,components}`. Niente cartelle `domain/`, `infrastructure/`, `services/`.
- Codice chiaro e semplice: niente astrazioni non richieste da un test o da un secondo caso d'uso reale già presente nel piano.
- Niente commenti nel codice a meno che spieghino un motivo non ovvio (mai cosa fa il codice).
- **Git**: si può creare/modificare file e fare `git add` liberamente. **Non eseguire mai `git commit`**: i commit li fa sempre e solo Andrea a mano. Ogni step "Commit" di questo piano significa in realtà "stage e segnala, non committare".
- Backend: dopo ogni modifica, eseguire per davvero `mvn test` da `backend/` con `JAVA_HOME` puntato al JDK 21 (il default di sistema è Java 8) e riportare l'esito reale.
- Frontend: dopo ogni modifica, eseguire per davvero `npm run test` da `frontend-professionisti/` e riportare l'esito reale.

---

## Task 1: Scaffold del progetto frontend

**Files:**
- Create: `frontend-professionisti/` (intero scaffold Vite: `package.json`, `vite.config.ts`, `tsconfig*.json`, `index.html`, `src/main.ts`, `src/App.vue`, `src/assets/main.css`, `src/App.spec.ts`)
- Modify: nessuno (il `CLAUDE.md` esistente va preservato, non sovrascritto)

**Interfaces:**
- Produce: un progetto Vite+Vue3+TS con Tailwind v4 e Vitest funzionanti, alias `@` → `src/`, script npm `dev`/`build`/`test`. Le task successive costruiscono su questa base.

- [ ] **Step 1: Scaffold Vite preservando il CLAUDE.md esistente**

```bash
cd frontend-professionisti
mv CLAUDE.md CLAUDE.md.bak
npm create vite@latest . -- --template vue-ts
mv CLAUDE.md.bak CLAUDE.md
npm install
```

- [ ] **Step 2: Aggiungere Tailwind v4**

```bash
npm install tailwindcss @tailwindcss/vite
```

Sostituisci il contenuto di `src/style.css` con `src/assets/main.css` (elimina `src/style.css`) e scrivi:

```css
/* src/assets/main.css */
@import "tailwindcss";

:root {
  --bg: #F7F6F2;
  --surf: #FFFFFF;
  --bd: #E6E4DC;
  --bd2: #E1E6E2;
  --div: #F0EFE9;
  --soft: #FBFBF8;
  --hover: #EFEEE8;
  --fg: #12211A;
  --fg2: #5D6B64;
  --fg3: #7C8A83;
  --fg4: #9AA6A0;
  --green: #1B5E3F;
  --green-d: #154A32;
  --on-green: #FFFFFF;
  --mint: #E3F3E9;
  --sage: #7BC299;
  --danger: #B3432E;
  --warn-bg: #FBF1E4;
}

body {
  margin: 0;
  background: var(--bg);
  color: var(--fg);
  font-family: Figtree, system-ui, sans-serif;
}
```

Questi valori sono gli stessi token già decisi nel mockup (`Hexis Login.dc.html`), non nuovi.

- [ ] **Step 3: Font e favicon in `index.html`**

Nel `<head>` di `index.html`, aggiungi prima di `<title>`:

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,400;0,9..144,500;0,9..144,600;0,9..144,700;1,9..144,500;1,9..144,600&family=Figtree:wght@400;500;600;700;800&display=swap" rel="stylesheet">
```

- [ ] **Step 4: Configurare `vite.config.ts` (plugin Tailwind, alias, Vitest)**

```ts
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
  },
})
```

Verifica che `tsconfig.app.json` contenga l'alias (se il template non lo include già, aggiungilo dentro `compilerOptions`):

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": { "@/*": ["./src/*"] }
  }
}
```

- [ ] **Step 5: Installare Vitest e Vue Test Utils**

```bash
npm install -D vitest @vue/test-utils jsdom
```

In `package.json`, aggiungi agli script:

```json
{
  "scripts": {
    "test": "vitest run",
    "test:watch": "vitest"
  }
}
```

- [ ] **Step 6: Ripulire il boilerplate del template e sostituire `App.vue`**

Elimina `src/components/HelloWorld.vue` e `src/assets/vue.svg` se presenti (non servono). Sostituisci `src/App.vue`:

```vue
<template>
  <div>Hexis Nutrition — in costruzione</div>
</template>
```

Sostituisci `src/main.ts`:

```ts
import { createApp } from 'vue'
import App from './App.vue'
import './assets/main.css'

createApp(App).mount('#app')
```

- [ ] **Step 7: Scrivere un test di verifica del toolchain**

```ts
// src/App.spec.ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import App from './App.vue'

describe('App', () => {
  it('monta senza errori', () => {
    const wrapper = mount(App)
    expect(wrapper.text()).toContain('Hexis Nutrition')
  })
})
```

- [ ] **Step 8: Verificare build e test**

```bash
npm run build
npm run test
```

Atteso: build senza errori, 1 test verde.

- [ ] **Step 9: Stage (non committare)**

```bash
git add frontend-professionisti
```

Segnala ad Andrea che lo scaffold è pronto in staging.

---

## Task 2: Backend — CORS per il frontend

**Files:**
- Modify: `backend/src/main/java/com/hexisnutrition/backend/auth/SecurityConfig.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/hexisnutrition/backend/auth/SecurityConfigTest.java`

**Interfaces:**
- Produce: proprietà `app.frontend-professionisti.url` (default `http://localhost:5173`), risposta con header `Access-Control-Allow-Origin` su richieste preflight da quell'origine.

- [ ] **Step 1: Scrivere il test che fallisce**

Aggiungi a `SecurityConfigTest.java`:

```java
    @Test
    void richiestaPreflightDaOrigineFrontendRicevaAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
```

Aggiungi gli import necessari:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
```

- [ ] **Step 2: Eseguire e verificare che fallisca**

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
cd backend
mvn test -Dtest=SecurityConfigTest
```

Atteso: FAIL (nessun header `Access-Control-Allow-Origin`, la richiesta preflight risulta bloccata o senza header CORS).

- [ ] **Step 3: Aggiungere la property in `application.yml`**

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: 480
  frontend-professionisti:
    url: ${FRONTEND_PROFESSIONISTI_URL:http://localhost:5173}
```

- [ ] **Step 4: Modificare `SecurityConfig.java`**

Sostituisci l'intero file con:

```java
package com.hexisnutrition.backend.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-professionisti.url}") String frontendUrl) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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

- [ ] **Step 5: Eseguire l'intera suite e verificare che sia verde**

```bash
mvn test
```

Atteso: BUILD SUCCESS, tutti i test verdi (inclusi i 36 preesistenti).

- [ ] **Step 6: Stage (non committare)**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/auth/SecurityConfig.java backend/src/main/resources/application.yml backend/src/test/java/com/hexisnutrition/backend/auth/SecurityConfigTest.java
```

---

## Task 3: Backend — endpoint `GET /auth/me`

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/auth/MeResponse.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/auth/AuthService.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/auth/AuthController.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java`

**Interfaces:**
- Consuma: `ProfessionistaRepository`, `PazienteRepository` (già iniettati in `AuthService`), `CredenzialiNonValideException` (esistente, `@ResponseStatus(UNAUTHORIZED)`).
- Produce: `GET /auth/me` autenticato → `MeResponse(UUID id, String nome, String cognome, String email, String ruolo)`; 401 senza token valido.

- [ ] **Step 1: Scrivere i test che falliscono**

Aggiungi a `AuthControllerTest.java` il campo mancante e i due test:

```java
    @Autowired
    private JwtService jwtService;
```

(`JwtService` è nello stesso package `com.hexisnutrition.backend.auth` di questo test, nessun import aggiuntivo serve.)

```java
    @Test
    void meRestituisceIDatiDelProfessionistaAutenticato() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "me@example.com", passwordEncoder.encode("password123"), "Anna", "Bianchi"));
        String token = jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Anna"))
                .andExpect(jsonPath("$.cognome").value("Bianchi"))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.ruolo").value("PROFESSIONISTA"));
    }

    @Test
    void meSenzaTokenRestituisce401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
```

Aggiungi l'import mancante:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
mvn test -Dtest=AuthControllerTest
```

Atteso: FAIL — `404 Not Found` su `GET /auth/me` (endpoint non esiste ancora).

- [ ] **Step 3: Creare `MeResponse.java`**

```java
package com.hexisnutrition.backend.auth;

import java.util.UUID;

public record MeResponse(UUID id, String nome, String cognome, String email, String ruolo) {
}
```

- [ ] **Step 4: Aggiungere `AuthService.me`**

In `AuthService.java`, aggiungi l'import mancante:

```java
import java.util.UUID;
```

Poi aggiungi il metodo (usa i repository già iniettati nel costruttore esistente):

```java
    public MeResponse me(UUID id, Ruolo ruolo) {
        if (ruolo == Ruolo.PROFESSIONISTA) {
            Professionista professionista = professionistaRepository.findById(id)
                    .orElseThrow(CredenzialiNonValideException::new);
            return new MeResponse(professionista.getId(), professionista.getNome(), professionista.getCognome(),
                    professionista.getEmail(), ruolo.name());
        }
        Paziente paziente = pazienteRepository.findById(id)
                .orElseThrow(CredenzialiNonValideException::new);
        return new MeResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getEmail(), ruolo.name());
    }
```

- [ ] **Step 5: Aggiungere l'endpoint in `AuthController.java`**

```java
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        UUID id = (UUID) authentication.getPrincipal();
        Ruolo ruolo = Ruolo.valueOf(authentication.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", ""));
        return authService.me(id, ruolo);
    }
```

Aggiungi gli import:

```java
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.UUID;
```

- [ ] **Step 6: Eseguire e verificare che passino**

```bash
mvn test -Dtest=AuthControllerTest
mvn test
```

Atteso: entrambi i comandi BUILD SUCCESS.

- [ ] **Step 7: Stage (non committare)**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/auth backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java
```

---

## Task 4: Backend — URL del link di reset password configurabile

**Files:**
- Modify: `backend/src/main/java/com/hexisnutrition/backend/auth/AuthService.java`
- Test: `backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java`

**Interfaces:**
- Consuma: property `app.frontend-professionisti.url` (già aggiunta nel Task 2).
- Produce: il corpo dell'email di reset password contiene `<frontend-professionisti.url>/reset-password?token=...` invece del placeholder hardcoded.

- [ ] **Step 1: Scrivere il test che fallisce**

Aggiungi a `AuthControllerTest.java`:

```java
    @Test
    void emailDiResetPasswordContieneLUrlDelFrontendConfigurato() throws Exception {
        professionistaRepository.save(new Professionista(
                "resetlink@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));

        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resetlink@example.com\"}"))
                .andExpect(status().isNoContent());

        String corpoEmail = fakeEmailSender.getInviate().get(0).corpoHtml();
        assertThat(corpoEmail).contains("http://localhost:5173/reset-password?token=");
    }
```

- [ ] **Step 2: Eseguire e verificare che fallisca**

```bash
mvn test -Dtest=AuthControllerTest
```

Atteso: FAIL — il corpo contiene ancora `https://app.hexisnutrition.example/...`.

- [ ] **Step 3: Modificare `AuthService.java`**

Aggiungi l'import (stesso pattern già usato in `ResendEmailSender`):

```java
import org.springframework.beans.factory.annotation.Value;
```

Aggiungi il campo e il parametro al costruttore esistente:

```java
    private final String frontendUrl;

    public AuthService(ProfessionistaRepository professionistaRepository,
                        PazienteRepository pazienteRepository,
                        TokenAzioneRepository tokenAzioneRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        EmailSender emailSender,
                        @Value("${app.frontend-professionisti.url}") String frontendUrl) {
        this.professionistaRepository = professionistaRepository;
        this.pazienteRepository = pazienteRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }
```

Sostituisci `corpoResetPassword`:

```java
    private String corpoResetPassword(String token) {
        return "<p>Reimposta la password: <a href=\"" + frontendUrl + "/reset-password?token="
                + token + "\">Reimposta password</a></p>";
    }
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
mvn test -Dtest=AuthControllerTest
mvn test
```

Atteso: entrambi BUILD SUCCESS (36+3 test precedenti + i nuovi di questo e del Task 3).

- [ ] **Step 5: Stage (non committare)**

```bash
git add backend/src/main/java/com/hexisnutrition/backend/auth/AuthService.java backend/src/test/java/com/hexisnutrition/backend/auth/AuthControllerTest.java
```

---

## Task 5: Frontend — client API

**Files:**
- Create: `frontend-professionisti/src/api/client.ts`
- Create: `frontend-professionisti/src/api/client.spec.ts`
- Modify: `frontend-professionisti/src/vite-env.d.ts`
- Create: `frontend-professionisti/.env.development`

**Interfaces:**
- Produce: `class ApiError extends Error { status: number }`, `function configureApiClient(options: { getToken: () => string | null; onUnauthorized: () => void }): void`, `function apiRequest<T>(path: string, options?: { method?: 'GET' | 'POST'; body?: unknown }): Promise<T>`.

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/api/client.spec.ts
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { apiRequest, ApiError, configureApiClient } from './client'

describe('apiRequest', () => {
  beforeEach(() => {
    configureApiClient({ getToken: () => null, onUnauthorized: () => {} })
    vi.stubGlobal('fetch', vi.fn())
  })

  it('invia il token come header Authorization quando presente', async () => {
    configureApiClient({ getToken: () => 'il-token', onUnauthorized: () => {} })
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }))

    await apiRequest('/qualcosa')

    const init = vi.mocked(fetch).mock.calls[0][1]
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer il-token')
  })

  it('restituisce undefined su risposta 204', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }))

    const risultato = await apiRequest('/qualcosa')

    expect(risultato).toBeUndefined()
  })

  it('lancia ApiError e notifica onUnauthorized su risposta 401', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('credenziali non valide', { status: 401 }))
    const onUnauthorized = vi.fn()
    configureApiClient({ getToken: () => 'token-scaduto', onUnauthorized })

    await expect(apiRequest('/qualcosa')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('lancia ApiError con lo status su altre risposte non ok', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('errore', { status: 500 }))

    const errore = await apiRequest('/qualcosa').catch((e) => e)

    expect(errore).toBeInstanceOf(ApiError)
    expect((errore as ApiError).status).toBe(500)
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

Atteso: FAIL — `./client` non esiste.

- [ ] **Step 3: Estendere `vite-env.d.ts`**

```ts
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
}
```

- [ ] **Step 4: Creare `.env.development`**

```
VITE_API_BASE_URL=http://localhost:8080
```

- [ ] **Step 5: Implementare `src/api/client.ts`**

```ts
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL

let getToken: () => string | null = () => null
let onUnauthorized: () => void = () => {}

export function configureApiClient(options: { getToken: () => string | null; onUnauthorized: () => void }) {
  getToken = options.getToken
  onUnauthorized = options.onUnauthorized
}

interface ApiRequestOptions {
  method?: 'GET' | 'POST'
  body?: unknown
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const token = getToken()
  const response = await fetch(BASE_URL + path, {
    method: options.method ?? 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (response.status === 401) {
    onUnauthorized()
    throw new ApiError(401, 'Sessione scaduta')
  }

  if (!response.ok) {
    const testo = await response.text()
    throw new ApiError(response.status, testo || 'Errore imprevisto')
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
```

- [ ] **Step 6: Eseguire e verificare che passino**

```bash
npm run test
```

Atteso: tutti i test verdi.

- [ ] **Step 7: Stage (non committare)**

```bash
git add frontend-professionisti/src/api frontend-professionisti/src/vite-env.d.ts frontend-professionisti/.env.development
```

---

## Task 6: Frontend — `api/auth.ts`

**Files:**
- Create: `frontend-professionisti/src/api/auth.ts`
- Create: `frontend-professionisti/src/api/auth.spec.ts`

**Interfaces:**
- Consuma: `apiRequest` da `./client` (Task 5).
- Produce: `login(request: { email: string; password: string }): Promise<{ token: string; ruolo: string }>`, `me(): Promise<{ id: string; nome: string; cognome: string; email: string; ruolo: string }>`, `richiediResetPassword(email: string): Promise<void>`, `resetPassword(token: string, nuovaPassword: string): Promise<void>`.

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/api/auth.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { login, me, richiediResetPassword, resetPassword } from './auth'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

describe('api/auth', () => {
  it('login chiama POST /auth/login con email e password', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ token: 't', ruolo: 'PROFESSIONISTA' })

    const risultato = await login({ email: 'a@b.it', password: 'segreta123' })

    expect(apiRequest).toHaveBeenCalledWith('/auth/login', {
      method: 'POST',
      body: { email: 'a@b.it', password: 'segreta123' },
    })
    expect(risultato.token).toBe('t')
  })

  it('me chiama GET /auth/me', async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA',
    })

    await me()

    expect(apiRequest).toHaveBeenCalledWith('/auth/me')
  })

  it('richiediResetPassword chiama POST /auth/password-dimenticata con l\'email', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await richiediResetPassword('a@b.it')

    expect(apiRequest).toHaveBeenCalledWith('/auth/password-dimenticata', {
      method: 'POST',
      body: { email: 'a@b.it' },
    })
  })

  it('resetPassword chiama POST /auth/reset-password con token e nuova password', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await resetPassword('il-token', 'nuovaPassword1')

    expect(apiRequest).toHaveBeenCalledWith('/auth/reset-password', {
      method: 'POST',
      body: { token: 'il-token', nuovaPassword: 'nuovaPassword1' },
    })
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

Atteso: FAIL — `./auth` non esiste.

- [ ] **Step 3: Implementare `src/api/auth.ts`**

```ts
import { apiRequest } from './client'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  ruolo: string
}

export interface MeResponse {
  id: string
  nome: string
  cognome: string
  email: string
  ruolo: string
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', { method: 'POST', body: request })
}

export function me(): Promise<MeResponse> {
  return apiRequest<MeResponse>('/auth/me')
}

export function richiediResetPassword(email: string): Promise<void> {
  return apiRequest<void>('/auth/password-dimenticata', { method: 'POST', body: { email } })
}

export function resetPassword(token: string, nuovaPassword: string): Promise<void> {
  return apiRequest<void>('/auth/reset-password', { method: 'POST', body: { token, nuovaPassword } })
}
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 5: Stage (non committare)**

```bash
git add frontend-professionisti/src/api/auth.ts frontend-professionisti/src/api/auth.spec.ts
```

---

## Task 7: Frontend — store Pinia `auth`

**Files:**
- Create: `frontend-professionisti/src/stores/auth.ts`
- Create: `frontend-professionisti/src/stores/auth.spec.ts`
- Modify: `frontend-professionisti/src/main.ts`

**Interfaces:**
- Consuma: `api/auth.ts` (`login`, `me`), `api/client.ts` (`configureApiClient`).
- Produce: `useAuthStore()` con stato `token: Ref<string | null>`, `professionista: Ref<{ id, nome, cognome, email, ruolo } | null>` e azioni `login(email: string, password: string, ricordami: boolean): Promise<void>`, `ripristinaSessione(): Promise<void>`, `logout(): void`.

- [ ] **Step 1: Installare Pinia e i pacchetti di test**

```bash
npm install pinia
npm install -D @pinia/testing
```

- [ ] **Step 2: Scrivere i test che falliscono**

```ts
// src/stores/auth.spec.ts
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import * as authApi from '@/api/auth'

vi.mock('@/api/auth')

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
  })

  it('dopo il login salva il token in localStorage se ricordami è true', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()

    await store.login('a@b.it', 'password123', true)

    expect(store.token).toBe('abc')
    expect(localStorage.getItem('hexis-auth-token')).toBe('abc')
    expect(sessionStorage.getItem('hexis-auth-token')).toBeNull()
    expect(store.professionista?.nome).toBe('Anna')
  })

  it('dopo il login salva il token in sessionStorage se ricordami è false', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()

    await store.login('a@b.it', 'password123', false)

    expect(sessionStorage.getItem('hexis-auth-token')).toBe('abc')
    expect(localStorage.getItem('hexis-auth-token')).toBeNull()
  })

  it('logout pulisce token, profilo ed entrambe le storage', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()
    await store.login('a@b.it', 'password123', true)

    store.logout()

    expect(store.token).toBeNull()
    expect(store.professionista).toBeNull()
    expect(localStorage.getItem('hexis-auth-token')).toBeNull()
  })

  it('ripristinaSessione fa logout se il token salvato non è più valido', async () => {
    localStorage.setItem('hexis-auth-token', 'scaduto')
    vi.mocked(authApi.me).mockRejectedValue(new Error('401'))
    const store = useAuthStore()

    await store.ripristinaSessione()

    expect(store.token).toBeNull()
  })

  it('ripristinaSessione non chiama /auth/me se non c\'è un token salvato', async () => {
    const store = useAuthStore()

    await store.ripristinaSessione()

    expect(authApi.me).not.toHaveBeenCalled()
  })
})
```

- [ ] **Step 3: Eseguire e verificare che falliscano**

```bash
npm run test
```

Atteso: FAIL — `./auth` (store) non esiste.

- [ ] **Step 4: Implementare `src/stores/auth.ts`**

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import { configureApiClient } from '@/api/client'

const STORAGE_KEY = 'hexis-auth-token'

interface Professionista {
  id: string
  nome: string
  cognome: string
  email: string
  ruolo: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(
    localStorage.getItem(STORAGE_KEY) ?? sessionStorage.getItem(STORAGE_KEY),
  )
  const professionista = ref<Professionista | null>(null)

  function logout() {
    token.value = null
    professionista.value = null
    localStorage.removeItem(STORAGE_KEY)
    sessionStorage.removeItem(STORAGE_KEY)
  }

  configureApiClient({
    getToken: () => token.value,
    onUnauthorized: () => logout(),
  })

  function salvaToken(nuovoToken: string, ricordami: boolean) {
    token.value = nuovoToken
    const storage = ricordami ? localStorage : sessionStorage
    storage.setItem(STORAGE_KEY, nuovoToken)
  }

  async function caricaProfilo() {
    professionista.value = await authApi.me()
  }

  async function login(email: string, password: string, ricordami: boolean) {
    const risposta = await authApi.login({ email, password })
    salvaToken(risposta.token, ricordami)
    await caricaProfilo()
  }

  async function ripristinaSessione() {
    if (!token.value) return
    try {
      await caricaProfilo()
    } catch {
      logout()
    }
  }

  return { token, professionista, login, ripristinaSessione, logout }
})
```

- [ ] **Step 5: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 6: Wire Pinia in `main.ts`**

```ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import './assets/main.css'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
app.use(createPinia())

const auth = useAuthStore()
auth.ripristinaSessione().finally(() => {
  app.mount('#app')
})
```

- [ ] **Step 7: Verificare build**

```bash
npm run build
npm run test
```

- [ ] **Step 8: Stage (non committare)**

```bash
git add frontend-professionisti/src/stores frontend-professionisti/src/main.ts frontend-professionisti/package.json frontend-professionisti/package-lock.json
```

---

## Task 8: Frontend — router e guardie di navigazione

**Files:**
- Create: `frontend-professionisti/src/router/index.ts`
- Create: `frontend-professionisti/src/router/index.spec.ts`
- Modify: `frontend-professionisti/src/main.ts`
- Modify: `frontend-professionisti/src/App.vue`
- Create (placeholder, sostituiti nei task successivi): `frontend-professionisti/src/views/LoginView.vue`, `PasswordDimenticataView.vue`, `ResetPasswordView.vue`, `DashboardView.vue`, `PazientiListView.vue`, `PazienteNuovoView.vue`, `PazienteDettaglioView.vue`

**Interfaces:**
- Consuma: `useAuthStore` (Task 7).
- Produce: `guardiaAutenticazione(to: { meta: { requiresAuth?: boolean }; name?: string; fullPath: string }, auth: { token: string | null }): true | { name: string; query?: Record<string, string> }` (esportata per test), router di default con le 7 rotte della fase.

- [ ] **Step 1: Creare i view placeholder minimi**

Crea in `src/views/` sette file, ciascuno con lo stesso template minimo ma un testo diverso, così le rotte del router (Step 4) hanno subito un componente valido da importare — il contenuto reale arriva nei Task 9-16:

```vue
<!-- LoginView.vue -->
<template><div>Login</div></template>
```

```vue
<!-- PasswordDimenticataView.vue -->
<template><div>Password dimenticata</div></template>
```

```vue
<!-- ResetPasswordView.vue -->
<template><div>Reset password</div></template>
```

```vue
<!-- DashboardView.vue -->
<template><div>Dashboard</div></template>
```

```vue
<!-- PazientiListView.vue -->
<template><div>Pazienti</div></template>
```

```vue
<!-- PazienteNuovoView.vue -->
<template><div>Nuovo paziente</div></template>
```

```vue
<!-- PazienteDettaglioView.vue -->
<template><div>Dettaglio paziente</div></template>
```

- [ ] **Step 2: Scrivere il test della guardia che fallisce**

```ts
// src/router/index.spec.ts
import { describe, expect, it } from 'vitest'
import { guardiaAutenticazione } from './index'

describe('guardiaAutenticazione', () => {
  it('rimanda al login se la rotta richiede autenticazione e non c\'è token', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: true }, name: 'pazienti', fullPath: '/pazienti' },
      { token: null },
    )
    expect(risultato).toEqual({ name: 'login', query: { redirect: '/pazienti' } })
  })

  it('lascia proseguire se la rotta richiede autenticazione e il token c\'è', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: true }, name: 'pazienti', fullPath: '/pazienti' },
      { token: 'abc' },
    )
    expect(risultato).toBe(true)
  })

  it('rimanda alla dashboard se si prova ad aprire il login da già autenticati', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: false }, name: 'login', fullPath: '/login' },
      { token: 'abc' },
    )
    expect(risultato).toEqual({ name: 'dashboard' })
  })

  it('lascia proseguire verso il login se non c\'è sessione', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: false }, name: 'login', fullPath: '/login' },
      { token: null },
    )
    expect(risultato).toBe(true)
  })
})
```

- [ ] **Step 3: Eseguire e verificare che falliscano**

```bash
npm run test
```

Atteso: FAIL — `./index` (router) non esiste.

- [ ] **Step 4: Installare vue-router e implementare il router**

```bash
npm install vue-router
```

```ts
// src/router/index.ts
import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth: boolean
  }
}

export function guardiaAutenticazione(
  to: Pick<RouteLocationNormalized, 'meta' | 'name' | 'fullPath'>,
  auth: { token: string | null },
) {
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.token) {
    return { name: 'dashboard' }
  }
  return true as const
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { requiresAuth: false } },
    { path: '/password-dimenticata', name: 'password-dimenticata', component: () => import('@/views/PasswordDimenticataView.vue'), meta: { requiresAuth: false } },
    { path: '/reset-password', name: 'reset-password', component: () => import('@/views/ResetPasswordView.vue'), meta: { requiresAuth: false } },
    { path: '/', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { requiresAuth: true } },
    { path: '/pazienti', name: 'pazienti', component: () => import('@/views/PazientiListView.vue'), meta: { requiresAuth: true } },
    { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: () => import('@/views/PazienteNuovoView.vue'), meta: { requiresAuth: true } },
    { path: '/pazienti/:id', name: 'paziente-dettaglio', component: () => import('@/views/PazienteDettaglioView.vue'), meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to) => guardiaAutenticazione(to, useAuthStore()))

export default router
```

- [ ] **Step 5: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 6: Wire router in `main.ts` e `App.vue`**

```ts
// src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
app.use(createPinia())
app.use(router)

const auth = useAuthStore()
auth.ripristinaSessione().finally(() => {
  app.mount('#app')
})
```

```vue
<!-- src/App.vue -->
<template>
  <router-view />
</template>
```

- [ ] **Step 7: Verificare build**

```bash
npm run build
npm run test
```

- [ ] **Step 8: Stage (non committare)**

```bash
git add frontend-professionisti/src/router frontend-professionisti/src/main.ts frontend-professionisti/src/App.vue frontend-professionisti/src/views frontend-professionisti/package.json frontend-professionisti/package-lock.json
```

---

## Task 9: Frontend — pagina Login

**Files:**
- Modify: `frontend-professionisti/src/views/LoginView.vue`
- Create: `frontend-professionisti/src/views/LoginView.spec.ts`
- Create: `frontend-professionisti/src/assets/hexis-logo.svg` (copia il file `hexis-logo.svg` dai mockup di Andrea)

**Interfaces:**
- Consuma: `useAuthStore` (Task 7), `ApiError` da `@/api/client` (Task 5).

- [ ] **Step 1: Installare `@pinia/testing` se non già presente e scrivere i test che falliscono**

```ts
// src/views/LoginView.spec.ts
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'
import LoginView from './LoginView.vue'

function creaRouterDiTest() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'login', component: LoginView },
      { path: '/', name: 'dashboard', component: { template: '<div>dashboard</div>' } },
      { path: '/password-dimenticata', name: 'password-dimenticata', component: { template: '<div />' } },
    ],
  })
}

describe('LoginView', () => {
  let router: ReturnType<typeof creaRouterDiTest>

  beforeEach(async () => {
    router = creaRouterDiTest()
    router.push('/login')
    await router.isReady()
  })

  it('dopo un login riuscito naviga alla dashboard', async () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()
    vi.mocked(auth.login).mockResolvedValue(undefined)

    await wrapper.find('input[type="email"]').setValue('anna@studio.it')
    await wrapper.find('input[type="password"]').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(auth.login).toHaveBeenCalledWith('anna@studio.it', 'password123', true)
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('mostra il banner di errore su credenziali non valide', async () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()
    vi.mocked(auth.login).mockRejectedValue(new ApiError(401, 'Credenziali non valide'))

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Email o password non corrette')
  })

  it('il link "password dimenticata" porta alla rotta dedicata', () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })

    const link = wrapper.find('a[href="/password-dimenticata"]')

    expect(link.exists()).toBe(true)
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm install -D @pinia/testing
npm run test
```

Atteso: FAIL — `LoginView` non ha ancora form/input.

- [ ] **Step 3: Copiare il logo**

Copia il file `hexis-logo.svg` fornito da Andrea nell'export dei mockup in `frontend-professionisti/src/assets/hexis-logo.svg`.

- [ ] **Step 4: Implementare `LoginView.vue`**

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'

const email = ref('')
const password = ref('')
const ricordami = ref(true)
const passwordVisibile = ref(false)
const inCorso = ref(false)
const erroreCredenziali = ref(false)

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function onSubmit() {
  if (inCorso.value) return
  inCorso.value = true
  erroreCredenziali.value = false
  try {
    await auth.login(email.value, password.value, ricordami.value)
    const destinazione = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(destinazione)
  } catch (errore) {
    if (errore instanceof ApiError && errore.status === 401) {
      erroreCredenziali.value = true
    } else {
      throw errore
    }
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="grid min-h-screen grid-cols-1 md:grid-cols-2" style="background: var(--bg)">
    <div class="hidden flex-col justify-between overflow-hidden p-11 md:flex" style="background: var(--green)">
      <div class="flex items-center gap-3.5">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-14 w-14 rounded-2xl bg-white p-1.5" />
        <span class="text-2xl font-semibold text-white" style="font-family: Fraunces, serif">Hexis Nutrition</span>
      </div>
      <div class="max-w-md">
        <p class="text-3xl italic text-white" style="font-family: Fraunces, serif">
          Il tuo studio nutrizionale, in un solo posto.
        </p>
        <p class="mt-3.5 text-sm text-white/75">
          Pazienti, piani alimentari e agenda: tutto quello che serve alla tua professione, ogni giorno.
        </p>
      </div>
      <p class="text-xs font-medium text-white/50">© 2026 Hexis Nutrition</p>
    </div>

    <div class="flex items-center justify-center p-10">
      <form class="w-full max-w-[360px]" @submit.prevent="onSubmit">
        <h1 class="text-2xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Accedi</h1>
        <p class="mb-6 mt-1.5 text-sm" style="color: var(--fg3)">Inserisci le tue credenziali per continuare</p>

        <div
          v-if="erroreCredenziali"
          class="mb-4 rounded-lg border px-3 py-2.5 text-sm font-semibold"
          style="background: var(--warn-bg); border-color: var(--bd2); color: var(--danger)"
        >
          Email o password non corrette.
        </div>

        <label class="mb-3.5 flex flex-col gap-1.5">
          <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Email</span>
          <input
            v-model="email"
            type="email"
            required
            autocomplete="username"
            placeholder="nome@studio.it"
            class="rounded-lg border px-3 py-2.5 text-sm"
            style="border-color: var(--bd2); background: var(--surf)"
          />
        </label>

        <label class="mb-2.5 flex flex-col gap-1.5">
          <span class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Password</span>
            <router-link to="/password-dimenticata" class="text-xs font-semibold">Password dimenticata?</router-link>
          </span>
          <span class="flex items-center rounded-lg border pl-3" style="border-color: var(--bd2); background: var(--surf)">
            <input
              v-model="password"
              :type="passwordVisibile ? 'text' : 'password'"
              required
              autocomplete="current-password"
              placeholder="••••••••"
              class="min-w-0 flex-1 border-0 bg-transparent py-2.5 text-sm outline-none"
            />
            <button
              type="button"
              class="m-1.5 flex h-[30px] w-[30px] items-center justify-center rounded-md"
              :aria-label="passwordVisibile ? 'Nascondi password' : 'Mostra password'"
              @click="passwordVisibile = !passwordVisibile"
            >
              <svg v-if="!passwordVisibile" width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1.5 8s2.3-4.5 6.5-4.5S14.5 8 14.5 8s-2.3 4.5-6.5 4.5S1.5 8 1.5 8z"></path><circle cx="8" cy="8" r="1.8"></circle></svg>
              <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1.5 8s2.3-4.5 6.5-4.5S14.5 8 14.5 8s-2.3 4.5-6.5 4.5S1.5 8 1.5 8z"></path><circle cx="8" cy="8" r="1.8"></circle><path d="M2 2l12 12"></path></svg>
            </button>
          </span>
        </label>

        <label class="my-1.5 mb-5 flex items-center gap-2">
          <input v-model="ricordami" type="checkbox" />
          <span class="text-sm" style="color: var(--fg2)">Ricordami su questo dispositivo</span>
        </label>

        <button
          type="submit"
          :disabled="inCorso"
          class="w-full rounded-lg py-3 text-sm font-bold text-white disabled:opacity-70"
          style="background: var(--green)"
        >
          {{ inCorso ? 'Accesso in corso…' : 'Accedi' }}
        </button>
      </form>
    </div>
  </div>
</template>
```

- [ ] **Step 5: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 6: Stage (non committare)**

```bash
git add frontend-professionisti/src/views/LoginView.vue frontend-professionisti/src/views/LoginView.spec.ts frontend-professionisti/src/assets/hexis-logo.svg
```

---

## Task 10: Frontend — pagina "Password dimenticata"

**Files:**
- Modify: `frontend-professionisti/src/views/PasswordDimenticataView.vue`
- Create: `frontend-professionisti/src/views/PasswordDimenticataView.spec.ts`

**Interfaces:**
- Consuma: `richiediResetPassword` da `@/api/auth` (Task 6).

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/views/PasswordDimenticataView.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import PasswordDimenticataView from './PasswordDimenticataView.vue'
import * as authApi from '@/api/auth'

vi.mock('@/api/auth')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/password-dimenticata', name: 'password-dimenticata', component: PasswordDimenticataView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('PasswordDimenticataView', () => {
  it('mostra il messaggio generico dopo l\'invio riuscito', async () => {
    vi.mocked(authApi.richiediResetPassword).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/password-dimenticata')
    await router.isReady()
    const wrapper = mount(PasswordDimenticataView, { global: { plugins: [router] } })

    await wrapper.find('input[type="email"]').setValue('a@b.it')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain("Se l'indirizzo esiste")
  })

  it('mostra un errore di rete se la chiamata fallisce', async () => {
    vi.mocked(authApi.richiediResetPassword).mockRejectedValue(new Error('rete'))
    const router = creaRouter()
    router.push('/password-dimenticata')
    await router.isReady()
    const wrapper = mount(PasswordDimenticataView, { global: { plugins: [router] } })

    await wrapper.find('input[type="email"]').setValue('a@b.it')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).not.toContain("Se l'indirizzo esiste")
    expect(wrapper.text().toLowerCase()).toContain('errore')
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

- [ ] **Step 3: Implementare `PasswordDimenticataView.vue`**

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { richiediResetPassword } from '@/api/auth'

const email = ref('')
const inviato = ref(false)
const erroreRete = ref(false)
const inCorso = ref(false)

async function onSubmit() {
  if (inCorso.value) return
  inCorso.value = true
  erroreRete.value = false
  try {
    await richiediResetPassword(email.value)
    inviato.value = true
  } catch {
    erroreRete.value = true
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center" style="background: var(--bg)">
    <div class="w-full max-w-[360px]">
      <h1 class="text-2xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Password dimenticata</h1>

      <p v-if="inviato" class="mt-4 text-sm" style="color: var(--fg2)">
        Se l'indirizzo esiste, riceverai un'email con le istruzioni per reimpostare la password.
      </p>

      <form v-else class="mt-4" @submit.prevent="onSubmit">
        <p v-if="erroreRete" class="mb-4 text-sm font-semibold" style="color: var(--danger)">
          Errore di rete, riprova.
        </p>

        <label class="mb-5 flex flex-col gap-1.5">
          <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Email</span>
          <input
            v-model="email"
            type="email"
            required
            placeholder="nome@studio.it"
            class="rounded-lg border px-3 py-2.5 text-sm"
            style="border-color: var(--bd2); background: var(--surf)"
          />
        </label>

        <button
          type="submit"
          :disabled="inCorso"
          class="w-full rounded-lg py-3 text-sm font-bold text-white disabled:opacity-70"
          style="background: var(--green)"
        >
          {{ inCorso ? 'Invio in corso…' : 'Invia istruzioni' }}
        </button>
      </form>

      <router-link to="/login" class="mt-4 inline-block text-sm font-semibold">← Torna al login</router-link>
    </div>
  </div>
</template>
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 5: Stage (non committare)**

```bash
git add frontend-professionisti/src/views/PasswordDimenticataView.vue frontend-professionisti/src/views/PasswordDimenticataView.spec.ts
```

---

## Task 11: Frontend — pagina "Imposta nuova password"

**Files:**
- Modify: `frontend-professionisti/src/views/ResetPasswordView.vue`
- Create: `frontend-professionisti/src/views/ResetPasswordView.spec.ts`

**Interfaces:**
- Consuma: `resetPassword` da `@/api/auth` (Task 6), `ApiError` da `@/api/client` (Task 5).

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/views/ResetPasswordView.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import ResetPasswordView from './ResetPasswordView.vue'
import * as authApi from '@/api/auth'
import { ApiError } from '@/api/client'

vi.mock('@/api/auth')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/reset-password', name: 'reset-password', component: ResetPasswordView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
      { path: '/password-dimenticata', name: 'password-dimenticata', component: { template: '<div/>' } },
    ],
  })
}

describe('ResetPasswordView', () => {
  it('segnala se le due password non coincidono, senza chiamare l\'API', async () => {
    const router = creaRouter()
    router.push('/reset-password?token=abc')
    await router.isReady()
    const wrapper = mount(ResetPasswordView, { global: { plugins: [router] } })

    await wrapper.findAll('input[type="password"]')[0].setValue('password123')
    await wrapper.findAll('input[type="password"]')[1].setValue('altra1234')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.text()).toContain('non coincidono')
    expect(authApi.resetPassword).not.toHaveBeenCalled()
  })

  it('su successo naviga al login', async () => {
    vi.mocked(authApi.resetPassword).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/reset-password?token=abc')
    await router.isReady()
    const wrapper = mount(ResetPasswordView, { global: { plugins: [router] } })

    await wrapper.findAll('input[type="password"]')[0].setValue('password123')
    await wrapper.findAll('input[type="password"]')[1].setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authApi.resetPassword).toHaveBeenCalledWith('abc', 'password123')
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('mostra un messaggio dedicato se il token non è più valido', async () => {
    vi.mocked(authApi.resetPassword).mockRejectedValue(new ApiError(400, 'Token non valido'))
    const router = creaRouter()
    router.push('/reset-password?token=scaduto')
    await router.isReady()
    const wrapper = mount(ResetPasswordView, { global: { plugins: [router] } })

    await wrapper.findAll('input[type="password"]')[0].setValue('password123')
    await wrapper.findAll('input[type="password"]')[1].setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('non è più valido')
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

- [ ] **Step 3: Implementare `ResetPasswordView.vue`**

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword } from '@/api/auth'
import { ApiError } from '@/api/client'

const route = useRoute()
const router = useRouter()
const token = typeof route.query.token === 'string' ? route.query.token : ''

const nuovaPassword = ref('')
const conferma = ref('')
const inCorso = ref(false)
const errore = ref('')

async function onSubmit() {
  errore.value = ''
  if (nuovaPassword.value.length < 8) {
    errore.value = 'La password deve avere almeno 8 caratteri.'
    return
  }
  if (nuovaPassword.value !== conferma.value) {
    errore.value = 'Le due password non coincidono.'
    return
  }
  inCorso.value = true
  try {
    await resetPassword(token, nuovaPassword.value)
    router.push({ name: 'login' })
  } catch (e) {
    errore.value = e instanceof ApiError && e.status === 400
      ? 'Il link non è più valido: richiedine uno nuovo.'
      : 'Errore imprevisto, riprova.'
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center" style="background: var(--bg)">
    <form class="w-full max-w-[360px]" @submit.prevent="onSubmit">
      <h1 class="text-2xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Imposta una nuova password</h1>

      <div
        v-if="errore"
        class="mt-4 rounded-lg border px-3 py-2.5 text-sm font-semibold"
        style="background: var(--warn-bg); border-color: var(--bd2); color: var(--danger)"
      >
        {{ errore }}
        <router-link v-if="errore.includes('richiedine')" to="/password-dimenticata" class="ml-1 underline">
          Richiedi un nuovo link
        </router-link>
      </div>

      <label class="mb-3.5 mt-4 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Nuova password</span>
        <input v-model="nuovaPassword" type="password" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label class="mb-5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Conferma password</span>
        <input v-model="conferma" type="password" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <button
        type="submit"
        :disabled="inCorso"
        class="w-full rounded-lg py-3 text-sm font-bold text-white disabled:opacity-70"
        style="background: var(--green)"
      >
        {{ inCorso ? 'Salvataggio…' : 'Imposta password' }}
      </button>
    </form>
  </div>
</template>
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 5: Stage (non committare)**

```bash
git add frontend-professionisti/src/views/ResetPasswordView.vue frontend-professionisti/src/views/ResetPasswordView.spec.ts
```

---

## Task 12: Frontend — `api/pazienti.ts`

**Files:**
- Create: `frontend-professionisti/src/api/pazienti.ts`
- Create: `frontend-professionisti/src/api/pazienti.spec.ts`

**Interfaces:**
- Consuma: `apiRequest` da `./client` (Task 5).
- Produce: `interface Paziente { id, nome, cognome, email, telefono, dataNascita, sesso, altezzaCm, statoAccount }`, `lista(): Promise<Paziente[]>`, `dettaglio(id: string): Promise<Paziente>`, `crea(request: CreaPazienteRequest): Promise<Paziente>`, `invita(id: string): Promise<void>`.

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/api/pazienti.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { lista, dettaglio, crea, invita } from './pazienti'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO',
}

describe('api/pazienti', () => {
  it('lista chiama GET /pazienti', async () => {
    vi.mocked(apiRequest).mockResolvedValue([pazienteEsempio])

    const risultato = await lista()

    expect(apiRequest).toHaveBeenCalledWith('/pazienti')
    expect(risultato).toEqual([pazienteEsempio])
  })

  it('dettaglio chiama GET /pazienti/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pazienteEsempio)

    await dettaglio('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1')
  })

  it('crea chiama POST /pazienti con i dati del form', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pazienteEsempio)

    await crea({ nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com' })

    expect(apiRequest).toHaveBeenCalledWith('/pazienti', {
      method: 'POST',
      body: { nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com' },
    })
  })

  it('invita chiama POST /pazienti/{id}/invito', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await invita('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/invito', { method: 'POST' })
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

- [ ] **Step 3: Implementare `src/api/pazienti.ts`**

```ts
import { apiRequest } from './client'

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

export interface CreaPazienteRequest {
  nome: string
  cognome: string
  email: string
  telefono?: string
  dataNascita?: string
  sesso?: string
  altezzaCm?: number
}

export function lista(): Promise<Paziente[]> {
  return apiRequest<Paziente[]>('/pazienti')
}

export function dettaglio(id: string): Promise<Paziente> {
  return apiRequest<Paziente>(`/pazienti/${id}`)
}

export function crea(request: CreaPazienteRequest): Promise<Paziente> {
  return apiRequest<Paziente>('/pazienti', { method: 'POST', body: request })
}

export function invita(id: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${id}/invito`, { method: 'POST' })
}
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 5: Stage (non committare)**

```bash
git add frontend-professionisti/src/api/pazienti.ts frontend-professionisti/src/api/pazienti.spec.ts
```

---

## Task 13: Frontend — shell dell'app (sidebar + header) e Dashboard

**Files:**
- Create: `frontend-professionisti/src/components/AppSidebar.vue`
- Create: `frontend-professionisti/src/components/AppHeader.vue`
- Create: `frontend-professionisti/src/components/AppHeader.spec.ts`
- Create: `frontend-professionisti/src/components/AppShell.vue`
- Modify: `frontend-professionisti/src/views/DashboardView.vue`
- Create: `frontend-professionisti/src/views/DashboardView.spec.ts`

**Interfaces:**
- Consuma: `useAuthStore` (Task 7, per il chip profilo e il logout), `lista` da `@/api/pazienti` (Task 12, per il conteggio pazienti attivi).
- Produce: componente `AppShell` con `<slot />` per il contenuto di pagina, usato da tutte le viste autenticate successive.

- [ ] **Step 1: Implementare `AppSidebar.vue`**

Nessun test dedicato: è puro markup di navigazione, l'unica logica (evidenziare la voce attiva confrontando `route.name`) è una derivazione a riga singola già coperta indirettamente da qualunque test che monta `AppShell`/le viste nei task successivi.

```vue
<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'

const voci = [
  { nome: 'Dashboard', routeName: 'dashboard', icona: '▦' },
  { nome: 'Agenda', icona: '📅' },
  { nome: 'Pazienti', routeName: 'pazienti', icona: '👥' },
  { nome: 'Piani alimentari', icona: '📄' },
  { nome: 'Chat', icona: '💬' },
  { nome: 'Analytics', icona: '📊' },
]

const route = useRoute()
</script>

<template>
  <aside class="flex w-64 flex-shrink-0 flex-col justify-between p-4" style="background: var(--green-d)">
    <div>
      <div class="mb-6 flex items-center gap-2 px-2">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-9 w-9 rounded-lg bg-white p-1" />
        <div>
          <div class="font-semibold text-white">Hexis</div>
          <div class="text-[10px] uppercase tracking-wide text-white/60">Professionisti</div>
        </div>
      </div>

      <nav class="flex flex-col gap-1">
        <RouterLink
          v-for="voce in voci.filter((v) => v.routeName)"
          :key="voce.nome"
          :to="{ name: voce.routeName }"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium"
          :class="route.name === voce.routeName ? 'bg-white/10 text-white' : 'text-white/80 hover:bg-white/5'"
        >
          <span>{{ voce.icona }}</span>
          <span>{{ voce.nome }}</span>
        </RouterLink>
        <span
          v-for="voce in voci.filter((v) => !v.routeName)"
          :key="voce.nome"
          class="flex cursor-not-allowed items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium text-white/35"
        >
          <span>{{ voce.icona }}</span>
          <span>{{ voce.nome }}</span>
        </span>
      </nav>
    </div>

    <div class="px-2 text-[10px] uppercase tracking-wide text-white/40">
      Risorse
      <div class="mt-2 flex items-center gap-2.5 text-sm font-medium text-white/35">
        <span>🍎</span><span>Alimenti</span>
      </div>
    </div>
  </aside>
</template>
```

- [ ] **Step 2: Scrivere il test del logout in `AppHeader.vue` (fallisce: il componente non esiste ancora)**

```ts
// src/components/AppHeader.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from './AppHeader.vue'

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('AppHeader', () => {
  it('il click su "Esci" fa logout e naviga al login', async () => {
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(AppHeader, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()

    await wrapper.find('button').trigger('click')

    expect(auth.logout).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.name).toBe('login')
  })
})
```

- [ ] **Step 3: Eseguire e verificare che fallisca**

```bash
npm run test
```

- [ ] **Step 4: Implementare `AppHeader.vue`**

```vue
<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function onLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="flex items-center justify-end gap-3 px-6 py-3" style="background: var(--surf)">
    <div class="flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm" style="border-color: var(--bd2)">
      <span
        class="flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold text-white"
        style="background: var(--green)"
      >
        {{ auth.professionista?.nome?.[0] }}{{ auth.professionista?.cognome?.[0] }}
      </span>
      <span class="font-semibold" style="color: var(--fg)">
        {{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}
      </span>
      <button type="button" class="ml-2 text-xs font-semibold" style="color: var(--fg3)" @click="onLogout">
        Esci
      </button>
    </div>
  </header>
</template>
```

- [ ] **Step 5: Eseguire e verificare che passi**

```bash
npm run test
```

- [ ] **Step 6: Implementare `AppShell.vue`**

```vue
<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
</script>

<template>
  <div class="flex min-h-screen" style="background: var(--bg)">
    <AppSidebar />
    <div class="flex flex-1 flex-col">
      <AppHeader />
      <main class="flex-1 p-8">
        <slot />
      </main>
    </div>
  </div>
</template>
```

- [ ] **Step 7: Scrivere il test della Dashboard che fallisce**

```ts
// src/views/DashboardView.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import DashboardView from './DashboardView.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: DashboardView },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('DashboardView', () => {
  it('mostra il numero di pazienti attivi calcolato dalla lista reale', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([
      { id: '1', nome: 'A', cognome: 'A', email: 'a@a.it', telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'ATTIVO' },
      { id: '2', nome: 'B', cognome: 'B', email: 'b@b.it', telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'ATTIVO' },
      { id: '3', nome: 'C', cognome: 'C', email: 'c@c.it', telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO' },
    ])
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(DashboardView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('Disponibile a breve')
  })

  it('mostra un trattino se il caricamento fallisce', async () => {
    vi.mocked(pazientiApi.lista).mockRejectedValue(new Error('rete'))
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(DashboardView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('—')
  })
})
```

- [ ] **Step 8: Eseguire e verificare che falliscano**

```bash
npm run test
```

- [ ] **Step 9: Implementare `DashboardView.vue`**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { lista } from '@/api/pazienti'

const pazientiAttivi = ref<number | null>(null)
const erroreCaricamento = ref(false)

onMounted(async () => {
  try {
    const pazienti = await lista()
    pazientiAttivi.value = pazienti.filter((p) => p.statoAccount === 'ATTIVO').length
  } catch {
    erroreCaricamento.value = true
  }
})
</script>

<template>
  <AppShell>
    <h1 class="text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Bentornata</h1>

    <div class="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
      <div class="rounded-xl border p-4" style="border-color: var(--bd2); background: var(--surf)">
        <div class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Pazienti attivi</div>
        <div class="mt-2 text-3xl font-semibold" style="color: var(--fg)">
          {{ erroreCaricamento ? '—' : (pazientiAttivi ?? '…') }}
        </div>
      </div>

      <div
        v-for="titolo in ['Visite oggi', 'Piani in scadenza', 'Messaggi non letti']"
        :key="titolo"
        class="rounded-xl border p-4 opacity-60"
        style="border-color: var(--bd2); background: var(--surf)"
      >
        <div class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">{{ titolo }}</div>
        <div class="mt-2 text-sm" style="color: var(--fg4)">Disponibile a breve</div>
      </div>
    </div>
  </AppShell>
</template>
```

- [ ] **Step 10: Eseguire e verificare che passino**

```bash
npm run test
npm run build
```

- [ ] **Step 11: Stage (non committare)**

```bash
git add frontend-professionisti/src/components frontend-professionisti/src/views/DashboardView.vue frontend-professionisti/src/views/DashboardView.spec.ts
```

---

## Task 14: Frontend — lista pazienti

**Files:**
- Modify: `frontend-professionisti/src/views/PazientiListView.vue`
- Create: `frontend-professionisti/src/views/PazientiListView.spec.ts`

**Interfaces:**
- Consuma: `lista`, `invita`, `type Paziente` da `@/api/pazienti` (Task 12), `AppShell` (Task 13).

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/views/PazientiListView.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazientiListView from './PazientiListView.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pazienti', name: 'pazienti', component: PazientiListView },
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: { template: '<div/>' } },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO' as const,
}

describe('PazientiListView', () => {
  it('mostra i pazienti caricati dal backend', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([pazienteEsempio])
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('Invita')
  })

  it('filtra i pazienti in base al testo di ricerca', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([
      pazienteEsempio,
      { ...pazienteEsempio, id: '2', nome: 'Marco', cognome: 'Bianchi', email: 'marco@example.com' },
    ])
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    await wrapper.find('input[type="search"]').setValue('marco')

    expect(wrapper.text()).toContain('Marco Bianchi')
    expect(wrapper.text()).not.toContain('Luca Verdi')
  })

  it('invita un paziente e ne aggiorna lo stato in tabella', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([pazienteEsempio])
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('INVITATO')
  })

  it('mostra uno stato vuoto se non ci sono pazienti', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([])
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Nessun paziente')
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

- [ ] **Step 3: Implementare `PazientiListView.vue`**

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { lista, invita, type Paziente } from '@/api/pazienti'

const pazienti = ref<Paziente[]>([])
const ricerca = ref('')
const caricamento = ref(true)
const errore = ref(false)
const invitoInCorsoId = ref<string | null>(null)

const pazientiFiltrati = computed(() => {
  const termine = ricerca.value.trim().toLowerCase()
  if (!termine) return pazienti.value
  return pazienti.value.filter((p) =>
    `${p.nome} ${p.cognome} ${p.email}`.toLowerCase().includes(termine),
  )
})

async function carica() {
  caricamento.value = true
  errore.value = false
  try {
    pazienti.value = await lista()
  } catch {
    errore.value = true
  } finally {
    caricamento.value = false
  }
}

async function onInvita(paziente: Paziente) {
  invitoInCorsoId.value = paziente.id
  try {
    await invita(paziente.id)
    paziente.statoAccount = 'INVITATO'
  } finally {
    invitoInCorsoId.value = null
  }
}

function etichettaAzione(paziente: Paziente) {
  if (paziente.statoAccount === 'MAI_INVITATO') return 'Invita'
  if (paziente.statoAccount === 'INVITATO') return 'Reinvia invito'
  return null
}

onMounted(carica)
</script>

<template>
  <AppShell>
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Pazienti</h1>
      <router-link
        to="/pazienti/nuovo"
        class="rounded-lg px-4 py-2 text-sm font-bold text-white"
        style="background: var(--green)"
      >
        + Nuovo paziente
      </router-link>
    </div>

    <input
      v-model="ricerca"
      type="search"
      placeholder="Cerca per nome, cognome o email"
      class="mb-4 w-full max-w-sm rounded-lg border px-3 py-2 text-sm"
      style="border-color: var(--bd2); background: var(--surf)"
    />

    <p v-if="errore" style="color: var(--danger)">Non è stato possibile caricare i pazienti.</p>
    <p v-else-if="caricamento" style="color: var(--fg3)">Caricamento…</p>
    <p v-else-if="pazientiFiltrati.length === 0" style="color: var(--fg3)">Nessun paziente, per ora.</p>

    <table v-else class="w-full text-left text-sm">
      <thead>
        <tr style="color: var(--fg3)">
          <th class="pb-2">Nome</th>
          <th class="pb-2">Email</th>
          <th class="pb-2">Stato</th>
          <th class="pb-2"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="paziente in pazientiFiltrati" :key="paziente.id" class="border-t" style="border-color: var(--div)">
          <td class="py-2">
            <router-link :to="`/pazienti/${paziente.id}`" class="font-medium" style="color: var(--fg)">
              {{ paziente.nome }} {{ paziente.cognome }}
            </router-link>
          </td>
          <td class="py-2" style="color: var(--fg2)">{{ paziente.email }}</td>
          <td class="py-2">{{ paziente.statoAccount }}</td>
          <td class="py-2 text-right">
            <button
              v-if="etichettaAzione(paziente)"
              type="button"
              :disabled="invitoInCorsoId === paziente.id"
              class="text-xs font-semibold"
              style="color: var(--green)"
              @click="onInvita(paziente)"
            >
              {{ etichettaAzione(paziente) }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </AppShell>
</template>
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 5: Stage (non committare)**

```bash
git add frontend-professionisti/src/views/PazientiListView.vue frontend-professionisti/src/views/PazientiListView.spec.ts
```

---

## Task 15: Frontend — crea paziente

**Files:**
- Modify: `frontend-professionisti/src/views/PazienteNuovoView.vue`
- Create: `frontend-professionisti/src/views/PazienteNuovoView.spec.ts`

**Interfaces:**
- Consuma: `crea` da `@/api/pazienti` (Task 12), `AppShell` (Task 13).

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/views/PazienteNuovoView.spec.ts
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
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: PazienteNuovoView },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('PazienteNuovoView', () => {
  it('crea il paziente e naviga al suo dettaglio', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '42', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com', telefono: undefined,
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
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile creare il paziente')
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

- [ ] **Step 3: Implementare `PazienteNuovoView.vue`**

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { crea } from '@/api/pazienti'

const nome = ref('')
const cognome = ref('')
const email = ref('')
const telefono = ref('')
const inCorso = ref(false)
const errore = ref('')

const router = useRouter()

async function onSubmit() {
  inCorso.value = true
  errore.value = ''
  try {
    const paziente = await crea({
      nome: nome.value,
      cognome: cognome.value,
      email: email.value,
      telefono: telefono.value || undefined,
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
    <h1 class="mb-6 text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Nuovo paziente</h1>

    <form class="max-w-md" @submit.prevent="onSubmit">
      <p v-if="errore" class="mb-4 text-sm font-semibold" style="color: var(--danger)">{{ errore }}</p>

      <label for="nome" class="mb-3.5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Nome</span>
        <input id="nome" v-model="nome" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label for="cognome" class="mb-3.5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Cognome</span>
        <input id="cognome" v-model="cognome" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label for="email" class="mb-3.5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Email</span>
        <input id="email" v-model="email" type="email" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label for="telefono" class="mb-5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Telefono</span>
        <input id="telefono" v-model="telefono" class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <button
        type="submit"
        :disabled="inCorso"
        class="rounded-lg px-4 py-2.5 text-sm font-bold text-white disabled:opacity-70"
        style="background: var(--green)"
      >
        {{ inCorso ? 'Salvataggio…' : 'Crea paziente' }}
      </button>
    </form>
  </AppShell>
</template>
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
npm run test
```

- [ ] **Step 5: Stage (non committare)**

```bash
git add frontend-professionisti/src/views/PazienteNuovoView.vue frontend-professionisti/src/views/PazienteNuovoView.spec.ts
```

---

## Task 16: Frontend — dettaglio paziente

**Files:**
- Modify: `frontend-professionisti/src/views/PazienteDettaglioView.vue`
- Create: `frontend-professionisti/src/views/PazienteDettaglioView.spec.ts`

**Interfaces:**
- Consuma: `dettaglio`, `invita`, `type Paziente` da `@/api/pazienti` (Task 12), `AppShell` (Task 13).

- [ ] **Step 1: Scrivere i test che falliscono**

```ts
// src/views/PazienteDettaglioView.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteDettaglioView from './PazienteDettaglioView.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: PazienteDettaglioView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('PazienteDettaglioView', () => {
  it('mostra i dati del paziente caricato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('luca@example.com')
  })

  it('invita il paziente e ne aggiorna lo stato mostrato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO',
    })
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('INVITATO')
  })

  it('mostra un messaggio se il paziente non è stato trovato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockRejectedValue(new Error('404'))
    const router = creaRouter()
    router.push('/pazienti/999')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Paziente non trovato')
  })
})
```

- [ ] **Step 2: Eseguire e verificare che falliscano**

```bash
npm run test
```

- [ ] **Step 3: Implementare `PazienteDettaglioView.vue`**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, type Paziente } from '@/api/pazienti'

const route = useRoute()
const paziente = ref<Paziente | null>(null)
const errore = ref(false)
const invitoInCorso = ref(false)

async function carica() {
  try {
    paziente.value = await dettaglio(route.params.id as string)
  } catch {
    errore.value = true
  }
}

async function onInvita() {
  if (!paziente.value) return
  invitoInCorso.value = true
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
  } finally {
    invitoInCorso.value = false
  }
}

onMounted(carica)
</script>

<template>
  <AppShell>
    <p v-if="errore" style="color: var(--danger)">Paziente non trovato.</p>
    <div v-else-if="paziente">
      <h1 class="text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">
        {{ paziente.nome }} {{ paziente.cognome }}
      </h1>
      <p style="color: var(--fg2)">{{ paziente.email }}</p>
      <p style="color: var(--fg3)">Stato account: {{ paziente.statoAccount }}</p>

      <button
        v-if="paziente.statoAccount !== 'ATTIVO'"
        type="button"
        :disabled="invitoInCorso"
        class="mt-4 rounded-lg px-4 py-2.5 text-sm font-bold text-white disabled:opacity-70"
        style="background: var(--green)"
        @click="onInvita"
      >
        {{ paziente.statoAccount === 'MAI_INVITATO' ? 'Invita' : 'Reinvia invito' }}
      </button>
    </div>
  </AppShell>
</template>
```

- [ ] **Step 4: Eseguire e verificare che passino**

```bash
npm run test
npm run build
```

- [ ] **Step 5: Stage (non committare)**

```bash
git add frontend-professionisti/src/views/PazienteDettaglioView.vue frontend-professionisti/src/views/PazienteDettaglioView.spec.ts
```

---

## Nota finale (fuori dai task)

`shadcn-vue` non è stato introdotto come dipendenza in questo piano: tutti i componenti sono `<script setup>` con Tailwind diretto, che è sufficiente per la superficie di questa fase (form, tabella, sidebar). Se una pagina futura (Piano alimentare, Agenda) richiederà componenti più complessi (combobox, date picker, dialog), integrare `shadcn-vue` in quel momento con `npx shadcn-vue@latest init`, mappando i suoi token CSS semantici (`--primary`, `--background`, ecc.) sui valori già definiti in `src/assets/main.css` — evita di introdurre ora una dipendenza che nessun task di questa fase usa davvero (YAGNI).

Al termine dei 16 task, verificare a mano (compito di Andrea, non dell'agente): avviare `mvn spring-boot:run` nel backend e `npm run dev` nel frontend, aprire `http://localhost:5173`, provare login → dashboard → pazienti → crea paziente → invita → controllare l'email nel log/Resend → password dimenticata → reset password.
