# Feedback AIV

Versione **6.03**: nel Feed le cartelle nascoste con `Mostra nascoste`, la selezione multipla, il tuo testo del velo e l'icona A5; il tondo del menu Start si comporta come quello della pillola a scomparsa. Il documento è cumulativo con la 5.92 e la 6.02.
Prosegue il giro **5.92**.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 6.03 è pubblicata: [v6.03](https://github.com/Roccobot/AIV/releases/tag/v6.03), con i due APK
[AIV-6.03.apk](https://github.com/Roccobot/AIV/releases/download/v6.03/AIV-6.03.apk) (AIV GitHub) e [AIV-Play-6.03.apk](https://github.com/Roccobot/AIV/releases/download/v6.03/AIV-Play-6.03.apk) (AIV Play), più [AIV-Play-6.03.aab](https://github.com/Roccobot/AIV/releases/download/v6.03/AIV-Play-6.03.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `8acf0ba`, release dal commit `8acf0ba` (SlimVer 6.03 / versionCode 378; APK GitHub 11.187.437 byte, digest `3eafdf63`; APK Play 11.077.029 byte, digest `10c6b178`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE). Le sei proposte per l'icona del Feed, fra cui hai scelto la A5: [Icona del Feed](https://claude.ai/artifact/SW5RJssonxpCJXH1WfzBjv).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **6.03**: cumulativo con la 5.92 e la 6.02, quindi le loro prove restano, con quello che hai già scritto; si aggiungono cinque prove (le ultime due sono le tue aggiunte: l'icona A5 e il tondo), e l'etichetta del velo cambia col tuo testo. Giro **6.02**: non ancora inviato. Giro **5.91**: il titolino `Disposizione delle cartelle` è approvato e registrato.

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

Le verifiche automatiche della 6.03 sono superate: banco di prova completo di AIV GitHub e prove di AIV Play (788 prove in tutto), controllo delle traduzioni (28 lingue, 0 problemi), compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata. In `FeedTest` quattro casi nuovi o cambiati: le cartelle nascoste con `Mostra nascoste`, la rilettura quando il minuto comincia e finisce, la selezione multipla e il corsivo del velo; in `MenuInferioreTest` il tondo che scorrendo resta un tasto solo. Rimesso ciascuno dei cinque difetti, il suo caso fallisce. L'icona nuova passa i due controlli dei disegni (`icon-check.py` e `icon-round.py`). Prima della release ho letto le note delle 11 dipendenze con una versione nuova. Nessuna è entrata nella 6.03: vanno aggiornate insieme in una versione a sé, perché il nuovo plugin di compilazione (AGP 9.4) rompe la build di release senza la Coil 3.6.3, e Material 3 beta01 cambia il cursore `Slider`, che AIV usa in 16 punti. Quello che conviene: Gradle 9.8.1 corregge tre vulnerabilità, Media3 1.11.1 dei blocchi nella riproduzione dei video, e Compose passa da una beta alla stabile.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.92-01 | Non provato | | Attendere il collaudo. |
| 6.02-01 | Non provato | | Attendere il collaudo. |
| 6.02-02 | Non provato | | Attendere il collaudo. |
| 6.02-03 | Non provato | | Attendere il collaudo. |
| 6.02-04 | Non provato | | Attendere il collaudo. |
| 6.02-05 | Non provato | | Attendere il collaudo. |
| 6.03-01 | Non provato | | Attendere il collaudo. |
| 6.03-02 | Non provato | | Attendere il collaudo. |
| 6.03-03 | Non provato | | Attendere il collaudo. |
| 6.03-04 | Non provato | | Attendere il collaudo. |
| 6.03-05 | Non provato | | Attendere il collaudo. |

## 1. Una vibrazione sola, piena, su ogni pressione lunga

Vale per AIV GitHub e per AIV Play. Prova la pressione lunga in quattro punti e confrontale fra loro: una miniatura in una cartella (inizia la selezione), una cartella nella griglia delle cartelle, `Elimina` del Disegno nell'editor completo (tenuto premuto fa `Elimina tutto`), e `Data` nella finestra di `Rinomina`. Devono vibrare tutte allo stesso modo, una volta sola, con la vibrazione piena del sistema.

Letture mie: è la tua scelta B1. Fino alla 5.91 la maggior parte dei tocchi lunghi vibrava due volte (il colpetto leggero di AIV più quello pieno che Compose, la libreria grafica dell'app, aggiunge da sé), e l'inizio della selezione solo col colpetto leggero: è la differenza che avevi sentito. Adesso quella di Compose è spenta ovunque e resta quella di AIV, piena. L'intensità dipende anche dalle impostazioni di vibrazione di HyperOS. Se ti sembra troppo forte resta la via B3, piena solo all'inizio della selezione. La versione sale di un centesimo: cambia una funzione che esiste già. Dalla 6.03 vale anche per la selezione nel Feed (prova 8).

## 2. Il Feed si apre dal menu Start

Vale per AIV GitHub e per AIV Play. Con il menu Start, apri il menu dalla casa: nella prima riga, nella casella dove c'era il commutatore fra tondo e pillola, c'è `Feed`. Toccalo: il menu si chiude e la casa mostra il Feed. Riapri il menu: adesso la prima riga ha le altre viste (`Griglia`, `Lista` e, in AIV GitHub, `Cartelle`), da cui torni dove eri.

Letture mie: il Feed è anche nel menu del FAB, nelle pillole e nelle opzioni di visualizzazione. Nella vista `Cartelle di sistema`, dentro una cartella, c'è `Crea` e le caselle sarebbero dieci, quindi là il Feed non compare, come prima non compariva il commutatore. Sullo schermo largo il Feed prende tutta la schermata, senza la colonna delle cartelle accanto, come la vista ad albero. Dalla 6.03 il glifo è la tua A5: è la prova 10.

## 3. Il flusso, i mesi e l'apertura

Nel Feed controlla che le foto e i video siano di tutte le cartelle che vedi in griglia, con i più recenti in alto, e che ogni mese abbia la sua intestazione (per esempio `Ottobre 2026`). Una cattura dello schermo deve stare al giorno in cui l'hai fatta, non in fondo. Tocca una miniatura: si apre nel visualizzatore, e scorrendo passi alle altre del Feed nello stesso ordine; Indietro ti riporta al Feed dove eri.

Letture mie: valgono le cartelle nascoste, incluse ed escluse della griglia (la tua D1), e il cestino resta fuori. Dalla 6.03 `Mostra nascoste` aggiunge al Feed anche le nascoste, per il suo minuto: è la prova 7. Le colonne sono quelle della griglia delle cartelle, una manopola sola per le due griglie della casa: cambiandole nel Feed cambiano anche là. La selezione multipla è arrivata nella 6.03: è la prova 8. Se hai appena scattato una foto, deve comparire in cima al ritorno nella casa.

## 4. Quale data conta

Tieni premuto il pulsante principale nel Feed: le opzioni di visualizzazione mostrano le colonne e, sotto, `Data per l'ordinamento` coi gettoni `Scatto, Modifica` e `Modifica`. Di fabbrica è acceso `Scatto, Modifica`. Passa a `Modifica`: una foto vecchia che hai modificato di recente sale in cima. Poi apri `Impostazioni` → `File e cartelle`: sotto `Disposizione in vista griglia` c'è `Disposizione in vista feed`, col tuo testo e gli stessi due gettoni, e la scelta è quella che hai appena fatto.

Letture mie: `Scatto, Modifica` usa la data di scatto, e per un file che non ce l'ha (una cattura, un'immagine scaricata) la data di modifica. `Modifica` usa la data in cui il file è stato scritto l'ultima volta, che per un file mai toccato coincide con quella di creazione: Android non conserva la data di creazione a parte. La sezione `Cartelle` delle impostazioni ora si chiama `File e cartelle`, e così la parte corrispondente di `Esporta e importa`, che fino alla 6.01 era `Cartelle e percorsi`; la data del Feed viaggia con lei.

## 5. Il velo del Feed

La prima volta che entri nel Feed, con delle immagini da mostrare, compare al centro il velo. Toccalo: si chiude, e non torna. Per rivederlo, in `Impostazioni` usa `Ripristina gli avvisi`.

Letture mie: dalla 6.03 il testo è il tuo, e si prova nella prova 9; questa resta per il comportamento del velo. Non indica il pulsante, perché il gesto della pressione lunga lo insegna già il velo delle colonne nella griglia.

## 6. Il tondo e la pillola nelle impostazioni, e le pillole a otto voci

Col menu Start scelto, in `Impostazioni` → `Pulsanti e indicatori` → `Elemento interattivo principale`, sotto i gettoni c'è una seconda riga: `Tondo` e `Pillola`, coi tuoi due disegni del commutatore. Scegli `Pillola`: a riposo il menu Start è la pillola verticale; torna a `Tondo`. Poi passa alla pillola estesa e, con almeno una cartella nascosta, guarda la casa: le voci sono otto, i tasti un poco più stretti.

Letture mie: il tondo resta il riposo di fabbrica. Le otto voci sono la tua scelta A1: senza cartelle nascoste le voci restano sette, e `Cerca` è al centro come prima; con otto è spostata di mezza casella.

## 7. Mostra nascoste porta nel Feed le cartelle nascoste

Vale per AIV GitHub e per AIV Play. Con almeno una cartella nascosta che contiene delle foto, apri il Feed: quelle foto non ci sono. Tocca `Mostra nascoste` (dal menu Start o dal menu del FAB): il Feed si aggiorna e le foto delle cartelle nascoste compaiono al loro posto nel tempo. Allo scadere del minuto, o toccando `Nascondi cartelle`, spariscono di nuovo.

Letture mie: è la tua richiesta, e vale come in griglia: per la durata del minuto e solo nella modalità con le cartelle escluse; in quella con le cartelle incluse il tasto non c'è. Il Feed si rilegge quando il minuto comincia e quando finisce, quindi per un istante vedi ancora il flusso di prima.

## 8. La selezione multipla nel Feed

Vale per AIV GitHub e per AIV Play. Nel Feed tieni premuta una miniatura: si seleziona, vibra, e al posto del pulsante principale compare la scheda delle azioni. Tocca altre miniature: si aggiungono, senza aprirsi. Tieni premuta una miniatura e trascina il dito verso il basso, anche oltre l'intestazione di un mese: si seleziona tutto l'intervallo, e in fondo allo schermo la griglia scorre. Prova poi un'azione (per esempio `Condividi` o `Rinomina`): alla fine la selezione si scioglie e il Feed si aggiorna. Indietro scioglie la selezione senza uscire dal Feed.

Letture mie: è la selezione della griglia di una cartella, con le stesse azioni nello stesso ordine; in AIV Play mancano quelle che AIV Play non ha nemmeno in griglia (copia e sposta). `Tutti` e `Inverti` lavorano su tutto il Feed, non sul mese. `Lista` copia i nomi senza il percorso della cartella in testa, perché le immagini vengono da cartelle diverse. Passando a un'altra vista la selezione si scioglie.

## 9. Il tuo testo nel velo del Feed

Vale per AIV GitHub e per AIV Play. Ripristina gli avvisi (`Impostazioni` → `Ripristina gli avvisi`) e apri il Feed: il velo dice il tuo testo, con *feed* in corsivo: *La vista feed mostra i file provenienti da tutti i percorsi autorizzati, senza la suddivisione in cartelle, con gli elementi più recenti in alto. Per scegliere se nella disposizione ha priorità la data di scatto o quella di modifica, tieni premuto il pulsante principale.*

Letture mie: il corsivo c'è anche nelle altre 27 lingue, sul nome della vista nella lingua. Il catalogo dei veli ha la schermata nuova. Il testo è anche fra le etichette qui sotto.

## 10. L'icona del Feed è la pila di immagini

Vale per AIV GitHub e per AIV Play. Guarda il Feed nel menu Start, nel menu del FAB, nelle pillole e nelle opzioni di visualizzazione: l'icona è la pila di immagini, la A5, un'immagine con la montagna davanti e due strisce dietro. A 24 dp deve leggersi bene, sia nel tema chiaro sia nello scuro.

Letture mie: è la tua scelta A5. L'ho ridisegnata sulla griglia delle icone dell'app, con gli angoli arrotondati come vuole la regola del progetto; il disegno è quello della pagina delle proposte. Prende il posto dell'icona di Material dappertutto.

## 11. Il tondo del menu Start scorrendo resta un tondo

Vale per AIV GitHub e per AIV Play. Col menu Start e il riposo `Tondo` (quello di fabbrica), apri una cartella e scorri la griglia: il tondo resta un tasto solo, il marchio diventa la freccia, come nella pillola a scomparsa, e toccandolo salti in cima o in fondo, dove punta la freccia. Smetti di scorrere: torna il marchio, e il tocco apre il menu Start. Prova anche la pressione lunga, che fa quello che faceva il FAB.

Letture mie: è la tua richiesta, *identico a quello della pillola a scomparsa*. Fino alla 6.02 scorrendo il tondo si allungava in su nella pillola verticale coi due salti, `in cima` e `in fondo`; adesso il salto è uno, quello della freccia, come nella pillola. Col riposo `Pillola` non cambia niente: la pillola verticale tiene i suoi due tasti.

## Etichette testuali

### e-view_feed · Il nome della vista, e il Feed vuoto
<!-- chiavi: feed_none -->
Nome: Feed

Senza immagini da mostrare: Nessuna foto o video da mostrare.

### e-feed_date · Il titolino e i gettoni delle opzioni di visualizzazione
<!-- chiavi: feed_date_taken feed_date_modified -->
Titolino: Data per l'ordinamento

Gettoni: Scatto, Modifica · Modifica

### e-feed_order · La voce in Impostazioni → File e cartelle
<!-- chiavi: feed_order_desc -->
Titolo: Disposizione in vista feed

Spiegazione: La modalità feed mostra solo foto e video, non organizzati in cartelle, con i più recenti in alto. Dato che 'recente' può avere accezioni diverse, puoi scegliere se usare la data di scatto (e ripiegare sulla data di modifica), o se considerare solo le date di creazione/modifica.

### e-hint_feed · Il velo del Feed
La vista *feed* mostra i file provenienti da tutti i percorsi autorizzati, senza la suddivisione in cartelle, con gli elementi più recenti in alto. Per scegliere se nella disposizione ha priorità la data di scatto o quella di modifica, tieni premuto il pulsante principale.

### e-settings_group_browse · La sezione delle impostazioni e la parte di 'Esporta e importa'
File e cartelle

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il Feed: nascoste, selezione, velo, icona | 6.03-01 | Richiesta del 2026-10-15 | `Mostra nascoste` anche nel Feed, la selezione multipla, il tuo testo del velo, una proposta d'icona; poi *A5 per l'icona*. | Fatto nella 6.03 (`6.03-01`...`6.03-04`). |
| Il tondo del menu Start | 6.03-05 | Richiesta del 2026-10-15 | *Rendi il comportamento del tondo identico a quello della pillola a scomparsa (non diventa pillola verticale al movimento).* | Fatto nella 6.03 (`6.03-05`). |
| Il Feed | 6.02-01 | Scelte del 2026-10-14 | Accesso dal menu Start, data B1 a scelta, mesi C1, cartelle D1, velo, `File e cartelle`; pillole a otto voci A1. | Fatto nella 6.02 (`6.02-01`...`6.02-05`). |
| La vibrazione della pressione lunga | 5.92-01 | Scelta B1 | Il tocco che apre la selezione era *leggerissimo, diverso da altre vibrazioni*. | Fatta nella 5.92 (`5.92-01`). |

## Prossimi passi

- **In collaudo, giro cumulativo 5.92 + 6.02 + 6.03**: la vibrazione (`5.92-01`), il Feed dal menu Start (`6.02-01`), il flusso e i mesi (`6.02-02`), la data (`6.02-03`), il velo (`6.02-04`), tondo e pillola nelle impostazioni (`6.02-05`), le nascoste nel Feed (`6.03-01`), la selezione nel Feed (`6.03-02`), il tuo testo del velo (`6.03-03`), l'icona A5 (`6.03-04`), il tondo del menu Start (`6.03-05`) e le cinque etichette.
- **Fermi fino al tuo via**: le funzioni sui file di AIV Play rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella); `Salva` sull'originale non serve, perché AIV Play salva già una copia `-aivEdit` accanto all'originale.
- **Dopo questo giro, senza chiedere**: l'aggiornamento delle dipendenze lette per la 6.03, in una versione a sé: Gradle 9.8.1, AGP 9.4.1 con Coil 3.6.3, Kotlin 2.4.21, Core, Lifecycle, Activity, Compose 1.12, Media3 1.11.1 e Robolectric 4.17. Material 3 beta01, col passaggio dei 16 cursori, viene in una versione successiva con le sue prove.
- **In attesa del tuo via, dalle verifiche**: i permessi di scrittura su Android 9 e 10, la correzione di Chromium in `feedback-check.py`, le 50 classi di prova da aggiornare, la prova dell'altezza del pannello dell'editor.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app, i 12 tester per i 14 giorni del test chiuso e le schermate del telefono per la scheda (risposta G1); icona e immagine in evidenza sono pronte in `docs/store/`, e i passi in ordine sono in fondo a `docs/Store.md`.
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
