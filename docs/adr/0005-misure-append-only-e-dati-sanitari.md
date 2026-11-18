# ADR-0005 — Misure append-only e dati sanitari

**Stato:** accettato

## Contesto
Le misure appartengono al cliente e sono prodotte da fonti e professionisti diversi; devono restare confrontabili e attribuibili.

## Decisione
`Measurement` è un dato condiviso legato a `Person`, append-only, con autore, `fonte`, `strumento_metodo` e serie per `(tipo, fonte)`. Le correzioni sono nuove righe con `rettifica_di`. Foto e dati medici hanno consenso separato (ADR-0003). Dieta: il PT vede target e allergie; piano e diario solo con consenso e se il nutrizionista lo consente, in sola lettura; l'autore delle diete è il nutrizionista abilitato.

## Conseguenze
- Storico completo e auditabile; nessuna sovrascrittura silenziosa.
- Il diritto alla cancellazione si gestisce a livello di `Person` (cancellazione/anonimizzazione), non per singola riga.
