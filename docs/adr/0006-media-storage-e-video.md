# ADR-0006 — Media, video e documenti

**Stato:** proposto

## Decisione
Mai blob in DB. Storage a oggetti privato in regione UE, chiavi `org/{id}/person/{id}/…`, URL firmati a scadenza breve, upload diretto firmato, antivirus sui documenti (`MediaAsset.scan_stato`), servizio video gestito (Mux o Cloudflare Stream) per l'MVP.

## Conseguenze
- Costo per minuti di video ma nessuna pipeline di transcodifica da mantenere.
- La revoca di un grant non invalida URL già emessi: durata massima URL = finestra di esposizione accettata (pochi minuti).
- Fornitore video da confrontare su costi e DPA al momento dell'avvio.
