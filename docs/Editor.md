# Editor.md: le funzioni dei due editor di AIV

> **Cos'è questo file.** Le specifiche dei due editor di AIV, cioè delle pose, dei moduli
> dell'editor completo e dei comandi del salvataggio: che cosa fa ognuno, le decisioni dell'utente
> su di lui e le sue trappole. Non si carica da solo: si legge per intero prima di toccare uno dei
> due editor. Le regole del repo vivono in `Rules.md`, e gli invarianti dell'editor completo (il
> conto in AGSL, il salvataggio a tessere, `ContoTest`, il senza perdita) in `Rules.md`
> § '🎚️ L'editor completo, e il conto che esiste in una copia sola'.

## 🔄 Le otto pose dell'editor, e la fila che è diventata di cinque

- ⚠️⚠️ **L'editor riflette** (sua richiesta), quindi le pose sono otto: le quattro rotazioni e le
  stesse riflesse. La posa è `Spin(turns, mirror)`, con l'ordine dichiarato 'specchia, poi gira'.
  - ⚠️⚠️ **Comporre non è sommare**: `M` dopo `R(k)` vale `R(-k)` dopo `M`. Il conto vive in un posto
    solo, `Spin.then`, e ne dipendono lo schermo, il tag EXIF del senza perdita e il ritaglio. Una
    somma al posto della sottrazione non dà errori e su un'immagine dritta non si vede: la prova è
    `RiflettiTest`, nata con la funzione.
  - ⚠️ **L'EXIF ha due cicli e non una tabella**: `DIRECT` (1, 6, 3, 8) e `MIRROR` (2, 7, 4, 5).
    Riflettere salta all'altro ciclo alla posizione `turns - at`; una tabella di sedici caselle
    sarebbe la stessa legge scritta due volte.
- ⚠️⚠️ **La cella si stringe** (`PadArrange` con `stretch`, e `PAD_CELL` è il tetto): cinque celle
  più i distacchi chiedono 412dp, e uno schermo ne ha 360.
  - **L'etichetta è 'Rifletti'**, e il verso lo dicono il glifo e il tocco lungo, che riflette in
    verticale e lo annuncia `holdLabel`: una parola sola entra in una cella stretta in ogni lingua.
  - ⚠️⚠️ **Il corpo delle etichette è un gradino sotto** (`padLabel`, sua segnalazione), misurato sui
    suoi pixel: col carattere più stretto del banco la parola entra sempre, quindi niente prova. Il
    corpo è uno per tutte le file, perché il tasto è uno.
  - ⚠️ **Le due file dell'editor (cinque contro quattro) non si allineano**, ed è un compromesso
    dichiarato: allinearle vorrebbe una cella vuota che invita a toccare niente.
- ⚠️ **Il glifo resta `Icons.Filled.Flip`** (sua risposta `resta`).
- ⚠️ **Un tasto nuovo entra in un ordine già salvato al posto dichiarato**: `padOrderOf` legge
  `TURN_KEYS`, che è insieme l'ordine di fabbrica e il numero di colonne. Senza, il quinto tasto
  comparirebbe in coda sul telefono di chi ha già riordinato.

## 🎚️ L'editor completo: i moduli, i gesti e la storia

### Fluidifica dentro Geometria

- **Fluidifica fa parte del modulo Geometria**, accanto ad 'Angoli': non aggiunge un decimo
  modulo e non entra negli stili.
- È disponibile da Android 13, come l'editor completo. Il suo tasto apre il proprio corpo di
  comandi senza cambiare modulo.
- **Un dito dipinge sulla maglia; due dita ingrandiscono e spostano liberamente l'immagine.** Una
  pennellata completa è un solo passo della storia generale, quindi 'Annulla' e 'Ripristina'
  lavorano come nel resto dell'editor.
- **Deforma** spinge i vertici nella direzione del dito. **Ricostruisci** li riporta verso la
  posizione di partenza. I due cursori regolano dimensione e forza del pennello; il diametro
  minimo è circa un trentesimo del lato lungo (il raggio è un sessantesimo).
- **I comandi sono 'Deforma', 'Ricostruisci' e 'Azzera'** (correzione dell'utente del
  2026-09-29): i primi due hanno il testo centrato, 'Ricostruisci' ha più spazio e 'Azzera'
  azzera soltanto la maglia di Fluidifica.
- **Il pennello è un cerchio anche sulle immagini non quadrate**. Dalla `3.03`, mentre si
  regola la dimensione il cerchio vuoto ha il tratto spesso precedente di **6 dp**, nel solo
  colore accento, senza contorni neri né alone (richiesta esplicita dell'utente). Compare in
  basso a destra sulla parte visibile del canvas. Durante una pennellata resta il bordo
  sottile della `3.02`: **2 pixel fisici** del canvas, non dp, nero al **60% di opacità**,
  senza alone. Compare al contatto, segue il dito e sparisce un secondo dopo il rilascio,
  mantenendo il bordo nero nel secondo di attesa. La richiesta precedente di lasciarlo fermo
  è revocata dalla correzione esplicita dell'utente.
- **Con un diametro visibile inferiore a 1 cm compare la lente**, soltanto finché il dito è
  appoggiato. Ingrandisce il punto toccato e si sposta nell'angolo del canvas più lontano dal
  dito. La misura usa `DisplayMetrics.xdpi`, cioè i DPI fisici dichiarati da Android; la
  precisione dipende dal dato del dispositivo, e il rapporto di densità è già incluso nelle
  coordinate fisiche del canvas. La lente del ritaglio mantiene il suo posto in alto.
- La maglia ha **128 celle per lato**, cioè 16.641 vertici. Gli scarti sono normalizzati sulla
  misura dell'immagine: anteprima e file pieno leggono lo stesso valore, senza due conti.
- Il bordo resta fermo, così una pennellata non scopre il fondo. Restano esclusi 'Congela', che
  sarebbe una maschera, gli strumenti che riconoscono i volti, 'Vortice', 'Contrai' ed 'Espandi'.

- ⚠️⚠️ **I moduli sono dieci, dalla `4.40`**: Dettaglio, Effetti, Geometria, Ritaglio, Luce, Colore,
  HSL, Curve, Stili e Disegno (§ '✏️ Il modulo Disegno, prima fase'). **Le maschere no, in modo assoluto** (sua risposta `d-preset-manca`): è una porta chiusa,
  non una tappa rimandata. L'ordine della fila vive in § '✂️ Il modulo Ritaglio, e la fila che è
  diventata di icone', e non è quello della catena del conto.
- ⚠️⚠️ **In testata i due editor dicono 'Modifica'** (sua istruzione): il titolo dice che cosa si fa,
  non con quale dei due arnesi, e la distinzione vive solo nel selettore che si apre toccando
  'Modifica' e nella voce delle impostazioni. È il testo di `menu_edit`, con la chiave
  `editor_title`. La regola vecchia 'si chiama Editor e non Modifica' è decaduta.

**La Luce e i cursori.**
- ⚠️⚠️ **La Luce include i sei cursori del pannello base di Lightroom, nel suo ordine**: esposizione,
  contrasto, luci, ombre, bianchi, neri. Scartata la luminosità (sua critica): un passo additivo
  che a fondo corsa dava un rettangolo pieno. I punti di bianco e di nero muovono gli estremi al
  massimo di un quarto, quindi non possono appiattire l'immagine.
- ⚠️⚠️ **I cursori sono scritti in casa**, coi tondi grossi che ha chiesto, e per il doppio tocco
  che azzera: uno `Slider` salterebbe al primo tocco e scriverebbe nella storia un valore che
  nessuno ha chiesto. Il passo aspetta di sapere se i tocchi erano due; il trascinamento no.
  - ⚠️⚠️ **`setProgress` si tiene**: senza, il cursore è muto per un lettore di schermo e invisibile
    al banco, che i cursori li muove da lì.
  - ⚠️ **I cursori sono una tabella**, non blocchi copiati: ognuno prevede tre gesti, e un cursore
    nuovo li prende per costruzione.
- ⚠️⚠️ **I gesti sono suoi**: il doppio tocco su nome, cursore o numero azzera; il tocco lungo sul
  nome mostra l'immagine senza quel solo cursore (sul nome e non sulla barra, dove il dito è già
  appoggiato mentre si trascina); il tocco lungo sull'immagine mostra il prima. Il valore da
  confrontare lo costruisce la scheda, con lo stesso `write` del cursore.
- ⚠️⚠️ **Il prima dell'immagine non toglie tutto** (sua richiesta): nei moduli Ritaglio e Geometria
  mostra tutto, negli altri tutto tranne posa e geometria, perché chi tara un colore vuole vedere
  quel colore com'era e non un'altra inquadratura. `Look.place` divide i campi del 'dove' da quelli
  del colore, e la vista confermata sta dalla parte del 'dove', o il confronto riaprirebbe un taglio
  già applicato. Quali moduli parlano del 'dove' lo dice la tabella dei moduli. ⚠️ Nel Ritaglio
  quel ramo non lo raggiunge nessun dito, e si dichiara.
- ⚠️ **Un confronto acceso si spegne in un `finally`**: un rilevatore annullato (basta un
  salvataggio che parte) non torna alla riga dopo, e il confronto restava acceso. Caso 14 di
  `LuceTest`.

**Lo zoom e il palco.**
- ⚠️⚠️ **L'immagine si ingrandisce** (sua richiesta): pinza, panoramica e doppio tocco.
  - ⚠️⚠️ **I gesti del palco vivono in un rilevatore solo**: il tocco lungo del confronto e la pinza
    nascono dallo stesso dito, e `waitForUpOrCancellation` risponde `null` sia allo scadere del tempo
    sia a un evento consumato da altri, quindi in due `pointerInput` il confronto si accenderebbe
    durante una pinza.
  - **Il doppio tocco ci arriva con una corsa** (sua nota): un progresso solo da cui si ricavano
    ingrandimento e spostamento, e un dito che scende la ferma dove è arrivata. Lo spostamento si
    riporta nei bordi anche mentre si disegna, perché il limite dipende dall'ingrandimento.
  - ⚠️⚠️ **Si ingrandisce anche a una mano** (sua nota): il secondo tocco di un doppio tocco resta giù
    e trascina. La corsa del doppio tocco parte quindi al rilascio del primo, perché a dire quale dei
    due gesti è c'è solo quello che il dito fa dopo. Si raddoppia a ogni `ZOOM_PULL` (l'ingrandimento
    si percepisce in rapporti), il punto fermo è quello toccato, e un compagno che arriva apre la
    pinza.
  - ⚠️ **Si scala il rettangolo e non la tela**: il pennello usa uno shader con la sua matrice.
- ⚠️⚠️ **Fermandosi, la finestra inquadrata si rilegge dal file a piena risoluzione** (sua risposta
  `pieno`) e si dipinge sopra l'anteprima, ancorata all'immagine: su una riduzione a 1600 pixel la
  grana del sensore è già mediata, e il Dettaglio si giudicherebbe su un'immagine senza rumore.
  - ⚠️⚠️ **Il conto è quello del visualizzatore, `sharpAsk` in `Regions.kt`**, traslocato e non
    riscritto: riceve il rettangolo in cui l'immagine intera finisce sullo schermo, e il guadagno si
    misura sui pixel del bitmap di base. La soglia si ricava: si legge dal file solo quando
    l'anteprima è disegnata più larga dei propri pixel.
  - ⚠️ **Si chiede a gesto finito, e il via è un contatore**: scala e spostamento si leggono nel
    disegno, e fra le chiavi di un effetto ricomporrebbero il palco a ogni fotogramma.
  - ⚠️ **Col modulo Geometria mosso non si legge**, e al Dettaglio si passa il lato dell'immagine
    intera, o il filtro cambierebbe forza con la panoramica.
  - **`TasselloTest`** misura il conto e i due conti del pezzo (dove si posa, quale pixel cade sotto
    un punto); non vede il pezzo letto, che vuole un file vero.

**Il Colore.**
- ⚠️⚠️ **Il Colore include temperatura, tinta, saturazione, vividezza e l'interruttore del bianco e
  nero.** La vividezza pesa sull'inverso di quanto un colore è già saturo (il cursore dei ritratti).
  Il bilanciamento tiene ferma la luminanza, o la tinta sarebbe un secondo cursore di esposizione.
  Il bianco e nero è un interruttore e non la saturazione a -100: viene dopo nel conto, e spegne
  saturazione e vividezza.
- ⚠️⚠️ **'Filtro BN'** (nome suo) è il filtro colorato davanti all'obiettivo, e ha tre regole:
  - è **un asse e non una ruota**, perché lo zero sono i pesi di Rec. 709 e un'immagine di chi
    aggiorna resta identica;
  - i **pesi sommano sempre uno**, o il cursore scurirebbe l'immagine come un'esposizione; il conto
    vive in Kotlin (`Chroma.greyMix`) e lo shader riceve il risultato;
  - **c'è sempre, sotto l'interruttore, spento a colori** (sua istruzione), e la posizione la decide
    l'identità della riga (`knob === FILTER_ROW`), non un indice che un cursore nuovo sposterebbe.
  - La corsa è poco più del doppio di quella di partenza (sua nota: se l'aspettava più ampia); il
    verde non va a zero, o un cielo blu puro diventerebbe nero. Il filtro verde si fa con la fascia
    verde dell'HSL.
  - ⚠️ **Un elenco a tendina dei tre filtri classici non si fa**: la sua condizione, 'senza occupare
    più spazio', non si può soddisfare.

**La fila, l'ordine del conto, le curve della Luce.**
- ⚠️ **La fila dei moduli**: il tocco lungo su un gettone azzera il modulo; il punto d'accento dice
  che il modulo ha toccato l'immagine; il gettone è scritto in casa con `combinedClickable`, perché
  un `FilterChip` non offre un secondo gesto sullo stesso bersaglio. Il modulo guardato non entra
  nella storia.
- ⚠️⚠️ **I conti si fanno in luce lineare, e l'ordine è la specifica**: bilanciamento, esposizione,
  ombre e luci, punti, contrasto, poi i colori. I punti vengono prima del contrasto, che lavora
  dentro l'intervallo che loro definiscono. Il perché di ogni passaggio vive in `Adjust.kt`.
  - ⚠️ **Le maschere di ombre e luci si costruiscono sul valore percettivo**, non su quello lineare,
    o il cursore delle ombre solleverebbe i mezzi toni.
- ⚠️⚠️ **L'esposizione piega le alte luci invece di tagliarle** (suo riscontro: i chiari diventavano
  bianchi troppo in fretta). La soglia si ricava dal guadagno, `1/g` elevato a `SHOULDER_SOFT`
  (1,5, sua nota 'un pelo più morbida'), quindi a guadagno uno la curva è l'identità per
  costruzione. I mezzi toni tengono il guadagno, e si applica per canale.
  - ⚠️ **Scartata la normalizzazione della coda**: terrebbe il bianco a 255, ma porterebbe la
    pendenza sopra uno e si vedrebbe un gradino. **Costo dichiarato**: il bianco pieno arriva a 252.
