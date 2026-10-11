# Feedback AIV

Versione **5.71**: l'aggiornamento automatico arriva fino all'installazione, un filmato grande non fa più chiudere AIV, e la durata dei video resta su una riga anche in orizzontale.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.71 è pubblicata: [v5.71](https://github.com/Roccobot/AIV/releases/tag/v5.71), con i due APK
[AIV-5.71.apk](https://github.com/Roccobot/AIV/releases/download/v5.71/AIV-5.71.apk) (AIV GitHub) e [AIV-Play-5.71.apk](https://github.com/Roccobot/AIV/releases/download/v5.71/AIV-Play-5.71.apk) (AIV Play), più [AIV-Play-5.71.aab](https://github.com/Roccobot/AIV/releases/download/v5.71/AIV-Play-5.71.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `0a382cf`, release dal commit `0a382cf` (SlimVer 5.71 / versionCode 372; APK GitHub 11.073.921 byte, digest `bd2bc4bf`; APK Play 10.948.349 byte, digest `adb29aca`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.71**: tre prove e le tre domande sullo Store, rimaste senza risposta nel giro 5.70. Giro **5.70**: l'aggiornamento automatico si fermava dopo lo scaricamento, e le dieci etichette sono confermate.

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

Le verifiche automatiche della 5.71 sono superate: banco di prova completo di AIV GitHub (719 prove), prove di AIV Play (43), controllo delle traduzioni, compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.71-01 | Non provato | | Attendere il collaudo. |
| 5.71-02 | Non provato | | Attendere il collaudo. |
| 5.71-03 | Non provato | | Attendere il collaudo. |

## 1. AIV GitHub: l'aggiornamento fino all'installazione

Parti dalla 5.70 di AIV GitHub che hai installato con Obtainium. In `Impostazioni`, sezione `Avanzate`, tocca `Controlla adesso`: compare la finestra `È disponibile AIV 5.71`. Tocca `Aggiorna`: la finestra si chiude, compare `Scarico l'aggiornamento...`, e alla fine si apre l'installazione di Android. Conferma, e riapri AIV: `Controlla adesso` deve rispondere `AIV è aggiornata`.

Letture mie: nella 5.70 la finestra si chiudeva al tocco e si portava via il lavoro appena avviato, quindi lo scaricamento finiva e l'installazione non partiva, senza nessun avviso. Adesso il lavoro non dipende più dalla finestra. Se Android chiede di consentire ad AIV l'installazione di app, attiva l'interruttore, torna indietro e tocca di nuovo `Aggiorna`: la finestra in quel caso resta aperta apposta. La prova sul banco simula lo scaricamento con un server finto, quindi l'installazione vera si vede solo sul telefono.

## 2. Un filmato grande

Apri dalla griglia il file MKV da 250 MB che nella 5.70 faceva chiudere AIV, e lascialo partire. Prova anche a scorrere al filmato prima e a quello dopo. AIV non deve chiudersi.

Letture mie: per ogni pagina il visualizzatore chiede se è un'immagine animata (GIF o WebP), filmati compresi, e fino alla 5.70 per rispondere leggeva tutto il file in memoria: 250 MB in un blocco solo sono più di quanto l'app può tenere. Adesso guarda i primi 64 byte, e di un filmato non legge altro. È la causa che ho trovato nel codice e che la prova sul banco misura; un crash con un'altra causa lo vedresti ancora, e allora mi serve sapere in che momento succede (all'apertura, durante la riproduzione, scorrendo).

## 3. La durata dei video in orizzontale

Metti il telefono in orizzontale e apri la cartella di un video, come nella schermata del giro 5.70. Sulla miniatura la durata resta su una riga sola, accanto al triangolo, anche quando la miniatura è stretta.

Letture mie: quando lo spazio non basta, la durata si rimpicciolisce fino a un corpo minimo di 8 punti, e solo sotto quella misura verrebbe tagliata. In verticale, dove lo spazio c'è, resta della misura di sempre.

## Domande

### d-store-testi · I testi per Google Play e l'informativa
Leggi i testi della scheda e le risposte ai moduli in [docs/Store.md](https://github.com/Roccobot/AIV/blob/main/docs/Store.md), e l'[informativa sulla privacy](https://roccobot.github.io/AIV/privacy.html). Ogni frase dell'informativa e dei moduli corrisponde a quello che fa il codice; le descrizioni della scheda elencano solo funzioni che AIV Play ha davvero.

**E1**: Approvo così: li uso nella Console come sono.

**E2**: Li correggo: le correzioni sono nel commento.

Parere: **E1** se ti tornano, altrimenti **E2** con le frasi da cambiare.

### d-store-contatto · Il contatto dell'informativa
Google vuole un modo per contattare lo sviluppatore. Oggi l'informativa rimanda alle segnalazioni del repository su GitHub.

**F1**: Le segnalazioni su GitHub, come adesso: niente indirizzi personali in pagina.

**F2**: Un indirizzo email: scrivilo nel commento, e lo metto nell'informativa.

Parere: **F1**, perché non espone un tuo indirizzo; la Console mostra comunque l'email che darai all'account.

### d-store-grafica · La grafica che Google chiede
Google vuole l'icona a 512 x 512, un'immagine in evidenza di 1024 x 500 e almeno due schermate del telefono.

**G1**: Icona e immagine in evidenza le preparo io dai disegni dell'app; le schermate le fai tu dal telefono, e me le mandi.

**G2**: Prepari tutto tu.

Parere: **G1**: l'icona e l'immagine nascono dai disegni che il repository ha già, le schermate devono mostrare l'app vera.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV GitHub: l'aggiornamento da una release alla successiva | 5.70-01 | Non approvato | Scaricato, ma poi non si installa: ripiegato su Obtainium. | Corretto nella 5.71. |
| AIV GitHub: l'aggiornamento automatico | 5.60-01 | Tutto OK | Quattro etichette corrette. | Corrette nella 5.61. |

## Prossimi passi

- **In collaudo**: l'aggiornamento fino all'installazione (`5.71-01`), il filmato grande (`5.71-02`), la durata in orizzontale (`5.71-03`), e le tre domande sullo Store.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app e i 12 tester per i 14 giorni del test chiuso; i passi in ordine sono in fondo a `docs/Store.md`.
- **Residuo per lo Store**: la grafica della scheda, secondo la risposta a `d-store-grafica`.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
