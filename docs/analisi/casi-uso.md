# Casi d'uso

Riferimenti: `docs/handoff/analisi-funzionale.md` e risposte alle domande aperte (vedi `docs/adr/0008-risposte-domande-aperte.md`).

## Attori

| Attore | Descrizione |
|---|---|
| Operatore backoffice | Staff dell'azienda: licenze, piani, supporto, verifica titoli |
| Owner organizzazione | Chi acquista l'abbonamento, gestisce posti e membri |
| Professionista | Membro di un'organizzazione con uno o più ruoli: `coach`, `nutrizionista`, `fisioterapista`, `medico` (ruoli = dato di configurazione) |
| Cliente | `Person` con identità globale, usa l'app |
| Sistema | Job, webhook (Stripe, SDI, provider video), notifiche |

## Backoffice (BO)

| ID | Caso d'uso | Attore | Note |
|---|---|---|---|
| BO-1 | Gestire catalogo piani (canone per posto, soglia clienti) | Operatore | Solo Italia, EUR |
| BO-2 | Creare/sospendere organizzazione e abbonamento | Operatore | Stripe Billing |
| BO-3 | Emettere e consultare fatture verso le org | Sistema/Operatore | SDI completo in v1; MVP fatture base |
| BO-4 | Verificare a campione l'autodichiarazione del titolo/albo | Operatore | Esito: `non_verificato / verificato / respinto`; il rifiuto disabilita i ruoli regolamentati |
| BO-5 | Impersonation tracciata per supporto | Operatore | Motivo obbligatorio, scadenza, audit immutabile, notifica all'org, niente accesso a dati Art. 9 salvo ticket e consenso |
| BO-6 | Cruscotto MRR/churn | Operatore | v1 |

## Organizzazione e professionista (PT)

| ID | Caso d'uso | Attore |
|---|---|---|
| ORG-1 | Registrare organizzazione (anche implicita, singolo professionista) | Owner |
| ORG-2 | Invitare/rimuovere membri entro i posti del piano | Owner |
| ORG-3 | Assegnare ruoli professionali e dichiarare titolo/n. albo | Owner/Professionista |
| ORG-4 | Firmare DPA e informativa (titolare/responsabile) | Owner |
| PRO-1 | Invitare un cliente (link/email) o collegare un cliente già esistente in piattaforma | Professionista |
| PRO-2 | Gestire anagrafica e note CRM | Professionista |
| PRO-3 | Compilare/far compilare anamnesi e PAR-Q | Professionista/Cliente |
| PRO-4 | Richiedere e registrare consensi per ambito | Professionista |
| PRO-5 | Gestire libreria esercizi (propri + catalogo) con video | Professionista |
| PRO-6 | Costruire scheda e periodizzazione, assegnarla | Professionista |
| PRO-7 | Registrare/consultare misure e foto progressi | Professionista/Cliente |
| PRO-8 | Chattare con cliente, allegare media | Professionista |
| PRO-9 | Vedere aderenza e progressi del cliente | Professionista |
| PRO-10 | Chiudere la relazione di cura e gestire il passaggio ad altro professionista | Professionista |
| PRO-11 | Piano dieta (autore: nutrizionista abilitato) | Nutrizionista | v1 |
| PRO-12 | Agenda, check-in, incassi dai clienti | Professionista | v1 |

## Collaborazione (anche cross-organizzazione)

| ID | Caso d'uso | Attore |
|---|---|---|
| COL-1 | Un professionista propone una `CareRelationship` con un cliente già seguito da altri | Professionista |
| COL-2 | Il cliente accetta/rifiuta e sceglie per dominio il livello (`none/summary/full`) | Cliente |
| COL-3 | Un professionista consulta dati secondo `AccessGrant` effettivo | Professionista |
| COL-4 | Il cliente revoca un accesso (effetto immediato, audit) | Cliente |
| COL-5 | Richiesta modifica a dati di dominio altrui via segnalazione (es. coach → nutrizionista) | Professionista |

Regola: nessun dato attraversa il confine di organizzazione senza `AccessGrant` attivo derivato da `Consent` valido.

## Cliente

| ID | Caso d'uso |
|---|---|
| CLI-1 | Registrarsi/accettare invito, vedere informative e dare/negare consensi granulari |
| CLI-2 | Vedere l'allenamento del giorno con video |
| CLI-3 | Registrare carichi/serie, usare timer, completare sessione |
| CLI-4 | Vedere progressi (grafici, misure, foto) |
| CLI-5 | Chattare con i propri professionisti |
| CLI-6 | Gestire privacy: elenco professionisti con accessi, revoca, export dati, cancellazione |
| CLI-7 | Lavorare offline e sincronizzare (v1) |
| CLI-8 | Pagare/vedere ricevute (v1) |

## Flussi critici (sequenza sintetica)

**Collegamento cliente esistente (COL-1/2/3)**
1. Professionista B cerca per email/codice invito; la piattaforma non rivela se la persona esiste (risposta neutra).
2. Se esiste, al cliente arriva richiesta con identità di B, ruolo e ambiti richiesti.
3. Il cliente accetta → `CareRelationship(stato=attiva)`, `Consent` e `AccessGrant` per dominio; evento `RelationshipActivated` via outbox.
4. Ogni lettura passa dalla policy centrale (relazione + grant + consenso + ruolo + organizzazione); ogni accesso a dati Art. 9 è in audit.

**Revoca (COL-4)**: `AccessGrant.fine = now`, sessioni/cache invalidate, URL firmati già emessi scadono entro pochi minuti; i dati scritti dal professionista restano nella sua organizzazione (titolare autonomo) ma non più leggibili dal professionista sui dati del cliente oltre quelli da lui prodotti, salvo obbligo di conservazione.

**Esecuzione allenamento (CLI-2/3)**: l'app scarica la scheda assegnata, registra `SetLog` con `client_uuid` (idempotenza), sincronizza in batch.
