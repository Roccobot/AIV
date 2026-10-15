# Feedback AIV

Versione **6.20**: le richieste del giro della 6.10, cioè il velo del Feed disegnato, la cartella sulla schermata Home (`Faro`) e le note A e B.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 6.20 è pubblicata: [v6.20](https://github.com/Roccobot/AIV/releases/tag/v6.20), con i due APK
[AIV-6.20.apk](https://github.com/Roccobot/AIV/releases/download/v6.20/AIV-6.20.apk) (AIV GitHub) e [AIV-Play-6.20.apk](https://github.com/Roccobot/AIV/releases/download/v6.20/AIV-Play-6.20.apk) (AIV Play), più [AIV-Play-6.20.aab](https://github.com/Roccobot/AIV/releases/download/v6.20/AIV-Play-6.20.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `184f0ed`, release dal commit `184f0ed` (SlimVer 6.20 / versionCode 380; APK GitHub 11.196.677 byte, digest `218b6804`; APK Play 11.069.885 byte, digest `869ef7fa`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **6.20**: quattro prove, una per ogni tua richiesta del giro scorso, e tre etichette. Giro **6.10**: tutte e tredici le prove del cumulativo 5.92 + 6.02 + 6.03 + 6.10 approvate; le etichette confermate sono registrate.

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

Le verifiche automatiche della 6.20 sono superate: banco di prova completo di AIV GitHub (763 prove), prove di AIV Play (43) e traduzioni (28 lingue, 0 problemi). Prove nuove: `SchizzoDelFeedTest` misura dove cadono i due riquadri pieni del velo (telefono, tablet, una riga sola); `FasciaEGettoniTest` il tasto `Aggiungi cartella` in vista con quaranta cartelle e i due gettoni dei pixel larghi uguali; in `ScorciatoieTest` la cartella sulla Home che si apre nelle cartelle di sistema per una volta e il contrasto del glifo su ogni colore; in `IconaScorciatoiaTest` i pixel dell'icona della cartella. Rimesso ciascuno degli otto difetti, i suoi casi falliscono. Il disegno della cartella passa i due controlli dei disegni. Le dipendenze nuove non sono entrate nella 6.20: vanno nella 6.21, in una versione a sé, come hai chiesto.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 6.20-01 | Non provato | | Attendere il collaudo. |
| 6.20-02 | Non provato | | Attendere il collaudo. |
| 6.20-03 | Non provato | | Attendere il collaudo. |
| 6.20-04 | Non provato | | Attendere il collaudo. |

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

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Velo del Feed, `Faro`, note A e B | 6.20-01 | Giro della 6.10 | Il velo del Feed come tuo disegno (voce `6.02-04`), `Disposizione in vista feed` riscritta, `Faro` col via (*solo dalla vista cartelle di sistema*), il tasto `Aggiungi cartella` fermo in fondo (A), i gettoni dei pixel larghi uguali (B). | Fatto nella 6.20 (`6.20-01`...`6.20-04`). |
| Le azioni dell'icona nel launcher | 6.10-01 | Scelte del 2026-10-15 | *Voglio aggiungere delle azioni utili alla pressione lunga dell'icona sulla homescreen*; poi A1 (*Feed solo se è una vista temporanea*), A2, A4 e B1; C in coda. | Fatte nella 6.10 e approvate; la C è la `Faro` della 6.20. |
| Il Feed: nascoste, selezione, velo, icona | 6.03-01 | Richiesta del 2026-10-15 | `Mostra nascoste` anche nel Feed, la selezione multipla, il tuo testo del velo, una proposta d'icona; poi *A5 per l'icona*. | Fatto nella 6.03 e approvato; il velo è rifatto nella 6.20. |

## Prossimi passi

- **In collaudo, giro 6.20**: il velo del Feed (`6.20-01`), `Aggiungi cartella` fermo in fondo (`6.20-02`), i gettoni dei pixel (`6.20-03`), la cartella sulla schermata Home (`6.20-04`) e le tre etichette.
- **Subito dopo questa release, senza chiedere** (tua istruzione): la 6.21, l'aggiornamento delle dipendenze in una versione a sé: Gradle 9.8.1, AGP 9.4.1 con Coil 3.6.3, Kotlin 2.4.21, Core, Lifecycle, Activity, Compose 1.12, Media3 1.11.1 e Robolectric 4.17. Material 3 beta01, col passaggio dei 16 cursori, viene in una versione successiva con le sue prove.
- **Fermi fino al tuo via**: le funzioni sui file di AIV Play rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella); `Salva` sull'originale non serve, perché AIV Play salva già una copia `-aivEdit` accanto all'originale.
- **In attesa del tuo via, dalle verifiche**: i permessi di scrittura su Android 9 e 10, la correzione di Chromium in `feedback-check.py`, le 50 classi di prova da aggiornare, la prova dell'altezza del pannello dell'editor.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app, i 12 tester per i 14 giorni del test chiuso e le schermate del telefono per la scheda (risposta G1); icona e immagine in evidenza sono pronte in `docs/store/`, e i passi in ordine sono in fondo a `docs/Store.md`.
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
