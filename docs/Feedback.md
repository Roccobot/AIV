# Feedback AIV

Versione **4.41**: nel modulo Disegno la punta vera mentre tieni `Spessore`, e la lente sulle forme piccole.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.41 è pubblicata: [v4.41](https://github.com/Roccobot/AIV/releases/tag/v4.41), con l'APK
[AIV-4.41.apk](https://github.com/Roccobot/AIV/releases/download/v4.41/AIV-4.41.apk).
Commit prodotto su `main`: `af59893`, release dal commit `af59893` (SlimVer 4.41 / versionCode 329; APK 8.508.645 byte, digest `ac354018`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.41**: due prove nuove. Giro **4.40**: `4.40-01`-`4.40-03` tutte OK.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale, coi conteggi in cima. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 4.41 sono superate: banco di prova completo (603 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.41-01 | Non provato | | Attendere il collaudo. |
| 4.41-02 | Non provato | | Attendere il collaudo. |

## 1. La punta vera mentre tieni Spessore

Nel modulo `Disegno`, con `Contorno` scelto, tieni il dito sul cursore `Spessore` e muovilo: in basso a destra sull'immagine compare un tondo pieno del colore della linea, grande quanto la punta che disegnerà, e cambia mentre sposti il cursore. Quando alzi il dito sparisce. L'angolo e il margine sono quelli del pennello di `Correggi` e `Fluidifica`, come hai chiesto in `4.40-01`.

## 2. La lente sulle forme piccole

Nel modulo `Disegno` disegna una forma piccola, per esempio un rettangolo largo un dito: appena il dito si muove compare la lente di `Correggi`, con l'inchiostro dentro, e segue il dito. Allarga la forma: oltre circa 1,5 cm sullo schermo la lente sparisce, e torna se la stringi di nuovo. Con la mano libera conta il riquadro del tratto fatto fin lì, con le altre penne la distanza dal punto di partenza al dito. La soglia di 1,5 cm è una mia scelta: se la vuoi più grande o più piccola, scrivilo qui.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La punta vera mentre tieni Spessore | 4.41-01 | Non provato | | Attendere il collaudo. |
| La lente sulle forme piccole | 4.41-02 | Non provato | | Attendere il collaudo. |
| Il modulo Disegno: rettangolo e freccia | 4.40-01 | OK | Tutto OK, con due richieste: la punta vera di Spessore e la lente sulle forme piccole. | Riprese in `4.41-01` e `4.41-02`. |
| Il riempimento con colore e opacità suoi | 4.40-02 | OK | Tutto OK. | Archiviata. |
| Il disegno nel file salvato, e Annulla | 4.40-03 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: la punta vera di `Spessore` e la lente sulle forme piccole (`4.41-01`, `4.41-02`).
- **Prossima versione**: Disegno G2 (4.50): un tocco seleziona un oggetto, il rettangolo mostra quattro vertici color accento e la freccia due punti, e i parametri cambiano l'oggetto scelto; poi spostare, eliminare, sopra e sotto, ridimensionare e ruotare con le maniglie.
- **Dopo**: Disegno G3 (4.60), il testo.
- **Concluso**: il modulo Disegno, prima fase (`4.40-01`-`4.40-03`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
