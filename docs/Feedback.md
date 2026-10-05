# Feedback AIV

Versione **4.10**: 'Elemento interattivo principale' in cima a 'Pulsanti e indicatori', e 'Aspetto dei pulsanti principali' che vale anche per il FAB.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.10 è pubblicata: [v4.10](https://github.com/Roccobot/AIV/releases/tag/v4.10), con l'APK
[AIV-4.10.apk](https://github.com/Roccobot/AIV/releases/download/v4.10/AIV-4.10.apk).
Commit prodotto su `main`: `b3e3b76` (SlimVer 4.10 / versionCode 315).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.10**: quattro prove, dalla tua nota E del giro 4.04; il menu inferiore arriva con la 4.15. Giro **4.05**: `4.05-01`-`4.05-03` OK.

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

Le verifiche automatiche della 4.10 sono superate: banco di prova completo (550 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.10-01 | Non provato | | Attendere il collaudo. |
| 4.10-02 | Non provato | | Attendere il collaudo. |
| 4.10-03 | Non provato | | Attendere il collaudo. |
| 4.10-04 | Non provato | | Attendere il collaudo. |

## 1. 'Elemento interattivo principale' in cima a 'Pulsanti e indicatori'

Con AIV **4.10**, sul telefono, apri Impostazioni → **Pulsanti e indicatori**: la prima voce è **Elemento interattivo principale**, col tuo paragrafo e i gettoni `Tasto fluttuante` e `Pillola di icone`. Con `Pillola di icone` compare la seconda fila, `A scomparsa` ed `Estesa`; con `Tasto fluttuante` la seconda fila non c'è. Il paragrafo nomina il menu inferiore solo dalla 4.15, quando ci sarà il suo gettone.

## 2. 'Aspetto dei pulsanti principali' in fondo a 'Tema e dettagli grafici'

Apri **Tema e dettagli grafici**: l'ultima voce è **Aspetto dei pulsanti principali**, coi gettoni `Solido`, `Trasparente` e `Traslucido` (prima `Vetro`). La spiegazione sotto il titolo è mia, da validare: *Vale per il pulsante fluttuante e per le pillole di icone.* La voce c'è anche sul tablet.

## 3. L'aspetto arriva al FAB

Con `Tasto fluttuante` scelto, prova i tre aspetti su una cartella con delle immagini e scorri: con `Trasparente` il FAB lascia vedere le miniature sotto, con `Traslucido` le sfoca, con `Solido` è pieno come prima. Premuto, il FAB passa all'accento dell'altro tema tenendo l'aspetto scelto; a menu aperto resta la sola tinta, perché sotto c'è già la sfocatura.

## 4. La scelta di prima resta dopo l'aggiornamento

Se sulla 4.05 avevi la pillola accesa, dopo l'aggiornamento **Elemento interattivo principale** dice `Pillola di icone`, con la stessa pillola (`A scomparsa` o `Estesa`) e lo stesso aspetto. Lo stesso vale importando un file di impostazioni esportato con una versione fra la 4.02 e la 4.05.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| 'Elemento interattivo principale' in cima a 'Pulsanti e indicatori' | 4.10-01 | Non provato | | Attendere il collaudo. |
| 'Aspetto dei pulsanti principali' in fondo a 'Tema e dettagli grafici' | 4.10-02 | Non provato | | Attendere il collaudo. |
| L'aspetto arriva al FAB | 4.10-03 | Non provato | | Attendere il collaudo. |
| La scelta di prima resta dopo l'aggiornamento | 4.10-04 | Non provato | | Attendere il collaudo. |
| 'Pulsanti e indicatori' al primo posto | 4.05-01 | OK | Tutto OK. | Archiviata. |
| Il marchio nel tasto tondo, più in alto | 4.05-02 | OK | Tutto OK. | Archiviata. |
| Il tocco tondo nella pillola | 4.05-03 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: elemento interattivo principale, aspetto dei pulsanti principali e FAB, scelta conservata (`4.10-01`-`4.10-04`).
- **4.15**: il menu inferiore, sovrapposto alla griglia, col gettone e il paragrafo completo.
- **4.20**: il menu angolare 3x3 (G1), dopo la tua scelta sulla nona casella (C1-C4, in chat).
- **Concluso**: 'Pulsanti e indicatori' al primo posto, marchio nel tondo, tocco tondo (`4.05-01`-`4.05-03`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
