# Feedback AIV

Versione **5.70**: AIV Play è pronta per Google Play (pacchetto, informativa, testi della scheda), e AIV GitHub fa il suo primo aggiornamento vero. Il documento è cumulativo con la 5.61.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.70 è pubblicata: [v5.70](https://github.com/Roccobot/AIV/releases/tag/v5.70), con i due APK
[AIV-5.70.apk](https://github.com/Roccobot/AIV/releases/download/v5.70/AIV-5.70.apk) (AIV GitHub) e [AIV-Play-5.70.apk](https://github.com/Roccobot/AIV/releases/download/v5.70/AIV-Play-5.70.apk) (AIV Play), più [AIV-Play-5.70.aab](https://github.com/Roccobot/AIV/releases/download/v5.70/AIV-Play-5.70.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `1af9f81`, release dal commit `1af9f81` (SlimVer 5.70 / versionCode 371; APK GitHub 11.073.921 byte, digest `70089324`; APK Play 10.948.349 byte, digest `cd3f4028`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.70**: una prova, tre domande e dieci etichette, cumulativo con la 5.61; nelle etichette i tre punti stanno per il carattere unico dei puntini, che nell'app è uno solo. Giro **5.60**: le finestre in cui si scrive e l'aggiornamento automatico confermati, con quattro etichette corrette.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale, coi conteggi in cima. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 5.70 sono superate: banco di prova completo di AIV GitHub (714 prove), prove di AIV Play (43), controllo delle traduzioni, compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.70-01 | Non provato | | Attendere il collaudo. |

## 1. AIV GitHub: l'aggiornamento da una release alla successiva

Non scaricare la 5.70 a mano: parti dalla 5.60 di AIV GitHub che hai già. In `Impostazioni`, sezione `Avanzate`, tocca `Controlla ora`: compare la finestra `AIV 5.70 è disponibile`. Tocca `Aggiorna`. La prima volta Android apre la sua pagina per consentire ad AIV l'installazione di app: attiva l'interruttore e torna indietro. AIV scarica l'aggiornamento e apre l'installazione di Android: conferma. Riapri AIV: in `Avanzate` la voce si chiama `Aggiornamento automatico` e il tasto `Controlla adesso`, e toccandolo compare `AIV è aggiornata`. Le impostazioni, le copertine e gli stili sono quelli di prima.

Letture mie: la finestra e i testi che vedi prima dell'installazione sono ancora quelli della 5.60, che è l'app che li mostra; le tue correzioni compaiono dalla 5.61 in poi. La 5.61 non la vedi mai: c'è, ma la 5.60 propone l'ultima. Il controllo all'apertura si fa una volta per giorno di calendario, e oggi lo ha già fatto la 5.60: per questo la prova parte da `Controlla ora`. Se tocchi `Più tardi`, la proposta torna all'apertura del giorno dopo. Dalla 5.61 i puntini di tutti i testi dell'app sono il carattere unico, come vuole Android: oltre alle etichette qui sotto, anche `Correzione in corso...` degli effetti.

## Domande

### d-store-testi · I testi per Google Play e l'informativa
Leggi i testi della scheda e le risposte ai moduli in [docs/Store.md](https://github.com/Roccobot/AIV/blob/main/docs/Store.md), e l'[informativa sulla privacy](https://roccobot.github.io/AIV/privacy.html). Ogni frase dell'informativa e dei moduli corrisponde a quello che fa il codice; le descrizioni della scheda elencano solo funzioni che AIV Play ha davvero.

**E1**: Approvo così: li uso nella Console come sono.

**E2**: Li correggo: le correzioni sono nel commento.

Parere: **E1** se ti tornano, altrimenti **E2** con le frasi da cambiare.

### d-store-contatto · Il contatto dell'informativa
Google vuole un modo per contattare lo sviluppatore. Oggi l'informativa rimanda alle segnalazioni del repository su GitHub.

**F1**: Le segnalazioni su GitHub, come adesso: niente indirizzi personali in pagina.

**F2**: Un indirizzo email: scrivilo nel commento, e lo metto nell'informativa.

Parere: **F1**, perché non espone un tuo indirizzo; la Console mostra comunque l'email che darai all'account.

### d-store-grafica · La grafica che Google chiede
Google vuole l'icona a 512 x 512, un'immagine in evidenza di 1024 x 500 e almeno due schermate del telefono.

**G1**: Icona e immagine in evidenza le preparo io dai disegni dell'app; le schermate le fai tu dal telefono, e me le mandi.

**G2**: Prepari tutto tu.

Parere: **G1**: l'icona e l'immagine nascono dai disegni che il repository ha già, le schermate devono mostrare l'app vera.

## Etichette testuali

### e-settings_updates · Titolo della voce in `Avanzate`
Aggiornamento automatico

### e-settings_updates_check · Tasto della voce
Controlla adesso

### e-update_title · Titolo della finestra, col numero della versione nuova
È disponibile AIV 5.71

### e-update_untrusted · Avviso se la firma o il digest non tornano
L'aggiornamento non è firmato con la chiave di AIV, e non può essere installato.

### e-update_downloading · Avviso durante lo scaricamento
Scarico l'aggiornamento...

### e-backup_exporting · Avviso durante l'esportazione del file di impostazioni
Esportazione...

### e-backup_importing · Avviso durante l'importazione del file di impostazioni
Importazione...

### e-dest_browse · Voce della finestra che sceglie la cartella di destinazione
Sfoglia tutte le cartelle...

### e-pick_counting · Avviso mentre il selettore conta i file
Sto contando...

### e-convert_clean_keeps · Spiegazione di `Pulisci` per un SVG
Non modifica il contenuto dell'immagine (tracciati, forme, linee, disegni, testi...): l'illustrazione rimane intatta al 100%.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV GitHub: l'aggiornamento automatico | 5.60-01 | Tutto OK | Quattro etichette corrette. | Corrette nella 5.61. |
| Le finestre in cui si scrive, con la tastiera aperta | 5.51-01 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: l'aggiornamento da una release alla successiva (`5.70-01`), e le tre domande sullo Store.
- **Tuoi, in parallelo** (scelta A1): l'account sviluppatore su Google Play, la creazione dell'app e i 12 tester per i 14 giorni del test chiuso; i passi in ordine sono in fondo a `docs/Store.md`.
- **Residuo per lo Store**: la grafica della scheda, secondo la risposta a `d-store-grafica`.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
