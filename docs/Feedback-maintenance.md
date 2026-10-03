# Manutenzione del documento di feedback AIV

Questa guida vale per qualsiasi agente e piattaforma di sviluppo. Il documento condiviso
è [Feedback AIV](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
Il proprietario ha confermato il funzionamento completo il 1 ottobre 2026.
Gli agenti mantengono lo stesso codice e indirizzo; recuperano il giro completo soltanto
dopo il via esplicito del proprietario in chat.
Non occorre una sessione Claude autenticata per aggiornare questo documento.

## Prima di prendere in carico il lavoro

1. Leggi `AGENTS.md`, `Rules.md` e il brief unico `Roccobot/tools/.memo/LATEST.md`.
   Il brief contiene richieste ancora aperte, priorità e riscontri da elaborare.
2. Allinea `main` con `git fetch origin main` e confronta i ref prima di modificare file.
   Conserva le modifiche già presenti nella copia di lavoro.
3. Leggi questa guida e, se tocchi il servizio, [la guida cloud](../cloud/feedback/README.md).
4. Distingui la manutenzione del documento dal collaudo dell'app: approvare la pagina
   non approva automaticamente le funzioni Android. Elabora le risposte solo dopo la
   consegna del giro completo da parte del proprietario.

## Fonti e responsabilità

| File | Responsabilità |
|---|---|
| [Feedback.md](Feedback.md) | Prove, identificatori, passi e risultati attesi |
| [feedback-build.py](../tools/feedback-build.py) | Generatore, struttura della pagina e dati incorporati |
| [feedback.html](../publish/feedback.html) | Documento generato da pubblicare, mai unica fonte di una modifica |
| [feedback-zip.js](../publish/feedback-zip.js) | Scrittura e lettura dello ZIP di `Esporta` e `Importa`, senza librerie |
| [feedback-data.js](../publish/feedback-data.js) | Dati della pagina, bozza, validazione JSON, riepilogo, coda dei salvataggi |
| [feedback-ui.js](../publish/feedback-ui.js) | Tema, messaggi, contatori, riquadri e allegati, Altro, i sei comandi di consegna |
| [feedback-nav.js](../publish/feedback-nav.js) | Striscia, spostamento fra i riquadri, pulsanti flottanti e pressione lunga |
| [feedback-start.js](../publish/feedback-start.js) | Avvio: caricamento della bozza e allineamento con gli altri dispositivi |
| [feedback-format.js](../publish/feedback-format.js) | Editor visivo, Markdown, icone e scorciatoie |
| [feedback.css](../publish/feedback.css) | Aspetto, esiti, evidenze e adattamento dello schermo |
| [feedback-cloud.js](../publish/feedback-cloud.js) | Bozza remota, originali, versioni e accesso lato pagina |
| [feedback-cloud-config.js](../publish/feedback-cloud-config.js) | Configurazione neutra per la pagina aperta fuori dal Worker (file locale, verifiche), sostituita dal Worker sul cloud |
| [worker.mjs](../cloud/feedback/worker.mjs) | Pagina, accesso GitHub, API private, controllo delle scritture e `Content-Security-Policy` della pagina |
| [supabase-store.mjs](../cloud/feedback/supabase-store.mjs) | Database e Storage Supabase, soltanto lato server |
| [supabase-setup.sql](../cloud/feedback/supabase-setup.sql) | Tabelle, revisioni, privilegi e bucket privato |
| [feedback-cloud.yml](../.github/workflows/feedback-cloud.yml) | Verifiche e distribuzione del servizio cloud |
| [pages.yml](../.github/workflows/pages.yml) | Paginetta di download su GitHub Pages; il DF ne è escluso |

- I quattro script della pagina (`feedback-data.js`, `feedback-ui.js`, `feedback-nav.js`,
  `feedback-start.js`) sono script classici che condividono le variabili globali, caricati
  in quest'ordine; durante il caricamento nessuno chiama le funzioni di un file successivo,
  e l'avvio vive nell'ultimo. Ognuno lo dichiara nell'intestazione. Moduli ES scartati: lo
  stato condiviso andrebbe riscritto, e non si caricano da una pagina aperta come file.
- Le versioni dei file nei link (`?v=`) le calcola `feedback-build.py` dal contenuto di
  ogni file: dopo una modifica a CSS o JS si rigenera la pagina, e `--check` lo ricorda.

La versione delle prove si ricava da `versionName` in `app/build.gradle.kts`.
Una modifica al documento non produce una nuova versione Android.
Le proposte tablet e impostazioni rimangono nei rispettivi file e pagine, collegati dal
documento. Il vecchio artefatto Claude è un riferimento storico per le sue voci ancora
da recuperare: non va cancellato né considerato approvato automaticamente.

## Convenzioni di contenuto e interfaccia

- Nome ufficiale: **documento di feedback** (DF). Titolo: **Feedback AIV**.
  Intestazione: `AIV · giro della X.XX`, terminata alla versione, senza nome dell'agente.
- ⚠️⚠️ **Il corpo del DF contiene solo feedback sull'app** (norma di Rocco, 2026-10-03).
  L'interfaccia del DF (striscia, Altro, overlay, shell, formattazione, Salva-only mobile,
  casella versione, PP, ...) si discute e si concorda **in chat**, e vive in codice, in questa
  guida e nel brief. **Non** compare nel documento: né come prova di collaudo, né come riga
  di `Aggiornamenti recenti`.
