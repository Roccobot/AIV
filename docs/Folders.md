# Folders.md: le funzioni delle cartelle di AIV

> **Cos'è questo file.** Le specifiche delle funzioni che riguardano le cartelle e le griglie di
> AIV: che cosa fa ognuna, le decisioni dell'utente su di lei e le sue trappole. Non si carica da
> solo: si legge per intero prima di toccare una di queste funzioni. Le regole del repo vivono in
> `Rules.md`, e un rimando a una sua sezione indica il nome del file.

## 🖼️ Intestazione delle cartelle, e le due schermate che la mostrano

**Com'è fatto.** L'intestazione vive nella schermata iniziale e in ogni cartella, e quello che
condividono vive in `Front.kt`: la frazione di schermo, la fascia che si chiude, lo scorrimento che
la chiude prima che l'elenco si muova, le sfumature in fondo. I numeri li ha dettati lui, e sono una
decisione per l'app: copiati in due schermate divergerebbero al primo ritocco.

- **Il nome della cartella è `titleLarge`**, più piccolo di `headlineSmall` della testata: con lo
  stesso corpo la traslazione non avrebbe niente da raccontare. I nomi lunghi vanno a capo
  (`FRONT_TITLE_LINES`), che è il modo in cui lascia spazio ai nomi lunghi. L'icona è a
  `FRONT_INK`, 0,3.
- **La traslazione del titolo è la parallasse della fascia**: la fascia posa il contenuto centrato
  in quel che resta, quindi chiudendosi lo alza verso la testata. Le due copie del nome si
  scambiano con opacità complementari, senza un punto della corsa in cui il nome si legga meno che
  agli estremi.
  - ⚠️ **Il nome è due volte nell'albero semantico**, e nessuna delle due copie si può togliere; chi
    vuole chiudere quel buco usa l'`alpha` semantico.
- **Quando è aperta** (sue risposte): mentre la griglia carica è aperta; tornando dal
  visualizzatore resta com'era. Le regge una regola sola: con la griglia scorsa la fascia non è
  aperta. Col dito è vero per costruzione, e la regola è scritta per il salto programmato
  all'immagine da cui si torna, che non passa dallo scorrimento annidato.
- ⚠️⚠️ **Durante una selezione la fascia non si chiude** (sua richiesta): la chiusura automatica
  faceva scorrere la griglia sotto un dito appoggiato, e il gesto da/a della selezione prendeva
  altre miniature. Si chiude in un modo solo, scorrendo. La ragione per cui si chiudeva (il conto
  dei selezionati in testata) è caduta, perché il conto vive sotto il titolo.
- ⚠️⚠️ **Il conto sotto il titolo dice 'elementi' e non 'immagini'** (ci sono anche i video),
  centrato, più piccolo e meno opaco (sua specifica); con una selezione dice 'N elementi
  selezionati' (`pick_count`).
  - ⚠️⚠️ **`folders_count` non si riscrive**: la schermata iniziale conta con lei le sole immagini
    (`bucket.pictures`, accanto a `folders_clips` per i video), e là 'immagini' è giusto. La
    griglia usa una chiave sua, `items_count`, perché i due scopi sono diversi e un ritocco a uno
    cambierebbe l'altro in silenzio.
- ⚠️ **L'icona si rimpicciolisce misurandola, e cede lo spazio**: scalata con `graphicsLayer`, il
  titolo sotto non salirebbe. **Sbiadisce dall'istante in cui comincia a stringersi**, con una
  rampa lineare (una curva che parte con pendenza zero nasconderebbe l'inizio della dissolvenza,
  che è quello che ha chiesto di vedere); la soglia si ricava dalla misura, cioè è l'apertura a
  cui lo spazio concesso all'icona vale il suo lato massimo.
- ⚠️ **Né il cestino né la ricerca hanno l'intestazione, ed è una scelta non rivista**: la ragione
  del cestino (un FAB che non poteva perdere la sfumatura) è decaduta quando il FAB ha preso a
  passare sopra le sfumature; nella ricerca la testata contiene un campo di testo.
- **Le sfumature in fondo sono due, sovrapposte**: la fascia grande, che non arriva mai al pieno, e la
  coda corta, che ci arriva subito (`FOOT_SOLID`, `FOOT_REACH`). Nella schermata iniziale e in cima a
  ogni cartella ci sono tutte e due; scorrendo una cartella se ne va la coda corta, insieme al titolo,
  e la fascia grande resta sempre, salvo quando la griglia tocca il fondo (sua correzione su
  `4.25-07`, dalla `4.30`; nella `4.25` era il rovescio). Dalla `1.85` alla `4.20` nelle cartelle la
  coda non c'era.

**La variante 10** (sua risposta a `d-frontespizio`).
- **Si compone di quattro interruttori** sotto 'Intestazione delle cartelle', in 'Aspetto':
  `frontWash` (il gradiente), `frontSerif` (il titolo graziato), `frontFacts` (le pastiglie del
  peso e dei video) e `frontPickAll` ('Seleziona tutto'). Di fabbrica tre accesi e `frontSerif`
  spento: è la sua risposta `tre` a `d-front-serif`, e non si tocca.
- ⚠️ **Il gradiente si accorcia per costruzione**: è dipinto dietro il blocco che si stringe, senza
  una seconda altezza calcolata a parte.
- **Il gradiente parte da `WASH_PEAK` (oggi 25%) e finisce prima della griglia**, e sbiadisce
  scorrendo (sue richieste); la storia dei suoi valori vive sulla costante. 'Titolo graziato'
  cambia solo il carattere.
- ⚠️⚠️ **La tinta vive sul blocco che contiene testata più fascia**, per la trappola di
  `drawBehind` (`Rules.md` § '🖍️ Due trappole del disegno in Compose'): su un nodo dopo la
  testata, il titolo in testata le spariva sotto. La prova guarda i pixel su una scena minima.
- **L'icona col gradiente**: in negativo sul tema chiaro (`FRONT_NEG_INK`, piena, perché una
  sagoma più trasparente sul gradiente si spegne), bianca al 20% sul tema scuro (`FRONT_DARK_INK`,
  sue misure). Il tema è quello dell'app (`LocalAivLight`, `Rules.md` § '🌗 Il tema scelto DENTRO
  l'app non è quello di sistema'), letto prima del `graphicsLayer`, dove non si è più in
  composizione.
- ⚠️⚠️ **Il gradiente arriva fin sotto la barra di sistema con una fascia piena** (sua richiesta),
  del colore della cartella, e si spegne scorrendo con la sfumatura. Una fascia piena e non un
  rettangolo più alto: allungando il gradiente il suo massimo salirebbe sopra la barra, e la rampa
  che lui ha tarato cambierebbe.
  - ⚠️ **La fascia legge `WASH_PEAK` e non una copia**, quindi la barra prende sempre il colore da
    cui la sfumatura parte. Le icone della barra non si toccano: chi alza `WASH_PEAK` guarda anche
    quelle.
