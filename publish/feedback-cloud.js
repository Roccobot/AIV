"use strict";
(() => {
  const config = window.feedbackCloudConfig;
  if (!config) return;
  const uploads = new Map();
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
  async function dataURL(bytes, type) {
    return new Promise((resolve,reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = () => reject(Error('Allegato non leggibile.'));
      reader.readAsDataURL(new Blob([bytes],{type}));
    });
  }
  async function load(validate = value => value) {
    const response = await request(config.endpoint);
    const loadedEtag = revisionTag(response);
    const draft = await response.json();
    if (draft.draft === null) { etag = loadedEtag; return null; }
    await parallel(files(draft),async file => {
      if (!/^[a-f0-9]{64}$/.test(file.storageKey)) throw Error('Riferimento allegato non valido.');
      const response = await request(config.files+file.storageKey);
      const bytes = await response.arrayBuffer();
      const digest = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),byte => byte.toString(16).padStart(2,'0')).join('');
      if (digest !== file.storageKey || bytes.byteLength !== file.size) throw Error('Allegato cloud incompleto.');
      file.data = await dataURL(bytes,file.type);
      uploads.set(file.data,Promise.resolve(file.storageKey));
      delete file.storageKey;
    });
    const clean = validate(draft);
    etag = loadedEtag;
    return clean;
  }
  async function upload(file) {
    if (!uploads.has(file.data)) {
      const pending = (async () => {
        const bytes = await (await fetch(file.data)).arrayBuffer();
        const digest = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),byte => byte.toString(16).padStart(2,'0')).join('');
        await request(config.files+digest,{method:'PUT',headers:{'Content-Type':'application/octet-stream'},body:bytes});
        return digest;
      })();
      uploads.set(file.data,pending);
      pending.catch(() => uploads.delete(file.data));
    }
    return uploads.get(file.data);
  }
  async function save(snapshot) {
    if (!etag) throw Error('Carica prima il documento cloud.');
    const wire = window.feedbackRemote.snapshot(snapshot);
    await parallel(files(wire),async file => {
      file.storageKey = await upload(file);
      delete file.data;
    });
    const response = await request(config.endpoint,{method:'PUT',headers:{'Content-Type':'application/json','If-Match':etag},body:JSON.stringify(wire)});
    etag = revisionTag(response);
    const active = new Set(files(snapshot).map(file=>file.data));
    for (const data of uploads.keys()) if (!active.has(data)) uploads.delete(data);
    return response.json();
  }
  async function hasUpdates() {
    return revisionTag(await request(config.endpoint,{method:'HEAD'})) !== etag;
  }
  document.querySelector('#persistence-info').textContent = 'Le risposte e gli allegati si salvano nel cloud privato: puoi continuare da un altro dispositivo accedendo con GitHub. Gli allegati restano originali, massimo 8 MB ciascuno e 20 MB totali. Attendi la conferma prima di chiudere; se manca la connessione, esporta il JSON per conservare le modifiche.';
  document.querySelector('#send-info').textContent = 'Invia rende leggibile il giro all\'agente, senza avviare nulla. Puoi continuare a modificare e inviare di nuovo. L\'agente recupererà l\'ultima versione inviata solo dopo il tuo via in chat.';
  document.querySelector('#delivery-info').textContent = 'Premi Invia anche se alcune prove restano Non provato. Quando vuoi che l\'agente prenda in carico il giro, chiedigli in chat di leggere l\'ultimo feedback. Esportare il JSON è facoltativo: serve per conservarne una copia.';
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
  const description = document.createElement('p');
  const signedOutText = 'Collegati con GitHub per il salvataggio cloud';
  description.textContent = signedOutText;
  const login = document.createElement('a');
  login.href = config.login;
  login.className = 'cloud-account-action';
  login.textContent = 'Accedi';
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
  notice.append(logo,username,description,login,logout);
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
    description.textContent = connected ? '' : signedOutText;
    login.textContent = connected ? '' : 'Accedi';
    description.hidden = connected;
    login.hidden = connected;
    logout.hidden = !connected;
  };
  request(config.account || '/auth/me').then(response => response.json()).then(account => {
    syncAccount(account.authenticated === true,account.username);
  }).catch(() => { /* The document load reports cloud errors separately. */ });
  login.addEventListener('click',event => {
    if (window.feedbackHasUnsaved?.() && !confirm('Le modifiche non sono salvate. Esporta il JSON prima di accedere di nuovo. Continuare?')) event.preventDefault();
  });
  window.feedbackRemote = {
    load,save,hasUpdates,snapshot,
    account: syncAccount,
    failed: error => {
      if (error.status === 401) {
        syncAccount(false);
      }
    }
  };
  // Keep the account chip optically centered with the title row at every density.
  const chipStyle = document.createElement('style');
  chipStyle.textContent = `
    .cloud-account {
      align-items: center;
      align-self: center;
      transform: none;
      gap: 8px;
      padding: 6px 12px;
      border-radius: 999px;
    }
    .cloud-account-logo {
      display: block;
      align-self: center;
      margin: 0;
      vertical-align: middle;
      width: 20px;
      height: 20px;
      flex-basis: 20px;
    }
    .cloud-account-user,
    .cloud-account p {
      line-height: 1.2;
    }
    .cloud-account-user {
      transform: none;
    }
    .cloud-account-action {
      display: inline-flex;
      align-self: center;
      align-items: center;
      justify-content: center;
      flex: 0 0 auto;
      min-width: 0;
      min-height: 0;
      padding: 4px 9px;
      border: 0;
      border-radius: 999px;
      background: var(--accent);
      color: var(--accent-on);
      font-size: 12px;
      font-weight: 700;
      line-height: 1.2;
    }
    .cloud-account-action:hover {
      background: var(--accent-focus);
      color: var(--accent-on);
    }
    @media (max-width: 640px) {
      .cloud-account {
        padding: 4px 8px;
        gap: 8px;
      }
      .cloud-account-logo {
        width: 18px;
        height: 18px;
        flex-basis: 18px;
      }
      .cloud-account-user {
        font-size: 13px;
      }
      .cloud-account-action {
        padding: 3px 8px;
      }
    }
  `;
  document.head.append(chipStyle);
})();
