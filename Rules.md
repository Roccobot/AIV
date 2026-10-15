# Rules.md: regole del progetto AIV

> **Cos'è questo file.** Le regole **specifiche** di `Roccobot/AIV`, l'app Android
> 'Astonishing Image Viewer'. Tutto quello che vale per ogni progetto vive nelle regole
> universali, `rules/Roccobot.md` di `Roccobot/tools`, e qui non si duplica.
> Vale per **tutti gli agenti**: il nucleo, cioè ogni regola in una riga, vive in `AGENTS.md`,
> e questo file ne dà il perché.
> ⚠️ **Le specifiche delle singole funzioni vivono in tre file a parte**, `docs/Folders.md`,
> `docs/Files.md` e `docs/Editor.md`: non si caricano da soli, e si leggono per intero quando il
> lavoro tocca una di quelle funzioni (§ '📚 Dove vivono le specifiche delle funzioni').
> ⚠️ **Fino al 2026-09-27 questo testo era il `CLAUDE.md` del repo**: una nota che nomina il
> `CLAUDE.md` di AIV (o `AIV/CLAUDE.md`) per una sezione parla di questo file, o del `docs/` in cui
> quella sezione vive adesso.

⚠️ **L'indirizzo di un documento che vive fuori dal repository, come un artefatto, si scrive in un
file committato**: il brief è stato volatile e non un archivio, e un indirizzo scritto solo là alla
sessione dopo è perso.

⚠️⚠️ **Il catalogo dei veli d'aiuto** (il micro-onboarding, uno per chiave di `Hint` più la
variante col menu Start) vive in un [artefatto](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE),
con la schermata, il punto in cui compare e la frase esatta di ogni velo (sua richiesta,
2026-10-06, voce `4.34-02`). **Si aggiorna da sé, senza aspettare che lui lo chieda, ogni volta
che un velo nasce o cambia**: la frase, il punto in cui compare, la condizione che lo mostra, il
disegno. Lo si fa nello stesso giro della versione che introduce la modifica (sua istruzione,
2026-10-08: *ogni volta che aggiungiamo un micro-onboarding, aggiorna l'artefatto relativo anche
se non te lo chiedo esplicitamente*, e subito dopo *in realtà non solo quando nasce: anche quando
modifichiamo un velo esistente*). Le schermate si fanno col banco a grafica nativa,
`it-w393dp-h873dp-xhdpi`.

## 📚 Dove vivono le specifiche delle funzioni

- **`docs/Folders.md`**: l'intestazione delle cartelle, le griglie che arrivano al vetro, il salto
  in cima e in fondo sul FAB, lo scorrimento che sopravvive alla schermata, il colore di una
  cartella, la copertina scelta a mano, 'Mostra nascoste', l'indicatore dell'ultimo media, il Feed,
  le azioni dell'icona nel launcher e la cartella sulla schermata Home.
- **`docs/Files.md`**: AIV come selettore per le altre app, il salvataggio in Download e il nome,
  l'anteprima della rinomina, lo spazio liberato dal cestino, disfare una copia o uno spostamento,
  le miniature che mentono dopo una riscrittura, le miniature memorizzate, la miniatura dei video,
  esporta e importa.
- **`docs/Editor.md`**: le otto pose, i moduli dell'editor completo coi loro gesti, gli stili, la
  filigrana, il ridimensionamento, i due tasti del salvataggio e l'uscita con del lavoro in corso.
- ⚠️⚠️ **Prima di toccare una di queste funzioni (codice, stringhe, voci di collaudo) si legge il
  suo file per intero**: là vivono le decisioni dell'utente su quella funzione, e non si rovesciano
  senza chiederglielo. Si caricano da soli solo `CLAUDE.md` e `AGENTS.md`, e questo file si
  legge prima di lavorare su AIV, quindi senza la lettura quelle decisioni non sono in scena.
- **Qui restano le regole che valgono in ogni lavoro su AIV**: il documento di feedback, il design
  system, i disegni, il vocabolario, le regole di interfaccia trasversali, gli invarianti
  dell'editor completo, il cestino, gli avvisi, le impostazioni, le prove, gli strumenti, il
  rilascio e la firma.
- Un rimando a una sezione di un altro file indica il suo nome (`docs/Editor.md` § '...'); dentro
  lo stesso file basta il titolo.

## 🔗 Il documento vivo del progetto

- Il **documento di feedback è condiviso da tutte le piattaforme e tutti gli agenti**.
  Indirizzo stabile: <https://aiv-feedback.roccobot-b90.workers.dev/feedback>.
  Titolo `Feedback AIV`; i testi visibili usano `l'agente`, senza attribuzione esclusiva.
  Il proprietario ha confermato il funzionamento completo il 1 ottobre 2026.
- ⚠️⚠️ **Ogni volta che un testo per l'utente nomina il DF, la parola è un link al suo indirizzo**
  (sua istruzione, 2026-10-05: *quando menzioni il DF, fa' sempre in modo che la parola sia
  linkata al suo URL effettivo*): in chat, nei corpi delle PR e negli artefatti si scrive
  `[DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback)`, o lo stesso link sul nome per
  esteso. Così lui apre il documento con un tocco, senza cercarlo.
- **Prima di prenderlo in carico o aggiornarlo leggi `docs/Feedback-maintenance.md`**:
  è la guida comune per fonti, convenzioni, schema JSON, salvataggio, sicurezza, errori,
  prove e pubblicazione. La configurazione tecnica vive in `cloud/feedback/README.md`.
- `docs/Feedback.md` è la fonte delle prove e degli identificatori; il generatore è
  `tools/feedback-build.py`. Si modifica la fonte, si rigenera `publish/feedback.html`,
  si controlla `--check` e si esegue `tools/feedback-check.py publish/feedback.html`.
  Per il cloud servono anche prove Node/PostgreSQL e due sessioni browser; verificare
  check e deploy del workflow `Feedback cloud`, senza dichiarare attiva una bozza locale.
- Cloudflare Workers Free serve pagina/accesso GitHub; Supabase Free conserva bozze e
  allegati privati. Credenziali soltanto lato server. `Invia` rende leggibile il giro
  senza avviare nulla. Il proprietario può modificarlo e inviarlo di nuovo; la presa in
  carico avviene solo dopo il suo via esplicito in chat. Il recupero cifrato è descritto
  nella guida comune: non leggere bozze non inviate né monitorare automaticamente il cloud.
- ⚠️⚠️ **Dopo il recupero, prima del lavoro prodotto**: travasare tutto nel brief
  `Roccobot/tools/.memo/LATEST.md`, poi toccare il codice (`Roccobot.md`
  § '📋 Prima cosa: tutto nel brief, prima del lavoro prodotto'; guida
  `docs/Feedback-maintenance.md`).
- ⚠️⚠️ **Prima di chiudere il giro: audit contro l'export precedente**: documento
  di feedback = **sorgente** del lavoro aperto; brief = **piano d'azione + backlog**
  (non archivio/changelog). Una richiesta esce dal DF solo se è fatta nel prodotto o scritta
  nel brief, e dal brief solo quando è fatta. Niente richieste perse;
  differimenti in sospeso finché fatti o cancellati; parziale ≠ completo
  (`Roccobot.md` § '🔍 Audit obbligatorio'; `docs/Feedback-maintenance.md`).
- Le prove ancora aperte restano fra release, con identificatori stabili; si archiviano
  soltanto dopo il giro completo consegnato. La riuscita delle verifiche automatiche o
  l'approvazione del documento non approva automaticamente le feature Android.
- **Il DF si pubblica o si aggiorna soltanto dopo la GitHub Release pubblicata della stessa versione, con l'APK allegato**: prima si verifica il tag `vX.XX`; il numero nella pagina o il collegamento all'APK non bastano. Se la release manca, resta una bozza locale. ⚠️ **Eccezione**: un ritocco **solo UX/documentale** del DF (layout, Altro e i suoi comandi, Etichette testuali, controlli, copy di manutenzione) si pubblica subito su Feedback cloud/Pages **senza** nuova Release APK (`docs/Feedback-maintenance.md`).
- ⚠️ **Domande**, **Etichette testuali** e **i comandi di consegna sotto Altro** (l'overlay `Consegna e copie` non c'è più dal 2026-10-03): regole operative in `docs/Feedback-maintenance.md` § Convenzioni.
- **Dopo ogni release si consegnano i passi di collaudo e il risultato atteso**, in chat
  o nel documento funzionante. Una bozza che richiede accesso a una piattaforma diversa
  non basta. Non creare un secondo documento per l'agente che prende in carico il lavoro.
- **Durante la compilazione si prepara una bozza senza ripubblicare**, salvo richiesta
  esplicita dell'utente. Nessun risveglio automatico è necessario: il giro si prende
  intero solo quando viene consegnato. La regola completa vive in `Roccobot.md`
  § '⏸️ Il giro si prende INTERO, e solo quando lo dice lui'.