- **Il dithering**: il gradiente si posa a mano (`Brush.applyTo` più `drawIntoCanvas`, con
  `isDither`), su un rettangolo più largo dello schermo, col pennello costruito sulla misura vera.
  Non è bastato (sua segnalazione, per una ragione che non si conosce), e il rumore è diventato uno
  shader per pixel con un livello di rumore triangolare (`Dither.kt`).
  - ⚠️⚠️ **Sotto Android 13 la rampa col rumore è precalcolata e stesa come maschera di sola
    opacità** (`rampMask`, una tessera larga 128), ed è la sua risposta `copri` a
    `d-dither-vecchi`: uno shader scritto a mano vuole `RuntimeShader`. Il colore lo mette il
    paint, e il rumore sull'opacità si taglia la dose da sé (`GRAIN_STEPS`). ⚠️ **Nessuno dei due ha
    un telefono sotto la 13 per guardarla**: quel ramo lo presidia il banco.
  - ⚠️ **`BandeTest` guarda la tessera e non il disegno**: nel disegno entra un rumore di Skia che
    non si distingue dal nostro, e la prima stesura restava verde col rumore azzerato. Il secondo
    caso misura che la maschera si tinga col colore del paint.
- **Le pastiglie dei dati costano una query** (`Folder.weigh`) e compaiono solo se il dato esiste;
  le quattro hanno lo stesso vestito neutro, e la terza conta le immagini (sua risposta `immagini`
  a `d-front-altro`). Il tocco lungo sul peso entra in selezione con tutto, quello sui video coi
  soli video (sue richieste); 'Seleziona tutto' dice 'Deseleziona' dopo il primo tocco.
- ⚠️ **Quando la fila va a capo si allinea al lato del FAB** (`fabEdge`, sua richiesta), così la
  riga che va a capo è sotto il pollice. Vale solo a capo, perché con le pastiglie su una riga la
  fila si dimensiona sul contenuto (lo ha misurato il banco). ⚠️ **Caso limite da guardare sul
  telefono**: su uno schermo molto stretto o coi caratteri grandi 'Seleziona tutto' può non
  vedersi, perché la fascia la ritaglia, ed è la ragione per cui la prova che lo misurava è stata tolta.

**I quattro gesti** (suoi): il tocco sul nome lo copia, il tocco lungo rinomina la cartella; il
tocco sull'icona sceglie la copertina (§ '🖼️ La copertina scelta a mano'), il tocco lungo sceglie
il colore del gradiente per quella cartella fra sedici tinte in una griglia 4x4.
- ⚠️ **Il gesto che apriva il gestore file di sistema non c'è più**, e con lui è stato tolto il codice
  che lo serviva (`Folder.openInFiles` e le sue stringhe).
- ⚠️⚠️ **La rinomina è la stessa finestra del file singolo** (sua istruzione): `RenameDialog` col
  parametro `folder`, che vuole il nome intero nel campo, nessun comando sotto, e un'estensione
  eventuale dentro il campo.
- ⚠️⚠️ **Rinominare cambia il `BUCKET_ID`**, che è il CRC del percorso: la griglia si riapre sulla
  cartella nuova, o mostrerebbe una cartella vuota senza errori, e copertina e tinta viaggiano con
  lei. Il disco si rinomina con `File.renameTo` e il MediaStore si aggiorna con
  `MediaScannerConnection`, o la cartella nuova resterebbe invisibile fino al giro
  dell'indicizzatore di sistema.
- ⚠️ **I gesti vivono sul nome grande, non sulla copia in testata**, che a fascia aperta è
  trasparente ma raggiungibile.

**Le sedici tinte.**
- ⚠️⚠️ **Ogni tinta è una coppia, una per tema** (sua richiesta), con un contrasto minimo dal
  fondo; nel selettore i tondi sono tagliati in due (sopra il chiaro, sotto lo scuro) in tutti e due
  i temi. La scelta resta un indice, e a cambiare col tema è come quel colore si scrive.
- ⚠️⚠️ **Le sedici coprono la ruota a passo uniforme in OkLCh** (sua istruzione: c'erano troppi
  verdi), non in HSL, che addensa i verdi e dirada i blu. I bersagli di luminosità sono misurati
  sulla tavolozza che aveva approvato, così cambia la distribuzione e non il carattere. Il
  grigio-blu non c'è più, e le cartelle già tinte hanno cambiato colore: non si evita rinumerando.
  Criterio, fondi e conti vivono in `FolderTint.kt`.
- ⚠️ **Una cartella cancellata non si rincorre** (sua istruzione): l'archivio delle tinte non si
  pota, e il perché vive su `FolderTints`.

## 📱 Schermo largo e tablet in verticale: dove vanno intestazione, elenco e FAB

