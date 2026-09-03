#!/usr/bin/env python3
"""Verifica le icone vettoriali di AIV: grammatica, vincoli Android, linee guida Material.

Perche esiste: un'icona entra in questo repository come tracciato, e da quel momento nessuno
la guarda piu. Quello che la conversione non sa fare lo perde in silenzio, e quello che un
parser accetta per indulgenza si rompe il giorno che cambia il parser. Qui la domanda 'e fatta
bene?' ha una risposta con dei numeri.

Uso: python3 tools/icon-check.py                  (tutte le icone del repository)
     python3 tools/icon-check.py FILE.xml [...]   (solo quelle)
Esce 1 se trova un difetto che blocca, 0 se e tutto in ordine.

⚠️ I CONTROLLI SONO DI DUE SPECIE, e la differenza e dichiarata a ogni giro:
  - quelli di GRAMMATICA e di VINCOLO girano sempre, perche usano la sola libreria standard;
  - quelli di RESA (l'inchiostro, le linee guida, l'avvolgimento, la cucitura) vogliono
    Chromium e Playwright, e se non ci sono si salta DICENDOLO. Un controllo che tace quello
    che non ha guardato mente peggio di uno che non esiste.
"""
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

RADICE = pathlib.Path(__file__).resolve().parent.parent
DRAWABLE = RADICE / 'app/src/main/res/drawable'
ANDROID = '{http://schemas.android.com/apk/res/android}'

# ── La grammatica dei tracciati ───────────────────────────────────────────────────────────
# ⚠️⚠️ IL TOKENIZZATORE E SCRITTO A MANO DI PROPOSITO, e non e una ruota reinventata: e IL
# controllo. Il parser di Android e indulgente e accetta forme che la grammatica SVG ammette a
# stento (due numeri incollati quando il secondo comincia col punto, `-.05.1`) e altre che non
# ammette affatto. Un tracciato che passa da qui e leggibile da QUALUNQUE parser conforme,
# che e la definizione operativa di 'a prova di futuro'. Appoggiarsi a una libreria
# significherebbe misurare l'indulgenza di quella libreria invece della grammatica.
COMANDI = {'M': 2, 'L': 2, 'H': 1, 'V': 1, 'C': 6, 'S': 4, 'Q': 4, 'T': 2, 'A': 7, 'Z': 0}
RE_NUMERO = re.compile(r'[-+]?(?:\d+\.\d*|\.\d+|\d+)(?:[eE][-+]?\d+)?')
# ⚠️ Un punto che segue un numero che ha GIA il punto apre un numero nuovo: `.05.1` sono due.
# E SVG legale (la grammatica non ammette due punti in un numero, quindi il secondo chiude il
# primo), Android lo legge, e un parser che pretende un separatore lo rifiuta. Si segnala e non
# si blocca: il tracciato e valido, e solo scomodo.
RE_INCOLLATI = re.compile(r'(\d*\.\d+)(?=\.)')


def separa(d):
    """La stessa `d` con una virgola fra due numeri incollati: nessun valore cambia."""
    return RE_INCOLLATI.sub(lambda m: m.group(1) + ',', d)


def leggi_tracciato(d):
    """I comandi di `d`, oppure un errore. Torna `(comandi, errore, incollati)`.

    Ogni comando e `(lettera, [numeri])`. `incollati` conta le coppie di numeri senza
    separatore, che sono legali ma non portabili.
    """
    incollati = len(RE_INCOLLATI.findall(d))
    testo = separa(d)
    fuori = []
    i = 0
    lettera = None
    n = len(testo)
    while i < n:
        c = testo[i]
        if c in ' \t\r\n,':
            i += 1
            continue
        if c.isalpha():
            if c.upper() not in COMANDI:
                return None, f"comando '{c}' che non esiste in SVG", incollati
            lettera = c
            i += 1
            quanti = COMANDI[c.upper()]
            if quanti == 0:
                fuori.append((c, []))
                lettera = None
            continue
        if lettera is None:
            return None, f'numeri senza un comando davanti, alla colonna {i + 1}', incollati
        quanti = COMANDI[lettera.upper()]
        numeri = []
        while len(numeri) < quanti:
            while i < n and testo[i] in ' \t\r\n,':
                i += 1
            m = RE_NUMERO.match(testo, i)
            if not m:
                return (None, f"il comando '{lettera}' vuole {quanti} numeri e alla colonna "
                        f'{i + 1} non ce n-e uno', incollati)
            numeri.append(float(m.group(0)))
            i = m.end()
        fuori.append((lettera, numeri))
        # ⚠️ Dopo il primo gruppo, `M` diventa `L` e `m` diventa `l`: e la regola SVG, e senza
        # di essa un tracciato con un `M` seguito da quattro numeri sembrerebbe rotto.
        if lettera == 'M':
            lettera = 'L'
        elif lettera == 'm':
            lettera = 'l'
    return fuori, None, incollati


