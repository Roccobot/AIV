#!/usr/bin/env python3
"""Build the static feedback document from the canonical Markdown and app version."""
from pathlib import Path
import hashlib
import html
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
# The project and the three outcomes are written here only: the page reads them from the
# embedded data, and the CSS colours an outcome through its kind, never through its label.
PROJECT = 'AIV'
OUTCOMES = [('Tutto OK', 'ok'), ('Accettabile', 'warn'), ('Non approvato', 'bad')]


def option(name, default):
    """`--source FILE` and `--output FILE` let `feedback-interactive-check.py` build a
    throwaway page from a synthetic source with the real generator and template, so a
    document with no open tests can still be exercised without a second copy of the markup."""
    if name in sys.argv:
        return Path(sys.argv[sys.argv.index(name) + 1])
    return default


md = option('--source', ROOT / 'docs/Feedback.md').read_text()
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
# Optional "Etichette testuali" section: each `### id[ · title]` heading plus its body is the
# proposed Italian text. The page asks no decisions any more; old drafts keep theirs (feedback-data.js).
labels = []
labels_match = re.search(r'^## Etichette testuali\n(.*?)(?=^## |\Z)', md, re.M | re.S)
if labels_match:
    for lm in re.finditer(r'^### ([^\n]+)\n(.*?)(?=^### |\Z)', labels_match[1], re.M | re.S):
        head = lm[1].strip()
        if ' · ' in head:
            lid, ltitle = head.split(' · ', 1)
        else:
            lid, ltitle = head, head
        raw = lm[2]
        notes = ' '.join(re.findall(r'<!--(.*?)-->', raw, re.S))
        a11y = bool(re.search(r'contentdescription|talkback|screen reader|non visibile|solo lettore|\baria\b', notes + ' ' + head, re.I))
        proposal = re.sub(r'<!--.*?-->', '', raw, flags=re.S).strip()
        if not proposal:
            raise SystemExit(f'Etichetta {lid}: manca il testo ITA proposto')
        entry = dict(id=lid.strip(), title=ltitle.strip(), proposal=proposal)
        if a11y:
            entry['a11y'] = True
        labels.append(entry)
data = dict(project=PROJECT, version=version, items=items, labels=labels,
            outcomes=[dict(label=label, kind=kind) for label, kind in OUTCOMES])
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
# The "Aggiornamenti recenti" card is built from the table in docs/Feedback.md, so the closed
# results live in one place. One sentence per round, newest first, e.g.
# "Giro 3.42: rail tablet 3.42-01 OK."; an empty table leaves the card out.
concluded = []
concluded_match = re.search(r'^## Aggiornamenti recenti\n(.*?)(?=^## |\Z)', md, re.M | re.S)
if concluded_match:
    for line in concluded_match[1].splitlines():
        cells = [c.strip() for c in line.strip().strip('|').split('|')]
        if len(cells) < 3 or not re.fullmatch(r'\d+\.\d+-\d+', cells[1]):
            continue
        concluded.append((cells[1], cells[0], cells[2]))
rounds = {}
for identifier, feature, status in concluded:
    rounds.setdefault(identifier.rsplit('-', 1)[0], []).append(
        (feature[:1].lower() + feature[1:]) + ' ' + identifier + ' ' + status)
concluded_html = ''
if rounds:
    ordered = sorted(rounds, key=lambda v: tuple(int(x) for x in v.split('.')), reverse=True)
    sentence = ' '.join('Giro ' + v + ': ' + ', '.join(rounds[v]) + '.' for v in ordered)
    concluded_html = ('<section class="card archive"><h2>Aggiornamenti recenti</h2><p>'
                      + inline_md(sentence) + '</p></section>')
esc = html.escape
cards = []
for index, item in enumerate(items, 1):
    cards.append(f'<article class="card test" id="{item["id"]}" data-id="{item["id"]}"><p class="check-position">Verifica <strong>{index}</strong>/{len(items)}</p><p class="eyebrow">{item["id"]} · AIV {item["version"]}</p><h3>{esc(item["title"])}</h3>')
    cards.extend('<p>' + inline_md(p) + '</p>' for p in item['paragraphs'])
    cards.append('<fieldset><legend>Esito della prova</legend>')
    for status, kind in OUTCOMES:
        cards.append(f'<button type="button" class="outcome" data-status="{status}" data-kind="{kind}" aria-pressed="false">{status}</button>')
    cards.append('</fieldset><label>Commento<textarea class="comment" rows="3"></textarea></label><label class="attachment" aria-label="Allega file o trascinali qui"><span class="attachment-plus" aria-hidden="true">+</span><input class="images" aria-label="Allega file o trascinali qui" type="file" accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml,.svg,application/zip,application/x-zip-compressed,.zip" multiple></label><div class="image-list"></div></article>')

tpl = (Path(__file__).resolve().parent / 'feedback-page.html.in').read_text()
page = (tpl
    .replace('__TOTAL__', str(len(items)))
    .replace('__VERSION__', version)
    .replace('__CARDS__', '\n'.join(cards))
    .replace('__NEXT_STEPS__', next_steps_html)
    .replace('__CONCLUDED__', concluded_html)
    .replace('__DATA__', json.dumps(data, ensure_ascii=False).replace('<', '\\u003c')))
# Each local file the page loads carries `?v=` plus the start of its own SHA-256, so a
# browser fetches it again exactly when it changes; nobody bumps a number by hand.
def versioned(match):
    path = ROOT / 'publish' / match[1]
    if not path.is_file():
        raise SystemExit(f'Il modello carica {match[1]}, che non esiste in publish/.')
    return match[1] + '?v=' + hashlib.sha256(path.read_bytes()).hexdigest()[:10]


page = re.sub(r'([\w./-]+)\?v=__HASH__', versioned, page)
if '__HASH__' in page:
    raise SystemExit('Un segnaposto __HASH__ del modello non è stato sostituito.')
output = option('--output', ROOT / 'publish/feedback.html')
if '--check' in sys.argv:
    if not output.exists() or output.read_text() != page:
        sys.exit('Il documento HTML non corrisponde a docs/Feedback.md: esegui tools/feedback-build.py.')
    print(f'Documento HTML allineato alle {len(items)} prove, {len(labels)} etichette e alla versione {version}')
else:
    output.write_text(page)
    print('Creato ' + str(output))