- ⚠️⚠️ **Il contrasto è la diagonale più una gobba dispari al cubo** (suo riscontro: 'vecchia
  scuola', e in negativo tutto grigio), quindi gli estremi restano intatti; monotono fino a
  `CONTRAST_RISE` 1,53, fondo corsa 1. Scartate la `smoothstep`, che mangiava ombre e luci, e la
  compressione lineare verso il perno, che spostava il nero e il bianco e ingrigiva.
  - ⚠️ **Un valore già scelto rende di più**, in uno stile salvato come nei sei stili di casa che ne
    hanno uno.
  - ⚠️⚠️ **'Auto' ricava il contrasto dalla pendenza al perno**, che vale `1 + k`, e i tre numeri che
    ricopia dallo shader li presidia il banco leggendo `LOOK_AGSL` (`SviluppoTest`).
- ⚠️⚠️ **'Auto' imita 'Colore automatico' di Photoshop e sistema anche la luce media** (sua richiesta,
  e la sua risposta `piu` a `d-auto-quanto`): scrive in sei cursori (i due punti, il
  bilanciamento, esposizione e contrasto), quindi 'Annulla' lo disfa come ogni passo. I conti sono
  le inverse delle formule dello shader (`AutoLook.kt`), calcolati nell'ordine della catena; il
  bersaglio dell'esposizione è la mediana a 0,5, il perno del contrasto; il contrasto va solo in
  su, perché un'immagine più dispersa ha un carattere e non un difetto. Legge l'anteprima,
  campionata.
  - ⚠️ **Il glifo è la bacchetta di Material ammorbidita**, e non è più provvisorio (sua risposta
    `resta`). 'Auto' compare solo in Luce e Colore, che sono i moduli dei suoi sei cursori.

**La storia, il salvataggio, la barra.**
- ⚠️⚠️ **La storia è di valori, un passo al rilascio del dito**, preso dallo stato vivo: il valore
  catturato da `onValueChangeFinished` conteneva quello di prima, perché Compose chiama le due
  funzioni senza ricomporre in mezzo, e 'Annulla' restava spento una volta su dieci. È una lista
  con un indice, non due pile.
- ⚠️ **I tre comandi della storia sono icone**, gli stessi glifi dell'editor semplice (sua
  richiesta, anche per il terzo, che non aveva nominato).
- ⚠️ **La qualità di scrittura è a tre ed è un'impostazione**, non una domanda a ogni salvataggio;
  'Senza perdita' scrive un PNG accanto, perché cambia formato.