def sottotracciati(comandi):
    """Quanti sottotracciati: uno per ogni comando di spostamento."""
    return sum(1 for lettera, _ in comandi if lettera in 'Mm')


# ── I vincoli del vettore Android ─────────────────────────────────────────────────────────
# ⚠️ Quello che il formato NON conosce va dichiarato qui, o passa di straforo: maschere,
# filtri, modalita di fusione, `<use>`, testo, campiture a motivo. Non esistono come elementi,
# quindi chi ne trova uno ha un file che Android non sa disegnare.
ELEMENTI_OK = {'vector', 'group', 'path', 'clip-path', 'aapt:attr', 'gradient', 'item'}
ATTRIBUTI_PATH_OK = {
    'name', 'pathData', 'fillColor', 'fillType', 'fillAlpha',
    'strokeColor', 'strokeWidth', 'strokeAlpha', 'strokeLineCap', 'strokeLineJoin',
    'strokeMiterLimit', 'trimPathStart', 'trimPathEnd', 'trimPathOffset',
}


def controlla_xml(percorso):
    """I difetti di un vettore Android: (blocca, avvisa, dati)."""
    blocca, avvisa = [], []
    testo = percorso.read_text(encoding='utf-8')
    try:
        radice = ET.fromstring(testo)
    except ET.ParseError as e:
        return [f'XML non valido: {e}'], [], {}

    tag = radice.tag.split('}')[-1]
    if tag != 'vector':
        blocca.append(f"la radice e '{tag}' invece di 'vector'")

    def num(elemento, nome):
        v = elemento.get(ANDROID + nome)
        if v is None:
            return None
        m = re.match(r'^(-?[\d.]+)(dp|px)?$', v.strip())
        return float(m.group(1)) if m else None

    larghezza, altezza = num(radice, 'width'), num(radice, 'height')
    vw, vh = num(radice, 'viewportWidth'), num(radice, 'viewportHeight')
    if vw is None or vh is None:
        blocca.append('manca viewportWidth o viewportHeight')
    if larghezza is None or altezza is None:
        blocca.append('manca android:width o android:height')

    # ⚠️ IL RAPPORTO FRA MISURA E TELA si riporta e non si giudica. Dove un'unita vale un dp la
    # famiglia ha la stessa scala ottica (ed e la ragione per cui il glifo che esce dalla tela
    # cresce di 1 in tutti e tre i numeri: vedi `Menus.kt`); dove la tela e grande di proposito
    # (800 per un disegno arrivato da Illustrator) il rapporto e un altro e va bene. Un avviso
    # su ognuna delle due sarebbe rumore su meta delle icone.
    rapporto = None
    if None not in (larghezza, vw) and vw:
        rapporto = larghezza / vw

    tracciati, gruppi = [], []
    for elemento in radice.iter():
        nome = elemento.tag.split('}')[-1]
        if nome not in ELEMENTI_OK:
            blocca.append(f"l-elemento '{nome}' non esiste nel vettore Android: "
                          f'maschere, filtri, fusioni e testo non si convertono, si espandono')
            continue
        if nome == 'path':
            tracciati.append(elemento)
            for chiave in elemento.attrib:
                corto = chiave.split('}')[-1]
                if corto not in ATTRIBUTI_PATH_OK:
                    avvisa.append(f"attributo '{corto}' su un path: non e fra quelli che il "
                                  f'formato usa')
        elif nome == 'group':
            gruppi.append(elemento)

    if not tracciati:
        blocca.append('nessun tracciato')

    # La trasformazione dei gruppi, che serve a rendere il vettore come Android lo rende.
    trasla = [0.0, 0.0]
    scala = [1.0, 1.0]
    for g in gruppi:
        for chiave, dove, indice in (('translateX', trasla, 0), ('translateY', trasla, 1),
                                     ('scaleX', scala, 0), ('scaleY', scala, 1)):
            v = num(g, chiave)
            if v is None:
                continue
            if dove is trasla:
                dove[indice] += v
            else:
                dove[indice] *= v

    dati = {'width': larghezza, 'viewport': (vw, vh), 'gruppi': len(gruppi), 'path': [],
            'trasforma': {'trasla': tuple(trasla), 'scala': tuple(scala)}}
    for p in tracciati:
        d = p.get(ANDROID + 'pathData')
        if not d:
            blocca.append('un path senza pathData')
            continue
        comandi, errore, incollati = leggi_tracciato(d)
        if errore:
            blocca.append(f'tracciato fuori grammatica: {errore}')
            continue
        tratto = p.get(ANDROID + 'strokeWidth') is not None
        riempimento = p.get(ANDROID + 'fillType') or 'nonZero'
        if incollati:
            avvisa.append(f'{incollati} coppie di numeri senza separatore: legale e letto da '
                          f'Android, rifiutato da un parser stretto. Costa {incollati} byte '
                          f'separarle')
        dati['path'].append({
            'd': d, 'byte': len(d), 'comandi': len(comandi),
            'sottotracciati': sottotracciati(comandi), 'incollati': incollati,
            'tratto': tratto, 'fillType': riempimento,
        })

    # ⚠️⚠️ UN GRUPPO DI SOLA TRASLAZIONE NON SI SEGNALA, ed e una correzione del 2026-09-03:
    # e il modo canonico di dire che il disegno ha un'origine, che un vettore Android non sa
    # dichiarare (`viewportWidth` e `viewportHeight` e nient'altro, quindi l'origine e sempre
    # 0,0). Prima qui si consigliava di appiattirlo nelle coordinate, e la misura ha detto due
    # volte no: appiattire `ic_aiv_mark` cambia 9 pixel su 230.400 con scarto 8, perche il
    # disegnatore somma in virgola mobile a 32 bit e non cade sullo stesso numero; e la
    # `pathData` smette di essere confrontabile carattere per carattere col file di partenza,
    # che e il solo modo di verificare un trasporto. Su tredici icone il consiglio scattava su
    # quattro legittime, cioe era rumore.
    # Quello che invece vale la pena di dire e altro: un gruppo che NON trasforma niente e un
    # livello a vuoto, e un gruppo che SCALA o RUOTA stacca i numeri del tracciato dal disegno
    # (li si legge e non dicono dove finisce l'inchiostro). Anche quello puo essere voluto: il
    # rientro del 65% dell'icona adattiva e una convenzione, non un residuo.
    for g in gruppi:
        mosse = sorted(k.split('}')[-1] for k in g.attrib if k.split('}')[-1] != 'name')
        if not mosse:
            avvisa.append('un gruppo senza trasformazioni: e un livello a vuoto, si appiattisce')
        elif any(m.startswith(('scale', 'rotation', 'pivot')) for m in mosse):
            avvisa.append(f'un gruppo con {", ".join(mosse)}: staccando i numeri dal disegno, '
                          f'legittimo solo se esprime una convenzione (il rientro '
                          f'dell-icona adattiva) e non un residuo di esportazione')
    if len(gruppi) > 1:
        avvisa.append(f'{len(gruppi)} gruppi: un livello solo basta a dichiarare un-origine')

    return blocca, avvisa, dati


