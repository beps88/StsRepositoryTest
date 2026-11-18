# ADR-0003 — CareRelationship, AccessGrant, Consent

**Stato:** accettato

## Contesto
Un cliente può avere più professionisti, anche di organizzazioni diverse (ADR-0008). I dati sanitari richiedono basi giuridiche esplicite (GDPR art. 9).

## Decisione
- `Person` ha identità globale.
- `CareRelationship` (persona–membership, ruolo, stato, periodo) è l'unico legame di cura.
- `AccessGrant` per relazione e dominio (allenamento, misure, dieta, foto, salute, chat) con livello `none|summary|full`.
- `Consent` per professionista e ambito, versionato e revocabile; foto e dati medici richiedono consenso separato.
- Livello effettivo = `min(AccessGrant, Consent valido, capacità del ruolo)`.
- Ogni lettura/scrittura passa da un'unica policy; gli accessi a dati art. 9 sono in audit.
- La ricerca di un cliente esistente non rivela la sua esistenza prima dell'accettazione del cliente.

## Conseguenze
- Revoca immediata ed efficace anche cross-organizzazione.
- Modello più complesso di un FK singolo, ma evita rework per studi e collaborazione.
- Necessari test sulla matrice ruolo×dominio×livello.
