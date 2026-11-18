# ADR-0007 — Licenze per organizzazione, incasso clienti via Connect

**Stato:** accettato

## Decisione
- Licenza e fatturazione legate all'organizzazione: un abbonamento, canone **per posto** con soglia di clienti attivi per piano (solo Italia, EUR). Stripe Billing per le org; fatturazione elettronica via provider SDI (base nell'MVP, completa in v1).
- Il **PT incassa dai clienti** con Stripe Connect sul proprio conto: la piattaforma non è marketplace né intermediario di pagamento (v1).
- Rispetto delle regole degli store sui pagamenti in-app da verificare prima di v1.

## Conseguenze
- Meno obblighi fiscali/normativi per la piattaforma; onboarding KYC del PT a carico di Stripe.
- La soglia clienti richiede una definizione di "cliente attivo" (domanda aperta).
- Multi-paese, valute e IVA UE/OSS esclusi finché non cambia il perimetro.
