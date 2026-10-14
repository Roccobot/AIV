# Feedback AIV

Versione **6.02**: il Feed, tutte le foto e i video in un flusso con le più recenti in alto. Il documento è cumulativo con la 5.92.
Prosegue il giro **5.92**.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
@@RELEASE@@
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **6.02**: cumulativo con la 5.92, quindi la prova della vibrazione resta, con quello che hai già scritto, e si aggiungono cinque prove e cinque etichette del Feed. Giro **5.91**: il titolino `Disposizione delle cartelle` è approvato e registrato.

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

Le verifiche automatiche della 6.02 sono superate: banco di prova completo di AIV GitHub e prove di AIV Play (785 prove in tutto), controllo delle traduzioni (28 lingue, 0 problemi), compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata. La prova nuova `FeedTest` misura l'ordine, il ripiego sulla data di modifica, i mesi, la griglia, il tocco e il velo; tolto il ripiego, due dei suoi casi falliscono.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.92-01 | Non provato | | Attendere il collaudo. |
| 6.02-01 | Non provato | | Attendere il collaudo. |
| 6.02-02 | Non provato | | Attendere il collaudo. |
| 6.02-03 | Non provato | | Attendere il collaudo. |
| 6.02-04 | Non provato | | Attendere il collaudo. |
| 6.02-05 | Non provato | | Attendere il collaudo. |

## 1. Una vibrazione sola, piena, su ogni pressione lunga

Vale per AIV GitHub e per AIV Play. Prova la pressione lunga in quattro punti e confrontale fra loro: una miniatura in una cartella (inizia la selezione), una cartella nella griglia delle cartelle, `Elimina` del Disegno nell'editor completo (tenuto premuto fa `Elimina tutto`), e `Data` nella finestra di `Rinomina`. Devono vibrare tutte allo stesso modo, una volta sola, con la vibrazione piena del sistema.

Letture mie: è la tua scelta B1. Fino alla 5.91 la maggior parte dei tocchi lunghi vibrava due volte (il colpetto leggero di AIV più quello pieno che Compose, la libreria grafica dell'app, aggiunge da sé), e l'inizio della selezione solo col colpetto leggero: è la differenza che avevi sentito. Adesso quella di Compose è spenta ovunque e resta quella di AIV, piena. L'intensità dipende anche dalle impostazioni di vibrazione di HyperOS. Se ti sembra troppo forte resta la via B3, piena solo all'inizio della selezione. La versione sale di un centesimo: cambia una funzione che esiste già.

## 2. Il Feed si apre dal menu Start

Vale per AIV GitHub e per AIV Play. Con il menu Start, apri il menu dalla casa: nella prima riga, nella casella dove c'era il commutatore fra tondo e pillola, c'è `Feed`. Toccalo: il menu si chiude e la casa mostra il Feed. Riapri il menu: adesso la prima riga ha le altre viste (`Griglia`, `Lista` e, in AIV GitHub, `Cartelle`), da cui torni dove eri.

Letture mie: il Feed è anche nel menu del FAB, nelle pillole e nelle opzioni di visualizzazione. Nella vista `Cartelle di sistema`, dentro una cartella, c'è `Crea` e le caselle sarebbero dieci, quindi là il Feed non compare, come prima non compariva il commutatore. Sullo schermo largo il Feed prende tutta la schermata, senza la colonna delle cartelle accanto, come la vista ad albero. Il glifo è quello di Material per un flusso di schede: se ne vuoi uno tuo, lo disegno sul tuo.

## 3. Il flusso, i mesi e l'apertura

Nel Feed controlla che le foto e i video siano di tutte le cartelle che vedi in griglia, con i più recenti in alto, e che ogni mese abbia la sua intestazione (per esempio `Ottobre 2026`). Una cattura dello schermo deve stare al giorno in cui l'hai fatta, non in fondo. Tocca una miniatura: si apre nel visualizzatore, e scorrendo passi alle altre del Feed nello stesso ordine; Indietro ti riporta al Feed dove eri.

Letture mie: valgono le cartelle nascoste, incluse ed escluse della griglia (la tua D1), e il cestino resta fuori. `Mostra nascoste` non aggiunge le nascoste al Feed: il minuto in prestito vale per le cartelle. Le colonne sono quelle della griglia delle cartelle, una manopola sola per le due griglie della casa: cambiandole nel Feed cambiano anche là. Per ora nel Feed non c'è la selezione multipla. Se hai appena scattato una foto, deve comparire in cima al ritorno nella casa.

## 4. Quale data conta

Tieni premuto il pulsante principale nel Feed: le opzioni di visualizzazione mostrano le colonne e, sotto, `Data per l'ordinamento` coi gettoni `Scatto, Modifica` e `Modifica`. Di fabbrica è acceso `Scatto, Modifica`. Passa a `Modifica`: una foto vecchia che hai modificato di recente sale in cima. Poi apri `Impostazioni` → `File e cartelle`: sotto `Disposizione in vista griglia` c'è `Disposizione in vista feed`, col tuo testo e gli stessi due gettoni, e la scelta è quella che hai appena fatto.

Letture mie: `Scatto, Modifica` usa la data di scatto, e per un file che non ce l'ha (una cattura, un'immagine scaricata) la data di modifica. `Modifica` usa la data in cui il file è stato scritto l'ultima volta, che per un file mai toccato coincide con quella di creazione: Android non conserva la data di creazione a parte. La sezione `Cartelle` delle impostazioni ora si chiama `File e cartelle`, e così la parte corrispondente di `Esporta e importa`, che fino alla 6.01 era `Cartelle e percorsi`; la data del Feed viaggia con lei.

