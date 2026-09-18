#!/usr/bin/env python3
"""Gli spigoli esterni di un'icona: quali sono ancora vivi, e con che raggio si raccordano.

⚠️⚠️ **NASCE NELLA `2.50`, ED E UNA SUA ISTRUZIONE** (2026-09-14, sulla voce `geo-glifo`: *mi
pare che manchi l'arrotondamento di 0,4px sugli spigoli netti. Impostalo come regola del progetto
d'ora in avanti per tutti gli SVG che ti passo o crei in autonomia. Verifica anche le altre icone
esistenti*). Fino alla `2.40` quel trattamento si faceva a mano, in sessione, sui soli glifi che
nascevano qui: scritto cosi, era una buona intenzione che nessuno poteva verificare.

⚠️⚠️ **VERIFICA E NON RISCRIVE, ED E UNA SCELTA MISURATA**: la prima stesura aveva un `--fix`, e
sul segno di spunta ha raccordato il vertice sbagliato (quello dove il giro si chiude invece
della punta), lasciando una quadratica lunga un millesimo di unita. Un tracciato riscritto male
non da nessun errore e si vede solo guardando, quindi lo strumento dice DOVE manca un raccordo e
con che raggio, e il raccordo lo fa chi disegna. E' lo stesso patto di `icon-check.py`: misura e
dichiara.

⚠️⚠️ **CHE COSA E UNO SPIGOLO ESTERNO, E LE CONDIZIONI SONO DUE**: convesso **e** esterno. Convesso
vuol dire che il giunto svolta nel verso del proprio sottotracciato, cioe una punta che sporge, e
un angolo che rientra e un raccordo interno. Esterno vuol dire che quel sottotracciato e un
contorno del disegno e non un **buco**: la profondita di contenimento dice quale dei due e, e solo
i sottotracciati a profondita pari portano punte.

⚠️⚠️ **LA SECONDA CONDIZIONE E SUA E ARRIVA DOPO LA PRIMA STESURA** (nota su `d-spigoli-icone`,
giro della `2.50`: *la regola va affinata: solo gli angoli convessi esterni (le 'punte')*). Fino a
quel giorno contava anche gli angoli di un buco, perche sporgono verso l'inchiostro: una punta pero
e convessa **e** esterna, e la seconda meta l'angolo di un buco non ce l'ha. Il conto e sceso da
138 a 87, e i disegni interessati da venti a quindici.

⚠️ **E il 142 scritto nella `2.50` non e piu il numero del criterio largo**: oggi quello stesso
criterio ne conta 138, perche i disegni sono cambiati nel frattempo. Chi confronta i due numeri
confronti prima le due date.

⚠️⚠️ **IL RAGGIO NON E COSTANTE, LO E QUANTO IL VERTICE ARRETRA**: con 0,4 fisso un angolo di 30
gradi arretra di 1,15 unita su 24 (il 5% della tela) contro le 0,17 di un angolo retto, e le punte
vengono tozze. Il tetto e l'arretramento dell'angolo retto, quindi sugli spigoli retti, che sono
quasi tutti, il raggio resta 0,4 esatto.

⚠️ **Avvisa e non blocca**, come le misure di resa di `icon-check.py`: gli spigoli vivi di oggi
vivono in disegni gia approvati, e toglierli e una decisione di chi li ha fatti.

⚠️⚠️ **E DALLA `2.56` I DISEGNI SONO RACCORDATI, TRANNE UNO: IL LOGO PERSONALE NON SI TOCCA MAI**
(sua istruzione, 2026-09-18, risposta `alcune` a `d-punte-adesso`: *arrotondale tutte, tranne
`ic_tian`: il mio logo personale non si tocca MAI*). Quell'esclusione non e tecnica e non si
ricava da nessuna misura, quindi vive scritta qui: il verificatore lo **dichiara** escluso invece
di contarne le punte, perche un numero accanto al suo nome si legge come un lavoro da fare.

Modi:
  icon-round.py                elenca quante punte vive ha ogni disegno di `res/drawable`
  icon-round.py -v [FILE ...]  scrive anche dove sono, con l'angolo e il raggio da usare
"""
import importlib.util
import math
import pathlib
import sys
import xml.etree.ElementTree as ET

