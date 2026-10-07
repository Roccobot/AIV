# Files.md: le funzioni dei file di AIV

> **Cos'è questo file.** Le specifiche delle funzioni che riguardano i file, le miniature e le
> impostazioni salvate: che cosa fa ognuna, le decisioni dell'utente su di lei e le sue trappole.
> Non si carica da solo: si legge per intero prima di toccare una di queste funzioni. Le regole del
> repo vivono in `Rules.md`, e un rimando a una sua sezione indica il nome del file.

## 📤 AIV come selettore: quando un'altra app chiede un'immagine

- ⚠️⚠️ **AIV compare fra le app a cui un'altra app chiede un'immagine** (sua richiesta), e risponde
  ad `ACTION_GET_CONTENT` e ad `ACTION_PICK`. Il filtro da solo non bastava, per due fatti di
  sistema letti sulle fonti:
  1. le app di terze parti compaiono nel navigatore file solo se chi chiede usa `GET_CONTENT` (in
     AOSP, `PickActivity.setupLayout`), e chi allega usa quasi sempre `ACTION_OPEN_DOCUMENT`, che
     elenca i soli archivi, cioè chi espone un `DocumentsProvider`;
  2. per le immagini `GET_CONTENT` lo prende il selettore foto di sistema, con priorità 105, e
     Android azzera la priorità dichiarata da un'app che non è di sistema.
  - ⚠️ **Quindi `ACTION_PICK` è la sola via servibile**, ed è quella di chi chiede un'immagine
    dalla galleria. Il filtro dichiara `vnd.android.cursor.dir/*` e i due tipi diretti, perché i
    chiamanti si dividono fra i due modi.
  - ⚠️⚠️ **Un `ACTION_PICK` indica la sorgente in cui scegliere, non un'immagine da aprire**: lo
    distingue una guardia in `handleIntent`, o l'app si aprirebbe sul visualizzatore.
  - ⚠️ **Comparire anche fra gli archivi vuole un `DocumentsProvider`**, che è un lavoro a sé.
- ⚠️⚠️ **Non è una schermata nuova**: l'app si apre com'è, e il tocco su una miniatura consegna il
  file e chiude. Vale per le tre griglie, i recenti e la vista 'Cartelle di sistema', perché una
  modalità che funziona in una schermata su tre sembra rotta. Indietro annulla (`RESULT_CANCELED`).
- ⚠️⚠️ **L'indirizzo si prepara** (`ImageActions.readableOutside`, la strada della condivisione): un
  `file://` che esce dal processo fa cadere l'app (`FileUriExposedException`), quindi diventa una
  copia servita dal FileProvider, e un `content://` passa com'è. Il permesso viaggia con
  `FLAG_GRANT_READ_URI_PERMISSION`.
- ⚠️ **`singleTop` qui non disturba**: chi chiede un risultato non passa da
  `FLAG_ACTIVITY_NEW_TASK`, quindi l'istanza nasce nel task di chi chiama.
- ⚠️ **Un'immagine per volta**: `EXTRA_ALLOW_MULTIPLE` non è gestito, e un file solo è una risposta
  legittima.
- ⚠️ **Nessuna prova del banco**: non ha un selettore di sistema né un'app che riceve. Si prova sul
  telefono.
- ⚠️⚠️ **Un intento nudo che arriva a giro iniziato non azzera niente**: toccare l'icona del launcher
  con l'app aperta consegna un `onNewIntent` col `MAIN` del launcher, che chiudeva l'editor e
  perdeva il lavoro (sua segnalazione). `handleIntent` riceve `fresh` (vero in `onCreate`, falso in
  `onNewIntent`), e un intento non fresco senza indirizzo e senza una richiesta di scelta esce senza
  toccare niente. La guardia è il primato della lettura, non l'elenco delle schermate.
  - ⚠️ **Una richiesta di scelta passa lo stesso**, perché chiede qualcosa anche senza un indirizzo.
  - ⚠️ **Se il sistema uccide il processo l'editor riparte**, perché il lavoro vive nella
    composizione, e si dichiara. La prova è `RitornoTest`.

## 💾 Il salvataggio va sempre in Download, e il nome si chiede solo se lo chiedi

- ⚠️⚠️ **'Scarica' scrive in Download senza selettore** (sua istruzione: l'unica cartella che
  funziona senza autorizzazioni, anche in vista di Play).
  - ⚠️⚠️ **Su Android 9 resta il selettore**: `MediaStore.Downloads` nasce con l'API 29, e sotto
    quella la cartella vuole `WRITE_EXTERNAL_STORAGE`, che l'app non chiede. Là il gesto apre il
    selettore col nome già scritto. `ImageActions.downloadsWritable` sceglie la strada prima di
    provare, quindi un `false` di `saveToDownloads` vuol dire guasto e nient'altro.
  - ⚠️ **`IS_PENDING`**: la riga si scrive in sospeso e si chiude alla fine, e se la copia fallisce
    si cancella; senza, un download interrotto lascia in galleria un'immagine tagliata.
  - ⚠️ **I nomi doppi li numera il MediaStore**, l'unico che può farlo senza una finestra fra
    l'elenco della cartella e la scrittura.