- ⚠️⚠️ **Il tocco lungo su 'Salva' scrive un file nuovo accanto all'originale** (sua richiesta), per
  la strada dei formati che non si sanno riscrivere (`FileTree.freeName`, notifica 'Copia
  salvata'); la copia di sicurezza si salta da sé, perché il bersaglio non è la sorgente. Il tasto
  è un `combinedClickable` con l'etichetta del gesto dichiarata, e a immagine intonsa è spento.
- ⚠️⚠️ **La barra in basso si specchia col lato del FAB, tranne 'Annulla' e 'Ripristina'** (sua
  richiesta): quei due sono un verso del tempo, e uno specchio li rovescerebbe. Si specchia anche
  'Salva stile' (sua conferma). Lo scambio vive in `barOrder`, una funzione pura che il banco
  chiama.
  - ⚠️ **La voce `settings_hand` ha il testo suo**: 'Posizione preferita dei pulsanti' e la
    spiegazione alla lettera, perché governa più del FAB. Le chiavi non si toccano.
- **Dalla `3.02`, la barra di Geometria riduce leggermente glifi e ingombro orizzontale dei
  comandi solo se la fila non entra nella larghezza disponibile** (sua richiesta). Tutte le
  icone restano su una fila, con lo stesso ordine per il lato del FAB; l'altezza dei comandi e
  il rientro esterno restano quelli di prima. Si contano i tasti presenti: senza Filigrana,
  se la fila entra già, non si riduce. Le altre larghezze e gli altri moduli non cambiano.
- ⚠️ **`SviluppoTest` si chiama così perché `ColoreTest` è preso**, dal colore di una cartella.

**Due difetti chiusi senza causa accertata.** Due volte i cursori si sono 'mescolati' (un gesto che
azzerava o muoveva un altro cursore, anche di un altro modulo), e il sintomo è sparito senza che
si sappia quale riga l'abbia tolto (sue risposte `via` e `trascina`, con 'comunque è risolto').
- ⚠️ **Le ipotesi misurate e cadute sono quattro, e non si rifanno**: una lambda catturata in un
  `pointerInput` che invecchia; un trascinamento vero che riporta indietro gli altri cursori; il
  rilascio di un tocco lungo che si perde; due tocchi ravvicinati su due cursori che si confondono.
- ⚠️ **Restano come difese**, dichiarate come tali: `Dial.set`, cioè un cambiamento da applicare al
  posto di un `Look` già fatto; il confronto tenuto come trasformazione; la riga risolta nel modulo
  in scena al momento della scrittura (`dialAt`); e la chiave sul `Dial`, perché senza Compose
  riusa i nodi di una lista per posizione.

## 🎨 Il modulo HSL, otto fasce e una macchina sola

- ⚠️⚠️ **Otto fasce come in Lightroom**, ognuna con tonalità, saturazione e luminanza: dove il Colore
  parla a tutta l'immagine, questo parla a un colore per volta.
- ⚠️⚠️ **Qui vivono i pesi per fascia del bianco e nero** (sua risposta `hsl` a `d-bn-pesi`): col
  bianco e nero acceso tonalità e saturazione si spengono, e la luminanza diventa il peso di quel
  colore nel grigio. È lo stesso conto, e il bianco e nero viene dopo per raccoglierne il
  risultato.
- ⚠️⚠️ **I pesi delle fasce sono triangolari, con un raggio per lato ricavato dai centri**, e così la
  somma vale uno senza normalizzare. Scartato il raggio unico: i centri non sono equispaziati, e un
  rosso pieno riceveva il 55% del proprio cursore. `SviluppoTest` misura la relazione fra i raggi,
  non il conto, che vive in AGSL.
- ⚠️ **Il conto passa per HSV dietro due guardie** (uniforme, e per pixel), perché l'andata e il
  ritorno non sono l'identità: a riposo non si fa affatto, e muovendo una fascia il resto resta
  identico.
- ⚠️ **La saturazione si moltiplica** (un grigio resta grigio); **la luminanza è pesata da quanto il
  pixel ha colore**, o schiarirebbe un cielo bianco; **la tonalità si sposta al massimo di trenta
  gradi**, la distanza fra due fasce vicine.
- ⚠️ **Nella catena viene fra il contrasto e la saturazione**: dopo il contrasto perché sceglie per
  tonalità, prima della saturazione perché il grigio del bianco e nero si ricava da quello che esce
  di qui.
- **Le pastiglie prendono il colore dal centro della fascia**, lo stesso numero del conto; il tocco
  sceglie, il tocco lungo azzera la fascia.
- ⚠️⚠️ **I ventiquattro cursori sono precostruiti**: la riga risolve modulo e fascia al momento
  della scrittura, e la chiave di ogni riga è il suo cursore. Generarli a ogni ricomposizione
  butterebbe e rifarebbe i nodi di continuo, cioè annullerebbe ogni gesto.
- ⚠️ **Il banco non vede i pixel**: che il blu tocchi il cielo e lasci l'incarnato si guarda sul
  telefono.

## 🔍 Il modulo Dettaglio, e le prime due operazioni che guardano i vicini

- ⚠️⚠️ **Cinque cursori, nell'ordine di Lightroom**: nitidezza, raggio, mascheratura, rumore, rumore
  colore (sue funzioni: maschera di contrasto e riduzione del rumore). In italiano il quinto si
  chiama **'Disturbo colore'** (sua istruzione, nel solo file italiano); la chiave
  `look_noise_color` non si tocca.
- ⚠️⚠️ **Le misure sono frazioni del lato e non pixel**: anteprima e file salvato le convertono con
  la propria misura, e coincidono in proporzione. Il raggio di serie è un millesimo del lato, che
  si vede anche a immagine intera. ⚠️ **L'anteprima è già mediata**, quindi la riduzione del rumore
  si giudica sul file salvato o sul pezzo a piena risoluzione, e si dichiara.
- ⚠️ **Raggio e mascheratura non cambiano un pixel senza nitidezza**: `Detail.idle` non li conta, o
  l'immagine si riscriverebbe per niente, e l'interfaccia li spegne col campo `off` della tabella
  dei cursori, che vale per tutti i moduli.
- ⚠️ **Il raggio è bipolare**, perché un raggio zero non esiste; un cursore monopolare non mostra il
  segno né la tacca dello zero.
- ⚠️⚠️ **Il Dettaglio è il primo della catena**, e lavora sui valori del file e non in luce lineare:
  parla del file, e il contrasto dopo di lui moltiplicherebbe la grana che deve togliere.
- ⚠️ **Dentro il modulo la riduzione viene prima della nitidezza**, e il dettaglio da accentuare si
  misura sull'immagine già ripulita.
- ⚠️ **La nitidezza usa la media binomiale a distanza del raggio, la riduzione una media
  bilaterale**: nove campioni per mestiere, dietro guardie separate. La mascheratura esce dagli
  stessi campioni; il rumore di colore prende la crominanza della media e la luminanza del centro,
  dove vive il disegno.
- ⚠️⚠️ **Nel salvataggio le tessere hanno un bordo pari alla portata del filtro più un pixel**
  (`Detail.bleed`, zero a modulo spento), il passo si stringe di quanto il bordo cresce, e ai
  margini dell'immagine il bordo si taglia.
- ⚠️ **Il filtro lineare sul `BitmapShader` va in tutti e due i posti**, anteprima e salvataggio:
  `isFilterBitmap` del pennello governa il disegno e non i campioni che uno shader chiede a un
  altro.
- ⚠️ **Il banco non vede i pixel che ne escono**: misura il riposo, lo spegnimento, il reset, la
  scala col lato e le tessere.

### Correggi/Rimuovi dentro Dettaglio

- Richiesta del 2026-10-01: icona disegnata nella barra inferiore, come Fluidifica.
  Il tasto entra nella modalità; un dito dipinge una selezione nel colore primario al 50%,
  con il lentino di Fluidifica per i pennelli piccoli. Due dita ingrandiscono o spostano
  senza dipingere. Lo slider mostra un cerchio spesso d'accento; durante il contatto il
  contorno è sottile. Le passate sovrapposte non aumentano l'opacità della selezione.
- **Solo Applica cambia i pixel**. Cancella selezione toglie soltanto il verde. Ogni
  applicazione è un passo normale di Annulla/Ripristina. Originale e il reset del modulo
  tolgono le correzioni; Indietro protegge anche una selezione ancora da applicare.
  Salva aspetta che la selezione sia applicata o cancellata, anche passando a un altro modulo.
- Il calcolo è interamente offline, senza AI, librerie aggiunte, modelli o servizi:
  `Inpaint.kt` cerca campioni integri nell'intorno e non ricava dettagli certi dove manca
  ogni indizio.
- ⚠️⚠️ **Dalla `3.52` il calcolo lavora su più scale** (richiesta del 2026-10-03: aree un
  po' più grandi, e più contesto anche per i difetti piccoli). Parte dall'immagine
  rimpicciolita, dove il buco è largo pochi tasselli e un tassello di 7x7 vede la struttura
  intorno; là lo riempie il calcolo delle versioni precedenti, che copia pezzi interi dal
  bordo verso l'interno. A ogni scala più grande ingrandisce la mappa dei pezzi scelti,
  ricompone il buco con i pixel veri di quella scala e la affina con una ricerca casuale
  (PatchMatch) su tutta l'area di lavoro, che è il doppio di prima intorno alla selezione.
  - Misurato su quattro scene sintetiche con buchi da 40 a 220 px: diagonale, mattoni ed erba
    ricostruiti senza difetti visibili; il calcolo precedente spezzava le fughe dei mattoni e
    l'orizzonte, e il suo raccordo dei colori lasciava puntini scuri e trattini chiari lungo i
    bordi netti.
  - ⚠️ **Limite noto**: una trama morbida come le nuvole diventa cielo liscio.
- ⚠️⚠️ **Dalla `3.53` il bordo della correzione è sfumato verso l'esterno** (suo riscontro sul
  `3.52-01`: *l'area della correzione ha bordi troppo nitidi. Se fosse sfumata funzionerebbe*).
  La selezione si allarga di una fascia pari a un decimo della sua misura, fra 3 e 24 pixel; la
  fascia viene ricostruita insieme al buco e poi miscelata coi pixel originali, dal tutto
  ricostruito sul bordo della selezione al niente sul bordo esterno, lungo una curva morbida.
  Dentro la selezione resta tutto ricostruito, così il difetto non riaffiora; oltre la fascia
  nessun pixel cambia. Misurato sul cielo con le nuvole: l'arco che chiudeva la correzione
  sparisce, e mattoni, erba e diagonale restano uguali.
  - ⚠️ **Scartati durante la prova**: ingrandire i pixel invece della mappa (il buco diventa una
    media piatta) e partire col tassello adattivo di 5-9 px su una scala di 35 px (una gobba
    sulla diagonale, conservata da tutte le scale successive).
- I campioni provengono dalla sorgente originale, con le correzioni precedenti già
  applicate, prima di posa, sviluppo e geometria. Le selezioni seguono le coordinate
  originali anche dopo zoom, posa e deformazione. Le patch conservano i pixel calcolati
  a piena risoluzione: anteprima, tasselli e salvataggio riusano quel risultato.
  I pixel esterni alla selezione e l'alfa si conservano; gli stili escludono le patch.
- Il lavoro locale ha limiti espliciti: 196.608 pixel selezionati per applicazione,
  1.048.576 pixel nel rettangolo di lavoro con il suo contorno, 16 MiB di patch conservate
  nella storia. Fino alla `3.42` erano 24.576 e 393.216, e su una foto da 12 MP il pennello
  al massimo era rifiutato. Quando il contorno doppio non entra nel rettangolo si restringe,
  e sopra 120.000 tasselli una scala ricompone senza cercare di nuovo, per tenere il tempo
  limitato. Una selezione troppo grande o senza campioni integri lascia immagine
  e selezione intatte; l'avviso compare **sopra la bottomsheet** come toast sul palco, non
  nel corpo della scheda (dalla `3.25`); dalla `3.37` sparisce al tocco o dopo 10 s. Il promemoria di selezione pendente è tappabile e apre Correggi/Rimuovi (dalla `3.26`). Il lentino di pennello riduce lo zoom se il cerchio ingrandito uscirebbe dal tondo e, dalla `3.37`, sparisce a una soglia più bassa. Si lavora su piccoli difetti uno alla volta.
  Quando il formato permette la lettura per regioni si decodifica soltanto il pezzo;
  negli altri formati si usa la decodifica dell'immagine prevista dal salvataggio.

## ✨ Il modulo Effetti, e i suoi cursori

- ⚠️⚠️ **Il modulo include Foschia, Grana e Vignettatura** (sua risposta `effetti` a
  `d-dopo-editor`), ognuno col suo secondario (§ '🎛️ I tre cursori secondari degli Effetti').
- ⚠️⚠️ **Chiarezza e Texture sono state tolte** (sua risposta `via` a `d-eff-restano`): facevano un
  reticolo, perché nove campioni radi campionano invece di mediare. Il rimedio, una media letta da
  una riduzione dell'immagine, esisteva e non è stato scritto: chi rimettesse quei cursori riparte
  di lì.
- ⚠️⚠️ **La Foschia toglie il velo stimato col canale scuro mediato sull'intorno**: dove c'è
  foschia l'aria alza tutti e tre i canali, quindi non scende nemmeno il più basso.
  - **Si prende la media dei minimi e non il minimo del blocco**, che fa una mappa a gradini e degli
    aloni. **La media segue i bordi** (soglia `HAZE_EDGE`, diversa da quella del rumore): una media
    uniforme spalmava il velo del cielo dentro il profilo sotto di lui. Il velo vero non si perde.
  - ⚠️⚠️ **I nove campioni cadono su due anelli, di cinque e di tre** (coprimi, raggi non in rapporto
    intero): su una griglia la stima a certe frequenze copiava la trama, e il reticolo si vedeva in
    negativo (suo riscontro). Scartate la riduzione, che costa una texture e un bordo in più per lo
    stesso risultato su una texture vera, e più campioni sugli anelli, che non migliorano
    abbastanza.
  - ⚠️⚠️ **La luce atmosferica si prende bianca**: un numero ricavato dall'immagine intera non può
    vivere in `Look` e darebbe una cucitura su ogni tessera. Con una foschia colorata resta una
    dominante, e si usa il bilanciamento.
  - **I due versi**: togliere è `(c - k) / (1 - k)`, aggiungere `c + k (1 - c)`. ⚠️⚠️ **Togliere non
    supera il velo presente nel pixel stesso** (tetto col canale scuro del pixel, nel solo verso che
    toglie), o le ombre dentro una zona velata si chiudevano sul nero; la nota vecchia che dava quel
    difetto 'fuori dalla corsa' era falsa.
  - ⚠️⚠️ **La Foschia ha il raggio più largo e legge i pixel di partenza**, quindi sta subito dopo il
    Dettaglio e prima del bilanciamento. Il bordo delle tessere è il massimo dei filtri mossi
    (`AdjustRender.bleedFor`), e la guardia si scrive sul raggio e non su 'modulo a riposo': con la
    sola grana o la sola vignettatura il bordo è zero.
- ⚠️⚠️ **Grana e Vignettatura non leggono i vicini ma leggono dov'è il pixel nell'immagine intera**
  (`Framed`, due uniform): senza, nel file salvato ogni tessera si vignetterebbe da sola e la grana
  si ripeterebbe a scacchi, e sull'anteprima non si vedrebbe. Il segno dell'origine lo mette la
  funzione, non il chiamante.
- **La vignettatura** si misura sulla mezza diagonale (l'angolo vale uno su qualunque formato),
  parte dal centro con una `smoothstep` ed è bipolare: il verso positivo apre gli angoli, che è il
  gesto di chi corregge un obiettivo.
- **La grana**: la cella è una frazione del lato, monopolare, pesa sui mezzi toni, lo stesso valore
  ai tre canali (è di densità e non di colore), e sotto il pixel non scende. Il pavimento entra
  sotto i 1.200 pixel di lato, e là la grana si vede un po' più grossa di quella del file.
- ⚠️ **Vignettatura e grana sono gli ultimi della catena, in quest'ordine**: l'obiettivo e poi la
  pellicola.
- ⚠️ **I nomi degli uniform si scelgono con cura**: `texture` è una funzione di GLSL.
- ⚠️⚠️ **Uno stile include anche gli Effetti**, quindi i moduli che uno stile governa sono sei; ma i
  venti di casa restano senza (sua risposta `lascia`: si rifanno da capo a modulo finito).
- **Il gettone vive subito dopo il Dettaglio** (suo ordine), l'altro modulo che guarda i vicini.
- ⚠️ **Il glifo è suo** (`ic_mod_effects.xml`, una superellisse col tondo scavato), e non ha punte da
  raccordare. I due lati ad arco di raggio 115 sono la firma di SVGO e si tengono; l'avviso '0%' di
  CleanSVG su quel file è un difetto di CleanSVG, scritto nelle regole di quel repo.
- **Il banco** misura il modulo, il reset, il senza perdita, la scala, la portata, il bordo,
  l'origine col segno, gli uniform che combaciano e il kernel della stima letto dalla stringa dello
  shader (centrato, pesi che sommano a sedici, dentro il raggio dichiarato, nessuna frequenza che
  passa intera); non vede i pixel.
  - ⚠️ **Nelle prove che toccano un gettone la fila si monta rovesciata**: la fila scorre, e in coda
    il gettone cade fuori dal banco, dove il tocco non fa niente senza errori.

## 🎛️ I tre cursori secondari degli Effetti

- ⚠️⚠️ **Ogni principale degli Effetti ha il suo secondario sotto di sé** (sua richiesta), e il
  modulo ne include sei. **Vince il suo elenco sulla frase 'uno per effetto'**: la grana ne ha due
  (**Luci**, **Dimensione**), la vignettatura uno (**Sfumatura**), la foschia nessuno. La lettura è
  dichiarata nella voce di collaudo.
- ⚠️ **Un secondario si spegne col suo principale a zero** (campo `off`), e non conta in
  `Effects.idle`.
- ⚠️⚠️ **'Luci' ha il valore di fabbrica sullo zero** (sua richiesta: grana nelle ombre e quasi niente
  sui cieli), e a fondo corsa torna esattamente la grana di prima. Uno stile salvato prima lo
  rilegge a riposo, e si dichiara.
  - **I numeri di oggi sono suoi**: a riposo la grana vale il 130% nelle ombre fino al quarto di
    scala, mezzo livello su 255 a metà scala e un decimo di livello sul cielo a `t=0,82`; la rampa è
    una `smoothstep` larga 70 livelli. Il massimo assoluto non cresce, si sposta. ⚠️ Le terne di
    numeri dei giri precedenti sono superate.
  - ⚠️ **La soglia alta si arrotonda per difetto**, o manca il vincolo per un decimillesimo di
    livello.
  - ⚠️ **Il cursore guarda solo quanto il pixel è chiaro, non se è uniforme**: guardare
    l'uniformità vorrebbe i vicini, cioè un raggio e un bordo sulle tessere.
- **'Dimensione' raddoppia e dimezza la cella**: verso il fine il pavimento del pixel entra anche
  sull'anteprima.
- ⚠️⚠️ **'Sfumatura' sposta il punto del pieno**, e la rampa parte sempre dal centro (sua richiesta:
  sfumatura sempre massima). 'Inizio' si legge guardando dall'esterno, ed è una lettura dichiarata:
  a +100 il pieno cade a 0,70 del raggio, a riposo sull'angolo, a -100 a 1,30, fuori dal
  fotogramma. **A riposo l'alone arriva più dentro di prima**, e si dichiara.
- ⚠️ **La scheda non cresce**: l'altezza comune la detta il modulo più alto (§ '📈 Il modulo Curve, e
  il colore mirato').
- ⚠️⚠️ **La prova ricalcola le sue richieste**, non una costante: le ombre al 130%, il mezzo livello,
  il decimo sul cielo, il ritorno pieno a fondo corsa; il guadagno si misura sul testo dello shader.
  I numeri vengono da un modello di sessione.

## 📈 Il modulo Curve, e il colore mirato

- ⚠️⚠️ **Il modulo non ha cursori**: è un grafico con quattro canali, RGB e i tre colori.
- ⚠️⚠️ **Il conto arriva allo shader come una tabella di 256 pixel** (`uniform shader tone`),
  calcolata in Kotlin: una spline vuole un ciclo sui suoi punti, e il risultato dipende solo dal
  livello in ingresso. Non è una seconda copia ma una lettura: anteprima, salvataggio e grafico
  leggono la stessa tabella. Tre letture per pixel, una per canale.
  - ⚠️ **La bitmap della tabella è opaca**, perché Skia consegna colori premoltiplicati, **e vuole il
    filtro lineare**, o la curva si vede a gradini.
- ⚠️⚠️ **La spline è monotona (Fritsch-Carlson)**: una cubica naturale oltrepassa fra due punti
  vicini e inverte i toni. Su punti allineati è la retta, quindi `Curve.idle` guarda i punti; la
  prova include un tratto piatto, che è il caso che distingue le due matematiche.
- ⚠️ **Le curve si compongono come `all(canale(v))`**, in Kotlin, che è la convenzione di ogni
  editor; al contrario una curva di canale cambierebbe posto toccando quella di tutti i toni.
- ⚠️ **Nella catena viene fra il contrasto e l'HSL**: dopo la curva a S predefinita, e prima di chi
  sceglie i colori per tonalità, perché le curve di canale la cambiano.
- ⚠️⚠️ **I gesti del grafico**: il trascinamento prende il punto sotto il dito o ne crea uno; il
  tocco lungo lo toglie; il tocco secco crea un punto sulla curva senza spostarla. Gli estremi si
  muovono solo in verticale e non si tolgono. Il riquadro non è quadrato (compromesso dichiarato,
  per non prendersi metà del palco), e l'accessibilità si ferma alla descrizione.
  - ⚠️⚠️ **La curva si fotografa quando il dito scende, e un estremo lo è solo se il punto c'era
    già**: rileggerla a gesto iniziato dava al punto appena nato l'indice dell'estremo, e il
    trascinamento appiattiva la curva fino al margine (sua segnalazione).
- ⚠️⚠️ **Il colore mirato vive nel solo HSL** (sua risposta `via` per le Curve, dove il palco torna a
  zoomare): il tocco sceglie la fascia del pixel al rilascio del dito, senza attesa né vibrazione
  (suo riscontro). Il tasto è un'icona; armato, il palco fa solo quello; c'è solo nei moduli che
  hanno un bersaglio, e lo dice la tabella dei moduli.
  - ⚠️ **Il colore letto è quello del file e non quello che si vede**, e si dichiara; si legge dal
    pezzo a piena risoluzione quando c'è. Un grigio non appartiene a nessuna fascia (`Mix.bandOf`
    risponde `-1`).
  - ⚠️ **La lente del mirato non c'è più** (sua risposta `via`); chi la volesse la ritrova nella
    storia git.
- ⚠️⚠️ **Trappola generale: un `Canvas` si ridisegna quando cambia uno stato che il suo disegno
  legge**, quindi una cosa che si muove sopra l'immagine vive in una tela sua, senza
  `pointerInput`, o ogni pixel di dito rifarebbe tutto il conto dello sviluppo.
- ⚠️ **La vibrazione del tocco lungo (`HOLD_BUZZ`) non si alza**: è discreta su sua richiesta, in
  tutta l'app.
- **Modulo, fascia e canale vivono in `Gaze`**, condiviso fra palco e scheda, fuori dalla storia.
- ⚠️⚠️ **La scheda ha un'altezza sola per tutti i moduli** (sua richiesta): `SteadyBody` misura ogni
  corpo e tiene il massimo, perché un numero in dp sarebbe giusto su un telefono solo. Le copie
  misurate vivono un giro e senza semantica, o un lettore di schermo annuncerebbe i cursori di
  tutti i moduli. Oggi il modulo più alto è quello delle Curve.
  - ⚠️ **I limiti dei riquadri passano da `within`**: un intervallo vuoto in `coerceIn` faceva cadere
    l'app tirando una squadretta su un palco corto.
- ⚠️⚠️ **Compattezza** (sua richiesta): `DIAL_ROW` è 36 punti, e sotto non si scende; il nome è un
  gradino sotto. **La colonna dei nomi si misura sul telefono** (`rememberTextMeasurer`), sui nomi di
  tutti i moduli, fra un minimo e un tetto: un numero fisso non regge le lingue coi nomi lunghi, e
  la colonna non deve cambiare larghezza da un modulo all'altro. Niente prova: il banco ha caratteri
  più stretti.
  - ⚠️ **Fra nome, barra e numero c'è `KNOB_GAP`**, pagato dalla barra (sua segnalazione: tutto
    troppo attaccato). Il nome può andare a capo su due righe (sua condizione: il passo fra i
    cursori non cambia), perché due righe valgono 32 punti contro i 36 della riga; oltre il 150% di
    scala dei caratteri la riga si allunga, e si dichiara.
- **L'aria che avanza nei moduli corti la distribuisce `Breathe`**, con un tetto, o i blocchi
  diventerebbero isole.
- **Il banco** misura la spline, la tabella, gli estremi, i canali, il mirato e il punto nato in
  mezzo; non vede i pixel né il tocco lungo che toglie un punto.

## 📐 Il modulo Geometria, e il conto che non passa dallo shader

- ⚠️⚠️ **Cinque comandi in una versione sola** (sua risposta `intera`): raddrizzamento, proporzioni,
  orizzontale, verticale, distorsione; più lo strumento 'Angoli'.
- ⚠️⚠️ **Non vive nello shader**: una tessera del salvataggio legge solo la sua porzione, e una
  deformazione fa leggere a un pixel un punto lontano. Il conto è scritto una volta in Kotlin
  (`Geometry.kt`), e palco e salvataggio lo leggono con `Warp.draw`; il banco lo misura.
- ⚠️ **Si disegna come una maglia di triangoli**, perché la distorsione curva le righe; a riposo
  resta il rettangolo. Trentadue celle per lato tengono lo scarto sotto il pixel.
- ⚠️ **La scala di copertura si ricava sul contorno, non sui quattro angoli**: il punto più rientrato
  dipende dal comando.
- ⚠️⚠️ **Sul palco la maglia si ritaglia al riquadro dell'immagine** (suo riscontro): il
  `clipToBounds` del palco non basta, e si vedeva una cosa e se ne salvava un'altra.
- ⚠️⚠️ **Il perno di un keystone è la retta di mezzo** (suo riscontro): ogni asse è una Möbius sul
  proprio lato, in sequenza (`Warp.SLANT`), quindi il trapezio è isoscele e il centro resta il
  centro. Scartato deformare la sola coordinata trasversale, che curva le verticali. Il prezzo è una
  copertura più alta, dichiarato; la corsa non cambia (sua risposta `bene`).
- ⚠️⚠️ **La distorsione ha il fondo corsa 0,12**, sotto il tetto oltre il quale il disegno si
  ripiega. Lo ha trovato il banco da un colore mirato sbagliato, senza nessun errore sul palco.
- ⚠️⚠️ **Mentre il dito muove 'Raddrizza' compaiono le linee dei terzi**, due verticali e due
  orizzontali, e spariscono quando il cursore si lascia (nota E del giro della `4.36`, dalla
  `4.37`). Sono i terzi del riquadro dell'immagine, disegnati dove l'immagine è sullo schermo
  (`levelThirds`), e a dirlo è l'identità della riga (`STRAIGHTEN_ROW`).
  - ⚠️⚠️ **Dalla `4.64` hanno l'aspetto delle guide del Disegno** (sua nota in Altro sul giro della
    `4.63`: *in tutto e per tutto simili (come aspetto) alle guide dinamiche dei bordi del modulo
    Disegno*): il solo accento, larghe 1 dp (`GUIDE_LINE`), senza alone. Fino alla `4.63` erano
    una linea chiara su un alone scuro, la lettura di 'sempre visibili' che lui ha sostituito.
  - ⚠️ **Toccando la barra senza strisciare le linee restano per il tempo del doppio tocco**
    (circa 0,3 secondi): il cursore aspetta di sapere se i tocchi erano due prima di chiudere il
    gesto.
  - La prova è in `SviluppoTest`, sui due fotogrammi con lo stesso valore, col dito giù e dopo il
    rilascio; dalla `4.64` misura anche il colore e lo spessore delle linee (`verificaGuide`).
- **Il colore mirato passa dalla mappatura inversa** (`WarpPlan.back`), perché il dito tocca
  l'immagine deformata.
- ⚠️ **Col modulo mosso il pezzo a piena risoluzione non si legge**, e si dichiara.
- ⚠️ **I keystone si chiamano col loro asse** ('Orizzontale', 'Verticale'), non 'prospettiva', che
  direbbe che l'altro è un'altra cosa.
