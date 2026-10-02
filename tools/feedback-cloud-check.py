"""Exercise the real Worker and its local Supabase/PostgreSQL service with isolated browser sessions.

Start Wrangler with the development-only secret documented in cloud/feedback/README.md.
This check is restricted to localhost and never uses a production login or storage.
"""
import base64
import hashlib
import hmac
import io
import json
from pathlib import Path
import re
import shutil
import sys
import tempfile
import time
from urllib.parse import urlparse
import zipfile
from playwright.sync_api import sync_playwright, expect

origin = sys.argv[1].rstrip('/') if len(sys.argv)>1 else 'http://127.0.0.1:8787'
assert urlparse(origin).hostname in ['127.0.0.1','localhost'], 'Usare solo il Worker di sviluppo locale.'

def open_delivery(page):
    overlay = page.locator('#delivery-overlay')
    if overlay.is_visible():
        return
    btn = page.locator('#open-delivery')
    if btn.count() and btn.is_visible():
        btn.click()
    else:
        fab = page.locator('#floating-save')
        fab.dispatch_event('pointerdown', {'button': 0})
        page.wait_for_timeout(500)
        fab.dispatch_event('pointerup', {'button': 0})
    expect(overlay).to_be_visible()

def encoded(value):
    return base64.urlsafe_b64encode(value).rstrip(b'=').decode()