Dalla `3.70` (richiesta dell'utente del 2026-10-04, mockup `Tablet_H` e `Tablet_V`, scelte B1-B7).
La forma dello schermo la decide `Adaptive.shape`: telefono vuol dire lato minore sotto i 600 dp.

- ⚠️⚠️ **Telefono: tutto schermo in orizzontale in tutta l'app, e in verticale nel solo
  visualizzatore** (B1, scelte B2 e A4; il verticale dalla `3.71`, voce `3.70-01`), con le barre di
  sistema nascoste e richiamabili con uno scorrimento dal bordo. La regola vive in
  `Adaptive.immersive`.
  - ⚠️ **Dove l'app non è a tutto schermo, la griglia conta le barre anche mentre sono nascoste**
    (`steadyDrawing`, dalla `3.73`, voce `3.72-01`): uscendo dal visualizzatore le barre ricompaiono
    con un'animazione, e uno spazio che le seguiva spostava la griglia a ogni fotogramma.
- ⚠️⚠️ **Schermo largo (telefono e tablet in orizzontale)**:
  - la griglia non ha la fascia dell'intestazione: in testa ci sono nome e numero di elementi su
    una riga, e il nome si accorcia per primo;
  - la colonna delle cartelle ha l'elenco ancorato in basso, dal 40 al 70% dell'altezza secondo
    quante cartelle ci sono, senza maniglia; sopra c'è la testa, con la sfumatura, l'icona e le
    pastiglie dell'intestazione (tutto tranne il titolo), e nella schermata iniziale l'identità
    dell'app;
  - dalla `3.71` nella testa le pastiglie sono centrate con 8 punti ai lati, la sfumatura finisce
    entro 200 punti (il banding sul tablet), e sparisce dove il foro della fotocamera lascia una
    fascia vuota dal lato della colonna; sul telefono l'identità di casa è coricata, con l'icona
    accanto al nome e allineata a sinistra (`IdentityRow`, voce `3.70-05`), e dalla `3.72` il nome è
    piccolo, al 70%, su due righe fisse ('Astonishing' e 'Image Viewer') e senza la firma, e dalla
    `3.73` l'icona comincia dove cominciano le cartelle sotto (`RailIdentity`, voce `3.72-04`);
  - la colonna la compone la griglia, perché 'Seleziona tutto' agisce sulla sua selezione;
  - i comandi del FAB sono in una pillola d'accento verticale a destra, agganciata in basso dalla
    `3.71` (voce `3.70-04`): in una cartella Cerca, Cestino e Impostazioni; nel cestino le sue tre
    voci; nella schermata iniziale tutte le voci del FAB di casa, scritte una volta sola in
    `hubEntries` e lette anche dal menu del FAB;
  - nella schermata iniziale l'invito `Scegli una cartella dalla barra di navigazione` è al 50% e
    centrato nell'area utile, cioè senza la fascia della pillola (`3.70-05`, `3.70-06`).
- ⚠️⚠️ **Tablet in verticale**: la colonna resta com'è, spostabile, ma senza Cerca, Cestino e
  Impostazioni in fondo (B7); quei comandi sono in una pillola compatta in basso a destra, con le
  sole tre icone (dalla `3.71`, voce `3.70-07`; nella `3.70` aveva in testa il campo 'Cerca in
  ...'). Nel cestino la pillola ha le sue tre voci. Dalla `3.71` la maniglia è al centro, con due
  righe tenui al posto dei puntini (dalla `3.73` a metà dell'opacità di prima, voce `3.72-02`), e le
  righe dell'elenco hanno 20 punti ai lati. ⚠️ **La maniglia è
  sopra l'elenco, mai sotto** (voce `3.71-06`): nella `3.71` era in fondo, e là il gesto di sistema dal
  bordo inferiore vince sul trascinamento.
- ⚠️ **Dove c'è la pillola il FAB non c'è**, e con lui il salto in cima e in fondo (B4). In
  selezione la pillola sparisce come il FAB, perché le azioni sono nella scheda in basso.
- ⚠️ **La vista ad albero resta fuori**: ha una navigazione sua, e lì il FAB resta.
- ⚠️⚠️ **Telefono in verticale, dalla `4.00`: la pillola può prendere il posto del FAB** (sua
  richiesta del 2026-10-05, risposte A1-D1 nel brief). ⚠️ **Dalla `4.10` la scelta è la voce
  `Elemento interattivo principale`, la prima di 'Pulsanti e indicatori'** (nota E del giro della
  `4.04`), solo sul telefono in verticale (risposta G4): gettoni `Tasto fluttuante` (di fabbrica fino alla `4.20`) e
  `Pillola di icone`, e per la pillola una seconda fila, `A scomparsa` ed `Estesa`. Titolo, gettoni
  e paragrafo sono suoi; il paragrafo nomina il menu inferiore solo dalla `4.15`, quando arriva.
  ⚠️⚠️ **Dalla `4.30` i due menu si chiamano `Menu` (il menu inferiore) e `Menu 'Start'` (il menu
  angolare)**, con gli apici dritti (nota A del giro della `4.25` e risposta `nome-start`); nelle
  altre lingue `Start` è il nome che ognuna dà al menu di Windows (risposta `start-lingue`). Nelle
  voci qui sotto restano i nomi di allora.
  Chiave `main-control`; un archivio o un file di impostazioni con l'interruttore della
  `4.02`-`4.05` (`phone-pill-on`) o col solo gettone della `4.00`-`4.01` si rilegge col suo
  significato.
  - ⚠️⚠️ **Dalla `4.15` c'è il terzo gettone, `Menu inferiore`** (nota D del giro della `4.04`,
    risposte G5, M1 e M2), e il paragrafo è completo con le sue parole. La seconda fila vale anche per
    lui, coi gettoni `A scomparsa` e `Fisso`, sullo stesso valore della pillola (`phone-pill`):
    - è una barra a tutta larghezza in fondo allo schermo, sotto la linea dei gesti, che sopra di lei
      diventa bianca; si sovrappone alla griglia, e l'ultima riga ha lo spazio per salirle sopra;
    - `A scomparsa`: a riposo resta nell'angolo del lato preferito una pillola verticale di due
      tasti, `in cima` sopra e il marchio sotto (col tocco lungo del FAB); il marchio apre la barra,
      che entra dal basso in 110 ms con la curva della pillola, con la × nell'angolo; la × e
      Indietro la richiudono, e una voce la richiude prima di agire;
    - `Fisso`: la barra è sempre aperta, senza × e senza pillola verticale;
    - ⚠️ **scorrendo, i due tasti nell'angolo diventano il salto** col meccanismo della pillola
      estesa: nella pillola verticale il marchio diventa `in fondo` (sopra resta `in cima`), nella
      barra i due tasti dell'angolo (la × e la voce accanto, o le ultime due voci nel fisso)
      diventano `in cima` e `in fondo`;
    - l'aspetto è quello dei pulsanti principali.
  - ⚠️⚠️ **Col lato preferito a sinistra, dalla `4.15`, l'ordine è il rovescio di quello di destra**
    (sua richiesta): Impostazioni, Cestino, Apri un indirizzo, Cerca, Mostra nascoste, poi le due viste.
    ⚠️ **Dalla `4.20` a specchio anche le due viste** (suo commento a `4.15-04`), che nella `4.15`
    restavano nel loro ordine (`mirrored` in `PhonePill.kt`). Vale per la pillola a scomparsa, per
    quella estesa e per il menu inferiore; il menu angolare specchia ogni riga.
  - ⚠️⚠️ **Il menu inferiore con 2, 3 o 4 tasti, × compresa, li raccoglie sul lato preferito**, a
    24dp dal bordo come le pillole (dalla `4.25`; 16dp prima) e con la spaziatura della pillola; da 5 in su li distribuisce
    su tutta la larghezza (sua nota N2 del giro della `4.15`, dalla `4.20`). Nel cestino, a destra:
    Cronologia, Ripristina tutto, Svuota cestino, ×.
  - ⚠️⚠️ **Dalla `4.20` c'è il quarto gettone, `Menu angolare`** (nota D del giro della `4.04`,
    decisioni G1 e C1-C3), sempre a scomparsa e quindi senza la seconda fila. Il marchio apre un
    pannello d'accento che cresce dal suo angolo, sopra la linea dei gesti (che non cambia colore), con
    caselle grandi quanto i tasti della pillola, e **dalla `4.25` si richiude dopo ogni tocco** (R1 del
    giro della `4.20`):
    - ⚠️⚠️ **a riposo, dalla `4.25`, è un tasto tondo solo** (R2), che scorrendo si allunga in su nella
      pillola verticale dei due salti, `in cima` sopra e `in fondo` al posto del marchio, col ritmo
      del salto (R4); oppure, scelta dal commutatore, la pillola verticale del menu inferiore com'era
      nella `4.20` (R3, `corner-round`);
    - nella schermata iniziale un 3x3: il commutatore e le due viste diverse da quella in cui sei;
      'Mostra nascoste' (spenta senza nascoste; senza permesso al suo posto 'Seleziona immagine'),
      Cerca, Indirizzo; Cestino, Impostazioni, ×. Il commutatore (R3, dalla `4.25`) mostra il riposo che
      il tocco mette, con le sue due icone uniformate alle altre (`ic_rest_pill`, `ic_rest_round`);
      nella `4.20` la prima riga aveva le tre viste, con quella corrente segnata da un disco;
    - nelle cartelle e nel cestino un 2x2: Cerca, Cestino; Impostazioni, × (nel cestino le sue tre
      voci);
    - la × prende il posto del marchio, e scorrendo la × e la casella sopra di lei diventano `in fondo`
      e `in cima`; a sinistra ogni riga è a specchio.
  - ⚠️⚠️ **Con sette voci, il massimo, la pillola estesa occupa tutta la riga fra i due margini**
    (sua regola, dalla `4.15`): le celle si allargano, i tasti no, e la quarta voce, `Cerca`, cade sulla
    linea di mezzo dello schermo. ⚠️ **Dalla `4.25` anche quella a scomparsa, aperta**, con la × fra le
    otto celle (nota A del giro della `4.20`, scelta A2 dopo l'anteprima); nella `4.15` valeva solo per
    l'estesa.
  - ⚠️⚠️ **Dalla `4.30` il tondo è uno solo** (nota B del giro della `4.25`): 44dp anche quando la
    pillola aperta stringe i tasti (nella home, su un telefono stretto, era più piccolo), e lo
    stesso angolo in home e nelle cartelle (`pillCorner`: 24dp di fianco, 16 da sotto; la griglia
    ne aveva 20). Il menu Start aperto si chiude con **qualunque** tocco fuori, anche un
    trascinamento sulla griglia, che intanto scorre (`OutsideTouch`, sua nota su `4.25-02`).
  - ⚠️⚠️ **Dalla `4.31` il centro del tondo è il punto di riferimento di ogni comando** (sua regola
    dopo la `4.30`: *tutti i posizionamenti di tutti i menu devono usare quel punto di riferimento.
    Anche il centro del FAB dev'essere centrato sul centro di quel tondo*). La posizione resta quella
    della `4.30`, la più bassa delle due della `4.25` (sua risposta: il salto l'aveva visto sulla
    `4.25`). Il FAB da 40dp ha lo stesso centro, quindi sta a 26dp di fianco e a 18 da sotto (prima
    16 per 16 nella home, 16 per 20 nelle cartelle); il menu del FAB ha il fianco a 24dp come il menu
    Start aperto (`cornerSide` in `BottomMenu.kt`), e i veli d'aiuto usano lo stesso angolo
    (`pillCorner`). ⚠️ La barra del menu inferiore resta una fascia in fondo: con 2, 3 o 4
    tasti quello d'angolo è sull'asse verticale del tondo, ma non alla sua altezza.
  - ⚠️⚠️ **Dalla `4.31` il menu Start è il comando di fabbrica** (sua risposta in chat dopo la
    `4.30`; nella `4.25` e nella `4.30` era la pillola a scomparsa). Vale per un telefono nuovo:
    chi ha già l'app ritrova la sua scelta. Al primo avvio il velo d'aiuto lo mostra **aperto**, con
    le sue caselle disegnate e la copia del tondo nell'angolo, e la frase `Tutti i comandi sono nel
    menu 'Start'...` (`corner_hint`, sua richiesta: *magari espanso*).
  - ⚠️⚠️ **Pillole e tasto tondo stanno a 24dp dal vetro di fianco (`PILL_SIDE`), dalla `4.25`**, tre
    margini della griglia e lo stesso rientro dei menu (scelta A2): la riga resta dentro i bordi delle
    miniature, e il tondo coincide col bordo della pillola aperta. Da sotto restano i margini del FAB,
    e dalla `4.31` il FAB ha il centro sul tondo (voce qui sopra).
  - Storico: dalla `4.02` alla `4.05` era l'interruttore `Pillola al posto del FAB in verticale`,
    in cima a 'Pulsanti e indicatori' fino alla `4.03` e in fondo a 'Tema e dettagli grafici'
    dalla `4.04`, con sotto le due file di gettoni.
  - `A scomparsa`: il FAB diventa tondo e color accento, e al tocco si allunga in pillola
    orizzontale con tutte le voci del menu e la × semitrasparente nell'angolo, in 160 ms con una
    curva che parte velocissima e rallenta; la × la richiude, e così Indietro. Il tasto tondo
    tiene il salto (il chevron mentre si scorre) e il tocco lungo del FAB (risposta C1);
  - `Estesa`: niente FAB, niente ×, niente menu. Scorrendo la pillola si piega a due tasti: i due
    verso il lato preferito diventano `↑` (interno) e `↓` (nell'angolo), con l'animazione del
    salto, e la pillola torna larga con gli stessi tempi del chevron che rientra (risposta C2).
    Il tocco lungo delle colonne qui non c'è (risposta B1);
  - ⚠️ **Nel tasto tondo il marchio è largo 22dp e non 24, e al centro c'è la A**, non il
    baricentro del disegno (sua nota, giro della `4.01`): il disco solare si sposta con lei.
    ⚠️ Sul telefono vale dalla `4.03` (`4.02-03` non approvata): il tondo vero lo disegna
    `SlidePill`, che nella `4.02` non lo diceva al marchio;
  - ⚠️⚠️ **Di che cosa sono fatti, dalla `4.10`, lo dice la voce `Aspetto dei pulsanti principali`,
    in fondo a 'Tema e dettagli grafici' e su ogni apparecchio** (nota E): `Solido` (di fabbrica fino
    alla `4.20`),
    `Trasparente` (colore all'80%), `Traslucido` (fino alla `4.05` `Vetro`: sfocatura di
    deviazione 8 e saturazione al 160% come la pillola del DF, colore all'80% sul tema chiaro e al
    65% sullo scuro dalla `4.01`). Da Android 11 in giù il traslucido non compare. ⚠️ **Dalla `4.20`, col
    traslucido scelto, compaiono quattro cursori con un'anteprima dal vivo** (sua nota N1 del giro
    della `4.15` e sua correzione: *serve un elemento traslucido che si aggiorna in tempo reale*):
    Raggio (della sfocatura, 13dp di fabbrica), Intensità (saturazione di quello che c'è dietro, 160%),
    Colore (quantità di accento rispetto al tono del tema, 100%), Luminosità (bianco sopra zero, nero
    sotto, 0). La lettura dei quattro è dichiarata nel DF; si nascondono spegnendo `GLASS_TUNING`, e i
    valori restano quelli scelti. ⚠️⚠️ **Dalla `4.25` i valori di fabbrica sono i suoi** (nota D del giro
    della `4.20`): pillola a scomparsa e traslucido di fabbrica, Raggio 16dp, Intensità 300%, `Opacità`
    (il cursore che si chiamava Colore) 20%, e `Scostamento` 35, senza segno, che scurisce sul tema
    chiaro e schiarisce sullo scuro (risposta C2) al posto della Luminosità col segno. Sotto i cursori
    ci sono `Colore chiaro` e `Colore scuro` (risposta B3): un tondo che apre un selettore di tonalità,
    saturazione e luminanza, con `Predefinito` che torna all'accento del vetro in quel tema; e
    `Ripristina` riporta tutto ai valori di fabbrica, che chi aveva già l'app non vede da sé.
    L'anteprima ha un bordo d'accento di 5dp dentro l'immagine, sopra le righe (punto B).
    ⚠️⚠️ **Dalla `4.30` sotto la copia sfocata c'è il fondo della pagina** (nota E del giro della
    `4.25`): testi e bordi, disegnati sul vuoto, si sfocavano in un alone trasparente e sotto si
    vedevano gli originali nitidi. E col traslucido il glifo del FAB ha un colore fisso, anche da
    premuto: `#ecfff7` sul tema chiaro, `#004247` sullo scuro (nota F, valori corretti in chat).
    Vale per il FAB
    (anche premuto, mentre passa all'accento dell'altro tema), per la pillola del telefono, per il
    menu inferiore e per le pillole degli schermi larghi e del tablet, anche dove la forma è imposta
    (suo commento a `4.10-02`); il FAB staccato sopra il suo menu tiene la sola tinta, perché
    sotto c'è già la sfocatura del velo. Fino alla `4.05` era la seconda fila della pillola, e il
    FAB restava pieno.
  - ⚠️⚠️ **Dalla `4.01` la pillola ha l'accento dell'ALTRO tema, coi suoi glifi** (sua scelta,
    varianti A2 e C1): la coppia del FAB, petrolio sul tema chiaro e verde acqua sullo scuro, in
    tutte e tre le tinte. Nella `4.00` era l'accento del tema, e sul vetro scuro quasi spariva.
- ⚠️⚠️ **Col vetro e la sfocatura dietro i pannelli, anche i menu sfocano come il vetro**, col velo
  nell'accento al 10% al posto del nero (sua richiesta). Dove il telefono non sfoca torna il velo
  nero.
- ⚠️⚠️ **Il tocco lungo su un tasto di una pillola mostra la sua etichetta in un fumetto, e basta**
  (risposte B1-B3), in tutte le pillole: per questo nella pillola il tocco lungo di 'Mostra
  nascoste' non c'è, e resta nel menu del FAB.
- ⚠️ **Dove la pillola c'è, la si vede in tutte le schermate che avevano il FAB** (risposta A3): la
  schermata iniziale in tutte e tre le viste, la griglia di una cartella, la ricerca e il cestino.
  Il tablet non ha la voce, perché in verticale ha già la sua pillola.

## 📐 Le griglie arrivano al vetro, anche in basso

- ⚠️⚠️ **Il rientro di sotto vive nel `contentPadding` della lista e non sul contenitore** (sua
  richiesta): `safeDrawingPadding()` sul contenitore toglie lo spazio prima che la griglia disegni,
  e sotto la barra gestuale non arriverebbe niente. Le miniature scorrono sotto la barra, e l'ultima
  riga resta raggiungibile. La misura la dà `bottomInset()`, in `Front.kt`.
  - ⚠️ **Anche il margine verticale della schermata si scompone**, o la griglia si ferma sopra il
    vetro.
- **La sfumatura in fondo arriva al vetro**, e tiene la barra gestuale su un fondo neutro.
- ⚠️ **Il FAB della griglia vive in una finestra sua** e i rientri se li mette da sé; quello della
  schermata iniziale usa `safeDrawingPadding()`.
- ⚠️ **Nessuna prova del banco**: là i rientri valgono zero. Si guarda sul telefono, con la
  navigazione gestuale e con quella a tre tasti.

## ⏫ Il salto in cima e in fondo, sul glifo del FAB

- ⚠️⚠️ **In cima e in fondo porta il FAB, e il suo glifo diventa un chevron** (sua scelta fra due
  mockup animati): niente tasti in più, niente seconda finestra, e il FAB non sparisce mai. Cambia
  il disegno dentro un tasto che c'era già, con lo stesso incrocio di zoom e dissolvenza della `×`
  a menu aperto.
  - **Il tasto è uno, e il verso lo decide lo scorrimento**: verso il fondo diventa 'Vai alla
    fine', verso l'alto 'Vai all'inizio'. ⚠️ Quello che si perde è dichiarato: i due versi non sono
    disponibili insieme.
  - ⚠️⚠️ **A tasto armato il tocco salta e non apre il menu**; l'armamento finisce un secondo dopo
    l'ultimo pixel scorso.
  - **I cinque numeri sono del mockup che ha approvato**, e vivono in `Jump.kt`.
  - ⚠️ **Il crossfade dei due glifi usa un esponente sotto uno (0,8)**: con due opacità lineari, a
    metà corsa i glifi sono al 9% e il tasto resta vuoto.
- ⚠️⚠️ **Dove il FAB non c'è, il comando non c'è**: nelle impostazioni è la sua risposta (le voci le
  trova con la ricerca e le sotto-pagine), in 'Cartelle di sistema' e nella vista ad albero è la
  conseguenza.
- ⚠️⚠️ **Il `nestedScroll` del gesto va prima di `frontScroll`**: scritto dopo, al glifo arrivava
  zero finché la fascia aveva spazio da chiudere (misurato con una spia dentro il nodo).
- ⚠️⚠️ **Il salto passa dallo scorrimento annidato come un dito**, e così l'intestazione si riapre
  in cima, che è la sua richiesta: succede perché è quello che succede già col dito.
- ⚠️⚠️ **Griglia ed elenco della home riempiono l'altezza rimanente, dalla `3.25`** (giro 3.24,
  voce `3.13-02`): in Modalità incluse le cartelle sono poche; senza `weight`/`fillMaxSize` il
  vano restava alto quanto il contenuto e il trascinamento sul vuoto non riapriva
  l'intestazione. In Modalità escluse l'elenco lungo mascherava il difetto.
  Dalla `3.37` la riapertura avviene in `onPreScroll` quando la lista è in cima (`inCima`),
  perché con poche cartelle il LazyGrid non emetteva un `onPostScroll` utile.
  - ⚠️ **I segni**: `scrollBy` conta positivo verso il fondo, il puntatore positivo verso il basso.
    Lo presidia il banco, perché un segno di troppo non lo vede nessun compilatore.
  - ⚠️ **La distanza è una stima e serve solo alla durata**: una lista pigra non sa quanto è alto
    quello che non ha composto, e la corsa si ferma al bordo lo stesso.
- ⚠️ **Scartata la guardia del mockup che escludeva la corsa dal conto del gesto**: due controprove
  l'hanno smentita (`glide` muove la lista dentro `state.scroll {}`, che non risale la catena dei
  modificatori, e una corsa verso l'inizio muove la lista nel verso che il chevron già indica).
- **`SaltiTest`** vede il passaggio dallo scorrimento annidato nei due versi, il verso che segue il
  dito, e che nella schermata vera il FAB annunci il salto solo a tasto armato; non vede la resa.
  Le due trappole del banco trovate qui vivono in `Rules.md` § '🧪 Quando si scrive una prova, e
  quando no'. ⚠️ **Il FAB non compare in una griglia montata senza destinazioni** (`FabPop`): una
  prova che lo guarda le passa almeno una destinazione.

## 🔖 Lo scorrimento di una schermata sopravvive alla schermata

- ⚠️⚠️ **La posizione di scorrimento sopravvive alla schermata** (sua richiesta), con un
  `SaveableStateHolder` in `AivApp`: `rememberLazyGridState` è un `rememberSaveable`. Scartato un
  archivio scritto a mano, che avrebbe coperto la sola griglia.
  - ⚠️ **La chiave (`Screen.saveKey`) è una stringa**, perché finisce in un `Bundle`, e contiene solo
    l'identità: non il nome di una cartella, perché una cartella rinominata è la stessa.
  - ⚠️ **Cresce di una voce per schermata visitata e non si pota**: un indice e uno scarto costano
    meno di un limite col suo sfratto.
- ⚠️⚠️ **L'apertura della fascia vive nello stesso `rememberSaveable`, come frazione e non in
  pixel**: la fascia è una funzione della posizione di scorrimento, e le due cose tornano insieme
  per costruzione (sua segnalazione: la fascia ripartiva aperta a lista scorsa). In pixel, dopo una
  rotazione direbbe un'altra apertura. La nota che voleva riaprirla alla rotazione è superata.
  - ⚠️ **Nella griglia di una cartella l'invariante resta scritto come regola** (con la lista scorsa
    la fascia si chiude), perché il salto all'immagine scorre senza scorrimento annidato.
  - **La prova è `RientroTest`**, che misura dove comincia la prima cartella prima di uscire e dopo
    il rientro.
- ⚠️⚠️ **Il salto all'immagine da cui si torna si fa solo se nel visualizzatore si è sfogliato fino
  a un'altra immagine**, e si ricorda quale immagine è già stata servita (un indice), non una
  bandierina: con lo stato che sopravvive, una bandierina non si azzererebbe più e il salto non si
  farebbe mai.

## 🎨 Dove si vede il colore di una cartella, fuori dall'intestazione

- ⚠️⚠️ **Gli stili sono quattro, scelti da lui fra i mockup** (`d-colore-come`): **filetto** (una
  riga sotto la copertina), **cornice**, **nome** (il titolo nel suo colore) e **alone** (una
  sfumatura dal bordo di sopra). I numeri sono del mockup, e il filetto è di quattro punti perché
  sotto quella misura le sedici tinte non si distinguono. ⚠️ **L'angolo piegato è stato visto e
  scartato.**
- ⚠️⚠️ **Di fabbrica c'è 'Nome'**, sua risposta dopo averli provati. Nell'elenco viene subito dopo
  'Nessuno' (sua istruzione): l'ordine dei chip è quello dell'enum, e nell'archivio vive il token,
  quindi riordinare non cambia le scelte salvate. La voce di collaudo include comunque il passo
  passo, perché senza una cartella con un colore suo non si vede niente.
- **Vale per copertine ed elenco**, con le stesse misure; nella vista 'Cartelle di sistema' non c'è
  niente da tingere, perché una tinta è appesa al `BUCKET_ID` del MediaStore.
- ⚠️⚠️ **La finestra delle destinazioni tinge** (sua risposta `tinta` a `d-dest-tinta`): chi sceglie
  dove mettere un file cerca la cartella che riconosce dal colore. Tinte e copertine si caricano
  dentro la finestra, nello stesso `produceState` delle cartelle.
- **La voce vive in 'Aspetto'**, e la gemella nel dialogo delle opzioni della schermata iniziale (la
  scorciatoia del tocco lungo sul FAB): una preferenza, una chiave, un valore di fabbrica.
- ⚠️⚠️ **Il tocco lungo sul FAB in una cartella seleziona tutto, e non ha un rovescio** (sua
  richiesta): appena c'è una selezione il FAB lascia il posto alla scheda, quindi un secondo gesto
  non arriva a nessuno. Il rovescio sono il tasto 'Tutti' col suo tocco lungo e la pastiglia
  dell'intestazione. L'ha misurato la prova, fallita alla prima corsa con *the node is no longer in
  the tree*.

## 🖼️ La copertina scelta a mano

- ⚠️⚠️ **L'immagine scelta si copia in casa dell'app, ridotta a mille pixel di lato**, ed è la sua
  condizione: la copertina deve restare anche se l'originale sparisce. Tenerne l'indirizzo sarebbe
  costato una riga, e la cartella tornerebbe alla predefinita appena l'originale si sposta. Una
  copertina si vede al massimo a cinquecento pixel.
  - ⚠️ **Il file è l'archivio**: nessuna preferenza da tenere allineata, e il nome indica l'istante,
    che tiene onesta la cache di Coil. Il perché vive in testa a `FolderCover.kt`.
- ⚠️⚠️ **Il gesto è il tocco sull'icona dell'intestazione** (sua risposta `d-copertina-come`), e
  l'immagine si sceglie dentro AIV da qualunque cartella: quello che parte è una modalità, non una
  finestra, che sarebbe una seconda galleria da tenere allineata.
  - ⚠️⚠️ **Al tocco si torna all'elenco iniziale** (sua richiesta), solo se la scelta è partita: lo
    decide `ViewerViewModel.coverAway`, che guarda `covering`. La prima volta si esce quando il
    mini-onboarding si chiude, o il velo che indica l'icona non si vedrebbe.
  - ⚠️ **La modalità vive nel modello** (`ViewerViewModel.covering`), perché fra l'inizio e la
    scelta si cambia schermata. Vale per le tre griglie, i recenti e la vista 'Cartelle di
    sistema', che consegna: una modalità che funziona in una vista su tre sembra rotta.
- ⚠️⚠️ **La copertina scelta si vede anche nella finestra delle destinazioni**, e non si vedeva: il
  parametro `covers` delle due viste aveva un valore di serie vuoto, e la finestra lo ereditava.
  Adesso non ha un valore di serie, e chi apre una vista di cartelle dichiara quali copertine mostra:
  il presidio è il compilatore, che al primo build ha preso un chiamante. Le copertine si caricano
  nel `produceState` della finestra. ⚠️ Il banco non poteva vederlo: il MediaStore di Robolectric
  è vuoto.
- ⚠️ **A dire che la scelta è in corso è la fascia `CoverInvite` in fondo**, accanto alla notifica
  di casa e sopra la transizione fra schermate, perché la scelta comincia in una cartella e può
  finire in un'altra. Non è una notifica: dice che cosa sta succedendo, e resta finché la modalità è
  viva. Il suo tasto annulla.
- ⚠️⚠️ **Lo stesso tocco sulla stessa cartella rimette la predefinita**, cioè l'ultima immagine (sua
  richiesta): il gesto è uno, e fa e disfa. **Su un'altra cartella sposta la scelta là**, ed è la
  ragione per cui la modalità conserva il bucket da cui è partita.
