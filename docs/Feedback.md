# Feedback AIV

Versione **5.12**: in AIV Play il comando di fabbrica è la pillola a scomparsa, come hai chiesto nel giro 5.11; AIV GitHub resta col menu Start.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.12 è pubblicata: [v5.12](https://github.com/Roccobot/AIV/releases/tag/v5.12), con i due APK
[AIV-5.12.apk](https://github.com/Roccobot/AIV/releases/download/v5.12/AIV-5.12.apk) (AIV GitHub) e [AIV-Play-5.12.apk](https://github.com/Roccobot/AIV/releases/download/v5.12/AIV-Play-5.12.apk) (AIV Play).
Commit prodotto su `main`: `a65dc16`, release dal commit `a65dc16` (SlimVer 5.12 / versionCode 363; APK GitHub 11.027.141 byte, digest `0a3dec5c`; APK Play 10.963.677 byte, digest `9c5b7cfd`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.12**: una prova. Giro **5.11**: le due prove confermate.

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

Le verifiche automatiche della 5.12 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (17), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.12-01 | Non provato | | Attendere il collaudo. |

## 1. AIV Play: la pillola a scomparsa di fabbrica

Installa `AIV-Play-5.12.apk` sopra la 5.11. Nella schermata iniziale, al posto del tondo del menu Start, c'è il tasto tondo della pillola: toccalo e si allunga nella pillola di icone, che si richiude sulla sua ×. Nelle impostazioni, in `Pulsanti e indicatori`, `Elemento interattivo principale` dice `Pillola di icone` con `A scomparsa`. Poi tocca `Ripristina gli avvisi` e torna sulla schermata iniziale: il velo d'aiuto è quello del tasto tondo (*Scorciatoia: tieni premuto il pulsante per le opzioni di visualizzazione.*).

Letture mie: il valore di fabbrica vale solo dove non hai mai scelto, quindi se in AIV Play avevi già scelto un altro elemento lo ritrovi com'era e la prova si fa scegliendo la pillola a mano; in AIV GitHub resta il menu Start. In AIV Play il menu Start torna di fabbrica solo quando lo dici tu.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV Play: pillola a scomparsa di fabbrica | 5.12-01 | Non provato | | Attendere il collaudo. |
| AIV Play: menu Start | 5.11-01 | Tutto OK | | Chiusa. |
| AIV Play: riga dell'accesso parziale | 5.11-02 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: la pillola a scomparsa di fabbrica in AIV Play (`5.12-01`).
- **AIV Play, pezzo 2**: le funzioni sui file rifatte con MediaStore, una per rilascio, cominciando da `Elimina`.
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