body = encoded(json.dumps({'kind':'session','owner':10722164,'username':'Roccobot','exp':time.time()+3600}).encode())
session = body+'.'+encoded(hmac.new(b'development-test-secret',body.encode(),hashlib.sha256).digest())
with sync_playwright() as pw, tempfile.TemporaryDirectory() as temporary:
    browser = pw.chromium.launch(executable_path=shutil.which('chromium'),args=['--no-sandbox'])
    errors=[]
    def context():
        ctx = browser.new_context(permissions=['clipboard-read','clipboard-write'])
        # Emulate the weak ETags produced by edge compression; HEAD can stay strong.
        def compressed(route):
            response=route.fetch()
            headers=dict(response.headers)
            if route.request.method in ['GET','PUT'] and headers.get('etag'):
                headers['etag']='W/'+headers['etag']
            route.fulfill(response=response,headers=headers)
        ctx.route('**/api/feedback',compressed)
        ctx.add_cookies([{'name':'__Host-aiv-session','value':session,'url':'https://'+urlparse(origin).hostname+'/','secure':True,'httpOnly':True,'sameSite':'Lax'}])
        return ctx
    def page(ctx):
        result=ctx.new_page()
        result.on('pageerror',lambda error:errors.append(str(error)))
        result.goto(origin+'/feedback.html')
        expect(result.locator('#save')).to_be_enabled()
        expect(result.locator('.cloud-account-logo')).to_be_visible()
        expect(result.locator('.cloud-account-user')).to_have_text('Roccobot')
        expect(result.locator('.cloud-account')).not_to_contain_text('Accedi')
        expect(result.locator('.cloud-account')).to_contain_text('Esci')
        return result

    def comment_editor(target):
        # First test-card comment (historical `.rich-editor`.first before Altro mirrors).
        return target.locator('.test').first.locator('.rich-editor')
    def altro_editor(target):
        # Visible Altro free-text (desktop rail, bottom card, or open mobile menu).
        return target.locator('#notes-editor, #notes-mobile-editor').locator('visible=true').first
    def save(page):
        page.locator('#floating-save').click()
        expect(page.locator('#saved')).to_contain_text('Salvato nel cloud')
    first_context=context()
    first=page(first_context)
    # A new test run starts with the current draft, then explicitly resets its test namespace.
    first.once('dialog',lambda dialog:dialog.accept())
    open_delivery(first)
    first.locator('#reset').click()
    expect(first.locator('#saved')).to_contain_text('Salvato nel cloud')
    legacy=Path(temporary)/'legacy.json'
    legacy.write_text(json.dumps({'schema':1,'project':'AIV','version':'3.24','installed':'3.24',
                                 'device':'Telefono precedente','notes':'','entries':{},'decisions':{},'extra':{'images':[]}}))
    open_delivery(first)
    with first.expect_file_chooser() as chooser:
        first.locator('.file-button').click()
    chooser.value.set_files(str(legacy))
    expect(first.locator('#device')).to_have_value('Telefono precedente')
    expect(first.locator('#tablet')).to_have_value('')
    expect(first.locator('#saved')).to_contain_text('Salvato nel cloud')
    first.locator('#device').fill('Telefono di prova, Android 13')
    first.locator('#tablet').fill('Tablet di prova, Android 15', timeout=2000)
    comment_editor(first).fill('Da telefono')
    save(first)
    assert first.evaluate('window.feedbackRemote.hasUpdates()') is False
    open_delivery(first)
    first.locator('#send').click()
    expect(first.locator('#summary')).to_have_value(re.compile('Telefono: Telefono di prova, Android 13'))
    expect(first.locator('#summary')).to_have_value(re.compile('Tablet: Tablet di prova, Android 15'))
    assert first.evaluate('localStorage.length')==0
    assert first.evaluate('indexedDB.databases().then(values=>values.length)')==0
    second_context=context()
    second=page(second_context)
    expect(second.locator('#device')).to_have_value('Telefono di prova, Android 13')
    expect(second.locator('#tablet')).to_have_value('Tablet di prova, Android 15')
    expect(comment_editor(second)).to_have_text('Da telefono')
    expect(second.locator('#saved')).to_contain_text('ripristinate dal cloud')
    comment_editor(second).fill('Da tablet')
    save(second)
    first.evaluate('window.dispatchEvent(new Event("focus"))')
    expect(comment_editor(first)).to_have_text('Da tablet')
    # Both editors share a revision. A stale write must preserve both the newer cloud draft and local text.
    comment_editor(first).fill('Modifica contemporanea')
    first.evaluate('clearTimeout(saveTimer)')
    comment_editor(second).fill('Ultima versione sul tablet')
    save(second)
    first.locator('#floating-save').click()
    expect(first.locator('#saved')).to_contain_text('versione salvata nel cloud è cambiata')
    expect(comment_editor(first)).to_have_text('Modifica contemporanea')
    first_context.close()
    # Files go to storage once; metadata saves do not re-upload them.
    put_files=[]
    second.on('request',lambda request:put_files.append(request.url) if request.method=='PUT' and '/api/files/' in request.url else None)
    archive=io.BytesIO()
    with zipfile.ZipFile(archive,'w') as container:
        container.writestr('nome originale.txt','File originale')
    svg=b'<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"><circle cx="10" cy="10" r="8" fill="#43B59E"/></svg>'
    second.locator('.test').first.locator('.images').set_input_files([{'name':'disegno originale.svg','mimeType':'image/svg+xml','buffer':svg},{'name':'fonti originali.zip','mimeType':'application/zip','buffer':archive.getvalue()}])
    save(second)
    assert len(put_files)==2, put_files
    altro_editor(second).fill('**Testo letterale**')
    save(second)
    assert len(put_files)==2, put_files
    wire=second.evaluate('fetch("/api/feedback").then(response=>response.json())')
    first_id=second.evaluate('() => document.querySelector(".test").dataset.id')
    assert all('data' not in file and len(file['storageKey'])==64 for file in wire['entries'][first_id]['images'])
    third_context=context()
    third=page(third_context)
    expect(comment_editor(third)).to_have_text('Ultima versione sul tablet')
    expect(third.locator('.test').first.locator('img')).to_have_count(1)
    expect(third.locator('.test').first.locator('.zip-download')).to_have_count(1)
    open_delivery(third)
    with third.expect_download() as pending:
        third.locator('#export').click()
    output=Path(temporary)/'cloud.json'
    pending.value.save_as(str(output))
    exported=json.loads(output.read_text())
    assert exported['device']=='Telefono di prova, Android 13'
    assert exported['tablet']=='Tablet di prova, Android 15'
    attached=exported['entries'][first_id]['images']
    assert [file['name'] for file in attached]==['disegno originale.svg','fonti originali.zip']
    assert base64.b64decode(attached[0]['data'].split(',')[1])==svg
    assert base64.b64decode(attached[1]['data'].split(',')[1])==archive.getvalue()
    assert third.evaluate('indexedDB.databases().then(values=>values.length)')==0
    # Failure remains visible; it must not pretend to be a successful cloud save.
    third.route('**/api/feedback',lambda route:route.abort() if route.request.method=='PUT' else route.continue_())
    altro_editor(third).fill('Modifica senza connessione')
    third.locator('#floating-save').click()
    expect(third.locator('#saved')).to_contain_text('Non salvato')
    expect(altro_editor(third)).to_have_text('Modifica senza connessione')
    open_delivery(third)
    third.locator('#send').click()
    expect(third.locator('#action-message')).to_contain_text('Invio non confermato')
    assert third.evaluate('fetch("/api/feedback").then(response=>response.json()).then(draft=>draft.completed)') is None
    third.unroute('**/api/feedback')
    third.evaluate('window.dispatchEvent(new Event("online"))')
    expect(third.locator('#saved')).to_contain_text('Salvato nel cloud')
    third_context.close()
    second_context.close()
    anonymous_context=browser.new_context()
    anonymous=anonymous_context.new_page()
    anonymous.goto(origin+'/feedback.html')
    expect(anonymous.get_by_role('link',name='Accedi')).to_be_visible()
    expect(anonymous.locator('.cloud-account p')).to_have_text('Collegati con GitHub per il salvataggio cloud')
    expect(anonymous.locator('.cloud-account-logo')).to_be_hidden()
    expect(anonymous.locator('.cloud-account-user')).to_be_hidden()
    expect(anonymous.locator('#save')).to_be_disabled()
    expect(anonymous.locator('#saved')).to_contain_text('Accedi con GitHub')
    assert anonymous.locator('button[data-status]').first.evaluate('element => getComputedStyle(element).cursor') == 'default'
    assert anonymous.locator('.file-button').evaluate('element => getComputedStyle(element).opacity') == '0.55'
    assert anonymous.locator('.file-button').evaluate('element => getComputedStyle(element).cursor') == 'default'
    assert anonymous.evaluate('indexedDB.databases().then(values=>values.length)')==0
    anonymous_context.close()
    assert not errors,errors
    browser.close()
print('Cloud locale: due dispositivi isolati, sincronizzazione, Supabase locale e PostgreSQL reale, conflitto senza sovrascrittura, SVG/ZIP originali, caricamento unico, JSON, assenza di memoria locale, accesso riservato ed errore di rete verificati.')
