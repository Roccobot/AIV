#!/usr/bin/env python3
"""L'immagine che accompagna il link della paginetta di download, quando lo si condivide.

⚠️⚠️ **NASCE CON LA `2.92`, ED È SUA RICHIESTA** (punto 1 del campo libero del giro della `2.91`:
*il mini-sito di AIV necessita di Immagine OpenGraph, Twitter e Telegram*). Telegram, X, WhatsApp
e gli altri leggono i meta tag della pagina e ne mostrano l'immagine accanto al link: senza, il
link arrivava nudo.

⚠️⚠️ **NON RIDISEGNA NIENTE: COMPONE QUELLO CHE LA PAGINA HA GIÀ.** L'icona è l'`<svg>` di
`publish/index.html`, il titolo è il suo `<h1>`, l'indirizzo è il suo `og:url`, e la schermata è
`publish/schermate/chiara.webp`, cioè quella vera che la pagina mostra. Così l'immagine non ha
una fonte sua che possa divergere: quando cambia una di quelle quattro cose, si rilancia questo
strumento e basta.
- ⚠️ **La schermata è quella CHIARA, e non è una svista**: l'anteprima è una sola, e il servizio
  che la mostra non sa che tema ha chi guarda. Il chiaro è quello di fabbrica del telefono.

⚠️⚠️ **I CARATTERI SONO QUELLI VERI O NIENTE**: il testo è in Roboto, che è il carattere dell'app
e quello con cui la pagina si legge sul telefono, e arriva da Google Fonts. Se non arriva, lo
strumento **si ferma** invece di scrivere un'immagine col carattere di ripiego: una misura fatta
senza i caratteri veri non si spaccia per buona (`rules/Roccobot.md`, § '🧪 Test e verifiche').

⚠️ **Un JPEG e non un PNG**: con dentro una fotografia il PNG pesa più di mezzo megabyte, e
WhatsApp scarta le anteprime troppo pesanti. Lo strumento dichiara il peso che ha scritto.

Uso, dalla radice del repository:
    python3 tools/og-image.py
"""
import html
import importlib.util
import pathlib
import re
import sys
import tempfile

RADICE = pathlib.Path(__file__).resolve().parent.parent
_spec = importlib.util.spec_from_file_location('browser', pathlib.Path(__file__).with_name('browser.py'))
browser = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(browser)
PAGINA = RADICE / 'publish' / 'index.html'
SCHERMATA = RADICE / 'publish' / 'schermate' / 'chiara.webp'
USCITA = RADICE / 'publish' / 'anteprima.jpg'

# La misura che i servizi si aspettano: 1,91 a 1, ed è quella che la pagina dichiara nei meta.
LARGO, ALTO = 1200, 630

# I colori del tema chiaro della pagina, cioè i token che lei stessa copia dal design system.
FONDO = '#FCFBF8'
INCHIOSTRO = '#1A1C1B'
MUTO = '#5B6360'
FILO = '#DAD9D5'
ACCENTO = '#43B59E'
GLIFO = '#EBFFF7'


def avvia(p):
    """Il browser, cercato come lo cercano tutti gli strumenti (`browser.py`)."""
    via = browser.percorso(p)
    if not via:
        sys.exit('manca Chromium: né quello di Playwright, né quello del container, né uno nel PATH')
    return p.chromium.launch(executable_path=via, args=['--no-sandbox'])


def righe_titolo(titolo):
    """Il titolo di tre parole va a capo dopo la prima: 'Astonishing' sopra, 'Image Viewer' sotto,
    perché su una riga sola al corpo delle due immagini non entra."""
    parole = titolo.split()
    if len(parole) == 3:
        return html.escape(parole[0]) + '<br>' + html.escape(' '.join(parole[1:]))
    return html.escape(titolo)