- ⚠️⚠️ **L'introduzione tiene due righe sullo stato del giro e la voce `Scarica AIV`**, e
  nient'altro. Un link a un documento esterno (per esempio una proposta di interfaccia) va
  in testa, subito dopo `Scarica AIV`, **finché serve**: chiusa la decisione che lo riguarda,
  si toglie.
- ⚠️⚠️ **`Aggiornamenti recenti` è un riepilogo degli ultimi due giri circa, non un changelog**:
  (si chiamava `Riscontri conclusi` fino al 2026-10-03, rinominata da Rocco)
  le righe dei giri più vecchi si tolgono quando entra un giro nuovo. La storia completa vive
  in git. La sezione della pagina la genera `tools/feedback-build.py` dalla tabella
  `Aggiornamenti recenti` di `Feedback.md` (una frase per giro, il più recente prima): si
  aggiorna la tabella, mai il modello `tools/feedback-page.html.in`.
- Sigle: **DF** = documento di feedback; **PP** = **Prossimi passi**.
- Ogni DF termina, prima della coda, con una sezione intitolata esattamente **Prossimi passi** (PP): un riepilogo breve e schematico di differiti, accorpati per dopo, voci da decidere e altre voci già nel brief. È una sorta di **mini-brief** per Rocco.
- Dopo aver letto un giro, la release successiva non deve comprendere tutto il backlog. L'agente sceglie liberamente il piano, ma lo comunica proattivamente a Rocco in chat, con ciò che entra e ciò che resta, senza aspettare che Rocco lo ricavi dal DF.
  Gli eventuali riferimenti all'autore nei testi visibili usano `l'agente`.
- Scrivi testi italiani e prove eseguibili: comandi da raggiungere, azione e risultato
  atteso. Mantieni le prove aperte fra release; archivia soltanto quelle concluse dal
  giro consegnato. Non sostituire riscontri manuali con prove automatiche.
- Conserva gli identificatori esistenti, come `3.13-01`.
  Per una prova nuova usa un identificatore nuovo: rinominare o riutilizzare una chiave
  può associare una risposta a una prova diversa.
- Ogni verifica mostra `Verifica X/Y`, con X in grassetto; il totale deriva dalle prove.
- Esiti: `Tutto OK`, `Accettabile`, `Non approvato`; nessuna scelta significa `Non provato`.
  Un secondo clic toglie l'esito. Verde, ambra e rosso seguono la scelta; commenti o allegati
  senza esito hanno evidenza neutra. Una risposta presente non equivale ad approvazione.
- Specifiche separate per telefono e tablet: modello e Android. I commenti indicano
  su quale dispositivo si è verificato un problema quando necessario.
- Editor con formattazione visibile, senza anteprima duplicata; icone grassetto, corsivo,
  link con nomi accessibili e suggerimenti. Cmd/Ctrl+B, I, K; Cmd/Ctrl+S salva.
  Il testo normale non è grassetto; incolli di testo semplice, nessun HTML interpretato.
  Su viewport stretti (≤ 720px) i controlli stanno nell'angolo in basso a destra,
  dentro la cornice del testo, così la barra di selezione di sistema (Taglia/Copia/Incolla),
  che compare sopra il cursore, non li copre. Su desktop restano in riga sopra il campo.
- Navigazione flottante: **una pillola verticale** color accento, ancorata in basso a destra,
  con dall'alto primo non compilato, riquadro precedente, successivo e salvataggio col dischetto
  (richiesta dell'utente, 2026-10-03). Mostra solo i tasti che in quel momento possono agire,
  si allunga e si accorcia con loro, e con un tasto solo è un tondo. Il fondo è opaco al 40%
  (sua scelta, con la sfocatura), le icone no, e sotto il fondo la pagina è sfocata come un
  vetro satinato (`backdrop-filter`, sua richiesta); dove il browser non la supporta resta il
  solo fondo. Commento o allegato
  contano come compilazione.
- ⚠️ **Il DF non ha un footer dal 2026-10-03** (*non serve a nulla*): lo spazio sotto l'ultima
  card, che la tiene libera dalla pillola, è del contenuto stesso (`main`).
- **Altro**: su desktop è una colonna laterale reale (preferibilmente a sinistra), sticky,
  sempre pronta, con toolbar di formattazione e allegati (`+` e trascinamento); la colonna
  è circa il 50% più larga del primo taglio laterale. Su desktop la striscia conteggi,
  la card Accedi/Salvataggio cloud e i paragrafi intro condividono la stessa larghezza
  totale di (colonne prove + Altro), senza fascia a tutta viewport. Su mobile resta in
  fondo **prima** del PP; la striscia sticky mostra solo i chip semaforo centrati (niente
  hamburger né tasto Altro). Pressione prolungata sul FAB flottante ⇥ **o** su Salva
  (dischetto) apre Altro a pannello overlay (chiudi con ×), sullo stesso campo `notes` del
  riquadro in fondo e con lo stesso salvataggio cloud; il tocco breve resta l'azione del FAB.
  Il titolo `Altro` è in grigio (`--muted`), non nel colore del testo. Non usare un riquadro `position: fixed` staccato dal flusso come unica
  sede di Altro.
- ⚠️ **Etichette testuali**: quando una feature introduce o aggiorna copy italiano di
  interfaccia (paragrafi, pulsanti, toast, voci, ...), l'agente può redigere la proposta e
  far uscire la versione; il DF deve elencare **ogni** nuova stringa ITA una per una in
  una sezione intitolata esattamente **Etichette testuali**, in basso **prima** delle
  sezioni conclusive/archivio (`Aggiornamenti recenti`) e del PP. Ogni sotto-card mostra il
  testo ITA proposto per intero e un campo libero: ciò che l'utente scrive sostituisce la
  proposta al prossimo rilascio utile; campo vuoto = approvato. Non è una sezione di prove
  (niente esiti, fuori dai contatori). Assente o vuota → sezione nascosta.
  ⚠️⚠️ **Dopo la prima conferma un'etichetta esce dal DF**: è risolta (campo vuoto, o testo
  dell'utente applicato), oppure, se servono chiarimenti o modifiche, torna nel brief. Non
  resta in pagina per un secondo giro.
- ⚠️ **Sotto il campo di Altro, due righe** (istruzione dell'utente, 2026-10-03), uguali nella
  colonna della pagina e nel pannello mobile:
  1. la riga divisa **esattamente a metà**: a sinistra il `+` tratteggiato degli allegati, a
     destra i quattro tasti di formattazione, che si dividono la metà in parti uguali;
  2. i sei comandi di consegna come **icone con tooltip**, ognuno largo 1/6 della riga, in
     quest'ordine: `Azzera tutto`, `Copia il riepilogo`, `Esporta`, `Importa`,
     `Salva`, `Invia`. `Copia il riepilogo` copia negli appunti e basta: il testo non compare
     in nessun campo.
  ⚠️ **L'overlay `Consegna e copie` non c'è più dal 2026-10-03**, e con lui la pressione lunga
  su Salva che lo apriva su desktop. I messaggi dei comandi compaiono nella striscia in alto,
  sotto lo stato del salvataggio (`#action-message`). Gli identificativi `#save`, `#copy`...
  sono sui pulsanti della pagina; la copia nel pannello mobile li riconosce da `data-command`.
