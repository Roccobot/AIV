#!/usr/bin/env python3
"""Validate and exercise the actual agent feedback page, including persistence and import."""
import base64
import colorsys
import functools
import http.server
import json
from pathlib import Path
import re
import shutil
import tempfile
import threading
import zipfile


def open_delivery(page):
    """Show Consegna e copie overlay so #save/#export/… are actionable."""
    overlay = page.locator('#delivery-overlay')
    if overlay.is_visible():
        return
    btn = page.locator('#open-delivery')
    if btn.count() and btn.is_visible():
        btn.click()
    else:
        # Mobile: long-press floating Salva
        fab = page.locator('#floating-save')
        fab.dispatch_event('pointerdown', {'button': 0})
        page.wait_for_timeout(500)
        fab.dispatch_event('pointerup', {'button': 0})
    expect = __import__('playwright.sync_api', fromlist=['expect']).expect
    expect(overlay).to_be_visible()


def check(path):
    text = Path(path).read_text()
    match = re.search(r'<script id="feedback-data" type="application/json">(.*?)</script>', text, re.S)
    if not match:
        raise AssertionError('Mancano i dati del documento.')
    data = json.loads(match[1])
    assert data.get('project') == 'AIV' and data.get('version'), 'Progetto o versione mancanti.'
    ids = set()
    for item in data['items']:
        assert all(item.get(k) for k in ['id', 'version', 'title', 'paragraphs']), 'Voce incompleta.'
        assert isinstance(item['paragraphs'], list) and all(isinstance(p, str) and p.strip() for p in item['paragraphs']), 'Passi mancanti.'
        assert item['id'] not in ids, 'Identificatore duplicato.'
        ids.add(item['id'])
    assert isinstance(data.get('labels', []), list), 'labels deve essere una lista.'
    for label in data.get('labels', []):
        assert all(label.get(k) for k in ['id', 'title', 'proposal']), 'Etichetta incompleta.'
        assert label['id'].startswith('e-'), 'id etichetta deve iniziare con e-.'
    for question in data['decisions']:
        assert all(question.get(k) for k in ['id', 'title', 'text', 'options']), 'Decisione incompleta.'
        assert len(set(question['options'])) == len(question['options']), 'Scelte duplicate.'
    browser = shutil.which('chromium') or shutil.which('google-chrome')
    if not browser:
        raise AssertionError('Chromium non disponibile: resa non verificata.')
    from playwright.sync_api import sync_playwright, expect

    class Quiet(http.server.SimpleHTTPRequestHandler):
        def log_message(self, *args):
            pass

    handler = functools.partial(Quiet, directory=str(Path(path).resolve().parent))
    server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    url = f'http://127.0.0.1:{server.server_port}/{Path(path).name}'
    errors = []
    try:
        with tempfile.TemporaryDirectory() as temporary, sync_playwright() as pw:
            browser = pw.chromium.launch(executable_path=browser, args=['--no-sandbox'])
            context = browser.new_context(permissions=['clipboard-read', 'clipboard-write'])
            page = context.new_page()
            navigation_context = browser.new_context(locale='it-IT', viewport={'width':390,'height':844}, is_mobile=True, has_touch=True)
            navigation = navigation_context.new_page()
            navigation.on('pageerror', lambda e: errors.append(str(e)))
            navigation.goto(url)
            expect(navigation.locator('#save')).to_be_enabled()
            numbered = navigation.locator('.test .check-position')
            assert numbered.count() == len(data['items'])
            for index, counter in enumerate(numbered.all(), 1):
                expect(counter).to_have_text(f'Verifica {index}/{len(data["items"])}')
                expect(counter.locator('strong')).to_have_text(str(index))
                assert counter.evaluate('(el)=>getComputedStyle(el).fontWeight') == '400'
                assert int(counter.locator('strong').evaluate('(el)=>getComputedStyle(el).fontWeight')) >= 700
            def aligned(card):
                box = card.bounding_box()
                dashboard = navigation.locator('.dashboard').bounding_box()
                assert abs(box['y'] - dashboard['height'] - 12) < 3, 'Card hidden beneath the sticky dashboard.'
            for width in [320,390,800,1280]:
                navigation.set_viewport_size({'width':width,'height':900})
                navigation.evaluate('window.scrollTo(0,0)')
                expect(navigation.locator('#previous-card')).to_be_disabled()
                navigation.locator('#next-card').tap()
                aligned(navigation.locator('.test').nth(0))
                navigation.locator('#next-card').tap()
                aligned(navigation.locator('.test').nth(1))
                navigation.locator('#previous-card').tap()
                aligned(navigation.locator('.test').nth(0))
                navigation.locator('#next-card').tap()
                expect(navigation.locator('#first-empty')).to_be_visible()
                navigation.locator('#first-empty').tap()
                aligned(navigation.locator('.test').nth(0))
                # Mobile keeps ⇥ visible for long-press Altro; desktop hides when on the empty card.
                if width <= 720:
                    expect(navigation.locator('#first-empty')).to_be_visible()
                    expect(navigation.locator('#menu-toggle')).to_have_count(0)
                    expect(navigation.locator('#jump-altro')).to_have_count(0)
                    expect(navigation.locator('#open-delivery')).to_be_hidden()
                else:
                    expect(navigation.locator('#first-empty')).to_be_hidden()
                    expect(navigation.locator('#open-delivery')).to_be_visible()
                save_box = navigation.locator('#floating-save').bounding_box()
                for ident in ['previous-card','next-card']:
                    box = navigation.locator('#'+ident).bounding_box()
                    assert box['y']+box['height'] < save_box['y']
                    assert box['width'] >= 48 and box['height'] >= 48
            navigation.evaluate('''() => {
                const previous = document.querySelector('#tests .test:last-child').getBoundingClientRect();
                // Prefer a following in-flow primary card; .extra is a side column on desktop.
                const nextNode = document.querySelector('.decision') || document.querySelector('.feedback-primary .archive');
                const next = nextNode.getBoundingClientRect();
                const offset = document.querySelector('.dashboard').getBoundingClientRect().height + 12;
                window.scrollTo(0, window.scrollY + (previous.bottom + next.top)/2 - offset);
            }''')
            navigation.locator('#previous-card').tap()
            aligned(navigation.locator('.test').last)
            # Con 4 o più prove: 0 e l'ultima compilate, la 1 con esito, buco all'indice 2.
            # Con 2 o 3 prove non c'è quel buco: l'esito sulla 1 resta, e lo si toglie più sotto.
            assert len(data['items']) >= 2, 'Servono almeno 2 prove aperte per il controllo di navigazione.'
            navigation.locator('.test').nth(0).locator('.rich-editor').fill('Solo commento')
            navigation.locator('.test').nth(1).locator('[data-status="Non approvato"]').click()
            if len(data['items']) >= 4:
                navigation.locator('.test').nth(len(data['items']) - 1).locator('.rich-editor').fill('Più in basso')
                navigation.locator('#first-empty').tap()
                aligned(navigation.locator('.test').nth(2))
            # Fill through normal input handlers; navigation must update without a reload.
            for field in navigation.locator('.test .rich-editor,.decision .rich-editor').all():
                field.fill('Risposta di verifica')
            # Desktop hides ⇥ when nothing is empty; mobile keeps it for long-press Altro.
            if navigation.viewport_size['width'] <= 720:
                expect(navigation.locator('#first-empty')).to_be_visible()
            else:
                expect(navigation.locator('#first-empty')).to_be_hidden()
            navigation.locator('.test').nth(1).locator('[data-status="Non approvato"]').click()
            navigation.locator('.test').nth(1).locator('.rich-editor').fill('')
            # Con poche prove si è già sulla carta vuota: ⇥ è nascosto e Avanti è fermo.
            if navigation.locator('#next-card').is_enabled():
                navigation.locator('#next-card').tap()
            if navigation.locator('#first-empty').is_visible():
                navigation.locator('#first-empty').tap()
            aligned(navigation.locator('.test').nth(1))
            last = navigation.locator('.extra')
            last.scroll_into_view_if_needed()
            navigation.evaluate('window.scrollTo(0, document.body.scrollHeight)')
            expect(navigation.locator('#next-card')).to_be_disabled()
            navigation.locator('#previous-card').tap()
            # With decisions: previous from the bottom lands on the last decision.
            # Without: responseCards end at .extra, so previous lands on the last test.
            if navigation.locator('.decision').count():
                aligned(navigation.locator('.decision').last)
            else:
                aligned(navigation.locator('.test').last)
            navigation_context.close()
            formatting_context = browser.new_context(permissions=['clipboard-read','clipboard-write'])
            formatting = formatting_context.new_page()
            formatting.on('pageerror', lambda e: errors.append(str(e)))
            formatting.goto(url)
            expect(formatting.locator('#save')).to_be_enabled()
            card = formatting.locator('.test').first
            field = card.locator('.rich-editor')
            stored = card.locator('.comment')
            def fill_plain(field, text):
                # Clear first so a new scenario does not inherit the previous selection's style.
                field.fill('')
                field.fill(text)
            def select(field, start, end):
                field.evaluate("""(box, offsets) => {
                    box.focus();
                    const walker = document.createTreeWalker(box, NodeFilter.SHOW_TEXT);
                    const points = [];
                    let offset = 0, node;
                    while ((node = walker.nextNode())) {
                        for (const [index, target] of offsets.entries()) {
                            if (!points[index] && target <= offset + node.length)
                                points[index] = [node, target - offset];
                        }
                        offset += node.length;
                    }
                    const range = document.createRange();
                    range.setStart(...points[0]); range.setEnd(...points[1]);
                    getSelection().removeAllRanges(); getSelection().addRange(range);
                }""", [start,end])
            expect(formatting.locator('.markdown-preview,.preview-caption')).to_have_count(0)
            expect(stored).to_be_hidden()
            fill_plain(field, 'prima abc dopo')
            select(field,6,9)
            field.press('Meta+b')
            expect(stored).to_have_value('prima **abc** dopo')
            expect(field.locator('b,strong')).to_have_text('abc')
            field.press('Meta+i')
            expect(stored).to_have_value('prima ***abc*** dopo')
            expect(field.locator('i,em')).to_have_text('abc')
            field.press('Meta+i')
            expect(stored).to_have_value('prima **abc** dopo')
            field.press('Meta+b')
            expect(stored).to_have_value('prima abc dopo')
            fill_plain(field, 'abc')
            select(field,0,3)
            card.locator('[data-format="bold"]').click()
            expect(stored).to_have_value('**abc**')
            field.press('Control+z')
            expect(stored).to_have_value('abc')
            select(field,0,3)
            field.press('Control+b')
            expect(stored).to_have_value('**abc**')
            field.press('Control+b')
            expect(stored).to_have_value('abc')
            select(field,0,3)
            destination = url.rsplit('/',1)[0]+'/tablet.html'
            formatting.once('dialog',lambda dialog:dialog.accept(destination))
            field.press('Meta+k')
            expected_comment = '[abc]('+destination+')'
            expect(stored).to_have_value(expected_comment)
            link = field.locator('a')
            expect(link).to_have_text('abc')
            assert link.get_attribute('target')=='_blank' and 'noopener' in link.get_attribute('rel')
            with formatting.expect_popup() as pending:
                link.click()
            linked = pending.value
            linked.wait_for_load_state()
            assert linked.url==destination
            assert linked.evaluate('window.opener === null')
            linked.close()
            fill_plain(field, 'abc')
            select(field,0,3)
            formatting.once('dialog',lambda dialog:dialog.dismiss())
            field.press('Control+k')
            expect(stored).to_have_value('abc')
            formatting.once('dialog',lambda dialog:dialog.accept('javascript:alert(1)'))
            field.press('Control+k')
            expect(stored).to_have_value('abc')
            expect(formatting.locator('#action-message')).to_contain_text('http:// o https:// valido')
            # Literal typed text stays literal; imported Markdown restores visual formatting.
            literal = '**<img src=x onerror="window.injected=true">** [pericoloso](javascript:alert(1))'
            fill_plain(field, literal)
            expect(field.locator('img,script,a,b,strong')).to_have_count(0)
            assert formatting.evaluate('window.injected === undefined')
            fill_plain(field, 'abc')
            select(field,0,3)
            formatting.once('dialog',lambda dialog:dialog.accept(destination))
            card.locator('[data-format="link"]').click()
            expect(stored).to_have_value(expected_comment)
            decision_card = formatting.locator('.decision').first
            decision_field = decision_card.locator('.rich-editor') if formatting.locator('.decision').count() else None
            if decision_field:
                fill_plain(decision_field, 'scelta')
                select(decision_field,0,6)
                decision_field.press('Control+i')
                expect(decision_card.locator('textarea')).to_have_value('*scelta*')
                expect(decision_field.locator('i,em')).to_have_text('scelta')
            notes_field = formatting.locator('.extra .rich-editor')
            fill_plain(notes_field, 'note')
            select(notes_field,0,4)
            formatting.locator('.extra [data-format="bold"]').click()
            expect(formatting.locator('#notes')).to_have_value('**note**')
            formatting.locator('.extra [data-format="bold"]').click()
            expect(formatting.locator('#notes')).to_have_value('note')
            formatting.locator('.extra [data-format="italic"]').click()
            expect(formatting.locator('#notes')).to_have_value('*note*')
            formatting.locator('#floating-save').click()
            expect(formatting.locator('#saved')).to_contain_text('Salvato in questo browser')
            formatting.reload()
            expect(formatting.locator('#save')).to_be_enabled()
            expect(stored).to_have_value(expected_comment)
            expect(field.locator('a')).to_have_text('abc')
            if decision_field:
                expect(decision_field.locator('em')).to_have_text('scelta')
            expect(notes_field.locator('em')).to_have_text('note')
            open_delivery(formatting)
            formatting.locator('#copy').click()
            expect(formatting.locator('#action-message')).to_contain_text('Riepilogo copiato')
            formatted_summary = formatting.evaluate('navigator.clipboard.readText()')
            expected_bits = [expected_comment, '*note*']
            if decision_field:
                expected_bits.append('*scelta*')
            assert all(value in formatted_summary for value in expected_bits)
            with formatting.expect_download() as pending:
                open_delivery(formatting)
                formatting.locator('#export').click()
            formatted_export=Path(temporary)/'formatted.json'
            pending.value.save_as(str(formatted_export))
            formatted_data=json.loads(formatted_export.read_text())
            assert formatted_data['entries'][data['items'][0]['id']]['comment']==expected_comment
            if data['decisions']:
                assert formatted_data['decisions'][data['decisions'][0]['id']]['comment']=='*scelta*'
            assert formatted_data['notes']=='*note*'
            # Old drafts containing formatted text, literal Markdown characters and line breaks.
            restored='**grassetto** e *corsivo*\n[link]('+destination+')\nPercorso C:\\foto, \\*letterale\\* <img src=x>'
            formatted_data['entries'][data['items'][0]['id']]['comment']=restored
            formatted_export.write_text(json.dumps(formatted_data))
            open_delivery(formatting)
            formatting.locator('#import').set_input_files(str(formatted_export))
            expect(formatting.locator('#action-message')).to_contain_text('JSON importato')
            expect(stored).to_have_value(restored)
            expect(field.locator('strong')).to_have_text('grassetto')
            expect(field.locator('em')).to_have_text('corsivo')
            expect(field.locator('a')).to_have_text('link')
            expect(field.locator('img')).to_have_count(0)
            expect(field).to_contain_text('*letterale* <img src=x>')
            expect(notes_field.locator('em')).to_have_text('note')
            # Editing after import, Enter, undo/redo and safe paste remain in the same visible field.
            fill_plain(field, 'riga uno')
            field.press('End')
            field.press('Enter')
            field.press('Space')
            field.press('Backspace')
            field.press('x')
            expect(stored).to_have_value('riga uno\nx')
            field.press('Control+z')
            field.press('Control+Shift+z')
            expect(stored).to_have_value('riga uno\nx')
            fill_plain(field, 'riga')
            field.press('End')
            field.press('Shift+Enter')
            expect(stored).to_have_value('riga\n')
            field.press('Shift+Enter')
            expect(stored).to_have_value('riga\n\n')
            field.press('x')
            expect(stored).to_have_value('riga\n\nx')
            fill_plain(field, 'prima')
            field.press('End')
            field.press('Enter')
            field.press('Enter')
            field.press('Enter')
            field.press('x')
            expect(stored).to_have_value('prima\n\n\nx')
            formatting.locator('#floating-save').click()
            expect(formatting.locator('#saved')).to_contain_text('Salvato in questo browser')
            formatting.reload()
            expect(stored).to_have_value('prima\n\n\nx')
            expect(field).to_have_text('prima\n\n\nx')
            fill_plain(field, 'abc')
            select(field,3,3)
            card.locator('[data-format="bold"]').click()
            field.press('x')
            expect(stored).to_have_value('abc**x**')
            card.locator('[data-format="bold"]').click()
            field.press('y')
            expect(stored).to_have_value('abc**x**y')
            fill_plain(field, 'abc ')
            select(field,4,4)
            formatting.once('dialog',lambda dialog:dialog.accept(destination))
            field.press('Meta+k')
            expect(stored).to_have_value('abc [testo del link]('+destination+')')
            expect(field.locator('a')).to_have_text('testo del link')
            fill_plain(field, 'inizio ')
            field.press('End')
            field.evaluate("""box => {
                const clipboard = new DataTransfer();
                clipboard.setData('text/plain', 'testo <img src=x> *semplice*');
                clipboard.setData('text/html', '<img src=x onerror="window.injected=true"><b>testo</b>');
                box.dispatchEvent(new ClipboardEvent('paste', {bubbles:true, cancelable:true, clipboardData:clipboard}));
            }""")
            expect(field).to_have_text('inizio testo <img src=x> *semplice*')
            expect(field.locator('img,b,strong')).to_have_count(0)
            formatting.locator('#floating-save').click()
            expect(formatting.locator('#saved')).to_contain_text('Salvato in questo browser')
            formatting.reload()
            expect(field).to_have_text('inizio testo <img src=x> *semplice*')
            for width in [320,390,800,1280]:
                formatting.set_viewport_size({'width':width,'height':900})
                assert formatting.evaluate('document.documentElement.scrollWidth <= innerWidth')
                for editor in formatting.locator('.rich-editor').all():
                    box = editor.bounding_box()
                    if box is None:
                        continue  # editors in the hidden Altro overlay
                    assert box['height'] >= 200
                    assert editor.evaluate('(el)=>getComputedStyle(el).resize') == 'vertical'
                    # Format controls sit outside the field, bottom-right of its block, every width.
                    place = editor.evaluate('''(el) => {
                        const bar = el.parentElement.querySelector('.format-toolbar');
                        const eb = el.getBoundingClientRect();
                        const tb = bar.getBoundingClientRect();
                        const overlap = tb.top < eb.bottom - 1 && tb.bottom > eb.top + 1 && tb.left < eb.right - 1 && tb.right > eb.left + 1;
                        return {
                            below: tb.top >= eb.bottom - 1,
                            insetRight: Math.abs(eb.right - tb.right),
                            overlap,
                        };
                    }''')
                    assert place['below'] and not place['overlap'] and place['insetRight'] < 8, (width, place)
                if width in (390, 1280):
                    pressed = formatting.evaluate('''() => {
                      const root = document.documentElement;
                      const previous = root.getAttribute('data-theme');
                      const bold = document.querySelector('article.test [data-format="bold"]');
                      const italic = document.querySelector('article.test [data-format="italic"]');
                      bold.setAttribute('aria-pressed', 'true');
                      italic.setAttribute('aria-pressed', 'true');
                      const lum = (c) => {
                        const parts = c.match(/[\\d.]+/g).slice(0, 3).map(Number);
                        const f = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
                        const [r, g, b] = parts.map(f);
                        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
                      };
                      const ratio = (a, b) => {
                        const hi = Math.max(lum(a), lum(b));
                        const lo = Math.min(lum(a), lum(b));
                        return (hi + 0.05) / (lo + 0.05);
                      };
                      const out = {};
                      for (const theme of ['light', 'dark']) {
                        root.setAttribute('data-theme', theme);
                        const bcs = getComputedStyle(bold);
                        const ics = getComputedStyle(italic);
                        out[theme] = {
                          bold: ratio(bcs.color, bcs.backgroundColor),
                          italic: ratio(ics.color, ics.backgroundColor),
                        };
                      }
                      if (previous) root.setAttribute('data-theme', previous);
                      bold.removeAttribute('aria-pressed');
                      italic.removeAttribute('aria-pressed');
                      return out;
                    }''')
                    for theme, ratios in pressed.items():
                        assert ratios['bold'] >= 4.5 and ratios['italic'] >= 4.5, (width, theme, ratios)
            touch_context = browser.new_context(is_mobile=True,has_touch=True,viewport={'width':390,'height':844})
            touch = touch_context.new_page()
            touch.on('pageerror', lambda e: errors.append(str(e)))
            touch.goto(url)
            expect(touch.locator('#save')).to_be_enabled()
            touch_field = touch.locator('.test .rich-editor').first
            touch_field.fill('mobile')
            select(touch_field,0,6)
            touch.locator('.test').first.locator('[data-format="bold"]').tap()
            expect(touch.locator('.comment').first).to_have_value('**mobile**')
            expect(touch_field.locator('b,strong')).to_have_text('mobile')
            touch_context.close()
            formatting_context.close()
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(url)
            expect(page.locator('#save')).to_be_enabled()
            assert page.locator('.test').count() == len(data['items'])
            assert page.locator('.decision').count() == len(data['decisions'])
            open_delivery(page)
            assert page.locator('#delivery-info').count() == 0
            assert page.locator('.browse-label').count() == 0
            import_button = page.locator('.file-button')
            expected = ['Azzera tutto', 'Salva', 'Copia il riepilogo', 'Invia', 'Importa JSON', 'Esporta JSON']
            for width in [320, 390, 800, 1280]:
                page.set_viewport_size({'width': width, 'height': 900})
                controls = page.locator('#delivery-section .actions > button, #delivery-section .actions > .file-button')
                assert controls.count() == 6
                boxes = []
                for index, control in enumerate(controls.all()):
                    box = control.bounding_box()
                    boxes.append(box)
                    assert control.inner_text().strip() == expected[index], (width, index, control.inner_text())
                title_box = import_button.evaluate('''label => {
                    const range = document.createRange();
                    range.selectNode(label.firstChild);
                    const text = range.getBoundingClientRect();
                    const row = label.getBoundingClientRect();
                    return {mid: (text.left + text.right) / 2, rowMid: (row.left + row.right) / 2};
                }''')
                assert abs(title_box['mid'] - title_box['rowMid']) < 2, (width, title_box)
                for row in range(3):
                    left, right = boxes[row * 2], boxes[row * 2 + 1]
                    assert abs(left['y'] - right['y']) < 2, (width, row, left, right)
                    assert abs(left['height'] - right['height']) < 2, (width, row)
                    assert abs(left['width'] - right['width']) < 2, (width, row, left['width'], right['width'])
                    assert right['x'] > left['x'] + left['width'] * 0.5
                    if row:
                        previous = boxes[(row - 1) * 2]
                        assert left['y'] >= previous['y'] + previous['height'] - 1, (width, row)
            page.locator('#delivery-overlay-close').click()
            expect(page.locator('#delivery-overlay')).to_be_hidden()
            # Desktop: long-press Save opens the same delivery overlay as mobile.
            page.locator('#floating-save').dispatch_event('pointerdown', {'button': 0})
            page.wait_for_timeout(500)
            page.locator('#floating-save').dispatch_event('pointerup', {'button': 0})
            expect(page.locator('#delivery-overlay')).to_be_visible()
            page.locator('#delivery-overlay-close').click()
            expect(page.locator('#delivery-overlay')).to_be_hidden()
            first = page.locator('.test').first
            first.locator('[data-status="Tutto OK"]').click()
            expect(first.locator('.item-state')).to_have_text('Tutto OK')
            expect(first).to_have_class(re.compile(r'\bhas-response\b'))
            first.locator('[data-status="Tutto OK"]').click()
            expect(first.locator('.item-state')).to_have_text('Non provato')
            expect(first).not_to_have_class(re.compile(r'\bhas-response\b'))
            first.locator('.rich-editor').fill('Risposta senza esito')
            expect(first).to_have_class(re.compile(r'\bhas-response\b'))
            first.locator('.rich-editor').fill('')
            expect(first).not_to_have_class(re.compile(r'\bhas-response\b'))
            for theme in ['light', 'dark']:
                page.emulate_media(color_scheme=theme)
                first.locator('.rich-editor').fill('Solo commento, senza approvazione')
                neutral = first.evaluate('(el)=>getComputedStyle(el).backgroundColor')
                backgrounds = []
                for status in ['Tutto OK', 'Accettabile', 'Non approvato']:
                    button = first.locator('[data-status="'+status+'"]')
                    button.click()
                    border = first.evaluate('(el)=>getComputedStyle(el).borderTopColor')
                    selected = button.evaluate('(el)=>getComputedStyle(el).backgroundColor')
                    def hue(rgb):
                        values = [int(value)/255 for value in re.findall(r'\d+', rgb)[:3]]
                        return colorsys.rgb_to_hsv(*values)[0]
                    difference = abs(hue(border)-hue(selected))
                    assert min(difference, 1-difference) < 0.07, 'The card color does not match its outcome.'
                    background = first.evaluate('(el)=>getComputedStyle(el).backgroundColor')
                    assert background != neutral
                    backgrounds.append(background)
                    button.click()
                    assert first.evaluate('(el)=>getComputedStyle(el).backgroundColor') == neutral
                assert len(set(backgrounds)) == 3, 'The three outcomes use the same card color.'
                first.locator('.rich-editor').fill('')
            page.emulate_media(color_scheme='light')
            for field in page.locator('.rich-editor,textarea:not([hidden]),input:not([type="file"])').all():
                assert field.evaluate('(el)=>getComputedStyle(el).fontWeight') == '400'
                assert field.evaluate('(el)=>parseFloat(getComputedStyle(el).fontSize)') >= 18
            for href in ['tablet.html', 'settings.html']:
                link = page.locator('nav a[href="'+href+'"]')
                with page.expect_popup() as popup:
                    link.click()
                proposal = popup.value
                proposal.wait_for_load_state()
                assert proposal.locator('link[rel="icon"][type="image/svg+xml"]').get_attribute('href') == 'assets/feedback-favicon.svg?v=2'
                assert proposal.evaluate('window.opener === null')
                proposal.close()
            for link in page.locator('a').all():
                assert link.get_attribute('target') == '_blank'
                assert 'noopener' in link.get_attribute('rel')

            first.locator('[data-status="Accettabile"]').click()
            first.locator('.rich-editor').fill('Commento di verifica: <script>test</script>')
            page.locator('.extra .rich-editor').fill('Osservazioni libere di verifica')
            page.locator('#device').fill('Dispositivo di verifica')
            if page.locator('.decision').count():
                page.locator('.decision').first.locator('button').first.click()
            image = Path(temporary) / 'feedback.png'
            # An original, complete PNG is attached without image transformations.
            image.write_bytes(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aP9sAAAAASUVORK5CYII='))
            attachment_only = page.locator('.test').nth(1)
            attachment_only.locator('.images').set_input_files(str(image))
            expect(attachment_only.locator('.image-list img')).to_have_count(1)
            expect(attachment_only).to_have_class(re.compile(r'\bhas-response\b'))
            attachment_only.locator('.image-list button').click()
            expect(attachment_only).not_to_have_class(re.compile(r'\bhas-response\b'))
            if page.locator('.decision').count() > 1:
                indication_only = page.locator('.decision').nth(1)
                indication_only.locator('.rich-editor').fill('Solo commento')
                expect(indication_only).to_have_class(re.compile(r'\bhas-response\b'))
                indication_only.locator('.rich-editor').fill('')
                expect(indication_only).not_to_have_class(re.compile(r'\bhas-response\b'))
            first.locator('.images').set_input_files(str(image))
            expect(first.locator('.image-list img')).to_have_count(1)
            # SVG stays an image resource: scripts and external resources cannot execute.
            svg = Path(temporary) / 'original.svg'
            svg_bytes = b'<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"><script>parent.svgExecuted=true</script><image href="https://example.invalid/forbidden.png"/><rect width="32" height="32" fill="#43B59E"/></svg>'
            svg.write_bytes(svg_bytes)
            external = []
            page.on('request', lambda request: external.append(request.url) if 'example.invalid' in request.url else None)
            first.locator('.images').set_input_files(str(svg))
            expect(first.locator('.image-list img')).to_have_count(2)
            transfer = page.evaluate_handle('''text => {
                const transfer = new DataTransfer();
                transfer.items.add(new File([text], 'dropped.svg', {type:'application/octet-stream'}));
                return transfer;
            }''', svg_bytes.decode())
            first.dispatch_event('dragenter', {'dataTransfer': transfer})
            expect(first).to_have_class(re.compile(r'\bdrop-active\b'))
            first.dispatch_event('drop', {'dataTransfer': transfer})
            expect(first.locator('.image-list img')).to_have_count(3)
            expect(first).not_to_have_class(re.compile(r'\bdrop-active\b'))
            assert page.evaluate('window.svgExecuted === undefined')
            assert not external, 'SVG fetched an external resource.'
            page.wait_for_function("Array.from(document.querySelectorAll('.test:first-child .image-list img')).every(image => image.complete && image.naturalWidth > 0)")
            bad_svg = Path(temporary) / 'invalid.svg'
            bad_svg.write_text('<html>not an SVG</html>')
            first.locator('.images').set_input_files(str(bad_svg))
            expect(page.locator('#action-message')).to_contain_text('SVG non è valido')
            expect(first.locator('.image-list img')).to_have_count(3)
            archive = Path(temporary) / 'sources.zip'
            with zipfile.ZipFile(archive, 'w') as zipped:
                zipped.writestr('original.svg', svg_bytes)
                zipped.writestr('original.png', image.read_bytes())
            first.locator('.images').set_input_files(str(archive))
            expect(first.locator('.zip-download')).to_have_count(1)
            notes_card = page.locator('.extra')
            notes_card.locator('.images').set_input_files(str(image))
            expect(notes_card.locator('img')).to_have_count(1)
            notes_card.dispatch_event('drop', {'dataTransfer': transfer})
            expect(notes_card.locator('img')).to_have_count(2)
            notes_card.locator('.images').set_input_files({'name': 'notes.zip', 'mimeType': 'application/x-zip-compressed', 'buffer': archive.read_bytes()})
            expect(notes_card.locator('.zip-download')).to_have_count(1)
            zip_transfer = page.evaluate_handle('''bytes => {
                const transfer = new DataTransfer();
                transfer.items.add(new File([new Uint8Array(bytes)], 'dropped.zip', {type:''}));
                return transfer;
            }''', list(archive.read_bytes()))
            first.dispatch_event('drop', {'dataTransfer': zip_transfer})
            expect(first.locator('.zip-download')).to_have_count(2)
            notes_card.dispatch_event('drop', {'dataTransfer': zip_transfer})
            expect(notes_card.locator('.zip-download')).to_have_count(2)
            bad_zip = Path(temporary) / 'invalid.zip'
            bad_zip.write_bytes(b'this is not a zip')
            notes_card.locator('.images').set_input_files(str(bad_zip))
            expect(page.locator('#action-message')).to_contain_text('ZIP non è riconosciuto')
            expect(notes_card.locator('.zip-download')).to_have_count(2)
            page.locator('#floating-save').click()
            expect(page.locator('#saved')).to_contain_text('Salvato in questo browser')
            page.reload()
            expect(page.locator('#save')).to_be_enabled()
            expect(first.locator('.item-state')).to_have_text('Accettabile')
            expect(first.locator('.comment')).to_have_value('Commento di verifica: <script>test</script>')
            expect(first.locator('.image-list img')).to_have_count(3)
            expect(page.locator('#notes')).to_have_value('Osservazioni libere di verifica')
            with page.expect_download() as pending:
                open_delivery(page)
                page.locator('#export').click()
            export = Path(temporary) / 'export.json'
            pending.value.save_as(str(export))
            exported = json.loads(export.read_text())
            encoded = exported['entries'][data['items'][0]['id']]['images'][0]['data'].split(',')[1]
            assert base64.b64decode(encoded) == image.read_bytes(), 'Immagine modificata.'
            for attached in exported['entries'][data['items'][0]['id']]['images'][1:3]:
                assert attached['type'] == 'image/svg+xml'
                assert base64.b64decode(attached['data'].split(',')[1]) == svg_bytes, 'SVG originale modificato.'
            for attached in exported['entries'][data['items'][0]['id']]['images'][3:]:
                assert attached['type'] == 'application/zip'
                assert base64.b64decode(attached['data'].split(',')[1]) == archive.read_bytes()
            assert len(exported['extra']['images']) == 4
            for attached, original in zip(exported['extra']['images'], [image.read_bytes(), svg_bytes, archive.read_bytes(), archive.read_bytes()]):
                assert base64.b64decode(attached['data'].split(',')[1]) == original, 'Allegato delle osservazioni modificato.'
            expect(first).to_have_class(re.compile(r'\bhas-response\b'))
            if page.locator('.decision').count():
                expect(page.locator('.decision').first).to_have_class(re.compile(r'\bhas-response\b'))

            second_context = browser.new_context(permissions=['clipboard-read', 'clipboard-write'])
            second = second_context.new_page()
            second.on('pageerror', lambda e: errors.append(str(e)))
            second.goto(url)
            expect(second.locator('#save')).to_be_enabled()
            open_delivery(second)
            with second.expect_file_chooser() as chooser:
                second.locator('.file-button').click()
            chooser.value.set_files(str(export))
            expect(second.locator('#action-message')).to_contain_text('JSON importato')
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Accettabile')
            expect(second.locator('.test').first.locator('.image-list img')).to_have_count(3)
            second.reload()
            expect(second.locator('#save')).to_be_enabled()
            expect(second.locator('.extra img')).to_have_count(2)
            expect(second.locator('.extra .zip-download')).to_have_count(2)
            expect(second.locator('.test').first.locator('.zip-download')).to_have_count(2)
            with second.expect_download() as pending_zip:
                second.locator('.extra .zip-download').first.click()
            zip_copy = Path(temporary) / 'downloaded.zip'
            pending_zip.value.save_as(str(zip_copy))
            assert zip_copy.read_bytes() == archive.read_bytes(), 'ZIP scaricato modificato.'
            expect(second.locator('.test').first.locator('.comment')).to_have_value('Commento di verifica: <script>test</script>')
            bad = Path(temporary) / 'invalid.json'
            invalid = json.loads(export.read_text())
            invalid['entries'][data['items'][0]['id']]['status'] = 'Esito inventato'
            bad.write_text(json.dumps(invalid))
            open_delivery(second)
            second.locator('#import').set_input_files(str(bad))
            expect(second.locator('#action-message')).to_contain_text('Importazione annullata')
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Accettabile')
            open_delivery(second)
            second.locator('#send').click()
            expect(second.locator('#summary')).to_have_value(re.compile('^Feedback AIV '+re.escape(data['version'])))
            open_delivery(second)
            second.locator('#copy').click()
            expect(second.locator('#action-message')).to_contain_text('Riepilogo copiato')
            clipboard = second.evaluate('navigator.clipboard.readText()')
            assert all(name in clipboard for name in ['Osservazioni libere di verifica', 'feedback.png', 'notes.zip', 'sources.zip', 'dropped.zip'])
            confirmations = []
            def cancel_reset(dialog):
                confirmations.append((dialog.type, dialog.message))
                dialog.dismiss()
            second.once('dialog', cancel_reset)
            open_delivery(second)
            second.locator('#reset').click()
            assert confirmations and confirmations[0][0] == 'confirm'
            assert 'Cancellare tutte le risposte' in confirmations[0][1]
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Accettabile')
            expect(second.locator('.test').first.locator('.image-list img')).to_have_count(3)
            second.on('dialog', lambda dialog: dialog.accept())
            open_delivery(second)
            second.locator('#reset').click()
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Non provato')
            expect(second.locator('.test').first.locator('.image-list img')).to_have_count(0)
            expect(second.locator('.extra .image-list figure')).to_have_count(0)
            for width in [320, 390, 800, 1280]:
                second.set_viewport_size({'width': width, 'height': 900})
                assert second.evaluate('document.documentElement.scrollWidth <= innerWidth'), f'Scorrimento orizzontale a {width}px.'
            second.emulate_media(color_scheme='dark')
            second.locator('.test').first.locator('.rich-editor').fill('Solo commento')
            expect(second.locator('.test').first).to_have_class(re.compile(r'\bhas-response\b'))
            if second.locator('.decision').count():
                second.locator('.decision').first.locator('.rich-editor').fill('Solo indicazione')
                expect(second.locator('.decision').first).to_have_class(re.compile(r'\bhas-response\b'))
            for field in second.locator('.rich-editor,textarea:not([hidden]),input:not([type="file"])').all():
                assert field.evaluate('(el)=>getComputedStyle(el).fontWeight') == '400'

            second.screenshot(path='/tmp/aiv-feedback-dark.png', full_page=False)
            assert second.locator('.test').first.evaluate('(el)=>getComputedStyle(el).backgroundColor') != second.locator('.test').nth(1).evaluate('(el)=>getComputedStyle(el).backgroundColor')
            page.set_viewport_size({'width': 1100, 'height': 900})
            page.screenshot(path='/tmp/aiv-feedback-light.png', full_page=False)
            # A previous release's saved draft must survive the cumulative document update.
            legacy = json.loads(export.read_text())
            legacy['version'] = legacy['installed'] = '3.14'
            legacy.pop('extra', None)
            # Earlier drafts contain images only and have no optional extra attachments.
            for value in legacy['entries'].values():
                value['images'] = [file for file in value['images'] if file['type'].startswith('image/')]

            # Keep the first current-card answer so migration still shows Accettabile + images;
            # orphaned 3.13/3.14 keys must not crash when those cards are no longer in the doc.
            first_id = data['items'][0]['id']
            kept = legacy['entries'].get(first_id)
            legacy['entries'] = {key: value for key, value in legacy['entries'].items()
                                 if key.startswith(('3.13-', '3.14-'))}
            if kept is not None:
                legacy['entries'][first_id] = kept
            legacy['entries']['3.14-03'] = {'status': 'Tutto OK', 'comment': 'Riscontro precedente conservato', 'images': []}
            legacy.setdefault('decisions', {})
            legacy['decisions']['d-settings-order'] = {
                'choice': 'Applica la proposta',
                'comment': 'Decisione precedente conservata',
            }
            migration_context = browser.new_context()
            migration = migration_context.new_page()
            migration.on('pageerror', lambda e: errors.append(str(e)))
            migration.goto(url)
            expect(migration.locator('#save')).to_be_enabled()
            migration.evaluate("""async draft => {
                const db = await new Promise((resolve, reject) => {
                    const request = indexedDB.open('aiv-feedback', 1);
                    request.onsuccess = () => resolve(request.result);
                    request.onerror = () => reject(request.error);
                });
                await new Promise((resolve, reject) => {
                    const transaction = db.transaction('drafts', 'readwrite');
                    transaction.objectStore('drafts').put(draft, 'current');
                    transaction.oncomplete = resolve;
                    transaction.onerror = () => reject(transaction.error);
                });
                db.close();
            }""", legacy)
            migration.reload()
            expect(migration.locator('#save')).to_be_enabled()
            expect(migration.locator(f'[data-id="{first_id}"] .item-state')).to_have_text('Accettabile')
            expect(migration.locator('[data-id="3.14-03"]')).to_have_count(0)
            if migration.locator('.decision').count():
                expect(migration.locator('.decision').first.locator('textarea')).to_have_value('Decisione precedente conservata')
                expect(migration.locator('.decision').first.locator('button[aria-pressed="true"]')).to_have_text(legacy['decisions']['d-settings-order']['choice'])
            expect(migration.locator('#installed-confirm')).not_to_be_checked()
            expect(migration.locator('#giro-version')).to_have_text(data['version'])
            expect(migration.locator('.test').first.locator('.image-list img')).to_have_count(3)
            for item in data['items']:
                if item['id'] == first_id:
                    continue
                if item['version'] == data['version']:
                    expect(migration.locator(f'[data-id="{item["id"]}"] .item-state')).to_have_text('Non provato')
            migration_context.close()
            browser.close()
        assert not errors, 'Errori nella pagina: '+str(errors)
        print(f'{len(data["items"])} prove, {len(data["decisions"])} decisioni: forma, browser, salvataggio, immagini, SVG e ZIP originali nei riquadri e nelle osservazioni, trascinamento, download, nuove schede, campi, allineamento, numerazione e navigazione mobile, formattazione e scorciatoie, conferma, colori degli esiti, evidenze, JSON, clipboard e larghezze verificati.')
    finally:
        server.shutdown()
        server.server_close()


if __name__ == '__main__':
    import sys
    check(sys.argv[1])