- ⚠️ **La voce del menu del FAB esiste ma è spenta** (`COVER_MENU_ROW`, in `FolderCover.kt`), col
  nome 'Copertina predefinita' (sua istruzione: spegnerla senza eliminarla); la prova misura il
  legame con l'interruttore. Nel menu non c'è una voce che sceglie: a scegliere è il tocco
  sull'icona.
- **Il primo tocco mostra un mini-onboarding col testo suo**, una volta sola (`Hint.COVER`). Il
  riquadro illuminato arriva da `onGloballyPositioned`, perché l'icona vive in una fascia che si
  stringe a ogni pixel di scorrimento.
- ⚠️⚠️ **Una rinomina fatta in AIV porta con sé copertina e tinta** (`FolderCovers.move`,
  `FolderTints.move`); una fatta da fuori no, perché il `BUCKET_ID` è il CRC del percorso, e per
  questo c'è la potatura.
  - ⚠️⚠️ **La potatura ha trenta giorni di grazia** (`FolderCovers.sweep`): una scheda SD smontata o
    un volume non ancora indicizzato fanno arrivare corto l'elenco del MediaStore. Un elenco vuoto
    non cancella niente (è il caso del permesso non concesso), e conta la data del file, che la
    potatura aggiorna per le cartelle vive.
