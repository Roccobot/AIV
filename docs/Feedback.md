# Feedback AIV

Versione **4.42**: il modulo Disegno si apre coi valori di fabbrica che hai scelto.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.42 è pubblicata: [v4.42](https://github.com/Roccobot/AIV/releases/tag/v4.42), con l'APK
[AIV-4.42.apk](https://github.com/Roccobot/AIV/releases/download/v4.42/AIV-4.42.apk).
Commit prodotto su `main`: `d15b764`, release dal commit `d15b764` (SlimVer 4.42 / versionCode 330; APK 8.507.033 byte, digest `4ec0c7fc`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.42**: una prova nuova. Giro **4.41**: `4.41-01` e `4.41-02` tutte OK.

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

Le verifiche automatiche della 4.42 sono superate: banco di prova completo (604 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.42-01 | Non provato | | Attendere il collaudo. |

## 1. I valori di fabbrica del Disegno

Chiudi e riapri l'editor completo, poi apri il modulo `Disegno`. Il cursore `Spessore` è a circa il 60% della corsa, il colore del tratto è il rosso `#FF4B3D` e il riempimento è il salmone `#FFAE8E` all'opacità del 15% circa (il tuo `#26FFAE8E`). Disegna un rettangolo e un'ellisse: nascono col tratto rosso e il riempimento salmone. Disegna una freccia: ha lo stesso tratto e nessun riempimento. Tutti gli strumenti di disegno partono dallo stesso spessore. Per tenere selezionabili i due colori di fabbrica, nella tavolozza il rosso di prima è diventato `#FF4B3D` e l'arancio è diventato il salmone `#FFAE8E` (nome `Salmone`): è una mia scelta, e se rivuoi il rosso e l'arancio di prima scrivilo qui.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| I valori di fabbrica del Disegno | 4.42-01 | Non provato | | Attendere il collaudo. |
| La punta vera mentre tieni Spessore | 4.41-01 | OK | Tutto OK, con una richiesta sui valori di fabbrica dello spessore, chiarita in chat. | Ripresa in `4.42-01`. |
| La lente sulle forme piccole | 4.41-02 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: i valori di fabbrica del Disegno (`4.42-01`).
- **Prossima versione**: Disegno G2 (4.50): un tocco seleziona un oggetto, il rettangolo mostra quattro vertici color accento e la freccia due punti, e i parametri cambiano l'oggetto scelto; poi spostare, eliminare, sopra e sotto, ridimensionare e ruotare con le maniglie.
- **Dopo**: Disegno G3 (4.60), il testo.
- **Concluso**: il modulo Disegno, prima fase (`4.40-01`-`4.40-03`), la punta vera di `Spessore` e la lente sulle forme piccole (`4.41-01`, `4.41-02`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