- ⚠️⚠️ **L'opzione 'Scegli il percorso di download'** (spenta di fabbrica, sua indicazione) aggiunge
  il comando 'Destinazione', che apre il selettore già esistente. Il nome finale lo decide il
  selettore: un fornitore può ritoccare il suffisso per far quadrare nome e tipo.
  - ⚠️⚠️ **Si torna a Download dalla finestra, e spegnere l'opzione scorda la cartella scelta**
    (`DownloadFolder.forget`, che rilascia anche il permesso persistente): il selettore di sistema
    non sa riportare a Download, quindi senza quella via una cartella scelta una volta era
    definitiva.
- ⚠️⚠️ **Il nome si chiede in due casi, e sono suoi**: con 'Consenti rinomina al salvataggio' accesa
  (spenta di fabbrica, perché salvare si fa di fretta) o col tocco lungo su 'Scarica', per quella
  volta.
  - **La finestra chiede il solo nome**, col suffisso accanto al campo; per cambiare formato c'è
    'Esporta/Converti'.
  - **I due gesti di 'Data'**: il tocco breve infila `YYYYMMDD` al cursore, il lungo rifà il nome
    con la sola data. Il conto è uno, in `SaveName.kt`, e lo usa anche 'Rinomina' (sua richiesta:
    'esattamente come in Scarica').
  - **Sotto il campo tre comandi solo testo** (`Quiet`), gli stessi di 'Rinomina': 'Seleziona
    tutto', 'Svuota', 'Data'.
  - ⚠️ **'Destinazione' ed 'Estensione' vivono sulla riga del titolo**, 'Estensione' ultima a destra
    (suo ordine), e la fila si allinea al bordo. Uno solo è una pastiglia scritta finché il titolo
    ci sta accanto (sua risposta `misura`: la misura si fa sul titolo, che è quello che cede),
    due sono due icone. `TitleRow`, `TitlePill` e `TitleIcon` sono condivisi con 'Rinomina'. I due
    glifi sono suoi (`Glyphs.FolderDownload`, `Glyphs.Extension`).
  - ⚠️⚠️ **Col tocco normale ognuno compare se la sua opzione è accesa; col tocco lungo ci sono tutti
    e due** (sua risposta a `d-dest-lettura`: due comandi sulla stessa riga non hanno due regole).
- ⚠️⚠️ **L'app si ricorda che cosa ha già scaricato** (sua richiesta): la firma è suffisso più byte,
  e la notifica 'Hai già scaricato questa immagine' offre 'Scarica di nuovo' per cinque secondi. Il
  registro è `DownloadLog`, trenta giorni e duecento voci (il conto in testa al file). Senza i byte
  non si firma: tutte le immagini senza misura avrebbero la stessa firma.
- ⚠️ **In una rinomina in blocco il tocco lungo su 'Data' mette la data, uno spazio e i cancelletti
  giusti per il set** (sua risposta a `d-data-blocco`), con la stessa funzione del template
  proposto all'apertura (`hashesFor`).
- ⚠️ **'Estensione' passa da `extensionGate`**, che si porta dietro le sue finestre: la griglia di
  sicurezza (spenta di fabbrica), l'avviso della prima volta e il pannellino.
  - ⚠️⚠️ **Con un suffisso cambiato il tipo dichiarato al MediaStore segue il nome**: con tipo ed
    estensione in disaccordo il MediaStore aggiunge l'estensione del tipo, e `foto.png` dichiarato
    `image/jpeg` nascerebbe `foto.png.jpg`. Che il file menta è dichiarato, perché cambiare
    l'estensione non converte. **Con il suffisso invariato resta il tipo misurato al
    caricamento**, perché un nome può mentire già in partenza.
- ⚠️ **È una modale vera**, con le due righe di `Rules.md` § '👆 Che cosa fa il tocco FUORI da una
  finestra'. L'interruttore vive in 'Modifica e backup', la sezione della domanda 'che cosa scrive
  l'app su disco, e con che nome'.
- ⚠️⚠️ **Trappola presa dal banco**: un `rememberSaveable` di un `TextFieldValue` vuole
  `TextFieldValue.Saver`, o l'app va in errore nell'istante in cui la finestra si apre. Il codice
  compilava, e il valore di serie di quell'argomento lancia invece di avvisare.

## 🔤 L'anteprima della rinomina, coppia per coppia

- ⚠️⚠️ **È il suo mockup, misurato sui suoi pixel** (1,625 pixel per dp): 12dp fra le due
  pastiglie di un abbinamento, 32dp fra due abbinamenti col separatore a metà, la punta della
  freccia a 2dp dalla pastiglia di sotto.
  - ⚠️ **La freccia è l'icona di Material da 20dp**, posata dalla punta e col gambo tagliato dal
    bordo di sopra: a zero pixel di scarto vince Material (`Rules.md` § '🖌️ Come entra un
    disegno').
  - ⚠️⚠️ **Il separatore è `FadedRule`, in `Theme.kt`, uno per l'app** (sua nota su `d-filo-uno`:
    come quello del Ritaglio), pieno a metà e sfumato fino ai capi; l'aria la mette il chiamante.