- **`CopertinaTest`** vede il gesto (che convive col tocco lungo del colore), il legame fra la voce
  del menu e il suo interruttore, la fascia, la precedenza sulla predefinita e il ritorno all'elenco
  iniziale; non vede la copia dell'immagine né la vista 'Cartelle di sistema', che legge il disco.

## 🗂️ Cartelle incluse/escluse

- La pagina 'Cartelle incluse/escluse' vive nelle impostazioni delle Cartelle ed è sempre
  raggiungibile, anche con gli elenchi vuoti. I gettoni sono 'Modalità incluse' e 'Modalità
  escluse'; la seconda resta predefinita e conserva tutte le esclusioni precedenti.
- Le due liste sono indipendenti. In modalità incluse sono visibili soltanto le cartelle
  autorizzate che contengono immagini o video, senza autorizzare automaticamente i discendenti.
  Camera, Screenshots e Movies sono preautorizzate se presenti, anche sulla scheda SD. Al primo
  elenco letto col permesso, si considerano i bucket con quei nomi e i percorsi convenzionali
  (anche `DCIM/Screenshots`), comprese directory vuote. Il promemoria `included-initialized`
  impedisce di aggiungerle di nuovo dopo una rimozione; non viaggia nel file di impostazioni.
  Senza contenuto non producono una cartella nelle modalità griglia e lista.