QUI = pathlib.Path(__file__).resolve().parent
RADICE = QUI.parent
DRAWABLE = RADICE / 'app/src/main/res/drawable'
ANDROID = 'http://schemas.android.com/apk/res/android'

# ⚠️ Il parser dei tracciati e quello del verificatore, non una seconda copia: due letture della
# stessa grammatica divergono al primo ritocco, e quella che sbaglia e quella che nessuno lancia.
_spec = importlib.util.spec_from_file_location('icon_check', QUI / 'icon-check.py')
_check = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_check)

# Il raggio nominale, sulla griglia di Material da 24: e la misura che lui da alle sue icone.
RAGGIO = 0.4
# Quanto arretra il vertice di un angolo retto raccordato con quel raggio: e il tetto.
TETTO = RAGGIO * (1 / math.sin(math.radians(45)) - 1)
# Sotto questo angolo (in gradi) due segmenti si leggono come uno solo, e non c'e nessuno spigolo.
DRITTO = 6.0
# Un segmento piu corto di questo non ha da dove arretrare.
CORTO = 1e-3
# ⚠️⚠️ **IL LOGO PERSONALE NON SI TOCCA MAI, ED E UNA SUA REGOLA**: non e un'esclusione tecnica,
# quindi non si ricava da nessuna misura e resta scritta qui, come in testa al file stesso.
INTOCCABILI = {'ic_tian.xml'}
# ⚠️⚠️ **QUANTO LONTANO PUO CADERE UN GIRO DAL PROPRIO PUNTO DI PARTENZA E RESTARE CHIUSO**: i
# tracciati sono scritti a due decimali, quindi un giro arrotondato torna a un centesimo di unita
# dal punto della `M` e la chiusura implicita diventa una rettina che svolta. Misurato su
# `ic_apply`, dove quel residuo lungo 0,014 unita si leggeva come uno spigolo vivo a 135 gradi.
CHIUSA = 0.05


def assoluti(comandi):
    """I comandi in coordinate assolute, uno per punto, con le curve tenute come sono."""
    fuori = []
    x = y = 0.0
    sx = sy = 0.0
    for lettera, n in comandi:
        su = lettera.upper()
        rel = lettera.islower()
        if su == 'Z':
            fuori.append(('Z', []))
            x, y = sx, sy
            continue
        if su == 'M':
            x, y = (x + n[0], y + n[1]) if rel else (n[0], n[1])
            sx, sy = x, y
            fuori.append(('M', [x, y]))
        elif su == 'L':
            x, y = (x + n[0], y + n[1]) if rel else (n[0], n[1])
            fuori.append(('L', [x, y]))
        elif su == 'H':
            x = x + n[0] if rel else n[0]
            fuori.append(('L', [x, y]))
        elif su == 'V':
            y = y + n[0] if rel else n[0]
            fuori.append(('L', [x, y]))
        elif su == 'A':
            # ⚠️ Di un arco solo gli ultimi due numeri sono un punto: i raggi, la rotazione e le
            # due bandierine non si spostano con l'origine.
            fatti = list(n[0:5])
            ax, ay = (x + n[5], y + n[6]) if rel else (n[5], n[6])
            fatti.extend([ax, ay])
            x, y = ax, ay
            fuori.append(('A', fatti))
        else:
            # Curve: si portano in assoluto punto per punto e non si toccano.
            fatti = []
            for i in range(0, len(n) - 1, 2):
                ax, ay = n[i], n[i + 1]
                if rel:
                    ax, ay = x + ax, y + ay
                fatti.extend([ax, ay])
            x, y = fatti[-2], fatti[-1]
            fuori.append((su, fatti))
    return fuori


def pezzi(comandi):
    """I sottotracciati: ognuno e la lista dei suoi comandi assoluti."""
    fuori = []
    for c in comandi:
        if c[0] == 'M' or not fuori:
            fuori.append([])
        fuori[-1].append(c)
    return fuori


def punti(sotto):
    """I vertici di un sottotracciato, cioe il punto finale di ogni comando."""
    fuori = []
    for lettera, n in sotto:
        if lettera == 'Z' or not n:
            continue
        fuori.append((n[-2], n[-1]))
    return fuori