- ⚠️⚠️ **Il conto è la sua specifica**: con uno, due o tre file si vedono tutti; con quattro o più i
  primi due, lo stacco e l'ultimo, che ha il numero più alto, cioè il solo modo di vedere se le
  cifre del template bastano.
  - ⚠️ **Lo stacco c'è solo se nasconde almeno un nome, e prende il posto del separatore**, quindi
    il passo fra le coppie non cambia. È `MoreHoriz` alla misura della freccia, e come lei non ha
    descrizione: a dire che ci sono altri nomi sono i numeri di quelli dopo.
- **`RinominaAnteprimaTest`** misura il conto da uno a nove file, le distanze e, a pixel, la
  freccia; ogni caso ha la sua controprova. Non vede come l'anteprima si legge sul telefono.

## 🧮 Svuotare il cestino dice lo spazio liberato

- ⚠️⚠️ **Lo svuotamento dice anche quanto spazio ha liberato** (sua richiesta). Il peso di ogni file
  si legge prima della cancellazione, perché dopo il file non c'è più, e si somma solo se la
  cancellazione riesce: il conto viaggia nell'esito (`FileTree.Outcome.freed`), e zero vuol dire
  'non misurato'.
- ⚠️⚠️ **È una frase a sé (`delete_freed`) e non un pezzo in coda**: in metà delle lingue il verbo,
  l'accordo o la particella cambiano con quello che segue. L'eliminazione di una selezione dice la
  frase di sempre, e la scelta la fa `outcomeText`.
  - ⚠️ **Il participio concorda con lo spazio**, dove concorderebbe col numero (in francese
    *d'espace libéré*, perché `formatBytes` scrive sempre dei decimali; nelle lingue slave la forma
    è impersonale).
- **Il numero è scritto da `formatBytes`**, come ogni peso dell'app (`Rules.md` § '🗣️ Come si
  chiamano le cose').
- **La prova è `CestinoSpazioTest`**, su file veri nella cartella del cestino, in una classe sua per
  la trappola di `OmbraArchivio` (`Rules.md` § '🧰 Gli strumenti che questo repo si porta dietro').

## 🚫 La cartella d'origine non è una destinazione

- ⚠️⚠️ **Dalla `4.46` la cartella da cui vengono i file è spenta nella scelta della destinazione**
  (sua nota C sul giro della `4.45`: *piuttosto che inserire un messaggio di errore sarebbe meglio
  impedirlo all'origine, ovvero disattivare (sia in senso effettivo che graficamente) la cartella di
  origine*). Nell'elenco delle cartelle, a griglia o a lista, è al 38% di opacità e non risponde al
  tocco (`off` su `Covers` e `Rows`, senza valore di serie).
- ⚠️ **Nell'albero si entra lo stesso, e si spegne solo il tasto che la sceglierebbe**: le sue
  sottocartelle sono destinazioni valide, e chiudere la porta vorrebbe dire non arrivarci. È una
  lettura della sua nota, dichiarata nella voce di collaudo.
- ⚠️ **Le cartelle d'origine si leggono prima che la finestra si apra** (`FileTree.sourcesOf`,
  una domanda al MediaStore per file): partendo da un insieme vuoto, l'origine sarebbe stata
  toccabile per il tempo della domanda. Un file di cui non si legge il percorso non spegne niente.
- La prova è `OrigineTest`, sulle due viste.

## ↩️ Disfare una copia o uno spostamento

- ⚠️⚠️ **Copia e spostamento offrono 'Annulla' per tre secondi, come l'eliminazione** (sua
  richiesta), con la stessa notifica: `Undo.Offer` ha due forme, un ritorno dal cestino e un elenco
  di passi da rifare al contrario.
- ⚠️⚠️ **Si disfa dai passi fatti e non dalla richiesta**: `FileTree` restituisce un `Undoable` per
  ogni file che ha davvero scritto. Ricostruire l'inverso dalla richiesta sbaglierebbe al primo nome
  rinumerato.
  - ⚠️ **Un file già sparito non conta come fallito**: il risultato voluto è che non ci sia.
  - ⚠️ **Il ritorno non sovrascrive niente**, e prende un nome libero se a casa è arrivato un file
    con lo stesso nome: il comando che ripara non fa danno.
  - 'Duplica' passa dalla stessa strada, perché è una copia nella cartella di partenza.
- ⚠️ **`DisfareTest` è nata con il lavoro e non dopo un difetto**: qui un difetto non si vede e non
  si disfa a sua volta.

## 🖼️ Le miniature che mentono dopo una riscrittura

