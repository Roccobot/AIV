# Feedback AIV

Versione **4.32**: un solo punto di riferimento per tutti i comandi, il menu Start di fabbrica, il velo d'aiuto del primo avvio e lo zoom all'apertura anche con le info spente. Le prove della 4.30 restano aperte.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.32 è pubblicata: [v4.32](https://github.com/Roccobot/AIV/releases/tag/v4.32), con l'APK
[AIV-4.32.apk](https://github.com/Roccobot/AIV/releases/download/v4.32/AIV-4.32.apk).
Commit prodotto su `main`: `0e7f0ab` (SlimVer 4.32 / versionCode 321).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.32**: le prove della 4.30 e tre nuove, dalle tue note dopo la 4.30; `4.30-05` riscritta per le info spente. Giro **4.25**: `4.25-01` e `4.25-03`-`4.25-06` OK; `4.25-02` e `4.25-07` rifatte nella 4.30.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 4.32 sono superate: banco di prova completo (572 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.30-01 | Non provato | | Attendere il collaudo. |
| 4.30-02 | Non provato | | Attendere il collaudo. |
| 4.30-03 | Non provato | | Attendere il collaudo. |
| 4.30-04 | Non provato | | Attendere il collaudo. |
| 4.30-05 | Non provato | | Attendere il collaudo. |
| 4.30-06 | Non provato | | Attendere il collaudo. |
| 4.30-07 | Non provato | | Attendere il collaudo. |
| 4.31-01 | Non provato | | Attendere il collaudo. |
| 4.31-02 | Non provato | | Attendere il collaudo. |
| 4.31-03 | Non provato | | Attendere il collaudo. |

## 1. I nomi dei menu

In **Elemento interattivo principale** i gettoni si chiamano `Menu` (era `Menu inferiore`) e `Menu 'Start'` (era `Menu angolare`), e il paragrafo sotto il titolo li nomina così. Nelle altre lingue `Start` è il nome che ognuna usa per il menu di Windows: per esempio `Démarrer` in francese, `Inicio` in spagnolo e `Start` dove è rimasto così, come in tedesco e in olandese.

## 2. Il tondo unico

Con la pillola a scomparsa, e col menu Start, il tondo è largo 44dp nella schermata iniziale come nelle cartelle, ed è nello stesso angolo: passa dalla home a una cartella e ritorno, e non deve muoversi né cambiare misura. Nella home su un telefono stretto era più piccolo, perché la pillola aperta divideva la riga per otto tasti, e nelle cartelle era 4dp più in alto.

## 3. Il menu Start si chiude a ogni tocco fuori

Apri il menu Start, poi tocca un'area vuota, oppure trascina il dito sulla griglia: il menu si chiude. Trascinando, la griglia intanto scorre.

## 4. Il glifo del FAB

Con `Tasto fluttuante`, nel FAB quadrato il triangolo del marchio è centrato in orizzontale, e il disco lo segue; l'altezza è quella di prima. Poi scegli `Traslucido` in **Tema e dettagli grafici**: il glifo del FAB è `#ecfff7` nel tema chiaro e `#004247` nel tema scuro, anche quando lo premi.

## 5. Lo zoom all'apertura

Apri un'immagine dalla griglia di una cartella, una volta con le info in alto accese e una volta spente: deve comparire subito nella sua misura, senza partire piccola e crescere. Con le info spente l'anteprima si disegnava prima di sapere la misura del file, quindi alla misura della miniatura, e cresceva quando la misura arrivava (la causa del tuo caso, corretta nella 4.32). Adesso aspetta la misura, cioè un attimo in più prima di comparire. Il banco non carica immagini, quindi la conferma viene dal tuo telefono.

## 6. La sfocatura sotto il vetro

Con `Traslucido` apri il menu Start o la pillola sopra la griglia della schermata iniziale: sotto il vetro anche i nomi delle cartelle, i numeri e i bordi delle miniature devono essere sfocati, non nitidi. Sotto la copia sfocata adesso c'è il fondo della pagina, che prima mancava: per questo si vedevano gli originali nitidi attraverso.

## 7. Le sfumature in basso

Nella schermata iniziale e in cima a ogni cartella ci sono tutte e due le sfumature. Scorri una cartella: se ne va la più corta, insieme al titolo, e resta la più ampia e leggera. Arriva in fondo: sparisce anche lei, così l'ultima riga si vede intera.

## 8. Il punto di riferimento dei comandi

Il centro del tondo è il centro di ogni comando, nella stessa posizione della 4.30. In **Elemento interattivo principale** scegli `Tasto fluttuante`: il FAB ha il centro dove la pillola e il menu Start hanno il tondo, nella schermata iniziale come in una cartella. Passa da una schermata all'altra e poi da `Tasto fluttuante` a `Menu 'Start'` e ritorno: il centro del comando non si deve muovere. Il menu del FAB, aperto, ha il fianco dove ce l'ha il menu Start aperto. La barra di `Menu` resta una fascia in fondo: con pochi tasti quello d'angolo è sull'asse del tondo, ma non alla sua altezza.

## 9. Il menu Start di fabbrica

Il menu Start è il comando di fabbrica di un telefono nuovo. Chi ha già l'app ritrova la sua scelta: per vederlo, scegli `Menu 'Start'` in **Elemento interattivo principale**, oppure installa l'app da zero.

## 10. Il velo d'aiuto del primo avvio

Con `Menu 'Start'` scelto, nelle impostazioni tocca `Ripristina gli avvisi` e torna alla schermata iniziale, in modalità griglia. Il velo mostra il menu Start aperto, in arancione, con la copia del tondo nell'angolo esattamente sopra il tondo vero, e la frase `Tutti i comandi sono nel menu 'Start': tocca il tasto tondo per aprirlo, tienilo premuto per le opzioni di visualizzazione.` Il tocco lungo sulla copia apre le opzioni di visualizzazione; un tocco qualunque chiude il velo. La frase è nuova: dimmi se la vuoi diversa.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| I nomi dei menu | 4.30-01 | Non provato | | Attendere il collaudo. |
| Il tondo unico | 4.30-02 | Non provato | | Attendere il collaudo. |
| Il menu Start si chiude a ogni tocco fuori | 4.30-03 | Non provato | | Attendere il collaudo. |
| Il glifo del FAB | 4.30-04 | Non provato | | Attendere il collaudo. |
| Lo zoom all'apertura | 4.30-05 | Non provato | | Attendere il collaudo. |
| La sfocatura sotto il vetro | 4.30-06 | Non provato | | Attendere il collaudo. |
| Le sfumature in basso | 4.30-07 | Non provato | | Attendere il collaudo. |
| Il punto di riferimento dei comandi | 4.31-01 | Non provato | | Attendere il collaudo. |
| Il menu Start di fabbrica | 4.31-02 | Non provato | | Attendere il collaudo. |
| Il velo d'aiuto del primo avvio | 4.31-03 | Non provato | | Attendere il collaudo. |
| Il menu angolare a riposo, e il commutatore | 4.25-01 | OK | Tutto OK. | Archiviata. |
| Il menu angolare si chiude a ogni tocco | 4.25-02 | Accettabile | Qualunque tocco fuori, anche un trascinamento. | Fatto nella 4.30 (`4.30-03`). |
| La pillola più corta | 4.25-03 | OK | Tutto OK. | Archiviata. |
| Il traslucido di fabbrica, e lo Scostamento | 4.25-04 | OK | Tutto OK. | Archiviata. |
| I due colori del vetro | 4.25-05 | OK | Tutto OK. | Archiviata. |
| Il bordo dell'anteprima | 4.25-06 | OK | Tutto OK. | Archiviata. |
| Le due sfumature in basso | 4.25-07 | Non approvato | Resta la sfumatura più ampia, non la corta. | Rifatto nella 4.30 (`4.30-07`). |

## Prossimi passi

- **In collaudo**: nomi dei menu, tondo unico, chiusura del menu Start, glifo del FAB, zoom all'apertura, sfocatura sotto il vetro, sfumature (`4.30-01`-`4.30-07`); punto di riferimento dei comandi, menu Start di fabbrica, velo del primo avvio (`4.31-01`-`4.31-03`).
- **Concluso**: menu Start a riposo e commutatore, pillola più corta, traslucido di fabbrica, due colori, bordo dell'anteprima (`4.25-01`, `4.25-03`-`4.25-06`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
