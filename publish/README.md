# Pagine AIV

Dalla 6.29 `.github/workflows/pages.yml` pubblica di questa cartella un elenco di file da
includere: `index.html`, `privacy.html`, `anteprima.jpg`, `schermate/` e `assets/Roboto.ttf`
con la sua licenza; `tools/site-check.py` ferma la pubblicazione se una pagina cita un file
locale che l'elenco non ha. Tutto il resto (il documento di feedback coi suoi script, stili e
icone, e il mockup del tablet) lo serve solo il Worker privato, che usa la cartella come asset.

| Pagina | Scopo | Fonte |
|---|---|---|
| [index.html](index.html) | Presentazione e download dell'APK | Pagina scritta direttamente |
| [privacy.html](privacy.html) | Informativa sulla privacy | Pagina scritta direttamente |
| [feedback.html](feedback.html) (solo sul Worker) | Prove condivise; risposte e allegati sul cloud privato | [docs/Feedback.md](../docs/Feedback.md), generatore `tools/feedback-build.py` |
| [tablet.html](tablet.html) (solo sul Worker) | Mockup interattivi da valutare | [docs/Tablet-proposal.md](../docs/Tablet-proposal.md), pagina scritta direttamente |

Gli asset condivisi vivono in `assets/`: favicon SVG e PNG, Roboto e relativa licenza.
La favicon del documento di feedback e della sua pagina di supporto (`tablet.html`) è `assets/feedback-favicon.svg`, il blocco note col glifo in `#43B59E`; il
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
python3 tools/feedback-build.py --check
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