- ⚠️⚠️ **'Angoli' deforma trascinando un angolo per volta** (sua richiesta): un'omografia da quadrato
  a quadrilatero (Heckbert), con l'inversa data dall'aggiunta, come quinto passo di `Warp.map`,
  così copertura, maglia e lettura inversa la prendono per costruzione.
  - ⚠️ **La guardia è la convessità** (`PULL`): oltre la diagonale il quadrilatero si ripiega e la
    mappa non è più invertibile.
  - ⚠️ **Armato, l'immagine si rimpicciolisce a una scala fissa del `fit` senza angoli**, o la
    maniglia resterebbe incollata al bordo mentre la si tira. Il riquadro della copertura si vede, e
    il palco fa solo quello.
  - ⚠️⚠️ **Con un taglio applicato, armando 'Angoli' il palco mostra l'immagine intera**, perché le
    maniglie sono gli angoli dell'immagine intera; il riquadro tenuto dice anche il taglio
    (`Look.crop`).
- **Il banco** misura l'identità, l'andata e il ritorno, la copertura, i keystone, il ritaglio a
  pixel, gli angoli col taglio (casi 70 e 71); non vede i pixel deformati. ⚠️ Il caso 70 non vede
  il gesto che legge il palco del primo tocco: a quello pensano i casi 68 e 69 del Ritaglio.

## ✂️ Il modulo Ritaglio, e la fila che è diventata di icone

**La fila dei moduli.**
- ⚠️⚠️ **L'ordine di fabbrica è Dettaglio, Effetti, Geometria, Ritaglio, Luce, Colore, HSL, Curve,
  Stili** (sua istruzione; `MOD_KEYS`, in `Settings.kt`), e di fabbrica si apre il Ritaglio:
  `LOOK_FIRST` si ricava dall'elenco. Chi ha riordinato tiene il suo ordine, e la tabella dei moduli
  e `MOD_KEYS` si riordinano insieme. L'ordine della fila non è quello della catena.
- ⚠️⚠️ **I gettoni dei moduli sono icone, e il nome è la loro descrizione parlata** (sua richiesta):
  coi nomi scritti la fila cresceva in altezza e il palco si riduceva a zero pixel. **Con nove
  moduli la fila scorre** sul suo schermo, lui l'ha accettato, e il mini-onboarding lo insegna. I
  quattro canali delle Curve restano scritti, perché i loro nomi sono una lettera o poco più.
- ⚠️⚠️ **La fila si riordina come i riquadri di casa** (sua richiesta): ogni modulo ha la sua chiave
  in `MOD_KEYS`, e si riordina la vista, non l'identità, quindi spostare un gettone non cambia il
  modulo aperto. La replica nelle impostazioni è di sole icone, e c'è solo da Android 13; la
  preferenza si salva comunque.

**Il Ritaglio.**
- **Squadrette, velo, terzi, presa e lato minimo sono il codice dell'editor semplice**
  (`EditorScreen.kt`: `cropOverlay`, `grabbed`, `dragged`, `cropBox`, `cropFractions`), che questo
  palco chiama: due disegni dello stesso comando divergerebbero.
- ⚠️⚠️ **Formati e centrature come nell'editor semplice** (sua risposta `formati`); di fabbrica
  'Libera'. **'Originale' è una forma il cui rapporto è quello dell'immagine** (sua richiesta).
- **Le due disposizioni** le dichiara `rows` (`ShapeRows.SIMPLE`, `ShapeRows.FULL`), senza valore di
  serie:
  - **editor completo**: una riga con 'Libero', 'Originale' e i due versi a icona (`CropPortrait`,
    `CropLandscape`, di Material, a zero punte), una riga coi numeri in parti uguali (suo punto
    `crop-giu`); il nome dei versi vive nella descrizione parlata;
  - **editor semplice**: riga 1 'Libero' e i numeri, riga 2 'Originale', 'Verticale', 'Orizzontale'
    scritti (sua richiesta), con corpo `labelMedium`, perché col corpo pieno il russo non entra.
  - ⚠️⚠️ **La prima riga dell'editor semplice ha celle misurate** (`shapeCell`): 'Libero' la sua
    parola, ogni numero il più largo dei numeri, l'avanzo in proporzione. Se non ci sta si stringe la
    parola e mai i numeri, perché '16:9' troncato si legge come un'altra proporzione. In tamil la
    parola prende l'ellissi, e la misura vera non è verificata (Android usa una variante del
    carattere che in sessione non c'è).
- ⚠️ **Le proporzioni sono in ordine crescente in tutti e due gli editor**: 1:1, 5:4, 4:3, 3:2, 16:9
  (sua istruzione).
- ⚠️⚠️ **`CROP_AIR` per lato vale in tutti i moduli** (sua richiesta, come anti-jitter): è l'aria che
  le squadrette chiedono fuori dall'immagine, e lasciandola sempre l'immagine non si rimpicciolisce
  entrando nel Ritaglio. Il costo è sotto l'uno per cento del lato.
- **Il Ritaglio prende il dito sull'immagine**: il palco fa solo quello, l'immagine torna intera, e
  un dito lontano da una presa non fa niente, invece di far saltare il rettangolo sotto di sé.
- **I comandi di posa sono tre** (ruota a sinistra, a destra, e rifletti col tocco lungo in
  verticale), disegnati da `ActionPad`; l'ordine salvato del riordino, che è di una fila da cinque,
  qui non si legge.
- ⚠️⚠️ **Una posa porta con sé il rettangolo** (`spunRect`, in `spunLook`), o un quarto di giro
  lascerebbe il ritaglio su un'altra porzione. Con 'Libero', con 'Originale' e con uno specchio la
  cornice gira con l'immagine; **con una proporzione un quarto di giro la rifà nel verso scelto,
  grande e centrata dentro la porzione** (`posedLook`, sua risposta `casa`), così gettone e cornice
  dicono sempre la stessa cosa.
- ⚠️ **La posa resta senza perdita e il ritaglio no**: senza niente da sviluppare il salvataggio
  delega a `ImageEdit.save`, la strada dell'editor semplice.
- ⚠️⚠️ **'Applica'** (sua richiesta): il palco inquadra la porzione e negli altri moduli si lavora su
  quella; rientrando nel Ritaglio l'immagine torna intera con le squadrette dov'erano. Scartata la
  lettura 'taglia davvero e riparti', per sua scelta. Il valore è `Look.framed`, fuori da `idle` e
  da `lossless`; glifo e parola sono quelli dell'editor semplice; `view` resta il riquadro
  dell'immagine intera (`spread`, `cutout`).
- ⚠️⚠️ **Tentativo revocato da lui**: alzare i quattro comandi del ritaglio con un distacco, che si
  prendeva l'avanzo e schiacciava il resto del corpo. Oggi quei comandi vivono dietro un separatore
  sfumato (`FadedRule`, con l'aria `CROP_RULE_AIR`) e sono distanziati fra loro (sua richiesta).
  Niente prova, perché è la classe di misure che la revoca ha dichiarato bugiarde.
- ⚠️⚠️ **Il gesto del palco legge posa, taglio e anteprima di adesso** (`rememberUpdatedState`; la
  chiave del `pointerInput` non si tocca, o si annullerebbe il gesto in corso): letti al primo
  tocco, dopo 'Applica' o dopo una rotazione la cornice non si prendeva (sua segnalazione).
- ⚠️ **Il verso di partenza lo decide la fotografia** (`startLay`), una volta, all'arrivo
  dell'anteprima.
- ⚠️⚠️ **'Originale' è l'immagine intera in qualunque verso, anche dopo una rotazione** (sua
  risposta `intera`; `Shape.fromImage`, uno per i due editor), e i due gettoni del verso si
  spengono con lei: è una lettura dichiarata, perché accesi non cambierebbero niente.
- ⚠️⚠️ **Tutti i comandi lavorano dentro la porzione applicata** (`framedCrop`, `relativeTo`,
  `absolute`): lo ha trovato il banco, e il file avrebbe incluso una striscia che il palco non
  mostrava. 'Originale' dentro una porzione è la porzione.
- ⚠️ **Un chip spento si vede spento nei due editor** (`SheetChip`, coi numeri di Material: 38% per
  testo e icona, 12% per fondo e filetto).
- ⚠️ **Anche l'editor completo ha la lente del ritaglio** (sua richiesta): la funzione `lens` è una,
  a pixel interi (Dettaglio e Foschia vi si vedono appena diversi, dichiarato), nella tela del
  palco. Non è la lente del colore mirato, che lui ha tolto.
- **Il banco** (`SviluppoTest`, `EditorCasaTest`) misura il rettangolo che segue la posa, il senza
  perdita, i gettoni col nome, le due disposizioni e l'ordine dei numeri, le celle misurate, il
  verso di partenza, la cornice dopo 'Applica' e dopo una rotazione, 'Originale' dopo un quarto di
  giro, i chip spenti a pixel, la porzione, e la lente; ogni caso ha la sua controprova. Non vede il
  file salvato. Le trappole dell'iniezione dei gesti trovate qui vivono in `Rules.md` § '🧪 Quando
  si scrive una prova, e quando no'.

## ✏️ Il modulo Disegno, prima fase

- ⚠️⚠️ **Le sue quattro risposte del 2026-10-06 decidono la forma** (nota G del giro della `4.32` e
  nota D del giro della `4.34`): **D1a**, vive solo nell'editor completo, come ultimo modulo a
  destra (dalla `4.50` penultimo, perché gli Stili sono sempre in fondo), quindi sotto Android 13
  non c'è; **D2a**, il disegno è attaccato all'immagine e gira, si
  deforma e si ritaglia con lei; **D3a**, si fonde nel file salvato come la filigrana, e un file
  riaperto contiene pixel e non forme; **D4a**, tre versioni: G1 (`4.40`) le forme, G2 (`4.50`)
  selezione e maniglie, G3 (`4.60`) il testo. I numeri sono quelli di allora: la G2 è diventata
  la `4.60`, e il testo viene dopo.
- **La G1 ha cinque strumenti di disegno** (si chiamano così, sua correzione del 2026-10-07; nel
  codice resta `Pen`): mano libera, linea, freccia, rettangolo arrotondato ed ellisse; otto
  colori fissi, lo spessore, il tratteggio, il riempimento (solo per rettangolo ed ellisse) e
  'Azzera', che dalla `4.64` si chiama `Elimina tutto`. Il rettangolo e la freccia sono i due strumenti principali (sua precisazione). Per la freccia il dito va dall'inizio alla punta; per il rettangolo e l'ellisse i due
  capi del trascinamento sono due vertici opposti. Un tocco senza movimento lascia un punto con la
  mano libera, e niente con gli altri strumenti. Ogni elemento è un passo di 'Annulla'.
- ⚠️⚠️ **I punti vivono nella cornice ORIGINALE dell'immagine, in frazioni dei lati**, come la
  selezione di Correggi; lo spessore è una frazione del lato lungo, così anteprima e file pieno
  hanno lo stesso tratto. Il perché per esteso vive in testa a `Drawing.kt`.
- ⚠️⚠️ **Si posa dopo lo sviluppo e prima della geometria**, sul palco e nel salvataggio
  (`Draw.onto` in `ImageEdit.saveLook`): i cursori del colore non ridipingono l'inchiostro, e
  raddrizzamento, prospettiva, Fluidifica e ritaglio lo portano con l'immagine.
- ⚠️ **Il palco prende sempre il dito**, come nel Ritaglio; pinza e panoramica restano a due dita.
- ⚠️ **Strumento, colore e tratto sono gli arnesi e non l'immagine**: non entrano nella storia, e
  cambiarli non tocca gli elementi già fatti. La selezione di un elemento per cambiarlo è la G2.
- ⚠️⚠️ **Il riempimento ha colore e opacità suoi, separati dal contorno**, ed è la sua precisazione
  arrivata a G1 in corso (*un bordo rosso primario e un riempimento bianco 50%*). Due gettoni,
  'Contorno' e 'Riempimento', dicono a che cosa si applicano la fila dei colori e il cursore
  (spessore o opacità), così la scheda non cresce di una seconda tavolozza; per il riempimento il
  primo colore è 'Nessuno', che è quello di fabbrica, e l'opacità di fabbrica è il 50%. La
  freccia, la linea e la mano libera lo ignorano, e l'elemento nasce senza.
- **Dalla `4.41` due aiuti del giro della `4.40`** (sue note su `4.40-01`):
  - **R1**: mentre il dito tiene `Spessore`, la punta nella sua misura vera, come tondo pieno del
    colore della linea in basso a destra sull'immagine, nello stesso angolo e con lo stesso margine
    del pennello di Correggi e Fluidifica; sparisce quando il dito si alza.
  - **R2**: mentre il dito disegna una forma piccola compare la lente di Correggi, con l'inchiostro
    dentro. La soglia la sceglie la sessione ed è dichiarata: **1,5 cm sullo schermo**, misurati sul
    riquadro del tratto a mano libera o sulla distanza da A al dito per gli altri strumenti. Oltre la
    soglia la lente sparisce, e torna se una forma dei quattro strumenti da A a B si restringe.
- **Dalla `4.42` i valori di fabbrica sono i suoi** (2026-10-07, dopo il giro della `4.41`):
  spessore al 60% della corsa del cursore, tratto `#FFFF4B3D`, riempimento delle forme `#26FFAE8E`
  (salmone al 15% circa), quindi rettangolo ed ellisse nascono riempiti. Nella tavolozza il rosso
  è diventato `#FF4B3D` e l'arancio il salmone `#FFAE8E`, perché i due colori di fabbrica restino
  selezionabili: è una scelta della sessione, dichiarata nella voce di collaudo.
- **Dalla `4.43` i tasti Contorno, Riempimento e Tratteggio sono disegni** (sua nota su
  `4.42-01`): Contorno è una linea spessa del colore della linea; Riempimento un rettangolo
  arrotondato del colore del riempimento, senza contorno, sopra una scacchiera, con l'opacità
  alzata in proporzione e mai sotto il 40% (`keyAlpha`); Tratteggio una linea spessa tratteggiata
  grigio scuro, e il tasto si accende e si spegne. La parola resta come descrizione per il lettore
  di schermo, e per lui il primo resta **Contorno** (sua indicazione: nella nota l'aveva chiamato
  'Tratto'). Scelte della sessione, dichiarate nella voce di collaudo: il filo sottile della
  tavolozza attorno alla linea di Contorno, perché il nero sul tema scuro e il bianco sul tema
  chiaro si vedano; con 'Nessuno' il rettangolo è la scacchiera vuota con la diagonale rossa.
- **Dalla `4.44` i tasti sono ridisegnati, e 'Contorno' si chiama `Traccia`** (sue note su
  `4.43-01`, il suo mockup e le sue risposte dello stesso giorno: *vale anche quando non contorna
  niente*; la chiave resta `draw_outline`).
  - **L'ordine è Tratteggio, Traccia, Riempimento**, su cinque colonne allineate con gli strumenti
    di disegno: Spessore entra nella `4.47` al posto di Luminosità (`4.45` e `4.46`), e dalla `4.49` è la terza; la quinta è vuota fino alla `4.50`, dalla `4.60` è `Elimina`, dalla `4.80` è `Sfocatura`, nella `4.91` di nuovo `Elimina`, e dalla `4.92` è vuota, perché `Elimina` è un tasto di testo accanto a `Elimina tutto`.
  - **Traccia e Tratteggio** sono una banda dello stesso spessore da bordo a bordo del tasto, senza
    filo: piena del colore della linea, tratteggiata in grigio scuro. **Riempimento** è un
    rettangolo della forma del tasto, staccato da un filetto, sopra la scacchiera; per il lettore di
    schermo resta `Riempimento` (sua risposta `D4`).
  - **La tavolozza è la sua**: `#FF4C3F`, `#FFBF00`, `#5ACB8C`, `#3EB7FF`, `#846AE2`, `#CC6898`
    (nome `Rosa`, chiave nuova `ink_pink`), poi bianco e nero per ultimi. Il salmone è uscito.
  - **Valori di fabbrica**: traccia `#FF4C3F` (`D1`) e riempimento `#33FFBF00`, l'ambra al 20%
    (`D2`), così all'apertura i due tondi di fabbrica risultano scelti.