# ── La resa: l'inchiostro, le linee guida, l'avvolgimento, la cucitura ────────────────────
PLAYWRIGHT = pathlib.Path('/opt/node22/lib/node_modules/playwright')
# Le linee guida Material per un'icona di sistema: tela 24, area viva 20 (2 di margine per
# lato), e quattro sagome di riferimento.
SAGOME = {'quadrato': 18.0, 'cerchio': 20.0, 'orizzontale': (20.0, 16.0), 'verticale': (16.0, 20.0)}


def resa(icone):
    """Le misure di resa, o None se Chromium non c-e. `icone` e una lista di (nome, svg)."""
    if not PLAYWRIGHT.exists():
        return None
    with tempfile.TemporaryDirectory() as tmp:
        cartella = pathlib.Path(tmp)
        for nome, svg in icone:
            (cartella / f'{nome}.svg').write_text(svg, encoding='utf-8')
        script = f'''const {{ chromium }} = require('{PLAYWRIGHT}');
const fs = require('fs');
(async () => {{
  const b = await chromium.launch();
  const p = await b.newPage({{ viewport: {{ width: 240, height: 240 }} }});
  const fuori = {{}};
  for (const n of {json.dumps([n for n, _ in icone])}) {{
    await p.goto('file://{cartella}/' + n + '.svg');
    await p.screenshot({{ path: '{cartella}/' + n + '.png' }});
    fuori[n] = await p.evaluate(() => {{
      const r = document.querySelector('svg > g').getBBox();
      return {{ x: r.x, y: r.y, w: r.width, h: r.height }};
    }});
  }}
  fs.writeFileSync('{cartella}/box.json', JSON.stringify(fuori));
  await b.close();
}})();
'''
        (cartella / 'r.js').write_text(script, encoding='utf-8')
        e = subprocess.run(['node', str(cartella / 'r.js')], capture_output=True, text=True)
        if e.returncode:
            return None
        box = json.loads((cartella / 'box.json').read_text())
        pixel = {}
        try:
            from PIL import Image
            import numpy as np
            for nome, _ in icone:
                a = np.asarray(Image.open(cartella / f'{nome}.png').convert('L'), dtype=int)
                pixel[nome] = a
        except Exception:
            pixel = {}
        return box, pixel