- La vista 'Cartelle di sistema' conserva la navigazione del disco e i suoi filtri specifici,
  indipendentemente dalle due liste. La pressione lunga su una cartella apre 'Autorizza
  cartella' in modalità incluse e 'Nascondi' in modalità escluse; una voce già presente propone
  di rimuovere l'autorizzazione o 'Mostra'. Le righe segnano 'autorizzata' o 'nascosta'.
  - ⚠️ **Su tablet e in orizzontale resta a una colonna, col FAB** (sua risposta del 2026-10-04,
    dopo la `3.73`): non prende la colonna delle cartelle né la pillola delle altre viste, perché
    ha una navigazione propria (`dualFolders` in `ViewerActivity.kt`).
- ⚠️⚠️ **Dalla `3.27`, il tocco lungo su un media entra in selezione multipla** (giro 3.24,
  campo libero): stessa scheda della griglia/lista ([PickSheet]), non più il riquadro centrato
  su un solo file. Indietro o 'Nessuno' azzerano; cambiando cartella la selezione si perde.
  Il FAB della casa si nasconde mentre la scheda è aperta.
- 'Autorizza cartella' apre 'Aggiungi alle cartelle visualizzate'. Una cartella senza media si
  può autorizzare, con l'avviso enfatizzato che apparirà quando conterrà un'immagine o un video.
  L'elenco nelle impostazioni permette di rimuovere ogni autorizzazione.
