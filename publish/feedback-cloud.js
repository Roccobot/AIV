"use strict";
(() => {
  const config = window.feedbackCloudConfig;
  if (!config) return;
  const uploads = new Map();
  /*
   * The attachments already verified, by storage key, since 6.29 (H3 of the 6.25 review). The key
   * is the hash of the content, so an attachment never changes: a reload of the draft takes the
   * ones it has from here, and asks the Worker only for the new ones. Until 6.28 every reload,
   * that is every save on the other device, downloaded them all again, up to 20 MB, and with
   * `no-store` even the browser's cache was skipped.
   */
  const verified = new Map();
  const files = draft => [...Object.values(draft.entries),draft.extra || {images:[]}].flatMap(entry => entry.images);
  // Strings are immutable: copy the mutable containers without duplicating attachment payloads.
  const snapshot = draft => ({...draft,
    entries:Object.fromEntries(Object.entries(draft.entries).map(([id,entry])=>[id,{...entry,images:entry.images.map(file=>({...file}))}])),
    decisions:Object.fromEntries(Object.entries(draft.decisions).map(([id,entry])=>[id,{...entry}])),
    extra:{images:(draft.extra?.images || []).map(file=>({...file}))}
  });
  let etag = null;
  // Compare the saved revision, independent of HTTP compression at the edge.
  const revisionTag = response => response.headers.get('ETag')?.replace(/^W\//,'') || null;
  async function request(path, options = {}) {
    const response = await fetch(path,{credentials:'same-origin',cache:'no-store',...options});
    if (!response.ok) {
      let text = 'Servizio cloud non disponibile.';
      try { text = (await response.json()).error || text; } catch { /* Preserve the fallback. */ }
      const error = Error(text);
      error.status = response.status;
      throw error;
    }
    return response;
  }
  async function parallel(values, callback) {
    let next = 0;
    await Promise.all(Array.from({length:Math.min(3,values.length)},async () => {
      while (next < values.length) await callback(values[next++]);
    }));
  }
  async function load(validate = value => value) {
    const response = await request(config.endpoint);
    const loadedEtag = revisionTag(response);
    const draft = await response.json();
    if (draft.draft === null) { etag = loadedEtag; return null; }
    const kept = new Map();
    await parallel(files(draft),async file => {
      if (!/^[a-f0-9]{64}$/.test(file.storageKey)) throw Error('Riferimento allegato non valido.');
      let blob = verified.get(file.storageKey);
      if (!blob || blob.size !== file.size) {
        // The Worker answers attachments as private and immutable: the browser's cache may serve them.
        const response = await request(config.files+file.storageKey,{cache:'default'});
        const bytes = await response.arrayBuffer();
        const digest = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),byte => byte.toString(16).padStart(2,'0')).join('');
        if (digest !== file.storageKey || bytes.byteLength !== file.size) throw Error('Allegato cloud incompleto.');
        // The original bytes as a file; the page never turns them into text.
        blob = new Blob([bytes],{type:file.type});
      }
      kept.set(file.storageKey,blob);
      file.blob = blob;
      uploads.set(blob,Promise.resolve(file.storageKey));
      delete file.storageKey;
    });
    verified.clear();
    for (const [key,blob] of kept) verified.set(key,blob);
    const clean = validate(draft);
    etag = loadedEtag;
    return clean;
  }
  async function upload(file) {
    // Uploads are keyed by the file object: the same attachment is sent once.
    if (!uploads.has(file.blob)) {
      const pending = (async () => {
        const bytes = await file.blob.arrayBuffer();
        const digest = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),byte => byte.toString(16).padStart(2,'0')).join('');
        await request(config.files+digest,{method:'PUT',headers:{'Content-Type':'application/octet-stream'},body:bytes});
        verified.set(digest,file.blob);
        return digest;
      })();
      uploads.set(file.blob,pending);
      pending.catch(() => uploads.delete(file.blob));
    }
    return uploads.get(file.blob);
  }
  async function save(snapshot) {
    if (!etag) throw Error('Carica prima il documento cloud.');
    const wire = window.feedbackRemote.snapshot(snapshot);
    await parallel(files(wire),async file => {
      file.storageKey = await upload(file);
      delete file.blob;
    });
    const response = await request(config.endpoint,{method:'PUT',headers:{'Content-Type':'application/json','If-Match':etag},body:JSON.stringify(wire)});
    etag = revisionTag(response);
    const active = new Set(files(snapshot).map(file=>file.blob));
    for (const data of uploads.keys()) if (!active.has(data)) uploads.delete(data);
    return response.json();
  }
  async function hasUpdates() {
    return revisionTag(await request(config.endpoint,{method:'HEAD'})) !== etag;
  }
  const notice = document.createElement('section');
  notice.className = 'cloud-account';
  notice.setAttribute('aria-label','Account GitHub');
  const logo = document.createElement('img');
  logo.className = 'cloud-account-logo';
  logo.src = 'assets/github-mark.svg';
  logo.alt = 'GitHub';
  logo.hidden = true;
  const username = document.createElement('strong');
  username.className = 'cloud-account-user';
  username.hidden = true;
  // Signed out, the chip is a single pill on the right of the title row.
  const login = document.createElement('a');
  login.href = config.login;
  login.className = 'cloud-account-action';
  login.textContent = 'Accedi con GitHub';
  const logout = document.createElement('button');
  logout.type = 'button';
  logout.className = 'cloud-account-action';
  logout.textContent = 'Esci';
  logout.hidden = true;
  logout.addEventListener('click',async () => {
    try {
      if (!await window.feedbackSaveForLogout?.()) return;
      await request(config.logout,{method:'POST'});
      location.reload();
    } catch (error) { report(error.message,true); }
  });
  notice.append(logo,username,login,logout);
  document.querySelector('.intro-title-row').append(notice);
  let accountUsername = '';
  const syncAccount = (signedIn, name) => {
    if (typeof name === 'string' && name.trim()) accountUsername = name.trim();
    if (!signedIn) accountUsername = '';
    const connected = Boolean(signedIn && accountUsername);
    notice.classList.toggle('is-connected',connected);
    logo.hidden = !connected;
    username.hidden = !connected;
    username.textContent = accountUsername;
    // ⚠️ Emptied as well as hidden: the hidden text is still in the chip's text, and a reader of
    // the chip (or the cloud check) would find 'Accedi' while signed in.
    login.textContent = connected ? '' : 'Accedi con GitHub';
    login.hidden = connected;
    logout.hidden = !connected;
  };
  request(config.account).then(response => response.json()).then(account => {
    syncAccount(account.authenticated === true,account.username);
  }).catch(() => { /* The document load reports cloud errors separately. */ });
  login.addEventListener('click',event => {
    if (window.feedbackHasUnsaved?.() && !confirm('Le modifiche non sono salvate. Esporta le risposte prima di accedere di nuovo. Continuare?')) event.preventDefault();
  });
  window.feedbackRemote = {
    load,save,hasUpdates,snapshot,
    // The cloud revision the page holds: a save starts from it (the safety copy keeps it).
    revision: () => etag,
    account: syncAccount,
    failed: error => {
      if (error.status === 401) {
        syncAccount(false);
      }
    }
  };
})();
