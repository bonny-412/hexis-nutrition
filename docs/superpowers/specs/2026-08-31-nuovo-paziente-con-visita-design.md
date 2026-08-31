# Design — Pagina "Nuovo paziente" con dati anagrafici + prima visita

## Contesto

Oggi `PazienteNuovoView.vue` raccoglie solo nome, cognome, email, telefono. Andrea vuole che la pagina di inserimento di un nuovo paziente copra l'intera prima visita: i dati anagrafici completi (incluso il livello di attività lavorativa, usato in futuro per calcoli di fabbisogno calorico) e le misure antropometriche rilevate alla prima visita. Questo introduce una nuova entità di dominio, `Visita`, che anticipa il sotto-progetto "Monitoraggio" della roadmap (vedi [architettura](../../../wiki/architettura.md)) limitatamente alla prima visita contestuale alla creazione del paziente — le visite successive (storico, endpoint dedicato) restano fuori scope.

## Decisioni prese in fase di brainstorming

- **Altezza**: spostata da `Paziente` a `Visita` (storicizzata per ogni visita, anche se di norma costante). Verificato che `altezzaCm` non è oggi mostrata in nessuna view del frontend — solo nel tipo/DTO e nei mock di test — quindi la rimozione da `Paziente` non rompe UI esistente.
- **Obbligatorietà misure**: in `Visita`, solo `altezzaCm` e `pesoKg` sono obbligatori; tutte le circonferenze sono opzionali.
- **BIA**: nessun campo dati per ora (i campi non sono ancora noti). In UI, sotto-sezione placeholder dentro la card "Dati della visita" con testo "Sarà disponibile a breve" — non è una card separata.
- **Tipo Lavoro**: enum a 4 valori — `SEDENTARIO`, `POCO_ATTIVO`, `ATTIVO`, `MOLTO_ATTIVO`.
- **Lavoro vs Tipo Lavoro**: due campi distinti. `lavoro` = testo libero (professione/mansione). `tipoLavoro` = livello di attività dal menu a tendina.
- **Nome entità**: `Visita` (non `Misurazione`, per lasciare libero quel nome al futuro sotto-progetto "Monitoraggio", che potrà referenziare/estendere `Visita`).
- **Spalle**: due misure distinte — `larghezzaSpalleCm` (ampiezza, misura ossea) e `circonferenzaSpalleCm` (circonferenza).
- **API**: un'unica chiamata `POST /pazienti` (path REST esistente, non un path dedicato), con payload esteso che include un oggetto `visita` obbligatorio. Creazione di `Paziente` e `Visita` nella stessa transazione.

## Modello dati

### Nuova tabella `visite` (migrazione `V5__create_visite.sql`)

| Colonna | Tipo | Obbligatorio |
|---|---|---|
| `id` | UUID PK | sì |
| `paziente_id` | UUID FK → `pazienti(id)` | sì |
| `data_visita` | DATE, default `CURRENT_DATE` | sì |
| `altezza_cm` | INTEGER | **sì** |
| `peso_kg` | NUMERIC(5,1) | **sì** |
| `circonferenza_vita_cm` | NUMERIC(5,1) | no |
| `circonferenza_ombelico_cm` | NUMERIC(5,1) | no |
| `circonferenza_fianchi_cm` | NUMERIC(5,1) | no |
| `circonferenza_petto_cm` | NUMERIC(5,1) | no |
| `circonferenza_coscia_dx_cm` | NUMERIC(5,1) | no |
| `circonferenza_coscia_sx_cm` | NUMERIC(5,1) | no |
| `circonferenza_polpaccio_dx_cm` | NUMERIC(5,1) | no |
| `circonferenza_polpaccio_sx_cm` | NUMERIC(5,1) | no |
| `larghezza_spalle_cm` | NUMERIC(5,1) | no |
| `circonferenza_spalle_cm` | NUMERIC(5,1) | no |
| `circonferenza_bicipite_dx_cm` | NUMERIC(5,1) | no |
| `circonferenza_bicipite_sx_cm` | NUMERIC(5,1) | no |
| `creato_il` | TIMESTAMPTZ NOT NULL DEFAULT now() | sì |

Indice su `paziente_id` (stesso pattern di `idx_pazienti_professionista_id`).

### Modifiche a `pazienti`

- `ALTER TABLE pazienti DROP COLUMN altezza_cm;`
- `ALTER TABLE pazienti ADD COLUMN lavoro VARCHAR(150);`
- `ALTER TABLE pazienti ADD COLUMN tipo_lavoro VARCHAR(20);`

### Classi Java

