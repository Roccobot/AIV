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
for match in re.finditer(r'^## (\d+)\. ([^\n]+)\n(.*?)(?=^## |\Z)', md, re.M | re.S):
    number = int(match[1])
    identifier = identifiers[number - 1]
    items.append(dict(id=identifier, version=identifier.rsplit('-', 1)[0], title=match[2],
                      paragraphs=[re.sub(r'`([^`]+)`', r'\1', p.replace('\n', ' ')).strip()
                                  for p in match[3].strip().split('\n\n')]))
data = dict(project='AIV', version=version, items=items, decisions=[
    dict(id='d-settings-order', title='A. Riordino delle impostazioni',
         text='Leggi il confronto prima/dopo. Questa scelta riguarda la disposizione, che sarà applicata dopo il tuo riscontro.',
         link='settings.html',
         options=['Applica la proposta', 'Rivedi la proposta', 'Conserva la struttura attuale']),
    dict(id='d-tablet-layout', title='B. Interfacce da tablet', text='Prova le schermate, gli orientamenti e i moduli dell\'editor. Indica quali disposizioni preferisci.',
         link='tablet.html', options=['Approvo la direzione', 'Richiedo modifiche', 'Rimandiamo']),
    dict(id='d-png-fullscreen', title='C. PNG modificato a schermo intero', text='Sul tuo telefono il PNG già modificato contiene le modifiche quando lo apri a schermo intero?',
         options=['Le modifiche si vedono', 'Compare l\'originale', 'Non verificato'])])
esc = html.escape
cards = []
for index, item in enumerate(items, 1):
    cards.append(f'<article class="card test" id="{item["id"]}" data-id="{item["id"]}"><p class="check-position">Verifica <strong>{index}</strong>/{len(items)}</p><p class="eyebrow">{item["id"]} · AIV {item["version"]}</p><h3>{esc(item["title"])}</h3>')
    cards.extend('<p>'+esc(p)+'</p>' for p in item['paragraphs'])
    cards.append('<fieldset><legend>Esito della prova</legend>')
    for status in ['Tutto OK', 'Accettabile', 'Non approvato']:
        cards.append(f'<button type="button" class="outcome" data-status="{status}" aria-pressed="false">{status}</button>')
    cards.append('</fieldset><label>Commento<textarea class="comment" rows="3"></textarea></label><label class="attachment">Allega file o trascinali qui<input class="images" type="file" accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml,.svg,application/zip,application/x-zip-compressed,.zip" multiple></label><div class="image-list"></div><p class="item-state">Non provato</p></article>')