- ⚠️⚠️ **La cache di `Thumbs` è indicizzata sull'indirizzo e non sul contenuto**, quindi un file
  riscritto o ripristinato può mostrare la miniatura di un altro (sua segnalazione: una copia di
  sicurezza uscita dal cestino con la miniatura dell'immagine modificata). Il rimedio vive in
  `FileTree.scan`, dove il chiamante dichiara che un percorso è cambiato, così un chiamante nuovo
  lo prende per costruzione; lo scan butta anche la miniatura del `file://`, che è un secondo
  indirizzo per lo stesso file.
- ⚠️⚠️ **Le vie chiuse sono tre**:
  - **la miniatura del sistema**: il provider la tiene per riga e non per contenuto, quindi un
    indirizzo dichiarato riscritto salta la strada di sistema una volta e passa dalla decodifica
    normale, che apre il file vero;
  - **il tetto della mappa di casa**: `Thumbs.forget` cerca tutte le chiavi con quell'indirizzo, e
    non più in una mappa più piccola della cache di Coil;
  - **i percorsi del cestino**, che il MediaStore non vede e che si riusano: il cestino dichiara i
    propri, quando un file arriva e quando se ne va.
- ⚠️⚠️ **La causa sul telefono non è accertata, e si scrive così**: dipende da come il provider
  rinumera, che senza il telefono non si misura. Quello che si è fatto è chiudere tutte le strade
  per cui l'app può servire una miniatura vecchia.
- **La prova è `MiniatureTest`**, quattro casi, ognuno controprovato. Non vede la miniatura del
  provider, e il callback del MediaScanner sul banco non arriva.

## PNG riscritti e PSD esclusi

Dalla 3.14:

- `Thumbs.forget` elimina anche la miniatura su disco dell'indirizzo, prima di controllare
  se la cache in memoria esiste. Il file può essere riscritto mantenendo la stessa data:
  senza questa invalidazione `AvifCache` restituisce i pixel precedenti. La cancellazione
  riguarda il solo indirizzo e il lato `Thumbs.PX`; le miniature degli altri file rimangono.
  `FileTree.scan` continua a invalidare sia l'indirizzo del provider sia quello del file.
- `SavedPngThumbnailTest` verifica i pixel dopo una riscrittura a data invariata e conserva
  la cache di un altro PNG. Entrambe le prove sono state viste fallire con il codice precedente.
  Il rendering GPU dell'editor completo non è disponibile sul banco JVM: resta il collaudo
  del salvataggio sul telefono, distinto dalla prova della cache.
- I PSD sono ignorati anche quando il MediaStore li classifica come immagini: la selezione
  comune esclude estensione, percorso e tipi Photoshop; griglie, ricerca, conteggi, peso e
  miniature usano quella selezione. I campi sconosciuti non fanno sparire i media validi.
  Cartelle di sistema, cestino e cronologia escludono l'estensione senza distinzione fra
  maiuscole e minuscole. Le cartelle chiamate `.psd` e i nomi `esempio.psd.png` rimangono validi.
- L'esclusione dal cestino e dalla cronologia riguarda la visualizzazione: file e registri
  di backup rimangono intatti. `IgnoredPsdTest` e `IgnoredPsdBinTest` coprono questi contratti;
  le cinque prove sono state viste fallire prima della rispettiva correzione.

## 🗃️ Le miniature memorizzate: svuotarle e generarle

- ⚠️⚠️ **La pagina 'Gestisci le miniature memorizzate' ha due comandi, 'Svuota' e 'Genera'**, ed è
  la sua specifica alla lettera. I paragrafi vivono dentro le loro conferme (testi suoi, con
  'immagini' al posto di 'foto'), coi tasti 'Sì' e 'Annulla'. Il perché di ogni pezzo vive su
  `ThumbsCard`.
- ⚠️ **'Genera' c'è su ogni versione di Android, dalla `2.99`**: fino alla `2.98` mancava sotto
  Android 10, dove nessuna miniatura restava su disco. Adesso là tutto passa dalla decodifica, e la
  decodifica si tiene.
- ⚠️⚠️ **'Svuota' svuota quello che è dell'app**: la memoria di Coil e le miniature in `AvifCache`,
  cioè in `cacheDir`. Le miniature del sistema le tiene il provider, e nessuna chiamata dell'app le
  toglie. La copertina scelta a mano non si tocca, perché vive in `filesDir`.
- ⚠️⚠️ **Ogni miniatura resta su disco, dalla `2.99`** (sua risposta `disco` a `d-mini-disco`). I
  posti sono due: la cartella `.thumbnails` del volume, dove il sistema salva quelle che genera con
  `loadThumbnail` (letto nel sorgente AOSP del `MediaProvider`), e `AvifCache`, che tiene quelle
  degli AVIF, il fotogramma che AIV sceglie per un video (§ '🎬 La miniatura di un video, quando il
  sistema la dà nera o non la dà') e ogni miniatura che passa dalla decodifica normale di Coil.
  - ⚠️ **Quell'ultima classe fino alla `2.98` si buttava**: le immagini con la trasparenza, i BMP,
    gli SVG, e quelle la cui miniatura di sistema è troppo piccola, che `tooSmall` rifiuta. Il caso
    più nascosto nasce dall'EXIF: `createImageThumbnail` prende la miniatura incorporata prima di
    decodificare il file, senza nessuna soglia di misura.
  - ⚠️⚠️ **A scrivere è un decodificatore e non un intercettore** (`KeepingDecoderFactory`, in
    `Thumbs.kt`): chiede al registro di Coil il decodificatore che verrebbe dopo di lui e ne salva
    il risultato. Da un intercettore le tre provenienze dicono tutte `DataSource.DISK`; qui arriva
    solo la decodifica, quindi una miniatura che il sistema tiene già non occupa spazio due volte.
  - ⚠️ **A leggere è un fetcher solo** (`DiskThumbnailFactory`), dopo quello di sistema e prima
    dell'AVIF: la domanda al MediaStore per sapere dov'è il file sarebbe spesa per niente sulle
    miniature che dà il sistema.
  - ⚠️ **Tutti e due si aprono solo per una richiesta di `Thumbs.request`**, che specifica l'indirizzo
    negli extra (`Thumbs.KEPT`): un decodificatore riceve i byte e non sa da che file vengono, e una
    richiesta che non chiede una miniatura non scrive nella loro cartella.
  - ⚠️⚠️ **Il tetto della cartella è di 30.000 file**, cioè una collezione intera, e la potatura gira
    una volta ogni cento scritture. ⚠️ **Quanto pesa è una stima e non una misura**: circa 300 MB
    ogni 10.000 miniature, alla misura della griglia (`Thumbs.PX`). La cartella vive in `cacheDir`,
    e Android la può svuotare quando lo spazio finisce.
  - ⚠️ **I due caricatori decodificano allo stesso modo**; quello della generazione ha la memoria di
    Coil spenta, perché quello che si genera non si guarda (`Thumbs.warmer`).
- ⚠️ **La riga del riepilogo misura la sola cache su disco dell'app**, su un thread di I/O perché i
  file possono essere decine di migliaia; il primo valore è l'ultima misura del processo
  (`AvifCache.known`), o la riga direbbe 'nessuna miniatura' per un istante.
- ⚠️⚠️ **Le cartelle sono quelle dell'elenco iniziale** (`Folder.everything`), senza le nascoste e
  senza il prestito di 'Mostra nascoste', dalla più recente: una generazione fermata a metà ha già
  fatto quello che si apre per primo.
- ⚠️⚠️ **Il lavoro vive col processo e non con la pagina** (`Warmup`), e con l'app in secondo piano
  continua: la risposta `aperta` a `d-cestino-chiusa` riguarda lo svuotamento automatico del
  cestino, un lavoro che parte da solo, e questo lo fa partire lui.
  - **La pagina è sua alla lettera**: il titolo, il numero X/Y, la barra, 'Annulla', e alla fine la
    notifica 'Miniature generate correttamente.' Mentre si contano le immagini la barra non ha un
    valore, e la riga del numero resta vuota senza sparire.
  - ⚠️ **Indietro vale 'Annulla', e 'Annulla' toglie la pagina subito**: ogni generazione ha il
    proprio numero, quindi un aggiornamento tardo non la fa ricomparire. Quello che è fatto resta, e
    la frase finale non si dice.
  - ⚠️ **Un file illeggibile conta come fatto**, o non si arriverebbe mai in fondo; **un indirizzo
    appena riscritto si salta**, perché passando di lì il fetcher di sistema consumerebbe il segno
    della miniatura da rifare. Le corsie sono due, perché la miniatura di un AVIF legge il file
    intero in memoria. ⚠️ **Quanto dura una generazione non è misurato.**
  - ⚠️ **Senza il permesso l'elenco è vuoto**, e quel tasto non lo raggiunge nessuno che non l'abbia
    già concesso.
- **Le prove** (`GeneraMiniatureTest`, `GeneraMiniatureCorsaTest` e un caso di
  `ImpostazioniTest`): che la generazione salti un indirizzo riscritto, che ogni file passi una
  volta e un errore conti come fatto, che un annullamento non ne faccia partire altre, le due
  conferme, la pagina, Indietro, la ricerca, e dalla `2.99` i casi del disco. Non vedono una
  collezione vera, perché il MediaStore del banco è vuoto.
  - ⚠️ **La seconda classe esiste per l'ombra**: `Warmup` chiede l'elenco a `Folder.everything`,
    che sul banco vuole `OmbraArchivio`.
  - ⚠️ **Una controprova non vale, e si dichiara**: la pagina che compare nell'istante del tocco,
    perché l'esito dipende da quale filo arriva prima.
  - ⚠️ **Una finestra di Compose si chiude con Indietro passando dal suo dispatcher**, che il banco
    raggiunge con `ShadowDialog.getLatestDialog()`.

## 🎬 La miniatura di un video, quando il sistema la dà nera o non la dà

- ⚠️⚠️ **AIV sceglie da sé il fotogramma quando la miniatura di sistema è nera o manca** (sua
  richiesta).
- ⚠️⚠️ **Il fotogramma del sistema dipende dalla versione di Android** (letto nel sorgente AOSP di
  `ThumbnailUtils.createVideoThumbnail`): fino ad Android 16 di lancio la copertina incorporata, o
  il fotogramma chiave più vicino alla metà; dal primo aggiornamento trimestrale di Android 16 il
  più pesante fra i primi venti fotogrammi chiave di un MP4. Un fotogramma nero pesa poco, quindi a
  darla nera serve un tratto nero lungo in apertura.
- ⚠️⚠️ **Si rimedia al risultato e non alla regola** (`ClipFrames`), perché la regola cambia col
  telefono. **Nera vuol dire** che al più l'1% dei punti di una griglia 32x32 ha il canale più
  acceso sopra 32 su 255: si contano i punti e non si fa la media (un titolo bianco su nero ha una
  media bassissima), e conta il canale più acceso e non la luminanza (un blu pieno varrebbe poco).
  - **Si cerca a metà, a un quarto, a tre quarti e a un decimo**; vince il primo non nero, e se sono
    neri tutti il più chiaro. Con `OPTION_CLOSEST_SYNC`, come il sistema.
- ⚠️ **È un componente della catena** (`ClipFrameFetcher`, dopo quello di sistema, che rifiuta la
  miniatura nera e passa la mano), e vale anche sotto Android 10, dove il fetcher di sistema non
  c'è.
- ⚠️⚠️ **`AvifCache` sceglie il formato come `ImageEdit` e `FolderCover`**: `WEBP_LOSSY` nasce con
  Android 11, e usato senza condizione l'oggetto sotto quella versione non si inizializzava.
  - ⚠️⚠️ **Lo stesso difetto viveva nell'elenco dei formati di 'Converti/Esporta'**
    (`Convert.Target`), e su Android 9 e 10 chiudeva l'app aprendo quella finestra: dalla `2.99`
    (sua segnalazione) il formato lo dà `Convert.Target.format`, che guarda la versione prima di
    nominare le due WebP, e sotto Android 11 usa `WEBP` con la qualità (a 100 senza perdita).
    `ConvertiTest` gira sulle piattaforme vere di Android 9, 10 e 11.
  - ⚠️ **Su Android 9 la WebP senza perdita non si offre** (`offered`): là `WEBP` a qualità 100
    comprime ancora con perdita, e un file che dice di non perdere niente perderebbe.
- ⚠️ **Il fotogramma scelto si tiene in `AvifCache`**, con la data del file nella chiave; 'Svuota'
  e 'Genera' lo coprono. Si paga una volta per video, e solo per quelli con la miniatura di sistema
  nera o assente.
- **`MiniaturaVideoTest`**, otto casi, ognuno controprovato: il riconoscimento del nero, l'ordine
  dei momenti, la scelta del fotogramma e la catena intera. Non vede quale fotogramma scelga il
  telefono vero.
- ⚠️ **Il video che ricominciava dopo mezzo secondo è sparito col riavvio del telefono**, e nel
  codice non c'è una strada che riporti il lettore a zero. **Le cause escluse**: il lettore non
  viene ricreato; nessun `seekTo` tranne la barra sotto il dito; media3-ui-compose non cerca da sé;
  ExoPlayer disegna il primo fotogramma anche a video fermo; l'osservatore del MediaStore segna solo
  la griglia. **Se ricapita, le domande**: se il tempo torna a 0:00, se ricomincia una volta o di
  continuo, che cosa si vede prima di Play, se succede con 'Riproduzione diretta dei video' accesa,
  e la versione di Android.

## 💼 Esporta e importa, e il file che solo AIV sa leggere

**Com'è fatto.** La pagina 'Esporta e importa' è una sotto-pagina della radice delle impostazioni;
il formato vive in `Backup.kt`, la pagina in `BackupSettings.kt`. La specifica è sua: un file che
contiene tutte le impostazioni che si possono salvare, ogni parte su richiesta con la sua casella,
cifrato e leggibile solo da AIV, e protetto da password se lo si chiede.

- ⚠️⚠️ **Il file è il 'file di impostazioni', e l'estensione è `.aivsettings`** (sue parole). Il
  nome proposto è `AIV_<AAAAMMGG>.aivsettings` (sua istruzione, dalla `2.99`); un file col suffisso
  vecchio si importa lo stesso, perché a dire che cosa è ci pensa la sua intestazione.
  - ⚠️ **Il tipo dichiarato è quello generico**, e deve esserlo: con un tipo vero il fornitore della
    cartella rimetterebbe il suffisso di quel tipo. Vale anche per gli stili (`docs/Editor.md`
    § '🎞️ I preset, venti di casa e quelli che si salvano').
- ⚠️⚠️ **Le parti sono macro-aree, ed è la sua riga**: 'Aspetto e navigazione', 'Comandi e
  indicatori', 'Impostazioni dell'editor', 'Stili dell'editor', 'Colore delle cartelle', 'Copertine
  delle cartelle', 'Cartelle incluse/escluse', 'Avvisi e micro-tutorial' e 'Cestino'. Le prime tre sono le
  sezioni della schermata delle impostazioni, le altre quello che l'app ricorda fuori da lì.
  - **'Impostazioni dell'editor' comprende anche il file originale della filigrana**: PNG o
    SVG, copiato senza conversione e ripristinato insieme a posizione, dimensione, distanza e
    opacità. L'SVG si disegna alla misura necessaria, quindi non c'è una seconda copia PNG da
    includere. La richiesta del 2026-09-29 è verificata dai due giri PNG e SVG in `BackupTest`.
  - ⚠️ **La terza non ripete il titolo della sua sezione** (sua nota su `d-backup-aree`): dentro una
    pagina di backup 'Modifica e backup' faceva pensare che la casella contenesse il backup stesso.
  - ⚠️ **Il token di un'area è il formato, e non cambia mai**: lo leggono anche i file già salvati,
    e un'area nuova prende un token nuovo.
  - ⚠️ **'Stili dell'editor' non c'è sotto Android 13**, dove l'editor completo non c'è.
  - ⚠️ **Il cestino è l'ultima casella, dice quanto pesa, ed è la sola spenta di fabbrica** (sua
    risposta `senza-cestino` a `d-backup-fabbrica`): può valere un gigabyte. La pagina non ricorda
    le caselle.
- ⚠️⚠️ **Le caselle scorrono, e 'Importa' ed 'Esporta' sono una riga fissa in fondo, dalla `2.99`**
  (sua nota su `backup-caselle`), con 'Importa' a sinistra. L'interlinea stretta vale per le sole
  caselle di questa pagina (`CheckRow` col parametro `tight`). ⚠️ **Il 'form factor più diffuso'
  in cui i tasti si vedono senza scorrere non è misurato**, e la voce di collaudo lo chiede.
- ⚠️⚠️ **È uno ZIP dentro un contenitore cifrato, e non uno ZIP con la password** (scelta dichiarata):
  la cifratura classica dello ZIP si rompe in pochi minuti, e quella AES di WinZip in
  `java.util.zip` non c'è. Dentro, i testi sono JSON e i file accessori (copertine, logo, file del
  cestino) sono copiati come sono.
  - ⚠️ **AES-GCM a segmenti da 64 kB**, perché su Android un GCM non restituisce un byte finché il
    tag non è verificato. **L'ultimo segmento si dichiara nel nonce**, o un file troncato fra due
    segmenti sarebbe fatto di segmenti tutti validi; **l'intestazione (35 byte in chiaro) entra in
    ogni segmento come dato autenticato**, quindi un suo byte cambiato rompe la lettura.