def svg_di(dati, regola=None, unisci=False):
    """La pagina SVG equivalente al vettore, per misurarne la resa.

    ⚠️⚠️ LA TRASFORMAZIONE DEL GRUPPO VA RIPORTATA, o la misura dell'inchiostro e falsa. Alla
    prima stesura questa funzione apriva un `<g>` vuoto: `ic_aiv_mark`, il cui gruppo trasla di
    -19, risultava con l'inchiostro fuori dalla tela di 6,51 unita. Un numero implausibile e
    l'unica spia che si ha, e va guardato invece che riportato.
    """
    vw, vh = dati['viewport']
    d = [p['d'] for p in dati['path']]
    regole = [regola or p['fillType'].lower() for p in dati['path']]
    if unisci:
        corpo = f'<path d="{" ".join(d)}" fill="#000" fill-rule="{regole[0]}"/>'
    else:
        corpo = ''.join(f'<path d="{x}" fill="#000" fill-rule="{r}"/>'
                        for x, r in zip(d, regole))
    pezzi = []
    if dati['trasforma']['scala'] != (1.0, 1.0):
        sx, sy = dati['trasforma']['scala']
        pezzi.append(f'scale({sx} {sy})')
    if dati['trasforma']['trasla'] != (0.0, 0.0):
        tx, ty = dati['trasforma']['trasla']
        pezzi.append(f'translate({tx} {ty})')
    dentro = f'<g transform="{" ".join(pezzi)}">{corpo}</g>' if pezzi else corpo
    # ⚠️⚠️ DUE GRUPPI ANNIDATI, e la ragione e nella specifica: `getBBox` torna i limiti nel
    # sistema di coordinate DELL'ELEMENTO, quindi ignora la trasformazione che l'elemento porta
    # su di se. Interrogando il gruppo che trasla si otterrebbero i limiti di prima della
    # traslazione, e su `ic_aiv_mark` (che trasla di -19) l'inchiostro risultava fuori dalla
    # tela di 6,51 unita. Il gruppo di fuori non trasforma niente, quindi i suoi limiti
    # includono la trasformazione di quello di dentro.
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" '
            f'viewBox="0 0 {vw} {vh}"><rect width="{vw}" height="{vh}" fill="#fff"/>'
            f'<g>{dentro}</g></svg>')


def sagoma(w, h):
    """A quale sagoma di riferimento Material corrisponde una misura, se a una."""
    for nome, misura in SAGOME.items():
        if isinstance(misura, tuple):
            if abs(w - misura[0]) < 0.51 and abs(h - misura[1]) < 0.51:
                return nome
        elif abs(w - misura) < 0.51 and abs(h - misura) < 0.51:
            return nome
    return None


