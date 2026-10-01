// Run only after the owner explicitly asks the agent to read a submitted feedback round.
import {generateKeyPairSync,randomUUID} from 'node:crypto';
import {spawn} from 'node:child_process';
import {mkdtemp,mkdir,readFile,writeFile,rm,stat} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {resolve,join,dirname} from 'node:path';
import {open} from '../cloud/feedback/feedback-transfer.mjs';

const args=process.argv.slice(2);
function option(name,fallback='') {
  const index=args.indexOf(name);
  if (index<0) return fallback;
  if (!args[index+1] || args[index+1].startsWith('--')) throw Error('Opzione incompleta: '+name);
  return args[index+1];
}
async function gh(arguments_,input) {
  return new Promise((resolve_,reject)=>{
    const child=spawn('gh',arguments_,{stdio:['pipe','pipe','pipe']});
    let output='';
    child.stdout.on('data',data=>{output+=data;});
    // Never forward raw HTTP errors or signed download URLs to the conversation.
    child.stderr.on('data',()=>{});
    child.on('error',()=>reject(Error('GitHub CLI non disponibile.')));
    child.on('close',code=>code===0 ? resolve_(output) : reject(Error('Operazione GitHub non riuscita: '+arguments_[0])));
    child.stdin.end(input || '');
  });
}
const pause=ms=>new Promise(resolve_=>setTimeout(resolve_,ms));
let temporary;
try {
  const verifyOnly=args.includes('--verify-only');
  const output=resolve(option('--output',join(tmpdir(),'aiv-feedback-'+randomUUID(),'feedback.json')));
  const version=option('--version');
  if (version && !/^\d+\.\d{2}$/.test(version)) throw Error('Versione non valida.');
  const repo='Roccobot/AIV',requestId=randomUUID();
  // The private key exists only in this process, never in a workflow or repository.
  const {publicKey,privateKey}=generateKeyPairSync('rsa',{modulusLength:3072});
  const input={request_id:requestId,public_key:Buffer.from(publicKey.export({format:'pem',type:'spki'})).toString('base64'),version,verify_only:verifyOnly,requested_at:new Date().toISOString()};
  await gh(['workflow','run','feedback-read.yml','--repo',repo,'--ref','main','--json'],JSON.stringify(input));
  const deadline=Date.now()+10*60*1000;
  let run;
  while (Date.now()<deadline) {
    const runs=JSON.parse(await gh(['run','list','--repo',repo,'--workflow','feedback-read.yml','--event','workflow_dispatch','--limit','30','--json','databaseId,displayTitle,status,conclusion']));
    run=runs.find(item=>item.displayTitle==='Feedback richiesto '+requestId);
    if (run?.status==='completed') break;
    await pause(3000);
  }
  if (!run || run.status!=='completed' || run.conclusion!=='success') throw Error('Recupero non completato. Verifica il workflow Leggi feedback inviato e la presenza di un giro inviato.');
  temporary=await mkdtemp(join(tmpdir(),'aiv-feedback-transfer-'));
  await gh(['run','download',String(run.databaseId),'--repo',repo,'--name','feedback-envelope','--dir',temporary]);
  const envelopePath=join(temporary,'feedback-envelope.json');
  if ((await stat(envelopePath)).size>60*1024*1024) throw Error('Trasferimento troppo grande.');
  const payload=open(JSON.parse(await readFile(envelopePath,'utf8')),privateKey,requestId);
  if (payload.schema!==1 || payload.project!=='AIV' || !payload.completed) throw Error('Giro recuperato non valido.');
  if (verifyOnly && payload.notes!=='Verifica trasferimento') throw Error('Prova del trasferimento non valida.');
  await mkdir(dirname(output),{recursive:true,mode:0o700});
  await writeFile(output,JSON.stringify(payload,null,2),{mode:0o600,flag:'wx'});
  // Remove the encrypted artifact after downloading; a failure does not expose plaintext.
  try {
    const artifacts=JSON.parse(await gh(['api',`repos/${repo}/actions/runs/${run.databaseId}/artifacts`]));
    for (const artifact of artifacts.artifacts || [])
      if (artifact.name==='feedback-envelope') await gh(['api','--method','DELETE',`repos/${repo}/actions/artifacts/${artifact.id}`]);
  } catch { console.log('Artefatto cifrato conservato fino alla scadenza di un giorno.'); }
  console.log(verifyOnly ? 'Prova del trasferimento verificata, nessun feedback personale letto.' : 'Ultimo giro inviato recuperato, senza modificare la bozza.');
  console.log('File privato: '+output);
} catch (error) {
  console.error(error.message);
  process.exitCode=1;
} finally { if (temporary) await rm(temporary,{recursive:true,force:true}); }
