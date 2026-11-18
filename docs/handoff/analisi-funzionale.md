# Handoff: analisi funzionale (piattaforma Personal Trainer)

Contesto: sessione di origine su analisi di prodotto. Questo documento riassume quanto già deciso/discusso. Lingua di lavoro: italiano.

## Prodotto
Applicazione web + mobile per professionisti del benessere, con tre aree:
- **Backoffice azienda**: licenze, attivazioni, fatturazione verso i PT, supporto, impersonation tracciata, catalogo piani, MRR/churn, fatturazione elettronica SDI.
- **Area professionista (PT)**: CRM clienti, anamnesi/PAR-Q, consensi GDPR, schede (libreria esercizi con video, costruttore, periodizzazione), misure biometriche e foto progressi, diete, chat/check-in, agenda, pagamenti ai clienti, dashboard (aderenza, rischio abbandono).
- **Area cliente (app)**: allenamento del giorno con video, registrazione carichi, timer, offline, progressi, dieta/diario, chat, pagamenti, privacy.

## Decisioni prese
1. **Partire dal PT singolo, ma non precludere il multi-professionista** (PT, allenatore, nutrizionista, eventuali fisio/medico).
2. **Organizzazione = tenant** (non il PT). Un PT singolo ha un'organizzazione implicita con un solo membro.
3. Modello: `Organization` ─ `Membership` ─ `User`/`Professional`; `Person` (cliente) con **identità globale**; `CareRelationship` (cliente–professionista, ruolo, stato, periodo) con `AccessGrant` per dominio e livello (`none|summary|full`); `Consent` per professionista e ambito.
4. Licenza e fatturazione legate all'**organizzazione**; il numero di professionisti è un limite del piano (posti/seat). MVP: un'organizzazione, un abbonamento, più posti. Collaborazione cross-organizzazione rimandata a v2.
5. **Dieta**: il PT vede target calorici/macro, riepilogo e allergie; piano completo e diario solo con consenso del cliente e se il nutrizionista lo consente. Solo lettura, modifiche via segnalazione. Il nutrizionista abilitato è autore delle diete (vincolo normativo italiano).
6. **Misure corporee** = dato condiviso legato al cliente, append-only, con autore, strumento/metodo e serie per fonte. Foto e dati medici richiedono consenso separato.
7. Da non fare: `cliente.personal_trainer_id` come chiave unica; permessi sparsi `if (isPT)`; fatture clienti legate a persona fisica.

## Architettura proposta
- **Monolite modulare** (Spring Boot 3.x / Java 21+, Spring Modulith), PostgreSQL con Row-Level Security + filtro applicativo, Redis, outbox per eventi, Flyway.
- Moduli: Identity&Access, Organizzazioni, Licenze&Billing, Persone&CRM, Training, Nutrizione, Misure, Messaggistica, Agenda, Pagamenti clienti, Consensi&Audit, Notifiche. Un modulo non legge le tabelle di un altro.
- Autorizzazione centralizzata (policy per ambito), cifratura a livello di campo per dati sanitari, audit immutabile, MFA, librerie auth consolidate.
- Web: React + TypeScript. Mobile: React Native (o Flutter) con DB locale e coda di sync, API idempotente. OpenAPI come fonte di verità.
- **Video e documenti**: mai in DB. Storage a oggetti privato (S3, regione UE), URL firmati a scadenza breve, upload diretto firmato, servizio video gestito (Mux/Cloudflare Stream) per l'MVP, antivirus sui documenti, percorso `org/{id}/person/{id}/...`.
- Pagamenti: Stripe Billing (PT) e Connect (clienti); fatturazione elettronica via provider SDI. Attenzione alle regole degli store sui pagamenti in-app.
- Versioni e librerie da verificare al momento dell'avvio, non fissate qui.

## Roadmap
- **MVP**: backoffice (tenant, piani, abbonamenti, fatture base), PT (clienti, esercizi+video, schede, misure, chat), cliente (esecuzione allenamento, progressi, chat), GDPR e consensi inclusi.
- **v1**: pagamenti clienti, agenda, diete, check-in, notifiche, SDI completo, offline.
- **v2**: wearable, white label, studio multi-trainer, analytics/alert.
- **v3**: AI, community/gamification, marketplace, videochiamate.

## Valore aggiunto da considerare
GDPR art. 9 e DPA con i PT, wearable (Apple Health/Health Connect/Garmin), offline, gamification, AI assistita dal PT, rilevamento abbandono, agenda e prenotazioni, pagina pubblica del PT, white label, team/studi, reportistica fiscale, videochiamate.

## Domande ancora aperte
1. PT e allenatore sono la stessa figura o due ruoli distinti?
2. Quali altri professionisti (fisioterapista, medico, psicologo)?
3. Studi strutturati o professionisti indipendenti che collaborano?
4. Un cliente può avere professionisti di organizzazioni diverse?
5. Chi incassa dai clienti: piattaforma (marketplace) o il PT?
6. Verifica dei titoli professionali (albo) da parte della piattaforma?
7. Modello di prezzo per i PT (canone, per cliente, posti, consumo)? Solo Italia o più paesi? Budget/tempi/stack?

## Prossimi passi proposti
ADR delle decisioni, diagramma ER completo, user story map dell'MVP, scheletro Spring Boot con test dei confini tra moduli e test di isolamento tra tenant.
