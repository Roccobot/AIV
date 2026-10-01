// Pipe this output straight into Wrangler, never to logs or tracked files.
import {readFileSync} from 'node:fs';
import {randomBytes} from 'node:crypto';
const configured = JSON.parse(readFileSync(process.argv[2],'utf8'));
if (!process.env.FEEDBACK_GITHUB_CLIENT_ID || !process.env.FEEDBACK_GITHUB_CLIENT_SECRET) throw Error('Configura le credenziali OAuth GitHub nei campi riservati del repository.');
const secrets = {GITHUB_CLIENT_ID:process.env.FEEDBACK_GITHUB_CLIENT_ID,GITHUB_CLIENT_SECRET:process.env.FEEDBACK_GITHUB_CLIENT_SECRET};
if (!configured.some(secret=>secret.name === 'SESSION_SECRET')) secrets.SESSION_SECRET = randomBytes(32).toString('hex');
process.stdout.write(JSON.stringify(secrets));