- ⚠️⚠️ **Senza password è un sigillo e non una protezione, e la pagina lo dice**: la chiave nasce da
  una costante dell'APK, quindi un altro programma non lo apre, ma chiunque abbia AIV lo importa.
  - ⚠️ **Con la password la chiave la fa PBKDF2 a 600.000 iterazioni, scritto a mano**: i byte della
    password sono NFC e poi UTF-8, così una lettera accentata da due tastiere diverse dà la stessa
    chiave, e il banco confronta il conto coi vettori pubblicati. Le iterazioni vivono
    nell'intestazione, con un tetto in lettura contro un file fatto apposta. ⚠️ **Quanto costano sul
    telefono non è misurato.**
  - ⚠️ **La password non va mai su disco**: vive nel processo per il tempo del selettore.
- ⚠️⚠️ **Drive arriva dal selettore di sistema** (`CreateDocument`, `OpenDocument`), senza permessi
  di rete. L'importazione accetta ogni tipo di file.
  - ⚠️⚠️ **Un file vuoto non si dà per esportato** (sua richiesta): la domanda è doppia
    (`Backups.landed`, prima il peso che il fornitore dichiara, poi il primo byte), perché il peso
    di Drive può arrivare in ritardo. La notifica ha il suo testo, e vale anche per una
    `SecurityException` del fornitore.