- **Dalla `4.45` Luminosità e il grigio** (sue note sul giro della `4.44` e risposta `D3`):
  - **Luminosità** è il quarto tasto: acceso, il cursore schiarisce (a destra) o scurisce (a
    sinistra) il colore del bersaglio scelto, Traccia o Riempimento, fermandosi al 15% e all'85% di
    luminosità, senza toccare tinta e opacità (`Draw.lit`). Il tondo resta scelto, e scegliere un
    tondo nuovo riporta lo scostamento a zero. Il disegno del tasto è il colore che il cursore
    muove, dal più scuro al più chiaro. Limiti e azzeramento sono scelte della sessione,
    dichiarate nella voce di collaudo.
  - **Il grigio** (`#B3B3B3`, chiave `ink_grey`) è fra il bianco e il nero: 'grigio 30%' letto
    come tinta al 30% di nero, lettura dichiarata. Con dieci posti la fila dei tondi si stringe
    sugli schermi stretti, fino a 32dp per tondo.
- **Dalla `4.46` la freccia tratteggiata comincia dalla punta** (sua nota B sul giro della `4.45`,
  con un disegno della freccia sbagliata e di quella giusta): contro la punta l'asta è piena per
  quanto le alette arrivano lungo di lei, più uno spessore per le loro estremità tonde; da lì il
  tratteggio va verso la coda cominciando con un vuoto, e il pezzo parziale cade alla coda. Così
  non resta un buco fra l'ultimo trattino e la punta, e il primo trattino esce dall'angolo
  rientrante. Un'asta più corta di quel pezzo è piena. La linea tratteggiata non cambia. La
  misura la fa `FrecciaTest`.
- **Dalla `4.47` il tasto Spessore, la luminosità sui tondi e l'aggancio** (sue note sul giro della
  `4.45`, risposte `L1a`, `L2a`, `L3a` e `S1`):
  - **Il quarto tasto è Spessore**, al posto di Luminosità: una linea del colore della traccia,
    più spessa col cursore. **Traccia, Riempimento e Spessore sono tre scelte esclusive** (`S1`) di
    che cosa regola il cursore: l'**opacità della linea** (nuova, piena di fabbrica), l'opacità
    del riempimento, lo spessore. La tavolozza regola il riempimento con Riempimento scelto, e la
    linea negli altri due casi: lettura della sessione, dichiarata nella voce di collaudo.
  - ⚠️ **L'opacità della linea vale per l'elemento intero**, posata con un livello: dove la punta
    della freccia incrocia l'asta, o una mano libera ripassa su sé stessa, il colore resta uguale.
  - **La luminosità si sceglie tenendo premuto un tondo**, con la pressione lunga di sistema
    (`L1a`): compare un cursore sotto la tavolozza, con accanto il colore che ne risulta. Se il dito
    scorre è un gesto solo, e quando il dito si stacca il cursore si chiude (`L2a`); se il dito si
    alza fermo il cursore resta, e un tocco fuori lo chiude (`L3a`). ⚠️ Dalla `4.63` il valore si
    posa mentre il dito scorre, e un elemento scelto cambia colore sotto il dito (sua nota su
    `4.61-04`); fino alla `4.62` si posava allo stacco. Il
    cursore è un menu, quindi quel tocco non arriva al palco. Il tondo resta del suo colore base, e
    un tocco successivo gli rende la luminosità base. Tenere un tondo non scelto lo sceglie prima.
  - **Linee e frecce si agganciano all'orizzontale e alla verticale** (nota A): entro 5 gradi
    dall'asse, misurati sullo schermo, il capo si posa esattamente sull'asse, e una linea guida
    color accento attraversa l'immagine finché il dito resta giù. I 5 gradi sono una scelta della
    sessione, dichiarata.
- **Dalla `4.49` Spessore è a sinistra di Riempimento, e il cursore della luminosità sopra i
  tondi** (sue note sul giro della `4.48`): l'ordine dei tasti è Tratteggio, Traccia, Spessore,
  Riempimento (`4.47-01`); il cursore compare sopra la fila dei tondi, perché sotto il dito che
  tiene il tondo lo copriva, ed è più corto della fila di 24 dp per lato (`4.47-03`).
- **Dalla `4.50` le correzioni del giro della `4.49`** (sue note `4.49-01`, `4.49-02` e A-F):
  - **Valori di fabbrica** (nota A): rettangolo arrotondato con la linea tratteggiata, traccia
    rossa con la luminosità al 25% della corsa del cursore (-0,5), l'opacità al 50% (55%, su un
    cursore che va dal 10 al 100%) e lo spessore al 40%; il riempimento resta l'ambra al 20%.
    'Cursore al X%' è letto come un posto sulla corsa: lettura dichiarata. La luminosità di
    fabbrica vale per il rosso di partenza, perché un tocco su un tondo gli rende il suo colore.
  - **La linea del tasto Spessore è spessa quanto il tratto sullo schermo** (`4.49-01`): lo
    spessore sul lato lungo dell'immagine com'è mostrata, quindi segue lo zoom, mai sotto i 2 dp e
    mai oltre l'altezza del tasto.
  - **L'anteprima di Spessore è una lineetta curva** (nota D), alla misura vera, al posto del tondo
    pieno.
  - **Il cursore della luminosità si apre sopra la fila dei tondi e la copre**, largo quanto lei,
    con il colore che ne risulta a lato (`4.49-02`); **il pollice va sotto il dito** (nota C): la
    luminosità è il posto del dito sul cursore, e non più lo spostamento aggiunto al valore di
    partenza, che da un tondo vicino al bordo non lasciava spazio per andare dall'altra parte.
  - **I tondi svaniscono mentre una superficie si apre sopra l'editor** (nota F): seguono
    l'avanzamento del velo, come la scacchiera del visualizzatore, così sotto la sfocatura i loro
    colori non sbordano dai dialoghi.
  - **Gli Stili sono sempre l'ultimo modulo a destra** (nota E), anche dove la fila è stata
    riordinata: il Disegno è penultimo, e un gettone degli Stili trascinato altrove nelle
    impostazioni torna in fondo (`stylesLast`, in `Settings.kt`).
  - **Il tratteggio più evidente** (nota B): sua risposta `B2`, entrata nella `4.60`.
- **La traccia di rettangoli ed ellissi è centrata sul bordo** (risposta alla sua domanda del
  2026-10-07): Android la disegna metà dentro e metà fuori, e la metà interna passa sopra il
  riempimento, che copre l'intera forma.
- **Sopra e sotto fra gli oggetti** (sua nota dello stesso giorno: *una versione semplificata di
  Z-index andrà gestita in qualche modo*): dalla `4.70`, nel menu della pressione lunga.
- ⚠️⚠️ **Dalla `4.91` la Sfocatura di rettangoli ed ellissi non c'è più: è diventata lo strumento
  `Pannello`** (voce più sotto). Quello che segue descrive la Sfocatura com'era dalla `4.80` alla
  `4.90`; il conto e la forma dell'area valgono per il Pannello.
- **Dalla `4.80` lo strumento Sfocatura** (sua specifica del giro della `4.43`: *una selezione
  tipo rettangolo arrotondato, che anziché riempire la propria area di un colore la sfoca. È una
  cosa che userei spesso per l'oscuramento di parti di immagini che voglio nascondere prima della
  condivisione*):
  - **è un interruttore nella quinta colonna dei tasti**, dove dalla `4.60` alla `4.70` c'era
    `Elimina`, che vive nel menu della pressione lunga (lettura della sessione, dichiarata nella
    voce di collaudo). Come gli altri parametri vale per l'elemento scelto, o per il prossimo
    disegnato (*anche se non esiste ancora la selezione*). Vale solo per rettangolo ed ellisse
    (*Si applica solo agli oggetti con un'area*), e per gli altri strumenti è spento;
  - **acceso, l'elemento ignora traccia e riempimento** (che tiene: spento, torna com'era), e
    l'area dentro di lui è sfocata. Si spengono i tasti e i tondi che li regolano, e il cursore
    regola l'entità, dallo 0,5 al 25% del lato maggiore dell'elemento; di fabbrica il 10%, scelta
    della sessione dichiarata;
  - **sfoca l'immagine con le altre modifiche e senza gli altri elementi** (*per definizione la
    sfocatura sarà sempre al livello più basso*): si posa prima dello sviluppo, come Correggi
    (`Draw.blurAreas`, sul palco, nel pezzo a piena risoluzione e nel salvataggio), quindi i
    cursori del colore sviluppano l'area sfocata, geometria e ritaglio la portano con l'immagine, e
    gli altri elementi le si disegnano sopra. Un tocco prende prima gli altri elementi, e la
    sfocatura si prende anche dentro;
  - **il conto**: l'area intorno all'elemento si dimezza finché il raggio è di pochi pixel, si
    sfoca con tre passate a scatola e si riposa attraverso il contorno dell'elemento, stirata con
    un filtro bilineare. Una sfocatura di centinaia di pixel costa come una di pochi;
  - `Copia` e `Incolla` portano la sfocatura fra due forme chiuse, come il riempimento;
  - ⚠️⚠️ **nella `4.80` l'area sfocata del rettangolo era storta** (suo `Non approvato` su
    `4.80-01`): nel contorno di `Draw.outline` gli archi degli angoli in alto a destra e in basso a
    sinistra erano percorsi al contrario, e il contorno si incrociava da solo. La `4.70` lo usava
    solo per l'ingombro, dove l'ordine dei punti non conta. Corretto nella `4.81`, con la prova
    `l'area sfocata ha la forma dell'elemento`, che guarda ogni pixel dentro e fuori, con angoli
    larghi: quella della `4.80` aveva angoli di 6 pixel, e la forma storta quasi non si vedeva.