- La **B** del grassetto è un tracciato SVG, non testo: ricavata dalla B di Arial Bold
  (Liberation Sans Bold, con le stesse misure), alla stessa dimensione e allo stesso tratto
  della lettera di prima, perché la sua resa non dipenda dai caratteri installati.
- Da scollegati, l'accesso è una pillola sola `Accedi con GitHub`, ancorata a destra della
  riga del titolo.
- La versione AIV del giro nel DF è testo fisso (`spec.version`); la conferma avviene solo
  con la casella 'Sì, ho installato questa versione' (`installed` = versione del giro o vuoto).
- Allegati mediante selettore e trascinamento nelle verifiche e in Altro:
  PNG, JPG, WebP, GIF, SVG e ZIP. Originali interi, nomi conservati, ZIP scaricabili.
  Limiti attuali: 8 MB per file, 20 MB totali, 30 allegati per riquadro.
- Collegamenti esterni in nuova scheda con `noopener noreferrer`. Il DF e le sue pagine di
  supporto usano la favicon `assets/feedback-favicon.svg` (il blocco note col glifo), colore
  `#43B59E`, e alternativa PNG. ⚠️ La paginetta di download `index.html` **non** la usa: ha il
  glifo nudo dell'app, e non cambia (segnalazione dell'utente, 2026-10-03).
