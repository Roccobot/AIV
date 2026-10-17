"""Il Chromium degli strumenti che aprono una pagina, cercato da tutti nello stesso modo.

⚠️⚠️ **UN MODO SOLO, dalla `6.30`** (rilievi G28, G30 e H31 della revisione Setaccio): prima ogni
strumento lo cercava a modo suo, chi nel `PATH`, chi nelle cartelle del container delle sessioni,
chi in un percorso fisso, e ognuno mancava una macchina. Sul Mac dell'utente gli strumenti delle
immagini dicevano che Chromium mancava, e su GitHub `feedback-check.py` non apriva la pagina.

L'ordine, dal più preciso al più generico:
1. quello di Playwright, se c'è davvero: sul Mac e su GitHub, dopo `playwright install`;
2. quello delle cartelle `chromium-*` sotto `PLAYWRIGHT_BROWSERS_PATH`: nel container delle
   sessioni Playwright chiede una build che non c'è, e scaricarne un'altra è vietato;
3. `/opt/pw-browsers/chromium`, il collegamento del container;
4. quello del `PATH`.

Uso, da uno strumento della stessa cartella:
    _spec = importlib.util.spec_from_file_location('browser', Path(__file__).with_name('browser.py'))

Author: Rocco Casadei, a.k.a. Roccobot
"""
import os
import shutil


def _cartelle():
    radice = os.environ.get('PLAYWRIGHT_BROWSERS_PATH') or '/opt/pw-browsers'
    if not os.path.isdir(radice):
        return
    for nome in sorted(os.listdir(radice), reverse=True):
        if nome.startswith('chromium-'):
            yield os.path.join(radice, nome, 'chrome-linux', 'chrome')
    yield os.path.join(radice, 'chromium')


def percorso(pw=None):
    """Il percorso del browser, o None se non ce n'è nessuno.

    `pw` è il Playwright già aperto da chi chiama; senza, se ne apre uno solo per chiedergli dove
    tiene il suo browser (dentro un `sync_playwright` aperto non se ne può aprire un secondo)."""
    candidati = []
    try:
        if pw is not None:
            candidati.append(pw.chromium.executable_path)
        else:
            from playwright.sync_api import sync_playwright
            with sync_playwright() as aperto:
                candidati.append(aperto.chromium.executable_path)
    except Exception:
        # Senza Playwright restano le altre tre vie: chi chiama dice da sé che cosa gli manca.
        pass
    candidati.extend(_cartelle())
    candidati.extend(shutil.which(n) for n in ('chromium', 'google-chrome'))
    return next((c for c in candidati if c and os.path.isfile(c)), None)