- **Poi il testo (G3) e la pillola**: le loro specifiche vivono nel brief.
- **I caratteri del testo (G3) si scelgono da un [artefatto](https://claude.ai/artifact/BnskaC7AgjgE5Dn23RVmoe)**
  con dodici caratteri liberi, Roboto di fabbrica (sua richiesta del 2026-10-08), più gli stili
  grassetto, corsivo, barrato, evidenziato ed etichettato; la scelta è sua e non è ancora
  arrivata.
- **Dalla `4.60` la G2, prima parte: scegliere, cambiare, spostare, eliminare** (sua specifica:
  *un tocco singolo seleziona un oggetto; il rettangolo selezionato mostra 4 vertici color accento,
  la freccia 2 punti color accento; con un oggetto selezionato, i parametri cambiano
  quell'oggetto*). Il numero è passato dalla `4.50` alla `4.60` perché le correzioni dei giri
  hanno preso le versioni in mezzo. Le scelte della sessione, dichiarate nella voce di collaudo:
  - **un tocco su un elemento lo sceglie**: il più in alto sotto il dito, con 24 dp di portata oltre
    metà della sua linea; una forma riempita si prende anche dentro, una vuota solo vicino alla
    linea, la freccia per l'asta (`Draw.hit`). Un tocco nel vuoto toglie la scelta, e se c'era una
    scelta non lascia il punto della mano libera;
  - **i punti**: quattro vertici del riquadro per rettangolo ed ellisse, i due capi per linea e
    freccia, i quattro vertici del riquadro per la mano libera; tondi color accento a misura fissa
    sullo schermo, con un filo bianco, portati dalla stessa geometria dell'immagine (`Warp.to`). Sono
    del palco, quindi il file salvato non li contiene;
  - **scegliere un elemento carica i suoi parametri nel modulo** (il tondo, la luminosità,
    l'opacità, lo spessore, il tratteggio, il riempimento), che restano poi per l'elemento nuovo.
    L'elemento ricorda
    la ricetta del suo colore (`Tint`), così il tondo scelto e i cursori tornano dove erano;
  - **ogni cambio vale per l'elemento scelto** ed entra nella storia dopo 400 ms di quiete, così un
    cursore trascinato è un passo solo;
  - **un tasto strumento toglie la scelta** e cambia strumento: lo strumento non è un parametro
    dell'elemento;
  - **trascinare partendo dall'elemento scelto lo sposta**; partendo altrove si disegna come prima;
  - **`Elimina`** è un tasto di testo a sinistra di `Elimina tutto`, con ogni strumento, dalla `4.92`
    (suo `Non approvato` su `4.91-04`: *Se sto usando la pillola o ne ho una selezionata, il tasto
    'Elimina' non appare ... Potrebbe essere anche un pulsante testuale a sinistra di 'Elimina
    tutto'*, e la sua risposta a `d-elimina-testo`). È acceso solo con un elemento scelto, e resta
    anche nel menu della pressione lunga. Fino alla `4.70` e nella `4.91` era la quinta colonna dei
    tasti delle forme, che la fila del testo non ha; dalla `4.80` alla `4.90` viveva solo nel menu.
    Annulla, Ripeti e Originale tolgono la scelta, perché dopo di loro l'indice può essere di un
    altro elemento.
- **Dalla `4.60` il tasto Tratteggio dice se è acceso** (sua risposta `B2` alla nota B sul giro
  della `4.49`): acceso, il tratteggio nel colore della traccia; spento, il grigio scuro sbiadito al
  35%. Fino alla `4.50` i due stati avevano lo stesso disegno e cambiava solo lo sfondo del tasto.
- **Dalla `4.61` le correzioni del giro della `4.60`** (sue note A-D):
  - **Il tasto acceso ha un bordo pieno color accento di 2 dp** (nota A), negli strumenti e nei
    tasti, oltre allo sfondo verde; è disegnato sopra il tasto, così le bande da bordo a bordo non
    lo interrompono. I 2 dp sono una scelta della sessione, dichiarata.
  - **Il velo d'aiuto del Disegno** (nota B): la prima volta che il modulo si apre, dopo le due
    slide dell'editor, il tondo rosso cerchiato d'arancione e il suo testo sopra, alla lettera
    (`hint_draw`, chiave `draw-hint-seen`). Nomina anche il testo e la pillola, che non ci sono
    ancora: entra com'è perché le lingue si scrivono una volta sola, lettura dichiarata. **Dalla
    `4.92` nomina anche i pannelli sfocati** (sua risposta `A1` a `d-velo-pannello`), in fondo
    all'elenco degli strumenti, in tutte le lingue; la chiave non cambia, quindi chi l'ha già visto
    non lo rivede.
  - **Il tratteggio del tasto comincia e finisce con un trattino** (nota C): il numero dei
    trattini è il più vicino al ritmo 10 e 6 del suo mockup, e trattino e spazio si allungano o si
    accorciano insieme fino a chiudere sui due bordi (`keyDashes`).
  - **Il cursore della luminosità non ha velo né sfocatura** (nota D): prende l'ombra della scelta
    'Ombra' delle impostazioni, l'unica dell'app, così l'elemento scelto e i tasti si vedono
    cambiare mentre il dito scorre.
- **Dalla `4.62` gli elementi si appoggiano ai bordi dell'immagine** (sua richiesta del
  2026-10-08, con un esempio: un rettangolo arrotondato tratteggiato appoggiato all'angolo in alto
  a sinistra, *a filo del bordo (totalmente visibile e a 0 pixel di distanza dal bordo)*):
  - quando la traccia di un elemento, col suo spessore e con la punta della freccia, arriva entro
    12 dp da un bordo dell'immagine, da dentro o da fuori, si posa sul bordo: a filo e tutta
    visibile. Più lontano l'elemento va oltre il bordo, e se ne vede solo la parte dentro;
  - vale disegnando (il primo punto e quello sotto il dito, per i quattro strumenti a due punti; la
    mano libera segue il dito) e spostando l'elemento scelto. Una linea agganciata
    all'orizzontale o alla verticale tiene la sua direzione, e si appoggia solo lungo l'altro asse;
  - mentre il dito tiene un elemento appoggiato, la guida color accento corre lungo quel bordo, e
    allo stacco sparisce (`Draw.rest`, `Draw.restEnd`, `ImageEdge`);
  - il conto è sullo schermo, sul riquadro dell'immagine come la si vede, ritaglio compreso; con
    la Geometria accesa porta a filo gli angoli dell'elemento;
  - scelte della sessione, dichiarate nella voce di collaudo: i 12 dp, e il primo tocco che deve
    ancora cadere dentro l'immagine.
- **Dalla `4.90` gli elementi si allineano anche agli altri elementi** (sua richiesta del
  2026-10-08: *le guide dinamiche sono eccezionali e funzionano davvero bene. Voglio che mi
  propongano di allineare dinamicamente gli elementi a lati/centro/estremi di altri elementi già
  presenti*):
  - un lato che si muove, o il centro quando si muove l'elemento intero, si posa sul lato o sul
    centro di un altro elemento entro gli stessi 12 dp del bordo; su ogni asse vince il bersaglio
    più vicino e, a parità, il bordo dell'immagine. Il riquadro di un elemento è quello della sua
    traccia, come per il bordo;
  - vale disegnando (l'inizio e la fine), spostando, e tirando una maniglia in `Trasforma`: il lato
    tirato si appoggia ai bordi e agli altri elementi. Fino alla `4.81` tirando una maniglia
    l'elemento non si appoggiava a niente. Un rettangolo o un'ellisse girati, tirati per una
    maniglia, non si appoggiano: il loro riquadro non segue la maniglia;
  - mentre il dito tiene l'elemento, una guida color accento va da un elemento all'altro lungo il
    lato o il centro che hanno in comune (`Draw.rest` con gli altri elementi, `Draw.lines`);
  - lettura della sessione, dichiarata nella voce di collaudo: la stessa portata del bordo, e le
    maniglie comprese (lettura B1).
- **Dalla `4.64` 'Azzera' si chiama `Elimina tutto`** (sua risposta in chat del 2026-10-08:
  *Allora può restare, ma rinominalo in 'Elimina tutto'*): svuota il disegno, mentre il tasto in
  fondo azzera tutti i moduli, e la parola era la stessa. La pressione lunga sul gettone del
  Disegno fa lo stesso lavoro e, come lui, toglie la scelta dell'elemento: fino alla `4.63` la
  scelta restava su un elemento che non c'era più, `Elimina` restava acceso, e l'elemento
  disegnato dopo nasceva scelto. La chiave è nuova (`draw_clear`), perché `editor_original`
  resta agli altri 'Azzera'.
- **Dalla `4.70` la G2, seconda parte: il menu della pressione lunga, le maniglie e la rotazione**
  (sua nota E sul giro della `4.60` e sue risposte su `4.64-03` e `4.64-04`):
  - **tenendo fermo un elemento lo si sceglie e si apre il suo menu**, col disegno del menu della
    pressione lunga sull'immagine nel visualizzatore e senza la parte sopra: sei icone con la
    parola, su due righe da tre, nell'ordine della sua nota letto per righe (`Sposta sopra`,
    `Copia`, `Duplica`; `Sposta sotto`, `Ruota`, `Elimina`). Le parole seguono l'impostazione dei
    riquadri, come nel visualizzatore. Tenendo fermo il vuoto il gesto resta quello di prima;
  - **`Copia` copia lo stile** (sua risposta: *È C2 (stile)*) e al suo posto compare `Incolla`,
    che lo posa su ogni elemento scelto finché lo si tiene premuto: allora la memoria si svuota,
    l'avviso dice `Stile in memoria eliminato.` e torna `Copia`. Passano colore, luminosità e
    opacità della traccia, lo spessore e il tratteggio; il riempimento solo fra rettangoli ed
    ellissi. La memoria dura finché l'editor è aperto, e lo stile incollato è un passo di
    'Annulla';
  - **`Duplica` posa la copia sopra l'originale, scostata del 3% del lato lungo in basso a destra,
    e la sceglie**; **`Sposta sopra` e `Sposta sotto` scambiano l'elemento col vicino**, e in cima
    o in fondo sono spenti;
  - **le maniglie sono otto**: i quattro vertici e il mezzo di ogni lato, per rettangolo, ellisse e
    mano libera; i due capi per linea e freccia. In `Trasforma` (la modalità di partenza) un
    vertice muove i suoi due lati e l'opposto resta, il mezzo di un lato muove quel lato; i lati
    sono quelli dell'elemento, quindi un rettangolo girato si allunga lungo sé stesso. Un capo di
    linea o di freccia si aggancia all'orizzontale e alla verticale come quando la si disegna. La
    mano libera trascinata oltre il lato opposto si specchia;
  - **`Ruota` mette le maniglie in rotazione, e il tasto diventa `Trasforma`**: trascinando una
    maniglia l'elemento gira attorno al centro del suo riquadro, libero, e si aggancia ai multipli
    di 45 gradi entro 5 gradi (la R2 della sua domanda). In rotazione le maniglie sono vuote. La
    modalità torna `Trasforma` quando la scelta cambia;
  - **dalla `4.81` anche un tocco sull'elemento scelto alterna `Trasforma` e `Ruota`** (sua nota su
    `4.70-04`: *un tap singolo su un oggetto già selezionato lo fa passare ciclicamente da
    trasformazione e rotazione*); il tasto del menu resta, e dice la modalità a cui porta;
  - **in tutte e due le modalità trascinare l'elemento lo sposta** (sua frase: *In entrambe le
    modalità immagino possibile anche lo spostamento*), e una maniglia vince sul corpo entro 16 dp;
  - letture della sessione, dichiarate nella voce di collaudo: l'ordine per righe, le maniglie dei
    lati, i 16 dp, lo scostamento della copia, i 45 gradi, la memoria che dura quanto l'editor, e
    l'appoggio ai bordi, che fino alla `4.81` valeva disegnando e spostando ma non tirando una
    maniglia.
  - ⚠️ **Rettangolo ed ellisse tengono un angolo** (`Mark.angle`), e la tela gira prima di
    disegnarli; linea, freccia e mano libera girano i loro punti. Il giro si conta nei pixel
    dell'immagine originale, non nelle sue frazioni, o su un'immagine non quadrata la forma si
    deformerebbe. Le prove sono in `DisegnoTest`.
- **Dalla `4.90` la G3, il Testo** (sue scelte del 2026-10-08 sull'artefatto dei caratteri: *tengo:
  Roboto, Montserrat, Archivo Narrow, Literata*, *nome dello stile: N1 Etichetta*, e *colori di
  fabbrica dell'etichetta: testo #FFFFFF, striscia #A3408F; evidenziatore #FFE15A*):
  - **il sesto strumento è `Testo`**: un tocco sul vuoto apre una finestra modale in cui si scrive,
    come `Rinomina`; `Applica` aspetta una lettera, e il testo nasce centrato dove si è toccato, già
    scelto. Nasce largo quanto la sua riga più lunga, quindi va a capo dove lo manda Invio. Un
    trascinamento con `Testo` non disegna niente (letture `A1` e `A8`);
  - **i quattro caratteri** vivono in `res/font`, un file dritto e uno corsivo per ciascuno, a peso
    variabile (400 il normale, 700 il grassetto); Roboto è quello di fabbrica. Le licenze, tutte SIL
    Open Font License, sono in `docs/fonts`. Si caricano una volta sola (`Faces`), dall'editor e dal
    salvataggio, perché il disegno si dipinge dove un contesto non c'è;
  - **i sette tasti** sotto gli strumenti, in quest'ordine dalla `4.91`: `Carattere` passa al
    carattere dopo a ogni tocco e mostra il suo 'Aa'; `Grassetto`, `Corsivo` e `Barrato` si
    accendono e si combinano; `Sfondo` accende e spegne la striscia dell'etichetta; `Allineamento`
    passa dal centro, che è quello di fabbrica, a sinistra e a destra, e vale anche per la pillola e
    il pannello (sua `B2` del 2026-10-08, il tasto a giro è una lettura della sessione; il ciclo è
    il suo dalla `4.92`, nota A sul giro della `4.91`: *il primo tocco deve portare il testo a
    sinistra, poi destra*, e fino alla `4.91` andava prima a destra); `Testo` riapre la
    finestra con le parole del testo scelto (lettura `A3`), ed è l'ultimo (sua nota E sul giro della
    `4.90`: *deve essere l'ultima icona a destra*). La fila ha otto colonne, come gli strumenti;
    - ⚠️ **fino alla `4.90` `Sfondo` si chiamava `Fondo` e passava da nessuno a `Evidenziato` e a
      `Etichetta`**, e `Testo` si chiamava `Modifica testo` e veniva prima di `Allineamento`.
      L'evidenziatore è uscito con la `4.91` (sua nota su `4.90-04`: *'Etichetta' è talmente ben
      fatta che 'Evidenziato' non serve più a niente, possiamo liberarcene!*), e i due nomi sono le
      sue riscritture delle etichette;
  - **il colore delle parole** si sceglie coi tondi della tavolozza, bianco di fabbrica, con la sua
    luminosità; le parole sono sempre opache. **La dimensione** si regola col cursore `Dimensione`,
    fra l'1% e il 150% del lato lungo, e con le maniglie d'angolo e quelle a metà dei lati di sopra
    e di sotto, che ingrandiscono e rimpiccioliscono il testo intero intorno al suo centro: un testo
    stirato su un asse sarebbe un carattere deformato (letture `A2` e `A4`).
  - ⚠️⚠️ **dalla `4.92` un testo nuovo nasce con la riga più lunga larga metà dell'immagine** (sua
    nota su `4.91-03`: *forse è bene stabilire una dimensione predefinita dei testi: la riga più
    lunga deve misurare il 50% della larghezza dell'immagine*; `Draw.fitSize`), fino al tetto del
    150%; fino alla `4.91` nasceva al 5% del lato lungo. Quindi `Dimensione` regola il testo scelto
    ed è spento senza un testo scelto (lettura della sessione, dichiarata). Lo stesso corpo prendono
    le parole di una pillola o di un pannello posati da un tocco, e anche questa è una lettura della
    sessione;
  - ⚠️⚠️ **dalla `4.92` cambiare il colore non riporta il testo al corpo di prima** (sua nota su
    `4.91-03`: *Se ingrandisco il testo e poi cambio colore, il colore si applica ma il testo
    ritorna piccolo come in origine*): una maniglia cambiava il corpo dell'elemento e non quello del
    modulo, e il primo tondo toccato posava sull'elemento il corpo vecchio. Alla fine di ogni gesto
    sul palco il modulo ricarica i parametri dell'elemento scelto; la prova è in `DisegnoTest`.
  - ⚠️⚠️ **il tetto è il 150% dalla `4.91`, e fino alla `4.90` era il 20%** (sua nota su `4.90-02`:
    *voglio poter fare un testo grande come l'intera immagine e anche oltre*; il numero è della
    sessione, dichiarato): una lettera più alta dell'immagine. Il cursore moltiplica il corpo per lo
    stesso fattore a ogni tratto (`Draw.textTrack`), perché su una pista uniforme dall'1% al 150% i
    corpi con cui si scrive (dal 2 al 10%) starebbero nel primo ventesimo, sotto la larghezza di un
    dito; il corpo di fabbrica cade a un terzo della pista;
  - ⚠️⚠️ **le maniglie a metà dei lati sinistro e destro cambiano la larghezza, e le parole vanno a
    capo da sé, dalla `4.90`** (sua `B3` del 2026-10-08: *Aggiungo volentieri B2 e B3 sul testo*):
    il lato opposto resta fermo, e la larghezza non scende sotto un corpo. La larghezza è del testo,
    come le parole: lo stile copiato da un altro testo non la cambia, e una maniglia d'angolo la
    ingrandisce insieme al corpo, così le righe restano quelle (lettura della sessione, dichiarata
    nella voce di collaudo); Il testo ha colore e dimensione suoi, distinti da
    quelli della traccia, così dopo un rettangolo rosso non si scrive in rosso;
  - **la fila delle strisce** compare sotto il cursore quando `Sfondo` è acceso: sei colori per
    l'etichetta, scuri perché reggono parole bianche, col suo di fabbrica in testa (fino alla `4.90`
    c'erano anche i sei chiari dell'evidenziatore). Si offrono solo quelli che staccano dall'immagine
    sotto il testo e tengono leggibili le parole; gli altri si vedono sbiaditi, così la fila non
    cambia lunghezza sotto il dito (letture `A5` e `A6`, `Draw.readable`);
    - ⚠️⚠️ **dalla `4.92` le strisce sono rettangoli affiancati senza spazio, alti 20 dp, e la loro
      fila ha il suo posto con ogni strumento** (sua nota E sul giro della `4.91`: *le due file di
      tasti principali del modulo si avvicinano fino a toccarsi ... Rendi diversamente la barra dei
      colori inferiore: rettangoli colorati affiancati, senza distanziamento, più bassi ... posiziona
      lo slider nella stessa posizione anche con gli altri strumenti*). La scheda è alta quanto il
      modulo più alto, misurato una volta, e la fila che compariva con `Sfondo` si prendeva l'aria
      fra le righe; adesso il posto c'è sempre, vuoto quando le strisce non servono, quindi il
      cursore e le file dei tasti non si muovono da uno strumento all'altro. Fino alla `4.91` le
      strisce erano tondi alti come quelli della tavolozza. Misure della sessione, dichiarate;
    - ⚠️⚠️ **dalla `4.94` la striscia scelta ha il filo chiaro e il colore stondati, concentrici al
      bordo scuro** (sua nota sul giro della `4.93`, col suo disegno: *Disegna meglio il selettore del
      colore 'secondario' in basso*): il bordo scuro riempie la striscia, e il filo e il colore girano
      con un raggio, così di fuori il bordo segue lo stondamento della fila e di dentro il filo lo
      segue a sua volta. Fino alla `4.93` filo e colore erano rettangoli a spigolo vivo;
  - ⚠️⚠️ **dalla `4.94` le file del modulo stanno a 12 dp l'una dall'altra** (sua nota sul giro della
    `4.93`, col suo mockup: *Disponi meglio gli elementi dell'interfaccia: c'è spazio per tutto*): i
    gettoni dei moduli, gli strumenti, i tasti, i tondi, il nome del cursore con `Elimina` ed `Elimina
    tutto`, il cursore e le strisce. Il nome e il cursore restano attaccati, in una fila alta 32 dp
    come i tasti, e sotto le strisce c'è un po' più d'aria, come nel suo disegno. Fino alla `4.93`
    ogni fila teneva solo il suo margine, quindi i tasti quasi si toccavano e l'aria si raccoglieva
    sotto il cursore. Il Disegno è il modulo più alto, quindi la scheda cresce con lui: 17 dp,
    misurati sul banco su uno schermo largo 320 dp. Misure della sessione, prese sul suo mockup e
    dichiarate;
  - ⚠️⚠️ **dalla `4.92` il tasto `Sfondo` è un rettangolino arrotondato con 'Aa' in negativo** (sua
    nota F sul giro della `4.91`: *Il pulsante etichetta è troppo simile a quello del carattere*): le
    lettere sono il fondo del tasto, il rettangolo ha il colore della striscia scelta quando il tasto
    è acceso e quello del tasto quando è spento;
    - ⚠️⚠️ **le due misure sono diverse, di proposito**: le parole si leggono per la luminosità,
      quindi contro il fondo vale il contrasto delle WCAG, almeno 3; il fondo stacca dall'immagine
      anche per la tinta, quindi contro l'immagine vale la distanza dei colori in CIELAB, almeno 20.
      Col contrasto su tutte e due, il giallo dell'evidenziatore (1,3 contro il bianco) non si
      sarebbe offerto su una pagina bianca, che era il suo posto;
  - **accendendo `Sfondo`, le parole bianche o nere prendono quella delle due che si legge meglio
    sulla striscia**, e le parole di un altro colore restano, se si leggono (lettura della sessione,
    `Draw.wordsOn`);
  - **l'etichetta** è una striscia stondata per riga, e le strisce si fondono in una forma sola, con
    un'ombra morbida sotto, come nel suo esempio;
    - ⚠️⚠️ **dalla `4.92` gli angoli sono tondi solo dove una striscia sporge, e dove una riga è più
      stretta della vicina l'incastro ha un raccordo concavo** (sua nota D sul giro della `4.91`, col
      suo mockup: *gli arrotondamenti non devono stare nelle linee intermedie, anzi lì ci vorrebbe un
      arrotondamento contrario, che crea una maggiore armonia*). Due lati entro mezzo pixel
      continuano dritti; quando due lati sono vicini, il tondo e il raccordo prendono metà della
      distanza, così non si incrociano (`Draw.labelGround`). Fino alla `4.91` ogni striscia aveva
      quattro angoli tondi, e fra due righe restava una tacca;
  - ⚠️⚠️ **le righe si centrano sulla metà della H sopra la linea di base, per tutti e quattro i
    caratteri** (sua nota: *Literata ha una baseline stranamente bassa: credo sia l'unico font per
    il quale sarà necessario aggiustare la centratura verticale dell'etichetta*). La causa è nelle
    metriche del file: Literata dichiara un'ascesa di 1177 unità per maiuscole di 700, quindi
    centrata sul riquadro del carattere la sua riga scende di quasi 7 pixel su un corpo di 80, e le
    altre tre di un pixel e mezzo al più. La regola generale toglie la correzione per un carattere
    solo, e la voce di collaudo lo dichiara (lettura `A7`);
    - ⚠️⚠️ **dalla `4.92` un testo fatto soprattutto di minuscole si centra sulla metà della x** (sua
      nota B sul giro della `4.91`: *mi piacerebbe che la centratura fosse 'ottica', sui pixel reali
      del peso maggiore del testo inserito*, col mockup di una pillola alta 126 px sopra le minuscole
      e 96 sotto). Vale per il testo, l'etichetta, la pillola e il pannello, con una fascia sola per
      tutto il testo, perché una per riga spazierebbe le righe in modo diverso; quale lettera conta
      lo decide la maggioranza fra minuscole da una parte e maiuscole e cifre dall'altra (lettura
      della sessione, dichiarata; `Draw.core`);
  - **`Ruota`, `Trasforma`, il menu della pressione lunga, `Copia` e `Incolla`** valgono come per
    gli altri elementi: il testo gira intero, con le righe dritte fra loro. Lo stile passa intero
    fra due testi, parole escluse; fra un testo e una forma passa solo il colore, con la sua ricetta;
  - **il testo si appoggia** ai bordi e agli altri elementi quando lo si sposta, col suo riquadro;
    tirandone una maniglia no.