- ⚠️⚠️ **Dalla `3.27`, in modalità incluse Impostazioni → Cartelle ha 'Aggiungi cartella'**
  (resto di `3.13-03`): apre l'albero destinazione e autorizza la cartella scelta.
- 'Nascondi' in modalità griglia e lista mantiene l'esclusione ricorsiva. La conferma offre
  'Applica a tutte le cartelle allo stesso livello' quando il genitore contiene almeno un'altra
  directory non nascosta: si contano anche le directory vuote sul disco. Una seconda conferma
  dice che nasconde le sottocartelle attuali e che quelle nuove saranno inizialmente visibili.
  Le sottocartelle sono voci separate, rimostrabili una per una.
- 'Nascondi' dalla vista di sistema apre 'Quali cartelle vuoi nascondere?': con almeno due figli,
  '<X> e le sue sottocartelle', 'Solo le sottocartelle di <X>', 'Solo <X>'; con un figlio,
  'Solo <X>' e '<X> e la sua sottocartella'; senza figli, conferma della sola cartella.
  'Solo <X>' nasconde soltanto il contenuto diretto, lasciando visibili i discendenti attuali e
  futuri. La pagina delle impostazioni indica 'Solo questa cartella' su queste esclusioni.
- ⚠️⚠️ **Dalla `3.26`, nascondendo X con le sue sottocartelle si registrano anche i figli
  presenti** (giro 3.24, `3.13-08`): compaiono in elenco e portano la dicitura 'nascosta' nella
  vista di sistema. La scheda tiene Annulla a sinistra e l'azione principale a destra; le scelte
  intermedie sono tasti pieni allineati a sinistra.
- Il confronto comune è `FolderSelection`: nascoste ricorsive in `hidden-relative`, esclusioni
  esatte in `hidden-only`, autorizzate in `included-relative`, modalità in `folder-mode`.
  I percorsi usano `portablePath`, conservando l'identità della scheda SD.
- Destinazioni, ricerca e generazione delle miniature rispettano la selezione. 'Sfoglia tutte
  le cartelle...' conserva l'accesso al disco, anche scegliendo la destinazione dalla vista di
  sistema: in modalità incluse l'elenco filtrato viene prima della navigazione completa. In modalità incluse 'Mostra nascoste' manca e
  l'eventuale prestito si spegne al cambio di modalità.
- `CartelleIncluseTest`, `CartelleDialoghiTest`, `CartelleMediaTest` e `BackupTest` misurano filtro, percorsi, figli
  futuri, directory vuote, destinazioni, conferme e importazione. L'indicizzazione reale del
  MediaStore e la resa dei testi richiedono il collaudo sul telefono.

## 👁️ 'Mostra nascoste', e perché dura un minuto

- ⚠️⚠️ **La specifica è sua, alla lettera**: nel menu del FAB della schermata iniziale 'Mostra
  nascoste' rende visibili le cartelle nascoste per un minuto; alla scadenza tornano nascoste con
  la notifica 'Cartelle di nuovo nascoste.', il cui 'Annulla' proroga di un altro minuto; in scena
  la voce diventa 'Nascondi cartelle' con un altro glifo; le cartelle in prestito hanno il 70% di
  opacità e il segno `∅` d'accento in alto a destra; il tocco lungo sulla voce apre un pannello con
  le nascoste, ripristinabili come nella pagina 'Cartelle nascoste'.
- ⚠️⚠️ **Il minuto vive nel modello** (`ViewerViewModel.peek`): le nascoste si mostrano per
  entrarci, quindi fra l'accensione e la scadenza si cambia schermata. Non si salva. La notifica è
  quella di casa, e spegnere a mano non notifica.
- ⚠️ **La voce c'è se e solo se una cartella è nascosta**: senza, accenderebbe un minuto in cui non
  compare niente.
- ⚠️⚠️ **Il tocco lungo su una cartella in prestito propone 'Mostra'** (sua segnalazione): il verso
  lo decide il fatto, perché una cartella in scena può essere nascosta solo durante il prestito, e
  non un secondo stato. I testi di 'Nascondi' includono il nome fra apici e dicono 'un'impostazione
  di visualizzazione' (sue frasi).
- ⚠️ **Il pannello riusa le due stringhe della pagina delle impostazioni**, e non aggiunge uno
  scorrimento: `Sheet` scorre già, e due scorrimenti verticali annidati sono un errore che Compose
  segnala.
