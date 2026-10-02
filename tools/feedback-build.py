#!/usr/bin/env python3
"""Build the static feedback document from the canonical Markdown and app version."""
from pathlib import Path
import html
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
md = (ROOT / 'docs/Feedback.md').read_text()
version = re.search(r'versionName\s*=\s*"([^"]+)"', (ROOT / 'app/build.gradle.kts').read_text()).group(1)
identifiers = re.findall(r'^\| (\d+\.\d+-\d+) \|', md, re.M)
items = []
def inline_md(text):
    """Render code, links, bold, italic used in Feedback.md paragraphs."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        if text[i] == "`":
            j = text.find("`", i + 1)
            if j != -1:
                out.append("<code>" + html.escape(text[i + 1:j]) + "</code>")
                i = j + 1
                continue
        m = re.match(r"\[((?:\\.|[^\]\\])+)\]\((https?://[^\s)]+)\)", text[i:])
        if m:
            label = re.sub(r"\\([\\*`\[\]_])", r"\1", m.group(1))
            href = html.escape(m.group(2), quote=True)
            out.append(f'<a target="_blank" rel="noopener noreferrer" href="{href}">' + inline_md(label) + "</a>")
            i += m.end()
            continue
        matched = False
        for marker, open_t, close_t in (
            ("***", "<strong><em>", "</em></strong>"),
            ("**", "<strong>", "</strong>"),
            ("*", "<em>", "</em>"),
            ("_", "<em>", "</em>"),
        ):
            if not text.startswith(marker, i):
                continue
            end = i + len(marker)
            while end < n:
                if text[end] == "\\":
                    end += 2
                    continue
                if text.startswith(marker, end):
                    break
                end += 1
            if end >= n or end <= i + len(marker):
                continue
            body = text[i + len(marker):end]
            out.append(open_t + inline_md(body) + close_t)
            i = end + len(marker)
            matched = True
            break
        if matched:
            continue
        if text[i] == "\\" and i + 1 < n and text[i + 1] in "\\*`[]_":
            out.append(html.escape(text[i + 1]))
            i += 2
        else:
            out.append(html.escape(text[i]))
            i += 1
    return "".join(out)

for match in re.finditer(r'^## (\d+)\. ([^\n]+)\n(.*?)(?=^## |\Z)', md, re.M | re.S):
    number = int(match[1])
    identifier = identifiers[number - 1]
    paragraphs = []
    for p in match[3].strip().split('\n\n'):
        flat = re.sub(r'\s*\n\s*', ' ', p).strip()
        if flat:
            paragraphs.append(flat)
    items.append(dict(id=identifier, version=identifier.rsplit('-', 1)[0], title=match[2],
                      paragraphs=paragraphs))
# Decisioni 3.24 chiuse: fuori dal flusso da rispondere (archivio HTML). JSON vecchio resta valido.
# Etichette testuali: sezione opzionale; ogni ### id[ · titolo] + corpo = proposta ITA.
labels = []
labels_match = re.search(r'^## Etichette testuali\n(.*?)(?=^## |\Z)', md, re.M | re.S)
if labels_match:
    for lm in re.finditer(r'^### ([^\n]+)\n(.*?)(?=^### |\Z)', labels_match[1], re.M | re.S):
        head = lm[1].strip()
        if ' · ' in head:
            lid, ltitle = head.split(' · ', 1)
        else:
            lid, ltitle = head, head
        proposal = lm[2].strip()
        if not proposal:
            raise SystemExit(f'Etichetta {lid}: manca il testo ITA proposto')
        labels.append(dict(id=lid.strip(), title=ltitle.strip(), proposal=proposal))
data = dict(project='AIV', version=version, items=items, decisions=[], labels=labels)
next_match = re.search(r'^## Prossimi passi\n(.*?)(?=^## |\Z)', md, re.M | re.S)
if not next_match:
    raise SystemExit('docs/Feedback.md deve contenere la sezione Prossimi passi')
next_steps = []
for line in next_match[1].splitlines():
    if line.startswith('- '):
        next_steps.append(line[2:].strip())
if not next_steps:
    raise SystemExit('la sezione Prossimi passi deve contenere almeno una voce')
next_steps_html = '<section class=\"card next-steps\"><h2>Prossimi passi</h2><ul>'
next_steps_html += ''.join('<li>' + inline_md(step) + '</li>' for step in next_steps)
next_steps_html += '</ul></section>'
esc = html.escape
cards = []
for index, item in enumerate(items, 1):
    cards.append(f'<article class="card test" id="{item["id"]}" data-id="{item["id"]}"><p class="check-position">Verifica <strong>{index}</strong>/{len(items)}</p><p class="eyebrow">{item["id"]} · AIV {item["version"]}</p><h3>{esc(item["title"])}</h3>')
    cards.extend('<p>' + inline_md(p) + '</p>' for p in item['paragraphs'])
    cards.append('<fieldset><legend>Esito della prova</legend>')
    for status in ['Tutto OK', 'Accettabile', 'Non approvato']:
        cards.append(f'<button type="button" class="outcome" data-status="{status}" aria-pressed="false">{status}</button>')
    cards.append('</fieldset><label>Commento<textarea class="comment" rows="3"></textarea></label><label class="attachment">Allega file o trascinali qui<input class="images" type="file" accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml,.svg,application/zip,application/x-zip-compressed,.zip" multiple></label><div class="image-list"></div><p class="item-state">Non provato</p></article>')

tpl = (Path(__file__).resolve().parent / 'feedback-page.html.in').read_text()
count = re.search(r'superate: (\d+) prove', md).group(1)
page = (tpl
    .replace('__COUNT__', count)
    .replace('__TOTAL__', str(len(items)))
    .replace('__VERSION__', version)
    .replace('__CARDS__', '\n'.join(cards))
    .replace('__NEXT_STEPS__', next_steps_html)
    .replace('__DATA__', json.dumps(data, ensure_ascii=False).replace('<', '\\u003c')))
output = ROOT / 'publish/feedback.html'
if '--check' in sys.argv:
    if not output.exists() or output.read_text() != page:
        sys.exit('Il documento HTML non corrisponde a docs/Feedback.md: esegui tools/feedback-build.py.')
    print(f'Documento HTML allineato alle {len(items)} prove, {len(labels)} etichette e alla versione {version}')
else:
    output.write_text(page)
    print('Creato ' + str(output))