- **Dalla `4.90` anche la Pillola**, il settimo strumento (sua nota A sul giro della `4.43`: *uno
  strumento 'pillola', ovvero un contenitore di testo 'standard', che posso aggiungere senza dover
  configurare ogni volta tratto, riempimento, opacità*, con i suoi valori ARGB):
  - **un trascinamento disegna la pillola come un rettangolo**, con gli stessi appoggi ai bordi e agli
    altri elementi, e subito dopo la finestra chiede le parole; **un tocco** chiede le parole e posa
    una pillola che le contiene, alla `Dimensione` scelta per il testo, centrata dove si è toccato.
    La pillola nasce scelta; `Annulla` nella finestra la lascia vuota;
  - ⚠️⚠️ **dalla `4.92` le parole non sono obbligatorie, per la pillola e per il pannello** (sua nota
    C sul giro della `4.91`: *Se inserisco un testo, poi cambio idea e cancello tutto, devo poter
    cliccare su 'Applica' anche con il campo vuoto*): `Applica` si accende anche a campo vuoto, e il
    riquadro resta senza parole. Un tocco a campo vuoto non posa niente, perché un tocco misura la
    pillola sulle sue parole (lettura della sessione, dichiarata). Per il testo `Applica` aspetta
    ancora una lettera;
  - **l'aspetto è fisso**: riempimento `#ccff4b3d`, due tracce dello 0,5% del lato maggiore, la chiara
    `#e6fffefa` dentro e la scura `#e6373737` sul bordo, e sotto il vetro, cioè l'immagine sfocata
    (`Draw.blurAreas`, come il Pannello). Le parole sono bianche opache. `Dimensione` è spento, non
    nascosto; dei tasti del testo valgono `Carattere`, `Grassetto`, `Corsivo`, `Barrato`,
    `Allineamento` e `Testo`, e `Sfondo` è spento;
  - ⚠️⚠️ **dalla `4.91` il colore si sceglie** (sua nota su `4.90-05`: *vorrei solo che i colori
    rimanessero attivi e che si potesse selezionare il colore di sfondo della pillola, ma devo avere
    a disposizione una palette diversa (proponi tu: tutti colori 'stravaganti', neon e ben visibili);
    gli altri parametri come bordo, trasparenza, ecc. restano invariati*): i tondi restano accesi e
    mostrano otto colori della pillola, il suo rosso per primo, poi arancio, rosa, magenta, indaco,
    blu, verde acqua e verde (`Draw.PILL_INKS`), tutti all'opacità `cc` di sempre. Sono vivaci e non
    chiari: ognuno tiene le parole bianche a un contrasto di almeno 3, quindi il giallo e il lime
    fluo, che le nasconderebbero, sono fuori (scelta della sessione, dichiarata). Non hanno nome: il
    lettore di schermo li dice `Sfondo 1`...`Sfondo 8`, e tenerli premuti non apre la luminosità.
    Fino alla `4.90` la tavolozza era spenta;
  - ⚠️⚠️ **dalla `4.91` il vetro si vede anche sul palco**: fino alla `4.90` l'anteprima metteva
    sotto il vetro le sole forme sfocate, e la pillola lo aveva solo nel file salvato;
  - ⚠️ **le tracce sono dentro il riquadro**, così il riquadro disegnato è tutta la pillola e si
    appoggia come un rettangolo; le estremità sono tonde quanto possono, metà del lato corto;
  - **le parole vanno a capo da sé e prendono la misura più grande che il riquadro contiene**, fino a
    un minimo del 2% del lato lungo dell'immagine (lettura della sessione: il suo *minimo di
    leggibilità* era *da definire*). Parole che non ci stanno nemmeno al minimo allungano la pillola
    verso il basso, col lato di sopra fermo, invece di sparire tagliate;
  - ⚠️ **i 12 px del vetro sono una parte del lato maggiore della pillola** (lettura della sessione):
    12 px sullo schermo per una pillola larga mezzo telefono, circa 545 px, quindi il 2,2%. Un numero
    fisso di pixel sfocherebbe in modo diverso l'anteprima e il file, e quasi niente su una foto
    grande;
  - **stile**: fra un testo e una pillola passano il carattere, i tre stili e l'allineamento, mai il
    colore né il fondo; fra due pillole anche il colore (dalla `4.91`); fra una pillola e una forma
    non passa niente;
  - lettura della sessione, dichiarata nella voce di collaudo: la Pillola è uno strumento a sé, il
    settimo della fila, e non un quarto `Fondo` del testo, perché lui la chiama *strumento*.
