# Feedback AIV

Versione **4.00**: la pillola al posto del FAB sul telefono in verticale, con le sue due file di gettoni.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.00 è pubblicata: [v4.00](https://github.com/Roccobot/AIV/releases/tag/v4.00), con l'APK
[AIV-4.00.apk](https://github.com/Roccobot/AIV/releases/download/v4.00/AIV-4.00.apk).
Commit prodotto su `main`: `91c4c0f` (SlimVer 4.00 / versionCode 309).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.00**: sei prove in collaudo, tutte nuove. Giro **3.73**: `3.73-01`, `3.73-02` e `3.73-03` OK.

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

Le verifiche automatiche della 4.00 sono superate: banco di prova completo (544 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.00-01 | Non provato | | Attendere il collaudo. |
| 4.00-02 | Non provato | | Attendere il collaudo. |
| 4.00-03 | Non provato | | Attendere il collaudo. |
| 4.00-04 | Non provato | | Attendere il collaudo. |
| 4.00-05 | Non provato | | Attendere il collaudo. |
| 4.00-06 | Non provato | | Attendere il collaudo. |

## 1. La voce nelle impostazioni

Con AIV **4.00**, sul **telefono**, apri Impostazioni → Aspetto → **Pulsanti e indicatori**: in cima c'è `Pillola al posto del FAB in verticale`, col tuo paragrafo e due file di gettoni. Nella prima `Disattivata`, `A scorrimento` e `Estesa` (di fabbrica `Disattivata`, cioè il FAB di sempre); nella seconda `Tinta unita`, `Semitrasparente` e `Vetro satinato` (di fabbrica `Tinta unita`). Da Android 11 in giù `Vetro satinato` non compare. Sul **tablet** la voce non c'è.

## 2. A scorrimento

Scegli `A scorrimento` e torna alla schermata iniziale, in verticale. Il FAB è tondo e color accento, sul lato di 'Posizione preferita dei pulsanti'. Toccalo: si allunga in una pillola orizzontale con tutte le icone del menu e la × semitrasparente nell'angolo, con un'animazione che parte velocissima e rallenta, in 160 ms al massimo. La × la richiude, e così il gesto Indietro; toccando un'icona la pillola si richiude e la funzione parte. Col tasto tondo restano il salto (scorrendo diventa la freccia) e il tocco lungo delle colonne. Prova anche dentro una cartella, nella ricerca e nel cestino.

## 3. Estesa, e la piegatura mentre scorri

Scegli `Estesa`: niente FAB, niente ×, niente menu; la pillola con tutte le icone è sempre in basso, sul lato preferito. Scorri una griglia: la pillola si accorcia a due tasti, e le due icone verso l'angolo diventano `↑` (quella interna) e `↓` (quella nell'angolo), insieme alla pillola; un secondo dopo che ti fermi torna larga. `↑` porta in cima, `↓` in fondo. Con `Estesa` il tocco lungo delle colonne non c'è.

## 4. Il fumetto al tocco lungo

In qualunque pillola (anche quelle del telefono in orizzontale e del tablet) tieni premuta un'icona: compare un fumetto con il suo nome, sopra il dito, e nient'altro. Con una cartella nascosta, il tocco lungo su 'Mostra nascoste' nella pillola mostra il fumetto e non apre l'elenco delle nascoste, che resta nel menu del FAB.

## 5. Tinta unita, Semitrasparente, Vetro satinato

Con la pillola in scena prova le tre tinte, nei due temi: piena, all'80%, e il vetro, che sfoca quello che scorre sotto come la pillola del DF, con l'accento al 70% sul tema chiaro e al 25% su quello scuro. Come nel DF, il vetro ravviva anche i colori che ci passano sotto. La scelta vale anche per la pillola del telefono in orizzontale.

## 6. I menu col vetro

Con `Vetro satinato` scelto ed 'Effetto dietro menu e pannelli' su `Sfocatura`, apri un menu o un dialogo: lo sfondo si sfoca come il vetro della pillola, velato dall'accento al 10% invece che dal nero. Qui i colori dietro restano un po' meno accesi che sulla pillola: la sfocatura dei menu la fa il sistema, che non li ravviva. Col risparmio energetico acceso il telefono non sfoca, e torna il velo nero.

## Etichette testuali

### e-pill-title · Titolo della voce
Pillola al posto del FAB in verticale

### e-pill-desc · Paragrafo della voce
Una serie di icone affiancate in un riquadro arrotondato al posto del menu con i comandi estesi. Premi a lungo su un'icona per visualizzare il tooltip esplicativo.

### e-pill-modes · Prima fila di gettoni
Disattivata · A scorrimento · Estesa

### e-pill-fills · Seconda fila di gettoni
Tinta unita · Semitrasparente · Vetro satinato

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La voce nelle impostazioni | 4.00-01 | Non provato | | Attendere il collaudo. |
| A scorrimento | 4.00-02 | Non provato | | Attendere il collaudo. |
| Estesa e piegatura | 4.00-03 | Non provato | | Attendere il collaudo. |
| Fumetto al tocco lungo | 4.00-04 | Non provato | | Attendere il collaudo. |
| Tinte della pillola | 4.00-05 | Non provato | | Attendere il collaudo. |
| Menu col vetro | 4.00-06 | Non provato | | Attendere il collaudo. |
| Ritorno alla griglia senza sfarfallio | 3.73-01 | OK | Tutto OK. | Archiviata. |
| Tablet in verticale: maniglia più tenue | 3.73-02 | OK | Tutto OK. | Archiviata. |
| Intestazione di casa sul telefono | 3.73-03 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: la pillola al posto del FAB (`4.00-01`-`4.00-06`).
- **Deciso**: la vista 'Cartelle di sistema' tiene il FAB anche su tablet e in orizzontale.
- **Concluso**: ritorno alla griglia, maniglia e intestazione di casa (`3.73-01`-`3.73-03`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
