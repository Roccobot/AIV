// Server-only adapter. The browser never receives Supabase credentials or endpoints.
const LIST_PAGE = 1000;
export class SupabaseStore {
  constructor(env) {
    this.url = new URL(env.SUPABASE_URL);
    this.secret = env.SUPABASE_SECRET_KEY;
    this.owner = String(env.OWNER_ID);
    const local = this.url.protocol === 'http:' && ['127.0.0.1','localhost'].includes(this.url.hostname)
      && this.secret === 'development-supabase-test-key';
    if (!local && !(this.url.protocol === 'https:' && this.url.hostname.endsWith('.supabase.co')))
      throw Error('Indirizzo Supabase non valido.');
    if (!this.secret || !/^[0-9]{1,20}$/.test(this.owner)) throw Error('Archivio non configurato.');
    this.prefix = `feedback/v1/${this.owner}/`;
  }
  async request(path, options = {}, allowed = []) {
    const headers = new Headers(options.headers);
    headers.set('apikey',this.secret);
    // New sb_secret keys belong only in apikey; legacy JWT keys also need Authorization.
    if (this.secret.startsWith('eyJ')) headers.set('Authorization','Bearer '+this.secret);
    const response = await fetch(new URL(path,this.url),{...options,headers,redirect:'manual',signal:options.signal || AbortSignal.timeout(15000)});
    if (response.ok) return response;
    let code = '';
    try {
      const error = await response.json();
      code = error.code || error.error || '';
      if (allowed.includes('missing') && (response.status === 404 || ['NoSuchKey','NotFound'].includes(code) || error.statusCode === '404')) return null;
      if (allowed.includes('duplicate') && (response.status === 409 || ['Duplicate','KeyAlreadyExists'].includes(code))) return null;
    } catch { /* Never expose an upstream error body or request headers. */ }
    if (allowed.includes('missing') && response.status === 404) return null;
    const error = Error(`Archivio Supabase non disponibile (HTTP ${response.status}).`);
    error.status = response.status;
    error.code = /^([A-Z][A-Za-z0-9_]{0,80}|[0-9]{5})$/.test(code) ? code : '';
    throw error;
  }
  path(key) {
    if (!key.startsWith(this.prefix)) throw Error('Percorso dell\'archivio non valido.');
    return key.split('/').map(encodeURIComponent).join('/');
  }
  async current() {
    const response = await this.request('/rest/v1/aiv_feedback_drafts?owner_id=eq.'+this.owner+'&select=draft,revision&limit=1');
    const rows = await response.json();
    if (!Array.isArray(rows) || rows.length > 1) throw Error('Documento cloud non valido.');
    const row = rows[0];
    if (!row?.draft) return null;
    if (!/^[a-f0-9-]{36}$/.test(row.revision)) throw Error('Versione cloud non valida.');
    return {httpEtag:'"'+row.revision+'"',etag:row.revision,body:JSON.stringify(row.draft)};
  }
  /*
   * Only the revision of the draft, since 6.29 (H2 of the 6.25 review): the page asks every 15
   * seconds whether the draft changed, and until 6.28 every ask brought the whole draft from the
   * database to the Worker, which threw it away.
   */
  async revision() {
    const response = await this.request('/rest/v1/aiv_feedback_drafts?owner_id=eq.'+this.owner+'&select=revision&limit=1');
    const rows = await response.json();
    if (!Array.isArray(rows) || rows.length > 1) throw Error('Documento cloud non valido.');
    const revision = rows[0]?.revision;
    if (!revision) return null;
    if (!/^[a-f0-9-]{36}$/.test(revision)) throw Error('Versione cloud non valida.');
    return {httpEtag:'"'+revision+'"',etag:revision};
  }
  async head(key) {
    if (key === this.prefix+'current.json') return this.revision();
    const response = await this.request('/storage/v1/object/info/aiv-feedback/'+this.path(key),{},['missing']);
    if (!response) return null;
    const info = await response.json();
    if (!Number.isInteger(info.size) || info.size < 1) throw Error('Allegato cloud incompleto.');
    return {size:info.size};
  }
  /*
   * The sizes of the attachments [keys] that are stored, since 6.29 (H1 of the 6.25 review): one
   * listing of the owner's files folder, a page of a thousand at a time, in place of one request per
   * attachment. Workers Free allows 50 outgoing requests per request, so a draft with 50
   * attachments could no longer be saved. The listing (`POST /storage/v1/object/list`, body read in
   * the storage sources) answers names relative to the prefix and the size in `metadata.size`;
   * whatever it does not confirm is asked one by one, as before, so a listing that answered in
   * another shape would cost the old requests and never a refused draft.
   */
  async sizes(keys) {
    const wanted = new Set(keys), found = new Map(), folder = this.prefix+'files';
    try {
      for (let offset = 0; ; offset += LIST_PAGE) {
        const response = await this.request('/storage/v1/object/list/aiv-feedback',{
          method:'POST',headers:{'Content-Type':'application/json'},
          body:JSON.stringify({prefix:folder,limit:LIST_PAGE,offset,sortBy:{column:'name',order:'asc'}})
        });
        const page = await response.json();
        if (!Array.isArray(page)) break;
        for (const item of page) {
          const key = folder+'/'+item?.name, size = item?.metadata?.size;
          if (wanted.has(key) && Number.isInteger(size) && size > 0) found.set(key,size);
        }
        if (page.length < LIST_PAGE || found.size === wanted.size) break;
      }
    } catch { /* The single checks below answer for whatever the listing did not confirm. */ }
    for (const key of wanted) {
      if (found.has(key)) continue;
      const stored = await this.head(key);
      if (stored) found.set(key,stored.size);
    }
    return found;
  }
  async get(key) {
    if (key === this.prefix+'current.json') return this.current();
    const response = await this.request('/storage/v1/object/authenticated/aiv-feedback/'+this.path(key),{},['missing']);
    return response ? {body:response.body} : null;
  }
  async put(key, body, options = {}) {
    if (key === this.prefix+'current.json') {
      const expected = options.onlyIf?.etagDoesNotMatch === '*' ? 'empty' : options.onlyIf?.etagMatches;
      if (!expected) throw Error('Versione obbligatoria.');
      const response = await this.request('/rest/v1/rpc/aiv_feedback_save',{
        method:'POST',headers:{'Content-Type':'application/json'},
        body:JSON.stringify({p_owner_id:this.owner,p_expected_revision:expected,p_draft:JSON.parse(body)})
      });
      const saved = await response.json();
      if (saved === null) return null;
      if (!/^[a-f0-9-]{36}$/.test(saved.revision)) throw Error('Conferma di salvataggio non valida.');
      return {etag:saved.revision,httpEtag:'"'+saved.revision+'"'};
    }
    await this.request('/storage/v1/object/aiv-feedback/'+this.path(key),{
      method:'POST',headers:{'Content-Type':'application/octet-stream','x-upsert':'false'},body
    },['duplicate']);
    return {size:body.byteLength};
  }
}
