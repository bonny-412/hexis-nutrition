# Fonte — Docker previsto solo per la produzione (9 agosto 2026)

Tipo: precisazione di Andrea, stessa sessione della migrazione dei test su Postgres locale.

## Contenuto

> Il docker servià quando andremo in produzione. Per tutta la fase di sviluppo e poi per la fase di test (che mi occuperò io) utilizzero il database creato con PGAdmin sulla mia macchina

## Cosa precisa

- L'uso di un PostgreSQL locale creato con pgAdmin **non è una soluzione temporanea** in attesa di installare Docker: è la modalità prevista per **tutta la fase di sviluppo**.
- La fase di test/verifica manuale del prodotto sarà curata da Andrea, sempre sulla stessa macchina e sullo stesso database locale.
- **Docker è previsto per la produzione**, non per l'ambiente di sviluppo. Nessun dettaglio deciso su come (immagini, orchestrazione, provider, versione di PostgreSQL in produzione).