- `Azzera tutto` richiede conferma e riguarda la bozza su tutti i dispositivi.
  Un comando disabilitato non indica un caricamento: cursore normale e aspetto coerente,
  anche per il selettore di `Importa`.

## Dati, privacy e compatibilità

Il JSON usa `schema: 1`, `project: AIV`. La bozza contiene:

| Campo | Significato |
|---|---|
| `version`, `installed` | Versione del documento; `installed` vale la stessa stringa solo se Rocco ha spuntato 'Sì, ho installato questa versione', altrimenti stringa vuota (non è più un campo libero) |
| `device` | Specifiche del telefono; chiave storica conservata |
| `tablet` | Specifiche del tablet; assente nei vecchi JSON, ripristinata come testo vuoto |
| `entries` | Risposte per ID: `status`, `comment`, `images` |
| `decisions` | Decisioni per ID: `choice`, `comment`. Dal 2026-10-03 la pagina non ne pone più (le domande si fanno in chat), ma la chiave resta: il Worker la richiede, e una bozza vecchia la conserva intatta |
| `labels` | Etichette testuali per ID: `revision` (campo libero; assente nei JSON vecchi → `{}`) |
| `notes`, `extra.images` | Osservazioni libere e relativi allegati |
| `updated`, `completed` | Data del salvataggio e della preparazione del giro |

