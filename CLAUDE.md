# CLAUDE.md: regole del progetto AIV

> **Cos'è questo file.** Le regole **specifiche** di `Roccobot/AIV`, l'app Android
> 'Astonishing Image Viewer'. Tutto quello che vale per ogni progetto vive nelle regole
> universali, `rules/Roccobot.md` di `Roccobot/tools`, e qui non si duplica: qui sta solo
> ciò che di questo repository non si ricava altrove.

⚠️ **Nasce il 2026-09-01, dopo 111 versioni**, e la ragione è precisa: fino a quel giorno
gli **indirizzi dei due documenti vivi** del progetto (il collaudo e il changelog) stavano
solo nel brief di consegna, che è **stato volatile** e non un archivio. Un artefatto vive
fuori dal repository per definizione, quindi se il suo indirizzo non è scritto in un file
committato, alla sessione dopo quel documento è perso.

## 🔗 I due documenti vivi del progetto

| documento | a che cosa serve | indirizzo |
|---|---|---|
| **Collaudo AIV** | le voci da provare della versione appena uscita, con i tre esiti e i commenti dell'utente. **Chiede.** | <https://claude.ai/code/artifact/a026a5d9-3bd0-4732-a8ea-69033d04fb48> |
| **Changelog AIV** | che cosa è cambiato a ogni versione pubblicata, in breve. **Racconta.** | <https://claude.ai/code/artifact/2dc50535-30a7-4a5c-88f5-0319c6107a5a> |

⚠️ **Tengono lo STESSO indirizzo a ogni ripubblicazione**: l'utente li ha fra i preferiti, e
un collegamento nuovo a ogni giro vuol dire un documento da ritrovare ogni volta.
⚠️ **Non si fondono**, per quanto si somiglino: la ragione sta in `rules/Roccobot.md`,
§ '🧾 Il changelog, il terzo pezzo di ogni rilascio'.

## 🚀 Che cosa produce un rilascio

**Tre cose, e vanno insieme**: il numero di **versione**, le voci nuove nel **collaudo**, e le
righe nuove nel **changelog**. La regola completa (che cosa entra in ognuno, come si
ripubblica un giro in cinque passi, la struttura standard del documento di collaudo) vive in
`rules/Roccobot.md`, § '🔁 Il giro del collaudo: rilascio, documento, riscontro'.

- **Versione in SlimVer** (`x.xx`), come gli altri progetti. La fonte unica è
  `versionName` in `app/build.gradle.kts`, e il workflow di rilascio ne ricava il tag: il tag
  **conferma** quel numero invece di essere un secondo posto in cui scriverlo.
- ⚠️ **Il `versionCode` è un'altra cosa e cresce da sé**: Android rifiuta un aggiornamento
  che non lo faccia salire, non è legato al `versionName`, e **nessuno controlla che sia stato
  toccato**. La `0.11` è uscita portando `1`, quindi da lì in poi ogni versione pubblicata ha
  il suo numero.
- **Come si pubblica**: il workflow `release.yml` ha due vie, e la seconda esiste apposta per
  una sessione. Un `workflow_dispatch` con l'ingresso `publish` acceso taglia il tag dal
  `versionName`, costruisce l'APK firmato, crea la release, e **copia APK e paginetta sotto
  `roccobot.github.io/AIV/`**. Senza `publish` costruisce e si ferma, che è il banco di prova.
- **Verifica di pubblicazione avvenuta**: un `curl` su <https://roccobot.github.io/AIV/> e il
  nome del file servito (`AIV-1.20.apk` e simili). Il merge su `master` del sito non basta:
  serve che il deploy Pages vada a buon fine.

## 🔐 La firma, e dove NON vive

La chiave di firma e le sue parole d'ordine stanno **solo** fra i secret GitHub di questo
repository (`AIV_KEYSTORE_FILE`, `AIV_KEYSTORE_PASSWORD`, `AIV_KEY_ALIAS`, `AIV_KEY_PASSWORD`),
e il job le scrive su disco per la durata di una sola esecuzione.

- ⚠️ **Senza quelle variabili il build di release NON fallisce: esce NON firmato.** È voluto,
  perché così chi non ha la chiave può comunque compilare e controllare il minificatore. Il
  rovescio è che un APK non firmato si riconosce solo guardando, e per questo il workflow ha
  un passo che **chiede all'APK se è firmato** invece di crederlo.

## 🧰 Gli strumenti che questo repo si porta dietro

- **`tools/i18n-check.py`**, da lanciare dalla radice: confronta tutte le lingue con
  l'inglese (chiavi mancanti, segnaposto, categorie di plurale, caratteri vietati). ⚠️ Deve
  dire **28 lingue, 0 problemi**: un numero più basso vuol dire che una cartella non è stata
  vista. Le **varianti regionali** (`values-b+es+419` e `values-pt-rPT`) portano solo le
  differenze, e il verificatore lo sa.
- ⚠️ **`refcheck.py` NON vive qui** ma in `roccobot.github.io/.memo/scripts/`: in una sessione
  che non monta quel repo i controlli sui caratteri e sui rimandi non girano, e prima di un
  commit va detto invece di darli per fatti.

## 🌿 Branch

Il branch principale è **`main`**. Le sessioni vincolate a un branch `claude/*` aprono la PR e
la mergiano subito (squash), come da regola universale sul go-live.