- **Dalla `4.91` il Pannello**, l'ottavo strumento, al posto della Sfocatura di rettangoli ed ellissi
  (suo `Non approvato` su `4.81-01`: *Funziona in modo ECCELLENTE! Ma ho deciso un cambio di
  paradigma: l'area sfocata non sarà più attributo di ogni forma, bensì uno strumento a parte. Si
  chiamerà 'Pannello', ma nella pratica si tratta di un'altra pillola con uno stile diverso*):
  - **si disegna e si scrive come la pillola**: un trascinamento disegna il riquadro e poi chiede le
    parole, un tocco chiede le parole e posa un pannello che le contiene; le parole sono bianche,
    vanno a capo da sé e prendono la misura più grande che il riquadro contiene, coi tasti del
    testo (`Sfondo` spento) (*Supporto testo: esattamente come l'altra pillola*);
  - **la forma** è un rettangolo con gli angoli stondati dell'1% del lato lungo del pannello dalla
    `4.92` (sua nota su `4.91-01`: *0,3% di arrotondamento è troppo poco, facciamo 1%*; nella `4.91`
    lo 0,3%; del pannello e non dell'immagine è una lettura della sessione, dichiarata), senza
    tracce. Le parole stanno al 15% del lato corto dal
    bordo, meno della pillola, che perde spazio nelle estremità tonde (scelta della sessione);
  - **la sfocatura** è quella della Sfocatura, col suo cursore e i suoi numeri, dallo 0,5 al 25%
    del lato maggiore del pannello e al 10% di fabbrica (*come adesso, con lo stesso slider, che
    funziona benissimo*): il vetro si posa prima dello sviluppo, sul palco e nel file, e il pannello
    si disegna sopra al suo posto fra gli altri elementi, come la pillola. Fino alla `4.90` l'area
    sfocata era sotto tutti gli elementi; adesso un tocco prende il più in alto, come per gli altri;
  - **il colore** si sceglie fra gli otto della pillola, sempre al 20%, oppure `Nessuno`, che è il
    primo tondo ed è quello di fabbrica (*si applica sempre e solo al 20% di opacità e deve esserci
    anche 'nessuna'*; nessuno di fabbrica è una lettura della sessione, dichiarata: il pannello è
    prima di tutto una sfocatura);
  - **stile**: fra due pannelli passano colore, sfocatura, carattere, stili e allineamento; fra un
    pannello e una pillola o un testo il carattere, gli stili e l'allineamento; con una forma niente;
  - ⚠️ **un rettangolo o un'ellisse sfocati con la `4.80`-`4.90` non esistono fuori dall'editor**: il
    disegno vive finché l'editor è aperto, quindi non c'è niente da convertire.
- **Dalla `4.91` lo spostamento si centra sull'immagine** (sua nota A sul giro della `4.90`: *devono
  apparire anche delle guide per la centratura (che faccia fare uno scatto allo spostamento di un
  elemento quando è al centro verticale/orizzontale/entrambi dell'intera immagine)*): spostando un
  elemento, il suo centro entro i soliti 12 dp va sul centro dell'immagine, su un asse o su tutti e
  due, e una guida color accento attraversa l'immagine lungo quel centro. Vale solo per il centro
  dell'elemento e solo spostandolo: un lato che si ferma a metà dell'immagine, o un elemento che si
  disegna a partire da lì, sarebbero scatti che non ha chiesto (lettura della sessione, dichiarata;
  `Draw.rest` con `centred`, `Draw.lines` con la cornice).
- **Dalla `4.93` un doppio tocco su un testo, una pillola o un pannello apre la finestra delle sue
  parole**, come il tasto `Testo` (sua nota in Altro sul giro della `4.92`: *gli elementi che hanno
  un testo (o che potrebbero averlo) dovrebbero accettare come input un doppio tap, che equivale al
  tasto 'Testo' per inserire o modificare il contenuto testuale*), con qualunque strumento in uso:
  - il primo tocco fa subito quello che fa un tocco (sceglie l'elemento, o alterna `Trasforma` e
    `Ruota`); il secondo, sullo stesso elemento entro il tempo del doppio tocco di sistema, riprende
    quell'alternanza e apre la finestra. Così il tocco singolo non diventa più lento, e un
    trascinamento subito dopo un tocco resta un trascinamento (lettura della sessione, dichiarata
    nella voce di collaudo; `MarkTap`);
  - la prova è `un doppio tocco su un testo apre le sue parole`, in `DisegnoTest`.
- **Dalla `4.91` i glifi degli strumenti sono più grandi** (sua nota B sul giro della `4.90`: *le
  icone degli strumenti della prima fila sono diventate troppo piccole ... e hanno tutto lo spazio
  per essere ingrandite*): gli strumenti sono tasti come quelli sotto, col glifo di 28 dp in un
  tasto alto 32. Fino alla `4.90` erano i gettoni di Material, che tengono 8 dp per lato attorno
  all'etichetta, quindi il glifo aveva al più 24 dp, e meno con otto strumenti.

## 🎞️ I preset, venti di casa e quelli che si salvano

- ⚠️⚠️ **Uno stile è un aspetto con un nome**, che si porta da un'immagine all'altra: governa Luce,
  Colore, HSL, Dettaglio, Curve ed Effetti, e non include posa, ritaglio e geometria, che dipendono da
  come è stata scattata quell'immagine.
- ⚠️⚠️ **Gli stili di casa sono venti**: quattordici suoi, convertiti dai suoi XMP di Lightroom
  dalla sessione (sua istruzione; nell'APK non c'è un lettore XMP, entrano i valori già tradotti), e
  sei della sessione, cioè quattro mestieri che i suoi non toccavano e due riscritture dichiarate,
  con un nome diverso dal suo. Cinque dei suoi XMP non avevano niente da travasare (taratura dei
  primari, color grading a tre zone, maschere).
  - ⚠️ **Restano fuori** sfrangiatura, viraggio diviso, chiarezza e texture. I venti non includono gli
    Effetti, e si rifanno da capo a modulo finito (sua risposta `lascia` a `d-preset-xmp`): uno stile
    di casa non si cambia da sé.
  - **I nomi dei venti sono suoi** (fra gli altri 'Roccobot', 'Rosso -', 'Rosso - -', i tre col
    prefisso 'T&O - ', 'Blu/Rosso'), e l'elenco è in ordine alfabetico. I nomi non si traducono.
- ⚠️⚠️ **Il tocco applica sostituendo tutto; il tocco lungo tocca solo i moduli che lo stile nomina**
  (sua specifica), e un modulo nominato si sostituisce per intero. Che cosa nomina lo dice il
  formato: un modulo a riposo non si scrive. Quello che ne esce è un `Look` come un altro, quindi
  'Annulla' lo disfa.
- ⚠️⚠️ **Gli Stili sono il nono modulo** (sua istruzione), col gettone in `MOD_KEYS` e l'elenco che
  scorre dentro l'altezza della scheda. L'immagine cambia in tempo reale e il pannello resta
  aperto, perché uno stile si sceglie confrontando.
  - ⚠️ **I venti si chiamano 'Stili AIV', ma nell'elenco quel titolo non si scrive** (sua istruzione:
    i pixel verticali sono preziosi): il separatore 'Stili salvati' (`look_preset_mine`) c'è solo se
    c'è almeno uno stile suo, e divide quelli di casa, sopra, dai suoi, sotto.
  - ⚠️⚠️ **'Salva stile' è un'icona fissa sulla barra in basso**, fuori dall'elenco che scorre (sua
    richiesta), e per questo gli stili salvati vivono sopra corpo e barra (`LookSheet`). Il glifo è
    `BookmarkAdd` di Material col raccordo di casa (`ic_preset_save.xml`, sua istruzione): non è
    più provvisorio.
- ⚠️⚠️ **L'archivio è un file in `filesDir`, scritto a mano con `org.json`**: un elenco di oggetti
  annidati non è una preferenza, e una libreria automatica legherebbe il file alla forma delle
  classi. Un campo che manca vale il suo valore di riposo, nei due versi, e un modulo a riposo non
  si scrive.
- ⚠️ **Un nome già usato sostituisce**, senza guardare le maiuscole. **Si cancella con un'offerta di
  rimetterlo**, non con una conferma, perché finché la notifica è in scena non si perde niente.
- ⚠️⚠️ **La pagina 'Stili dell'editor'** (nome suo) riordina, rinomina e cancella, e ha 'Ripristina'
  col testo suo, che dice che cosa si perde. I due gruppi si riordinano separatamente; uno stile di
  casa cancellato finisce fra i nascosti, e 'Ripristina' porta via anche quell'elenco. La pagina è
  una sotto-pagina perché è un elenco con comandi per riga.
- ⚠️⚠️ **'Importa' ed 'Esporta' passano dal selettore di sistema**, e il file è l'archivio stesso,
  col suffisso `.aivcollection` (sua istruzione) e il tipo generico; si importa ogni file.
  L'importazione di questa pagina sostituisce (`docs/Files.md` § '💼 Esporta e importa, e il file
  che solo AIV sa leggere'). **`.aivstyle`, lo stile singolo, non c'è ancora**: è una domanda del
  giro.
- ⚠️⚠️ **Il primo avvio dell'editor completo mostra due mini-onboarding coi testi suoi**: la fila dei
  moduli (`Hint.MODULES`, lo scorrimento e il tocco lungo che azzera un modulo) e i tre tasti dai due
  gesti (`Hint.EDITOR_TOOLS`, con una chiave sua perché chi ha già l'app la veda).
  - ⚠️ **Ogni copia illuminata ha il suo riquadro misurato**, perché i tasti non sono larghi
    uguali e sono due o tre; **i paragrafi sono due, e da che parte cade ciascuno lo decide la
    misura**, o uno coprirebbe i tasti che indica (caso 66 di `SviluppoTest`). I glifi sono quelli
    dei tasti veri.
  - ⚠️ **Il velo consuma il primo tocco**: nelle prove `@Before` scrive le due chiavi come già viste.
- **`PresetTest`** misura il formato, l'esclusione della geometria, che applicare due volte dia la
  stessa immagine, il nome doppio, la cancellazione, i venti, l'ordine e il pannello; non vede come
  uno stile cambia un'immagine.

## 🔏 La filigrana, e perché il file si copia in casa

- ⚠️⚠️ **La specifica è sua**: si configura nelle impostazioni, sezione dell'editor; PNG o SVG, un
  logo per volta; vale per ogni salvataggio se l'interruttore è acceso; il file si copia in una
  cartella interna perché sopravviva all'originale.
- ⚠️⚠️ **Non vive in `Look`**, perché è una firma e non un aspetto: arriva al salvataggio come
  argomento. Conta come lavoro da salvare, quindi accende 'Salva' nei due editor, e toglie il senza
  perdita. Il testo di 'Applica al salvataggio' è suo.
- ⚠️⚠️ **Il file si copia com'è** in `filesDir`: un SVG resta vettore, nitido a ogni misura, e un
  PNG non si riduce. Si chiama sempre `mark` col suffisso del tipo, e quello dell'altro tipo si
  toglie a mano (caso 3), o resterebbero due filigrane.
- ⚠️⚠️ **Il tipo si riconosce dai byte, non dal nome**: un JPEG non ha trasparenza e stamperebbe un
  rettangolo pieno. I tipi si dichiarano anche al selettore; il file si disegna prima di adottarlo,
  e il vecchio si cancella solo dopo.
- ⚠️ **La misura è una frazione del lato lungo** (il lato corto la farebbe pesare un quarto su una
  verticale); l'aria dal bordo è una sola (`Watermark.AIR`); un PNG piccolo si ingrandisce e resta
  morbido, e chi vuole nitidezza usa un SVG. I posti sono cinque: i quattro angoli e il centro.
- ⚠️ **Si scrive sul bitmap ricevuto**, e la copia è il ripiego: se non si può fare, la firma salta,
  che è meglio di un salvataggio fallito.
- ⚠️⚠️ **La firma si posa su pixel interi, senza filtro** (sua segnalazione: veniva sfocata): un
  angolo in virgola mobile campionava ogni pixel su quattro vicini.
- **Il tasto dell'editor scrive la stessa chiave delle impostazioni**, e senza un logo non c'è
  (§ '🎛️ I due tasti del salvataggio, e i loro due gesti').
- ⚠️⚠️ **Revocata da lui la firma sul palco dei due editor** ('Mostra nell'editor'), perché non serve
  e confonde. La sua chiave resta negli archivi e non si pota; resta il riquadro delle
  impostazioni, che usa gli stessi numeri del disegno vero.
- ⚠️⚠️ **La distanza dal bordo si scrive col decimale** (sua richiesta): il valore resta un intero,
  in decimi di centesimo (`AIR` 0..250, `AIR_STEP` 10), con la chiave `mark-air-tenths` e
  `mark-air` come ripiego in lettura moltiplicato per il passo. Non è una migrazione: nessuno scrive
  più quella chiave. Il separatore è quello della lingua, a scrivere valgono punto e virgola, e un
  numero tondo non mostra la coda.
- ⚠️⚠️ **Il posto si sceglie sul riquadro** (sua richiesta), con cinque selettori d'accento in una
  fascia intorno, che non coprono l'immagine; il riquadro c'è anche senza logo, perché è il comando.
  I cinque nomi restano per il lettore di schermo e per la ricerca; il bersaglio è di 44 punti.
  - **Le misure dei segni sono sue**: fascia 22, braccio 22, tratto 5 (3 da spento), tondo del
    centro 18, inchiostro spento al 55%; a pagare è il riquadro, che si rimpicciolisce. Gli angoli
    del riquadro sono di 2 punti, perché rappresenta una fotografia.
  - ⚠️ **Il disegno dell'anteprima si rilegge con una chiave** (il contatore della pagina), o resta il
    logo di prima.
- ⚠️⚠️ **La pagina è una sotto-pagina di 'Editor e salvataggio'**, e dal tocco lungo sul tasto si apre
  come una scheda sopra l'editor (`MarkPage` in una `Sheet`, sua richiesta): uscendo dall'editor il
  `Look` in lavorazione, che non è salvabile, si perderebbe.
- **`FiligranaTest`** misura il tipo dai byte, un file solo, un file illeggibile, il tetto di otto
  megabyte, la misura, l'angolo, niente senza file, 'Salva' acceso e il bordo nitido; e i decimali
  nei due versi, perché lo stesso difetto è un fattore dieci da due parti. Il piano della prova è
  scelto perché l'angolo venga frazionario, o sarebbe verde a vuoto.

## 📏 Il ridimensionamento, e i due gesti di un tasto solo

- ⚠️⚠️ **'Ridimensiona' è un parametro del salvataggio nell'editor** (sua risposta `editor`): non vive
  in `Look`, non si vede sul palco, e toglie il senza perdita. C'è nei due editor (`ResizeButton`, in
  `EditorTools.kt`), perché sotto Android 13 c'è solo il semplice.
- ⚠️ **I due gesti sono sullo stesso tasto** (§ '🎛️ I due tasti del salvataggio, e i loro due
  gesti'); 'Applica' della finestra accende l'interruttore, e spegnere non porta via il piano, che
  le preferenze tengono in tre campi.
- ⚠️⚠️ **La finestra è la sua specifica**: sotto il titolo le misure correnti (dopo posa e ritaglio),
  senza dicitura e senza grassetto; due campi coi pixel di destinazione, che sono la verità; il
  gettone dice solo quale dei due comanda, e il valore del piano si ricava (`Resize.valueOf`).
  L'unico travaso è coi per cento.
  - ⚠️⚠️ **'Pixel' è il gettone del libero e non si accende mai** (lettura dichiarata). Il piano di
    fabbrica non fa niente (`Resize.NONE`, il libero con un tetto di ventimila pixel), e i campi si
    precompilano col risultato del piano, o con le misure correnti. Senza misure resta la sola
    percentuale.
  - **La misura si scrive con una funzione sola**; l'anteprima si chiama 'Risultato' (stringa sua) ed
    è centrata; il segno × fra i campi si allinea alle cifre (`FIELD_TEXT_DROP`).
  - ⚠️⚠️ **'Ripristina' torna al libero con le misure correnti**, vive in fondo a destra (sua
    richiesta: si raggiunge con una mano) ed è una pastiglia che scrive nei campi. La nota della
    finestra è sua, in testo regolare.
  - ⚠️⚠️ **'Applica' scrive nel modello, e vale finché l'app vive; 'Rendi predefinito' scrive nelle
    preferenze** e accende l'interruttore (sua funzione). Si accende solo in 'Lato lungo', 'Lato
    corto' e '%' (`Resize.portable`), i modi che non dipendono da come è girata l'immagine, e si
    spegne dopo il tocco, perché una notifica cadrebbe dietro la finestra.
  - ⚠️ **'%' è scritto col segno ed è nella prima riga** (il nome per esteso nella descrizione
    parlata), e il titoletto 'Adatta a' non c'è (suo mockup).
- ⚠️⚠️ **Una pastiglia a due righe**: la larghezza della parola più lunga si misura una volta e la
  usano il conto e il disegno (`TitleRow`), o il titolo andrebbe a capo a metà parola (sua
  segnalazione). Corpo più piccolo e interlinea come rapporto (`TITLE_PILL_LEAD`), con lo stile da
  una funzione sola (`titlePillStyle`); a una riga non cambia niente.
- ⚠️ **Le righe dei gettoni sono dichiarate, tre e tre**, con pesi misurati sul testo e corpo
  `labelMedium`; in qualche lingua si tronca lo stesso, e si dichiara.
- ⚠️⚠️ **Le proporzioni si mantengono sempre, e non si ingrandisce mai**: il tetto è una riga sola per
  tutti i modi, e la finestra dice quando il piano non fa niente.
- **Sei modi**: lato lungo, lato corto, larghezza, altezza, percentuale, libero (suo elenco). I
  confini dipendono dal modo, e il valore salvato si riporta dentro i suoi in lettura.
- ⚠️⚠️ **La misura di partenza nasce da due fonti**: il lato lungo da `Pixels`, la forma
  dall'anteprima raddrizzata, perché l'EXIF gira dopo la lettura delle misure. Posa e ritaglio
  entrano nel conto (`Resize.frameSize`); senza misura, 'Salva' non si accende per il solo
  ridimensionamento.
- ⚠️ **Si ridimensiona prima della firma**, o la filigrana verrebbe rimpicciolita con l'immagine.
- **`RidimensionaTest`** misura i modi, i confini, la misura di partenza, il piano di fabbrica, i
  campi, 'Ripristina', 'Rendi predefinito', i gesti e il segno fra i campi; non vede la resa del
  filtro. ⚠️ La prova delle righe dei gettoni vuole una scena larga (`w600dp-h900dp`), perché sulla
  scena di serie la forma sbagliata dava lo stesso risultato.

## 🎛️ I due tasti del salvataggio, e i loro due gesti

- ⚠️⚠️ **'Filigrana' e 'Ridimensiona' vivono nella barra in basso, non del tutto al bordo** (sua
  richiesta: in testata erano lontani dal pollice). Il blocco cambia lato col FAB, e il suo ordine
  interno si specchia con lui (lettura dichiarata, come la barra); il lato lo decide la schermata,
  l'ordine la barra. `TOOL_EDGE` vale `STAGE_SIDE`. 'Salva stile' va verso il centro, così passando
  da un modulo all'altro i due tasti non si spostano.
- ⚠️⚠️ **Il tocco accende e spegne, il tocco lungo configura, per tutti e due** (sua istruzione);
  'Filigrana' prima di 'Ridimensiona'. 'Salva' resta in testata, perché chiude il lavoro.
- **Un pezzo solo li disegna** (`EditorTool`, in `EditorTools.kt`): due disegni divergerebbero al
  primo ritocco.
- ⚠️⚠️ **`combinedClickable` più la semantica scritta a mano** (`toggleableState`, `Role.Switch`):
  `Modifier.toggleable` non offre il gesto lungo, e senza lo stato scritto il tasto è muto per un
  lettore di schermo. L'etichetta del gesto lungo è la schermata a cui porta; l'accento dice acceso,
  l'inchiostro attenuato dice salvataggio in corso.
- ⚠️ **'Filigrana' non c'è senza un logo**, perché un interruttore che non cambia nessuna immagine è
  un comando che non fa niente; 'Ridimensiona' c'è sempre, e il piano di fabbrica non toglie un
  pixel.
- ⚠️ **Il titolo della testata è 'Modifica'**, sua parola, anche ora che lo spazio è tornato: una sua
  istruzione non si rovescia perché la sua ragione è caduta.
- ⚠️⚠️ **I glifi**: `ic_resize.xml` (quarantotto punte raccordate) e `ic_watermark.xml`, specchiato su
  sua richiesta perché il rettangolino cada in basso a sinistra, dove lui mette la firma. Con lo
  specchio Material non lo contiene più, e la trappola del verso di percorrenza vive in testa al file.
- **Il banco** misura i gesti, il posto nella barra, lo specchio con le sue quattro controprove, e
  le copie dell'onboarding.

## 🚪 Uscire dall'editor completo con del lavoro in corso

- ⚠️⚠️ **Se un modulo diverso dal Ritaglio ha toccato l'immagine, Indietro chiede 'Vuoi scartare le
  modifiche?'** (sua frase e sua richiesta), con 'Scarta' e 'Annulla'.
- ⚠️ **Le due porte, la freccia e il gesto di sistema, sono una funzione sola** (`leave`).
- ⚠️⚠️ **La condizione la dà la tabella dei moduli** (`developed`), la stessa del punto d'accento, così
  un modulo nuovo entra nell'avviso da sé. Gli Stili non rispondono mai di sì: i loro valori vivono
  nei moduli che governano.
- ⚠️ **L'editor semplice non chiede mai**: è fatto di posa e ritaglio.
- ⚠️ **Conta lo stato di adesso**: dopo 'Originale' o dopo aver disfatto tutto si esce senza
  domanda, e la strada di 'Ripristina' che si perde si dichiara.
- ⚠️ **Filigrana e ridimensionamento non contano**, perché sono preferenze che restano; durante un
  salvataggio non chiede.
- ⚠️ **Non è una modale vera** (`Rules.md` § '👆 Che cosa fa il tocco FUORI da una finestra'): il
  tocco fuori vale 'Annulla'. 'Scarta' ha il colore dell'errore.
- ⚠️ **In russo, ucraino e vietnamita 'Scarta' usa un'altra parola**, perché quella ovvia coincide
  con 'Annulla'.
- **Casi 80, 81 e 82 di `SviluppoTest`**, controprovati rimettendo quattro difetti.
