# ADR-0004 — Isolamento tenant e autorizzazione centralizzata

**Stato:** proposto

## Contesto
Con collaborazione cross-org nell'MVP, la sola RLS per `organization_id` non basta: i dati su `Person` vengono letti da più organizzazioni.

## Decisione
- RLS PostgreSQL: tabelle di tenant filtrate da `organization_id` (variabile di sessione); tabelle su `Person` filtrate dalla presenza di `AccessGrant` attivo, esposto dal modulo Consensi tramite funzione/vista dedicata.
- Filtro applicativo duplicato (difesa in profondità); policy per ambito in un punto solo, nessun `if (isPT)` sparso.
- Cifratura a livello di campo per dati sanitari con chiavi per organizzazione; audit immutabile; MFA obbligatoria; librerie di autenticazione consolidate (niente crypto/auth custom).
- Test in CI: isolamento tra tenant, grant cross-org, revoca, impersonation.

## Conseguenze
- Maggiore complessità SQL/performance sulle policy cross-org: richiede benchmark precoce.
- Le chiavi per organizzazione rendono più onerosa la lettura cross-org dei campi cifrati: da progettare (re-wrapping o chiave per `Person`). Aperto.
