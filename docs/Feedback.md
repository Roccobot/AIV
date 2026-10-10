# Feedback AIV

Versione **5.10**: AIV esiste in due varianti. **AIV GitHub** è quella di sempre; **AIV Play** è la galleria pensata per Google Play, che chiede solo l'accesso a immagini e video, non ha le funzioni che scrivono sui file e col `Salva` degli editor scrive un file nuovo in Download.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.10 è pubblicata: [v5.10](https://github.com/Roccobot/AIV/releases/tag/v5.10), con i due APK
[AIV-5.10.apk](https://github.com/Roccobot/AIV/releases/download/v5.10/AIV-5.10.apk) (AIV GitHub) e [AIV-Play-5.10.apk](https://github.com/Roccobot/AIV/releases/download/v5.10/AIV-Play-5.10.apk) (AIV Play).
Commit prodotto su `main`: `05e2ff1`, release dal commit `05e2ff1` (SlimVer 5.10 / versionCode 361; APK GitHub 11.027.145 byte, digest `3b89c8b3`; APK Play 10.963.677 byte, digest `12ed688e`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.10**: cinque prove e cinque etichette. Giro **5.03**: il menu Start riordinato è confermato.

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

Le verifiche automatiche della 5.10 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (12), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.10-01 | Non provato | | Attendere il collaudo. |
| 5.10-02 | Non provato | | Attendere il collaudo. |
| 5.10-03 | Non provato | | Attendere il collaudo. |
| 5.10-04 | Non provato | | Attendere il collaudo. |
| 5.10-05 | Non provato | | Attendere il collaudo. |

## 1. AIV GitHub resta com'era

Installa `AIV-5.10.apk` come al solito, sopra la versione che hai. Prova le funzioni sui file che in AIV Play non ci sono: tieni premuta una foto e usa `Copia` o `Rinomina`, apri il `Cestino` dal menu, passa alla vista `Cartelle di sistema`. Deve funzionare tutto come nella 5.03.

## 2. AIV Play: l'installazione e il permesso

Installa anche `AIV-Play-5.10.apk`: è un'app a sé e si installa accanto all'altra, quindi sul telefono vedi due icone uguali con lo stesso nome. Aprila: la schermata iniziale dice `Per elencare le cartelle serve l'accesso a immagini e video.`. Tocca `Concedi l'accesso`: Android chiede l'accesso a foto e video con la sua finestra, senza mandarti in una pagina delle impostazioni. Rispondi `Non consentire` due volte: dopo il secondo rifiuto Android non mostra più la sua finestra, e AIV Play apre la propria pagina nelle impostazioni di Android, dove concedi `Foto e video`; tornando nell'app compaiono le cartelle.

Letture mie: il nome dell'app è lo stesso nelle due varianti, ed è il punto da decidere prima di pubblicare su Google Play.

## 3. AIV Play: l'accesso solo ad alcune foto

Togli il permesso ad AIV Play dalle impostazioni di Android (`App` → AIV → `Autorizzazioni` → `Foto e video`) e torna nell'app: torna la frase del permesso. Tocca `Concedi l'accesso` e nella finestra di Android scegli di selezionare solo alcune foto. La schermata iniziale mostra le cartelle di quelle sole foto, e sotto il titolo compare la riga `Vedi solo le immagini che hai scelto.` con `Consenti tutte`. Tocca `Consenti tutte` e concedi tutto: la riga sparisce. Il permesso si rilegge a ogni ritorno nell'app, quindi anche cambiandolo dalle impostazioni di Android la riga compare o sparisce da sé.

## 4. AIV Play: le funzioni che non ci sono

In AIV Play controlla che manchino, senza buchi nei menu:
- tenendo premuta una foto nel visualizzatore, il riquadro ha solo `Condividi` e `Info`;
- nella selezione della griglia ci sono `Condividi`, `Info`, `Lista`, `Tutti`, `Nessuno` e `Inverti`;
- nei menu della schermata iniziale e di una cartella non c'è `Cestino`, e fra le viste non c'è `Cartelle di sistema`;
- tenendo premuto il titolo di una cartella non si apre la rinomina (il tocco breve copia ancora il nome), e lo stesso vale per il nome nel pannello `Info`;
- `Modifica` apre sempre l'editor dell'app, e tenendola premuta non compare la scelta dell'app;
- in `Esporta/Converti` di un SVG non c'è `Pulisci`;
- nelle impostazioni non ci sono la pagina `Cestino`, la scelta dell'editor e la `Copia di sicurezza prima di sovrascrivere`, e in `Ordine dei pulsanti` i due riquadri dei file hanno solo i tasti che restano.

Letture mie: le funzioni sui file torneranno una per rilascio, rifatte con MediaStore, cioè con la finestra di Android che chiede il consenso a ogni modifica.

## 5. AIV Play: il `Salva` degli editor scrive in Download

In AIV Play apri una foto, tocca `Modifica`, girala di un quarto e tocca `Salva`: l'avviso dice `Salvata in Download`. Nella cartella `Download` c'è un file nuovo con lo stesso nome, girato, e la foto di partenza è com'era. Rifai la prova con l'editor completo (una correzione qualsiasi): stesso avviso, stesso risultato. Nell'editor completo il velo d'aiuto del `Salva`, se lo vedi, dice che la modifica diventa un nuovo file in Download.

Letture mie: in AIV Play il tocco lungo su `Salva` fa la stessa cosa del tocco breve, perché un file nuovo esce comunque; su Android 9 il `Salva` di AIV Play non può scrivere in Download e risponde che non è riuscito; AIV GitHub salva come prima.

## Etichette testuali

### e-folders_permission_media · Schermata iniziale di AIV Play senza permesso
Per elencare le cartelle serve l'accesso a immagini e video.
<!-- Nuova nella 5.10. -->

### e-folders_partial · Riga sotto il titolo, con l'accesso solo ad alcune foto
Vedi solo le immagini che hai scelto.
<!-- Nuova nella 5.10. -->

### e-folders_partial_all · Tasto accanto alla riga
Consenti tutte
<!-- Nuova nella 5.10. -->

### e-editor_done_downloads · Avviso dopo il Salva in AIV Play
Salvata in Download
<!-- Nuova nella 5.10. -->

### e-hint_save_play · Velo d'aiuto del Salva nell'editor completo di AIV Play
Quando hai finito, tocca il tasto 'Salva': la modifica diventa un nuovo file in Download, e l'immagine di partenza resta com'è.
<!-- Nuova nella 5.10. In AIV GitHub il velo resta quello di prima. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV GitHub invariata | 5.10-01 | Non provato | | Attendere il collaudo. |
| AIV Play: permesso | 5.10-02 | Non provato | | Attendere il collaudo. |
| AIV Play: accesso parziale | 5.10-03 | Non provato | | Attendere il collaudo. |
| AIV Play: funzioni assenti | 5.10-04 | Non provato | | Attendere il collaudo. |
| AIV Play: `Salva` in Download | 5.10-05 | Non provato | | Attendere il collaudo. |
| Il menu Start | 5.03-01 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: le due varianti (`5.10-01`-`5.10-05`) e cinque etichette.
- **AIV Play, pezzo 2**: le funzioni sui file rifatte con MediaStore, una per rilascio, cominciando da `Elimina`.
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi; prima va scelto il nome dell'app.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
