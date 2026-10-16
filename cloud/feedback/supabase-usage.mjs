// How much the feedback service holds, measured at every deploy since 6.29 (H7 of the 6.25
// review). The free plan of Supabase turns the project read only past 500 MB of database, and from
// there no save succeeds; files have a gigabyte. Above half of either the deploy warns: that is the
// owner's threshold for pruning (choice J1, 2026-10-16). It reads sizes and counts only, never the
// content, and it never stops a deploy.
import {appendFileSync, readFileSync} from 'node:fs';
import {pathToFileURL} from 'node:url';
import {SupabaseStore} from './supabase-store.mjs';
export const DATABASE_LIMIT = 500*1024*1024, FILES_LIMIT = 1024*1024*1024;
const megabytes = bytes => (bytes/1024/1024).toFixed(1).replace('.',',')+' MB';
export async function usage(store) {
  let database = null;
  try {
    const response = await store.request('/rest/v1/rpc/aiv_feedback_usage',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'});
    database = await response.json();
  } catch (error) {
    // PostgREST answers 404 for a function the database does not have yet.
    if (error.status !== 404) throw error;
  }
  let files = 0, bytes = 0;
  for await (const item of store.attachments()) { files++; bytes += item.size; }
  return {database,files,bytes};
}
export function report({database,files,bytes}) {
  const lines = [], warnings = [];
  if (database) {
    lines.push(`Database: ${megabytes(database.database_bytes)} su 500 MB, di cui ${megabytes(database.history_bytes)} di revisioni (${database.history_rows}).`);
    if (database.database_bytes > DATABASE_LIMIT/2) warnings.push(`Il database del DF è oltre metà del limite gratuito (${megabytes(database.database_bytes)} su 500 MB): è il momento della potatura delle revisioni.`);
  } else {
    lines.push('Database: misura non disponibile, rilancia `cloud/feedback/supabase-setup.sql` nell\'editor SQL di Supabase.');
  }
  lines.push(`Allegati: ${files}, ${megabytes(bytes)} su 1 GB.`);
  if (bytes > FILES_LIMIT/2) warnings.push(`Gli allegati del DF sono oltre metà del limite gratuito (${megabytes(bytes)} su 1 GB).`);
  return {lines,warnings};
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const url = process.env.FEEDBACK_SUPABASE_URL || readFileSync(new URL('./wrangler.toml',import.meta.url),'utf8').match(/^SUPABASE_URL = "([^"]+)"/m)?.[1];
    const owner = readFileSync(new URL('./wrangler.toml',import.meta.url),'utf8').match(/^OWNER_ID = "([^"]+)"/m)?.[1];
    const store = new SupabaseStore({SUPABASE_URL:url,SUPABASE_SECRET_KEY:process.env.FEEDBACK_SUPABASE_SECRET_KEY,OWNER_ID:owner});
    const {lines,warnings} = report(await usage(store));
    if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY,'\n'+lines.map(line => '- '+line).join('\n')+'\n');
    lines.forEach(line => console.log(line));
    warnings.forEach(warning => console.log('::warning title=Spazio del DF::'+warning));
  } catch (error) {
    const status = Number.isInteger(error.status) ? ` HTTP ${error.status}` : '';
    console.log('::warning title=Spazio del DF::Misura non riuscita.'+status);
  }
}
