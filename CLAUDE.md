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
| **Piano d'azione AIV** | le versioni in cantiere in sequenza, una tappa per giro di collaudo. **Non chiede**: è la vista d'insieme. | <https://claude.ai/code/artifact/ed40ee4b-ce9b-4588-b1c9-5e5b7e773cd3> |

⚠️⚠️ **IL PIANO D'AZIONE È UNO STRUMENTO RICORRENTE dal 2026-09-04, e non un artefatto di
passaggio** (decisione dell'utente: *lo ufficializziamo come strumento di lavoro ricorrente,
insieme al brief*). La regola universale, con la divisione dei compiti fra lui e il brief, vive
in `rules/Roccobot.md` § '🗺️ Il piano e il brief: due strumenti, due domande'; qui
resta solo il suo indirizzo, che è la cosa che questo file esiste per non far perdere.
- ⚠️ **Si chiamava con una metafora ferroviaria fino a quel giorno**, e il nome è cambiato su
  istruzione dell'utente (*lascia stare le metafore*). Chi ne trova il nome vecchio in un
  messaggio o in un commit sappia che è lo stesso documento, allo stesso indirizzo.
- ⚠️⚠️ **E A MORIRE NON È SOLO IL NOME DEL DOCUMENTO: È TUTTA LA METAFORA.** Il piano è fatto
  di **tappe**, e una tappa è il gruppo di lavori che escono in una versione: non si dice
  'vagone', non si dice 'treno', né in chat né in un commit né in un artefatto. ⚠️ **È
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
- ⚠️ **È qui e non solo là perché questo file sopravvive alla compattazione**, come la
  regola di registro nel `CLAUDE.md` di root: un file di regole entra in scena quando lo si
  legge, e da un riassunto sparisce.
- ⚠️⚠️ **QUINDI IL RISVEGLIO AUTOMATICO SU QUESTI DUE DOCUMENTI NON SERVE, E LA SUA ASSENZA NON
  SI SCRIVE COME UNA MANCANZA** (sua precisazione, 2026-09-08: *in ogni caso è giusto che sia
  così: abbiamo stabilito che sarò sempre io a darti il via libera*). La sottoscrizione è
  rifiutata da mesi con `mint_failed`, e il brief la registrava a ogni giro come un difetto da
  rimediare: anche funzionando sveglierebbe la sessione **a ogni salvataggio**, cioè in mezzo a
  una compilazione. Quello che si scrive è il fatto, cioè che il via libera arriva da lui.

⚠️⚠️ **ERANO DUE FINO AL 2026-09-03, e il secondo era il Changelog AIV** (decisione
dell'utente: *cancella l'artefatto changelog, ho visto che non mi serve e non l'ho mai
usato*). Chi ne trova ancora l'indirizzo in un messaggio vecchio, o la pagina in galleria,
sappia che non si aggiorna più: il perché per esteso, e la domanda che l'aveva fatto nascere e
che resta valida, vivono in `rules/Roccobot.md`, § '🧾 Il changelog, provato e ritirato'.

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
        - ⚠️⚠️ **E I CASI DELLA MIGRAZIONE SONO TRE E NON DUE**: chi aveva **acceso** la
          sfocatura tiene la sfocatura, chi l'aveva spenta tiene il niente, chi non ha mai
          toccato la voce riceve l'ombra. Con un `else` solo, una scelta esplicita sarebbe
          cambiata da un aggiornamento.
        - ⚠️ **Il valore di fabbrica vive in DUE posti**, il campo di `Settings` e la lettura del
          flusso: cambiarne uno solo dà un'app accesa al primo avvio e spenta dopo il primo
          salvataggio, che è un difetto che non dà nessun errore.
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