def area(ps):
    """Il doppio dell'area con segno: dice in che verso gira il sottotracciato."""
    s = 0.0
    for i in range(len(ps)):
        x1, y1 = ps[i]
        x2, y2 = ps[(i + 1) % len(ps)]
        s += x1 * y2 - x2 * y1
    return s


def dentro(punto, ps):
    """Se un punto cade dentro il poligono dei vertici, col conto dei raggi."""
    x, y = punto
    interno = False
    for i in range(len(ps)):
        x1, y1 = ps[i]
        x2, y2 = ps[(i + 1) % len(ps)]
        if (y1 > y) != (y2 > y) and x < x1 + (y - y1) * (x2 - x1) / (y2 - y1):
            interno = not interno
    return interno


def punto_dentro(ps):
    """Un punto sicuramente interno al poligono: il lato piu lungo, scostato verso l'interno.

    ⚠️ **Non si prende un VERTICE**, che e proprio il posto in cui due giri che si toccano cadono
    l'uno sul bordo dell'altro, e la un conto dei raggi risponde a caso. Il mezzo di un lato
    scostato di un millesimo cade dentro, e lontano da ogni altro contorno.
    """
    verso = 1.0 if area(ps) > 0 else -1.0
    lungo, quale = 0.0, 0
    for i in range(len(ps)):
        x1, y1 = ps[i]
        x2, y2 = ps[(i + 1) % len(ps)]
        d = math.hypot(x2 - x1, y2 - y1)
        if d > lungo:
            lungo, quale = d, i
    if lungo < CORTO:
        return ps[0]
    x1, y1 = ps[quale]
    x2, y2 = ps[(quale + 1) % len(ps)]
    dx, dy = (x2 - x1) / lungo, (y2 - y1) / lungo
    # La normale che punta verso l'interno dipende dal verso del giro.
    nx, ny = -dy * verso, dx * verso
    passo = min(lungo / 4, 1e-3)
    return ((x1 + x2) / 2 + nx * passo, (y1 + y2) / 2 + ny * passo)


def contorni(sottos):
    """I sottotracciati che sono un contorno del disegno, cioe non un buco.

    Si conta in quanti altri sottotracciati ognuno e contenuto: profondita pari vuol dire contorno,
    dispari vuol dire buco, e un'isola dentro un buco torna a essere un contorno.
    """
    poligoni = [punti(s) for s in sottos]
    fuori = []
    for i, ps in enumerate(poligoni):
        if len(ps) < 3:
            continue
        p = punto_dentro(ps)
        giri = sum(1 for j, altro in enumerate(poligoni)
                   if j != i and len(altro) >= 3 and dentro(p, altro))
        if giri % 2 == 0:
            fuori.append(sottos[i])
    return fuori


def tangenti_arco(p0, n):
    """Le due tangenti di un arco SVG: (entrante nel punto finale, uscente dal punto iniziale).

    ⚠️⚠️ **SENZA QUESTO CONTO UN RACCORDO GIA FATTO SI LEGGE COME UNO SPIGOLO VIVO**: prendendo la
    corda al posto della tangente, un arco di novanta gradi sbaglia di quarantacinque, cioe
    esattamente quanto basta a far sembrare vivo un angolo appena smussato. Il conto e quello del
    W3C, dagli estremi al centro.
    """
    rx, ry, rot, fa, fs, x2, y2 = n
    x1, y1 = p0
    phi = math.radians(rot)
    rx, ry = abs(rx), abs(ry)
    if rx < CORTO or ry < CORTO:
        d = (x2 - x1, y2 - y1)
        return d, d
    dx2, dy2 = (x1 - x2) / 2, (y1 - y2) / 2
    x1p = math.cos(phi) * dx2 + math.sin(phi) * dy2
    y1p = -math.sin(phi) * dx2 + math.cos(phi) * dy2
    lam = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry)
    if lam > 1:
        rx *= math.sqrt(lam)
        ry *= math.sqrt(lam)
    su = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
    giu = rx * rx * y1p * y1p + ry * ry * x1p * x1p
    fatt = math.sqrt(max(0.0, su / giu)) if giu > 0 else 0.0
    if (fa > 0.5) == (fs > 0.5):
        fatt = -fatt
    cxp = fatt * rx * y1p / ry
    cyp = -fatt * ry * x1p / rx
    t1 = math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx)
    t2 = math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx)
    dt = t2 - t1
    if fs > 0.5 and dt < 0:
        dt += 2 * math.pi
    if fs < 0.5 and dt > 0:
        dt -= 2 * math.pi
    verso = 1.0 if dt > 0 else -1.0

    def tang(t):
        ax = -rx * math.sin(t) * verso
        ay = ry * math.cos(t) * verso
        return (math.cos(phi) * ax - math.sin(phi) * ay, math.sin(phi) * ax + math.cos(phi) * ay)

    return tang(t1 + dt), tang(t1)


