# Feedback AIV

Versione **5.80**: la disposizione delle cartelle in griglia, le cartelle preferite, la scelta dell'azione al tocco lungo su una cartella, 'Esporta e importa' in sei parti e i file DNG. Il documento è cumulativo con la 5.71.
Prosegue il giro **5.71**.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.80 è pubblicata: [v5.80](https://github.com/Roccobot/AIV/releases/tag/v5.80), con i due APK
[AIV-5.80.apk](https://github.com/Roccobot/AIV/releases/download/v5.80/AIV-5.80.apk) (AIV GitHub) e [AIV-Play-5.80.apk](https://github.com/Roccobot/AIV/releases/download/v5.80/AIV-Play-5.80.apk) (AIV Play), più [AIV-Play-5.80.aab](https://github.com/Roccobot/AIV/releases/download/v5.80/AIV-Play-5.80.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `84f3b94`, release dal commit `84f3b94` (SlimVer 5.80 / versionCode 373; APK GitHub 11.120.013 byte, digest `101df747`; APK Play 10.993.221 byte, digest `080f3a49`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.80**: cumulativo con la 5.71, quindi le tre prove e le tre domande della 5.71 restano, con quello che hai già scritto, e si aggiungono cinque prove e cinque etichette della 5.80. Giro **5.70**: l'aggiornamento automatico si fermava dopo lo scaricamento, e le dieci etichette sono confermate.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale, coi conteggi in cima. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, salvo in un documento cumulativo, dove restano quelli del giro che prosegue, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 5.80 sono superate: banco di prova completo di AIV GitHub (727 prove), prove di AIV Play (43), controllo delle traduzioni, compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.71-01 | Non provato | | Attendere il collaudo. |
| 5.71-02 | Non provato | | Attendere il collaudo. |
| 5.71-03 | Non provato | | Attendere il collaudo. |
| 5.80-01 | Non provato | | Attendere il collaudo. |
| 5.80-02 | Non provato | | Attendere il collaudo. |
| 5.80-03 | Non provato | | Attendere il collaudo. |
| 5.80-04 | Non provato | | Attendere il collaudo. |
| 5.80-05 | Non provato | | Attendere il collaudo. |

## 1. AIV GitHub: l'aggiornamento fino all'installazione

Parti dalla 5.71 di AIV GitHub: se hai ancora la 5.70, installa prima l'[APK della 5.71](https://github.com/Roccobot/AIV/releases/download/v5.71/AIV-5.71.apk) a mano, perché è la 5.71 che contiene la correzione. In `Impostazioni`, sezione `Avanzate`, tocca `Controlla adesso`: compare la finestra `È disponibile AIV 5.80`. Tocca `Aggiorna`: la finestra si chiude, compare `Scarico l'aggiornamento...`, e alla fine si apre l'installazione di Android. Conferma, e riapri AIV: `Controlla adesso` deve rispondere `AIV è aggiornata`.

Letture mie: nella 5.70 la finestra si chiudeva al tocco e si portava via il lavoro appena avviato, quindi lo scaricamento finiva e l'installazione non partiva, senza nessun avviso. Adesso il lavoro non dipende più dalla finestra. Se Android chiede di consentire ad AIV l'installazione di app, attiva l'interruttore, torna indietro e tocca di nuovo `Aggiorna`: la finestra in quel caso resta aperta apposta. Questa prova installa anche la 5.80, che serve alle prove dalla 4 in poi; il testo della finestra cita la 5.80 perché è l'ultima release, mentre quando ho scritto questa prova era la 5.71.

## 2. Un filmato grande

Apri dalla griglia il file MKV da 250 MB che nella 5.70 faceva chiudere AIV, e lascialo partire. Prova anche a scorrere al filmato prima e a quello dopo. AIV non deve chiudersi.

Letture mie: per ogni pagina il visualizzatore chiede se è un'immagine animata (GIF o WebP), filmati compresi, e fino alla 5.70 per rispondere leggeva tutto il file in memoria: 250 MB in un blocco solo sono più di quanto l'app può tenere. Adesso guarda i primi 64 byte, e di un filmato non legge altro. È la causa che ho trovato nel codice e che la prova sul banco misura; un crash con un'altra causa lo vedresti ancora, e allora mi serve sapere in che momento succede (all'apertura, durante la riproduzione, scorrendo).

## 3. La durata dei video in orizzontale

Metti il telefono in orizzontale e apri la cartella di un video, come nella schermata del giro 5.70. Sulla miniatura la durata resta su una riga sola, accanto al triangolo, anche quando la miniatura è stretta.

Letture mie: quando lo spazio non basta, la durata si rimpicciolisce fino a un corpo minimo di 8 punti, e solo sotto quella misura verrebbe tagliata. In verticale, dove lo spazio c'è, resta della misura di sempre.

## 4. La disposizione delle cartelle in griglia

Vale per AIV GitHub e per AIV Play. Aperta la 5.80 per la prima volta, nella griglia delle cartelle compare la domanda `Come vuoi che siano ordinate le cartelle in vista griglia?`, coi due tasti `Ordine alfabetico` e `Più recenti in alto`, larghi uguali e uno sopra l'altro. Scegli `Più recenti in alto`: in cima va la cartella in cui hai salvato o modificato un file per ultimo. Poi tieni premuto il pulsante principale, che apre le impostazioni delle cartelle: la prima voce è `Disposizione in vista griglia`, col gettone `Più recenti in alto` acceso. Passa ad `Alfabetica` e torna alla griglia, che deve tornare in ordine di nome. La vista a lista resta in ordine alfabetico in tutti e due i casi.

Letture mie: la domanda compare una volta anche a chi aggiorna (scelta `C1`). Se la chiudi senza rispondere torna al prossimo avvio, e intanto la griglia resta alfabetica. 'Più recente' vuol dire la data di modifica del file più nuovo della cartella, la stessa con cui il contenuto della cartella mette in cima gli elementi più recenti. Sul tablet in orizzontale la casa è la colonna delle cartelle, che è una lista: là la domanda non compare, e la voce resta nelle impostazioni.

## 5. Le cartelle preferite

In fondo a `Impostazioni` → `Cartelle` apri `Cartelle preferite`: la pagina è vuota e dice `Nessuna cartella preferita`. Tocca `Aggiungi cartella`, scegli una cartella, poi aggiungine una seconda. In griglia e in lista le due cartelle compaiono in cima, nell'ordine della pagina, e sotto di loro tutte le altre. Torna nella pagina, spostale con le manopole come nella pagina delle info e controlla che l'ordine in griglia segua. La × accanto a una cartella la toglie subito.

Letture mie: in AIV GitHub `Aggiungi cartella` apre l'albero delle cartelle; in AIV Play, che non vede le cartelle del disco, apre la vista a griglia delle cartelle con immagini. Una preferita è la cartella e basta: le sue sottocartelle restano al loro posto. Con `Più recenti in alto` le preferite restano comunque in cima, e l'ordine per data vale per le altre.

## 6. Il tocco lungo su una cartella

In griglia tieni premuta una cartella: compare `Scegli l'azione sulla cartella`, con due tasti grandi uno sopra l'altro, `Nascondi` e `Aggiungi alle preferite`. Tocca `Aggiungi alle preferite`: la finestra si chiude e la cartella va in cima. Tieni premuta di nuovo la stessa cartella: il secondo tasto ora dice `Rimuovi dalle preferite`. Poi tieni premuta un'altra cartella e tocca `Nascondi`: si apre la domanda di sempre per nasconderla. Ripeti in lista e, sul tablet, nella colonna delle cartelle.

Letture mie: la finestra vale in griglia, in lista e nella colonna del tablet (scelta `B1`). Con `Mostra nascoste` acceso, su una cartella nascosta il primo tasto dice `Mostra`, e porta alla domanda per mostrarla. Nella modalità delle cartelle incluse `Nascondi` porta alla domanda per togliere l'autorizzazione.

## 7. 'Esporta e importa' in sei parti

In `Impostazioni` apri `Esporta e importa`. Le caselle sono sei, in quest'ordine: `Aspetto e navigazione`, `Cartelle e percorsi`, `Impostazioni dell'editor`, `Stili dell'editor`, `Avvisi e micro-tutorial`, `Cestino`. `Comandi e indicatori` non c'è più, e `Proteggi con password` è nella fascia fissa in basso con `Importa` ed `Esporta`, quindi resta al suo posto quando le caselle scorrono. Esporta con le caselle di serie. Poi importa un file di impostazioni salvato con una versione precedente: la conferma elenca le parti nuove, e l'importazione va in porto.

Letture mie: `Cartelle e percorsi` comprende colore e copertine delle cartelle, cartelle incluse ed escluse, la disposizione in griglia, le preferite e l'indicatore dell'ultimo media; i comandi sono entrati in `Aspetto e navigazione`. Dentro il file le sezioni sono quelle di prima, per questo i file vecchi si importano ancora (scelta `A1`) e una versione vecchia di AIV legge quelli nuovi. `Stili dell'editor` manca sotto Android 13, come prima.

## 8. I file DNG

Copia sul telefono un file DNG (il formato RAW di molte fotocamere e di diversi telefoni) e aprilo da AIV, dalla griglia e dall'albero delle cartelle. Si vede, con la miniatura e le informazioni. Apri l'editor, fai una modifica qualunque e salva: accanto al DNG compare una copia JPEG col suo nome, e il DNG resta com'era.

Letture mie: il DNG lo decodifica Android, quindi la resa è quella del decodificatore del telefono, e un DNG di una fotocamera che Android non conosce potrebbe non aprirsi; in quel caso mi serve sapere da quale apparecchio viene. Gli altri formati RAW restano esclusi: il telefono non li sa leggere. Il salvataggio crea una copia JPEG perché AIV non sa riscrivere un DNG (scelta `D1`), come già succede con HEIC e AVIF.

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

## Etichette testuali

### e-folder_order · La voce della disposizione, in Impostazioni → Cartelle
<!-- chiavi: folder_order_desc folder_order_alpha folder_order_recent -->
Titolo: Disposizione in vista griglia

Spiegazione: Imposta l'ordine delle cartelle valido per la vista griglia. Il contenuto delle cartelle mostra sempre gli elementi più recenti in cima.

Gettoni: Alfabetica · Più recenti in alto

### e-folder_order_ask · La domanda al primo avvio
<!-- chiavi: folder_order_alpha_long -->
Domanda: Come vuoi che siano ordinate le cartelle in vista griglia? Potrai cambiare la disposizione in qualsiasi momento dalle impostazioni.

Tasti: Ordine alfabetico · Più recenti in alto

### e-favourite_folders · La pagina delle cartelle preferite
<!-- chiavi: favourite_folders_desc favourite_folders_none -->
Titolo: Cartelle preferite

Spiegazione: Compaiono in cima alla griglia e alla lista, in quest'ordine.

Con la pagina vuota: Nessuna cartella preferita

### e-folder_action_title · La finestra del tocco lungo su una cartella
<!-- chiavi: favourite_add favourite_remove -->
Titolo: Scegli l'azione sulla cartella

Secondo tasto: Aggiungi alle preferite · Rimuovi dalle preferite (`Aggiungi alle preferite` è anche il tasto che conferma la cartella scelta in `Aggiungi cartella`)

### e-backup_area_folders · La parte nuova di 'Esporta e importa'
Cartelle e percorsi

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV GitHub: l'aggiornamento da una release alla successiva | 5.70-01 | Non approvato | Scaricato, ma poi non si installa: ripiegato su Obtainium. | Corretto nella 5.71. |
| AIV GitHub: l'aggiornamento automatico | 5.60-01 | Tutto OK | Quattro etichette corrette. | Corrette nella 5.61. |

## Prossimi passi

- **In collaudo, giro cumulativo 5.71 + 5.80**: l'aggiornamento fino all'installazione (`5.71-01`), il filmato grande (`5.71-02`), la durata in orizzontale (`5.71-03`), la disposizione in griglia (`5.80-01`), le preferite (`5.80-02`), il tocco lungo (`5.80-03`), le sei parti di 'Esporta e importa' (`5.80-04`), i DNG (`5.80-05`), le cinque etichette e le tre domande sullo Store.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app e i 12 tester per i 14 giorni del test chiuso; i passi in ordine sono in fondo a `docs/Store.md`.
- **Residuo per lo Store**: la grafica della scheda, secondo la risposta a `d-store-grafica`.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
