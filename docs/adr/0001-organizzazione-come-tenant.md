# ADR-0001 — Organizzazione come tenant

**Stato:** accettato

## Contesto
Si parte dal PT singolo ma si vuole supportare studi e più professionisti senza riscrivere il modello.

## Decisione
Il tenant è l'`Organization`, non il PT. Un PT singolo ha un'organizzazione `individuale` con una sola `Membership`. Utenti e professionisti sono collegati tramite `Membership`/`ProfessionalRole`. Licenza e fatturazione sono dell'organizzazione.

## Conseguenze
- Nessun `cliente.personal_trainer_id`; nessuna fattura legata a persona fisica.
- Il passaggio da PT singolo a studio è un cambio di piano/posti, non una migrazione.
- `Person` (cliente) è globale e non appartiene a un tenant (vedi ADR-0003).
