#!/usr/bin/env python3
"""Checks the Google Play listing texts of docs/Store.md against Google's limits.

Author: Rocco Casadei, a.k.a. Roccobot

The limits are Google's (name 30, short description 80, full description 4,000
characters), and Play Console refuses a longer text only when it is pasted, which is
the worst moment to find out. Run from the repository root: it prints every count and
exits with 1 when a text is over its limit or a section is missing.
"""
import pathlib
import re
import sys

STORE = pathlib.Path('docs/Store.md')
LIMITS = {'name': 30, 'short': 80, 'full': 4000}


def section(md, title):
    """The body of the `### title` section, up to the next heading of any level."""
    match = re.search(r'^### ' + re.escape(title) + r'\n(.*?)(?=^#{1,3} |\Z)', md, re.M | re.S)
    if not match:
        raise SystemExit(f'docs/Store.md: manca la sezione "{title}"')
    return match[1].strip()


def texts(md):
    """Every listing text as (label, limit, text)."""
    name = re.search(r'`([^`]+)`', section(md, 'Nome'))[1]
    found = [('nome', LIMITS['name'], name)]
    for line in section(md, 'Descrizione breve').splitlines():
        match = re.match(r'- \*\*(\w+)\*\*: (.+)', line)
        if match:
            found.append((f'descrizione breve, {match[1]}', LIMITS['short'], match[2].strip()))
    for language in ('italiano', 'English'):
        found.append((f'descrizione completa, {language}', LIMITS['full'],
                      section(md, f'Descrizione completa, {language}')))
    return found


def main():
    md = STORE.read_text(encoding='utf-8')
    over = 0
    found = texts(md)
    if len([t for t in found if t[0].startswith('descrizione breve')]) != 2:
        raise SystemExit('docs/Store.md: la descrizione breve vuole una riga italiana e una inglese')
    for label, limit, text in found:
        length = len(text)
        mark = 'ok' if length <= limit else 'TROPPO LUNGO'
        over += length > limit
        print(f'{label}: {length}/{limit} {mark}')
    sys.exit(1 if over else 0)


if __name__ == '__main__':
    main()