- ⚠️⚠️ **Le preferenze sono un elenco scritto a mano, `PREF_KEYS`, e lo tiene onesto il caso 2 di
  `BackupTest`**: ogni chiave dell'archivio deve essere là col tipo giusto, o in `PREF_OUTSIDE`. La
  regola per chi aggiunge una preferenza vive in `Rules.md` § '⚙️ Dove va un'impostazione, e chi la
  deve trovare'.
  - ⚠️ **Le chiavi dei promemoria di quel telefono restano fuori**: il permesso già
    chiesto, la cartella di download col suo permesso persistente, l'ultimo indirizzo degli appunti
    già aperto, che cosa c'è già nella cartella Download, e l'inizializzazione delle autorizzazioni predefinite.
  - ⚠️ **Le due chiavi vecchie ci sono di proposito** (`veil` e `mark-air`): nessuno le scrive più,
    ma l'app le legge ancora come ripiego.
- **'Cartelle incluse/escluse' conserva il token storico `hidden`** e contiene entrambe le liste,
  le esclusioni della sola cartella e la modalità corrente. Le liste si fondono, la modalità
  del file sostituisce quella del telefono. Un file precedente alle incluse ripristina la modalità
  escluse solo quando si importa l'area che contiene le cartelle. La chiave storica
  `hidden-relative` conserva le esclusioni ricorsive; i campi nuovi sono aggiunte al formato.
