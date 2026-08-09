# hexis-nutrition — backend

Questo è il backend (Spring Boot) di hexis-nutrition. Contesto completo, decisioni architetturali e stato del progetto: [`../wiki/`](../wiki/) — leggi `../wiki/index.md` e `../wiki/stato.md` prima di modifiche sostanziali.

Se aggiungi o modifichi un endpoint o il modello dati, aggiorna nello stesso passaggio `../wiki/api-contracts.md` o `../wiki/modello-dati.md`.

## Test — da eseguire sempre

I test di integrazione girano su un PostgreSQL locale, **senza Docker** (vedi [`../wiki/decisioni/0004-test-su-postgres-locale.md`](../wiki/decisioni/0004-test-su-postgres-locale.md)): non c'è nessun motivo per non eseguirli.

Dopo ogni modifica al codice, prima di considerare il lavoro finito:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"   # il JDK di sistema di default è Java 8
mvn test                                          # da backend/
```

Richiede il servizio `postgresql-x64-13` attivo e il database `hexis_test` (utente `hexis`, password `hexis`).

Riporta l'esito **reale**, mai una previsione: se i test non sono stati eseguiti, dillo esplicitamente invece di lasciar intendere che passino. Se aggiungi una tabella, aggiungila al `TRUNCATE` in `AbstractIntegrationTest`, altrimenti i dati sopravvivono tra un test e l'altro.

**Divisione dei compiti**: la suite automatica è responsabilità dell'agente, da eseguire sempre. Le **verifiche manuali del prodotto** (avviare l'app, provare i flussi da browser o client HTTP, controllare le email inviate) le fa **Andrea**: non avviare l'applicazione per provarla di tua iniziativa e non trattare quella verifica come condizione per considerare finito il lavoro. Segnala invece cosa resta da provare a mano.

Panoramica progetto: [`../CLAUDE.md`](../CLAUDE.md).
