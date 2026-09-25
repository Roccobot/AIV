# CLAUDE.md: regole del progetto AIV

> **Cos'è questo file.** Le regole **specifiche** di `Roccobot/AIV`, l'app Android
> 'Astonishing Image Viewer'. Tutto quello che vale per ogni progetto vive nelle regole
> universali, `rules/Roccobot.md` di `Roccobot/tools`, e qui non si duplica: qui c'è solo
> ciò che di questo repository non si ricava altrove.

⚠️ **Nasce il 2026-09-01, dopo 111 versioni**, e la ragione è precisa: fino a quel giorno
l'**indirizzo del documento vivo** del progetto era solo nel brief di consegna, che è
**stato volatile** e non un archivio. Un artefatto vive fuori dal repository per definizione,
quindi se il suo indirizzo non è scritto in un file committato, alla sessione dopo quel
documento è perso.

## 🔗 Il documento vivo del progetto

| documento | a che cosa serve | indirizzo |
|---|---|---|
| **Documento di feedback** | le voci da provare della versione appena uscita, con i tre esiti e i commenti dell'utente. **Chiede.** | <https://claude.ai/code/artifact/a026a5d9-3bd0-4732-a8ea-69033d04fb48> |

⚠️⚠️ **IL NOME UFFICIALE È 'DOCUMENTO DI FEEDBACK', e si usa quello** (precisazione
dell'utente, 2026-09-04: *per me il nome esatto è 'Documento di feedback'. Se lo chiami così
capisco lo stesso (sono sinonimi), ma preferisco la dicitura ufficiale*). **Feedback AIV** è il
titolo scritto in testa al documento; **documento di lavoro**, **foglio condiviso** e **foglio
di collaudo** sono sinonimi che lui alterna e che non vanno corretti a lui. ⚠️ **Quello che
cambia è come lo chiamiamo NOI**: la dicitura ufficiale in un testo scritto da me, un commit o
un artefatto è una sola.
- ⚠️ **'Collaudo' resta il nome della PROCEDURA**, ed è la ragione per cui non è terminologia
  morta: la regola universale si chiama ancora `Roccobot.md` § '🔁 Il giro del collaudo:
  rilascio, documento, riscontro', e cambiare quel titolo romperebbe i rimandi senza guadagnare
  niente. Il documento ha un nome, il giro ne ha un altro.

⚠️ **Tiene lo STESSO indirizzo a ogni ripubblicazione**: l'utente lo ha fra i preferiti, e
un collegamento nuovo a ogni giro vuol dire un documento da ritrovare ogni volta.

⚠️⚠️ **IL DOCUMENTO SI APRE IN UN BROWSER PRIMA DI PUBBLICARLO, DAL 2026-09-19, E IL PRESIDIO È
`tools/feedback-check.py`**: quel giorno la sezione del giro della `2.67` è stata composta senza
il campo `sotto`, la pagina ha letto `undefined.length` e si è **spenta intera**, cioè lui ha
trovato la sola testata e sotto niente. È il secondo documento muto in due settimane, dopo quello
del 2026-09-05, e le due cause non hanno niente in comune: la prima era una stringa non chiusa,
questa un campo assente nei dati.
- ⚠️⚠️ **NESSUNA RILETTURA DEL CODICE LO POTEVA VEDERE, e questo è il punto**: il programma era
  valido, il campo mancava dall'altra parte, e il difetto si vede solo **aprendo** la pagina. È
  la stessa famiglia del blocco totale della `1.70`, ed è la ragione per cui quel giorno è nato
  il banco di prova dell'app.
- **Che cosa misura**, e sono due cose di specie diversa: la **forma** dei dati letti dal file
  (i campi che il disegno legge) e la **resa** in Chromium, cioè che la pagina parta e disegni
  tanti riquadri quanti ne portano i dati. Senza browser **dichiara** che la seconda metà non è
  stata provata, che è il patto di `icon-check.py` con le sue misure di resa.
- ⚠️ **Misura il file da pubblicare e non lo script che lo compone**: quegli script vivono nello
  scratchpad di una sessione e spariscono con lei, mentre il file HTML è la cosa che arriva a
  lui.
- ⚠️ **La pagina adesso regge un campo assente** (un sommario che manca non la spegne più), e il
  verificatore serve proprio perché reggerlo vuol dire disegnarne uno in meno: senza di lui, un
  campo dimenticato passerebbe in silenzio.

⚠️⚠️ **UNA COMPILAZIONE A METÀ NON SI PRENDE IN CARICO, E IL DOCUMENTO NON SI RIPUBBLICA
MENTRE LUI LO COMPILA** (istruzione del 2026-09-03). Il giro si consegna con **'Invia'** o con
una riga in chat, e si prende **intero**: spezzarlo in più versioni è una decisione mia, che
viene dopo la consegna. Nel frattempo le voci nuove e le correzioni si tengono in una
**bozza**, perché una pubblicazione ricarica ogni vista aperta e arriva sotto le mani di chi
sta scrivendo. ⚠️ La pagina che si ripubblica **da sé sul suo input** è un'altra cosa e resta:
è quella che gli garantisce di non perdere il riscontro. La regola per esteso, con il perché
di ogni pezzo, vive in `rules/Roccobot.md` § '⏸️ Il giro si prende INTERO, e solo quando lo
dice lui'.
- ⚠️ **È qui e non solo là perché questo file sopravvive alla compattazione**, come la
  regola di registro nel `CLAUDE.md` di root: un file di regole entra in scena quando lo si
  legge, e da un riassunto sparisce.
- ⚠️⚠️ **QUINDI IL RISVEGLIO AUTOMATICO SU QUESTO DOCUMENTO NON SERVE, E LA SUA ASSENZA NON
  SI SCRIVE COME UNA MANCANZA** (sua precisazione, 2026-09-08: *in ogni caso è giusto che sia
  così: abbiamo stabilito che sarò sempre io a darti il via libera*). La sottoscrizione è
  rifiutata da mesi con `mint_failed`, e il brief la registrava a ogni giro come un difetto da
  rimediare: anche funzionando sveglierebbe la sessione **a ogni salvataggio**, cioè in mezzo a
  una compilazione. Quello che si scrive è il fatto, cioè che il via libera arriva da lui.

⚠️⚠️ **DAL 2026-09-25 IL DOCUMENTO VIVO È UNO, E PRIMA ERANO DUE.** Il secondo è stato il
**Changelog AIV** fino al 2026-09-03 (decisione dell'utente: *cancella l'artefatto changelog, ho
visto che non mi serve e non l'ho mai usato*), e poi il **Piano d'azione AIV**, ufficializzato il
2026-09-04 e ritirato il 2026-09-25 (*smettiamo di aggiornare il piano d'azione e anzi
eliminiamolo: esiste già il brief per quello*). Chi ne trova l'indirizzo in un messaggio vecchio
sappia che il primo non si aggiorna più e il secondo non esiste più: il perché per esteso vive in
`rules/Roccobot.md`, § '🧾 Il changelog, provato e ritirato' e § '🗺️ Il piano d'azione, provato
e ritirato'.
- ⚠️ **L'ordine dei lavori in attesa vive nel brief**, una voce per lavoro col suo posto
  nell'ordine, e per lui la vista d'insieme è il promemoria del documento di feedback.
- ⚠️⚠️ **E IL DIVIETO DELLA METAFORA FERROVIARIA RESTA**, perché non era del piano: la sequenza
  dei lavori non si chiama 'treno' e un gruppo di lavori che esce in una versione non si chiama
  'vagone', né in chat né in un commit né in un artefatto. ⚠️ **È scritto per esteso perché la
  prima formulazione non bastava**: diceva che era cambiato il *nome del documento*, quindi la
  parola per il gruppo di lavori sembrava salva, ed è rientrata in una frase il 2026-09-04
  (*> vagone / piano d'azione\**). Un divieto che nomina un solo caso si legge come il permesso
  per tutti gli altri.

## 🎨 Il design system, che vive fuori dal repository

⚠️⚠️ **LE FONTI VISIVE DI AIV VIVONO IN CLAUDE DESIGN, nel progetto `Roccobot Design`**, e non
in questo repository. Come si aggancia e che cosa contiene vive in `rules/Roccobot.md`,
§ '🎨 Grafica' → '🎨 Claude Design, dove vive il design system'; qui restano i tre pezzi che
riguardano AIV.

⚠️⚠️ **MA NON SI VA A PESCARE LÀ DA SÉ: È L'UTENTE CHE DICE, DI VOLTA IN VOLTA, CHE COSA
PRENDERE** (istruzione del 2026-09-02: *ti dico io di volta in volta cosa cercare: con Design
mi piace sperimentare, non vorrei ripetere lo stesso errore dell'ultima volta*). Là dentro lui
**prova** delle cose, quindi quello che ci si trova non è per forza approvato: è un banco di
lavoro, non un capitolato.
- ⚠️ **L'errore che la regola evita è già costato una versione**: nella `1.33` sono entrati
  otto glifi nuovi perché erano nel brief dei disegni, e nella `1.35` sono usciti tutti
  (*volevo cambiare solo l'icona principale e la pagina di download*). Il perché è in testa
  a `Glyphs.kt`.
- **Quindi il verso giusto è**: si legge Design quando serve **quello che lui ha chiesto**
  (un colore che ha nominato, una misura che ha nominato, un componente che ha nominato), e
  tutto il resto che ci si trova accanto non si porta nell'app nemmeno se sembra migliore.
  Se una cosa sembra da cambiare, si **propone** e si aspetta.

- **`ui_kits/aiv_android/`** è la ricostruzione delle **quattro schermate** dell'app (cartelle,
  griglia, visualizzatore, impostazioni), fatta **dal sorgente Compose** e non dagli
  screenshot, col suo `README.md` che dichiara anche quello che è finto (nessuna fotografia
  vera, e il clic destro al posto del tocco lungo).
- **`assets/aiv-mark.svg`** è la A di AIV senza la piastrella, quella che firma un testo. ⚠️ Il
  brief dei disegni del 2026-09-02 diceva di **sostituirla** con la sua versione nuova: non è
  stato fatto, perché è un lavoro nel design system e non nell'app.
- **`assets/icons/`** porta i nove glifi, gli otto della famiglia nuova più `text-cursor`. ⚠️
  Nell'app **non** ci sono: sono entrati con la `1.33` e sono usciti con la `1.35`, e il perché
  è in testa a `Glyphs.kt`.

⚠️⚠️ **MA LE FRASI DELLA PAGINETTA DI DOWNLOAD NON SONO LÀ**, ed è la ragione per cui quel
lavoro è fermo: vivono in un **documento** di Claude Design (`.dc.html`), che è fuori dai
progetti di design system e quindi non si raggiunge con lo strumento. Serve che l'utente lo
mandi in chat o lo semini nello spazio di lavoro.

⚠️⚠️ **E NEL REPOSITORY NON SE NE TIENE UNA COPIA: la cartella `dev/` è stata svuotata il
2026-09-03** (istruzione dell'utente: *cancella anche tutto quello che c'è dentro AIV/dev, non
credo che serva più*). Là dentro c'erano i file che lui aveva mandato per la `1.32` e le
versioni dopo: gli otto glifi, i tre disegni dell'icona col vector XML del foreground, le due
versioni di 'Copia immagine', quella di 'Inverti selezione', e il brief di implementazione.
- **Perché non è una perdita**: quei disegni sono **entrati** nell'app (l'icona) o sono
  **usciti su sua istruzione** (i glifi, nella `1.35`), quindi la cartella teneva sorgenti già
  lavorate. Quelli che servono ancora vivono in Claude Design, e la storia git ha tutto.
- ⚠️ **Chi la ricreasse rifarebbe due fonti di verità**: un disegno che l'utente sta ancora
  provando vive nel design system, non qui, e uno approvato vive in `res/`. Un file mandato in
  chat si lavora e non si archivia.

## 🖌️ Come entra un disegno

⚠️⚠️ **UN DISEGNO VIVE IN `res/drawable/ic_<nome>.xml`, UNO PER GLIFO, e `Glyphs.kt` è il
catalogo**: là ci sono i nomi con cui il codice chiama un'icona e che cosa vuol dire ognuna,
qui c'è la geometria. Fino alla `1.45` i tracciati erano costanti di stringa in Kotlin, e lo
spostamento della `1.46` ha una ragione misurata: così **il verificatore e l'app leggono lo
stesso file**. Prima il verificatore doveva ricostruire il tracciato dalle stringhe, e quella
ricostruzione **ha sbagliato**, dando due glifi alti 0,75 unità su 24 perché trattava ogni
riga come un sottotracciato mentre là le righe spezzano un numero a metà.
- ⚠️ **L'eccezione è il disegno CALCOLATO**, e ce n'è uno: `TextCursor` nasce da quattro
  costanti con una relazione dichiarata, e in XML quella relazione diventerebbe due numeri
  qualunque. Chi ne aggiunge un altro così lo tenga in Kotlin: un tracciato composto da
  chiamate tipizzate non può essere malformato, quindi non ha niente da guadagnare dal
  trasloco.
- ⚠️⚠️ **E NON SI CREDA DI AVERE UNA VALIDAZIONE AL BUILD, perché non c'è**: `aapt2` **non**
  guarda dentro `android:pathData`. Provato con un tracciato che contiene la parola `ciao`:
  compila con esito 0. La rete è `tools/icon-check.py`, e va lanciato.

⚠️⚠️ **CHE COSA DEVE FARE L'UTENTE PRIMA DI MANDARE UN FILE, e la risposta è: quasi niente**
(sua domanda, 2026-09-03: *solo alcune le ho esportate come unico tracciato unito ed espanso,
senza maschere né livelli. Serve farlo, oppure una volta che le inglobi nei file di risorse
assumono già la configurazione ideale e pulita?*). Il trasporto **scarta da sé** tutto quello
che in un file di risorse non serve, perché copia il solo tracciato: metadati, `<defs>`,
identificatori, fogli di stile, e il rettangolo trasparente con cui Illustrator dichiara la
tela. Quindi si esporta come viene comodo.
- ⚠️ **Quello che invece va ESPANSO PRIMA, perché il formato non lo conosce**: maschere,
  filtri, modalità di fusione, `<use>` e simboli, testo non convertito in tracciato, campiture
  a motivo, `stroke-dasharray`, e l'allineamento del tratto interno o esterno. Non esistono
  come elementi di un vettore Android: un file che li porta non si può trasportare, e il
  verificatore lo **blocca** invece di lasciarlo passare a metà.
- ⚠️ **Un tratto di spessore COSTANTE non va espanso**, ed è l'unica cosa che si tende a
  espandere per niente: `android:strokeWidth` esiste, e un tratto vero resta un tratto (lo fa
  `TextCursor`). Va espanso il tratto a spessore variabile, quello con un pennello, e quello
  tratteggiato.
- ⚠️ **La conversione la fa la sessione, non lui** (sua istruzione, 2026-09-03: *lascio fare a
  te la conversione. Ma non basta: tratta TUTTE le prossime icone che ti invio in modo che
  entrino ottimizzate*). Quindi un file che arriva si lavora e non si archivia, e nel
  repository non se ne tiene una copia: il perché è in § '🎨 Il design system, che vive
  fuori dal repository'.

⚠️ **LIVELLO UNICO E FORMA UNICA: che cosa vuol dire qui**, perché lui lo fa già in
esportazione (*se poi vuoi uniformare a 'Livello unico e forma unica', giusto per avere
uniformità di trattamento, aggiungi anche quello*) e nel formato di arrivo si traduce in due
cose distinte, con due esiti diversi.
- **Forma unica, cioè un `<path>` solo: si fa dove la cucitura costa zero, e si misura.**
  Due forme che si toccano, disegnate come tracciati separati, hanno il bordo condiviso
  coperto due volte e viene pieno; unite, quel bordo diventa un bordo interno solo e si
  sfrangia. Il verificatore lo misura per ogni icona nella colonna `cuc`: zero vuol dire che
  si possono unire, e allora si uniscono.
- ⚠️⚠️ **LIVELLO UNICO NON VUOL DIRE VIA I GRUPPI, e questa è una correzione del 2026-09-03**:
  un gruppo di **sola traslazione** è il modo canonico di dichiarare che il disegno ha
  un'origine, che un vettore Android non sa scrivere (dichiara `viewportWidth` e
  `viewportHeight` e nient'altro, quindi l'origine è sempre 0,0, e un tracciato con coordinate
  negative viene ritagliato). Resta, e non si appiattisce nelle coordinate, per due misure:
  appiattire cambia **9 pixel su 230.400** con scarto 8 (provato su `ic_aiv_mark`: i valori
  sommati in decimale esatto non cadono sullo stesso numero in virgola mobile a 32 bit), e la
  `pathData` smette di essere confrontabile carattere per carattere col file di partenza, che
  è il solo modo di verificare un trasporto.
  - **Quello che invece è un livello di troppo**: un gruppo che non trasforma niente, e più di
    un gruppo. Un gruppo che **scala o ruota** stacca i numeri del tracciato dal disegno, e va
    bene solo se esprime una convenzione: l'unico caso in casa è il rientro del 65%
    dell'icona adattiva, in `ic_launcher_foreground.xml`.
- ⚠️ **Chi legge una nota vecchia sappia che dicevo il contrario**: fino a metà del 2026-09-03
  la regola scritta era 'livello unico sempre, costa 7 pixel'. Quel numero veniva da un
  appiattimento fatto in virgola mobile, cioè misurava il mio errore di calcolo e non la
  differenza fra le due forme.

⚠️ **I NUMERI INCOLLATI SI SEPARANO**, e questo è il ritocco che vale sempre: `-.05.1` sono
**due** numeri, perché un punto che segue un numero che ha già il punto ne apre uno nuovo. È
SVG legale, Android lo legge, e un parser che segue la grammatica alla lettera lo rifiuta.
Separarli non cambia un solo valore (misurato: zero pixel di scarto su tutti i glifi) e costa
poche centinaia di byte in tutto, che è il prezzo per cui qualunque strumento riesce a leggere
quei tracciati.
- ⚠️ **E il numero di byte non è un criterio** (sua istruzione, 2026-09-03: *non mi interessa
  recuperare 100 byte. Voglio che il lavoro sia fatto formalmente bene, massima compatibilità
  e correttezza del codice per avere descrizioni di forme future-proof*). Quindi un separatore
  si mette anche dove il minimo basterebbe, e un comando implicito si scrive per esteso: si
  ottimizza per chi legge il tracciato, non per la sua lunghezza.

⚠️ **`android:fillType` SI DICHIARA SEMPRE**, anche quando il disegno verrebbe uguale con
l'altra regola. La regola dichiarata dice quello che il disegno vuole; quella scelta perché
tanto viene uguale dice solo com'è andata, e la coincidenza cade il giorno che qualcuno
aggiunge un sottotracciato o un editor li riordina. Il verificatore segnala il caso pericoloso
(il disegno **dipende** dal verso e la regola **non** è dichiarata) e tace sugli altri.

⚠️ **La griglia di Material è 24 con 2 di margine per lato, ma una tela diversa non è un
difetto**: i due glifi dell'allineamento sono arrivati in 800x800 e la tela resta la loro,
perché riscalarne i numeri a mano vorrebbe dire mille arrotondamenti e un disegno che non si
può più confrontare col file di partenza. A dichiarare quanto è grande l'icona sono
`android:width` e `android:height`; il viewport dice in quante unità è disegnata. ⚠️ Il
verificatore **riporta** il rapporto fra le due e non lo giudica.

⚠️⚠️ **UN FILE CHE ARRIVA NON DIVENTA PER FORZA UN DISEGNO: PRIMA SI MISURA CONTRO QUELLO CHE
COMPOSE GIÀ PORTA, e a zero pixel di scarto vince Material.** Nasce il 2026-09-04, quando ne
sono arrivati dodici e sei erano il glifo di sistema scaricato dal catalogo: metterli in `res/`
sarebbe stato tenere due copie dello stesso disegno, che è quello che `Glyphs.kt` vieta in
testa, e la prima a cambiare sarebbe stata quella che nessuno guarda.
- **Come si misura, perché a occhio non si vede**: si ricostruisce il tracciato di
  `Icons.Filled.<nome>` dal **bytecode** di `material-icons` (le chiamate a `PathBuilder` in
  ordine, che è la sola fonte che non richieda di fidarsi di una memoria), si rende accanto al
  file dell'utente a 240px e si contano i pixel diversi. ⚠️ **Il confronto fra le due `d` non
  serve**: lo stesso disegno viene scritto assoluto da una parte e relativo dall'altra, quindi cambiano
  quasi tutti i numeri mentre non cambia un pixel. Su `public` i numeri diversi erano 36 su 95
  e lo scarto reso era **zero**.
- ⚠️ **Zero vuol dire zero**, e non 'poco': un file **ammorbidito** dall'utente parte dallo 0,1%
  della tela in su, quindi la soglia non è un giudizio.
- ⚠️ **Il caso in mezzo esiste e si dichiara**: `settings` differiva del 5,6% ma era **la stessa
  ruota al 96%**, cioè due esportazioni successive dello stesso disegno di Google. Là non
  decide nessuno dei due, quindi resta Material e la misura si scrive nel commento. Chi trova
  uno scarto sopra zero guardi **prima** se è una scala o una traslazione uniforme, che si vede
  dall'inchiostro: stesso centro e lati in proporzione.

⚠️⚠️ **E DALLA `2.50` L'ARROTONDAMENTO A 0,4 È UNA REGOLA DEL PROGETTO E NON PIÙ UN TRATTAMENTO
CHE SI RICORDA, ED È SUA ISTRUZIONE** (2026-09-14, voce `geo-glifo`: *mi pare che manchi
l'arrotondamento di 0,4px sugli spigoli netti. Impostalo come regola del progetto d'ora in avanti
per tutti gli SVG che ti passo o crei in autonomia. Verifica anche le altre icone esistenti*).
Vale per **ogni** disegno che entra in `res/` da qui in poi, suo o di casa, e il verificatore è
`tools/icon-round.py`.
- ⚠️ **Verifica e non riscrive**, ed è misurato: la prima stesura aveva un `--fix`, e sul segno
  di spunta ha raccordato il vertice dove il giro si chiude invece della punta, lasciando una
  quadratica lunga un millesimo di unità. Un tracciato riscritto male non dà nessun errore, si
  vede solo guardando, quindi lo strumento dice **dove** manca un raccordo e con che raggio, e il
  raccordo lo fa chi disegna.
- ⚠️⚠️ **LE ESISTENTI NE AVEVANO 99, SU QUINDICI DISEGNI SU QUARANTUNO, E DALLA `2.56` NE RESTANO
  SOLO QUELLE DEL SUO LOGO** (sua istruzione, 2026-09-18, risposta `alcune` a `d-punte-adesso`:
  *arrotondale tutte, tranne `ic_tian`: il mio logo personale non si tocca MAI*). Quindi 'verifica
  anche le altre' ha avuto la sua risposta sei versioni dopo, e l'ha data guardando la tavola del
  prima e dopo: le punte raccordate sono **89** su quattordici disegni, e le **dieci** che restano
  vive sono le sue.
  - ⚠️⚠️ **IL LOGO È FUORI PER REGOLA E NON PER QUESTO GIRO, E LA REGOLA È UNIVERSALE**: vive in
    `rules/Roccobot.md` § '🧹 Bonifica e ottimizzazione degli asset', perché il suo segno lo può
    mettere in qualunque progetto. Qui resta il come: `tools/icon-round.py` lo **dichiara** escluso
    invece di contarne le punte, perché un numero accanto al suo nome si legge come un lavoro da
    fare.
  - ⚠️ **La misura di ogni disegno vive in coda al suo commento**, cioè quante punte ha preso e
    quanti pixel sono cambiati su 57.600: sono gli stessi numeri della tavola che ha guardato, e
    scritti là non dipendono da una sessione che se li ricordi.
  - ⚠️⚠️ **QUEL NUMERO È DEL CRITERIO STRETTO, CHE È IL SUO: SOLO GLI ANGOLI CONVESSI ESTERNI**
    (sua nota su `d-spigoli-icone`, giro della `2.50`: *la regola va affinata: solo gli angoli
    convessi esterni (le 'punte')*). L'affinamento è entrato nel verificatore il 2026-09-18: un
    sottotracciato porta punte solo se è un **contorno** e non un buco, e a dirlo è la profondità
    di contenimento, quindi un'isola dentro un buco torna a contare.
  - ⚠️⚠️ **ERANO 87 FINO ALLA `2.54`, E LE DODICI IN PIÙ NON SONO DISEGNI CAMBIATI: ERA IL
    VERIFICATORE CHE NON GUARDAVA IL VERTICE DELLA `M`** (trovato il 2026-09-18 lavorando al
    glifo di 'Salva stile', § '🧰 Gli strumenti che questo repo si porta dietro'). Un giro che
    torna esattamente sul proprio punto di partenza ha là uno spigolo come tutti gli altri, e
    quello non si contava mai: il conto vero delle esistenti è 99, e i quindici disegni sono gli
    stessi.
  - ⚠️⚠️ **I NUMERI DI QUEL BLOCCO SONO DUE E SI LEGGONO INSIEME**: col criterio largo ne
    contava **138** prima di quella correzione, e con quello stretto **99** oggi. ⚠️ **E il 142
    della `2.50` non è più il numero del criterio largo**, perché i disegni sono cambiati nel
    frattempo: chi confronta guardi prima le tre date.
  - ⚠️⚠️ **E CON LUI CADE LA FRASE DELLA `2.32` SUI BUCHI**, che diceva il contrario: là un buco
    prendeva gli stessi raccordi *perché i suoi angoli sporgono verso l'inchiostro*. Adesso no,
    ed è la stessa parola che lo dice: una punta è convessa **e** esterna, e l'angolo di un buco
    la seconda metà non ce l'ha.
    - ⚠️ **Quanto pesi quella metà è misurato, e non è un dettaglio**: sono **51** punte su 138,
      cioè più di un terzo, e cinque disegni escono dall'elenco per intero. In `ic_download` le
      sette punte erano quelle della freccia **scavata dentro** la cartella.
  - ⚠️⚠️ **LA DECISIONE ERA PASSATA DA UNA TAVOLA, ED ERA LA SUA SCELTA `guardo`**: la domanda
    del giro della `2.50` offriva di arrotondarle tutte, solo quelle di casa, o nessuna, e lui ha
    scelto di vedere prima il **prima e il dopo**. Per cinque versioni in `res/` non si è toccato
    niente, e la tavola è nata col giro della `2.55`: chi legge che la decisione aspetta sappia
    che è arrivata.
- ⚠️⚠️ **E LA VERIFICA HA PAGATO ALLA PRIMA CORSA, TROVANDO UN DIFETTO CHE NESSUNO AVEVA
  VISTO**: in `ic_mod_detail` i dodici archi portavano i raggi al posto della rotazione e della
  bandierina dell'arco maggiore, quindi quattro tondi su sei venivano disegnati col raggio
  sbagliato (2.849 pixel su 57.600, il 4,95% della tela). La rete che avrebbe dovuto prenderlo
  è `icon-check.py`, che dalla `2.50` **rifiuta** una bandierina che non sia `0` o `1`.
- ⚠️ **Quanto si vede è un'altra domanda, e il conto la chiude**: su un angolo retto un raccordo
  da 0,4 fa arretrare il vertice di **0,166 unità** su 24, cioè mezzo pixel su uno schermo a tre
  volte. È un trattamento di correttezza formale, non un effetto visibile a 24dp.

⚠️⚠️ **E DALLA `2.32` UN DISEGNO PUÒ NASCERE QUI: I NOVE GLIFI DELL'EDITOR COMPLETO SONO
AMMORBIDITI DALLA SESSIONE**, ed è sua richiesta (2026-09-13, dopo aver guardato la tavola dei
glifi: *cerca solo di arrotondare di ~0,4px i bordi esterni, che è il trattamento che ho fatto a
tutte quelle preparate da me finora*). Fino a quel giorno un disegno di casa arrivava da lui, e
questo è lo stesso trattamento applicato ai glifi di Material che l'editor già usava.
- **Che cosa si tocca**: i giunti che svoltano nel verso del proprio sottotracciato, cioè gli
  spigoli che **sporgono**. Un angolo che rientra è un raccordo interno e non un bordo esterno.
  ⚠️⚠️ **E la coda di questa riga è decaduta con la `2.50`**: diceva che *un buco prende gli
  stessi raccordi, perché i suoi angoli sporgono verso l'inchiostro*, e la sua nota su
  `d-spigoli-icone` dice **solo gli angoli convessi esterni**. Il perché per esteso vive qui
  sopra, nel blocco della regola del progetto.
- ⚠️⚠️ **IL RAGGIO NON È COSTANTE, LO È QUANTO IL VERTICE ARRETRA, E SENZA QUELLA MISURA LE
  PUNTE VENGONO TOZZE**: con 0,4 fisso un angolo di 30 gradi arretra di **1,15 unità** su 24,
  cioè il 5% della tela, contro le 0,17 di un angolo retto, e le tre stelle di 'Auto' si
  accorciavano di brutto. Adesso il tetto è l'arretramento dell'angolo retto, quindi sulla
  famiglia Material, dove gli spigoli sono quasi tutti retti, il raggio resta 0,4 esatto.
- ⚠️ **Il raccordo è un arco fra due rette e una quadratica dove c'è una curva**: un arco è
  tangente ai suoi due lati solo se sono rette, mentre una quadratica col controllo nel vertice
  lo è per costruzione. L'unico caso in casa sono i due spigoli della semiluna del 'Dettaglio'.
- ⚠️⚠️ **DUE DEI NOVE NON SONO ENTRATI, ED È IL CRITERIO QUI SOPRA APPLICATO ALLA LETTERA**:
  `Palette` e `Timeline` sono disegnati tutti di curve e cerchi, quindi non hanno un solo
  spigolo e l'ammorbidimento li lascia a **zero pixel** di scarto. Là vince Material, e i moduli
  'Colore' e 'Curve' chiamano `Icons` invece di un file.
  - ⚠️⚠️ **E LA `2.73` HA DIVISO ALLO STESSO MODO I DUE TASTI DELLA TESTATA, SU SUA RICHIESTA**
    (punto 4 del campo libero del giro della `2.70`: *le icone di 'Filigrana' e 'Ridimensiona'
    possono venire da Material ma vanno arrotondate come da regola nuova*). `PhotoSizeSelectLarge`
    ne aveva **quarantotto**, quindi è diventata `ic_resize.xml` (451 pixel su 57.600);
    `BrandingWatermark` è una cornice già stondata col rettangolino **scavato** dentro, cioè zero
    angoli convessi esterni e zero pixel, quindi resta dov'è. Chi rifà quel conto si fermi
    prima: la misura vive in testa a `ic_resize.xml`.
- ⚠️ **Il tracciato si ricostruisce dal bytecode**, cioè dalla stessa fonte con cui si misura uno
  scarto: si legge dalle chiamate a `PathBuilder` in ordine, si raccorda, e si riscrive in
  coordinate assolute coi comandi per esteso. Gli scarti misurati vivono in testa a ogni file.
- ⚠️⚠️ **E UNO DEI SETTE È CAMBIATO DI DISEGNO CON LA `2.36`, SU SUA ISTRUZIONE** (2026-09-13, dopo
  aver guardato la fila dei gettoni: *l'icona di HLS dev'essere più simile a una serie di slider
  stilizzati, non dev'essere un contagocce*): l'HSL nasceva da `Icons.Filled.Colorize` e adesso
  nasce da `Icons.Filled.Tune`, cioè le tre guide col cursore. Un contagocce dice *prendi un
  colore*, che è il gesto del colore mirato; quel modulo invece porta tre cursori per ognuna delle
  otto fasce. ⚠️ **Il trattamento non cambia**: stesso raccordo, stessa misura in testa al file
  (210 pixel su 57.600), e lo stesso verificatore.
  - ⚠️ **Ci resta, con una riserva sua** (giro della `2.36`, voce `hsl-glifo` accettabile: *sembra
    più 'Impostazioni', ma va bene lo stesso*): tre guide con un cursore sono il segno che Material
    dà anche alle impostazioni, quindi la somiglianza è del disegno e non una svista. Chi volesse
    chiuderla gli proponga un glifo suo, che è la strada dei nove della `2.32`.

## 🗣️ Come si chiamano le cose

⚠️⚠️ **LA FASCIA IN CIMA A UNA CARTELLA SI CHIAMA 'INTESTAZIONE', DAL 2026-09-08** (sua
istruzione: *manca solo qualche ritocco al frontespizio, che da adesso chiamerò intestazione*), e
nello stesso giro ha riscritto la voce delle impostazioni in **'Intestazione delle cartelle'**.
Quindi il nome non è più una preferenza di chi scrive: è la parola che si trova nel telefono, che
è il criterio della voce sul velo qui sotto.
- ⚠️ **'Frontespizio' è terminologia morta**: non si usa né in chat, né in un commit, né in una
  voce del documento di feedback, né in un artefatto. Chi la trova in un branch vecchio o in un
  commento sappia che è la stessa cosa.
- ⚠️ **Nel codice il prefisso resta `front`** (`Front.kt`, `frontWash`, `FRONT_INK`), e non è una
  dimenticanza: rinominare cinquanta simboli non cambia niente per chi usa l'app e romperebbe
  ogni rimando scritto finora. La bonifica del 2026-09-08 ha toccato la **prosa** dei commenti,
  che è la parte che si legge parlando con lui.

⚠️⚠️ **E LA SFUMATURA D'ACCENTO DIETRO L'INTESTAZIONE SI CHIAMA 'GRADIENTE'** (sua correzione,
2026-09-08: *se per tinta intendi la sfumatura/gradiente*), che è l'etichetta del suo interruttore
in 'Aspetto'. **Tinta** è il nome interno (`frontWash`, `WASH_STOPS`) e resta nel codice.
- ⚠️ **Le sfumature in fondo allo schermo sono un'altra cosa**, e si dicono così: quando in una
  frase compaiono tutte e due, quella dell'intestazione si nomina per esteso.

⚠️⚠️ **QUEL PULSANTE SI CHIAMA 'FAB', E 'TASTINO' NON SI USA PARLANDO CON LUI** (riscontro del
giro della `1.60`: *e il pulsante si chiama FAB, lascia perdere 'tastino'*). Vale nelle voci del
documento di feedback, in chat, negli artefatti e nei messaggi di commit.
- ⚠️ **È lo stesso criterio della voce sul velo, applicato a un nome che LUI usa**: la sigla
  compare nella spiegazione della voce 'Posizione dei tasti flottanti', quindi è la parola che
  si trova nel telefono; 'tastino' era un vezzeggiativo mio, che non compariva da nessuna parte.
- ⚠️ **La bonifica dei commenti è fatta il 2026-09-08**, su sua richiesta (punto D del campo
  libero del giro della `1.85`): 190 occorrenze in 17 file. ⚠️ **Restano due citazioni SUE, e non
  sono residui**: quello che ha detto lui si riporta come l'ha detto, e la prima delle due
  (*i tastini su e giù sono scomodi*, in `Reorder.kt`) non parla nemmeno del FAB, ma delle due
  frecce del riordino.

⚠️⚠️ **'VELO' NON SI USA PARLANDO CON LUI: si dice 'Sfocatura dietro i pannelli', cioè il nome
della voce nelle impostazioni** (riscontro del 2026-09-04, giro della `1.46`: *io continuo a
non capire 'sto cazzo di 'velo', e continuo a non capire quando me ne parli. Nelle impostazioni
ho 'Sfocatura dietro i pannelli': parliamo di quello?*). Sì, è quello, ed è la stessa cosa: la
riga `lowered()` accende insieme il centro abbassato, la sfocatura e la patina scura dietro la
finestra. Ma 'velo' è il nome che ha il **pezzo di codice**, non quello che ha la cosa per chi
la usa, e per tre versioni gli ho chiesto di provare una funzione chiamandola con una parola
che non compariva da nessuna parte nel telefono.
- ⚠️ **Il criterio è più largo del caso**: quando una funzione ha una voce nelle impostazioni,
  in una voce di collaudo si chiama **con l'etichetta di quella voce**, alla lettera. Il nome
  interno resta nel codice, dove serve a chi legge il codice.
- ⚠️⚠️ **E QUANDO LA PROVA NON È OVVIA, SI SCRIVE IL PASSO PASSO** (sua richiesta nella stessa
  riga: *altrimenti devi dirmi passo-passo cosa devo fare per testare*). Una funzione spenta di
  fabbrica non si prova toccandola: prima si accende, e dirlo è parte della voce. 'Che cosa
  provare' non è un rimando, è una procedura.

⚠️⚠️ **'IMMAGINE' E NON 'FOTOGRAFIA', e non è una sfumatura di stile** (correzione
dell'utente, 2026-09-02, su una mia frase nel documento di collaudo: *'Fotografia' usato in
modo improprio*). Questa app apre GIF, WebP animate, PNG con trasparenza, tavole, scansioni,
schermate: **una fotografia è un caso particolare di immagine**, e chiamare tutto
'fotografia' esclude a parole metà di quello che il visualizzatore fa. Vale per i testi
dell'interfaccia, per i commenti del codice, per le voci del collaudo e per le risposte in
chat.

- **Quando 'fotografia' è invece la parola giusta**: quando si parla davvero di uno scatto,
  cioè di dati EXIF, di tempi e diaframmi, del sensore, della galleria del telefono.
- ⚠️ **Le stringhe dell'interfaccia erano già a posto** (misurato il 2026-09-02: nessuna
  occorrenza in `values/` né in `values-it/`), quindi il difetto vive nella **prosa**, che è
  il posto in cui nessun verificatore lo guarda.

## 📍 Che cosa vuol dire 'centrato'

⚠️⚠️ **CENTRATO IN ORIZZONTALE, E CENTRATO MA IL 15% PIÙ IN BASSO IN VERTICALE**
(definizione dell'utente, 2026-09-02: *da questo momento in AIV dire 'centrato' (su elementi
di UI di questo tipo) significa centrato in orizzontale + centrato, ma un 15% più in basso, in
verticale*). Il pollice arriva più facilmente sotto la metà dello schermo, e su un telefono
grande un dialogo esattamente al centro fa allungare la mano.

- **Vale per tutto quello che si apre in mezzo**: dialoghi di conferma, pannelli, modali, la
  scheda delle informazioni sul file, i menu a pressione lunga.
- ⚠️ **La stretta è parte della definizione**, non una prudenza aggiunta: *le cose
  particolarmente alte si prendono lo spazio che serve*. Lo spostamento si riduce da sé fino a
  sparire quando sotto non c'è più aria.
  - ⚠️⚠️ **E UNA TASTIERA APERTA RENDE ALTO QUALUNQUE DIALOGO, dalla `1.60`** (riscontro del
    giro della `1.59`: *la finestra di rinomina dev'essere 'pronta' a scorrere più in alto
    quando appaiono tastiere alte*). Fino alla `1.59` il 15% si contava sull'altezza intera
    anche a tastiera aperta, quindi l'aria che la stretta misurava era aria che la tastiera
    aveva già preso, e il pannello scendeva dentro di lei. **Non è servita una regola nuova**:
    bastava che la misura dicesse la verità, e adesso `windowHeight()` toglie anche la
    tastiera. Vale per ogni superficie centrata, non per la sola rinomina.
  - ⚠️⚠️ **E DALLA `1.62` QUELLA MISURA NON BASTA PIÙ: A TASTIERA APERTA LA CENTRATURA HA UNA
    DEROGA, ED È SUA** (riscontro del giro della `1.60`: *voglio che il pannello scorra MOLTO in
    alto, con il campo testo praticamente in cima allo schermo, quando la tastiera è aperta. In
    quella circostanza la centratura dell'app ha una deroga, che serve per rendere davvero
    fruibile il pannello*). Non è la stretta portata all'estremo: la stretta **riduce** la
    discesa fino a zero, qui il pannello **sale**, fino a fermarsi a `LOWER_AIR` dal bordo di
    sopra dell'area che la tastiera lascia libera.
    - ⚠️ **Vale per ogni superficie centrata**, come la misura da cui nasce, e finisce
      nell'istante in cui la tastiera se ne va.
    - ⚠️ **Con lei arriva un TETTO sull'altezza**, ed è un difetto a sé che la stretta non
      poteva togliere (*in presenza di un nome molto lungo (ma valido) la finestra è tagliata
      brutalmente*): ridurre lo spostamento non accorcia un pannello più alto della finestra.
      Adesso una superficie centrata non può superare la finestra meno l'aria, e lo scorrimento
      che ha già dentro entra in funzione.
  - ⚠️⚠️ **MA DALLA `1.83` QUELLA DEROGA NON C'È PIÙ, E LE QUATTRO FINESTRE IN CUI SI SCRIVE NON
    SONO CENTRATE AFFATTO: IL LORO BORDO DI SOPRA VALE `TEXT_AIR`, SEMPRE** (riscontro del giro
    della `1.82`, voce `rinomina-ferma` non approvata: *secondo terzo carattere inserito o
    cancellato la finestra si sposta da troppo in basso a molto in alto, e poi ogni 3/4 caratteri
    c'è un flash della stessa finestra in posizione molto più ribassata*). La deroga faceva
    **salire** il pannello all'arrivo della tastiera, e ricavava la salita dallo spazio libero
    sopra di lei: un numero che **cambia mentre si scrive** (la barra dei suggerimenti, un gesto
    che allarga la tastiera, l'animazione dell'IME ancora in corso). La `1.82` ci aveva messo
    un'isteresi, cioè aveva reso il ballo più raro invece di togliergli la causa.
    - ⚠️⚠️ **A TOGLIERE IL BALLO È CHE LA MISURA NON GUARDA PIÙ LA TASTIERA, e il conto è
      algebrico**: vive su `pinned`, in `Centred.kt`. La finestra centra la scatola dichiarata,
      quindi posando il pannello in cima a una scatola alta `pannello + 2*salita` con
      `salita = (box - pannello) / 2 - aria`, il bordo di sopra vale **esattamente** `aria`
      qualunque siano la finestra e il pannello. La tastiera può muovere la finestra quanto
      vuole: a spostarsi è solo il bordo di sotto.
    - ⚠️ **Quali finestre**: quelle che si dichiarano modali vere, cioè che passano `null` a
      `lowered`, e per il criterio di § '👆 Che cosa fa il tocco FUORI da una finestra' sono le
      sole con un campo di testo. Ogni altra superficie centrata resta al 15% in basso, con la
      stretta e con il tetto.
    - ⚠️ **Il tetto toglie di sotto e non di sopra**: un pannello più alto della finestra si
      accorcia e il suo scorrimento entra in funzione, mentre il campo di testo in cima non si
      muove di un pixel.
    - ⚠️⚠️ **QUINDI `LOWER_AIR`, I 56dp DI `KEYBOARD_AIR` E LA SALITA A SOGLIA SONO NOTE
      SUPERATE**: erano la deroga della `1.62` e i suoi due ritocchi della `1.82`, e con loro
      sono usciti dal codice `climbFor` e la sua prova. Il numero di oggi è uno solo, `TEXT_AIR`,
      e la prova che lo presidia è `AltoTest`: misura che il bordo di sopra non si muove al
      variare della finestra, che è la forma esatta del difetto arrivato a lui.
- ⚠️ **Il 15% si misura sull'altezza della FINESTRA**, non sullo spazio libero: sullo spazio
  libero sarebbe una frazione di una frazione, quindi su un dialogo alto il movimento
  sparirebbe proprio dove il pollice fatica di più.
- **Come si applica**: `Modifier.lowered(onOutside)`, che vive in `Centred.kt` insieme al
  numero. ⚠️ La
  riga si scrive a **ogni** chiamata e non c'è modo di evitarlo: in Compose non esiste un
  aggancio globale per i dialoghi, perché `AlertDialog` centra la sua superficie dentro la
  propria finestra e nessuna sua proprietà sposta quel centro. Quello che si può avere è **un**
  modificatore, e ce l'ha: chi apre un dialogo nuovo lo aggiunge, e il valore non è mai scritto
  due volte.
  - ⚠️⚠️ **DALLA `1.70` QUEL PARAMETRO DICE SE LA FINESTRA È UNA MODALE VERA, E NON HA UN
    VALORE DI SERIE DI PROPOSITO**: chi apre una finestra nuova deve **dichiararlo**, e non può
    farlo per omissione. Passando la sua chiusura, un tocco sull'aria sopra il pannello vale
    come il tocco fuori; passando `null`, quell'aria non risponde, che è il comportamento di
    una modale.
  - ⚠️⚠️ **DALLA 1.38 QUELLA RIGA FA ANCHE UN'ALTRA COSA: mette il VELO e la SFOCATURA dietro
    la finestra** (richiesta dell'utente, 2026-09-02: *sfocatura leggera + velo chiaro/scuro a
    seconda del tema: dietro qualsiasi pannello, popup, modale, menu*). Non è un accorpamento
    di comodo: l'elenco delle superfici che vogliono il velo è **lo stesso** elenco di chi
    chiama `lowered()`, e un secondo modificatore da ricordare avrebbe raddoppiato il modo di
    dimenticarsene, con l'aggravante che un velo mancante non si vede (un centro mancante sì).
    Il perché per esteso, e le due vie con cui si applica, sono in testa a `Veil.kt`.
    - ⚠️⚠️ **E DALLA 1.54 UNA TERZA: il BORDO D'ACCENTO, al posto dell'ombra**
      (richiesta dell'utente, 2026-09-04: *via le ombre e vai con il bordino da 2px del colore
      di accento*, e *potrebbe essere l'elemento distintivo che cercavo*). Il bordo **non
      dipende dall'interruttore** della sfocatura, perché non è una funzione che si accende ma
      il modo in cui l'app è fatta. Come si disegna, e che cosa c'entra col 'quadrato sfocato'
      che lui vedeva intorno ai menu, sono in testa a `Edge.kt`.
      - ⚠️⚠️ **MA IL SUO ELENCO NON È PIÙ QUELLO DEL VELO, dalla `1.55`**, ed è una decisione
        sua contro il criterio che aveva escluso due superfici (*voglio la riga anche lì: in
        realtà dappertutto ... per coerenza deve avere il tratto intorno come tutti gli altri
        elementi simili*). La scheda della selezione e il pannello dei comandi dell'editor
        adesso hanno il bordo e restano **senza** velo: il velo dice 'mi apro sopra qualcosa',
        il bordo dice 'sono una superficie di questa app'.
      - ⚠️ **Lo spessore è 2dp, dopo un giro a 3**: la `1.55` lo aveva alzato su sua richiesta e
        la `1.56` lo ha riportato giù, sempre su sua richiesta (*torna a 2dp (preferisco)*).
      - ⚠️⚠️ **E IL RAGGIO DEGLI ANGOLI È COSTATO DUE GIRI, con due cause diverse.** La prima è
        geometrica: due riquadri stondati concentrici hanno raggi diversi, e tenere quello del
        pannello staccava l'arco dal bordo (*sembra che la linea di accento non abbia il raggio
        di stondatura corretto*). La seconda, che restava dopo la correzione, è
        l'**antialiasing sommato due volte**: il pannello sfuma il suo ultimo pixel e il tratto
        sfuma il proprio, e sull'arco quel poco che resta scoperto si mette in fila (*si
        intravedono dei pixel di sfondo chiaro/scuro oltre la curva verde*). Il rimedio è mezzo
        pixel di sconfinamento in fuori, ed è misurato in testa alla costante che lo porta.
      - ⚠️⚠️ **SULLE SCHEDE IN FONDO LA RIGA VA DI FUORI, dalla `1.56`** (sua prova: *le
        bottomsheet non stanno bene con la riga intorno. Vorrei fare una prova con la linea di
        2dp color accento che appare verso l'esterno, in modo da stare solo sul lato sopra e
        sulla curva per poi sparire fuori dallo schermo*). Non è una variante grafica: i fianchi
        di una scheda in fondo sono sui bordi dello schermo, quindi una linea che corre fuori
        da quei fianchi è già fuori dal vetro, e a interrompere il tratto ci pensa il bordo
        invece di un numero scritto a mano.
    - ⚠️⚠️ **E IL VELO NON È PIÙ UN ATTRIBUTO DELLE FINESTRE, dalla 1.54: LO DIPINGE L'APP**
      (`AppVeil`, messo in scena da `AivTheme`). Il fatto che ha costretto al cambio, dopo tre
      bocciature della stessa voce: **due finestre non cambiano il proprio velo nello stesso
      fotogramma**, quindi durante il passaggio da un menu a un dialogo esisteva sempre un
      fotogramma con due veli (più scuro) o con nessuno (più chiaro), e nessun ordine di
      chiamate lo poteva togliere. Il velo dipinto è uno, attraversa la transizione e vale il
      **massimo** delle richieste in scena. ⚠️ La **sfocatura** resta di finestra: quella non si
      può dipingere senza rifare i menu, che oggi sono finestre (il perché in fondo a
      `Veil.kt`).
      - ⚠️⚠️ **UNA SFOCATURA CHE CALA VUOLE QUALCOSA IN SCENA CHE SE NE STIA ANDANDO, E QUESTO
        GOVERNA TUTTO IL RESTO.** Il fatto viene da un confronto che ha fatto l'utente (giro
        della 1.63): il difetto era **solo** sotto il menu del FAB, mentre *aprendo e chiudendo
        le info dettagliate sul file l'arrivo e la sparizione della sfumatura sono PERFETTE*. Là
        la sfocatura cala con la **stessa curva** e per lo **stesso tempo**: quindi non erano né
        la curva né la durata.
        - ⚠️⚠️ **LA DIFFERENZA È CHE LA SCHEDA RESTA IN SCENA MENTRE LA SFOCATURA CALA.** Una
          scheda in fondo è opaca per due terzi della sua uscita e svanisce alla fine, quindi
          l'occhio attribuisce il cambiamento a lei. Il pannello di un menu spariva in 120 ms e
          per il resto della coda il raggio scendeva **da solo** in uno schermo vuoto: senza una
          causa in scena, quel movimento è una **messa a fuoco**, che è la parola che ha usato.
        - ⚠️⚠️ **LA 1.65 CI ARRIVAVA ALLUNGANDO L'USCITA DEL MENU, E DALLA 1.67 NON SI FA PIÙ
          COSÌ**: quei 280 ms cadevano addosso alla composizione di una schermata nuova ogni volta
          che una voce navigava (riscontro del giro della 1.66: *toccando 'Impostazioni' o
          'Cestino' ... rimane bloccato a schermo a mezza opacità*), e allora *semplifica al massimo
          e fai sparire il menu in modo fluido e senza glitch, con una dissolvenza veloce*. Adesso
          l'uscita è una dissolvenza sola di 75 ms, in cui pannello, sfocatura e livello scuro se ne
          vanno insieme: **la regola resta soddisfatta**, perché niente cala da solo.
          - ⚠️ **La coda resta dove non costa niente, cioè sulla scheda in fondo**, che è la
            superficie di cui lui ha detto che l'arrivo e la sparizione sono perfette. Chi la
            riportasse sui menu rifarebbe il difetto della 1.66, ⚠️ **che non si vede aprendo e
            chiudendo un menu sul posto**: si vede toccando una voce che porta altrove.
          - ⚠️ **In uscita il pannello non si rimpicciolisce più**, e non è una rifinitura: quel
            numero faceva rimisurare tutto il contenuto del menu e riposizionare la **finestra** a
            ogni fotogramma, cioè spendeva i fotogrammi che servivano alla schermata di arrivo.
          - ⚠️ **L'entrata resta 120 ms** e non è mai cambiata: aprire deve essere immediato.
          - ⚠️ **La 1.64 aveva sbagliato bersaglio**: legare la sfocatura al pannello le toglieva
            la decelerazione, cioè proprio la cosa chiesta nella 1.60.
        - ⚠️ **La misura della 1.61 era corretta e non serve più** (la percezione della sfocatura
          approssimata con la radice del raggio): diceva quanto sarebbe durato quel tratto, e una
          misura dice quanto si vede, non che cosa si capisce.
        - ⚠️ **Restano separati i due MECCANISMI**, come li ha divisi la 1.64: il livello scuro è un
          rettangolo che l'app dipinge e la sfocatura è un attributo di finestra, e confonderli era
          la ragione di metà dei difetti. ⚠️ **Il numero che li muove invece è di nuovo uno**, dalla
          1.67: erano due per dare al livello scuro una coda più lunga dell'uscita, e quella coda
          sui menu non c'è più.
        - ⚠️⚠️ **E COSÌ LA FINESTRA NON SOPRAVVIVE PIÙ AL PROPRIO PANNELLO**, che nella 1.61 era
          il prezzo da pagare perché la sfocatura è un attributo di finestra. Con lei se ne vanno
          tre contropartite: il flag di passante ai tocchi, il focus che cadeva a metà uscita, e
          il doppio senso di `inScene`. ⚠️⚠️ **E DALLA 1.67 NON CI SONO PIÙ NEMMENO DUE STATI**:
          finché la patina durava più del pannello servivano `veiling` ('l'app è velata') e
          `visible` ('il pannello si vede'), e sbagliarli non dava nessun errore; adesso i due
          tratti coincidono e il nome è **uno**, `visible`. Chi trova `veiling` in una nota vecchia
          sappia che i suoi chiamanti leggono quello.
        - ⚠️⚠️ **IL FOCUS CHE CAMBIAVA A METÀ COSTAVA UNO SFARFALLIO, ed è misurato sul bytecode
          di Compose**: `PopupLayout.updatePopupProperties` **assegna** `params.flags` invece di
          aggiungerli, e quel valore lo compone dalle sole `PopupProperties`. Quindi nel
          fotogramma in cui `focusable` cambiava, la finestra perdeva sfocatura e velo in un
          colpo, e il fotogramma dopo se li riprendeva. ⚠️ **Chi rimettesse un `PopupProperties`
          che cambia durante un'animazione se lo riprende**, e non darà nessun errore.
        - ⚠️ **In ENTRATA non è mai cambiato niente**: patina e sfocatura crescono col pannello,
          perché il conto della 1.50 (nessun fotogramma in cui la finestra sfoca più di quanto il
          pannello sia in scena) regge solo così.
        - ⚠️ **La scheda in fondo ha lo stesso trattamento dalla 1.64**, e prima aveva lo stesso
          difetto: là la sfocatura segue la dissolvenza, che è la più corta delle sue uscite.
          Restano fuori i **dialoghi di Material**, che non hanno un'uscita da animare: là la
          finestra sparisce nell'istante in cui il dialogo si chiude.
    - ⚠️⚠️ **MA DALLA 1.39 QUEL VELO È SPENTO DI FABBRICA, dietro un'impostazione** (richiesta
      dell'utente, 2026-09-03: *mettilo dietro un'opzione disattivata di default. Penserò se
      tenere o meno la feature: rende tutto visibilmente più lento*). Quindi la riga
      `lowered()` si scrive **sempre**, e quello che fa dipende dall'interruttore: il centro
      abbassato è incondizionato, il velo no.
      - ⚠️⚠️ **E DALLA `1.80` QUELL'INTERRUTTORE NASCE ACCESO, perché ci ha pensato** (riscontro
        del giro della `1.79`, campo libero punto A: *imposta la sfocatura come accesa di
        fabbrica*). Il *penserò se tenere o meno* di allora è la risposta di adesso, dopo
        quaranta versioni di prova. ⚠️ **L'impostazione resta**, e con lei tutto quello che è
        scritto qui sopra: cambia il valore di partenza, non il meccanismo.
        - ⚠️⚠️ **E DALLA `1.82` DI FABBRICA C'È L'OMBRA E NON LA SFOCATURA** (riscontro del giro
          della `1.81`, voce `sfoc-ombra`: *mi piace talmente tanto che voglio l'ombreggiatura
          come nuova opzione predefinita di fabbrica*), cioè al primo giro in cui l'ombra è
          esistita. La sfocatura resta una delle tre risposte.
          - ⚠️⚠️ **MA DALLA `2.61` SI TORNA ALLA SFOCATURA, ED È SUA ISTRUZIONE** (campo libero
            del giro della `2.55`: *cambio di valore predefinito di fabbrica: l'effetto
            `sfocatura` dev'essere attivo all'installazione (al posto di `ombra`)*). È la
            seconda volta che quel valore cambia in settanta versioni, e l'ombra non se ne va:
            resta una delle tre risposte, come lo era la sfocatura fino a ieri.
          - ⚠️⚠️ **CHI HA GIÀ L'APP NON SE NE ACCORGE, E IL FATTO SI DICHIARA INVECE DI
            PROMETTERE IL CONTRARIO**: `SettingsStore.save` riscrive **tutte** le chiavi in un
            colpo, quindi chi ha mai toccato una qualunque impostazione porta già questa
            scritta, e un valore di fabbrica si vede solo dove l'archivio tace. Sul telefono di
            chi ha seguito i giri di collaudo la sfocatura si accende a mano, una volta.
          - ⚠️⚠️ **E NON SI RIMEDIA CON UNA MIGRAZIONE COME QUELLA DELL'INDICATORE**
            (§ '🏷️ L'indicatore dell'ultimo media, e la sua migrazione'): là il valore di prima
            era **assente** e qui è scritto, quindi l'archivio non distingue 'ho scelto l'ombra'
            da 'avevo l'ombra e ho toccato dell'altro'. Una migrazione rovescerebbe anche una
            scelta vera, che è la cosa che la nota qui sotto esiste per non fare.
          - ⚠️⚠️ **E LA DOMANDA CHE HA FATTO INSIEME ALLA RICHIESTA HA UNA RISPOSTA NEL CODICE**
            (*fammi sapere se si tratta di un effetto che intacca pesantemente le prestazioni su
            dispositivi non nuovissimi*): la sfocatura **non la dipinge l'app**, è un attributo
            di finestra (`FLAG_BLUR_BEHIND` più `blurBehindRadius`), quindi da Android 12 la
            esegue il compositore di sistema ed è il sistema a dire se farla. Dove dice di no
            non si vede l'effetto e non si perde un fotogramma, e la domanda l'app la fa già
            (`blurs`, in `Veil.kt`), perché con la sfocatura assente il velo dipinto sarebbe
            troppo leggero.
            - ⚠️ **Quando c'è, si paga solo mentre un pannello è aperto**: un menu, un dialogo,
              una scheda in fondo. Lo scorrimento di una griglia e l'editor completo non ne
              hanno, quindi i due posti in cui l'app lavora di più non la incontrano mai.
            - ⚠️⚠️ **QUANTO PESI NON È MISURATO, E SI SCRIVE COSÌ INVECE DI RASSICURARLO**: il
              *rende tutto visibilmente più lento* della `1.39` è un suo giudizio di allora, su
              un'app che quaranta versioni dopo l'ha rimessa accesa di fabbrica; il banco non
              disegna niente e una sfocatura di finestra non si misura senza uno schermo. Se la
              vede lenta, la risposta è la stessa voce delle impostazioni.
        - ⚠️⚠️ **E I CASI DELLA MIGRAZIONE SONO TRE E NON DUE**: chi aveva **acceso** la
          sfocatura tiene la sfocatura, chi l'aveva spenta tiene il niente, chi non ha mai
          toccato la voce riceve il valore di fabbrica di adesso. Con un `else` solo, una scelta
          esplicita sarebbe cambiata da un aggiornamento. ⚠️ **Dalla `2.61` il primo e il terzo
          rispondono lo stesso e non si accorpano**: coincidono per una coincidenza che il giro
          dopo può far cadere.
        - ⚠️ **Il valore di fabbrica vive in DUE posti**, il campo di `Settings` e la lettura del
          flusso: cambiarne uno solo dà un'app accesa al primo avvio e spenta dopo il primo
          salvataggio, che è un difetto che non dà nessun errore.
          - ⚠️⚠️ **E DALLA `2.61` LO PRESIDIA IL BANCO, CHE PRIMA NON POTEVA**: la lettura viveva
            dentro la `map` del flusso, cioè si provava solo con un archivio vero su disco;
            adesso è una funzione a sé (`SettingsStore.read`) e il caso 6 di `ProfonditaTest`
            legge un archivio **vuoto** e lo confronta con `Settings()`, cioè con tutti i campi
            insieme e non col solo effetto. Controprovato rimettendo il difetto: la prova cade.
    - ⚠️ **Spento vuol dire non toccare niente**, che è un'altra cosa dal dipingere un velo
      trasparente: i dialoghi tornano al velo che Android dà loro (`0,6`), i menu a
      non averne. L'unica eccezione è la scheda in fondo, che se lo chiede da sé perché la sua
      finestra non ne ha uno di serie: `SHEET_DIM` in `Sheet.kt`, col perché misurato.
      - ⚠️ **Fino alla `1.43` qui c'era il numero**, 'i tredici dialoghi', e la `1.44` lo ha
        reso falso togliendone uno (la conferma di buttare via la selezione) senza che
        nessuno toccasse questa riga. Il criterio che lo vieta è universale e vive in
        `rules/Roccobot.md` § '🪶 Come si mantiene un file di regole'.
  - ⚠️ **Chi apre un `Popup` o un `Dialog` scritto in casa chiama `WindowVeil()` a mano**,
    perché là il modificatore non passa: lo fanno `MenuShell` e `Sheet`.
    - ⚠️⚠️ **ERA UN ELENCO DI TRE FINO ALLA 1.46, E UNO DEI TRE ERA SENZA VELO**: i menu si
      aprivano con due meccanismi (`MenuShell` più due `DropdownMenu` di Material), e il
      filtro nella testata della griglia non chiamava `WindowVeil()`, quindi accendendo
      l'impostazione era l'unica superficie dell'app a restare senza. A nasconderlo è stata
      una frase falsa nel codice, che dava il menu della schermata iniziale per *l'unico menu
      dell'app che non passa da `MenuShell`*: chi cercava i chiamanti ne trovava due e la nota
      gli diceva che erano tutti. ⚠️ **Un velo mancante non si vede**, come dice il blocco qui
      sopra, e con l'impostazione spenta di fabbrica non si vedeva nemmeno accendendola per
      caso.
    - ⚠️⚠️ **ADESSO LA DIFESA NON È PIÙ CHE L'ELENCO SIA VERO: è che l'alternativa non
      esiste.** Dalla `1.46` ogni menu dell'app passa da `MenuShell`, quindi il velo, lo
      stondamento, l'entrata, l'uscita, lo scorrimento e la collisione col bordo si scrivono
      **una** volta e li prendono tutti. Chi aggiunge un menu non ha un secondo modo con cui
      sbagliare. La ragione è dell'utente e non estetica: *è per avere un sistema affidabile.
      Se volessi reintrodurre un elemento decorativo come la vecchia linea color accento,
      basterebbe un unico ragionamento per tutti gli elementi*.
- ⚠️ **I menu non usano quel modificatore ma lo stesso numero**: là il posto lo decide un
  `PopupPositionProvider` (`MenuCenter` in `Menus.kt`), che riceve pixel e nessun `Density`. Il
  15% è la costante `LOWER_BY`, condivisa.
  - ⚠️⚠️ **UN MENU ANCORATO SI FERMA A TRE MARGINI DI GRIGLIA DAL VETRO, DALLA `1.91`, ED È
    UGUALE PER I TRE EFFETTI** (riscontro del giro della `1.89`, voce `menu-bordo` non approvata:
    *non voglio che il margine del menu sia a filo con i margini delle colonne ... e in più
    spostarlo un po' a sinistra*, e *la stessa soluzione funzionerebbe anche con la sfocatura*).
    Fino alla `1.90` la distanza dipendeva dalla scelta: al vetro con la sfocatura, metà del
    margine del FAB negli altri due casi.
    - ⚠️⚠️ **IL DIFETTO DELLA `1.67` NON ERA LA FASCIA SFOCATA, ERANO DUE BORDI VICINI**, e questa
      rilettura è quello che fa cadere i tre numeri di allora: *la vicinanza tra i due bordi genera
      un effetto 'linea sfocata'*. La `1.68` li aveva allontanati portando il pannello **al
      vetro**, la `1.91` li allontana nell'altro verso, tirandolo **dentro** oltre il margine
      della griglia. Per questo la stessa distanza va bene con tutti e tre gli effetti.
    - ⚠️⚠️ **E IL PANNELLO È PIÙ LARGO DI DUE CELLE PIÙ IL LORO SPAZIO** (*bisogna far sì che con
      l'ombra attiva il pannello sia più largo di due miniature della griglia più lo spazio che le
      separa*), col **tetto** che nella schermata iniziale entra in funzione: là le colonne sono
      due, quindi quel conto darebbe un pannello largo quanto la finestra. Le misure prese sul suo
      mockup e il conto vivono su `menuFloor`, in `Menus.kt`.
    - ⚠️⚠️ **SI SCRIVE COME UNA SOGLIA E NON COME UN MARGINE, ED È MISURATO DAL BANCO**: un margine
      è un **minimo**, e il candidato naturale di un menu ancorato lo rispetta già, quindi non lo
      muove. La `1.91` ci ha provato: col margine il pannello restava a 32 dal bordo invece dei 24
      voluti, e a prenderlo è stata la prova.
    - ⚠️ **L'aria dell'ombra si sconta**, perché vive **dentro** la finestra: senza toglierla, con
      l'ombra il pannello cadrebbe più dentro degli altri due.
- ⚠️ **Un dialogo a tutto schermo NON si sposta**, e non è una dimenticanza: `DestinationDialog`
  riempie la finestra, quindi non c'è nessun centro da spostare.
- ⚠️⚠️ **E DALLA 1.38 UNA COSA CHE ERA CENTRATA NON LO È PIÙ: le 'Info dettagliate sul file'**,
  diventate una **bottomsheet** appoggiata al bordo di sotto (richiesta dell'utente, giro della
  1.37). Una scheda ancorata a un bordo non ha un centro da abbassare, quindi non porta
  `Modifier.lowered()`: il velo, che dalla stessa versione viaggia con quel modificatore, se lo
  chiede da sé. Chi legge l'elenco qui sopra sappia che 'la scheda delle informazioni sul file'
  non ne fa più parte.

## 👆 Che cosa fa il tocco FUORI da una finestra

⚠️⚠️ **MODALE VERA SOLO SE LA FINESTRA ESISTE PER RACCOGLIERE UN INPUT SCRITTO. Tutto il resto
si chiude toccando fuori** (criterio dell'utente, riscontro del giro della `1.69`: *l'importante
è che ci sia coerenza e che solo le finestre che devono ASSOLUTAMENTE fornire un input siano
modali vere*). Applicato alla lettera, taglia l'elenco in modo netto: hanno un campo di testo
**Rinomina**, **Estensione**, **Indirizzo** e **Nuova cartella**, e sono le sole a non
rispondere al tocco fuori.
- ⚠️ **Le conferme di eliminazione NON sono modali**, e non è una svista: là il tocco fuori vale
  `Annulla`, cioè l'esito sicuro, quindi non c'è niente da proteggere. Una modale protegge il
  lavoro che si perderebbe, non la scelta che si eviterebbe.
- **Come si dichiara**: il parametro di `Modifier.lowered`, che non ha un valore di serie, **e
  `properties = loweredWindow(...)` con lo stesso argomento**.

⚠️⚠️ **QUELLE DUE RIGHE VANNO INSIEME, E DALLA `1.70` ALLA `1.72` LA SECONDA NON C'ERA: LE
QUATTRO MODALI NON ERANO MODALI** (riscontro dell'utente, giro della `1.70`, voce
`modali-quattro` non approvata). Il modificatore governa l'**aria dentro** la finestra, le
proprietà governano lo **schermo fuori**, e nessuno dei due può fare il lavoro dell'altro: la
fascia trasparente sopra il pannello appartiene alla finestra e la chiude un nodo di Compose,
mentre sotto il pannello si è fuori dalla finestra e a decidere è `dismissOnClickOutside`, che
il gestore delle finestre legge prima che l'app veda qualcosa. `lowered(null)` **dichiarava**
l'intenzione senza applicarla, quindi Rinomina, Estensione, Indirizzo e Nuova cartella si
chiudevano toccando sotto, e un nome scritto a metà si perdeva.
- ⚠️ **Un dialogo nuovo che dimentica la seconda riga torna a non essere modale e non dà nessun
  errore**: è la stessa forma di trappola del velo mancante, e per questo la regola è scritta
  qui e non solo nel KDoc.

⚠️⚠️ **E IL TOCCO SULL'ARIA NON HA FATTO NIENTE PER TRE VERSIONI, PER UNA RAGIONE CHE VALE
SEMPRE: UN NODO CHE È SIA DI LAYOUT SIA DI TOCCO NON RICEVE NESSUN EVENTO** (misurato dal banco
di prova il 2026-09-05, ed è la prima cosa che il banco ha trovato). Nella stessa catena di
modificatori, un nodo che implementa il solo `PointerInputModifierNode` prende il tocco e uno
che implementa **anche** `LayoutModifierNode` non lo prende, a parità di tutto il resto: la
hit-test dei tocchi scorre i nodi fino al primo nodo di layout e si ferma là, quindi un nodo che
è anche quel confine resta fuori dalla propria passata.
- **Quindi chi misura e chi ascolta sono due nodi**, e chi ascolta va **prima**, perché così il
  suo riquadro è la scatola gonfiata, cioè quella che comprende l'aria.
- ⚠️ **Nessun controllo sul testo del programma poteva vederlo**: il codice era giusto,
  compilava, e la funzione non c'era. È lo stesso genere di difetto del blocco della `1.70`, ed
  è la ragione per cui il banco di prova esiste.

⚠️⚠️ **E L'ASIMMETRIA CHE HA FATTO NASCERE LA REGOLA ERA UN DIFETTO, non due comportamenti**
(sua segnalazione, con schermata: *hanno un comportamento da modale se si tocca lo schermo SOPRA
e da finestra 'secondaria' se si tocca SOTTO*). La causa è nel modo in cui si ottiene il 15%:
`LowerNode` non sposta il pannello, **gonfia la scatola** dichiarata e ce lo posa in fondo,
quindi sopra resta una fascia trasparente alta fino al 30% della finestra che **appartiene al
dialogo**. Toccarla è toccare dentro, e `dismissOnClickOutside` non scatta; sotto si è fuori, e
scatta. Il perché per esteso vive su `airTop`, in `Centred.kt`.

⚠️⚠️ **I MENU SONO UN CASO GEMELLO MA DIVERSO, E IL LORO DIFETTO È CHE IL TOCCO ARRIVA ANCHE
SOTTO** (trovato dal censimento della UI del 2026-09-05, e misurato sul bytecode di Compose:
`createFlags` di `AndroidPopup_androidKt` parte da `FLAG_WATCH_OUTSIDE_TOUCH` e non mette mai
`FLAG_NOT_TOUCH_MODAL`). Da Android 12 la finestra di un popup **non è modale al tocco**: un
dito fuori dal pannello arriva a tutte e due le finestre, quindi il menu si chiude **e** l'app
apre la riga, la miniatura o la cartella che era sotto il dito.
- **A ripararlo dalla `1.70` è `MenuGuard`, uno solo, in `AivTheme`.** Fino alla `1.69` il velo
  che consuma quel tocco viveva **dentro una schermata** e ne copriva una su cinque.
- ⚠️ **Non si aggancia a `VeilStage`**, che è la mappa del velo: quella è vuota quando
  'Sfocatura dietro i pannelli' è spenta, cioè di fabbrica, e legare a lei una correzione la
  renderebbe una funzione facoltativa.

⚠️⚠️ **UN VELO CHE COPRE TUTTO ESISTE SOLO QUANDO SERVE, E LA `1.70` HA BLOCCATO L'INTERA APP
PER AVERLO IGNORATO**: si avviava, mostrava la griglia delle cartelle e non rispondeva a nessun
tocco (riscontro dell'utente, 2026-09-05). Il `MenuGuard` era in scena sempre e si limitava a
non **consumare** a menu chiuso.

- ⚠️⚠️ **La causa non è il consumo: è la HIT-TEST**, che sceglie **a chi mandare** l'evento
  prima che una riga del gestore giri. Fra fratelli sovrapposti quel giro va dall'ultimo
  disegnato al primo e **si ferma sul primo ramo che colpisce**, quindi un nodo con
  `pointerInput` grande quanto lo schermo e disegnato dopo il contenuto se li prende tutti.
  **Non consumare** vuol dire 'lascio decidere anche agli altri che hanno ricevuto l'evento',
  **non** 'lo lascio passare a chi è sotto': quelli sotto non lo ricevono affatto.
- **La forma giusta era già in casa, in `ViewerScreen.kt`**, e il suo commento la enuncia: il
  riquadro che richiama i comandi vive dentro un `if (!visible)`, *così quando ci sono non ruba
  il tocco al tasto*. Il rimedio della `1.71` è quello schema applicato al `MenuGuard`.
- ⚠️ **Il rimedio è l'ASSENZA, non una condizione più furba**: finché nessun menu è in scena il
  nodo non è nell'albero, quindi non esiste niente che possa intercettare. Una guardia dentro
  il giro dei tocchi lascia in piedi il nodo, cioè la causa. Il prezzo è una ricomposizione
  della radice a ogni apertura e chiusura di un menu, e costa un `Box` vuoto.
- ⚠️⚠️ **E IL FATTO ERA GIÀ SCRITTO IN `Menus.kt`**, sul cancello di `MenuShell` (*un popup
  sempre presente e trasparente in più si mangerebbe i tocchi*): la stessa trappola, presa con
  un `Box` invece che con un `Popup`. Una nota che descrive un difetto non impedisce di rifarlo
  con un altro strumento, e questa è la ragione per cui la regola è qui invece che in un
  commento accanto a un solo chiamante.
- ⚠️ **Non c'è modo di provarlo senza un dispositivo**, e questo difetto è arrivato in
  produzione per quello: `compileDebugKotlin` non vede niente, e il progetto non ha un banco di
  prova strumentale. Chi tocca un nodo che copre lo schermo lo sappia, e guardi **prima** se il
  nodo esiste anche quando non serve.

## 🌗 Il tema scelto DENTRO l'app non è quello di sistema

⚠️⚠️ **UNA RISORSA LETTA CON `colorResource` NON SA CHE TEMA HA SCELTO L'UTENTE NELL'APP, E
QUESTO DIFETTO È ARRIVATO A LUI DUE VOLTE** (riscontro del giro della `1.85`, punto A del campo
libero: *l'icona della testata della schermata home non passa più ai colori del tema scuro quando
si passa al tema scuro; nemmeno il FAB lo fa*). Una risorsa si risolve dalla **configurazione**,
cioè da quello che ha scelto Android; il tema dell'app vive in un `CompositionLocal`
(`LocalAivLight`) e può dire il contrario, perché l'app ha una voce sua in 'Aspetto'. Con
'Chiaro' scelto dentro AIV su un telefono in tema scuro, i due valori divergono e vince quello
sbagliato.
- **Come si legge una risorsa nel tema dell'app**: si costruisce un contesto con `uiMode`
  forzato (`createConfigurationContext`) e si legge da lì. Il pezzo è `aivLauncher` in
  `Theme.kt`, e serve a tenere le risorse come **fonte unica** dei due colori dell'icona invece
  di ricopiarli in Kotlin.
- ⚠️ **È la stessa famiglia del difetto delle BARRE DI SISTEMA** trovato dal censimento del
  2026-09-05: `enableEdgeToEdge()` senza argomenti costruisce due `SystemBarStyle.auto`, che
  leggono la configurazione, mentre il tema dell'app si risolve nella composizione due righe più
  sotto. Chi trova un colore che non segue il tema guardi **prima** da dove viene quel colore.
- ⚠️ **Il banco non lo vede**: una prova gira in una configurazione sola, quindi i due valori
  coincidono e la misura non dice niente. Questa è una cosa che si guarda sul telefono, con i due
  temi.

⚠️⚠️ **E DALLA `1.86` I DUE COLORI SONO INCROCIATI DI PROPOSITO** (stessa richiesta): l'icona in
testata segue il tema in vigore, il FAB porta **l'accento dell'altro** tema, e a menu aperto passa
a quello del tema in vigore. La sua ragione è scritta: *dà uno stacco maggiore e un accento
opposto mette più in risalto il pulsante flottante*. Chi trova un FAB che 'non segue il tema' non
lo corregga: è la specifica.

## 🎬 Le animazioni dentro una schermata che arriva

⚠️⚠️ **LE OPACITÀ SI MOLTIPLICANO, QUINDI UN'ANIMAZIONE GIOCATA SOTTO LA DISSOLVENZA DI UNA
SCHERMATA NON SI VEDE.** Il cambio di schermata è una dissolvenza di 180 ms su tutto il
contenuto (`ViewerActivity`, `cambioSchermata`): quello che un elemento fa in quel tratto lo fa
dietro un velo che parte da zero, e l'occhio ne prende soltanto la coda.

- **Misurato sull'entrata del FAB** (giro della `1.73`, voce `fab-centro` non approvata):
  all'avvio, dove nessuna schermata sfuma, il FAB parte visibile al 65% e si vede tutta la
  crescita dal 75%; tornando da una cartella, a metà opacità (67 ms) la misura era **già al 90%**,
  quindi restavano l'ultimo decimo e il rimbalzo. Lo stesso codice dava **due animazioni
  diverse**, e quella che si vedeva più spesso era la peggiore.
- **La regola**: un'animazione che deve *farsi vedere* aspetta che la schermata sia arrivata,
  perché giocata sotto la dissolvenza si vede solo per la coda.
- ⚠️ **Non vale per tutto**: quello che deve *arrivare insieme alla schermata* (uno sfondo, una
  fascia sfumata, il contenuto) resta giusto dov'è. La distinzione è fra un elemento che si limita
  a esserci e uno che racconta qualcosa mentre entra.
- ⚠️ **Fra la fine della dissolvenza e il primo fotogramma di un'animazione che aspetta passano
  quattro fotogrammi**, ed è della transizione e non dell'attesa: un `Transition` porta
  `currentState` su `targetState` un paio di fotogrammi dopo l'ultimo valore animato. Non si
  compensa accorciando l'attesa, perché quello è il momento in cui la schermata ha davvero
  finito di arrivare.

⚠️⚠️ **E IL FAB CHE 'TRABALLA/FLASHA' TORNANDO DA UNA CARTELLA NON ERA UN'ANIMAZIONE: SI
STACCAVA IN UNA FINESTRA SUA PER SEI FOTOGRAMMI** (riscontro del giro della `1.75`, voce
`fab-via`, misurato dal banco). La causa è a monte del cambio di schermata e non c'entra con le
opacità: `MenuShell` animava alla **prima composizione** (un `LaunchedEffect` parte anche là, e
`animateTo` verso un valore già raggiunto non muove niente ma tiene l'animazione in corsa per
75 ms), quindi `MenuState.visible` rispondeva 'menu in scena' e il FAB si spostava nella finestra
che gli serve sopra la sfocatura. Una finestra non è toccata dalla dissolvenza della schermata:
per quei sei fotogrammi il FAB si vedeva **pieno** mentre tutto il resto sfumava, e poi rientrava.
- ⚠️⚠️ **IL SECONDO DANNO NON SI VEDEVA AFFATTO**: con quel menu fantasma in scena entrava anche
  `MenuGuard`, che consuma **ogni** tocco nella passata `Initial`. Cioè per un decimo di secondo
  dopo **ogni** cambio di schermata l'app non rispondeva, che è in piccolo il blocco totale della
  `1.70`.
- ⚠️ **La correzione è in due punti**, e non sono due cure per lo stesso sbaglio: la guardia in
  `MenuShell` toglie l'animazione che non ha niente da animare, e `MenuState.visible` perde il
  termine `show.isRunning`, che non diceva niente che il valore non dicesse già e che era la via
  per cui quell'animazione diventava una risposta sbagliata.
- ⚠️⚠️ **E LA PROVA SI ERA SCUSATA DI NON VEDERLO**: `CambioSchermataTest` saltava i fotogrammi
  senza FAB con una nota che li dava per normali. Erano il difetto. La regola che ne esce vive in
  § '🧪 Quando si scrive una prova, e quando no': una scusa scritta accanto a un'asserzione
  saltata è il modo in cui una prova mente in verde.

⚠️⚠️ **E DALLA `2.11` IL FAB SI CONGEDA RIMPICCIOLENDOSI, QUANDO SI VA DOVE NON C'È** (punto A
del campo libero del giro accorpato: *quando dal menu del FAB approdo ad una schermata senza FAB
(esempio → Impostazioni), il pulsante deve sparire rimpicciolendosi fino a sparire*). Fra le due
varianti che aveva descritto la scelta era mia, e la dichiaro: **si rimpicciolisce tutto, glifo
compreso**, perché la seconda (il pulsante che fa da maschera) taglia il glifo mentre il cerchio
si stringe, cioè mostra per qualche fotogramma un disegno mutilato.
- ⚠️⚠️ **A SAPERE DOVE SI STA ANDANDO È SOLO CHI METTE IN SCENA LE SCHERMATE**, cioè `AivApp`: una
  schermata non sa dove porta una voce del proprio menu. Il valore viaggia in un `CompositionLocal`
  (`LocalSenzaFab`) fornito **dentro** la transizione, dove il contenuto uscente vive ancora e il
  bersaglio è già quello nuovo.
- ⚠️ **Lo legge `TapHoldFab` e non i suoi chiamanti**, così un FAB nuovo prende l'uscita per
  costruzione: è lo stesso criterio per cui `lowered()` porta con sé il velo.
- ⚠️⚠️ **DURA MENO DELLA DISSOLVENZA DI SCHERMATA, E IL CONTO È LA REGOLA QUI SOPRA LETTA AL
  ROVESCIO**: quei 180 ms scendono ripidi, e l'opacità della schermata che se ne va vale 0,54 a
  60 ms e 0,16 a 100. Con cento millisecondi e una curva che parte veloce, a 40 ms il tasto è già
  sotto la metà mentre la schermata è ancora al 72%. **Allungare quel numero peggiora**, perché il
  rimpicciolimento finirebbe sotto un velo che non lascia passare niente.
- ⚠️ **La scala si moltiplica al rimbalzo invece di sostituirlo**: toccando una voce del menu il
  FAB sta ancora tornando su dal suo rimbalzo, e due scale su due nodi darebbero un tasto che si
  stringe mentre un altro lo allarga.
- ⚠️ **Che cosa il banco misura** (`UscitaFabTest`): che il tasto copra meno pixel a metà corsa e
  nessuno alla fine, e le quattro combinazioni di *quando* l'uscita parte. La scala vive in un
  `graphicsLayer`, quindi il riquadro non cambia e una prova che guardasse `boundsInRoot` sarebbe
  verde con e senza la correzione.

⚠️⚠️ **IL MECCANISMO CHE SERVIVA AD ASPETTARE NON C'È PIÙ, DALLA `1.75`, E LA REGOLA RESTA.**
`LocalArrivo` e `ConArrivo` erano nati nella `1.74` per il solo chiamante che ne avesse bisogno,
l'entrata del FAB, e con lei se ne sono andati (riscontro dell'utente, giro della `1.74`:
*animazione all'ingresso (avvio e ritorno in home): se ne va. Preferisco semplificare*). Chi
dovesse far aspettare un'animazione li ritrova nella storia git, misure comprese: erano un
`CompositionLocal` acceso da dentro la `AnimatedContent` dei cambi di schermata, che è l'unico
posto ad avere la transizione in mano.
- ⚠️ **Il fatto che regge la regola non dipendeva da loro**: le opacità si moltiplicano comunque,
  e la misura dei due casi (all'avvio tutta la crescita, tornando da una cartella l'ultimo
  decimo) resta vera per qualunque animazione che qualcuno rimetta là sotto.
- ⚠️ **La prova del banco è cambiata di bersaglio insieme al codice**: `EntrataTest` misurava
  l'attesa e non aveva più niente da guardare, `CambioSchermataTest` misura che durante il cambio
  di schermata il FAB **non si muove e non cambia misura**, cioè la forma esatta del difetto che
  gli era arrivato due volte.

## 🖼️ Intestazione delle cartelle, e le due schermate che la portano

⚠️⚠️ **DALLA `1.76` L'INTESTAZIONE NON È PIÙ DELLA SOLA SCHERMATA INIZIALE, e quello che
condividono vive in `Front.kt`**: la frazione di schermo, il meccanismo della fascia che si
chiude, lo scorrimento che la chiude prima che l'elenco si muova, e le due sfumature in fondo.
La richiesta è del giro della `1.67` (*l'icona va posizionata esattamente come quella oggi
presente sulla schermata home, ma semitrasparente (~50%), e sotto, al posto del nome dell'app, il
titolo della cartella scritto un po' più piccolo per lasciare spazio anche a nomi lunghi. Poi, più
in basso, con un posizionamento analogo alla home, inizia la griglia delle immagini*).
- ⚠️ **Il trasloco non è di comodo**: quei numeri li ha dettati lui giro per giro, e sono **una**
  decisione per l'app. Copiarli nella seconda schermata avrebbe fatto due tavolozze che divergono
  al primo ritocco, che è la trappola scritta in `rules/Roccobot.md` § '🪶 Come si mantiene un
  file di regole'.
- ⚠️⚠️ **DUE NUMERI SONO CAMBIATI CON LA `1.77`, E LI HA DETTATI COL TELEFONO IN MANO**
  (riscontro del giro della `1.76`, voce `front-cartella` approvata: *Eccellente! Aggiustamenti
  molto secondari: il testo del titolo dev'essere un po' più grande, e l'icona può essere meno
  visibile (proviamo con opacità 30%)*). Il titolo sale di un gradino (`titleSmall` diventa
  `titleMedium`) e `FRONT_INK` scende da 0,5 a 0,3.
  - ⚠️ **La sua ragione di prima non cade con il corpo più piccolo**, e conviene saperlo per non
    rimetterlo: *un po' più piccolo per lasciare spazio anche a nomi lunghi* si ottiene
    dall'**andata a capo** (`FRONT_TITLE_LINES`), che non è cambiata. Il corpo ridotto era un
    secondo modo di dire la stessa cosa, e di quei due ne serviva uno.
  - ⚠️ **Resta comunque più piccolo di quello della testata**, che è `headlineSmall`: se la
    fascia scrivesse il nome alla misura in cui lo troverà in cima, la traslazione non avrebbe
    niente da raccontare.

⚠️⚠️ **LA TRASLAZIONE DEL TITOLO NON È UN'ANIMAZIONE IN PIÙ: È LA PARALLASSE DELLA FASCIA.** Lui
l'ha chiesta così (*il nome in alto deve traslare con un'animazione fluida nella testata e apparire
come già appare adesso in posizione finale*), e la fascia quel movimento lo fa da sempre: misura il
contenuto all'altezza piena e lo posa **centrato in quel che resta**, quindi chiudendosi lo alza
verso la testata. Le due copie del nome (quella grande nella fascia e quella della testata) si
scambiano con due opacità **complementari**, cioè senza un punto della corsa in cui il nome si
legga meno che agli estremi.
- ⚠️ **Il nome finisce due volte nell'albero semantico**, e nessuna delle due copie si può
  togliere: l'una si dissolve nell'altra, e un titolo che comparisse a metà corsa sarebbe un
  salto. Chi volesse chiudere quel buco lo faccia con `alpha` **semantico**.
- ⚠️ **In posizione finale è quella di sempre, per costruzione**: la copia della testata non è
  stata toccata, ha solo un'opacità.

⚠️⚠️ **QUANDO L'INTESTAZIONE DI UNA CARTELLA È CHIUSA: DUE RISPOSTE SUE, ED ERANO TRE.** Mentre
la griglia **carica** è aperto; tornando dal **visualizzatore** resta com'era, cioè chiuso se la
griglia non è in cima.
- **La seconda si ottiene da una regola sola e non da un ricordo**: con la griglia scorsa il
  intestazione non può essere aperto. Con le dita è già vero per costruzione (lo scorrimento si
  spende prima là), e quello che sfuggiva è il salto **programmato** all'immagine da cui si è
  tornati, che non passa dallo scorrimento annidato.

⚠️⚠️ **E LA TERZA SI È ROVESCIATA CON LA `1.78`: DURANTE UNA SELEZIONE NON SI CHIUDE PIÙ**
(riscontro del giro della `1.77`, voce `front-misure` non approvata). La ragione è sua e nessuna
delle due parti l'aveva prevista: *appena si tocca a lungo per iniziare a selezionare, lo
spostamento delle miniature in alto fa già selezionare più elementi a causa dello spostamento
repentino mentre si tiene premuto*. Cioè la chiusura automatica non era spazio guadagnato: era
una griglia che scorreva **sotto un dito appoggiato**, e il gesto da/a della selezione prendeva
tutte le miniature che le passavano sotto.
- ⚠️⚠️ **LA RAGIONE PER CUI ESISTEVA È DECADUTA CON IL CONTATORE, e questo è il pezzo che spiega
  perché il rovesciamento non costa niente**: si chiudeva perché in selezione la testata
  diventava il conto dei selezionati, e con la fascia aperta quel conto non aveva posto. Dalla
  `1.78` il conto vive **sotto il titolo** in tutti e due i posti, quindi la testata non ha più
  niente da liberare, e il titolo resta il nome della cartella anche in selezione.
- ⚠️ **Quindi la fascia si chiude in un modo solo, scorrendo**, e la costante che dava la durata
  della chiusura animata è uscita da `Front.kt`: serviva a quel caso e a nessun altro.
- ⚠️ **La nota 'finita la selezione non si riapre' è decaduta con la chiusura**: non c'è più
  niente da riaprire.

⚠️⚠️ **IL CONTO DEGLI ELEMENTI VA SOTTO IL TITOLO, E DICE 'ELEMENTI' E NON 'IMMAGINI'** (sua
specifica, giro della `1.77`: *il numero di elementi (non immagini) totali / selezionati
dev'essere indicato sotto il titolo, centrato, con un carattere leggermente più piccolo e meno
opaco*; e il punto (b) del campo libero: *non va più bene da quando ci sono anche i video*). Le
due copie, quella della fascia e quella della testata, si scambiano con le stesse opacità
complementari del nome.
- ⚠️⚠️ **`folders_count` NON SI RISCRIVE, e chi lo facesse romperebbe l'altro chiamante**: quella
  chiave ha **due** posti che la usano con due significati, e dice il vero in uno solo. La
  griglia contava tutto (immagini e video) con una stringa che dice 'immagini', ed era il
  difetto; la schermata iniziale conta `bucket.pictures`, cioè le sole immagini, accanto a
  `folders_clips` che conta i video, e là 'immagini' è giusto. Perciò la griglia ha una chiave
  **nuova**, `items_count`.
- ⚠️ **Il testo di `items_count` è copiato da `pick_count` lingua per lingua**, che diceva già
  esattamente 'N elementi': quello che cambia è la chiave, perché i due scopi sono diversi e un
  ritocco al conto della selezione cambierebbe l'altro in silenzio.
  - ⚠️⚠️ **E QUEL RITOCCO È ARRIVATO AL GIRO DOPO, cioè la divergenza per cui le due chiavi
    esistono**: dalla `1.80` `pick_count` dice *N elementi selezionati* (nota sulla voce
    `front-selezione`: *quando elenchi solo il numero di elementi s'intende il totale. Invece se
    c'è una selezione scrivi 'elementi selezionati'*), e `items_count` resta il totale. Chi legge
    che le due dicono lo stesso testo sappia che era vero fino alla `1.79`.

⚠️ **L'icona si rimpicciolisce prima di sparire, dalla `1.78`** (*man mano che si scorre, deve
prima rimpicciolirsi e adattarsi ad ogni fotogramma allo spazio disponibile in verticale, poi
sparire con una dissolvenza come fa adesso*), e la differenza fra le due vie è misurabile:
scalarla con `graphicsLayer` rimpicciolirebbe il **disegno** lasciando il posto occupato, quindi
il titolo sotto non salirebbe; misurandola, l'icona **cede** lo spazio. Il conto e la soglia
della dissolvenza vivono in `Front.kt`.
- ⚠️⚠️ **MA 'PRIMA' NON VOLEVA DIRE 'DOPO UN TRATTO A INCHIOSTRO PIENO', E DALLA `1.80` LE DUE
  COSE VANNO INSIEME** (riscontro del giro della `1.79`, voce `front-icona` approvata con una
  nota: *l'icona cartella deve iniziare la sua dissolvenza appena inizia a ridursi di dimensione,
  e arrivare alla dimensione minima e a opacità 0 contemporaneamente*). La `1.78` e la `1.79`
  avevano una soglia scritta a mano che non coincideva col punto in cui la misura comincia a
  stringersi: fra i due punti l'icona rimpiccioliva senza sbiadire.
- ⚠️ **La soglia si RICAVA dalla misura invece di essere un numero**: è l'apertura alla quale lo
  spazio concesso all'icona vale esattamente il suo lato massimo, quindi le due cose cominciano
  nello stesso istante per costruzione e non per coincidenza. E la rampa dell'inchiostro è
  **lineare**: una curva che parte con pendenza zero non farebbe vedere l'inizio della
  dissolvenza, che è esattamente quello che lui ha chiesto di vedere.

⚠️⚠️ **NÉ NEL CESTINO NÉ NELLA RICERCA, e nessuno dei due è una dimenticanza**: nel cestino il FAB
c'è sempre, quindi la sfumatura che lo tiene su un fondo neutro non potrebbe andarsene scorrendo,
che è metà di quello che ha chiesto; nella ricerca la testata porta un campo di testo e non un
titolo, cioè non c'è niente che possa traslare là dentro.

⚠️ **Le due sfumature se ne vanno scorrendo QUI e restano sempre nella schermata iniziale**, ed è
la stessa ragione al rovescio: là il FAB c'è sempre. La richiesta era *le due sfumature in basso
devono progressivamente sparire e lasciare campo libero alla griglia piena su tutto lo schermo*.
- ⚠️⚠️ **MA QUELLA RAGIONE È DECADUTA CON LA `1.83`, e conviene saperlo per non ripeterla come
  se reggesse ancora**: diceva che dove il FAB c'è sempre la sfumatura non può andarsene, perché
  serve a tenerlo su un fondo neutro. Dalla `1.83` il FAB di una cartella **passa sopra** le due
  sfumature, e in una cartella c'è sempre anche lui: quindi le due cose convivono. Per il
  cestino e per la ricerca resta vero il resto (nessuno l'ha chiesto, e nella ricerca la testata
  porta un campo di testo invece di un titolo che possa traslare), ma è una scelta non rivista,
  non una conseguenza.
- ⚠️⚠️ **E DALLA `1.85` QUI DI SFUMATURE CE N'È UNA SOLA** (riscontro del giro della `1.83`, voce
  `fab-sopra` approvata con una prova: *togli la seconda sfumatura sovrapposta, quella corta. SOLO
  DALLE CARTELLE, resta in home*). La coda esisteva per chiudere in pieno l'ultima striscia di
  schermo, e là sotto adesso passa il FAB. ⚠️ **Nella schermata iniziale resta**, e il valore di
  serie di `GroundFade` è di averla: un valore di serie rovesciato l'avrebbe tolta anche a lei.
  - ⚠️⚠️ **E NELLA SCHERMATA INIZIALE QUELLA CODA SI È ALZATA CON LA `1.91`** (sua richiesta dopo
    la `1.90`: *adesso che la griglia arriva fino alla fine del vetro anche in basso, la seconda
    sfumatura deve essere a 100% 10dp più in alto e finire il gradiente 15dp più in alto*). La
    ragione è la `1.90`: finché la griglia si fermava sopra la barra gestuale, sotto la coda c'era
    il fondo dell'app; adesso là sotto passano le miniature, quindi la stessa coda ha più da
    coprire. I due numeri vivono su `FOOT_SOLID` e `FOOT_REACH`.

⚠️⚠️ **DALLA `1.83` L'INTESTAZIONE DI UNA CARTELLA È LA VARIANTE 10 DEL MOCKUP, E LA COMPONGONO
QUATTRO INTERRUTTORI** (sua risposta a `d-frontespizio` e punto H del giro della `1.81`), che
vivono in 'Aspetto' sotto 'Intestazione delle cartelle': `frontWash` (la sfumatura dell'accento),
`frontSerif` (il titolo col carattere graziato), `frontFacts` (le pastiglie del peso e dei video)
e `frontPickAll` (la pastiglia 'Seleziona tutto').
- ⚠️ **La sfumatura si accorcia per COSTRUZIONE e non con un secondo conto**: è dipinta dietro il
  blocco che si stringe, quindi segue la fascia senza che nessuno la segua. Un'altezza calcolata
  a parte avrebbe due sorgenti dello stesso numero, che divergono al primo ritocco.
- ⚠️ **Col gradiente acceso l'icona passa in negativo**, e non è una variante estetica: sul fondo
  d'accento l'inchiostro grigio della cartella non si distinguerebbe.
- ⚠️⚠️ **I VALORI DI FABBRICA SONO TRE SU QUATTRO, E LA SCELTA È DICHIARATA PERCHÉ IL BRIEF
  DICEVA DUE COSE**: il titolo della decisione era *variante 10 di fabbrica* e l'elenco dei chip
  diceva *gli ultimi due accesi di fabbrica*. Senza la sfumatura la 10 non è la 10, quindi è
  acceso anche `frontWash`; resta spento `frontSerif`, il solo dei quattro a cambiare **come è
  scritto** il nome invece di aggiungere qualcosa. ⚠️⚠️ **E LA RISPOSTA A `d-front-serif` È
  ARRIVATA NEL GIRO DELLA `1.83`: `tre`**, cioè *va bene così*, quindi il valore di fabbrica non
  si tocca più.
- ⚠️ **Le pastiglie dei dati costano UNA query**, `Folder.weigh`, una volta per cartella: il peso
  non si ottiene sommando i file uno per uno, e il numero di video non si conta scorrendo
  l'elenco già caricato. La pastiglia c'è solo se il suo dato esiste, quindi in una cartella
  senza video la seconda non compare.
- ⚠️ **I due tocchi lunghi sono suoi** (punto A del campo libero della `1.82`): sul peso entra in
  selezione con tutto selezionato, sui video coi soli video.
- ⚠️⚠️ **E DALLA `1.89` LA FILA SI ALLINEA AL LATO DEL FAB** (sua richiesta, con schermata:
  *quando le pastiglie vanno a capo, voglio che quella nella seconda (che è sempre 'Seleziona
  tutto', essendo in ultima posizione) sia centrata a destra o a sinistra a seconda del lato in
  cui si trova il FAB*), così la riga che va a capo si trova sotto il pollice invece che dalla
  parte opposta. Il lato lo dà `fabEdge`, che legge la stessa scelta di `fabSide`.
  - ⚠️⚠️ **TOCCA SOLO IL CASO IN CUI SI VA A CAPO, ed è misurato dal banco e non ragionato**: il
    blocco dell'intestazione è centrato, quindi la fila si dimensiona sul **contenuto** e con le
    pastiglie tutte su una riga non le avanza un pixel da distribuire. La prima stesura della
    prova falliva **col codice giusto** proprio per questo.
  - ⚠️ **Quella prova poi è uscita, e va detto invece di lasciarla credere scritta**: per far
    andare a capo la fila serve una scena stretta, e là la pastiglia del comando non si disegna
    affatto, perché la fascia la ritaglia. È un caso limite che vale la pena guardare sul
    telefono: su uno schermo molto stretto, o con i caratteri grandi, 'Seleziona tutto' potrebbe
    non vedersi.

⚠️⚠️ **E DALLA `1.85` QUELLA VARIANTE È RIDISEGNATA, PERCHÉ DAL VIVO NON GLI È PIACIUTA**
(riscontro del giro della `1.83`, voce `front-dieci` non approvata: *a vederla dal vivo non sono
più così convinto della grafica, e mancano anche delle funzionalità che avevo dimenticato di
chiedere*). Quello che cambia, tutto suo:
- **Il gradiente parte dal 70% e finisce prima della griglia** (`WASH_PEAK`), dove la `1.83`
  partiva dal pieno e scendeva **una riga di miniature dentro** la griglia, cioè tingeva la prima
  fila di immagini.
- **Sbiadisce mentre si scorre** e sparisce quando la griglia è nella posizione nuova: prima
  restava pieno a fascia chiusa, ed è la metà della ragione per cui il titolo in testata non si
  vedeva.
- **Il titolo è un gradino più grande** (`titleLarge`) e **'Titolo graziato' cambia solo il
  carattere**: la cartellina d'accento che la `1.83` gli metteva sopra è uscita.
- **L'icona in negativo va al pieno** (`FRONT_NEG_INK`), perché su un gradiente al 70% una sagoma
  all'80% si spegne.
- **Le quattro pastiglie hanno lo stesso vestito neutro**, e il comando dice **'Deseleziona'**
  dopo il primo tocco.
- **La terza pastiglia dei dati conta le immagini**, che è la risposta `immagini` a
  `d-front-altro`.

⚠️⚠️ **IL TITOLO IN TESTATA SPARIVA SOTTO IL GRADIENTE, E LA CAUSA VALE PER OGNI `drawBehind`
CHE SCONFINA** (stesso riscontro: *il nome della cartella e gli elementi (selezionati o meno) non
passano più in testa allo scorrimento (lo spazio rimane vuoto)*). `drawBehind` disegna dietro il
contenuto **del proprio nodo**, non dietro i fratelli che il genitore ha già disegnato: il
gradiente viveva su un nodo che veniva dopo la testata, quindi il rettangolo che sale le finiva
sopra. Adesso la tinta vive sul blocco che contiene **testata più fascia**, e si accorcia da sé
come prima.
- ⚠️ **Con lei sparisce la misura della testata**: l'altezza da cui partire è quella del nodo,
  quindi non serve più un `onGloballyPositioned` con la sua ricomposizione.
- ⚠️⚠️ **E LA PROVA CHE LO PRESIDIA GUARDA I PIXEL**, perché nessuna misura di struttura poteva
  vederlo: il titolo c'era, era al posto giusto ed era opaco. Il come, e perché la prova sulla
  schermata da sola non bastava, vivono in § '🧪 Quando si scrive una prova, e quando no'.

⚠️⚠️ **DALLA `1.87` IL GRADIENTE ARRIVA FIN SOTTO LA BARRA DI SISTEMA** (sua richiesta, con
schermata: *puoi colorare la barra di sistema di Android dello stesso colore dell'inizio della
sfumatura? ... Stesso colore della prima striscia di pixel sul bordo (colore scelto al 40%)*).
Fino alla `1.86` il rettangolo si fermava al bordo dell'area sicura, quindi sopra restava una
striscia del fondo dell'app e la tinta cominciava con un gradino netto proprio dove l'occhio la
incontra per prima.
- ⚠️⚠️ **È UNA FASCIA PIENA E NON UN RETTANGOLO PIÙ ALTO, e la differenza non è di comodo**:
  allungando il gradiente il suo massimo si sposterebbe sopra la barra, e sotto la testata la
  tinta verrebbe più chiara del 40%, cioè cambierebbe la rampa che lui ha tarato al giro prima.
- ⚠️ **Il colore è quello della cartella**, come il resto del gradiente, ed è la sua precisazione
  (*ovviamente s'intende un colore diverso a seconda del colore di ogni cartella*): la fascia
  legge la stessa tinta, quindi non c'è un secondo posto da tenere allineato.
- ⚠️ **Segue lo scorrimento**: scorrendo si spegne insieme alla sfumatura, o resterebbe una
  striscia colorata in cima a una griglia che non ha più niente di colorato.
- ⚠️ **Le icone della barra non si toccano**: a `WASH_PEAK` sopra il fondo dell'app il contrasto
  con cui il sistema le disegna resta quello di prima. Chi alzasse quel numero guardi anche
  quelle.
- ⚠️⚠️ **E QUEL NUMERO GOVERNA TUTTI E DUE, che è la ragione per cui il ritocco della `1.89` è
  costato un carattere** (sua domanda: *se decido di passare da 40% a 25% come opacità d'inizio
  per la sfumatura, adatterai automaticamente il colore della barra di sistema?*, e poi *allora
  porta il valore a 25%*). La fascia legge `WASH_PEAK` e non una sua copia, quindi la barra prende
  sempre esattamente il colore da cui la sfumatura parte. ⚠️ **Il valore di oggi è 25%**, ed è il
  quarto in quattro versioni: la storia dei quattro vive sulla costante.

⚠️⚠️ **IL GRADIENTE SI DIPINGE COL DITHERING, DALLA `1.95`, E SENZA DI LUI FA LE BANDE** (sua
segnalazione: *noto un banding fastidioso nel gradiente dell'intestazione: riducilo al massimo. La
sfumatura dev'essere omogenea e di *MASSIMA* qualità*). La causa è aritmetica e non si toglie
scegliendo colori migliori: fra il picco (`WASH_PEAK`, un quarto) e il fondo ci sono pochi livelli
su 255, distribuiti su tutta l'altezza della fascia, quindi ogni gradino di colore è alto decine
di pixel ed è **visibile per costruzione**.
- **Il rimedio a 8 bit è il rumore ordinato**, cioè il dithering di Skia: sparpaglia l'errore di
  arrotondamento fra i pixel vicini e il gradino si scioglie. Android lo accende da sé in un
  `GradientDrawable`, e Compose **no**, ed è la ragione per cui la fascia lo faceva.
- ⚠️ **Non si ottiene con `Modifier.background(brush)`**: quella strada non dà accesso al
  `Paint`. Il pennello si posa a mano (`Brush.applyTo` più `drawIntoCanvas`), con
  `asFrameworkPaint().isDither = true`, che è la riga che fa tutto il lavoro.
- ⚠️ **Il rettangolo si dipinge più largo dello schermo** (l'aria della fascia piena sotto la
  barra di sistema): il pennello si costruisce sulla misura vera, o la rampa finirebbe prima del
  bordo.

⚠️⚠️ **E DALLA `2.04` IL RUMORE LO SCRIVIAMO NOI, PERCHÉ QUELLO DEL PAINT NON GLI È BASTATO**
(riscontro del giro della `2.03`: *secondo me la sfumatura può essere ulteriormente migliorata,
vedo ancora del banding. Se per fare un gradiente di qualità superiore serve gestire una
profondità colore più alta, o più memoria, o più risorse, per me va bene*). Il pezzo è `Dither.kt`
e vive in un file suo perché la spiegazione del difetto è più lunga del rimedio: uno shader che
gira **su ogni pixel**, legge la sfumatura e le somma un livello pieno di rumore triangolare.
- ⚠️⚠️ **PERCHÉ IL RIMEDIO DELLA `1.95` NON SIA BASTATO NON SI SA, E SI SCRIVE COSÌ INVECE DI
  INVENTARE UNA CAUSA**: sul banco quel dither si vede **agire** (184 righe miste su 210 col solo
  paint, contro 0 senza niente), ma il banco disegna col processore e il telefono con la scheda
  grafica. Il rimedio nuovo non dipende da quella risposta, ed è tutto il suo valore.
- ⚠️⚠️ **E DALLA `2.06` C'È ANCHE SOTTO ANDROID 13, PER SUA RISPOSTA** (`d-dither-vecchi` del giro
  della `2.05`: **`copri`**). Uno shader scritto a mano vuole `RuntimeShader`, che nasce con la 13,
  quindi fino alla `2.05` là restava la sfumatura della `1.95`, cioè quella in cui le bande le
  vedeva. La strada che resta senza shader è **precalcolare** la rampa col rumore già dentro e
  stenderla come **maschera** (`rampMask`): lo stesso conto, fatto una volta per misura invece che
  a ogni pixel di ogni fotogramma.
  - ⚠️ **La maschera è di sola opacità e il colore lo mette il paint**, perché il gradiente è una
    tinta sola con l'opacità che scende: un byte per pixel invece di quattro, e una tessera larga
    128 che si ripete, cioè un paio di centinaio di kB in tutto. Le bande sono orizzontali, quindi
    a romperle serve che il rumore cambi lungo la riga, e che si ripeta ogni 128 pixel non si vede.
  - ⚠️⚠️ **IL RUMORE VA SULL'OPACITÀ, E SI TAGLIA DA SÉ LA DOSE**: un livello di opacità ne muove
    `|tinta - fondo| / 255` sul risultato, quindi arriva forte dove i due colori distano tanto
    (cioè dove le bande si vedono) e piano dove distano poco. Il conto che porta ai due livelli
    scelti vive su `GRAIN_STEPS`.
  - ⚠️⚠️ **NESSUNO DEI DUE PUÒ GUARDARLA, E LA DOMANDA LO DICEVA PRIMA**: né lui né io abbiamo un
    telefono sotto la 13, quindi questo ramo lo presidia il banco e non l'ha guardato nessuno.
- ⚠️⚠️ **E NON SI PORTA ALLE ALTRE SFUMATURE, PERCHÉ IL CONTO DICE CHE LÀ NON SERVE**: quelle in
  fondo allo schermo attraversano **tutti** i livelli in un centinaio di punti, quindi un gradino
  viene alto **un pixel**; il gradiente dell'intestazione ne attraversa un quarto su quasi tutto
  lo schermo, e là un gradino viene alto **quasi quaranta pixel**. Stesso disegno, due aritmetiche.
- ⚠️ **La prova che lo presidia guarda i pixel** (`BandeTest`): misura che in una riga del
  gradiente i pixel non siano tutti uguali, che è il mattone di cui una banda è fatta, e che il
  rumore resti di un livello invece di diventare una grana. **Non** vede se le strisce si vedano:
  quello è percezione, e si guarda sul telefono.
  - ⚠️⚠️ **MA PER LA MASCHERA DELLA `2.06` QUELLA MISURA NON BASTA, ED È MISURATO**: la prima
    stesura contava le righe miste nel **disegno** ed è rimasta verde con il rumore azzerato,
    perché sul banco una riga porta due toni adiacenti anche senza niente, cioè nel disegno entra
    un rumore di Skia che non si distingue dal nostro. La prova buona guarda **la tessera**, dove
    il rumore o c'è o non c'è, più un secondo caso sul disegno che misura la cosa che può davvero
    rompersi: che la maschera si **tinga** col colore del paint invece di venire nera.

⚠️⚠️ **NEL TEMA SCURO L'ICONA TORNA POSITIVA, DALLA `1.95`, ED È SUA ISTRUZIONE** (*l'icona
dell'intestazione deve ritornare positiva (sovrapposta) per il tema scuro: bianco, opacità 40%*).
Quindi i casi sono tre e non due: senza gradiente l'icona è quella di sempre, col gradiente sul
tema chiaro è in negativo (il colore del fondo, che è la nota della `1.83`), col gradiente sul tema
scuro è **bianca** (`FRONT_DARK_INK`).
- ⚠️⚠️ **E DALLA `2.00` QUEL NUMERO È IL 20%, PERCHÉ L'HA GUARDATA SUL TELEFONO** (riscontro del
  giro della `1.95`, voce `front-icona-scura` approvata con una nota: *OK, ma mettila al 20%*). Il
  40% era il numero della richiesta, questo è quello della prova.
- ⚠️ **Il tema da guardare è quello dell'app e non quello di sistema**, cioè `LocalAivLight`: è
  la stessa famiglia del difetto che ha già colpito due volte l'icona in testata e il FAB, e che
  questo file racconta più sopra, dove il tema scelto dentro AIV diverge da quello di Android. E
  si legge **prima** del `graphicsLayer`, perché là dentro non si è più in composizione.

⚠️⚠️ **I QUATTRO GESTI DELL'INTESTAZIONE, DALLA `1.85`, SONO SUOI** (stesso riscontro): il tocco
sul **nome** lo copia e il tocco lungo **rinomina la cartella**; il tocco sull'**icona** sceglie
la copertina, e il tocco lungo sceglie il colore del gradiente **per quella cartella** fra sedici
tinte in una griglia 4x4.
- ⚠️⚠️ **DUE DEI QUATTRO SONO CAMBIATI DOPO**, e chi legge una nota vecchia lo sappia: il tocco
  sull'icona apriva il gestore file di sistema fino alla `1.93` (dalla `1.94` sceglie la
  copertina, § '🖼️ La copertina scelta a mano'), e il tocco lungo sul nome copiava il percorso
  fino alla `1.94`.
- ⚠️⚠️ **LA RINOMINA È LA STESSA FINESTRA DEL FILE SINGOLO, ED È SUA ISTRUZIONE** (2026-09-08:
  *usa esattamente la stessa finestra di rinomina del file singolo, ovviamente senza percorso né
  estensione ... tutte le altre logiche di rinomina sono identiche*). Quello che cambia lo dice
  il parametro `folder` di `RenameDialog`, e sono tre cose: il campo parte dal nome della
  cartella **intero**, i comandi sotto il campo non ci sono, e un'estensione eventuale resta nel
  campo invece di vivere accanto (*nei rari casi in cui la cartella dovesse avere un'estensione,
  eccezionalmente dev'essere visualizzata direttamente nello spazio nome*).
- ⚠️⚠️ **RINOMINARE CAMBIA IL `BUCKET_ID`, QUINDI LA GRIGLIA SI RIAPRE SU UN'ALTRA CARTELLA**:
  per il MediaStore l'identificatore viene dal percorso, e quello di prima non esiste più.
  Restando sul vecchio, la stessa schermata mostrerebbe una cartella vuota senza dare nessun
  errore. Con lui viaggiano copertina e tinta (§ '🖼️ La copertina scelta a mano').
- ⚠️ **Il nome nuovo si dà al disco e poi si dice al MediaStore**: `File.renameTo` sposta
  l'albero in un colpo, ma l'indice non se ne accorge da sé, quindi i file di prima si tolgono e
  quelli di dopo si aggiungono con `MediaScannerConnection`. Senza, la cartella nuova resterebbe
  invisibile all'app fino al prossimo giro dell'indicizzatore di sistema.
- ⚠️ **I gesti vivono sul nome grande e non sulla copia in testata**, ed è una scelta: la copia in
  testata è trasparente finché la fascia è aperta, e un nodo trasparente riceve comunque i tocchi,
  quindi metterli anche là darebbe un tocco sul vuoto che copia un nome.
- ⚠️⚠️ **APRIRE IL GESTORE FILE DIPENDE DAL TELEFONO, e la sua domanda era proprio questa** (*non
  so se si può fare*): Android non ha un'azione standard per 'mostrami questa cartella'. Si prova
  in due modi (la cartella esatta come documento dell'archivio primario, poi la radice), e se
  nessuno risponde l'app lo dice. Il perché non si chiede prima chi risponde vive su
  `Folder.openInFiles`.
- ⚠️ **Le prime otto tinte sono sue e le altre otto completano la ruota**: il criterio, e perché
  sono numeri e non risorse colore, vivono in `FolderTint.kt`.
- ⚠️⚠️ **E DALLA `1.89` OGNI TINTA È UNA COPPIA, UNA PER TEMA** (sua richiesta: *ognuno dei 16
  colori dovrebbe essere in realtà una COPPIA di colori: una per il tema chiaro e una per il tema
  scuro, fatti in modo che ci sia sempre una differenza minima dal colore di fondo*). Fino alla
  `1.88` il numero era uno solo, e la conseguenza è misurata: la menta `C0FFE5` sul fondo chiaro
  aveva un contrasto di **1,03**, cioè spariva. Il criterio, i fondi contro cui si misura e la
  coppia che ha avuto bisogno anche della saturazione vivono in `FolderTint.kt`.
  - ⚠️ **Nel selettore i tondi sono tagliati in due in orizzontale**, come ha chiesto: sopra la
    variante del tema chiaro, sotto quella del tema scuro, **in tutti e due i temi**. Undici
    coppie su sedici hanno le due metà diverse, e le altre cinque no perché quella tinta stacca
    già da tutti e due i fondi.
  - ⚠️ **La scelta resta un indice e non un colore**, quindi la nota di allora non è rovesciata:
    a cambiare col tema è come quel colore si scrive, non quale ha scelto lui.
- ⚠️⚠️ **E DALLA `1.91` LE SEDICI SONO ALTRE SEDICI: LA RUOTA INTERA, SU SUA ISTRUZIONE**
  (riscontro del giro della `1.89`, voce `tinte-coppie` accettabile: *crea tu una nuova palette di
  16 coppie che coprano tutte le tonalità possibili*, perché *al momento ci sono troppi verdi,
  verdini e azzurrini*). La sua critica ha un numero, ed è la ragione per cui la tavolozza non si
  poteva ritoccare: **sette tinte su sedici** cadevano in 55 gradi di ruota, perché otto erano sue
  e partivano dai colori di casa.
  - ⚠️ **Le tonalità sono a passo uniforme in OkLCh e non in HSL**, che è percettivo: lo stesso
    passo in HSL addensa i verdi e dirada i blu, cioè rifarebbe il difetto.
  - ⚠️ **I due bersagli di luminosità sono MISURATI sulla tavolozza che aveva approvato**, non
    scelti: così cambia la distribuzione delle tonalità e non il carattere. Il conto, il tetto di
    croma e i contrasti vivono in `FolderTint.kt`.
  - ⚠️ **Sono più distinguibili di prima**, ed è la cosa che ha chiesto: la coppia più vicina passa
    da **0,012 a 0,042** di distanza percettiva.
  - ⚠️⚠️ **CON LORO ESCE IL GRIGIO-BLU, cioè l'unico neutro**, e le cartelle già tinte **cambiano
    colore**: nell'archivio vive l'indice, e i sedici colori sono altri sedici. Non si evita
    rinumerando, perché non esiste una corrispondenza da tenere.
- ⚠️⚠️ **UNA CARTELLA CANCELLATA NON SI RINCORRE** (sua istruzione, 2026-09-08: *se una cartella
  ha un colore associato e viene cancellata, non occorre che l'app ricordi il suo colore*), quindi
  l'archivio non si pota. Il perché quello non sia nemmeno una perdita (il `BUCKET_ID` è il CRC
  del percorso, quindi una cartella ricreata si ritrova il suo colore) vive su `FolderTints`.

## 📐 Le griglie arrivano al vetro, anche in basso

⚠️⚠️ **DALLA `1.90` IL RIENTRO DI SOTTO NON STA PIÙ SUL CONTENITORE** (sua richiesta: *così come
abbiamo colorato la barra di sistema in alto, non sarebbe possibile riempire tutto lo spazio fino
al bordo, anche se c'è la linea della navigazione gestuale? Quella può restare in
sovrapposizione*). Fino alla `1.89` le due schermate mettevano `safeDrawingPadding()` sul
contenitore intero, e quello **toglie lo spazio prima** che la griglia cominci a disegnare:
sotto la barra gestuale non ci poteva arrivare niente.
- **Adesso quel rientro vive nel `contentPadding` della lista**, che è lo stesso spazio dalla
  parte giusta: le miniature scorrono sotto la barra e l'ultima riga resta raggiungibile, perché
  lo scorrimento ha quello spazio in più in fondo. La misura la dà `bottomInset()`, in
  `Front.kt`, che la leggono in due.
- ⚠️ **Anche il margine verticale della schermata si scompone**, e non è pedanteria: lasciato sul
  contenitore avrebbe fermato la griglia dodici punti sopra il vetro, cioè avrebbe risolto la
  cosa a metà.
- **Con lui la sfumatura in fondo arriva al vetro**, ed è quello che tiene la barra gestuale
  sopra un fondo neutro invece che sopra le miniature nude: è il gemello della fascia piena che
  in cima tiene la barra di sistema sopra il gradiente.
- ⚠️ **Il FAB della griglia non si è mosso**, perché vive in una finestra sua e i rientri se li
  mette da sé; quello della schermata iniziale invece li prendeva dal contenitore, quindi adesso
  porta `safeDrawingPadding()` come il velo della scorciatoia che lo illumina. Le due righe
  adesso coincidono, e prima divergevano.
- ⚠️⚠️ **NON HA UNA PROVA DEL BANCO, e la ragione è la stessa delle altre due della `1.89`**: là
  i rientri di sistema valgono zero, quindi una prova misurerebbe una somma di zeri. Si guarda
  sul telefono, con la navigazione gestuale e con quella a tre tasti.

## ⏫ Il salto in cima e in fondo, sul glifo del FAB

⚠️⚠️ **DALLA `2.07` I DUE TASTI NON CI SONO PIÙ: A PORTARE IN CIMA E IN FONDO È IL FAB, E IL SUO
GLIFO DIVENTA UN CHEVRON** (sua scelta del 2026-09-09, dopo aver guardato due mockup animati:
*questo è molto più pulito e fluido ... ho già scelto, appena possibile implementiamo questo*).
Il pezzo nuovo non esiste: niente colonna che compare, niente seconda finestra, niente tasto in
più da mettere da qualche parte, e il FAB non sparisce mai dallo schermo. Quello che cambia è il
**disegno** dentro un tasto che c'era già, con lo stesso incrocio di zoom e dissolvenza con cui
diventa la `×` a menu aperto.
- ⚠️⚠️ **E IL TASTO È UNO SOLO, DECISO DAL VERSO DELLO SCORRIMENTO** (*se scorro per vedere altre
  immagini in basso, appare solo il tasto 'giù'*): scorrendo verso il fondo il glifo diventa 'Vai
  alla fine', scorrendo verso l'alto 'Vai all'inizio'. Le due domande di prima (dove voglio
  andare, e quale dei due tasti tocco) diventano una sola. ⚠️ **Quello che si perde è
  dichiarato**: i due versi non sono più disponibili insieme, e chi vuole l'altro scorre un
  momento nell'altro senso.
- ⚠️⚠️ **A TASTO ARMATO IL TOCCO FA IL SALTO E NON APRE IL MENU**, che è la conseguenza diretta
  di un comando che vive **sul** FAB: il tratto in cui il menu non si apre è quello in cui il
  chevron si vede, e finisce da sé un secondo dopo l'ultimo pixel scorso.
- **I cinque numeri sono del mockup che ha approvato guardandolo**, e vivono in `Jump.kt`: 44
  pixel di corsa piena, 8 nel verso opposto per cambiare chevron, 150 ms di quiete a corsa
  incompleta, un secondo di attesa a tasto armato e un quarto di secondo di rientro.
- ⚠️⚠️ **IL CROSSFADE VUOLE UN ESPONENTE SOTTO UNO, ED È MISURATO**: con due opacità lineari
  incrociate, a metà corsa i due glifi sono tutti e due al 9% nello stesso fotogramma, cioè il
  tasto resta vuoto. A 0,8 la somma non scende mai sotto il pieno.

⚠️⚠️ **DOVE IL FAB NON C'È, IL COMANDO NON C'È PIÙ.** Nelle **impostazioni** è la sua risposta
alla lettera (2026-09-09: *lì non serve nessun tasto, in realtà ... le impostazioni che cerco le
trovo o con la ricerca o con le sezioni e le sotto-pagine, non scorrendo una lista finché non
vedo quello che cerco*); in **'Cartelle di sistema'** e nella vista ad albero della schermata
iniziale è la conseguenza di un tasto che vive sul FAB, e là il FAB non esiste.

⚠️⚠️ **IL GESTO SI GUARDA PRIMA DI `frontScroll`, E L'ORDINE DEI DUE `nestedScroll` È MISURATO**:
in una catena di modificatori quello scritto **per primo** riceve per primo il delta della lista,
e la fascia dell'intestazione ne consuma la sua parte per chiudersi. Scritto dopo, al motore del
glifo arrivava **zero** finché la fascia aveva spazio da chiudere: un colpo solo con somma 0,
contato da una spia messa dentro il nodo.

⚠️⚠️ **IL SALTO PASSA DALLO SCORRIMENTO ANNIDATO, ESATTAMENTE COME UN DITO, E QUELLA È LA RIGA
CHE FA FUNZIONARE LA SUA RICHIESTA** (*il tasto 'su' fa scorrere in cima fino alla visualizzazione
piena dell'intestazione*). Muovendo la sola lista, il salto arriverebbe in cima con la fascia
ancora chiusa; mandando il delta a `frontScroll` prima e dopo la lista, l'intestazione si riapre
**perché è quello che già succede col dito**, e non per una riga in più.
- ⚠️ **I segni sono due mondi**: `scrollBy` conta positivo verso il fondo, il puntatore conta
  positivo verso il basso, cioè verso l'inizio. Il banco lo presidia, perché un `-` di troppo dà
  un salto che va dalla parte sbagliata e non lo vede nessun compilatore.
- ⚠️ **La distanza è una STIMA, e serve solo alla durata**: una lista pigra non sa quanto è alto
  quello che non ha ancora composto. La corsa finisce quando nessuno prende più niente, quindi
  una stima lunga si ferma al bordo lo stesso.

⚠️⚠️ **UNA GUARDIA CHE IL MOCKUP AVEVA, E CHE QUI NON SERVE: DUE CONTROPROVE L'HANNO SMENTITA.**
Là una riga escludeva la corsa dal conto del gesto (*toccando 'vai all'inizio' la lista sale,
cioè scorre nel verso opposto a quello che ha armato il tasto*), e la prima stesura l'aveva
copiata. Togliendola, il banco restava verde, perché `glide` muove la lista dentro
`state.scroll {}` e quel movimento non risale la catena dei modificatori; e facendogliela
attraversare a mano, il chevron **non si gira lo stesso**, perché una corsa verso l'inizio muove
la lista nel verso che il chevron già indica. Il rischio non esiste in nessuna delle due strade,
e quella riga non c'è più, insieme alla prova che la presidiava.

⚠️ **Che cosa il banco misura e che cosa no** (`SaltiTest`): vede che il salto passa dallo
scorrimento annidato nei due versi, che a riposo il chevron non è in scena, che il verso segue il
dito e cambia con lui, e che nella schermata vera il FAB **annuncia** il salto solo a tasto
armato. **Non** vede quanto il glifo impieghi a cambiare per l'occhio, la curva del rientro né la
decelerazione: sono rese, e si guardano sul telefono.
- ⚠️⚠️ **DUE TRAPPOLE DEL BANCO, TROVATE SCRIVENDO QUESTE PROVE**: una `LazyColumn` senza
  `fillMaxSize` dentro una `Box` si dimensiona sul contenuto, quindi non ha un viewport più
  corto di lui e **non genera nessun evento** di scorrimento annidato; e uno `swipe` con la sua
  durata, col clock fermo, inietta i passi a un tempo che non avanza. Tutte e due davano una
  prova rossa **col codice giusto**.
- ⚠️ **E il FAB non c'è in una griglia montata con gli argomenti minimi**: `FabPop` compare solo
  se la schermata ha dove mandare, quindi una prova che lo guarda deve passarle almeno una
  destinazione.

## 🔖 Lo scorrimento di una schermata sopravvive alla schermata

⚠️⚠️ **DALLA `1.91`, ED È UNA SUA RICHIESTA** (campo libero del giro della `1.89`, punto A: *se
scorro la schermata home, entro in una cartella e poi torno alla home, voglio che sia nello stesso
punto dello scorrimento in cui si trovava al mio tocco sulla cartella*). Fino alla `1.90` la
posizione viveva **dentro** la schermata, e una schermata che cambia esce dalla composizione
portandosela via.

⚠️⚠️ **NON C'È UN ARCHIVIO SCRITTO A MANO: COMPOSE NE HA UNO FATTO PER QUESTO.** Un
`SaveableStateHolder` in `AivApp` tiene da parte quello che una schermata ha in `rememberSaveable`
e glielo ridà quando rientra, e `rememberLazyGridState` è proprio un `rememberSaveable`. Una mappa
di posizioni scritta da noi avrebbe coperto la sola griglia, e ogni schermata nuova avrebbe dovuto
ricordarsi di usarla.
- ⚠️ **La chiave distingue le cartelle fra loro** (`Screen.saveKey`), ed è una **stringa** perché
  finisce in un `Bundle`. Porta solo quello che fa identità: il nome di una cartella no, perché una
  cartella rinominata è la stessa cartella.
- ⚠️ **Cresce di una voce per schermata visitata e non si pota**: quello che tiene sono un indice e
  uno scarto, mentre un limite col suo sfratto sarebbe più codice di quanto ne risparmi.

⚠️⚠️ **E DALLA `1.92` TORNA ANCHE L'INTESTAZIONE, PERCHÉ LA `1.91` NE AVEVA RIPRISTINATA UNA
METÀ SOLA** (riscontro del giro della `1.91`, voce `scorri-torna` accettabile: *al ritorno in home
ritorno al punto giusto ma l'intestazione è attiva. Comportamento sbagliato: l'intestazione deve
apparire solo se mi trovo in cima alla griglia/lista*). Lo scorrimento rientrava dov'era e la
fascia ripartiva aperta: due misure della stessa cosa che dicevano il contrario.
- **La fascia è una FUNZIONE della posizione di scorrimento, quindi vive dove vive lei**: la sua
  apertura è passata in `rememberSaveable`, cioè dentro lo stesso `SaveableStateHolder`. Le due
  cose tornano insieme per costruzione, e non perché qualcuno le sincronizza.
- ⚠️ **Una FRAZIONE e non i pixel**: la rotazione cambia l'altezza della fascia, e i pixel di
  prima direbbero un'altra apertura. ⚠️ **Con lei cade la nota che diceva il contrario** (*riaprirlo
  alla rotazione è la cosa giusta da vedere*): valeva quando la lista si azzerava insieme, e dalla
  `1.91` la lista resta dov'era.
- ⚠️ **La griglia di una cartella non cambia**: là l'invariante è scritto come regola (con la lista
  scorsa la fascia si chiude) perché il salto all'immagine da cui si torna scorre **senza** passare
  dallo scorrimento annidato. Nella schermata iniziale quel salto non esiste.
- **La prova è `RientroTest`**, e misura la scena come lui la vede: dove comincia la prima cartella
  prima di uscire e dopo il rientro. ⚠️ Controprovata rimettendo il difetto: la cartella scende di
  180 pixel invece di 20.

⚠️⚠️ **E IL SALTO ALL'IMMAGINE DA CUI SI TORNA È DIVENTATO IL SECONDO PASSO**: la griglia riparte
da dov'era per conto suo, quindi quel salto serve solo quando nel visualizzatore si è **sfogliato**
fino a un'altra immagine, che di là non si vedeva. Sono la stessa richiesta letta fino in fondo.
- ⚠️⚠️ **LA BANDIERINA DI PRIMA SI SAREBBE ROTTA IN SILENZIO, ed è il difetto che questa nota
  esiste per non far rifare**: il salto si faceva 'una volta per visita', e a rimetterla a zero
  ci pensava il cambio di schermata che portava via il composable. Con lo scorrimento che
  sopravvive, anche la bandierina tornava indietro a `true` e il salto non si sarebbe fatto **mai**
  più. Adesso si ricorda **quale** immagine è già stata servita, e un indice risolve i due casi
  (la rotazione e il ritorno) con un dato solo.

## 🎨 Dove si vede il colore di una cartella, fuori dall'intestazione

⚠️⚠️ **DALLA `1.87` GLI STILI SONO QUATTRO, E LI HA SCELTI LUI FRA I MOCKUP** (giro della `1.86`,
domanda `d-colore-come`: *applica i seguenti stili di colore esterni all'intestazione:
`filetto`, `cornice`, `nome`, `alone`*). Fino alla `1.86` la tinta scelta col tocco lungo
sull'icona di una cartella si vedeva **solo** nel gradiente della sua intestazione, cioè dopo che
quella cartella era già aperta; adesso può servire a **riconoscerla** nella schermata iniziale.
- **Il filetto** è una riga sotto la copertina, **la cornice** un bordo intorno, **il nome** il
  titolo scritto nel suo colore, **l'alone** una sfumatura che scende dal bordo di sopra. I
  numeri sono quelli del mockup che ha guardato, e la ragione per cui il filetto è spesso quattro
  punti è scritta là: sotto quella misura le sedici tinte non si distinguono.
- ⚠️ **Erano cinque nel mockup**, e l'angolo piegato è quello che non ha preso: chi lo ritrovasse
  fra i disegni sappia che è stato visto e scartato.
- ⚠️⚠️ **DALLA `1.91` DI FABBRICA C'È 'NOME', ED È LA SUA RISPOSTA DOPO AVERLI PROVATI** (giro
  della `1.89`, `d-colore-come`: *imposta solo 'Nome' (il testo del titolo della cartella) come
  attivo per impostazione di fabbrica*). Fino alla `1.90` non se ne vedeva nessuno, e nemmeno
  quello era una scelta mia: la sua posizione dichiarata era *sono propenso a lasciare il colore
  solo lì*, e i quattro stili erano le proposte per cambiarla provandole.
  - ⚠️ **Resta vero che un valore di fabbrica non si sceglie per far vedere una funzione**: qui a
    sceglierlo è stato lui, con l'app in mano.
  - ⚠️ **Nell'elenco 'Nome' viene subito dopo 'Nessuno'**, ed è sua istruzione (testo
    `t-colore-stili`). L'ordine dei chip è l'ordine di dichiarazione dell'enum, e cambiarlo **non**
    tocca quello che è già salvato, perché nell'archivio vive il token e non la posizione.
  - ⚠️ **La voce di collaudo porta comunque il passo passo**, perché la funzione non si vede se la
    cartella non ha un colore suo: le due cose servono tutte e due.
- ⚠️ **Vale per le due viste della schermata iniziale**, copertine ed elenco, con le stesse
  misure: la domanda è *come riconosco una cartella*, e non cambia cambiando vista. Nella terza
  vista non c'è niente da tingere, perché là le cartelle sono percorsi letti dal disco e una tinta
  è appesa al `BUCKET_ID` del MediaStore.
- ⚠️⚠️ **NELLA FINESTRA DELLE DESTINAZIONI SI TINGE, DALLA `2.02`, ED È SUA RISPOSTA**
  (`d-dest-tinta` del giro della `2.01`: **`tinta`**, cioè *il colore entri anche fra le
  destinazioni*). Fino alla `2.01` era una scelta dichiarata al contrario, e la ragione tecnica
  che la reggeva era già caduta: diceva che portare le tinte là vorrebbe dire farle passare da
  `DestLook`, che è uno `staticCompositionLocalOf`, cioè ricomporre l'app intera a ogni colore
  scelto. ⚠️ **Ma la strada era già scritta**: dalla `2.01` le copertine si caricano **dentro**
  la finestra, nello stesso `produceState` che chiede le cartelle al MediaStore, e la tinta
  viaggia di lì insieme a loro.
  - ⚠️ **Quindi restava solo l'argomento di merito**, che era: una tinta serve a riconoscere una
    cartella **nell'elenco di casa**. A rispondere è stato lui, ed è una domanda che si fa
    guardando l'app: chi sceglie dove mettere un file cerca la stessa cartella che riconosce
    dal colore in casa.
  - ⚠️⚠️ **LA COPERTINA ERA ARRIVATA UN GIRO PRIMA, E NON PER LO STESSO MOTIVO**: quella era un
    **difetto** (la finestra mostrava la copertina predefinita al posto di quella scelta), questa
    è una funzione in più che ha chiesto lui. Il difetto e la sua causa vivono in § '🖼️ La
    copertina scelta a mano'.
  - **Le due viste non sono state toccate**, e questa è la prova che il trasloco era già fatto:
    `covers` e `tints` sono parametri che hanno da sempre, e la finestra adesso li scrive tutti
    e due invece di scriverne uno solo.
- **La voce vive in 'Aspetto' e la sua gemella nel dialogo delle opzioni**, cioè la scorciatoia
  del tocco lungo sul FAB della schermata iniziale, dove lui l'ha chiesta accanto alle colonne:
  una preferenza, una chiave, un valore di fabbrica, e i cinque nomi da una funzione sola.

⚠️⚠️ **E IL TOCCO LUNGO SUL FAB NON PUÒ AVERE UN ROVESCIO, PERCHÉ QUEL TASTO SPARISCE A METÀ
GESTO** (sua richiesta nello stesso giro: *la pressione lunga sul FAB in una cartella deve
selezionare/deselezionare tutto*). Appena c'è una selezione il FAB lascia il posto alla scheda
dei comandi, quindi un secondo gesto su di lui non arriva a nessuno: un'alternanza scritta là
sarebbe un ramo che nessun dito può raggiungere.
- **Il rovescio esiste e sono due**: il tasto 'Tutti' del pannello col proprio tocco lungo, e la
  pastiglia dell'intestazione dalla `1.85`.
- ⚠️ **A misurarlo è stata la prova, non una lettura del codice**: la prima stesura di
  `SelezioneTest` provava il gesto due volte ed è fallita alla prima corsa con *the node is no
  longer in the tree*. È il caso proattivo di § '🧪 Quando si scrive una prova, e quando no'
  applicato a una richiesta invece che a un difetto.

## 🖼️ La copertina scelta a mano

⚠️⚠️ **DALLA `1.94`, E LA CONDIZIONE CON CUI L'HA CHIESTA DECIDE COME È FATTA** (*immagine
personalizzata per le cartelle*, con la clausola *solo se si può fare in modo che resti la stessa
anche dopo l'eventuale eliminazione dell'originale*). Tenere l'indirizzo dell'immagine scelta
sarebbe costato una riga, e il giorno che quell'immagine viene cancellata o spostata la cartella
tornerebbe alla copertina automatica senza che nessuno abbia toccato niente: quindi l'immagine si
**copia in casa dell'app**, ridotta, e da quel momento non dipende più da dove è nata.
- ⚠️ **Non è più *quell'immagine*, ed è giusto dirlo**: è una sua riduzione a mille pixel di lato,
  perché una copertina si vede al massimo a cinquecento. Quello che ha chiesto è che resti.
- ⚠️ **Il file È l'archivio**: non c'è nessuna preferenza da tenere allineata, e la domanda
  *questa cartella ha una copertina?* si risponde guardando se il file esiste. Il perché per
  esteso, e il nome che porta l'istante (che è quello che tiene onesta la cache di Coil), vivono
  in testa a `FolderCover.kt`.

⚠️⚠️ **IL GESTO È IL TOCCO SULL'ICONA DELL'INTESTAZIONE, ED È SUA RISPOSTA** (`d-copertina-come`,
giro della `1.92`: *solo con il tocco singolo sull'icona dell'intestazione di una cartella*). È il
gesto che la `1.86` aveva lasciato libero chiedendo *un'azione alternativa realmente utile*, e la
sua risposta lo ha riempito.
- ⚠️⚠️ **E L'IMMAGINE SI SCEGLIE DENTRO AIV, DA QUALUNQUE CARTELLA** (seconda metà della stessa
  risposta: *può essere scelta dalla normale vista di AIV da qualsiasi cartella, non
  necessariamente quella di cui si sta impostando la copertina*). Quindi quello che parte dal
  tocco è una **modalità** e non una finestra: si sfoglia l'app com'è, e il tocco su una
  miniatura vale come scelta. Una finestra che elencasse immagini sarebbe una seconda galleria da
  tenere allineata a quella vera.
- **Non si esce dalla cartella**, ed è il caso comune: la copertina che si vuole è quasi sempre
  una delle immagini che si hanno davanti. Per prenderne una di un'altra cartella basta uscire e
  navigare.
- ⚠️ **La modalità vive nel modello e non in una schermata** (`ViewerViewModel.covering`), per la
  stessa ragione del minuto di 'Mostra nascoste': fra l'inizio e la scelta si cambia schermata, e
  uno stato dentro la griglia se ne andrebbe proprio nel momento in cui serve.
- **Vale per tutte e tre le griglie e per i recenti**, come la modalità del selettore di sistema:
  una modalità che funziona in una schermata su tre sembra rotta.

⚠️⚠️ **E SI VEDE ANCHE NELLA FINESTRA DELLE DESTINAZIONI, DALLA `2.01`: PRIMA NO, ED ERA UN
DIFETTO** (sua segnalazione, 2026-09-09: *quando copio o sposto e devo selezionare la
destinazione, le cartelle appaiono con la loro copertina originale, non con la personalizzata*).
Quella finestra riusa le **stesse due viste** della schermata iniziale, e le due viste hanno il
parametro delle copertine da sempre: quello che mancava era il valore, perché il parametro aveva
un **valore di serie vuoto** e la finestra lo ereditava senza scriverlo. Con la mappa vuota
`coverIn` cade sempre sulla copertina predefinita, e nel codice non c'era niente da leggere che
lo dicesse.
- ⚠️⚠️ **IL PRESIDIO È IL COMPILATORE E NON UNA PROVA, e qui è più forte**: `covers` ha perso il
  valore di serie, quindi chi apre una vista di cartelle **deve dichiarare** che copertine porta.
  Al primo build ha subito preso un chiamante che lo ometteva (`ColoreTest`), che è la
  controprova. È lo stesso criterio del parametro di `Modifier.lowered`, e la prova che regge è
  la **tinta**: quel parametro il valore di serie non l'ha mai avuto, e infatti la finestra la
  sua scelta la scrive a chiare lettere.
- ⚠️ **Il banco non poteva vederlo**, e va detto invece di fingere una prova: montare quella
  finestra vuole un MediaStore con delle cartelle dentro, che in Robolectric è vuoto, quindi
  l'elenco sarebbe vuoto e non ci sarebbe niente da misurare.
- ⚠️ **Le copertine si caricano DENTRO la finestra**, nello stesso `produceState` che chiede le
  cartelle al MediaStore, e con la stessa ragione: chi apre quella finestra (il visualizzatore,
  la griglia) non ha la mappa in mano. Nello stesso e non in un secondo, o l'elenco comparirebbe
  per un tratto con le copertine predefinite.

⚠️ **A dire che la scelta è in corso è una fascia in fondo allo schermo** (`CoverInvite`), che
vive accanto alla notifica di casa e sopra la transizione fra schermate: la scelta comincia in una
cartella e può finire in un'altra, quindi l'unica cosa che lo dice deve sopravvivere al cambio di
schermata. ⚠️ **Non è una notifica**: una notifica dice che una cosa **è** successa e se ne va da
sé, questa dice che cosa **sta** succedendo e resta finché la modalità è viva. Il suo tasto è la
via per lasciar perdere.

⚠️⚠️ **A TOGLIERE LA COPERTINA È LO STESSO GESTO CHE LA METTE, DALLA `1.95`, ED È SUA
RICHIESTA** (*aggiungiamo un gesto ricorsivo: se in modalità 'scegli copertina' tocco di nuovo
l'icona dell'intestazione DELLA STESSA CARTELLA, la copertina torna quella predefinita (ultima
immagine)*). Quindi il gesto è uno e fa e disfa, che è la forma che si impara una volta sola.
- ⚠️⚠️ **'DELLA STESSA CARTELLA' È METÀ DELLA SPECIFICA, e senza quella metà il gesto sarebbe
  un'altra cosa** (*se tocco una cartella di intestazione, poi vado in un'altra cartella e tocco
  la cartella dell'intestazione, si attiva la scelta della copertina per quella immagine*): il
  secondo tocco **su un'altra** cartella non azzera niente, sposta la scelta là. È la ragione per
  cui la modalità porta con sé il bucket da cui è partita.
- ⚠️ **La voce del menu del FAB c'è ancora ma è SPENTA** (`COVER_MENU_ROW`, in `FolderCover.kt`),
  ed è sua istruzione (*spegni la funzionalità del FAB senza eliminarla, in caso cambiassi idea,
  ma rinomina la voce in `Copertina predefinita`*). Chi la riaccende trova il ramo intero,
  compresa la prova del banco, che misura il **legame** con l'interruttore invece dell'assenza.
- ⚠️ **Nel menu non c'è mai stata la voce che SCEGLIE**, e non era una dimenticanza: a scegliere
  è il tocco sull'icona, e una seconda porta per la stessa cosa sarebbe un secondo modo da
  imparare per un comando che si dà una volta per cartella.

⚠️⚠️ **IL PRIMO TOCCO PORTA UN MINI-ONBOARDING, E IL TESTO È SUO ALLA LETTERA** (richiesta del
2026-09-08): il velo illumina l'icona dell'intestazione nell'arancione degli onboarding e sotto
scrive che cosa si può fare, comprese le due cose che non si vedono (l'immagine può venire da
un'altra cartella, e resta anche se l'originale sparisce) e come si torna indietro. Si vede una
volta sola, e la chiave è `Hint.COVER`.
- ⚠️ **Il riquadro da illuminare arriva da una misura e non da un conto**: l'icona vive dentro
  una fascia che si stringe a ogni pixel di scorrimento, quindi la sua posizione la dà
  `onGloballyPositioned`, e il velo la riceve come `Rect`. Un rettangolo calcolato dalle costanti
  dell'intestazione sarebbe giusto solo a fascia tutta aperta.

⚠️⚠️ **UNA RINOMINA FATTA DENTRO AIV SI PORTA DIETRO COPERTINA E COLORE, E DA FUORI NO**
(sua domanda, 2026-09-08: *cosa succede all'immagine memorizzata come copertina se rinomino la
cartella da AIV? E se la rinomino dall'esterno?*). Il `BUCKET_ID` del MediaStore è il CRC del
**percorso**, quindi una cartella rinominata è un'altra cartella per l'archivio: senza fare
niente, ogni rinomina lascerebbe orfani la copertina e la tinta. Da dentro si travasano
(`FolderCovers.move` e `FolderTints.move`); da fuori nessuno ci può arrivare, ed è per questo che
esiste la potatura.
- ⚠️⚠️ **LA POTATURA HA UN PERIODO DI GRAZIA, E NON È PRUDENZA GENERICA** (`FolderCovers.sweep`,
  trenta giorni): l'elenco delle cartelle vive del MediaStore, e una scheda SD smontata o un
  volume non ancora indicizzato lo fanno arrivare **corto**. Cancellando al primo giro, un
  telefono con la SD fuori perderebbe le copertine di tutto quello che c'è sopra. Con la grazia,
  una cartella che torna entro il mese si ritrova la sua.
- ⚠️ **L'elenco vuoto non cancella niente**: è il caso del permesso non ancora concesso, dove
  'nessuna cartella' non vuol dire che non ce ne sono.
- ⚠️ **La data che conta è quella del FILE**, che la potatura aggiorna a ogni giro per le
  cartelle vive: così non serve un secondo archivio con gli istanti, e la domanda *da quanto
  questa copertina non ha più una cartella* si risponde guardando il disco.

⚠️⚠️ **'PREDEFINITA' E NON 'AUTOMATICA', DALLA `1.95`** (sua correzione, 2026-09-08:
`PREDEFINITA* (non 'automatica')`): è la parola della voce nelle impostazioni e della notifica,
quindi vale in chat, nelle voci del documento di feedback e nei commenti, per il criterio di
§ '🗣️ Come si chiamano le cose'. La copertina 'predefinita' è quella che l'app sceglie da sé,
cioè l'ultima immagine della cartella.

⚠️ **Che cosa il banco misura e che cosa no** (`CopertinaTest`): vede il gesto sull'icona (che
convive col tocco lungo del colore senza confondersi), il legame fra la voce del menu e il suo
interruttore, la fascia dell'invito e la precedenza della scelta sulla predefinita. **Non** vede
la copia dell'immagine, che decodifica e riscrive un file: quella si prova sul telefono.

## 👁️ 'Mostra nascoste', e perché dura un minuto

⚠️⚠️ **DALLA `1.92`, ED È SUA SPECIFICA ALLA LETTERA** (campo libero del giro della `1.91`): il
menu del FAB della schermata iniziale porta **'Mostra nascoste'**, che rende visibili le cartelle
nascoste *temporaneamente*, per un minuto; alla scadenza tornano nascoste con una notifica
(*'Cartelle di nuovo nascoste.'*) il cui 'Annulla' **proroga** di un altro minuto; mentre sono in
scena la voce diventa **'Nascondi cartelle'** e cambia glifo; le cartelle in prestito hanno il 70%
di opacità e il segno `∅` d'accento nell'angolo in alto a destra; il **tocco lungo** sulla voce apre
un pannello con le nascoste, ripristinabili come nella pagina 'Cartelle nascoste'.

⚠️⚠️ **IL MINUTO VIVE NEL MODELLO E NON NELLA SCHERMATA, e la ragione è il caso d'uso**: le
nascoste si mostrano **per entrarci**, quindi fra l'accensione e la scadenza c'è un giro dentro
una cartella e il ritorno. Uno stato dentro `FolderScreen` se ne andrebbe con la composizione, cioè
proprio nel momento in cui serve. Vive su `ViewerViewModel.peek`, col suo conto alla rovescia.
- ⚠️ **Non si salva**: quello che si spegne da sé dopo un minuto non ha senso ritrovarlo riaprendo
  l'app, dove quel minuto sarebbe passato da un pezzo.
- **La notifica è quella di casa** (`Notices`, dalla `1.84`), quindi la voce dell'app resta una: a
  cambiare è che qui 'Annulla' guarda **avanti** invece che indietro.
- ⚠️ **Spegnere a mano non dice niente**: la notifica spiega una sparizione che l'utente non ha
  chiesto, e chi tocca 'Nascondi cartelle' l'ha chiesta.

⚠️ **La voce c'è SE E SOLO SE una cartella è nascosta**: senza, accenderebbe un minuto in cui non
compare niente e il tocco lungo aprirebbe un pannello vuoto.

⚠️⚠️ **E DALLA `1.93` IL TOCCO LUNGO SU UNA CARTELLA IN PRESTITO PROPONE IL CONTRARIO** (sua
segnalazione, con schermata: *la pressione lunga su una cartella nascosta deve proporre il
contrario, ovvero di renderla di nuovo visibile*). Fino alla `1.92` quel gesto offriva di
nascondere una cartella **già** nascosta, cioè un comando che non faceva niente.
- **Il verso lo decide il fatto e non un secondo stato**: il gesto è uno, e una cartella in scena
  può essere nascosta solo mentre il minuto la tiene in prestito. Due stati paralleli si
  contraddicono il giorno che ne cambia uno.
- **Il tasto riusa 'Mostra'**, la parola del pannello e delle impostazioni: un sinonimo nuovo qui
  sarebbe una terza parola per lo stesso comando.
- ⚠️⚠️ **CON LEI CAMBIANO ANCHE I DUE TESTI DEL 'NASCONDI', ED È SUA ISTRUZIONE** (*per
  uniformare cambia anche la versione 'nascondi'*): il nome della cartella va **fra apici** in
  tutte e due le finestre, e la spiegazione dice che cosa è (*un'impostazione di
  visualizzazione*) invece di raccontare dove si ritrova la cartella. Le quattro frasi sono sue
  alla lettera.

⚠️ **Il pannello del tocco lungo riusa le due stringhe della pagina delle impostazioni**, il titolo
e il comando: è la stessa richiesta fatta da due posti, e due testi nuovi sarebbero due traduzioni
da tenere allineate. ⚠️ E **non** si aggiunge uno scorrimento: `Sheet` scorre già da sé, e due
scorrimenti verticali annidati sono un errore che Compose segnala.

⚠️⚠️ **I DUE GLIFI SONO SUOI, E NON SONO L'OCCHIO DI MATERIAL** (arrivati il 2026-09-08, in tre
mandate: vale l'ultima): sono una **cartella** con un occhio, aperto e sbarrato. Material ne ha uno
che dice 'vedi' senza dire di che cosa, ed è la ragione per cui questi entrano in `res/` invece di
essere presi dalla famiglia, che è il criterio di § '🖌️ Come entra un disegno'.
- ⚠️⚠️ **UN ANGOLO L'HA ARROTONDATO LA SESSIONE, E LUI L'HA CHIESTO** (*nell'icona
  `folderHide.svg` non sono riuscito ad arrotondare come gli altri l'angolo evidenziato nello
  screenshot: pensaci tu prima di caricarla*). Il raggio non è scelto: è **quello degli altri
  angoli dello stesso disegno**, cioè la stessa curva ruotata di un quarto di giro, e la sua
  misura in Illustrator (0,4px) lo conferma. Fa 43 pixel diversi su 57.600, tutti sull'angolo.

⚠️ **Il testo che chiede conferma dice 'nessun file sarà eliminato', dalla `1.95`**, ed è sua
istruzione: prima diceva *nulla è cancellato*, che è la stessa cosa detta in modo da far pensare
proprio a quello che non succede.

⚠️⚠️ **UNA CARTELLA NASCOSTA NON È NEMMENO UNA DESTINAZIONE, DALLA `2.02`, ED È SUA ISTRUZIONE**
(2026-09-09: *le cartelle nascoste devono rimanere nascoste anche quando si copiano/spostano file
(se serve le rendo visibili di volta in volta)*). Fino alla `2.01` la finestra delle destinazioni
elencava **tutto** quello che il MediaStore restituiva, quindi nascondere una cartella la toglieva
dalla schermata iniziale e la lasciava in bella vista appena si copiava un file.
- **Il filtro è quello di casa e non un secondo conto**: `Folder.Bucket.isHidden` viveva in
  `FolderScreen.kt` e adesso vive accanto al tipo che interroga, in `Folder.kt`, così le due
  schermate che chiedono *questa cartella è nascosta?* leggono la stessa riga. Un secondo
  confronto sul percorso avrebbe due modi di trattare il separatore, e il primo a divergere
  sarebbe quello che nessuno guarda.
- ⚠️⚠️ **MA IL MINUTO DI 'MOSTRA NASCOSTE' APRE UN'ECCEZIONE, DALLA `2.03`, ED È IL SUO
  RISCONTRO** (giro della `2.02`, voce `dest-nascoste` accettabile: *deve valere anche per le
  destinazioni*). La `2.02` aveva letto la sua parentesi al contrario, e la voce di collaudo
  gliel'aveva chiesto in chiare lettere: fino a lei una cartella in prestito compariva in casa e
  non fra le destinazioni, cioè il prestito valeva a metà.
  - ⚠️ **La ragione di allora sembrava buona e guardava dalla parte sbagliata**: diceva che un
    elenco legato a un conto alla rovescia acceso altrove è imprevedibile proprio nel gesto in cui
    si sposta un file. Quello che non guardava è **perché** il prestito si accende, cioè per
    entrare in una cartella nascosta: copiarci dentro è la cosa che si vuole fare mentre dura.
  - **Il prestito arriva alla finestra con un `CompositionLocal`** (`LocalPeek`), come le
    preferenze di vista e per la stessa ragione: a chiederlo è una finestra, e passarlo come
    argomento vorrebbe dire quattro livelli per un dato che nessuno di loro guarda. ⚠️ **Non è
    `staticCompositionLocalOf` come `LocalDestLook`**, e la differenza è misurata sul costo:
    questo valore cambia due volte per prestito, e uno static local ricomporrebbe l'app intera a
    ogni cambiamento.
  - ⚠️ **Si fotografa all'apertura, come l'elenco**: il minuto scade da sé, e una finestra che lo
    seguisse farebbe sparire delle righe da sotto il dito mentre si sceglie dove mettere un file.
  - ⚠️ **Il cestino resta fuori lo stesso**, ed è la metà che si perde scrivendo il prestito come
    un'uscita anticipata dal filtro: quell'esclusione non ha niente a che vedere con le nascoste.
    A presidiarlo è il caso 5 di `DestinazioniTest`.

⚠️ **Che cosa il banco misura e che cosa no** (`NascosteTest`): vede il filtro nei due versi, il
segno sulla sola cartella in prestito e il testo della voce che cambia con lo stato. **Non** vede
il minuto, che vive nel modello: la scadenza, il suo avviso e la proroga si guardano sul telefono.
⚠️ Il filtro delle **destinazioni** invece lo misura `DestinazioniTest`, insieme al cestino: là la
prova è di sola logica, perché `destinations` è una funzione che si chiama senza montare niente.

## 📤 AIV come selettore: quando un'altra app chiede un'immagine

⚠️⚠️ **DALLA `1.89` AIV COMPARE FRA LE APP DEL SELETTORE DI SISTEMA** (sua richiesta, con
schermata di WhatsApp: *vorrei che AIV comparisse anche quando scelgo 'Altre app'*). Quella
schermata è **DocumentsUI**, e sotto gli archivi elenca le app che rispondono a
`ACTION_GET_CONTENT`: per comparire servono due cose insieme, il filtro nel manifesto e la
capacità di **restituire** un file.

⚠️⚠️ **MA QUEL FILTRO DA SOLO NON BASTAVA, E LA `1.91` AGGIUNGE `ACTION_PICK`** (riscontro del giro
della `1.89`, voce `selettore-app` non approvata: *non appare in elenco: né tra le app galleria,
né tra le app per allegare file*). Il filtro **c'era** davvero nell'APK, misurato col dump del
manifesto binario del file servito da Pages: la causa è a valle, e sono due fatti di sistema letti
sulle fonti.
1. **Le app di terze parti compaiono nel navigatore file SOLO se chi chiede usa `GET_CONTENT`**:
   in AOSP, `PickActivity.setupLayout` passa `includeApps` vero soltanto per quell'azione. Chi
   allega un file usa quasi sempre `ACTION_OPEN_DOCUMENT`, e là l'elenco è fatto dei soli
   **archivi**, cioè di chi espone un `DocumentsProvider`.
2. **Per le immagini, `GET_CONTENT` se lo prende il selettore foto di sistema**, che lo dichiara
   con priorità **105** (il navigatore file ha 100). Un'app normale non può competere, perché
   Android **azzera** la priorità dichiarata da chi non è di sistema.
- ⚠️ **Quindi `ACTION_PICK` è l'unica delle tre vie che un'app di terze parti può ancora servire**,
  ed è quella che usa chi chiede *un'immagine dalla galleria*. Il filtro dichiara i due tipi di
  cartella (`vnd.android.cursor.dir/*`) **e** i due diretti, perché i chiamanti si dividono fra i
  due modi.
- ⚠️⚠️ **UN `ACTION_PICK` PORTA UN INDIRIZZO, E NON È UN'IMMAGINE DA APRIRE**: è la sorgente in cui
  scegliere. Senza la guardia in `handleIntent` l'app si aprirebbe sul visualizzatore invece di
  lasciar scegliere.
- ⚠️ **Comparire anche fra gli archivi vuole un `DocumentsProvider`**, che è un lavoro a sé e non
  un filtro in più.

⚠️⚠️ **NON È UNA SCHERMATA NUOVA: L'APP SI APRE COM'È, E CAMBIA UNA COSA SOLA.** Il tocco su una
miniatura, che aprirebbe il visualizzatore, consegna il file a chi lo ha chiesto e chiude. Tutto
il resto (le cartelle, la ricerca, il cestino, le impostazioni) resta quello di sempre, quindi
non c'è una seconda navigazione da tenere allineata alla prima.
- **Vale per tutte e tre le griglie e per i recenti**: la ricerca e il cestino mostrano immagini
  come la cartella, e una modalità che funziona in una schermata su tre sembra rotta.
- ⚠️ **Indietro annulla**, perché chiudere senza `setResult` vale `RESULT_CANCELED`, che è
  esattamente quello che chi ha chiesto si aspetta.

⚠️⚠️ **L'INDIRIZZO SI PREPARA E NON SI PASSA COM'È, O L'APP PUÒ CADERE**: un `file://` che esce
dal processo fa scattare `FileUriExposedException` da Android 7, e le cartelle lette dal disco
portano proprio quello. La strada è la stessa della condivisione (`ImageActions.readableOutside`,
estratta nella `1.89` proprio perché adesso la leggono in due): un `content://` passa senza
copiare niente, un `file://` diventa una copia servita dal FileProvider.
- ⚠️ **Il permesso viaggia con l'intento**, cioè `FLAG_GRANT_READ_URI_PERMISSION`: senza, chi
  riceve si vede un indirizzo che non può aprire.

⚠️ **`launchMode="singleTop"` NON dà fastidio qui, e conviene saperlo perché sembra il
contrario**: chi chiede un risultato non passa da `FLAG_ACTIVITY_NEW_TASK`, quindi il sistema
crea l'istanza **nel task di chi chiama**, e `singleTop` riusa solo quella già in cima allo
stesso task. Con AIV già aperta per conto suo, le due copie convivono in due task.

⚠️ **Un'immagine per volta**: `EXTRA_ALLOW_MULTIPLE` non è gestito, e un chiamante che lo chiede
riceve comunque un file solo, che è una risposta legittima. La selezione multipla dell'app
esiste, ma consegnarla vorrebbe dire un comando in più nella scheda dei comandi.

⚠️⚠️ **NON HA UNA PROVA DEL BANCO, e la ragione è quella dichiarata in § '🧪 Quando si scrive una
prova, e quando no'**: il banco non ha un selettore di sistema né un'app che riceve, quindi
quello che si potrebbe misurare qui è il ramo interno e non la funzione. Si prova sul telefono.

⚠️⚠️ **MA UN INTENTO NUDO CHE ARRIVA A GIRO INIZIATO NON AZZERA PIÙ NIENTE, DALLA `2.78`, ED È UN
DIFETTO CHE GLI È COSTATO DEL LAVORO** (punto A del campo libero del giro dalla `2.75` alla `2.77`:
*quando l'editor è aperto, se passo a un'altra app (senza chiudere AIV), poi torno, mi ritrovo
l'editor chiuso e le modifiche in corso perse*). La causa è la stessa funzione che legge gli
intenti, letta due volte con due significati diversi.
- ⚠️⚠️ **TOCCARE L'ICONA DEL LAUNCHER CON L'APP GIÀ APERTA CONSEGNA UN `onNewIntent`, E NON UNA
  CREAZIONE**: è quello che `launchMode="singleTop"` promette, ed è scritto più sopra per un altro
  caso. Quell'intento è il `MAIN` del launcher, cioè non porta nessun indirizzo, e fino alla `2.77`
  cadeva nel ramo dell'avvio dall'icona: schermata a casa, cartella d'avvio riaperta, editor
  chiuso.
- **La guardia è il PRIMATO della lettura e non la schermata in cui si è**: `handleIntent` riceve
  `fresh`, che `onCreate` passa vero e `onNewIntent` falso, e un intento non fresco senza indirizzo
  e senza una richiesta di scelta esce senza toccare niente. Scritta come 'non azzerare se sono
  nell'editor' sarebbe un elenco di schermate da tenere aggiornato.
- ⚠️ **Una richiesta di scelta passa lo stesso**, anche a giro iniziato: un `GET_CONTENT` non porta
  un indirizzo ma chiede qualcosa, e chi lo manda aspetta il selettore. Per questo la guardia
  guarda tutte e due le cose.
- ⚠️⚠️ **QUELLO CHE RESTA FUORI SI DICHIARA**: se il sistema uccide il processo in secondo piano,
  il modello muore con lui e l'editor riparte comunque, perché il lavoro in corso vive nella
  composizione. La correzione copre il caso in cui l'app è viva, che è quello che succede tornando
  da un'altra app, e la voce di collaudo glielo dice.
- ⚠️ **Il banco lo vede perché la decisione è nel modello** (`RitornoTest`, tre casi controprovati
  togliendo la guardia): `handleIntent` si chiama senza montare niente, e quello che si misura è
  dove la schermata resta.

## 💾 Il salvataggio va sempre in Download, e il nome si chiede solo se lo chiedi

⚠️⚠️ **DALLA `1.77` 'SCARICA' NON APRE PIÙ IL SELETTORE DI SISTEMA: scrive in Download e basta**
(istruzione dell'utente: *niente scelta della cartella, sempre Downloads, che è l'unica che
funziona senza autorizzazioni, anche in vista di Play*). Fino alla `1.76` quel gesto passava da
`ACTION_CREATE_DOCUMENT`, che chiedeva dove e con che nome: due schermate per salvare
un'immagine.

⚠️⚠️ **MA DALLA `1.80` IL SELETTORE SI PUÒ RICHIAMARE, DIETRO UNA SUA OPZIONE** (riscontro del
giro della `1.79`, campo libero punto C: *va aggiunta un'opzione 'Scegli il percorso di
download'. Se attiva (di fabbrica, NO), deve apparire l'icona 'Percorso', per scegliere dove
scaricare il file*). Non rovescia il blocco qui sopra: la strada di serie resta Download, e
questa è la deroga per chi la chiede.
- **Spenta di fabbrica, e lo ha scritto lui fra parentesi**: il valore di fabbrica non si sceglie
  per far vedere la funzione.
- ⚠️ **Il comando è un'icona sulla riga del titolo della finestra del nome**, accanto a
  'Estensione', e il selettore che apre è **quello che c'era già**, cioè il ripiego di Android 9.
  Quella strada non è stata scritta due volte.
- ⚠️ **Il nome finale lo decide il selettore**: un fornitore di documenti può ritoccare il
  suffisso per far quadrare nome e tipo dichiarato, quindi un'estensione cambiata a mano nella
  finestra può tornare quella di prima. È lo stesso genere di fatto del `MediaStore` che aggiunge
  la propria estensione, scritto più sotto.

- ⚠️⚠️ **SU ANDROID 9 IL SELETTORE RESTA, E NON È UNA DIMENTICANZA**: `MediaStore.Downloads`
  nasce con l'API 29, e sotto quella la stessa cartella vuole `WRITE_EXTERNAL_STORAGE`, cioè un
  permesso che questa app non chiede e che a Play andrebbe motivato. Là il gesto torna al
  selettore, col nome già scritto dentro. Chi legge `ImageActions.downloadsWritable` sappia che
  serve a scegliere la strada **prima** di provare: un `false` di `saveToDownloads` vuol dire
  guasto e nient'altro.
- ⚠️ **`IS_PENDING` è la metà che si dimentica**: senza, un download interrotto lascia in
  galleria un'immagine tagliata. La riga si scrive in sospeso e si chiude alla fine, e se la
  copia fallisce si cancella.
- ⚠️ **I nomi doppi non si risolvono a mano**: li numera il `MediaStore`, che è l'unico a poterlo
  fare senza una finestra fra l'elenco della cartella e la scrittura.

⚠️⚠️ **IL NOME SI CHIEDE IN DUE CASI, E SONO SUOI**: l'impostazione **'Consenti rinomina al
salvataggio'** accesa, oppure un **tocco lungo** su 'Scarica', che vale per quella volta sola.
L'impostazione è **spenta di fabbrica**, e il valore di fabbrica non è scelto per far vedere la
funzione: salvare è un gesto che si fa di fretta.
- **La finestra chiede il SOLO nome**, e il suffisso si vede accanto al campo senza entrare nel
  campo: senza quello giusto la galleria non sa che cosa tiene in mano. Chi vuole cambiare
  formato ha 'Esporta/Converti', che è un'altra cosa e lo dice.
- **I due gesti della data**: il tocco breve infila `YYYYMMDD` dov'è il cursore, il lungo rifà
  il nome da capo con la sola data. Sono le due cose che si vogliono davvero fare con una data in
  un nome, e nessuna delle due si ottiene dall'altra senza cancellare a mano.

⚠️⚠️ **I COMANDI SOTTO IL CAMPO SONO TRE, E SONO GLI STESSI DI 'Rinomina'**: 'Seleziona tutto',
'Svuota' e 'Data', *tutti con lo stile solo-testo, senza tasto/pillola già usato in 'Rinomina'*
(riscontro del giro della `1.77`, voce `scarica-download`). Quindi anche 'Data', che nella `1.77`
era un gettone tonale, e il pezzo che li disegna è `Quiet`, lo stesso delle due finestre.
- ⚠️⚠️ **ERANO QUATTRO FINO ALLA `1.79`, E 'Estensione' È SALITA SULLA RIGA DEL TITOLO**
  (riscontro del giro della `1.79`, voce `scarica-comandi`: *mi ero espresso male ... 'Estensione'
  deve apparire sotto forma di icona a destra, allineato alla linea di base del titolo*). Con lei
  c'è **'Percorso'**, e l'ordine è il suo: *prima 'Percorso' e poi 'Estensione' ultima a destra*;
  una sola in scena sta comunque a destra, perché è la **fila** che si allinea al bordo.
  - ⚠️⚠️ **UNO SOLO È UNA PASTIGLIA COL TESTO, DUE SONO DUE ICONE, DALLA `1.82`** (campo libero
    del giro della `1.81`, punto C: *chiaramente non possono coesistere due pulsanti testuali in
    'Scarica' quando 'Destinazione' ed 'Estensione' sono entrambi attivi ... in quel caso si usano
    le icone ... Quando solo una delle due è attiva, si torna al pulsante testuale*). I due pezzi
    che li disegnano sono `TitlePill` e `TitleIcon`, condivisi con 'Rinomina', e a scegliere è il
    **conto** dei comandi in scena: scritte due volte, le due condizioni darebbero una pastiglia
    accanto a un'icona.
    - ⚠️⚠️ **MA DALLA `1.86` IL CASO DI UNO SOLO LO DECIDE LA MISURA, ED È SUA RISPOSTA**
      (`d-pill-soglia`: **misura**). Il conto resta per il caso di due, dove la risposta è già
      sua; con un comando solo la pastiglia resta scritta finché il **titolo** entra su una riga
      accanto a lei, e passa all'icona quando non entra più. La riga la compone `TitleRow`, uno
      solo per le due finestre, e il conto vive là insieme alla larghezza.
    - ⚠️ **Che entri si misura sul TITOLO e non sulla pastiglia**: la pastiglia entra sempre,
      perché è il titolo a cedere (non ha peso, quindi va a capo). Con la parola più lunga delle
      ventotto lingue, il polacco *Miejsce docelowe*, il titolo andava a capo e il testo non si
      troncava mai: il difetto era quello.
  - ⚠️⚠️ **I DUE GLIFI SONO SUOI DALLA `1.82`** (`Glyphs.FolderDownload` e `Glyphs.Extension`,
    arrivati nello ZIP del giro della `1.81`), e non sono più i due provvisori di Material che la
    `1.80` teneva in attesa della sua scelta.
  - ⚠️⚠️ **E DALLA `1.82` LE DUE CONDIZIONI SONO ROVESCIATE, SU SUA ISTRUZIONE** (voce
    `save-comandi`: *'Destinazione' deve comparire sempre solo quando fai un tocco lungo su
    'Scarica'*, e *'Estensione' deve seguire la propria opzione di visibilità nelle
    impostazioni*). Quindi 'Destinazione' vuole l'opzione accesa **e** il tocco lungo, e
    'Estensione' segue la sola opzione, con qualunque gesto. Fino alla `1.81` era il contrario:
    il tocco lungo le accendeva tutte e due a impostazioni spente.
    - ⚠️ **La `e` al posto della `o` non è un'interpretazione libera**: nello stesso giro lui ha
      riscritto la spiegazione di 'Scegli il percorso di download' in *aggiunge il pulsante
      'Destinazione' alla schermata di download*, e ha tagliato da quella della rinomina la coda
      che prometteva *in quel caso è disponibile anche 'Destinazione'*.
  - ⚠️⚠️ **E DALLA `1.83` LA REGOLA TORNA A ESSERE UNA SOLA PER TUTTI E DUE** (riscontro del giro
    della `1.82`, voce `save-quando` non approvata): col tocco **normale** ognuno dei due compare
    se la **sua** opzione è accesa; col tocco **lungo** ci sono tutti e due, qualunque sia lo
    stato delle opzioni. Quindi a impostazioni spente il gesto lungo li fa comparire lo stesso,
    che è la cosa che la `1.82` aveva tolto a 'Destinazione'.
    - ⚠️ **La lettura della `1.82` era dichiarata e gli è stata chiesta**, nella domanda
      `d-dest-lettura` del documento di feedback: la sua frase si poteva leggere con una `e`, e
      la risposta è che i due comandi sulla stessa riga non possono avere due regole diverse.
      L'opzione governa il tocco normale, il gesto lungo vale per entrambi.

⚠️⚠️ **SI TORNA A DOWNLOAD DALLA FINESTRA, DALLA `1.82`, PERCHÉ IL SELETTORE NON SA RIPORTARCI**
(voce `save-percorso`, non approvata: *se cambio cartella di download, non posso più tornare a
storage/emulated/0/Download. Il file picker mi dice che 'per tutelare la mia privacy' non posso
scegliere quella cartella*). Cioè una cartella scelta una volta era **definitiva**. Adesso la riga
che dice la cartella porta accanto il comando che la scorda, e **spegnere l'opzione la scorda
anche lei**: sono le due vie da cui si può restare chiusi fuori.
- ⚠️ **Il ritorno non passa dal selettore**, ed è la ragione per cui funziona: scordare l'albero
  scelto riporta a `MediaStore.Downloads`, che non è un percorso da scegliere.
- ⚠️ **Scordare rende anche il permesso persistente** (`DownloadFolder.forget`): un albero che non
  si usa più tiene uno dei posti che il sistema concede all'app.

⚠️⚠️ **E DALLA `1.82` L'APP SI RICORDA CHE COSA HA GIÀ SCARICATO** (campo libero del giro della
`1.81`, punto E: *l'app deve verificare se ha già scaricato di recente un file con la stessa
estensione e lo stesso numero di byte ... Testo: 'Hai già scaricato questa immagine'; azione a
destra: 'Scarica di nuovo'*). La firma è **suffisso più byte**, la notifica è la stessa
dell'eliminazione annullata con cinque secondi invece di tre, e il registro vive in
`DownloadLog`.
- ⚠️ **Trenta giorni e duecento voci**, che è il consiglio che aveva chiesto (*consigliami tu*):
  il conto che regge i due numeri è in testa a `DownloadLog`.
- ⚠️ **Senza i byte non si firma niente e si salva**: un sorgente di cui non si legge la lunghezza
  darebbe `0`, e allora tutte le immagini senza misura avrebbero la stessa firma.
- ⚠️⚠️ **E 'Rinomina' HA PRESO 'Data' NEL GIRO DELLA `1.78`**, che è la simmetria dall'altra parte
  (campo libero, punto (a): *voglio che 'Rinomina' abbia 'Data', che inserisce YYYYMMDD
  esattamente come implementato in 'Scarica'*). 'Esattamente come' regge solo se il gesto lo fa
  la stessa funzione, quindi il conto sul cursore è uno e vive in `SaveName.kt`.
- ⚠️⚠️ **E IN UNA RINOMINA IN BLOCCO IL TOCCO LUNGO PORTA I CANCELLETTI, DALLA `1.80`**
  (risposta a `d-data-blocco`: *con più file, scrivi AAAAMMDD più uno spazio seguito da un numero
  di cancelletti adeguato alla dimensione del set. Ad esempio, se sono 100 file, aggiunge 3
  cancelletti; con 50 file aggiungi 2 cancelletti. È la stessa logica di creazione del primo
  template*). Fino alla `1.79` lasciava la sola data, cioè un nome uguale per tutti, e 'Rinomina'
  restava **spento** finché non se ne aggiungeva uno a mano: era il prezzo di 'esattamente come
  in Scarica', dove il file è uno solo, ed era una domanda aperta invece di una decisione mia.
  - ⚠️ **'La stessa logica' è la stessa FUNZIONE**, `hashesFor`, quella da cui esce il template
    proposto all'apertura: due conti che si somigliano darebbero due numeri diversi il giorno che
    uno dei due cambia.
- ⚠️⚠️ **'Estensione' PASSA DAL PEZZO CONDIVISO `extensionGate`, che si porta dietro le proprie
  finestre**: la griglia di sicurezza (l'impostazione, spenta di fabbrica), l'avviso della prima
  volta e il pannellino. Chi lo chiama ottiene un tasto che funziona, non due righe da ricordare
  in fondo alla funzione, ed è lo stesso criterio per cui `lowered()` porta il velo.
- ⚠️⚠️ **E QUANDO IL SUFFISSO CAMBIA, IL TIPO DICHIARATO AL `MediaStore` SEGUE IL NOME E NON I
  BYTE, O IL FORNITORE RIMETTE IL SUO**: se tipo ed estensione del `DISPLAY_NAME` non vanno
  d'accordo, il `MediaStore` **aggiunge** l'estensione del tipo, quindi un `foto.png` dichiarato
  `image/jpeg` finisce in Download come `foto.png.jpg`. Cioè il comando non avrebbe fatto niente,
  senza dare nessun errore. Che il file menta è dichiarato e voluto: cambiare l'estensione non
  converte niente, e il pannellino lo dice a chi lo apre.
  - ⚠️ **Se il suffisso NON cambia resta il tipo misurato al caricamento**, e non si ricava dal
    nome: un nome può mentire già in partenza (il JPEG di Pexels dichiarato AVIF), e là il vero è
    quello che il caricamento ha letto.
- ⚠️ **È una modale vera**, quindi porta tutte e due le righe (`Modifier.lowered(null)` e
  `properties = loweredWindow(null)`): esiste per raccogliere un input scritto, che è il criterio
  di § '👆 Che cosa fa il tocco FUORI da una finestra'.
- ⚠️ **L'interruttore vive in 'Modifica e backup'**, che è la sezione della domanda *che cosa
  scrive l'app su disco, e con che nome*, e non ne apre una sua: una voce sola non prende un
  titolo.

⚠️⚠️ **E LA PROVA PROATTIVA HA PAGATO ALLA PRIMA CORSA, che è il fatto da tenere**: la prima
stesura della finestra teneva il testo in un `rememberSaveable` **senza** `TextFieldValue.Saver`.
Compilava, e in un'activity vera sarebbe andata in errore nell'istante in cui si apriva
(`IllegalArgumentException: cannot be saved using the current SaveableStateRegistry`), cioè un
difetto da segnalazione dell'utente. Il banco l'ha preso prima che uscisse.
- **Perché nessun altro controllo poteva vederlo**: il codice era valido e il tipo giusto; a
  mancare era un argomento che ha un valore di serie, e quel valore di serie lancia invece di
  avvisare. È la stessa forma del blocco della `1.70`, in piccolo.

## 🔄 Le otto pose dell'editor, e la fila che è diventata di cinque

⚠️⚠️ **DALLA `2.02` L'EDITOR RIFLETTE, ED È SUA RICHIESTA** (2026-09-09: *riusciamo ad aggiungere
un 'Rifletti in orizzontale' (a pressione lunga diventa 'Rifletti in verticale') nell'editor
interno? il comando potrebbe stare al centro fra 'Centra in orizzontale' e 'Ruota a
sinistra'/antiorario. Però forse non ci sta l'etichetta di testo*). Fino alla `2.01` l'editor
sapeva mettere un'immagine in quattro pose, adesso in **otto**: le quattro rotazioni e le stesse
quattro riflesse.

⚠️⚠️ **UNO SPECCHIO DAVANTI A UNA ROTAZIONE LA ROVESCIA, E QUESTO GOVERNA TUTTO IL RESTO.** La
posa è `Spin(turns, mirror)`, con l'ordine dichiarato **specchia, poi gira**, e comporre due gesti
non è sommare due numeri: vale `M ∘ R(k) = R(-k) ∘ M`, quindi un gesto di specchio che arriva
dopo un quarto di giro dà 'riflesso e girato di **tre**', non di uno.
- ⚠️⚠️ **UNA SOMMA AL POSTO DELLA SOTTRAZIONE NON DÀ NESSUN ERRORE, e su un'immagine dritta non
  si vede affatto**: le due pose coincidono finché non c'è già una rotazione. È il caso proattivo
  di § '🧪 Quando si scrive una prova, e quando no', e la prova è `RiflettiTest`, nata **con** la
  funzione.
- **Il conto vive in un posto solo**, `Spin.then`, e da lui dipendono le altre tre cose che si
  muovono insieme: che cosa si vede a schermo, il tag EXIF che il salvataggio senza perdita
  scrive, e il rettangolo di ritaglio.

⚠️⚠️ **L'EXIF HA DUE CICLI E NON UNA TABELLA, e uno specchio passa dall'uno all'altro
ROVESCIANDO L'INDICE**: `DIRECT` sono `1, 6, 3, 8` e `MIRROR` sono `2, 7, 4, 5`, cioè le stesse
quattro rotazioni viste allo specchio. Girare vuol dire avanzare di `turns` dentro il proprio
ciclo; riflettere vuol dire saltare all'altro ciclo alla posizione `turns - at`, che è la stessa
legge di sopra scritta con gli indici.
- ⚠️ **Non si ricopia una tabella di sedici caselle**: sarebbe la stessa legge scritta una seconda
  volta, e la prova che le lega (`ImageEdit.spun` composto due volte contro la posa composta, su
  tutte e otto le partenze) sta in piedi solo perché la fonte è una.

⚠️⚠️ **IL SUO DUBBIO SULL'ETICHETTA ERA FONDATO, E LA CAUSA È MISURATA**: con cinque celle da
`PAD_CELL` (76dp) più i quattro distacchi da `PAD_GAP` servono **412dp**, che su uno schermo da
360 non ci stanno. ⚠️ **E il difetto era già in casa**, cioè non lo portava il tasto nuovo: la
scheda della selezione con le etichette accese aveva lo stesso conto, e nessuno l'aveva
guardato.
- **Il rimedio è che la cella si stringe**, come faceva già la replica del riordino
  (`PadArrange`): con `stretch` la larghezza si divide fra le colonne e `PAD_CELL` resta il
  **tetto**, quindi dove c'è posto non cambia niente.
- ⚠️ **L'etichetta è 'Rifletti' e non 'Rifletti in orizzontale'**, e il verso lo dicono il glifo e
  il tocco lungo: una parola sola entra in una cella stretta in tutte e ventotto le lingue, mentre
  la locuzione intera non entrerebbe in nessuna. Il verticale ha la sua etichetta e la annuncia
  `holdLabel`, che è il meccanismo con cui l'app dichiara un tocco lungo.
- ⚠️⚠️ **MA LA CELLA STRETTA NON BASTAVA PER 'Centra in orizzontale', E DALLA `2.03` IL CORPO
  DELL'ETICHETTA È UN GRADINO SOTTO** (sua segnalazione con schermata: *riduci leggermente la
  dimensione delle etichette di testo delle funzioni dell'editor, in modo che ci stia l'intero
  contenuto*). A `labelSmall` quella parola chiede **77dp** e la cella ne vale 76: mancava un
  punto, e l'ellissi si mangiava tre lettere. Il numero di oggi vive su `padLabel`, in
  `ActionPad.kt`, con la misura che lo regge.
  - ⚠️⚠️ **LA MISURA VIENE DAI SUOI PIXEL E NON DAL BANCO, ed è la ragione per cui non c'è una
    prova**: con la grafica di Robolectric quella parola a 11sp entra perfino in 64dp, perché là
    il carattere è più stretto di quello del telefono. È il caso dichiarato in § '🧪 Quando si
    scrive una prova, e quando no', cioè quello che dipende dall'apparecchio: una prova sarebbe
    verde con e senza la correzione.
  - ⚠️ **Il corpo è UNO per tutte le file**, perché il tasto è uno solo: due corpi diversi sotto
    due icone identiche si vedrebbero prima nella scheda della selezione, dove le due file stanno
    una sopra l'altra.
- ⚠️ **Le due file dell'editor non si allineano più**, cinque contro quattro, ed è un compromesso
  dichiarato: allinearle vorrebbe dire una cella vuota in mezzo alla seconda, cioè un posto che
  invita a toccare qualcosa che non c'è.

⚠️⚠️ **IL GLIFO È DI MATERIAL E CI RESTA, DALLA `2.03`, ED È SUA RISPOSTA** (`d-flip-glifo` del
giro della `2.02`: **`resta`**, cioè *va bene quello di Material*). `Icons.Filled.Flip` era nato
provvisorio come i due della `1.80`, e la domanda esisteva perché là, nella stessa situazione,
aveva risposto mandando i suoi: qui ha scelto il contrario, quindi non si richiede più. Il criterio
che decide fra un glifo di Material e uno in `res/` vive in § '🖌️ Come entra un disegno'.

⚠️⚠️ **E IL TASTO NUOVO SI INFILA IN UN ORDINE GIÀ SALVATO, che è il caso che nessuno guarda**:
sul telefono di chi ha già usato l'app la fila è salvata con quattro gettoni, e senza un
meccanismo il quinto comparirebbe **in coda**, cioè dopo le due rotazioni invece che al posto che
ha chiesto lui. A metterlo dov'è dichiarato è `padOrderOf`, che legge `TURN_KEYS`: quella
costante è insieme l'ordine di fabbrica e il numero di colonne, e le due cose sono lo stesso
elenco.

## 🎚️ L'editor completo, e il conto che esiste in una copia sola

⚠️⚠️ **DALLA `2.14` GLI EDITOR SONO DUE, E IL NUOVO NON SOSTITUISCE QUELLO DI CASA: È LA SUA
SCELTA** (2026-09-11: *due editor separati*). Quello di casa mette un'immagine in **posa** e la
ritaglia senza toccare un pixel, questo la **sviluppa**. Un editor solo che facesse tutti e due i
mestieri dovrebbe
ricomprimere anche quando gira una fotografia, cioè perdere qualità per un gesto che oggi non ne
fa perdere. Chi tocca 'Modifica' sceglie fra i due la prima volta, e la scelta si ricorda.
- ⚠️ **Arriva in più versioni e il modulo **Luce** è la prima**: dopo di lei sono usciti il Colore,
  l'HSL, il Dettaglio, le **Curve**, l'anteprima a risoluzione piena, la **Geometria**, il
  **Ritaglio** e, con la `2.39`, i **preset** (§ '🎞️ I preset, venti di casa e quelli che si
  salvano'). ⚠️ **L'ordine di uscita non è l'ordine della fila**, che dalla `2.31` comincia dagli
  ultimi due arrivati: § '✂️ Il modulo Ritaglio, e la fila che è diventata di icone'.
  - ⚠️⚠️ **I PRESET ERANO 'L'ULTIMO PEZZO PREVISTO', E DALLA RISPOSTA `effetti` NON LO SONO
    PIÙ** (`d-dopo-editor`, giro della `2.50`, con la sua nota: *'Effetti', con 'Chiarezza',
    'Texture', 'Foschia', `Grana` e `Vignettatura`*). La domanda gli chiedeva da dove ripartire
    e offriva quattro strade; lui ne ha **accorpate due** in un modulo solo, quindi il nono
    portava **cinque** cursori e non tre, nell'ordine in cui li ha scritti.
    - ⚠️ **Le maschere restano fuori**, ed è la sua risposta del giro prima (`d-preset-manca`,
      2026-09-14: *le maschere no, nel modo più assoluto*): quella non è una tappa rimandata,
      è una porta chiusa.
    - ⚠️⚠️ **E DALLA `2.64` SONO TRE, PERCHÉ CHIAREZZA E TEXTURE SONO USCITE** (sua risposta
      `via` a `d-eff-restano`, giro della `2.63`). Quindi la foschia è **l'unica** del modulo a
      leggere i pixel vicini, cioè a costare il bordo delle tessere nel salvataggio (§ '🔍 Il
      modulo Dettaglio, e le prime due operazioni che guardano i vicini'); grana e vignettatura
      no, e il perché per esteso vive in § '✨ Il modulo Effetti, e i suoi cursori'.
    - ⚠️⚠️ **ED È USCITO IN TRE GIRI, DALLA `2.53` ALLA `2.57`**: chiarezza e texture per
      prime, poi la foschia, e infine grana e vignettatura. Il modulo vive in § '✨ Il modulo
      Effetti, e i suoi cursori'. ⚠️ **Un po' alla volta è sua istruzione** (2026-09-18:
      *procediamo un po' alla volta con le versioni e i test necessari ad ogni giro*), e il via
      libera per gli ultimi due è del giro della `2.55` (*Via libera per Vignettatura e
      Grana*).
    - ⚠️ **'Previsto' era vero quando era scritto**, e questa nota esiste per non farlo leggere
      come una promessa mancata: il piano di allora arrivava ai preset, e la domanda che lo
      chiudeva ha aperto la tappa dopo.

⚠️⚠️ **MA I DUE EDITOR SI CHIAMANO ALLO STESSO MODO IN TESTATA, DALLA `2.20`, ED È SUA
ISTRUZIONE** (2026-09-12: *in testa/titolo, mentre modifico le immagini, deve apparire 'Modifica
immagine', che è quello che sto facendo, sia che usi l'editor semplice, sia che usi quello
completo. L'utente deve pensare alla differenza tra i due (e alla loro stessa esistenza) solo
quando fa la scelta*). Il titolo dice **che cosa si sta facendo** e non con quale dei due arnesi:
chi ha già scelto ha finito di pensarci.
- ⚠️ **La distinzione resta dov'è la scelta**, cioè nel selettore che si apre toccando 'Modifica'
  e nella voce delle impostazioni: là `editor_full` continua a nominare l'editor completo, ed è
  l'unico posto in cui quel nome compare.
- ⚠️⚠️ **E DALLA `2.72` QUEL TITOLO DICE 'MODIFICA' E BASTA, ED È SUO** (campo libero del giro
  della `2.70`: *'Modifica immagine' diventa solo 'Modifica'*). La regola non cambia, cambia la
  lunghezza: i due editor si chiamano ancora allo stesso modo, e a chiedere la parola più corta è
  stata la testata, che nello stesso giro ha preso due icone (§ '🎛️ I due tasti del
  salvataggio, e i loro due gesti'). ⚠️ **È il testo di `menu_edit`**, cioè la voce da cui si
  entra, quindi non nasce nessuna stringa.
- ⚠️ **Quindi la regola della `1.49` è decaduta** (*questa schermata si chiama Editor e non
  Modifica*): valeva quando l'editor era uno e il titolo poteva dire il nome dell'arnese. Chi
  trova quella nota in un commento vecchio sappia che il criterio di oggi è il rovescio, per il
  criterio di § '🗣️ Come si chiamano le cose'.

⚠️⚠️ **I CURSORI DELLA LUCE SONO I SEI DEL PANNELLO BASE DI LIGHTROOM, DALLA `2.16`, E IL SESTO
NON C'ERA MENTRE UNO DI QUELLI DI PRIMA È USCITO** (riscontro del giro della `2.15`, voce
`luce-taratura` accettabile: *'Luminosità', oltre a confondermi (Lightroom ha solo 'Esposizione'),
è anche ben poco 'smart' dato che l'output va da 100% nero a 100% bianco*). Fino alla `2.15` erano
cinque e uno era **Luminosità**, cioè un passo additivo verso il bianco o verso il nero: al fondo
della corsa dava esattamente il rettangolo pieno che lui ha descritto.
- **Al suo posto entrano i punti di BIANCO e di NERO**, che spostano i due estremi dell'intervallo
  tonale e ridistribuiscono quello che c'è in mezzo. ⚠️ **Non possono appiattire l'immagine per
  costruzione**: ogni punto si muove al massimo di un quarto della scala, quindi l'intervallo più
  stretto che si può chiedere vale comunque metà.
- ⚠️ **Non è una sostituzione alla pari, ed è la ragione per cui sono due**: la luminosità toccava
  tutto allo stesso modo, i punti toccano gli estremi. Quello che lei faceva bene lo fa
  l'esposizione, che è il cursore che Lightroom ha al suo posto.
- ⚠️ **Ombre e luci restano un'altra cosa dai punti**, e le due coppie si distinguono per quanto
  sono larghe: una fascia contro un punto. Chi ne togliesse una perderebbe il recupero.
- ⚠️ **L'ordine è il suo, cioè quello di Lightroom**: esposizione, contrasto, luci, ombre,
  bianchi, neri. Chi apre questo editor ha in mente quel pannello.

⚠️⚠️ **E I CURSORI NON SONO PIÙ QUELLI DI MATERIAL, PER DUE RAGIONI CHE SONO TUTTE E DUE SUE.** La
prima è l'aspetto, dallo stesso riscontro (*credo che mi piacerebbero di più dei bei tondi grossi
al posto delle barrette verticali Material*), e da sola non basterebbe, perché quel pezzo accetta
un tondo scritto da noi. La seconda è il **doppio tocco che azzera**: uno `Slider` risponde al
primo tocco saltando al punto, quindi il primo dei due scriverebbe nella storia un valore che
nessuno ha chiesto.
- **Adesso il passo aspetta di sapere se i tocchi erano due**, e chi tocca due volte ottiene un
  passo solo. ⚠️ **Il trascinamento invece non aspetta niente**: appena il dito supera la soglia
  si muove, e il doppio tocco non è più possibile.
- ⚠️⚠️ **L'AZIONE SEMANTICA VA TENUTA**: senza `setProgress` un cursore scritto in casa è muto per
  un lettore di schermo e **invisibile al banco di prova**, che i cursori li muove da lì. Costa tre
  righe, e senza di lei `LuceTest` non misura più niente.
- ⚠️ **I sei cursori sono una tabella e non sei blocchi copiati**: ognuno porta tre gesti, e
  scritti riga per riga sarebbero diciotto occasioni di sbagliarne uno. Un cursore nuovo prende i
  gesti per costruzione, che è lo stesso criterio per cui `Modifier.lowered()` si porta dietro il
  velo.

⚠️⚠️ **I DUE GESTI NUOVI SONO SUOI, E IL SECONDO CONFRONTA UN CURSORE SOLO** (richiesta del
2026-09-11: *doppio tocco sul nome, sul cursore o sul percorso = reset dello slider. Dito premuto
sul nome = 'vedi originale' fino a rilascio del dito, ma solo relativo alla modifica dello slider
stessi rispetto all'originale*). Quindi i confronti col prima sono due: il tocco lungo
sull'**immagine** mostra l'originale intero, quello sul **nome** mostra l'immagine senza quel solo
cursore, che è la risposta alla domanda che ci si fa muovendo una manopola.
- ⚠️ **Il tocco lungo vive sul nome e non sulla barra**, e non è una scelta di comodo: sulla barra
  il dito è già appoggiato mentre si trascina, quindi scatterebbe ogni volta che ci si ferma un
  istante a guardare.
- ⚠️ **Il valore da confrontare lo costruisce la scheda e non il palco**, con lo stesso `write` con
  cui il cursore scrive: al palco arriva un `Look` già fatto, e lui disegna quello che riceve.
- **Il numero accanto al cursore azzera ancora**, ed è l'unica delle quattro superfici che si vede
  da sola, cioè che dice 'sono io il comando'.

⚠️⚠️ **E DALLA `2.58` IL CONFRONTO SULL'IMMAGINE NON TOGLIE PIÙ TUTTO, ED È SUA RICHIESTA** (campo
libero del giro della `2.55`: *pressione lunga sulla foto nell'editor: se mi trovo nei moduli
Ritaglio o Geometria il 'Prima' deve mostrare *tutto*; se è attivo un altro modulo il 'Prima' deve
mostrare tutto *tranne Geometria e Ritaglio**). Chi sta tarando un colore e preme per vedere com'era
vuole vedere **quel** colore com'era: fino alla `2.57` l'immagine tornava anche alla sua inquadratura
di partenza, cioè fra il prima e il dopo c'era un movimento che con quel cursore non c'entra.
- ⚠️ **La divisione è quella dichiarata in testa a `Look`**: i sei campi che passano dallo shader
  dicono di che **colore** è un pixel, gli altri quattro dicono **dove** va. Il confronto ne toglie
  un gruppo, e quale dei due lo decide il modulo che si sta guardando (`Look.place`).
- ⚠️⚠️ **LA VISTA CONFERMATA STA DALLA PARTE DEL 'DOVE', E SENZA DI LEI IL CONFRONTO RIAPRIREBBE UN
  TAGLIO GIÀ APPLICATO**: quel campo non cambia un pixel del file, ma dice che cosa il palco
  inquadra. Chi aggiunge un campo a `Look` guardi quella riga: un campo di colore dimenticato là
  resta applicato nel confronto, cioè smette di essere confrontabile, e nessuno dei due casi dà
  errore.
- ⚠️⚠️ **NEL RITAGLIO QUEL RAMO NON LO RAGGIUNGE NESSUN DITO, E SI DICHIARA**: là il palco fa solo
  quello, cioè il dito serve alle squadrette e il confronto non parte, ed è così da quando quel
  modulo esiste. La condizione lo nomina lo stesso perché la sua richiesta nomina i due moduli
  insieme; oggi si vede nella **Geometria**, dove il palco risponde finché lo strumento 'Angoli' è
  spento.
- ⚠️ **A dire quali moduli parlano del 'dove' è la tabella dei moduli**, come per il colore mirato e
  per le squadrette: scritta come un elenco di nomi accanto al palco, un modulo nuovo che spostasse
  i pixel si ritroverebbe il confronto sbagliato senza che niente lo dica.

⚠️⚠️ **E L'IMMAGINE SI PUÒ INGRANDIRE, DALLA `2.16`** (campo libero dello stesso giro: *qui capita
di lavorare sui dettagli, perciò credo sia necessario che si possa zoomare nell'immagine che si sta
editando*): pinza, panoramica e doppio tocco.
- ⚠️⚠️ **I QUATTRO GESTI DEL PALCO VIVONO IN UN RILEVATORE SOLO, E NON È UNA SCELTA DI STILE**: il
  tocco lungo del confronto e la pinza nascono dallo stesso dito che scende, quindi scritti in due
  `pointerInput` si contenderebbero l'evento. Il caso peggiore non è che un gesto non parta: è che
  il confronto si **accenda durante una pinza**, perché `waitForUpOrCancellation` risponde `null`
  sia allo scadere del tempo sia a un evento consumato da altri, e quel `null` là vale 'il dito è
  fermo da mezzo secondo'. Il precedente in casa è la strisciata del visualizzatore, che per la
  stessa ragione non ha mai funzionato fino alla `0.22`.
- ⚠️ **Ingrandisce l'ANTEPRIMA e non il file**: quello che si vede è la riduzione che l'editor
  decodifica per lavorare in fretta, quindi il tetto serve a fermarsi prima che l'immagine diventi
  un mosaico.
- ⚠️ **Si scala il rettangolo e non la tela**: il pennello porta uno shader con la sua matrice, e
  una tela scalata ingrandirebbe il conto invece dell'immagine.

⚠️⚠️ **MA DALLA `2.27` QUELLO CHE SI VEDE INGRANDITO ARRIVA DAL FILE, ED È LA SUA RISPOSTA
`pieno` A `d-dett-vedere`** (giro della `2.22`): fermandosi, la finestra inquadrata si **rilegge
a piena risoluzione** e si dipinge sopra l'anteprima. È l'unico dei sette lavori in sequenza che
non è un modulo e non porta cursori, e serve al Dettaglio più che a tutti gli altri: su una
riduzione a 1600 pixel di lato la grana del sensore è **già stata mediata**, quindi là la
riduzione del rumore si giudica su un'immagine che il rumore non ce l'ha più.
- ⚠️⚠️ **IL CONTO CHE SCEGLIE IL PEZZO È QUELLO DEL VISUALIZZATORE, TRASLOCATO E NON RISCRITTO**:
  vive in `Regions.kt` (`sharpAsk`), dove già viveva la lettura a pezzi, e la stessa funzione la
  chiamano tutte e due le schermate. Due copie di quell'aritmetica sarebbero divergenti al primo
  ritocco, ed è la seconda volta che questo repository incontra quel conto.
  - **Quello che le due geometrie non condividono** è come si arriva al rettangolo: il
    visualizzatore lo ricava dal proprio `graphicsLayer`, l'editor da `viewport`. Quindi il conto
    riceve il **risultato**, cioè dove l'immagine intera finisce sullo schermo, e non scala e
    spostamento.
  - ⚠️ **Con lui cambia un dato del visualizzatore, e si dichiara**: il guadagno si misura sui
    **pixel del bitmap di base** e non sulla sua misura su schermo. Le due coincidono al 100%, e
    dove divergono la formula nuova è quella giusta: un bitmap campionato disegnato piccolo non
    guadagna niente da una rilettura, e uno ingrandito già a riposo sì.
- ⚠️⚠️ **LA SOGLIA SI RICAVA E NON È UN NUMERO**: si legge dal file solo quando l'anteprima è
  disegnata **più larga dei propri pixel**, perché sotto quel confine un suo pixel copre meno di
  un pixel di schermo e il dettaglio che manca non si vedrebbe comunque.
- ⚠️⚠️ **IL PEZZO SI DIPINGE SOPRA L'ANTEPRIMA E NON AL SUO POSTO**, ed è quello che rende innocua
  tutta la strada: sotto c'è sempre l'immagine intera, quindi finché il pezzo non arriva non manca
  niente, e se un giorno finisse fuori posto si vedrebbe un rettangolo spostato invece di un
  buco. ⚠️ **È ancorato all'immagine e non allo schermo**: quello che si tiene sono le frazioni
  che copre, quindi resta incollato alla fotografia mentre il dito la muove.
- ⚠️⚠️ **SI CHIEDE A GESTO FINITO, E IL VIA È UN CONTATORE**: durante una pinza la vista passa per
  cento posizioni, e leggere a ognuna vorrebbe dire decodificare sessanta volte al secondo pezzi
  che nessuno ha ancora guardato. ⚠️ **E dev'essere un contatore invece di scala e spostamento**,
  che si leggono nel **disegno** e non in composizione: metterli fra le chiavi di un effetto
  ricomporrebbe il palco a ogni fotogramma di panoramica, che è il costo che quella scelta esiste
  per non pagare.
- ⚠️⚠️ **LA LENTE DEL COLORE MIRATO MOSTRA QUESTO PEZZO, DALLA `2.28`, ED È LA SUA RISPOSTA
  `pieno` A `d-lente-pieno`**: quello che si vede nel mirino è esattamente il pixel che si prende,
  perché `colourAt` campiona dallo stesso pezzo con lo stesso conto. ⚠️ **Fino alla `2.27` era il
  contrario e la ragione era buona**: la lente mostrava l'anteprima perché il colore veniva di là,
  e mostrare i pixel del file campionandone altri avrebbe fatto vedere un pixel e preso l'altro.
  A cadere non è quell'argomento, è la metà che teneva ferma: adesso si muovono insieme, e il
  perché vive in § '📈 Il modulo Curve, e il colore mirato'.
- ⚠️ **Al Dettaglio si consegna il lato dell'immagine INTERA**, non quello del pezzo: quel modulo
  ragiona in frazioni del lato, e col lato del pezzo il filtro cambierebbe forza mentre si sposta
  la panoramica.
- ⚠️⚠️ **CHE COSA IL BANCO MISURA E CHE COSA NO** (`TasselloTest`, ogni caso controprovato): il
  conto, cioè la soglia, che il pezzo sia la porzione inquadrata in pixel del file, che senza
  guadagno non si legga, e che il tetto alzi il campionamento invece di chiedere una montagna;
  dalla `2.28` anche i **due conti del pezzo**, cioè dove si posa e quale pixel cade sotto un
  punto, che si misurano con un bitmap scritto a mano e senza nessun file da aprire.
  **Non** vede il pezzo letto: quello vuole un file vero e un `BitmapRegionDecoder` che lo apra,
  e si guarda sul telefono.

⚠️⚠️ **E DALLA `2.18` SI INGRANDISCE ANCHE A UNA MANO** (nota sulla voce `zoom-corsa` del giro della
`2.17`, approvata: *mi piacerebbe anche il gesto di ingrandimento a una mano: doppio tocco con
trascinamento al secondo (giù per ingrandire)*): il secondo tocco di un doppio tocco, invece di
alzarsi, resta giù e trascina.
- ⚠️⚠️ **A DIRE QUALE DEI DUE GESTI È NON C'È NESSUN INDIZIO QUANDO IL DITO SCENDE, e da qui viene
  l'unico prezzo**: lo dice quello che il dito fa dopo, cioè se si alza o se trascina, quindi la
  corsa del doppio tocco parte **quando il dito si alza** e non quando scende. Dura quanto un
  tocco, e chi tocca due volte non sta ancora guardando l'immagine.
- ⚠️ **Si raddoppia a ogni `ZOOM_PULL` e non si cresce di un tanto al pixel**: l'ingrandimento si
  percepisce in rapporti, quindi con una crescita lineare lo stesso dito varrebbe moltissimo vicino
  a uno e quasi niente vicino al tetto. Il conto è `pulled`, e da uno al tetto ci vuole poco più di
  un terzo di schermo.
- ⚠️ **Ingrandisce senza spostare**: il punto fermo è quello toccato, per tutto il gesto. Chi vuole
  spostare ha la panoramica, e un gesto che facesse tutte e due le cose seguirebbe il dito mentre
  l'immagine cresce.
- ⚠️ **Un compagno che arriva mentre il secondo tocco è giù apre la pinza**: chi allarga due dita
  vuole quella, non un doppio tocco. La pinza è scritta una volta sola e la raggiungono due strade.
- ⚠️⚠️ **IL BANCO HA IMPOSTO COME SI INIETTA QUEL GESTO, E LA PRIMA STESURA ERA VERDE A VUOTO**: il
  primo evento che supera la soglia del gesto se lo prende `settled`, quindi un trascinamento
  spezzato in passi uguali su un palco alto sessanta pixel superava i sedici della soglia solo
  **all'ultimo**, e a valle arrivava il solo dito che si alzava. Adesso il movimento va in due
  colpi, uno che paga la soglia e uno che il gesto legge.

⚠️⚠️ **IL CONTO VIVE IN AGSL E NON ANCHE IN KOTLIN, ED È LA DECISIONE CHE REGGE TUTTO IL RESTO.**
La via comoda sarebbe scriverlo due volte: uno shader per l'anteprima, che dev'essere immediata,
e un giro sui pixel in Kotlin per il salvataggio, che lavora sul file pieno. Sono **due
implementazioni della stessa matematica**, e il giorno che una cambia l'altra mente: l'utente
vedrebbe un'anteprima e salverebbe un'altra immagine, senza che niente dia errore.
- ⚠️⚠️ **IL PREZZO È CHE SOTTO ANDROID 13 L'EDITOR COMPLETO NON C'È**, ed è la sua istruzione
  (2026-09-11): `RuntimeShader` nasce con quella versione, e là resta l'editor di casa, che non
  perde niente perché lavora sulla posa. Chi lo chiede lo chiede a `advancedEditorAvailable()`,
  uno solo, perché la stessa domanda la fanno il selettore degli editor, il modello e le
  impostazioni.
- ⚠️⚠️ **QUINDI IL SALVATAGGIO DISEGNA DAVVERO, FUORI SCHERMO** (`AdjustRender.kt`): un
  `RuntimeShader` non gira su una tela di memoria, quindi applicare lo **stesso** conto a venti
  megapixel vuol dire un `ImageReader` più un `HardwareRenderer`, e si lavora a **tessere**
  perché una texture ha un tetto che non è lo stesso su ogni telefono.
  - ⚠️⚠️ **LE TESSERE SI SOVRAPPONGONO DALLA `2.22`, E FINO ALLA `2.21` NO**: là ogni operazione
    guardava **un pixel per volta**, quindi due tessere accostate non avevano nessuna cucitura, e
    questa nota diceva che chi avesse aggiunto un'operazione che guarda i vicini avrebbe dovuto
    dare a ogni tessera un bordo da buttare via dopo. Quel giorno è arrivato col modulo Dettaglio,
    e il come vive in § '🔍 Il modulo Dettaglio, e le prime due operazioni che guardano i vicini'.
    ⚠️⚠️ **E DALLA `2.53` I MODULI COSÌ SONO DUE, QUINDI IL BORDO È IL MASSIMO DEI DUE**: il conto
    vive in `AdjustRender.bleedFor`, e il perché in § '✨ Il modulo Effetti, e i suoi cursori'.
  - ⚠️ **E prima di fidarsi si prova** (`AdjustRender.works`): se quel percorso non funziona su
    un telefono, quello che se ne ricava è un'immagine **nera**, e scritta sul file prende il
    posto della fotografia. Un quadrato di colore noto costa un millesimo di secondo e distingue
    'non ha funzionato' da 'è venuto nero davvero'.

⚠️⚠️ **IL MODULO COLORE È IL SECONDO, DALLA `2.19`, ED È IL SUO CAMPO LIBERO** (giro della
`2.18`: *vai avanti con gli altri step dell'editor completo*): **temperatura** e **tinta** rifanno
il bilanciamento del bianco, **saturazione** e **vividezza** decidono quanto i colori sono accesi,
e un interruttore porta l'immagine in **bianco e nero**.
- ⚠️⚠️ **SATURAZIONE E VIVIDEZZA NON SONO LO STESSO CURSORE PIÙ PIANO**: la saturazione muove
  tutti i colori insieme, quindi alzandola quelli già accesi arrivano al limite e si impastano; la
  vividezza pesa il suo effetto sull'**inverso** di quanto un colore è già saturo, cioè lavora sui
  colori spenti e lascia stare gli altri. È il cursore dei ritratti, dove l'incarnato è poco saturo
  e il cielo dietro no.
- ⚠️⚠️ **IL BILANCIAMENTO TIENE FERMA LA LUMINANZA, E SENZA QUELLA RIGA SAREBBE UN TERZO CURSORE
  DI ESPOSIZIONE**: il verde pesa il 71% della luminanza percepita, quindi il solo cursore della
  tinta cambierebbe di brutto quanto l'immagine sembra luminosa. I tre moltiplicatori si dividono
  per la loro luminanza, e un grigio resta della stessa chiarezza cambiando solo colore.
- ⚠️⚠️ **IL BIANCO E NERO È UN INTERRUTTORE E NON LA SATURAZIONE A -100**: è una scelta e non una
  quantità, e scritto come fondo corsa di un cursore resterebbe esposto a chiunque muova quel
  cursore. Nel conto viene **dopo**, e nella scheda spegne saturazione e vividezza, che là non
  avrebbero più niente da fare.
- ⚠️⚠️ **E DALLA `2.35` IL BIANCO E NERO PORTA IL SUO 'FILTRO', ED È SUA RICHIESTA** (2026-09-13:
  *in 'Colore', se attivo 'bianco e nero', voglio che appaia uno slider 'Filtro' che definisca la
  resa del bianco e nero in base a come sono mappati i colori nell'output*). È il filtro colorato
  che si metteva davanti all'obiettivo: schiarisce i soggetti del proprio colore e scurisce i
  complementari, quindi verso il caldo il cielo viene cupo e l'incarnato chiaro, verso il freddo il
  contrario.
- ⚠️⚠️ **È UN ASSE E NON UNA RUOTA, E LA RAGIONE È LO ZERO**: su una ruota ogni angolo è un colore
  e 'nessun filtro' non avrebbe un posto; su un asse lo zero è il centro, sono i pesi di Rec. 709,
  e un'immagine di chi aggiorna resta identica a ieri. La corsa attraversa i quattro filtri
  classici (blu, ciano, giallo-arancione, rosso), che stanno tutti su quell'asse.
- ⚠️⚠️ **I PESI SOMMANO SEMPRE UNO, E SENZA QUELLA PROPRIETÀ IL CURSORE SAREBBE UN'ESPOSIZIONE**:
  con una somma diversa un grigio cambierebbe valore, cioè a fondo corsa l'immagine si scurirebbe
  senza che nessuno abbia toccato la Luce. Il conto vive in Kotlin (`Chroma.greyMix`) e lo shader
  ne riceve il risultato: non è la seconda copia che `Adjust.kt` vieta, perché il conto è scritto
  una volta sola e ha un lettore solo, ed è per questo che il banco lo può misurare.
- ⚠️⚠️ **SI CHIAMA 'FILTRO BN' DALLA `2.37`, C'È SEMPRE, ED È SPENTO A COLORI: LA `2.36` LO
  NASCONDEVA** (punto A del suo campo libero del giro della `2.36`: *'Filtro' diventa 'Filtro BN' /
  'B/W Filter' / ecc. e va posizionato (non attivo) DOPO l'interruttore 'Bianco e nero'. Si attiva
  solo con l'interruttore ON*). La `2.36` aveva letto la sua istruzione di allora (*deve apparire
  solo quando l'interruttore è acceso*) come 'sparisce', e quello che voleva era un'altra cosa:
  **quella riga vive sotto il comando da cui dipende**, cioè si legge come la sua conseguenza.
  - ⚠️⚠️ **L'INTERRUTTORE NON È PIÙ IN FONDO, E A METTERLO DAVANTI AL FILTRO È L'IDENTITÀ DELLA
    RIGA E NON UN INDICE** (`knob === FILTER_ROW`, in `AdvancedEditorScreen.kt`): un numero di riga
    scritto là dentro direbbe il vero finché nessuno tocca l'ordine dei cursori del Colore, e il
    giorno che qualcuno ce ne infila uno l'interruttore comparirebbe in mezzo a due manopole senza
    che niente dia errore.
  - ⚠️⚠️ **CON LEI ESCE TUTTO IL MECCANISMO CHE LA NASCONDEVA, PERCHÉ NON AVEVA PIÙ CHIAMANTI**:
    il campo `Dial.hide` e la misura **col corpo pieno** di `SteadyBody`, che esisteva solo per
    pagare quella comparsa. Quindi oggi un modulo ha **un'altezza sola**, e la nota della `2.36`
    che la dava per doppia è superata.
  - ⚠️ **L'argomento della `2.35`, che era rimasto non misurato, adesso è misurato e non serve più
    a niente**: sul banco il modulo più alto è quello delle **Curve** (238 punti contro i 220 del
    Colore col 'Filtro' in scena), quindi nemmeno la `2.36` faceva ballare la scheda.
  - ⚠️ **La prova del banco misura due cose insieme**, il verso della condizione (scritta al
    contrario il cursore sarebbe acceso proprio dove non governa niente) e il **posto**, in pixel,
    perché l'interruttore non è un cursore e fra le righe non ha un numero. Controprovata due
    volte, spostando l'interruttore sotto il filtro e rovesciando la condizione.
- ⚠️⚠️ **E DALLA `2.37` LA CORSA FA POCO PIÙ DEL DOPPIO, ED È LA SUA NOTA** (giro della `2.36`,
  voce `bn-filtro` accettabile: *me l'aspettavo più ampio, ma può anche andare*). Il conto misura
  quanto: coi pesi della `2.35` un cielo azzurro scendeva di 11 punti su 100 e l'incarnato saliva
  di 8, adesso scende di 19 e sale di 13, e lo stacco fra i due passa da 29 a **42**. ⚠️ **Il verde
  non arriva a zero**, e quello che lo tiene su è misurato: coi pesi di un canale solo un cielo blu
  puro diventerebbe nero, cioè le nuvole scure perderebbero ogni disegno.
- ⚠️ **Il verde resta dov'è ai due estremi**: il filtro verde del fogliame è il terzo della
  famiglia e su un asse solo non ci sta; chi lo vuole muove la luminanza della fascia verde
  dell'HSL, che dalla `2.21` è la miscela per fascia del bianco e nero.
  - ⚠️⚠️ **E I TRE FILTRI CLASSICI NON DIVENTANO UN ELENCO A TENDINA: LA SUA CONDIZIONE NON SI PUÒ
    SODDISFARE** (`d-filtro-verde` del giro della `2.36`, senza scelta, con la nota *se si può fare
    una dropdown o qualcosa del genere con i 3 filtri classici senza occupare più spazio, OK.
    Sennò va bene così*). Un elenco a tendina è un comando in più sulla riga, quindi o si prende lo
    spazio del cursore o ne prende uno suo: il *senza occupare più spazio* è proprio quello che non
    si può avere, e la risposta è quindi la seconda metà della sua frase.

⚠️⚠️ **I PESI PER FASCIA DEL BIANCO E NERO NON SONO QUI, E DALLA `2.21` SI SA DOVE SONO**: il
  piano d'azione li metteva in questo modulo, ma sono la **stessa macchina** delle otto fasce
  dell'HSL, e là vivono (§ '🎨 Il modulo HSL, otto fasce e una macchina sola'), che è la sua
  risposta **`hsl`** a `d-bn-pesi`. Qui il grigio viene dai pesi percettivi di Rec. 709, cioè da
  come l'occhio lo vede, e dalla `2.35` il cursore 'Filtro' li sposta.
  - ⚠️⚠️ **E L'ORDINE DI USCITA È CAMBIATO CON LA `2.21`, SU SUA ISTRUZIONE** (campo libero del
    giro della `2.20`: *mi sembra più logico implementare HSL dopo il colore, va' avanti con
    quello*): l'HSL è uscito prima del Dettaglio. ⚠️ **Prevale sulla sua risposta `subito` a
    `d-dettaglio`** (giro della `2.16`), che metteva il Dettaglio al terzo posto: è un'istruzione
    più recente dello stesso utente, e le note che dànno il Dettaglio per il giro dopo il Colore
    sono superate. ⚠️⚠️ **E QUELL'ORDINE NON È PIÙ QUELLO DELLA FILA, DALLA `2.31`**: chi legge
    qui l'ordine dei gettoni guardi § '✂️ Il modulo Ritaglio, e la fila che è diventata di icone'.
    ⚠️ **Dalla `2.32` il Dettaglio è l'ultimo**, cioè dopo le Curve, ed è sua istruzione (giro
    della `2.31`: *per ora fa' un ulteriore spostamento: modulo 'Dettagli' ultimo in fondo*).
  - ⚠️⚠️ **E DALLA `2.23` NE ESISTE UNO IN PIÙ CHE NON È UN MODULO**: l'**anteprima a risoluzione
    piena quando si ingrandisce**, che è la sua risposta `pieno` a `d-dett-vedere` (giro della
    `2.22`). Non porta cursori: ridecodifica dal file la sola finestra inquadrata, perché oggi
    l'editor lavora su una riduzione e là la grana del sensore è già mediata. La scelta stessa
    dichiarava che costa una versione a parte, ed è **uscita con la `2.27`**, dopo le Curve: il
    come vive più sotto, sotto il blocco dell'ingrandimento.

⚠️⚠️ **E CON LUI ARRIVA LA FILA DEI MODULI, COL 'RESET MODULO' SUL TOCCO LUNGO** (campo libero del
giro della `2.14`, punto 2: *per ciascun modulo ci dev'essere anche un 'Reset modulo'... potrebbe
essere il tocco lungo sul nome del modulo*). Tutti e due arrivano adesso per la stessa ragione: con
un modulo solo la fila avrebbe detto dove si è, che era l'unico posto possibile, e l'azzeramento
avrebbe fatto quello che fa 'Originale', che è lì accanto.
- ⚠️ **Un gettone dice anche se il suo modulo ha toccato l'immagine**, col punto d'accento: i
  cursori di un modulo che non si sta guardando non si vedono, quindi senza quel segno un'immagine
  cambiata da un modulo chiuso non avrebbe niente che lo dica.
- ⚠️⚠️ **IL GETTONE È SCRITTO IN CASA E NON È UN `FilterChip`, E LA RAGIONE È IL TOCCO LUNGO**:
  quel pezzo di Material prende il suo `onClick` e non offre un secondo gesto, quindi il 'Reset
  modulo' andrebbe messo con un `pointerInput` nel modificatore, cioè in un **secondo nodo** che
  consuma il tocco prima che il chip lo veda. Con `combinedClickable` i gesti sono due e il
  bersaglio resta uno, che è la regola di ogni riga di questa app.
- ⚠️ **Quale modulo si sta guardando non entra nella storia dei passi**: è dove si ha lo sguardo e
  non una proprietà dell'immagine, quindi 'Annulla' non deve riportarcelo.

⚠️⚠️ **I CONTI SI FANNO IN LUCE LINEARE, E L'ORDINE DELLE OPERAZIONI È LA SPECIFICA**: il
bilanciamento del bianco, poi l'esposizione, poi ombre e luci, poi i punti di bianco e di nero, poi
il contrasto, e per ultimo quanto sono accesi i colori. Un valore sRGB
non è la quantità di luce ma quella quantità passata per una curva, quindi sommare o moltiplicare
là dentro dà i risultati sporchi che si vedono negli editor fatti male: un contrasto che vira,
un'esposizione che spegne i colori. Il perché di ogni passaggio, e il perno del contrasto, vivono
in `Adjust.kt`.
- ⚠️ **I punti vengono prima del contrasto** perché dichiarano dove finisce l'immagine, e la curva
  a S lavora dentro l'intervallo che quei due estremi definiscono. Al contrario, taglierebbero i
  toni che la curva ha appena creato.

⚠️⚠️ **E DALLA `2.18` L'ESPOSIZIONE PIEGA LE ALTE LUCI INVECE DI TAGLIARLE, ED È IL SUO RISCONTRO**
(campo libero del giro della `2.17`: *l'esposizione è troppo brusca sulle tonalità chiare:
aumentandola le parti chiare diventano bianche troppo velocemente*). La causa era un taglio: la
luce si moltiplica, e quello che usciva dalla scala veniva schiacciato sul bianco, quindi sopra una
certa esposizione tutti i toni chiari diventavano **lo stesso** bianco. La misura è netta: a +1,5
stop, dei 77 livelli sopra il 70% di scala ne restava **uno**, e con la piega ne restano 17.
- ⚠️⚠️ **LA SOGLIA SI RICAVA DAL GUADAGNO E NON È UN NUMERO, ed è questo che tiene la funzione
  neutra a riposo**: la piega comincia al tono che, moltiplicato per il guadagno, arriva esattamente
  al bianco. A guadagno uno quella soglia vale uno, quindi la curva è l'**identità** su tutto
  l'intervallo e un'immagine non toccata esce identica (misurato: scarto nullo su tutti e 256 i
  livelli). Con una soglia scritta a mano, un'immagine a riposo perderebbe i suoi chiari senza che
  nessuno abbia mosso niente.
- ⚠️ **I mezzi toni tengono il guadagno pieno**: sotto la soglia non si tocca niente, quindi a +1
  stop un grigio medio raddoppia come prima. La piega lavora solo dove il taglio bruciava, e
  l'esposizione resta un'esposizione.
- ⚠️⚠️ **IL COSTO È DICHIARATO E LA VARIANTE CHE LO EVITAVA È STATA SCARTATA**: un bianco pieno non
  resta esattamente pieno (a +1 stop arriva a 252 su 255), perché la curva tende al bianco senza
  raggiungerlo. È uniforme su tutta l'area, quindi non ha un bordo da cui si veda. Normalizzare la
  coda lo terrebbe a 255, ma porterebbe la pendenza alla piega **sopra** uno (1,12 a un quarto di
  stop), cioè aprirebbe un tratto in cui il contrasto cresce invece di comprimersi, e
  un'inversione di pendenza si vede come un gradino.
- ⚠️⚠️ **E DALLA `2.19` COMINCIA PRIMA, PERCHÉ L'HA GUARDATA** (nota sulla voce `luce-piega`,
  approvata: *ancora un pelo più morbida*): la soglia non è più `1/g` ma `1/g` elevato a
  `SHOULDER_SOFT`, cioè **1,5**. ⚠️ **L'esponente non tocca la neutralità a riposo, ed è la ragione
  per cui si agisce lì**: a guadagno uno la soglia vale `1` elevato a qualunque cosa, quindi la
  curva resta l'identità (rimisurato: scarto nullo su tutti e 256 i livelli), mentre una soglia
  abbassata con una sottrazione avrebbe perso quella proprietà.
  - **Il numero è misurato e non tentato**: a +1,5 stop i livelli distinti sopra il 70% di scala
    passano da **17 a 23**, e il grigio medio a +1 stop non si muove di un livello. Oltre 1,5 il
    guadagno si ferma (24 a esponente 2) e i mezzi toni alti cominciano a cedere, quindi quello è
    il punto in cui l'immagine guadagna senza che l'esposizione smetta di lavorare sui mezzi toni.
- ⚠️ **Si applica per CANALE**: un colore acceso che satura un canale solo virava, perché quel
  canale si fermava mentre gli altri salivano. Piegandoli tutti e tre con la stessa curva, il
  colore si desatura dolcemente verso i chiari, che è quello che fa una pellicola.
- ⚠️⚠️ **IL BANCO NON LA PUÒ MISURARE, e va detto**: il conto vive in AGSL e su una tela di memoria
  non gira, quindi `ContoTest` dice che il programma **compila** e non che la curva è giusta. I
  numeri qui sopra vengono da un modello di sessione, scritto e buttato, che è la stessa strada dei
  versi dei cursori.

⚠️⚠️ **E DALLA `2.59` IL CONTRASTO LAVORA SUI MEZZI TONI INVECE CHE SUGLI ESTREMI, ED È IL SUO
RISCONTRO** (campo libero del giro della `2.55`: *andrebbe un po' migliorato pure il contrasto ...
mi è sembrato un po' troppo vecchia scuola, e fa diventare tutto un po' troppo grigio in
negativo*). Le due metà della sua frase erano **due difetti distinti con la stessa radice**, cioè
una curva che spendeva il proprio effetto dove non doveva: i due estremi.
- ⚠️⚠️ **IN SU C'ERA UNA `smoothstep` MESCOLATA ALLA DIAGONALE, E QUELLA AGLI ESTREMI HA PENDENZA
  ZERO**: a fondo corsa, dei 26 livelli sotto il 10% di scala ne restavano **8**, e altrettanti in
  cima. Cioè il contrasto sembrava forte perché **mangiava** il dettaglio nelle ombre e nei chiari,
  mentre al perno la pendenza arrivava appena a 1,5. È la definizione operativa di 'vecchia
  scuola', ed è misurata: oggi quei livelli sono **21** e **22**, e al perno la pendenza è **2**.
- ⚠️⚠️ **IN GIÙ C'ERA UNA COMPRESSIONE LINEARE VERSO IL PERNO, CHE SPOSTA ANCHE IL NERO E IL
  BIANCO**: a -100 il nero usciva a 0,30 (livello **76**) e il bianco a 0,70 (livello **178**),
  quindi l'immagine viveva in 102 livelli su 255. Ecco il grigio, e non era un'impressione: era
  aritmetica. Adesso il nero resta **0**, il bianco resta **1**, e a comprimersi sono i soli mezzi
  toni.
- **La forma nuova è la diagonale più una gobba dispari**, `(t - 0.5)` per `(4t(1-t))` al cubo:
  quel secondo fattore vale uno al perno e zero ai due estremi **con derivata nulla**, quindi la
  curva arriva agli estremi tangente alla diagonale e là non tocca niente. Lo stesso pezzo serve
  tutti e due i versi, col segno del cursore: in su allontana dal perno, in giù ci avvicina.
- ⚠️ **Al cubo e non al quadrato, ed è misurato**: il cubo concentra la gobba sui mezzi toni, e a
  parità di pendenza al perno la pendenza minima passa da **0,20 a 0,35**, cioè i quarti di tono si
  comprimono meno. La monotonia regge fino a `CONTRAST_RISE` **1,53**, e il fondo corsa è 1.
- ⚠️⚠️ **CHI USAVA IL CONTRASTO A FONDO CORSA LO RITROVA A METÀ, E VA DETTO**: a `k` 0,5 la
  pendenza al perno vale 1,5, cioè esattamente quella che prima si otteneva a 1. Quindi un valore
  già scelto (in uno stile salvato, o nei sei stili di casa che ne portano uno) adesso rende di
  più, ed è quello che ha chiesto.
- ⚠️⚠️ **E IL CONTO DI 'Auto' HA DOVUTO CAMBIARE, O DAVA IL DOPPIO**: quel comando ricava il
  cursore dalla **pendenza al perno**, che era `1 + 0,5k` e adesso è `1 + k`, quindi la formula
  perde il suo `2`. ⚠️ **Il valore che scrive si dimezza e lo stacco resta lo stesso**: la curva è
  il doppio più ripida.
- ⚠️⚠️ **I TRE NUMERI CHE `Auto` RICOPIA DALLO SHADER ADESSO HANNO UN PRESIDIO, E PRIMA NO**:
  vivono dentro `LOOK_AGSL`, che per il compilatore è un testo qualunque, quindi cambiarne uno di
  là e lasciare la copia in Kotlin non dà **nessun** errore. Il banco adesso quella stringa la
  legge davvero e confronta. ⚠️ **La nota di prima diceva che da Kotlin non si potevano leggere, ed
  era falsa**: è una stringa, e leggerla costa una riga.
- ⚠️ **Il banco non vede la curva**, come per la piega: i numeri di questo blocco vengono da un
  modello di sessione, scritto e buttato, e quello che il banco misura è la costante condivisa. Che
  il contrasto sia più moderno e che il negativo non ingrigisca si guarda sul telefono.

⚠️⚠️ **LA PILA È DI VALORI E NON DI GESTI, e un passo nasce quando il dito LASCIA il cursore**:
dentro un trascinamento un cursore passa per cento valori, e una pila che li prendesse tutti
renderebbe 'Annulla' inutilizzabile. Quello che si disfa è un **gesto compiuto**, che è la cosa
che l'utente ricorda di aver fatto.
- ⚠️⚠️ **E IL PASSO SE LO VA A PRENDERE DALLO STATO VIVO, PERCHÉ IL VALORE CATTURATO ERA UN
  DIFETTO**: la prima stesura consegnava il parametro del cursore a `onValueChangeFinished`, e
  Compose chiama quella e `onValueChange` **senza per forza ricomporre in mezzo**, quindi il
  passo portava il valore di prima. L'immagine cambiava e 'Annulla' restava spento. ⚠️ **Non
  sarebbe arrivato sempre**, ed è la ragione per cui fa più paura: con un dito vero fra le due
  chiamate c'è quasi sempre un fotogramma, quindi si sarebbe visto una volta su dieci.
- ⚠️ **La storia è una lista con un indice e non due pile**: con due pile ogni passo nuovo deve
  ricordarsi di svuotare la seconda, e chi se ne dimentica lascia un 'Ripristina' che riporta a
  una strada abbandonata.

⚠️⚠️ **'SENZA PERDITA' È UNA PROPRIETÀ DEL MODELLO E NON UNA RIGA DEL SALVATAGGIO** (`Look.lossless`),
ed è la clausola dell'utente (*quelle che non prevedono la riscrittura del file pixel per pixel
devono essere lossless*): finché c'è solo la posa il file si gira cambiando un tag EXIF, come
l'editor di casa fa dalla `1.03`; appena entra un valore di Luce i pixel vanno riscritti e non
c'è modo di evitarlo. ⚠️ **E dalla `2.31` la posa è un modulo di questo editor**, quindi quella
distinzione vive qui come là: un ritaglio riscrive, una rotazione no. ⚠️⚠️ **E LA GEOMETRIA È ENTRATA CON LA `2.29`, con la risposta che era già
scritta qui**: quel modulo **ricampiona** per definizione, quindi un suo cursore mosso toglie il
senza perdita come uno di Luce. Chi legge la nota vecchia, che lo dava come un caso futuro, sappia
che il caso è arrivato e la risposta non è cambiata.

⚠️ **La qualità di scrittura è a tre ed è una voce delle impostazioni**, non una domanda a ogni
salvataggio: salvare è un gesto che si fa di fretta, ed è la stessa lettura che ha avuto
'Scarica'. ⚠️ **Le prime due sono un JPEG e la terza un altro formato**: 'Senza perdita' scrive
un PNG **accanto** invece di sovrascrivere, perché il formato cambia.

⚠️⚠️ **E DALLA `2.58` IL TOCCO LUNGO SU 'SALVA' SCRIVE UN FILE NUOVO ACCANTO ALL'ORIGINALE, ED È
SUA RICHIESTA** (campo libero del giro della `2.55`: *tocco lungo su 'Salva' (in alto a destra)
nell'editor: salva un nuovo file accanto all'originale*). Fino alla `2.57` l'editor riscriveva
l'immagine dov'è, e chi voleva tenere anche il prima doveva uscire, duplicare il file e rientrare.
- ⚠️⚠️ **NON APRE UNA STRADA NUOVA: PERCORRE QUELLA CHE C'ERA GIÀ**, cioè quella di un formato che
  non si sa riscrivere (un HEIC, un AVIF) e quella di 'Senza perdita'. Quindi il nome lo sceglie
  `FileTree.freeName`, che è la funzione di ogni copia dell'app, e la notifica dice 'Copia salvata'
  senza un ramo in più: a deciderla è già il confronto fra il file scritto e quello di partenza.
- ⚠️ **La copia di sicurezza si salta da sé**, e non con una condizione in più: quella si chiede
  quando il bersaglio **è** la sorgente, e col file accanto non lo è mai. È la stessa nota che
  `ImageEdit.save` porta sulla via della copia.
- ⚠️⚠️ **IL TASTO NON È PIÙ UN `TextButton`, E LA RAGIONE È LA STESSA DEI GETTONI DEI MODULI**: quel
  pezzo di Material prende il suo `onClick` e non offre un secondo gesto, quindi il tocco lungo
  andrebbe messo con un `pointerInput` nel modificatore, cioè in un **secondo nodo** che consuma il
  tocco prima che il tasto lo veda. Con `combinedClickable` i gesti sono due e il bersaglio resta
  uno. ⚠️ **L'etichetta del gesto si dichiara**, o resta una scorciatoia che esiste solo per chi ha
  letto queste righe: è quello che un lettore di schermo annuncia.
- ⚠️ **A immagine intonsa resta spento**, come prima: senza niente da applicare non c'è niente da
  salvare, né sopra né accanto.

⚠️⚠️ **E NELLA `2.14` QUEL PROGRAMMA NON COMPILAVA AFFATTO, SU NESSUN TELEFONO** (riscontro del
giro, voce `luce-cursori` non approvata: *nessuno slider ha avuto effetto sull'immagine*, e
salvando *'Questo telefono non è riuscito ad applicare le modifiche'*). La causa è una parola:
**`out` è un qualificatore di parametro del linguaggio**, quindi `half3 out = ...` è un errore di
sintassi e il compilatore rifiuta il programma intero, dalla prima riga. Quattro voci su cinque
sono tornate indietro per quella.
- ⚠️⚠️ **A NASCONDERLA È STATA LA RETE CHE DOVEVA PROTEGGERE**: `lookShader` avvolge la
  compilazione in un `runCatching` perché un programma rifiutato non faccia cadere l'app mentre
  disegna un fotogramma. Quella riga serve e resta, ma **trasforma un errore di sintassi in un
  `null`**, cioè in 'questo telefono non sa farlo': la diagnosi arrivava rovesciata, e il testo
  che l'utente leggeva accusava il suo telefono.
- ⚠️⚠️ **UNA STRINGA DI PROGRAMMA NON LA GUARDA NESSUN COMPILATORE, ed è la stessa famiglia del
  `pathData` che `aapt2` non legge**: Kotlin compila `LOOK_AGSL` come compilerebbe una poesia.
  Il presidio è `ContoTest`, che il programma lo **compila davvero**, senza rete, così un errore
  di sintassi arriva col messaggio, la riga e la colonna.
- ⚠️⚠️ **MA IL BANCO NON PUÒ ESEGUIRLO, ed è misurato**: disegnare con un `RuntimeShader` su una
  tela di memoria fallisce con *Software rendering doesn't support RuntimeShader*, perché
  Robolectric disegna col processore e Android quella strada la vieta. Quindi il banco risponde
  *questo programma è valido* e non *questo conto è giusto*, e la differenza fra le due cose si
  guarda sul telefono.
- ⚠️ **I versi dei cinque cursori si verificano con un modello di SESSIONE**, scritto e buttato:
  riscrivere il conto in Kotlin dentro il repository sarebbe la seconda copia che questa sezione
  esiste per non avere, mentre un conto fatto una volta in una sessione è una misura come le
  altre.

⚠️⚠️ **E LA STESSA PASSATA HA TROVATO DUE DIFETTI DI MERITO, che nessuno aveva potuto vedere
perché il programma non è mai partito**:
- **Il contrasto negativo andava dalla parte sbagliata**: il ramo `k < 0` portava un tono da 0,6 a
  0,72, cioè **allontanava** dal perno, quindi il cursore alzava il contrasto in tutti e due i
  versi. La `2.15` lo ha fatto comprimere verso il perno. ⚠️⚠️ **MA QUELLA COMPRESSIONE ERA IL
  GRIGIO CHE LUI HA SEGNALATO QUARANTA VERSIONI DOPO, e con la `2.59` non c'è più**: teneva il 40%
  della distanza dal perno **a ogni tono**, cioè spostava anche il nero e il bianco. Chi legge qui
  quel 40% sappia che è il numero di una curva che non esiste più.
- **Le maschere di ombre e luci guardavano la luce e non l'occhio**: in luce lineare un grigio
  medio vale 0,22, quindi la maschera delle ombre gli dava 0,61 e il cursore sollevava i mezzi
  toni come fa la luminosità. Adesso la maschera si costruisce sul valore percettivo, che è la
  sola cosa che distingue quei due cursori dal terzo.

⚠️⚠️ **IL TASTO 'AUTO' IMITA 'COLORE AUTOMATICO' DI PHOTOSHOP, DALLA `2.32`, ED È SUA RICHIESTA**
(campo libero del giro della `2.31`: *aggiungi un tasto 'Auto' che imita 'Colore automatico' di
Photoshop. Può stare a sinistra di 'Annulla', solo icona; dev'essere annullabile*). Quel comando fa
due cose e non una: porta i due estremi dell'intervallo tonale al nero e al bianco, e toglie la
dominante. Qui diventano **quattro cursori**, i due punti della Luce e i due del bilanciamento, e
nessun altro si muove.
- ⚠️⚠️ **SCRIVE NEI CURSORI E NON DIPINGE NIENTE, ED È QUELLO CHE LO RENDE ANNULLABILE**: quello
  che ne esce è un `Look` come un altro, quindi entra nella storia dei passi e 'Annulla' lo disfa
  senza una strada sua. ⚠️ **E si vede**: dopo un 'Auto' si continua a mano da dove il conto è
  arrivato.
- ⚠️ **I due conti sono le INVERSE delle formule dello shader** e non due tarature: per portare il
  percentile basso a zero serve esattamente `-lo / POINT_SHIFT`, e la dominante si toglie
  invertendo `balance`, cioè con due rapporti. Il perché di ognuno, il taglio dello 0,5% e la
  guardia dell'immagine piatta vivono in `AutoLook.kt`.
- ⚠️ **Legge l'anteprima e la campiona**: percentili e medie su duecentomila punti valgono quanto
  su due milioni, e leggere il file pieno costerebbe una pausa per una cifra che non si muove di
  un livello.
- ⚠️ **Il glifo è la bacchetta di Material ammorbidita**, come i sette dei moduli e quello del
  mirato: il criterio e la misura vivono in § '🖌️ Come entra un disegno'. ⚠️⚠️ **E DALLA `2.34`
  NON È PIÙ PROVVISORIO**, che è la sua risposta `resta` a `d-auto-glifo` (giro della `2.32`: *va
  bene quello di Material*): la domanda esisteva perché nella stessa situazione, con i due glifi
  della `1.80`, aveva risposto mandando i suoi.

⚠️⚠️ **E DALLA `2.34` 'AUTO' SISTEMA ANCHE LA LUCE MEDIA, CHE È LA SUA RISPOSTA `piu` A
`d-auto-quanto`** (giro della `2.32`: *che tocchi anche esposizione e contrasto*, cioè *un colpo
solo che sistema anche la luce media, in un tasto solo*). Photoshop quelle due le tiene in comandi
separati ('Tono automatico' e 'Contrasto automatico'); la domanda gli chiedeva se accorparle, e i
cursori mossi passano da quattro a **sei**.
- ⚠️⚠️ **I SEI SI CALCOLANO NELL'ORDINE IN CUI LA CATENA LI APPLICA, E SENZA QUELLA CURA I DUE
  NUOVI ROMPONO I DUE VECCHI**: l'esposizione viene **prima** dei punti, quindi i percentili
  misurati sull'immagine com'è non sono quelli che i punti troveranno. Prima il guadagno dalla
  mediana, poi i due estremi **già esposti**, e per ultimo il contrasto sulla distribuzione che ne
  esce. A presidiarlo è la prova, che misura una fotografia scura con qualche alta luce: là dopo
  due stop il punto di bianco non ha più niente da fare, mentre col conto sbagliato spenderebbe
  mezza corsa.
- ⚠️ **Il bersaglio dell'esposizione è il perno del contrasto**, cioè `0,5`: una mediana che cade
  lì riceve una curva a S simmetrica, mentre spostata darebbe una S che allarga da una parte e
  schiaccia dall'altra. Il numero non è una taratura, è quello che la formula del cursore accanto
  usa come centro. ⚠️ **E si guarda la mediana e non la media**, per la stessa ragione per cui i
  punti tagliano mezzo per cento: un cielo bianco tira la media di parecchi livelli.
- ⚠️⚠️ **IL CONTRASTO VA SOLO IN SU, COME QUELLO DI PHOTOSHOP**: un'immagine più dispersa del
  bersaglio non ha un difetto da correggere, ha un carattere, e spianarla vorrebbe dire che 'Auto'
  toglie qualcosa a chi lo tocca su una fotografia già buona. Il legame fra il cursore e la
  dispersione è la **pendenza al perno**, che dalla `2.59` vale `1 + k` (prima `1 + 0,5k`, perché
  la curva di allora arrivava a 1,5 a fondo corsa), quindi è un conto chiuso e non una taratura;
  che sia del primo ordine è dichiarato, e il verso in cui sbaglia è quello timido.

⚠️⚠️ **I TRE COMANDI DELLA STORIA SONO ICONE DALLA `2.15`, ED È IL SUO RISCONTRO** (voce
`luce-storia`: *'Annulla' e 'Ripristina' devono essere icone, non testo*). I glifi sono quelli
che l'**editor di casa** usa già per gli stessi tre comandi, cioè i suoi: disegnarne altri
vorrebbe dire due segni per lo stesso gesto a un tocco di distanza, visto che dalla stessa
immagine si entra nell'uno o nell'altro editor. ⚠️ **Anche il terzo, che lui non ha nominato**:
due icone accanto a una scritta sarebbero una fila che si legge in due modi.

⚠️⚠️ **E DALLA `2.52` QUELLA BARRA SI SPECCHIA COL LATO DEL FAB, CON UN'ECCEZIONE SUA** (sua
richiesta del 2026-09-14, con una schermata: *l'ordine delle icone della barra bassa deve essere speculare
quando il FAB è a sinistra, con la sola eccezione di 'Annulla'/'Ripristina', che devono essere
sempre il primo a sinistra del secondo*). Il lato lo dà la stessa preferenza che governa il FAB,
cioè quella che già decide da che parte vanno le pastiglie dell'intestazione e dove cade il menu
ancorato.
- ⚠️⚠️ **L'ECCEZIONE NON È UN CAPRICCIO: QUEI DUE COMANDI SONO UN VERSO DEL TEMPO**, e uno
  specchio lo rovescerebbe, cioè metterebbe 'Ripristina' sotto il dito che cerca 'Annulla'. Le
  altre icone un ordine che voglia dire qualcosa non ce l'hanno, e per questo si specchiano.
- ⚠️ **Con loro si specchia anche 'Salva stile'**, che di quella barra fa parte: col FAB a
  destra è a sinistra, col FAB a sinistra passa dall'altro lato. ⚠️ **Era una mia lettura e
  adesso è una sua parola** (2026-09-18: *la tua lettura è corretta*), quindi non si richiede
  più.
- ⚠️⚠️ **LO SCAMBIO DELLA COPPIA VIVE IN UNA FUNZIONE PURA, `barOrder`, E NON IN UNA RIGA DENTRO
  LA FILA**: così il banco lo misura chiamandola, mentre uno specchio scritto a mano nella Row
  si proverebbe solo contando i pixel di cinque icone. ⚠️ **E lo spazio elastico in mezzo fa
  tutto l'allineamento**: senza il comando che salva resta lui solo, quindi i comandi si trovano
  comunque appoggiati al lato giusto, e non serve una seconda condizione.
- ⚠️⚠️ **E IL TESTO DELLA VOCE DELLE IMPOSTAZIONI È RISCRITTO DA LUI, ALLA LETTERA**:
  `settings_hand` diventa **'Posizione preferita dei pulsanti'** e `settings_hand_desc` *Il lato
  preferito per i tasti principali nell'uso a una mano per destri e mancini. Definisce il
  posizionamento del tasto d'azione fluttuante ('FAB'), di alcuni pulsanti di navigazione e di
  parti di UI dell'editor.* ⚠️ **La ragione è che quella voce governa più del FAB**, e il titolo
  di prima ('Posizione dei tasti flottanti') ne nominava uno solo. ⚠️ **Le chiavi non si
  toccano**, e le altre ventisette lingue sono entrate con lui, nella `2.52`, insieme al
  `t-stili-pagina` del giro della `2.50`.

⚠️ **Che cosa il banco misura e che cosa no**: `LuceTest` guarda il modello (la soglia del
riposo, il guadagno in stop, il senza perdita) e la **storia dei passi** montando la schermata
vera, coi comandi che camminano avanti e indietro; dalla `2.59` `SviluppoTest` guarda anche che i
**tre numeri che `Auto` ricopia dallo shader** combacino con la stringa del programma, che è il solo
presidio possibile di una copia fra due linguaggi; dalla `2.58` guarda i **due gesti della `2.58`**, cioè che il confronto tenga la posa fuori dai moduli che la governano (a pixel,
nella Geometria contro la Luce), che `Look.place` tenga i quattro campi del 'dove' e butti i sei del
colore, che il tocco lungo su 'Salva' chieda un file nuovo e il tocco normale no, e che il bersaglio
di quel file sia un nome libero accanto all'originale; tutti e quattro controprovati rimettendo il
difetto uno per uno; dalla `2.16` guarda anche il **doppio tocco**
sul nome e sulla barra, e l'ingrandimento dell'immagine, che misura a **pixel**; dalla `2.18` il
conto dell'**ingrandimento a una mano** (il verso, il raddoppio, il tetto) e il gesto sul palco,
anche lui a pixel; `ContoTest` guarda che il programma dello shader compili e che `lookShader` lo
consegni. **Non** vedono i pixel che escono dal conto, né il confronto col prima, né la pinza a
due dita: quelli si guardano sul telefono.
- ⚠️⚠️ **DALLA `2.19` C'È ANCHE `SviluppoTest`, E NON SI CHIAMA `ColoreTest` PERCHÉ QUEL NOME È
  GIÀ PRESO**: là vive il colore di una **cartella**, cioè la tinta della sua intestazione, e due
  prove omonime in un repository che parla di tutte e due in ogni giro sono due cose che qualcuno
  scambia. Misura quello che il secondo modulo porta di rompibile in silenzio: che il gettone
  cambi i cursori in scena, che il 'Reset modulo' azzeri **solo** il suo, che il bianco e nero
  spenga saturazione e vividezza, e che un valore di Colore tolga il senza perdita. Controprovata
  rimettendo i tre difetti, uno per prova.
  - ⚠️ **Dalla `2.21` copre anche il terzo modulo**, e per la stessa ragione: là a rompersi in
    silenzio sono la fascia scelta (un cursore che scrive nel colore sbagliato) e i raggi che
    devono combaciare. L'elenco per esteso vive in § '🎨 Il modulo HSL, otto fasce e una macchina
    sola'; anche questi cinque casi sono controprovati rimettendo il difetto.
  - ⚠️ **E dalla `2.22` il quarto**, dove le cose che si rompono in silenzio sono un modulo che si
    dichiara da riscrivere quando non lo è, due cursori che restano accesi senza governare niente,
    e gli indici delle tessere del salvataggio. L'elenco vive in § '🔍 Il modulo Dettaglio, e le
    prime due operazioni che guardano i vicini', e anche questi cinque casi sono controprovati.
- ⚠️⚠️ **DUE DELLE TRE PROVE NUOVE SONO NATE VERDI PER CASO, E LA CONTROPROVA LO HA DETTO.** Quella
  del doppio tocco sulla barra toccava il **centro**, dove la barra vale già zero: col passo di
  troppo rimesso a mano restava verde, perché il salto del primo tocco portava proprio dove il
  doppio tocco voleva arrivare. Adesso tocca a un quarto, e il passo intermedio si vede. È il
  caso generale di § '🧪 Quando si scrive una prova, e quando no': una prova che non si vede
  fallire col difetto rimesso non misura niente.

⚠️⚠️ **UN CONFRONTO ACCESO NON PUÒ PIÙ RESTARE ACCESO, DALLA `2.17`, E QUESTA È L'UNICA COSA
MISURATA DI QUEL GIRO** (riscontro del giro della `2.16`, voce `luce-sei` approvata con una nota:
*C'è uno strano collegamento tra i diversi cursori ... quasi sempre se modifico il contrasto la
luminosità si azzera; se faccio un doppio tocco su un nome di slider se ne resetta anche un
altro*). Il confronto di un cursore si accende con `onHold(true)` e si spegne con `onHold(false)`,
e in mezzo c'è un'attesa: un rilevatore di gesti viene **annullato** quando il suo `pointerInput`
cambia chiave (qui basta un salvataggio che parte, che spegne i cursori), e un'attesa annullata
non torna alla riga dopo. Adesso lo spegnimento vive in un `finally`, di là e sul palco.
- ⚠️ **Perché è il candidato**: col confronto acceso l'immagine mostra un cursore in meno, e
  nessun numero lo dice. Chi guarda vede un cursore che 'si è azzerato' senza che il suo valore
  sia cambiato, ed è la descrizione che lui ha dato.
- ⚠️ **La prova è `LuceTest`, caso 14**, e monta il modificatore da solo: il confronto si vede dai
  pixel, e sul banco lo shader non gira. Controprovata togliendo il `finally`.

⚠️⚠️ **MA LA CAUSA NON È ACCERTATA, E SI SCRIVE COSÌ INVECE DI INVENTARLA**: quel difetto **non si
riproduce sul banco**, e le quattro ipotesi misurate in sessione sono cadute una per una. Chi ci
torna sopra non le rifaccia:
1. **Una lambda catturata dentro un `pointerInput` invecchia**: no. Misurato con una sonda, sia
   catturando uno stato delegato sia catturando un parametro: il gesto vede sempre il valore
   fresco.
2. **Un trascinamento vero riporta indietro gli altri cursori**: no. Misurato iniettando gli
   eventi uno per uno con una ricomposizione in mezzo, che è la condizione del telefono.
3. **Il rilascio di un tocco lungo si perde nel caso normale**: no, arriva.
4. **Due tocchi ravvicinati su due cursori diversi si confondono**: no.

⚠️ **Quello che resta sono due DIFESE, dichiarate come tali**: un cambiamento da applicare al
posto di un'immagine già fatta (§ `Dial.set`), così nessun gesto porta più con sé un `Look` che
possa invecchiare; e il confronto tenuto come **trasformazione** invece che come fotografia, così
un confronto rimasto acceso non congela quello che si vede. Nessuna delle due è la cura misurata
di quel difetto, e restano perché il sintomo è sparito senza che si sappia quale delle tre righe
lo abbia tolto.

⚠️⚠️ **E IL SINTOMO NON C'È PIÙ, CHE È LA SUA RISPOSTA `via` A `d-legame-cosa`** (giro della
`2.17`: *non succede più*). La domanda serviva a distinguere le due cause (si azzera il numero
del cursore, o soltanto l'immagine?), e la risposta la chiude **senza** distinguerle: quello che
si sa è che il difetto è uscito con la `2.17`, non quale delle tre righe lo abbia tolto.
- ⚠️ **Le quattro ipotesi qui sopra non decadono**: restano misure vere, e chi ritrovasse quel
  sintomo riparte da lì invece di rifarle.

⚠️⚠️ **E COL SECONDO MODULO IL SINTOMO È TORNATO PIÙ GRANDE: DALLA `2.20` UN GESTO SCRIVE LA RIGA
CHE TOCCA E NON UN CURSORE CHE SI PORTA DENTRO** (campo libero del giro della `2.19`: *il mio
tocco, mentre provo a spostare la tinta o la saturazione, sposta invece il contrasto che è
nell'altro modulo; lo stesso succede altrove, c'è qualcosa di mescolato*). La coppia che ha
nominato è la **stessa riga di due moduli**: la tinta è la seconda del Colore e il contrasto la
seconda della Luce.
- ⚠️⚠️ **LA CAUSA NON È ACCERTATA NEMMENO QUESTA VOLTA, E IL BANCO NON LA RIPRODUCE**: il caso
  nuovo di `SviluppoTest` tocca **col dito**, e resta verde anche togliendo la correzione; una
  spia messa dentro il gesto dice che a rispondere è il cursore toccato, col tocco secco come con
  un trascinamento vero, e la Luce resta a zero. Quindi quello che si è fatto è **togliere la
  possibilità**, non curare una causa, e la voce di collaudo lo dice a lui.
- **Il meccanismo**: la lambda di un cursore non porta più il proprio `Dial`, porta il **numero di
  riga**, e chi scrive risolve quella riga nel modulo in scena in quell'istante (`dialAt`). Così
  il cursore numero N scrive sempre il numero N di quello che si sta guardando, che è quello che
  il dito ha sotto.
- ⚠️ **Il `key` è la seconda metà**: senza, Compose riusa i composable di una lista **per
  posizione**, quindi cambiando modulo i nodi dei cursori passano di mano con tutto quello che un
  nodo tiene. La chiave è il `Dial`, che è un oggetto della tabella dei moduli, quindi stabile per
  tutta la vita del processo.
- ⚠️ **Somiglia alla correzione della `2.17` e non è la stessa cosa**: là si era tolta la cattura
  di un `Look`, qui quella di un oggetto. Tutte e due tolgono una cattura, e nessuna delle due
  nasce da un difetto riprodotto sul banco.
- ⚠️⚠️ **E IL SINTOMO NON C'È PIÙ, CHE È LA SUA NOTA SU `d-mescola-gesto`** (giro della `2.20`:
  **`trascina`**, con la coda *comunque è risolto*). Quindi il difetto è chiuso **senza** che la
  sua causa sia stata accertata, come quello della `2.17`: la risposta dice con quale gesto lo
  vedeva, non perché succedesse. ⚠️ **La domanda gemella `d-mescola-cosa` è rimasta senza
  risposta**, e non si ripropone: serviva a distinguere due cause su un sintomo che non c'è più.

⚠️⚠️ **E IL DOPPIO TOCCO SULL'IMMAGINE CI ARRIVA CON UNA CORSA, DALLA `2.17`** (stesso giro, voce
`luce-zoom` approvata con una nota: *mi piacerebbe di più se al doppio tocco l'immagine passasse
da uno zoom all'altro con un'animazione anziché con uno stacco netto*).
- ⚠️ **A muoversi è un progresso solo**, e da lui si ricavano ingrandimento e spostamento: due
  corse separate sarebbero due cose da tenere allineate, e quella che finisse prima farebbe
  scivolare l'immagine a ingrandimento fermo.
- ⚠️ **Un dito che scende la ferma dove è arrivata**: chi tocca mentre l'immagine si ingrandisce
  vuole prendere il comando, non aspettare il suo turno.
- ⚠️⚠️ **E LO SPOSTAMENTO SI RIPORTA NEI BORDI DOVE SI DISEGNA, non solo nel gesto**: il limite
  dipende dall'ingrandimento, quindi un valore buono per l'arrivo è **troppo** a metà corsa, e per
  qualche fotogramma si vedrebbe una striscia di fondo da un lato. La funzione è pura e
  applicarla due volte non cambia niente.
- ⚠️ **La prova guarda a metà corsa col clock fermo**: con l'avanzamento automatico l'animazione
  finisce dentro `waitForIdle`, e la misura direbbe solo dove si arriva. La durata e la curva
  invece si guardano sul telefono.

## 🎨 Il modulo HSL, otto fasce e una macchina sola

⚠️⚠️ **È IL TERZO MODULO DELL'EDITOR COMPLETO, DALLA `2.21`, ED È IL SUO CAMPO LIBERO** (giro della
`2.20`: *mi sembra più logico implementare HSL dopo il colore, va' avanti con quello*). Dove il
modulo Colore parla a tutta l'immagine, questo parla a **un colore per volta**: la saturazione del
Colore accende tutto insieme, qui si accende il cielo lasciando stare l'incarnato. Le fasce sono
otto, come in Lightroom, e ognuna porta i tre cursori di quel pannello: tonalità, saturazione,
luminanza.

⚠️⚠️ **E QUI DENTRO VIVONO ANCHE I PESI PER FASCIA DEL BIANCO E NERO, CHE È LA SUA RISPOSTA `hsl` A
`d-bn-pesi`** (giro della `2.19`): col bianco e nero acceso le prime due righe non hanno più niente
da fare e si spengono, mentre la **luminanza** diventa quanto quel colore pesa nel grigio. Non è un
secondo meccanismo che gli somiglia, è lo stesso conto, e il bianco e nero viene **dopo** di lui
proprio per raccoglierne il risultato. ⚠️ **Chi volesse scurire un cielo in una fotografia in
bianco e nero** muove la luminanza della fascia del blu, che è il gesto di chi metteva un filtro
giallo davanti all'obiettivo.

⚠️⚠️ **I PESI DELLE FASCE SONO TRIANGOLARI CON UN RAGGIO PER LATO, E COSÌ LA SOMMA VALE UNO SENZA
NORMALIZZARE NIENTE**: il raggio di una fascia da un lato è esattamente la distanza dal centro
vicino, quindi fra due centri adiacenti i due pesi sommano a uno e tutti gli altri valgono zero.
- ⚠️⚠️ **LA PRIMA STESURA AVEVA UN RAGGIO UNICO ED È STATA SCARTATA SU UNA MISURA**: i centri di
  Lightroom **non sono equispaziati** (tre fasce nei primi sessanta gradi, due nei centoventi
  successivi), quindi con sessanta gradi per tutti un rosso pieno riceveva tre fasce e del proprio
  cursore gli arrivava il **55 per cento**. Normalizzare dividendo per la somma toglie lo
  sbilanciamento ma non quello: il centro continuerebbe a dividere il suo effetto con i vicini.
- ⚠️ **I due raggi si RICAVANO dai centri e non si scrivono**: chi spostasse un centro si
  ritroverebbe i raggi giusti senza toccare altro. A presidiarlo è `SviluppoTest`, che misura la
  **relazione** (il raggio di una fascia verso l'alto è quello che la vicina ha verso il basso, e
  la somma fa un giro intero) invece di ricopiare il conto, che vive in AGSL e il banco non lo può
  eseguire.

⚠️⚠️ **IL CONTO PASSA PER HSV E A RIPOSO NON SI FA AFFATTO, E LE DUE GUARDIE SONO DUE.** L'andata e
il ritorno da HSV in `half` non sono esattamente l'identità, quindi senza guardie un'immagine non
toccata perderebbe un livello qua e là: una guardia **uniforme** salta il blocco quando nessuna
fascia è mossa, e una guardia **per pixel** lo salta per chi appartiene a fasce tutte a zero.
Muovendo una fascia sola, il resto dell'immagine resta identico.
- ⚠️ **La saturazione si moltiplica e non si somma**: un grigio ha saturazione zero, quindi resta
  grigio qualunque cosa chieda la sua fascia, e a -100 il colore arriva esattamente al grigio
  invece di attraversarlo.
- ⚠️ **La luminanza è pesata da quanto il pixel ha colore**, e senza quel peso schiarirebbe anche
  un cielo bianco e il rumore degli scuri, che tonalità non ne hanno. Tonalità e saturazione quel
  peso non ce l'hanno scritto perché ce l'hanno per costruzione: ruotare o saturare un grigio non
  cambia un grigio.
- ⚠️ **La tonalità si sposta al massimo di trenta gradi**, che è la distanza fra due fasce vicine
  nella metà fitta della ruota: oltre, un rosso spinto diventerebbe giallo e il cursore si
  leggerebbe come rotto.

⚠️⚠️ **IL POSTO NELLA CATENA È FRA IL CONTRASTO E LA SATURAZIONE, E LE DUE COSE HANNO DUE RAGIONI
DIVERSE**: dopo il contrasto, perché sceglie i colori **per tonalità** e la tonalità è quella che
la luce ha finito di definire; prima della saturazione, perché quella è il giudizio finale su tutta
l'immagine mentre questo è mirato, e perché il grigio del bianco e nero si ricava da quello che
esce **di qui**.

⚠️ **La fila delle otto pastiglie prende il colore dal CENTRO della fascia**, che è lo stesso
numero che il conto usa per sapere a quale fascia appartiene un pixel: con due elenchi, il primo a
divergere sarebbe quello che nessuno guarda. I due gesti sono quelli del gettone di un modulo un
gradino più in basso, tocco per scegliere e tocco lungo per azzerare: là si azzera il modulo, qui
la fascia.

⚠️⚠️ **I VENTIQUATTRO CURSORI SONO PRECOSTRUITI, ED È QUELLO CHE TIENE IN PIEDI LA CORREZIONE DELLA
`2.20`**: le righe in scena sono tre e le fasce otto, quindi lo stesso nodo serve otto insiemi di
valori. La riga risolve **modulo e fascia al momento della scrittura**, che è la stessa correzione
di allora su una dimensione in più; e la chiave di ogni riga è il suo cursore, che è un oggetto
costruito una volta sola, quindi due fasce non ne hanno nessuno in comune e cambiando fascia i nodi
si buttano invece di passare di mano. ⚠️ **Generare i cursori a ogni ricomposizione romperebbe
tutto**: una chiave che cambia a ogni giro butterebbe e rifarebbe i nodi in continuazione, cioè
annullerebbe ogni gesto in corso.

⚠️ **Che cosa il banco misura e che cosa no** (`SviluppoTest`, più `ContoTest` per il programma):
che la fascia scelta cambi i valori che i tre cursori mostrano e scrivono, che il tocco lungo su
una pastiglia azzeri **solo** quella, che col bianco e nero resti accesa la sola luminanza, che le
pastiglie ci siano solo nel modulo che ne ha, e che il programma compili con gli uniform ad array.
**Non** vede i pixel che ne escono: che il cursore del blu tocchi davvero il cielo e lasci stare
l'incarnato si guarda sul telefono, e la voce di collaudo lo chiede.

## 🔍 Il modulo Dettaglio, e le prime due operazioni che guardano i vicini

⚠️⚠️ **È IL QUARTO MODULO DELL'EDITOR COMPLETO, DALLA `2.22`, E SONO LE DUE FUNZIONI CHE HA
CHIESTO LUI** (campo libero del giro della `2.15`: *'Maschera di contrasto' e 'Riduzione
rumore'*). Dove gli altri tre moduli parlano del **colore** di un pixel, questo parla del suo
**intorno**: sono le prime due operazioni dell'editor che leggono i pixel vicini, e da lì viene
tutto quello che costano. I cursori sono cinque, nell'ordine del pannello di Lightroom: nitidezza,
raggio, mascheratura, rumore, rumore colore.

⚠️⚠️ **LE MISURE SONO FRAZIONI DEL LATO E NON PIXEL, ED È QUESTO CHE TIENE INSIEME L'ANTEPRIMA E
IL FILE SALVATO.** Il conto gira su due immagini di misura diversa: l'anteprima, che l'editor
riduce a 1600 pixel di lato per lavorare in fretta, e il file pieno, che il salvataggio lavora a
tessere. Un raggio scritto in pixel peserebbe più del doppio sull'anteprima; scritto come frazione
del lato, ognuno dei due lo converte con la **propria** misura e i due risultati coincidono in
proporzione, senza nessun secondo dato da tenere allineato.
- ⚠️ **Quello che resta fuori si dichiara**: l'anteprima è già una riduzione, quindi la grana fine
  del sensore là è **già stata mediata**, e la riduzione del rumore si giudica davvero sul file
  salvato. È lo stesso limite per cui in un editor da tavolo la nitidezza si guarda al 100%.
- ⚠️ **Il raggio di serie è un millesimo del lato**, cioè quattro pixel su un file da quattromila:
  è micro-contrasto, e si vede anche guardando l'immagine intera. Un raggio da nitidezza di cattura
  (un pixel) si vedrebbe solo ingrandendo, e un cursore che a occhio non fa niente si legge come
  rotto: quel raggio c'è, ed è il fondo corsa del cursore 'Raggio'.

⚠️⚠️ **DUE CURSORI SU CINQUE NON CAMBIANO UN PIXEL DA SOLI, E L'INTERFACCIA LI SPEGNE**: il raggio
e la mascheratura non sono quantità, sono **come** la maschera di contrasto lavora. Con la
nitidezza a zero non c'è nessuna maschera da governare, e `Detail.idle` non li conta: contandoli,
un'immagine con la sola mascheratura mossa si dichiarerebbe da riscrivere, cioè verrebbe
ricompressa per niente.
- ⚠️ **Il meccanismo che li spegne è nato qui e vale per tutti**: fino alla `2.21` un cursore
  poteva dichiarare solo 'il bianco e nero mi spegne', e adesso dichiara **quando** non governa
  niente. I casi sono due, e scriverne un campo per ognuno moltiplicherebbe la tabella dei cursori.

⚠️⚠️ **E IL RAGGIO È IL PRIMO CURSORE BIPOLARE DI UN MODULO CHE NE HA QUATTRO MONOPOLARI**: 'niente
nitidezza' e 'niente riduzione' sono il fondo naturale di quei quattro, perché una nitidezza
negativa sarebbe una sfocatura e una riduzione negativa non vuol dire niente; un raggio zero invece
non esiste, quindi là lo zero è il raggio di serie e la corsa lo raddoppia o lo dimezza.
- ⚠️ **Un cursore monopolare non porta il segno e non disegna la tacca dello zero**: là il numero
  non può essere negativo, e la tacca cadrebbe sotto il tondo a riposo.

⚠️⚠️ **IL DETTAGLIO VIENE PER PRIMO NELLA CATENA, PRIMA DEL BILANCIAMENTO DEL BIANCO**, ed è
l'unico modulo che parla del **file** invece che dell'immagine: quanto rumore ha il sensore, e
quanto il disegno fine va accentuato. Messo dopo, il contrasto avrebbe già moltiplicato la grana
che questo modulo esiste per togliere. ⚠️ **E lavora sui valori del file e non in luce lineare**,
al contrario della Luce: il rumore è quello che l'occhio vede nei numeri del file, e una
conversione per ognuno dei diciotto campioni costerebbe più di tutto il resto del programma.

⚠️⚠️ **IL QUINTO CURSORE IN ITALIANO SI CHIAMA 'DISTURBO COLORE', DALLA `2.38`** (sua istruzione,
2026-09-13: *`Rumore colore` diventa `Disturbo colore`*). ⚠️ **Cambia il solo file italiano**: la
sua riga ne nomina uno, quindi il quarto resta 'Rumore' e le altre ventisette lingue non c'entrano,
come per la grafia americana dell'inglese. ⚠️ **La chiave non si tocca** (`look_noise_color`): il
posto nell'interfaccia e la chiave nell'archivio sono due cose indipendenti.

⚠️ **Dentro il modulo la riduzione viene prima della nitidezza**, perché accentuare e poi spianare
vuol dire lavorare due volte contro se stessi; e il dettaglio da accentuare si misura
sull'immagine **già ripulita**, o il rumore appena tolto tornerebbe dentro moltiplicato.

⚠️⚠️ **LE DUE MEDIE ESCONO DA DUE VICINATI DIVERSI, E OGNUNO PAGA SOLO CHI LO USA**: la nitidezza
prende la media **binomiale** (pesi 1-2-1) a distanza del raggio, la riduzione una media
**bilaterale** in cui ogni vicino pesa per quanto somiglia al centro, così la grana si media e un
contorno no. Sono nove campioni per mestiere, dietro due guardie separate: chi non chiede la
nitidezza non paga i suoi nove.
- ⚠️ **La mascheratura si ricava dagli stessi campioni**, cioè dalla differenza massima dal centro,
  e non ne costa altri. A zero passa tutto, e salendo la nitidezza arriva solo dove c'è un contorno
  vero: senza di lei, alzare la nitidezza su un cielo vuol dire alzare il suo rumore.
- ⚠️ **Il rumore di colore prende la crominanza della media e le rimette la luminanza del centro**,
  quindi le macchie colorate spariscono e il disegno resta, perché il disegno vive nella luminanza.
  Senza quella riga sarebbe una seconda sfocatura.

⚠️⚠️ **E IL SALVATAGGIO A TESSERE HA DOVUTO CAMBIARE, che è la cosa annunciata da quando l'editor
completo esiste**: fino alla `2.21` ogni operazione guardava un pixel per volta, quindi due tessere
accostate non avevano nessuna cucitura. Adesso il filtro legge i vicini, e senza un bordo di
sovrapposizione l'ultima colonna di una tessera leggerebbe il **bordo ripetuto** invece del pixel
che sta di là: su ogni giunzione comparirebbe una riga.
- ⚠️⚠️ **QUANTO LARGO DEBBA ESSERE QUEL BORDO LO DICE IL FILTRO, E NON È UN NUMERO SCRITTO A MANO**
  (`Detail.bleed`): è esattamente quanto il filtro arriva lontano, più un pixel per il
  campionamento bilineare. A modulo spento vale **zero**, quindi le tessere tornano quelle di
  prima e chi non usa il Dettaglio non paga niente.
- ⚠️ **Il passo si stringe di quanto il bordo cresce**, o una tessera col bordo supererebbe il
  tetto della texture, che è la ragione per cui le tessere esistono.
- ⚠️ **Il bordo si taglia ai margini dell'immagine**, dove non c'è niente da leggere: là il filtro
  legge il bordo ripetuto, che è il comportamento giusto.
- ⚠️ **Il filtro lineare sul `BitmapShader` è la riga gemella**, e va in tutti e due i posti
  (l'anteprima e il salvataggio): i campioni cadono a distanze che non sono pixel interi, e senza
  filtro verrebbero arrotondati al pixel più vicino, cioè il vicinato si accartoccerebbe su meno
  punti di quanti ne chiede. ⚠️ **Non basta `isFilterBitmap` del pennello**, che governa il disegno
  e non i campioni che uno shader chiede a un altro.

⚠️ **Che cosa il banco misura e che cosa no** (`SviluppoTest`, più `ContoTest` per il programma):
che il raggio e la mascheratura da soli lascino il modulo a riposo, che si spengano finché la
nitidezza è a zero, che il 'Reset modulo' azzeri **solo** il suo, che le misure si scalino col lato
dell'immagine, e che le tessere leggano il bordo e copino solo il centro. **Non** vede i pixel che
ne escono: che la nitidezza sia nitida e che il rumore se ne vada si guarda sul telefono, e la voce
di collaudo lo chiede.

## ✨ Il modulo Effetti, e i suoi cursori

⚠️⚠️ **È IL NONO MODULO DELL'EDITOR COMPLETO, DALLA `2.53`, ED È LA SUA RISPOSTA `effetti` A
`d-dopo-editor`** (giro della `2.50`, con la sua nota: *'Effetti', con 'Chiarezza', 'Texture',
'Foschia', `Grana` e `Vignettatura`*). I cursori erano **cinque**, nell'ordine in cui li ha
scritti, arrivati in tre giri (la `2.53` i primi due, la `2.54` il terzo e la `2.57` gli ultimi
due, che è la sua istruzione dello stesso giorno: *procediamo un po' alla volta con le versioni e
i test necessari ad ogni giro*), e dalla `2.64` sono **tre**: Foschia, Grana e Vignettatura.
⚠️⚠️ **E DALLA `2.66` OGNUNO DEI TRE PORTA IL SUO SECONDARIO, QUINDI LA FILA È DI SEI**:
§ '🎛️ I tre cursori secondari degli Effetti'.

⚠️⚠️ **CHIAREZZA E TEXTURE SONO USCITE CON LA `2.64`, ED È LA SUA RISPOSTA `via` A
`d-eff-restano`** (giro della `2.63`: *Toglili tutti e due*). La domanda nasceva dalla sua voce
`eff-texture` del giro prima, e gli offriva di correggere il difetto o di togliere i due cursori:
ha scelto la seconda, quindi non c'è una correzione rimandata, c'è un modulo di tre.
- ⚠️⚠️ **IL DIFETTO ERA UN RETICOLO IN NEGATIVO, E LA CAUSA È MISURATA**: quel filtro legge **nove
  campioni radi** a distanza del proprio raggio, cioè campiona invece di mediare, quindi quello
  che è più fine del passo si ripiega e a fondo corsa si vede come una trama. L'energia alla
  frequenza del campionamento passa da 2,8 a riposo a 5,6 col conto di allora. ⚠️ **È lo stesso
  meccanismo che a raggio stretto non si vede**, ed è la ragione per cui la nitidezza del
  Dettaglio, che lavora a un millesimo del lato, non ha mai avuto quel problema.
- ⚠️ **Il rimedio esisteva e il conto lo dice**: leggendo la media da una **riduzione**
  dell'immagine invece che da nove punti, quella misura torna a 2,8. Non è stato scritto perché
  la risposta è arrivata prima, ed è scritto qui perché chi rimettesse quei due cursori riparta
  di lì invece di rifare la misura.
- ⚠️⚠️ **QUELLO CHE RESTA NON È UN MODULO DIMEZZATO, E LA DISTINZIONE È QUELLA CHE REGGE LA SUA
  SCELTA**: il contrasto locale accentua **il disegno che c'è**, e a quel mestiere risponde già la
  nitidezza del Dettaglio; la foschia toglie una cosa che il disegno lo **copre**, e le altre due
  dicono dove il fotogramma si scurisce e di che grana è fatto. Nessuno dei tre ha un gemello in
  un altro modulo.
- ⚠️ **Le due stringhe escono dalle 28 lingue** (`look_clarity` e `look_texture`), e con loro il
  campo, l'uniform, le costanti e la media a nove campioni, che non aveva più chiamanti.

⚠️⚠️ **LA FOSCHIA TOGLIE IL VELO, DALLA `2.54`**, cioè la luce bianca che l'aria aggiunge fra
l'obiettivo e quello che si vede. Non è un contrasto locale: quello misura quanto un pixel stacca
dal proprio intorno, questo misura quanto di quel pixel è aria, e lo sottrae.

⚠️⚠️ **LA STIMA DEL VELO È IL CANALE SCURO, ED È QUELLO CHE RENDE IL CONTO POSSIBILE QUI**: dove
c'è foschia l'aria alza tutti e tre i canali insieme, quindi non scende più nemmeno il più basso;
dove non ce n'è, un colore ha quasi sempre un canale quasi spento, perché un colore è tale proprio
quando i tre non sono uguali. Il minimo dei tre, mediato sull'intorno, dice quanto velo c'è **in
quel punto**.
- ⚠️⚠️ **SI PRENDE LA MEDIA DEI MINIMI E NON IL MINIMO DEL BLOCCO**, che è la forma classica: il
  minimo su un blocco fa una mappa a gradini, e ogni gradino diventa un alone intorno ai contorni
  forti. La media cambia piano, che è quello che una mappa di velo deve fare.
  - ⚠️⚠️ **MA DALLA `2.60` QUELLA MEDIA SEGUE I BORDI, ED È LA PRIMA METÀ DEL SUO RISCONTRO**
    (giro della `2.55`: *Foschia: servirebbero regolazioni più raffinate*). Una media uniforme
    prende il velo del cielo e lo spalma **dentro** il profilo che ha sotto di sé, quindi là la
    sottrazione arriva più in basso di quanto spetti: su un profilo contro un cielo velato a 0,62
    la roccia vicino al bordo perdeva **15 livelli** rispetto a quella lontana e finiva sul nero,
    su quattro righe, e il cielo appena sopra riceveva una stima sbagliata di **33 livelli**. Con
    un peso che cade dove il vicino è diverso dal centro, tutti e due gli scarti vanno a **zero**.
  - ⚠️ **E il velo vero non si perde**: sul cielo che sfuma dolcemente, cioè il caso per cui il
    cursore esiste, la mappa coincide col canale scuro a meno di **zero** livelli con e senza il
    peso. Quello che il peso toglie è la sbavatura fra due cose diverse, non la stima.
  - ⚠️ **La soglia è sua e non quella del rumore** (`HAZE_EDGE`, 20 contro 120): là si separa la
    grana di un sensore da un contorno, qui il velo di una regione da quello della regione
    accanto, e le due distanze non sono la stessa. Il numero è il primo che azzera l'alone.
  - ⚠️⚠️ **E DALLA `2.65` I NOVE CAMPIONI CADONO SU DUE ANELLI E NON SU UNA GRIGLIA, PERCHÉ UNA
    GRIGLIA FA UN PETTINE** (riscontro del giro chiuso il 2026-09-19, voce `eff-foschia` non
    approvata: *mi sembra che adesso i valori negativi introducano un difetto simile a quello già
    rilevato per Chiarezza e Texture*, cioè un reticolo). Nove delta su una griglia a passo `s`
    hanno una risposta in frequenza **periodica**, quindi ai periodi `s`, `s/2`, `s/3` e `s/4` la
    stima **non media affatto**: misurato, la risposta valeva esattamente **1,000**, cioè la mappa
    del velo copiava la trama che a quei periodi l'immagine ha. Con gli anelli vale 0,19-0,62.
    - ⚠️⚠️ **E PERCHÉ SI VEDESSE SOLO IN NEGATIVO LO DICE IL SEGNO, NON IL TETTO DELLA `2.60`**:
      dove la stima copia il contenuto, quelle frequenze si comportano diversamente dalle altre.
      Aggiungendo velo tutta la texture si attenua e quelle no, quindi **sporgono** e si leggono
      come un reticolo; togliendolo tutta la texture si accentua e quelle no, quindi rientrano, e
      un buco non si nota. Sulla texture di prova il pettine passava da 1,236 (l'originale) a
      **1,312** aggiungendo, e adesso vale **1,230**, cioè quanto l'originale.
    - ⚠️⚠️ **CINQUE E TRE SONO COPRIMI, ED È QUELLO CHE ROMPE LA PERIODICITÀ**: con due anelli di
      quei conti, sfasati e con raggi non in rapporto intero, non esiste nessuna direzione in cui
      i campioni cadano a passo costante. ⚠️ **Non azzera il massimo fuori banda**, che resta
      0,870 contro 1,000: quello che cambia è che i picchi residui cadono a frequenze e direzioni
      sparse invece che su una griglia allineata agli assi, quindi non compongono una trama.
    - ⚠️⚠️ **LA STRADA SCARTATA È LA RIDUZIONE, ED È MIGLIORE SULLA CARTA**: leggere i campioni da
      una versione rimpicciolita dell'immagine porta il massimo fuori banda a **0,246**, perché
      ogni campione è già la media della sua cella. Costa una texture in più nello shader, la sua
      costruzione a ogni tessera del salvataggio, una griglia da allineare fra le tessere e un
      bordo più largo; e sulla texture di prova dà **1,229** contro i 1,230 dei due anelli, cioè
      lo stesso. Chi ci tornasse riparta di lì invece di rifare la misura.
    - ⚠️⚠️ **E GLI ANELLI NON BASTANO CON PIÙ CAMPIONI, che è la misura che ha chiuso la scelta**:
      cercando la disposizione migliore, con 17 campioni il massimo fuori banda si ferma a 0,567 e
      con 25 a 0,550. Cioè triplicare il costo non avvicina alla riduzione, e a parità di nove
      campioni la differenza fra le due strade non si vede su una texture vera.
- ⚠️⚠️ **LA LUCE ATMOSFERICA SI PRENDE BIANCA, E LA SCELTA È DICHIARATA PERCHÉ L'ALTRA STRADA NON
  STA IN PIEDI QUI**: il modello completo la stima sull'immagine **intera**, cioè con un numero
  che l'anteprima e il file pieno dovrebbero condividere. Quel numero non può vivere in `Look`,
  perché uno stile se lo porterebbe da un'immagine all'altra, e ricalcolato sulla tessera del
  salvataggio darebbe un valore diverso per ogni tessera, cioè una cucitura su ogni giunzione.
  Con la luce bianca il conto sta tutto nello shader e non c'è niente da tenere allineato.
  ⚠️ **Quello che si perde**: con una foschia molto colorata resta una dominante, e là il cursore
  giusto è il bilanciamento del bianco, che è lì accanto.
- ⚠️ **I due versi sono l'uno l'inverso dell'altro**: togliere il velo è `(c - k) / (1 - k)`,
  aggiungerlo è `c + k (1 - c)`, con lo stesso `k`. Verso il basso la foschia si **amplifica**
  invece di essere tolta, che è la lettura simmetrica e non un secondo meccanismo.
  - ⚠️ **Dalla `2.60` quella simmetria ha un'eccezione dichiarata**: dove il tetto delle ombre
    entra in funzione, togliere il velo ne toglie meno di quanto rimetterlo ne rimetta, quindi i
    due versi non si disfanno più esattamente. Vale su un dettaglio scuro fine, e il prezzo è quello di non
    chiudere le ombre.
- ⚠️ **Il suo raggio è il più largo dei tre**, perché il velo è una proprietà di una **regione**:
  una stima che cambiasse in fretta verrebbe scambiata per il disegno, e il conto ne accentuerebbe
  i bordi.
- ⚠️⚠️ **NON CHIUDE PIÙ LE OMBRE, DALLA `2.60`, ED È LA SECONDA METÀ DELLO STESSO RISCONTRO**: il
  velo tolto non supera quello che il pixel stesso porta. ⚠️⚠️ **E LA NOTA DI PRIMA ERA FALSA,
  SMENTITA DA UN CONTO**: diceva che il fondo corsa teneva quell'effetto *fuori dalla corsa*, e la
  misura dice il contrario. Dentro una zona velata a 0,62, a cursore pieno, i **64** livelli sotto
  il quarto di scala uscivano tutti allo stesso valore, cioè ne restava **uno**: il disegno negli
  scuri spariva del tutto. Col tetto ne restano **40** distinti.
  - ⚠️ **Il tetto è lo stesso fattore applicato al canale scuro del PIXEL**, e non una costante
    nuova: dove il velo c'è davvero quel numero vale quanto la stima, quindi il `min` non entra
    in funzione (misurato: **zero** livelli di scarto su una zona uniforme e sui pixel chiari).
    Entra solo dove il pixel è più scuro del proprio intorno, cioè su un dettaglio fine che la
    stima non ha visto.
  - ⚠️⚠️ **E VALE SOLO NEL VERSO CHE TOGLIE**: aggiungere velo non azzera nessun pixel, quindi non
    c'è niente da proteggere, e limitarlo sugli scuri toglierebbe al cursore proprio quello che fa
    da quella parte, cioè alzare le ombre.

⚠️ **L'ordine dentro il modulo non ha più niente da governare, dalla `2.64`**: la foschia veniva
per ultima perché chiarezza e texture misuravano lo **scarto** fra il pixel e il suo intorno, e
messa prima avrebbe cambiato il solo centro, cioè avrebbe riempito l'immagine di aloni dove il
velo cambia. Adesso è l'unica a leggere i vicini, e quel vincolo resta scritto perché chi
rimettesse un contrasto locale lo trovi.

⚠️⚠️ **E GLI ALTRI DUE SONO DI UN'ALTRA SPECIE: NON LEGGONO I PIXEL VICINI, LEGGONO DOVE SI
TROVA IL PIXEL.** La foschia paga campioni e un bordo sulle tessere del salvataggio; questi due
costano **zero campioni** e in cambio pretendono un dato che fino alla `2.56` non serviva a nessuno,
cioè **dov'è questa tessera dentro l'immagine intera**. Quel dato è `Framed`, e lo shader lo
riceve in due uniform (l'origine dell'immagine e la sua misura).
- ⚠️⚠️ **SENZA DI LUI I DUE DIFETTI NON SI VEDONO SULL'ANTEPRIMA, E QUESTA È LA COSA DA SAPERE**:
  là la tessera è **una sola**, quindi l'origine vale zero e tutto torna; nel file salvato invece
  ogni tessera si vignetterebbe per conto suo (un angolo scuro su ogni giunzione) e porterebbe la
  stessa grana ripetuta, cioè un motivo a scacchi grande quanto una tessera. È la forma di difetto
  che arriva **solo** a chi salva un file grande.
- ⚠️ **Il segno lo mette la funzione e non il chiamante**: dentro una tessera le coordinate partono
  da zero sul suo angolo, quindi l'immagine intera comincia a un'origine **negativa**. Scritta dal
  chiamante, quella negazione sarebbe una riga da ricordare e un difetto che non dà nessun errore.

⚠️⚠️ **LA VIGNETTATURA SI MISURA SULLA MEZZA DIAGONALE, E DALLA `2.67` LA RAMPA PARTE DAL CENTRO**:
sulla diagonale perché l'angolo deve valere uno su qualunque formato (dividendo per il lato, su un
panorama lo stesso valore del cursore scurirebbe i due lati corti molto più degli altri due); dal
centro perché è quello che fa un obiettivo, che cala da subito, e perché è la **sfumatura sempre
massima** che ha chiesto. Lo scalino al centro lo toglie `smoothstep`, che parte con pendenza zero.
- ⚠️ **È bipolare, e il verso positivo APRE l'angolo invece di chiuderlo**: è il gesto di chi
  corregge la vignettatura che l'obiettivo ha già messo, e non un riempitivo per simmetria.
- ⚠️⚠️ **FINO ALLA `2.66` PARTIVA DA METÀ RAGGIO E IL PIENO STAVA SEMPRE SULL'ANGOLO**, e adesso è
  il contrario: la rampa parte sempre dal centro e a muoversi è il punto del **pieno**, che lo
  sposta il cursore 'Sfumatura' (§ '🎛️ I tre cursori secondari degli Effetti'). Chi legge che
  'il centro resta intatto' sappia che era vero fino a quella versione.

## 🎛️ I tre cursori secondari degli Effetti

⚠️⚠️ **DALLA `2.66`, ED È IL SUO CAMPO LIBERO** (giro chiuso il 2026-09-19: *Prima di chiudere il
modulo Effetti voglio fare una cosa che ti avevo anticipato, ovvero raffinarli con parametri
aggiuntivi. Rimane spazio verticale per tre slider secondari, uno per effetto, che si dovrà
attivare solo se l'effetto relativo sta modificando l'immagine*). Il modulo passa da tre cursori a
**sei**, e ognuno dei tre nuovi vive **sotto** quello che governa, cioè si legge come la sua
conseguenza: è il criterio con cui il 'Filtro BN' è finito sotto l'interruttore del bianco e nero
nella `2.37`.

⚠️⚠️ **'UNO PER EFFETTO' E IL SUO ELENCO NON DÀNNO LO STESSO CONTO, E VINCE L'ELENCO**: la frase
dice uno per effetto, e le tre voci che scrive sotto sono **due** per la grana e **una** per la
vignettatura, quindi alla foschia non ne tocca nessuna. Il numero torna, la ripartizione no, e
l'elenco è la parte dettagliata, perché ognuna delle tre porta la sua ragione scritta; 'uno per
effetto' è il conto dello spazio verticale. ⚠️ **La voce di collaudo gli dice questa lettura in
chiare lettere**, che è la regola del `CLAUDE.md` di root sulle letture dichiarate.

⚠️ **Si spengono quando il loro principale è a zero**, che è la seconda metà della sua richiesta, e
il meccanismo è quello della maschera di contrasto senza nitidezza: il campo `off` della tabella dei
cursori, nato col Dettaglio nella `2.22`. ⚠️ **E non contano in `Effects.idle`**, o un'immagine con
la sola 'Sfumatura' mossa si dichiarerebbe da riscrivere, cioè verrebbe ricompressa per niente.

⚠️⚠️ **'Luci' HA IL VALORE DI FABBRICA SULLO ZERO, E QUELLO DECIDE COME È SCRITTO** (sua richiesta:
*'Luci', che è l'abbreviazione di 'Applica alle luci'. Voglio che di default la grana sia aggiunta
solo alle ombre, e alle luci in misura non proprio zero ma quasi. Questo per evitare che ad aree
uniformi come il cielo sia aggiunta grana inutilmente*). Scritto al rovescio, cioè con lo zero sul
comportamento della `2.65`, il valore di fabbrica sarebbe stato un numero diverso da zero, e allora
un preset che non nomina quel campo lo rileggerebbe sbagliato.
- ⚠️⚠️ **A FONDO CORSA IL CONTO TORNA ESATTAMENTE QUELLO DELLA `2.65`** (misurato: scarto nullo su
  1001 toni), quindi il comportamento di prima non si perde, si sposta a un capo della corsa.
- ⚠️⚠️ **MA CHI HA UNO STILE CON LA GRANA MOSSA LA RITROVA DIVERSA, E SI DICHIARA**: il peso di
  fabbrica è cambiato, e un preset salvato con la `2.65` non porta quel campo, quindi lo rilegge a
  riposo. Sulle sue immagini la grana resta dov'era nelle ombre e quasi sparisce nei chiari.
- ⚠️⚠️ **E DALLA `2.67` I TRE NUMERI SONO SUOI, DETTATI COL TELEFONO IN MANO** (riscontro del giro
  della `2.66`, voce `eff-grana-luci` accettabile: *il 'quasi zero' passa da 0,8 a 0,1. Ovviamente
  il passaggio da ombre a luci dev'essere graduale; metà scala a 0,35*). I vincoli sono tre e
  ognuno fissa una costante: il pavimento è quello che dà **0,1 livelli su 255** sul cielo a
  `t=0,82`; la soglia bassa cade sul **quarto di scala**, cioè dove finiscono le ombre che restano
  intatte; e quella alta è quella che fa valere **0,35** il peso a metà scala.
  - ⚠️⚠️ **'0,35' È IL PESO E NON I LIVELLI, ED È UNA LETTURA DICHIARATA**: la voce della `2.66` gli
    diceva che a metà tono la grana teneva l'**81%**, e la sua riga risponde a quel numero; letto in
    livelli darebbe 0,35 su 255 a metà scala contro i 9,2 del quarto, cioè un crollo, che è il
    contrario del *graduale* della stessa frase. La voce di collaudo gli dice questa lettura in
    chiare lettere.
  - **Che cosa cambia, in livelli su 255 a grana piena**: fino al quarto di scala niente, a metà
    scala si passa da 9,9 a **4,2**, a due terzi da 6,5 a **0,8**, sul cielo da 0,8 a **0,1**. Il
    picco si sposta da 0,39 a **0,31**, cioè ancora più nelle ombre.
  - ⚠️ **Sotto la quantizzazione, e va detto**: un decimo di livello su 255 non si vede affatto,
    quindi sul cielo la grana di fatto non c'è. È quello che ha chiesto, e il posto in cui si
    chiede il contrario è il cursore stesso.
- ⚠️⚠️ **E DALLA `2.68` QUESTO CURSORE A RIPOSO ALZA ANCHE LE OMBRE, PERCHÉ HA CHIESTO UNA
  RIDISTRIBUZIONE** (riscontro del giro della `2.67`, voce `grana-luci-2` accettabile: *anche al
  minimo (0,1) i cieli hanno troppa grana, mentre forse le ombre troppo poca. Cerchiamo di
  raggiungere un cielo quasi immacolato e delle ombre con un 30% in più di grana, il tutto senza
  bordi netti e con toni medi sfumati*). Quindi i numeri di quel blocco sono **quattro**, e il
  quarto è il guadagno delle ombre.
  - ⚠️⚠️ **LE OMBRE NON POTEVANO SALIRE SENZA UN TERMINE NUOVO, ED È IL PEZZO CHE CAMBIA**: là la
    rampa vale **uno**, cioè il peso pieno, che era il massimo che la `2.66` sapeva dare. Adesso i
    capi sono due: a riposo le ombre valgono `1,3` e le luci il pavimento, a fondo corsa tutti e
    due valgono uno, cioè torna esattamente la `2.65`.
  - ⚠️⚠️ **IL MASSIMO ASSOLUTO NON CRESCE, SI SPOSTA, e questa misura dice che il `30% in più` non
    è una grana più forte di quanto sia mai stata**: il picco vale **12,4 livelli a `t=0,28`**,
    contro i **12,2 a metà tono** che la `2.65` dava a fondo corsa del cursore principale.
  - ⚠️⚠️ **'CIELO QUASI IMMACOLATO' È UNA LETTURA DICHIARATA, E FISSA LA SOGLIA ALTA**: si legge
    come *sotto la quantizzazione a metà scala*, cioè mezzo livello su 255, perché un cielo azzurro
    vive fra metà scala e i tre quarti. Il decimo di livello che aveva dettato per `t=0,82` non si
    tocca: il difetto non era il pavimento, era la fascia prima di lui.
  - **Che cosa cambia, in livelli su 255 a grana piena**: fino al quarto di scala **+30%** esatto
    (il quarto passa da 9,2 a 11,9), a `t=0,35` +7%, a `t=0,45` **-54%**, a metà scala da 4,3 a
    **0,5**, e dai tre quarti in su niente.
  - ⚠️ **Il suo *senza bordi netti* è la rampa, e resta una sfumatura**: larga **70 livelli su 255**
    invece di 105, con la stessa `smoothstep`, quindi la pendenza è nulla ai due capi.
  - ⚠️ **La soglia alta si arrotonda per DIFETTO**, e a dirlo è stata la prova: il valore esatto è
    `0,52360`, e scritto a quattro decimali per eccesso manca il vincolo per un decimillesimo di
    livello.
- **I numeri della `2.66` erano altri tre**: sul cielo si fermava a `0,8` livelli e a metà tono la
  grana teneva l'81% di quanto teneva nella `2.65`. Chi li ritrova in una nota sappia che sono
  quelli del giro prima.
- ⚠️ **Quello che la sua richiesta non copre**: un cielo è chiaro **e** uniforme, e questo cursore
  guarda solo quanto è chiaro. Guardare anche l'uniformità vorrebbe dire leggere i pixel vicini,
  cioè un raggio e un bordo sulle tessere del salvataggio, che è il prezzo che la foschia paga da
  sola; qui costa zero campioni ed è la strada che lui ha indicato (*Se ci sono soluzioni più
  'smart', proponi pure, ma penso che funzionerebbe bene*).

⚠️ **'Dimensione' raddoppia e dimezza la cella**, come il raggio del Dettaglio e per la stessa
ragione: una misura di quel genere si percepisce in rapporti, e lo zero è quella che lui ha
approvato (*L'attuale va benissimo ed è il default, ma a volte mi piace generare grana un po' più
grossa*). Su un file da quattromila pixel la corsa va da 1,7 a 6,7 pixel, con 3,3 a riposo.
- ⚠️⚠️ **VERSO IL FINE IL PAVIMENTO DEL PIXEL ENTRA ANCHE SULL'ANTEPRIMA, e va detto**: a 1600
  pixel di lato la cella scende sotto il pixel intorno a `-0,4`, quindi là la grana si vede un po'
  più grossa di quella che il file salvato porterà. Fino alla `2.65` quel pavimento non si
  incontrava mai sopra i 1200 pixel, e questo cursore è la ragione per cui adesso si incontra.

⚠️⚠️ **'Sfumatura' SPOSTA IL PUNTO IN CUI LA VIGNETTATURA ARRIVA AL PIENO, DALLA `2.67`, E FINO
ALLA `2.66` SPOSTAVA QUELLO IN CUI COMINCIAVA** (riscontro del giro della `2.66`, voce
`eff-vign-sfuma` accettabile: *voglio che la sfumatura sia sempre massima. Deve cambiare il punto
di *inizio*, che in negativo dev'essere lontano (fuori dal fotogramma), mentre in positivo arriva
poco all'interno del bordo*). La rampa parte sempre dal centro, che è la sfumatura più lunga che il
fotogramma consenta, e a muoversi è il suo capo esterno.
- ⚠️⚠️ **'IL PUNTO DI INIZIO' È QUELLO DEL PIENO, E LA LETTURA È DICHIARATA**: guardando
  dall'esterno, la vignettatura comincia dove l'alone è pieno e sfuma verso il centro. Letto come
  il capo **interno** la sua frase non sta in piedi, perché in negativo il capo interno fuori dal
  fotogramma vorrebbe dire nessun alone, e in positivo un alone stretto sul bordo: due estremi che
  dicono la stessa cosa.
- **I due estremi sui raggi di `fromCentre`**, dove l'angolo vale `1` e il mezzo di ogni lato
  `0,707`: a `+100` il pieno cade a **0,70**, cioè appena dentro il bordo; a riposo **sull'angolo**;
  a `-100` a **1,30**, cioè fuori dal fotogramma, e là l'angolo si ferma all'86%.
- ⚠️⚠️ **A RIPOSO L'IMMAGINE CAMBIA, E SI DICHIARA INVECE DI PROMETTERE IL CONTRARIO**: con la
  vignettatura a `-50`, a metà raggio si perdono **35 livelli su 255** invece di zero e a metà di un
  lato **56** invece di 26; l'angolo resta identico. Cioè l'alone arriva molto più dentro, ed è la
  conseguenza diretta della sfumatura massima: chi aveva una vignettatura tarata la rivede più
  diffusa e la ritocca col cursore principale.
- ⚠️ **Il suo primo punto è assorbito e non rimandato** (*il minimo dev'essere ciò che adesso è
  -60: sotto è inutile e direi anche dannoso*): il 'dannoso' erano i quattro angoli scuri che la
  `2.66` lasciava a fondo corsa negativa, e la rampa dal centro toglie la causa invece di spostare
  il limite. La corsa negativa nuova è corta per costruzione.
- **I numeri della `2.66` erano questi**: la rampa cominciava a `0,9` in negativo (alone nei soli
  angoli, il 2% del fotogramma), a metà raggio a riposo (61%) e a `0,1` in positivo (98%).

⚠️ **La scheda non cresce**: i sei cursori pareggiano quelli della Luce, e l'altezza comune la detta
comunque il grafico delle Curve (§ `SteadyBody`).

⚠️ **Che cosa il banco misura e che cosa no**: che i tre si spengano col loro principale (col
**verso** della condizione, che è la cosa che può rompersi in silenzio), che non contino come
lavoro né come bordo delle tessere, che la cella della grana si scali col lato e col cursore, e che
i numeri dei due conti restino dentro i loro confini, letti dalla stringa dello shader come i
tre che `Auto` ricopia. ⚠️⚠️ **E DALLA `2.67` QUELLA PROVA RICALCOLA I SUOI NUMERI**, cioè presidia
la sua richiesta e non una costante ricopiata: dalla `2.68` sono **le ombre al 130%**, il **mezzo
livello a metà scala** e il **decimo di livello sul cielo**, più il fatto che a 'Luci' pieno il peso
torni quello della `2.65`. ⚠️ **Quest'ultimo si misura sul TESTO dello shader**, perché un guadagno
scritto come costante lascerebbe le ombre al 130% anche a cursore pieno senza che nessun conto lo
dica. Controprovata tre volte, rimettendo la rampa della `2.67` (cade il cielo), togliendo il
guadagno (cadono le ombre) e scrivendolo come costante (cade il testo); e prima, stringendo la corsa
della vignettatura (cade il confine del bordo). **Non** vede i pixel che ne escono: il conto vive in AGSL, quindi che il
cielo resti pulito e che l'alone si allarghi si guardano sul telefono, e la voce di collaudo lo
chiede. ⚠️ **I numeri di questo blocco vengono da un modello di sessione**, scritto e buttato, che è
la strada dichiarata per questo genere di conto.

⚠️⚠️ **LA GRANA HA UNA CELLA, ED È MONOPOLARE**: un rumore alto un pixel su un file da quattromila
si vede solo ingrandendo e rimpicciolito si media via, quindi la cella è una frazione del lato come
ogni altra misura di questo editor. ⚠️ **Verso il basso non c'è niente**: 'meno grana' non vuol dire
niente su un'immagine che non ce l'ha, e toglierla è il mestiere della riduzione del rumore, che
vive nel Dettaglio.
- ⚠️ **Pesa sui mezzi toni**: una pellicola mostra il grano dove l'emulsione è esposta a metà, e
  quasi niente nel nero chiuso e nel bianco bruciato.
  Senza quel peso il cursore sporcherebbe prima di tutto le ombre, che è l'effetto del rumore
  digitale e non della grana. ⚠️⚠️ **E DALLA `2.66` QUEL PESO SI RITIRA ANCHE DALLE LUCI**, quanto
  lo dice il suo cursore secondario: § '🎛️ I tre cursori secondari degli Effetti'.
- ⚠️ **Si somma lo stesso valore ai tre canali**: la grana di una pellicola è di densità e non di
  colore, e un rumore per canale darebbe i puntini che la riduzione del rumore esiste per togliere.
- ⚠️⚠️ **SOTTO IL PIXEL LA CELLA NON SCENDE, E QUEL PAVIMENTO ROMPE LA PROPORZIONE**: una cella più
  stretta di un pixel non è una grana più fine, è uno sfarfallio. Il pavimento entra in funzione
  sotto i 1200 pixel di lato, quindi **mai** sull'anteprima dell'editor (1600) né su un file da
  fotocamera, e dove entra la grana si vede un po' più grossa di quella del file salvato: si
  dichiara invece di prometterla identica.

⚠️⚠️ **VENGONO PER ULTIMI NELLA CATENA, E I DUE POSTI HANNO DUE RAGIONI DIVERSE**: la vignettatura
sta dopo tutto quello che parla di colore perché è quello che fa un **obiettivo**, e messa prima
ogni cursore della Luce la rimetterebbe in discussione (un 'Auto' calcolato su un'immagine già
vignettata leggerebbe un istogramma che non è il suo); la grana sta dopo ancora, perché è la
**pellicola**, cioè il supporto su cui l'immagine è stampata, e messa prima il contrasto e la
saturazione la tratterebbero come disegno.

⚠️⚠️ **LA FOSCHIA È LA TERZA OPERAZIONE CHE GUARDA I PIXEL VICINI, E DA LÌ VIENE QUELLO CHE
COSTA**: nella catena sta **subito dopo il Dettaglio** e prima del bilanciamento del bianco,
perché legge `image`, cioè i pixel di partenza, quindi deve stare dove quella lettura vale
ancora; e dopo la riduzione del rumore, o la stima del velo leggerebbe la grana che il Dettaglio
ha appena mediato.
- ⚠️⚠️ **QUINDI IL BORDO DELLE TESSERE È IL MASSIMO DEI DUE MODULI, E NON LA SOMMA**: una tessera
  ha un bordo solo, e i filtri girano su di lei e non uno sull'uscita dell'altro. Prendendo
  quello del solo Dettaglio, con la foschia mossa su ogni giunzione comparirebbe una riga. Il
  conto vive in `AdjustRender.bleedFor`, che è una **funzione** e non una riga dentro il giro
  delle tessere, perché il banco la possa chiamare: là dentro disegnare vuole una scheda grafica.
  - ⚠️ **La forma resta quella di un massimo anche con un cursore solo**, dalla `2.64`: chi
    aggiungesse un secondo filtro che legge i vicini la trova già scritta.
  - ⚠️⚠️ **E FINO ALLA `2.64` QUEL BORDO ERA PIÙ STRETTO DEL FILTRO CHE DOVEVA COPRIRE, E NESSUNO
    LO AVEVA VISTO**: i quattro campioni **diagonali** della griglia stavano a radice di due volte
    il raggio, cioè il **41%** oltre quello che `hazeReach` promette, quindi su una giunzione, con
    la foschia mossa, quella fascia leggeva il bordo ripetuto invece del pixel che sta di là. È
    esattamente la riga che il bordo esiste per non far comparire. ⚠️ **Dalla `2.65` i due
    coincidono**, perché l'anello largo tocca esattamente il raggio, e a prenderlo è stata la
    prova nuova e non una rilettura del codice.
- ⚠️ **A riposo il bordo vale zero**, come prima, quindi chi non usa quel cursore paga le
  tessere di sempre. E la guardia uniforme dello shader salta i campioni per pixel.
- ⚠️⚠️ **E LA GUARDIA SI SCRIVE SUL RAGGIO E NON SU 'MODULO A RIPOSO'**: un modulo mosso con la
  sola vignettatura o la sola grana **non** è a riposo, e nessuna delle due legge un vicino.
  Scritta sull'altra condizione, quel caso pagherebbe un pixel di bordo per niente, cioè un passo
  più stretto su ogni tessera per un filtro che non c'è.

⚠️ **L'uniform `matter` non c'è più dalla `2.64`**, ed era il nome con cui la texture arrivava
allo shader: `texture` è una funzione di GLSL e AGSL le somiglia abbastanza da non volerlo
scoprire su un telefono. La prudenza resta scritta perché vale per qualunque nome nuovo, e il
posto in cui un nome vive decide se può dare fastidio.

⚠️⚠️ **UN PRESET SE LI PORTA, QUINDI I MODULI CHE UNO STILE GOVERNA PASSANO DA CINQUE A SEI**:
i cursori sono un **aspetto**, cioè una cosa che si porta da un'immagine all'altra, come la
luce e il colore; i tre che restano fuori sono ancora la posa, il ritaglio e la geometria, che
dipendono da come è stata scattata quell'immagine. ⚠️ **Il formato non si rompe**, perché ogni
campo che manca vale il suo valore di riposo: uno stile salvato con la `2.52` si rilegge oggi, e
uno salvato con la `2.63` si rilegge oggi perdendo i due cursori che non esistono più. ⚠️ **E i
tre secondari della `2.66` viaggiano con loro**, con la stessa regola.
- ⚠️⚠️ **MA I VENTI DI CASA RESTANO SENZA, E VA DETTO INVECE DI LASCIARLO CREDERE**: i suoi
  quattordici sono convertiti dai suoi XMP di Lightroom, e la foschia era fra le cose che quel
  travaso aveva scartato perché l'editor non le aveva (§ '🎞️ I preset, venti di casa e quelli
  che si salvano'). Riportarla vorrebbe dire riaprire i suoi XMP, e la sua risposta `lascia` a
  `d-preset-xmp` dice di rifarli da capo quando il modulo è finito.

⚠️ **Il gettone vive subito dopo il Dettaglio dalla `2.55`, ed è sua istruzione** (riscontro del
giro della `2.54`: *il nuovo ordine dei moduli dev'essere: Dettagli, Effetti, Geometria, Ritaglio,
Luce, Colore, HLS, Curve, Stili*). ⚠️ **Nella `2.53` e nella `2.54` stava fra l'HSL e gli Stili**,
perché parla dei colori come i tre che lo precedevano, e la ragione di oggi è migliore: vive
accanto all'unico altro modulo che **guarda i pixel vicini**, cioè quello con cui divide il bordo
delle tessere.

⚠️⚠️ **E DALLA `2.62` IL GLIFO È IL SUO: LA VIGNETTATURA RIDISEGNATA DA LUI** (arrivata il
2026-09-18, dopo che gliel'avevo mandata come SVG alla fine del giro della `2.55`). È una cornice
quadrata col tondo scavato, cioè l'inchiostro dove una vignettatura scurisce, e le misure vivono in
testa a `ic_mod_effects.xml`.
- ⚠️ **Le note che lo dànno di Material sono superate**, e sono due giri: la `2.53` lo aveva preso
  da `Icons.Filled.Deblur`, che la `2.55` ha cambiato in `Vignette` perché era lo stesso del
  Dettaglio (voce `eff-glifo`). Il disegno di oggi è quella vignettatura, rifatta da lui.
- ⚠️ **Non ha una punta da raccordare**, quindi il trattamento a 0,4 della `2.50` non ha niente da
  fare: è fatto di curve e archi dal primo all'ultimo comando (`icon-round.py` risponde 0).
  ⚠️ **Il trasporto non cambia un pixel**: zero pieni diversi su 57.600, e quel che si vede cade
  sul bordo antialiasato.
- ⚠️⚠️ **E DALLA `2.63` È IL SUO SECONDO DISEGNO, ARRIVATO POCHE ORE DOPO IL PRIMO**: la cornice
  è un **superellisse** invece di un rettangolo stondato, cioè i quattro lati sono leggermente
  bombati in fuori, e il cerchio dentro è un filo più grande. Su 240px cambiano 1.734 pixel pieni
  su 57.600, cioè il 3% della tela.
  - ⚠️⚠️ **I DUE LATI ORIZZONTALI SONO ARCHI DI RAGGIO 115 E I VERTICALI SONO CUBICHE, E NON È UNA
    SCELTA DI CHI HA DISEGNATO**: è la firma della pulizia che il file ha attraversato (CleanSVG,
    cioè SVGO), che sostituisce una curva con un arco quando la differenza resta nella sua
    tolleranza. Il trasporto tiene quello che trova, perché riscrivere un arco come curva vorrebbe
    dire calcolare dei numeri che nel file di partenza non ci sono.
  - ⚠️⚠️ **CLEANSVG HA DATO UN AVVISO CHE DICE '0%', ED È UN DIFETTO SUO E NON DEL FILE**: quel
    ramo scatta sopra lo 0,2% dei pixel e stampa la quota arrotondata all'intero, quindi fra lo
    0,2% e il 0,49% scrive sempre zero. Il perché per esteso, e la correzione, vivono nel
    `CLAUDE.md` di `CleanSVG`.

⚠️ **Che cosa il banco misura e che cosa no** (`SviluppoTest`, più `ContoTest` per il programma):
che il modulo porti i suoi cursori e nessun altro li abbia, che il 'Reset modulo' azzeri
**solo** i suoi, che un loro valore tolga il senza perdita, che le misure si scalino col lato
dell'immagine, che la stima del velo guardi più lontano del raggio massimo della nitidezza, e che
il bordo delle tessere sia il più largo dei filtri mossi **e resti zero coi due che non guardano i
vicini**; dalla `2.57` anche che una tessera dichiari la propria origine **col segno giusto**, che
è la sola cosa di quel giro che possa rompersi in silenzio. `ContoTest` compila il programma **con
gli Effetti dentro**, che è il solo modo di accorgersi di un uniform che non combacia, in un verso
come nell'altro (dalla `2.64` anche di uno di troppo, rimasto nella consegna dopo un cursore
tolto), e `PresetTest` misura che uno stile se li porti tutti e che a riposo non si
scrivano; dalla `2.65` anche il **kernel della mappa del velo**, letto dalla stringa dello shader
come i tre numeri che `Auto` ricopia: che i campioni siano nove, che i pesi sommino a sedici, che
il kernel sia centrato, che nessuno superi il raggio dichiarato al bordo delle tessere, e che
**nessuna frequenza passi intera**, che è la proprietà da cui il reticolo nasceva. **Non** vede i
pixel che ne escono: che la foschia se ne vada, che la vignettatura sia centrata e che la grana
somigli a una pellicola si guarda sul telefono, e la voce di collaudo lo chiede.
- ⚠️⚠️ **LA CONTROPROVA DEL CASO DEL KERNEL DICE DUE COSE, E LA SECONDA NON ERA PREVISTA**:
  rimettendo la griglia della `2.64` cade per prima l'asserzione sul **raggio** (radice di due
  invece di uno, cioè il difetto del bordo delle tessere qui sopra), e sospendendo quella cade
  l'asserzione del pettine con **1,000**, che è il numero del modello. ⚠️ **La soglia è più bassa
  del valore misurato di proposito** (0,95 contro 0,870): si presidia il fatto che nessuna
  frequenza passi intera, non il numero di oggi.
- ⚠️⚠️ **E LA `2.60` NON PORTA NESSUNA PROVA NUOVA, CHE VA DETTO INVECE DI LASCIARLO CREDERE**: le
  due correzioni della foschia vivono **tutte e due** in AGSL, e su una tela di memoria quel
  programma non gira. Quello che il banco fa è compilarlo; i numeri di quel giro vengono da un
  modello di sessione, scritto e buttato, che è la strada dichiarata per questo genere di conto.
- ⚠️⚠️ **LA FILA SI MONTA ROVESCIATA NELLE DUE PROVE CHE TOCCANO IL GETTONE, E SENZA QUELLA RIGA
  MENTIVANO**: col nono modulo la fila scorre, quindi in coda la pastiglia cade fuori dalla
  larghezza del banco; il tocco non dà nessun errore e non cambia modulo, e si contavano zero
  cursori credendo di guardare gli Effetti. È la stessa trappola del sesto gettone della `2.30`.

## 📈 Il modulo Curve, e il colore mirato

⚠️⚠️ **È IL QUINTO MODULO DELL'EDITOR COMPLETO, DALLA `2.23`, ED È IL PRIMO SENZA CURSORI**: quello
che una manopola sa dire è *quanto*, e una curva dice *quanto per ogni tono*, cioè una cosa che
nessun cursore può esprimere. Il suo comando è un **grafico** con dei punti da prendere col dito, e
i canali sono quattro: il composito (RGB) e i tre colori.

⚠️⚠️ **IL CONTO PASSA ALLA SCHEDA GRAFICA COME UNA TABELLA, E QUESTA È LA DECISIONE CHE REGGE
TUTTO IL RESTO.** Gli altri quattro moduli mandano allo shader dei **numeri** e il conto vive in
AGSL, che è la regola scritta in testa a `Adjust.kt`; una spline invece vuole un ciclo sui suoi
punti, e i punti sono in numero variabile. Scritta là costerebbe quel ciclo venti milioni di volte
per un risultato che dipende **solo** dal livello in ingresso, cioè da 256 valori possibili: la si
calcola una volta e la si consegna come una riga di 256 pixel (`uniform shader tone`).
- ⚠️⚠️ **NON È LA SECONDA COPIA CHE QUELLA REGOLA ESISTE PER NON AVERE, e la distinzione è
  precisa**: una seconda copia sarebbe lo **stesso conto** scritto due volte, una per l'anteprima e
  una per il salvataggio. Qui il conto è uno e vive in Kotlin; quello che gira sui pixel è una
  **lettura** della sua tabella, e la stessa tabella la leggono l'anteprima, il salvataggio e il
  grafico che la disegna. Una fonte, tre lettori.
- ⚠️ **Tre letture per pixel e non una**: i tre canali entrano nella tabella a tre posizioni
  diverse, quindi una lettura sola darebbe i tre canali dello stesso livello, che è un'altra cosa.
- ⚠️⚠️ **DUE TRAPPOLE EVITATE NELLA BITMAP, e nessuna delle due dà errore**: Skia consegna a uno
  shader i colori **premoltiplicati**, quindi la tabella è opaca (moltiplicare per uno non cambia
  niente); e il campionamento vuole il **filtro lineare**, o due livelli vicini che cadono nella
  stessa voce escono identici e la curva si vede a gradini. La seconda è la riga gemella di quella
  del Dettaglio.

⚠️⚠️ **LA SPLINE È MONOTONA (Fritsch-Carlson) E NON UNA CUBICA NATURALE, E NON È UN DETTAGLIO DI
QUALITÀ**: una cubica naturale **oltrepassa** fra due punti vicini, quindi un tratto piatto fra due
punti alla stessa altezza si gonfia e poi torna giù, cioè un tono più chiaro esce più scuro del suo
vicino. Sull'immagine è un anello di tono invertito, ed è il difetto classico delle curve fatte
male.
- ⚠️ **Su punti allineati la spline è ESATTAMENTE la retta**, ed è la ragione per cui `Curve.idle`
  può guardare i soli punti invece di confrontare 256 valori: punti sulla diagonale vogliono dire
  tabella identità.
- ⚠️ **Il tratto piatto è il caso che distingue i due conti**, e per questo la prova ne porta uno:
  con una curva a tre punti le due matematiche danno quasi lo stesso disegno, e la controprova
  resterebbe verde.

⚠️⚠️ **LE QUATTRO CURVE SI COMPONGONO IN `all(canale(v))`, E L'ORDINE ROVESCIATO NON DÀ NESSUN
ERRORE**: è la convenzione di ogni editor che ha questo pannello (prima la curva del canale, poi
quella del composito), e al contrario una curva sul rosso cambierebbe di posto ogni volta che si
tocca quella di tutti i toni. ⚠️ **La composizione si fa in Kotlin**, dentro la tabella, quindi sui
pixel resta una lettura per canale.

⚠️ **Il posto nella catena è fra il contrasto e l'HSL, e le due cose hanno due ragioni diverse**:
dopo il contrasto, perché la curva a S è una rimappatura predefinita e questa è quella fatta a mano,
e al contrario i due comandi si contenderebbero gli stessi toni; prima dell'HSL, perché le tre curve
di canale **cambiano la tonalità** di un pixel, e chi sceglie i colori per tonalità deve leggere
quella definitiva.

⚠️⚠️ **I GESTI DEL GRAFICO SONO DUE, E SONO QUELLI DI CASA**: il **trascinamento** prende il punto
sotto il dito, o ne fa uno nuovo, e lo porta dove si vuole; il **tocco lungo** su un punto lo
toglie, che è lo stesso gesto con cui si azzera un modulo e una fascia, un gradino più in basso.
- **Un tocco secco fa nascere un punto SULLA curva**, senza spostarla: chi tocca vuole prendere
  quella curva in quel punto, e farla saltare al dito sarebbe un movimento che nessuno ha chiesto.
- ⚠️ **I due estremi si muovono solo in verticale e non si tolgono**: una curva tonale deve dire
  che cosa fare di **ogni** tono, e un primo punto a mezza scala lascerebbe la prima metà senza
  risposta.
- ⚠️ **Il riquadro non è quadrato, ed è un compromesso dichiarato**: un grafico tonale si disegna
  quadrato, ma qui la scheda porta già due file di gettoni e i comandi della storia, e un quadrato
  largo quanto lo schermo si prenderebbe metà del palco, cioè l'immagine su cui si lavora.
- ⚠️ **Che cosa resta fuori**: un grafico a punti non si governa con un lettore di schermo, quindi
  là l'accessibilità si ferma alla descrizione. Chi lavora così ha i sei cursori della Luce, che
  coprono lo stesso mestiere con dei comandi che si annunciano.

⚠️⚠️ **E LA CURVA SI FOTOGRAFA QUANDO IL DITO SCENDE, DALLA `2.51`: RILEGGERLA A GESTO INIZIATO
DISTRUGGEVA L'IMMAGINE** (sua segnalazione, 2026-09-14: *se tocco e trascino la curva direttamente,
si crea una retta orizzontale che arriva fino al margine sinistro o destro*). Il gesto leggeva lo
stato **dopo** aver fatto nascere il punto, e fra le due righe non c'è nessuna ricomposizione:
quindi `rememberUpdatedState` rispondeva ancora la curva di prima, e l'indice del punto appena nato
cadeva esattamente sul suo ultimo indice. Da lì il gesto si credeva su un estremo, faceva nascere
il gemello della `2.40` e portava il bordo al livello del dito, cioè appiattiva la tabella dal
punto trascinato fino al margine.
- ⚠️⚠️ **CON LA CURVA A RIPOSO SUCCEDEVA SEMPRE**: là i punti sono due, quindi l'ultimo indice vale
  uno ed è esattamente il posto in cui `grow` infila il punto nuovo. Non era un caso limite, era
  ogni tocco sull'ultimo segmento.
- **La correzione è in due metà**: la fotografia della curva alla discesa del dito, e la condizione
  che un estremo lo sia **solo se il punto c'era già** (`near >= 0`). La seconda chiude anche il
  caso del tetto dei punti, dove `grow` risponde un indice qualunque.
- ⚠️⚠️ **E LA PROVA CHE LO PRESIDIA È NATA VERDE A VUOTO, PER LA TRAPPOLA DELLA `2.18`**: col
  gesto iniettato in un colpo solo, quel movimento se lo prende `settled` per pagare la soglia e a
  `drag` non arriva niente, quindi il punto nasceva e non si muoveva. Serve un **secondo** colpo, e
  la prima stesura misurava una curva rimasta la diagonale credendo di aver misurato il difetto.
- ⚠️ **Il caso 48 misura meno di quanto il suo nome dica**, e adesso si sa: con un colpo solo
  quello che cambia nel grafico è il **pallino** del punto nuovo, non la curva.

⚠️⚠️ **E IL COLORE MIRATO ARRIVA QUI, PERCHÉ È LA SUA RISPOSTA `curve` A `d-hsl-mirato`** (giro
della `2.21`). Il tasto **'Mirato'** arma una modalità in cui il dito lavora **sull'immagine**
invece che sui comandi, e i due moduli che lo offrono rispondono in due modi, che sono le loro due
nature: nelle **Curve** il tocco prende il punto al tono toccato e il trascinamento verticale lo
muove; nell'**HSL** il tocco **sceglie la fascia** del colore toccato.

⚠️⚠️ **MA DALLA `2.32` NELLE CURVE IL MIRATO NON C'È PIÙ, ED È LA SUA RISPOSTA `via` A
`d-mirato-curve`** (giro della `2.31`: *via, e si torna subito a zoomare/spostare l'immagine
toccandola*). Quindi il tasto vive nel **solo** HSL, e su un grafico che si governa col dito il
palco torna a fare quello che fa dappertutto: pinza, panoramica, doppio tocco e confronto.
- ⚠️ **Con lui cadono le due cose che distinguevano i due modi**: il gesto resta uno (si sceglie
  la fascia quando il dito si alza) e `Aim` non ha più tre stati da dichiarare. Chi legge qui che
  i moduli col mirato sono due sappia che era vero fino alla `2.31`.
- ⚠️ **Il tasto è un'icona dalla `2.32`**, come i sette gettoni dei moduli: il nome resta la
  descrizione parlata, che è quello che un lettore di schermo annuncia e quello che il banco
  cerca.

⚠️⚠️ **E LA LENTE VIVE IN UNA TELA SUA, DALLA `2.32`: È QUELLO CHE LA RENDE VELOCE** (riscontro
del giro della `2.31`, voce `geo-lente` non approvata: *continua ad essere lento e a non mostrare
l'immagine deformata*). Un `Canvas` si ridisegna quando cambia uno stato che il suo **disegno**
legge: fino alla `2.31` il tondo viveva dentro il disegno del palco, quindi ogni pixel di dito
invalidava la tela dell'immagine, cioè faceva rigirare tutto il conto dello sviluppo su tutta
l'anteprima (col Dettaglio acceso sono diciotto campioni per pixel) sessanta volte al secondo.
- ⚠️ **La seconda tela non porta nessun `pointerInput`**, quindi non entra nella hit-test e non
  può rubare i tocchi al palco, che è la trappola del `MenuGuard` della `1.70`.
- ⚠️ **Il conto della vista si rifà invece di passarselo**: tutte e due le tele leggono scala e
  spostamento nella propria passata di disegno, e un valore catturato porterebbe la geometria di
  un fotogramma prima.
- ⚠️⚠️ **L'ALTRA METÀ DEL SUO RISCONTRO NON SI RIPRODUCE, E IL BANCO DICE IL CONTRARIO**: dentro
  il tondo l'immagine **è** quella deformata, misurata a pixel (`SviluppoTest`, la lente col dito
  giù confrontata fra geometria ferma e mossa). ⚠️⚠️ **E LA STESSA MISURA DICE PERCHÉ LUI PUÒ NON
  VEDERLA: AL CENTRO DELL'IMMAGINE UNA DEFORMAZIONE NON C'È PER COSTRUZIONE** (toccando il centro
  cambiavano 103 pixel su 10.421, cioè l'1%). Il raddrizzamento, i due keystone e la distorsione
  tengono fermo il centro e crescono col raggio, e la lente ingrandisce sei volte: quello che si
  inquadra al centro è il punto in cui non succede niente. Chi lo prova tocchi **lontano dal
  centro**, e la voce di collaudo glielo dice passo passo.
- ⚠️⚠️ **NELL'HSL IL TRASCINAMENTO NON MUOVE NIENTE, ED È UNA SCELTA DICHIARATA**: là i cursori
  sono tre, e sceglierne uno per il dito sarebbe una decisione che lui non ha preso. Il giro della
  `2.23` lo chiede con `d-mirato-hsl`, e la sua risposta è **`niente`**.
- ⚠️⚠️ **QUINDI NELL'HSL NON C'È NESSUNA ATTESA E NESSUNA VIBRAZIONE, DALLA `2.30`, ED È IL SUO
  RISCONTRO** (giro della `2.29`, voce `geo-mirato`: *perché anche HLS mirato ha la vibrazione dopo
  1,2 secondi? È una cosa che ha senso solo per le curve*). L'attesa di `AIM_ARM_MS` è nata per
  **separare** la scelta dal trascinamento, e dove il trascinamento non muove niente separava una
  cosa sola: era un pedaggio con un segno in fondo che annunciava un potere che non arrivava. Là la
  fascia si sceglie quando il dito si alza, che è la strada che il codice già percorreva per chi
  non aspettava.
  - **Il palco lo sa da una funzione sola a tre stati** (`Aim`: spento, sceglie, trascina), e a
    rispondere è la tabella dei moduli: due predicati separati sarebbero due letture della stessa
    tabella, e il giorno che una cambia si contraddicono.
  - ⚠️ **Il banco non lo vede**, e va detto: l'attesa è un `delay` e il tempo del banco è fermo,
    quindi questa correzione si guarda sul telefono.
- ⚠️⚠️ **ARMATO, IL PALCO FA SOLO QUELLO**: pinza, panoramica, doppio tocco e confronto restano
  fermi finché il tasto è acceso. La via alternativa era un quinto gesto accanto agli altri quattro,
  e in quel rilevatore ognuno nasce dallo stesso dito che scende: cinque strade da distinguere in
  mezzo secondo, e la prima a sbagliare sarebbe quella che si usa di più.
- ⚠️ **Il tasto c'è nei soli moduli che hanno un bersaglio**, e non è una coincidenza: mirare vuol
  dire *questo colore qui*, e ha senso dove esiste qualcosa da puntare. La condizione si legge
  dalla tabella dei moduli, o passando a un terzo modulo la modalità resterebbe armata senza più un
  tasto per spegnerla.
- ⚠️⚠️ **IL COLORE LETTO È QUELLO DEL FILE E NON QUELLO CHE SI VEDE, e va detto**: quello che si
  vede è il risultato del conto, che vive sulla scheda grafica e non si può rileggere. Con
  un'esposizione già alzata di molto, il punto nasce un po' più in basso di dove il dito lo vede.
- ⚠️ **Un grigio non appartiene a nessuna fascia**, e `Mix.bandOf` risponde `-1` invece di dire
  'rosso': mandare il dito su una fascia che con quel pixel non c'entra farebbe parlare i tre
  cursori di un colore che là non esiste.

⚠️⚠️ **LA LENTE NON C'È PIÙ, DALLA `2.35`, ED È LA SUA RISPOSTA `via` A `d-mirino-resta`** (giro
della `2.34`, con la sua ragione scritta nella scelta: *Il colore mirato resta, ma senza il tondo
ingrandito: il dito sceglie e basta*, e nel giro prima *continuo a non essere sicuro che funzioni
come mi aspetto*). Quindi tutto il blocco qui sotto è **storia**: racconta una funzione che è
vissuta dalla `2.24` alla `2.34`.
- ⚠️ **Il colore mirato non perde niente**: sceglie la fascia del pixel sotto il dito come prima, e
  continua a leggerlo dal pezzo a risoluzione piena quando c'è. Quello che se n'è andato è il
  **disegno**, cioè il tondo, il suo mirino e la seconda tela che esisteva per ridisegnarlo senza
  rifare il conto dello sviluppo.
- ⚠️ **Con lui esce anche `tintOfPixel`**, che non aveva più nessun chiamante, e con lei le cinque
  prove del banco che misuravano la lente. Chi la volesse rimettere la ritrova nella storia git,
  misure comprese.

⚠️⚠️ **E DALLA `2.24` IL DITO PORTAVA UNA LENTE, PERCHÉ IL PIXEL CHE SI PRENDE È COPERTO DAL DITO**
(sua risposta a `d-mirato-hsl`, giro della `2.23`: **`niente`**, cioè nessun cursore si muove col
trascinamento, *ma serve un selettore con zoom e anteprima dei pixel campionati. Anche per le
curve, forse*). Un tondo ingrandito compariva sopra il dito, col mirino sul pixel campionato.
- ⚠️⚠️ **IL 'FORSE' LO SCIOLGO IO, E LA SCELTA È DICHIARATA: LA LENTE C'È NEI DUE MODULI.** Il
  gesto è lo stesso e il meccanismo è uno, quindi farla in un modulo solo vorrebbe dire due
  comportamenti per lo stesso tasto, a un tocco di distanza. La voce di collaudo gli chiede se
  toglierla dalle Curve, dove il bersaglio è un tono e non un colore.
- ⚠️⚠️ **MA DALLA `2.25` IL MIRINO SI TRASCINA, ED È SUA ISTRUZIONE** (riscontro del giro, voce
  `mirato-lente` non approvata: *funziona benissimo, ma dev'essere possibile trascinare il
  'mirino', perché difficilmente con il dito si azzecca il punto giusto al primo colpo*). ⚠️ **E
  con lei cade la nota della `2.24`**, che diceva il contrario (*si ancora al punto in cui il dito
  è sceso e non lo segue*, perché una lente che insegue mostrerebbe un colore diverso da quello
  preso): quell'argomento reggeva finché il pixel si prendeva all'istante del tocco, e adesso il
  pixel è quello sotto il dito **ora**, quindi la lente e la scelta dicono la stessa cosa.
  - ⚠️⚠️ **QUINDI IL TRASCINAMENTO NON MUOVE PIÙ LA CURVA FINCHÉ NON CI SI FERMA, E IL NUMERO È
    SUO** (nota su `d-lente-curve`: *visto che deve essere trascinabile, deve esserci un contatore
    'visuale': solo se mi fermo in un punto per 1,5 secondi poi il trascinamento su/giù agisce
    sulla curva*). Senza quella soglia i due gesti sarebbero lo stesso movimento, e scegliere un
    tono vorrebbe già dire spostarlo.
  - ⚠️ **Il conto riparte a ogni movimento e si azzera al distacco** (sua precisazione del
    2026-09-12), e 'si muove' vuol dire **oltre la soglia del tocco**: un dito appoggiato trema di
    un pixel o due, quindi con un conto che riparte a ogni evento l'armamento non arriverebbe mai.
  - ⚠️⚠️ **MA QUEL CONTATORE È DURATO UNA VERSIONE: DALLA `2.26` L'ATTESA È MUTA, DURA 1,2 SECONDI
    E FINISCE CON UNA VIBRAZIONE** (riscontro del giro della `2.25`, voce `mirino-trascina` non
    approvata: *il contatore visuale che deve ripartire ad ogni spostamento lo rende lentissimo (si
    aggiorna a scatti, inutilizzabile). Lascia stare il contatore: abbassa il tempo a 1,2 secondi ma
    non mostrare nulla: semplicemente, si sente una breve vibrazione allo scattare degli 1,2 secondi
    e da quel momento si può trascinare*). Il numero vive su `AIM_ARM_MS` e la vibrazione è quella
    di casa, `HOLD_BUZZ`, cioè quella del tocco lungo: una seconda da tarare non nasce.
    - ⚠️⚠️ **PERCHÉ COSTASSE TANTO SI LEGGE NEL CODICE, E NON È MISURATO SUL TELEFONO**: il
      progresso dell'arco si leggeva **dentro il disegno del `Canvas` del palco**, che è lo stesso
      che dipinge l'immagine con tutto il conto dello sviluppo. Quindi ogni fotogramma del
      contatore costava una passata intera dello shader sull'anteprima (col Dettaglio acceso sono
      diciotto campioni per pixel), sessanta volte al secondo **mentre il dito era fermo**, e
      ricominciava da capo a ogni movimento. Con l'attesa muta un dito fermo non produce nessun
      fotogramma.
    - ⚠️ **Non c'era un secondo nodo su cui spostarlo**: la lente si dipinge sopra l'immagine
      dentro quello stesso `Canvas`, quindi l'arco non si poteva ridisegnare da solo.
    - ⚠️⚠️ **QUINDI 'IL TEMPO E IL DISEGNO DALLA STESSA SORGENTE' È UNA NOTA SUPERATA**: era la
      ragione per cui l'attesa era un'animazione invece di un timeout, e con il contatore se ne va
      anche lei. Chi la ritrova in un commento vecchio sappia che oggi l'attesa è un `delay`.
    - ⚠️⚠️ **E DALLA `2.27` QUELLA VIBRAZIONE NON È PIÙ QUELLA DEL TOCCO LUNGO, PERCHÉ LA VOLEVA
      PIÙ FORTE** (nota su `d-armato-segno`, giro della `2.26`: *Vibrazione lievemente più forte*).
      Il tipo è `AIM_BUZZ`, che vive accanto a `HOLD_BUZZ` in `ActionPad.kt` ed è il gradino
      subito sopra: `ContextClick` vale **6** e `TextHandleMove` vale **9** nel bytecode di
      `PlatformHapticFeedbackType`, dove il nove è il colpetto più leggero della famiglia.
      - ⚠️⚠️ **LE COSTANTI SONO DUE PERCHÉ `HOLD_BUZZ` NON SI PUÒ TOCCARE**: quella è il colpetto
        di **ogni** pressione lunga dell'app, e la `1.21` l'ha resa più discreta su sua richiesta
        (*vorrei una vibrazione leggermente più breve ... morbida e discreta*). Alzarla qui
        vorrebbe dire rovesciare quella richiesta in venti punti che col mirino non c'entrano.
      - ⚠️ **E non `LongPress`**, che è il colpo pieno già scartato dalla `1.21`: 'lievemente' non
        lo giustifica.
  - ⚠️ **Un dito che si alza prima sceglie lo stesso**: l'attesa è nata per separare il
    trascinamento dalla scelta, non per mettere un pedaggio davanti alla scelta, e senza quella
    riga un tocco secco non farebbe più niente.
- ⚠️⚠️ **E L'ANELLO DEL MIRINO PORTA IL COLORE DELLA FASCIA, DALLA `2.25`** (stessa voce:
  *l'anello del 'mirino' deve essere più spessa e deve variare dinamicamente il colore per
  corrispondere a uno degli 8 colori standard, in modo che si capisca all'istante su cosa si
  agirà se ci si ferma lì*): il tratto raddoppia, e il tondo interno prende il colore del **centro**
  della fascia, dalla stessa funzione delle pastiglie. Su un grigio resta bianco, perché un grigio
  non appartiene a nessuna fascia.
  - ⚠️ **Il colore è della fascia e non del pixel**, ed è quello che l'anello deve dire: col colore
    del pixel direbbe una cosa vera e inutile, cioè quello che si vede già.
- ⚠️⚠️ **DENTRO LA LENTE IL FILTRO È A PIXEL INTERI, ED È IL SUO SCOPO**: là si guarda **quale**
  pixel si sta prendendo, e il filtro lineare che il Dettaglio pretende mescolerebbe i vicini
  proprio nel punto in cui bisogna distinguerli. Il pennello è **uno** per i due rettangoli
  (`pennello`, in `AdvancedEditorScreen.kt`), o la lente mostrerebbe un'immagine sviluppata in un
  altro modo.
- ⚠️⚠️ **E DALLA `2.28` QUELLO CHE INGRANDISCE È IL PEZZO LETTO DAL FILE, QUANDO C'È, CHE È LA SUA
  RISPOSTA `pieno` A `d-lente-pieno`** (giro della `2.27`): dalla `2.24` alla `2.27` mostrava
  l'anteprima, cioè la riduzione a 1600 pixel di lato su cui l'editor lavora, quindi ingrandita sei
  volte faceva vedere i pixel di quella e non quelli della fotografia. Adesso sotto il mirino c'è
  il file.
  - ⚠️⚠️ **E IL COLORE SI PRENDE DALLO STESSO PEZZO, CON LO STESSO CONTO**: sono **una** modifica e
    non due, perché disegnare il file e campionare l'anteprima (o il contrario) è esattamente il
    difetto che la nota della `2.24` esisteva per evitare, cioè vedere un pixel e prenderne un
    altro. I due conti vivono in `SharpPiece`, in `Regions.kt`, e chi tocca uno dei due chiamanti
    guardi l'altro.
  - ⚠️ **Quello che si guadagna è un colore più preciso**, e si vede dove il disegno è fine: un
    pixel dell'anteprima è la media di due o tre pixel veri, quindi puntando un capello o una riga
    di testo il colore preso poteva cadere in una fascia che con quel pixel non c'entrava.
  - ⚠️ **Il pezzo copre la sola finestra inquadrata**, quindi fuori di lì il colore torna a venire
    dall'anteprima: è il caso normale e non un ripiego, perché quello che si vede fuori da quella
    finestra è l'anteprima anche sul palco.
  - ⚠️ **Il banco misura i due conti e non il disegno**: montare il palco col pezzo vuole un file
    vero e un `BitmapRegionDecoder`, quindi che la lente mostri davvero i pixel del file si guarda
    sul telefono, e la voce di collaudo lo chiede.
- ⚠️ **L'ingrandimento si moltiplica a quello del palco invece di sostituirlo**: chi ha già
  ingrandito sta guardando da vicino, e una lente a scala fissa gliela mostrerebbe più piccola di
  quello che ha davanti.
- ⚠️ **Lo spegnimento vive in un `finally`**, per la stessa ragione del confronto della `2.17`: un
  rilevatore annullato non torna alla riga dopo, e la lente resterebbe in scena senza un dito.
- **La prova è `SviluppoTest`**, e misura quello che il banco può vedere: che il palco cambi
  disegno col dito giù e torni **identico** al rilascio, che la lente **segua** il dito, e che il
  colore dell'anello sia quello della fascia. ⚠️ **Non** vede se il pixel mostrato sia quello
  giusto, né il contatore e l'armamento: il conto vive sulla scheda grafica e il tempo del banco è
  fermo, quindi quelli si guardano sul telefono.
  - ⚠️⚠️ **IL BANCO HA IMPOSTO DUE COSE, E TUTTE E DUE LE HA DETTE LA CONTROPROVA.** La prova del
    trascinamento misura **dove** stanno i pixel cambiati e non quanti sono, perché fra due
    fotogrammi cambia anche il contatore, che cresce da sé: contandoli, restava verde col difetto
    rimesso. E si misura **in orizzontale**, perché là il palco di prova è alto una quarantina di
    pixel e un movimento verticale grande quanto un quarto di lui non arriva nemmeno alla soglia
    del tocco (misurato con una spia dentro il rilevatore: sei pixel contro sedici di soglia).

⚠️⚠️ **E 'DOVE SI HA LO SGUARDO' HA DOVUTO TRASLOCARE, PERCHÉ IL MIRATO VIVE SUL PALCO**: il
modulo, la fascia e il canale stavano dentro la scheda, e il gesto che tocca l'immagine deve sapere
quale modulo si sta guardando. Adesso vivono in un oggetto solo (`Gaze`) che la schermata passa a
tutti e due. ⚠️ **Non entrano nella storia dei passi**, che era la ragione per cui stavano fuori dal
modello, e non è cambiata.

⚠️⚠️ **LA FILA DEI MODULI SCORREVA DALLA `2.23`, E SENZA QUELLA RIGA IL PALCO SPARIVA**: col quinto
gettone i nomi non entravano più nella larghezza, quindi ognuno andava a capo dentro la propria
pastiglia e la fila cresceva in altezza; la scheda è alta quanto il suo contenuto, e il palco si
prende quello che resta. ⚠️ **Il banco l'ha misurato come un'immagine alta zero pixel**, cioè
l'editor senza più niente da guardare, e non è arrivato a lui: è il caso per cui le prove sui pixel
esistono. ⚠️⚠️ **DALLA `2.31` LO SCORRIMENTO NON C'È PIÙ**, perché i gettoni sono icone e ci stanno
tutti: il perché, e perché lo scorrimento era un rimedio e non una scelta, vivono in § '✂️ Il modulo
Ritaglio, e la fila che è diventata di icone'.

⚠️⚠️ **E DALLA `2.33` LA SCHEDA NON CAMBIA PIÙ ALTEZZA PASSANDO DA UN MODULO ALL'ALTRO, ED È SUA
RICHIESTA** (2026-09-13: *voglio che la bottomsheet dell'editor completo sia sempre alta uguale:
non deve ballare da un modulo all'altro*). Fino alla `2.32` la scheda si dimensionava sul proprio
contenuto e il palco prendeva quel che restava, quindi ogni gettone toccato cambiava la misura
dell'immagine su cui si lavora.
- ⚠️⚠️ **L'ALTEZZA SI MISURA E NON SI SCRIVE**: `SteadyBody` compone ogni corpo una volta, lo
  misura e tiene il massimo, perché quanto sia alta una riga di cursori dipende dal corpo del
  carattere, dalla sua scala e dalla lingua. Un numero in `dp` sarebbe giusto su un telefono e
  sbagliato sul prossimo, e per accorgersene servirebbe che qualcuno guardasse.
- ⚠️⚠️ **LE COPIE MISURATE VIVONO UN GIRO SOLO, E LA PRIMA STESURA LE TENEVA IN SCENA**: con un
  `SubcomposeLayout` che le misura a ogni passata quei corpi **restano nell'albero**, quindi un
  lettore di schermo annuncia i cursori di sette moduli e il banco li conta; sette prove sono
  diventate rosse in un colpo. Adesso la misura si scrive in uno stato e la ricomposizione che ne
  segue le porta via, e mentre ci sono non hanno semantica.
- ⚠️ **Il costo si vede e si dichiara**: il modulo più alto è la Luce (sei cursori) e il più corto
  l'HSL, e fra i due ballavano settanta punti su una scena di prova. Cioè il palco non si allunga
  più nei moduli corti, e non si accorcia in quelli alti.
- ⚠️⚠️ **E IL BANCO HA TROVATO UN DIFETTO VERO MENTRE LO MISURAVA**: con un palco più corto, tirare
  una squadretta faceva **cadere l'app**, perché un riquadro più piccolo del lato minimo dà a
  `coerceIn` un intervallo vuoto. Non dipende dalla scheda: lo stesso succede con un'immagine molto
  allungata, e adesso i quattro limiti passano da `within`, che un intervallo vuoto non lo può
  avere.

⚠️⚠️ **E DALLA `2.35` QUELL'ALTEZZA SI PAGA MENO, PERCHÉ I MODULI ALTI SI SONO STRETTI E I CORTI
RESPIRANO** (sua richiesta, 2026-09-13: *cerca di mantenere tutto più compatto: riduci dimensioni
del testo, padding, ecc. per i moduli che occupano più spazio verticale, e fa' respirare di più
quelli ristretti inutilmente*). Sono le due metà di una cosa sola: l'altezza comune la detta il
modulo più alto, quindi stringere lui allunga il palco **in tutti e sette**, e quello che avanza
nei corti va distribuito invece di restare in fondo.
- ⚠️⚠️ **IL NUMERO CHE PAGA È L'ALTEZZA DI UNA RIGA DI CURSORE** (`DIAL_ROW`, da 40 a 36 punti):
  vale **sei volte** nella Luce, che è il modulo che detta la misura, e il conto è che sei righe
  passano da 240 a 216 su una scheda che ne vale circa 360. ⚠️ **Sotto non si scende**: il
  bersaglio resta largo tutta la riga, ma l'altezza è già sotto i 48 punti di Material.
- ⚠️ **Il corpo del nome scende di un gradino e compra LARGHEZZA**, non altezza: la stessa parola
  entra in una colonna più stretta, e i punti che avanzano vanno alla barra, cioè alla sola parte
  della riga con cui si lavora. In più allontana il caso in cui un nome lungo va a capo e fa
  crescere la sua riga.
  - ⚠️⚠️ **E DALLA `2.37` QUELLA COLONNA SI MISURA INVECE DI ESSERE UN NUMERO, ED È IL PUNTO B DEL
    SUO CAMPO LIBERO** (giro della `2.36`: *'Mascheratura' deve stare per esteso nel modulo
    Dettagli, senza andare a capo: aumenta la larghezza quanto basta, oppure fallo diventare
    'Maschera'*). Le due vie che ha dato hanno lo stesso difetto, ed è il conto a dirlo: **'quanto
    basta' non è un numero**. In Roboto a corpo pieno quella parola chiede 79 punti su 84, cioè
    entra con un margine del 6% e va a capo appena il testo cresce di un decimo, che è quello che
    succede alzando la dimensione dei caratteri di sistema; e non è il caso peggiore, perché su
    ventotto lingue **quattordici** nomi superano quegli 84 punti già a scala uno: il più largo
    dei latini è il francese 'Hautes lumières', che ne chiede 93, e il russo 'orizzontale' arriva a
    95.
  - ⚠️ **A misurare è il telefono** (`rememberTextMeasurer`), quindi la colonna cresce insieme al
    testo; un conto fatto in casa con le metriche di Roboto sarebbe di nuovo un numero. Fra un
    minimo (gli 84 di prima, o in una lingua dai nomi corti la barra partirebbe da un altro punto)
    e un tetto (oltre il quale la colonna si mangerebbe la barra, e allora si torna al nome su due
    righe, che è il male minore).
  - ⚠️ **Si misurano i nomi di TUTTI i moduli**: una colonna che si dimensionasse sul modulo aperto
    cambierebbe larghezza a ogni gettone toccato, cioè rifarebbe in orizzontale il ballo che la
    `2.33` ha tolto in verticale.
  - ⚠️⚠️ **NON HA UNA PROVA DEL BANCO, E VA DETTO**: con la grafica di Robolectric i caratteri sono
    più stretti di quelli del telefono, quindi là la misura cade sempre sul minimo e una prova
    sarebbe verde con e senza la correzione. È il caso dichiarato in § '🧪 Quando si scrive una
    prova, e quando no'.
  - ⚠️⚠️ **E DALLA `2.38` LE TRE COLONNE NON SI TOCCANO PIÙ, ED È LA SUA SEGNALAZIONE CON
    SCHERMATA** (2026-09-13: *lascia più spazio per i testi ... più un po' di aria, perché al
    momento è tutto troppo attaccato*). La colonna è larga quanto il nome più largo di tutti i
    moduli, quindi **proprio quel nome** arrivava a filo del tondo, che a riposo ha il centro sul
    bordo della barra: il testo non era tagliato, ma si leggeva incollato al comando. Adesso fra
    le tre colonne c'è `KNOB_GAP`, e i ventiquattro punti li paga la **barra**, che è la sola a
    non avere una larghezza dichiarata.
    - ⚠️⚠️ **IL NOME PUÒ ANDARE A CAPO, E LA SUA CONDIZIONE È UN NUMERO** (*'colore' può anche
      andare a capo, a patto che lo slider rimanga alla stessa distanza da quello sopra*): due
      righe di `bodySmall` valgono **32 punti** contro i 36 di `DIAL_ROW`, quindi la riga non
      cresce e il passo fra due cursori resta quello. La terza riga la esclude `maxLines`.
    - ⚠️ **Oltre il 150% di scala dei caratteri quella misura cade**, e quella riga si allunga: si
      dichiara invece di chiuderla con un'altezza fissa, perché là il testo sborderebbe sulla riga
      vicina invece di essere tagliato, che è peggio del passo diverso.
    - ⚠️ **La prova misura il fatto e non il numero**: che fra il nome e la barra resti dell'aria,
      con una soglia più bassa della costante, così un ritocco a `KNOB_GAP` non la fa diventare
      rossa mentre il comportamento è ancora giusto. Controprovata togliendo il distacco: l'aria
      misurata è **zero**, cioè il nome e la barra si toccano.
- ⚠️⚠️ **IL RESPIRO HA UN TETTO, E SENZA DI LUI SAREBBE PEGGIO DEL VUOTO**: nel Ritaglio avanzano
  un centinaio di punti su tre blocchi, e divisi in parti uguali darebbero mezzo centimetro fra una
  fila di tasti e l'altra, cioè tre isole invece di un pannello. Col tetto ognuno prende il suo
  respiro e quello che resta **centra** il blocco. Il pezzo è `Breathe`, un `Arrangement` in
  `AdvancedEditorScreen.kt`, e siccome è Kotlin puro il banco lo misura chiamandolo.

⚠️ **Che cosa il banco misura e che cosa no** (`SviluppoTest`, più `ContoTest` per il programma):
che la curva a riposo sia l'identità **esatta**, che la spline non oltrepassi e che un tratto piatto
resti piatto, che la tabella componga il canale sotto il composito, che gli estremi non si muovano
in orizzontale e non si tolgano, che il modulo porti i quattro canali e nessun cursore, che il
mirato si offra nei soli due moduli, e i due conti che fa su un pixel; dalla `2.51` anche che un
punto nato in mezzo **non** trascini con sé il bordo, e quello si misura a pixel. **Non** vede i
pixel che ne escono, né il tocco lungo che toglie un punto: quelli si guardano sul telefono, e la
voce di collaudo li chiede.

## 📐 Il modulo Geometria, e il conto che non passa dallo shader

⚠️⚠️ **È IL SESTO MODULO DELL'EDITOR COMPLETO, DALLA `2.29`, E PORTA TUTTI E CINQUE I COMANDI IN
UNA VERSIONE SOLA: È LA SUA RISPOSTA `intera` A `d-geo-quanto`** (giro della `2.28`). La domanda
chiedeva se spezzarla in due giri, col raddrizzamento e l'aspetto davanti e le tre correzioni
dell'obiettivo dietro; la risposta è di provarla come un pannello finito. I cursori sono
raddrizzamento, proporzioni, orizzontale, verticale e distorsione.

⚠️⚠️ **GLI ALTRI CINQUE MODULI DICONO DI CHE COLORE È UN PIXEL, QUESTO DICE DOVE VA, E PER QUESTO
NON PUÒ VIVERE NELLO SHADER.** La regola di `Adjust.kt` manda il conto in AGSL perché anteprima e
salvataggio leggano la stessa matematica; qui non si può, e la ragione è **misurata**: il
salvataggio lavora a tessere, e una tessera legge la propria porzione di sorgente, mentre una
deformazione fa leggere a un pixel di uscita un punto che può stare dall'altra parte della
fotografia. Con la geometria dentro lo shader, ogni tessera leggerebbe il pezzo sbagliato.
- ⚠️⚠️ **MA LA REGOLA DI FONDO RESTA SODDISFATTA, e la distinzione è la stessa delle Curve**:
  quella regola non dice 'il conto vive in AGSL', dice che la **stessa matematica non si scrive
  due volte**. Qui il conto è scritto una volta, in Kotlin (`Geometry.kt`), e i lettori sono due:
  il palco e il salvataggio passano tutti e due da `Warp.draw`, con la stessa griglia.
- ⚠️ **E in più il banco lo può misurare**, che con l'AGSL non succede: l'andata e il ritorno, la
  scala di copertura e la griglia sono Kotlin puro, quindi si provano davvero.

⚠️⚠️ **SI DISEGNA COME UNA MAGLIA DI TRIANGOLI E NON COME UNA MATRICE**: quattro comandi su cinque
sarebbero una matrice 3x3, che Android sa applicare da sé, ma la distorsione **curva le righe** e
nessuna matrice lo sa fare. Con una maglia fitta i cinque comandi passano dalla stessa strada, e
non esistono due meccanismi che possono divergere.
- ⚠️ **A geometria ferma resta il rettangolo di sempre**, e non è un'ottimizzazione: mille
  triangoli per disegnare un rettangolo sarebbero mille occasioni di una cucitura che a rettangolo
  non esiste.
- ⚠️ **Il costo dichiarato è l'approssimazione**: dentro una cella la deformazione è lineare,
  quindi la sola curvatura si vede a tratti. Con trentadue celle per lato lo scarto resta sotto il
  pixel, ed è la stessa via di `drawBitmapMesh`.

⚠️⚠️ **LA SCALA DI COPERTURA SI RICAVA E NON È UN SESTO CURSORE**: senza di lei un raddrizzamento
lascerebbe quattro cunei vuoti agli angoli, che è quello che si vede in ogni editor che quel conto
non ce l'ha. Si misura sul **contorno** e non sui quattro angoli, perché il punto più rientrato
dipende dal comando: con un keystone è un angolo, con la distorsione a barile è il mezzo di un
lato.

⚠️⚠️ **MA IL SUO RITAGLIO NON SI VEDEVA, E DALLA `2.30` SÌ** (riscontro del giro della `2.29`, voce
`geo-dritto` non approvata: *anche il ritaglio per non lasciare angoli vuoti dovrebbe vedersi in
tempo reale*). La copertura c'era e faceva il suo lavoro; a mancare era un confine: sul palco la
maglia si disegnava **senza ritaglio**, quindi ingrandita usciva dal riquadro dell'immagine e
andava a finire sul fondo, mentre il salvataggio disegna dentro un bitmap grande quanto
l'originale, cioè taglia. Si vedeva una cosa e se ne salvava un'altra.
- ⚠️ **Il riquadro da ritagliare è quello dell'immagine e non quello del palco**: il `clipToBounds`
  del palco c'era già e non bastava, perché il palco è più grande.
- ⚠️ **La prova guarda un pixel del FONDO accanto all'immagine**, e ⚠️⚠️ **il banco ha imposto due
  cose**: al modulo si passa **prima** dello scatto a riposo (la Geometria ha un cursore in meno
  della Luce, quindi la scheda si accorcia, il palco si allunga e l'immagine cresce: si
  confrontavano due scene diverse), e il cursore da muovere è **'Proporzioni'** e non il
  raddrizzamento, che alla riga di mezzo sporge di un decimo di pixel.

⚠️⚠️ **E IL PERNO DI UN KEYSTONE È LA RETTA DI MEZZO, DALLA `2.30`** (stesso riscontro: *dovrebbero
avere come perno una retta che rimane al centro, anziché un appoggio laterale*). Fino alla `2.29` i
due keystone erano **una** divisione prospettica sola, che è l'omografia da manuale e manda rette in
rette, ma non è centrata: il lato che si apre andava a +54% e quello che si stringe a -26%, quindi
il trapezio scivolava tutto da una parte e l'immagine sembrava appoggiata a un bordo invece di
ruotare attorno a sé.
- **Adesso ogni asse è una Möbius sul proprio lato**, e i due keystone si applicano in sequenza: i
  bordi non si muovono, il trapezio è isoscele, il centro dei quattro vertici resta il centro, e
  resta un'omografia. Il conto, e le due varianti scartate, vivono su `Warp.SLANT`.
- ⚠️ **Deformare la sola coordinata trasversale sarebbe stato più corto e CURVA LE VERTICALI**:
  quella mappa manda una retta in un'iperbole, che in un comando di prospettiva è peggio del difetto
  che toglie.
- ⚠️ **Il prezzo è dichiarato**: su un 4:3 col cursore a fondo corsa la copertura passa da 1,26 a
  1,34, perché un trapezio centrato rientra da tutte e due le parti invece che da una sola.
- ⚠️ **Il numero non cambia**, ed è la sua risposta `bene` a `d-geo-corsa`: cambia come si
  distribuisce, non quanto pesa.

⚠️⚠️ **IL FONDO CORSA DELLA DISTORSIONE È IL TETTO OLTRE IL QUALE IL DISEGNO SI RIPIEGA, E LO HA
TROVATO IL BANCO**: la mappa radiale è invertibile finché `1 + 3k r²` resta positivo, e il raggio
più grande, in coordinate isotrope, è quello dell'angolo di un'immagine **quadrata**, cioè radice
di due. Là il tetto vale `-0,167`, quindi il `-0,25` della prima stesura stava oltre e agli angoli
l'inversa non esisteva. Il numero di oggi è `0,12`, con la misura scritta sulla costante.
- ⚠️ **Il palco non dava nessun errore**, ed è la ragione per cui questo difetto è interessante: a
  sbagliare era il **colore mirato**, cioè un pixel preso da un'altra parte della fotografia. A
  prenderlo è stata la prova dell'andata e ritorno, non una lettura del codice.

⚠️⚠️ **IL COLORE MIRATO PASSA DALLA MAPPATURA INVERSA, E LA LENTE CON LUI**: il dito tocca
l'immagine **deformata** e il colore vive prima della deformazione, quindi `colourAt` chiede a
`WarpPlan.back` da dove viene il punto toccato.

⚠️⚠️ **MA LA LENTE INQUADRA IL PUNTO TOCCATO E MOSTRA L'IMMAGINE DEFORMATA, DALLA `2.30`, E LA NOTA
DELLA `2.29` È ROVESCIATA** (riscontro del giro, voce `geo-mirato` non approvata: *Il punto non è
quello giusto, si vede l'immagine prima della distorsione*). Quella nota diceva che la lente si
sposta sul punto **sorgente**, perché dentro il tondo l'immagine si disegnava non deformata e
centrarla sul dito avrebbe fatto vedere un pixel e prenderne un altro. Il conto tornava, e quello
che si vedeva era un'altra immagine: la fotografia com'era prima della geometria.
- **Adesso la maglia c'è anche dentro il tondo**, costruita sul riquadro della lente: il conto è
  **invariante per similitudine**, quindi la deformazione è la stessa scalata e il punto toccato
  cade al centro del mirino per costruzione, non per un aggiustamento.
- ⚠️ **Il colore non cambia strada** e continua a venire da `WarpPlan.back`: i due dicono la stessa
  cosa, perché il pixel che si vede nel punto deformato **è** il pixel sorgente.
- ⚠️ **Anche là dentro si ritaglia**, come sul palco: quello che si vede nel mirino è quello che il
  salvataggio scriverà, ingrandito, bordo tagliato compreso.
- ⚠️ **Il banco misura il conto e non il disegno**: che il riquadro ingrandito attorno al dito posi
  il pixel toccato al centro del tondo è Kotlin puro, e la controprova (il riquadro attorno al punto
  sorgente, cioè la strada della `2.29`) vive dentro la prova stessa.

⚠️ **Col modulo mosso il pezzo a risoluzione piena non si legge, e si dichiara**: `sharpAsk` ricava
la porzione inquadrata dal rettangolo in cui l'immagine **intera** è disegnata, e con la
deformazione quel rettangolo non dice più dove finisce un pixel. Quello che si perde è l'anteprima
nitida mentre si raddrizza, e sotto c'è sempre l'immagine intera.

⚠️ **I due keystone si chiamano col proprio asse e non 'prospettiva'**: sono tutti e due una
correzione di prospettiva, quindi chiamarne uno 'Prospettiva' direbbe che l'altro è un'altra cosa.
Lightroom li chiama 'Verticale' e 'Orizzontale', e qui il campo del codice porta la stessa parola
che si legge nel telefono.

⚠️⚠️ **E DALLA `2.50` LA GEOMETRIA HA UN COMANDO CHE NON È UN CURSORE: 'ANGOLI', ED È SUA
RICHIESTA** (2026-09-14: *il modulo 'Geometria' deve includere uno strumento 'Angoli', che
permette di deformare l'immagine trascinando un angolo per volta, sempre con un auto-ritaglio per
non lasciare parti vuote*). I cinque cursori dicono **quanto**; questo dice **dove va quel
vertice**, che è una cosa che nessuna manopola può esprimere: è la stessa distanza che c'è fra un
cursore e il grafico delle Curve.
- ⚠️⚠️ **È UN'OMOGRAFIA DA QUADRATO A QUADRILATERO, E IL CONTO VIVE ACCANTO AGLI ALTRI**: la mappa
  di Heckbert porta il quadrato unitario sui quattro angoli spostati, e la sua inversa è la
  matrice aggiunta, che serve al colore mirato come per gli altri comandi. Entra come quinto
  passo di `Warp.map`, quindi il ritaglio di copertura, la maglia e la lettura inversa la
  prendono per costruzione invece di essere una seconda strada.
- ⚠️⚠️ **UNA MANIGLIA NON PUÒ ANDARE OVUNQUE, E LA GUARDIA È LA CONVESSITÀ**: con un vertice
  tirato oltre la diagonale il quadrilatero si ripiega, e la mappa non è più invertibile, cioè il
  colore mirato prenderebbe un pixel da un'altra parte della fotografia (è lo stesso difetto che
  il banco aveva trovato sul fondo corsa della distorsione). Il movimento si accetta solo se i
  quattro prodotti vettoriali restano dello stesso segno, e il guinzaglio è `PULL`.
- ⚠️⚠️ **A STRUMENTO ARMATO L'IMMAGINE SI RIMPICCIOLISCE, E LA SCALA NON DIPENDE DAGLI ANGOLI**:
  le quattro maniglie vivono **fuori** dall'immagine, quindi senza aria non si potrebbero
  prendere. La scala di lavoro è una frazione fissa del `fit` calcolato **senza** gli angoli, e
  non del `fit` vero: legata a quello, ogni pixel di dito muoverebbe anche la cornice, e la
  maniglia resterebbe incollata al bordo mentre la si tira.
- ⚠️ **Il riquadro di quello che si salva si vede dentro**, cioè il rettangolo della copertura
  disegnato sopra l'immagine rimpicciolita: senza, si tirerebbe un angolo senza sapere quanto il
  ritaglio automatico sta mangiando.
- ⚠️ **Il palco fa solo quello finché è armato**, come nel Ritaglio e nel colore mirato: pinza,
  panoramica, doppio tocco e confronto restano fermi, perché il dito ha già un mestiere.

⚠️⚠️ **E DALLA `2.85` 'ANGOLI' FUNZIONA ANCHE CON UN TAGLIO APPLICATO, E PRIMA NO** (seconda delle
sue tre segnalazioni del giro della `2.83`: *una volta applicato un ritaglio, non sono in grado di
modificare la geometria con Angoli*). Le cause erano due, e il banco le ha separate.
- ⚠️⚠️ **LA PRIMA È CHE LA VISTA ARMATA INQUADRAVA LA PORZIONE**: le maniglie sono gli angoli
  dell'immagine **intera**, e con 'Applica' il palco la ingrandisce quanto serve a far riempire lo
  schermo dalla porzione, quindi con un taglio piccolo le maniglie finivano fuori dallo schermo.
  Adesso, armato lo strumento, il palco mostra l'immagine intera rimpicciolita, e il taglio torna
  appena si spegne.
- ⚠️ **La seconda è il gesto che leggeva il palco del primo tocco**, ed è la causa della terza
  segnalazione: vive in § '✂️ Il modulo Ritaglio, e la fila che è diventata di icone'.
- ⚠️ **Il riquadro tenuto dice anche il taglio**, perché con l'immagine intera in scena un velo
  fermo alla copertura prometterebbe più di quanto il file porterà. Il taglio è `Look.crop`, cioè
  quello che il salvataggio taglia davvero, e non quello applicato.

⚠️ **Che cosa il banco misura e che cosa no** (`SviluppoTest`): che a riposo il conto sia
l'identità **esatta** e la maglia coincida con la griglia, che l'andata e il ritorno si disfacciano
a vicenda con tutti e cinque i cursori a fondo corsa, che la copertura non lasci bordi vuoti (con
la controprova a scala uno dentro la prova stessa), e che il sesto modulo porti i suoi cinque
cursori e azzeri solo i propri; dalla `2.30` anche che un keystone tenga il **centro** e apra i due
lati alla pari, che l'immagine deformata non esca dal proprio riquadro (e questa guarda i pixel), e
il conto su cui la lente si regge; dalla `2.85` anche che con un taglio applicato le maniglie si
prendano e che il riquadro tenuto dica il taglio (i casi 70 e 71). ⚠️ **Il caso 70 non vede il
gesto che legge il palco del primo tocco**, e la controprova lo ha detto: quel gesto ignorava il
taglio per caso, esattamente come adesso fa la vista armata, quindi senza quella correzione la
prova resta verde. A lei pensano i casi 68 e 69. **Non** vede i pixel deformati: che un orizzonte venga dritto e
che una facciata si raddrizzi si guardano sul telefono, e la voce di collaudo lo chiede.
- ⚠️⚠️ **IL SESTO GETTONE ANDAVA RAGGIUNTO SCORRENDO, E SENZA QUELLA RIGA LA PROVA MENTIVA**: fino
  alla `2.30` la fila scorreva in orizzontale, quindi col sesto nome la pastiglia cadeva fuori dalla
  larghezza del banco; il tocco non dava nessun errore e non cambiava modulo, e si contavano i sei
  cursori della Luce credendo di guardare la Geometria. ⚠️ **Dalla `2.31` la fila è di icone e non
  scorre**, quindi il gettone si cerca per **descrizione** e lo scorrimento è uscito dalle prove.

## ✂️ Il modulo Ritaglio, e la fila che è diventata di icone

⚠️⚠️ **DALLA `2.35` L'ORDINE DELLA FILA È UN ALTRO, E IL RITAGLIO È IL MODULO APERTO DI FABBRICA**
(sua istruzione, 2026-09-13: *voglio cambiare anche l'ordine dei moduli: per impostazione
predefinita, da sinistra a destra, dev'essere: dettagli, curve, geometria, ritaglio (nuovo default
attivo all'avvio), luce, contrasto, HSL*). Fino alla `2.34` la fila cominciava dal Ritaglio e si
apriva sulla Luce.
- ⚠️⚠️ **'CONTRASTO' È IL MODULO COLORE, E LA LETTURA È PER ESCLUSIONE**: i moduli sono sette e lui
  ne nomina sette, sei col loro nome; quello che resta è il Colore, e nessun altro può stare in
  quel posto. La voce di collaudo glielo chiede in chiare lettere.
- ⚠️ **Chi ha già riordinato la fila tiene il suo ordine**, e non è un difetto: `MOD_KEYS` è il
  valore di fabbrica, e `padOrderOf` lo usa solo per quello che l'archivio non dice.
- ⚠️ **I due elenchi si riordinano insieme** (la tabella dei moduli e `MOD_KEYS`): non cambia
  niente per chi usa l'app, ma le note che dicono 'nell'ordine in cui la fila li disegna'
  resterebbero false.

⚠️⚠️ **ERA IL PRIMO MODULO DALLA `2.31`, ED ERA IL SUO ORDINE DI ALLORA** (campo libero del giro
della `2.29`: *`Geometria` dev'essere il secondo modulo; il primo dev'essere `Ritaglio` (più o meno
ciò che fa già l'editor semplice). Il terzo (ma attivo di default) 'Luce', e gli altri di seguito
nell'ordine attuale*). I due moduli che non parlano di colore venivano per primi perché sono le
domande che si fanno per prime davanti a una fotografia: che cosa ci sta dentro, e se sta dritta.
- ⚠️⚠️ **E FINO ALLA `2.34` APERTO DI FABBRICA ERA IL TERZO, CIOÈ DUE COSE DIVERSE**: la fila è
  l'ordine in cui si lavora, l'apertura è dove si lavora quasi sempre. Dalla `2.35` le due cose
  coincidono nel Ritaglio, ma restano due letture: l'indice vive in `LOOK_FIRST` e si **ricava**
  dall'elenco, quindi chi sposta un modulo si ritrova l'apertura giusta senza toccare altro.
- ⚠️ **Il posto nella fila e il posto nella catena non coincidono più**: il conto del Ritaglio si fa
  per ultimo (si taglia quello che il resto ha prodotto) e quello della Geometria dopo lo shader,
  mentre nella fila vengono per primi.

⚠️⚠️ **QUELLO CHE FA È QUELLO CHE FA L'EDITOR DI CASA, E IL CODICE È LO STESSO**: le quattro
squadrette, il velo intorno, i terzi, la presa del dito e il lato minimo vivono in
`EditorScreen.kt` (`cropOverlay`, `grabbed`, `dragged`, `cropBox`, `cropFractions`), e questo palco
li **chiama**. Due disegni dello stesso comando divergerebbero al primo ritocco, e chi lo vedrebbe
per primo è lui, che i due editor li apre dalla stessa immagine.
- ⚠️⚠️ **E DALLA `2.32` CI SONO ANCHE I FORMATI E LE DUE CENTRATURE, ED È LA SUA RISPOSTA
  `formati` A `d-crop-formati`** (giro della `2.31`: *portali, con le centrature*). Fino alla
  `2.31` il rettangolo era libero, e senza una forma scelta le due centrature non avrebbero avuto
  niente da centrare: con lei ce l'hanno, quindi la fila della posa passa da tre tasti a cinque,
  cioè agli stessi dell'editor di casa.
  - ⚠️⚠️ **'ORIGINALE' È UNA FORMA IN PIÙ, ED È SUA RICHIESTA** (2026-09-13: *tra i vincoli di
    proporzione dev'esserci anche 'Originale', ma scelta di default resta 'Libera'*). È la sola
    il cui rapporto **non è scritto nel codice**: lo porta l'immagine, quindi in `Shape` il
    rapporto di ogni forma è una **funzione** del riquadro invece di una costante. Nel verso
    naturale dell'immagine non taglia niente per costruzione, che è la proprietà da cui dipende
    il senza perdita.
  - ⚠️⚠️ **E DALLA `2.35` VANNO A CAPO, TRE PER RIGA, ED È IL SUO RISCONTRO** (giro della `2.34`,
    voce `crop-fila` non approvata: *In realtà, come ho scritto in chat, non serve. Anzi, devono
    occupare più spazio*, e in chat *i chip delle proporzioni possono stare anche su 3 righe*).
    ⚠️⚠️ **LA RAGIONE DELLA FILA UNICA ERA GIÀ CADUTA CON LA `2.33`, E NESSUNO DEI DUE SE N'ERA
    ACCORTO**: *preferisco lo spazio per l'immagine* vale dove la scheda si dimensiona sul proprio
    contenuto, e da quando è alta quanto il modulo più alto una riga in più nel Ritaglio **non
    toglie un pixel al palco**, perché là lo spazio avanza comunque. Con lei cade la domanda
    `d-crop-corpo`, e la sua risposta lo dice: *Non serve*.
    - ⚠️ **Solo nell'editor completo**: di là la scheda è alta quanto il suo contenuto, quindi la
      fila resta una e col corpo ridotto. Lo dichiara il parametro `wrap` di `ShapeRow`, che non ha
      un valore di serie 'a capo'.
    - ⚠️ **Tre celle per riga e non 'quelle che ci stanno'**: con un flusso libero le celle si
      dimensionano sul testo, quindi la riga finirebbe con un vuoto diverso in ogni lingua; con tre
      colonne uguali le due righe si leggono come una griglia e la parola più lunga delle ventotto
      lingue sta comoda per costruzione. Con lei torna il **corpo pieno**, perché il gradino più
      piccolo serviva a non troncare in una riga da sei.
  - ⚠️⚠️ **E DALLA `2.80` LE DUE RIGHE SONO 'LE PAROLE' E 'I NUMERI', COI DUE VERSI A ICONA
    ACCANTO ALLE PAROLE, ED È IL SUO PUNTO `crop-giu`** (riscontro del giro dalla `2.75` alla
    `2.77`, col mockup: *le proporzioni numeriche tutte in una riga*, e *'Orizzontale' e
    'Verticale' diventano icone a destra di 'Originale'*). Fino alla `2.79` erano due righe da tre
    celle uguali, quindi i quattro numeri stavano a cavallo delle due, e i due versi vivevano in
    una **terza** riga scritta a parole.
    - ⚠️⚠️ **QUELLA TERZA RIGA SE NE VA, ED È LA COSA CHE PAGA IL RESTO DEL GIRO**: il Ritaglio si
      accorcia di `CHIP_TALL` più il suo distacco, e senza quel guadagno l'aria del punto D
      farebbe di lui il modulo più alto, cioè alzerebbe la scheda **in tutti e nove**
      (§ `SteadyBody`).
    - ⚠️ **I due versi restano di Material**: `CropPortrait` e `CropLandscape` hanno il contorno
      esterno già stondato e gli spigoli vivi nel solo buco interno, quindi col criterio stretto
      l'arrotondamento a 0,4 li lascia a **zero pixel** e vince Material (§ '🖌️ Come entra un
      disegno'). Non nasce nessun file in `res/`.
    - ⚠️ **Il nome vive nella descrizione parlata**, come nei gettoni dei moduli: è quello che un
      lettore di schermo annuncia e quello che il banco cerca, e nessuna stringa nasce o resta
      orfana.
    - ⚠️ **I quattro numeri si dividono la loro riga in parti uguali**, perché non si traducono
      mai; le due parole prendono quello che avanza accanto alle due celle dei versi, larghe
      quanto un bersaglio di Material.
  - ⚠️⚠️ **LE FILE ERANO DIVENTATE DUE CON LA `2.32`, E DALLA `2.34` TORNANO UNA: È LA SUA
    RISPOSTA `una` A `d-crop-righe`** (giro della `2.32`: *rimettile su una fila sola*, con la
    ragione scritta nella scelta: *anche a costo di troncare le due parole: preferisco lo spazio
    per l'immagine*). La `2.32` le aveva divise perché con 'Originale' le parole vere erano
    diventate due e su 360dp si troncavano; la domanda gli chiedeva se quella riga da 32dp valesse
    lo spazio che toglieva al palco, e la risposta è no. ⚠️ **Quella risposta è durata una
    versione**, e il perché è nel blocco qui sopra.
    - ⚠️⚠️ **E NON SI TRONCANO LO STESSO, PERCHÉ IL CORPO SCENDE DI UN GRADINO**: è l'altra metà
      del suo riscontro, dal campo libero (*puoi rimpicciolire i testi dei pulsanti proporzione*).
      Il conto, con la parola più lunga delle ventotto lingue (il polacco *Oryginalne*): su 360dp
      la fila ne ha 312 netti, meno i cinque distacchi restano 282; col peso in più alle due
      parole una prende 57dp e un numero 42, e a `labelMedium` quella parola ne chiede una
      quarantina più i due rientri del chip.
    - ⚠️ **Il peso in più va alle due parole e non a tutte e sei**: '16:9' sono quattro caratteri
      che non si traducono mai, quindi dividere la riga in parti uguali regalerebbe ai numeri lo
      spazio che serve alle parole.
    - ⚠️ **Il corpo lo passa la fila e non il chip**: gli altri chip della scheda (il verso della
      selezione) hanno due celle su tutta la larghezza, quindi là non c'è niente da stringere. Il
      pezzo resta **uno solo** (`ShapeRow`, in `EditorScreen.kt`) e lo chiamano i due editor.
- ⚠️⚠️ **L'IMMAGINE LASCIA L'ARIA ALLE SQUADRETTE, DALLA `2.32`, ED È IL SUO RISCONTRO** (giro
  della `2.31`, voce `crop-modulo` non approvata: *all'avvio del modulo gli angoli di ritaglio non
  sono del tutto visibili*). Una squadretta si disegna **a cavallo** del bordo del rettangolo,
  quindi metà del suo spessore cade fuori dall'immagine: col riquadro a filo del palco, che
  ritaglia il proprio contenuto, quella metà spariva. Adesso l'immagine si adatta a una stanza
  ridotta di `CROP_AIR` per lato, che è esattamente `HANDLE_THICK + GRIP_HALO`, cioè quanto la
  squadretta sporge.
  - ⚠️⚠️ **MA DALLA `2.37` QUELL'ARIA VALE IN TUTTI E SETTE I MODULI, ED È IL PUNTO C DEL SUO
    CAMPO LIBERO** (giro della `2.36`: *Consideralo un anti-jitter tra moduli: al cambio da un
    altro modulo al ritaglio, l'immagine *NON* deve rimpicciolirsi, il che significa che le
    maniglie dell'area di ritaglio devono essere ESTERNE allo spazio dedicato all'anteprima
    immagine*). La nota della `2.32` diceva che fuori dal Ritaglio sarebbe stata spazio tolto per
    niente, e guardava **un modulo per volta** invece del passaggio da uno all'altro: entrando nel
    Ritaglio l'immagine perdeva `CROP_AIR` per lato e si rimpiccioliva sotto gli occhi, cioè in
    orizzontale lo stesso ballo che la `2.33` aveva tolto in verticale.
    - ⚠️ **Quello che costa è dichiarato**: negli altri sei moduli l'immagine è più piccola di
      cinque punti per lato di quanto sarebbe, cioè meno dell'uno per cento su uno schermo da
      telefono. È il prezzo di una misura che non cambia mai, ed è lo stesso baratto di
      `SteadyBody`.
    - ⚠️ **La prova guarda i bordi dell'immagine e non l'altezza del palco**: il palco non si è mai
      mosso, a muoversi era il rettangolo in cui l'immagine è disegnata dentro di lui. ⚠️ **Un
      pixel per lato si concede**, perché il velo del ritaglio ha il bordo interno esattamente sul
      bordo dell'immagine e il suo antialiasing non è fondo; la controprova è di un altro ordine di
      grandezza (quattro pixel).
- ⚠️⚠️ **È IL PRIMO MODULO CHE PRENDE IL DITO SULL'IMMAGINE**: gli altri sei mettono i comandi nella
  scheda, questo li mette **sul palco**, quindi finché è in scena il palco fa solo quello (pinza,
  panoramica, doppio tocco e confronto restano fermi), che è la stessa modalità dichiarata del
  colore mirato. ⚠️ **E l'immagine torna intera**: le squadrette si tirano ai bordi di quello che si
  vede, e con l'immagine ingrandita metà di quei bordi starebbe fuori dallo schermo.
- ⚠️ **Un dito che scende lontano da una presa non fa niente**, e non è una dimenticanza:
  l'alternativa sarebbe spostare il rettangolo dal punto toccato, cioè farlo saltare sotto il dito.

⚠️⚠️ **I TRE COMANDI DI POSA VIVONO QUI, E SONO QUELLI DELL'EDITOR DI CASA**: 'Ruota a sinistra',
'Ruota a destra' e 'Rifletti', col tocco lungo che riflette in verticale, disegnati dallo stesso
`ActionPad` con gli stessi glifi e le stesse etichette. ⚠️ **Tre e non cinque**: le due centrature
lavorano sul rettangolo dentro una **forma scelta**, e senza i formati non avrebbero niente da
centrare. ⚠️ **L'ordine salvato dal riordino non si legge**: quello è l'ordine di una fila da
cinque, e infilarci tre tasti darebbe una fila che si riordina in un modo che nessuno ha chiesto.

⚠️⚠️ **UNA POSA PORTA CON SÉ IL RETTANGOLO, E SENZA QUELLA RIGA IL RITAGLIO SI SPOSTEREBBE IN
SILENZIO**: il rettangolo è in frazioni dell'immagine **già posata**, quindi un quarto di giro che
non lo riscrivesse lo lascerebbe dov'è sullo schermo, cioè su un'altra porzione di fotografia. Lo
riscrive `spunRect`, la stessa funzione dell'editor di casa, dentro `spunLook`.
- ⚠️ **Qui la rotazione NON rifà il rettangolo**, al contrario dell'editor di casa, e la differenza
  è che là esiste una forma scelta da rifare sull'aspetto nuovo. Con un rettangolo libero, girarlo
  lo lascia esattamente sulla stessa porzione di immagine.

⚠️⚠️ **LA POSA RESTA SENZA PERDITA E IL RITAGLIO NO, E LE DUE DOMANDE NON SONO LA STESSA**: una
posa si scrive in un tag EXIF, quindi l'immagine è cambiata ma il file non si riscrive; un ritaglio
toglie dei pixel, quindi va riscritto. Confonderle vorrebbe dire ricomprimere una fotografia per
averla girata, che è proprio quello che l'editor di casa non fa dalla `1.03`.
- **Quindi il salvataggio delega**: quando non c'è niente da sviluppare e la geometria è ferma, il
  file passa da `ImageEdit.save`, cioè dalla strada di sempre, posa e ritaglio compresi. La catena
  completa (posa, shader, geometria, taglio) entra in funzione solo quando serve davvero.

⚠️⚠️ **E LA FILA DEI MODULI NON SCORRE PIÙ: SONO SETTE ICONE CHE SI DIVIDONO LA LARGHEZZA, ED È SUA
RICHIESTA** (stesso campo libero: *forse al posto dei nomi dei moduli (che resterebbero per gli
screen reader) dovremmo usare delle icone, che sono molto più brevi e sarebbero tutte visibili senza
scorrere in orizzontale*). ⚠️⚠️ **LO SCORRIMENTO DELLA `2.23` NON ERA UNA SCELTA MA UN RIMEDIO**:
coi nomi scritti la fila cresceva in altezza e il palco si riduceva a zero pixel, e scorrere teneva
metà dei moduli fuori dallo schermo per chi non sa che si scorre.
- ⚠️ **Il nome non si perde**: è il `contentDescription` del gettone, cioè quello che un lettore di
  schermo legge, ed è anche quello che il banco cerca. ⚠️ **Quindi la nota sul sesto gettone da
  raggiungere scorrendo è superata**, e con lei le righe delle prove che lo facevano.
- ⚠️ **I quattro canali delle Curve restano scritti**, e non è un'incoerenza: là i gettoni sono
  quattro e i loro nomi sono una lettera o poco più, quindi un'icona direbbe meno della parola. Il
  pezzo è lo stesso e sceglie da sé, perché il glifo è facoltativo.

⚠️⚠️ **E DALLA `2.34` QUELLA FILA SI RIORDINA COME I QUATTRO RIQUADRI DI CASA, ED È SUA
RICHIESTA** (2026-09-13: *voglio poter ordinare anche i pulsanti dei moduli*). Il riordino a
trascinamento esisteva dalla `1.57` per il menu su un file, la scheda della selezione e le due file
dell'editor: i moduli entrano **là dentro** invece di avere il proprio, perché un secondo
meccanismo per lo stesso gesto sarebbe una seconda occasione di divergere.
- ⚠️⚠️ **OGNI MODULO PORTA LA PROPRIA CHIAVE, E NOME E GLIFO RESTANO NELLA TABELLA DEI MODULI**:
  un ordine nell'archivio è un elenco di gettoni, quindi senza una chiave un modulo si potrebbe
  salvare solo per indice, cioè con un numero che cambia significato appena se ne aggiunge uno.
  ⚠️ **I due elenchi si coprono a vicenda e il banco lo misura**: un modulo nuovo che si
  dimenticasse di `MOD_KEYS` sparirebbe dalla fila senza che niente dia errore.
- ⚠️⚠️ **QUELLO CHE SI RIORDINA È COME I GETTONI SI VEDONO, E NON L'IDENTITÀ DI NIENTE**: lo
  sguardo tiene il posto nella **tabella**, quindi spostare un gettone non cambia il modulo aperto
  e `LOOK_FIRST` continua a dire la Luce. Con un indice legato alla fila, ogni trascinamento
  avrebbe cambiato che cosa si apre.
- ⚠️⚠️ **LA REPLICA NELLE IMPOSTAZIONI È DI SOLE ICONE, ED È IL CRITERIO DI `PadArrange` APPLICATO,
  non una deroga**: quel riquadro deve somigliare al modello, e il modello le parole non le ha (le
  ha tolte la `2.31`, perché sette nomi non entrano in nessuna larghezza). Scriverle darebbe sette
  parole troncate su sette colonne. ⚠️ **Il nome resta quello che un lettore di schermo annuncia**,
  come nel gettone vero, e le due azioni parlate che spostano una cella non cambiano.
- ⚠️ **Il riquadro c'è solo dove c'è l'editor completo**, cioè da Android 13: è l'unico dei cinque
  a dipendere dal telefono, e mostrarlo altrove vorrebbe dire far riordinare una fila che non si
  può aprire. ⚠️ **La preferenza invece si salva lo stesso**, perché un archivio non deve cambiare
  forma con la versione di Android.

⚠️⚠️ **E DALLA `2.33` C'È 'APPLICA', ED È SUA RICHIESTA** (2026-09-13: *manca 'Applica' per il
ritaglio*). Fino alla `2.32` il rettangolo si tirava e non si vedeva applicato mai: il taglio
compariva soltanto nel file salvato, quindi non esisteva il momento in cui l'immagine su cui si
lavora diventa quella tagliata.
- ⚠️⚠️ **QUELLO CHE FA È LA SUA SCELTA FRA DUE LETTURE, E LA DOMANDA GLI È STATA FATTA**: il palco
  passa a inquadrare la porzione tenuta e negli altri moduli si lavora su quella; **rientrando nel
  Ritaglio l'immagine torna intera** con le squadrette dov'erano, quindi il taglio si può allargare
  o rifare. L'altra lettura, cioè tagliare davvero e ripartire come fa l'editor di casa, è stata
  scartata da lui.
- ⚠️⚠️ **NON TAGLIA NIENTE, E QUESTO È IL PUNTO**: il rettangolo era già nel modello e il file si
  salva tagliato da sempre. Quello che mancava era **vederlo**, quindi il tasto scrive un valore
  (`Look.framed`) invece di riscrivere un'immagine.
- ⚠️ **Quel valore vive nel modello e non nello sguardo**, ed è la ragione per cui 'Annulla' lo
  disfa come ogni altro passo: è l'unico campo di `Look` che non cambia un pixel del file, e per
  questo non entra né in `idle` né in `lossless`.
- ⚠️ **Il glifo e la parola sono quelli dell'editor di casa**, come i tre comandi della storia: lo
  stesso gesto a un tocco di distanza non può avere due segni, e nessuna stringa nuova nasce.
- ⚠️ **Il conto non tocca nessun altro**: `view` resta il riquadro in cui l'immagine **intera** è
  disegnata, che è quello su cui si reggono il pezzo a risoluzione piena, la lente, il colore
  mirato e la maglia della geometria; a cambiare è che lo si ricava da dove deve cadere la porzione
  (`spread`), e che il disegno si ferma al suo confine (`cutout`).

⚠️⚠️ **NELLA `2.73` I QUATTRO COMANDI DEL RITAGLIO ERANO PIÙ IN ALTO, E DALLA `2.75` NON PIÙ: È
UNA REVOCA SUA** (chiesta col punto 2 del campo libero del giro della `2.70`, *i pulsanti
indietro/avanti/applica/azzera del ritaglio devono stare un pelo più in alto, più lontani dai
tasti principali in basso*, e ritirata al giro dopo, voce `crop-alti` non approvata: *Mi sembrava
ci fosse spazio, invece con lo spostamento s'è ammucchiato tutto. Riporta allo stato precedente*).
- ⚠️⚠️ **LA MISURA DEL BANCO DICEVA IL VERO E GUARDAVA LA COSA SBAGLIATA**: l'aria fra quei
  comandi e la barra passava da 7 pixel a 14, cioè raddoppiava, e la scheda non si alzava di un
  pixel. Quello che nessuna misura poteva vedere è come il blocco si legge sul telefono: `Breathe`
  distribuisce quello che avanza, quindi il distacco se l'è preso dall'avanzo e il **resto** del
  corpo si è stretto della stessa quantità. Un numero che cresce da una parte lo toglie da
  un'altra, e quale delle due si veda è una resa.
- ⚠️ **Con lei esce anche la prova che la presidiava**, per la stessa ragione: non ha più niente
  da guardare. Il fatto che regge il tetto resta scritto qui: un distacco più grande dell'avanzo
  farebbe del Ritaglio il modulo più alto, e allora la scheda si alzerebbe in tutti e nove.

⚠️⚠️ **E DALLA `2.80` QUEI QUATTRO COMANDI RISALGONO, MA DIETRO UN SEPARATORE SFUMATO: È LA STESSA
RICHIESTA CON IL PEZZO CHE LE MANCAVA** (punto D del campo libero del giro dalla `2.75` alla
`2.77`: *sposta più su i 4 tasti del controllo del ritaglio, e separali dalla barra in basso usando
un separatore sfumato*). Le due metà si leggono insieme: il separatore occupa dell'aria in fondo al
corpo, quindi i comandi salgono **perché** qualcosa li separa, e non per un distacco scritto sopra
di loro.
- ⚠️⚠️ **LA REVOCA DELLA `2.75` NON SI STA RIFACENDO, PERCHÉ IL CONTO È CAMBIATO**: là il distacco
  se lo prendeva dall'avanzo che `Breathe` distribuisce, e il resto del corpo si stringeva della
  stessa quantità (*s'è ammucchiato tutto*). Qui la riga dei due versi se n'è andata dentro le
  forme, quindi l'avanzo c'è davvero, e questo separatore ne spende meno di quanto quella riga
  liberi.
- ⚠️ **Sfuma ai due capi e non arriva ai bordi della scheda**: una linea piena da bordo a bordo
  dividerebbe il pannello in due superfici, mentre quello che ha chiesto è uno stacco.
- ⚠️ **E i quattro comandi si distanziano fra loro**, che è l'altra metà del punto `crop-giu` (*le
  quattro icone-pulsanti del ritaglio più distanziate*): costa **zero** in altezza, perché quella
  riga si divide una larghezza che ha già, e le celle restano più larghe di un `IconButton` di
  Material. Con lui il blocco delle forme prende un'aria in cima, che è il suo *tutto più distante
  dai gettoni dei moduli*.
- ⚠️⚠️ **IL PUNTO D NON HA UNA PROVA DEL BANCO, E VA DETTO**: è la stessa classe di misure che la
  `2.75` ha dichiarato bugiarde, cioè quelle che vedono un numero crescere da una parte senza
  vedere che cosa si stringe dall'altra. Come si legge sul telefono si guarda sul telefono, e la
  voce di collaudo lo chiede.

⚠️⚠️ **DALLA `2.85` IL GESTO DEL PALCO LEGGE LA POSA E IL TAGLIO DI ADESSO, E FINO ALLA `2.84` NO**
(terza delle sue segnalazioni del giro della `2.83`: *scelgo l'area, tasto 'Applica', voglio fare
un altro ritaglio successivo ... ma la cornice di ritaglio non si muove*). Il corpo del
`pointerInput` del palco parte al **primo tocco** e da lì non riparte più, e leggeva tre valori
della composizione (la forma dell'immagine in posa, il taglio applicato e l'anteprima in posa)
com'erano in quel tocco. Dopo 'Applica' il dito cercava la cornice dentro l'immagine intera mentre
le squadrette si disegnavano ai bordi della porzione.
- ⚠️⚠️ **SEMBRAVA CAPRICCIOSO, E IL BANCO HA DETTO PERCHÉ**: un gesto che nasce **dopo** il
  cambiamento lo vede giusto, quindi il difetto compariva solo col palco già toccato prima. Dopo
  'Applica' succedeva quasi sempre, perché il primo taglio si fa col dito; e si vedeva di più con
  un taglio piccolo, come il suo quadrato, perché la cornice del gesto e quella disegnata si
  allontanavano oltre la presa.
- ⚠️ **La stessa causa colpiva dopo una rotazione**, che lui non aveva segnalato: il gesto cercava
  la cornice nel riquadro dell'immagine coricata. E per la stessa via il colore mirato dell'HSL e
  i limiti dello zoom leggevano la forma di prima. Questi ultimi due sono ragionati e non misurati.
- ⚠️ **Il rimedio è la prudenza che `geoNow` aveva già**: tre `rememberUpdatedState`, letti dentro
  il gesto. La chiave del `pointerInput` non si tocca, perché cambiarla annullerebbe il gesto in
  corso.

⚠️⚠️ **E DALLA `2.85` IL VERSO DI PARTENZA LO DECIDE LA FOTOGRAFIA, COME NELL'EDITOR DI CASA**
(prima delle tre segnalazioni: *ho toccato 'originale', la foto era 4:3 orizzontale, ma la cornice
di ritaglio è diventata verticale*). La regola è sua ed è del 2026-08-31, scritta su `startLay`, ma
la applicava un editor solo: l'editor completo partiva sempre da 'Verticale', quindi 'Originale' su
una fotografia larga dava per costruzione il rapporto trasposto.
- ⚠️ **Si decide una volta, quando l'anteprima arriva**, e da lì il verso è una sua scelta: una
  rotazione non lo cambia, che è la regola di casa.
- ⚠️ **Scelto a mano 'Verticale', 'Originale' dà ancora il rapporto trasposto**, ed è dichiarato:
  è come si comportano le quattro proporzioni, in tutti e due gli editor.

⚠️ **Che cosa il banco misura e che cosa no** (`SviluppoTest`): che il rettangolo segua la posa nei
due gesti e dopo quattro giri torni dov'era, che una posa resti senza perdita e un ritaglio no, che
i sette gettoni si annuncino col nome senza scriverlo, che il Ritaglio porti i tre comandi e nessun
cursore, che tirando una squadretta il palco cambi disegno; dalla `2.34` anche che la fila dei
moduli **segua l'ordine scelto** portandoli tutti e sette; dalla `2.80` che le due parole e i due
versi stiano su una riga e i quattro numeri sull'altra, e che il tocco su un verso a icona lo
scelga ancora (controprovato rimettendo i numeri a cavallo delle due righe, e spegnendo il legame
del verso); dalla `2.85` che su un'immagine larga il verso parta orizzontale, e che la cornice si
tiri ancora dopo 'Applica' e dopo una rotazione (i casi 67, 68 e 69, ognuno controprovato togliendo
la sua correzione). ⚠️ **Le prove sulla cornice toccano il palco PRIMA del cambiamento**, e la
controprova lo ha imposto: la prima stesura della rotazione girava prima di ogni tocco, e restava
verde col difetto dentro. **Non** vede il file salvato, cioè che
i pixel tagliati siano quelli giusti: quello si guarda sul telefono, e la voce di collaudo lo chiede.
- ⚠️⚠️ **E LA PROVA HA PAGATO ALLA PRIMA CORSA, TROVANDO UN DIFETTO CHE NESSUN COMPILATORE POTEVA
  VEDERE**: il gesto consumava l'evento **prima** di leggerne il delta, e `positionChange()` risponde
  **zero** su un evento già consumato. Il codice era valido, il gesto partiva, la presa scattava, e
  il rettangolo non si muoveva di un pixel.
- ⚠️⚠️ **IL BANCO HA IMPOSTO ANCHE COME SI INIETTA QUEL GESTO**: i tre momenti del dito (giù,
  movimento, su) vanno in **tre** chiamate separate, perché scritti in un blocco solo il movimento e
  il distacco arrivano insieme e a `drag` resta un evento con delta zero.

## 🎞️ I preset, venti di casa e quelli che si salvano

⚠️⚠️ **DALLA `2.39`**: un preset è un **aspetto** che si porta da un'immagine all'altra, cioè i
moduli di colore con un nome sopra. Ne arrivano **venti** in casa e se ne salvano quanti se ne
vogliono.
- ⚠️⚠️ **ERANO CINQUE FINO ALLA `2.52` E DALLA `2.53` SONO SEI**: Luce, Colore, HSL, Dettaglio,
  Curve e gli **Effetti**, che sono un aspetto come gli altri. Chi legge 'cinque' in una nota
  vecchia sappia che il sesto è entrato con quel modulo, e che il formato regge nei due versi
  perché un campo che manca vale il suo valore di riposo.
- ⚠️⚠️ **E 'L'ULTIMO PEZZO DELL'EDITOR COMPLETO' NON È PIÙ VERO**: lo era quando è stato scritto,
  e la sua risposta `effetti` a `d-dopo-editor` ha aperto la tappa dopo.

⚠️⚠️ **QUATTORDICI DEI VENTI SONO SUOI, CONVERTITI DAI SUOI XMP DI LIGHTROOM, E LA CONVERSIONE L'HA
FATTA LA SESSIONE** (sua istruzione, 2026-09-14: *aggiungi i miei, più uno creato ex novo da te per
arrivare alla cifra tonda di 20. Per ora lasciamo stare l'importazione degli XMP*). Quindi
nell'APK non c'è **nessun lettore XMP**: quello che è entrato sono i valori già tradotti, scritti
in Kotlin come qualunque altra costante.
- ⚠️⚠️ **I SUOI XMP ERANO DICIANNOVE E I PRESET SUOI SONO QUATTORDICI, PERCHÉ CINQUE NON AVEVANO
  NIENTE DA TRAVASARE**: erano fatti di **taratura dei primari della fotocamera**, di color
  grading a tre zone e di maschere locali, che AIV non ha. Portarli avrebbe voluto dire cinque
  righe nell'elenco che non cambiano un pixel, ed è il difetto che il banco misura (caso 6).
- ⚠️ **Quindi quelli di casa sono SEI e non uno**: quattro coprono i mestieri che i suoi
  quattordici non toccavano (bianco e nero, ritratto, notturno, pellicola) e **due sono
  riscritture dichiarate** di 'Contrasto colore classico' e 'foliage', cioè lo stesso effetto
  rifatto nelle otto fasce dell'HSL invece che nei primari. Hanno un nome diverso dal suo proprio
  perché non sono la stessa cosa.
- ⚠️ **Che cosa resta fuori si dichiara**, e vale per tutti e quattordici: chiarezza, texture,
  foschia, grana, vignettatura, sfrangiatura e viraggio diviso non esistevano in questo editor.
  ⚠️⚠️ **DALLA `2.57` NE RESTAVANO FUORI DUE SOLE, E DALLA `2.64` SONO QUATTRO**: la foschia, la
  grana e la vignettatura esistono; la sfrangiatura e il viraggio diviso non sono mai arrivati, e
  chiarezza e texture sono uscite con la sua risposta `via` a `d-eff-restano`. ⚠️ **I venti
  restano lo stesso senza**, perché ogni campo che manca vale il suo valore di riposo.
  ⚠️⚠️ **E LA SUA RISPOSTA A `d-preset-xmp` È `lascia`** (giro della `2.55`: *Saranno da rifare ex
  novo una volta inseriti tutti gli effetti*), quindi non si riaprono i suoi XMP: i venti si
  rifanno da capo quando il modulo è finito. Un preset di casa non si cambia da sé, per la stessa
  ragione della `1.33`.

⚠️⚠️ **UN PRESET NON PORTA LA POSA, IL RITAGLIO E LA GEOMETRIA**: quei tre dipendono da **come è
stata scattata quell'immagine** (da che parte sta il cielo, dove finisce il soggetto, quanto pende
l'orizzonte), mentre un preset si porta da un'immagine all'altra. Applicarne uno che raddrizza di
tre gradi girerebbe anche le fotografie dritte.
- ⚠️ **Sono i cinque moduli che i preset di Lightroom sanno dire**, e la coincidenza non è casuale:
  quel formato quei tre non li tratta come un aspetto.

⚠️⚠️ **APPLICARE SOSTITUISCE INVECE DI SOMMARE, ED È QUELLO CHE RENDE UN PRESET PREVEDIBILE**: con
una somma, applicarne uno sopra un altro darebbe qualcosa che nessuno dei due descrive, e
applicare due volte lo stesso darebbe due immagini diverse. Così un preset dice **dove si
arriva**, e per tornare indietro c'è 'Annulla', perché quello che ne esce è un `Look` come un
altro. È la stessa strada del tasto 'Auto', che scrive nei cursori e non dipinge niente.

⚠️⚠️ **IL PANNELLO NON È UN OTTAVO MODULO, ED È UNA SCELTA MISURATA**: dalla `2.33` la scheda è
alta quanto il **modulo più alto**, quindi un modulo fatto di un elenco che cresce con quello che
si salva alzerebbe la scheda di tutti e sette gli altri, cioè accorcerebbe il palco anche a chi i
preset non li usa. Il tasto vive **accanto ad 'Auto'**, che è il suo parente stretto.
- ⚠️ **Il tocco applica e il pannello RESTA APERTO**: un preset si sceglie confrontando, e un
  pannello che si chiudesse a ogni tocco costringerebbe a riaprirlo per provare il prossimo.
- ⚠️ **Il glifo è di Material e nasce provvisorio**, come quello di 'Auto' nella `2.32`: se non
  dice abbastanza, il giro di collaudo lo chiede e lui manda il suo.

⚠️⚠️ **L'ARCHIVIO È UN FILE E NON UNA PREFERENZA**, al contrario di quasi tutto il resto dell'app:
una preferenza tiene un valore, qui invece cresce un elenco di oggetti annidati (otto fasce e
quattro curve per ognuno), e scriverlo in un `DataStore` vorrebbe dire una stringa lunghissima
sotto una chiave sola, cioè un file travestito. Vive in `filesDir` come le copertine, perché
quello che vive nella cache il sistema lo può buttare.
- ⚠️⚠️ **OGNI CAMPO CHE MANCA VALE IL SUO VALORE DI RIPOSO, ed è quello che tiene il formato
  compatibile nei due versi**: un file scritto oggi si legge domani anche se domani i cursori sono
  sei, e uno scritto domani si legge oggi perdendo quello che oggi non esiste. ⚠️ **E un modulo a
  riposo non si scrive affatto**: un preset di sola Luce deve rileggersi come tale, o direbbe che
  tocca anche il colore e le curve, a zero.
- ⚠️ **Si legge e si scrive a mano con `org.json`**, che è nella piattaforma: una libreria di
  serializzazione automatica avrebbe legato la forma del file a quella delle classi, e il giorno
  che un modulo prende un campo i preset salvati non si leggerebbero più.

⚠️ **Un nome già usato SOSTITUISCE**, senza guardare le maiuscole: due preset che si chiamano
uguale sono indistinguibili nell'elenco, quindi l'unica cosa che si potrebbe fare col secondo è
cercare di capire quale sia. ⚠️ **E il nome non si traduce**, nemmeno quello dei venti di casa: è
un nome proprio, come quello di una cartella.

⚠️ **Si cancella con un'offerta di rimetterlo e non con una conferma**, che è il criterio di casa:
una conferma protegge quello che si perderebbe, e qui non si perde niente finché la notifica è in
scena. ⚠️ **Rimetterlo è salvarlo di nuovo**, cioè la stessa porta dell'andata.

⚠️⚠️ **MA DALLA `2.50` NON SONO PIÙ UN PANNELLO ACCANTO AD 'AUTO': SONO L'OTTAVO MODULO, E SI
CHIAMA 'STILI'** (sua istruzione, 2026-09-14: *il modulo Preset ... deve vivere nello stesso
spazio della bottomsheet*). Quindi la nota qui sopra, che spiegava perché non potevano essere un
modulo, è **superata**: quell'argomento diceva che un elenco che cresce alzerebbe la scheda di
tutti e sette gli altri, e la risposta è che l'elenco **scorre** dentro l'altezza che la scheda
ha già (§ `SteadyBody`).
- ⚠️ **Il gettone entra in `MOD_KEYS` come gli altri**, quindi si riordina e si ritrova
  nell'archivio di chi ha già scelto il proprio ordine.
- ⚠️⚠️ **'Di serie' SI CHIAMA 'STILI AIV', ED È SUO** (stessa istruzione): i venti di casa hanno
  un nome proprio, e 'di serie' diceva com'erano arrivati invece di che cosa sono.
  - ⚠️⚠️ **MA NELL'ELENCO DEL MODULO QUEL TITOLO NON SI SCRIVE, DALLA `2.52`** (suo ritocco del
    2026-09-14: *il nome della categoria ('Stili AIV') a ben vedere non serve: in questo contesto
    i pixel verticali sono preziosi e si capisce perfettamente che i primi sono di fabbrica*). A
    dividere i due gruppi resta un separatore solo, **'Stili salvati'**, e compare **solo se c'è
    almeno uno stile suo**: senza stili salvati non c'è niente da separare, e un titoletto da solo
    annuncerebbe una parte che non esiste.
    - ⚠️⚠️ **E QUEL SEPARATORE HA CAMBIATO TESTO, PERCHÉ MI AVEVA DETTO DUE COSE DIVERSE E HA
      SCELTO** (2026-09-18: *hai ragione sulla dicitura degli stili utente: ti ho detto due cose
      diverse; scelgo 'Stili salvati'*). Fino alla `2.51` la stringa diceva **'Stili personali'**,
      che è la parola del giro della `2.50`; adesso `look_preset_mine` dice 'Stili salvati' in
      tutte e ventotto le lingue. ⚠️ **La chiave non si tocca**, come sempre.
    - ⚠️ **Il nome non diventa terminologia morta**: 'Stili AIV' resta come si chiamano quei
      venti, e la pagina delle impostazioni lui non l'ha nominata, quindi là non si tocca niente.
    - ⚠️ **L'ordine dei due gruppi non cambia**: gli stili dell'app sopra e i propri sotto, che è
      la sua istruzione del giro della `2.50` (*quelli salvati, in basso*).
  - ⚠️⚠️ **E 'SALVA STILE' VIVE SULLA BARRA DELLE ICONE, IN BASSO A SINISTRA, DALLA `2.52`**
    (stessa richiesta): allineato all'inizio delle righe degli stili, e **fisso**, cioè fuori
    dall'elenco che scorre. Un comando che se ne va insieme all'elenco si ritrova risalendo, e
    qui l'elenco cresce con quello che si salva.
    - ⚠️⚠️ **QUINDI È UN'ICONA E NON PIÙ UNA SCRITTA, ED È UNA SCELTA DICHIARATA**: quella barra
      è tutta di icone dalla `2.32`, e una parola in mezzo la farebbe leggere in due modi, che è
      la ragione per cui anche 'Originale' è un'icona pur non essendo stato nominato. Il nome
      resta la descrizione parlata. ⚠️ **Il glifo è di Material e nasce provvisorio**, come
      quello di 'Auto' nella `2.32`: la voce di collaudo lo chiede.
    - ⚠️⚠️ **E GLI STILI SALVATI HANNO DOVUTO TRASLOCARE NELLA SCHERMATA**: chi li crea adesso
      vive sulla barra e chi li elenca vive nel corpo del modulo, cioè due pezzi diversi, quindi
      quell'elenco vive sopra tutti e due (in `LookSheet`). Tenuto dentro il corpo, un
      salvataggio si sarebbe visto solo riaprendo l'editor.
- ⚠️⚠️ **IL TOCCO AZZERA E RISCRIVE, IL TOCCO LUNGO TOCCA SOLO I MODULI CHE IL PRESET NOMINA, ED
  È LA SUA SPECIFICA ALLA LETTERA** (*tocco sullo stile = modifica assoluta (azzera tutto, poi
  modifica); tocco prolungato = modifica additiva (tocca i valori inclusi, non modifica gli
  altri)*). La regola della `2.39` resta il gesto normale; il gesto lungo serve a **comporre**,
  cioè a mettere un preset di sole curve sopra uno di sola luce.
  - ⚠️ **'Additiva' non vuol dire che i numeri si sommano**: un modulo nominato si **sostituisce**
    per intero, perché dentro un modulo i cursori si leggono insieme, e sommarli darebbe una
    taratura che nessuno dei due descrive.
  - ⚠️⚠️ **CHE COSA UN PRESET 'NOMINA' LO DICE IL FORMATO, e non serve un secondo dato**: un
    modulo a riposo non si scrive nel file, quindi *questo preset parla di luce?* si risponde
    chiedendo se la sua luce è a riposo. Un elenco scritto accanto sarebbe la stessa informazione
    in due posti, e il primo a divergere sarebbe quello che nessuno guarda.
- ⚠️ **L'immagine cambia mentre si scorre l'elenco** (*man mano che si tocca un predefinito o un
  altro, l'immagine deve aggiornarsi in tempo reale*), che è quello che rende il pannello un
  banco di prova invece di un elenco da leggere.

⚠️⚠️ **I VENTI HANNO NOMI NUOVI E SONO IN ORDINE ALFABETICO, E SONO SUOI** (stessa istruzione):
'Combo' diventa **'Roccobot'**, i due rossi si chiamano **'Rosso -'** e **'Rosso - -'**, il color
grading caldo porta il suo verso fra parentesi, i tre del trattamento portano il prefisso
**'T&O - '**, e il misto si chiama **'Blu/Rosso'**. ⚠️ **L'ordine è alfabetico e non quello di
arrivo**: un elenco che cresce con quelli salvati non ha un ordine naturale, e quello di arrivo lo
sapeva solo chi lo aveva scritto.

⚠️⚠️ **E SI GOVERNANO DALLE IMPOSTAZIONI, NELLA PAGINA 'STILI DELL'EDITOR'** (sua richiesta): là
si riordinano, si rinominano e si cancellano tutti in un colpo, e c'è **'Ripristina'** per
rimettere i venti di casa. ⚠️ **Il testo dell'avviso è suo alla lettera**, e dice che cosa si
perde invece di chiedere se si è sicuri.
- ⚠️⚠️ **QUEL NOME È SUO E ARRIVA DAL GIRO DELLA `2.50`**: la `2.50` la chiamava **'Stili di
  modifica'**, che era la mia proposta, e lui l'ha riscritta in **'Stili dell'editor'** nel campo
  del testo `t-stili-pagina`. ⚠️ **Nel telefono è arrivato con la `2.52`**, insieme alle altre
  ventotto lingue, che è il modo in cui un testo riscritto entra sempre; chi legge il nome
  vecchio in un commento o in una schermata sappia che è la stessa pagina.
- ⚠️ **La riga sotto non cambia**, e dice già quello che il nome nuovo dice meglio: *Riordina,
  rinomina e cancella gli stili dell'editor completo.*
- ⚠️ **'Salva preset' si chiama 'Salva stile'**, perché 'preset' è la parola del codice e 'stile'
  quella che si legge nel telefono: è il criterio di § '🗣️ Come si chiamano le cose'.
- ⚠️⚠️ **'IMPORTA' ED 'ESPORTA' PASSANO DAL SELETTORE DI SISTEMA E NON DA UNA CARTELLA DI CASA**,
  cioè da `CreateDocument` e `OpenDocument`: un file che l'utente deve poter mandare a qualcuno
  vive dove lo mette lui. ⚠️ **Il formato è quello dell'archivio**, non un secondo: importare è
  leggere lo stesso JSON, quindi un file esportato oggi si rilegge domani con le stesse regole
  di compatibilità (ogni campo che manca vale il suo valore di riposo).
- ⚠️ **Un preset di casa cancellato non sparisce dall'app**: i suoi valori vivono nel programma,
  quindi l'archivio ne tiene la chiave fra i **nascosti**. 'Ripristina' porta via quell'elenco
  insieme a tutto il resto, ed è la ragione per cui il comando è uno e non due.
- ⚠️ **I due gruppi si riordinano separatamente**: l'elenco vero ha gli stili dell'app sopra e i
  propri sotto (sua istruzione: *quelli salvati, in basso*), quindi un ordine unico permetterebbe
  di infilare un proprio in mezzo a quelli di casa, cioè di chiedere una cosa che l'elenco non sa
  mostrare.
- ⚠️ **La pagina nasce con la strada delle sotto-pagine**, cioè per il primo dei quattro modi: è
  un **elenco** che cresce e porta comandi propri riga per riga (§ '⚙️ Dove va un'impostazione, e
  chi la deve trovare').

⚠️⚠️ **E 'AUTO' COMPARE SOLO NEI DUE MODULI CHE GOVERNA, DALLA `2.50`** (sua istruzione: *l'icona
di 'Auto' deve apparire solo se è attivo 'Luce' o 'Colore'*). Quel comando scrive in sei cursori,
e sono tutti di quei due moduli: negli altri cinque era un tasto che cambiava valori che non si
vedevano. ⚠️ **La condizione si legge dalla tabella dei moduli**, come quella del colore mirato, e
non da un elenco di nomi scritto accanto al tasto.

⚠️⚠️ **IL PRIMO AVVIO DELL'EDITOR PORTA UN MINI-ONBOARDING SULLA FILA DEI MODULI, E IL TESTO È SUO
ALLA LETTERA** (2026-09-14): le icone dei moduli passano all'arancione degli onboarding e sfumano
verso destra dentro il velo, una freccia dello stesso colore indica lo scorrimento, e sopra c'è
scritto che si scorre e che il tocco lungo azzera un modulo solo. La chiave è `Hint.MODULES`.
- ⚠️ **Nasce con l'ottavo gettone**: fino alla `2.40` i sette stavano nella larghezza, e con gli
  Stili la fila scorre sul suo schermo (*nel mio caso, con il mio schermo, sarà l'unico a
  richiedere uno scorrimento a destra, ma va benissimo così*).
- ⚠️⚠️ **IL VELO CONSUMA IL PRIMO TOCCO, E QUESTO ROMPE LE PROVE CHE TOCCANO UN GETTONE**: quattro
  prove del banco sono diventate rosse in un colpo, e non per un difetto. Adesso `@Before` scrive
  la chiave come già vista, che è anche la condizione vera di chi ha già aperto l'editor una
  volta. ⚠️ **Dalla `2.73` le chiavi da scrivere sono due**, per la stessa ragione.
- ⚠️⚠️ **E DALLA `2.73` LE SLIDE SONO DUE: LA SECONDA È LA TESTATA**, ed è sua richiesta (punto 3
  del campo libero del giro della `2.70`, col testo dettato da lui). Evidenzia 'Filigrana',
  'Ridimensiona' e 'Salva', che portano **due gesti ognuno**, e dice che il tocco accende e il
  tocco lungo configura. Il velo è `HintSpots`, la chiave `Hint.EDITOR_TOOLS`.
  - ⚠️⚠️ **LA CHIAVE È SUA E NON UN SECONDO PASSO DELLA PRIMA, E LA RAGIONE È CHI HA GIÀ L'APP**:
    con una chiave sola, chi aveva aperto l'editor prima della `2.73` avrebbe la prima archiviata
    e non vedrebbe **mai** la seconda, cioè proprio chi ha seguito i giri di collaudo.
  - ⚠️ **Vive nell'editor completo e non anche in quello di casa**, dove i due tasti ci sono: lo
    dice il suo testo dalla prima parola (*Oltre ai moduli*), e là i moduli non esistono.
  - ⚠️ **Il testo ha un punto fermo dove la sua riga aveva una virgola** (*alle impostazioni di
    ciascuna, Quando hai finito*): è un refuso di battitura, e la voce di collaudo lo dichiara.
  - ⚠️⚠️ **OGNI COPIA PORTA IL PROPRIO RIQUADRO MISURATO, E NON UNA CELLA CALCOLATA**: i tre tasti
    non sono larghi uguali (due icone e una parola) e sono **due o tre** a seconda che un logo sia
    stato scelto, quindi dividere in parti uguali poserebbe l'arancione accanto ai comandi invece
    che sopra. È lo stesso criterio del velo della copertina, su tre riquadri invece che su uno.
  - ⚠️⚠️ **E DALLA `2.79` I PARAGRAFI SONO DUE, UNO PER GRUPPO, ED È LA SECONDA METÀ DEL PUNTO C**
    (*va rifatto anche il mini-onboarding, con i due tasti in basso e il tasto 'Salva' in alto
    evidenziati, e due paragrafi separati, uno sopra e uno sotto, ciascuno vicino all'oggetto cui
    fa riferimento*). I tre tasti adesso vivono in due posti lontani, quindi una frase sola
    sarebbe lontana da metà di quello che indica: `hint_tools` perde la coda su 'Salva', che
    diventa `hint_save`, e nessuna parola cambia.
  - ⚠️⚠️ **DA CHE PARTE CADE UNA FRASE LO DECIDE LA MISURA E NON UNA RIGA SCRITTA FISSA**: un
    gruppo nella metà di sopra si prende la frase **sotto**, uno nella metà di sotto la prende
    **sopra**. Scritta fissa da una parte, una delle due coprirebbe i tasti che indica, e la
    controprova dice di più: quella dei due comandi finisce **fuori dal vetro**, cioè non si
    vede affatto. A presidiarlo è il caso 66 di `SviluppoTest`, che misura tutti e due i versi.
  - ⚠️ **I due glifi sono quelli dei tasti veri** (`Glyphs.Watermark` e `Glyphs.Resize`): due
    `Icons.Filled` scritte in due file divergono al primo ritocco, e la copia direbbe un'altra
    cosa.

⚠️ **Che cosa il banco misura e che cosa no** (`PresetTest`, ogni caso controprovato): l'andata e
ritorno campo per campo coi cinque moduli pieni, che un preset non porti e non tocchi i tre moduli
di geometria, che applicare due volte dia la stessa immagine, il nome doppio, la cancellazione, i
venti di casa (venti, pieni, distinti), l'ordine dell'elenco, e il pannello che applica restando
aperto col comando che toglie sui soli propri. **Non** vede come un preset cambia un'immagine, che
è la sola cosa che conta davvero: quella si guarda sul telefono, e la voce di collaudo lo chiede.

## 🔏 La filigrana, e perché il file si copia in casa

⚠️⚠️ **DALLA `2.69`, ED È LA SUA SPECIFICA ALLA LETTERA** (risposta `altro` a `d-filigrana-file`,
giro della `2.67`: *è configurabile dalle impostazioni, sezione editor. Input PNG o SVG. Si sceglie
un solo logo o simbolo per volta e vale per tutti gli editing se il suo interruttore è attivo al
salvataggio. Per cambiare watermark, si rientra nelle impostazioni e si sceglie un altro file. Cosa
importante: l'utente lo sceglie e l'app lo memorizza in una sua cartella interna, in modo che
sopravviva anche alla cancellazione dell'originale*). È la prima delle due cose che ha chiesto col
suo `insieme` a `d-prossimo` (*prima la filigrana, poi il ridimensionamento*).

⚠️⚠️ **NON VIVE IN `Look`, E NON È UN DETTAGLIO DI COMODO**: un `Look` è un **aspetto**, cioè
quello che uno stile si porta da un'immagine all'altra; una filigrana è una **firma**, vale per
tutti gli editing e dentro un preset non vorrebbe dire niente. Quindi arriva al salvataggio come un
argomento a sé, come la copia di sicurezza, e i moduli che uno stile governa restano sei.
- ⚠️ **Conta come lavoro da salvare**, ed è la riga che rende la funzione usabile: chi apre
  l'editor per firmare un'immagine e basta non muove nessun cursore, quindi senza quella
  condizione il tasto 'Salva' resterebbe spento e la firma non arriverebbe mai su un file. Vale
  nei due editor.
- ⚠️ **Il testo di 'Applica al salvataggio' è suo alla lettera dalla `2.72`**, e dice tre cose:
  che cosa fa, che il tasto in testata è lo stesso interruttore, e che con una firma le operazioni
  senza perdita diventano una riscrittura.
- ⚠️ **E toglie il senza perdita**, come un cursore di Luce: scrivere dei pixel sopra la
  fotografia è una riscrittura, e non c'è modo di ottenerla con un tag EXIF.

⚠️⚠️ **IL FILE SI COPIA COM'È E NON SI RASTERIZZA ALL'ADOZIONE, ED È LA RAGIONE PER CUI UN SVG
SERVE A QUALCOSA**: un vettore disegnato al momento del salvataggio resta nitido su un file da
venti megapixel, mentre uno ridotto a una misura scelta oggi sarebbe pixel come gli altri. Il PNG
si copia per il motivo opposto: è già pixel, e ridurlo all'adozione butterebbe via quello che
porta.
- ⚠️ **Vive in `filesDir` e non in `cacheDir`**, come le copertine e gli stili: quello che sta
  nella cache il sistema lo può buttare quando ha bisogno di spazio, e qui la promessa è che il
  logo resti anche dopo che l'originale è sparito. È la sua clausola, e decide l'archivio.
- ⚠️⚠️ **UN FILE SOLO PER VOLTA, E LO DICE IL NOME**: la copia si chiama sempre `mark` col suffisso
  del proprio tipo, quindi sceglierne un altro dello stesso tipo prende il posto del primo da sé.
  ⚠️ **Quello dell'altro tipo va tolto a mano**, ed è l'unica riga che potrebbe dimenticarsi: senza,
  resterebbero due filigrane e a decidere quale si usa sarebbe l'ordine di un `enum`, cioè la
  scelta dell'utente verrebbe ignorata in silenzio. A presidiarla è il caso 3 del banco.

⚠️⚠️ **IL TIPO SI RICONOSCE DAI BYTE E NON DAL NOME, E IL CASO CHE LO PRETENDE È IL JPEG**: chi
sceglie un file passa dal selettore di sistema, che consegna un indirizzo e un tipo dichiarato da
chi lo serve, e tutti e due possono mentire. Un JPEG non ha trasparenza, quindi come filigrana
stamperebbe un **rettangolo pieno** sopra la fotografia, e quel difetto si vede solo sul file già
salvato.
- ⚠️ **I tipi si dichiarano anche al selettore**, così il navigatore mostra i soli file che si
  possono usare invece di lasciar scegliere e rispondere di no dopo: sono due presidi con due
  scopi, e il controllo vero resta sui byte.
- ⚠️⚠️ **E IL FILE SI DISEGNA PRIMA DI ADOTTARLO**: un documento valido che non si rende darebbe
  una filigrana che non compare, cioè un salvataggio che non fa quello che promette. ⚠️ **Il
  vecchio si cancella solo dopo**, o chi prova un file sbagliato resterebbe senza quello che aveva.

⚠️ **La misura è una frazione del lato LUNGO dell'immagine**, con le stesse due ragioni del
Dettaglio: una misura in pixel darebbe una firma enorme su un file piccolo e invisibile su uno da
fotocamera; e il lato **lungo** invece della larghezza, o la stessa scelta peserebbe un quarto su
una fotografia verticale, che è un difetto che non dà nessun errore. Le misure sono quattro e
l'aria dal bordo è una sola (`Watermark.AIR`), letta anche dall'anteprima delle impostazioni.
- ⚠️ **Un PNG piccolo chiesto grande si ingrandisce, e il costo si dichiara**: i pixel che mancano
  non li inventa nessuno. La via alternativa, cioè fermarsi ai pixel del file, darebbe una firma
  che cambia misura a seconda dell'immagine: un'impostazione che non fa quello che dice. Chi vuole
  una firma nitida a ogni misura usa un SVG, ed è la ragione per cui si accettano tutti e due i
  formati.
- ⚠️ **Cinque posti e non nove**: i quattro angoli sono dove una firma va a finire da sempre, e il
  centro è il caso di chi marca un'immagine che verrà condivisa. I mezzi dei lati non aggiungono un
  gesto che qualcuno faccia, e porterebbero quattro nomi in più in ventotto lingue.

⚠️⚠️ **SI SCRIVE SUL BITMAP RICEVUTO QUANDO SI PUÒ, E LA COPIA È IL RIPIEGO**: chi chiama ha in
mano l'immagine finita e la sta per comprimere, e una copia a venti megapixel sono ottanta megabyte
per un logo in un angolo. Nel caso comune non serve, perché quello che esce dal ritaglio, dallo
shader e dalla maglia della geometria è un bitmap costruito da loro, cioè mutabile. ⚠️ **Se la
copia non si può fare, la firma salta**: chi chiama scrive l'immagine senza, che è meglio di un
salvataggio fallito.

⚠️⚠️ **E DALLA `2.72` HA UN TASTO NELL'EDITOR, ED È SUA ISTRUZIONE** (riscontro del
giro della `2.70`, voce `filigrana` accettabile: *aggiungi un'icona 'Filigrana' in alto a destra,
prima di 'Ridimensiona', che si accende o spegne con un tap normale. Il tap prolungato porta alle
impostazioni della filigrana*). ⚠️ **In testata fino alla `2.78`, e dalla `2.79` nella barra in
basso**, che è il punto C del campo libero del giro dopo. I due gesti, il pezzo condiviso col
ridimensionamento e il perché di ognuno vivono in § '🎛️ I due tasti del salvataggio, e i loro
due gesti'.
- ⚠️ **Il tocco scrive la STESSA chiave delle impostazioni**, ed è la sua frase alla lettera
  (*equivale esattamente a muovere questo interruttore*): un valore che valesse per il solo editor
  aperto sarebbe una terza cosa da capire.
- ⚠️⚠️ **SENZA UN LOGO SCELTO IL TASTO NON C'È**, perché non ci sarebbe niente da accendere: è lo
  stesso criterio di 'Mostra nascoste' (§ '👁️ Mostra nascoste, e perché dura un minuto'), e la
  porta per scegliere un logo resta quella della sua specifica, cioè le impostazioni.

⚠️⚠️ **LA FIRMA SI POSA SU PIXEL INTERI, DALLA `2.75`, E FINO ALLA `2.74` ARRIVAVA MORBIDA** (sua
segnalazione, punto 4 del campo libero del giro dalla `2.71` alla `2.74`: *la filigrana è stampata
sull'immagine in modo molto morbido, quasi sfocato*). Il disegno è **già reso** alla misura
chiesta, quindi la scala è uno a uno e non c'era niente da rimpicciolire: a sfocare era l'angolo in
**virgola mobile** che `cornerFor` dà, perché un bitmap posato a `123,7` si campiona bilinearmente
su ogni pixel, cioè ognuno diventa la media di quattro vicini.
- ⚠️ **Col filtro se ne va la causa**: quel flag era là perché la destinazione non cadeva su pixel
  interi, cioè rimediava a quello che adesso non succede.
- ⚠️ **Mezzo pixel di scarto non si vede e uno sfocato sì**: l'arrotondamento sposta la firma al
  massimo di mezzo pixel su una fotografia da quattromila.
- ⚠️ **Quello che resta fuori si dichiara**: un PNG più piccolo della misura chiesta si ingrandisce
  e resta morbido, ed è il costo già scritto sopra. Chi vuole una firma nitida a ogni misura usa un
  SVG.

⚠️⚠️ **E NELLA `2.74` SI VEDEVA ANCHE SUL PALCO DEI DUE EDITOR, MA DALLA `2.75` NON PIÙ: È UNA
FUNZIONE PROVATA E REVOCATA** (chiesta col punto 5 del campo libero del giro della `2.70`, *dietro
un interruttore nuovo 'Mostra nell'editor'*, e tolta al giro dopo, voce `mark-palco` non approvata:
*In realtà funziona bene, ma mi sono accorto che non serve, e forse confonde pure. Funzionalità da
togliere*). Quindi non c'è un difetto da correggere: c'è una cosa che ha visto e non vuole.
- ⚠️ **Con lei se ne vanno tutti i suoi pezzi**, perché nessuno aveva un secondo lettore:
  l'interruttore 'Mostra nell'editor' con la sua chiave e le sue 28 stringhe, il disegno sul palco,
  la soglia dello zoom a riposo, il parametro che la schermata passava all'editor e le cinque prove
  del banco. ⚠️ **La chiave resta scritta negli archivi di chi ha la `2.74`**, e non si pota: una
  chiave che nessuno legge non fa danno, e cancellarla costerebbe una migrazione.
- ⚠️ **Il riquadro delle impostazioni resta**, ed è l'anteprima che lui tiene: là la firma si vede
  su un fondo neutro mentre si tarano i quattro numeri, che è il posto in cui quei numeri si
  scelgono.

⚠️⚠️ **LA DISTANZA DAL BORDO SI SCRIVE COL DECIMALE, DALLA `2.76`, ED È SUA RICHIESTA** (punto 1
del campo libero del giro dalla `2.71` alla `2.74`: *Filigrana / Distanza dal bordo: aggiungi i
valori 0,2 e 0,5*). Fino alla `2.75` quella corsa contava in **centesimi interi**, quindi sotto
l'uno per cento non c'era niente: il salto da 0 a 1 su un file da quattromila pixel sono quaranta
pixel di margine, che è molto più di quanto una firma appoggiata al bordo chieda.
- ⚠️⚠️ **IL VALORE RESTA UN INTERO, E QUELLO CHE CAMBIA È L'UNITÀ**: adesso conta in **decimi di
  centesimo** (`AIR` diventa `0..250`, `AIR_STEP` vale dieci), e il decimale vive nel solo campo.
  Un `Float` nell'archivio avrebbe portato dentro l'arrotondamento binario per un dato che ha
  duecentocinquantun valori possibili.
- ⚠️⚠️ **CHI AGGIORNA DALLA `2.75` PORTA LA CHIAVE VECCHIA, E SI LEGGE PER DIECI**: la chiave nuova
  è `mark-air-tenths`, e `mark-air` resta come **ripiego** in lettura, moltiplicata per il passo.
  ⚠️ **Non è una migrazione come quella dell'indicatore** (§ '🏷️ L'indicatore dell'ultimo media, e
  la sua migrazione'), e la differenza è quale domanda si fa: là si guardava se l'archivio fosse
  **vuoto**, cioè una cosa che cambia appena si salva qualunque altra impostazione; qui si guarda
  una chiave che **nessuno scrive più**, quindi la risposta non può cambiare sotto i piedi.
- ⚠️ **Il separatore è quello della lingua del telefono** e non la virgola scritta a mano, perché
  l'app parla ventotto lingue; **a scrivere** invece valgono uguale il punto e la virgola, che è
  quello che le due tastiere numeriche di Android offrono a seconda di come sono fatte.
- ⚠️ **Il numero tondo non porta la coda**: la distanza di fabbrica si legge `3` e non `3,0`.

⚠️⚠️ **E IL POSTO SI SCEGLIE SUL RIQUADRO, DALLA `2.76`, ED È SUA RICHIESTA** (punto 3 dello stesso
campo libero: *la scelta tra i quattro angoli e il centro dovrebbe essere visuale, con dei
selettori angolari color accento, poco fuori dal riquadro, per i quattro angoli, più un selettore
superiore, sempre fuori dal riquadro, per selezionare il centro senza coprirlo*). Fino alla `2.75`
erano cinque pastiglie scritte sotto l'anteprima, cioè delle **parole** che dicevano dove sarebbe
andata una cosa che il riquadro accanto mostrava già.
- ⚠️ **I cinque selettori vivono in una fascia sola intorno al riquadro**: i quattro angoli
  l'abbracciano da fuori e il quinto sta in mezzo a quella di sopra. Due arie diverse sarebbero
  due numeri da tenere allineati, e il *senza coprirlo* della sua richiesta è proprio quello che
  la fascia garantisce.
- ⚠️⚠️ **QUINDI IL RIQUADRO C'È ANCHE SENZA UN LOGO SCELTO, E LA NOTA DELLA `2.71` È DECADUTA**:
  diceva che *un riquadro vuoto non direbbe niente*, e valeva finché era la sola anteprima. Adesso
  è il comando: toglierlo vorrebbe dire non poter scegliere il posto prima di scegliere il file.
- ⚠️ **I cinque nomi restano**, perché sono quello che un lettore di schermo annuncia e quello che
  la ricerca delle impostazioni confronta: chi cerca 'centro' cerca questa voce. Nessuna stringa
  nasce e nessuna resta orfana.
- ⚠️ **Il bersaglio è più grande del segno**, quarantaquattro punti contro diciotto: una squadretta
  larga quanto il dito coprirebbe l'angolo dell'immagine che deve mostrare.
- ⚠️⚠️ **E DALLA `2.78` I CINQUE SEGNI SONO PIÙ GROSSI, A PAGARE È IL RIQUADRO, E IL CENTRO È UN
  TONDO DA SQUADRETTA** (voce `mark-posto` approvata con una richiesta: *rendi solo i selettori
  DECISAMENTE più spessi e visibili ... se necessario rimpicciolisci il riquadro di quanto basta a
  far stare all'esterno dei selettori ben pasciuti e visibili ... Il selettore del centro dev'essere
  un tondo più o meno delle stesse dimensioni dei selettori di angolo, anche se di forma diversa*).
  I numeri sono cinque: la fascia passa da 16 a 22 punti, il braccio da 18 a 22, il tratto da 3 a 5
  (e da 2 a 3 quando non è scelto), il tondo del centro da 10 a 18 di diametro, e l'inchiostro di
  uno spento dal 35% al 55%.
  - ⚠️ **A stringersi è il riquadro e non il bersaglio**: la fascia si prende dal rientro
    dell'anteprima, quindi sono sei punti per lato, e il dito continua ad avere i suoi
    quarantaquattro.
  - ⚠️ **Il tondo non arriva a un braccio intero, e il conto dice perché**: vive dentro la fascia,
    quindi il suo bordo esterno (il raggio più mezzo tratto) deve restare sotto la larghezza della
    fascia stessa, o tornerebbe a coprire l'immagine, che è proprio quello che la sua richiesta
    della `2.76` esclude.
- ⚠️⚠️ **E IL RIQUADRO HA GLI ANGOLI QUASI VIVI, CHE È IL PUNTO 2** (*dev'essere molto meno
  arrotondata (giusto un paio di pixel*): da dodici punti a **due**. Rappresenta una fotografia, e
  una fotografia gli angoli stondati non ce li ha; zero avrebbe fatto di lui un rettangolo nudo in
  una pagina in cui ogni superficie è stondata.
- ⚠️⚠️ **E LA MISURA AVEVA UN DIFETTO CHE NESSUNO AVEVA VISTO**: il disegno si leggeva in un
  `produceState` **senza chiave**, quindi scegliendo un altro file dello stesso tipo il riquadro
  mostrava ancora il logo di prima. Si vedeva solo uscendo e rientrando nella pagina, ed è arrivato
  fin qui perché fino alla `2.75` quel riquadro nasceva insieme alla prima filigrana. Adesso la
  chiave è il contatore della pagina, cioè lo stesso che fa rileggere il tipo.

⚠️ **La pagina delle impostazioni è una sotto-pagina, e la soglia lo pretende**: la domanda è una
sola (*che logo scrivo sulle immagini che salvo*) e le voci sono molte più del *2-3* della sua
soglia (§ '⚙️ Dove va un'impostazione, e chi la deve trovare'). Vive
dentro 'Editor e salvataggio', che è la pagina della domanda *che cosa succede quando modifico
un'immagine*.
- ⚠️⚠️ **E DAL TOCCO LUNGO SUL TASTO SI APRE COME UNA SCHEDA SOPRA L'EDITOR, DALLA `2.75`** (voce
  `mark-imposta` accettabile: *quando entro nelle impostazioni della filigrana con il tocco lungo
  poi se torno indietro deve tornare direttamente nell'editor aperto, senza rifare il giro dalle
  impostazioni alla home e di nuovo all'editor*). La lettura è dichiarata e va oltre la sua
  lettera: dall'editor **non si esce affatto**, quindi non c'è nessun ritorno da governare.
  - ⚠️⚠️ **FAR TORNARE INDIETRO ALL'EDITOR NON BASTAVA, ED È MISURATO**: il `Look` su cui si sta
    lavorando è un `remember(uri)` e non un `rememberSaveable`, quindi il `SaveableStateHolder` di
    `AivApp` non lo tiene da parte; rientrando, l'editor si sarebbe riaperto **vuoto**, cioè
    peggio del giro che lui ha segnalato.
  - ⚠️ **È il pezzo che c'era già**, cioè la stessa pagina (`MarkPage`) dentro una `Sheet`: senza
    una seconda copia da tenere allineata, e la porta delle impostazioni resta dov'è.
  - ⚠️ **Con lei i due tasti della testata diventano gemelli anche nel gesto lungo**: quello del
    ridimensionamento apriva già una finestra sopra l'editor.
  - ⚠️⚠️ **E CADE LA PILA DELLA SCORCIATOIA, CHE NON HA PIÙ CHIAMANTI**: `SettingsPage`, il
    parametro di `SettingsScreen`, la pagina d'arrivo di `Screen.Settings` e la prova del banco
    che la misurava. La risalita di un gradino per volta, che è il fatto della `2.09`, resta e
    resta misurata.
- ⚠️ **L'anteprima non è un ornamento**: posizione e misura si vedono sul file salvato, cioè dopo,
  e provarle vorrebbe dire salvare un'immagine per ogni tentativo. Quel riquadro usa **gli stessi
  due numeri** del disegno vero, quindi quello che si vede è quello che si avrà.

⚠️ **Che cosa il banco misura e che cosa no** (`FiligranaTest`, ogni caso controprovato): che un
JPEG non si adotti e un PNG sì **qualunque cosa dica il nome**, che ne resti uno solo, che un file
illeggibile non porti via quello scelto, il tetto degli otto megabyte, che la misura sia la
frazione scelta del lato lungo (misurata anche girando l'immagine, a pixel), che la firma cada
nell'angolo scelto e non nell'opposto, che senza un file scelto non si scriva niente, e che una
filigrana pronta accenda 'Salva' su un'immagine intonsa; dalla `2.75` anche che il **bordo della
firma non sfumi**, cioè la misura del difetto arrivato a lui; dalla `2.76` che la distanza scritta
in centesimi **si rilegga in decimi** (coi tre casi: la chiave vecchia sola, l'archivio vuoto, e
tutte e due le chiavi, dove vince la nuova) e che il numero col decimale si scriva e si rilegga su
tutta la corsa, col punto come con la virgola. **Non** vede la resa della firma su una fotografia
vera, né la scelta del file dal selettore di sistema, che è una schermata di Android: quelle si
guardano sul telefono, e la voce di collaudo le chiede.
- ⚠️⚠️ **I DUE CASI DELLA `2.76` PRESIDIANO LO STESSO DIFETTO DA DUE PARTI, ED È UN FATTORE
  DIECI**: un ripiego che non moltiplica dà una firma a un decimo della distanza scelta, e un
  testo letto senza il passo la dà dieci volte più lontana. Nessuno dei due dà un errore, e tutti
  e due si vedono solo sul file salvato. Controprovati uno per uno: togliendo il `times` cade il
  primo, leggendo il campo come un intero cade il secondo.
- ⚠️ **Le due funzioni del numero sono `internal` per questo**, come `SettingsStore.read`: un
  conto fra un testo e un numero è Kotlin puro, e misurarlo attraverso il campo vorrebbe dire
  montare una pagina per verificare un'aritmetica.
- ⚠️⚠️ **QUELLA PROVA HA UN PIANO SCELTO PERCHÉ L'ANGOLO VENGA FRAZIONARIO, e senza quella cura
  sarebbe verde a vuoto**: il disegno è nero pieno e opaco, quindi un livello **in mezzo** può
  nascere solo dall'interpolazione; ma in alto a sinistra con l'aria a zero l'angolo cade su
  `0,0`, cioè su un numero intero, e il difetto non si vedrebbe nemmeno rimettendolo.
  Controprovata: i livelli in mezzo passano da zero a **135**.

## 📏 Il ridimensionamento, e i due gesti di un tasto solo

⚠️⚠️ **DALLA `2.70`, ED È LA SUA RISPOSTA `editor` A `d-resize-dove`** (giro della `2.67`: *mi
piacerebbe più nell'editor, fatto in modo che funzioni tipo un parametro del salvataggio (un
parametro complicato, ovvio, ma tipo 'Salva alle dimensioni...'). Nella pratica, comunque, si
chiamerà 'Ridimensiona' e sarà costituito da un tasto più un interruttore. Imposti il
ridimensionamento dal tasto e poi l'interruttore stabilisce se il ridimensionamento si applica al
salvataggio, come per il watermark. Se si entra nell'opzione per configurarlo, una volta che premo
'OK' l'interruttore è acceso e salva con ridimensionamento se non lo spengo*). È la seconda delle
due cose che ha chiesto col suo `insieme` a `d-prossimo` (*prima la filigrana, poi il
ridimensionamento*), e con lei quella tappa è chiusa.

⚠️⚠️ **NON VIVE IN `Look`, PER LA STESSA RAGIONE DELLA FILIGRANA**: un `Look` è un **aspetto**,
cioè quello che uno stile si porta da un'immagine all'altra; questo dice **come scrivere il
file**, e dentro un preset non vorrebbe dire niente. Arriva al salvataggio come un argomento a sé,
e i moduli che uno stile governa restano sei.
- ⚠️ **E per la stessa ragione non si vede sul palco**: l'immagine su cui si lavora resta quella,
  perché ridimensionare non cambia che cosa si vede ma quanti pixel si scrivono.
- ⚠️ **Toglie il senza perdita**, come un cursore di Luce: scrivere meno pixel è una riscrittura, e
  non c'è modo di ottenerla con un tag EXIF.

⚠️⚠️ **'UN TASTO PIÙ UN INTERRUTTORE' SU UN BERSAGLIO SOLO, ED È UNA LETTURA DICHIARATA**: la sua
frase ne descrive due, e in testata non entrano. Il conto, su uno schermo da 360 punti: il tasto
Indietro ne prende 48, 'Salva' una settantina, l'icona nuova 48, e uno `Switch` di Material altri
52; al titolo, che è l'unico a cedere, ne resterebbero un centinaio, cioè 'Modifica immagine' a
`headlineSmall` andrebbe a capo. Quindi i gesti sono due sullo stesso tasto, che è il modo di
questa app (la regola di `SaveButton` e dei gettoni dei moduli), e l'accento dice se è acceso.
⚠️⚠️ **E LA LETTURA È PASSATA, PERCHÉ NELLA `2.72` HA CHIESTO UN SECONDO TASTO COSÌ**: la voce
`resize` è tornata accettabile, il titolo si è accorciato in 'Modifica' per far posto a due icone,
e i due gesti si sono rovesciati (§ '🎛️ I due tasti del salvataggio, e i loro due gesti').
⚠️⚠️ **E DALLA `2.79` QUEL CONTO NON GOVERNA PIÙ NIENTE, PERCHÉ I DUE TASTI SONO SCESI NELLA
BARRA**: la testata torna a portare il solo 'Salva', e lo spazio che quel conto misurava non è più
stretto. Resta scritto perché dice **perché** i gesti sono due su un bersaglio solo, che è una
scelta che vale dovunque quel tasto viva.
- ⚠️ **Ad accendere è anche 'Applica' della finestra**, che è la sua specifica alla lettera.
  Quindi la finestra non chiede due volte la stessa cosa: chi entra a configurare ha già detto
  che lo vuole.
- ⚠️ **Spegnere non porta via il piano**, che è l'altra metà della stessa frase: quello che si era
  scelto resta scritto, e riaccendere non chiede di riscriverlo. Per questo le preferenze tengono
  **tre** campi e non uno.

⚠️⚠️ **E DALLA `2.72` LA SUA FINESTRA HA 'RIPRISTINA', ED È SUA RICHIESTA** (2026-09-19, con una
schermata: *riporti tutto su 'Lato lungo' con il valore letto dall'immagine allo stato corrente*).
Quel valore è il piano che **non fa niente**, cioè il punto da cui si riparte, e 'allo stato
corrente' vuol dire dopo la posa e il ritaglio, che è quello che la finestra già riceve.
- ⚠️ **Vive sulla riga del titolo**, dove questa app mette un comando di una finestra dalla `1.79`:
  `TitleRow` sceglie da sé fra la pastiglia scritta e l'icona, misurando se il titolo ci sta
  accanto.
- ⚠️ **Scrive nei campi e non applica**, e senza la misura di partenza non c'è: là non si saprebbe
  da dove si riparte.
- ⚠️⚠️ **MA DALLA `2.77` NON RIPORTA PIÙ A 'LATO LUNGO': RIPORTA AL LIBERO** (voce
  `resize-ripristina` non approvata: *'Ripristina' deve riportare i valori non all'originale della
  visualizzazione o dell'immagine in memoria, bensì dell'immagine reale al suo stato corrente*). La
  misura era già quella giusta, cioè quella dopo la posa e il ritaglio; a cambiare è che con la
  forma nuova due campi pieni con le misure correnti **sono** il libero, e lasciare acceso un
  gettone direbbe che una regola governa un ridimensionamento che non c'è.
- ⚠️⚠️ **LA NOTA DELLA FINESTRA È RISCRITTA DA LUI**, alla lettera: *Le proporzioni restano
  invariate; è possibile solo ridurre le dimensioni.* ⚠️ **In testo regolare**, che è la sua
  precisazione dello stesso momento: la prima stesura rendeva l'enfasi in corsivo.

⚠️⚠️ **E DALLA `2.77` LA FINESTRA È RIDISEGNATA PER INTERO, ED È LA SUA SPECIFICA ALLA LETTERA**
(stessa voce: *la schermata va ridisegnata e migliorata: sotto 'Ridimensiona' va indicata la
dimensione di origine (o dello stato attuale, dopo un ritaglio; il ridimensionamento avviene per
ultimo, DOPO il ritaglio). Es. `Dimensioni attuali: **1800**×**1200** px`, con i grassetti. Di
default ci devono essere due campi compilabili con il numero dei pixel di destinazione, e nessun
chip deve essere selezionato ... Se poi si tocca un chip, si disattivano i campi che dipendono dal
compilabile ... Sotto i campi compilabili, un'anteprima simile a quella di 'Rinomina'*). Fino alla
`2.76` c'erano quattro gettoni e **un** campo, quindi 'lato lungo 1600' diceva un numero solo e
l'altra misura si scopriva in una riga di testo.
- ⚠️⚠️ **I DUE CAMPI SONO LA VERITÀ, E IL GETTONE DICE SOLTANTO QUALE DEI DUE COMANDA**: le
  proporzioni si mantengono sempre, quindi i due numeri sono uno solo scritto due volte. Il valore
  del piano si **ricava** (`Resize.valueOf`), e per questo cambiare gettone non muove una cifra,
  che è la cosa che nella `2.76` costringeva la finestra a inventarsi un numero a ogni cambio di
  modo. ⚠️ **L'unico travaso resta quello coi per cento**, che sono un'altra unità.
- ⚠️⚠️ **'Pixel' È IL GETTONE DEL LIBERO E NON SI ACCENDE MAI, ED È UNA LETTURA DICHIARATA**: la
  sua riga dice *nessun chip deve essere selezionato* proprio nello stato in cui i due campi si
  scrivono tutti e due, e quello stato è il libero. Un gettone acceso direbbe che una regola c'è
  mentre non ce n'è nessuna: il gettone resta perché è la porta per **tornarci**, e il riscontro
  che dà è il campo disattivato che si riaccende.
- ⚠️⚠️ **QUINDI IL PIANO DI FABBRICA NON FA PIÙ NIENTE** (`Resize.NONE`, il libero con un tetto di
  ventimila pixel), e fino alla `2.76` era 'Lato lungo 1600': senza quel cambiamento la finestra si
  aprirebbe al primo giro con un gettone acceso e un numero che nessuno ha scritto, cioè il
  contrario della sua riga. ⚠️ **Con lui cade la frase che diceva che quello di fabbrica
  rimpicciolisce quasi ogni fotografia**, in § '🎛️ I due tasti del salvataggio, e i loro due gesti'.
- ⚠️ **I campi si precompilano col RISULTATO del piano, e con le misure correnti quando il piano
  non fa niente**: è una regola sola invece di due, e copre insieme il primo giro, il 'Ripristina'
  e un piano che su quell'immagine non toglie un pixel.
- ⚠️⚠️ **SENZA LE MISURE RESTA LA SOLA PERCENTUALE**, ed è l'unico modo che non ha bisogno di
  sapere da dove si parte: là non si possono precompilare i campi né tradurre l'uno nell'altro,
  cioè manca tutto quello su cui la finestra nuova è costruita. Succede dove il lato lungo non si
  legge, che è il caso già dichiarato più sotto.
- ⚠️ **La misura si scrive con una funzione sola** per la riga di partenza e per l'anteprima (i due
  numeri in grassetto, il segno di moltiplicazione in mezzo, `px` in coda): scritte due volte, la
  prima a divergere sarebbe quella che nessuno guarda. ⚠️ **Gli spazi intorno al segno sono una
  lettura dichiarata**: la sua riga dell'anteprima li ha (*in W × H px*) e quella delle dimensioni
  attuali no, perché là erano fra due asterischi di grassetto.
- ⚠️ **L'anteprima riusa la stringa di 'Rinomina'** (`rename_preview`), che dice esattamente
  'Anteprima' in tutte e ventotto le lingue: una parola nuova sarebbe la stessa cosa scritta due
  volte. ⚠️ **Le stringhe nuove sono due**, 'Lato corto' e la riga delle dimensioni attuali, e
  `look_resize_to` esce perché non ha più chiamanti. ⚠️⚠️ **TUTTE E DUE QUESTE RIGHE SONO DECADUTE
  CON LA `2.81`**: l'anteprima si chiama 'Risultato' e ha una stringa sua, e la riga delle
  dimensioni attuali è diventata il sottotitolo, cioè il solo numero.

⚠️⚠️ **E DALLA `2.81` QUELLA FINESTRA HA UN SECONDO MOCKUP, ED È LA SUA VOCE `resize-finestra`**
(esito 'quasi': *ECCELLENTE miglioramento. Manca ancora qualche aggiustamento, e ti invio un mockup
per mostrarti cosa voglio*). Sono sette ritocchi, e quello che li tiene insieme è il conto dello
spazio: *con tutte queste modifiche applicate ... la finestra diventa MOLTO più compatta e usabile
(e sarà meno coperta dalla tastiera attiva)*.
- ⚠️⚠️ **LA MISURA DI PARTENZA È IL SOTTOTITOLO, E LA DICITURA SE NE VA** (*'Dimensioni attuali'
  non serviva: toglilo, e metti i pixel larghezza × altezza correnti subito sotto il titolo*, e il
  testo `t-resize-now`). Il posto dice già che cosa è quel numero, quindi la dicitura ripeteva a
  parole quello che il posto dichiara, e `look_resize_now` non ha più niente da tradurre: esce
  dalle ventotto lingue.
  - ⚠️ **E i numeri non sono più in grassetto**, che è il suo mockup: là il grassetto distingueva
    le cifre dalla dicitura intorno, e senza dicitura distinguerebbe una riga da sé stessa.
- ⚠️⚠️ **'RENDI PREDEFINITO' È UNA FUNZIONE NUOVA, E QUELLO CHE FA VA DETTO PERCHÉ UNA METÀ C'ERA
  GIÀ** (*fa in modo che le impostazioni di ridimensionamento restino memorizzate per i salvataggi
  seguenti*). Fino alla `2.80` 'Applica' scriveva **anche** nelle preferenze, quindi il piano
  sopravviveva già al riavvio e questo comando non avrebbe avuto niente da fare. Adesso i due si
  dividono il mestiere: 'Applica' scrive nel **modello**, cioè vale finché l'app è viva, e 'Rendi
  predefinito' scrive nelle **preferenze** e accende l'interruttore.
  - ⚠️⚠️ **SI ACCENDE NEI SOLI TRE MODI CHE NON DIPENDONO DALL'IMMAGINE APERTA, ED È LA SUA RIGA**
    (*è cliccabile solo se sono attivi 'Lato lungo', 'Lato corto' o '%'*). Il perché è scritto in
    `Resize.portable`: quei tre non guardano come una fotografia è girata, mentre la larghezza,
    l'altezza e il libero fissano un numero su un lato preciso, cioè direbbero una cosa diversa
    sulla prossima immagine.
  - ⚠️⚠️ **IL SUO RISCONTRO È CHE IL TASTO SI SPEGNE, E LA SCELTA È DICHIARATA**: una notifica di
    casa si aprirebbe **dietro** la finestra e non si vedrebbe, quindi a dire che il piano è
    arrivato è il confronto col predefinito, che dopo il tocco coincide. Senza, il tasto resterebbe
    acceso e si toccherebbe due volte.
  - ⚠️ **È una pastiglia su due righe**, perché quella locuzione su una riga sola non entra
    accanto al titolo in nessuna delle ventotto lingue: `TitleRow` misura la parola più lunga
    invece della frase intera.
- ⚠️⚠️ **'RIPRISTINA' SCENDE IN FONDO A DESTRA** (*Tasto 'Ripristina' spostato in basso: si
  raggiunge meglio con una mano*), e lascia il posto del titolo al comando che si tocca una volta
  sola. ⚠️ **Resta la stessa pastiglia** e non diventa un `TextButton` accanto ad 'Applica': là si
  leggerebbe come un terzo tasto di conferma, mentre scrive nei campi e basta.
- ⚠️ **La percentuale si scrive col segno e sale nella prima riga** (*'Percentuale' diventa '%' e
  va nella prima riga dei gettoni: più compatto (bastano due righe) e più elegante*): l'ordine
  dell'enum diventa quello del mockup, e siccome nell'archivio vive il **token** nessuna scelta
  già salvata cambia significato. ⚠️ **Il nome per esteso resta nella descrizione parlata**, che è
  quello che un lettore di schermo annuncia: `resize_share` non resta orfana e nessuna stringa
  nasce, perché un segno non si traduce.
- ⚠️ **Col titoletto se ne va anche 'Adatta a'** (`look_resize_mode`): una fila di gettoni sotto un
  titolo che dice quello che i gettoni dicono già costa una riga di pixel verticali, che è la cosa
  che questo giro esiste per guadagnare.
- ⚠️⚠️ **IL SEGNO FRA I CAMPI SI ALLINEA ALLE CIFRE E NON AL CAMPO** (*il segno `×` è allineato
  meglio in verticale, in modo che sia centrato in verticale rispetto alle cifre*), e il conto vive
  su `FIELD_TEXT_DROP`: un campo con l'etichetta in alto porta il proprio testo più in basso del
  suo centro, quindi il segno centrato sul campo si leggeva più alto delle cifre che separa.
- ⚠️ **'Anteprima' diventa 'Risultato', e tutto è centrato** (*'Anteprima' diventa 'Risultato', ed
  è più piccolo e centrato ... Le misure finali sono centrate, e forse per coerenza potrebbero
  avere la stessa resa grafica e gli stessi colori del risultato della rinomina (ma con il testo
  più grande)*): quindi la stringa di 'Rinomina' non si riusa più e ne nasce una, perché le due
  parole dicono due cose diverse.
- ⚠️ **Che cosa il banco misura in più** (`RidimensionaTest`, ogni caso controprovato): che
  `Resize.portable` risponda ai soli tre modi, che 'Rendi predefinito' si accenda, scriva e si
  spenga, e che il segno cada **sotto** il centro del campo. ⚠️ **Il verso e non il numero**: una
  soglia sui punti cadrebbe al primo ritocco della costante. **Non** vede quanto la finestra sia
  più compatta col telefono in mano, e la voce di collaudo lo chiede.

⚠️⚠️ **E DUE DI QUEI SETTE RITOCCHI ERANO RIUSCITI A METÀ: LA `2.82` LI CHIUDE, ED È LA SUA VOCE
`resize-finestra-2`** (*'Rendi predefinito' deve andare a capo: 'Rendi' / 'predefinito'. E
'Ridimensiona' deve stare interamente senza troncature ... '%' deve stare sulla prima riga*). I
punti sono tre e le cause due, perché i primi due sono lo stesso difetto visto dai suoi due capi.
- ⚠️⚠️ **IL CONTO DICHIARAVA UN'INTENZIONE CHE IL DISEGNO NON APPLICAVA**: il campo `lines` dà al
  testo il **permesso** di prendere due righe, e un testo va a capo solo quando non ci sta; la
  pastiglia si dimensionava sul proprio contenuto, quindi la frase restava su una riga. Intanto
  `TitleRow` sceglieva fra pastiglia e icona **misurando la parola più lunga**, cioè contando su
  un'andata a capo che non c'era: prometteva al titolo uno spazio che la pastiglia si prendeva, e
  'Ridimensiona' andava a capo in mezzo a una parola.
  - **Adesso la larghezza si misura una volta e la usano in due**, il conto e il disegno: con la
    misura della parola più lunga imposta alla pastiglia, la frase non entra su una riga **per
    costruzione** e la seconda riga ci sta esatta.
  - ⚠️ **Si impone solo a chi va a capo**: a una riga sola la larghezza del contenuto è già quella
    che il conto misura, e scriverla sarebbe lo stesso numero due volte.
- ⚠️⚠️ **E LE DUE RIGHE DEI GETTONI SI DICHIARANO INVECE DI DIPENDERE DALLO SPAZIO**: erano una
  fila che andava a capo da sé, quindi la ripartizione la decideva quanto le parole misurano nella
  lingua del telefono. Sul suo, misurato sulla sua schermata, le prime due chiedono **213 punti** e
  il segno altri **46**, contro i **270** che la finestra ha: mancavano **quattro punti**, e il
  segno scendeva. Un conto che si perde per quattro punti non si aggiusta allargando qualcosa.
  - ⚠️ **Le celle hanno un peso MISURATO e non uguale**: con tre colonne uguali una cella vale 85
    punti e 'Larghezza' ne chiede 105, cioè si leggerebbe troncata. Col peso preso dalla larghezza
    vera del proprio testo, chi ha una parola lunga riceve di più e l'avanzo si distribuisce in
    proporzione, che è la resa che la fila delle forme del Ritaglio ha già.
  - ⚠️ **Il corpo scende di un gradino**, per la stessa ragione di quella fila: a `labelMedium` la
    prima riga chiede 251 punti invece di 274, quindi in italiano avanza spazio e nelle lingue dai
    nomi lunghi si tronca più tardi.
  - ⚠️⚠️ **IN QUALCHE LINGUA SI TRONCA LO STESSO, E VA DETTO**: in francese quelle due locuzioni
    sono il doppio delle nostre (*Côté le plus long*), quindi nessuna forma le fa entrare intere.
    Quello che la forma nuova garantisce è che il segno resti dov'è e che la finestra non cresca
    di una riga.
- ⚠️ **Che cosa il banco misura in più**: la **ripartizione** dei sei gettoni (i primi tre alla
  stessa altezza, gli altri tre più sotto) in `RidimensionaTest`, e in `TitoloTest` che una
  pastiglia a due righe sia più stretta e più alta della stessa a una riga. ⚠️ **Non si misurano
  le larghezze in punti**: quanto un testo misura sul banco non è quanto misura su un telefono, e
  una soglia direbbe una cosa che là non vale.
  - ⚠️⚠️ **LA PROVA DEI GETTONI VUOLE UNA SCENA LARGA, E LA CONTROPROVA HA SMENTITO QUELLO CHE
    MI ASPETTAVO**: sulla scena di serie del banco la ripartizione viene tre e tre **anche** con
    la fila che va a capo da sé, quindi rimettendo il difetto la prova restava **verde**, cioè
    non misurava niente. Con `@Config(qualifiers = "w600dp-h900dp")` le due forme si separano,
    perché questa dichiara le sue due righe e quella le ricava dallo spazio: là la fila della
    `2.81` si ripartisce `3 + 2 + 1` e la prova cade.
  - ⚠️ **Il KDoc di quella prova diceva il contrario prima della misura** (*ci stanno tutti su
    una riga*), ed è il caso generale di § '🧪 Quando si scrive una prova, e quando no': una
    controprova serve a smentire, e quando smentisce si riscrive quello che era scritto.

⚠️⚠️ **E DALLA `2.83` UNA PASTIGLIA A DUE RIGHE SCRIVE PIÙ PICCOLO E PIÙ STRETTA DI RIGA, ED È LA
SUA NOTA** (riscontro del giro della `2.82`, voce `resize-pastiglia` approvata con una richiesta:
*riduci leggermente la dimensione del carattere di 'Rendi predefinito', e/o riduci leggermente
l'interlinea*). La `2.82` le aveva dato la seconda riga, e quella riga arrivava col corpo e
l'aria di un testo che si legge a paragrafi: qui sono due parole, e quell'aria le faceva leggere
come due cose invece che come una.
- ⚠️⚠️ **LO STILE LO DÀ UNA FUNZIONE SOLA, E QUESTA È LA RIGA CHE TIENE IN PIEDI LA `2.82`**
  (`titlePillStyle`): il conto misura la parola più lunga per imporre la larghezza e il disegno
  scrive dentro quella larghezza, quindi due corpi diversi rifarebbero il difetto di quel giro in
  un verso o nell'altro, cioè una frase che non entra nello spazio promesso oppure una pastiglia
  più larga del testo che porta. È la stessa disciplina della `2.82`, su un dato in più.
- ⚠️ **L'interlinea è un RAPPORTO e non una misura** (`TITLE_PILL_LEAD`): scritta in `sp` direbbe
  il vero finché nessuno tocca la tipografia, e il giorno che quel corpo cambia resterebbe quella
  di prima.
- ⚠️ **A una riga non si tocca niente**: quel corpo è quello che lui ha approvato nella `1.85`, e
  le pastiglie di 'Rinomina' e di 'Scarica' non le ha nominate.
- ⚠️⚠️ **LE DUE ASSERZIONI SONO LE DUE METÀ DELLA SUA `e/o`, E OGNUNA HA LA SUA CONTROPROVA**: la
  larghezza dice che il corpo è sceso, l'altezza che l'interlinea si è stretta. Rimettendo il
  corpo pieno la parola lunga passa da più stretta a **65 punti contro 64**, cioè più larga di
  quella a riga sola; rimettendo l'interlinea di serie due righe valgono **32 contro le 16** di
  una, cioè il doppio esatto. ⚠️ **La soglia è il doppio e non un numero**: l'interlinea di serie
  darebbe quel valore esatto, quindi un ritocco a `TITLE_PILL_LEAD` non fa cadere la prova mentre
  il comportamento è ancora giusto.

⚠️⚠️ **C'È NEI DUE EDITOR, E NON È UNA COMODITÀ**: l'editor completo sotto Android 13 non esiste
(§ '🎚️ L'editor completo, e il conto che esiste in una copia sola'), quindi un ridimensionamento
che vivesse solo là mancherebbe a tutti i telefoni più vecchi. Il comando è **un pezzo solo**
(`ResizeButton`, in `EditorTools.kt`) e lo chiamano tutte e due le testate.

⚠️ **Le proporzioni si mantengono sempre, e non è un'opzione che manca**: un ridimensionamento che
le rompe deforma l'immagine, e chi vuole cambiare il rapporto ha il modulo Ritaglio, che toglie
dei pixel invece di stirarli. ⚠️⚠️ **E NON SI INGRANDISCE MAI**: i pixel che mancano non li inventa
nessuno, quindi chiedere un lato più lungo di quello che il file ha darebbe un'immagine più pesante
e non più nitida. Il tetto è **una riga sola** valida per tutti i modi, e la finestra **dice** che
su quell'immagine il piano non fa niente invece di lasciar credere il contrario.

⚠️ **Sei modi e non uno, dalla `2.77`, e prima erano quattro**: il lato lungo e quello corto
valgono per le verticali come per le orizzontali; la larghezza e l'altezza servono a chi ha un
vincolo su una dimensione sola; la percentuale a chi non ragiona in pixel; il libero a chi i pixel
di destinazione li scrive e basta. L'elenco è il suo alla lettera, e l'ordine dei gettoni è quello
in cui li ha scritti. ⚠️ **I confini dipendono dal modo**, e il valore salvato si riporta dentro i
suoi **in lettura**: chi sceglie 1600 pixel e poi passa alla percentuale lascia nell'archivio due
chiavi che, lette alla lettera, darebbero un 1600 per cento.

⚠️⚠️ **LA MISURA DI PARTENZA NASCE DA DUE FONTI, E LA RAGIONE È L'EXIF**: `inJustDecodeBounds`
legge le misure del flusso codificato, che per una fotografia scattata in verticale sono quelle
orizzontali, perché la rotazione vive in un tag e la applica chi decodifica. Il **lato lungo**
invece è lo stesso prima e dopo un quarto di giro. Quindi la misura viene da `Pixels` e la forma
dall'anteprima già raddrizzata, che è la stessa divisione che il KDoc di `Pixels` dichiara.
- ⚠️ **E la posa e il ritaglio entrano nel conto** (`Resize.frameSize`): il ridimensionamento si
  applica **dopo** il taglio, quindi chi legge 'da 4000 x 3000' su un'immagine tagliata a metà si
  ritroverebbe un file grande la metà di quello che gli è stato detto.
- ⚠️ **Senza quella misura il tasto 'Salva' non si accende per il solo ridimensionamento**: non
  sapendo se rimpicciolisce, accenderlo prometterebbe una scrittura che potrebbe non fare niente.
  Succede dove il lato lungo non si legge, cioè un indirizzo remoto o un formato che il
  decodificatore non riconosce.

⚠️ **Si applica prima della firma**: la filigrana si scrive sull'immagine finita, e ridimensionando
dopo la firma verrebbe rimpicciolita insieme a lei, cioè disegnata a una misura e resa a un'altra.

⚠️ **Che cosa il banco misura e che cosa no** (`RidimensionaTest`, ogni caso controprovato): che
nessun modo ingrandisca e che la misura identica non conti come lavoro, che ogni modo
governi il proprio lato tenendo le proporzioni, i confini per modo, che la misura di partenza
prenda il lato lungo dal file e la forma dall'anteprima, che la posa scambi i lati e il ritaglio li
riduca, che il valore salvato si rilegga dentro i confini del suo modo, che l'immagine
ridimensionata abbia la misura chiesta e a vuoto non se ne faccia una nuova, che un piano che
rimpicciolisce accenda 'Salva' su un'immagine intonsa **e uno che non rimpicciolisce no**, e i due
gesti del tasto; dalla `2.77` anche che il **piano di fabbrica non faccia niente**, quale campo
ogni modo comanda (col verso giusto su una verticale, che è la cosa che si rompe in silenzio), i
due conti da cui il valore e il campo compagno si ricavano, che nel libero si scrivano tutti e due
i campi e che un gettone ne chiuda uno, che il gettone del libero **non si accenda mai**, che
scrivendo in un campo l'altro segua, che 'Ripristina' torni alle misure correnti senza gettone, e
che la misura entri **dentro** la frase tradotta al posto del suo segnaposto. **Non** vede la resa
del filtro, cioè che l'immagine rimpicciolita sia nitida, né come la finestra si legge sul
telefono: quelle si guardano sul telefono, e la voce di collaudo le chiede.

## 🎛️ I due tasti del salvataggio, e i loro due gesti

⚠️⚠️ **DALLA `2.79` VIVONO NELLA BARRA IN BASSO E NON PIÙ IN TESTATA, ED È SUA RICHIESTA** (punto C
del campo libero del giro dalla `2.75` alla `2.77`: *ci ho ripensato, i tasti 'Filigrana' e
'Ridimensiona' sono troppo lontani e poco raggiungibili dal pollice: mettili nella barra delle
funzioni in basso, non del tutto a sinistra*). In testata ci sono arrivati con la `2.72` e ci sono
rimasti sette versioni; quello che cambia è dove la schermata li mette, e il pezzo che li disegna
resta uno (`EditorToolBar`, in `EditorTools.kt`).
- ⚠️⚠️ **IL BLOCCO CAMBIA LATO COL FAB, E L'ORDINE DEI DUE SI SPECCHIA CON LUI**: la seconda metà
  della sua richiesta dice *con il FAB sul lato opposto, anche 'Filigrana' e 'Ridimensiona'
  cambiano posizione e passano a destra, lasciando un po' di spazio dopo per raggiungibilità*.
  ⚠️ **Che si specchi anche l'ordine interno è una LETTURA dichiarata**: la sua frase sposta il
  blocco e non nomina l'ordine, ma questa è la barra della `2.52`, dove *l'ordine delle icone
  deve essere speculare* con la sola eccezione della coppia del tempo. Specchiati, 'Filigrana'
  resta il più vicino al bordo da cui il pollice arriva, che è la ragione per cui si sono mossi.
- ⚠️⚠️ **E LE DUE METÀ DELLO SPECCHIO VIVONO IN DUE FILE, CHE È QUELLO CHE IL BANCO HA DOVUTO
  SEPARARE**: il **lato** lo decide la schermata, che mette il blocco in testa o in coda alla
  riga; l'**ordine** interno lo decide la barra. Disfacendone una sola, l'altra tiene in piedi
  metà del comportamento, quindi le due controprove sono due.
- ⚠️ **Lo spazio dal bordo è il suo *non del tutto a sinistra*, ed è misurato sul mockup**: là il
  bersaglio del primo tasto comincia a 41 punti dal vetro, e la scheda ne ha già 16 di suoi. Il
  numero scritto è `TOOL_EDGE`, cioè `STAGE_SIDE`: la stessa aria che il palco lascia ai fianchi.
- ⚠️ **'Salva stile' va verso il centro e la coppia resta al bordo**: quel comando c'è nel solo
  modulo Stili, quindi messo per primo sposterebbe i due tasti di quarantotto punti passando da un
  modulo all'altro.

⚠️⚠️ **DALLA `2.72` I DUE GESTI SONO QUESTI: IL TOCCO ACCENDE E SPEGNE, IL TOCCO LUNGO CONFIGURA**
(riscontro del giro della `2.70`,
voce `resize` accettabile: *Il pulsante deve funzionare come l'altro tasto che ho descritto prima:
tocco normale = on/off. Tocco prolungato = imposti il ridimensionamento*; e sulla voce `filigrana`:
*aggiungi un'icona 'Filigrana' in alto a destra, prima di 'Ridimensiona', che si accende o spegne
con un tap normale. Il tap prolungato porta alle impostazioni della filigrana*). Il gesto corto fa
la cosa che si fa a ogni salvataggio, quello lungo la cosa che si fa una volta.
- ⚠️⚠️ **NELLA `2.70` IL RIDIMENSIONAMENTO LI AVEVA ROVESCIATI**, cioè il tocco apriva la finestra
  e il gesto lungo spegneva. Chi trova quella nota in un commento vecchio sappia che oggi i due
  tasti rispondono allo stesso modo: due comandi gemelli a mezzo centimetro di distanza, con i
  gesti scambiati, sarebbero due cose da imparare per un mestiere solo.
- ⚠️ **L'ordine è il suo**, cioè 'Filigrana' prima di 'Ridimensiona'. ⚠️ **'Salva' invece è
  rimasto in testata**, e non è una dimenticanza: è il comando che chiude il lavoro, mentre quei
  due si toccano mentre lo si fa, ed è la distinzione su cui la sua richiesta si regge.

⚠️⚠️ **UN PEZZO SOLO PER TUTTI E DUE (`EditorTool`, in `EditorTools.kt`), E NON È UN RISPARMIO DI
RIGHE**: due disegni separati divergono al primo ritocco, e chi lo vedrebbe per primo è lui, che li
ha davanti insieme. È lo stesso criterio per cui il Ritaglio dell'editor completo chiama le
squadrette di casa. ⚠️ **Con lui `ResizeButton` ha traslocato** e non vive più in
`ResizeDialog.kt`, dove resta la sola finestra.

⚠️⚠️ **`combinedClickable` E NON `Modifier.toggleable`, E LA DIFFERENZA COSTA UNA RIGA**: quel
modificatore mette lo stato nella semantica da sé ma non offre il gesto lungo, quindi si prende il
primo e la semantica si scrive a mano (`toggleableState`). Senza quella riga `Role.Switch` non
annuncia né 'acceso' né 'spento', cioè il tasto è muto per un lettore di schermo e il banco non ha
niente da misurare.
- ⚠️ **L'etichetta del gesto lungo è quella della schermata a cui porta** ('Impostazioni', per la
  filigrana), e non una stringa nuova: dice dove si va, esiste in tutte e ventotto le lingue, e il
  conto delle stringhe si fa prima di cominciare (§ '⚙️ Dove va un'impostazione, e chi la deve
  trovare').
- ⚠️ **A dire se è acceso è l'accento**, e l'inchiostro attenuato dice che il comando è spento del
  tutto, cioè che un salvataggio è in corso: un tasto che non porta un tondo non ha un altro modo
  di dirlo.
- ⚠️⚠️ **'FILIGRANA' NON C'È SENZA UN LOGO SCELTO**, ed è il criterio di 'Mostra nascoste'
  (§ '👁️ Mostra nascoste, e perché dura un minuto'): un interruttore che non cambia nessuna
  immagine è un comando che non fa niente. 'Ridimensiona' invece c'è sempre, perché il piano
  esiste anche spento. ⚠️⚠️ **E LA CODA CHE DICEVA CHE QUELLO DI FABBRICA RIMPICCIOLISCE QUASI
  OGNI FOTOGRAFIA È DECADUTA CON LA `2.77`**: adesso il piano di fabbrica non fa niente
  (`Resize.NONE`), quindi chi accende quel tasto senza aver configurato non toglie un pixel,
  che è la sua specifica letta fino in fondo (*imposti il ridimensionamento dal tasto e poi
  l'interruttore stabilisce se si applica*).

⚠️⚠️ **IL TITOLO SI È ACCORCIATO IN 'MODIFICA' NELLA `2.72`, ED È SUO** (campo libero del giro
della `2.70`: *'Modifica immagine' diventa solo 'Modifica'*). Le due cose si leggono insieme: il
conto della testata era già al limite con un'icona sola (§ '📏 Il ridimensionamento, e i due gesti
di un tasto solo'), e lui ha accorciato il titolo nel giro in cui ha chiesto la seconda.
- ⚠️⚠️ **DALLA `2.79` QUEL CONTO NON SERVE PIÙ E IL TITOLO RESTA CORTO LO STESSO**: i due tasti
  sono scesi nella barra, quindi in testata lo spazio è tornato. Ma 'Modifica' è una sua parola e
  non un rimedio, e un'istruzione non si rovescia perché la sua ragione è caduta.
- ⚠️ **Non è una stringa nuova**: è il testo di `menu_edit`, cioè la voce del menu da cui si entra,
  copiato lingua per lingua. ⚠️ **La chiave resta `editor_title`**, perché il posto
  nell'interfaccia e la chiave nell'archivio delle stringhe sono due cose indipendenti.
- ⚠️ **E la regola della `2.20` non cade**: i due editor si chiamano ancora allo stesso modo in
  testata, perché il titolo dice **che cosa si sta facendo** e non con quale dei due arnesi
  (§ '🎚️ L'editor completo, e il conto che esiste in una copia sola').

⚠️⚠️ **E DALLA `2.73` UNO DEI DUE GLIFI VIVE IN `res/` E L'ALTRO NO, ED È UNA MISURA E NON UNA
DIMENTICANZA** (punto 4 del campo libero del giro della `2.70`: *le icone di 'Filigrana' e
'Ridimensiona' possono venire da Material ma vanno arrotondate come da regola nuova*). La regola
di § '🖌️ Come entra un disegno' dice che a **zero pixel** di scarto vince Material, e
l'arrotondamento a 0,4 lascia a zero solo chi non ha punte: quella della filigrana è una cornice
già stondata con un rettangolino **scavato** dentro, cioè di angoli convessi esterni non ne ha
nemmeno uno; il gemello ne aveva **quarantotto**, e il raccordo gli cambia 451 pixel su 57.600.
- ⚠️⚠️ **MA DALLA `2.79` ANCHE 'FILIGRANA' VIVE IN `res/`, E A FARLA ENTRARE È UNO SPECCHIO**
  (punto B del campo libero del giro dalla `2.75` alla `2.77`: *specchia in orizzontale l'icona
  'Filigrana', in modo che il rettangolino arrotondato cada nell'angolo in basso a sinistra ... è
  ciò che associo istantaneamente al concetto di filigrana perché di solito la metto lì*). Quindi
  la regola non è cambiata, è cambiata la misura: l'arrotondamento su quel disegno valeva **zero
  pixel**, uno specchio ne cambia **10.800 su 57.600**, cioè il 18,75% della tela, e a quel punto
  Material non lo porta più. Il conto e la trappola del verso di percorrenza vivono in testa a
  `res/drawable/ic_watermark.xml`.
- ⚠️ **Quindi dalla `2.73` alla `2.78` 'Ridimensiona' era `ic_resize.xml` e 'Filigrana' restava
  `Icons.Filled`**, e chi legge
  che i due tasti sono gemelli sappia che lo sono nel **pezzo** che li disegna, non nella
  provenienza del glifo.
- ⚠️ **E `MARK_GLYPH` non c'è più dalla `2.79`**: era la costante che teneva il glifo di Material
  in un posto solo, perché lo legge anche il velo che insegna i due tasti, e adesso quel disegno
  è un file che `Glyphs.Watermark` chiama da `res/`, cioè già una fonte sola.

⚠️ **Che cosa il banco misura e che cosa no** (`FiligranaTest` e `RidimensionaTest`, ogni caso
controprovato): che il tocco accenda e spenga, che il tocco lungo apra la finestra o la scheda
delle impostazioni della filigrana, e che senza un logo quel tasto non si disegni affatto;
dalla `2.79` anche che i due tasti stiano **sotto 'Salva'**, cioè nella barra, che rientrino dal
bordo e che col FAB dall'altra parte il blocco passi a destra scambiandoli, con le quattro
controprove che quel caso ha imposto; dalla `2.73` `SviluppoTest` misura la **seconda slide**
dell'onboarding, cioè che arrivi dopo la prima, che le sue copie cadano sui tasti veri e, dalla
`2.79`, che i suoi due paragrafi cadano uno per verso. **Non**
vede il tasto sul telefono, cioè se le due icone stiano comode accanto al titolo accorciato:
quello si guarda sul telefono, e la voce di collaudo lo chiede.

## 🗑️ Lo svuotamento automatico del cestino, e le tre decisioni che lo governano

⚠️⚠️ **LE TRE RISPOSTE SONO SUE, SI CITANO CON LA LORO CHIAVE, E UNA ERA STATA REGISTRATA AL
CONTRARIO** (giro della 1.67; l'archivio del giro è in `Roccobot/tools`, `.memo/files/`):
- `d-cestino-quando`: **`file`**. Ogni file conta la **propria** età e se ne va quando l'ha
  compiuta, come fanno i cestini di sistema. Non si svuota tutto a intervalli.
- `d-cestino-chiusa`: **`aperta`**. La pulizia gira **solo mentre l'app è in primo piano**, e la
  sua formulazione del 2026-09-06 non lascia margine: *nulla deve avvenire al di fuori dell'app
  aperta in primo piano*. Quindi **nessuna operazione programmata di sistema e nessuna libreria
  in più**.
- `d-cestino-editor`: **`fuori`**. Le copie di sicurezza dell'editor **non scadono mai**: si
  tolgono solo svuotando il cestino a mano. Una rete che sparisce da sé non è una rete.

⚠️⚠️ **LA SECONDA ERA SCRITTA ROVESCIATA NEL BRIEF PER DUE GIORNI, e l'ha dovuto trovare lui**
(*nell'apposito artefatto ti avevo già risposto la stessa cosa e tu hai memorizzato il
contrario*). Da quell'inversione erano nati un `WorkManager` da aggiungere, una ragione per
rimandare la tappa, e un blocco del brief che spiegava per bene una cosa falsa. La regola che ne
esce è universale e vive in `Roccobot.md` § '🔑 Una risposta si travasa con la sua chiave, e si
rilegge dalla fonte': quando una scelta a caselle diventa una frase, la chiave si scrive accanto
e l'archivio del giro si riapre prima di scriverla.

⚠️ **Come si distingue una copia di sicurezza da un'eliminazione**: dalla `1.75` l'archivio del
cestino ha una **quarta colonna** facoltativa (`del` o `bak`), e il perché di ogni pezzo, compreso
perché non basta guardare se l'originale esiste ancora, è su `Bin.Record` e su `Bin.expiring`.
- ⚠️ **La colonna è facoltativa per sempre**: un archivio scritto prima della `1.75` deve
  continuare a leggersi, o aggiornando l'app ogni file già nel cestino perde la provenienza, cioè
  non si può più ripristinare.

## ↩️ Disfare una copia o uno spostamento

⚠️⚠️ **DALLA `1.83` ANCHE LA COPIA E LO SPOSTAMENTO OFFRONO 'Annulla', COME L'ELIMINAZIONE**
(campo libero del giro della `1.82`, punto B: *aggiungi degli 'Annulla' temporizzati (avvisi in
basso) anche per le operazioni di copia e spostamento*). La notifica è la stessa e dura gli
stessi tre secondi; a cambiare è che cosa si disfa, e lo dice `Undo.Offer`, che ha due forme: un
ritorno dal cestino, e un elenco di passi da rifare al contrario.

⚠️⚠️ **UN'OPERAZIONE SI DISFA DAI PASSI CHE HA FATTO, NON DA QUELLI CHE DOVEVA FARE**: `FileTree`
restituisce un `Undoable` per ogni file che ha davvero scritto, con dentro dove l'ha messo e, per
uno spostamento, da dove veniva. Ricostruire l'inverso dalla richiesta (la cartella di partenza e
quella di arrivo) sbaglierebbe al primo nome rinumerato, che è il caso normale quando a
destinazione c'è già un file con quel nome.
- ⚠️ **Un file già sparito NON conta come fallito**: fra l'operazione e il tocco su 'Annulla'
  passano dei secondi, e in quei secondi un'altra app può aver cancellato la copia. Il risultato
  voluto è che quel file non ci sia, e quello si è ottenuto: contarlo come errore direbbe che
  qualcosa è andato storto mentre è andato tutto bene.
- ⚠️ **Il ritorno non sovrascrive niente**: se a casa nel frattempo è arrivato un file con lo
  stesso nome, quello resta dov'è e il ritorno prende un nome libero. L'alternativa sarebbe
  cancellare qualcosa che nessuno ha chiesto di toccare, cioè fare danno con il comando che
  serve a ripararlo.
- ⚠️ **'Duplica' passa dalla stessa strada**, perché è una copia dentro la cartella di partenza.

⚠️ **La prova è `DisfareTest`, ed è nata CON il lavoro e non dopo un difetto**: questa funzione
cancella file e ne sposta altri, quindi un suo difetto non si vede e non si può disfare a sua
volta. È il caso proattivo di § '🧪 Quando si scrive una prova, e quando no'.

## 📢 Il canale degli avvisi, e la superficie unica

⚠️⚠️ **DALLA `1.84` L'APP HA UNA VOCE SOLA, ED È SUA RISPOSTA** (`casa` a `d-avvisi`, giro della
`1.81`). La questione era aperta dal censimento della UI del 2026-09-05 e il fatto era questo:
con il cestino acceso un'eliminazione taceva e parlava la notifica di casa, con il cestino spento
la stessa azione produceva un **avviso di sistema**, e copia, spostamento e rinomina parlavano
sempre con l'avviso di sistema. Cioè lo stesso genere di esito aveva **due voci**, e quale delle
due si sentisse dipendeva da un'impostazione che con la notizia non c'entra.

⚠️⚠️ **UN CANALE E NON DICIASSETTE RITOCCHI, e la ragione è la stessa per cui la tappa è
esistita**: gli avvisi erano diciassette in sette file, e correggerne uno per volta avrebbe fatto
una seconda incoerenza al posto della prima, perché la stessa chiamata compare identica in più
schermate. Con `Notices` una schermata nuova non ha un secondo modo di parlare, e la superficie
(`AppNotice`) la disegna **`AivApp`**, sopra la transizione fra schermate: una notizia deve poter
sopravvivere alla schermata che l'ha prodotta.

⚠️⚠️ **UNA RIGA PER VOLTA, E LA PIÙ NUOVA VINCE: la coda non c'è, ed è una scelta.** Una coda
mostrerebbe a turno cose che si riferiscono a un momento già passato, e la seconda arriverebbe
quando la schermata è già cambiata; quello che l'app ha da dire riguarda **l'ultima** cosa
successa. ⚠️ **E con lei sparisce un difetto**: le due notifiche che si coprivano a vicenda
(quella dell'azzeramento e l'offerta di disfare) adesso non possono più coesistere, quindi lo
spegnimento incrociato scritto a mano in `GridScreen` non serve più.

⚠️⚠️ **DUE COSE NON SI TOCCANO, E OGNUNA CHIUDE UNA CORSA.**
- **L'identificatore della riga**: senza, due messaggi **uguali** di fila sono un messaggio solo
  (il conto alla rovescia non riparte e il secondo scade quando scadeva il primo), e un congedo
  cieco porterebbe via il messaggio arrivato nel frattempo. Per questo si toglie con
  `dismiss(id)` e non con un `clear()`.
- **`onGone`**: quello che deve morire con la riga (l'offerta di disfare, o lo stato di una
  schermata) lo dichiara là. Con due attese parallele i due istanti sarebbero due sorgenti della
  stessa verità, e il giorno che una cambia resterebbe in scena un tasto che non fa più niente.

⚠️ **Il testo arriva GIÀ RISOLTO e non come identificatore di risorsa**: metà dei chiamanti
compone una frase con un plurale o con un nome di file, e un canale che accettasse solo
`@StringRes` costringerebbe a due strade. Chi chiama ha già il `Context`, perché serviva anche
all'avviso di sistema.

⚠️ **Le durate erano quelle dell'avviso di sistema**, 2 secondi e 3,5: erano le stesse frasi di
prima, e cambiare superficie **e** tempo insieme avrebbe reso indistinguibili le due cause al
primo 'mi sembra troppo veloce'.
- ⚠️⚠️ **E DALLA `1.85` QUELLA LUNGA È DI TRE SECONDI, PERCHÉ QUEL VINCOLO È SPESO** (riscontro
  del giro della `1.84`, voce `voce-unica` approvata con una domanda: *cosa dura tre secondi e
  mezzo? A meno che non ci sia un motivo specifico, portalo a 3*). La superficie nuova è provata e
  approvata, quindi non c'è più niente da tenere fermo. ⚠️ Adesso coincide con i tre secondi in
  cui si può disfare, e non è un caso da correggere: due frasi che si somigliano restano in scena
  lo stesso tempo.

⚠️⚠️ **DALLA `2.11` LA NOTIFICA SALE SOPRA LA SCHEDA DELLA SELEZIONE, E A MUOVERSI È LEI**
(punto C del campo libero del giro accorpato: *in alcune circostanze (es. si inizia una selezione
dopo un 'copia', 'sposta' o 'elimina'), la bottomsheet della selezione va a finire sotto la
notifica in basso*). Le due superfici sono appoggiate allo stesso bordo e nascono da due gesti che
si susseguono, quindi prima o poi si incontrano.
- ⚠️⚠️ **LA SUA PROPOSTA ERA IL CONTRARIO, E LA SCELTA È DICHIARATA** (*finché è visibile l'avviso
  la bottomsheet arriva più in alto e poi si abbassa?*): muovere la scheda sposterebbe i
  **comandi** mentre il dito sta per toccarli, che è la stessa famiglia del difetto della griglia
  che scorre sotto un dito appoggiato (la nota della `1.78` sull'intestazione). Una notifica
  invece si tocca di rado, e quando la si tocca è per disfare, cioè prima che la selezione
  ricominci.
- ⚠️⚠️ **NON C'È NESSUNA ANIMAZIONE IN PIÙ, E QUI STA IL VALORE**: la scheda dichiara a ogni
  fotogramma quanta parte di schermo occupa (`PickStage`), e chi le si appoggia sopra la segue **per
  costruzione**, senza una seconda curva da tenere allineata alla prima. Due animazioni scritte in
  due posti divergono al primo ritocco.
- ⚠️ **Un oggetto di processo e non un `CompositionLocal`**: chi deve sapere (la notifica di casa e
  la fascia della copertina) vive sopra la transizione fra schermate, cioè in un ramo che non
  discende dalla scheda, e un local va dall'alto in basso.
- ⚠️⚠️ **E IL VALORE SI LEGGE IN COMPOSIZIONE, PERCHÉ LA VIA CHE COSTA MENO NON FUNZIONA**: letto
  dentro `Modifier.offset { }`, cioè nella fase di layout, quel lambda è stato valutato **una
  volta sola** con lo zero di partenza e non è più tornato quando il valore è salito. Perché non
  torni non si sa, e si scrive così invece di inventare una causa: il sospetto è che a scriverlo
  sia un `onGloballyPositioned`, cioè la stessa passata che dovrebbe rileggerlo.

⚠️⚠️ **E DALLA `2.24` SALE ANCHE SOPRA IL FAB, PERCHÉ I CHIEDENTI SONO DIVENTATI DUE**
(segnalazione dell'utente, punto A1 del campo libero del giro della `2.23`: *la notifica inferiore
con 'Annulla' (es. per 'Sposta') a volte va sopra il FAB (su qualunque lato sia). Si può aggirare
il problema?*). Sì, e col meccanismo che c'era già: `PickStage` diventa `FootStage`, cioè una
**mappa** di chi occupa il fondo dello schermo, e la notifica si alza del massimo.
- ⚠️⚠️ **NON SI INCONTRANO 'A VOLTE': SI INCONTRANO SEMPRE**, e il 'a volte' della sua frase è
  quanto spesso capita di avere una notifica in una schermata col FAB. Quel tasto vive in un
  angolo, la notifica è larga quasi tutto lo schermo, e a disegnarla è la radice dell'app, cioè
  **dopo** la schermata: la sovrapposizione è per costruzione, non una combinazione sfortunata.
- ⚠️ **A dichiarare l'ingombro è `TapHoldFab` e non i due chiamanti**, così un FAB nuovo lo fa per
  costruzione: è lo stesso criterio per cui `lowered()` si porta dietro il velo, e per cui l'uscita
  verso una schermata senza FAB la legge quella funzione.
- ⚠️ **La misura regge anche a menu aperto**, quando il FAB si stacca in una finestra sua: là resta
  un segnaposto della stessa misura, che vive nella finestra dell'app ed è quello che si misura.
- ⚠️ **Il costo è dichiarato**: nelle due griglie il FAB c'è sempre, quindi là la notifica sta
  stabilmente più in alto di prima. È il prezzo di non muovere un comando, che è il criterio della
  `2.11`, e la voce di collaudo lo mette per iscritto.
- **La prova è `AvvisiTest`**, con lo stesso caso della scheda della selezione su un secondo
  chiedente. ⚠️ Controprovata togliendo la dichiarazione al FAB: la notifica torna a coprirlo.

⚠️⚠️ **MA DALLA `2.25` NON SALE: SI STRINGE ACCANTO AL FAB, ED È LA SUA RISPOSTA `stringe` A
`d-avviso-forma`** (giro della `2.24`, voce `avviso-fab` approvata con una richiesta: *non si
potrebbe fare lo stesso avviso meno largo di quel tanto che basta a stare a fianco del FAB?*).
Salire è la risposta giusta davanti a una scheda larga tutto lo schermo, perché accanto non c'è
niente; davanti a un tasto che vive in un angolo lascia vuota una striscia larga quanto la
finestra, mentre lo spazio di fianco c'è.
- ⚠️⚠️ **QUINDI `FootStage` TIENE DUE COSE E NON UNA**: chi occupa una **fascia** (la scheda della
  selezione) e chi occupa un **fianco** (il FAB). Un modificatore solo, `aboveFoot`, le legge tutte
  e due: sale sopra la prima e si stringe accanto al secondo.
- ⚠️⚠️ **DA CHE PARTE STA IL FAB SI MISURA E NON SI LEGGE DA `fabSide`**: il nodo sa dov'è nella
  finestra, quindi il lato si ricava dal suo centro. Leggendo la preferenza ci sarebbero due posti
  a decidere dov'è quel tasto, e il giorno che uno dei due cambia la notifica si stringerebbe
  dalla parte sbagliata. È la sua stessa osservazione (*'di fianco' ha un significato di default e
  un altro se il FAB è a sinistra*), risolta misurando.
- ⚠️ **Qui il rientro è la scelta giusta, al contrario della salita**: la larghezza di un comando
  non cambia mentre lo si guarda, quindi una rimisurazione si paga solo quando quel comando compare
  o sparisce; l'alzata invece è animata, e là un `padding` costerebbe una misura per fotogramma.
- ⚠️ **Il testo ha meno spazio, e lo ha previsto lui** (*ci sarebbe meno spazio per il testo, ma
  basta diminuire un pelo la spaziatura o la larghezza o il corpo del carattere*): il corpo non è
  stato toccato, perché quanto si perde si vede sul telefono e non sul banco. La voce di collaudo
  glielo chiede, insieme al suo *potrebbe anche essere un'opzione*, che per ora non è un'opzione.
- **La prova sono i due casi di `AvvisiTest`**, uno per lato. ⚠️⚠️ **E LA CONTROPROVA HA TROVATO UN
  DIFETTO NELLA PRIMA STESURA**: misurava `onNodeWithText`, cioè la frase, che dentro la sua
  superficie finisce ben prima del bordo, quindi col rientro tolto a mano il caso del FAB a destra
  **restava verde**. Adesso si misura il nodo della notifica.
- ⚠️⚠️ **E CHI SALE NON SI STRINGE, DALLA `2.26`, ED È LA SUA RISPOSTA `sempre` A
  `d-avviso-forma-2`** (giro della `2.25`: *può stare massimizzata il larghezza solo quando (per la
  presenza della bottomsheet) si sposta sopra*). Sopra una scheda larga tutto lo schermo non c'è
  nessun comando da schivare di fianco, quindi rientrare là costerebbe spazio al testo senza
  guadagnare niente. ⚠️ **Oggi i due casi non si incontrano quasi mai**, perché appena c'è una
  selezione il FAB lascia il posto alla scheda: 'quasi' non è una regola, e durante quel cambio le
  due dichiarazioni convivono per qualche fotogramma. A presidiarlo è il terzo caso di
  `AvvisiTest`, controprovato togliendo la condizione.

⚠️⚠️ **E LA MINIATURA VECCHIA NON SE N'ERA ANDATA: DALLA `2.25` LE VIE CHIUSE SONO TRE, E LA CAUSA
NON È ACCERTATA** (riscontro del giro della `2.24`, voce `mini-cestino` non approvata: *ancora
sbagliata, solo nella miniatura (griglia). Era così anche prima*). La correzione della `2.24`
buttava la cache di Coil per l'indirizzo dichiarato cambiato, e quello che il suo riscontro dice è
che il difetto non passava di lì.
- **Via 1, la miniatura del SISTEMA**: il provider tiene le proprie per **riga** e non per
  contenuto, quindi una fotografia riscritta sopra il proprio indirizzo può farsi servire quella di
  prima, e `Thumbs.forget` su quella cache non ha nessuna presa. ⚠️ **La nota della `2.24` diceva
  che la rifaceva il MediaScanner**: non era misurato, e il sintomo dice il contrario. Adesso un
  indirizzo dichiarato riscritto **salta la strada di sistema una volta** e passa dalla decodifica
  normale, che apre il file vero: non si cura la causa, si chiude la via.
- **Via 2, il tetto della mappa di casa**: `Thumbs.forget` cercava la chiave in una mappa di 64
  voci, mentre la cache di Coil ne tiene quante la memoria le concede, quindi oltre quel numero non
  c'era niente da rimuovere. Adesso si cercano tutte le chiavi che portano quell'indirizzo.
- **Via 3, i percorsi del CESTINO**: là il MediaStore non vede niente, quindi `FileTree.scan` non
  produce nessun indirizzo e la chiamata che vive dentro non si fa **mai**; e quei percorsi si
  riusano, perché il nome lo sceglie `FileTree.freeName`. Adesso il cestino dichiara i propri, sia
  quando un file arriva sia quando se ne va.
- ⚠️ **E lo `scan` butta anche la miniatura del `file://`**, che è un secondo indirizzo per lo
  stesso file: nella vista 'Cartelle di sistema' le immagini viaggiano come percorsi, quindi la
  stessa fotografia si correggeva in una vista e non nell'altra.
- ⚠️⚠️ **CHE COSA RESTA FUORI, E SI DICHIARA**: perché la miniatura sbagliata compaia proprio là
  dipende da come il provider rinumera e da che cosa tiene in cache, e senza il telefono non si
  misura. Quello che si è fatto è togliere **tutte** le strade per cui l'app può servire una
  miniatura vecchia per un indirizzo dichiarato cambiato.
- **La prova è `MiniatureTest`**, quattro casi, controprovati uno per uno rimettendo il difetto.
  ⚠️ **Non** vede la miniatura che si vede: quella la fa il provider, e il banco non ne ha uno.

⚠️⚠️ **UNA MINIATURA VECCHIA SOPRAVVIVE A UN FILE CHE CAMBIA, E DALLA `2.24` LO SCAN LA BUTTA**
(segnalazione dell'utente, punto A2 dello stesso campo libero: *se si modifica una foto, poi si
recupera la copia di backup dal cestino, questa ha la miniatura dell'immagine modificata, non la
propria. Per le copie di sicurezza l'anteprima va ricreata quando escono dal cestino*). La cache
di `Thumbs` è indicizzata sull'**indirizzo** e non sul contenuto, ed è l'unica dell'app che possa
mentire: Coil qui non ha cache su disco, e quella di sistema si rifà da sé con lo scan.
- **Il rimedio vive in `FileTree.scan`**, cioè dove il chiamante **dichiara** che un percorso è
  cambiato: le due cose vanno insieme, e scritte là un chiamante nuovo le prende per costruzione
  invece di doversi ricordare la seconda riga. `Bin.restore` chiamava già lo scan.
- ⚠️⚠️ **LA CAUSA NON È ACCERTATA FINO IN FONDO, E SI SCRIVE COSÌ**: perché il file ripristinato si
  ritrovi l'indirizzo di quello riscritto dipende da come il MediaStore rinumera, e senza il
  telefono non si misura. Quello che si è fatto è **chiudere l'unica via** per cui l'app può
  mostrare una miniatura vecchia, e la voce di collaudo lo dice a lui.
- ⚠️ **Non ha una prova del banco, e va detto**: il callback del MediaScanner in Robolectric non
  arriva, quindi una prova misurerebbe una riga che non gira. Si guarda sul telefono.

⚠️⚠️ **UN AVVISO DI SISTEMA RESTA, ED È UNO SOLO**: quello che spiega perché si sta per aprire la
pagina delle impostazioni di Android (`folder_why`, in `ViewerActivity`). Là l'app va in
**secondo piano** nello stesso istante, quindi una notifica di casa non si vedrebbe affatto: dire
una cosa mentre si esce è il caso per cui l'avviso di sistema esiste. Chi ne aggiunge un altro
dichiari la stessa cosa, o è un ritorno alle due voci.

⚠️⚠️ **E IL BANCO HA UNA TRAPPOLA SUA, misurata scrivendo `AvvisiTest`**: [Notices] è un oggetto
di **processo**, e una sua scrittura fatta **dopo** `setContent` non arriva alla composizione
finché lo snapshot non viene propagato, cosa che col clock fermo non succede da sé. La spia messa
nell'albero leggeva `null` mentre lo stato portava già il messaggio. Quindi in una prova la riga
si mette **prima** di montare la scena, che è anche il caso vero: una notifica nasce da un gesto
fatto in una schermata già in scena.
- ⚠️ **E il clock va fermato** (`autoAdvance = false`): con l'avanzamento automatico
  `waitForIdle` porta a termine le attese pendenti, cioè fa **scadere** la notifica prima che la
  prova possa toccarla.

## ⚙️ Dove va un'impostazione, e chi la deve trovare

⚠️⚠️ **UNA VOCE VA CON QUELLE CHE RISPONDONO ALLA SUA STESSA DOMANDA, e la domanda è quella
di chi apre il pannello, non quella del codice.** La si scrive in una riga, nella forma 'come
faccio a...', e le voci che la condividono sono una **famiglia**. Non fanno famiglia le voci
che il codice legge nello stesso ramo, né quelle che agiscono sulla stessa schermata: quello è
il posto in cui l'effetto si **vede**, e non è il posto in cui la voce si **cerca**. Il
precedente è una correzione dell'utente ed è costata una versione (*l'opzione relativa allo
zoom va messa nella sotto-pagina 'Adattamento e zoom' delle impostazioni. Quando aggiungi
un'impostazione nuova, attenzione a metterla nel posto giusto*): quella voce agisce sul menu
del visualizzatore ed è finita nella pagina dello zoom, perché di zoom parla.
- **La prova che una famiglia è una**: se per elencarne le voci serve una `e` fra due domande
  diverse, sono due famiglie e si contano separate.
- ⚠️ **Una collocazione che ha bisogno di giustificarsi è una famiglia che non esiste ancora.**
  Il pannello ne ha portati tre per versioni, ognuno con la sua scusa scritta accanto: il
  cestino messo fra le cartelle *per mancanza di uno migliore*, lo sfoglio delle sole immagini
  messo accanto a lui, e l'interruttore della barra delle info lontano dall'elenco dei dati che
  governa. Quando si scrive una scusa, la voce ha trovato il posto sbagliato.

⚠️⚠️ **FAMIGLIA E SEZIONE SONO DUE COSE, E LA SOGLIA SI CONTA SULLA FAMIGLIA.** La **famiglia**
sono le voci di una domanda sola; la **sezione** è il titolo di gruppo nella pagina piatta, e
raccoglie le famiglie vicine per dire, mentre si scorre, di che cosa si sta parlando.
Confonderle è l'errore che manda dietro un tocco il tema dell'app: sotto 'Aspetto' vivono il
tema e l'effetto dietro i pannelli, cioè due famiglie, e nessuna arriva alla soglia.
- **La soglia è dell'utente** (*fino a 2-3 opzioni correlate basta una sotto-sezione della
  pagina principale; più di 2-3 si va con la sotto-pagina*) e si applica alla lettera. Il
  trasloco si fa **nello stesso giro** in cui entra la voce che fa scattare la soglia, e nello
  stesso giro si copre la ricerca: una famiglia che scende dietro un tocco senza copertura esce
  dalla ricerca, e quello peggiora l'app.
- ⚠️ **Un titolo di sezione può nominare due famiglie vicine**, come Modifica e backup, e non
  viola la prova della `e`: quella prova dice quando due **voci** non rispondono alla stessa
  domanda, e una sezione a nessuna domanda risponde, dice dove si è.
  - ⚠️ **L'esempio era 'Video e scorrimento' fino al 2026-09-04**, e la `1.48` lo ha fatto
    sparire riscrivendolo in 'Navigazione' su istruzione dell'utente. Un esempio preso da una
    stringa pubblicata invecchia il giorno che quella stringa cambia: quello nuovo è di nuovo
    vero oggi, e il criterio non dipende da nessuno dei due.

⚠️⚠️ **SOTTO-PAGINA SI DIVENTA IN QUATTRO MODI, E OGNUNO SI DICHIARA QUANDO LA PAGINA NASCE.**
Due erano già scritti in testa a `SettingsScreen.kt` e restano; il terzo è la soglia; il quarto
nasce con la `1.54`.
- Perché la voce è un **elenco** che cresce e porta comandi propri riga per riga.
- Perché le voci sono **delicate** e il tocco in più è una protezione (*sono impostazioni
  delicate*: da lì è nata 'Adattamento e zoom').
- Perché la **famiglia** ha superato la soglia.
- ⚠️ Perché è un **comando che ha bisogno di un paragrafo**, e non è un'impostazione affatto:
  da lì nasce 'Elimina le miniature memorizzate', chiesta così dall'utente (*un > che ti porta
  ad una sotto-schermata dove c'è un avviso al centro ... Sotto, un pulsante*). ⚠️ **Non è il
  secondo modo travestito**, ed è la distinzione che tiene chiuso l'elenco di che cosa è
  delicato: lui stesso ha detto che *non è un'operazione con risvolti potenzialmente dannosi*,
  quindi il tocco in più non protegge niente. Quello che il tocco compra è lo **spazio**: un
  avviso di quattro righe dentro la pagina piatta darebbe una riga alta il doppio delle altre
  per un comando che si dà una volta l'anno.
  - **Il paletto che lo tiene stretto**: il paragrafo dev'essere **necessario**, cioè spiegare
    che cosa succede dopo che si è toccato. Una spiegazione che il titolo già dà non lo rende
    necessario, e allora la voce resta nella pagina piatta come tutte le altre.
- ⚠️⚠️ **E DALLA `2.09` CE N'È UN QUINTO, CHE È UNA SUA SCELTA E NON UN CRITERIO NUOVO**
  (`d-imp-strada` del giro della `2.07`: **`livelli`**): la famiglia **ne contiene già
  un'altra**. Nella pagina piatta la riga che apriva la pagina interna e le sue voci sorelle si
  leggevano allo stesso livello, quindi la famiglia non si vedeva come una cosa sola; con la
  porta si vede, e il suo riepilogo dice che cosa c'è dentro. È il modo con cui nascono le
  cinque pagine del primo livello, ed è esattamente il caso che la profondità uno vietava.

⚠️⚠️ **E L'ELENCO DI CHE COSA È DELICATO È CHIUSO, DUE CASI E NON PIÙ**: sbagliare la voce può
costare un file, o toglie la rete che lo protegge; oppure la voce cambia il **metro** con cui
un'immagine viene misurata, quindi rende ogni immagine diversa da come ci si aspetta senza
rompere niente. Un elenco aperto si allarga da sé: qualunque voce, con abbastanza argomenti, si
guadagna il tocco in più, e la pagina piatta si svuota una riga per volta.
- ⚠️ **Il rovescio resta vero, ed è la clausola che vale più di tutte**: una riga sola che non è
  né un elenco né delicata, in una sotto-pagina costerebbe un tocco senza guadagnare niente.

⚠️⚠️ **LA PROFONDITÀ È DUE DALLA `2.09`, E FINO ALLA `2.08` ERA UNO** (sua risposta `livelli` a
`d-imp-strada`, giro della `2.07`). La regola di allora diceva che *una sotto-pagina non ne apre
un'altra, perché la navigazione è un valore solo senza pila e Indietro riporta alla radice*:
adesso la navigazione è una **pila**, quindi una pagina ne può aprire un'altra e Indietro risale
un gradino per volta.
- ⚠️⚠️ **QUELLO CHE LA REGOLA VECCHIA VIETAVA ERA PROPRIO IL CASO DELLA STRADA B**: quattro
  delle cinque famiglie scese ne contengono già una (l'elenco dei dati, l'ordine dei pulsanti,
  la vista delle cartelle con le nascoste, 'Rinomina e download'), quindi senza la pila non si
  potevano chiudere. La regola non è caduta per comodità: è caduta perché lui ha scelto la
  strada che la rendeva falsa.
- ⚠️ **Due e non di più**: la pila regge qualunque profondità, ma le famiglie di questo pannello
  arrivano a due, e un terzo livello vorrebbe dire una famiglia dentro una famiglia dentro una
  famiglia, che è più di quanto chi cerca una voce tenga a mente.
- ⚠️ **La pila è misurata e non ragionata** (`ImpostazioniTest`): si aprono due livelli, il primo
  Indietro deve riportare a quello di sopra e il secondo deve uscire. Controprovata rimettendo il
  `back()` che svuotava, cioè il comportamento della `2.08`.
- ⚠️⚠️ **E LA COPERTURA DELLA RICERCA SI ANNIDA DA SÉ, CHE ERA LA COSA DA MISURARE**: il corpo di
  una pagina si compone **dentro** il provider della ricerca, quindi una pagina dentro una pagina
  incontra la stessa condizione una seconda volta e si appiattisce a sua volta. Misurato cercando
  una voce di 'Adattamento e zoom', che vive due livelli sotto, e controprovato togliendo
  l'appiattimento alla porta che la contiene.

⚠️⚠️ **LE PAGINE DEL PRIMO LIVELLO, E LE DUE SEZIONI SPARITE CON LORO** (dalla `2.09`):
**'Cartelle'** (intestazione, colore, opzioni di visualizzazione, nascoste),
**'Visualizzatore'** (sfondo, tinta dello sfondo, adattamento e zoom, riproduzione diretta dei
video), **'Informazioni'** (barra delle info, elenco dei dati, contatore dei fotogrammi, peso di
una selezione), **'Comandi e tasti'** (lato del FAB, etichette, ordine dei pulsanti) ed
**'Editor e salvataggio'** (editor, copia di sicurezza, rinomina e download).
- ⚠️⚠️ **'Visualizzatore' E 'Cartelle' NON SONO PIÙ TITOLI DI SEZIONE, e i loro testi non sono
  stati tradotti di nuovo**: titolano le due pagine omonime. Una sezione che conterrebbe
  **soltanto** la porta della propria famiglia scriverebbe la stessa parola due volte a mezzo
  centimetro di distanza, che è il caso di *una voce sola non prende un titolo*.
- ⚠️ **Le tre porte che restavano senza sezione vivono sotto 'Aspetto'**, che è la sezione della
  domanda *che cosa vedo*: come si vedono le cartelle, come si vede un'immagine aperta, che cosa
  l'app dice di lei.
- ⚠️⚠️ **GLI OTTO TRASLOCHI NON TOCCANO NESSUNA CHIAVE**, quindi nessuno perde le sue scelte
  aggiornando: il posto nell'interfaccia e la chiave nell'archivio sono due cose indipendenti.
- ⚠️ **Le stringhe nuove sono i titoli delle tre pagine che non ne avevano uno**, più la
  spiegazione della riproduzione diretta dei video: quella voce era **muta** per richiesta
  dell'utente (*senza testo esplicativo*) perché stava sotto 'Sfoglia solo le immagini', che la
  spiegava, e il trasloco l'ha staccata da lei.

⚠️ **UNA VOCE SOLA NON PRENDE UN TITOLO**, e va nella sezione della famiglia la cui domanda le
è più vicina, mai sopra un titolo, perché sopra un titolo si legge come la prima riga di
quello che segue. L'unica eccezione è dell'utente ed è dichiarata nel codice (*nelle
impostazioni creiamo una nuova sezione **Funzionalità avanzate** al cui interno c'è una
voce...*), dove il titolo è **metà dell'avviso** su una funzione che può fare danni. Fuori da
quel caso un titolo con una riga sotto è una parola in più che non aiuta a trovarla.

⚠️ **UNA VOCE FRA DUE FAMIGLIE VA DOVE SI CERCA, NON DOVE SI VEDE**, e se dopo questa prova la
scelta resta in bilico va nella famiglia più **piccola**: una famiglia grande non si accorge di
una voce in più, una di due sì, e nella famiglia piccola la voce si trova scorrendo. In quel
caso soltanto, la riga riceve fra i testi che la ricerca confronta il nome della sezione in cui
l'effetto si vede, e la ragione della scelta si scrive accanto alla riga: il ballottaggio si
paga una volta, la ricerca lo annulla sempre, e quei titoli esistono già. ⚠️ **Non si mette una
voce in due famiglie per farla trovare**: a farla trovare ci pensa la ricerca.

⚠️⚠️ **LA RICERCA DEVE TROVARE OGNI VOCE, DOVUNQUE VIVA, E LA COPERTURA SI SCRIVE NELLO STESSO
GIRO DELLA SOTTO-PAGINA.** Il fatto da cui parte tutto è misurato: `LocalQuery` è fornito nel
solo ramo della radice, le sotto-pagine non lo ricevono, e là con la ricerca vuota `shown`
risponde di sì a tutto. Quindi una voce spostata dietro un tocco **esce** dalla ricerca, e la
ricerca è una richiesta dell'utente.
- **Pagina fatta di RIGHE**: mentre una ricerca è in corso la radice compone il **corpo** della
  pagina al posto della riga che la apre (`PageOfRows`). Le righe si filtrano già da sé; un
  blocco scritto a mano (un cursore, una casella, un tasto) si avvolge in `Searchable`, o resta
  in scena mentendo.
- **Pagina che è un ELENCO con comandi per riga**: il corpo **non** si appiattisce, perché le
  frecce lavorano sull'ordine intero e sposterebbero un campo in una posizione che non si vede.
  La riga che apre la pagina riceve invece le parole delle righe interne come testi in più
  (`extra` di `PageRow`): i nomi dei campi esistono già in tutte le lingue, e un nome di
  cartella è un dato.
- ⚠️ **La terza via non si accetta, cioè fidarsi del riepilogo**: un riepilogo scritto a mano
  invecchia al primo trasloco, e il precedente è misurato (quello di 'Adattamento e zoom'
  nominava tre argomenti mentre la pagina ne portava quattro dalla `1.26`).
- **Il collaudo di una voce nuova è di due tocchi**: si cerca una parola del titolo e una della
  spiegazione, e la voce deve comparire da sé. Se compare la riga che apre la pagina invece
  della voce, il rimedio non è scrivere quella parola nel riepilogo, è coprire la voce.
- ⚠️⚠️ **E DALLA `2.11` UNA RICERCA FINISCE QUANDO PORTA DA QUALCHE PARTE** (riscontro del giro
  della `2.10`, voce `imp-cerca-titoli` approvata con una nota: *se dalla ricerca poi approdo ad
  un elemento con cui interagisco (es. apro una sotto-pagina), la ricerca si resetta e torno
  all'inizio delle impostazioni senza nulla digitato nel 'cerca'*). Una ricerca è il modo di
  **arrivare** a una voce, non uno stato in cui restare: trovarsela ancora accesa al ritorno
  costringe a svuotare il campo per rivedere il pannello intero.
  - ⚠️ **Vale per la NAVIGAZIONE e non per ogni tocco**, che è più stretto della lettera della
    sua frase: un interruttore toccato mentre la ricerca è in corso deve restare dove lui lo
    sta guardando, e svuotare il campo glielo farebbe sparire da sotto il dito.
  - ⚠️ **Si scrive nella funzione che apre una pagina e non nei chiamanti**: ogni `PageRow`
    passa di là, quindi una voce nuova prende la regola per costruzione.
  - ⚠️ **Lo scorrimento torna in cima solo se la ricerca c'era**: senza quella condizione si
    rifarebbe il difetto che la radice esiste per evitare, cioè una pagina piatta che si azzera
    a ogni giro in una sotto-pagina.

⚠️⚠️ **UNA VOCE PUÒ VIVERE IN DUE POSTI: IL PANNELLO È LA CASA, IL DIALOGO 'OPZIONI DI
VISUALIZZAZIONE' È LA SCORCIATOIA.** Quattro clausole, e nessuna è negoziabile.
1. **Una preferenza, una chiave, un valore di fabbrica.** Le due superfici scrivono la stessa
   cosa e passano dallo stesso salvataggio, com'è già per le colonne (*che resta globale per
   tutte le cartelle*). Un valore 'della sessione' sarebbe una terza cosa da capire.
2. **La casa ha titolo e spiegazione, la scorciatoia no.** Nel dialogo la voce compare nuda e
   solo per la vista scelta, perché il titolino dice già di che cosa si parla e un dialogo con
   tutte le voci sarebbe un secondo pannello.
3. **Chi tocca la voce tocca due posti**: la riga è scritta due volte con due componenti, e un
   cambiamento di forma va fatto in entrambi o divergono.
4. ⚠️ **Una preferenza che vive SOLO nella scorciatoia è ammessa, e si dichiara nel KDoc del
   campo**, come `folderView` (*Non compare nella schermata delle impostazioni, e non è una
   dimenticanza*): una vista si sceglie guardandola. Senza quella riga scritta, l'assenza dal
   pannello non è una scelta ma un difetto, ed è quello che è stata fino alla `1.46` per le
   opzioni delle altre due viste.

⚠️ **LA RIGA DI UN INTERRUTTORE È UN BERSAGLIO SOLO, IN TUTTE E DUE LE SUPERFICI**: `toggleable`
con `role = Role.Switch` sulla riga, e dentro l'interruttore niente. Non basta un `clickable`
sulla riga: lasciando vivo anche l'interruttore i bersagli diventano due e un lettore di schermo
annuncia due voci per una scelta sola, che era il difetto del dialogo mentre il pannello non
aveva bersaglio affatto. Il tocco lo mette il **componente** e non il chiamante, così una voce
nuova ce l'ha per costruzione.

⚠️ **LE CHIAVI NON SI TOCCANO QUANDO UNA VOCE SI SPOSTA**: il posto nell'interfaccia e la chiave
nell'archivio sono due cose indipendenti, e chi aggiorna non deve perdere le sue scelte. Una
chiave nuova si scrive quando la **domanda** cambia verso, e allora la voce non è spostata ma
nuova (il precedente è `sequence-reversed`). ⚠️ E una voce nuova tocca **cinque punti**, che
vanno fatti tutti e cinque perché nessuno li controlla al build: il campo col suo KDoc, la
chiave, la lettura nel flusso, la scrittura in salvataggio, la riga nella schermata. Il KDoc
dice **perché** quello è il valore di fabbrica, e il valore di fabbrica non si sceglie per far
vedere la funzione.

⚠️ **IL CONTO DELLE STRINGHE SI SCRIVE NELLA PROPOSTA, PRIMA DI COMINCIARE.** Un testo nuovo si
scrive a mano in tutte le cartelle di lingua, e un plurale costa molto più di una stringa.
Quindi: si riusa una stringa che dice **esattamente** quella cosa e non una che le somiglia (la
descrizione parlata di una miniatura non diventa il titolo di una sezione, o un ritocco di
accessibilità rinomina una sezione in silenzio); un titolo che la prima riga della famiglia già
dice non si scrive; e il riepilogo di una pagina si **compone** dalle stesse stringhe che la
pagina usa dentro, così non può invecchiare. ⚠️ **Ogni testo nuovo di questo pannello si valida
prima del rilascio**, qualunque sia la sua lunghezza: finché non è validato la modifica non è
pronta, e questo non è in conflitto col go-live, che riguarda il pubblicare una modifica già
pronta.

⚠️ **QUELLO CHE NON DECIDE**: il gruppo in cui la voce era prima, la comodità del codice, la
lunghezza della pagina piatta, e il fatto che una sezione risulti sbilanciata. Se una famiglia
viene di quattro voci e un'altra di una, sta bene: le domande non si fanno tutte con la stessa
frequenza. ⚠️ **E i conti non si scrivono**, qui come nei commenti del pannello: quante sono le
sezioni, le famiglie e le voci si contano nel codice.

## 🇺🇸 L'inglese dell'app è americano

⚠️⚠️ **DALLA `1.92`, ED È SUA RISPOSTA** (`d-inglese-colore` del giro della `1.91`: **`americano`**,
cioè *uniforma tutto all'americano*). La domanda era nata da una sua riscrittura (*Folder color*)
che aveva messo una grafia americana in un'app scritta in britannico: nella stessa pagina delle
impostazioni si leggevano 'Folder color' e 'Header colour'.
- **Che cosa è cambiato**: nel solo file inglese, `colour` diventa `color`, `centre` diventa
  `center`, `Greyscale` diventa `Grayscale`. ⚠️ **I nomi delle chiavi NON si toccano**
  (`settings_colour`, `facts_colour_grey`): il posto nell'interfaccia e la chiave nell'archivio
  sono due cose indipendenti, e rinominarle non cambierebbe niente per chi usa l'app.
- ⚠️ **Le altre ventisette lingue non c'entrano**: la scelta riguarda l'inglese, e l'italiano dice
  'Scala di grigi' come prima.
- ⚠️ **Da qui in poi l'inglese nuovo si scrive americano**, che è la metà della risposta che vale
  per il futuro: una stringa nuova in britannico rimetterebbe le due grafie nella stessa pagina.

## 🏷️ L'indicatore dell'ultimo media, e la sua migrazione

⚠️⚠️ **DALLA `2.11` I SEGNI SONO DUE E SI SCEGLIE, ED È SUA RICHIESTA** (punto D del campo libero
del giro accorpato: *aggiungi 'Indicatore dell'ultimo media visualizzato' con due chip di
selezione esclusiva: 'Cornice' e 'Angolo' (predefinito di fabbrica: 'Cornice')*). Fino alla `2.10`
il segno era uno solo, il nastro triangolare nell'angolo, e non si poteva cambiare.

⚠️⚠️ **LA CORNICE RIMETTE IN SCENA QUELLO CHE LA `0.58` AVEVA SCARTATO, E L'ARGOMENTO DI ALLORA
REGGE ANCORA**: là la cornice era fra le cinque proposte e lui aveva scelto il nastro, perché *una
cornice attorno a una miniatura è il gesto universale della selezione, quindi da lontano quel segno
dice la cosa sbagliata*. Quello che è cambiato non è l'argomento, è la sua preferenza, e la scelta
resta doppia proprio perché i due segni dicono cose diverse. Chi trova quella nota in `GridScreen`
non la corregga: vale per tutte e due le versioni.

⚠️⚠️ **CHI AGGIORNA TIENE L'ANGOLO, ED È L'ALTRA METÀ DELLA SUA CLAUSOLA** (*chi aggiorna dovrà
trovare la doppia scelta, ma anche se 'Cornice' è l'impostazione di fabbrica, deve restare
'Angolo', per non stravolgere la UI di chi è già utente*). Cioè il valore di fabbrica è uno solo, e
da che parte cade dipende da quando l'app è arrivata sul telefono.
- ⚠️⚠️ **SI DECIDE UNA VOLTA SOLA E SI SCRIVE, e la via che sembrava più corta è un difetto**:
  leggere 'l'archivio è vuoto' a ogni lettura darebbe la cornice finché l'utente non tocca una
  qualunque altra impostazione, e da quel momento l'angolo. Una **migrazione** dell'archivio gira
  prima della prima lettura e lascia una risposta che non cambia più.
- ⚠️ **Il segno di 'già utente' è che l'archivio porti qualcosa**, e regge perché in quello store
  vivono anche i promemoria che l'app scrive da sé: la domanda sul permesso ai file, che si fa al
  primo avvio, e i mini onboarding.
- ⚠️ **Il caso limite si dichiara**: un archivio davvero vuoto è indistinguibile da
  un'installazione nuova, e là arriva la cornice. Sono i telefoni su cui l'app è stata installata
  e mai aperta.

⚠️⚠️ **DALLA `2.12` LA CORNICE È ARANCIONE, SPESSA IL 5% DEL LATO E OPACA ALL'80%, E I TRE NUMERI
SONO SUOI** (riscontro del giro della `2.11`, voce `ind-ultimo` non approvata: *se lo spessore
viene dal mio mockup, ho sbagliato io: serve più spesso (5%, più vivido e all'80% di opacità.
Proviamo con l'arancione-onboarding*). La `2.11` aveva preso le sue misure dal **mockup**, cioè da
un disegno: quello che il disegno non dice è quanto di un tratto sottile si perde su una
fotografia vera.
- ⚠️⚠️ **E CAMBIA L'UNITÀ, NON SOLO IL NUMERO: la nota che diceva 'in dp e non in frazione' è
  superata.** Valeva per il bordo di una **superficie dell'app**, che è sempre la stessa (il
  filetto sotto una copertina, il bordo d'accento dei pannelli); qui il segno vive su una
  piastrella le cui colonne le sceglie lui, e fra due e cinque colonne il lato quasi si triplica.
  Con un numero in punti lo stesso segno peserebbe il triplo da una parte e un terzo dall'altra.
- ⚠️ **Il tratto si disegna doppio dentro un ritaglio della sagoma**: un tratto è centrato sul
  contorno, quindi metà cadrebbe fuori dalla miniatura. È quello che faceva `border`, che però non
  può leggere una misura per ricavarne lo spessore.

⚠️⚠️ **MA DALLA `2.13` IL COLORE È L'ACCENTO DELL'APP, E L'ARANCIONE È DURATO UNA VERSIONE**
(riscontro del giro della `2.12`, voce `ind-cornice` approvata con una nota: *forse con questo
spessore sarebbe visibile anche nel colore d'accento. Proviamo*). La `2.12` era passata a
`HINT_MARK` perché a `2,7%` il verde acqua *non era abbastanza vivido*, e il 5% ha tolto proprio
quella causa: un tratto spesso ha l'area per farsi vedere anche in un colore di casa.
- ⚠️⚠️ **CADE LA NOTA CHE ESCLUDEVA `colorScheme.primary`** (*i due segni devono distinguersi a
  colpo d'occhio*), e cade perché guardava dalla parte sbagliata: nastro e cornice **non si vedono
  mai insieme**, sono le due risposte dello stesso interruttore. Lo stesso colore dice il vero,
  cioè che sono due forme di un segno solo.
- ⚠️ **Quindi l'arancione torna a dire una cosa sola**, l'evidenziatore dei mini onboarding, che
  è l'unico posto in cui la tavolozza dell'app si rompe apposta. La nota della `2.12` che lo dava
  per doppio è superata.
- ⚠️ **Il colore arriva dal chiamante**, perché `lastFrame` non è un composable e un colore del
  tema non lo può leggere da sé. È anche quello che rende la prova più forte: misura il **legame**
  invece di ricopiare una costante.
- ⚠️⚠️ **E LA PROVA HA CAMBIATO BERSAGLIO CON LUI**: fino alla `2.12` `CorniceTest` misurava che
  il tratto fosse arancione, cioè ricopiava un valore, ed è diventata rossa per una **decisione**
  invece che per un difetto. Adesso misura che il tratto prenda il colore ricevuto, fuso
  all'opacità dichiarata: è il caso generale di una prova che verifica un comportamento invece di
  un'implementazione, scritto in § '🧪 Quando si scrive una prova, e quando no'.

⚠️ **La voce vive in 'Etichette e pulsanti' perché lo ha chiesto lui**, e la famiglia lo regge per
il titolo della pagina che la contiene, 'Comandi e indicatori': quella parola ce l'ha già, mentre
la domanda della famiglia (*come si presentano i comandi che uso*) da sola non basterebbe.

⚠️ **Che cosa il banco misura e che cosa no** (`IndicatoreTest`): la migrazione nei tre casi, che è
la sola cosa qui dentro che possa rompersi **in silenzio**, perché nessuno la vede finché non
aggiorna l'app su un telefono già usato. **Non** vede il segno dentro la griglia: una miniatura
vuole delle immagini vere, e il MediaStore di Robolectric è vuoto.
- ⚠️⚠️ **MA IL TRATTO SÌ, DALLA `2.12`, E LA STRADA È QUELLA DEL GRADIENTE**: `CorniceTest` monta
  il modificatore che lo disegna su una scena minima e guarda i pixel, cioè misura il
  **meccanismo** invece della schermata. Vede che lo spessore raddoppia col lato, e che il tratto
  prende il colore che gli si passa all'opacità dichiarata. Controprovata in tutti e due i versi,
  rimettendo prima la misura fissa e poi una costante al posto del colore ricevuto.

## 🧪 Quando si scrive una prova, e quando no

⚠️⚠️ **UN DIFETTO CHE È ARRIVATO A LUI TORNA INDIETRO CON LA PROVA CHE LO AVREBBE FERMATO, NELLA
STESSA VERSIONE DELLA CORREZIONE** (istruzione dell'utente, 2026-09-05: *aggiungi tutti i
meccanismi e la documentazione che servono affinché le prossime versioni usino lo strumento
correttamente, con criterio e in modo proattivo*). Non è una buona abitudine: è la sola cosa che
trasforma un giro di collaudo speso in un presidio che non si spende più. Una voce non approvata
che si corregge senza prova è una voce che può tornare non approvata.
- **Il paletto che lo tiene stretto**: vale per i difetti che il banco **può** vedere, cioè
  quelli di struttura. Per gli altri la correzione va da sola, e va detto nella voce.
- ⚠️ **Nella stessa versione, non 'più avanti'**: una prova rimandata è una prova che non si
  scrive, perché il giro dopo porta altre cose e quel difetto non fa più male a nessuno.

⚠️⚠️ **E LA PRIMA COSA CHE UNA PROVA NUOVA DEVE FARE È MISURARE LA CORREZIONE CHE È GIÀ
USCITA**, quando quella correzione era **ragionata e non misurata**. Il precedente è la `1.72`:
aveva tolto una `SizeTransform` dal cambio di schermata per correggere il FAB che *arriva da in
basso a destra*, il ragionamento era pulito, ed era un **niente**. La `1.74` lo ha misurato: con
e senza quella riga, il centro del FAB è nello stesso punto a ogni fotogramma, cifra per
cifra, nella schermata vera. Nel frattempo la voce di collaudo aveva annunciato all'utente una
causa che non era la sua.
- **Come si riconosce una correzione da rimisurare**: è uscita senza che nessuno abbia visto il
  difetto sparire, e il suo posto nel documento di feedback dice 'adesso dovrebbe'. Sono quelle
  che il banco deve prendere per prime, perché sono già state pagate una volta.
- ⚠️ **E quando la misura smentisce, si dice**: la nota nel codice si riscrive con la causa
  vera o con la dichiarazione che la causa non si conosce, e la voce nuova lo dice all'utente.
  Una nota che tiene in piedi una causa falsa manda la sessione dopo a cercare dove ha già
  guardato qualcuno.
- ⚠️⚠️ **IL SECONDO PRECEDENTE È LA `2.07`, E DICE UNA COSA IN PIÙ: UNA CONDIZIONE NECESSARIA
  SEMBRA UNA CAUSA.** Là il filmato non se ne andava scorrendo, e la correzione ha portato la
  superficie del lettore da nativa a texture, cioè ha tolto **l'impedimento**: una `SurfaceView`
  non si lascia traslare. Il ragionamento era giusto e il difetto è rimasto, perché nessuno
  traslava quella superficie: lo sfoglio col dito viveva nel solo `ImageCanvas`, e sul filmato
  il baratto era **scritto nel codice** (*qui il dito non trascina la pagina*). La `2.08` ha
  portato là la stessa macchina.
  - **Come si riconosce prima di spendere un giro**: si guarda che cosa **fa muovere** la cosa
    che deve muoversi, e non solo che cosa glielo impedisce. Tolto l'impedimento, la domanda
    *adesso chi lo muove?* ha una risposta o non ce l'ha.

⚠️⚠️ **E UNA MODIFICA CHE TOCCA LA GERARCHIA DEI TOCCHI PORTA LA SUA PROVA ANCHE SENZA UN
DIFETTO ALLE SPALLE**, che è la metà proattiva della regola. Sono tre i casi, e si riconoscono
da soli: un nodo che **copre** lo schermo o una schermata intera; un modificatore che **misura**
e ci posa dentro qualcosa (la scatola gonfiata di `lowered` è l'esempio in casa); una superficie
che si **apre sopra** un'altra e deve decidere che cosa fa il tocco fuori. In tutti e tre il
codice può essere valido e non fare niente, e quello non lo vede nessun compilatore.

⚠️ **Quando NON si scrive una prova**: per quello che dipende dalla resa vera o
dall'apparecchio, perché il banco non lo vede e una prova che finge di vederlo è peggio del
niente; e per riscrivere in una prova quello che il codice già dice, che non verifica un
comportamento ma ricopia un'implementazione, e cade al primo ritocco senza che nulla sia rotto.

⚠️⚠️ **DALLA `1.85` IL BANCO SA GUARDARE I PIXEL, E CON LORO VEDE UNA CLASSE DI DIFETTI CHE PRIMA
NON VEDEVA**: quelli di **disegno**, cioè un elemento che c'è, è al posto giusto, è opaco, e
finisce sotto qualcos'altro. Serve `@GraphicsMode(NATIVE)` sulla classe, e poi
`captureToImage().toPixelMap()` dà i colori veri. Il caso che l'ha fatto nascere è il titolo in
testata che spariva sotto il gradiente dell'intestazione.
- ⚠️ **Non allarga il confine dichiarato**: quello che dipende dall'**apparecchio** resta fuori, e
  anche quello che dipende da come una cosa si **percepisce** (una dissolvenza troppo lenta, una
  sfocatura che stona). Il banco adesso vede *che cosa è coperto da che cosa*, che è un fatto.
- ⚠️ **Costa la grafica vera per tutta la classe**, e va bene: le altre prove non guardano i
  pixel e girare in `NATIVE` non le cambia.
- ⚠️⚠️ **E LA VUOLE ANCHE CHI MISURA UN TESTO, che è il secondo caso e non si vede arrivare**:
  con la grafica di serie un testo misura una frazione di quello che misura su un telefono,
  quindi una prova che dipende da quanto è **larga** una parola passa con qualunque codice.
  Misurato nella `1.86`, scrivendo la prova della pastiglia del titolo: nella scena stretta il
  comando restava scritto, e il difetto era della prova. Chi ne scrive una così le dia una classe
  sua, perché `@GraphicsMode` vale per tutta la classe.

⚠️⚠️ **E UNA PROVA CHE NON SI VEDE FALLIRE COL DIFETTO RIMESSO NON MISURA NIENTE: SI RIMETTE IL
DIFETTO E SI GUARDA.** Nella `1.85` la prima stesura della prova sui pixel guardava il titolo in
testata a schermata intera, ed è rimasta **verde** con il difetto rimesso a mano in due forme
diverse, perché là dove passa il titolo il gradiente è già quasi finito. Quella che lo rileva
guarda il **meccanismo** su una scena minima: un quadrato bianco pieno dentro un nodo che porta
il gradiente. Dietro, il bianco resta bianco; sopra, il centro si tinge.
- ⚠️ **La prima non si è buttata**: presidia la scena **come lui la vede**, che è un'altra cosa
  buona da presidiare. Quello che è cambiato è il suo KDoc, che adesso dichiara che cosa misura e
  che cosa no.
- ⚠️ **Il costo della controprova è un minuto**, ed è la sola cosa che distingue una prova da una
  riga verde: il criterio universale vive in `rules/Roccobot.md` § '🧪 Test e verifiche'.

⚠️⚠️ **UNA PROVA ROSSA NON SI AGGIRA MAI**: non si salta, non si spegne, non si mette in
quarantena, e non si rilascia con la scusa che 'quella riga non c'entra'. Se la prova è
sbagliata si corregge **la prova**, e la ragione si scrive accanto: una prova cambiata senza una
ragione scritta è una prova che qualcuno ha piegato per far passare un build.

⚠️ **Il banco NON sostituisce il giro con lui**, e prometterlo sarebbe la bugia peggiore: gli
toglie dalle mani la classe di difetti che una macchina sa vedere, e gli lascia tutto il resto,
che è la maggior parte. Un banco verde vuol dire che la struttura regge, non che la versione è
buona.

⚠️ **Si lancia PRIMA di aprire la PR**, e non si conta sul CI per scoprire un rosso: il difetto
si corregge dove lo si è scritto, e una corsa rossa su `main` è una corsa che qualcuno deve
guardare. Le due corse automatiche (§ '🧰 Gli strumenti che questo repo si porta dietro') sono
la rete.

## 🚀 Che cosa produce un rilascio

**Due cose, e vanno insieme**: il numero di **versione** e le voci nuove nel **collaudo**. La
regola completa (che cosa entra in ognuno, come si ripubblica un giro in cinque passi, la
struttura standard del documento di collaudo) vive in `rules/Roccobot.md`, § '🔁 Il giro del
collaudo: rilascio, documento, riscontro'.

- **Versione in SlimVer** (`x.xx`), come gli altri progetti. La fonte unica è
  `versionName` in `app/build.gradle.kts`, e il workflow di rilascio ne ricava il tag: il tag
  **conferma** quel numero invece di essere un secondo posto in cui scriverlo.
- ⚠️ **Il `versionCode` è un'altra cosa e cresce da sé**: Android rifiuta un aggiornamento
  che non lo faccia salire, non è legato al `versionName`, e **nessuno controlla che sia stato
  toccato**. La `0.11` è uscita portando `1`, quindi da lì in poi ogni versione pubblicata ha
  il suo numero.
- **Come si pubblica**: il workflow `release.yml` ha due vie, e la seconda esiste apposta per
  una sessione. Un `workflow_dispatch` con l'ingresso `publish` acceso taglia il tag dal
  `versionName`, costruisce l'APK firmato, crea la release, e **copia APK e paginetta sotto
  `roccobot.github.io/AIV/`**. Senza `publish` costruisce e si ferma, che è la **corsa a vuoto**.
  - ⚠️ **Quella corsa NON si chiama 'il banco di prova', e fino alla `1.73` qui era scritto
    così**: dalla `1.73` quel nome è di un'altra cosa, cioè le prove che aprono l'app finta
    (§ '🧰 Gli strumenti che questo repo si porta dietro'). Due cose con lo stesso nome, in un
    repository che ne parla in ogni giro, sono due cose che prima o poi qualcuno scambia.
- ⚠️⚠️ **E PRIMA DI TUTTO QUESTO C'È UN CANCELLO, dal 2026-09-05**: `release.yml` lancia il banco
  di prova e il controllo delle traduzioni **prima** della chiave di firma e del build, quindi
  una prova rossa o una lingua incompleta fermano il rilascio invece di produrre un APK da
  ritirare. Il perché per esteso, e i due posti in cui il banco gira, vivono in § '🧰 Gli
  strumenti che questo repo si porta dietro'.
  - ⚠️ **I controlli a costo zero restano davanti al cancello**: un tag che non coincide col
    `versionName`, o un `publish` da un branch che non è quello principale, falliscono in un
    secondo, e non ha senso spendere due minuti di prove per scoprirlo dopo.
- **Verifica di pubblicazione avvenuta**: un `curl` su <https://roccobot.github.io/AIV/> e il
  nome del file servito (`AIV-1.20.apk` e simili). Il merge su `master` del sito non basta:
  serve che il deploy Pages vada a buon fine.
  - ⚠️⚠️ **MA QUEL NOME NON È NELL'HTML, E UNA SONDA CHE LO CERCA LÀ RISPONDE SEMPRE VUOTO**: la
    paginetta chiede l'elenco alle release di GitHub mentre si carica e compone il link con
    `assets[i].name`, quindi nel testo servito quel nome non compare mai. Misurato il 2026-09-19:
    un controllo che aspettava `AIV-2.65.apk` dentro la pagina ha atteso venti minuti per niente,
    mentre il file era già servito. È il falso negativo del `--diff` senza pipe
    (`roccobot.github.io/CLAUDE.md`): il comando risponde 'niente' e si legge come 'non è ancora
    live', mentre è 'ho guardato nel posto sbagliato'.
  - **Quindi si chiede il FILE**, che è quello che 'il file servito' vuol dire: un `curl` su
    `https://roccobot.github.io/AIV/AIV-<versione>.apk` deve rispondere **200** col peso
    dell'asset della release. Sulla `2.70`: 200 e 7.722.920 byte.
  - ⚠️⚠️ **MA IL CODICE DA SOLO NON DISTINGUE NIENTE, E IL CRITERIO È IL TIPO DICHIARATO**: la
    versione prima risponde **200** anche lei, perché Pages serve la propria pagina di ripiego a
    un percorso che non esiste. Quindi si guarda `Content-Type`, che per l'APK vale
    `application/vnd.android.package-archive` e per il ripiego `text/html`. Misurato il
    2026-09-19: la `2.70` è l'APK, la `2.69` e la `2.68` sono la stessa paginetta da 9.379 byte.
    ⚠️ **Fino alla `2.65` qui era scritto che la versione prima dà 404**, ed era vero allora: un
    controllo che aspetti quel numero adesso dà per non pubblicata una versione che è live.
  - ⚠️⚠️ **E IL PESO NON DISTINGUE DUE VERSIONI, PERCHÉ LA PRIMA LIBRERIA NATIVA È ALLINEATA A 16
    KB** (misurato il 2026-09-25 sulla `2.84`, che pesa quanto la `2.81`, la `2.82` e la `2.83`:
    7.778.976 byte). `classes.dex` è salvato senza compressione, e la voce subito dopo, la prima
    libreria di `lib/arm64-v8a/`, porta un riempimento che allinea i suoi dati a 16.384 byte:
    quel riempimento assorbe ogni variazione del dex fino a quella misura, e le voci che seguono
    non cambiano fra una versione e l'altra.
    - **A distinguere due build è l'impronta**: lo SHA-256 del file servito deve coincidere col
      `digest` dell'asset della release, e non con quello della precedente. Il peso resta una
      prova che il file è arrivato intero, non che è quello nuovo.

## 🔐 La firma, e dove NON vive

La chiave di firma e le sue parole d'ordine vivono **solo** fra i secret GitHub di questo
repository (`AIV_KEYSTORE_FILE`, `AIV_KEYSTORE_PASSWORD`, `AIV_KEY_ALIAS`, `AIV_KEY_PASSWORD`),
e il job le scrive su disco per la durata di una sola esecuzione.

- ⚠️ **Senza quelle variabili il build di release NON fallisce: l'APK risulta NON
  firmato.** È voluto,
  perché così chi non ha la chiave può comunque compilare e controllare il minificatore. Il
  rovescio è che un APK non firmato si riconosce solo guardando, e per questo il workflow ha
  un passo che **chiede all'APK se è firmato**.

## 🧰 Gli strumenti che questo repo si porta dietro

- **`tools/feedback-check.py`**, dal 2026-09-19: apre il documento di feedback in Chromium e
  verifica che parta e disegni quello che i dati portano. ⚠️ **Si lancia PRIMA di pubblicarlo**,
  e la ragione vive in § '🔗 Il documento vivo del progetto': quel giorno la pagina si è spenta
  su un campo assente, e lui ha trovato un documento muto.
- **`tools/i18n-check.py`**, da lanciare dalla radice: confronta tutte le lingue con
  l'inglese (chiavi mancanti, segnaposto, categorie di plurale, caratteri vietati). ⚠️ Deve
  dire **28 lingue, 0 problemi**: un numero più basso vuol dire che una cartella non è stata
  vista. Le **varianti regionali** (`values-b+es+419` e `values-pt-rPT`) portano solo le
  differenze, e il verificatore lo sa.
- **`tools/icon-check.py`**, da lanciare dalla radice: legge ogni `res/drawable/*.xml` e
  misura quello che di un'icona si può misurare. Il criterio che applica vive in
  § '🖌️ Come entra un disegno'; qui basta sapere quali sono le due specie di controlli,
  perché non hanno lo stesso peso.
  - **La grammatica e i vincoli girano sempre** (sola libreria standard) e **bloccano**: un
    elemento o un attributo che Android non conosce, un tracciato che un parser stretto
    rifiuta, una tela non dichiarata.
  - ⚠️⚠️ **IL TOKENIZZATORE DEI TRACCIATI È SCRITTO A MANO DI PROPOSITO**, e non è
    ostinazione: il parser di Android è indulgente, quindi appoggiarsi a una libreria
    misurerebbe l'indulgenza di quella libreria invece della grammatica. Un tracciato che
    passa da lì lo legge qualunque parser conforme, che è la definizione operativa di 'a prova
    di futuro'.
  - **Le misure di resa vogliono Chromium** e **avvisano** invece di bloccare: dov'è
    l'inchiostro, il margine dalla tela, la sagoma di Material più vicina, se il disegno
    dipende dal verso di avvolgimento, e quanto costerebbe unire i tracciati. ⚠️ Se Chromium
    non c'è **lo dichiara** invece di tacere, che è la differenza fra un controllo saltato e un
    controllo passato.
- **`tools/icon-round.py`**, dalla `2.50`: dice quali spigoli **esterni** di un'icona sono
  ancora vivi, con l'angolo e il raggio che spetta a ognuno. È il presidio della regola
  dell'arrotondamento a 0,4, che vive in § '🖌️ Come entra un disegno'. ⚠️ **Avvisa e non
  blocca**, come le misure di resa di `icon-check.py`: un disegno che arriva da lui può portare
  uno spigolo vivo che vuole così, e quella è una decisione di chi disegna.
  - ⚠️⚠️ **DALLA `2.56` RISPONDE ZERO, E IL LOGO NON ENTRA NEL CONTO**: le punte delle esistenti
    sono state raccordate su sua istruzione, e `ic_tian` è **dichiarato escluso** per la regola
    universale che vive in `rules/Roccobot.md` § '🧹 Bonifica e ottimizzazione degli asset'.
    ⚠️ **Da qui in poi un numero diverso da zero riguarda un disegno appena entrato**, che è la
    cosa che questo strumento serve a vedere: finché le esistenti ne portavano 99, quel conto
    copriva l'unico caso che conta.
  - ⚠️⚠️ **DUE FALSI POSITIVI L'HANNO COSTRETTO A GUARDARE LE TANGENTI E NON LE CORDE**, e la
    differenza è un fattore quattro: un raccordo già fatto è un arco, e la sua corda svolta di
    metà arco, cioè esattamente quanto basta a farlo sembrare vivo; e le curve **smooth** (`s`,
    `t`) sono tangenti per costruzione, quindi il loro giunto non è mai uno spigolo. Prima
    contava 1.059 punte su 41 disegni, poi 259, e poi 142.
  - ⚠️ **Un giro che torna a un centesimo di unità dal proprio punto di partenza è chiuso**: i
    tracciati sono scritti a due decimali, e senza quella tolleranza la chiusura diventa una
    rettina che svolta, cioè uno spigolo che non esiste.
  - ⚠️⚠️ **E IL CRITERIO SI È STRETTO UN'ALTRA VOLTA IL 2026-09-18, SU SUA NOTA: SOLO GLI ANGOLI
    CONVESSI ESTERNI** (`d-spigoli-icone`, giro della `2.50`). Quindi i numeri sono cinque e
    ognuno misura un criterio diverso: **1.059** con le corde, **259** con le tangenti, **142**
    alla `2.50` (lo stesso criterio ne contava 138 il 2026-09-18, perché i disegni sono
    cambiati), **87** con gli angoli di un buco esclusi, e **99** da quando si guarda anche il
    vertice della `M`. Il perché vive in § '🖌️ Come entra un disegno'.
  - ⚠️⚠️ **IL VERTICE DELLA `M` NON SI GUARDAVA MAI, E LA LACUNA È DURATA DALLA `2.50` ALLA
    `2.54`**: un giro che torna esattamente sul proprio punto di partenza ha là due vertici nello
    stesso punto, la chiusura è lunga zero, e la guardia che salta i lati troppo corti (quella
    che esiste per non smussare il residuo di un arrotondamento) lo prendeva sempre. Il lato vero
    che lo precede è quello che arriva al **penultimo** punto, e adesso la guardia lo legge di
    là. ⚠️ **L'intenzione c'era già**, e si vede in `versi()`, che per quel vertice aggiusta le
    due tangenti apposta: mancava solo di non saltarlo.
  - ⚠️⚠️ **A TROVARLO È STATO UN DISEGNO NUOVO, e non una rilettura del codice**: il glifo di
    'Salva stile' usciva dal generatore con due punte ancora vive (una per sottotracciato), e il
    verificatore diceva **zero**. Su `BookmarkAdd` crudo il conto passa da 10 a 12.
  - ⚠️⚠️ **A DIRE SE UN SOTTOTRACCIATO È UN CONTORNO O UN BUCO È LA PROFONDITÀ DI CONTENIMENTO,
    NON IL VERSO DEL GIRO**: pari vuol dire contorno, dispari vuol dire buco, e così un'isola
    dentro un buco torna a portare punte (in casa succede, in `ic_folder_new`). Il verso da solo
    direbbe il vero solo dove chi ha disegnato lo ha usato per quello, e `android:fillType` può
    essere `evenOdd`, dove non significa niente.
  - ⚠️ **Il punto con cui si misura il contenimento non è un vertice**, ed è la trappola che
    quella scelta evita: due giri che si toccano hanno i vertici l'uno sul bordo dell'altro, e là
    un conto dei raggi risponde a caso. Si prende il mezzo del lato più lungo, scostato di un
    millesimo verso l'interno.
- ⚠️⚠️ **IL BANCO DI PROVA, dalla `1.73`: `./gradlew :app:testDebugUnitTest`**, e apre l'app
  **finta** su una macchina senza telefono e senza emulatore, la tocca e verifica che risponda.
  Le prove vivono in `app/src/test/`, e le librerie (Robolectric più `ui-test-junit4`) sono di
  sola prova: nell'APK non entra niente.
  - **Perché esiste**: il blocco totale della `1.70`, dove il codice era valido,
    `compileDebugKotlin` è passato senza una parola, e l'app si avviava senza rispondere a
    niente. Nessun controllo di questo repository poteva vederlo, perché tutti guardano il
    **testo** del programma e quello era giusto.
  - ⚠️⚠️ **CHE COSA NON PRENDE, e va detto invece di lasciarlo credere**: quello che dipende
    dalla resa vera (sfocature, ombre, animazioni come si percepiscono) e quello che dipende
    dall'apparecchio (permessi, provider di file, memoria grafica). Prende i difetti di
    **struttura**, non quelli di **aspetto**. Un banco verde non vuol dire che l'app è a posto.
  - ⚠️ **Su quale Android gira si dichiara** in `app/src/test/resources/robolectric.properties`,
    invece di lasciare il valore di serie, che è il `targetSdk`: quel numero cambia quando cambia
    la politica di Google Play, e con lui cambierebbe in silenzio la piattaforma delle prove.
    - ⚠️⚠️ **E QUEL NUMERO DECIDE LA VERSIONE DI JAVA CHE SERVE**: Robolectric ne dichiara una
      minima per piattaforma, e la 36 vuole **Java 21** (letto nel bytecode di
      `DefaultSdkProvider`: le API 34 e 35 portano 17, la 36 porta 21). Su Java 17 non parte
      nemmeno una prova, e l'errore si legge come una catena di strumenti rotta invece che come
      un controllo di versione. Il CI monta 21 per questo, e chi tocca uno dei due numeri
      guarda l'altro.
  - ⚠️ **Le prove montano `AivTheme` e non un albero finto**, ed è la ragione per cui valgono: il
    velo dell'app e il cancello dei menu vivono là dentro, quindi un nodo che rubasse i tocchi
    entra in scena da sé, senza che una prova debba ricordarsi di chiamarlo.
  - ⚠️⚠️ **E DALLA `1.74` MONTA LE SCHERMATE VERE, non solo il tema**: `EntrataTest` apre
    `FolderScreen` con gli argomenti minimi dentro la transizione vera, e da lì misura. A
    tenerlo fuori era una cosa sola, `Environment.isExternalStorageManager()`, che Robolectric
    non copre e che muore con un `ArrayIndexOutOfBoundsException` dentro il metodo di sistema,
    cioè con un errore che si legge come un difetto dell'app. La copre `OmbraArchivio`, uno
    shadow di venti righe.
    - ⚠️⚠️ **NON È UN DETTAGLIO DI COMODO: È LA DIFFERENZA FRA MISURARE E NON MISURARE.** La
      prima stesura di quella prova montava una **miniatura** scritta accanto, e passava anche
      rimettendo il difetto che doveva prendere. Il criterio universale vive in `Roccobot.md`
      § '🧪 Test e verifiche'.
  - ⚠️⚠️ **DAL 2026-09-05 GIRA DA SÉ, E IN DUE POSTI**: è un passo di `check.yml` a ogni push su
    `main` e a ogni PR, ed è il **cancello** di `release.yml`, dove una prova rossa ferma il
    rilascio prima ancora che si tocchino la chiave di firma e il build. I due non sono un
    doppione: il cancello rifiuta di pubblicare una versione rotta, il controllo dice che è
    rotta **prima** che qualcuno provi a pubblicarla.
    - **Lanciarlo a mano resta il modo di lavorare**, e il modo giusto: si lancia **prima** di
      aprire la PR, perché il difetto lo si corregge dove lo si è scritto. Le due corse in CI
      sono la rete, non il controllo.
  - ⚠️ **La piattaforma finta pesa 213 MB e si scarica al primo giro**: in CI la tiene una cache
    con la chiave sui due file che decidono quale piattaforma sia (il catalogo delle versioni e
    `robolectric.properties`), perché `setup-gradle` non copre `~/.m2`. Su una macchina di
    sessione il contenitore è effimero, quindi il primo giro paga il download (misurato: 1
    minuto e 49 contro i 14-26 secondi delle corse successive).
- ⚠️ **`refcheck.py` NON vive qui** ma in `roccobot.github.io/.memo/scripts/`: in una sessione
  che non monta quel repo i controlli sui caratteri e sui rimandi non girano, e prima di un
  commit va detto invece di darli per fatti.
  - ⚠️⚠️ **MA DAL 2026-09-02 QUESTO FILE È COPERTO, e prima no**: `AIV/CLAUDE.md` è un file di
    **regole**, non un documento di repo terzo, quindi i suoi titoli entrano nell'indice dei
    rimandi e le altre regole possono citarne una sezione. Fino a quel giorno era fuori
    copertura, e un rimando **corretto** a una sua sezione veniva segnalato come 'sezione
    inesistente': è il sintomo rovesciato già visto due volte nel repo del sito, e là c'è
    scritto per esteso (`roccobot.github.io/CLAUDE.md`, la voce sui controlli pre-commit).
  - ⚠️ **Il rovescio vale ancora, e adesso è dichiarato**: in una sessione che monta il sito
    ma non AIV, i rimandi che nominano questo repo restano **non verificabili** e non
    bloccano il commit, e il verificatore stampa quanti sono.

## 🌿 Branch

Il branch principale è **`main`**. Le sessioni vincolate a un branch `claude/*` aprono la PR e
la mergiano subito (squash), come da regola universale sul go-live.
