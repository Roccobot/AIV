# Pagine AIV

Questa cartella è pubblicata da `.github/workflows/pages.yml`, che lascia fuori i file del
documento di feedback (`feedback.html`, i suoi script `feedback*.js`, e questo README): quelli
li serve solo il Worker privato, che usa la stessa cartella come asset. `feedback.css` e le
favicon restano, perché le usano anche le altre pagine.

| Pagina | Scopo | Fonte |
|---|---|---|
| [index.html](index.html) | Presentazione e download dell'APK | Pagina scritta direttamente |
| [feedback.html](feedback.html) (solo sul Worker) | Prove condivise; risposte e allegati sul cloud privato | [docs/Feedback.md](../docs/Feedback.md), generatore `tools/feedback-build.py` |
| [tablet.html](tablet.html) | Mockup interattivi da valutare | [docs/Tablet-proposal.md](../docs/Tablet-proposal.md), pagina scritta direttamente |
| [settings.html](settings.html) | Proposta di riordino delle impostazioni | [docs/Settings-proposal.md](../docs/Settings-proposal.md), generatore `tools/settings-build.py` |

Gli asset condivisi vivono in `assets/`: favicon SVG e PNG, Roboto e relativa licenza.
La favicon del documento di feedback e delle sue pagine di supporto (`tablet.html`,
`settings.html`) è `assets/feedback-favicon.svg`, il blocco note col glifo in `#43B59E`; il
PNG trasparente è l'alternativa per i browser meno recenti. ⚠️ `index.html`, la paginetta di
download, **non** la usa: ha il glifo nudo dell'app incorporato nella pagina, e la prova
`PaginettaTest` lo controlla.
CSS e JavaScript portano il nome della pagina a cui appartengono. `feedback-format.js`
gestisce gli editor visivi dei commenti, selezione e scorciatoie. I campi mostrano la
formattazione direttamente, mentre salvataggi ed esportazioni conservano Markdown.
I commenti esistenti sono ripristinati senza interpretare HTML e gli incolli inseriscono testo semplice.
Le schermate dell'app vivono in `schermate/`; `anteprima.jpg` è l'immagine dei link condivisi.

Dalla radice del repository:

```sh
python3 tools/feedback-build.py
python3 tools/settings-build.py
python3 tools/feedback-build.py --check
python3 tools/settings-build.py --check
python3 tools/feedback-check.py publish/feedback.html
```

Le risposte conservano gli identificatori fra aggiornamenti. Gli allegati PNG/JPG/WebP/GIF,
SVG e ZIP sono conservati interi nel cloud e nel JSON, sia nelle voci sia nelle osservazioni
libere; selettore e trascinamento usano gli stessi limiti (8 MB per file, 20 MB complessivi).
Il JSON rimane unico, anche in presenza di allegati. Le verifiche mostrano X/totale;
i comandi flottanti percorrono i riquadri e trovano il primo senza risposte, escludendo
le osservazioni libere opzionali. Non modificare gli stati del
collaudo senza il giro completo consegnato dall'utente. L'aggiornamento del sito non crea
una release Android: l'APK e la sua versione sono indipendenti.

Il documento principale è [Feedback AIV](https://aiv-feedback.roccobot-b90.workers.dev/feedback),
condiviso da tutti gli agenti. La [guida di manutenzione](../docs/Feedback-maintenance.md)
descrive convenzioni, fonti, dati, prove e procedura di aggiornamento.

Il servizio privato è in [cloud/feedback](../cloud/feedback/README.md), con accesso GitHub
riservato al proprietario e salvataggio in Supabase Free. Il Worker Cloudflare Free serve
la stessa cartella `publish/` insieme all'API, per mantenere pagina e cookie sulla stessa
origine. `feedback-cloud-config.js` è neutro fuori dal Worker (file locale, verifiche) e il Worker lo
sostituisce con la configurazione pubblica; `feedback-cloud.js` gestisce risposte, allegati e
sincronizzazione. La copia su GitHub Pages non è più pubblicata dal 2026-10-03: una vecchia
bozza locale si recupera, se serve, dalla storia git.
Il workflow `Feedback cloud` verifica e distribuisce il servizio già configurato;
la guida tecnica descrive i cinque campi riservati/pubblici Actions e la verifica remota.