- ⚠️ **La copia su GitHub Pages non c'è più dal 2026-10-03** (sua decisione: *è talmente vecchio
  che possiamo liberarcene senza problemi*): `pages.yml` lascia fuori dal sito la pagina del
  documento e i suoi script, che restano in `publish/` perché il Worker serve quella stessa cartella. La vecchia
  bozza locale è recuperabile dalla storia git, se mai servisse. Il precedente [artefatto Claude](https://claude.ai/artifact/Ln1tAcq47MCidgYd3BaNfq)
  rimane un riferimento storico per le sue voci ancora aperte, senza chiuderle implicitamente.

- **Dopo aver letto un giro, la release successiva può coprire solo una parte del backlog**: l'agente sceglie liberamente che cosa programmare, accorpare o lasciare da decidere, comunica proattivamente a Rocco in chat il piano e il residuo, e chiude ogni DF con la sezione intitolata esattamente **Prossimi passi**, in forma breve e schematica: una sorta di mini-brief.
- ⚠️⚠️ **Una voce ferma nel brief da più di dieci giri si ripropone nel DF** (sua istruzione,
  2026-10-08; la regola completa vive in `Roccobot.md` § '⏳ Dopo dieci giri, una voce ferma nel
  brief torna nel documento di feedback'). Entra nel blocco **Domande**, non numerato, dopo le
  prove e prima delle etichette, con le opzioni con la lettera e il parere; un lavoro rimandato
  entra con la domanda 'lo facciamo adesso?'. Se lui rimanda, la voce resta nel brief e torna
  dopo altri dieci giri. La forma vive in `docs/Feedback-maintenance.md` § 'Convenzioni di
  contenuto e interfaccia'.
  - ⚠️⚠️ **Nel DF di AIV entrano solo le voci di AIV, dei file di regole, delle convenzioni e
    delle impostazioni, e quelle sul DF stesso** (sua precisazione sul giro della 4.64: *in questo
    DF devono arrivare solo le questioni di AIV, ciò che riguarda file di regole, convenzioni e
    impostazioni, più le meta-questioni DF (che andrebbero fatte in chat, ma ogni 10 giri le leggo
    anche qui). Nient'altro. Questo documento è nato principalmente per AIV.*). Le voci ferme di
    un altro progetto si ripropongono in chat.
- ⚠️⚠️ **I blocchi numerati del DF sono soltanto prove: una funzione, una serie di controlli**
  (sua istruzione, 2026-10-08: *gli unici blocchi numerati sono versioni atomiche di funzionalità
  precisa (1 blocco = 1 serie di controlli)*). Dopo le prove vengono, non numerati, le
  **Domande** e le **Etichette testuali**. Nel DF della 4.64 le domande erano entrate come prove
  (`4.64-03`...`4.64-12`), e quella forma non si ripete.
- ⚠️⚠️ **OGNI TESTO ITALIANO NUOVO O CAMBIATO DELL'APP VA NELLE ETICHETTE TESTUALI DEL DF, ANCHE
  QUELLO SCELTO DA LUI** (sua istruzione, ribadita il 2026-10-08: *tutti i testi nuovi (anche
  quelli scelti da me) mi fossero sottoposti nel DF, dopo i blocchi normali, in una serie non
  numerata di etichette da confermare o riscrivere*). Dal DF 4.02 al 4.64 non ne è comparsa
  nessuna, mentre entravano 54 stringhe nuove e ne cambiavano 10: l'obbligo viveva solo nella
  guida, si leggeva come rivolto ai testi proposti dalla sessione, e con la 4.64 `Elimina tutto`
  è passato per approvato perché l'aveva scelto lui.
  - **Il presidio è una macchina**: `tools/feedback-build.py` confronta `values-it/strings.xml`
    col registro dei testi approvati, `docs/Labels-approved.json`, e si ferma se un testo nuovo o
    cambiato non ha un'etichetta. Letto il giro, `--approve-labels` registra i testi confermati.
    Il come vive in `docs/Feedback-maintenance.md` § 'Convenzioni di contenuto e interfaccia'.
- ⚠️⚠️ **Le norme sulla forma del DF** (sue, 2026-10-03), per esteso in `docs/Feedback-maintenance.md`
  § 'Convenzioni di contenuto e interfaccia':
  - il corpo contiene solo feedback sull'app, e l'interfaccia del DF si gestisce in chat;
  - l'introduzione tiene due righe sullo stato del giro e `Scarica AIV`; un link a un documento
    esterno va subito dopo, finché serve, poi si toglie;
  - ⚠️ **il link della riga di download è AIV Play dalla `5.11`** (sue richieste, 2026-10-10:
    *nella testata del DF d'ora in avanti inserisci il link per scaricare la versione Play
    dell'app*, poi *basta un link solo a Play, l'altra la aggiorno da Obtainium*):
    `Scarica e installa AIV Play x.xx`, all'APK `AIV-Play` della release del giro. Lo scrive il
    modello (`tools/feedback-page.html.in`) e lo presidia `feedback-interactive-check.py`;
  - `Aggiornamenti recenti` riepiloga un paio di giri e non è un changelog: il resto si toglie;
  - un'etichetta di testo esce dopo la prima conferma: è risolta, o torna nel brief.
- ⚠️⚠️ **Il documento vivo è uno solo**: il Changelog AIV non si aggiorna più e il Piano d'azione
  AIV non esiste più (decisioni sue). Chi ne trova l'indirizzo in un messaggio vecchio lo sappia; il
  perché vive in `Roccobot.md` § '🧾 Il changelog, provato e ritirato' e § '🗺️ Il piano d'azione,
  provato e ritirato'.
  - ⚠️ **L'ordine dei lavori in attesa vive nel brief**, una voce per lavoro col suo posto
    nell'ordine; per lui la vista d'insieme è il promemoria del documento di feedback.
  - ⚠️⚠️ **La sequenza dei lavori non si chiama 'treno', e un gruppo di lavori che esce in una
    versione non si chiama 'vagone'**, né in chat né in un commit né in un artefatto. È scritto
    per esteso perché un divieto che nomina un solo caso si legge come il permesso per gli altri.

## 🎨 Il design system, che vive fuori dal repository

- ⚠️⚠️ **Le fonti visive di AIV vivono in Claude Design, nel progetto `Roccobot Design`**, e non
  nel repository. Come si aggancia e che cosa contiene vive in `rules/Roccobot.md` § '🎨 Grafica'
  → '🎨 Claude Design, dove vive il design system'.
- ⚠️⚠️ **Non si va a pescare là da sé: è l'utente che dice, di volta in volta, che cosa prendere**
  (sua istruzione). Là lui sperimenta, quindi quello che c'è non è per forza approvato.
  - Si legge Design per quello che lui ha nominato (un colore, una misura, un componente), e il
    resto non si porta nell'app nemmeno se sembra migliore. Se una cosa sembra da cambiare, si
    propone e si aspetta.
  - ⚠️ **Tentativo scartato**: con la `1.33` sono entrati otto glifi presi dal brief dei disegni, e
    con la `1.35` sono stati tolti su sua istruzione. Il perché vive in testa a `Glyphs.kt`.
- **Che cosa c'è là per AIV**: `ui_kits/aiv_android/` ricostruisce le quattro schermate
  dell'app (cartelle, griglia, visualizzatore, impostazioni) dal sorgente Compose, e il suo
  `README.md` dichiara quello che è finto; `assets/aiv-mark.svg` è la A di AIV senza la
  piastrella, e la sostituzione con la versione nuova è un lavoro del design system, non fatto;
  `assets/icons/` contiene nove glifi che nell'app non ci sono.
- ⚠️⚠️ **Le frasi della paginetta di download vivono in un documento `.dc.html`** di Claude Design,
  fuori dai progetti di design system, che lo strumento non raggiunge: serve che lui lo mandi in
  chat o lo semini nello spazio di lavoro.
- ⚠️⚠️ **Nel repository non se ne tiene una copia, e la cartella `dev/` non si ricrea**: sarebbero
  due fonti di verità. Un disegno in prova vive nel design system, uno approvato in `res/`, e un
  file mandato in chat si lavora e non si archivia.

## 🖌️ Come entra un disegno

- ⚠️⚠️ **Un disegno vive in `res/drawable/ic_<nome>.xml`, uno per glifo, e `Glyphs.kt` è il
  catalogo**: là i nomi con cui il codice chiama un'icona e che cosa vuol dire ognuna, qui la
  geometria. Così il verificatore e l'app leggono lo stesso file. Scartati i tracciati come
  costanti di stringa in Kotlin: la loro ricostruzione nel verificatore ha sbagliato (due glifi
  alti 0,75 unità su 24, perché là le righe spezzano un numero a metà).
  - ⚠️ **L'eccezione è il disegno calcolato**: `TextCursor` nasce da quattro costanti con una
    relazione dichiarata, che in XML diventerebbe due numeri qualunque. Un altro disegno così resta
    in Kotlin, dove un tracciato fatto di chiamate tipizzate non può essere malformato.
  - ⚠️⚠️ **Al build non c'è nessuna validazione**: `aapt2` non guarda dentro `android:pathData`
    (un tracciato con la parola `ciao` compila con esito 0). La rete è `tools/icon-check.py`, e va
    lanciato.
- ⚠️⚠️ **Prima di mandare un file l'utente non deve fare quasi niente**: il trasporto copia il solo
  tracciato e scarta da sé metadati, `<defs>`, identificatori, fogli di stile e il rettangolo
  trasparente con cui Illustrator dichiara la tela. Lui esporta come gli viene comodo.
  - ⚠️ **Va espanso prima quello che il formato non conosce**: maschere, filtri, modalità di
    fusione, `<use>` e simboli, testo non convertito in tracciato, campiture a motivo,
    `stroke-dasharray`, allineamento del tratto interno o esterno. Il verificatore blocca il file
    invece di trasportarlo a metà.
  - ⚠️ **Un tratto di spessore costante non si espande**: `android:strokeWidth` esiste. Si espande
    il tratto a spessore variabile, a pennello o tratteggiato.
  - ⚠️ **La conversione la fa la sessione** (sua istruzione), e ogni icona che arriva entra
    ottimizzata; il file arrivato si lavora e non si archivia.
- ⚠️ **Forma unica e livello unico sono due cose.**
  - **Forma unica, cioè un `<path>` solo, dove la cucitura costa zero**: due forme che si toccano,
    disegnate separate, coprono due volte il bordo condiviso e viene pieno; unite, quel bordo si
    sfrangia. Lo misura la colonna `cuc` del verificatore, e a zero si uniscono.
  - ⚠️⚠️ **Livello unico non vuol dire via i gruppi**: un gruppo di sola traslazione dichiara
    l'origine, che un vettore Android non sa scrivere (l'origine è sempre 0,0, e un tracciato con
    coordinate negative si ritaglia). Appiattirlo è scartato: cambia 9 pixel su 230.400 (virgola
    mobile a 32 bit, misurato su `ic_aiv_mark`) e rompe il confronto carattere per carattere col
    file di partenza, che è il solo modo di verificare un trasporto.
  - **È un livello di troppo** un gruppo che non trasforma niente, o più di un gruppo. Un gruppo
    che scala o ruota va bene solo se esprime una convenzione, come il rientro del 65% dell'icona
    adattiva in `ic_launcher_foreground.xml`.
  - ⚠️ **Misura scartata**: 'livello unico sempre, costa 7 pixel' veniva da un appiattimento fatto
    in virgola mobile, cioè da un errore di calcolo.
- ⚠️ **I numeri incollati si separano**: `-.05.1` sono due numeri. Android li legge e un parser
  stretto li rifiuta; separarli dà zero pixel di scarto.
  - ⚠️ **Il numero di byte non è un criterio** (sua istruzione: massima compatibilità e
    correttezza): un separatore si mette anche dove il minimo basterebbe, e un comando implicito si
    scrive per esteso. Si ottimizza per chi legge il tracciato.
- ⚠️ **`android:fillType` si dichiara sempre**, anche quando l'altra regola darebbe lo stesso
  disegno: la coincidenza cade il giorno che qualcuno aggiunge un sottotracciato. Il verificatore
  segnala il caso pericoloso (il disegno dipende dal verso e la regola non è dichiarata) e tace
  sugli altri.
- ⚠️ **La griglia di Material è 24 con 2 di margine, ma una tela diversa non è un difetto**: i due
  glifi dell'allineamento restano 800x800, perché riscalarli vorrebbe dire mille arrotondamenti e
  un disegno non più confrontabile col file di partenza. `android:width` e `android:height` dicono
  la misura, il viewport le unità; il verificatore riporta il loro rapporto e non lo giudica.
- ⚠️⚠️ **Un file che arriva si misura prima contro il glifo di Material, e a zero pixel di scarto
  vince Material**: due copie dello stesso disegno sono vietate in testa a `Glyphs.kt`.
  - **Come si misura**: il tracciato di `Icons.Filled.<nome>` si ricostruisce dal bytecode di
    `material-icons` (le chiamate a `PathBuilder` in ordine), si rende accanto al file a 240px e si
    contano i pixel diversi. ⚠️ Confrontare le due `d` non serve: assoluto contro relativo cambia
    quasi tutti i numeri e nessun pixel.
  - ⚠️ **Zero vuol dire zero**: un file ammorbidito da lui parte dallo 0,1% della tela.
  - ⚠️ **Il caso in mezzo si dichiara**: uno scarto sopra zero può essere una scala o una
    traslazione uniforme dello stesso disegno, che si vede dall'inchiostro (stesso centro, lati in
    proporzione). Allora resta Material, e la misura va nel commento.
- ⚠️⚠️ **L'arrotondamento a 0,4 degli spigoli è una regola del progetto** (sua istruzione) per ogni
  disegno che entra in `res/`, suo o di casa. Il presidio è `tools/icon-round.py` (§ '🧰 Gli
  strumenti che questo repo si porta dietro').
  - ⚠️ **Il verificatore dice dove manca un raccordo e con che raggio, e il raccordo lo fa chi
    disegna**. Scartato un `--fix` automatico: sul segno di spunta ha raccordato il vertice in cui
    il giro si chiude al posto della punta, senza nessun errore.
  - ⚠️⚠️ **Le punte delle icone esistenti sono raccordate, tranne quelle di `ic_tian`**, il suo logo
    personale, che non si tocca mai (regola universale in `Roccobot.md` § '🧹 Bonifica e
    ottimizzazione degli asset'): `icon-round.py` lo dichiara escluso invece di contarne le punte.
    La misura di ogni disegno (punte raccordate, pixel cambiati su 57.600) vive in coda al suo
    commento.
  - ⚠️⚠️ **Il criterio è quello stretto, suo: solo gli angoli convessi esterni.** Un buco non prende
    raccordi, e contorno o buco lo dice la profondità di contenimento, quindi un'isola dentro un
    buco torna a contare. La frase vecchia 'un buco prende gli stessi raccordi' è decaduta.
  - ⚠️ **Quanto si vede**: su un angolo retto il raccordo arretra il vertice di 0,166 unità su 24,
    mezzo pixel a tre volte. È correttezza formale, non un effetto visibile.
  - ⚠️ **La verifica ha trovato un difetto vero**: in `ic_mod_detail` gli archi avevano i raggi
    al posto della rotazione e della bandierina, e quattro tondi su sei venivano col raggio
    sbagliato. Da lì `icon-check.py` rifiuta una bandierina di arco che non sia `0` o `1`.
- ⚠️⚠️ **Un disegno può nascere qui**: la sessione ammorbidisce i glifi di Material col trattamento
  che lui dà ai suoi (sua richiesta), toccando i giunti che sporgono e non gli angoli che rientrano.
  - ⚠️⚠️ **Il raggio non è costante: è costante quanto il vertice arretra**, con tetto pari
    all'arretramento dell'angolo retto. Con 0,4 fisso le punte acute vengono tozze (1,15 unità su
    24 a 30 gradi, contro le 0,17 dell'angolo retto); sulla famiglia Material, quasi tutta ad
    angoli retti, il raggio resta 0,4.
  - ⚠️ **Il raccordo è un arco fra due rette e una quadratica col controllo nel vertice dove c'è
    una curva**: un arco è tangente ai suoi due lati solo se sono rette.
  - ⚠️ **Il tracciato si ricostruisce dal bytecode**, si raccorda e si riscrive in coordinate
    assolute coi comandi per esteso; gli scarti misurati vivono in testa a ogni file.
  - ⚠️⚠️ **Un glifo senza spigoli convessi esterni resta a zero pixel, e resta di Material**:
    `Palette` e `Timeline` (i moduli Colore e Curve chiamano `Icons`), `CropPortrait` e
    `CropLandscape`. `PhotoSizeSelectLarge` ne aveva quarantotto ed è diventato `ic_resize.xml`;
    `BrandingWatermark` ne aveva zero, ed è entrato in `res/` come `ic_watermark.xml` solo quando è
    stato specchiato su sua richiesta (`docs/Editor.md` § '🎛️ I due tasti del salvataggio, e i
    loro due gesti').
  - ⚠️ **L'icona dell'HSL nasce da `Icons.Filled.Tune`** (tre cursori) e non da `Colorize` (il
    contagocce, che è il gesto del colore mirato), su sua istruzione. La sua riserva ('sembra
    Impostazioni') è accettata, e chi vuole chiuderla gli propone un glifo suo.

## 🗣️ Come si chiamano le cose

- ⚠️⚠️ **Una funzione che ha una voce nelle impostazioni si chiama con l'etichetta di quella voce,
  alla lettera**, nelle voci di collaudo, in chat, negli artefatti e nei commit. Il nome interno
  resta nel codice, dove serve a chi legge il codice. Il caso che l'ha fatto nascere: per tre
  versioni gli si è chiesto di provare una funzione con una parola ('velo') che nel telefono non
  compariva.
  - ⚠️⚠️ **Quando la prova non è ovvia si scrive il passo passo** (sua richiesta): una funzione
    spenta di fabbrica prima si accende, e dirlo è parte della voce.
- **I nomi**, ognuno con la parola del codice che resta e non si rinomina (rinominare non cambia
  niente per chi usa l'app e rompe ogni rimando):

| si dice | non si dice | nel codice resta |
|---|---|---|
| **intestazione** (la fascia in cima a una cartella; la voce è 'Intestazione delle cartelle') | 'frontespizio', terminologia morta ovunque | il prefisso `front` (`Front.kt`, `frontWash`, `FRONT_INK`) |
| **gradiente** (la sfumatura d'accento dietro l'intestazione, etichetta del suo interruttore) | 'tinta' | `frontWash`, `WASH_STOPS` |
| **FAB** | 'tastino' | |
| **Sfocatura dietro i pannelli** | 'velo' | `Veil.kt`, `lowered()` |
| **immagine** | 'fotografia', se non si parla di uno scatto vero (EXIF, tempi, sensore, galleria) | |
| **editor semplice** ed **editor completo** | 'editor di casa'; 'editor avanzato' non è il nome (domanda fatta, risposta: resta 'Editor completo') | `EditorScreen.kt`, `AdvancedEditorScreen.kt`, `Editors.INTERNAL`, `EditorCasaTest`, e le chiavi dei giri che dicono `casa` |
| copertina **predefinita** (quella che l'app sceglie da sé, l'ultima immagine della cartella) | 'automatica' | |
| **file di impostazioni** (il file dell'esportazione, parola sua) | | `Backup`, `backup_*` |
| **tondo** (il terzo segno dell'ultimo media) | 'pallino' | `DOT`, `lastDot`, `MARK_DOT` |
| **Menu 'Start'**, con gli apici dritti (il menu che cresce dall'angolo; sua nota A del giro della `4.25`) | 'menu angolare' | `MainControl.CORNER`, `CornerMenu`, la stringa `main_control_corner` |
| **Menu basso** (la barra in fondo allo schermo; stessa nota, e dalla `4.33` la voce `4.30-01`) | 'menu inferiore', e 'Menu' da solo | `MainControl.BOTTOM`, `BottomMenu`, la stringa `main_control_bar` |
| **stili** ('Salva stile', 'Stili AIV', 'Stili salvati') | 'preset' | la parola `preset` del codice |
| **elemento** o **oggetto** (quello che si disegna col modulo Disegno: una linea, una freccia, un'ellisse, un rettangolo; sua correzione del 2026-10-08: *quelli che chiami 'segni' sono 'elementi' o 'oggetti'. In questo contesto, per un umano, 'segno' non ha senso*) | 'segno' | `Mark`, `marks`, e il nome della variabile `segno` |
| **modalità griglia o lista**, e 'modalità griglia e lista' (le due viste dell'elenco iniziale nominate insieme; sua istruzione del 2026-09-28, per la cacofonia 'vista'/'lista') | 'vista griglia o lista', 'viste griglia e lista' | |

- ⚠️ **Le sfumature in fondo allo schermo sono un'altra cosa dal gradiente**: se in una frase ci
  sono tutte e due, quella dell'intestazione si nomina per esteso.
- ⚠️ **'Immagine' perché questa app apre GIF, WebP animate, PNG con trasparenza, tavole, scansioni,
  schermate**: chiamare tutto 'fotografia' esclude a parole metà di quello che fa. Le stringhe
  erano già a posto, quindi il difetto vive nella prosa.
- ⚠️ **Le citazioni sue restano come le ha dette**, anche con una parola che qui non si usa.
- ⚠️ **Una bonifica di un nome nella prosa si cerca senza distinguere le maiuscole, e anche
  spezzata su due righe**: una passata a sole minuscole aveva lasciato le occorrenze nei titoli
  delle note, scritti in maiuscolo.
- ⚠️ **Tutti i pesi dell'app si scrivono con `formatBytes`** (sua risposta `punto` a
  `d-pesi-scrittura`): lo stesso file non deve leggersi in due modi in due posti.

## 📍 Che cosa vuol dire 'centrato'

- ⚠️⚠️ **Centrato vuol dire centrato in orizzontale e, in verticale, centrato ma il 15% più in
  basso** (definizione dell'utente): il pollice arriva meglio sotto la metà dello schermo. Vale per
  tutto quello che si apre in mezzo: dialoghi di conferma, pannelli, modali, i menu a pressione
  lunga.
  - ⚠️ **La stretta è parte della definizione**: una cosa alta si prende lo spazio che le serve, e
    lo spostamento si riduce fino a sparire quando sotto non c'è più aria.
  - ⚠️ **Il 15% si misura sull'altezza della finestra**, non sullo spazio libero, o su un dialogo
    alto il movimento sparirebbe proprio dove il pollice fatica di più.
  - ⚠️ **`windowHeight()` toglie anche la tastiera**, per ogni superficie centrata: senza, l'aria
    che la stretta misurava era già presa dalla tastiera e il pannello scendeva dentro di lei.
  - ⚠️ **Il tetto**: una superficie centrata non supera la finestra meno l'aria, toglie di sotto e
    non di sopra, e il suo scorrimento interno entra in funzione. L'aria minima della stretta e
    del tetto è `LOWER_AIR` (16dp).
- ⚠️⚠️ **Le finestre in cui si scrive non sono centrate: il loro bordo di sopra vale `TEXT_AIR`
  finché il pannello ci sta, qualunque sia la tastiera.** Sono le modali vere, che passano `null`
  a `lowered` (§ '👆 Che cosa fa il tocco FUORI da una finestra'), e il conto vive su `pinned` in
  `Centred.kt`: finché c'è spazio, a spostarsi è solo il bordo di sotto. La prova è `AltoTest`.
  - ⚠️⚠️ **Quando la tastiera non lascia spazio cedono l'aria sopra e poi il margine sotto i
    tasti (`TEXT_TUCK`), che può finire dietro la tastiera; solo oltre scorre il contenuto**
    (sua scelta B1, 2026-10-11). Il conto vive su `pinTop` e `pinRoof`.
  - ⚠️⚠️ **La scatola è alta quanto la finestra, e lo spazio si misura dentro di lei, dalla
    `5.51`** (nota del giro della `5.50`: la rinomina aperta dal visualizzatore era tagliata). Con
    il decoro che segue le barre, `DialogWrapper` non dà a queste finestre una modalità per la
    tastiera, quindi Android non le ridimensiona: le sposta in alto, e un conto che contava sulla
    centratura posava il pannello altrove. Siccome il vincolo non cambia, il nodo si rimisura da
    sé quando lo spazio visibile cambia: senza, dalla griglia il conto non vedeva la tastiera.
  - ⚠️⚠️ **Al primo fotogramma conta la tastiera che sta per arrivare, dalla `5.60`** (sua nota
    prima del giro della `5.51`: il pannello saltava sempre alla comparsa). Il campo prende il
    fuoco all'apertura, quindi la tastiera arriva sempre ma un attimo dopo: per un secondo al
    massimo si conta l'altezza ricordata (`Keyboard`, per orientamento, fra un avvio e l'altro), e
    la prima volta, senza un'altezza ricordata, il pannello resta invisibile finché lei arriva. Il
    conto vive su `keyboardBottom`, e lo presidia `AltoTest`.
  - ⚠️⚠️ **Tentativo scartato: far salire il pannello all'arrivo della tastiera** (la deroga con
    `KEYBOARD_AIR`, la salita a soglia, `climbFor`, l'isteresi). La salita si ricavava da una
    misura che cambia mentre si scrive (la barra dei suggerimenti, l'animazione dell'IME), e il
    pannello ballava. Quelle costanti non ci sono più nel codice; `LOWER_AIR` resta.
- **Come si applica**: `Modifier.lowered(onOutside)`, in `Centred.kt` col numero. La riga si scrive
  a ogni chiamata, perché in Compose non c'è un aggancio globale per i dialoghi (`AlertDialog`
  centra dentro la propria finestra); quello che si può avere è un modificatore solo, e il valore
  non è mai scritto due volte.
  - ⚠️⚠️ **Il parametro di `lowered` non ha un valore di serie, di proposito**: chi apre una
    finestra dichiara se è una modale vera, e non lo può fare per omissione.
- ⚠️ **I menu non usano quel modificatore ma lo stesso numero** (`LOWER_BY`), da un
  `PopupPositionProvider` (`MenuCenter` in `Menus.kt`).
  - ⚠️⚠️ **Un menu ancorato si ferma a tre margini di griglia dal vetro, uguale coi tre effetti**
    (sua richiesta): il difetto di allora erano due bordi vicini, non la fascia sfocata.
  - ⚠️ **Il pannello di un menu è più largo di due celle più il loro spazio**, col tetto che nella
    schermata iniziale, a due colonne, entra in funzione. Misure e conto vivono su `menuFloor`.
  - ⚠️⚠️ **Quella distanza si scrive come una soglia e non come un margine**: un margine è un
    minimo, e il candidato naturale di un menu ancorato lo rispetta già, quindi non lo muove. Lo ha
    misurato il banco.
  - ⚠️ **L'aria dell'ombra si sconta**, perché vive dentro la finestra.
- ⚠️⚠️ **In una finestra bassa (sotto i 480 punti, cioè il telefono in orizzontale) un pannello
  centrato si allarga fino a `Adaptive.dialogMaxWidth`**, dalla `3.72` (voce `3.71-08`: la conferma
  'Vuoi nascondere...' aveva l'ultimo tasto tagliato). Lo fanno insieme `loweredWindow`, che là
  spegne la larghezza di serie dei dialoghi di Android (in orizzontale è circa il lato corto dello
  schermo), e lo spostamento di `lowered`, che alza il minimo. Per questo anche un dialogo che si
  chiude toccando fuori passa `properties = loweredWindow(onDismiss)`.
  - ⚠️ **L'altezza utile di un dialogo toglie le barre dell'attività** (`insideBars`, dalla `3.71`):
    la finestra del dialogo è disposta dentro le barre e i suoi rientri non le contano.
- ⚠️ **Un dialogo a tutto schermo non si sposta** (`DestinationDialog`): non ha un centro da
  spostare.
- ⚠️ **Le 'Info dettagliate sul file' sono una scheda in fondo, e non una superficie centrata**:
  non usano `lowered()`, e il velo se lo chiedono da sé.

## 🌫️ Sfocatura dietro i pannelli e bordo

- ⚠️⚠️ **`lowered()` mette anche il velo e la sfocatura dietro la finestra**: l'elenco delle
  superfici che li vogliono è lo stesso di chi chiama `lowered()`, e un velo mancante non si vede
  (un centro mancante sì). Il perché per esteso vive in testa a `Veil.kt`.
- ⚠️⚠️ **Ogni menu dell'app passa da `MenuShell`**: velo, stondamento, entrata, uscita,
  scorrimento e collisione col bordo si scrivono una volta sola, e chi aggiunge un menu non ha un
  secondo modo con cui sbagliare. La ragione è sua: un sistema affidabile, in cui un ritocco si
  ragiona una volta per tutti. Chi apre un `Popup` o un `Dialog` scritto in casa chiama
  `WindowVeil()` a mano (lo fanno `MenuShell` e `Sheet`).
- ⚠️⚠️ **Dalla `4.38` il bordo d'accento è spento: i pannelli sono nudi** (sua scelta del
  2026-10-06, variante A di un mockup a tre varianti: *implementiamo subito i pannelli nudi (A)
  spegnendo il contorno, e facendo in modo che lo si possa recuperare in seguito se cambio idea*).
  Pannelli e schede staccano dallo sfondo col solo colore della superficie, sopra la sfocatura e il
  velo. **Si riaccende con una riga**, `ACCENT_EDGE` in `Edge.kt`: i modificatori restano chiamati
  da tutte le superfici e il disegno resta intatto. Il bordo non ha mai occupato spazio, quindi
  spegnerlo non sposta nessuna misura; lo presidia `BordoTest`. Le note qui sotto descrivono il
  bordo com'era fino alla `4.37`, e valgono di nuovo se si riaccende.
- **Il bordo d'accento da 2dp sostituisce l'ombra, e non dipende dall'interruttore della
  sfocatura** (sua richiesta): è il modo in cui l'app è fatta, non una funzione che si accende.
  Spessore scartato: 3dp (sua preferenza, dopo un giro). Il come vive in testa a `Edge.kt`.
  - ⚠️ **Il bordo ha un elenco suo**: la scheda della selezione e il pannello dei comandi
    dell'editor hanno il bordo e non il velo. Il velo dice 'mi apro sopra qualcosa', il bordo 'sono
    una superficie di questa app'.
  - ⚠️ **Il raggio del bordo**: due riquadri stondati concentrici hanno raggi diversi, e
    l'antialiasing sommato due volte lascia pixel scoperti sull'arco. Il rimedio è mezzo pixel di
    sconfinamento in fuori, misurato in testa alla costante in `Edge.kt`.
  - ⚠️ **Sulle schede in fondo la riga va di fuori** (sua prova): i fianchi sono sui bordi dello
    schermo, quindi a interrompere il tratto è il bordo del vetro.
- ⚠️⚠️ **Il velo lo dipinge l'app** (`AppVeil`, messo in scena da `AivTheme`) e vale il massimo
  delle richieste in scena: due finestre non cambiano il proprio velo nello stesso fotogramma,
  quindi fra un menu e un dialogo c'era sempre un fotogramma con due veli o con nessuno. La
  sfocatura resta un attributo di finestra.
  - ⚠️⚠️ **Dalla `4.44` il velo è il 30%, nero sul tema chiaro e bianco sullo scuro, e compare in
    800 ms** mentre pannello e sfocatura arrivano col loro tempo (sua richiesta sul giro della
    `4.43`); scende invece subito, insieme a chi lo chiede. Numeri, curva e misure scartate vivono
    in `Veil.kt` (`VEIL_DOSE`, `veilInk`, `AppVeil`).
  - ⚠️⚠️ **Dalla `4.46` la finestra che esce tiene la sfocatura piena finché un'altra superficie
    chiede più velo di lei** (sua nota su `4.45-01`: *lampeggio 'attenuato' ma ancora visibile*):
    la finestra nuova compare qualche fotogramma dopo la sua composizione, e in quei fotogrammi la
    sfocatura del menu che esce calava già. La decisione è `blurHeld`, misurata da `VeloTest`; la
    sfocatura di finestra il banco non la vede.
  - ⚠️⚠️ **E la selezione ha un'ombra sua, dalla `4.46`** (sua nota E, col suo mockup): la scheda
    della selezione non vela la griglia, che deve restare toccabile, quindi sopra di lei una
    sfumatura nera (bianca sul tema scuro) sale da zero, 120dp sopra, fino al massimo sul bordo,
    disegnata dietro le celle per gli spazi fra di loro e dentro ogni cella sotto la spunta
    (`PickShade.kt`, `OmbraSceltaTest`).
    - ⚠️⚠️ **Dalla `4.48` la metà dietro le celle vive sulla colonna intera della schermata, prima
      dei suoi margini, e il massimo è il 24,5%** (sua nota su `4.46-04`: *l'ombra appare in un
      rettangolo che non dovrebbe esserci*, e *l'opacità massima dev'essere la metà*). Sul nodo
      della griglia si fermava 8dp dentro ogni lato e sopra la scheda. La copia della scena in
      `OmbraSceltaTest` non poteva vederlo, perché il difetto era in dove la schermata mette
      l'ombra: lo misura `OmbraGrigliaTest`, che monta `GridScreen`.
  - ⚠️⚠️ **Dalla `4.45` il passaggio fra due superfici non lampeggia** (sua nota su `4.44-02`, dal
    menu di una miniatura a 'Info'): il velo non scende finché un'altra superficie sta entrando
    (`VeilStage.sale`), e una finestra che si apre con un velo già in scena sfoca subito al pieno
    invece di ripartire da zero. Il velo lo misura `VeloTest`; la sfocatura di finestra il banco
    non la vede.
- ⚠️⚠️ **Una sfocatura che cala vuole in scena qualcosa che se ne va**: senza una causa in scena, il
  raggio che scende si legge come una messa a fuoco. Quindi i menu escono con una dissolvenza sola
  di 75 ms (pannello, sfocatura e livello scuro insieme), e la coda lunga resta sulla sola scheda in
  fondo, dove lui ha detto che l'arrivo e la sparizione sono perfette.
  - ⚠️⚠️ **Scartati**: un'uscita lunga dei menu (280 ms), che cadeva sulla composizione della
    schermata d'arrivo e si vede solo toccando una voce che porta altrove; la sfocatura legata al
    pannello, che toglieva la decelerazione; il pannello che si rimpicciolisce in uscita, che
    rimisura e riposiziona la finestra a ogni fotogramma.
  - L'entrata dei menu resta 120 ms, e in entrata patina e sfocatura crescono col pannello.
  - ⚠️ **Livello scuro e sfocatura sono due meccanismi separati mossi da un numero solo**: il primo
    è un rettangolo dipinto, la seconda un attributo di finestra. Lo stato è uno, `visible`
    (`veiling` non c'è più), e la finestra non sopravvive al proprio pannello.
  - ⚠️⚠️ **Trappola, misurata sul bytecode di Compose**: `PopupLayout.updatePopupProperties`
    assegna `params.flags` invece di aggiungerli, quindi un `PopupProperties` che cambia durante
    un'animazione fa perdere sfocatura e velo per un fotogramma, senza errori.
  - I dialoghi di Material non hanno un'uscita da animare.
- ⚠️⚠️ **La sfocatura è dietro un'impostazione, e di fabbrica è accesa la sfocatura** (sua
  istruzione; l'ombra resta una delle tre risposte).
  - ⚠️⚠️ **Chi ha già l'app non vede il valore di fabbrica nuovo, e si dichiara**:
    `SettingsStore.save` riscrive tutte le chiavi, quindi un valore di fabbrica si vede solo dove
    l'archivio tace. Non si rimedia con una migrazione: l'archivio non distingue una scelta da un
    valore ereditato, e una migrazione rovescerebbe una scelta vera.
  - ⚠️ **I casi della migrazione del velo sono tre** (acceso, spento, mai toccato) e non si
    accorpano, anche se due oggi rispondono lo stesso.
  - ⚠️ **Il valore di fabbrica vive in due posti** (il campo di `Settings` e la lettura del
    flusso): cambiarne uno solo dà un'app accesa al primo avvio e spenta dopo il primo salvataggio.
    Lo presidia il caso 6 di `ProfonditaTest`, che legge un archivio vuoto con
    `SettingsStore.read` e lo confronta con `Settings()`.
  - ⚠️ **La sfocatura la esegue il compositore di sistema** (`FLAG_BLUR_BEHIND` più
    `blurBehindRadius`, da Android 12): dove il sistema dice di no non si vede e non costa
    fotogrammi, e l'app lo chiede (`blurs`, in `Veil.kt`). Si paga solo a pannello aperto, e lo
    scorrimento di una griglia e l'editor completo non la incontrano. ⚠️ **Quanto pesi non è
    misurato**, e si scrive così.
  - ⚠️ **Spento vuol dire non toccare niente**: i dialoghi tornano al velo di Android (0,6), i menu
    senza. La scheda in fondo se lo chiede da sé (`SHEET_DIM` in `Sheet.kt`).

## 👆 Che cosa fa il tocco FUORI da una finestra

- ⚠️⚠️ **Una finestra è una modale vera solo se esiste per raccogliere un input scritto** (criterio
  dell'utente): oggi Rinomina, Estensione, Indirizzo e Nuova cartella. Tutto il resto si chiude
  toccando fuori.
  - ⚠️ **Le conferme di eliminazione non sono modali**: il tocco fuori vale `Annulla`, l'esito
    sicuro. Una modale protegge il lavoro che si perderebbe, non la scelta che si eviterebbe.
- ⚠️⚠️ **Una modale si dichiara con due righe insieme: `Modifier.lowered(null)` e
  `properties = loweredWindow(null)`.** Senza la seconda la finestra non è modale, e nessun errore
  lo dice. Il modificatore governa l'aria dentro la finestra, le proprietà lo schermo fuori
  (`dismissOnClickOutside` lo legge il gestore delle finestre prima dell'app).
  - ⚠️ **L'aria sopra un pannello abbassato appartiene alla finestra**: `LowerNode` gonfia la
    scatola e ci posa il pannello in fondo, quindi toccarla è toccare dentro. Il perché vive su
    `Air` e `OutsideNode`, in `Centred.kt`.
- ⚠️⚠️ **Un nodo che è insieme di layout e di tocco non riceve eventi**: la hit-test scorre i nodi
  fino al primo nodo di layout e si ferma là. Quindi chi misura e chi ascolta sono due nodi, e chi
  ascolta va prima, così il suo riquadro è la scatola gonfiata. Lo ha misurato il banco; nessun
  controllo sul testo del programma poteva vederlo.
- ⚠️⚠️ **Da Android 12 il popup di un menu non è modale al tocco** (`createFlags` parte da
  `FLAG_WATCH_OUTSIDE_TOUCH` e non mette `FLAG_NOT_TOUCH_MODAL`, letto nel bytecode), quindi un
  tocco fuori arriva anche all'app sotto, che apre la riga o la miniatura sotto il dito. Lo ripara
  `MenuGuard`, uno solo, in `AivTheme`.
  - ⚠️ **`MenuGuard` non si aggancia a `VeilStage`**, che è vuota quando la sfocatura è spenta: la
    correzione diventerebbe una funzione facoltativa.
- ⚠️⚠️ **Un nodo che copre lo schermo esiste solo quando serve.** La `1.70` si avviava senza
  rispondere a nessun tocco, perché `MenuGuard` era sempre in scena e si limitava a non consumare.
  La hit-test sceglie a chi mandare l'evento prima che il gestore giri, e fra fratelli si ferma al
  primo ramo colpito: 'non consumare' non vuol dire 'lascio passare a chi è sotto'.
  - **La forma giusta è il nodo dentro un `if`**, lo schema di `ViewerScreen.kt`: il rimedio è
    l'assenza, non una condizione più furba. Il prezzo è una ricomposizione della radice a ogni
    apertura di un menu.
  - ⚠️ **Una nota che descrive un difetto non impedisce di rifarlo con un altro strumento**: la
    stessa trappola era già scritta sul cancello di `MenuShell`, in `Menus.kt`, presa con un
    `Popup` e non con un `Box`. Per questo la regola vive qui.
  - ⚠️ **Lo prova il banco, senza dispositivo** (`TocchiTest`: senza menu aperti il tocco arriva
    all'app, e `MenuGuard` ferma quello fuori da un menu aperto). Il difetto era arrivato in
    produzione perché allora il banco non c'era; chi tocca un nodo che copre lo schermo guarda
    comunque prima se il nodo esiste anche quando non serve.

## 🌗 Il tema scelto DENTRO l'app non è quello di sistema

- ⚠️⚠️ **Una risorsa letta con `colorResource` segue la configurazione di Android, non il tema
  scelto nell'app** (`LocalAivLight`, la voce in 'Aspetto'): con 'Chiaro' scelto in AIV su un
  telefono in tema scuro, vince quello sbagliato. È arrivato a lui due volte.
  - **Come si legge una risorsa nel tema dell'app**: con un contesto a `uiMode` forzato
    (`createConfigurationContext`), come fa `aivLauncher` in `Theme.kt`; così le risorse restano la
    fonte unica dei colori dell'icona.
  - ⚠️ **Stessa famiglia**: `enableEdgeToEdge()` senza argomenti costruisce due
    `SystemBarStyle.auto`, che leggono la configurazione. Chi trova un colore che non segue il tema
    guarda prima da dove viene quel colore.
  - ⚠️ **Il banco non lo vede**: una prova gira in una configurazione sola. Si guarda sul telefono,
    coi due temi.
- ⚠️⚠️ **Le info mostrate insieme all'immagine seguono 'Tema dello sfondo', non il tema dell'app**
  (sua nota, giro della `4.02`): la barra delle info e il pannello laterale degli schermi larghi
  prendono la tavolozza da `AivPalette`, in `Theme.kt`, con lo stesso chiaro o scuro che il
  visualizzatore dà allo sfondo.
- ⚠️⚠️ **L'icona in testata e il FAB sono incrociati di proposito** (sua specifica): l'icona segue
  il tema in vigore, il FAB mostra l'accento dell'altro tema, e a menu aperto quello del tema in
  vigore. Un FAB che 'non segue il tema' non si corregge.

## 🎬 Le animazioni dentro una schermata che arriva

- ⚠️⚠️ **Nessun cambio di schermata è un taglio netto, dalla `4.36`** (sua indicazione del
  2026-10-06: *una transizione di 100ms ogni volta che c'è un cambio netto di schermata*, nata
  dal 'flash' al tocco di una miniatura, nota A del giro della `4.35`). Dove prima la schermata
  si sostituiva di colpo (l'entrata e l'uscita del visualizzatore, le pagine delle impostazioni)
  c'è una dissolvenza di sola opacità da 100 ms, `CUT_FADE_MS`; le schermate che avevano già
  quella da 180 ms la tengono. Una schermata o una pagina nuova passa da uno dei due, e lo
  misura `TagliTest`. ⚠️ **È una prova**: se non va, alla versione dopo si toglie.
- ⚠️⚠️ **Le opacità si moltiplicano**: un'animazione giocata sotto la dissolvenza di schermata
  (180 ms, `cambioSchermata` in `ViewerActivity`) si vede solo per la coda. Quindi un'animazione
  che deve farsi vedere aspetta che la schermata sia arrivata, e quello che arriva con la schermata
  (uno sfondo, una fascia, il contenuto) resta dov'è.
  - ⚠️ **Fra la fine della dissolvenza e il primo fotogramma di un'animazione che aspetta passano
    quattro fotogrammi**, e sono della transizione (`Transition` porta `currentState` su
    `targetState` dopo l'ultimo valore animato): non si compensano.
  - ⚠️ **Il meccanismo per aspettare (`LocalArrivo`, `ConArrivo`) è stato tolto** con l'entrata del FAB,
    per sua scelta di semplificare; chi ne ha bisogno lo ritrova nella storia git. La regola resta.
- ⚠️⚠️ **Trappola: `MenuShell` animava alla prima composizione.** Un `LaunchedEffect` con
  `animateTo` verso il valore già raggiunto tiene l'animazione in corsa, quindi `MenuState.visible`
  rispondeva 'menu in scena': il FAB si staccava in una finestra sua per sei fotogrammi (pieno,
  mentre il resto sfumava), e `MenuGuard` mangiava ogni tocco per un decimo di secondo dopo ogni
  cambio di schermata. Il rimedio è in due punti: la guardia in `MenuShell`, e `MenuState.visible`
  senza `show.isRunning`.
- ⚠️⚠️ **Il FAB si congeda rimpicciolendosi, glifo compreso, quando si va dove non c'è** (sua
  richiesta; la scelta fra le due varianti è dichiarata: quella a maschera mutila il glifo).
  - **Dove si sta andando lo sa solo `AivApp`**: il valore viaggia in `LocalSenzaFab`, fornito
    dentro la transizione, e lo legge `TapHoldFab`, così un FAB nuovo lo prende per costruzione.
  - ⚠️ **L'uscita dura meno della dissolvenza** (100 ms, con una curva che parte veloce):
    allungarla peggiora, perché il rimpicciolimento finirebbe sotto un velo che non lascia passare
    niente.
  - ⚠️ **La scala si moltiplica al rimbalzo invece di sostituirlo.**
  - **Le prove**: `UscitaFabTest` conta i pixel coperti (la scala vive in un `graphicsLayer`,
    quindi `boundsInRoot` non vedrebbe niente); `CambioSchermataTest` misura che durante il cambio
    il FAB non si muove e non cambia misura.

## 🖍️ Due trappole del disegno in Compose

- ⚠️⚠️ **`drawBehind` disegna dietro il contenuto del proprio nodo, non dietro i fratelli che il
  genitore ha già disegnato**: una tinta che deve stare dietro a più nodi vive sul blocco che li
  contiene. Il caso è il titolo in testata che spariva sotto il gradiente dell'intestazione
  (`docs/Folders.md` § '🖼️ Intestazione delle cartelle, e le due schermate che la mostrano').
- ⚠️⚠️ **Un gradiente a pochi livelli su un'altezza grande fa bande per aritmetica**, non per i
  colori scelti: ogni gradino di colore è alto decine di pixel. Il dithering del `Paint` non è
  bastato, per una ragione che non si conosce, e `Modifier.background(brush)` non dà accesso al
  `Paint`; il rumore lo scrive l'app (`Dither.kt`).
  - ⚠️⚠️ **Dalla `4.90` vale per ogni rampa di una tinta sola, e lo strumento è `GrainedRamp`**
    (sua segnalazione del 2026-10-08, *vedo di nuovo un po' di banding*): fino alla `4.81` qui era
    scritto che le fasce in fondo allo schermo non ne avevano bisogno, perché vanno dal fondo pieno
    al trasparente. Il conto era sbagliato: sopra un'immagine una rampa attraversa i livelli fra il
    fondo e l'immagine, e là un gradino viene alto una dozzina di pixel. Restano fuori le sfumature
    alte o larghe pochi punti (il filetto, l'alone di una copertina), dove un gradino è di un pixel.

## 🎛️ Ogni cursore è `ValueSlider`

- ⚠️⚠️ **Dalla `6.22` un cursore nuovo si scrive con `ValueSlider`, mai con lo `Slider` di
  Material**: Material 3 1.5.0-beta01 dichiara superata la forma `Slider(value, onValueChange,
  ...)`, che AIV usava in 16 punti, e la sostituzione che suggerisce
  (`rememberSliderState(value, ...)`) usa il valore solo come partenza. Con lei la barra di un video
  resterebbe ferma mentre il video va, e su un intervallo da 0 a 0, perché la durata arriva dopo il
  primo fotogramma. `ValueSlider` ripete il corpo della forma vecchia, letto nei sorgenti della
  beta01, e `ValueSliderTest` fallisce con la sostituzione suggerita.
- Il passaggio non ha cambiato l'aspetto: tre cursori disegnati con la alpha26 e con la beta01 danno
  0 pixel diversi, in chiaro e in scuro. Un parametro che `ValueSlider` non ha ancora (colori, la
  sorgente delle interazioni) si aggiunge lì, passato a `Slider` così com'è.

## 🎚️ L'editor completo, e il conto che esiste in una copia sola

- ⚠️⚠️ **Gli editor sono due, ed è la sua scelta**: il semplice mette l'immagine in posa e la
  ritaglia senza toccare un pixel, il completo la sviluppa. La scelta si fa la prima volta che si
  tocca 'Modifica' e si ricorda. Le specifiche dei moduli vivono in `docs/Editor.md`.
- ⚠️⚠️ **Il conto vive in AGSL e non anche in Kotlin**: due implementazioni della stessa matematica
  divergerebbero, e si salverebbe un'immagine diversa da quella vista, senza nessun errore.
  - ⚠️⚠️ **Sotto Android 13 l'editor completo non c'è** (sua istruzione: `RuntimeShader` nasce con
    la 13), e là resta l'editor semplice. La domanda la fa solo `advancedEditorAvailable()`, perché
    la fanno in tre (il selettore, il modello e le impostazioni).
  - ⚠️ **Un conto scritto una volta in Kotlin e consegnato allo shader non è una seconda copia**
    (le Curve come tabella, i pesi del 'Filtro BN', la Geometria che non passa dallo shader): la
    seconda copia è lo stesso conto scritto due volte, e qui il conto è uno coi suoi lettori.
- ⚠️⚠️ **Il salvataggio disegna davvero, fuori schermo, a tessere** (`AdjustRender.kt`,
  `ImageReader` più `HardwareRenderer`): un `RuntimeShader` non gira su una tela di memoria, e una
  texture ha un tetto diverso su ogni telefono.
  - ⚠️ **Le tessere si sovrappongono del bordo più largo fra i filtri mossi che leggono i vicini**
    (`AdjustRender.bleedFor`, una funzione perché il banco la chiami): senza, su ogni giunzione
    comparirebbe una riga. A moduli spenti il bordo vale zero.
  - ⚠️ **Prima di fidarsi prova un quadrato noto** (`AdjustRender.works`): se quel percorso non
    funziona il risultato è un'immagine nera, e scritta sul file prende il posto della fotografia.
- ⚠️⚠️ **Una stringa di programma non la guarda nessun compilatore**, come il `pathData` che
  `aapt2` non legge: Kotlin compila `LOOK_AGSL` come un testo qualunque. `ContoTest` la compila
  davvero, così un errore arriva con la riga e la colonna. Il caso: `out` è un qualificatore del
  linguaggio, e `half3 out = ...` rompeva il programma intero.
  - ⚠️⚠️ **Il `runCatching` di `lookShader` resta, ma trasforma un errore di sintassi in un 'il
    telefono non sa farlo'**: la diagnosi arriva rovesciata, e il testo che l'utente legge accusa
    il suo telefono.
  - ⚠️⚠️ **Il banco non esegue il programma**: Robolectric disegna col processore, e Android vieta
    `RuntimeShader` in software. Il banco dice che il programma è valido, non che il conto è
    giusto; i conti si verificano con un modello di sessione, scritto e buttato, perché riscriverli
    nel repository sarebbe la seconda copia.
  - ⚠️ **I numeri che il Kotlin ricopia dallo shader li presidia il banco leggendo `LOOK_AGSL`** (i
    tre di 'Auto', il kernel della foschia, il guadagno della grana): cambiarne uno da una parte
    sola non dà nessun errore.
- ⚠️⚠️ **'Senza perdita' è una proprietà del modello** (`Look.lossless`), ed è la sua clausola:
  finché c'è solo la posa il file si gira cambiando un tag EXIF, e appena entra un valore che
  riscrive i pixel (un colore, un ritaglio, la geometria) il file si riscrive. Filigrana e
  ridimensionamento non vivono in `Look`, e tolgono il senza perdita lo stesso.

## 🗑️ Lo svuotamento automatico del cestino, e le tre decisioni che lo governano

- ⚠️⚠️ **Le tre risposte sono sue, e si citano con la loro chiave**:
  - `d-cestino-quando`: **`file`**. Ogni file conta la propria età, come nei cestini di sistema.
  - `d-cestino-chiusa`: **`aperta`**. La pulizia gira solo con l'app in primo piano: nessuna
    operazione programmata di sistema e nessuna libreria in più.
  - `d-cestino-editor`: **`fuori`**. Le copie di sicurezza dell'editor non scadono, e si tolgono
    solo svuotando il cestino a mano: una rete che sparisce da sé non è una rete.
- ⚠️ **Un'eliminazione e una copia di sicurezza si distinguono con una quarta colonna
  dell'archivio** (`del` o `bak`), facoltativa per sempre: un archivio vecchio deve continuare a
  leggersi, o un aggiornamento toglierebbe ai file già nel cestino la provenienza. Il perché vive
  su `Bin.Record` e `Bin.expiring`.

## 📢 Il canale degli avvisi, e la superficie unica

- ⚠️⚠️ **L'app ha una voce sola** (sua risposta `casa` a `d-avvisi`): `Notices`, disegnato da
  `AppNotice` in `AivApp`, sopra la transizione fra schermate, perché una notizia deve sopravvivere
  alla schermata che l'ha prodotta. Prima lo stesso esito parlava con due voci, e quale dipendeva
  da un'impostazione.
- **Una riga per volta, la più nuova vince, nessuna coda**: quello che l'app ha da dire riguarda
  l'ultima cosa successa.
  - ⚠️ **Ogni riga ha il suo identificatore** e si toglie con `dismiss(id)`, non con `clear()`:
    senza, due messaggi uguali di fila sono uno solo, e un congedo cieco porta via quello arrivato
    nel frattempo. Quello che deve sparire con la riga (l'offerta di disfare, uno stato di
    schermata) lo dichiara `onGone`.
  - ⚠️ **Il testo arriva già risolto**, perché metà dei chiamanti compone una frase con un plurale
    o un nome di file.
  - **Le durate sono 2 secondi e 3** (`NOTICE_MS` e `NOTICE_LONG_MS`; la lunga è di 3 su sua
    richiesta).
- ⚠️⚠️ **La notifica sale sopra la scheda della selezione, e non il contrario**: muovere la scheda
  sposterebbe i comandi sotto il dito (sua proposta, scartata e dichiarata). Lo fa leggendo
  `FootStage`, un oggetto di processo, in composizione: letto nel layout (`Modifier.offset { }`) il
  valore non si rileggeva, per una causa non accertata.
  - ⚠️⚠️ **Accanto al FAB si stringe invece di salire** (sua risposta `stringe` a `d-avviso-forma`). Il lato del FAB si
    misura dal nodo e non da `fabSide`, o ci sarebbero due posti a decidere dov'è quel tasto; il
    rientro va bene qui perché la larghezza di un comando non cambia mentre lo si guarda. Il corpo
    del testo non è stato ridotto, perché quanto si perde si vede sul telefono e non sul banco.
    Quando lui ha chiesto il rientro ha aggiunto *potrebbe anche essere un'opzione*: oggi non lo è,
    e la porta resta aperta.
  - ⚠️⚠️ **Accanto a un tasto in basso l'avviso si centra sul suo centro, dalla `4.46`** (sua nota D
    sul giro della `4.45`): il tasto dichiara a `FootStage` anche dove è il suo centro, contato
    dalla radice della composizione, che è il riferimento su cui l'avviso si posa (sul banco la
    finestra e la radice differiscono di 8 dp). Sopra una scheda non vale, come dice la sua nota.
  - ⚠️ **Chi sale non si stringe** (sua risposta `sempre` a `d-avviso-forma-2`), ma si stringe appena entra nella fascia
    in altezza del FAB (`FootStage.tall`), con uno scatto e con una soglia un poco larga.
  - ⚠️ **A dichiarare l'ingombro del FAB è `TapHoldFab`** (un segnaposto, a menu aperto), così un
    FAB nuovo lo fa per costruzione. Nelle griglie la notifica è quindi più in alto, e si
    dichiara.
  - ⚠️ **Il FAB non si nasconde più dopo Indietro**: la ragione, cioè una notifica larga tutto lo
    schermo che copriva 'Annulla', è caduta quando la notifica ha preso a stringersi.
- ⚠️ **Un solo avviso di sistema resta**: `folder_why`, in `ViewerActivity`, perché là l'app va in
  secondo piano nello stesso istante e una notifica di casa non si vedrebbe. Chi ne aggiunge un
  altro dichiara la stessa ragione.
- ⚠️ **Le prove misurano il nodo della notifica, non la frase**, che dentro la superficie finisce
  prima (`AvvisiTest`); `CestinoAvvisiTest` guarda ogni fotogramma della discesa, perché a corsa
  finita la notifica era giusta anche col difetto.
- Una miniatura vecchia che sopravvive a un file riscritto è un altro caso, e vive in
  `docs/Files.md` § '🖼️ Le miniature che mentono dopo una riscrittura'.

## ⚙️ Dove va un'impostazione, e chi la deve trovare

- ⚠️⚠️ **Una voce va con quelle che rispondono alla sua stessa domanda, e la domanda è quella di
  chi apre il pannello** ('come faccio a...'), non quella del codice. Non fanno famiglia le voci
  che il codice legge nello stesso ramo, né quelle che agiscono sulla stessa schermata: quello è il
  posto in cui l'effetto si vede, non quello in cui la voce si cerca. Il precedente è una sua
  correzione, su una voce del menu del visualizzatore finita nella pagina dello zoom perché di zoom
  parla.
  - **La prova che una famiglia è una**: se per elencarne le voci serve una `e` fra due domande
    diverse, le famiglie sono due.
  - ⚠️ **Una collocazione che ha bisogno di giustificarsi è una famiglia che non esiste ancora**:
    quando si scrive una scusa accanto a una voce, la voce ha trovato il posto sbagliato.
- ⚠️⚠️ **Famiglia e sezione sono due cose, e la soglia si conta sulla famiglia.** La famiglia sono
  le voci di una domanda sola; la sezione è il titolo di gruppo della pagina piatta, e dice dove si
  è. Un titolo di sezione può nominare due famiglie vicine ('Modifica e backup'), perché non
  risponde a nessuna domanda.
  - **La soglia è sua, alla lettera**: fino a 2-3 voci correlate basta una sotto-sezione della
    pagina principale, di più si va con la sotto-pagina. Il trasloco si fa nello stesso giro in
    cui entra la voce che fa scattare la soglia, e nello stesso giro si copre la ricerca, o la
    famiglia esce dalla ricerca.
- ⚠️⚠️ **Sotto-pagina si diventa in cinque modi, e ognuno si dichiara quando la pagina nasce**:
  1. la voce è un **elenco** che cresce e include comandi propri riga per riga;
  2. le voci sono **delicate**, e il tocco in più è una protezione;
  3. la famiglia ha superato la **soglia**;
  4. è un **comando che ha bisogno di un paragrafo necessario**, cioè che spiega che cosa succede
     dopo il tocco (il caso delle miniature memorizzate): il tocco compra spazio, non protezione, e
     una spiegazione che il titolo già dà non rende il paragrafo necessario;
  5. la famiglia **ne contiene un'altra** (sua risposta `livelli` a `d-imp-strada`): con la porta la
     famiglia si vede come una cosa sola, e il riepilogo dice che cosa c'è dentro.
- ⚠️⚠️ **L'elenco di che cosa è delicato è chiuso, a due casi**: la voce può costare un file o
  togliere la rete che lo protegge, oppure cambia il metro con cui un'immagine si misura. Un elenco
  aperto si allarga da sé, e la pagina piatta si svuota una riga per volta. ⚠️ **Il rovescio**: una
  riga sola che non è né un elenco né delicata resta nella pagina piatta, perché in una sotto-pagina
  costerebbe un tocco senza guadagnare niente.
- ⚠️⚠️ **La profondità è due**: la navigazione è una pila, e Indietro risale un gradino per volta.
  Due e non di più, perché un terzo livello è più di quanto chi cerca una voce tenga a mente. La
  pila e la copertura della ricerca, che si annida da sé perché il corpo di una pagina si compone
  dentro il provider della ricerca, le misura `ImpostazioniTest`.
- ⚠️ **Una pagina che titola una famiglia non ripete il titolo in una sezione**: la stessa parola
  comparirebbe due volte a mezzo centimetro. Le porte senza sezione vivono sotto 'Aspetto', la
  sezione della domanda 'che cosa vedo'.
- ⚠️ **Una voce sola non prende un titolo**, e va nella sezione la cui domanda le è più vicina:
  sopra un titolo si legge come la prima riga di quello che segue. L'eccezione è sua ed è
  dichiarata nel codice: 'Funzionalità avanzate', dove il titolo è metà dell'avviso su una funzione
  che può fare danni.
- ⚠️ **Una voce fra due famiglie va dove si cerca, non dove si vede**. Se resta in bilico va nella
  famiglia più piccola, perché una famiglia grande non si accorge di una voce in più e in una
  piccola la voce si trova scorrendo; e fra i testi della ricerca riceve il nome della sezione in
  cui l'effetto si vede, che esiste già e non costa stringhe nuove, con la ragione scritta accanto
  alla riga. Non si mette in due famiglie: a farla trovare ci pensa la ricerca.
- ⚠️⚠️ **La ricerca deve trovare ogni voce, dovunque viva.** `LocalQuery` è fornito solo alla
  radice, quindi una voce dietro un tocco esce dalla ricerca se nessuno la copre.
  - **Pagina fatta di righe**: durante una ricerca la radice compone il corpo della pagina al posto
    della riga che la apre (`PageOfRows`); un blocco scritto a mano (un cursore, una casella, un
    tasto) si avvolge in `Searchable`, o resta in scena mentendo.
  - **Pagina che è un elenco con comandi per riga**: il corpo non si appiattisce, perché le frecce
    lavorano sull'ordine intero; la riga che la apre riceve le parole delle righe interne (`extra`
    di `PageRow`).
  - ⚠️ **Non ci si fida del riepilogo**: scritto a mano invecchia al primo trasloco. Il riepilogo
    di una pagina si compone dalle stringhe che la pagina usa dentro.
  - **Il collaudo di una voce nuova**: si cerca una parola del titolo e una della spiegazione, e
    deve comparire la voce, non la riga che apre la sua pagina.
  - ⚠️ **Una ricerca finisce quando porta a una pagina**, non a ogni tocco (sua nota): un
    interruttore toccato durante una ricerca resta dove lui lo guarda. La regola vive nella
    funzione che apre una pagina, e lo scorrimento torna in cima solo se la ricerca c'era.
- ⚠️⚠️ **Una voce può vivere in due posti: il pannello è la casa, il dialogo 'Opzioni di
  visualizzazione' la scorciatoia.** Una preferenza, una chiave, un valore di fabbrica; la casa ha
  titolo e spiegazione, la scorciatoia è nuda; chi la cambia la cambia in due posti. Una preferenza
  che vive solo nella scorciatoia è ammessa, e si dichiara nel KDoc del campo, come `folderView`.
- ⚠️ **La riga di un interruttore è un bersaglio solo** (`toggleable` con `Role.Switch` sulla riga,
  niente dentro l'interruttore), e il tocco lo mette il componente, non il chiamante: con due
  bersagli un lettore di schermo annuncia due voci per una scelta.
- ⚠️⚠️ **Le chiavi non si toccano quando una voce si sposta**: il posto nell'interfaccia e la chiave
  nell'archivio sono indipendenti, e chi aggiorna non perde le sue scelte. Una chiave nuova nasce
  quando la domanda cambia verso (il precedente è `sequence-reversed`).
- ⚠️⚠️ **Una voce nuova tocca cinque punti, e nessuno li controlla al build**: il campo col suo
  KDoc, la chiave, la lettura nel flusso, la scrittura in salvataggio, la riga nella schermata.
  **In più entra in `PREF_KEYS`** (in `Backup.kt`), con la sua area del file di impostazioni, che
  non cambia mai anche se la voce si sposta di pagina: senza, resta fuori da ogni esportazione, e
  la presidia il caso 2 di `BackupTest` (`docs/Files.md` § '💼 Esporta e importa, e il file che
  solo AIV sa leggere').
  - ⚠️ **Il KDoc dice perché quello è il valore di fabbrica**, e il valore di fabbrica non si
    sceglie per far vedere la funzione.
- ⚠️ **Il conto delle stringhe va nella proposta, prima di cominciare**: un testo nuovo si scrive a
  mano in tutte le lingue, e un plurale costa molto più di una stringa. Si riusa solo una stringa
  che dice esattamente quella cosa (una descrizione parlata non diventa il titolo di una sezione);
  un titolo che la prima riga della famiglia dice già non si scrive; e ogni testo nuovo del
  pannello entra con la proposta e si valida nel collaudo, nelle Etichette testuali del DF
  (`rules/Roccobot.md` § '✍️ I testi nuovi entrano con la proposta, e si validano nel collaudo').
- ⚠️ **Non decidono**: il gruppo in cui la voce era prima, la comodità del codice, la lunghezza
  della pagina piatta, lo sbilanciamento fra sezioni. E i conti di sezioni, famiglie e voci non si
  scrivono: si contano nel codice (`SettingsScreen.kt`).

## 🇺🇸 L'inglese dell'app è americano

- ⚠️⚠️ **L'inglese dell'app è americano** (sua risposta `americano` a `d-inglese-colore`): nel file
  inglese `color`, `center`, `Grayscale`. Da qui in poi l'inglese nuovo si scrive americano, o le
  due grafie tornano nella stessa pagina.
- ⚠️ **Le chiavi delle stringhe non si toccano** (`settings_colour`, `facts_colour_grey`), e le
  altre lingue non c'entrano.

## 🧪 Quando si scrive una prova, e quando no

- ⚠️⚠️ **Il paletto di AIV sulla regola del nucleo** (un difetto arrivato all'utente torna con la
  prova che lo avrebbe fermato): vale per i difetti che il banco può vedere, cioè di struttura; per
  gli altri la correzione va da sola, e la voce di collaudo lo dice. La prova si scrive **nella
  stessa versione** della correzione, non più avanti: il giro dopo introduce altre modifiche.
- ⚠️⚠️ **La prima cosa che una prova nuova misura è una correzione già uscita, ragionata e non
  misurata**, che si riconosce da 'adesso dovrebbe' nella sua voce di collaudo. Il precedente: una
  `SizeTransform` tolta per correggere l'arrivo del FAB, che misurata dopo non cambiava niente.
  Quando la misura smentisce la causa, la nota nel codice si riscrive con la causa vera o con la
  dichiarazione che non si conosce, e la voce nuova lo dice.
  - ⚠️ **Una condizione necessaria sembra una causa**: tolto l'impedimento (una `SurfaceView` che
    non si lascia traslare), il filmato non si muoveva lo stesso, perché nessuno lo traslava. Si
    chiede che cosa fa muovere la cosa, oltre a che cosa glielo impedisce.
- ⚠️⚠️ **Richiede una prova anche senza un difetto alle spalle una modifica alla gerarchia dei
  tocchi**: un nodo che copre lo schermo, un modificatore che misura e posa (la scatola gonfiata di
  `lowered`), una superficie che si apre sopra un'altra. In tutti e tre il codice può essere valido
  e non fare niente.
- ⚠️ **Non si scrive una prova** per quello che dipende dalla resa o dall'apparecchio, perché il
  banco non lo vede e una prova che finge di vederlo è peggio del niente; né per ricopiare
  un'implementazione: una prova verifica un comportamento, e il colore ricevuto conta più della
  costante ricopiata.
- ⚠️⚠️ **`@GraphicsMode(NATIVE)`, in una classe sua, serve per i pixel**
  (`captureToImage().toPixelMap()`) **e per ogni misura di testo**: con la grafica di serie un
  testo misura una frazione di quello che misura su un telefono, e una prova che dipende dalla
  larghezza di una parola passa con qualunque codice. Vede che cosa è coperto da che cosa, non come
  si percepisce.
  - ⚠️ **Quando la scena intera non vede il difetto, si guarda il meccanismo su una scena
    minima**: la prova del titolo sotto il gradiente, a schermata intera, restava verde col difetto
    rimesso, perché là il gradiente è già quasi finito.
- ⚠️⚠️ **Una scusa scritta accanto a un'asserzione saltata è il modo in cui una prova mente in
  verde**: `CambioSchermataTest` saltava i fotogrammi senza FAB dandoli per normali, ed erano il
  difetto.
- ⚠️ **Una prova di impaginazione sceglie la scena perché la forma giusta e quella sbagliata diano
  risultati diversi**: sulla scena di serie la fila dei gettoni del ridimensionamento veniva giusta
  anche col difetto, e serve `@Config(qualifiers = "w600dp-h900dp")`.
- ⚠️ **Il nome di una prova non contiene lettere accentate**: su una macchina con codifica di sistema
  stretta il rapporto non si genera, e il build cade proprio quando una prova cade.
- ⚠️⚠️ **Le trappole dell'iniezione dei gesti**, e ognuna ha dato una prova verde a vuoto o rossa
  col codice giusto:
  - il primo evento oltre la soglia se lo prende `settled`, quindi un trascinamento va in due
    colpi, uno che paga la soglia e uno che il gesto legge;
  - giù, movimento e su vanno in tre chiamate separate, o a `drag` resta un evento con delta zero;
  - `positionChange()` vale zero su un evento già consumato, quindi il delta si legge prima di
    consumare;
  - una `LazyColumn` senza `fillMaxSize` dentro una `Box` non genera eventi di scorrimento
    annidato;
  - uno `swipe` con la sua durata, a clock fermo, inietta i passi a un tempo che non avanza;
  - una prova sulla cornice del ritaglio tocca il palco prima del cambiamento, o il gesto nasce
    già aggiornato e il difetto non si vede.
- ⚠️⚠️ **Una finestra con un campo di testo, aperta sopra l'editor completo durante una prova con
  `@Config(qualifiers = "w411dp-h891dp")`, non lascia mai il banco in quiete** (misurato il
  2026-10-08 con la finestra delle parole del Testo e con quella di `Ridimensiona`): nessuno stato
  cambia, eppure Compose non si ferma, e ogni attesa cade dopo 60 secondi. Con lo schermo di serie
  del banco la stessa prova passa. Non è un difetto dell'app, e il perché non è accertato: la
  finestra nuda si ferma, il fuoco e il modificatore delle modali sono esclusi. Una prova che apre
  una di quelle finestre usa lo schermo di serie, e tocca in proporzione all'immagine misurata.
  - ⚠️ **Conta la larghezza, non l'altezza** (misurato il 2026-10-09 con la finestra delle parole
    del Pannello): con `w320dp-h891dp` la stessa prova passa, e il palco è abbastanza alto da
    misurarci i pixel; con `w411dp-h891dp` cade dopo 60 secondi.
- ⚠️ **Il banco arrotonda il corpo del testo al pixel intero** (misurato il 2026-10-09: a 20 e a
  20,75 px la stessa riga è larga 107 px, a 21 px 113): una prova che confronta una larghezza di
  testo con un conto ammette lo scarto di un pixel di corpo, e lo dichiara. Il telefono misura in
  frazioni di pixel.
- ⚠️ **Un oggetto di processo scritto dopo `setContent` non arriva alla composizione a clock
  fermo**: in una prova la riga (per esempio di `Notices`) si mette prima di montare la scena, col
  clock fermo (`autoAdvance = false`), o `waitForIdle` la fa scadere.
- ⚠️⚠️ **Una prova che fa leggere una cartella al modello monta `OmbraArchivio` o
  `ArchivioAperto`**, anche se non guarda niente della memoria: `openGrid` legge in sottofondo, la
  lettura chiede `Environment.isExternalStorageManager()`, che Robolectric non copre, e senza
  l'ombra fallisce sul suo filo. L'errore non cade nella prova che l'ha causato, ma nella prima
  classe successiva che usa il banco di Compose: sui banchi della `6.20` sono cadute `AggiornaTest`
  e `SelezioneTest` per colpa di `ScorciatoieTest`, che dalla `6.10` non montava l'ombra.
  - ⚠️ **Si riproduce con tre classi in quest'ordine**: una col banco di Compose, che accende il
    raccoglitore delle eccezioni, poi la colpevole, poi un'altra col banco di Compose (misurato con
    `CopertinaTest`, `ScorciatoieTest`, `SelezioneTest`). Gradle fa girare per prime le classi rosse
    della corsa precedente, quindi l'ordine giusto si ottiene dopo una corsa verde, con `--rerun`.
  - ⚠️ **La prima ipotesi, scartata**: una lettura che finisce dopo la prova, ad ambiente smontato.
    Chiudere il modello e aspettarne le letture non cambiava niente, perché la lettura falliva già
    durante la prova.
- ⚠️ **Il banco non sostituisce il giro con lui**: un banco verde dice che la struttura regge, non
  che la versione è buona.
- ⚠️ **Si lancia prima di aprire la PR**: le corse automatiche (§ '🧰 Gli strumenti che questo
  repo si porta dietro') sono la rete, non il controllo.

## 🔗 L'anteprima del link della paginetta

- **I meta tag di Open Graph vivono nel sorgente di `publish/index.html`** (sua richiesta), con
  indirizzi assoluti: chi li legge non esegue JavaScript, e un'anteprima si legge fuori dalla
  pagina. Di X c'è solo `twitter:card`, perché per il resto X ripiega su Open Graph; `og:site_name`
  è 'Roccobot' (scelta dichiarata: 'AIV' ripeterebbe il titolo).
- **L'immagine la compone `tools/og-image.py`** da icona, titolo, `og:url` e schermata chiara
  (`publish/schermate/`), e scrive `publish/anteprima.jpg` (1200 per 630, un JPEG sui 110 KB: con
  una schermata dentro un PNG supera il mezzo megabyte, e WhatsApp scarta le anteprime pesanti).
  È un file committato, e `pages.yml` la pubblica insieme al resto di `publish/` a ogni push che
  cambia quella cartella.
  - ⚠️ **Chi cambia una di quelle quattro cose rilancia lo strumento**: nessun controllo lo ricorda.
    Lo stesso vale per `tools/store-graphics.py`, che con gli stessi pezzi compone l'icona e
    l'immagine in evidenza della scheda di Google Play in `docs/store/` (`docs/Store.md`).
  - ⚠️ **I caratteri sono quelli veri** (Roboto da Google Fonts), o lo strumento si ferma invece di
    scrivere un'immagine col carattere di ripiego.
- **Verifica**: la pagina servita contiene `og:image`, e quell'indirizzo risponde 200 con
  `image/jpeg`. Un link nudo in un servizio può essere la sua cache.
- ⚠️⚠️ **La favicon della paginetta è il glifo nudo dell'app in `#43B59E`, incorporato nella
  pagina** (sua richiesta del giro della 1.46). Non è la favicon del documento di feedback (il
  blocco note col glifo, `assets/feedback-favicon.*`), che vale per il DF e le sue pagine di
  supporto: il 2026-10-02 erano state unificate, e lui l'ha segnalato il 2026-10-03. La presidia
  `PaginettaTest`, che confronta i tracciati con `ic_aiv_mark.xml`.

## 🏪 Le due varianti, e che cosa manca ad AIV Play

**Dalla `5.10` lo stesso codice produce due app** (sua scelta B1, 2026-10-10): **AIV GitHub**, quella
di sempre, che chiede l'accesso a tutti i file (`MANAGE_EXTERNAL_STORAGE`), e **AIV Play**, una
galleria per Google Play, che chiede solo l'accesso a immagini e video. Google concede l'accesso a
tutti i file soltanto a gestori di file, backup, antivirus e app di documenti, e una galleria non
rientra in nessuna delle quattro categorie. Le due varianti sono i due `productFlavors` di
`app/build.gradle.kts` (`github` e `play`); AIV Play ha il suffisso `.play` nell'identificativo,
quindi le due app si installano una accanto all'altra.

- ⚠️⚠️ **Ogni modifica si assegna a una variante, anche quando lui dice solo 'app' o 'AIV'**
  (sua regola, 2026-10-11; il testo generale vive in `Roccobot.md` § '🧬 Un prodotto con più
  varianti: ogni modifica si assegna a una variante'). Tutte e due: il nucleo, la grafica, le
  funzioni di base, le correzioni. Solo AIV GitHub: l'aggiornamento automatico, il menu Start, le
  funzioni che scrivono coi permessi pieni. Solo AIV Play: l'accesso alle foto e le funzioni sui
  file rifatte col MediaStore. La scelta si dichiara nel resoconto e nella voce del DF, e il
  comune si rilascia sempre, senza chiedere: le due varianti le pubblica l'agente, anche AIV Play
  quando sarà sullo Store.
  - **La verifica del 2026-10-11**, dalla `5.10` alla `5.71`: ogni release ha i due APK, le
    varianti non hanno codice proprio (solo i due manifest), e ogni lettura di `Store` corrisponde
    a una differenza voluta di questa sezione. Nessuna funzione comune mancava a una delle due.
- **Nel codice la differenza la legge un posto solo**: `Store.kt` (`Store.files`, `Store.allows`,
  `Store.hasBin`, `Store.renames`, `Store.views`, `Store.access`). Un secondo lettore di `BuildConfig.FILES` sarebbe una seconda
  risposta alla stessa domanda.
- **I permessi vivono nei manifest delle varianti** (`app/src/github/` e `app/src/play/`), e il
  manifest comune non ne dichiara nessuno sull'archivio.
- ⚠️⚠️ **Che cosa manca ad AIV Play, e perché**: le funzioni che scrivono sui file con `java.io.File`.
  Senza il permesso su tutti i file falliscono su ogni file che non ha creato l'app, e un comando
  che fallisce sempre è peggio di nessun comando. Sono nascosti: i tasti Copia e Sposta dei due
  riquadri dei file (e con loro dalla pagina che li riordina), il cestino di AIV (dalla `5.40` la
  pagina `Cestino` mostra quello di sistema, voce qui sotto), la vista 'Cartelle di sistema' con
  'Nuova cartella', la rinomina di una cartella, la scelta di un'app esterna per 'Modifica', la copia di sicurezza prima di
  sovrascrivere e 'Pulisci' degli SVG. La scaletta per riportarle, una per rilascio con MediaStore,
  vive nel brief.
- ⚠️⚠️ **'Elimina' è tornato in AIV Play dalla `5.20`, attraverso il MediaStore** (sue scelte A1 e
  B1, 2026-10-11; `MediaDelete.kt`):
  - **da Android 11 sposta nel cestino di sistema** (`createTrashRequest`), dove il sistema tiene
    i file circa 30 giorni (`MediaColumns.DATE_EXPIRES`). La conferma è la finestra di Android, che
    non si può saltare, e AIV non ne aggiunge una sua; col no non succede niente e non si dice
    niente. Non c'è 'Annulla', quindi l'esito lo dice la notifica (`FileKind.speaks`).
  - **su Android 9 e 10 elimina per sempre**, dopo la conferma di AIV: su Android 10 un file di
    un'altra app chiede il consenso di Android file per file (`RecoverableSecurityException`), su
    Android 9 serve il permesso di scrittura, dichiarato solo fino alla 28 nel manifest di `play`.
  - ⚠️ **La finestra di sistema la apre `SystemAsk`, in cima a `FileJobDialogs`**: il lavoro
    continua dopo che la finestra di AIV si è chiusa, e un launcher tolto dalla composizione non
    consegnerebbe la risposta. Se l'attività si ricrea mentre la finestra di sistema è aperta, la
    risposta si perde: è accettato e dichiarato.
  - Le presidia `EliminaPlayTest`.
- ⚠️⚠️ **La pagina `Cestino` di AIV Play c'è dalla `5.40`, e mostra il cestino di sistema** (residuo
  del pezzo 2, sua scelta A1; `MediaDelete.trashed`): immagini e video che il sistema tiene nel suo
  cestino, dal più recente, senza i PSD. Esiste **da Android 11**, dove il cestino di sistema c'è
  (`Store.hasBin`); su Android 9 e 10 'Elimina' elimina per sempre, quindi la pagina non c'è.
  - **Le tre azioni passano dalla finestra di Android, e AIV non ne aggiunge una sua**: `Ripristina`
    e `Ripristina tutto` con `createTrashRequest` spento, `Elimina` e `Svuota il cestino` con
    `createDeleteRequest` (`SystemWindow` in `FileOps.kt`). In AIV GitHub le conferme di AIV
    restano, perché là il cestino è dell'app.
  - ⚠️ **Il tocco lungo su `Elimina` fuori dal cestino elimina per sempre anche in AIV Play**, con
    la finestra di Android che lo dice: prima della `5.40` spostava comunque nel cestino.
  - **`Ripristina` è il tasto della rinomina**, come nel cestino di AIV GitHub. Dalla `5.50` quel
    tasto è nelle liste di fabbrica di AIV Play (voce qui sotto), quindi un ordine salvato che non
    lo nomina lo riceve al suo posto di fabbrica (`padOrderOf`); nella `5.40` era in testa.
  - **`Cronologia` non c'è**: il sistema non registra da dove viene un file del suo cestino.
  - ⚠️ Miniature, apertura e `Info` di un file nel cestino di sistema dipendono da quello che il
    sistema concede su quelle righe, e si dichiarano nel collaudo. Le presidia `CestinoPlayTest`.
- ⚠️⚠️ **`Rinomina` è tornato in AIV Play dalla `5.50`, da Android 11** (scelta dell'agente dopo il
  giro della `5.40`, nel piano di una funzione per rilascio; `MediaRename.kt`, `Store.renames`):
  il tasto nei due riquadri dei file e il tocco lungo sul nome nella finestra `Info`.
  - **È la stessa rinomina di AIV GitHub**: ordine alfabetico, cancelletti del modello, due fasi
    con un nome temporaneo (senza il punto davanti, perché un nome nascosto il MediaStore smette
    di elencarlo), un nome libero se quello voluto è di un file estraneo. Cambia solo chi scrive:
    il nome nuovo va nella riga del MediaStore (`DISPLAY_NAME`), e il sistema rinomina il file.
  - **La conferma è la finestra di Android** (`createWriteRequest`, una per tutti i file), come per
    `Elimina`; col no non succede niente e non si dice niente (`FileKind.speaks` tace quando non è
    passato e non è fallito niente).
  - ⚠️ **L'indirizzo resta lo stesso**, quindi la memoria dei nomi della griglia si dimentica a mano
    (`Names.forget`), o mostrerebbe il nome vecchio.
  - ⚠️ **L'estensione non cambia**: in AIV Play il tasto `Estensione` non c'è (`Store.fit`).
  - **Sotto Android 11 non c'è**: là `createWriteRequest` non esiste, e le strade di Android 10
    (un consenso per file) e di Android 9 (il permesso di scrittura) non sono misurate. La
    rinomina delle cartelle resta fuori. Le presidia `RinominaPlayTest`.
- ⚠️⚠️ **Dalla `5.30` AIV Play è più semplice, ed è sua scelta, voce per voce** (nota in Altro del
  giro della `5.20`, intestata *solo AIV Play*): niente `Menu basso` né menu Start (restano FAB e
  pillola, e un archivio che nomina uno dei due menu dà la pillola); niente `Lista` nella scheda
  della selezione; niente indicatore dell'ultimo elemento visto; dietro i pannelli solo l'ombra;
  nell'intestazione delle cartelle il gradiente sempre acceso e niente titolo graziato (`Info` e
  `Seleziona tutti` restano, accese di fabbrica); come colore delle cartelle il solo `Nome`; la
  sequenza sempre dalla più recente; l'estensione mai modificabile, né scaricando né rinominando.
  Le voci di quelle impostazioni non compaiono, nemmeno nel dialogo delle opzioni.
  - **I valori li fissa `Store.fit` in lettura** (`SettingsStore.read`), e non si riscrive niente:
    un archivio di una AIV Play di prima, o un file di impostazioni di AIV GitHub, non riporta
    quello che è stato tolto, e tornare indietro su una scelta non chiede una migrazione.
  - ⚠️ **Lettura dichiarata**: la sua riga sull'estensione (*modifica dell'estensione sia per
    download che per rinomina*) è letta come 'via la voce e il tasto', come le altre righe
    dell'elenco; la voce di collaudo lo dice.
  - ⚠️ **La spiegazione di `Elemento interattivo principale` ha un testo suo in AIV Play**
    (`settings_main_control_desc_play`): quella di AIV GitHub nomina i due menu.
  - Le presidia `SemplificazioniPlayTest`.
- ⚠️⚠️ **Il `Salva` degli editor di AIV Play scrive in Download**: il file nasce nella cache
  dell'app e poi passa a `MediaStore.Downloads` (`ImageEdit.outDir` e `delivered`), e l'originale
  resta com'è. ⚠️ **Su Android 9 non c'è questa strada** (`MediaStore.Downloads` nasce con Android
  10), quindi là il `Salva` di AIV Play risponde che non è riuscito.
- **L'accesso parziale di Android 14**: chi sceglie solo alcune foto vede quelle, e la schermata
  iniziale lo dice con una riga e il tasto 'Consenti tutte'. Il permesso si rilegge a ogni ritorno
  nell'app, perché si cambia dalle impostazioni di sistema.
  - ⚠️⚠️ **'Consenti tutte' apre la pagina di AIV nelle impostazioni di Android, dalla `5.11`**
    (sua nota sulla prova `5.10-03`: *un tocco non rimanda alla vera autorizzazione: torna alla
    selezione 'limitata'*): con l'accesso parziale già dato, richiedere il permesso ripropone la
    scelta delle foto, mentre nella pagina dell'app `Foto e video` si concede per intero
    (`Store.rememberAppPage`). La presidia `AccessoPlayTest`.
  - ⚠️ **La riga segue i bordi della griglia**: la frase comincia dove cominciano le copertine e il
    testo del tasto finisce dove finiscono (*Consenti tutte è allineato male*, stessa nota). Per
    questo il tasto non è un `TextButton`, che ha un margine interno suo.
- ⚠️ **Dopo il secondo rifiuto Android non mostra più la sua finestra**, e ogni richiesta risponde
  'no' subito: là AIV Play apre la propria pagina nelle impostazioni di Android, l'unico posto in
  cui il permesso si può ancora concedere (`Store.rememberAccessRequest`).
- **Le prove della sola variante play vivono in `app/src/testPlay/`** e i loro nomi finiscono in
  `PlayTest`: `check.yml` e `release.yml` lanciano `testPlayDebugUnitTest --tests '*PlayTest'`, e
  se nessuna classe corrisponde la corsa fallisce. Il resto del banco gira sulla variante github.
- ⚠️⚠️ **Il comando di fabbrica di AIV Play è la pillola a scomparsa, dalla `5.12`, e la scelta è
  sua e temporanea** (nota sul giro della `5.11`: *Fino a nuovo ordine, solo per AIV Play, il
  pulsante predefinito sarà la pillola a scomparsa*): AIV GitHub resta col menu Start. Il valore lo
  dà `Store.mainControl`, vale dove l'archivio tace, lo presidia `PulsantePlayTest`, e torna come
  prima solo quando lo dice lui. Dalla `5.30` AIV Play il menu Start non lo ha più (voce qui sopra).
- ⚠️⚠️ **AIV GitHub si aggiorna da sé dalla `5.60`, AIV Play no** (sue scelte C1 e D1, 2026-10-11;
  `Updater.kt`, `Store.updates`). All'apertura, al massimo una volta per giorno di calendario,
  chiede a GitHub l'ultima release e la propone se è più nuova; `Controlla ora`, in `Avanzate`,
  cerca subito e dice com'è andata. Con `Aggiorna` scarica `AIV-x.xx.apk` (mai quello di AIV Play),
  ne confronta il digest con quello che GitHub dichiara e la firma con quella dell'app installata,
  e solo allora lo passa all'installazione di Android, attraverso la cartella `updates/` del
  `FileProvider`. Se una delle due verifiche fallisce l'APK si cancella.
  - **Il permesso di installare pacchetti è solo nel manifest di `github`**: la politica di Google
    non lo ammette in un'app del Play Store. Android lo chiede la prima volta, nella pagina
    'Installa app sconosciute'.
  - ⚠️ **Senza digest nella release non si propone niente**: le verifiche scelte sono due, e una
    sola non basta. Le presidiano `UpdaterTest` e `AggiornamentiPlayTest`.
  - ⚠️⚠️ **Il lavoro di `Aggiorna` vive in `Updater` e non nella finestra, dalla `5.71`** (giro
    della `5.70`, `5.70-01` non approvato: *scaricato, ma poi non succede niente*). La finestra si
    chiude al tocco, e fino alla `5.70` il lavoro partiva dentro di lei: si annullava con lei dopo
    lo scaricamento, e l'installazione non si apriva, senza nessun avviso. Lo presidia
    `AggiornaTest`.
- ⚠️ **Il nome dell'app è lo stesso, ed è la sua scelta** (A2, 2026-10-10: *il nome va bene
  com'è, non le confondo*): sotto l'icona tutte e due dicono `AIV`, nelle pagine di sistema
  `Astonishing Image Viewer`. Non si cambia senza chiederglielo.
- ⚠️ **La pubblicazione su Google Play è un lavoro dell'utente** (account sviluppatore, scheda,
  prova chiusa con 12 tester per 14 giorni per gli account personali nuovi), ed è il pezzo 3 del
  brief.

## 🚀 Che cosa produce un rilascio

**Due cose, e vanno insieme**: il numero di **versione** e le voci nuove nel **documento di
feedback**. Il documento, però, si pubblica o aggiorna soltanto dopo che la GitHub Release della
stessa versione esiste. La regola completa vive in `rules/Roccobot.md`, § '🔁 Il giro del collaudo: rilascio,
documento, riscontro'.

- **Versione in SlimVer** (`x.xx`), con la fonte unica in `versionName` di `app/build.gradle.kts`:
  il workflow di rilascio ne ricava il tag, che conferma quel numero invece di essere un secondo
  posto in cui scriverlo.
- ⚠️ **Il `versionCode` va fatto salire a ogni versione pubblicata, e nessuno lo controlla**:
  Android rifiuta un aggiornamento che non lo fa salire, e non è legato al `versionName`.
- **Come si pubblica**: `release.yml` in `workflow_dispatch` con l'ingresso `publish` acceso taglia
  il tag dal `versionName`, costruisce gli APK firmati (e dalla `5.70` l'AAB di AIV Play) e crea la
  release. Senza `publish` costruisce e
  si ferma, ed è la **corsa a vuoto**.
- ⚠️⚠️ **Dalla `5.10` una release ha due APK**: `AIV-x.xx.apk` (la variante github) e
  `AIV-Play-x.xx.apk` (§ '🏪 Le due varianti, e che cosa manca ad AIV Play'). La paginetta prende
  il primo **per nome**, con un'espressione regolare che `PaginettaTest` confronta coi nomi scritti
  da `release.yml`: prima prendeva il primo `.apk` dell'elenco.
  - ⚠️ **Quella corsa non si chiama 'il banco di prova'**: quel nome è delle prove che aprono l'app
    finta (§ '🧰 Gli strumenti che questo repo si porta dietro'), e due cose con lo stesso nome
    prima o poi si scambiano.
- ⚠️⚠️ **Dalla `5.70` c'è anche `AIV-Play-x.xx.aab`**, il pacchetto da caricare su Google Play, che
  per un'app nuova non accetta un APK (scelte A1 e B1 dell'utente, 2026-10-11). È firmato con la
  stessa chiave degli APK, che su Google Play è solo la chiave di caricamento: Google firma l'app che
  consegna con una chiave sua (Play App Signing). `apksigner` non legge un AAB, quindi la firma la
  controlla `jarsigner -verify`. Nessun link e nessun aggiornamento lo prendono, perché scelgono per
  nome.
  - **Tutto quello che non è codice vive in `docs/Store.md`**: i testi della scheda in italiano e in
    inglese (scelta D1), le risposte ai moduli della Console e i passi dell'utente in ordine. I limiti
    di lunghezza di Google li controlla `tools/store-check.py`, dalla radice.
  - ⚠️⚠️ **L'informativa sulla privacy è `publish/privacy.html`** (italiano e inglese, scelta C1),
    servita da Pages, e vale per le due varianti. Le sue frasi sono affermazioni sul codice:
    nessuna libreria di statistiche, pubblicità o rapporti sugli errori, e la rete si apre solo per
    un indirizzo scelto dalla persona o, in AIV GitHub, per l'aggiornamento. **Chi aggiunge una
    libreria o una richiesta che manda qualcosa cambia la pagina e le risposte di `docs/Store.md`
    nella stessa versione.**
- ⚠️⚠️ **Il rilascio non scrive nel sito**: la paginetta la pubblica `pages.yml` da `publish/`,
  all'indirizzo <https://roccobot.github.io/AIV/> (sua scelta: Pages pubblicato da Actions).
  **L'APK non è sul sito**: il pulsante di download punta all'asset dell'ultima release
  (`browser_download_url`), e nome, peso e data li chiede all'API mentre la pagina si carica.
  Quindi un rilascio non richiede nessun deploy, e il sito si ripubblica solo quando cambia
  `publish/`.
- ⚠️⚠️ **Nessuna release senza un banco verde sullo stesso commit**: il cancello di `release.yml`
  è il banco di prova col controllo delle traduzioni, quindi una prova rossa o una lingua
  incompleta fermano il rilascio invece di produrre un APK da ritirare. I controlli a costo zero
  (il tag contro il `versionName`, un `publish` da un branch che non è quello principale) vengono
  prima, perché falliscono in un secondo.
  - ⚠️⚠️ **Dalla `4.45` il banco non si paga due volte** (sua richiesta, 2026-10-07, B1: un
    rilascio durava 8-9 minuti perché rifaceva il banco che il merge aveva già lanciato in
    `check.yml`). Tre lavori: `build` costruisce e firma subito, `gate` chiede a `check.yml` com'è
    andata sullo **stesso commit**, `publish` aspetta tutti e due. Verde vuol dire banco non
    rifatto; **rosso ferma il rilascio**, senza un secondo tentativo, che passando nasconderebbe il
    difetto; annullato, assente o non finito in 30 minuti, il banco gira nel cancello come prima.
  - ⚠️ **Conta solo la corsa di un push**: quella di una PR prova il commit di merge che GitHub
    costruisce per lei, che non è quello rilasciato.
- **Verifica di pubblicazione**: la release col suo tag e l'APK allegato (`get_release_by_tag`),
  perché è da lì che la paginetta prende il download. Il sito si controlla solo quando cambia
  `publish/`, e allora fa fede la corsa di `pages.yml`.
  - ⚠️ **Pages risponde 200 anche a un percorso che non esiste**, servendo la propria pagina di
    ripiego: una sonda sul sito guarda il `Content-Type`, non il solo codice.
  - ⚠️⚠️ **Il peso non distingue due build**: la prima libreria nativa di `lib/arm64-v8a/` è
    allineata a 16 KB, e quel riempimento assorbe ogni variazione del `classes.dex`, che è salvato
    senza compressione. A distinguere due build è il `digest` dell'asset, che non deve coincidere
    con quello della precedente; il peso prova solo che il file è arrivato intero.

## 🔐 La firma, e dove NON vive

La chiave di firma e le sue parole d'ordine vivono **solo** fra i secret GitHub di questo
repository (`AIV_KEYSTORE_FILE`, `AIV_KEYSTORE_PASSWORD`, `AIV_KEY_ALIAS`, `AIV_KEY_PASSWORD`), e
il job le scrive su disco per la durata di una sola esecuzione.

- ⚠️ **Senza quelle variabili il build di release non fallisce: l'APK risulta non firmato.** È
  voluto, perché chi non ha la chiave possa compilare e controllare il minificatore. Il rovescio è
  che un APK non firmato si riconosce solo guardando, e per questo il workflow ha un passo che
  chiede all'APK se è firmato.

## 🧰 Gli strumenti che questo repo si porta dietro

- **`tools/feedback-check.py`**: si lancia prima di pubblicare il documento di feedback (§ '🔗 Il
  documento vivo del progetto').
- **Il registro delle dipendenze, `docs/Dependencies.json`**: lo confronta con le ultime versioni
  pubblicate lo script unico del repo del sito, lanciato dalla radice
  (`python3 ../roccobot.github.io/.memo/scripts/check-dependencies.py`), prima di ogni release
  (`Roccobot.md` § '🔗 Le dipendenze si seguono'). Contiene le librerie del catalogo
  `gradle/libs.versions.toml`, Gradle, i font dell'editor e quelli del DF (Roboto e Noto Color
  Emoji, che `tools/feedback-emoji.py` porta all'ultima versione). La versione in uso si legge dove
  è scritta, l'ultima da Maven (Google e Central), da Gradle, da Google Fonts e dai file dei font in
  `google/fonts`. Compose segue la linea di prova col BOM alpha (`compose-bom-alpha`, dalla
  `6.24`), Material 3 compreso, perché la versione di prova è voluta. ⚠️ Una libreria
  nuova nel catalogo entra anche nel registro, o nessuno la segue. ⚠️ Lo script è uno solo dal
  2026-10-15 (scelta F1a di Rocco): fino a quel giorno ne vivevano due copie, una qui in `tools/` e
  una in Aomidori.
  - **E gira da sé ogni lunedì, dal 2026-10-15** (richiesta di Rocco):
    `.github/workflows/dependencies.yml` richiama il workflow riusabile del repo del sito, che apre
    la issue `Dipendenze da leggere` quando c'è una versione nuova e la chiude quando il registro la
    raggiunge; Dependabot (`.github/dependabot.yml`) apre una pull request per le azioni di GitHub.
    Una sessione che trova la issue aperta la prende come la lista di quello che va letto
    (`docs/Workflow.md` del repo del sito, § '🔗 Il controllo settimanale delle dipendenze').
- **`tools/i18n-check.py`**, dalla radice: confronta tutte le lingue con l'inglese (chiavi
  mancanti, segnaposto, categorie di plurale, caratteri vietati) e deve dire **28 lingue, 0
  problemi**: un numero più basso vuol dire che una cartella non è stata vista. Le varianti
  regionali (`values-b+es+419`, `values-pt-rPT`) contengono solo le differenze, e il verificatore lo
  sa.
  - ⚠️ **Nei testi dell'app i puntini sono il carattere unico `…`, e i tre punti sono vietati**
    (dalla `5.61`): è la convenzione di Android, e la deroga di `Roccobot.md` § 'Caratteri' (2026-10-11)
    la fa valere nei testi di interfaccia. Fino alla `5.60` il verificatore vietava il contrario. Nella
    prosa (DF, regole, commenti) restano i tre punti, e un'etichetta del DF li scrive così.
- **`tools/icon-check.py`**, dalla radice: legge ogni `res/drawable/*.xml`, col criterio di
  § '🖌️ Come entra un disegno'.
  - **Grammatica e vincoli girano sempre e bloccano**: un elemento o un attributo che Android non
    conosce, un tracciato che un parser stretto rifiuta, una tela non dichiarata.
  - ⚠️⚠️ **Il tokenizzatore dei tracciati è scritto a mano di proposito**: il parser di Android è
    indulgente, e una libreria misurerebbe la propria indulgenza e non la grammatica.
  - **Le misure di resa vogliono Chromium e avvisano** (l'inchiostro, il margine, la sagoma di
    Material più vicina, la dipendenza dal verso, il costo dell'unione); se Chromium manca lo
    dichiarano.
- **`tools/icon-round.py`**: dice quali spigoli esterni sono ancora vivi, con l'angolo e il raggio
  che spetta a ognuno. Avvisa e non blocca: uno spigolo vivo in un disegno suo può essere voluto.
  - ⚠️⚠️ **Oggi risponde zero, con `ic_tian` dichiarato escluso**: un numero diverso da zero
    riguarda un disegno appena entrato.
  - **Come conta, e ogni punto ha tolto un falso positivo o una lacuna**: guarda le tangenti e non
    le corde (un raccordo già fatto è un arco, e la sua corda svolta di metà arco); le curve smooth
    (`s`, `t`) non fanno spigolo; un giro che torna a un centesimo dal punto di partenza è chiuso
    (i tracciati sono scritti a due decimali); il vertice della `M` di un giro chiuso si conta, e
    la guardia che salta i lati troppo corti legge il lato che arriva al penultimo punto; contorno
    o buco lo dice la profondità di contenimento (pari contorno, dispari buco), misurata dal mezzo
    del lato più lungo scostato di un millesimo verso l'interno, perché un vertice può cadere sul
    bordo di un vicino. Il verso del giro non basta: con `evenOdd` non significa niente.
  - ⚠️ **I conti fatti con criteri diversi non si confrontano.**
- ⚠️⚠️ **Il banco di prova è `./gradlew :app:testGithubDebugUnitTest`**, più le prove della
  variante play (`:app:testPlayDebugUnitTest --tests '*PlayTest'`, dalla `5.10`): apre l'app finta con
  Robolectric, senza telefono né emulatore, e la tocca; le prove vivono in `app/src/test/`, le
  librerie sono di sola prova, e nell'APK non entra niente. È nato col blocco totale della `1.70`,
  un'app che non rispondeva ai tocchi e compilava senza una parola. Prende i difetti di struttura,
  non quelli di resa né di apparecchio: un banco verde non vuol dire che l'app è a posto.
  - ⚠️ **La piattaforma delle prove si dichiara in `app/src/test/resources/robolectric.properties`**,
    invece del `targetSdk`, che cambia con la politica di Google Play.
    ⚠️⚠️ **E quel numero decide la versione di Java**: la 36 vuole Java 21 (letto nel bytecode di
    `DefaultSdkProvider`), e su Java 17 non parte una prova, con un errore che sembra una catena di
    strumenti rotta. Il CI monta 21, e chi tocca uno dei due numeri guarda l'altro. Una prova che
    deve girare su versioni diverse di Android le dichiara nel suo `@Config` (`ConvertiTest`, su
    Android 9, 10 e 11). Robolectric 4.17 (dalla `6.21`) conosce anche la 37, sempre con Java 21:
    il passaggio è una scelta a sé, il lavoro `Diciassette` del brief.
    ⚠️⚠️ **Nella `6.24` la 37 non ha retto, per difetti del simulatore** (sua scelta A1 del
    2026-10-16: la `6.24` esce sulla 36). Sulla 37 l'onda del tocco di Material resta ferma a metà
    sopra l'elemento toccato, quindi una prova che legge i pixel di un pulsante appena toccato li
    trova più scuri del 6-7%; e nella prova della luminosità di `DisegnoTest` la cattura del palco
    non si aggiorna, anche se il valore arriva all'elemento. Banco intero: quattro corse con 3-5
    prove rosse sulla 37, due verdi sulla 36 con lo stesso codice.
    ⚠️⚠️ **E la 37 vuole Compose 1.13**: la 1.12 si porta dietro Espresso 3.5.0, che al primo tocco
    cerca per riflessione un metodo di Android (`InputManager.getInstance()`) che sulla 37 non
    esiste più, e al primo tentativo sono fallite 521 prove su 766. La 1.13, in uso dalla `6.24`, si
    porta dietro Espresso 3.7.0, che lo chiede al `Context`.
    ⚠️⚠️ **E da Robolectric 4.17 la JVM delle prove apre `java.base/jdk.internal.access`**
    (`testOptions` in `app/build.gradle.kts`): senza, 665 prove su 763 falliscono con `Failed to
    interact with raw FileDescriptor internals`, perché preparando Android 16 Robolectric scrive un
    campo interno di `FileDescriptor`. La guida di Robolectric elenca nove aperture per Java 17 e
    successivi: entra solo quella che una prova chiede.
  - ⚠️ **Le prove montano `AivTheme` e le schermate vere**: il velo e il cancello dei menu vivono là
    dentro. Una prova che montava una copia accanto, al posto della schermata vera, è passata col
    difetto dentro.
  - ⚠️⚠️ **`OmbraArchivio` copre `Environment.isExternalStorageManager()`**, che Robolectric non
    copre, **e prende il posto dell'ombra di Robolectric per `Environment`**: chi la dichiara perde
    le altre implementazioni, e `getExternalFilesDir` va in errore dentro `Environment`. Il `@Config` di
    un metodo si somma a quello della classe, quindi una prova che la vuole e una che no vivono in
    due classi.
  - **Gira da sé in due posti**: in `check.yml` a ogni push su `main` e a ogni PR, tranne i push
    che cambiano soltanto file `.md` (il banco non li legge, e l'Action `core-sync` vi committa
    `AGENTS.md`); ed è il cancello di `release.yml`, che si fida della corsa di `check.yml` sullo
    stesso commit (§ '🚀 Che cosa produce un rilascio'). Si lancia comunque a mano prima della PR.
  - ⚠️⚠️ **Le cache di Actions si potano da sole dalla `4.45`** (B1): il lavoro `caches` di
    `check.yml`, a ogni push su `main`, toglie per prime le meno usate di recente finché il totale
    è sotto i 6 GB, e scrive l'occupazione nel riepilogo della corsa. Prima lo svuotamento lo
    faceva l'utente dal terminale (`gh cache delete --all -R Roccobot/AIV`), due volte: al
    rilascio della `4.36`, dopo un'ora e sei corse cadute sulle prove del velo d'aiuto con le
    cache a 9,7 GB su 10, e a quello della `4.43`. ⚠️ La sessione non legge l'occupazione da sé: il
    suo proxy rifiuta le chiamate alle cache, e il riepilogo è il posto in cui guardare.
  - ⚠️⚠️ **Le cache non spiegano la prova del velo d'aiuto**: il 2026-10-07 la corsa della PR della
    `4.45` è caduta sulle tre prove che aspettano il velo, con le cache appena svuotate, e il
    rilascio dello stesso codice è passato. **Quando cade, cadono tutte quelle della corsa**:
    qualcosa che una classe precedente lascia nel processo spegne il velo fino alla fine. In
    locale le 615 prove passano nell'ordine solito e in quello rovesciato, quindi sul server il
    banco scrive l'ordine in cui partono le prove (`build.gradle.kts`, solo con `CI`), e alla
    caduta successiva quell'ordine si rifà in locale. Il messaggio di `aspettaIlVelo` dice
    permesso, flusso e archivio al momento della caduta.
    - ✅ **La causa è trovata e corretta nella `4.98`** (voce B2, dopo che aveva fermato i rilasci
      della `4.96` e della `4.97`): la vecchia `createComposeRule` fa girare gli effetti di una
      composizione su un dispatcher di prova non confinato, quindi il valore dell'archivio, quando
      arriva da un thread di sfondo, scrive lo stato fuori dal thread principale e la schermata non
      si ridisegna. Le due classi che aspettano il velo usano `junit4.v2.createComposeRule`. Misure,
      tentativo scartato e perché l'app non c'entra vivono nel KDoc di `aspettaIlVelo`.
    - ⚠️⚠️ **Dalla `6.24` tutte le classi usano la regola nuova** (`junit4.v2.createComposeRule` e
      `junit4.v2.createAndroidComposeRule`), e una prova nuova si scrive solo con quella: la
      vecchia, che Compose dichiara deprecata, non compare più nel banco. Fino alla `6.23` le altre
      classi usavano la vecchia, e il passaggio era il lavoro `Travaso` del brief.
    - ⚠️ **Il 2026-10-10 la regola vecchia ha fermato il rilascio della `5.10`**: sul server
      `VideoScorreTest` è caduta a metà di un trascinamento con 'performMeasureAndLayout called
      during measure layout', cioè una seconda misura partita dentro la prima. La classe è passata
      alla regola nuova; la causa è probabile e non riprodotta (in locale 0 cadute su 5).
  - ⚠️⚠️ **Un banco verde può non aver girato**: con `org.gradle.caching` acceso il compito delle
    prove è nella cache di Gradle, e su un codice già provato il registro dice
    `testDebugUnitTest FROM-CACHE` e riprende l'esito della corsa di prima (misurato il
    2026-10-07 sul rilascio a vuoto di B1: banco in 16 secondi). Una prova caduta non entra in
    cache, quindi una corsa che la rivede la riesegue; ma un verde ripreso non dice niente di una
    prova che cade a caso, e l'ordine delle prove compare nel registro solo quando girano davvero.
    - ⚠️⚠️ **Un file che una prova legge fuori dal modulo va dichiarato come ingresso del compito**
      (`inputs.file` in `build.gradle.kts`, dalla `5.10`): `PaginettaTest` legge
      `publish/index.html` e `release.yml`, e finché Gradle non lo sapeva la prova restava verde col
      difetto rimesso, perché l'esito arrivava dalla cache.
  - ⚠️ **La piattaforma finta pesa 213 MB e si scarica al primo giro**: in CI la tiene una cache
    sui due file che la decidono (il catalogo delle versioni e `robolectric.properties`), perché
    `setup-gradle` non copre `~/.m2`. In una sessione il primo giro paga il download.
  - ⚠️⚠️ **In un container senza `LANG` il banco si lancia con `LC_ALL=C.UTF-8`** (dalla `6.21`, con
    Gradle 9.8.1): il report di Gradle scrive un file per prova col nome della prova, e senza una
    codifica UTF-8 Java scrive i nomi di file in ASCII, quindi il compito delle prove fallisce su
    `Malformed input or input contains unmappable characters` mentre scrive il report. La codifica
    la fissa il demone al suo avvio: prima lo si ferma (`./gradlew --stop`).
  - ⚠️ **Maven Central limita le richieste**, e al primo giro dopo un aggiornamento delle dipendenze
    risponde `429` o, peggio, come se il file mancasse (`Could not find`): misurato il 2026-10-15 su
    `kotlin-compiler-embeddable` 2.4.21, Robolectric 4.17 e `test-parameter-injector` 1.22, tutti
    pubblicati. Ogni giro conserva quello che ha scaricato, quindi si rilancia dopo un minuto e
    mezzo: alla `6.21` sono serviti otto giri.
- ⚠️ **`refcheck.py` non vive qui** ma in `roccobot.github.io/.memo/scripts/`: in una sessione
  senza l'hub i controlli sui caratteri e sui rimandi non girano, e prima di un commit va detto.
  `AGENTS.md`, `Rules.md` e il `CLAUDE.md` di questo repo sono coperti come file di regole, e i loro
  titoli entrano nell'indice dei rimandi; senza AIV montato, i rimandi a lui restano non
  verificabili e non bloccano. Il sintomo rovesciato (un rimando corretto dato per rotto quando il
  file non è coperto) è scritto in `roccobot.github.io/Rules.md`, nella voce sui controlli
  pre-commit.

## 🌿 Branch

Il branch principale è **`main`**. Le sessioni vincolate a un branch `claude/*` aprono la PR e la
mergiano subito (squash), come da regola universale sul go-live.
