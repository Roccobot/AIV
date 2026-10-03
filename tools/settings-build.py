#!/usr/bin/env python3
"""Render the settings proposal's headings, lists, tables and inline Markdown."""
from pathlib import Path
import html
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def inline(text):
    parts = []
    offset = 0
    for match in re.finditer(r'\[([^\]]+)\]\(([^)]+)\)|\*\*([^*]+)\*\*|`([^`]+)`', text):
        parts.append(html.escape(text[offset:match.start()]))
        if match[1]:
            href = match[2]
            if href == '../Rules.md':
                href = 'https://github.com/Roccobot/AIV/blob/main/Rules.md'
            if not href.startswith(('https://', 'http://')):
                raise ValueError('Unsupported proposal link: '+href)
            parts.append(f'<a href="{html.escape(href, quote=True)}" target="_blank" rel="noopener noreferrer">{html.escape(match[1])}</a>')
        elif match[3]:
            parts.append('<strong>'+html.escape(match[3])+'</strong>')
        else:
            parts.append('<code>'+html.escape(match[4])+'</code>')
        offset = match.end()
    parts.append(html.escape(text[offset:]))
    return ''.join(parts)


blocks = (ROOT / 'docs/Settings-proposal.md').read_text().strip().split('\n\n')
body = []
for block in blocks:
    lines = block.splitlines()
    if lines[0].startswith('#'):
        if len(lines) != 1:
            raise ValueError('Unsupported heading block')
        heading = re.fullmatch(r'(#{1,6}) (.+)', lines[0])
        if not heading:
            raise ValueError('Unsupported heading')
        level = len(heading[1])
        body.append(f'<h{level}>{inline(heading[2])}</h{level}>')
    elif lines[0].startswith('|'):
        if len(lines) < 3 or not re.fullmatch(r'[| :\-]+', lines[1]):
            raise ValueError('Unsupported table')
        headers = [cell.strip() for cell in lines[0].strip('|').split('|')]
        body.append('<div class="table-scroll" tabindex="0" role="region" aria-label="Tabella di confronto"><table><thead><tr>'+''.join('<th scope="col">'+inline(cell)+'</th>' for cell in headers)+'</tr></thead><tbody>')
        for row in lines[2:]:
            cells = [cell.strip() for cell in row.strip('|').split('|')]
            if len(cells) != len(headers):
                raise ValueError('Mismatched table cells')
            body.append('<tr>'+''.join('<td>'+inline(cell)+'</td>' for cell in cells)+'</tr>')
        body.append('</tbody></table></div>')
    elif lines[0].startswith('- '):
        items = []
        for line in lines:
            if line.startswith('- '):
                items.append(line[2:])
            elif line.startswith('  ') and items:
                items[-1] += ' '+line.strip()
            else:
                raise ValueError('Unsupported list')
        body.append('<ul>'+''.join('<li>'+inline(item)+'</li>' for item in items)+'</ul>')
    else:
        body.append('<p>'+inline(' '.join(lines))+'</p>')
page = '''<!doctype html>
<html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>AIV: proposta per le impostazioni</title>
<link rel="icon" href="assets/feedback-favicon.png?v=2" type="image/png" sizes="32x32">
<link rel="icon" href="assets/feedback-favicon.svg?v=2" type="image/svg+xml">
<link rel="stylesheet" href="feedback.css?v=3"><link rel="stylesheet" href="settings.css">
</head><body><header class="intro"><p class="eyebrow">AIV · proposta dell'agente</p>
<nav><a href="https://aiv-feedback.roccobot-b90.workers.dev/feedback#decisions" target="_blank" rel="noopener noreferrer">Lascia il feedback</a>
<a href="tablet.html" target="_blank" rel="noopener noreferrer">Mockup da tablet</a>
<a href="./" target="_blank" rel="noopener noreferrer">Scarica AIV</a></nav></header>
<main>__BODY__</main><footer>Fonte: <a href="https://github.com/Roccobot/AIV/blob/main/docs/Settings-proposal.md" target="_blank" rel="noopener noreferrer">Proposta delle impostazioni su GitHub</a>.</footer></body></html>
'''.replace('__BODY__', '\n'.join(body))
output = ROOT / 'publish/settings.html'
if '--check' in sys.argv:
    if not output.exists() or output.read_text() != page:
        sys.exit('Esegui tools/settings-build.py per aggiornare settings.html.')
    print('Proposta HTML allineata al documento sorgente.')
else:
    output.write_text(page)
    print('Creata la proposta delle impostazioni per il sito.')
