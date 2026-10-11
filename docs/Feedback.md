# Feedback AIV

Versione **5.40**: AIV Play ha la pagina `Cestino`, che mostra il cestino di Android, e la spiegazione di `Elemento interattivo principale` ha il testo che hai scritto tu. AIV GitHub non cambia.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.40 è pubblicata: [v5.40](https://github.com/Roccobot/AIV/releases/tag/v5.40), con i due APK
[AIV-5.40.apk](https://github.com/Roccobot/AIV/releases/download/v5.40/AIV-5.40.apk) (AIV GitHub) e [AIV-Play-5.40.apk](https://github.com/Roccobot/AIV/releases/download/v5.40/AIV-Play-5.40.apk) (AIV Play).
Commit prodotto su `main`: `1b0df29`, release dal commit `1b0df29` (SlimVer 5.40 / versionCode 366; APK GitHub 11.032.813 byte, digest `b82231bf`; APK Play 10.928.105 byte, digest `9e224a1e`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.40**: una prova e un'etichetta. Giro **5.30**: le due prove confermate, e l'etichetta riscritta da te.

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

Le verifiche automatiche della 5.40 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (33), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.40-01 | Non provato | | Attendere il collaudo. |

## 1. AIV Play: la pagina Cestino

Installa `AIV-Play-5.40.apk` sopra la 5.30, su Android 11 o successivo. Elimina due o tre immagini da una cartella e conferma nella finestra di Android. Dalla schermata iniziale apri `Cestino`: ci sono le immagini appena eliminate, dalla più recente; controlla che si veda la miniatura, che un tocco la apra nel visualizzatore e che `Info` funzioni. Seleziona un'immagine e tocca `Ripristina`: Android chiede conferma, e l'immagine torna nella sua cartella. Poi prova `Ripristina tutto` e, dopo aver eliminato di nuovo qualcosa, `Svuota il cestino`: in tutti e due i casi compare solo la finestra di Android, senza una conferma di AIV prima. Nel menu del cestino non c'è `Cronologia`. Rifai la prova sul tablet.

Letture mie: miniature, apertura e `Info` di un file nel cestino dipendono da quello che Android concede su quel file, e il banco di prova non lo vede; se una delle tre non funziona, scrivilo nel commento. Fuori dal cestino il tocco lungo su `Elimina` ora elimina per sempre, con la finestra di Android che lo dice, come in AIV GitHub: fino alla 5.30 spostava nel cestino. Su Android 9 e 10 il cestino di sistema non c'è, e la pagina non compare.

## Etichette testuali

### e-settings_main_control_desc_play · Spiegazione di 'Elemento interattivo principale' in AIV Play
Scegli come vuoi interagire con l'app quando tieni lo smartphone in verticale: puoi scegliere tra pulsante fluttuante ('FAB') e 'pillola' di icone.
<!-- Riscritta da te nel giro 5.30: è il tuo testo, in tutte le 28 lingue. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV Play: la pagina `Cestino` | 5.40-01 | Non provato | | Attendere il collaudo. |
| AIV Play: comandi e griglie | 5.30-01 | Tutto OK | | Chiusa. |
| AIV Play: impostazioni fissate | 5.30-02 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: la pagina `Cestino` di AIV Play (`5.40-01`) e l'etichetta che hai riscritto.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio.
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
