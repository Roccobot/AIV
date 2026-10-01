#!/usr/bin/env python3
"""Validate and exercise the actual Codex feedback page, including persistence and import."""
import base64
import functools
import http.server
import json
from pathlib import Path
import re
import shutil
import tempfile
import threading


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
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(url)
            expect(page.locator('#save')).to_be_enabled()
            assert page.locator('.test').count() == len(data['items'])
            assert page.locator('.decision').count() == len(data['decisions'])
            first = page.locator('.test').first
            first.locator('[data-status="Tutto OK"]').click()
            expect(first.locator('.item-state')).to_have_text('Tutto OK')
            first.locator('[data-status="Tutto OK"]').click()
            expect(first.locator('.item-state')).to_have_text('Non provato')
            first.locator('[data-status="Accettabile"]').click()
            first.locator('.comment').fill('Commento di verifica: <script>test</script>')
            page.locator('#notes').fill('Osservazioni libere di verifica')
            page.locator('#device').fill('Dispositivo di verifica')
            page.locator('.decision').first.locator('button').first.click()
            image = Path(temporary) / 'feedback.png'
            # An original, complete PNG is attached without image transformations.
            image.write_bytes(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aP9sAAAAASUVORK5CYII='))
            first.locator('.images').set_input_files(str(image))
            expect(first.locator('.image-list img')).to_have_count(1)
            page.locator('#save').click()
            expect(page.locator('#saved')).to_contain_text('Salvato in questo browser')
            page.reload()
            expect(page.locator('#save')).to_be_enabled()
            expect(first.locator('.item-state')).to_have_text('Accettabile')
            expect(first.locator('.comment')).to_have_value('Commento di verifica: <script>test</script>')
            expect(first.locator('.image-list img')).to_have_count(1)
            expect(page.locator('#notes')).to_have_value('Osservazioni libere di verifica')
            with page.expect_download() as pending:
                page.locator('#export').click()
            export = Path(temporary) / 'export.json'
            pending.value.save_as(str(export))
            exported = json.loads(export.read_text())
            encoded = exported['entries'][data['items'][0]['id']]['images'][0]['data'].split(',')[1]
            assert base64.b64decode(encoded) == image.read_bytes(), 'Immagine modificata.'
            second_context = browser.new_context(permissions=['clipboard-read', 'clipboard-write'])
            second = second_context.new_page()
            second.on('pageerror', lambda e: errors.append(str(e)))
            second.goto(url)
            expect(second.locator('#save')).to_be_enabled()
            second.locator('#import').set_input_files(str(export))
            expect(second.locator('#action-message')).to_contain_text('JSON importato')
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Accettabile')
            expect(second.locator('.test').first.locator('.image-list img')).to_have_count(1)
            second.reload()
            expect(second.locator('#save')).to_be_enabled()
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
            assert 'Osservazioni libere di verifica' in clipboard and 'feedback.png' in clipboard
            second.on('dialog', lambda dialog: dialog.accept())
            second.locator('#reset').click()
            expect(second.locator('.test').first.locator('.item-state')).to_have_text('Non provato')
            expect(second.locator('.test').first.locator('.image-list img')).to_have_count(0)
            for width in [320, 390, 800, 1280]:
                second.set_viewport_size({'width': width, 'height': 900})
                assert second.evaluate('document.documentElement.scrollWidth <= innerWidth'), f'Scorrimento orizzontale a {width}px.'
            second.emulate_media(color_scheme='dark')
            second.screenshot(path='/tmp/aiv-feedback-dark.png', full_page=False)
            page.set_viewport_size({'width': 1100, 'height': 900})
            page.screenshot(path='/tmp/aiv-feedback-light.png', full_page=False)
            # A previous release's saved draft must survive the cumulative document update.
            legacy = json.loads(export.read_text())
            legacy['version'] = legacy['installed'] = '3.14'
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
            expect(migration.locator('.test').first.locator('.image-list img')).to_have_count(1)
            for item in data['items']:
                if item['version'] == data['version']:
                    expect(migration.locator(f'[data-id="{item["id"]}"] .item-state')).to_have_text('Non provato')
            migration_context.close()
            browser.close()
        assert not errors, 'Errori nella pagina: '+str(errors)
        print(f'{len(data["items"])} prove, {len(data["decisions"])} decisioni: forma, browser, salvataggio, immagini, JSON, clipboard e larghezze verificati.')
    finally:
        server.shutdown()
        server.server_close()


if __name__ == '__main__':
    import sys
    check(sys.argv[1])
