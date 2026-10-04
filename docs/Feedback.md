# Feedback AIV

Versione **3.73**: le correzioni del giro 3.73, cioè doppio tocco, maniglia, conferme in orizzontale e intestazione di casa sul telefono.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 3.73 è pubblicata: [v3.73](https://github.com/Roccobot/AIV/releases/tag/v3.73), con l'APK
[AIV-3.73.apk](https://github.com/Roccobot/AIV/releases/download/v3.73/AIV-3.73.apk).
Commit prodotto su `main`: `26cf4e1` (SlimVer 3.73 / versionCode 308).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **3.73**: tre prove in collaudo, che sostituiscono `3.72-01`, `3.72-02` e `3.72-04`. Giro **3.72**: `3.72-01`, `3.72-02` e `3.72-03` OK.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.73 sono superate: banco di prova completo (537 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.73-01 | Non provato | | Attendere il collaudo. |
| 3.73-02 | Non provato | | Attendere il collaudo. |
| 3.73-03 | Non provato | | Attendere il collaudo. |

## 1. Tornando alla griglia niente sfarfallio

Con AIV **3.73**, sul **telefono in verticale** apri un'immagine da una cartella e torna indietro alla griglia, più volte: il passaggio deve essere una dissolvenza pulita, senza righe che saltano.

La causa è dedotta e non misurata: dalla 3.71 il visualizzatore in verticale è a tutto schermo, e tornando alla griglia le barre di sistema ricompaiono con un'animazione. Lo spazio che la griglia lasciava loro cambiava a ogni fotogramma, mentre la schermata sfumava. Ora la griglia lascia lo spazio delle barre anche quando sono ancora nascoste. Se lo sfarfallio resta, dimmi se compare anche entrando nel visualizzatore o solo uscendo.

## 2. Tablet in verticale: maniglia più tenue

Sul **tablet in verticale** le due righe della maniglia sopra l'elenco hanno la metà dell'opacità della 3.72.

## 3. Intestazione di casa sul telefono in orizzontale

Sul **telefono in orizzontale**, nella schermata iniziale: il bordo sinistro dell'icona dell'app è allineato a quello delle icone delle cartelle sotto, e 'Astonishing' e 'Image Viewer' sono interi. Prova anche girando il telefono dall'altra parte, con il foro della fotocamera sul lato della colonna.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Ritorno alla griglia senza sfarfallio | 3.73-01 | Non provato | | Attendere il collaudo. |
| Tablet in verticale: maniglia più tenue | 3.73-02 | Non provato | | Attendere il collaudo. |
| Intestazione di casa sul telefono | 3.73-03 | Non provato | | Attendere il collaudo. |
| Doppio tocco sul posto | 3.72-01 | OK | Sfarfallio tornando alla griglia. | Archiviata; lo sfarfallio è la 3.73-01. |
| Tablet in verticale: maniglia sopra | 3.72-02 | OK | Opacità dimezzata. | Archiviata; la maniglia è la 3.73-02. |
| Conferme in orizzontale | 3.72-03 | OK | Tutto OK. | Archiviata. |
| Intestazione di casa sul telefono | 3.72-04 | Non approvato | Icona da allineare, testo tagliato. | Sostituita da 3.73-03. |

## Prossimi passi

- **In collaudo**: ritorno alla griglia (`3.73-01`), maniglia (`3.73-02`), intestazione di casa (`3.73-03`).
- **Rimasti fuori, da decidere se servono**: la vista 'Cartelle di sistema' tiene il FAB anche su tablet e in orizzontale; nella pillola di casa il tocco lungo su 'Mostra nascoste' non apre il pannello.
- **Concluso**: doppio tocco e conferme in orizzontale (`3.72-01`, `3.72-03`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
