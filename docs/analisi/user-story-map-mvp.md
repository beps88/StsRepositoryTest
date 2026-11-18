# User story map — MVP

Backbone (attività, da sinistra a destra nel tempo) e storie per priorità. **R1** = walking skeleton, **R2** = MVP completo, **R3** = fine MVP/hardening. Voci marcate *(v1)* restano fuori MVP.

> Assunzione da confermare: l'MVP abilita operativamente il ruolo **coach**; i ruoli nutrizionista/fisioterapista/medico sono già nel modello (`RoleDefinition`), nel flusso di invito e nella collaborazione cross-org, ma le funzioni di dominio (dieta, cartella clinica) arrivano in v1.

| Attività → | 1. Onboarding org | 2. Clienti & consensi | 3. Programmazione | 4. Esecuzione | 5. Monitoraggio | 6. Collaborazione | 7. Operatività BO |
|---|---|---|---|---|---|---|---|
| **R1** | Registrare org + login con MFA | Invitare cliente, accettare invito | Libreria esercizi base, scheda semplice | Vedere allenamento del giorno, registrare serie | Storico sessioni | — | Creare org/piano a mano |
| **R2** | Inviti membri e posti; ruoli; DPA; autodichiarazione titolo | Anagrafica CRM, anamnesi/PAR-Q, consensi granulari, informative versionate | Video esercizi, costruttore, periodizzazione base, versioni | Timer, note, completamento sessione | Misure append-only, foto con consenso separato, grafici progressi, chat | Richiesta collegamento cliente esistente, grant per dominio (`summary/full`), revoca | Abbonamenti Stripe, limite posti/soglia clienti, fatture base, supporto con impersonation tracciata |
| **R3** | Gestione sospensione/morosità org | Export e cancellazione dati (GDPR), registro accessi per il cliente | Duplicare/ riusare schede e template | Notifiche push base | Indicatori semplici di aderenza | Segnalazioni tra professionisti | Verifica titoli a campione, audit consultabile |
| *(v1)* | SDI completo | Check-in | Diete | Offline completo | Rischio abbandono | Agenda condivisa | MRR/churn, pagamenti clienti (Connect) |

## Storie R1 (walking skeleton) — criteri di accettazione sintetici

1. **Registrazione org** — Dato un nuovo utente, quando registra org e verifica email, allora esiste Organization `individuale` con Membership owner e MFA obbligatoria al secondo accesso.
2. **Invito cliente** — Dato un coach, quando invita un'email, allora il cliente riceve link monouso a scadenza; alla registrazione nasce `Person` + `CareRelationship(attiva)` + `ClientRecord`.
3. **Scheda semplice** — Un coach crea una scheda con esercizi/serie/rip e la assegna; il cliente la vede solo dopo la pubblicazione.
4. **Registrazione serie** — Il cliente salva carico/reps; ripetere lo stesso `client_uuid` non crea duplicati.
5. **Isolamento** — Un coach di un'altra organizzazione non può leggere né enumerare dati (test automatico RLS).

## Storie R2 — criteri chiave

- **Consensi**: nessun ambito sanitario/foto è scrivibile senza `Consent` valido; revoca → blocco lettura immediato; versioni dei testi tracciate.
- **Misure**: modifiche solo come nuova riga con `rettifica_di`; grafico per serie fonte; autore e strumento obbligatori.
- **Collaborazione cross-org**: richiesta → accettazione del cliente → grant per dominio; la risposta alla ricerca di un cliente non rivela esistenza prima del consenso; ogni lettura cross-org è in audit.
- **Billing**: invito oltre i posti bloccato con messaggio chiaro; superata la soglia clienti, solo upgrade proposto (nessun blocco dei clienti esistenti).
- **Impersonation**: motivo + scadenza ≤ 60 min + banner visibile + notifica all'org; dati Art. 9 oscurati.

## Fuori scope MVP (esplicito)
Diete, agenda, pagamenti ai clienti, SDI completo, offline completo, wearable, white label, AI, community, videochiamate, marketplace.

## Rischi e domande residue
1. Definizione di "cliente attivo" per la soglia del piano (almeno una sessione negli ultimi 30 giorni? relazione attiva?).
2. Verifica dei titoli: con più ruoli regolamentati (fisio, medico) l'autodichiarazione basta o serve parere legale? **Consigliato un parere legale prima del rilascio** dei ruoli sanitari.
3. Rapporto titolare/responsabile del trattamento nei casi cross-org (ogni org titolare autonomo? contitolarità?) — richiede DPO/legale.
4. Regole degli store per pagamenti in-app quando arriveranno i pagamenti clienti (v1).
5. Budget, tempi e stack: non ancora forniti; lo stack nell'ADR-0002 resta una proposta.
