# Feedback AIV

Versione **4.90**: nel modulo Disegno il testo, la pillola, le guide verso gli altri elementi, e le sfumature senza bande (giro cumulativo della 4.81 e della 4.90).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.90 è pubblicata: [v4.90](https://github.com/Roccobot/AIV/releases/tag/v4.90), con l'APK
[AIV-4.90.apk](https://github.com/Roccobot/AIV/releases/download/v4.90/AIV-4.90.apk).
Commit prodotto su `main`: `e5f5bb8`, release dal commit `e5f5bb8` (SlimVer 4.90 / versionCode 347; APK 10.999.097 byte, digest `11e62677`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.90**, cumulativo con la 4.81: due prove sulla Sfocatura e sugli elementi scelti, cinque sul testo, sulla pillola e sulle guide, una sulle sfumature, e in fondo le **etichette testuali** dei testi nuovi. Campo vuoto vuol dire approvato. Giro **4.80**: le quattro prove della 4.70 OK, la Sfocatura rifatta nella 4.81.

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

Le verifiche automatiche della 4.90 sono superate: banco di prova completo (666 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.81-01 | Non provato | | Attendere il collaudo. |
| 4.81-02 | Non provato | | Attendere il collaudo. |
| 4.90-01 | Non provato | | Attendere il collaudo. |
| 4.90-02 | Non provato | | Attendere il collaudo. |
| 4.90-03 | Non provato | | Attendere il collaudo. |
| 4.90-04 | Non provato | | Attendere il collaudo. |
| 4.90-05 | Non provato | | Attendere il collaudo. |
| 4.90-06 | Non provato | | Attendere il collaudo. |

## 1. La forma dell'area sfocata

Nel modulo Disegno, col rettangolo come strumento, accendi `Sfocatura` e disegna un rettangolo su una zona con dettagli: l'area sfocata ha esattamente la forma del rettangolo, con gli angoli stondati. Scegli il rettangolo, tieni premuto, tocca `Ruota` e giralo di una trentina di gradi: l'area resta un rettangolo stondato girato, senza parallelogrammi e senza angoli staccati. Prova anche un'ellisse, dritta e girata.

Nella 4.80 il rettangolo girato diventava un parallelogramma con due angoli stondati staccati: la causa era il contorno con cui l'area si ritaglia, che in due angoli girava al contrario.

## 2. Un tocco sull'elemento scelto alterna `Trasforma` e `Ruota`

Disegna un rettangolo e toccalo: è scelto, coi punti pieni. Toccalo di nuovo: i punti diventano vuoti, e trascinandone uno l'elemento gira. Un terzo tocco riporta i punti pieni. Il tasto del menu della pressione lunga resta, e dice la modalità a cui porta.

## 3. Le guide verso gli altri elementi

Disegna un rettangolo. Disegnane un secondo accanto, e mentre lo disegni avvicina il suo lato sinistro al lato sinistro del primo: entro circa 2 mm si posa sullo stesso allineamento, e una guida color accento va da un elemento all'altro lungo il lato comune. Lo stesso vale per i centri e per gli estremi (il lato di sopra di uno sul lato di sotto dell'altro), in orizzontale e in verticale.

Sposta il secondo rettangolo vicino al primo: si allinea allo stesso modo, coi lati e col centro. Scegli un rettangolo, tira una maniglia verso il lato di un altro elemento: il lato tirato si ferma su di lui. Prova anche con una linea, una freccia e un testo.

Letture mie: la distanza è quella delle guide sul bordo dell'immagine, 12 dp; a parità di distanza vince il bordo dell'immagine; un rettangolo o un'ellisse girati, tirati per una maniglia, non si appoggiano, perché il loro riquadro non segue la maniglia.

## 4. Lo strumento Testo

Nel modulo Disegno tocca il sesto strumento, `Testo`. Sotto compaiono i suoi sette tasti, e i tondi dei colori scelgono il colore delle parole, bianco di fabbrica. Tocca un punto vuoto dell'immagine: si apre una finestra in cui scrivi, con la tastiera; `Applica` resta spento finché non c'è una lettera. Scrivi due righe, con Invio fra le due, e tocca `Applica`: il testo compare centrato dove avevi toccato, già scelto.

Tocca `Modifica testo`: la finestra si riapre con le parole, e cambiandole il testo resta dov'era. Trascinare il dito con `Testo` non disegna niente. Sposta il testo, giralo con un tocco su di lui o col menu, copialo e incollane lo stile su un altro testo: tutto come per gli altri elementi.

Scrivi una frase lunga su una riga sola, sceglila e tira verso sinistra il punto a metà del lato destro: il lato sinistro resta fermo, il testo si stringe e le parole vanno a capo da sole. Tirando il punto a metà del lato sinistro succede lo stesso dall'altra parte. Poi tira un angolo: il testo ingrandisce tutto, e le righe restano le stesse.

Letture mie: la finestra è modale, come `Rinomina`, perché raccoglie un testo; un testo nuovo nasce largo quanto la sua riga più lunga, quindi va a capo da solo solo dopo che ne hai stretto un lato; i punti a metà dei lati di sopra e di sotto ingrandiscono, come gli angoli; la larghezza è del testo, quindi incollandogli lo stile di un altro testo non cambia.

## 5. Caratteri, stili, allineamento e dimensione

Con un testo scelto tocca `Carattere`: a ogni tocco passa al carattere dopo, Roboto, Montserrat, Archivo Narrow e Literata, e il tasto mostra il suo 'Aa' in quel carattere. `Grassetto`, `Corsivo` e `Barrato` si accendono e si spengono e si combinano fra loro. Scrivi un testo su due righe di lunghezza diversa e tocca l'ultimo tasto, `Allineamento`: le righe passano a destra, poi a sinistra, poi tornano al centro, e il tasto mostra dove sono. Sposta il cursore `Dimensione`: va dall'1% al 20% del lato lungo dell'immagine, 5% di fabbrica. Salva una copia, tenendo premuto `Salva`, e apri il file: caratteri, stili e allineamento sono quelli dell'editor.

Letture mie: `Carattere` e `Allineamento` vanno a giro, perché pochi valori si scorrono bene con un tocco, e l'allineamento parte dal centro, com'era prima; il testo ha colore e dimensione suoi, distinti da quelli della linea, così dopo un rettangolo rosso non scrivi in rosso.

## 6. Il fondo: `Evidenziato` ed `Etichetta`

Con un testo bianco scelto tocca `Fondo`: passa a `Evidenziato`, il testo ha dietro una fascia gialla per riga e le parole diventano nere. Toccalo ancora: passa a `Etichetta`, una striscia viola stondata per riga, che si fondono in una forma sola con un'ombra morbida sotto, e le parole tornano bianche. Un terzo tocco toglie il fondo.

Con un fondo acceso, sotto il cursore compare la fila dei suoi sei colori: si offrono solo quelli che staccano dall'immagine sotto il testo e lasciano leggibili le parole, gli altri sono sbiaditi e non si scelgono. Su una pagina bianca, con `Evidenziato`, il bianco è spento e il giallo no. Scrivi un testo con `Etichetta` in ciascuno dei quattro caratteri, Literata compresa: la riga è al centro della striscia in tutti e quattro.

Letture mie: le parole bianche o nere prendono, cambiando fondo, quella delle due che si legge meglio sul fondo nuovo, mentre un colore diverso resta, se si legge; un fondo stacca dall'immagine per distanza dei colori, che conta anche la tinta, e le parole si leggono per contrasto di luminosità, quindi il giallo su una pagina bianca si offre; la centratura della riga sulla striscia è una regola per tutti e quattro i caratteri, la metà della H sopra la linea di base, invece di una correzione per la sola Literata (la causa è nelle metriche di Literata, che dichiara un'ascesa di 1177 unità per maiuscole di 700).

## 7. Lo strumento Pillola

Tocca il settimo strumento, `Pillola`: i tondi dei colori, il cursore `Dimensione` e `Fondo` sono spenti, perché la pillola ha il suo aspetto fisso. Trascina il dito: disegni la pillola come un rettangolo, con gli stessi appoggi ai bordi e agli altri elementi; alzando il dito si apre la finestra delle parole. Scrivi una frase lunga e tocca `Applica`: le parole sono bianche, vanno a capo da sole e prendono la misura più grande che la pillola contiene. Tira una maniglia: le parole si riadattano. Tocca `Allineamento`: le righe passano a destra e a sinistra anche qui.

Tocca un punto vuoto con `Pillola`: dopo le parole compare una pillola che le contiene, alla dimensione scelta per il testo. Controlla l'aspetto su una zona chiara e su una scura: riempimento rosso traslucido, una traccia chiara dentro e una scura sul bordo, e sotto l'immagine sfocata come un vetro.

Letture mie: la Pillola è uno strumento a sé, il settimo, perché la chiami strumento; le due tracce sono dentro il riquadro disegnato, così la pillola si appoggia come un rettangolo; i 12 px di sfocatura sono il 2,2% del lato maggiore della pillola, cioè 12 px su una pillola larga mezzo telefono, così l'anteprima e il file sfocano allo stesso modo; il minimo di leggibilità è il 2% del lato lungo dell'immagine, e parole che non ci stanno nemmeno al minimo allungano la pillola verso il basso, invece di sparire tagliate; carattere, stili e allineamento si cambiano coi tasti del testo, e `Annulla` nella finestra lascia la pillola vuota.

## 8. Le sfumature senza bande

Col tema scuro apri una cartella con fotografie scure e lisce in fondo allo schermo (un cielo di sera, un muro in ombra) e guarda la sfumatura che sale dal fondo sopra il menu: deve passare dal fondo alla fotografia senza gradini, anche da vicino. Poi tieni premuta una miniatura per scegliere più immagini, scorri fino a fotografie chiare e guarda l'ombra che scende sulla griglia sopra la scheda della selezione: nemmeno lì devono vedersi bande. Guarda anche il gradiente dell'intestazione di una cartella col colore, come prima.

Fino alla 4.81 le due fasce in fondo allo schermo e l'ombra della selezione si dipingevano senza il rumore che toglie le bande, che aveva solo l'intestazione: avevo calcolato che non servisse, e il conto era sbagliato, perché sopra una fotografia i gradini dipendono dalla differenza fra il fondo e la fotografia. Adesso tutte e quattro passano dallo stesso rimedio. Se vedi ancora bande, allega una schermata: dai pixel si capisce se il rumore arriva allo schermo.

## Etichette testuali

### e-draw_text · Gli strumenti Testo e Pillola
Testo · Pillola
<!-- chiavi: draw_text draw_pill -->
<!-- I nomi che il lettore di schermo legge sui due strumenti nuovi, e il titolo della finestra delle parole. -->

### e-draw_face · I tasti del testo
Carattere · Grassetto · Corsivo · Barrato · Fondo · Modifica testo · Allineamento
<!-- chiavi: draw_face draw_bold draw_italic draw_strike draw_ground draw_text_edit draw_align -->

### e-draw_center · Dove sono le righe
Sinistra · Centro · Destra
<!-- chiavi: draw_center -->
<!-- Il lettore di schermo legge 'Allineamento: Centro'. Sinistra e Destra sono le parole del lato preferito nelle impostazioni, già approvate. -->

### e-draw_highlight · I due fondi del testo
Evidenziato · Etichetta
<!-- chiavi: draw_highlight draw_label -->
<!-- `Etichetta` è il nome dello stile che hai scelto (N1). -->

### e-draw_size · Il cursore del testo
Dimensione

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La forma della Sfocatura e il tocco che alterna `Trasforma` e `Ruota` | 4.81-01, 4.81-02 | Non provato | | Attendere il collaudo. |
| Guide, Testo, caratteri, fondi, Pillola e sfumature | da 4.90-01 a 4.90-06 | Non provato | | Attendere il collaudo. |
| Il menu della pressione lunga, `Copia` e `Incolla`, le maniglie e `Ruota` | 4.70-01, 4.70-02, 4.70-03, 4.70-04 | OK | Un tocco sull'elemento scelto alterna `Trasforma` e `Ruota` | Fatto nella 4.81 (prova `4.81-02`). |
| Lo strumento Sfocatura | 4.80-01 | Non approvato | Il rettangolo sfocato diventava un parallelogramma | Corretto nella 4.81 (prova `4.81-01`). |

## Prossimi passi

- **In collaudo**: la Sfocatura e il tocco sull'elemento scelto (`4.81-01`, `4.81-02`), le guide (`4.90-01`), il Testo con l'a capo (`4.90-02`), caratteri e allineamento (`4.90-03`), i fondi (`4.90-04`), la Pillola (`4.90-05`), le sfumature senza bande (`4.90-06`); le etichette testuali.
- **Dopo**: gli stili, nella 4.95, con uno o due rilasci di assestamento; poi la 5.00.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