def versi(sotto):
    """Per ogni vertice: la tangente con cui ci si arriva e quella con cui si riparte.

    ⚠️⚠️ **LE CURVE SMOOTH (`S` E `T`) SONO TANGENTI PER COSTRUZIONE, E TRATTARLE COME LE ALTRE
    DA 26 SPIGOLI VIVI SU UN DISEGNO CHE NON NE HA NESSUNO**: il loro primo controllo e la
    riflessione dell'ultimo controllo della curva prima, quindi la tangente con cui partono e
    esattamente quella con cui si e arrivati. Misurato su `ic_folder_eye`, che e tutto di `s`.
    """
    corpo = [c for c in sotto if c[0] != 'Z']
    ps = punti(sotto)
    dentro, fuori = [], []
    for i, (lettera, n) in enumerate(corpo):
        prima = ps[(i - 1) % len(ps)]
        qui = ps[i]
        if lettera == 'Q':
            dentro.append((qui[0] - n[0], qui[1] - n[1]))
            fuori.append((n[0] - prima[0], n[1] - prima[1]))
        elif lettera == 'C':
            dentro.append((qui[0] - n[2], qui[1] - n[3]))
            fuori.append((n[0] - prima[0], n[1] - prima[1]))
        elif lettera == 'S':
            dentro.append((qui[0] - n[0], qui[1] - n[1]))
            fuori.append(dentro[i - 1] if i > 0 else (qui[0] - prima[0], qui[1] - prima[1]))
        elif lettera == 'T':
            d = dentro[i - 1] if i > 0 else (qui[0] - prima[0], qui[1] - prima[1])
            cx, cy = prima[0] + d[0], prima[1] + d[1]
            dentro.append((qui[0] - cx, qui[1] - cy))
            fuori.append(d)
        elif lettera == 'A':
            a, b = tangenti_arco(prima, n)
            dentro.append(a)
            fuori.append(b)
        else:
            d = (qui[0] - prima[0], qui[1] - prima[1])
            dentro.append(d)
            fuori.append(d)
    # ⚠️ Un giro che torna sul proprio punto di partenza ha DUE vertici sullo stesso punto: la
    # chiusura e lunga zero, e senza questa riga lo spigolo vero fra i due sparisce.
    if len(ps) >= 3 and math.hypot(ps[-1][0] - ps[0][0], ps[-1][1] - ps[0][1]) < CHIUSA:
        dentro[0] = dentro[-1]
        fuori[0] = fuori[1]
    return dentro, fuori


def raggio_per(dentro, scala):
    """Il raggio che spetta a uno spigolo di quell'angolo interno, col tetto sull'arretramento."""
    mezzo = math.radians(dentro / 2)
    if math.sin(mezzo) <= 0:
        return 0.0
    passo = 1 / math.sin(mezzo) - 1
    r = RAGGIO * scala
    if r * passo > TETTO * scala:
        r = TETTO * scala / passo
    return r


