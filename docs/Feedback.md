# Feedback AIV

Versione **5.60**: AIV GitHub si aggiorna da sé, e le finestre in cui si scrive non saltano più alla comparsa. Il documento è cumulativo con la 5.51.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.60 è pubblicata: [v5.60](https://github.com/Roccobot/AIV/releases/tag/v5.60), con i due APK
[AIV-5.60.apk](https://github.com/Roccobot/AIV/releases/download/v5.60/AIV-5.60.apk) (AIV GitHub) e [AIV-Play-5.60.apk](https://github.com/Roccobot/AIV/releases/download/v5.60/AIV-Play-5.60.apk) (AIV Play).
Commit prodotto su `main`: `6384cb0`, release dal commit `6384cb0` (SlimVer 5.60 / versionCode 369; APK GitHub 11.073.913 byte, digest `20c18612`; APK Play 10.948.341 byte, digest `1cd5b23d`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.60**: due prove e undici etichette, cumulativo con la 5.51. Giro **5.50**: la rinomina di AIV Play confermata, con la finestra tagliata quando si apre dal visualizzatore.

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

Le verifiche automatiche della 5.60 sono superate: banco di prova completo di AIV GitHub (714 prove), prove di AIV Play (43), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.51-01 | Non provato | | Attendere il collaudo. |
| 5.60-01 | Non provato | | Attendere il collaudo. |

## 1. Le finestre in cui si scrive, con la tastiera aperta

Installa la 5.60 delle due varianti. Apri un'immagine nel visualizzatore, tieni premuto e scegli `Rinomina`: la finestra compare già al suo posto, senza salti, ed è intera, anteprima compresa, sopra la tastiera. Rifallo dalla griglia, con un file solo e poi con quattro o cinque file: i tasti `Annulla` e `Rinomina` restano sempre sopra la tastiera, e se lo spazio non basta scorre l'anteprima. Mentre scrivi, la finestra non deve muoversi. In AIV GitHub controlla allo stesso modo `Salva con nome`, `Estensione`, `Scarica` e `Nuova cartella`. Rifai la prova sul tablet, anche in orizzontale.

Letture mie, scelta B1: con spazio a sufficienza la finestra resta a 70 punti dal bordo alto; quando la tastiera non le lascia spazio sale fino al bordo alto, e il margine sotto i tasti può finire dietro la tastiera. Il salto della 5.51 (tua nota in chat) veniva dalla tastiera che arriva un attimo dopo la finestra: adesso AIV ricorda quanto è alta e la mette nel conto da subito. La prima volta dopo l'installazione, in verticale e poi in orizzontale, non la conosce ancora: la finestra compare quando arriva la tastiera, cioè un istante dopo il tocco, sempre senza salto. In AIV Play, da Android 11, in `Impostazioni` sotto il riquadro dei tasti del menu compare anche la riga sul cestino.

## 2. AIV GitHub: l'aggiornamento automatico

Installa `AIV-5.60.apk` sopra la 5.51. In `Impostazioni`, sezione `Avanzate`, trovi `Aggiornamenti automatici`, acceso, e il tasto `Controlla ora`: toccalo, e compare `AIV è aggiornata`. Metti il telefono in modalità aereo e ritoccalo: compare `Aggiornamento non riuscito`. In AIV Play la voce non c'è.

Letture mie: la prova vera dell'aggiornamento arriva con la versione dopo questa. Quando la pubblico, all'apertura di AIV (o con `Controlla ora`) compare la finestra `AIV x.xx è disponibile`: con `Aggiorna`, la prima volta Android chiede di consentire ad AIV l'installazione di app, poi AIV scarica, verifica digest e firma e apre l'installazione di Android. Con `Più tardi` la proposta torna il giorno dopo. Il controllo all'apertura si fa una volta per giorno di calendario, e senza rete non dice niente.

## Etichette testuali

### e-settings_updates · Titolo della voce in `Avanzate`
Aggiornamenti automatici

### e-settings_updates_desc · Spiegazione della voce
Una volta al giorno, all'apertura, AIV cerca una versione nuova su GitHub.

### e-settings_updates_check · Tasto della voce
Controlla ora

### e-update_title · Titolo della finestra, col numero della versione nuova
AIV 5.61 è disponibile

### e-update_install · Tasto di conferma della finestra
Aggiorna

### e-update_later · Tasto di rinuncia della finestra
Più tardi

### e-update_downloading · Avviso durante lo scaricamento
Scarico l'aggiornamento...
<!-- Tre punti come in tutte le stringhe di AIV: il controllo delle traduzioni del repo non ammette il carattere unico. -->

### e-update_latest · Avviso dopo `Controlla ora`, se non c'è niente di nuovo
AIV è aggiornata

### e-update_failed · Avviso se scaricamento o installazione falliscono
Aggiornamento non riuscito

### e-update_untrusted · Avviso se la firma o il digest non tornano
L'aggiornamento non è firmato con la chiave di AIV, quindi non lo installo.

### e-update_permission · Avviso prima della pagina di Android sulle app sconosciute
Per aggiornare, consenti ad AIV di installare app.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV GitHub: l'aggiornamento automatico | 5.60-01 | Non provato | | Attendere il collaudo. |
| Le finestre in cui si scrive, con la tastiera aperta | 5.51-01 | Non provato | Nella 5.51 la finestra Rinomina saltava alla comparsa (tua nota in chat). | Corretto nella 5.60. |
| AIV Play: la rinomina dei file | 5.50-01 | Tutto OK | La finestra Rinomina, aperta dal visualizzatore, era tagliata in basso. | Corretto nella 5.51. |

## Prossimi passi

- **In collaudo**: le finestre in cui si scrive (`5.51-01`) e l'aggiornamento automatico di AIV GitHub (`5.60-01`).
- **AIV GitHub, alla versione dopo**: la prova vera dell'aggiornamento, da una release alla successiva.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
