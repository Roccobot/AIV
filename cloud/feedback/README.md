# Salvataggio cloud del documento di feedback

La soluzione scelta usa **Supabase Free** per database e allegati e **Cloudflare Workers
Free** per pagina e accesso GitHub. R2 non va attivato. Cloudflare è un servizio statunitense,
già usato per il Worker delle regole; questo servizio è separato e non modifica `rules-proxy`.
Il codice vive su GitHub, che non conserva le risposte private.

**Preparazione in corso:** lo script SQL Supabase è disponibile e verificato localmente.
L'adattamento del servizio da R2 a Supabase e del workflow di distribuzione non è ancora
completo. Non eseguire il workflow cloud attuale né configurare un bucket R2: il codice
precedente usa ancora quel servizio. La pagina pubblica continua a salvare nel browser
finché configurazione, distribuzione e migrazione non sono concluse.

Supabase richiede un dominio personalizzato a pagamento per servire HTML dalle funzioni:
vedi [limiti delle funzioni](https://supabase.com/docs/guides/functions/limits) e
[domini personalizzati](https://supabase.com/docs/guides/platform/custom-domains).
Il Worker gratuito permette pagina e API sulla stessa origine, con cookie riservati al
server e senza problemi di cookie tra siti su Safari. Supabase rimane l'archivio dei dati.

L'accesso GitHub ammette soltanto l'ID pubblico `10722164` (Roccobot). Non chiede accesso ai
repository: serve solo a riconoscere il proprietario. Il cookie di sessione è Secure,
HttpOnly, SameSite=Lax, scade dopo sette giorni. Token e secret rimangono sul server;
nessuna credenziale di GitHub o Cloudflare entra nei file pubblicati o nella memoria
persistente del browser. Le scritture richiedono l'origine corretta e una versione ETag.

Il documento sul Worker usa il cloud come unica memoria persistente. Il sito GitHub conserva
la vecchia bozza locale per la migrazione; esportarla da lì e importarla sul Worker.
Il JSON di esportazione conserva il formato originale con allegati completi. Non trasferire
mai feedback personali o allegati nei commit del repository pubblico.

## Preparazione del progetto Supabase

La sessione che ha preparato il codice non ha credenziali Cloudflare; il servizio non è
ancora distribuito. Anche la lettura dei nomi dei secret GitHub è negata all'integrazione.
I valori segreti vanno inseriti nei campi riservati, mai in chat o nel repository.

1. Crea un progetto sul piano Free, nella regione europea scelta. Mantieni la Data API
   attiva, disattiva l'esposizione automatica delle nuove tabelle e attiva RLS automatico.
   RLS limita l'accesso alle righe; lo script applica anche privilegi espliciti.
2. In Storage crea il bucket privato `aiv-feedback`. Il collegamento dei repository GitHub
   al progetto Supabase non è necessario.
3. Apri [supabase-setup.sql](supabase-setup.sql), copia tutto il contenuto e incollalo in
   SQL Editor, New query. Premi Run e verifica che l'esecuzione sia riuscita.
   Lo script crea la bozza corrente, la storia delle revisioni e il salvataggio atomico;
   configura anche il bucket privato. Si può rieseguire senza cancellare le risposte.
   Solo il ruolo server `service_role` ha accesso: nessuna chiave va nella pagina pubblica.
4. I passaggi successivi di accesso GitHub e distribuzione verranno completati dopo
   l'adattamento del servizio. Non usare la procedura R2 della versione precedente.

La pausa di Supabase non equivale a cancellazione. La
[guida attuale al recupero](https://github.com/supabase/supabase/blob/master/apps/docs/content/troubleshooting/restore-project-after-90-days-pause.mdx)
indica che oltre un anno di pausa serve scaricare database e oggetti Storage e migrarli in
un nuovo progetto. Una cancellazione elimina anche i backup. Non è una garanzia di
conservazione illimitata. Le revisioni nello stesso database aiutano a recuperare modifiche
precedenti, ma non costituiscono una copia indipendente dal progetto.

## Comportamento

- Salvataggio automatico dopo la scrittura, dischetto e Cmd/Ctrl+S. Il testo e i riferimenti
  viaggiano separati dagli allegati; gli originali sono caricati una volta, identificati da
  SHA-256 e verificati al recupero. Tre trasferimenti paralleli al massimo.
- Aggiornamento all'ingresso nella scheda e ogni 15 secondi quando non ci sono modifiche
  locali. Se due dispositivi modificano la stessa versione, il secondo salvataggio riceve
  un conflitto: il server conserva la nuova bozza e il campo conserva il testo locale.
  Esporta quest'ultimo prima di ricaricare; nessuna sovrascrittura silenziosa.
- Connessione necessaria per salvare. Un errore o una sessione scaduta non vengono indicati
  come salvataggio riuscito. Esporta il JSON per conservare le modifiche prima di chiudere.
- Il salvataggio cloud conserva la bozza, senza consegnare automaticamente il giro all'agente.
  `Invia` prepara il riepilogo da mandare in chat; il JSON consegna anche gli originali.
- Stessi limiti: 8 MB per file, 20 MB di allegati nella bozza, 30 file per riquadro. Reset con
  conferma riguarda la bozza su tutti i dispositivi. Gli oggetti originali scollegati restano
  privati nel bucket: non vengono cancellati in modo concorrente a un altro salvataggio.
  Lo spazio fisico può quindi superare i 20 MB della bozza; va monitorato nel servizio attivo.
- Nessuna misurazione di latenza o prova di accesso su un servizio pubblico è dichiarata
  finché il proprietario non ha configurato i campi necessari e il deploy è verificato.

## Verifica locale riproducibile

Dalla radice AIV, Node 24 e Python con Playwright/Chromium:

```sh
npm ci --prefix cloud/feedback
node cloud/feedback/worker-test.mjs
cloud/feedback/node_modules/.bin/wrangler dev --config cloud/feedback/wrangler.toml --local --port 8787 --persist-to /tmp/aiv-feedback-r2 --var GITHUB_CLIENT_ID:local-test-id --var GITHUB_CLIENT_SECRET:local-test-secret --var SESSION_SECRET:development-test-secret
```

In un secondo terminale:

```sh
python3 tools/feedback-cloud-check.py
python3 tools/feedback-check.py publish/feedback.html
```

Il controllo cloud accetta solo localhost, firma una sessione di prova con la chiave
fittizia della riga sopra e usa R2 locale reale. Non avvia OAuth di produzione e non tocca
risposte del proprietario. Non configurare mai la chiave di prova su un servizio pubblico.
