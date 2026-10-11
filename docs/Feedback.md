# Feedback AIV

Versione **5.30**: AIV Play è più semplice, con l'elenco che hai scritto nel giro 5.20: meno comandi e meno impostazioni, coi valori che hai scelto. AIV GitHub non cambia.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.30 è pubblicata: [v5.30](https://github.com/Roccobot/AIV/releases/tag/v5.30), con i due APK
[AIV-5.30.apk](https://github.com/Roccobot/AIV/releases/download/v5.30/AIV-5.30.apk) (AIV GitHub) e [AIV-Play-5.30.apk](https://github.com/Roccobot/AIV/releases/download/v5.30/AIV-Play-5.30.apk) (AIV Play).
Commit prodotto su `main`: `a561e1e`, release dal commit `a561e1e` (SlimVer 5.30 / versionCode 365; APK GitHub 11.033.165 byte, digest `e85de6f8`; APK Play 10.928.457 byte, digest `347bf3da`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.30**: due prove e un'etichetta. Giro **5.20**: `Elimina` in AIV Play confermato.

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

Le verifiche automatiche della 5.30 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (26), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.30-01 | Non provato | | Attendere il collaudo. |
| 5.30-02 | Non provato | | Attendere il collaudo. |

## 1. AIV Play: comandi e griglie

Installa `AIV-Play-5.30.apk` sopra la 5.20. Nelle impostazioni apri `Pulsanti e indicatori`: `Elemento interattivo principale` ha solo `Tasto fluttuante` e `Pillola di icone`, e la spiegazione sotto il titolo non nomina più i due menu; `Indicatore dell'ultimo media visualizzato` non c'è. Apri una cartella, apri un'immagine, sfoglia fino a un'altra e torna indietro: la griglia torna su quell'immagine, ma nessuna miniatura ha il segno. Tieni premuta una miniatura: nella scheda della selezione non c'è più `Lista`, e in `Ordine dei pulsanti` non c'è nemmeno nel riquadro della selezione. Rifai la prova sul tablet.

Letture mie: se nella 5.20 avevi scelto `Menu basso` o il menu Start, AIV Play riparte con la pillola; se avevi il `Tasto fluttuante`, lo ritrovi.

## 2. AIV Play: le impostazioni fissate

Sempre in AIV Play:
- in `Tema e dettagli grafici` non c'è `Effetto dietro menu e pannelli`, e dietro ogni menu e pannello c'è l'ombra;
- in `Cartelle`, `Intestazione delle cartelle` ha solo `Info` e `Seleziona tutti`, tutte e due accese di fabbrica; il gradiente dell'intestazione c'è sempre e il titolo non è mai graziato;
- `Colore delle cartelle` non c'è né in `Cartelle` né nelle opzioni che apri tenendo premuto il FAB della schermata iniziale: una cartella con un colore suo lo mostra sul nome;
- in `Visualizzatore` non c'è `Sfoglia le immagini verso destra dalla meno recente`, e le immagini si sfogliano sempre dalla più recente;
- in `Rinomina e download` non c'è `Consenti la modifica dell'estensione`, e la finestra di `Scarica` non ha il tasto `Estensione` nemmeno col tocco lungo.

Letture mie: la tua riga *modifica dell'estensione sia per download che per rinomina* l'ho letta come le altre dell'elenco, cioè via la voce e il tasto; se volevi altro, dimmelo. In AIV Play la rinomina non c'è ancora, quindi la riga riguarda oggi il solo `Scarica`. I valori che avevi scelto nella 5.20 restano scritti nell'app: AIV Play li ignora, e torna a leggerli il giorno che una voce rientra.

## Etichette testuali

### e-settings_main_control_desc_play · Spiegazione di 'Elemento interattivo principale' in AIV Play
Scegli come vuoi interagire con l'app quando tieni lo smartphone in verticale: puoi scegliere tra pulsante fluttuante ('FAB') con menu e pillola con icone.
<!-- Nuova nella 5.30: è la frase di AIV GitHub senza i due menu. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV Play: comandi e griglie | 5.30-01 | Non provato | | Attendere il collaudo. |
| AIV Play: impostazioni fissate | 5.30-02 | Non provato | | Attendere il collaudo. |
| AIV Play: `Elimina` col cestino di sistema | 5.20-01 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: AIV Play più semplice (`5.30-01`, `5.30-02`) e un'etichetta.
- **AIV Play, 5.40**: la pagina `Cestino` che legge il cestino di Android, con `Ripristina` e `Svuota`.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio.
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
