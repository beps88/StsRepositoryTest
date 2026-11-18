# ADR-0008 — Risposte alle domande aperte

**Stato:** accettato (risposte dell'utente)

| # | Domanda | Risposta | Effetto |
|---|---|---|---|
| 1–2 | PT/allenatore e altri professionisti | Ruolo unico **coach**, più nutrizionista, fisioterapista, medico | `RoleDefinition` data-driven; funzioni di dominio non-coach in v1 (assunzione, da confermare) |
| 3–4 | Studi vs indipendenti; cliente con più org | Anche **indipendenti che collaborano**, cliente con professionisti di org diverse | La collaborazione cross-organizzazione **anticipa dalla v2 all'MVP** (modifica la decisione 4 del handoff, parte "rimandata a v2"); vedi ADR-0003/0004 |
| 5–6 | Chi incassa; verifica titoli | **Il PT incassa** (Connect); titoli **autodichiarati**, verifica manuale a campione dal backoffice | ADR-0007; `Credential.verifica_stato` |
| 7 | Prezzo, paesi | **Canone per posto + soglia clienti, solo Italia** | ADR-0007 |
| 7 | Budget, tempi, stack | Non indicati | Stack resta proposta (ADR-0002) |

## Rischi segnalati
- L'anticipo della collaborazione cross-org nell'MVP aumenta scope e rischio (RLS cross-org, cifratura per organizzazione, titolarità del trattamento): valutare un MVP con collaborazione limitata (sola lettura `summary`) se i tempi sono stretti.
- Fisioterapista e medico implicano dati sanitari professionali e obblighi diversi: serve parere legale/DPO prima di abilitarli; l'autodichiarazione potrebbe non bastare per questi ruoli.
