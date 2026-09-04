#!/usr/bin/env python3
"""Controlla le traduzioni contro l'inglese. Si lancia dalla radice del repo:

    python3 tools/i18n-check.py

Che cosa guarda, e perché ognuna di queste cose è un difetto che non si vede provando
l'app nella propria lingua:

- CHIAVI MANCANTI: una stringa che esiste in `values/` e non in `values-fr/` fa comparire
  l'inglese in mezzo al francese. Lint lo segnala come errore, ma solo quando qualcuno
  compila una release;
- SEGNAPOSTO: un `%1$d` diventato `%1$s`, o un `%2$s` inventato, è un'eccezione a runtime
  nella lingua di qualcun altro. È il difetto peggiore del lotto, perché l'app va in
  crash solo su quei telefoni;
- CATEGORIE DI PLURALE: dipendono dalla lingua, non dal numero di forme che ha l'inglese.
  Il russo ne vuole quattro, l'arabo sei, il giapponese una;
- CARATTERI VIETATI dal repo: trattini lunghi, apici curvi, ellissi.

⚠️⚠️ **LE VARIANTI REGIONALI PORTANO SOLO LE DIFFERENZE, e qui non si pretendono le
chiavi**: `values-pt-rPT` e `values-b+es+419` (dalla 0.80) scrivono le sole stringhe che
cambiano rispetto a `values-pt` e `values-es`, e tutto il resto lo risolve Android sulla
cartella senza paese, **una risorsa per volta**. Verificato con `lintVitalRelease`, che su
una variante parziale non si lamenta. Una variante si riconosce dal fatto che la cartella
della sua lingua **esiste**: `values-zh-rCN` non ha nessuna `values-zh` sopra di sé, quindi
resta una traduzione intera e va controllata come le altre.

⚠️ **Sui plurali la regola è il SOTTOINSIEME e non l'uguaglianza**: una forma può
**omettere** il numero (in arabo 'una sola immagine' si scrive a parole, ed è giusto
così), perché un argomento in più passato a `String.format` viene ignorato. Il difetto
vero è il contrario, cioè un segnaposto che il richiamo non passa.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

RES = 'app/src/main/res'
SRC = os.path.join(RES, 'values', 'strings.xml')

# Le categorie di plurale che ogni lingua vuole davvero (CLDR). Una lingua nuova va
# aggiunta qui, o il controllo non sa che cosa pretendere.
CATS = {
    'it': ['one', 'other', 'many'],
    # Le undici arrivate nella 0.70. ⚠️ Il POLACCO ne vuole quattro come il russo, e le
    # altre dieci una o due: la lingua decide, non il numero di forme che ha l'inglese.
    # ⚠️ Indonesiano, vietnamita e thailandese ne vogliono UNA, come il giapponese: in
    # quelle lingue il nome non cambia col numero, e scrivere una forma 'one' sarebbe una
    # riga che il sistema non chiede mai.
    'id': ['other'],
    'vi': ['other'],
    'th': ['other'],
    'pl': ['one', 'few', 'many', 'other'],
    'fa': ['one', 'other'],
    'bn': ['one', 'other'],
    'ta': ['one', 'other'],
    'te': ['one', 'other'],
    'mr': ['one', 'other'],
    'ur': ['one', 'other'],
    'nl': ['one', 'other'],
    # ⚠️ Francese, spagnolo e portoghese vogliono anche 'many', e non lo sapevo: CLDR la
    # usa per i milioni. Su un conto di fotografie non si raggiunge mai, ma la lingua ce
    # l'ha e lint la pretende, quindi vale 'other'.
    'fr': ['one', 'many', 'other'],
    'de': ['one', 'other'],
    'es': ['one', 'many', 'other'],
    'pt': ['one', 'many', 'other'],
    'tr': ['one', 'other'],
    'sw': ['one', 'other'],
    'hi': ['one', 'other'],
    'ja': ['other'],
    'ko': ['other'],
    'zh-rCN': ['other'],
    'zh-rTW': ['other'],
    'ru': ['one', 'few', 'many', 'other'],
    'uk': ['one', 'few', 'many', 'other'],
    'ar': ['zero', 'one', 'two', 'few', 'many', 'other'],
}

BANNED = {'—': 'trattino lungo', '–': 'trattino medio',
          '‘': 'apice curvo', '’': 'apice curvo',
          '“': 'virgolette curve', '”': 'virgolette curve',
          '…': 'ellissi'}


def holders(text):
    return sorted(re.findall(r'%\d+\$[sd]', text or ''))


def base_lang(lang):
    """La lingua senza paese di una variante regionale: `pt-rPT` -> `pt`, `b+es+419` -> `es`.

    ⚠️ Restituisce un codice anche per `zh-rCN`, che una variante non è: a decidere è chi
    chiama, guardando se quella cartella esiste. Separare le due domande (come si chiama la
    lingua, e se la sua cartella c'è) è ciò che tiene la funzione pura.
    """
    if lang.startswith('b+'):
        parti = lang.split('+')
        return parti[1] if len(parti) > 2 else None
    if '-r' in lang:
        return lang.split('-r')[0]
    return None


def read(path):
    """Le stringhe, i plurali, e i nomi che si dichiarano NON traducibili.

    ⚠️⚠️ **`translatable="false"` NON è una scorciatoia per saltare un lavoro**: dice che
    quel valore non è testo, quindi tradurlo sarebbe un difetto. Il caso in casa è l'URL di
    esempio del campo dell'indirizzo, che vale in ogni lingua ed è ventotto posti in cui
    cambiare un dominio (istruzione dell'utente, 2026-09-04: *non occorre che sia
    multilingua*).
    """
    strings, plurals, fissi = {}, {}, set()
    for el in ET.parse(path).getroot():
        if el.tag == 'string':
            strings[el.get('name')] = el.text or ''
            if el.get('translatable') == 'false':
                fissi.add(el.get('name'))
        elif el.tag == 'plurals':
            plurals[el.get('name')] = {i.get('quantity'): (i.text or '') for i in el}
    return strings, plurals, fissi


def main():
    base_s, base_p, base_fissi = read(SRC)
    langs = sorted(d[len('values-'):] for d in os.listdir(RES)
                   if d.startswith('values-') and
                   os.path.isfile(os.path.join(RES, d, 'strings.xml')))
    problems = 0
    for lang in langs:
        here_s, here_p, _ = read(os.path.join(RES, 'values-%s' % lang, 'strings.xml'))
        said = []

        # ⚠️ Vedi la nota in testa: una variante regionale porta solo le differenze, quindi
        # le chiavi che non ha non sono un difetto, e le categorie di plurale sono quelle
        # della sua lingua.
        madre = base_lang(lang)
        parziale = madre is not None and os.path.isdir(os.path.join(RES, 'values-%s' % madre))
        cats = madre if parziale else lang

        for name, text in base_s.items():
            # ⚠️ Una stringa non traducibile si controlla al ROVESCIO: qui non deve esserci,
            # e se c'è è una copia rimasta indietro, cioè il difetto che la dichiarazione
            # serve a evitare. Segnalarla è l'unico modo di accorgersene, perché una copia
            # vecchia non fa comparire l'inglese: fa comparire un valore sbagliato.
            if name in base_fissi:
                if name in here_s:
                    said.append('la stringa %s non e\' traducibile e qui c\'e\' ancora' % name)
            elif name not in here_s:
                if not parziale:
                    said.append('manca la stringa %s' % name)
            elif holders(here_s[name]) != holders(text):
                said.append('segnaposto diversi in %s: %s contro %s'
                            % (name, holders(here_s[name]), holders(text)))
        for name, forms in base_p.items():
            if name not in here_p:
                if not parziale:
                    said.append('manca il plurale %s' % name)
                continue
            base = forms.get('other', '')
            wanted = CATS.get(cats)
            if wanted is None:
                said.append('lingua sconosciuta al controllo: aggiungila a CATS')
                break
            for cat in wanted:
                if cat not in here_p[name]:
                    said.append('manca la forma %s di %s' % (cat, name))
                elif not set(holders(here_p[name][cat])) <= set(holders(base)):
                    said.append('segnaposto sconosciuti in %s/%s: %s'
                                % (name, cat, holders(here_p[name][cat])))
        for name, text in list(here_s.items()) + [
            (n + '/' + c, t) for n, f in here_p.items() for c, t in f.items()
        ]:
            for ch, why in BANNED.items():
                if ch in text:
                    said.append('%s in %s' % (why, name))

        extra = set(here_s) - set(base_s) | set(here_p) - set(base_p)
        if extra:
            said.append('chiavi che l\'inglese non ha: %s' % sorted(extra))

        problems += len(said)
        etichetta = lang + (' (variante)' if parziale else '')
        print('%-20s %s' % (etichetta, 'a posto' if not said else '%d PROBLEMI' % len(said)))
        for line in said[:10]:
            print('         -', line)

    print('%d lingue, %d problemi' % (len(langs), problems))
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
