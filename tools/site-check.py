#!/usr/bin/env python3
"""Check that the site built by `pages.yml` holds every local file its pages name, since 6.29.

Usage: python3 tools/site-check.py site

⚠️ The site is copied from `publish/` by a list of files to INCLUDE (H26 of the 6.25 review):
until 6.28 it was `publish/` minus a list to leave out, so 20 files of the feedback document that
no page of the site used were public, and a new working file put in `publish/` went public unless
somebody remembered the list. With an inclusion list the risk turns the other way, a file the page
needs and the list forgets, and this check is what stops that: it reads every `src`, `href`,
`srcset` and `url(...)` of the pages and fails on a local one that is not in the site.
"""
from pathlib import Path
import re
import sys

site = Path(sys.argv[1])
missing = []
for page in sorted(site.rglob('*.html')):
    text = page.read_text()
    values = []
    for attribute, double, single in re.findall(r'''\b(src|href|srcset)\s*=\s*(?:"([^"]*)"|'([^']*)')''', text):
        value = double or single
        values.extend(value.split(',') if attribute == 'srcset' else [value])
    values.extend(re.findall(r'''url\(\s*["']?([^"')]+)["']?\s*\)''', text))
    for value in values:
        target = value.strip().split(' ')[0].split('#')[0].split('?')[0]
        if not target or re.match(r'^(?:[a-z]+:|//)', target):
            continue
        if not (page.parent / target).resolve().is_file():
            missing.append(f'{page.relative_to(site)} -> {target}')
if missing:
    sys.exit('Il sito non contiene file che le sue pagine citano:\n' + '\n'.join(missing))
print(f'Sito: tutti i file locali citati dalle pagine ci sono ({len(list(site.rglob("*")))} voci).')
