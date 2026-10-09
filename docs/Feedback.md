# Feedback AIV

Versione **4.91**: nel modulo Disegno il Pannello al posto della Sfocatura, i colori della pillola, il testo ritoccato, `Elimina` di nuovo fra i tasti, la centratura sull'immagine e gli strumenti più grandi.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.91 è pubblicata: [v4.91](https://github.com/Roccobot/AIV/releases/tag/v4.91), con l'APK
[AIV-4.91.apk](https://github.com/Roccobot/AIV/releases/download/v4.91/AIV-4.91.apk).
Commit prodotto su `main`: `dc382bd`, release dal commit `dc382bd` (SlimVer 4.91 / versionCode 348; APK 10.998.405 byte, digest `3d23c92c`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.91**: sei prove sul modulo Disegno, due **domande** e in fondo l'**etichetta testuale** del testo nuovo. Campo vuoto vuol dire approvato. Giro **4.90**: tutte le prove OK; la Sfocatura, non approvata, diventa lo strumento Pannello.

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

Le verifiche automatiche della 4.91 sono superate: banco di prova completo (670 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.91-01 | Non provato | | Attendere il collaudo. |
| 4.91-02 | Non provato | | Attendere il collaudo. |
| 4.91-03 | Non provato | | Attendere il collaudo. |
| 4.91-04 | Non provato | | Attendere il collaudo. |
| 4.91-05 | Non provato | | Attendere il collaudo. |
| 4.91-06 | Non provato | | Attendere il collaudo. |

## 1. Lo strumento Pannello

Nel modulo Disegno tocca l'ultimo strumento della prima fila, `Pannello`. Sotto compaiono i tasti del testo, con `Sfondo` spento; i tondi mostrano i colori della pillola, con `Nessuno` per primo e già scelto; il cursore è `Sfocatura`. Trascina il dito su una zona con dettagli: disegni un rettangolo dagli angoli appena stondati, e alzando il dito si apre la finestra delle parole. Scrivi una frase e tocca `Applica`: dentro il pannello l'immagine è sfocata già nell'editor, e le parole sono bianche, vanno a capo da sole e prendono la misura più grande che il pannello contiene.

Sposta il cursore `Sfocatura`: l'area si sfoca di più o di meno. Tocca un colore: il pannello lo prende, leggero, al 20%; `Nessuno` lo toglie. Tocca un punto vuoto con `Pannello`: dopo le parole compare un pannello che le contiene. Gira un pannello, poi copiane lo stile e incollalo su un altro: passano colore e sfocatura. Salva una copia, tenendo premuto `Salva`, e apri il file: è come nell'editor. Rettangolo ed ellisse non hanno più `Sfocatura`.

Letture mie: lo 0,3% dell'arrotondamento è del lato lungo del pannello, non dell'immagine; di fabbrica il pannello non ha colore, perché è prima di tutto una sfocatura; le parole hanno dal bordo il 15% del lato corto, meno della pillola, che perde spazio nelle estremità tonde; il pannello non ha le due tracce della pillola, che non hai elencato; si disegna al suo posto fra gli altri elementi, quindi un tocco prende quello più in alto, mentre la Sfocatura era sotto tutti.

## 2. I colori della pillola

Tocca `Pillola`: i tondi restano accesi e mostrano otto colori, il tuo rosso per primo, poi arancio, rosa, magenta, indaco, blu, verde acqua e verde. Disegna una pillola con una parola e prova i colori uno per uno, su una zona chiara e su una scura: il riempimento cambia colore e resta traslucido come prima, mentre le tracce e le parole bianche restano uguali. Il vetro sotto la pillola adesso si vede anche nell'editor: fino alla 4.90 c'era solo nel file salvato.

Letture mie: i colori sono vivaci ma non chiari, perché ognuno deve tenere leggibili le parole bianche (contrasto di almeno 3), quindi il giallo e il lime fluo sono fuori; non hanno un nome, e il lettore di schermo li chiama `Sfondo 1`...`Sfondo 8`; tenerli premuti non apre la luminosità, che la pillola non ha.

## 3. Il testo: `Sfondo`, `Testo` e la dimensione

Tocca lo strumento `Testo`. I sette tasti sono, in quest'ordine, `Carattere`, `Grassetto`, `Corsivo`, `Barrato`, `Sfondo`, `Allineamento` e `Testo`. Scrivi un testo bianco e lascialo scelto. Tocca `Sfondo`: si accende, dietro le parole compare la striscia viola dell'etichetta e sotto il cursore la fila dei suoi colori; toccalo ancora e si spegne. `Evidenziato` non c'è più. Tocca l'ultimo tasto, `Testo`: si riapre la finestra con le parole.

Porta il cursore `Dimensione` tutto a destra: una lettera diventa più alta dell'immagine. Riportalo verso sinistra: nella prima parte il cursore cambia la dimensione con calma, e il 5% di fabbrica è a circa un terzo. Tira un angolo del testo: arriva allo stesso massimo.

Letture mie: il massimo è il 150% del lato lungo dell'immagine (avevi chiesto almeno il 110%); il cursore moltiplica la dimensione per lo stesso fattore a ogni tratto, perché su un cursore uniforme dall'1% al 150% le dimensioni con cui si scrive sarebbero tutte nel primo ventesimo.

## 4. `Elimina` nella quinta colonna

Col rettangolo come strumento, la quinta colonna dei tasti è `Elimina`, spento. Disegna due rettangoli, tocca il secondo per sceglierlo e tocca `Elimina`: il secondo sparisce, il primo resta, e `Elimina` torna spento. `Annulla` lo riporta. Il menu della pressione lunga ha ancora il suo `Elimina`.

Letture mie: `Elimina` è nella fila delle forme, dov'era fino alla 4.70; nella fila del testo, della pillola e del pannello non c'è, perché `Testo` deve restare l'ultimo a destra (vedi la domanda `d-elimina-testo`).

## 5. La centratura sull'immagine

Disegna un rettangolo lontano dal centro e spostalo verso il centro dell'immagine: quando il suo centro arriva a circa 2 mm dal centro orizzontale, scatta lì, e una guida color accento attraversa l'immagine dall'alto in basso. Lo stesso col centro verticale, con la guida da sinistra a destra, e con tutti e due insieme. Prova anche con una linea, un testo e una pillola. Disegnando un elemento nuovo vicino al centro, il suo primo punto non scatta.

Letture mie: scatta solo il centro dell'elemento, non i suoi lati, e solo spostandolo, non disegnandolo né tirando una maniglia, perché hai chiesto la centratura dello spostamento.

## 6. Gli strumenti più grandi

Guarda la prima fila del modulo Disegno: gli otto strumenti hanno glifi più grandi di prima, e lo strumento scelto ha lo sfondo verde e il bordo pieno, come i tasti sotto. Controllalo col telefono in verticale e in orizzontale.

Letture mie: il glifo è di 28 dp in un tasto alto 32; gli strumenti sono adesso tasti uguali a quelli sotto, perché i gettoni di Material tenevano 8 dp per lato attorno al glifo, che quindi non poteva crescere.

## Domande

### d-velo-pannello · Il velo del Disegno e il Pannello

Il velo d'aiuto del Disegno elenca gli strumenti con le tue parole: *linee, frecce, ellissi, rettangoli arrotondati, testi semplici e riquadri 'pillola'*. Il Pannello non c'è.

**A1**: aggiungo *e pannelli sfocati* in fondo all'elenco, in tutte le lingue.

**A2**: lo lascio com'è.

Parere: **A1**, perché il velo è il solo posto in cui si scopre uno strumento prima di averlo toccato.

### d-elimina-testo · `Elimina` anche per testo, pillola e pannello

La fila dei tasti del testo ha sette tasti in otto colonne, e `Testo` deve restare l'ultimo a destra. Con un testo, una pillola o un pannello scelti, `Elimina` oggi è solo nel menu della pressione lunga.

**B1**: `Elimina` nella quinta colonna anche lì, come nella fila delle forme: `Carattere`, `Grassetto`, `Corsivo`, `Barrato`, `Elimina`, `Sfondo`, `Allineamento`, `Testo`.

**B2**: `Elimina` nella settima colonna, fra `Allineamento` e `Testo`.

**B3**: resta solo nel menu della pressione lunga.

Parere: **B1**, perché `Elimina` è nella stessa colonna con ogni strumento, e il dito lo trova senza guardare.

## Etichette testuali

### e-draw_panel · Lo strumento Pannello
Pannello
<!-- Il nome che il lettore di schermo legge sull'ottavo strumento. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il Pannello | 4.91-01 | Non provato | | Attendere il collaudo. |
| I colori della pillola | 4.91-02 | Non provato | | Attendere il collaudo. |
| Il testo con `Sfondo`, `Testo` e la dimensione | 4.91-03 | Non provato | | Attendere il collaudo. |
| `Elimina` nella quinta colonna | 4.91-04 | Non provato | | Attendere il collaudo. |
| La centratura sull'immagine | 4.91-05 | Non provato | | Attendere il collaudo. |
| Gli strumenti più grandi | 4.91-06 | Non provato | | Attendere il collaudo. |
| Le guide verso gli altri elementi | 4.90-01 | OK | | Chiusa. |
| Lo strumento Testo | 4.90-02 | OK | Più grande | Fatto nella 4.91 (prova `4.91-03`). |
| Caratteri, stili, allineamento e dimensione | 4.90-03 | OK | | Chiusa. |
| I fondi del testo | 4.90-04 | OK | Via `Evidenziato` | Fatto nella 4.91 (prova `4.91-03`). |
| Lo strumento Pillola | 4.90-05 | OK | I colori della pillola | Fatto nella 4.91 (prova `4.91-02`). |
| Le sfumature senza bande | 4.90-06 | OK | | Chiusa. |
| La forma dell'area sfocata | 4.81-01 | Non approvato | La Sfocatura diventa lo strumento Pannello | Fatto nella 4.91 (prova `4.91-01`). |
| Il tocco che alterna `Trasforma` e `Ruota` | 4.81-02 | OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: il Pannello (`4.91-01`), i colori della pillola (`4.91-02`), il testo (`4.91-03`), `Elimina` (`4.91-04`), la centratura (`4.91-05`), gli strumenti più grandi (`4.91-06`); le domande `d-velo-pannello` e `d-elimina-testo`; l'etichetta del Pannello.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D), come hai deciso.
- **Dopo**: gli stili, nella 4.95, con uno o due rilasci di assestamento; poi la 5.00.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