page = '''<!doctype html>
<html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Feedback AIV</title><link rel="icon" href="assets/feedback-favicon.png?v=2" type="image/png" sizes="32x32"><link rel="icon" href="assets/feedback-favicon.svg?v=2" type="image/svg+xml"><link rel="stylesheet" href="feedback.css?v=8"><script src="feedback.js?v=7" defer></script><script src="feedback-format.js?v=1" defer></script></head>
<body data-feedback="codex"><header class="intro"><p class="eyebrow">AIV · giro della __VERSION__ · Codex</p><h1>Feedback AIV</h1><p>Correggi/Rimuovi nel modulo Dettaglio. Tutte le prove aperte dalla 3.13, comprese quelle su PNG e PSD, rimangono qui; Filigrana e Fluidifica sono approvati e archiviati.</p><nav><a target="_blank" rel="noopener noreferrer" href="https://github.com/Roccobot/AIV/releases/download/v__VERSION__/AIV-__VERSION__.apk">Scarica AIV __VERSION__</a><a target="_blank" rel="noopener noreferrer" href="tablet.html">Mockup da tablet</a><a target="_blank" rel="noopener noreferrer" href="settings.html">Riordino delle impostazioni</a></nav><p>Scegli <strong>Tutto OK</strong>, <strong>Accettabile</strong> oppure <strong>Non approvato</strong>. Nessuna scelta significa <strong>Non provato</strong>; tocca di nuovo l'esito per cancellarlo. Aggiungi commenti e allegati quando servono.</p><p>Le risposte si salvano in questo browser, su questo dispositivo. Esporta il JSON per trasferirle o conservarne una copia. Puoi allegare PNG, JPG, WebP, GIF, SVG e ZIP, anche trascinandoli sulla voce o in Qualsiasi altra cosa. Gli allegati vengono conservati interi, senza ritagli o ridimensionamenti: massimo 8 MB ciascuno, 20 MB in totale. Se il salvataggio fallisce, esporta il JSON prima di chiudere.</p><p><strong>Invia</strong> prepara il riepilogo: devi copiarlo e inviarmelo in chat. Il documento non spedisce dati a un server. Gli allegati vanno consegnati con il JSON oppure come allegati in chat.</p><p>Verifiche automatiche: __COUNT__ prove superate, 28 traduzioni controllate e compilazione riuscita. Il collaudo sul telefono resta tuo. Codex aggiornerà stati e archivio dopo il giro completo.</p></header>
<aside class="dashboard" aria-label="Avanzamento"><div id="counts"></div><progress id="progress" value="0" max="__TOTAL__" aria-label="Prove con risposta"></progress><p id="saved" role="status" aria-live="polite">Caricamento delle risposte...</p></aside>
<main><section class="card"><h2>Il tuo dispositivo</h2><label>Telefono o tablet e versione Android<input id="device" placeholder="Per esempio: modello, Android 16"></label><label>Versione AIV installata<input id="installed" value="__VERSION__"></label></section><section><h2>Prove sul telefono e nel browser</h2><p>Prima le prove sulle cartelle della 3.13 ancora aperte; poi PNG, PSD e il documento interattivo. <span id="answered"></span></p><div id="tests">__CARDS__</div></section><section id="decisions"><h2>Decisioni da concordare</h2><p>Le proposte delle impostazioni e dei tablet si valutano separatamente dal collaudo delle funzioni. Queste risposte non entrano nei contatori delle prove.</p><div id="decision-list"></div></section><section class="card extra"><h2>Qualsiasi altra cosa</h2><label>Osservazioni libere<textarea id="notes" rows="7" placeholder="Comportamenti inattesi, preferenze e altre osservazioni..."></textarea></label><label class="attachment">Allega file o trascinali qui<input class="images" type="file" accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml,.svg,application/zip,application/x-zip-compressed,.zip" multiple></label><div class="image-list"></div></section><section class="card"><h2>Consegna e copie</h2><div class="actions"><button id="save" type="button">Salva</button><button id="send" type="button">Invia</button><button id="copy" type="button">Copia il riepilogo</button><button id="export" type="button">Esporta JSON</button><label class="file-button">Importa JSON<span class="browse-label" aria-hidden="true">Sfoglia</span><input id="import" type="file" accept="application/json,.json"></label><button id="reset" type="button">Azzera tutto</button></div><p id="action-message" role="status" aria-live="polite"></p><label>Riepilogo da copiare<textarea id="summary" rows="12" readonly></textarea></label><p>Invia il giro completo anche se alcune prove restano Non provato. Le tue risposte sono sul dispositivo: per farle leggere a Codex occorre consegnarle in chat.</p></section><section class="card archive"><h2>Riscontri conclusi</h2><p>Filigrana: approvata il 30 settembre 2026, AIV 3.03. Fluidifica: completato e approvato il 30 settembre 2026, AIV 3.03.</p><p>Gli stili attendono il tuo via libera. Sfogliatore Web e Play Store restano sospesi. Le altre voci del documento Claude conservano il proprio stato.</p></section></main><div class="floating-controls" aria-label="Navigazione e salvataggio"><button id="previous-card" type="button" aria-label="Riquadro precedente" title="Riquadro precedente"><svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20V4m-6 6 6-6 6 6M5 4h14"/></svg></button><button id="next-card" type="button" aria-label="Riquadro successivo" title="Riquadro successivo"><svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v16m-6-6 6 6 6-6M5 20h14"/></svg></button><button id="first-empty" type="button" aria-label="Primo riquadro non compilato" title="Primo riquadro non compilato" hidden><svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12h14m-5-5 5 5-5 5M20 5v14"/></svg></button><button id="floating-save" type="button" aria-label="Salva le risposte" title="Salva le risposte"><svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 3h12l4 4v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"/><path d="M7 3v6h10V3M7 21v-8h10v8M14 5v2"/></svg></button><span id="navigation-position" class="sr-only" role="status" aria-live="polite"></span></div><footer>Documento di Codex · <a target="_blank" rel="noopener noreferrer" href="https://github.com/Roccobot/AIV/blob/main/docs/Feedback.md">Versione leggibile nel repository</a> · Ctrl/Cmd+S salva le risposte.</footer><script id="feedback-data" type="application/json">__DATA__</script></body></html>
'''
# Counts are read from the canonical feedback text.
count = re.search(r'superate: (\d+) prove', md).group(1)
page = page.replace('__COUNT__', count).replace('__TOTAL__', str(len(items))).replace('__VERSION__', version).replace('__CARDS__', '\n'.join(cards)).replace('__DATA__', json.dumps(data, ensure_ascii=False).replace('<', '\\u003c'))
output = ROOT / 'publish/feedback.html'
if '--check' in sys.argv:
    if not output.exists() or output.read_text() != page:
        sys.exit('Il documento HTML non corrisponde a docs/Feedback.md: esegui tools/feedback-build.py.')
    print(f'Documento HTML allineato alle {len(items)} prove e alla versione {version}')
else:
    output.write_text(page)
    print('Creato '+str(output))
