# Pagine AIV

Questa cartella è pubblicata integralmente da `.github/workflows/pages.yml`.

| Pagina | Scopo | Fonte |
|---|---|---|
| [index.html](index.html) | Presentazione e download dell'APK | Pagina scritta direttamente |
| [feedback.html](feedback.html) | Prove, risposte e allegati conservati nel browser | [docs/Feedback.md](../docs/Feedback.md), generatore `tools/feedback-build.py` |
| [tablet.html](tablet.html) | Mockup interattivi da valutare | [docs/Tablet-proposal.md](../docs/Tablet-proposal.md), pagina scritta direttamente |
| [settings.html](settings.html) | Proposta di riordino delle impostazioni | [docs/Settings-proposal.md](../docs/Settings-proposal.md), generatore `tools/settings-build.py` |

Gli asset condivisi vivono in `assets/`: favicon SVG e PNG, Roboto e relativa licenza.
La favicon comune è `assets/feedback-favicon.svg`, in `#43B59E`; il nome conserva il link
consegnato all'utente. Il PNG trasparente è l'alternativa per i browser meno recenti.
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
SVG e ZIP sono conservati interi nel browser e nel JSON, sia nelle voci sia nelle osservazioni
libere; selettore e trascinamento usano gli stessi limiti (8 MB per file, 20 MB complessivi).
Il JSON rimane unico, anche in presenza di allegati. Le verifiche mostrano X/totale;
i comandi flottanti percorrono i riquadri e trovano il primo senza risposte, escludendo
le osservazioni libere opzionali. Non modificare gli stati del
collaudo senza il giro completo consegnato dall'utente. L'aggiornamento del sito non crea
una release Android: l'APK e la sua versione sono indipendenti.