def main():
    scelti = [a for a in sys.argv[1:] if not a.startswith('-')]
    file = [pathlib.Path(a) for a in scelti] if scelti else sorted(DRAWABLE.glob('*.xml'))
    if not file:
        print('nessuna icona da guardare')
        return 0

    rotti = 0
    da_rendere, dati_di = [], {}
    print(f'{"icona":<28}{"tela":>6}{"path":>6}{"sub":>5}{"byte":>7}{"incoll":>8}  esito')
    print('-' * 92)
    for f in file:
        blocca, avvisa, dati = controlla_xml(f)
        if dati.get('path'):
            vw = dati['viewport'][0]
            byte = sum(p['byte'] for p in dati['path'])
            sub = sum(p['sottotracciati'] for p in dati['path'])
            inc = sum(p['incollati'] for p in dati['path'])
            print(f'{f.name:<28}{vw:>6.0f}{len(dati["path"]):>6}{sub:>5}{byte:>7}{inc:>8}  '
                  f'{"DIFETTI" if blocca else "in ordine"}')
            da_rendere.append(f.stem)
            dati_di[f.stem] = dati
        else:
            print(f'{f.name:<28}{"":>6}{"":>6}{"":>5}{"":>7}{"":>8}  DIFETTI')
        for x in blocca:
            print(f'   !! {x}')
            rotti += 1
        for x in avvisa:
            print(f'   ~~ {x}')

    # ── La resa ──
    pagine = []
    for nome in da_rendere:
        d = dati_di[nome]
        pagine.append((f'{nome}--nz', svg_di(d, 'nonzero')))
        pagine.append((f'{nome}--eo', svg_di(d, 'evenodd')))
        if len(d['path']) > 1:
            pagine.append((f'{nome}--uno', svg_di(d, 'nonzero', unisci=True)))
    esito = resa(pagine) if pagine else None
    if esito is None:
        print('\n⚠️ Resa NON misurata: Chromium o Playwright non ci sono in questo ambiente. '
              'Restano fuori: inchiostro, linee guida Material, avvolgimento, cucitura.')
        return 1 if rotti else 0

    box, pixel = esito
    print(f'\n{"icona":<28}{"inchiostro a 24 (x, y, w, h)":<30}{"marg":>6}{"sagoma":>13}'
          f'{"verso":>7}{"cuc":>6}')
    print('-' * 92)
    for nome in da_rendere:
        d = dati_di[nome]
        vw = d['viewport'][0]
        b = box[f'{nome}--nz']
        scala = 24.0 / vw
        x0, y0 = b['x'] * scala, b['y'] * scala
        w, h = b['w'] * scala, b['h'] * scala
        marg = min(x0, y0, 24 - (x0 + w), 24 - (y0 + h))
        nomeSagoma = sagoma(w, h) or "-"

        def diverso(a, b2):
            if not pixel or a not in pixel or b2 not in pixel:
                return '?'
            import numpy as np
            return int((np.abs(pixel[a] - pixel[b2]) > 2).sum())

        # ⚠️⚠️ LA FRAGILITA E LA REGOLA IMPLICITA, non la dipendenza dalla regola. Un vettore
        # che DICHIARA `fillType="evenOdd"` dipende da quella regola per costruzione, ed e
        # giusto cosi: il disegno e definito. Fragile e il vettore che NON la dichiara, si
        # affida al `nonZero` di fabbrica, e cambierebbe aspetto se qualcuno la dichiarasse o
        # se un editor riordinasse i sottotracciati. Alla prima stesura questo controllo
        # segnalava `ic_aiv_mark`, che la dichiara: un avviso su un file corretto.
        dichiarata = any(p['fillType'].lower() == 'evenodd' for p in d['path'])
        verso = diverso(f'{nome}--nz', f'{nome}--eo')
        cuc = diverso(f'{nome}--nz', f'{nome}--uno') if len(d['path']) > 1 else '-'
        print(f'{nome:<28}{f"{x0:.2f}, {y0:.2f}, {w:.2f}, {h:.2f}":<30}{marg:>6.2f}'
              f'{nomeSagoma:>13}{str(verso):>7}{str(cuc):>6}')
        if isinstance(verso, int) and verso > 0 and not dichiarata:
            print(f'   ~~ la regola di riempimento NON e dichiarata e il disegno ne dipende '
                  f'({verso} pixel): scrivi fillType di proposito, o un editor che riordina i '
                  f'sottotracciati lo cambia senza che nessuno lo veda')
        if isinstance(cuc, int) and cuc > 0:
            print(f'   ~~ unire i tracciati costerebbe {cuc} pixel sulla cucitura: restano '
                  f'{len(d["path"])} elementi <path>')
        if marg < 2 - 1e-6 and marg > -1e-6:
            print(f'   ~~ margine {marg:.2f} dove Material chiede 2: legittimo se il disegno '
                  f'lo vuole, da sapere')
        if marg < -1e-6:
            print(f'   ~~ l-inchiostro ESCE dalla tela di {-marg:.2f}: voluto solo se la tela '
                  f'e cresciuta per permetterlo')

    print('\nverso = pixel oltre 2/255 fra nonZero ed evenOdd (0 = robusto)')
    print('cuc   = pixel che costerebbe unire i tracciati in uno (0 = si possono unire)')
    print('marg  = distanza minima dal bordo della tela, riportata alla griglia 24')
    if rotti:
        print(f'\nicon-check: {rotti} difetti che bloccano')
        return 1
    print('\nicon-check: tutto in ordine')
    return 0


if __name__ == '__main__':
    sys.exit(main())
