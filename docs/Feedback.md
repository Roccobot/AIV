# Feedback AIV

Versione **3.42**: collaudo del rail tablet (Cerca, trascinamento, pressione lunga).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
La Release 3.42 è pubblicata: [v3.42](https://github.com/Roccobot/AIV/releases/tag/v3.42), con l'APK
[AIV-3.42.apk](https://github.com/Roccobot/AIV/releases/download/v3.42/AIV-3.42.apk).
Commit prodotto su `main`: `3d3b661` (SlimVer 3.42 / versionCode 300).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.40** è stato consegnato e travasato il 3 ottobre 2026 (~01:07 Europe/Rome).
Le prove OK del 3.40 sono in archivio sotto. Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor, HEIC e inpaint restano chiusi: l'inpaint resta com'è, niente timbro clone.
Il giro **3.41** è collaudato: riordino Impostazioni `3.41-02` Tutto OK, in archivio. Il rail `3.41-01` è Non approvato e non si segna OK: lo sostituisce `3.42-01`.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ per il pannello); tieni premuto Salva (dischetto)
per **Consegna e copie**. Su desktop: Altro in colonna laterale, con pulsante Consegna a
destra dell'allegato. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.42 sono superate: 495 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet resta tuo.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.42-01 | Non provato | | Attendere il collaudo. |

## 1. Rail tablet: Cerca, trascinamento, pressione lunga

Su tablet (larghezza ≥ 600), con AIV **3.42**.

**Cerca** è subito sopra **Cestino** e **Impostazioni**, non in cima alla colonna.

Trascina la lista delle cartelle con la maniglia: si muove col dito e **resta** dov'è. Non deve traballare né saltare indietro.

La pressione lunga sul nome di una cartella offre gli stessi comandi del telefono (nascondi, mostra, sorelle, autorizza) e chiede prima di nascondere. Il gesto chiede; non nasconde da solo.

## Etichette testuali

Nessuna etichetta aperta in questo giro. Queste quattro sono già nel prodotto e **non** vanno rilistate:

- `e-settings-page-controls`: Sotto-pagina pulsanti: Pulsanti e indicatori. Riordino Impostazioni 3.41-02 Tutto OK.
- `e-settings-group-files`: Sezione file: Gestione dei file. Riordino Impostazioni 3.41-02 Tutto OK.
- `e-settings-group-advanced`: Sezione avanzate: Avanzate. Riordino Impostazioni 3.41-02 Tutto OK.
- `e-folders-rail-move`: Maniglia elenco cartelle, solo lettore di schermo (contentDescription): Sposta la lista delle cartelle. Già nel prodotto.

Non sono prove. Non rimetterle tra le etichette aperte del DF.

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
| Riordino Impostazioni | 3.41-02 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **Collaudo 3.42**: rail `3.42-01` (Cerca subito sopra Cestino e Impostazioni; la lista segue il dito e resta, senza traballare né saltare indietro; pressione lunga con gli stessi comandi del telefono, e chiede prima di nascondere).
- **Concluso**: riordino Impostazioni `3.41-02` (Tutto OK).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`; inpaint `3.40-05` (resta così, niente timbro clone).
- **Progettato, non in questa build**: misure relative (rem/em).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
