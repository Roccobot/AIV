# CLAUDE.md: regole del progetto AIV

> **Cos'è questo file.** Le regole **specifiche** di `Roccobot/AIV`, l'app Android
> 'Astonishing Image Viewer'. Tutto quello che vale per ogni progetto vive nelle regole
> universali, `rules/Roccobot.md` di `Roccobot/tools`, e qui non si duplica: qui sta solo
> ciò che di questo repository non si ricava altrove.

⚠️ **Nasce il 2026-09-01, dopo 111 versioni**, e la ragione è precisa: fino a quel giorno
l'**indirizzo del documento vivo** del progetto stava solo nel brief di consegna, che è
**stato volatile** e non un archivio. Un artefatto vive fuori dal repository per definizione,
quindi se il suo indirizzo non è scritto in un file committato, alla sessione dopo quel
documento è perso.

## 🔗 Il documento vivo del progetto

| documento | a che cosa serve | indirizzo |
|---|---|---|
| **Documento di feedback** | le voci da provare della versione appena uscita, con i tre esiti e i commenti dell'utente. **Chiede.** | <https://claude.ai/code/artifact/a026a5d9-3bd0-4732-a8ea-69033d04fb48> |
| **Piano d'azione AIV** | le versioni in cantiere in sequenza, una tappa per giro di collaudo. **Non chiede**: è la vista d'insieme. | <https://claude.ai/code/artifact/ed40ee4b-ce9b-4588-b1c9-5e5b7e773cd3> |

⚠️⚠️ **IL PIANO D'AZIONE È UNO STRUMENTO RICORRENTE dal 2026-09-04, e non un artefatto di
passaggio** (decisione dell'utente: *lo ufficializziamo come strumento di lavoro ricorrente,
insieme al brief*). La regola universale, con la divisione dei compiti fra lui e il brief, sta
in `rules/Roccobot.md` § '🗺️ Il piano e il brief: due strumenti, due domande'; qui
resta solo il suo indirizzo, che è la cosa che questo file esiste per non far perdere.
- ⚠️ **Si chiamava con una metafora ferroviaria fino a quel giorno**, e il nome è cambiato su
  istruzione dell'utente (*lascia stare le metafore*). Chi ne trova il nome vecchio in un
  messaggio o in un commit sappia che è lo stesso documento, allo stesso indirizzo.
