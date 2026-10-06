# Feedback AIV

Versione **4.34**: le correzioni del giro 4.33. Il menu Start più grande con le etichette intere e `Apri URL`, il tondo giallo del velo d'aiuto, la pillola a scomparsa con i salti anche aperta e i capi più distanti dai bordi, i salti solo con un lancio veloce, e l'inchiostro sul vetro scelto per contrasto.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.34 è pubblicata: [v4.34](https://github.com/Roccobot/AIV/releases/tag/v4.34), con l'APK
[AIV-4.34.apk](https://github.com/Roccobot/AIV/releases/download/v4.34/AIV-4.34.apk).
Commit prodotto su `main`: `c325c20` (SlimVer 4.34 / versionCode 323).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.34**: sei prove nuove dal giro 4.33. Giro **4.33**: `4.33-01`, `4.33-02`, `4.33-03`, `4.33-04`, `4.33-06` e `4.33-08` OK; `4.33-05` e `4.33-07` rifatte nella 4.34.

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

Le verifiche automatiche della 4.34 sono superate: banco di prova completo (583 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.34-01 | Non provato | | Attendere il collaudo. |
| 4.34-02 | Non provato | | Attendere il collaudo. |
| 4.34-03 | Non provato | | Attendere il collaudo. |
| 4.34-04 | Non provato | | Attendere il collaudo. |
| 4.34-05 | Non provato | | Attendere il collaudo. |
| 4.34-06 | Non provato | | Attendere il collaudo. |

## 1. Il menu Start più grande

Apri il menu Start nella schermata iniziale, poi in una cartella e nel cestino. Le icone hanno la stessa misura; le colonne sono più larghe (72dp invece di 64) e il pannello cresce di 6dp verso il bordo dello schermo, 4 in basso e 10 in alto, con la × sempre sul centro del tondo. Il fianco destro del pannello adesso cade fuori dal bordo delle miniature, in home e nelle cartelle. `Impostazioni` ha lo stesso corpo delle altre etichette ed è intera: il taglio veniva dal tasto, che ritagliava a cerchio tutto quello che conteneva. `URL` diventa `Apri URL`.

## 2. Il velo d'aiuto del primo avvio

Nelle impostazioni tocca `Ripristina gli avvisi` e torna alla schermata iniziale col menu Start: nell'angolo del menu disegnato c'è il tondo giallo col glifo scuro del tuo mockup, al posto della copia arancione che sul pannello non si vedeva come un tasto.

## 3. La pillola a scomparsa aperta

Scegli la pillola a scomparsa, apri la pillola nella schermata iniziale (dove la fila è piena): `Lista` a un capo e la × all'altro sono 6dp più dentro, e le altre icone si sono distribuite di conseguenza.

## 4. I salti con la pillola a scomparsa aperta

Con la pillola a scomparsa aperta, in una cartella, nel cestino e nella schermata iniziale, lancia la griglia con un gesto veloce: i due tasti accanto alla × diventano su e giù, e finito lo scorrimento tornano alle loro funzioni, come nelle altre modalità.

## 5. I salti solo con un lancio veloce

In tutte le modalità, scorri piano di poche righe: i tasti su e giù non compaiono. Lancia la griglia con un gesto veloce: compaiono, e il glifo passa da un disegno all'altro in qualche fotogramma invece che di colpo. La soglia è la velocità con cui il dito si stacca, 1000dp al secondo: è una mia prima stima, dimmi se la vuoi più alta o più bassa.

## 6. L'inchiostro sul vetro

Scegli il vetro come aspetto dei pulsanti principali e dagli un colore chiaro, per esempio un giallo: le icone della pillola, del menu e del FAB diventano scure. Con un colore scuro tornano chiare. È la tua risposta A1: fra i due inchiostri del vetro l'app sceglie quello che stacca di più dal colore che hai scelto. Senza un colore tuo non cambia niente.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il menu Start più grande | 4.34-01 | Non provato | | Attendere il collaudo. |
| Il velo d'aiuto del primo avvio | 4.34-02 | Non provato | | Attendere il collaudo. |
| La pillola a scomparsa aperta | 4.34-03 | Non provato | | Attendere il collaudo. |
| I salti con la pillola a scomparsa aperta | 4.34-04 | Non provato | | Attendere il collaudo. |
| I salti solo con un lancio veloce | 4.34-05 | Non provato | | Attendere il collaudo. |
| L'inchiostro sul vetro | 4.34-06 | Non provato | | Attendere il collaudo. |
| Lo zoom all'apertura con le info spente | 4.33-01 | OK | Tutto OK. | Archiviata. |
| Il trascinamento col menu Start aperto | 4.33-02 | OK | Tutto OK. | Archiviata. |
| Menu basso | 4.33-03 | OK | Tutto OK. | Archiviata. |
| Le sfumature in basso | 4.33-04 | OK | Tutto OK. | Archiviata. |
| Il velo d'aiuto del primo avvio | 4.33-05 | Accettabile | Il tondo giallo del mockup. | Rifatto nella 4.34 (`4.34-02`). |
| Il glifo che torna sul tondo | 4.33-06 | OK | Tutto OK. | Archiviata. |
| Le etichette del menu Start | 4.33-07 | Non approvato | Etichette tagliate, menu da allargare, `Apri URL`. | Rifatto nella 4.34 (`4.34-01`). |
| Il commutatore con dieci icone | 4.33-08 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: menu Start, velo d'aiuto, pillola aperta, salti nella pillola aperta, salti col lancio, inchiostro sul vetro (`4.34-01`-`4.34-06`).
- **Prossima versione**: il modulo Disegno, prima fase (4.40: mano libera, segmenti, frecce, rettangoli, ellissi, colore e tratto), con le tue risposte D1a, D2a, D3a e D4a.
- **Da decidere in chat**: la dissolvenza al tocco di una miniatura (nota A del giro 4.32); la soglia del lancio, se 1000dp al secondo non va.
- **Concluso**: zoom con le info spente, trascinamento unico, Menu basso, sfumature, glifo, commutatore (`4.33-01`-`4.33-04`, `4.33-06`, `4.33-08`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
