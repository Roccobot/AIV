# Feedback AIV

Versione **6.25**: le impostazioni si scrivono in un modo che non blocca più il banco di prova. Il documento è cumulativo con la 6.20, la 6.21, la 6.22, la 6.23 e la 6.24, cioè il velo del Feed disegnato, la cartella sulla schermata Home (`Faro`), le note A e B, le librerie aggiornate, i cursori con Material 3 1.5.0-beta01, i comandi sui file su Android 9 e 10 e Compose 1.13.
Prosegue il giro **6.20**.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 6.25 è pubblicata: [v6.25](https://github.com/Roccobot/AIV/releases/tag/v6.25), con i due APK
[AIV-6.25.apk](https://github.com/Roccobot/AIV/releases/download/v6.25/AIV-6.25.apk) (AIV GitHub) e [AIV-Play-6.25.apk](https://github.com/Roccobot/AIV/releases/download/v6.25/AIV-Play-6.25.apk) (AIV Play), più [AIV-Play-6.25.aab](https://github.com/Roccobot/AIV/releases/download/v6.25/AIV-Play-6.25.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `e655cbc`, release dal commit `04e2a40` (SlimVer 6.25 / versionCode 385; APK GitHub 11.263.325 byte, digest `0f2ef973`; APK Play 11.136.481 byte, digest `6e37092f`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **6.25**: cumulativo con la 6.20, la 6.21, la 6.22, la 6.23 e la 6.24, quindi le loro otto prove e le tre etichette restano, con quello che hai già scritto in pagina e in `Altro`, e si aggiunge una prova sul salvataggio delle impostazioni. Giro **6.10**: tutte e tredici le prove del cumulativo 5.92 + 6.02 + 6.03 + 6.10 approvate; le etichette confermate sono registrate.

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

Le verifiche automatiche della 6.25 sono superate: banco di prova completo di AIV GitHub (771 prove, ripetuto otto volte senza blocchi), prove di AIV Play, traduzioni (28 lingue, 0 problemi), compilazione delle due varianti e del pacchetto per Google Play. Sulle macchine di GitHub il banco intero dura adesso circa 3 minuti invece di 6.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 6.20-01 | Non provato | | Attendere il collaudo. |
| 6.20-02 | Non provato | | Attendere il collaudo. |
| 6.20-03 | Non provato | | Attendere il collaudo. |
| 6.20-04 | Non provato | | Attendere il collaudo. |
| 6.21-01 | Non provato | | Attendere il collaudo. |
| 6.22-01 | Non provato | | Attendere il collaudo. |
| 6.23-01 | Non provato | | Attendere il collaudo. |
| 6.24-01 | Non provato | | Attendere il collaudo. |
| 6.25-01 | Non provato | | Attendere il collaudo. |

## 1. Il velo del Feed disegna l'ordine

Vale per AIV GitHub e per AIV Play. In `Impostazioni` usa `Ripristina gli avvisi`, poi apri il Feed. Il velo copre del tutto le immagini: sul fondo della pagina c'è una griglia di riquadri gialli su tre colonne, scurita dal velo nero, e due riquadri restano gialli pieni sopra il velo, quello in alto a sinistra con `Più recenti` e quello più in basso a destra con `Meno recenti`. Fra i due c'è in bianco il testo di `Disposizione in vista feed`. Tocca un punto qualunque: il velo si chiude, e non torna. Riprova col telefono in orizzontale.

Letture mie: è il tuo disegno, sulla voce `6.02-04` del giro scorso. Lo schizzo ha sempre tre colonne, anche se il tuo Feed ne mostra di più o di meno, perché disegna l'ordine e non la griglia; i riquadri si fermano a 160 dp, quindi su un tablet restano più piccoli e le righe sono di più. 'Il più in basso fra quelli visibili' l'ho letto come l'ultimo a destra dell'ultima riga intera: la riga tagliata dal bordo resta disegnata sotto, scurita. Con una riga sola, col telefono in orizzontale, il testo va sotto la riga. Il catalogo dei veli ha la schermata nuova. Il paragrafo è il tuo, riscritto nel giro scorso, ed è fra le etichette qui sotto con le due parole.

## 2. Aggiungi cartella resta fermo in fondo

Vale per AIV GitHub e per AIV Play. In `Impostazioni` → `File e cartelle` → `Cartelle incluse/escluse`, scegli `Modalità incluse`. Il paragrafo in testa è nel corpo piccolo, e `Aggiungi cartella` è in fondo, fermo su una fascia che non scorre. Con molte cartelle nell'elenco, scorri: scorrono il paragrafo, i gettoni e l'elenco, il tasto resta dov'è. Toccalo: si apre la scelta della cartella, come prima.

Letture mie: è la tua nota A. La fascia ha il fondo della pagina, che nel tema chiaro è il bianco che chiedevi e nel tema scuro è scuro. È la stessa forma di `Cartelle preferite`.

## 3. I due gettoni dei pixel, larghi uguali

Vale per AIV GitHub e per AIV Play. In `Impostazioni` → `Visualizzatore` → `Adattamento e zoom`, sotto `Rendering dei pixel` i due gettoni sono uno sopra l'altro, larghi uguali e a tutta larghezza, con la scritta centrata. Toccali a turno: la scelta passa dall'uno all'altro come prima.

Letture mie: è la tua nota B. Il cambio vale solo per questa scelta, le cui due frasi sono lunghe: gli altri gettoni delle impostazioni restano in riga, larghi quanto le loro parole.

## 4. Una cartella sulla schermata Home

Vale solo per AIV GitHub. Nella vista `Cartelle di sistema`, tieni premuta una cartella: nella finestra c'è `Aggiungi alla schermata Home`. Toccalo: il launcher chiede dove metterla, o la mette da sé. L'icona ha il colore della cartella, se gliel'hai dato, oppure il verde di AIV, con sopra la cartella di AIV in trasparenza e sotto il nome della cartella. Toccala: AIV si apre su quella cartella nella vista `Cartelle di sistema`. Chiudi con Indietro e riapri AIV dalla sua icona: torni alla tua vista di prima. Prova una cartella chiara (giallo, verde chiaro) e una scura.

Letture mie: è la tua `Faro`, con la tua specifica. Il glifo è il tuo `AIVfolder.svg` così com'è, al 45% del lato dell'icona; il sole e la montagna restano vuoti, perché nel tuo disegno lo sono. È bianco all'85%, e diventa nero al 55% sui colori su cui il bianco non arriva al contrasto minimo delle norme per la grafica (3 a 1): succede sui colori più chiari e sul verde di AIV. Il colore è quello del momento in cui aggiungi l'icona: se lo cambi dopo, l'icona resta com'era, perché un'icona fissata la ridisegna solo il launcher. La cartella si apre nella vista `Cartelle di sistema` per quella volta sola, come il `Feed` dell'icona: la vista salvata non cambia. In AIV Play la vista `Cartelle di sistema` non c'è, quindi non c'è nemmeno questa voce. Se il tuo launcher non accetta le icone fissate, la voce non compare.

## 5. Le librerie aggiornate

Vale per AIV GitHub e per AIV Play. La 6.21 non cambia funzioni: aggiorna le librerie su cui AIV è costruita, quindi si confronta con la 6.20 nei punti in cui lavorano. Scorri veloce una cartella con molte foto e video: le miniature arrivano come prima. Apri un video nel visualizzatore: parte, si ferma, e la barra lo fa scorrere. Ingrandisci un'immagine con due dita. Apri il menu Start, poi l'editor completo, e chiudi tutto col gesto Indietro. Infine lascia AIV in sottofondo per qualche minuto e riaprila: torni dov'eri.

Letture mie: le librerie sono quelle che disegnano l'interfaccia (Compose), caricano le miniature (Coil), riproducono i video (Media3) e seguono le schermate, il loro stato e il gesto Indietro (Lifecycle e Activity), più gli strumenti che compilano l'app. Material 3, che disegna cursori e finestre, resta dov'era: la sua versione nuova è nella 6.22, con la prova qui sotto.

## 6. I cursori con la versione nuova di Material 3

Vale per AIV GitHub e per AIV Play. La 6.22 non cambia funzioni né aspetto: porta Material 3, la libreria che disegna cursori e finestre, alla prima beta della 1.5.0. Apri un video e lascialo andare: la barra avanza col video e arriva in fondo quando il video finisce. Trascinala: il video salta lì, e la barra resta dove l'hai lasciata. Nelle opzioni della vista `Lista` di una cartella, `Dimensione del testo` si ferma sui suoi tre scatti. In `Impostazioni` → `Visualizzatore` → `Adattamento e zoom`, il numero di `Zoom massimo` segue il dito e resta al rilascio. Nell'editor completo, nel Disegno, `Spessore` e `Opacità` cambiano il tratto mentre trascini. I cursori hanno l'aspetto di prima, in chiaro e in scuro.

Letture mie: la beta01 dichiara superata la forma con cui AIV creava i suoi 16 cursori, e la sostituzione che suggerisce non fa la stessa cosa: ricorda il valore solo all'inizio, quindi la barra di un video resterebbe ferma mentre il video va, e ferma a zero, perché la durata arriva dopo il primo fotogramma. Per questo i 16 cursori passano da un componente solo di AIV, che fa esattamente quello che faceva la forma vecchia, letta nel codice della beta01. La prova nuova lo controlla, e con la sostituzione suggerita fallisce. Tre cursori disegnati con la versione di prima e con questa danno 0 pixel diversi, in chiaro e in scuro; i valori che AIV copia da Material (le misure dei menu, il colore del FAB, le opacità dei gettoni spenti, il rientro dei pulsanti di testo) sono uguali nella beta01.

## 7. I comandi sui file su Android 9 e 10

Vale solo per AIV GitHub, su un apparecchio con Android 9 o 10. Installa la 6.23 sopra la versione che c'è: la vista delle cartelle mostra di nuovo l'invito a concedere l'accesso. Toccalo e conferma; se la lettura era già concessa, Android non chiede niente e le cartelle tornano subito. In una cartella di prova, con immagini di cui hai una copia, prova uno alla volta: rinomina un'immagine, mettila nel cestino e ripristinala, copiala e spostala in un'altra cartella, eliminala. Ogni comando riesce, e il cambiamento si vede anche nella galleria di sistema. Sui tuoi apparecchi con Android 14 e 16 la 6.23 non cambia niente: una rinomina funziona come prima.

Letture mie: è la tua `Veterani`, scelta B1. Fino alla 6.22 AIV GitHub chiedeva sotto Android 11 la sola lettura: le cartelle si vedevano, ma i comandi che scrivono sui file fallivano, su Android 9 perché mancava il permesso di scrittura, su Android 10 perché il sistema blocca la scrittura sui file delle altre app. Adesso la richiesta chiede lettura e scrittura insieme, e su Android 10 AIV chiede l'archivio classico, che da Android 11 in su il sistema ignora. AIV Play non cambia. La prova del banco controlla i permessi, la richiesta e il manifest; la scrittura su un archivio vero la misura questa prova. Il punto da guardare è Android 10 dopo l'aggiornamento dalla 6.22: non ho trovato scritto se il sistema accende l'archivio classico anche per un'app aggiornata o solo per una installata da zero, quindi se lì i comandi falliscono ancora, è da segnalare qui.

## 8. Compose, la versione di prova

Vale per AIV GitHub e per AIV Play. La 6.24 non cambia funzioni: porta Compose, la libreria che disegna tutta l'interfaccia di AIV, dalla 1.12 alla prima beta della 1.13. Fai un giro normale e guarda se qualcosa si comporta in modo diverso dalla 6.23: apri una cartella e scorri veloce, apri un'immagine e ingrandiscila con due dita, apri il menu Start e un paio di finestre (`Info`, `Rinomina`), scrivi un nome in `Rinomina`, guarda un video. Nell'editor completo apri un'immagine, sposta un cursore e usa `Fluidifica`: l'immagine e la deformazione passano da una funzione di disegno che nella 1.13 è cambiata. Se usi l'intestazione con `Gradiente`, guardala sopra una copertina chiara: il gradiente resta liscio, senza bande.

Letture mie: hai chiesto *l'ultimo compose*. La versione definitiva più recente, la 1.12.1, è in AIV dalla 6.21, quindi l'ho letto come l'ultima pubblicata in assoluto, cioè la 1.13.0-beta01. È una beta, come Material 3 dalla 6.22: le funzioni sono fissate, ma può avere difetti. Nel codice di AIV sono cambiate due cose: la funzione con cui l'editor completo e il gradiente passavano ad Android il loro pennello non c'è più, e adesso ne usano un'altra che restituisce lo stesso oggetto; e AIV si compila contro Android 17.1 invece che 17.0, cosa che non cambia il comportamento sul telefono. Le prove del banco usano adesso tutte la regola nuova di Compose, e per te non cambia niente. Il banco su Android 17 invece non è entrato: il perché è nei Prossimi passi.

## 9. Le impostazioni si salvano come prima

Vale per AIV GitHub e per AIV Play. La 6.25 cambia solo il modo in cui AIV scrive le impostazioni. Cambia qualche scelta in punti diversi: `Tema dell'app` in `Impostazioni`, il numero di colonne dalle opzioni della vista, la vista stessa (per esempio `Lista` al posto di `Griglia`), e chiudi un velo d'aiuto. Poi chiudi AIV dalla schermata delle app recenti e riaprila: ritrovi tutte le scelte come le hai lasciate, e il velo chiuso non torna. Se usi `Esporta e importa`, prova anche quello: l'importazione di un file di impostazioni rimette le sue scelte.

Letture mie: fino alla 6.24 ogni scrittura delle impostazioni faceva il suo lavoro nel posto da cui veniva chiesta. Sul telefono non si vedeva niente, ma nel banco di prova una scrittura rimasta a metà fermava tutte quelle dopo, e una corsa su GitHub è rimasta ferma quattro ore. Adesso le quindici scritture passano da un punto solo, che le esegue sui thread riservati alla lettura e alla scrittura dei file. Sul telefono il risultato è lo stesso di prima, quindi questa prova cerca solo che niente si sia perso per strada.

## Etichette testuali

### e-feed_order · La voce in Impostazioni → File e cartelle, e la frase del velo del Feed
<!-- chiavi: feed_order_desc -->
Titolo: Disposizione in vista feed

Spiegazione: La modalità feed mostra solo foto e video, non organizzati in cartelle, con i più recenti in alto. Puoi scegliere se ordinare per data di scatto (e ripiegare sulla data di modifica), o se considerare solo le date di ultima modifica.

### e-hint_feed_newer · Le due parole nel velo del Feed
<!-- chiavi: hint_feed_older -->
In alto a sinistra: Più recenti

In basso a destra: Meno recenti

### e-folder_pin_home · La voce nella pressione lunga su una cartella
Aggiungi alla schermata Home

### e-folder_create_fail · L'avviso quando una cartella nuova non si può creare
Non è stato possibile creare la cartella

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il banco sistemato, Android 17 rimandato | 6.25-01 | Scelte del 2026-10-16 | Il tuo A1 sull'ordine di Setaccio (*la 6.25 col banco sistemato e Android 17*), poi B1: il banco resta su Android 16 finché il simulatore della 17 non è corretto. | Fatto nella 6.25 (`6.25-01`); Android 17 rimandato. |
| Compose 1.13 e le prove sulla regola nuova | 6.24-01 | Richiesta del 2026-10-15 | *l'ultimo compose, le prove riscritte e `Diciassette` fatto*; poi la tua A1: la 6.24 esce senza Android 17. | Fatto nella 6.24 (`6.24-01`); Android 17 nella 6.25. |
| I comandi sui file su Android 9 e 10 | 6.23-01 | Scelta del 2026-10-15 | La tua B1 sulla voce `Veterani`: *correggo*. | Fatto nella 6.23 (`6.23-01`). |
| I cursori con Material 3 1.5.0-beta01 | 6.22-01 | Richiesta del 2026-10-15 | *passa alla nuova versione di Material e rilascia la 6.22*, dopo la misura dei cursori (0 pixel diversi). | Fatto nella 6.22 (`6.22-01`). |
| Le librerie aggiornate | 6.21-01 | Richiesta del 2026-10-15 | *dopo il rilascio fai l'aggiornamento delle dipendenze, in una versione a sé*. | Fatto nella 6.21 (`6.21-01`). |
| Velo del Feed, `Faro`, note A e B | 6.20-01 | Giro della 6.10 | Il velo del Feed come tuo disegno (voce `6.02-04`), `Disposizione in vista feed` riscritta, `Faro` col via (*solo dalla vista cartelle di sistema*), il tasto `Aggiungi cartella` fermo in fondo (A), i gettoni dei pixel larghi uguali (B). | Fatto nella 6.20 (`6.20-01`...`6.20-04`). |

## Prossimi passi

- **In collaudo, giro cumulativo 6.20 + 6.21 + 6.22 + 6.23 + 6.24 + 6.25**: il velo del Feed (`6.20-01`), `Aggiungi cartella` fermo in fondo (`6.20-02`), i gettoni dei pixel (`6.20-03`), la cartella sulla schermata Home (`6.20-04`), le librerie aggiornate (`6.21-01`), i cursori con Material 3 1.5.0-beta01 (`6.22-01`), i comandi sui file su Android 9 e 10 (`6.23-01`), Compose 1.13 (`6.24-01`), il salvataggio delle impostazioni (`6.25-01`) e le tre etichette.
- **Rimandato, il banco su Android 17** (`Diciassette`, la tua B1): aggirati l'onda del tocco e la prova della luminosità, sulle macchine di GitHub sono cadute altre tre prove, tutte perché il simulatore della 17, a caso, non disegna il contenuto aggiornato. È un difetto del simulatore, non dell'app, e si riprova con una versione nuova di Robolectric, lo strumento che lo fornisce.
- **Le versioni dopo, dal rapporto di Setaccio** (il tuo A1 sull'ordine): la 6.26 sui dati, la 6.27 sui difetti visibili, la 6.28 sulle prestazioni misurate prima, la 6.29 su DF, sito e workflow, la 6.30 sulla pulizia. Le scelte che spettano a te te le chiedo in chat prima della versione che le riguarda.
- **Fermi fino al tuo via**: le funzioni sui file di AIV Play rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella); `Salva` sull'originale non serve, perché AIV Play salva già una copia `-aivEdit` accanto all'originale.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app, i 12 tester per i 14 giorni del test chiuso e le schermate del telefono per la scheda (risposta G1); icona e immagine in evidenza sono pronte in `docs/store/`, e i passi in ordine sono in fondo a `docs/Store.md`.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
