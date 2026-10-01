# Salvataggio cloud del documento di feedback

Pagina e API sono nello stesso Worker Cloudflare; R2 conserva la bozza privata e gli
allegati originali. Cloudflare è un servizio statunitense, già usato per il Worker delle
regole; questo servizio è separato e non modifica `rules-proxy`.

L'accesso GitHub ammette soltanto l'ID pubblico `10722164` (Roccobot). Non chiede accesso ai
repository: serve solo a riconoscere il proprietario. Il cookie di sessione è Secure,
HttpOnly, SameSite=Lax, scade dopo sette giorni. Token e secret rimangono sul server;
nessuna credenziale di GitHub o Cloudflare entra nei file pubblicati o nella memoria
persistente del browser. Le scritture richiedono l'origine corretta e una versione ETag.

Il documento sul Worker usa il cloud come unica memoria persistente. Il sito GitHub conserva
la vecchia bozza locale per la migrazione; esportarla da lì e importarla sul Worker.
Il JSON di esportazione conserva il formato originale con allegati completi. Non trasferire
mai feedback personali o allegati nei commit del repository pubblico.

## Attivazione sull'account del proprietario

La sessione che ha preparato il codice non ha credenziali Cloudflare; il servizio non è
ancora distribuito. Anche la lettura dei nomi dei secret GitHub è negata all'integrazione.
I valori segreti vanno inseriti nei campi riservati, mai in chat o nel repository.

1. Nella [dashboard Cloudflare](https://dash.cloudflare.com/), abilita R2 e crea il bucket
   privato `aiv-feedback`. Non attivare l'accesso pubblico. Controlla il piano e le condizioni
   di R2: l'eventuale attivazione della fatturazione è una scelta del proprietario.
2. Registra una [OAuth App GitHub](https://github.com/settings/applications/new) chiamata
   `AIV Feedback`. Sullo stesso account Cloudflare di `rules-proxy`, l'indirizzo previsto è
   `https://aiv-feedback.roccobot-b90.workers.dev/feedback.html` e il callback è
   `https://aiv-feedback.roccobot-b90.workers.dev/auth/callback`. Verifica il sottodominio
   Workers dell'account; se è diverso, usa quello nei due indirizzi. La callback è obbligatoria.
3. Crea un token API Cloudflare limitato a quell'account, con Workers Scripts: Edit,
   Workers R2 Storage: Edit e Account Settings: Read. Non serve un token globale.
4. In [Settings, Secrets and variables, Actions di AIV](https://github.com/Roccobot/AIV/settings/secrets/actions)
   configura i quattro campi:

   | Tipo | Nome | Contenuto |
   |---|---|---|
   | Secret | `CLOUDFLARE_API_TOKEN` | Token API Cloudflare |
   | Secret | `FEEDBACK_GITHUB_CLIENT_SECRET` | Client secret della OAuth App |
   | Variable | `CLOUDFLARE_ACCOUNT_ID` | ID pubblico dell'account Cloudflare |
   | Variable | `FEEDBACK_GITHUB_CLIENT_ID` | Client ID pubblico della OAuth App |

5. Esegui il workflow [Feedback cloud](https://github.com/Roccobot/AIV/actions/workflows/feedback-cloud.yml).
   Verifica prima Worker, R2 e due sessioni isolate del browser in locale, poi distribuisce
   il servizio. La chiave di sessione viene generata al primo deploy e conservata nei secret
   del Worker nei deploy successivi. Il riepilogo dell'Action restituisce l'indirizzo effettivo.
   Gli aggiornamenti successivi di pagina o servizio si distribuiscono automaticamente,
   dopo le prove, quando le due variabili dell'account sono configurate.
6. Apri l'indirizzo restituito, accedi con Roccobot, importa il JSON esportato dal sito GitHub
   e attendi `Salvato nel cloud`. Apri lo stesso indirizzo su un altro dispositivo e accedi:
   devono tornare risposte e allegati. Solo dopo questa conferma il trasferimento è concluso.

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
  Lo spazio fisico può quindi superare i 20 MB della bozza; monitoralo nell'account R2.
- Nessuna misurazione di latenza o prova di accesso su un servizio pubblico è dichiarata
  finché il proprietario non ha configurato i quattro campi e il deploy è verificato.

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
