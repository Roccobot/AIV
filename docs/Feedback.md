# Feedback AIV

Versione **3.60**: tre prove sul visualizzatore (barra delle info e spazio dell'immagine), più la prova ancora aperta della 3.54.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 3.60 è pubblicata: [v3.60](https://github.com/Roccobot/AIV/releases/tag/v3.60), con l'APK
[AIV-3.60.apk](https://github.com/Roccobot/AIV/releases/download/v3.60/AIV-3.60.apk).
Commit prodotto su `main`: `4f3d8da` (SlimVer 3.60 / versionCode 304).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **3.60**: `3.60-01`, `3.60-02`, `3.60-03` in collaudo; `3.54-01` resta aperta. Giro **3.53**: `3.53-01` OK.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.60 sono superate: banco di prova completo (515 prove, due classi nuove sulla barra e sull'immagine), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.60-01 | Non provato | | Attendere il collaudo. |
| 3.60-02 | Non provato | | Attendere il collaudo. |
| 3.60-03 | Non provato | | Attendere il collaudo. |
| 3.54-01 | Non provato | | Attendere il collaudo. |

## 1. Barra delle info: una riga quando c'è posto

Apri un'immagine con AIV **3.60** e guarda la barra delle info in alto (o in basso, se l'hai spostata). Sul **telefono in orizzontale** e sul **tablet**, in tutti e due gli orientamenti, nome, dati e contatore devono essere su **una riga sola**: il nome a sinistra, i dati allineati a destra accanto al contatore, il glifo di AIV in fondo. Sul **telefono in verticale** restano due righe, col nome sopra i dati.

Sfoglia qualche immagine con nomi di lunghezza diversa e ingrandisci con due dita: la barra non deve cambiare altezza né passare da una a due righe. Il nome lungo si accorcia al centro, con l'estensione sempre visibile. Se la riga unica ti sembra troppo stretta per il nome, o la vorresti anche dove non compare, dillo nel commento: lo spazio minimo per il nome è una scelta (160 dp).

## 2. L'immagine a riposo non va sotto la barra

Sul **telefono in verticale**, apri un'immagine alta (per esempio una schermata del telefono): a riposo deve cominciare subito sotto la barra delle info e finire in fondo allo schermo, intera. Prima la sua parte alta restava nascosta sotto la barra.

Sul **telefono in orizzontale**, apri un'immagine verticale: può passare un poco sotto la barra (al massimo il 5% dell'altezza dello schermo), ma solo nel tratto centrale, dove la barra non ha testo; nessuna scritta deve trovarsi sopra l'immagine. Poi ingrandisci un poco e trascina verso il basso: la striscia che era sotto la barra deve poter scendere nello spazio libero.

## 3. Barra nascosta: l'immagine riprende tutto lo schermo

Apri un'immagine verticale, tieni premuto sull'immagine, tocca `Info` e spegni `Barra delle info`. L'immagine deve allargarsi fino a tutto lo schermo con una breve animazione, senza scatti. Riaccendi `Barra delle info`: l'immagine deve tornare a farle posto, con lo stesso movimento.

Alla prima immagine che apri, l'immagine compare a tutto schermo e si stringe subito mentre la barra compare: dimmi se quel movimento d'apertura ti disturba.

## 4. Elenco cartelle del tablet: i nomi lunghi vanno a capo

Con AIV **3.54** o successiva, sul **tablet**, guarda l'elenco delle cartelle nella colonna a lato: una cartella con un nome lungo scritto senza spazi (per esempio `FotografieVacanzeEstate`) deve andare a capo fra una parola e l'altra, dove la maiuscola segue la minuscola, come nella finestra delle destinazioni e nella pagina delle cartelle nascoste.

Due righe al massimo: un nome che non entra nemmeno in due viene troncato in fondo con i tre puntini. Un nome con gli spazi va a capo come prima. Se puoi, allega una schermata con un nome lungo.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Barra delle info su una riga | 3.60-01 | Non provato | | Attendere il collaudo. |
| Immagine nello spazio libero | 3.60-02 | Non provato | | Attendere il collaudo. |
| Barra nascosta, immagine a tutto schermo | 3.60-03 | Non provato | | Attendere il collaudo. |
| Elenco cartelle del tablet: nomi lunghi a capo | 3.54-01 | Non provato | | Attendere il collaudo. |
| Correggi/Rimuovi: bordo sfumato | 3.53-01 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: barra delle info e spazio dell'immagine nel visualizzatore, `3.60-01`, `3.60-02`, `3.60-03`; i nomi lunghi nell'elenco cartelle del tablet, `3.54-01`.
- **In lavorazione**: telefono e tablet in orizzontale (tutto schermo, colonna delle cartelle in basso dal 40 al 70%, pillola d'accento al posto del FAB) e tablet verticale (pillola in basso con la ricerca), versioni 3.70 e 3.80; prima del rilascio della 3.70 ti chiedo conferma.
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
