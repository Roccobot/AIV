# AIV — brief di implementazione

Da dare al tuo agente nel repo `Roccobot/AIV`. Due lavori indipendenti: l'icona
dell'app e la pagina di rilascio. I file citati sono in questa cartella.

---

## 1. Icona dell'app

Il disegno nuovo: la **A di AIV** con angoli arrotondati, il **disco solare fuori
dalla A** in alto a sinistra, il **controcampo trasparente**. Griglia 108.

Nessuna scacchiera dentro l'icona: era una versione intermedia, scartata. La
scacchiera resta quello che è nell'app, il fondo che rivela la trasparenza di una
vera immagine, e non va usata come decorazione del marchio.

### File da sostituire

| percorso nel repo | sorgente qui |
|---|---|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | `icona/ic_launcher_foreground.xml` |

Il layer di background resta un colore pieno: `#43B59E` in `values/`, `#00727B`
in `values-night/`. Il controcampo della A è **trasparente** nel foreground, così
il background traspare: non riempirlo.

Il glifo sta dentro la safe zone da 66dp (estensione reale 24→84 in x, 19→74 in y,
più il disco 20→40). Verifica con Image Asset Studio prima di committare.

### Colori del glifo

- chiaro: `#EBFFF7` su `#43B59E`
- scuro: `#4FD9BE` su `#00727B`

### Marca fuori-tile

`icona/aiv-mark.svg` è la stessa A senza tile, in `currentColor`, controcampo
vuoto con `evenodd`. Sostituisce `assets/aiv-mark.svg` del design system. Serve
dove la A firma un testo, non come icona d'app.

---

## 2. Otto glifi interni

In `glifi/`: photo-pair, folder-pair, folder-pair-dashed, pick-all, pick-none,
pick-invert, align-across, align-down. Il nono, `text-cursor`, non è toccato.

I path vanno riportati in `Glyphs.kt` come `ImageVector` — sono su griglia 24,
quindi `viewportWidth = 24f` anche per align-across e align-down, che prima erano
authored su 800.

Grammatica comune, da applicare a ogni glifo futuro della famiglia:

- tratto **1,8dp**, capi e giunti tondi, niente tratto sotto 1,8
- la coppia copy/move è una **staffa a L a due tratti** dietro, non una seconda
  forma cava
- la forma davanti è piena (cartella) o contornata (lastra immagine), mai entrambe
  cave: a 24px il contorno interno si chiude
- copiare vs spostare = staffa continua vs tratteggiata (`2.8 2.6`), stesso disegno
- le tre voci di selezione condividono **un solo riquadro** (`6.8,4` · 15,2 ·
  r 2,4) e si distinguono per il segno dentro: due spunte, croce, mezzo riquadro
  pieno in diagonale
- align-across e align-down sono asse più tre blocchi; l'uno è l'altro ruotato di 90°

---

## 3. Pagina di rilascio

Rifare `publish/index.html` sul modello di `AIV: pagina di download.dc.html`.
Struttura, dall'alto:

1. **Hero**: icona nuova a 88px, titolo, una frase su cosa fa l'app, pulsante di
   download, riga tecnica, istruzioni di installazione al 70% di opacità.
   Nessuna intestazione sopra: la pagina parte dall'hero.
2. **Sei fatti** su due colonne: trasparenza, GIF e WebP animate, pannello
   dettagli, cestino (con il ⚠️ del design system, testo invariato), uso con una
   mano, nativa non webview.
3. **Note di rilascio** lette da GitHub.
4. **Footer**: icona Roccobot nel supercerchio, `by Roccobot 天`,
   `vibes ✦ 2026 Rocco Casadei | roccobot.me`, e il cambio lingua.

### Lingua

Italiano su browser o OS italiani, inglese per tutti gli altri:

```js
const tags = navigator.languages?.length ? navigator.languages : [navigator.language || 'en'];
const lang = tags.some(t => /^it\b/i.test(t)) ? 'it' : 'en';
```

Il cambio manuale è un pulsante di testo sobrio nel footer, con bordo punteggiato,
che mostra **l'altra** lingua. Non un selettore in evidenza.

### Tema

Segue la preferenza di sistema via `matchMedia('(prefers-color-scheme: dark)')`,
che imposta `data-theme="dark"` sul contenitore; i token del design system fanno
il resto. Con un listener su `change`, così cambia mentre la pagina è aperta.

### Versione e download, sempre da GitHub

Una sola chiamata a
`https://api.github.com/repos/Roccobot/AIV/releases?per_page=1`, e da quella:

- numero di versione dal `tag_name` senza la `v`
- data da `published_at`, formattata nella lingua della pagina
- nome file e peso dall'asset `.apk` (`size` in byte → MB con un decimale)
- `href` del pulsante = `browser_download_url` dell'asset; se una release non ha
  apk allegato, ricadi su `html_url` della release invece di rompere il pulsante

### Note di rilascio leggibili

Ogni bullet del corpo markdown diventa una riga: **etichetta in parole** più la
frase ripulita. Il parser toglie link markdown (tenendo le parole), backtick,
asterischi, numeri di issue, hash dei commit e `by @utente`; poi riconosce il
prefisso del bullet e lo traduce in Novità / Corretto / Migliorato / Rimosso.
Massimo sei righe, poi il link a tutte le versioni.

**Da correggere rispetto al prototipo**, perché le tue note reali lo mostrano: i
bullet iniziano col numero di versione ("1.31: gli SVG si vedono...") e contengono
URL nudi di GitHub. Il parser deve togliere gli URL nudi e usare quel numero di
versione come etichetta della riga, invece di ricadere su "Altro".

### Vincoli

- Stili dal design system Roccobot, `var(--*)`: niente colori nuovi
- Il contenuto del pulsante di download è `#EBFFF7`, lo stesso bianco-verde del
  glifo dell'icona
- Le miniature del mockup sono `Thumb` del design system, con `Placeholder`
  dentro: la pagina non inventa immagini
- Copy invariato dove il design system lo fissa, ⚠️ compreso
