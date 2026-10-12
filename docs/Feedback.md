# Feedback AIV

Versione **5.90**: la pagina delle cartelle preferite (nota A del giro 5.80), le opzioni di visualizzazione col tuo mockup `homeLongPress.png` (nota B) e la grafica per la scheda di Google Play.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.90 è pubblicata: [v5.90](https://github.com/Roccobot/AIV/releases/tag/v5.90), con i due APK
[AIV-5.90.apk](https://github.com/Roccobot/AIV/releases/download/v5.90/AIV-5.90.apk) (AIV GitHub) e [AIV-Play-5.90.apk](https://github.com/Roccobot/AIV/releases/download/v5.90/AIV-Play-5.90.apk) (AIV Play), più [AIV-Play-5.90.aab](https://github.com/Roccobot/AIV/releases/download/v5.90/AIV-Play-5.90.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `72641cd`, release dal commit `72641cd` (SlimVer 5.90 / versionCode 374; APK GitHub 11.124.641 byte, digest `861ffa58`; APK Play 11.014.233 byte, digest `dfecc9e6`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.90**: le due note del giro 5.80 e la domanda sulle immagini per lo Store. Giro **5.80**, cumulativo con la 5.71: le otto prove sono approvate, quattro etichette sono confermate e la quinta è riveduta da te, e le tre domande sullo Store hanno le tue risposte (E1, F1, G1).

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale, coi conteggi in cima. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, salvo in un documento cumulativo, dove restano quelli del giro che prosegue, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 5.90 sono superate: banco di prova completo di AIV GitHub (731 prove), prove di AIV Play (43), controllo delle traduzioni, compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.90-01 | Non provato | | Attendere il collaudo. |
| 5.90-02 | Non provato | | Attendere il collaudo. |

## 1. La pagina delle cartelle preferite

Vale per AIV GitHub e per AIV Play. In `Impostazioni` → `Cartelle` apri `Cartelle preferite` e togli con la × tutte le preferite: sotto la spiegazione nuova, la riga `Nessuna cartella preferita` è centrata e più staccata dal testo sopra, e `Aggiungi cartella` la segue subito sotto. Poi aggiungi una decina di cartelle, tante da riempire lo schermo: `Aggiungi cartella` resta fermo in fondo, sempre in vista, e scorre la sola lista.

Letture mie: la fascia fissa in basso è la stessa di `Esporta e importa`. Ho letto *fisso e sempre sopra la lista* come un pulsante che resta in vista sotto la lista che scorre, senza coprirla: se intendevi un pulsante sovrapposto alla lista, che le scorre sotto, dimmelo.

## 2. Le opzioni di visualizzazione

Vale per AIV GitHub e per AIV Play. In griglia tieni premuto il pulsante principale (il tondo o la pillola del menu Start, il menu in basso, il tondo della pillola a scomparsa): sotto `Dimensione della griglia` c'è `Disposizione`, coi gettoni `Alfabetica` e `Più recenti in alto`, e in fondo la riga `Cartelle preferite`. Ogni fila di gettoni occupa tutta la larghezza: il bordo sinistro del primo gettone è sulla stessa verticale dei titolini, l'ultimo finisce sul bordo destro, e i gettoni `2`, `3` e `4` sono allineati come gli altri. Tocca `Più recenti in alto`: la griglia dietro si riordina. Tocca `Cartelle preferite`: si apre la pagina in una scheda sopra la casa, e Indietro ti riporta alla casa. Ripeti in lista: c'è `Cartelle preferite` ma non `Disposizione`. Nelle `Cartelle di sistema` non c'è nessuna delle due.

Letture mie: i colori delle cartelle non sono più in questa finestra, come hai deciso; restano in `Impostazioni` → `Cartelle`. La scheda è la stessa strada della `Filigrana` nell'editor, così da lì non si gira nelle altre impostazioni. Il contenuto della finestra adesso scorre, perché col telefono in orizzontale le voci nuove non ci stanno. Lo spazio avanzato di ogni fila si divide in parti uguali fra i gettoni, quindi un nome lungo tiene il suo spazio e uno corto si allarga.

## Domande

### d-store-immagini · L'icona e l'immagine in evidenza per Google Play
Secondo la tua risposta G1 le ho composte dai disegni dell'app, con lo stesso strumento dell'anteprima del link della paginetta: l'[icona 512 x 512](https://github.com/Roccobot/AIV/blob/main/docs/store/icon-512.png) è il disegno intero, senza ritaglio, perché gli angoli arrotondati li aggiunge Google; l'[immagine in evidenza 1024 x 500](https://github.com/Roccobot/AIV/blob/main/docs/store/feature-1024x500.png) ha a sinistra l'icona e il titolo, a destra la schermata chiara della paginetta.

**H1**: Approvo così: le carico nella Console come sono.

**H2**: Le correggo: le correzioni sono nel commento.

Parere: **H1** se ti tornano; la composizione è quella dell'anteprima del link, che conosci già.

## Etichette testuali

### e-favourite_folders · La pagina delle cartelle preferite, riveduta da te
<!-- chiavi: favourite_folders_desc favourite_folders_none -->
Titolo: Cartelle preferite

Spiegazione: Sono cartelle mostrate per prime (in alto) nelle modalità griglia e lista, nell'ordine che preferisci: trascina gli elementi della lista qui sotto per riordinarle.

Con la pagina vuota: Nessuna cartella preferita

### e-folder_order_short · Il titolino della disposizione, nelle opzioni di visualizzazione
Disposizione

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Le cartelle preferite | 5.80-02 | Tutto OK | Nota A: riga vuota centrata e staccata, `Aggiungi cartella` in alto da vuota e fisso in basso con la lista. | Fatta nella 5.90 (`5.90-01`). |
| Le opzioni di visualizzazione | 5.80 | Nota B | Mockup `homeLongPress.png`: `Disposizione` in griglia, `Cartelle preferite`, gettoni a tutta larghezza, niente colori. | Fatta nella 5.90 (`5.90-02`). |
| AIV GitHub: l'aggiornamento da una release alla successiva | 5.71-01 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo, giro 5.90**: la pagina delle preferite (`5.90-01`), le opzioni di visualizzazione (`5.90-02`), le due etichette e la domanda sulle immagini per lo Store.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app, i 12 tester per i 14 giorni del test chiuso e le schermate del telefono per la scheda (risposta G1); i passi in ordine sono in fondo a `docs/Store.md`.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
