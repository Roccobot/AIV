# Feedback AIV

Versione **3.41**: collaudo del rail e del riordino delle Impostazioni.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
La Release 3.41 è pubblicata: [v3.41](https://github.com/Roccobot/AIV/releases/tag/v3.41), con l'APK
[AIV-3.41.apk](https://github.com/Roccobot/AIV/releases/download/v3.41/AIV-3.41.apk).
Commit prodotto su `main`: `cbba078` (SlimVer 3.41 / versionCode 299).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.40** è stato consegnato e travasato il 3 ottobre 2026 (~01:07 Europe/Rome).
Le prove OK del 3.40 sono in archivio sotto. Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor, HEIC e inpaint restano chiusi: l'inpaint resta com'è, niente timbro clone.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ per il pannello); tieni premuto Salva (dischetto)
per **Consegna e copie**. Su desktop: Altro in colonna laterale, con pulsante Consegna a
destra dell'allegato. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.41 sono superate: 492 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet resta tuo.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.41-01 | Non provato | | Attendere il collaudo. |
| 3.41-02 | Non provato | | Attendere il collaudo. |

## 1. Rail tablet: lista e tasti in basso

Su tablet (larghezza ≥ 600), con AIV **3.41**.

Trascina la lista delle cartelle su o giù con la maniglia: la lista **si sposta** e **resta** dov'è. Non deve traballare senza muoversi. Toccare una cartella non la riporta all'inizio.

**Cestino** e **Impostazioni** sono centrati in fondo alla colonna, non spostati a destra.

**Cerca** non finisce sotto l'orologio, né in orizzontale né in verticale. La maniglia resta poco visibile. Il suo testo è `Sposta la lista delle cartelle`.

## 2. Impostazioni: riordino

Apri Impostazioni, su telefono e su tablet.

- **Navigazione** non c'è, né come sezione né come sotto-pagina. Le due voci che erano lì stanno nella sotto-pagina **Visualizzatore**.
- La sezione **Comandi e indicatori** non c'è.
- **Etichette e pulsanti** si chiama **Pulsanti e indicatori** ed è l'ultima sotto-pagina di **Aspetto**.
- Le due voci del cestino stanno in una sotto-pagina **Cestino**, ultima di **Gestione dei file** (prima si chiamava Modifica e protezione dei file).
- **Funzionalità avanzate** e **Gestione dell'app** sono una sezione sola, **Avanzate**, con le voci nello stesso ordine di prima.

## Etichette testuali

### e-settings-page-controls · Sotto-pagina pulsanti
Pulsanti e indicatori

### e-settings-group-files · Sezione file
Gestione dei file

### e-settings-group-advanced · Sezione avanzate
Avanzate

### e-folders-rail-move · Maniglia elenco cartelle
Sposta la lista delle cartelle

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Riscontri conclusi

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Prove 3.37 OK senza residuo | 3.37 | OK | 3.37-01..07 (con residui assorbiti). | Archiviata. |
| UX documento | 3.38 | In codice | Non è prova di collaudo app. | Manutenzione DF. |
| Griglia tablet più aria | 3.38-05 | OK | Tutto OK. | Archiviata. |
| Cestino a tutta larghezza | 3.38-06 | OK | Tutto OK; chiude 3.35-01. | Archiviata. |
| Apici a grassetto Impostazioni | 3.38-07 | OK | Tutto OK; chiude residuo 3.26-03 / 3.37-03. | Archiviata. |
| Indicatore già inclusa | 3.38-08 | OK | Tutto OK. | Archiviata. |
| Lentino soglia più bassa | 3.38-09 | OK | Tutto OK; chiude 3.37-08. | Archiviata. |
| Editor tablet flicker | 3.39-01 | OK | Flicker chiuso. | Archiviata. |
| Menu FAB ~35% | 3.39-02 | OK | Tutto OK. | Archiviata. |
| Empty state mockup | 3.39-04 | OK | Tutto OK. | Archiviata. |
| Intestazione Modalità incluse | 3.13-02 | OK | Riprova OK in 3.39. | Archiviata. |
| Nascosta elenco/dicitura/avvisi | 3.13-08 | OK | Riprova OK in 3.39. | Archiviata. |
| Lati editor | 3.40-02 | OK | Tutto OK. Non riaprire. | Archiviata. |
| HEIC senza flash | 3.40-03 | OK | Tutto OK. Non riaprire. | Archiviata. |
| Inpaint | 3.40-05 | OK | Per ora resta così. Niente timbro clone. | Archiviata. |

## Prossimi passi

- **Collaudo 3.41**: rail `3.41-01` (lista che si sposta e resta; tasti in basso centrati; Cerca e maniglia senza regressione) e riordino Impostazioni `3.41-02`.
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`; inpaint `3.40-05` (resta così, niente timbro clone).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
