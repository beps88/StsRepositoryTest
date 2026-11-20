# Analisi: approcci per l'AI nella piattaforma (addestrare, adattare, self-hosting, API)

Data di lettura dei prezzi: **20 novembre 2026**. Continua `analisi-mercato.md`, `analisi-mercato-seguito.md`, `tabella-funzionalita.md` (sez. 5), `user-story-map-mvp.md`, ADR-0002 e ADR-0008. Non rimette in discussione la decisione del proprietario: **l'AI non genera piani di allenamento né piani alimentari e non sostituisce il professionista**; automatizza processi, sempre con anteprima e approvazione.

**Legenda.** **[F]** fatto verificato su una fonte citata. **[D]** deduzione o calcolo mio, con ipotesi esplicite. **[?]** non verificato, illeggibile o in contraddizione.

**Limiti di metodo.** Ho letto le pagine con fetch automatico. Alcune hanno risposto 403 (OpenAI `openai.com/api/pricing`, Scaleway `/en/pricing/gpu` e `/fr/tarifs/gpu`, Scaleway Generative APIs) o erano troncate (OVHcloud prezzi). Per Scaleway e OVHcloud i prezzi orari vengono da **risultati di ricerca che citano le pagine ufficiali**, non da una lettura diretta della pagina: li marco di conseguenza. Nessuna prova di qualità sull'italiano è stata eseguita: tutto ciò che riguarda la qualità è **[?]** o **[D]**. Budget, tempi e team sono ignoti; le cifre di personale sono ipotesi.

---

## 0. Sintesi (leggere questa se si ha un minuto)

