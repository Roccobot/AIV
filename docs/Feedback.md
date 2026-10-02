# Feedback AIV

Versione **3.40**: collaudo di rail, lati editor, HEIC, sotto-pagine Impostazioni e ultimo inpaint.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
La Release 3.40 è pubblicata: [v3.40](https://github.com/Roccobot/AIV/releases/tag/v3.40), con l'APK
[AIV-3.40.apk](https://github.com/Roccobot/AIV/releases/download/v3.40/AIV-3.40.apk).
Commit prodotto su `main`: `862b2c6` (icona `dbc0f50`; SlimVer 3.40 / versionCode 298).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.39** è stato consegnato due volte e travasato (ultimo invio 3 ottobre 2026, ~00:34 Europe/Rome, `cloudRevision` `c93be007-541d-44ae-863c-0d603935203a`).
Le prove OK del 3.39 sono in archivio sotto. Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Il giro **3.40** è stato consegnato e travasato il 3 ottobre 2026 (~01:07 Europe/Rome). Le prove sotto restano il testo del collaudo; gli esiti sono nella tabella.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ per il pannello); tieni premuto Salva (dischetto)
per **Consegna e copie**. Su desktop: Altro in colonna laterale, con pulsante Consegna a
destra dell'allegato. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.40 sono superate: 486 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet resta tuo.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.40-01 | Non approvato | Ricerca e maniglia OK. Lista non torna giù al tocco, ma non si sposta (traballa). Due icone in basso non centrate. | Rail in 3.41. |
| 3.40-02 | Tutto OK | | Chiusa. |
| 3.40-03 | Tutto OK | | Chiusa. |
| 3.40-04 | Accettabile | Riordino: Navigazione sciolta; Pulsanti e indicatori; Cestino; Gestione dei file; Avanzate. | Struttura Impostazioni in 3.41. |
| 3.40-05 | Tutto OK | Per ora resta così. | Filone inpaint chiuso, niente timbro clone. |

## 1. Rail tablet: Cerca, maniglia, lista, tasti

Su tablet (≥ 600), con AIV **3.40**: la colonna laterale.

- **Cerca** non finisce sotto l'orologio di sistema, né in orizzontale né in verticale.
- La **maniglia** è poco visibile (più trasparente). Il testo accessibile è `Sposta la lista delle cartelle`.
- Trascini la lista su o giù: si sposta e **resta** lì. Toccare una cartella **non** la riporta all'inizio e **non** deve bloccare il trascinamento successivo (niente lista che traballa e non si muove).
- **Cestino** e **Impostazioni** sono centrati nella colonna, non spostati a destra.

Sul documento 3.39 (ultimo invio) Cerca e maniglia erano già OK; il trascinamento no, e i due tasti restavano troppo a destra. Qui si verifica l'APK pubblicato.

## 2. Editor tablet: lati

Su tablet in **orizzontale** (larghezza ≥ 1024 dp): apri l'editor completo e quello semplice.
Di default i comandi stanno a **destra** e l'anteprima a **sinistra**.
Con il FAB a sinistra i lati si invertono (comandi a sinistra, anteprima a destra).
Lo schermo non sfarfalla.

## 3. HEIC: flash di colore in sfoglio

Apri e sfoglia un HEIC (esempio nel brief: `.memo/files/aiv-heic-color-flash-example.heic`).
Per tutta l'animazione di ingresso i colori restano quelli della foto: niente flash da toni più neutri a toni più vividi a fine animazione.
Ripeti su telefono e, se puoi, su tablet.

## 4. Impostazioni: Tema e pannelli, Navigazione, Avvio

Apri Impostazioni. Oltre alle pagine già note ci sono tre sotto-pagine: **Tema e pannelli**, **Navigazione**, **Avvio**.
I titoli non vanno a capo in modo illeggibile; dall'indice (anche su tablet) si aprono e si torna indietro.
Le voci che prima stavano ammassate sono in queste pagine, non duplicate a caso.

## 5. Correggi/Rimuovi: ultimo tentativo

Su un orlo o una linea (gli screenshot storici di `3.30-01`): riprova Correggi/Rimuovi.
L'icona dello strumento è un **cerotto adesivo**.
Il calcolo resta euristico, senza riconoscimento di forme: orli sottili e trame possono restare spezzati.
Segna se questo giro è accettabile come limite, oppure no.
Un riferimento manuale tipo timbro clone **non** è in questa versione: se lo vuoi, scrivilo nel commento.

## Etichette testuali

### e-folders-rail-move · Maniglia elenco cartelle
Sposta la lista delle cartelle

### e-settings-page-look · Sotto-pagina tema
Tema e pannelli

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Riscontri conclusi

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Prove 3.37 OK senza residuo | 3.37 | OK | 3.37-01..07 (con residui assorbiti). | Archiviata. |
| UX documento | 3.38 | In codice | Non è prova di collaudo app. | Manutenzione DF. |
| Griglia tablet più aria | 3.38-05 | OK | Tutto OK. | Archiviata. |
| Cestino a tutta larghezza | 3.38-06 | OK | Tutto OK; chiude 3.35-01. | Archiviata. |
| Apici → grassetto Impostazioni | 3.38-07 | OK | Tutto OK; chiude residuo 3.26-03 / 3.37-03. | Archiviata. |
| Indicatore già inclusa | 3.38-08 | OK | Tutto OK. | Archiviata. |
| Lentino soglia più bassa | 3.38-09 | OK | Tutto OK; chiude 3.37-08. | Archiviata. |
| Editor tablet flicker | 3.39-01 | OK | Flicker chiuso. I lati sono la prova 3.40-02. | Archiviata. |
| Menu FAB ~35% | 3.39-02 | OK | Tutto OK. | Archiviata. |
| Empty state mockup | 3.39-04 | OK | Tutto OK. | Archiviata. |
| Intestazione Modalità incluse | 3.13-02 | OK | Riprova OK in 3.39. | Archiviata. |
| Nascosta elenco/dicitura/avvisi | 3.13-08 | OK | Riprova OK in 3.39. | Archiviata. |

## Prossimi passi

- **3.41 (entra)**: rail `3.40-01`, il trascinamento della lista traballa e non la sposta; Cestino e Impostazioni non centrati. Cerca e maniglia OK.
- **3.41 (entra)**: riordino Impostazioni da `3.40-04` (Navigazione dentro Visualizzatore; sparisce Comandi e indicatori; Pulsanti e indicatori ultima di Aspetto; sotto-pagina Cestino; Gestione dei file; fusione in Avanzate).
- **Chiuso in questo giro**: lati editor `3.40-02`; HEIC `3.40-03`; inpaint `3.40-05` (per ora resta così, niente timbro clone).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