`images` è il nome storico anche per gli ZIP: non cambiarlo senza migrazione.
**Al cambio di giro la bozza si alleggerisce** (dal 2026-10-03, scelta dell'utente): restano
telefono, tablet, le risposte alle prove ancora in pagina, le etichette ancora aperte e le
decisioni; escono `Altro` coi suoi allegati, la conferma della versione installata, e le risposte
alle prove chiuse coi loro allegati. Prima si accumulavano: un JSON del 3.42 conteneva 76 risposte
dal giro 3.13 in poi, e importato sembrava vuoto. L'importazione dice quante risposte riguardano
prove non più in pagina.
**Gli allegati sono file veri**, mai testo: nella pagina e nella bozza del browser un
allegato è `{name, type, size, blob}`, con i byte originali (dal 2026-10-03; prima erano testo
base64, un terzo più pesante e tenuto per intero nella memoria della pagina).

**`Esporta` scrive uno ZIP senza compressione** (`feedback-zip.js`, senza librerie):
`feedback.json` con le risposte, e accanto gli allegati coi nomi corti scelti
dall'utente, cioè la posizione della prova su due cifre più una lettera (`01a.png`, `01b.jpg`,
`02a.webp`), `00` per Altro (`00a.png`), e l'identificativo per le risposte a prove non più in
pagina (`3.40-02a.png`). Ogni allegato in `feedback.json` è `{name, type, size, file}`:
`name` è il nome originale, `file` quello nello ZIP. **`Importa` accetta lo ZIP** (anche
ricompresso da un altro programma) **e i JSON esportati prima del 2026-10-03**, con gli
allegati in base64 (`data`), che il controllo di validità trasforma in file veri.
Sul cloud il JSON contiene riferimenti SHA-256 `storageKey`; gli originali rimangono nel
bucket privato. Il caricamento verifica dimensione e hash prima di ricostruire il JSON
completo. Il servizio carica al massimo tre allegati in parallelo e riusa quelli già presenti.

Una nuova proprietà deve essere mantenuta da validazione, copia della bozza, recupero,
importazione/esportazione, riepilogo e validazione server. Verifica sempre i vecchi JSON
e le risposte già esistenti. Non resettare la bozza per facilitare un aggiornamento.

La pagina servita dal Worker ha una `Content-Security-Policy` (dal 2026-10-03): solo i suoi
script, stili, caratteri e API, e immagini solo da sé o dagli allegati in memoria (`blob:`).
Chi aggiunge una risorsa esterna o uno stile in linea allarga la regola nel Worker, o il browser
la blocca; il controllo cloud fallisce se il browser segnala un blocco.

Le risposte personali e gli allegati non entrano nei commit pubblici, nei log o nel brief.
L'accesso cloud è riservato al proprietario GitHub, ID `10722164`; gli agenti ricevono il
giro reso leggibile con Invio e richiesto esplicitamente in chat. Essere manutentore non autorizza a leggere il suo
database privato. `Invia` rende leggibile una copia del giro, senza avviare lettura o lavorazione.
Il salvataggio della bozza da solo non rende leggibili le nuove modifiche.

## Servizio e gestione degli errori

Cloudflare Workers Free serve pagina e API sulla stessa origine. Supabase Free conserva
PostgreSQL e bucket privato `aiv-feedback`. GitHub conserva il codice. R2 non è utilizzato.
Credenziali e token rimangono sul server; la guida cloud descrive i cinque campi Actions,
OAuth, SQL e configurazione. Non chiedere segreti in chat e non metterli nella pagina.

La pagina cloud non usa IndexedDB o localStorage come memoria persistente. La copia su
GitHub Pages, che conservava il vecchio salvataggio locale, non è più pubblicata dal
2026-10-03 (decisione del proprietario): `pages.yml` esclude dal sito la pagina del documento
e i suoi script, che restano in `publish/` perché il Worker serve quella cartella come asset. Se una vecchia
bozza servisse, la pagina si recupera dalla storia git e si apre in locale.

Il salvataggio usa una revisione UUID e un confronto atomico in PostgreSQL. Cloudflare
può trasformare `"revisione"` in `W/"revisione"` durante la compressione: client e server
rimuovono il prefisso W/ prima di confrontare la stessa revisione. Conserva questa gestione
sia sul caricamento/salvataggio sia sul controllo HEAD. Non aggirare un vero conflitto
ritentando con la revisione corrente: sovrascriverebbe un altro salvataggio.

La pagina si sincronizza al ritorno nella scheda e ogni 15 secondi, quando non ci sono
modifiche locali. Senza accesso i campi e l'importazione sono disabilitati con un messaggio
esplicito. Un errore o conflitto conserva le modifiche nella scheda e indica `Non salvato`:
esportare prima di chiudere o ricaricare. Attendere `Salvato nel cloud` prima di considerare
confermata la scrittura. La sessione dura sette giorni; la chiave di sessione viene
conservata dai deploy successivi.

