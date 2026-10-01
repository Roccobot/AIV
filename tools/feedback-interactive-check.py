#!/usr/bin/env python3
"""Validate and exercise the actual Codex feedback page, including persistence and import."""
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
                expect(navigation.locator('#first-empty')).to_be_hidden()
                save_box = navigation.locator('#floating-save').bounding_box()
                for ident in ['previous-card','next-card']:
                    box = navigation.locator('#'+ident).bounding_box()
                    assert box['y']+box['height'] < save_box['y']
                    assert box['width'] >= 48 and box['height'] >= 48
            navigation.evaluate('''() => {
                const previous = document.querySelector('#tests .test:last-child').getBoundingClientRect();
                const next = document.querySelector('.decision').getBoundingClientRect();
                const offset = document.querySelector('.dashboard').getBoundingClientRect().height + 12;
                window.scrollTo(0, window.scrollY + (previous.bottom + next.top)/2 - offset);
            }''')
            navigation.locator('#previous-card').tap()
            aligned(navigation.locator('.test').last)
            navigation.locator('.test').nth(0).locator('.comment').fill('Solo commento')
            navigation.locator('.test').nth(1).locator('[data-status="Non approvato"]').click()
            navigation.locator('.test').nth(3).locator('.comment').fill('Più in basso')
            navigation.locator('#next-card').tap()
            navigation.locator('#first-empty').tap()
            aligned(navigation.locator('.test').nth(2))
            # Fill through normal input handlers; navigation must update without a reload.
            for field in navigation.locator('.test .comment,.decision textarea').all():
                field.fill('Risposta di verifica')
            expect(navigation.locator('#first-empty')).to_be_hidden()
            navigation.locator('.test').nth(1).locator('[data-status="Non approvato"]').click()
            navigation.locator('.test').nth(1).locator('.comment').fill('')
            navigation.locator('#next-card').tap()
            expect(navigation.locator('#first-empty')).to_be_visible()
            navigation.locator('#first-empty').tap()
            aligned(navigation.locator('.test').nth(1))
            last = navigation.locator('.extra')
            last.scroll_into_view_if_needed()
            navigation.evaluate('window.scrollTo(0, document.body.scrollHeight)')
            expect(navigation.locator('#next-card')).to_be_disabled()
            navigation.locator('#previous-card').tap()
            aligned(navigation.locator('.decision').last)
            navigation_context.close()
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(url)
            expect(page.locator('#save')).to_be_enabled()
            assert page.locator('.test').count() == len(data['items'])
            assert page.locator('.decision').count() == len(data['decisions'])
            import_button = page.locator('.file-button')
            for width in [320, 390, 800, 1280]:
                page.set_viewport_size({'width': width, 'height': 900})
                import_box = import_button.bounding_box()
                for button in page.locator('.actions button').all():
                    assert abs(button.bounding_box()['height'] - import_box['height']) < 1, 'Importa JSON height differs from its peers.'
                title_right = import_button.evaluate('''label => {
                    const range = document.createRange();
                    range.selectNode(label.firstChild);
                    return range.getBoundingClientRect().right;
                }''')
                browse = import_button.locator('.browse-label').bounding_box()
                assert browse['x'] > title_right, 'Sfoglia is not beside the title.'
                assert abs(browse['y'] + browse['height']/2 - import_box['y'] - import_box['height']/2) < 1
            first = page.locator('.test').first
            first.locator('[data-status="Tutto OK"]').click()
            expect(first.locator('.item-state')).to_have_text('Tutto OK')
            expect(first).to_have_class(re.compile(r'\bhas-response\b'))
            first.locator('[data-status="Tutto OK"]').click()
            expect(first.locator('.item-state')).to_have_text('Non provato')
            expect(first).not_to_have_class(re.compile(r'\bhas-response\b'))
            first.locator('.comment').fill('Risposta senza esito')
            expect(first).to_have_class(re.compile(r'\bhas-response\b'))
            first.locator('.comment').fill('')
            expect(first).not_to_have_class(re.compile(r'\bhas-response\b'))
            for theme in ['light', 'dark']:
                page.emulate_media(color_scheme=theme)
                first.locator('.comment').fill('Solo commento, senza approvazione')
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
                first.locator('.comment').fill('')
            page.emulate_media(color_scheme='light')
            for field in page.locator('textarea,input:not([type="file"])').all():
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
            first.locator('.comment').fill('Commento di verifica: <script>test</script>')
            page.locator('#notes').fill('Osservazioni libere di verifica')
            page.locator('#device').fill('Dispositivo di verifica')
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
            indication_only = page.locator('.decision').nth(1)
            indication_only.locator('textarea').fill('Solo commento')
            expect(indication_only).to_have_class(re.compile(r'\bhas-response\b'))
            indication_only.locator('textarea').fill('')
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
            page.locator('#save').click()
            expect(page.locator('#saved')).to_contain_text('Salvato in questo browser')
            page.reload()
            expect(page.locator('#save')).to_be_enabled()
            expect(first.locator('.item-state')).to_have_text('Accettabile')
            expect(first.locator('.comment')).to_have_value('Commento di verifica: <script>test</script>')
            expect(first.locator('.image-list img')).to_have_count(3)
            expect(page.locator('#notes')).to_have_value('Osservazioni libere di verifica')
            with page.expect_download() as pending:
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
            expect(page.locator('.decision').first).to_have_class(re.compile(r'\bhas-response\b'))

            second_context = browser.new_context(permissions=['clipboard-read', 'clipboard-write'])
            second = second_context.new_page()
            second.on('pageerror', lambda e: errors.append(str(e)))
            second.goto(url)
            expect(second.locator('#save')).to_be_enabled()
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
            second.locator('#import').set_input_files(str(bad))
            expect(second.locator('#action-message')).to_contain_text('Importazione annullata')
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Accettabile')
            second.locator('#send').click()
            expect(second.locator('#summary')).to_have_value(re.compile('^Feedback AIV '+re.escape(data['version'])))
            second.locator('#copy').click()
            expect(second.locator('#action-message')).to_contain_text('Riepilogo copiato')
            clipboard = second.evaluate('navigator.clipboard.readText()')
            assert all(name in clipboard for name in ['Osservazioni libere di verifica', 'feedback.png', 'notes.zip', 'sources.zip', 'dropped.zip'])
            confirmations = []
            def cancel_reset(dialog):
                confirmations.append((dialog.type, dialog.message))
                dialog.dismiss()
            second.once('dialog', cancel_reset)
            second.locator('#reset').click()
            assert confirmations and confirmations[0][0] == 'confirm'
            assert 'Cancellare tutte le risposte' in confirmations[0][1]
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Accettabile')
            expect(second.locator('.test').first.locator('.image-list img')).to_have_count(3)
            second.on('dialog', lambda dialog: dialog.accept())
            second.locator('#reset').click()
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Non provato')
            expect(second.locator('.test').first.locator('.image-list img')).to_have_count(0)
            expect(second.locator('.extra .image-list figure')).to_have_count(0)
            for width in [320, 390, 800, 1280]:
                second.set_viewport_size({'width': width, 'height': 900})
                assert second.evaluate('document.documentElement.scrollWidth <= innerWidth'), f'Scorrimento orizzontale a {width}px.'
            second.emulate_media(color_scheme='dark')
            second.locator('.test').first.locator('.comment').fill('Solo commento')
            expect(second.locator('.test').first).to_have_class(re.compile(r'\bhas-response\b'))
            second.locator('.decision').first.locator('textarea').fill('Solo indicazione')
            expect(second.locator('.decision').first).to_have_class(re.compile(r'\bhas-response\b'))
            for field in second.locator('textarea,input:not([type="file"])').all():
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

            legacy['entries'] = {key: value for key, value in legacy['entries'].items()
                                 if key.startswith(('3.13-', '3.14-'))}
            legacy['entries']['3.14-03'] = {'status': 'Tutto OK', 'comment': 'Riscontro precedente conservato', 'images': []}
            legacy['decisions']['d-settings-order']['comment'] = 'Decisione precedente conservata'
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
            expect(migration.locator('[data-id="3.13-01"] .item-state')).to_have_text('Accettabile')
            expect(migration.locator('[data-id="3.14-03"] .comment')).to_have_value('Riscontro precedente conservato')
            expect(migration.locator('.decision').first.locator('textarea')).to_have_value('Decisione precedente conservata')
            expect(migration.locator('.decision').first.locator('button[aria-pressed="true"]')).to_have_text(legacy['decisions']['d-settings-order']['choice'])
            expect(migration.locator('#installed')).to_have_value('3.14')
            expect(migration.locator('.test').first.locator('.image-list img')).to_have_count(3)
            for item in data['items']:
                if item['version'] == data['version']:
                    expect(migration.locator(f'[data-id="{item["id"]}"] .item-state')).to_have_text('Non provato')
            migration_context.close()
            browser.close()
        assert not errors, 'Errori nella pagina: '+str(errors)
        print(f'{len(data["items"])} prove, {len(data["decisions"])} decisioni: forma, browser, salvataggio, immagini, SVG e ZIP originali nei riquadri e nelle osservazioni, trascinamento, download, nuove schede, campi, allineamento, numerazione e navigazione mobile, conferma, colori degli esiti, evidenze, JSON, clipboard e larghezze verificati.')
    finally:
        server.shutdown()
        server.server_close()


if __name__ == '__main__':
    import sys
    check(sys.argv[1])
