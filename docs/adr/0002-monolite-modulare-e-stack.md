# ADR-0002 — Monolite modulare e stack

**Stato:** proposto (budget, tempi e team non ancora noti)

## Contesto
Dominio ampio ma team presumibilmente piccolo; necessità di confini chiari per dati sensibili e per una futura estrazione di servizi.

## Decisione
Monolite modulare: Spring Boot 3.x / Java 21+ con Spring Modulith, PostgreSQL (RLS), Redis, outbox per eventi, Flyway. Moduli: Identity&Access, Organizzazioni, Licenze&Billing, Persone&CRM, Training, Nutrizione, Misure, Messaggistica, Agenda, Pagamenti clienti, Consensi&Audit, Notifiche. Un modulo non legge le tabelle di un altro (schema per modulo, verificato da test dei confini). Web React+TypeScript; mobile React Native (o Flutter) con DB locale e coda di sync; OpenAPI come fonte di verità. Versioni da verificare all'avvio.

## Conseguenze
- Deploy unico, transazioni semplici, costo operativo basso.
- La comunicazione tra moduli passa da API interne ed eventi (outbox); niente join cross-modulo.
- La scelta mobile (RN vs Flutter) resta aperta fino a conoscere competenze del team.
