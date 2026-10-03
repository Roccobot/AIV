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

- ⚠️⚠️ **I moduli sono nove**: Dettaglio, Effetti, Geometria, Ritaglio, Luce, Colore, HSL, Curve,
  Stili. **Le maschere no, in modo assoluto** (sua risposta `d-preset-manca`): è una porta chiusa,
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
  - ⚠️ **Limite noto**: una trama morbida come le nuvole diventa cielo liscio, con un bordo
    appena visibile.
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