def spigoli(sotto, scala):
    """Gli spigoli esterni vivi di un sottotracciato: (punto, angolo interno, raggio)."""
    if not any(c[0] == 'Z' for c in sotto):
        return []
    ps = punti(sotto)
    if len(ps) < 3:
        return []
    verso = 1.0 if area(ps) > 0 else -1.0
    entra, esce = versi(sotto)
    doppio = math.hypot(ps[-1][0] - ps[0][0], ps[-1][1] - ps[0][1]) < CHIUSA * scala
    fuori = []
    for i in range(len(ps)):
        if doppio and i == len(ps) - 1:
            # Lo stesso punto del vertice 0, che porta gia quello spigolo.
            continue
        # ⚠️ Un tratto piu corto della tolleranza di chiusura non e un lato: e il residuo di un
        # arrotondamento, e raccordarlo vorrebbe dire smussare un difetto di scrittura.
        # ⚠️⚠️ **MA IL VERTICE DELLA `M` FA ECCEZIONE QUANDO IL GIRO TORNA ESATTAMENTE LÌ, E
        # SENZA QUESTA RIGA NON SI GUARDAVA MAI** (misurato il 2026-09-18 su `BookmarkAdd`, il
        # segnalibro col più: dieci punte contate su dodici vere). Il lato che lo precede è la
        # chiusura, lunga zero, quindi la guardia qui sotto lo saltava sempre; il lato vero è
        # quello che arriva al penultimo punto, cioè allo stesso punto. I versi `versi()` li
        # aggiusta già (vedi la nota sul giro doppio): mancava solo di non saltarlo.
        prima = ps[i - 1] if i > 0 else (ps[-2] if doppio and len(ps) >= 3 else ps[-1])
        dopo = ps[(i + 1) % len(ps)]
        if (math.hypot(ps[i][0] - prima[0], ps[i][1] - prima[1]) < CHIUSA * scala
                or math.hypot(dopo[0] - ps[i][0], dopo[1] - ps[i][1]) < CHIUSA * scala):
            continue
        # ⚠️ **L'angolo si misura sulle TANGENTI e non sulle corde**: un raccordo gia fatto e un
        # arco, e la sua corda svolta di meta arco, cioe quanto basta a farlo sembrare vivo.
        ax, ay = entra[i]
        bx, by = esce[(i + 1) % len(ps)]
        la = math.hypot(ax, ay)
        lb = math.hypot(bx, by)
        if la < CORTO or lb < CORTO:
            continue
        cross = (ax * by - ay * bx) / (la * lb)
        dot = (ax * bx + ay * by) / (la * lb)
        gira = math.degrees(math.atan2(abs(cross), dot))
        if gira < DRITTO:
            continue
        if cross * verso <= 0:
            # Svolta verso l'interno: e un raccordo interno, non un bordo esterno.
            continue
        dentro = 180.0 - gira
        r = raggio_per(dentro, scala)
        if r > 0:
            fuori.append((ps[i], dentro, r))
    return fuori


def lavora(percorso, dettaglio):
    """Guarda un file di disegno e torna quanti spigoli esterni vivi ha."""
    albero = ET.fromstring(percorso.read_text(encoding='utf-8'))
    lato = albero.get(f'{{{ANDROID}}}viewportWidth')
    scala = float(lato) / 24.0 if lato else 1.0
    vivi = []
    # ⚠️ Gli elementi non portano il namespace di Android: quello sta sugli ATTRIBUTI.
    for path in albero.iter('path'):
        d = path.get(f'{{{ANDROID}}}pathData')
        if not d:
            continue
        comandi, errore, _ = _check.leggi_tracciato(d)
        if errore:
            print(f'{percorso.name}: tracciato illeggibile ({errore})')
            continue
        # ⚠️ Il contenimento si guarda DENTRO un `<path>` e non fra path diversi, perche e la
        # regola di riempimento di quel path a decidere che cosa e un buco.
        for sotto in contorni(pezzi(assoluti(comandi))):
            vivi.extend(spigoli(sotto, scala))
    if vivi:
        print(f'{percorso.name}: {len(vivi)} punte vive')
        if dettaglio:
            for (px, py), dentro, r in vivi:
                print(f'    {px:7.3f},{py:<7.3f}  angolo {dentro:6.1f}  raggio {r:.3f}')
    return len(vivi)


def main():
    argomenti = sys.argv[1:]
    dettaglio = '-v' in argomenti
    nomi = [a for a in argomenti if not a.startswith('-')]
    file = [pathlib.Path(n) for n in nomi] if nomi else sorted(DRAWABLE.glob('*.xml'))
    fuori = [f for f in file if f.name in INTOCCABILI]
    guardati = [f for f in file if f.name not in INTOCCABILI]
    totale = sum(lavora(f, dettaglio) for f in guardati)
    print(f'{len(guardati)} disegni, {totale} punte vive')
    for f in fuori:
        print(f'{f.name}: escluso per regola, il logo personale non si tocca')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
