# Manutenzione del documento di feedback AIV

Questa guida vale per qualsiasi agente e piattaforma di sviluppo. Il documento condiviso
è [Feedback AIV](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
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
| [feedback-build.py](../tools/feedback-build.py) | Generatore, struttura della pagina, decisioni e dati incorporati |
| [feedback.html](../publish/feedback.html) | Documento generato da pubblicare, mai unica fonte di una modifica |
| [feedback.js](../publish/feedback.js) | Risposte, validazione JSON, importazione/esportazione, riepilogo, navigazione |
| [feedback-format.js](../publish/feedback-format.js) | Editor visivo, Markdown, icone e scorciatoie |
| [feedback.css](../publish/feedback.css) | Aspetto, esiti, evidenze e adattamento dello schermo |
| [feedback-cloud.js](../publish/feedback-cloud.js) | Bozza remota, originali, versioni e accesso lato pagina |
| [feedback-cloud-config.js](../publish/feedback-cloud-config.js) | Configurazione neutra su GitHub, sostituita dal Worker sul cloud |
| [worker.mjs](../cloud/feedback/worker.mjs) | Pagina, accesso GitHub, API private e controllo delle scritture |
| [supabase-store.mjs](../cloud/feedback/supabase-store.mjs) | Database e Storage Supabase, soltanto lato server |
| [supabase-setup.sql](../cloud/feedback/supabase-setup.sql) | Tabelle, revisioni, privilegi e bucket privato |
| [feedback-cloud.yml](../.github/workflows/feedback-cloud.yml) | Verifiche e distribuzione del servizio cloud |
| [pages.yml](../.github/workflows/pages.yml) | Copia pubblica GitHub Pages |

La versione delle prove si ricava da `versionName` in `app/build.gradle.kts`.
Una modifica al documento non produce una nuova versione Android.
Le proposte tablet e impostazioni rimangono nei rispettivi file e pagine, collegati dal
documento. Il vecchio artefatto Claude è un riferimento storico per le sue voci ancora
da recuperare: non va cancellato né considerato approvato automaticamente.

## Convenzioni di contenuto e interfaccia

- Nome ufficiale: **documento di feedback** (DF). Titolo: **Feedback AIV**.
  Intestazione: `AIV · giro della X.XX`, terminata alla versione, senza nome dell'agente.
- ⚠️ **Separazione UX DF / collaudo app**: miglioramenti di interfaccia del DF (striscia,
  Altro, overlay, shell, formattazione, Salva-only mobile, casella versione, PP, …) vivono
  in codice, in questa guida e nel brief. **Non** vanno elencati come prove di collaudo
  del prodotto Android nel documento.
- Sigle: **DF** = documento di feedback; **PP** = **Prossimi passi**.
- Ogni DF termina, prima della coda, con una sezione intitolata esattamente **Prossimi passi** (PP): un riepilogo breve e schematico di differiti, accorpati per dopo, voci da decidere e altre voci già nel brief.
- Dopo aver letto un giro, la release successiva non deve comprendere tutto il backlog. L'agente sceglie liberamente il piano, ma lo comunica proattivamente a Rocco in chat, con ciò che entra e ciò che resta, senza aspettare che Rocco lo ricavi dal DF.
  Gli eventuali riferimenti all'autore nei testi visibili usano `l'agente`.
- Scrivi testi italiani e prove eseguibili: comandi da raggiungere, azione e risultato
  atteso. Mantieni le prove aperte fra release; archivia soltanto quelle concluse dal
  giro consegnato. Non sostituire riscontri manuali con prove automatiche.
- Conserva gli identificatori esistenti, come `3.13-01`, e le chiavi delle decisioni.
  Per una prova nuova usa un identificatore nuovo: rinominare o riutilizzare una chiave
  può associare una risposta a una prova diversa.
- Ogni verifica mostra `Verifica X/Y`, con X in grassetto; il totale deriva dalle prove.
  Le decisioni sono separate e non entrano nel conteggio degli esiti.
- Esiti: `Tutto OK`, `Accettabile`, `Non approvato`; nessuna scelta significa `Non provato`.
  Un secondo clic toglie l'esito. Verde, ambra e rosso seguono la scelta; commenti o allegati
  senza esito hanno evidenza neutra. Una risposta presente non equivale ad approvazione.
- Specifiche separate per telefono e tablet: modello e Android. I commenti indicano
  su quale dispositivo si è verificato un problema quando necessario.
- Editor con formattazione visibile, senza anteprima duplicata; icone grassetto, corsivo,
  link con nomi accessibili e suggerimenti. Cmd/Ctrl+B, I, K; Cmd/Ctrl+S salva.
  Il testo normale non è grassetto; incolli di testo semplice, nessun HTML interpretato.
- Navigazione flottante: riquadro precedente, successivo, primo non compilato quando
  esiste, salvataggio con dischetto. Commento o allegato contano come compilazione.
- **Altro**: su desktop è una colonna laterale reale (preferibilmente a sinistra), sticky,
  sempre pronta, con toolbar di formattazione e allegati (`+` e trascinamento); la colonna
  è circa il 50% più larga del primo taglio laterale. Su desktop la striscia conteggi,
  la card Accedi/Salvataggio cloud e i paragrafi intro condividono la stessa larghezza
  totale di (colonne prove + Altro), senza fascia a tutta viewport. Su mobile resta in
  fondo **prima** del PP; la striscia sticky mostra solo i chip semaforo centrati (niente
  hamburger né tasto Altro). Pressione prolungata sul FAB flottante ⇥ apre Altro a pannello
  overlay (chiudi con ×), sullo stesso campo `notes` del riquadro in fondo e con lo stesso
  salvataggio cloud. Non usare un riquadro `position: fixed` staccato dal flusso come unica
  sede di Altro.
- ⚠️ **Etichette testuali**: quando una feature introduce o aggiorna copy italiano di
  interfaccia (paragrafi, pulsanti, toast, voci, …), l'agente può redigere la proposta e
  far uscire la versione; il DF deve elencare **ogni** nuova stringa ITA una per una in
  una sezione intitolata esattamente **Etichette testuali**, in basso **prima** delle
  sezioni conclusive/archivio (`Riscontri conclusi`) e del PP. Ogni sotto-card mostra il
  testo ITA proposto per intero e un campo libero: ciò che l'utente scrive sostituisce la
  proposta al prossimo rilascio utile; campo vuoto = approvato. Non è una sezione di prove
  (niente esiti, fuori dai contatori). Assente o vuota → sezione nascosta.
- ⚠️ **Consegna e copie**: non resta una card sempre visibile nel flusso. Si apre come
  overlay popup. Desktop: pulsante nella riga strumenti di Altro, a destra dell'allegato.
  Mobile: pressione prolungata sul FAB Salva (dischetto) apre lo stesso popup; tocco breve
  resta Salva. Si chiude con ×, tap fuori, Escape, o a fine interazione utile (Invia, Copia,
  Esporta, Importa, Azzera); dopo Salva resta aperto per poter Inviare.
- La versione AIV del giro nel DF è testo fisso (`spec.version`); la conferma avviene solo
  con la casella 'Sì, ho installato questa versione' (`installed` = versione del giro o vuoto).
- Allegati mediante selettore e trascinamento nelle verifiche e in Altro:
  PNG, JPG, WebP, GIF, SVG e ZIP. Originali interi, nomi conservati, ZIP scaricabili.
  Limiti attuali: 8 MB per file, 20 MB totali, 30 allegati per riquadro.
- Collegamenti esterni in nuova scheda con `noopener noreferrer`. Tutte le pagine AIV
  usano la stessa favicon `assets/feedback-favicon.svg`, colore `#43B59E`, e alternativa PNG.
- `Azzera tutto` richiede conferma e riguarda la bozza su tutti i dispositivi.
  Un comando disabilitato non indica un caricamento: cursore normale e aspetto coerente,
  anche per il selettore di `Importa JSON`.

## Dati, privacy e compatibilità

Il JSON usa `schema: 1`, `project: AIV`. La bozza contiene:

| Campo | Significato |
|---|---|
| `version`, `installed` | Versione del documento; `installed` vale la stessa stringa solo se Rocco ha spuntato 'Sì, ho installato questa versione', altrimenti stringa vuota (non è più un campo libero) |
| `device` | Specifiche del telefono; chiave storica conservata |
| `tablet` | Specifiche del tablet; assente nei vecchi JSON, ripristinata come testo vuoto |
| `entries` | Risposte per ID: `status`, `comment`, `images` |
| `decisions` | Decisioni per ID: `choice`, `comment` |
| `labels` | Etichette testuali per ID: `revision` (campo libero; assente nei JSON vecchi → `{}`) |
| `notes`, `extra.images` | Osservazioni libere e relativi allegati |
| `updated`, `completed` | Data del salvataggio e della preparazione del giro |

`images` è il nome storico anche per gli ZIP: non cambiarlo senza migrazione.
L'esportazione JSON include gli allegati completi come data URL e i nomi originali.
Sul cloud il JSON contiene riferimenti SHA-256 `storageKey`; gli originali rimangono nel
bucket privato. Il caricamento verifica dimensione e hash prima di ricostruire il JSON
completo. Il servizio carica al massimo tre allegati in parallelo e riusa quelli già presenti.

Una nuova proprietà deve essere mantenuta da validazione, copia della bozza, recupero,
importazione/esportazione, riepilogo e validazione server. Verifica sempre i vecchi JSON
e le risposte già esistenti. Non resettare la bozza per facilitare un aggiornamento.

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

La pagina cloud non usa IndexedDB o localStorage come memoria persistente. La copia
[GitHub Pages](https://roccobot.github.io/AIV/feedback.html) mantiene il vecchio salvataggio
locale per compatibilità: può esportare la bozza da importare sul cloud, ma non sincronizza
i dispositivi. Non introdurre reindirizzamenti che rendano inaccessibili le vecchie risposte.

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
2. ⚠️ **Gate Release APK**: vale quando il DF introduce o aggiorna prove di collaudo per una **nuova versione app**. Un ritocco **solo UX/documentale** del DF (layout, Altro, controlli, copy di manutenzione) si pubblica subito su Feedback cloud/Pages **senza** nuova Release APK.
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
   documentato + backlog (non archivio/changelog); solo il fatto esce da entrambi.** Si rilegge il JSON del
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
