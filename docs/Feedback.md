# Feedback AIV

Versione **5.51**: le finestre in cui si scrive non sono più tagliate dalla tastiera, in AIV GitHub e in AIV Play.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.51 è pubblicata: [v5.51](https://github.com/Roccobot/AIV/releases/tag/v5.51), con i due APK
[AIV-5.51.apk](https://github.com/Roccobot/AIV/releases/download/v5.51/AIV-5.51.apk) (AIV GitHub) e [AIV-Play-5.51.apk](https://github.com/Roccobot/AIV/releases/download/v5.51/AIV-Play-5.51.apk) (AIV Play).
Commit prodotto su `main`: `d05a135`, release dal commit `d05a135` (SlimVer 5.51 / versionCode 368; APK GitHub 11.032.813 byte, digest `6f51a19a`; APK Play 10.931.081 byte, digest `0253f39a`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.51**: una prova. Giro **5.50**: la rinomina di AIV Play confermata, con la finestra tagliata quando si apre dal visualizzatore.

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

Le verifiche automatiche della 5.51 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (40), compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.51-01 | Non provato | | Attendere il collaudo. |

## 1. Le finestre in cui si scrive, con la tastiera aperta

Installa la 5.51 delle due varianti. Apri un'immagine nel visualizzatore, tieni premuto e scegli `Rinomina`: con la tastiera aperta la finestra è intera, anteprima compresa, e finisce sopra la tastiera. Rifallo dalla griglia, con un file solo e poi con quattro o cinque file: i tasti `Annulla` e `Rinomina` restano sempre sopra la tastiera, e se lo spazio non basta scorre l'anteprima. Mentre scrivi, la finestra non deve muoversi. In AIV GitHub controlla allo stesso modo `Salva con nome`, `Estensione`, `Scarica` e `Nuova cartella`. Rifai la prova sul tablet, anche in orizzontale.

Letture mie, scelta B1: con spazio a sufficienza la finestra resta a 70 punti dal bordo alto; quando la tastiera non le lascia spazio sale fino al bordo alto, e il margine sotto i tasti può finire dietro la tastiera. Sul tuo Xiaomi 15, dalla griglia l'anteprima di un file solo scorre di circa 2dp. La causa e la correzione sono ragionate sulle tue due schermate e su come Android tratta queste finestre: il banco di prova non ha una tastiera, quindi la conferma è questa prova. In AIV Play, da Android 11, in `Impostazioni` sotto il riquadro dei tasti del menu ora compare anche la riga sul cestino (`Ripristina` al posto di `Rinomina`).

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Le finestre in cui si scrive, con la tastiera aperta | 5.51-01 | Non provato | | Attendere il collaudo. |
| AIV Play: la rinomina dei file | 5.50-01 | Tutto OK | La finestra Rinomina, aperta dal visualizzatore, era tagliata in basso. | Corretto nella 5.51. |

## Prossimi passi

- **In collaudo**: le finestre in cui si scrive con la tastiera aperta (`5.51-01`).
- **AIV GitHub, dopo**: l'aggiornamento automatico, tua richiesta del 2026-10-11; è una modifica strutturale, quindi prima ti porto le opzioni.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