Le revisioni precedenti sono conservate nel database, ma non costituiscono un backup
indipendente. Gli originali scollegati non vengono cancellati automaticamente: lo spazio
occupato può superare quello della bozza. La pausa del progetto Supabase non equivale a
cancellazione; limiti, recupero e precauzioni sono nella guida cloud. Non promettere
conservazione illimitata né considerare un dump SQL una copia degli allegati Storage.

## Aggiornare e pubblicare

1. Registra obiettivo e stato nel brief prima di un intervento su più passi.
2. ⚠️ **Gate Release APK**: il DF di collaudo di una versione esce **dopo** la GitHub Release pubblicata **con l'APK allegato**. Vale quando il DF introduce o aggiorna prove di collaudo per una **nuova versione app**. Un ritocco **solo UX/documentale** del DF (layout, Altro, controlli, copy di manutenzione) si pubblica subito su Feedback cloud/Pages **senza** nuova Release APK.
   Prima di pubblicare o aggiornare il DF per un collaudo `X.XX`, verifica che esista la GitHub Release pubblicata della
   stessa versione, non una bozza, col tag `vX.XX`. Il numero nella pagina, il collegamento all'APK o un tag senza
   release non sono prove sufficienti. Se la release manca, prepara soltanto una bozza locale.
3. Aggiorna le fonti corrette e conserva tutte le risposte aperte. Se il proprietario sta
   compilando, prepara una bozza senza ripubblicarla, salvo sua richiesta esplicita.
4. Rigenera e verifica dalla radice AIV:

   ```sh
   python3 tools/feedback-build.py
   python3 tools/feedback-build.py --check
   python3 tools/feedback-check.py publish/feedback.html
   ```

   Con un giro senza prove aperte il controllo verifica che la pagina parta senza errori e
   senza schede, poi genera una copia temporanea con una prova di sintesi (lo stesso
   generatore, con `--source` e `--output`) e la esercita per intero: la copia non tocca
   `publish/`.

5. Se cambi cloud, protocollo o salvataggio, esegui anche `npm --prefix cloud/feedback test`
   e la prova con due sessioni browser `tools/feedback-cloud-check.py`, avviando i servizi
   locali come descritto nella guida cloud. Usa esclusivamente chiavi e dati fittizi.
   Il controllo remoto usa il proprietario sintetico `0`, mai la bozza personale.
6. Un difetto segnalato richiede la prova che lo riproduce prima del fix. Mantieni le
   verifiche di concorrenza, intestazioni W/, originali e recupero dopo errori.
7. Se cambi gli asset, aggiorna il parametro di versione dei relativi script/CSS nel
   generatore per evitare una pagina nuova con codice vecchio. Non forzare ricariche.
8. Esegui i controlli del diff e del messaggio con `refcheck.py`, poi commit e push su `main`
   secondo le regole del repository, incluso il footer `Agent` della piattaforma effettiva.
9. Attendi `Feedback cloud`: check e deploy devono riuscire. Controlla l'indirizzo nel
   riepilogo, pagina pubblica disponibile e API anonima 401. Se cambia `publish/`, verifica
   anche `Pages`. Una modifica delle sole istruzioni agli agenti non richiede un nuovo APK.
10. Comunica a Rocco in chat il piano scelto per la release successiva e il riepilogo `Prossimi passi` del DF, poi comunica risultato, verifiche e collaudo da fare; aggiorna il brief con il residuo.
   Non chiudere il collaudo dell'app sulla sola riuscita del documento.

Un agente privo di credenziali cloud può aggiornare il repository e usare il workflow
già configurato. Se non può pubblicare, lascia una modifica concreta e verificata e indica
nel brief il passaggio mancante, senza creare un secondo documento o un nuovo indirizzo.

## Invio e presa in carico: due fasi

1. **Invia rende leggibile il giro**, solo dopo un salvataggio riuscito. Non esegue
   workflow di lettura, non avvisa un agente e non avvia lavori. Il proprietario può
   modificare, salvare e inviare di nuovo liberamente. Il nuovo invio sostituisce
   la versione proposta; le modifiche non ancora inviate restano nella bozza.
