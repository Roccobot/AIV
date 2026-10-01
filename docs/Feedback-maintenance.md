# Manutenzione del documento di feedback AIV

Questa guida vale per qualsiasi agente e piattaforma di sviluppo. Il documento condiviso
è [Feedback AIV](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
Il proprietario ha confermato il funzionamento completo il 1 ottobre 2026.
Gli agenti mantengono lo stesso codice e indirizzo; ricevono il giro completo in chat.
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

- Nome ufficiale: **documento di feedback**. Titolo: **Feedback AIV**.
  Intestazione: `AIV · giro della X.XX`, terminata alla versione, senza nome dell'agente.
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
- Allegati mediante selettore e trascinamento nelle verifiche e in `Qualsiasi altra cosa`:
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
| `version`, `installed` | Versione del documento e dell'app installata |
| `device` | Specifiche del telefono; chiave storica conservata |
| `tablet` | Specifiche del tablet; assente nei vecchi JSON, ripristinata come testo vuoto |
| `entries` | Risposte per ID: `status`, `comment`, `images` |
| `decisions` | Decisioni per ID: `choice`, `comment` |
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
JSON o il riepilogo consegnato in chat. Essere manutentore non autorizza a leggere il suo
database privato. `Invia` prepara il riepilogo; il salvataggio cloud non consegna il giro.

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
2. Aggiorna le fonti corrette e conserva tutte le risposte aperte. Se il proprietario sta
   compilando, prepara una bozza senza ripubblicarla, salvo sua richiesta esplicita.
3. Rigenera e verifica dalla radice AIV:

   ```sh
   python3 tools/feedback-build.py
   python3 tools/feedback-build.py --check
   python3 tools/feedback-check.py publish/feedback.html
   ```

4. Se cambi cloud, protocollo o salvataggio, esegui anche `npm --prefix cloud/feedback test`
   e la prova con due sessioni browser `tools/feedback-cloud-check.py`, avviando i servizi
   locali come descritto nella guida cloud. Usa esclusivamente chiavi e dati fittizi.
   Il controllo remoto usa il proprietario sintetico `0`, mai la bozza personale.
5. Un difetto segnalato richiede la prova che lo riproduce prima del fix. Mantieni le
   verifiche di concorrenza, intestazioni W/, originali e recupero dopo errori.
6. Se cambi gli asset, aggiorna il parametro di versione dei relativi script/CSS nel
   generatore per evitare una pagina nuova con codice vecchio. Non forzare ricariche.
7. Esegui i controlli del diff e del messaggio con `refcheck.py`, poi commit e push su `main`
   secondo le regole del repository, incluso il footer `Agent` della piattaforma effettiva.
8. Attendi `Feedback cloud`: check e deploy devono riuscire. Controlla l'indirizzo nel
   riepilogo, pagina pubblica disponibile e API anonima 401. Se cambia `publish/`, verifica
   anche `Pages`. Una modifica delle sole istruzioni agli agenti non richiede un nuovo APK.
9. Comunica risultato, verifiche e collaudo da fare; aggiorna il brief con il residuo.
   Non chiudere il collaudo dell'app sulla sola riuscita del documento.

Un agente privo di credenziali cloud può aggiornare il repository e usare il workflow
già configurato. Se non può pubblicare, lascia una modifica concreta e verificata e indica
nel brief il passaggio mancante, senza creare un secondo documento o un nuovo indirizzo.