## 5. Il velo del Feed

La prima volta che entri nel Feed, con delle immagini da mostrare, compare al centro una frase che dice dove si sceglie la data. Toccala: il velo si chiude, e non torna. Per rivederlo, in `Impostazioni` usa `Ripristina gli avvisi`.

Letture mie: il testo è mio, come mi avevi lasciato fare, ed è fra le etichette qui sotto. Non indica il pulsante, perché il gesto della pressione lunga lo insegna già il velo delle colonne nella griglia. Il catalogo dei veli è aggiornato con questo.

## 6. Il tondo e la pillola nelle impostazioni, e le pillole a otto voci

Col menu Start scelto, in `Impostazioni` → `Pulsanti e indicatori` → `Elemento interattivo principale`, sotto i gettoni c'è una seconda riga: `Tondo` e `Pillola`, coi tuoi due disegni del commutatore. Scegli `Pillola`: a riposo il menu Start è la pillola verticale; torna a `Tondo`. Poi passa alla pillola estesa e, con almeno una cartella nascosta, guarda la casa: le voci sono otto, i tasti un poco più stretti.

Letture mie: il tondo resta il riposo di fabbrica. Le otto voci sono la tua scelta A1: senza cartelle nascoste le voci restano sette, e `Cerca` è al centro come prima; con otto è spostata di mezza casella.

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
Il Feed mostra foto e video di tutte le cartelle, i più recenti in alto. Per scegliere se conta la data di scatto o quella di modifica, tieni premuto il pulsante.

### e-settings_group_browse · La sezione delle impostazioni e la parte di 'Esporta e importa'
File e cartelle

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il Feed | 6.02 | Scelte del 2026-10-14 | Accesso dal menu Start, data B1 a scelta, mesi C1, cartelle D1, velo, `File e cartelle`; pillole a otto voci A1. | Fatto nella 6.02 (`6.02-01`...`6.02-05`). |
| La vibrazione della pressione lunga | 5.92 | Scelta B1 | Il tocco che apre la selezione era *leggerissimo, diverso da altre vibrazioni*. | Fatta nella 5.92 (`5.92-01`). |

## Prossimi passi

- **In collaudo, giro cumulativo 5.92 + 6.02**: la vibrazione (`5.92-01`), il Feed dal menu Start (`6.02-01`), il flusso e i mesi (`6.02-02`), la data (`6.02-03`), il velo (`6.02-04`), tondo e pillola nelle impostazioni (`6.02-05`), e le cinque etichette.
- **Fermi fino al tuo via**: le funzioni sui file di AIV Play rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella); `Salva` sull'originale non serve, perché AIV Play salva già una copia `-aivEdit` accanto all'originale. Le 11 dipendenze più nuove non le ho lette prima di questa release: le leggo prima della prossima.
- **In attesa del tuo via, dalle verifiche**: i permessi di scrittura su Android 9 e 10, la correzione di Chromium in `feedback-check.py`, le 50 classi di prova da aggiornare, la prova dell'altezza del pannello dell'editor.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app, i 12 tester per i 14 giorni del test chiuso e le schermate del telefono per la scheda (risposta G1); icona e immagine in evidenza sono pronte in `docs/store/`, e i passi in ordine sono in fondo a `docs/Store.md`.
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
