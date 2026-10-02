# Feedback AIV

Versione **3.39** (Release pubblicata). Prodotto **3.40** su `main` (298, niente Release ancora): rail, editor, HEIC, impostazioni, inpaint.
Collaudo 3.39 consegnato e travasato il 2 ottobre 2026 (~23:25 Europe/Rome).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
La Release 3.39 resta pubblicata: [v3.39](https://github.com/Roccobot/AIV/releases/tag/v3.39), con l'APK
[AIV-3.39.apk](https://github.com/Roccobot/AIV/releases/download/v3.39/AIV-3.39.apk).
Commit prodotto su `main`: `b3600a5` (SlimVer 3.39 / versionCode 297).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.39** è stato consegnato e travasato. Le prove OK del 3.39 (e le riprove 3.13)
sono in archivio sotto; restano aperte le Non approvato e i residui del collaudo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte. Questa è **manutenzione** del
documento post-collaudo, non una nuova versione di collaudo 3.40 (niente prove `3.40-xx`
finché non c'è Release 3.40).

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ per il pannello); tieni premuto Salva (dischetto)
per **Consegna e copie**. Su desktop: Altro in colonna laterale, con pulsante Consegna a
destra dell'allegato. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.39 sono superate: 486 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet del 3.39 è stato consegnato.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.39-03 | Non approvato | Cerca sotto orologio; maniglia troppo visibile; posizione lista resettata al tap; Cestino/Impostazioni non centrati. | Ritocchi rail in 3.40. |
| 3.39-01 | Tutto OK + residuo | Flicker risolto. Invertire lati: comandi a destra, canvas a sinistra (default); opposto col FAB a sinistra. | Layout editor in 3.40. |
| 3.27-03 | Accettabile | Crea ancora più sotto-pagine per tenere tutto più in ordine. | Entra in 3.40 (era differito). |
| 3.38-11 | Non approvato | Nessun miglioramento visto (continuità con 3.24-04 / 3.30-01 / 3.37-09). | Ultimo tentativo serio, poi documentare i limiti. |

## 1. Rail tablet: ritocchi collaudo

Su tablet (≥ 600), con AIV 3.39 installata: la colonna laterale segue già il mockup di base
(niente Cartelle in alto; Cerca; Cestino e Impostazioni in basso con etichetta; elenco in
basso con maniglia). Residui del collaudo 3.39 da correggere:

- **Cerca** non deve finire sotto l'orologio di sistema, né in orizzontale né in verticale
  (rispettare inset / safe area).
- La **maniglia** per spostare la lista cartelle deve essere più trasparente (meno evidente).
- Lo spostamento verticale della lista funziona, ma la posizione **non** deve resettarsi quando
  si tocca una cartella: resta dove l'hai messa.
- I due tasti **Cestino** e **Impostazioni** devono risultare **centrati** (oggi un po' troppo
  a destra).

Allegato collaudo: `tab.png` (privato); copia brief `.memo/files/aiv-339-rail-tab.png`.

## 2. Editor tablet: lati invertiti

Su tablet in **orizzontale** (larghezza ≥ 1024 dp): lo sfarfallio del collaudo 3.38 è
**risolto** (3.39-01 Tutto OK). Resta un residuo di layout: di default i comandi/pulsanti
stanno a **destra** e l'anteprima/canvas a **sinistra**; con il FAB a sinistra i lati si
invertono (comandi a sinistra, canvas a destra). Ripeti con editor completo e semplice.

## 3. Più sotto-pagine Impostazioni

Apri Impostazioni: devono esserci **ancora più sotto-pagine** rispetto a oggi, così le voci
restano ordinate e non ammassate. Residuo `3.27-03` Accettabile (differito da 3.27; ora in
piano 3.40 per ordine di Rocco).

## 4. Qualità Correggi/Rimuovi (ultimo tentativo)

Su un orlo o una linea (vedi screenshot storici su `3.30-01`): valuta se resta più continuo.
Allega prima/dopo se ancora debole. Residuo collaudo 3.38/3.39: nessun miglioramento visto.
**Ultimo tentativo serio** in 3.40; oltre serve riconoscimento di pattern / AI. Se resta corto,
documentare i limiti euristici nel brief e chiudere il filone.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse (impostazioni =
Applica la proposta; tablet = Approvo la direzione; PNG = Le modifiche si vedono) e **non**
sono riproposte.

## Riscontri conclusi

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Prove 3.37 OK senza residuo | 3.37 | OK | 3.37-01..07 (con residui assorbiti). | Archiviata. |
| UX documento (note A 3.30 + chat) | 3.38 | In codice | Striscia, Altro, overlay, shell: non è prova di collaudo app. | Manutenzione DF. |
| Griglia tablet più aria | 3.38-05 | OK | Tutto OK. | Archiviata. |
| Cestino a tutta larghezza | 3.38-06 | OK | Tutto OK; chiude 3.35-01. | Archiviata. |
| Apici → grassetto Impostazioni | 3.38-07 | OK | Tutto OK; chiude residuo 3.26-03 / 3.37-03. | Archiviata. |
| Indicatore già inclusa | 3.38-08 | OK | Tutto OK. | Archiviata. |
| Lentino soglia più bassa | 3.38-09 | OK | Tutto OK; chiude 3.37-08. | Archiviata. |
| Editor tablet flicker | 3.39-01 | OK | Problema risolto (resta invertire i lati, prova aperta sopra). | Flicker chiuso. |
| Menu FAB ~35% | 3.39-02 | OK | Tutto OK. | Archiviata. |
| Empty state mockup | 3.39-04 | OK | Tutto OK. | Archiviata. |
| Intestazione Modalità incluse | 3.13-02 | OK | Riprova OK in 3.39. | Archiviata. |
| Nascosta elenco/dicitura/avvisi | 3.13-08 | OK | Riprova OK in 3.39. | Archiviata. |

## Prossimi passi

- **3.40 prodotto su main** (`862b2c6` + icona `dbc0f50`): rail, lati editor, HEIC, sotto-pagine Impostazioni, ultimo inpaint (limiti nel brief). **Niente Release/APK** finché non la chiede Rocco; niente prove `3.40-xx` prima della Release.
- **Dopo Release 3.40**: nuove prove DF `3.40-xx` + archiviare residui 3.39 OK.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