- `com.hexisnutrition.backend.pazienti.Visita` — entità JPA, stesso pattern di `Paziente` (costruttore con parametri obbligatori, getter, niente setter tranne dove serve).
- `com.hexisnutrition.backend.pazienti.VisitaRepository extends JpaRepository<Visita, UUID>`.
- `com.hexisnutrition.backend.pazienti.TipoLavoro` — enum, stesso pattern di `StatoAccountPaziente`.
- `Paziente.java`: rimuovere `altezzaCm` (campo, getter, parametro costruttore); aggiungere `lavoro` (String) e `tipoLavoro` (`TipoLavoro`, `@Enumerated(EnumType.STRING)`), entrambi opzionali nel costruttore.
- `PazienteResponse`: togliere `altezzaCm`, aggiungere `lavoro`, `tipoLavoro`.
- `CreaPazienteRequest`: togliere `altezzaCm`, aggiungere `lavoro`, `tipoLavoro`, e un campo `@NotNull @Valid VisitaRequest visita`.
- Nuovo record `VisitaRequest` (14 campi: `altezzaCm` e `pesoKg` con `@NotNull`, resto opzionale).
- `PazienteService.crea(...)`: diventa `@Transactional`; dopo `pazienteRepository.save(paziente)`, costruisce e salva la `Visita` collegata al `paziente.getId()` appena generato.
- `AbstractIntegrationTest`: aggiungere `visite` al `TRUNCATE`.

## Contratto API

`POST /pazienti` (invariato come path/ruolo, payload esteso):

```jsonc
{
  "nome": "Mario", "cognome": "Rossi", "email": "mario@esempio.it", "telefono": "3331234567",
  "dataNascita": "1990-05-12", "sesso": "M",
  "lavoro": "Impiegato", "tipoLavoro": "ATTIVO",
  "visita": {
    "altezzaCm": 178, "pesoKg": 82.5,
    "circonferenzaVitaCm": 95.0, "circonferenzaOmbelicoCm": null,
    "circonferenzaFianchiCm": 102.0, "circonferenzaPettoCm": 100.0,
    "circonferenzaCosciaDxCm": 58.0, "circonferenzaCosciaSxCm": 58.0,
    "circonferenzaPolpaccioDxCm": 38.0, "circonferenzaPolpaccioSxCm": 38.0,
    "larghezzaSpalleCm": 45.0, "circonferenzaSpalleCm": 110.0,
    "circonferenzaBicipiteDxCm": 32.0, "circonferenzaBicipiteSxCm": 32.0
  }
}
```

Risposta 201: `PazienteResponse` (senza dati di visita — nessuna view del dettaglio paziente mostra ancora uno storico visite; sarà lavoro futuro del sotto-progetto Monitoraggio, con un endpoint dedicato es. `GET /pazienti/{id}/visite`).

400 se `visita` manca, o se `altezzaCm`/`pesoKg` al suo interno sono nulli, con lo stesso meccanismo di validazione già in uso (`@Valid`/`@NotNull` + gestione errori esistente).

Aggiornare `wiki/api-contracts.md` e `wiki/modello-dati.md` di conseguenza.

## Frontend (`frontend-professionisti`)

- Aggiungere il componente shadcn-vue `select` (`npx shadcn-vue@latest add select`), non ancora presente in `src/components/ui/`.
- `src/api/pazienti.ts`: estendere `Paziente` (togliere `altezzaCm`, aggiungere `lavoro`, `tipoLavoro`), estendere `CreaPazienteRequest` allo stesso modo + campo `visita: CreaVisitaRequest` (nuova interfaccia con gli stessi 14 campi del backend).
- `src/views/PazienteNuovoView.vue`: due card in sequenza.
  1. **"Dati anagrafici"**: Nome*, Cognome*, Sesso (select Maschio/Femmina), Email*, Telefono, Data di nascita (`Input type="date"`), Lavoro (testo libero), Tipo Lavoro (select coi 4 livelli). Solo Nome/Cognome/Email restano obbligatori, come oggi.
  2. **"Dati della visita"**: Altezza (cm)*, Peso (kg)*, poi le 12 circonferenze in griglia a 2 colonne, poi una sotto-sezione con separatore "Misurazione BIA" e testo placeholder "Sarà disponibile a breve" (nessun campo).
  - Un solo submit → `crea()` con il payload combinato.
- Aggiornare i mock/fixture in `PazienteNuovoView.spec.ts`, `PazienteDettaglioView.spec.ts`, `PazientiListView.spec.ts`, `DashboardView.spec.ts`, `pazienti.spec.ts` che referenziano `altezzaCm` sul tipo `Paziente` (va tolto; eventualmente aggiungere `lavoro`/`tipoLavoro: null` dove i fixture costruiscono un `Paziente` completo).

## Testing

- **Backend**: estendere `PazienteControllerTest` — payload con `visita` valida → 201 e persistenza di `Visita` collegata; `visita` mancante o con `altezzaCm`/`pesoKg` nulli → 400. Eseguire `mvn test` (richiede `postgresql-x64-13` attivo, `JAVA_HOME` a JDK 21) e riportare l'esito reale.
- **Frontend**: riscrivere `PazienteNuovoView.spec.ts` per il nuovo form (rendering campi, submit con payload esteso, validazione client-side minima). Aggiornare gli altri spec toccati dal cambio di tipo. Eseguire `npm run test` e `tsc --noEmit`.
- Nessuna verifica manuale in browser da parte dell'agente (convenzione di progetto) — segnalare ad Andrea come prossimo passo.

## Fuori scope (esplicitamente)

- Campi reali della misurazione BIA (da definire in futuro).
- Storico visite / visite successive alla prima (endpoint dedicato, vista dettaglio paziente con storico).
- Modifica della sola anagrafica paziente senza visita (endpoint PATCH separato) — non richiesto ora.
