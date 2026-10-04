# Feedback AIV

Versione **3.71**: le correzioni del giro 3.70, cioè barra delle info, tutto schermo del telefono, pillola, schermata iniziale e tablet in verticale.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 3.71 è pubblicata: [v3.71](https://github.com/Roccobot/AIV/releases/tag/v3.71), con l'APK
[AIV-3.71.apk](https://github.com/Roccobot/AIV/releases/download/v3.71/AIV-3.71.apk).
Commit prodotto su `main`: `fc03f81` (SlimVer 3.71 / versionCode 306).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **3.71**: otto prove in collaudo, che sostituiscono `3.70-01`, `3.70-04`, `3.70-05`, `3.70-06` e `3.70-07`. Giro **3.70**: `3.70-02` e `3.70-03` OK.

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

Le verifiche automatiche della 3.71 sono superate: banco di prova completo (531 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.71-01 | Non provato | | Attendere il collaudo. |
| 3.71-02 | Non provato | | Attendere il collaudo. |
| 3.71-03 | Non provato | | Attendere il collaudo. |
| 3.71-04 | Non provato | | Attendere il collaudo. |
| 3.71-05 | Non provato | | Attendere il collaudo. |
| 3.71-06 | Non provato | | Attendere il collaudo. |
| 3.71-07 | Non provato | | Attendere il collaudo. |
| 3.71-08 | Non provato | | Attendere il collaudo. |

## 1. Barra delle info sottile anche su due righe

Con AIV **3.71**, apri un'immagine sul **telefono in verticale** e sul **tablet**: la barra delle info deve avere sotto il testo solo un filo d'aria, e fra il nome e i dati un'interlinea stretta. Fino alla 3.70 sotto il testo restava una fascia vuota alta quanto la barra dei gesti di sistema: era quella lo spessore in più.

## 2. Telefono in verticale: tutto schermo nel solo visualizzatore

Sul **telefono in verticale** apri un'immagine: barra di stato e barra di navigazione devono sparire, e uno scorrimento dal bordo le fa ricomparire per qualche secondo. Torna all'elenco delle cartelle o alla griglia: lì le barre devono esserci. In orizzontale resta tutto schermo in tutte le schermate, come nella 3.70.

## 3. Tablet in orizzontale: le immagini alte non entrano nella barra

Sul **tablet in orizzontale** apri un'immagine alta (uno screenshot del telefono): può passare sotto la barra delle info solo fra il nome e i dati, e non deve mai arrivare sotto la barra di stato, con l'orologio e le icone. Fino alla 3.70 il 5% di tolleranza arrivava fin lassù.

## 4. Schermo largo: la pillola in basso

Sul **telefono** e sul **tablet in orizzontale**, apri una cartella: la pillola con Cerca, Cestino e Impostazioni è a destra, **in basso**, staccata di poco dal bordo. Lo stesso nel cestino e nella ricerca.

## 5. Schermata iniziale in orizzontale

Sul **telefono in orizzontale**, torna alla schermata iniziale:

- in cima alla colonna l'icona dell'app è a sinistra, col nome e la firma accanto;
- la pillola è a destra, in basso;
- al centro dello spazio a destra della colonna, esclusa la fascia della pillola, c'è `Scegli una cartella dalla barra di navigazione`, più tenue del resto.

Sul **tablet in orizzontale** la testa della colonna resta com'era, con l'icona sopra il nome; pillola e frase come sul telefono.

## 6. Tablet in verticale: pillola compatta e maniglia sotto

Sul **tablet in verticale**:

- la pillola è in basso a destra, con le sole tre icone Cerca, Cestino e Impostazioni, sia nella schermata iniziale sia dentro una cartella;
- la frase al centro è `Scegli una cartella dalla barra di navigazione`, più tenue;
- le righe dell'elenco delle cartelle hanno più margine ai lati;
- la maniglia è **sotto** l'elenco, al centro, con due righe tenui al posto dei puntini: trascinala per alzare e abbassare l'elenco.

## 7. Testa della colonna: pastiglie, sfumatura e foro della fotocamera

In orizzontale apri una cartella e guarda la testa della colonna:

- le pastiglie (peso, immagini, video, `Seleziona tutto`) vanno a capo solo quando non entrano, e ogni riga è centrata;
- sul **tablet** la sfumatura finisce più in alto, entro circa un terzo della testa: dimmi se le bande si vedono ancora;
- sul **telefono** con il foro della fotocamera dal lato della colonna la sfumatura non c'è; girando il telefono dall'altra parte, con il foro sul lato opposto, torna.

## 8. Le conferme non arrivano al fondo dello schermo

Sul **telefono in orizzontale**, tieni premuta una cartella e scegli di nasconderla: la finestra 'Vuoi nascondere...' deve lasciare un po' d'aria sotto di sé, sopra la barra dei gesti. Era la tua schermata `popup_phoneH`.

## Etichette testuali

### e-folders-tablet-pick-2 · Invito della schermata iniziale
Scegli una cartella dalla barra di navigazione

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Barra delle info sottile anche su due righe | 3.71-01 | Non provato | | Attendere il collaudo. |
| Telefono in verticale: tutto schermo nel visualizzatore | 3.71-02 | Non provato | | Attendere il collaudo. |
| Tablet in orizzontale: immagini alte e barra di stato | 3.71-03 | Non provato | | Attendere il collaudo. |
| Schermo largo: pillola in basso | 3.71-04 | Non provato | | Attendere il collaudo. |
| Schermata iniziale in orizzontale | 3.71-05 | Non provato | | Attendere il collaudo. |
| Tablet in verticale: pillola compatta e maniglia | 3.71-06 | Non provato | | Attendere il collaudo. |
| Testa della colonna | 3.71-07 | Non provato | | Attendere il collaudo. |
| Conferme in orizzontale | 3.71-08 | Non provato | | Attendere il collaudo. |
| Barra delle info più sottile | 3.70-01 | Non approvato | Più sottile anche su due righe; tutto schermo nel visualizzatore in verticale; tablet in orizzontale sotto la barra. | Sostituita da 3.71-01, 3.71-02 e 3.71-03. |
| Immagine subito nella misura giusta | 3.70-02 | OK | Tutto OK. | Archiviata. |
| Telefono in orizzontale: tutto schermo | 3.70-03 | OK | Tutto OK. | Archiviata. |
| Schermo largo: griglia di una cartella | 3.70-04 | Accettabile | Pillola agganciata in basso. | Sostituita da 3.71-04. |
| Schermo largo: schermata iniziale | 3.70-05 | Non approvato | Pillola in basso, frase nuova, testa coricata sul telefono. | Sostituita da 3.71-05. |
| Schermo largo: cestino e ricerca | 3.70-06 | Accettabile | Pillola in basso e frase nuova. | Sostituita da 3.71-04 e 3.71-05. |
| Tablet in verticale: pillola in basso | 3.70-07 | Accettabile | Pillola compatta a destra, frase, margini, maniglia sotto. | Sostituita da 3.71-06. |

## Prossimi passi

- **In collaudo**: barra delle info e tutto schermo (`3.71-01` - `3.71-03`); pillola, schermata iniziale e tablet in verticale (`3.71-04` - `3.71-06`); testa della colonna e conferme in orizzontale (`3.71-07`, `3.71-08`).
- **Rimasti fuori, da decidere se servono**: la vista 'Cartelle di sistema' tiene il FAB anche su tablet e in orizzontale; nella pillola di casa il tocco lungo su 'Mostra nascoste' non apre il pannello.
- **Concluso**: apertura senza animazione (`3.70-02`), tutto schermo in orizzontale (`3.70-03`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