- ⚠️⚠️ **Importando, le preferenze sostituiscono, e il cestino si aggiunge**, con la cronologia e
  senza doppioni: sostituirlo vorrebbe dire cancellare per sempre dei file.
  - ⚠️⚠️ **Copertine, tinte, cartelle nascoste e stili si fondono, e sul conflitto vince il file**
    (sua risposta `fonde` a `d-backup-importa`): una copertina esce solo se il file ne contiene una per
    la stessa cartella (`FolderCovers.adopt`); tinte e nascoste si fondono voce per voce
    (`PREF_MERGED`), e lo stile con cui il colore si vede resta un valore solo; uno stile salvato
    col nome già usato prende i valori del file, e degli stili di casa si sommano i soli cambiamenti
    dalla fabbrica (`Presets.merge`). Su un telefono nuovo la fusione dà esattamente il file.
  - ⚠️ **L'importazione della pagina degli stili sostituisce** (sua risposta `sostituisce` a
    `d-stili-importa`): là si importa una raccolta, e chi la sceglie vuole quella.
  - ⚠️ **I due testi della pagina dicono che cosa si fonde** (sua risposta `si` a `d-backup-testi`):
    l'introduzione e la conferma dell'importazione nominano stili, colori, copertine, cartelle
    nascoste e cestino.
