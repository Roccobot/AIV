# Feedback AIV

Versione **4.35**: le correzioni del giro 4.34. Il menu Start sempre quadrato, `Impostazioni` mai nell'angolo arrotondato in basso, il tocco lungo su `Mostra`/`Nascondi`, il tondo giallo pieno del velo d'aiuto e i salti con la × nella pillola a scomparsa aperta.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.35 è pubblicata: [v4.35](https://github.com/Roccobot/AIV/releases/tag/v4.35), con l'APK
[AIV-4.35.apk](https://github.com/Roccobot/AIV/releases/download/v4.35/AIV-4.35.apk).
Commit prodotto su `main`: `bd36459` (SlimVer 4.35 / versionCode 324).
Tutti i veli d'aiuto, con le schermate: [Veli d'aiuto di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.35**: cinque prove nuove dal giro 4.34. Giro **4.34**: `4.34-03`, `4.34-05` e `4.34-06` OK; `4.34-01`, `4.34-02` e `4.34-04` rifatte nella 4.35.

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

Le verifiche automatiche della 4.35 sono superate: banco di prova completo (586 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.35-01 | Non provato | | Attendere il collaudo. |
| 4.35-02 | Non provato | | Attendere il collaudo. |
| 4.35-03 | Non provato | | Attendere il collaudo. |
| 4.35-04 | Non provato | | Attendere il collaudo. |
| 4.35-05 | Non provato | | Attendere il collaudo. |

## 1. Il menu Start quadrato

Apri il menu Start nella schermata iniziale e in una cartella: è alto quanto è largo. Il fondo resta sul tondo, quindi il pannello sale; le righe sono passate da 64 a 68dp e fra icona ed etichetta ci sono 3dp. I 4dp che avanzano nella schermata iniziale vanno sopra la prima riga.

## 2. Impostazioni mai nell'angolo in basso

Apri il menu Start in una cartella: `Impostazioni` è in alto, accanto a `Cerca`, e `Cestino` è in basso, accanto alla ×. La regola è generale: l'etichetta più lunga non va mai nella casella d'angolo arrotondata della riga in basso, e si scambia con la casella sopra la ×. Nella schermata iniziale e nel cestino quella casella ha già un'etichetta corta, quindi lì non cambia niente.

## 3. Il tocco lungo su Mostra

Con almeno una cartella nascosta, apri il menu Start nella schermata iniziale e tieni premuto `Mostra` (o `Nascondi`): il menu si chiude e si apre l'elenco `Cartelle nascoste`. Il tocco breve fa quello che faceva.

## 4. Il tondo giallo del velo d'aiuto

Scegli il vetro come aspetto dei pulsanti principali, poi nelle impostazioni tocca `Ripristina gli avvisi` e torna alla schermata iniziale col menu Start: il tondo nell'angolo del menu disegnato è `#ffda3c` pieno, come l'arancione del pannello. Fino alla 4.34 prendeva l'aspetto dei pulsanti veri, e col vetro arrivava velato. Il menu disegnato è anche quadrato, come quello vero.

## 5. I salti con la ×

Con la pillola a scomparsa aperta, in una cartella, nel cestino e nella schermata iniziale, lancia la griglia con un gesto veloce: la × diventa giù e il tasto accanto su. Finito lo scorrimento tornano la × e il tasto di prima.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il menu Start quadrato | 4.35-01 | Non provato | | Attendere il collaudo. |
| Impostazioni mai nell'angolo in basso | 4.35-02 | Non provato | | Attendere il collaudo. |
| Il tocco lungo su Mostra | 4.35-03 | Non provato | | Attendere il collaudo. |
| Il tondo giallo del velo d'aiuto | 4.35-04 | Non provato | | Attendere il collaudo. |
| I salti con la × | 4.35-05 | Non provato | | Attendere il collaudo. |
| Il menu Start più grande | 4.34-01 | Accettabile | Quadrato, `Impostazioni` mai in basso, tocco lungo su `Mostra`. | Rifatto nella 4.35 (`4.35-01`-`4.35-03`). |
| Il velo d'aiuto del primo avvio | 4.34-02 | Accettabile | Tondo `#ffda3c` pieno; un artefatto con tutti i veli. | Rifatto nella 4.35 (`4.35-04`); artefatto pubblicato. |
| La pillola a scomparsa aperta | 4.34-03 | OK | Tutto OK. | Archiviata. |
| I salti con la pillola a scomparsa aperta | 4.34-04 | Non approvato | I due tasti più vicini all'angolo, × compresa. | Rifatto nella 4.35 (`4.35-05`). |
| I salti solo con un lancio veloce | 4.34-05 | OK | Tutto OK. | Archiviata. |
| L'inchiostro sul vetro | 4.34-06 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: menu quadrato, `Impostazioni` in alto, tocco lungo su `Mostra`, tondo giallo pieno, salti con la × (`4.35-01`-`4.35-05`).
- **Prossima versione**: il modulo Disegno, prima fase (4.40: mano libera, segmenti, frecce, rettangoli, ellissi, colore e tratto), con le tue risposte D1a, D2a, D3a e D4a.
- **Da decidere in chat**: la dissolvenza al tocco di una miniatura (nota A del giro 4.32).
- **Concluso**: pillola aperta coi capi più distanti, salti col lancio, inchiostro sul vetro (`4.34-03`, `4.34-05`, `4.34-06`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
