# Feedback AIV

Versione **5.20**: in AIV Play torna `Elimina`, che da Android 11 sposta i file nel cestino di sistema, come hai scelto (A1); su Android 9 e 10 elimina per sempre (B1).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.20 è pubblicata: [v5.20](https://github.com/Roccobot/AIV/releases/tag/v5.20), con i due APK
[AIV-5.20.apk](https://github.com/Roccobot/AIV/releases/download/v5.20/AIV-5.20.apk) (AIV GitHub) e [AIV-Play-5.20.apk](https://github.com/Roccobot/AIV/releases/download/v5.20/AIV-Play-5.20.apk) (AIV Play).
Commit prodotto su `main`: `9529ddc`, release dal commit `9529ddc` (SlimVer 5.20 / versionCode 364; APK GitHub 11.027.141 byte, digest `e30e21da`; APK Play 10.980.093 byte, digest `bac9dde7`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.20**: una prova. Giro **5.12**: la prova confermata.

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

Le verifiche automatiche della 5.20 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (21), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.20-01 | Non provato | | Attendere il collaudo. |

## 1. AIV Play: `Elimina` sposta nel cestino di Android

Installa `AIV-Play-5.20.apk` sopra la 5.12. Nel visualizzatore tieni premuta un'immagine: il riquadro ha `Condividi`, `Info` ed `Elimina`. Tocca `Elimina`: si apre la finestra di Android che chiede di spostare l'immagine nel cestino. Rispondi di no: non succede niente. Rifai e rispondi di sì: l'immagine sparisce e la notifica dice `1 file spostato nel cestino`. Poi nella griglia seleziona tre immagini e tocca `Elimina`: una finestra sola per tutte e tre, e la notifica dice `3 file spostati nel cestino`. Nelle impostazioni, in `Ordine dei pulsanti`, i due riquadri dei file hanno di nuovo `Elimina`. Rifai la prova sul tablet.

Letture mie: la finestra di Android non si può saltare, quindi AIV non ne aggiunge una sua; non c'è `Annulla`, perché il cestino di sistema è già la rete. Android tiene i file nel cestino circa 30 giorni; fino alla pagina `Cestino` di AIV Play (la 5.30) un file si recupera con un'app che legge quel cestino, per esempio Files di Google. Su Android 9 e 10 `Elimina` cancella per sempre dopo la conferma di AIV, e i tuoi due dispositivi non hanno quelle versioni.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV Play: `Elimina` col cestino di sistema | 5.20-01 | Non provato | | Attendere il collaudo. |
| AIV Play: pillola a scomparsa di fabbrica | 5.12-01 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: `Elimina` in AIV Play (`5.20-01`).
- **AIV Play, 5.30**: la pagina `Cestino` che legge il cestino di Android, con `Ripristina` e `Svuota`.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio.
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
