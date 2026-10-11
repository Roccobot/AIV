# Feedback AIV

Versione **5.50**: AIV Play rinomina i file, da Android 11, con la stessa finestra di AIV GitHub e la conferma di Android. AIV GitHub non cambia.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.50 è pubblicata: [v5.50](https://github.com/Roccobot/AIV/releases/tag/v5.50), con i due APK
[AIV-5.50.apk](https://github.com/Roccobot/AIV/releases/download/v5.50/AIV-5.50.apk) (AIV GitHub) e [AIV-Play-5.50.apk](https://github.com/Roccobot/AIV/releases/download/v5.50/AIV-Play-5.50.apk) (AIV Play).
Commit prodotto su `main`: `887f28f`, release dal commit `887f28f` (SlimVer 5.50 / versionCode 367; APK GitHub 11.032.813 byte, digest `957933b6`; APK Play 10.928.105 byte, digest `3e4db741`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.50**: una prova. Giro **5.40**: la pagina `Cestino` confermata, e la tua etichetta registrata.

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

Le verifiche automatiche della 5.50 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (40), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.50-01 | Non provato | | Attendere il collaudo. |

## 1. AIV Play: la rinomina dei file

Installa `AIV-Play-5.50.apk` sopra la 5.40, su Android 11 o successivo. Apri una cartella, tieni premuta una miniatura, scegline altre due o tre e tocca `Rinomina`: la finestra è quella di AIV GitHub, con l'anteprima dei nomi e senza il tasto `Estensione`. Conferma: Android chiede il permesso di modificare i file, e dopo il sì i file prendono i nomi nuovi, numerati in ordine alfabetico. Rifallo e rispondi no ad Android: non cambia niente e non compare nessun avviso. Poi apri un'immagine, apri `Info` e tieni premuto il nome: si apre la stessa finestra, per quel file solo. Se in griglia hai acceso il nome sotto le miniature, controlla che mostri quelli nuovi. Rifai la prova sul tablet.

Letture mie: il permesso di Android si chiede a ogni rinomina, una volta per tutti i file scelti; se lo chiede anche per le immagini salvate da AIV, o non lo chiede per niente, scrivilo nel commento, perché il banco di prova non lo vede. Nel cestino `Ripristina` non è più il primo tasto: è al posto di `Rinomina`, come in AIV GitHub. Sotto Android 11 il tasto non c'è, e le cartelle non si rinominano.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV Play: la rinomina dei file | 5.50-01 | Non provato | | Attendere il collaudo. |
| AIV Play: la pagina `Cestino` | 5.40-01 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: la rinomina dei file in AIV Play (`5.50-01`).
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