1. **Addestrare da zero non ha senso** per questo prodotto. Solo il calcolo di calcolo-GPU di un modello da 7 miliardi di parametri è ~200.000 € **[D]**, prima di dati, tentativi falliti e persone; la qualità resterebbe sotto quella di modelli pronti. Escluso a ogni fase.
2. **Il fine-tuning costa poco in GPU ma molto in dati e competenze** **[D]**. Con un team senza esperienza di machine learning e senza un archivio di esempi approvati dai professionisti, non è un punto di partenza. Si valuta solo in v2+, se prompt e controlli non bastano e se esiste un dataset di correzioni reali.
3. **Il self-hosting non conviene per costo** nelle dimensioni considerate (100–5.000 organizzazioni) **[D]**. Una coppia di GPU costa 2.000–4.000 €/mese anche a vuoto; il carico stimato (vedi sez. 5) vale 11–540 $/mese con un modello piccolo via API. Il pareggio con un modello di pari qualità via API sta oltre le 14.000 organizzazioni, e oltre le 1.300 solo se i volumi reali fossero 10 volte quelli ipotizzati, oppure se si confronta con un modello API di fascia alta. Il self-hosting si giustifica per **sovranità/privacy**, non per costo.
4. **Nodo GDPR da risolvere prima di scegliere il fornitore: l'API diretta di Anthropic non offre inferenza in UE.** I valori ammessi di `inference_geo` sono solo `us` e `global`, e la residenza dei dati a riposo è solo `us` **[F]**. Per dati sanitari art. 9 con vincolo UE, con Anthropic occorre passare da un canale partner (Bedrock, Vertex) di cui non ho verificato le regioni UE **[?]**. Google (Vertex AI) offre un endpoint multi-regione `eu` per molti modelli Gemini **[F]**. Mistral è un fornitore europeo, ma la pagina prezzi letta non dichiara dove ospita l'API **[F]**/**[?]**.
5. **Raccomandazione:** MVP senza AI (ma con due scelte architetturali a costo quasi zero: un'interfaccia `AiGateway` e minimizzazione dei dati); v1 con **API di un fornitore con inferenza UE contrattualizzata**, modello piccolo, solo processi a basso rischio (importazione, bozze, riassunti), con anteprima e approvazione; v2+ rivalutare **API europea con modelli aperti** (es. Scaleway/Mistral) e, solo con prove, il fine-tuning. Self-hosting solo se un vincolo contrattuale o normativo lo impone.

> Nota sulla roadmap esistente: `analisi-funzionale.md` colloca l'AI in v3 e la user story map la esclude dall'MVP. Qui propongo di **anticipare alla v1 solo l'importazione guidata e le bozze di messaggi**, perché il costo di cambio (migrazione schede/storico) è un rischio già segnalato in `analisi-mercato.md`. È una proposta, non una decisione.

---

## 1. Fonti di prezzo (lette il 20/11/2026)

### 1.1 API di fornitori (USD per milione di token, senza IVA)

| Fornitore | Modello | Input | Output | Fonte e stato lettura |
|---|---|---|---|---|
| Anthropic | Claude Haiku 5.5 (prompt ≤100k) | 0,10 | 0,50 | **[F]** [pricing Anthropic](https://platform.claude.com/docs/en/about-claude/pricing), letta per intero |
| Anthropic | Claude Sonnet 5.5 | 2 | 10 | **[F]** stessa pagina |
| Anthropic | Cache hit Sonnet 5.5 / Haiku 5.5 | 0,10 / 0,01 | — | **[F]** stessa pagina; sconto batch 50% (es. Sonnet 5.5 batch 1 / 5) **[F]** |
| OpenAI | gpt-6-luna | 0,10 | 0,50 | **[F]** [pricing OpenAI (developers)](https://developers.openai.com/api/docs/pricing.md); la pagina marketing `openai.com/api/pricing` dà 403 |
| OpenAI | gpt-6.1-terra | 1 | 5 | **[F]** stessa pagina; batch = metà |
| OpenAI | gpt-6.2-sol | 2 | 10 | **[F]** stessa pagina |
| Google | Gemini 3.5 Flash-Lite | 0,30 | 2,50 | **[F]** [pricing Gemini](https://aistudio.google.com/docs/pricing) (letta per i primi 100.000 caratteri su 117.956) |
| Google | Gemini 3.8 Flash | 0,75 fino al 31/12/2026, poi 1,50 | 3,75, poi 7,50 | **[F]** stessa pagina; **il prezzo raddoppia dal 1/1/2027**, da tenere nel budget |
| Mistral | Mistral Small 4 | 0,15 | 0,60 | **[F]** [docs Mistral pricing](https://docs.mistral.ai/inference/pricing) |
| Mistral | Mistral Large 4 | 0,7 | 2,1 | **[F]** stessa pagina, ma vedi incoerenza sotto |

**Incoerenze rilevate.**
- Mistral: la pagina `mistral.ai/pricing` (da me letta in un fetch) dice "Mistral Large costs $0.5 /M tokens in and $1.5 /M tokens out", mentre `docs.mistral.ai/inference/pricing` dà Large 4 a 0,7/2,1. Una ricerca riporta anche Small a 0,1/0,3 invece di 0,15/0,6, e terze parti altri valori. **[?]** Uso 0,15/0,60 dalla pagina documentazione, la più dettagliata, e invito a rileggere prima di impegnarsi.
- OpenAI: le fonti di terze parti in ricerca citano nomi e prezzi diversi (GPT-5.5, GPT-6 Astra a 10/50…); ho usato solo la pagina ufficiale developers. La pagina elenca molti modelli contemporaneamente; quali siano "correnti" è una mia lettura **[?]**.
- Gemini: la pagina chiama "Pro-tier" solo 3.1 Pro Preview e ha nomi di modello non intuitivi (Carbon, Argon, Neon, Hydrogen); per chiarezza uso solo i modelli Flash/Flash-Lite.
- I prezzi sono in USD; Mistral dichiara una vista in EUR che non ho letto. Per i confronti uso **1 € = 1 $ come semplificazione** **[D]**: l'errore (decine di punti percentuali al massimo) non cambia nessuna conclusione.

### 1.2 Residenza dati e addestramento sui dati

| Fornitore | Residenza UE | Addestramento sui dati | Fonte |
|---|---|---|---|
| Anthropic API diretta | **No UE.** `inference_geo`: solo `us` o `global`; workspace geo: solo `us`. US-only costa 1,1× **[F]** | ZDR "eligible" (esclude modelli "Covered") **[F]**; politica di training **[?]** non verificata qui | [Data residency](https://platform.claude.com/docs/en/manage-claude/data-residency) |
| OpenAI | Endpoint "regional processing" con **+10%** per modelli dal 5/3/2026 **[F]**; la pagina non nomina l'UE **[?]** | **[?]** non verificato | [pricing](https://developers.openai.com/api/docs/pricing.md) |
| Google (Vertex AI) | Endpoint multi-regione `eu` "strictly covers data residency within EU member states"; regioni singole FR, DE, NL; UK è fuori UE; gli endpoint globali non garantiscono residenza **[F]**. Supplemento di prezzo: la pagina non lo dice **[?]** | API a pagamento: i dati non sono usati per migliorare i prodotti; **il livello gratuito sì** **[F]** | [Vertex data residency](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/learn/data-residency), [pricing](https://aistudio.google.com/docs/pricing) |
| Mistral | La pagina prezzi non indica dove è ospitata l'API **[F]**; società europea **[D]**, da confermare nel DPA | Riga "Model training: Opt-out" non attribuibile a un piano specifico **[F]**/**[?]** | [pricing](https://mistral.ai/pricing#api-pricing) |

Un fornitore **UE è condizione necessaria, non sufficiente**: servono DPA art. 28 GDPR, elenco dei sub-responsabili, conservazione zero o minima, esclusione dall'addestramento e trasferimenti extra-UE regolati, tutti da leggere nei contratti, non nelle pagine prezzi **[D]**.

### 1.3 GPU in UE

| Offerta | GPU | Prezzo | Regione | Stato lettura |
|---|---|---|---|---|
| Scaleway | L40S 48 GB (L40S-1-48G) | 1,40 €/h (~1.022 €/mese) | Parigi (PAR-2) | **[F]** riportato da ricerca su pagine Scaleway; fetch diretto 403 → **[?]** da confermare in console |
| Scaleway | H100 PCIe 80 GB | 2,73 €/h (~1.993 €/mese), da 1/7 (prima 2,52) | UE (Parigi) **[?]** | come sopra. Le pagine SXM danno cifre non coerenti tra loro (2,88 €/GPU/h ecc.) |
| OVHcloud | L40S 48 GB (l40s-90) | 1,40 €/h | Gravelines (FR) secondo un blog **[?]** | **[?]** prezzi da pagina irlandese di OVH via ricerca; la pagina letta direttamente era troncata. I prezzi potrebbero essere cambiati dal lancio |
| OVHcloud | H100 PCIe 80 GB | 2,80 €/h per GPU | come sopra | idem. Nelle pagine lette direttamente compaiono anche H200, B300, RTX Pro 6000 senza prezzo **[F]** |

Un fetch di OVH in locale inglese mostrava L4 a 1,00 $, L40S a 1,80 $ e H100 a 2,99 $ all'ora **[F]**: valuta e listino diversi dai 1,40/2,80 € sopra, non riconciliati **[?]**. Per il calcolo uso Scaleway (1,40 e 2,73 €/h) come riferimento.

### 1.4 Alternativa intermedia: modelli aperti ospitati da un fornitore UE

Scaleway Generative APIs (serverless, a token) elenca, secondo ricerca su pagine ufficiali di snapshot diversi, ad esempio Mistral-Small-3.2-24B a 0,15/0,35 € e gpt-oss-120b a 0,15/0,60 € per milione di token, con descrizione "data center europei" **[?]** (fetch diretto 403; i risultati provengono da snapshot diversi e i modelli variano). Fonte: [Scaleway Generative APIs](https://www.scaleway.com/en/generative-apis/). Serve come ponte fra "API chiusa" e "self-hosting" (modello aperto, senza gestire GPU), ma i prezzi vanno riletti.

---

## 2. Confronto dei quattro approcci

### 2.1 Addestrare un modello da zero

- **Costi una tantum.** Calcolo GPU per un modello da 7 miliardi di parametri su 1.000 miliardi di token: regola 6·N·D ≈ 4,2·10²² operazioni; con H100 a ~400 TFLOP/s al 40% di efficienza ≈ 73.000 ore-GPU ≈ **~200.000 €** a 2,73 €/h **[D]**. Si aggiungono: raccolta e pulizia di un corpus italiano, tentativi falliti (moltiplicatore 2–3×), valutazione, sicurezza. Ordine di grandezza realistico: **centinaia di migliaia di euro, fino a milioni** **[D]**; un dato più basso non sarebbe credibile. Non verificato su fonti **[?]**.
- **Ricorrenti.** Hosting come in 2.3, più riaddestramento a ogni salto generazionale.
- **Persone.** Ricercatori ML, data engineer, MLOps: competenze assenti nel team (Spring Boot).
- **Qualità sull'italiano.** Un modello piccolo addestrato in casa resterebbe peggiore dei modelli pronti **[D]**; non ha senso per testi disordinati.
- **Tempi.** Mesi.
- **Privacy.** Massimo controllo, ma i dati di addestramento sarebbero dati dei clienti: art. 9, base giuridica, minimizzazione, diritto alla cancellazione impossibile da garantire su pesi già addestrati **[D]**.
- **Lock-in.** Nessuno verso fornitori; totale verso il proprio team.
- **Rischi di manutenzione.** Massimi: sicurezza, aggiornamenti, deriva.
- **Adatto a.** Nessun processo elencato. **Scartare.**

### 2.2 Adattare un modello aperto (fine-tuning)

- **Costi una tantum.** Calcolo GPU per un fine-tuning leggero (LoRA) su 8–30 miliardi di parametri: ore o pochi giorni di GPU, quindi **decine–centinaia di euro per ciclo** a 1,40–2,73 €/h **[D]**. Il costo vero è il **dataset**: serve un insieme di coppie (input disordinato → output atteso) corrette da professionisti; senza dati reali dell'app, dati sintetici o anonimizzati. Stima **[D]**: 1.000–5.000 esempi curati = settimane di lavoro umano di cui non conosco il costo **[?]**.
- **Ricorrenti.** Servire il modello (come 2.3) o un fine-tuning ospitato dal fornitore, più rivalutazione a ogni nuova versione del modello base: il tuning va rifatto.
- **Persone.** Almeno una persona con esperienza ML/valutazione (0,25–0,5 FTE **[D]**); nessuna competenza interna oggi.
- **Qualità sull'italiano e su testi disordinati.** Può migliorare un compito stretto e ripetitivo (es. mappare colonne Excel verso il tuo schema) **[D]**. Su testo libero non garantisce di battere un buon modello pronto con un buon prompt. Nessun benchmark verificato **[?]**.
- **Tempi.** Settimane–mesi, dominati dalla raccolta dati.
- **Privacy.** Dati dei clienti usati per addestrare i pesi: stesse difficoltà di 2.1 (cancellazione, base giuridica, art. 9). Solo con dati sintetici o anonimizzati in modo robusto **[D]**.
- **Lock-in.** Vincolato al modello base scelto e alla sua licenza **[?]** (licenze dei singoli modelli non verificate qui: da leggere).
- **Rischi di manutenzione.** Regressioni a ogni aggiornamento del modello base; valutazione continua.
- **Adatto a.** Eventualmente importazione/mappatura in v2+ **se** la qualità con prompt non basta. Non adatto a messaggi liberi e riassunti (rischio di regressione e di memorizzazione di dati personali).

### 2.3 Self-hosting di un modello aperto in UE

- **Costi una tantum.** Integrazione (server di inferenza, bilanciamento, monitoraggio, sicurezza, valutazione): **~2–6 settimane-persona** **[D]**.
- **Ricorrenti.** GPU sempre accese: 1 × L40S (Scaleway) ≈ 1.022 €/mese; 1 × H100 PCIe ≈ 1.993 €/mese **[F]/[?]** (sez. 1.3). Per alta disponibilità servono **due** istanze: ~2.044 € (L40S) o ~3.986 € (H100) al mese, più rete e archiviazione. Più **0,25–0,5 FTE** di operazione **[D]**, non verificato.
- **Persone.** Un profilo DevOps/MLOps con esperienza di inferenza GPU; non presente.
- **Qualità sull'italiano e su testi disordinati.** Dipende dal modello aperto scelto e dalla taglia che entra nella GPU (L40S 48 GB: modelli intorno a 24–30 miliardi quantizzati **[D]**; H100 80 GB: più margine). Verosimilmente inferiore ai modelli di frontiera a pagamento **[D]**. Nessuna misura **[?]**.
- **Tempi.** 1–2 mesi per un servizio affidabile **[D]**.
- **Privacy.** **Massima**: i dati non lasciano l'infrastruttura scelta in UE, nessun subfornitore di modello. Restano il DPA con il fornitore cloud, cifratura e log. Questo è il vero argomento a favore.
- **Lock-in.** Basso sul modello (si può cambiare), medio sul fornitore cloud; licenze dei modelli aperti da verificare **[?]**.
- **Rischi di manutenzione.** Alti: aggiornamenti di driver/engine, capacità, sicurezza (prompt injection, esfiltrazione), costi fissi anche a carico zero, obsolescenza dei modelli.
- **Adatto a.** Gli stessi processi dell'API ma solo se si accetta qualità da modello medio. Per riassunti e bozze di messaggi è plausibile; per importazione di documenti molto disordinati è più rischioso **[D]**.

### 2.4 Riferimento: API di un fornitore pronto

- **Costi una tantum.** Quasi zero: integrazione di un client dietro un'interfaccia propria (~1–2 settimane-persona **[D]**), DPA e valutazione fornitori.
- **Ricorrenti.** A consumo (sez. 5): da ~0,1 a ~2 $/mese per organizzazione con i volumi ipotizzati, a seconda del modello. Nessun costo fisso.
- **Persone.** Backend Java (Spring Boot) già presente; competenze di prompt e valutazione (non ML).
- **Qualità sull'italiano e su testi disordinati.** Presumibilmente la migliore disponibile, ma **non misurata** da me **[?]**: va misurata su un set di prova (sez. 7).
- **Tempi.** Giorni–settimane.
- **Privacy.** Dipende dal fornitore: **residenza UE non uniforme** (sez. 1.2). Anthropic diretta: no UE **[F]**. Google Vertex: `eu` **[F]**. OpenAI: endpoint regionali, UE da confermare **[?]**. Mistral: da confermare nel DPA **[?]**.
- **Lock-in.** Medio-basso se si usa un'interfaccia propria e il formato dei prompt si può portare. Rischio **di prezzo**: il caso Gemini 3.8 Flash dimostra che i listini possono raddoppiare in una data fissa **[F]**.
- **Rischi di manutenzione.** Ritiro dei modelli (la pagina Google segnala modelli deprecati con data), cambi di comportamento a ogni versione, limiti di velocità, disponibilità.
- **Adatto a.** Tutti i processi elencati, con le cautele di privacy della sez. 3.

---

## 3. Privacy e GDPR art. 9: criteri minimi per qualunque fornitore

I dati di una piattaforma di allenamento e nutrizione (misure, foto, anamnesi, allergie, aderenza) sono in larga parte **dati relativi alla salute** **[D]**. Condizioni per inviarli a un modello:

1. **Base giuridica e consenso** coerenti con i consensi granulari già nel prodotto (user story R2): un consenso specifico per il trattamento da parte di strumenti automatici **[D]**.
2. **DPA ex art. 28** con il fornitore, sub-responsabili noti, **regione UE contrattuale** (non solo "preferenza"), conservazione zero o minima, **nessun addestramento** sui dati, notifica violazioni.
3. **Minimizzazione e pseudonimizzazione prima dell'invio**: non mandare nome, contatti, foto né note libere se non servono; funzione di redazione lato server **[D]**. In ottica ADR-0004/0005, i dati art. 9 restano cifrati per campo: l'AI vede solo ciò che il modulo autorizza, mai l'intero database.
4. **Audit**: ogni invio registrato (chi, cosa, quale modello) nel registro append-only.
5. **DPIA** (art. 35) prima dell'uso su dati sanitari e informativa aggiornata **[D]**.
6. **Ruoli sanitari** (fisioterapista, medico, vedi ADR-0008): l'AI non va abilitata finché non c'è il parere legale/DPO già raccomandato.
7. **Normativa AI**: valutare con il legale gli obblighi di trasparenza e di supervisione umana (regolamento UE sull'IA e disciplina nazionale) **[?]**. Non li ho verificati. L'architettura "anteprima + approvazione del professionista" è già coerente con l'idea di supporto, non di decisione.

Esito: con **fornitori che non offrono UE contrattuale** (oggi Anthropic diretta, e OpenAI se non confermato), i processi che toccano dati art. 9 non vanno attivati; restano possibili processi senza dati sanitari (supporto backoffice dei PT, smistamento ticket) **[D]**.

---

## 4. Idoneità per processo

Scala: ✅ adatto, 🟡 con cautele, ❌ sconsigliato. Tutte le uscite passano da **anteprima e approvazione** del professionista.

| Processo | Dati art. 9? | API UE | Open aperto via API UE | Self-hosting | Fine-tuning | Note |
|---|---|---|---|---|---|---|
| Importazione schede/storico da Excel/app | Sì (misure, note) | ✅ | ✅ | 🟡 | 🟡 (v2+, solo se serve) | Compito strutturato: spesso bastano prompt + validazione di schema. Cruciale per il costo di cambio. L'AI propone la mappatura, il professionista conferma riga per riga **[D]**. |
| Smistamento supporto e bozze risposte backoffice | No (se si filtra) | ✅ (anche non UE, con DPA) | ✅ | 🟡 | ❌ | Il più semplice e a basso rischio. |
| Bozze di messaggi al cliente | A volte | ✅ | ✅ | 🟡 | ❌ | Tono e italiano contano. Mai invio automatico. |
| Riassunti per il professionista | Sì | ✅ | ✅ | 🟡 | ❌ | Rischio di inesattezza: mostrare fonte e dati originali accanto al riassunto. Ricorrenti, quindi il processo che pesa di più sui costi. |
| Precompilazione moduli (anamnesi) | Sì | ✅ | 🟡 | 🟡 | ❌ | Serve controllo campo per campo; mai su ambiti non consentiti. |
| Segnalazione aderenza calante | Sì (dati sessioni) | ❌ **non serve l'LLM** | ❌ | ❌ | ❌ | **[D]** Si fa con regole e statistiche semplici sulle sessioni registrate (già previsti gli "indicatori di aderenza" in R3). Un LLM può eventualmente solo formulare la bozza del messaggio. |

---

## 5. Scenario di costo per organizzazione

### 5.1 Ipotesi esplicite sui volumi (tutte **[D]**, da validare con i dati reali)

| Voce | Ipotesi per organizzazione | Token in / out |
|---|---|---|
| Importazione una tantum | 1 esportazione (Excel/app) con ~12 settimane di storico per ~30 clienti, con un tentativo di correzione medio | 300.000 / 60.000 |
| Riassunti settimanali | 30 clienti attivi × 1 riassunto/settimana; 3.000 token in, 400 out ciascuno → 4,33 sett./mese | 390.000 / 52.000 al mese |
| Messaggi, supporto, moduli | 200.000 in e 45.000 out al mese in totale | 200.000 / 45.000 al mese |
| **Totale ricorrente** | | **590.000 in / 97.000 out al mese** |

Non incluse: video e foto (fuori scope; i modelli di visione costano di più), caching e batch (riducono i costi: sconto batch 50% **[F]**, cache fino al 90% per prompt ripetuti), picchi, retry oltre quelli già inclusi, costi di valutazione. **Sensibilità:** se i volumi reali fossero 10×, tutti i costi sotto si moltiplicano per 10.

### 5.2 API: costo mensile (USD, senza IVA) per numero di organizzazioni

Calcolo: (590.000 × prezzo input + 97.000 × prezzo output) ÷ 1.000.000 per organizzazione al mese; una tantum = (300.000 × input + 60.000 × output) ÷ 1.000.000. Prezzi dalla sez. 1.1.

| Modello (fascia) | Una tantum / org | Mese / org | 100 org | 1.000 org | 5.000 org |
|---|---|---|---|---|---|
| Haiku 5.5, gpt-6-luna (entrata) | 0,06 | 0,11 | 11 | 108 | 538 |
| Mistral Small 4 | 0,08 | 0,15 | 15 | 147 | 734 |
| Gemini 3.5 Flash-Lite | 0,24 | 0,42 | 42 | 420 | 2.098 |
| gpt-6.1-terra (media) | 0,60 | 1,08 | 108 | 1.075 | 5.375 |
| Gemini 3.8 Flash (da 1/1/2027) | 0,90 | 1,61 | 161 | 1.613 | 8.063 |
| Sonnet 5.5 (alta) | 1,20 | 2,15 | 215 | 2.150 | 10.750 |

Lettura **[D]**: anche con il modello più caro il costo è ~2 $/mese per organizzazione, cioè circa **il 5% di un ricavo ipotizzato di ~45 €/mese** (`analisi-mercato-seguito.md`, sez. 6) e dentro la fascia di costi variabili di 5–9 €/account già ipotizzata. Con i modelli di entrata scende allo 0,3%. **Il rischio di far lievitare i costi non sta nel listino dei token ma in:** (a) volumi molto superiori alle ipotesi (video/foto, storico lungo, cicli di correzione automatici); (b) uso di modelli di fascia alta "per sicurezza"; (c) cambi di listino come il raddoppio Gemini; (d) tempo di sviluppo e valutazione, che nei piccoli volumi supera di gran lunga la spesa in token.

### 5.3 Self-hosting: costo fisso e soglia di pareggio

Costi mensili fissi (€, GPU Scaleway sez. 1.3, 730 ore; **[?]** prezzi da confermare):

| Configurazione | Costo mensile |
|---|---|
| 2 × L40S (alta disponibilità), solo GPU | 2.044 |
| 2 × L40S + 0,25 FTE operazione (ipotesi 3.000 €) | ~5.044 |
| 2 × H100 PCIe, solo GPU | 3.986 |
| 2 × H100 + 0,5 FTE (ipotesi 6.000 €) | ~9.986 |

Il costo per FTE (3.000 €/mese per 0,25 FTE ≈ 12.000 €/mese per persona a pieno carico) è **un'ipotesi mia, non verificata** **[?]**.

**Organizzazioni necessarie perché il self-hosting costi meno dell'API** (costo fisso ÷ costo API per organizzazione, volumi base della sez. 5.1):

| Confronto con… | 2×L40S solo GPU | 2×L40S + 0,25 FTE | 2×H100 + 0,5 FTE |
|---|---|---|---|
| Sonnet 5.5 (alta) | ~950 | ~2.350 | ~4.650 |
| gpt-6.1-terra | ~1.900 | ~4.700 | ~9.300 |
| Gemini 3.8 Flash (da 1/1/27) | ~1.270 | ~3.100 | ~6.200 |
| Mistral Small 4 (qualità comparabile a un open 24B) | ~13.900 | ~34.400 | ~68.100 |
| Scaleway Mistral Small 3.2 (open ospitato in UE) | ~16.700 | ~41.200 | ~81.600 |

**Come leggere la soglia [D].**
- **Confronto onesto = stessa qualità.** Un modello aperto da 24–30 miliardi nella L40S è paragonabile a un modello piccolo, non a Sonnet. Quindi il termine giusto è la riga "Mistral Small / open via API UE": la soglia è **oltre 14.000 organizzazioni** (oltre 34.000 con la persona di operazione). Ben oltre il caso massimo di 5.000.
- **Con volumi 10× superiori** (stessa qualità): soglia ~1.400 organizzazioni (solo GPU) o ~3.400 (con 0,25 FTE). Il self-hosting diventa ragionevole solo se i volumi reali sono davvero dell'ordine di 10× quelli ipotizzati *e* si hanno oltre 1.500 organizzazioni.
- **Contro un modello di fascia alta** la soglia è 1.000–4.600 organizzazioni, ma è un confronto non omogeneo: un modello aperto ospitato non offre quella qualità.
- **Carico:** 5.000 organizzazioni ≈ 3,4 miliardi di token/mese ≈ 1.300 token/s in media **[D]**. Se una L40S regga questo carico con un modello da ~24B non lo so (dipende da batching e lunghezza dei prompt) **[?]**: va misurato prima di dimensionare.
- Il self-hosting ha un costo fisso anche a zero clienti: a 100 organizzazioni costerebbe ~20–50 € per organizzazione al mese (solo GPU: 2.044/100 ≈ 20 €), contro 0,11–2,15 $ con l'API. **[D]**

---

## 6. Raccomandazione per fase

### MVP (R1–R3): nessuna funzione AI, preparare il terreno
- Non costruire AI nell'MVP: coerente con la story map (fuori scope) e con la priorità (isolamento tenant, consensi, cross-org).
- **Due decisioni a costo quasi nullo:** (1) un'interfaccia `AiGateway` nel monolite (ADR-0002) che nasconda fornitore e modello; (2) registrare nei consensi un ambito "trattamento con strumenti automatici" per non dover riconsentire tutti più avanti **[D]**.
- Nessun costo ricorrente. Usare questo tempo per **costruire il set di prova** (sez. 7) e per contattare i fornitori sul DPA.
- **Non** sviluppare l'importazione in R1–R3, ma disegnare ora il formato interno di importazione (schede, serie, misure), che serve comunque.

### v1: API con residenza UE contrattuale, modello piccolo, processi a basso rischio
- **Modello di consegna:** API di un fornitore con inferenza UE garantita da contratto. Candidati verificabili: Google Vertex (endpoint `eu` **[F]**), Mistral (da confermare **[?]**), un open ospitato da fornitore UE (Scaleway **[?]**). Anthropic e OpenAI solo dopo aver ottenuto per iscritto regione UE e conservazione zero **[?]**.
- **Processi in ordine:** (1) importazione guidata da Excel/CSV con anteprima riga per riga; (2) bozze di messaggi e smistamento supporto; (3) riassunti settimanali, dopo (1) e (2).
- **Controlli di costo** (la preoccupazione principale): modello di entrata come predefinito, limite mensile di token per organizzazione (tetto rigido con avviso), cache e batch dove si può, nessun retry automatico illimitato, **cruscotto di consumo per organizzazione**, e un test di regressione sul costo per task. Preventivo di spesa: 0,1–2 $/org/mese con le ipotesi della sez. 5, quindi 11–215 $/mese a 100 organizzazioni **[D]**.
- **Persone:** nessuna nuova competenza ML; un/a sviluppatore/trice backend più una persona di prodotto per i prompt e la valutazione.
- **Condizione di uscita:** qualità misurata sul set di prova (sez. 7) e DPIA approvata.

### v2+: ottimizzare per costo, privacy e qualità, solo con dati
- Rivalutare a ogni trimestre il prezzo contro i listini (il raddoppio Gemini dal 1/1/2027 mostra il rischio **[F]**), passando facilmente fra fornitori grazie ad `AiGateway`.
- **Open model via API UE** per i dati più sensibili, se i clienti lo chiedono come argomento commerciale ("i tuoi dati non lasciano l'UE"); costo comparabile a un modello piccolo.
- **Self-hosting** solo se: (a) un vincolo contrattuale o normativo lo impone, oppure (b) oltre ~1.500 organizzazioni *e* volumi reali ≥ 10× le ipotesi, con una persona di operazione già in organico. Non per risparmiare nei volumi attuali.
- **Fine-tuning** solo se il test mostra che prompt e validazione non bastano su un compito stretto (probabilmente la mappatura di importazione), e se esiste un archivio di correzioni reali approvate dai professionisti con la base giuridica giusta.
- **Addestramento da zero:** mai.

---

## 7. Passi concreti e cosa misurare prima di decidere

1. **Costruire un set di prova** di 100–200 esempi reali o realistici: file Excel disordinati di PT, scambi di messaggi in italiano con errori e abbreviazioni, riassunti attesi. Metriche: accuratezza della mappatura, percentuale di righe da correggere a mano, errori gravi (dato inventato) e tempo risparmiato. Soglia da fissare con i PT intervistati **[?]**.
2. **Confrontare 3 modelli** (uno di entrata per fornitore con UE verificabile) sullo stesso set, con tempi e costo per task.
3. **Chiedere per iscritto** a Google, Mistral, Scaleway, e a OpenAI/Anthropic (anche via Bedrock/Vertex): regione UE contrattuale, DPA, sub-responsabili, conservazione, addestramento.
4. **Rileggere i prezzi** al momento della decisione: Mistral (incoerenze), OpenAI (pagina marketing 403), Scaleway e OVH (leggere in console), valuta EUR.
5. **Misurare il carico reale** della v1 (token per task) per sostituire le ipotesi della sez. 5.1.
6. **Parere legale/DPO** su: art. 9, base giuridica dell'uso di AI sui dati dei clienti, titolarità nei casi cross-org, ruoli sanitari.

## 8. Fonti

- Anthropic: [Pricing](https://platform.claude.com/docs/en/about-claude/pricing), [Data residency](https://platform.claude.com/docs/en/manage-claude/data-residency) (lette il 20/11/2026).
- OpenAI: [Pricing, pagina developers](https://developers.openai.com/api/docs/pricing.md) (letta); `openai.com/api/pricing` → 403.
- Google: [Gemini API pricing](https://aistudio.google.com/docs/pricing) (letta parzialmente), [Vertex AI data residency](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/learn/data-residency).
- Mistral: [docs pricing](https://docs.mistral.ai/inference/pricing), [mistral.ai/pricing](https://mistral.ai/pricing#api-pricing) (parziale, incoerente con la prima per Large).
- Scaleway: [GPU Instances](https://www.scaleway.com/en/gpu-instances/), [Generative APIs](https://www.scaleway.com/en/generative-apis/): prezzi da risultati di ricerca; fetch diretto 403 → **[?]**.
- OVHcloud: [prezzi Public Cloud (IE)](https://www.ovhcloud.com/en-ie/public-cloud/prices/), [GPU](https://www.ovhcloud.com/en/public-cloud/gpu/): prezzi da risultati di ricerca e fetch parziale; USD/EUR non riconciliati **[?]**.
- Documenti interni: `analisi-mercato.md`, `analisi-mercato-seguito.md`, `analisi-funzionale.md`, `tabella-funzionalita.md` (sez. 5), `user-story-map-mvp.md`, ADR-0002, ADR-0008.