- ⚠️ **I due glifi sono suoi, una cartella con l'occhio aperto e sbarrato**, non l'occhio di
  Material, che dice 'vedi' senza dire di che cosa. L'angolo che lui non era riuscito ad
  arrotondare prende il raggio degli altri angoli dello stesso disegno.
- ⚠️ **La conferma dice 'nessun file sarà eliminato'** (sua istruzione).
- ⚠️⚠️ **Una cartella nascosta non è una destinazione** (sua istruzione): il filtro è
  `Folder.Bucket.isHidden`, in `Folder.kt`, uno per le due schermate che fanno la domanda.
  - ⚠️⚠️ **Durante il minuto di prestito anche le destinazioni la mostrano** (suo riscontro): il
    prestito si accende per entrare in una cartella nascosta, e copiarci dentro è quello che si
    vuole fare mentre dura. Arriva con `LocalPeek`, che non è statico perché cambia due volte per
    prestito; l'elenco si fotografa all'apertura, e il cestino resta fuori (caso 5 di
    `DestinazioniTest`).
- ⚠️⚠️ **Una voce è un percorso senza la radice dell'archivio del telefono** (sua richiesta: una
  voce deve valere anche su un altro telefono, dopo un'importazione). `DCIM/Temp` vale su ogni
  telefono, con qualunque nome l'archivio porti (`/storage/emulated/0`, `/sdcard`, un profilo di
  lavoro).
  - ⚠️⚠️ **Una scheda resta intera, dalla `2.99`** (sua nota su `nascoste-percorsi`: si nasconde
    solo la cartella di quel percorso, riconducibile al suo corrispettivo su qualunque telefono):
    una scheda tiene `/storage/<id>/...`, e il suo identificativo la ritrova in qualunque telefono
    in cui la si infili. `DCIM/Temp` non nasconde più quella della scheda.
  - ⚠️ **Le voci della `2.96` non si migrano**: quelle di una scheda erano scritte senza la radice,
    e adesso valgono per il telefono. Chi aveva nascosto una cartella della scheda la ritrova sul
    telefono, e la rinasconde.
  - ⚠️ **La radice di un volume nasconde solo se stessa**, cioè le immagini che vivono proprio là;
    un percorso fuori da un volume resta intero. Il conto vive su `portablePath`, in `Folder.kt`.
  - ⚠️⚠️ **I confronti ricorsivi passano da `hiddenIn` e `coveringOf`, quelli completi da `FolderSelection`**: una voce e il percorso di una
    cartella non sono scritti nella stessa forma, quindi un `in` o uno `startsWith` sull'elenco è
    un difetto che non dà errore.
  - ⚠️ **La chiave è nuova** (`hidden-relative`), e `HiddenMigration` traduce e toglie la vecchia.
  - Il pannello e la pagina delle impostazioni scrivono il percorso con la barra davanti, come l'ha
    scritto lui.
  - Nel file di impostazioni le nascoste hanno una casella loro, e si fondono (`docs/Files.md`
    § '💼 Esporta e importa, e il file che solo AIV sa leggere').
- ⚠️ **'Mostra' su una cartella dentro una nascosta toglie tutte le voci che la coprono**, in una
  scrittura sola (due scritture partite dallo stesso elenco si rimetterebbero a vicenda quello che
  l'altra ha tolto), e il titolo nomina la più in alto, che è quella che torna davvero.
- **`NascosteTest`** vede il filtro nei due versi, il segno, il testo, i confronti (le forme della
  radice, la scheda che resta intera, le forme che un volume non sono), la migrazione e il dialogo;
  non vede il minuto, e scadenza, avviso e proroga si guardano sul telefono. Il filtro delle
  destinazioni lo misura `DestinazioniTest`, con una prova di sola logica.

## 🏷️ L'indicatore dell'ultimo media, e la sua migrazione

- ⚠️⚠️ **I segni sono tre e si sceglie**: **Cornice**, **Angolo** (il nastro nell'angolo) e
  **Tondo** (sue richieste). Di fabbrica il tondo (sua istruzione); il suo gettone resta in coda,
  perché l'ordine è quello di dichiarazione.
- ⚠️⚠️ **La cornice era stata scartata perché una cornice intorno a una miniatura è il gesto
  universale della selezione, e l'argomento regge ancora**: la scelta è doppia proprio perché i
  segni dicono cose diverse. La nota in `GridScreen` non si corregge.
- ⚠️⚠️ **Chi aggiorna tiene il segno che ha** (sua clausola, per non stravolgere la UI di chi è già
  utente): `MarkMigration` decide una volta sola e scrive la chiave. Leggere 'archivio vuoto' a ogni
  lettura cambierebbe risposta appena si salva un'altra impostazione. 'Già utente' vuol dire
  archivio non vuoto (ci vivono anche i promemoria che l'app scrive da sé); un archivio davvero
  vuoto, cioè un'app installata e mai aperta, riceve il valore di fabbrica.
  - ⚠️ **Il valore di fabbrica vive in tre posti** (il campo di `Settings`, la lettura del flusso e
    il ramo vuoto di `MarkMigration`, che scrive il campo di `Settings`), tenuti allineati dal caso 6
    di `ProfonditaTest` e dal caso 1 di `IndicatoreTest`. Il tondo arriva alle sole installazioni
    nuove, e la voce di collaudo lo dice.
- **La cornice**: spessa il 5% del lato (una frazione e non dp, perché la piastrella cambia misura
  con le colonne), opaca all'80%, nel colore d'accento che le passa il chiamante (sue misure). Il
  tratto si disegna doppio dentro un ritaglio della sagoma, perché un tratto è centrato sul
  contorno. ⚠️ **L'arancione è stato provato e scartato**: resta il colore dei soli
  mini-onboarding, e nastro e cornice, che non si vedono mai insieme, usano lo stesso accento.
- **Il tondo**: un disco d'accento nell'angolo del nastro, diagonalmente opposto alla spunta;
  diametro 14 come frazione (sua risposta, guardando le anteprime) e opacità 60% (`MARK_DOT_ALPHA`,
  sua nota); l'aria dai due bordi vale il raggio, così cresce tutto insieme. Nelle altre lingue la
  parola è quella del cerchio (Circle, Kreis, Rond).
- ⚠️⚠️ **Nastro e tondo si specchiano da destra a sinistra**: disegnati in coordinate assolute, in
  arabo, persiano e urdu il nastro cadeva nell'angolo della durata di un filmato e dallo stesso lato
  della spunta. `lastCorner` lo monta il banco.
- ⚠️ **La voce vive in 'Etichette e pulsanti'** (sua richiesta), e la regge il titolo della pagina,
  'Comandi e indicatori'.
- **Le prove**: `IndicatoreTest` misura la migrazione, la sola cosa che può rompersi in silenzio;
  `CorniceTest` il tratto, che prende il colore ricevuto e segue il lato; `SegniTest` l'angolo nei
  due versi della lingua, la crescita, l'opacità letta dalla costante e lo specchio. Non vedono il
  segno in una griglia vera, perché il MediaStore del banco è vuoto.
