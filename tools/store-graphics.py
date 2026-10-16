#!/usr/bin/env python3
"""The two pictures the Google Play listing asks for: the 512 x 512 icon and the 1024 x 500
feature graphic.

⚠️⚠️ **His answer G1 to `d-store-grafica` (giro of the `5.80`)**: the agent prepares the icon and
the feature graphic from the drawings of the app, he takes the screenshots on the phone.

⚠️⚠️ **NOTHING IS REDRAWN: THE PIECES ARE THOSE OF `og-image.py`**, that is the icon `<svg>`, the
title and the light screenshot of the download page, read by the same functions. When one of them
changes, both tools are run again.
- ⚠️ **The icon is the whole square of the drawing, with no crop and no rounding**: Google applies
  its own mask to the listing icon, and an icon is used as it is (`rules/Roccobot.md`,
  § '🎨 Grafica').
- ⚠️ **The feature graphic keeps the words away from the edges**: Google may lay the play button
  over the middle and crop the sides on some devices, so the title stays in the left half and the
  screenshot in the right one, both inside a margin.

⚠️ **Real fonts or nothing**, as in `og-image.py`: if Roboto does not arrive the tool stops.

Usage, from the root of the repository:
    python3 tools/store-graphics.py

Author: Rocco Casadei, a.k.a. Roccobot
"""
import importlib.util
import pathlib
import sys
import tempfile

RADICE = pathlib.Path(__file__).resolve().parent.parent
USCITA = RADICE / 'docs' / 'store'

_spec = importlib.util.spec_from_file_location('og_image', RADICE / 'tools' / 'og-image.py')
og = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(og)

ICONA = 512
LARGO, ALTO = 1024, 500


def scena_icona(svg):
    """The icon alone, filling the square."""
    return f'''<!doctype html>
<html><head><meta charset="utf-8"><style>
  html, body {{ margin: 0; }}
  body {{ width: {ICONA}px; height: {ICONA}px; overflow: hidden; }}
  svg {{ width: {ICONA}px; height: {ICONA}px; display: block; }}
</style></head><body>{svg}</body></html>
'''


def scena_evidenza(svg, titolo):
    """Icon and title on the left, the light screenshot on the right."""
    righe = og.righe_titolo(titolo)
    return f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Roboto:wght@500&display=block">
<style>
  html, body {{ margin: 0; }}
  body {{
    width: {LARGO}px; height: {ALTO}px; overflow: hidden; position: relative;
    background: {og.FONDO}; color: {og.INCHIOSTRO}; font-family: Roboto, sans-serif;
  }}
  .parole {{
    position: absolute; left: 80px; top: 0; bottom: 0; width: 520px;
    display: flex; flex-direction: column; justify-content: center; gap: 28px;
  }}
  .icona {{ width: 112px; height: 112px; border-radius: 24%; display: block; }}
  h1 {{ font-size: 68px; line-height: 1.02; letter-spacing: -0.02em; font-weight: 500; margin: 0; }}
  .schermata {{
    position: absolute; left: 676px; top: 40px; width: 268px; display: block;
    border-radius: 22px; border: 1px solid {og.FILO}; box-shadow: 0 10px 26px rgba(0, 0, 0, .15);
  }}
</style></head><body>
<div class="parole">
{svg}
<h1>{righe}</h1>
</div>
<img class="schermata" src="{og.SCHERMATA.as_uri()}" alt="">
</body></html>
'''


def main():
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        sys.exit('Playwright is missing: without a browser nothing is composed')
    if not og.SCHERMATA.exists():
        sys.exit('the screenshot is missing: %s' % og.SCHERMATA)

    svg, titolo, _ = og.pezzi(og.PAGINA.read_text(encoding='utf-8'))
    USCITA.mkdir(parents=True, exist_ok=True)
    icona = USCITA / 'icon-512.png'
    evidenza = USCITA / 'feature-1024x500.png'
    with tempfile.TemporaryDirectory() as cartella:
        prima = pathlib.Path(cartella) / 'icona.html'
        seconda = pathlib.Path(cartella) / 'evidenza.html'
        # The icon on its own has no rounded border: the class of the page would add one.
        prima.write_text(scena_icona(svg.replace('class="icona"', '')), encoding='utf-8')
        seconda.write_text(scena_evidenza(svg, titolo), encoding='utf-8')
        with sync_playwright() as p:
            b = og.avvia(p)
            pg = b.new_page(viewport={'width': ICONA, 'height': ICONA}, device_scale_factor=1)
            pg.goto(prima.as_uri())
            pg.screenshot(path=str(icona), type='png')
            pg = b.new_page(viewport={'width': LARGO, 'height': ALTO}, device_scale_factor=1)
            pg.goto(seconda.as_uri())
            # The same check and capture as the link preview (og-image.py, `fotografa`).
            og.fotografa(pg, evidenza, ['500'], type='png')
            b.close()
    for f in (icona, evidenza):
        print('%s: %d bytes' % (f.relative_to(RADICE), f.stat().st_size))


if __name__ == '__main__':
    main()