2. **La richiesta esplicita in chat autorizza la presa in carico**, per esempio
   `Leggi l'ultimo giro di feedback`. Solo allora l'agente esegue il recupero.
   Nessun monitoraggio automatico di salvataggi o invii, nessuna lettura preventiva.
3. ⚠️⚠️ **Subito dopo il recupero, e prima di qualsiasi lavoro prodotto**, si travasa
   **tutto** nel brief privato `Roccobot/tools/.memo/LATEST.md` (esiti, commenti,
   decisioni con chiave, note del campo libero, ordine di lavoro). Solo dopo si
   tocca il codice. Regola universale: `rules/Roccobot.md` § '📋 Prima cosa: tutto
   nel brief, prima del lavoro prodotto'. Allegati e JSON restano fuori dal repo.

4. ⚠️⚠️ **Prima di chiudere l'elaborazione del giro: audit obbligatorio contro
   l'export precedente** (`rules/Roccobot.md` § '🔍 Audit obbligatorio').
   **Documento di feedback = sorgente del lavoro aperto; brief = piano d'azione
   documentato + backlog (non archivio/changelog). Una richiesta esce dal DF solo se è
   fatta nel prodotto o scritta nel brief; dal brief esce solo quando è fatta.** Si rilegge il JSON del
   giro **prima** (o l'allegato rimesso in chat) e si verifica che ogni Non
   approvato / Accettabile / nota del campo libero / decisione operativa sia
   *fatta con prova*, *nel brief e/o ancora nel documento*, oppure *cancellata
   esplicitamente*. Mai far sparire una richiesta. Differito resta in sospeso;
   parziale ≠ completo. Il nuovo documento non azzera quei debiti.

Per recuperare, con GitHub CLI autenticato come proprietario e Node 24, dalla radice AIV:

```sh
node tools/feedback-read.mjs --version 3.24 --output /tmp/aiv-feedback-3.24.json
```

Ometti `--version` per l'ultimo giro inviato di qualsiasi versione. Lo strumento avvia
soltanto su comando il workflow `Leggi feedback inviato`, con chiave pubblica temporanea.
Il server recupera dalla storia l'ultima copia inviata entro la data di avvio della
richiesta: nuovi invii mentre il workflow gira non cambiano il giro preso in carico.
Il contenuto e gli originali vengono cifrati con AES-256-GCM; la chiave viene protetta
con RSA-OAEP. Nei log e negli artefatti GitHub non compare feedback in chiaro. La chiave
privata rimane in memoria nella sessione dell'agente. Il risultato decifrato viene scritto
fuori dal repository, con permessi privati; non commetterlo. L'artefatto cifrato viene
rimosso dopo il recupero o scade dopo un giorno. Nessuna chiave Supabase passa all'agente.

Il workflow ammette solo richieste avviate dal proprietario GitHub e non ha trigger su
push, salvataggio o Invio. L'accesso GitHub è necessario: non è una lettura pubblica.
Negli ambienti con rete limitata, il download degli artefatti GitHub richiede anche
`*.blob.core.windows.net`: GitHub usa più server Azure per gli allegati. Aggiungi il
dominio alla configurazione senza rimuovere quelli esistenti e verifica il download
con `--verify-only` prima di dichiarare pronto il collegamento.
Se una piattaforma non può eseguire il comando, lo dichiara; JSON/riepilogo manuali
restano disponibili come alternativa. Registra nel brief la revisione presa in carico
(`cloudRevision`) e il lavoro risultante, senza contenuti o allegati personali.

Per verificare il collegamento senza leggere feedback personali:

```sh
node tools/feedback-read.mjs --verify-only --output /tmp/aiv-feedback-transfer-test.json
```

La prova usa un proprietario sintetico con zero iniziale, dati e SVG fittizi. Verifica
Supabase, selezione della copia inviata, cifratura, artefatto e decifratura, senza aprire
il giro reale. La suite `feedback-transfer-test.mjs` verifica anche modifiche non inviate,
invii successivi, filtro temporale, originali, chiave errata e manomissioni.