- ⚠️⚠️ **E A MORIRE NON È SOLO IL NOME DEL DOCUMENTO: È TUTTA LA METAFORA.** Il piano è fatto
  di **tappe**, e una tappa è il gruppo di lavori che escono in una versione: non si dice
  'vagone', non si dice 'treno', né in chat né in un commit né in un artefatto. ⚠️ **Sta
  scritto perché la prima formulazione non bastava**: diceva che era cambiato il *nome del
  documento*, quindi la parola per il gruppo di lavori sembrava salva, ed è rientrata in una
  frase il 2026-09-04 (*> vagone / piano d'azione\**). Un divieto che nomina un solo caso si
  legge come il permesso per tutti gli altri.

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

⚠️⚠️ **UNA COMPILAZIONE A METÀ NON SI PRENDE IN CARICO, E IL DOCUMENTO NON SI RIPUBBLICA
MENTRE LUI LO COMPILA** (istruzione del 2026-09-03). Il giro si consegna con **'Invia'** o con
una riga in chat, e si prende **intero**: spezzarlo in più versioni è una decisione mia, che
viene dopo la consegna. Nel frattempo le voci nuove e le correzioni si tengono in una
**bozza**, perché una pubblicazione ricarica ogni vista aperta e arriva sotto le mani di chi
sta scrivendo. ⚠️ La pagina che si ripubblica **da sé sul suo input** è un'altra cosa e resta:
è quella che gli garantisce di non perdere il riscontro. La regola per esteso, con il perché
di ogni pezzo, vive in `rules/Roccobot.md` § '⏸️ Il giro si prende INTERO, e solo quando lo
dice lui'.
- ⚠️ **Sta qui e non solo là perché questo file sopravvive alla compattazione**, come la
  regola di registro nel `CLAUDE.md` di root: un file di regole entra in scena quando lo si
  legge, e da un riassunto sparisce.

⚠️⚠️ **ERANO DUE FINO AL 2026-09-03, e il secondo era il Changelog AIV** (decisione
dell'utente: *cancella l'artefatto changelog, ho visto che non mi serve e non l'ho mai
usato*). Chi ne trova ancora l'indirizzo in un messaggio vecchio, o la pagina in galleria,
sappia che non si aggiorna più: il perché per esteso, e la domanda che l'aveva fatto nascere e
che resta valida, stanno in `rules/Roccobot.md`, § '🧾 Il changelog, provato e ritirato'.

## 🎨 Il design system, che vive fuori dal repository

⚠️⚠️ **LE FONTI VISIVE DI AIV STANNO IN CLAUDE DESIGN, nel progetto `Roccobot Design`**, e non
in questo repository. Come si aggancia e che cosa contiene sta in `rules/Roccobot.md`,
§ '🎨 Grafica' → '🎨 Claude Design, dove vive il design system'; qui restano i tre pezzi che
riguardano AIV.

⚠️⚠️ **MA NON SI VA A PESCARE LÀ DA SÉ: È L'UTENTE CHE DICE, DI VOLTA IN VOLTA, CHE COSA
PRENDERE** (istruzione del 2026-09-02: *ti dico io di volta in volta cosa cercare: con Design
mi piace sperimentare, non vorrei ripetere lo stesso errore dell'ultima volta*). Là dentro lui
**prova** delle cose, quindi quello che ci si trova non è per forza approvato: è un banco di
lavoro, non un capitolato.
- ⚠️ **L'errore che la regola evita è già costato una versione**: nella `1.33` sono entrati
  otto glifi nuovi perché stavano nel brief dei disegni, e nella `1.35` sono usciti tutti
  (*volevo cambiare solo l'icona principale e la pagina di download*). Il perché sta in testa
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
  sta in testa a `Glyphs.kt`.

⚠️⚠️ **MA LE FRASI DELLA PAGINETTA DI DOWNLOAD NON SONO LÀ**, ed è la ragione per cui quel
lavoro è fermo: vivono in un **documento** di Claude Design (`.dc.html`), che sta fuori dai
progetti di design system e quindi non si raggiunge con lo strumento. Serve che l'utente lo
mandi in chat o lo semini nello spazio di lavoro.

⚠️⚠️ **E NEL REPOSITORY NON SE NE TIENE UNA COPIA: la cartella `dev/` è stata svuotata il
2026-09-03** (istruzione dell'utente: *cancella anche tutto quello che c'è dentro AIV/dev, non
credo che serva più*). Là dentro stavano i file che lui aveva mandato per la `1.32` e le
versioni dopo: gli otto glifi, i tre disegni dell'icona col vector XML del foreground, le due
versioni di 'Copia immagine', quella di 'Inverti selezione', e il brief di implementazione.
- **Perché non è una perdita**: quei disegni sono **entrati** nell'app (l'icona) o sono
  **usciti su sua istruzione** (i glifi, nella `1.35`), quindi la cartella teneva sorgenti già
  lavorate. Quelli che servono ancora vivono in Claude Design, e la storia git ha tutto.
- ⚠️ **Chi la ricreasse rifarebbe due fonti di verità**: un disegno che l'utente sta ancora
  provando sta nel design system, non qui, e uno approvato sta in `res/`. Un file mandato in
  chat si lavora e non si archivia.

## 🖌️ Come entra un disegno

⚠️⚠️ **UN DISEGNO VIVE IN `res/drawable/ic_<nome>.xml`, UNO PER GLIFO, e `Glyphs.kt` è il
catalogo**: là stanno i nomi con cui il codice chiama un'icona e che cosa vuol dire ognuna,
qui sta la geometria. Fino alla `1.45` i tracciati erano costanti di stringa in Kotlin, e lo
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
  repository non se ne tiene una copia: il perché sta in § '🎨 Il design system, che vive
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

## 🗣️ Come si chiamano le cose

⚠️⚠️ **QUEL PULSANTE SI CHIAMA 'FAB', E 'TASTINO' NON SI USA PARLANDO CON LUI** (riscontro del
giro della `1.60`: *e il pulsante si chiama FAB, lascia perdere 'tastino'*). Vale nelle voci del
documento di feedback, in chat, negli artefatti e nei messaggi di commit.
- ⚠️ **È lo stesso criterio della voce sul velo, applicato a un nome che LUI usa**: la sigla
  compare nella spiegazione della voce 'Posizione dei tasti flottanti', quindi è la parola che
  si trova nel telefono; 'tastino' era un vezzeggiativo mio, che non compariva da nessuna parte.
- ⚠️ **Nel codice la parola vecchia c'è ancora in molti commenti**, e non si corregge con una
  passata a parte: entra nella bonifica dei commenti che il piano tiene come tappa senza numero.
  Quello che conta è che da qui in avanti non se ne scrivano di nuovi.

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
    Il perché per esteso, e le due vie con cui si applica, stanno in testa a `Veil.kt`.
    - ⚠️⚠️ **E DALLA 1.54 UNA TERZA: il BORDO D'ACCENTO, al posto dell'ombra**
      (richiesta dell'utente, 2026-09-04: *via le ombre e vai con il bordino da 2px del colore
      di accento*, e *potrebbe essere l'elemento distintivo che cercavo*). Il bordo **non
      dipende dall'interruttore** della sfocatura, perché non è una funzione che si accende ma
      il modo in cui l'app è fatta. Come si disegna, e che cosa c'entra col 'quadrato sfocato'
      che lui vedeva intorno ai menu, stanno in testa a `Edge.kt`.
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
        di una scheda in fondo stanno sui bordi dello schermo, quindi una linea che corre fuori
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
    - ⚠️ **Spento vuol dire non toccare niente**, che è un'altra cosa dal dipingere un velo
      trasparente: i dialoghi tornano al velo che Android dà loro (`0,6`), i menu a
      non averne. L'unica eccezione è la scheda in fondo, che se lo chiede da sé perché la sua
      finestra non ne ha uno di serie: `SHEET_DIM` in `Sheet.kt`, col perché misurato.
      - ⚠️ **Fino alla `1.43` qui c'era il numero**, 'i tredici dialoghi', e la `1.44` lo ha
        reso falso togliendone uno (la conferma di buttare via la selezione) senza che
        nessuno toccasse questa riga. Il criterio che lo vieta è universale e sta in
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
  errore**: è la stessa forma di trappola del velo mancante, e per questo la regola sta scritta
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
e da finestra 'secondaria' se si tocca SOTTO*). La causa sta nel modo in cui si ottiene il 15%:
`LowerNode` non sposta il pannello, **gonfia la scatola** dichiarata e ce lo posa in fondo,
quindi sopra resta una fascia trasparente alta fino al 30% della finestra che **appartiene al
dialogo**. Toccarla è toccare dentro, e `dismissOnClickOutside` non scatta; sotto si è fuori, e
scatta. Il perché per esteso vive su `airTop`, in `Centred.kt`.

⚠️⚠️ **I MENU SONO UN CASO GEMELLO MA DIVERSO, E IL LORO DIFETTO È CHE IL TOCCO ARRIVA ANCHE
SOTTO** (trovato dal censimento della UI del 2026-09-05, e misurato sul bytecode di Compose:
`createFlags` di `AndroidPopup_androidKt` parte da `FLAG_WATCH_OUTSIDE_TOUCH` e non mette mai
`FLAG_NOT_TOUCH_MODAL`). Da Android 12 la finestra di un popup **non è modale al tocco**: un
dito fuori dal pannello arriva a tutte e due le finestre, quindi il menu si chiude **e** l'app
apre la riga, la miniatura o la cartella che stava sotto il dito.
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
  **non** 'lo lascio passare a chi sta sotto': quelli sotto non lo ricevono affatto.
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
  con un altro strumento, e questa è la ragione per cui la regola sta qui invece che in un
  commento accanto a un solo chiamante.
- ⚠️ **Non c'è modo di provarlo senza un dispositivo**, e questo difetto è arrivato in
  produzione per quello: `compileDebugKotlin` non vede niente, e il progetto non ha un banco di
  prova strumentale. Chi tocca un nodo che copre lo schermo lo sappia, e guardi **prima** se il
  nodo esiste anche quando non serve.

## ⚙️ Dove va un'impostazione, e chi la deve trovare

⚠️⚠️ **UNA VOCE STA CON QUELLE CHE RISPONDONO ALLA SUA STESSA DOMANDA, e la domanda è quella
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
Confonderle è l'errore che manda dietro un tocco il tema dell'app: 'Aspetto' porta il tema, la
coppia dello sfondo e il velo, cioè tre famiglie, e nessuna arriva alla soglia.
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

⚠️⚠️ **E L'ELENCO DI CHE COSA È DELICATO È CHIUSO, DUE CASI E NON PIÙ**: sbagliare la voce può
costare un file, o toglie la rete che lo protegge; oppure la voce cambia il **metro** con cui
un'immagine viene misurata, quindi rende ogni immagine diversa da come ci si aspetta senza
rompere niente. Un elenco aperto si allarga da sé: qualunque voce, con abbastanza argomenti, si
guadagna il tocco in più, e la pagina piatta si svuota una riga per volta.
- ⚠️ **Il rovescio resta vero, ed è la clausola che vale più di tutte**: una riga sola che non è
  né un elenco né delicata, in una sotto-pagina costerebbe un tocco senza guadagnare niente.
- ⚠️ **La profondità è UNO**: una sotto-pagina non ne apre un'altra, perché la navigazione è un
  valore solo senza pila e Indietro riporta alla radice. Una famiglia che ne conterrebbe
  un'altra tiene nella pagina piatta la riga che apre la seconda.

⚠️ **UNA VOCE SOLA NON PRENDE UN TITOLO**, e va nella sezione della famiglia la cui domanda le
sta più vicina, mai sopra un titolo, perché sopra un titolo si legge come la prima riga di
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

⚠️ **QUELLO CHE NON DECIDE**: il gruppo in cui la voce stava prima, la comodità del codice, la
lunghezza della pagina piatta, e il fatto che una sezione risulti sbilanciata. Se una famiglia
viene di quattro voci e un'altra di una, sta bene: le domande non si fanno tutte con la stessa
frequenza. ⚠️ **E i conti non si scrivono**, qui come nei commenti del pannello: quante sono le
sezioni, le famiglie e le voci si contano nel codice.

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
  ritirare. Il perché per esteso, e i due posti in cui il banco gira, stanno in § '🧰 Gli
  strumenti che questo repo si porta dietro'.
  - ⚠️ **I controlli a costo zero restano davanti al cancello**: un tag che non coincide col
    `versionName`, o un `publish` da un branch che non è quello principale, falliscono in un
    secondo, e non ha senso spendere due minuti di prove per scoprirlo dopo.
- **Verifica di pubblicazione avvenuta**: un `curl` su <https://roccobot.github.io/AIV/> e il
  nome del file servito (`AIV-1.20.apk` e simili). Il merge su `master` del sito non basta:
  serve che il deploy Pages vada a buon fine.

## 🔐 La firma, e dove NON vive

La chiave di firma e le sue parole d'ordine stanno **solo** fra i secret GitHub di questo
repository (`AIV_KEYSTORE_FILE`, `AIV_KEYSTORE_PASSWORD`, `AIV_KEY_ALIAS`, `AIV_KEY_PASSWORD`),
e il job le scrive su disco per la durata di una sola esecuzione.

- ⚠️ **Senza quelle variabili il build di release NON fallisce: l'APK risulta NON
  firmato.** È voluto,
  perché così chi non ha la chiave può comunque compilare e controllare il minificatore. Il
  rovescio è che un APK non firmato si riconosce solo guardando, e per questo il workflow ha
  un passo che **chiede all'APK se è firmato**.

## 🧰 Gli strumenti che questo repo si porta dietro

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
  - **Le misure di resa vogliono Chromium** e **avvisano** invece di bloccare: dove sta
    l'inchiostro, il margine dalla tela, la sagoma di Material più vicina, se il disegno
    dipende dal verso di avvolgimento, e quanto costerebbe unire i tracciati. ⚠️ Se Chromium
    non c'è **lo dichiara** invece di tacere, che è la differenza fra un controllo saltato e un
    controllo passato.
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