def fotografa(pg, uscita, pesi, **cattura):
    """Scrive la cattura della scena aperta in `pg`, solo se Roboto è arrivato nei `pesi` chiesti e
    la schermata si è caricata; altrimenti si ferma senza scrivere niente.

    ⚠️⚠️ **SI CHIEDE IL CARATTERE, E SI GUARDA CHE SIA ARRIVATO** (rilievo G29 della revisione
    Setaccio, 6.30): `document.fonts.check` risponde sì anche per un carattere mai arrivato,
    perché su una pagina senza caratteri da caricare non c'è niente che manchi (misurato: sì anche
    per una famiglia che non esiste). `document.fonts.load` restituisce le facce del foglio di
    stile, e una lista vuota vuol dire che il foglio di Google Fonts non è arrivato."""
    pg.wait_for_load_state('networkidle')
    pronti = pg.evaluate('''async (pesi) => {
        await document.fonts.ready;
        const caratteri = [];
        for (const peso of pesi) {
            const facce = await document.fonts.load(peso + ' 40px Roboto');
            caratteri.push(facce.length > 0 && facce.every(f => f.status === 'loaded'));
        }
        const img = document.querySelector('.schermata');
        if (img && !img.complete) await new Promise(r => img.addEventListener('load', r));
        return { caratteri: caratteri.every(Boolean), schermata: !img || img.naturalWidth > 0 };
    }''', list(pesi))
    if not pronti['caratteri']:
        sys.exit('Roboto non è arrivato da Google Fonts: nessuna immagine scritta')
    if not pronti['schermata']:
        sys.exit('la schermata non si è caricata: nessuna immagine scritta')
    pg.screenshot(path=str(uscita), **cattura)


def pezzi(testo):
    """L'icona, il titolo e l'indirizzo, letti dalla pagina."""
    icona = re.search(r'<svg class="icona".*?</svg>', testo, re.S)
    titolo = re.search(r'<h1>(.*?)</h1>', testo, re.S)
    indirizzo = re.search(r'<meta property="og:url" content="([^"]+)"', testo)
    if not (icona and titolo and indirizzo):
        sys.exit('la pagina non porta icona, titolo e og:url dove lo strumento li cerca')
    svg = icona.group(0)
    svg = svg.replace('var(--accent)', ACCENTO).replace('var(--glyph)', GLIFO)
    # ⚠️ La misura la decide il foglio di stile qui sotto, non gli attributi della pagina.
    svg = re.sub(r' width="\d+" height="\d+"', '', svg, count=1)
    breve = re.sub(r'^https?://', '', indirizzo.group(1)).rstrip('/')
    return svg, titolo.group(1).strip(), breve


def composizione(svg, titolo, breve):
    """La scena, in HTML: a sinistra icona, titolo e indirizzo, a destra la schermata."""
    righe = righe_titolo(titolo)
    return f'''<!doctype html>
<html lang="it"><head><meta charset="utf-8">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&display=block">
<style>
  html, body {{ margin: 0; }}
  body {{
    width: {LARGO}px; height: {ALTO}px; overflow: hidden; position: relative;
    background: {FONDO}; color: {INCHIOSTRO}; font-family: Roboto, sans-serif;
  }}
  .parole {{
    position: absolute; left: 88px; top: 0; bottom: 0; width: 620px;
    display: flex; flex-direction: column; justify-content: center; gap: 34px;
  }}
  .icona {{ width: 136px; height: 136px; border-radius: 24%; display: block; }}
  h1 {{
    font-size: 82px; line-height: 1.02; letter-spacing: -0.02em; font-weight: 500; margin: 0;
  }}
  .indirizzo {{ font-size: 30px; color: {MUTO}; }}
  .schermata {{
    position: absolute; left: 772px; top: 48px; width: 340px; display: block;
    border-radius: 26px; border: 1px solid {FILO}; box-shadow: 0 12px 30px rgba(0, 0, 0, .15);
  }}
</style></head><body>
<div class="parole">
{svg}
<h1>{righe}</h1>
<div class="indirizzo">{html.escape(breve)}</div>
</div>
<img class="schermata" src="{SCHERMATA.as_uri()}" alt="">
</body></html>
'''


def main():
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        sys.exit('manca Playwright: senza browser non si compone niente')
    if not SCHERMATA.exists():
        sys.exit('manca la schermata: %s' % SCHERMATA)

    svg, titolo, breve = pezzi(PAGINA.read_text(encoding='utf-8'))
    with tempfile.TemporaryDirectory() as cartella:
        scena = pathlib.Path(cartella) / 'scena.html'
        scena.write_text(composizione(svg, titolo, breve), encoding='utf-8')
        with sync_playwright() as p:
            b = avvia(p)
            pg = b.new_page(viewport={'width': LARGO, 'height': ALTO}, device_scale_factor=1)
            pg.goto(scena.as_uri())
            fotografa(pg, USCITA, ['500', '400'], type='jpeg', quality=90)
            b.close()
    print('%s: %dx%d, %d byte' % (USCITA.relative_to(RADICE), LARGO, ALTO, USCITA.stat().st_size))


if __name__ == '__main__':
    main()
