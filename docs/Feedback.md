# Feedback AIV

Versione **3.53**: collaudo del bordo sfumato di Correggi/Rimuovi.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 3.53 è pubblicata: [v3.53](https://github.com/Roccobot/AIV/releases/tag/v3.53), con l'APK
[AIV-3.53.apk](https://github.com/Roccobot/AIV/releases/download/v3.53/AIV-3.53.apk).
Commit prodotto su `main`: `15640bb` (SlimVer 3.53 / versionCode 302).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **3.52**: `3.52-02` OK; `3.52-01` Non approvato per il bordo troppo netto, che la 3.53 sfuma (`3.53-01`).

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.53 sono superate: banco di prova completo, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono resta tuo.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.53-01 | Non provato | | Attendere il collaudo. |

## 1. Correggi/Rimuovi: il bordo sfuma nell'immagine

Con AIV **3.53**, ripeti la prova della 3.52: apri una **foto del telefono ad alta risoluzione** nell'**editor completo**, modulo **Dettaglio**, tocca **Correggi/Rimuovi**, dipingi sopra un oggetto da togliere e tocca **Applica**.

Il bordo dell'area corretta non deve più vedersi come una linea: per qualche pixel fuori dalla selezione la ricostruzione si mescola all'immagine, e dentro la selezione il difetto non riaffiora. Prova sia col pennello piccolo sia al massimo; se puoi, allega il prima e il dopo.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Correggi/Rimuovi: bordi netti e trame | 3.52-02 | OK | Tutto OK. | Archiviata. |
| Correggi/Rimuovi: aree grandi | 3.52-01 | Non approvato | Bordo troppo netto, da sfumare. | Sostituita da 3.53-01. |
| Rail tablet | 3.42-01 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: bordo sfumato di Correggi/Rimuovi, `3.53-01`.
- **Concluso**: bordi netti e trame regolari `3.52-02` (Tutto OK).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