- ⚠️⚠️ **Prima si legge tutto e poi si scrive**: le voci vanno in due cartelle d'appoggio e il file
  si legge fino all'ultimo segmento, quindi un file tagliato si scopre quando non è cambiato niente.
  Le cartelle d'appoggio si puliscono anche all'avvio, per un'importazione uccisa a metà.
  - ⚠️ **Le caselle valgono nei due versi** (scelta dichiarata), e la conferma elenca le parti che il
    file contiene. Dopo un'importazione `Backups.imported` rilegge logo, tinte e copertine.
- ⚠️⚠️ **Vale fra versioni diverse di AIV, nei due versi** (sua istruzione). Le quattro regole vivono
  in testa a `Backup`:
  1. il formato cresce solo aggiungendo: quello che cambia prende un nome nuovo;
  2. ⚠️⚠️ una preferenza che il file non nomina non si tocca: per ogni area il file elenca tutte le
     chiavi che conosceva, col valore o fra le **assenti**, o una chiave nata dopo e una rimasta di
     fabbrica sarebbero lo stesso silenzio;
  3. l'unico cancello è la versione del contenitore, il quinto byte dell'intestazione;
  4. quello che non si sa leggere si salta, si conta e non cancella niente, e la notifica ha il suo
     testo.
  - ⚠️⚠️ **Una chiave ritirata si traduce** (`PREF_RETIRED`) invece di contarsi come sconosciuta, o la
    notifica direbbe che un file vecchio viene da una versione più recente.
  - ⚠️ **I numeri viaggiano come stringhe col tipo accanto**, e gli archivi del cestino e della
    cronologia non crescono per colonne: una colonna in più farebbe scartare le righe a una versione
    vecchia.
- ⚠️ **Restano rifiuti** quello che nessuna versione scrive: un nome che esce dalla sua cartella, un
  nome doppio, una struttura illeggibile, un testo oltre il tetto, un file del cestino che dice di
  tornare dentro la casa dell'app, una voce gonfiata apposta (AIV scrive i file senza comprimerli,
  quindi una voce non può superare quello che è entrato).
- ⚠️ **L'avanzamento è una riga della pagina, e il lavoro vive col processo**: un backup col cestino
  dura minuti. Un'importazione che scrive va fino in fondo anche annullandola.
- ⚠️ **Le finestre della password sono modali vere, la conferma no** (`Rules.md` § '👆 Che cosa fa
  il tocco FUORI da una finestra').
- **`BackupTest`** misura le preferenze e la copertura dell'archivio, le caselle, le regole fra
  versioni con una controprova per ognuna, il contenitore (password, byte cambiato, file tagliato su
  ogni confine, segmenti scambiati, vettori di PBKDF2), i file fatti a mano, la fusione e il file
  vuoto. Non vede il selettore di sistema, Drive, né il costo della chiave sul telefono.
  - ⚠️⚠️ **Trappola del banco**: la JVM rifiuta di cifrare due volte con la stessa chiave e lo stesso
    nonce, quindi un difetto che congela il contatore fa cadere le prove per la ragione sbagliata.
    Per misurarlo serve un cifrario per segmento, ed è scritto sul caso 16.
