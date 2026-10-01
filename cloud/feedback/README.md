# Salvataggio cloud del documento di feedback

La soluzione scelta usa **Supabase Free** per database e allegati e **Cloudflare Workers
Free** per pagina e accesso GitHub. R2 non va attivato. Cloudflare è un servizio statunitense,
già usato per il Worker delle regole; questo servizio è separato e non modifica `rules-proxy`.
Il codice vive su GitHub, che non conserva le risposte private.

Il servizio usa il database PostgreSQL e il bucket privato Supabase: nessun binding R2.
Prima di distribuire, il workflow verifica SQL, privilegi, due sessioni del browser,
credenziali dell'app GitHub e salvataggio/conflitti/allegati sul progetto Supabase reale.
La pagina GitHub conserva la bozza locale finché il proprietario non la trasferisce nel
cloud e ne conferma il recupero su un secondo dispositivo.

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
4. Registra su GitHub una OAuth App chiamata `AIV Feedback`, senza permessi sui repository.
   Sullo stesso account di `rules-proxy`, homepage prevista
   `https://aiv-feedback.roccobot-b90.workers.dev/feedback.html` e indirizzo di ritorno
   `https://aiv-feedback.roccobot-b90.workers.dev/auth/callback`. Verifica il sottodominio
   Workers dell'account; se diverso, usa quello in entrambi gli indirizzi.
5. Crea un token Cloudflare limitato a quell'account con Workers Scripts: Edit e
   Account Settings: Read. Il piano Workers Free non richiede l'attivazione R2.
6. In [Actions di AIV](https://github.com/Roccobot/AIV/settings/secrets/actions) configura:

   | Tipo | Nome | Contenuto |
   |---|---|---|
   | Secret | `CLOUDFLARE_API_TOKEN` | Token Cloudflare limitato all'account |
   | Secret | `FEEDBACK_GITHUB_CLIENT_SECRET` | Client secret dell'app GitHub |
   | Secret | `FEEDBACK_SUPABASE_SECRET_KEY` | Secret key Supabase, con prefisso sb_secret_ |
   | Variable | `CLOUDFLARE_ACCOUNT_ID` | ID pubblico dell'account Cloudflare |
   | Variable | `FEEDBACK_GITHUB_CLIENT_ID` | Client ID pubblico dell'app GitHub |

   La chiave Supabase rimane sul Worker. Il solo URL pubblico del progetto vive in
   `wrangler.toml`; non occorre una chiave pubblicabile nel browser.
7. Esegui [Feedback cloud](https://github.com/Roccobot/AIV/actions/workflows/feedback-cloud.yml).
   I controlli sul progetto reale usano il proprietario sintetico 0, separato dall'ID
   GitHub 10722164: non leggono né alterano il feedback del proprietario. Conservano poche
   revisioni sintetiche, eliminando il file di prova appena verificato. Il workflow genera
   la chiave di sessione al primo deploy e la conserva nei successivi. Il riepilogo mostra
   l'indirizzo effettivo, verificando pagina disponibile e lettura anonima negata.
8. Esporta il JSON dalla vecchia pagina GitHub, accedi al nuovo indirizzo e importalo.
   Attendi `Salvato nel cloud`, poi apri lo stesso indirizzo sul secondo dispositivo e
   controlla risposte e allegati. Solo dopo questa verifica la migrazione è conclusa.

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
- Ogni salvataggio mantiene una revisione del testo e dei riferimenti agli allegati nel
  database. Lo spazio di revisione si aggiunge alla bozza corrente; non viene cancellato
  automaticamente. Il recupero delle revisioni si effettua dal database, non dall'editor.
  Le revisioni nello stesso progetto non sostituiscono un backup indipendente.
- Nessuna misurazione di latenza o prova di accesso su un servizio pubblico è dichiarata
  finché il proprietario non ha configurato i campi necessari e il deploy è verificato.

## Verifica locale riproducibile

Dalla radice AIV, Node 24 e Python con Playwright/Chromium:

```sh
npm ci --prefix cloud/feedback
npm --prefix cloud/feedback test
node cloud/feedback/supabase-test-server.mjs 8790
```

In un secondo terminale, dalla radice AIV:

```sh
cloud/feedback/node_modules/.bin/wrangler dev --config cloud/feedback/wrangler.toml --local --port 8787 --var SUPABASE_URL:http://127.0.0.1:8790 --var SUPABASE_SECRET_KEY:development-supabase-test-key --var GITHUB_CLIENT_ID:local-test-id --var GITHUB_CLIENT_SECRET:local-test-secret --var SESSION_SECRET:development-test-secret
```

In un terzo terminale:

```sh
python3 tools/feedback-cloud-check.py
python3 tools/feedback-check.py publish/feedback.html
```

Il controllo cloud accetta solo localhost, firma una sessione di prova con la chiave
fittizia della riga sopra e usa il servizio HTTP locale con PostgreSQL reale e archivio di file fittizio. Non avvia OAuth di produzione e non tocca
risposte del proprietario. Non configurare mai la chiave di prova su un servizio pubblico.
