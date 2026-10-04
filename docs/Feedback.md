# Feedback AIV

Versione **3.70**: telefono e tablet in orizzontale, tablet in verticale, e le due correzioni del visualizzatore dal giro 3.60.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 3.70 è pubblicata: [v3.70](https://github.com/Roccobot/AIV/releases/tag/v3.70), con l'APK
[AIV-3.70.apk](https://github.com/Roccobot/AIV/releases/download/v3.70/AIV-3.70.apk).
Commit prodotto su `main`: `f73dc24` (SlimVer 3.70 / versionCode 305).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **3.70**: sette prove in collaudo; `3.70-01` e `3.70-02` sostituiscono `3.60-01` e `3.60-02`, non approvate. Giro **3.60**: `3.54-01` e `3.60-03` OK.

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

Le verifiche automatiche della 3.70 sono superate: banco di prova completo (524 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.70-01 | Non provato | | Attendere il collaudo. |
| 3.70-02 | Non provato | | Attendere il collaudo. |
| 3.70-03 | Non provato | | Attendere il collaudo. |
| 3.70-04 | Non provato | | Attendere il collaudo. |
| 3.70-05 | Non provato | | Attendere il collaudo. |
| 3.70-06 | Non provato | | Attendere il collaudo. |
| 3.70-07 | Non provato | | Attendere il collaudo. |

## 1. Barra delle info più sottile

Con AIV **3.70**, apri un'immagine sul **telefono in orizzontale** e sul **tablet**: la barra delle info su una riga sola deve essere più bassa della 3.60, alta quanto il testo più un filo d'aria sopra e sotto. Sul telefono in orizzontale non ci sono più la barra di stato e quella di navigazione, quindi la barra delle info arriva al bordo dello schermo.

Sul **telefono in verticale** la barra resta su due righe, come prima. Se la riga unica ti sembra ancora spessa, dimmi di quanto la vorresti più sottile.

## 2. L'immagine compare subito nella misura giusta

Apri dall'elenco uno screenshot o un'altra immagine alta, sul **telefono in verticale**: deve comparire direttamente sotto la barra delle info, già nella misura finale, senza nascere più grande e restringersi. Ripeti in orizzontale con un'immagine verticale.

Poi tieni premuto sull'immagine, tocca `Info` e spegni `Barra delle info`: l'immagine deve allargarsi con una breve animazione, e tornare a fare posto con la stessa animazione quando riaccendi la barra.

## 3. Telefono in orizzontale: tutto schermo

Gira il **telefono** in orizzontale: in tutte le schermate (elenco delle cartelle, griglia, visualizzatore, impostazioni) la barra di stato e quella di navigazione devono sparire. Uno scorrimento dal bordo le fa ricomparire per qualche secondo. Tornando in verticale devono ricomparire stabilmente.

Sul **tablet** le barre di sistema restano in tutti e due gli orientamenti.

## 4. Schermo largo: la griglia di una cartella

Sul **telefono in orizzontale** e sul **tablet in orizzontale**, apri una cartella:

- in testa alla griglia ci sono il nome della cartella e il numero di elementi su una riga, senza la fascia grande dell'intestazione;
- l'elenco delle cartelle a lato è ancorato in basso, e non si sposta più con la maniglia: con poche cartelle occupa il 40% inferiore della colonna, con molte sale fino al 70% e poi scorre dentro;
- sopra l'elenco ci sono la sfumatura, l'icona della cartella e le pastiglie (peso, immagini, video, `Seleziona tutto`), con gli stessi gesti dell'intestazione del telefono;
- sotto il tasto del filtro, a destra, c'è una pillola del colore d'accento con Cerca, Cestino e Impostazioni, e il FAB non c'è più.

Prova `Seleziona tutto` dalla colonna: la selezione deve comparire nella griglia, e durante la selezione la pillola deve sparire.

## 5. Schermo largo: la schermata iniziale

Sempre in orizzontale, torna alla schermata iniziale: in cima alla colonna c'è l'identità dell'app, sotto l'elenco delle cartelle ancorato in basso, e a destra l'invito a scegliere una cartella con accanto la pillola delle voci del FAB di casa: le due altre viste, `Mostra nascoste` se ne hai, Cerca, Indirizzo, Cestino e Impostazioni.

Prova almeno Indirizzo (si apre la stessa finestra di prima) e Impostazioni. Il tocco lungo su `Mostra nascoste` qui non apre il pannello delle nascoste.

## 6. Schermo largo: cestino e ricerca

In orizzontale apri il **cestino**: a destra c'è la pillola con Cronologia, Ripristina tutto e Svuota il cestino, nello stesso ordine del menu; a cestino vuoto le ultime due sono spente.

Poi apri una **ricerca**: la colonna ha l'elenco in basso e la testa vuota, e la pillola ha Cestino e Impostazioni.

## 7. Tablet in verticale: la pillola in basso

Sul **tablet in verticale**, in fondo alla colonna delle cartelle non ci sono più Cerca, Cestino e Impostazioni: sono in una pillola d'accento in basso al centro, col campo 'Cerca in ...' in testa, poi Impostazioni e Cestino. Nella schermata iniziale il campo dice 'Cerca nelle cartelle', dentro una cartella 'Cerca in' col suo nome. Nel cestino la pillola in basso ha le sue tre voci.

La colonna delle cartelle si sposta ancora con la maniglia, come prima. Controlla che l'ultima riga di miniature possa salire sopra la pillola.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Barra delle info più sottile | 3.70-01 | Non provato | | Attendere il collaudo. |
| Immagine subito nella misura giusta | 3.70-02 | Non provato | | Attendere il collaudo. |
| Telefono in orizzontale: tutto schermo | 3.70-03 | Non provato | | Attendere il collaudo. |
| Schermo largo: griglia di una cartella | 3.70-04 | Non provato | | Attendere il collaudo. |
| Schermo largo: schermata iniziale | 3.70-05 | Non provato | | Attendere il collaudo. |
| Schermo largo: cestino e ricerca | 3.70-06 | Non provato | | Attendere il collaudo. |
| Tablet in verticale: pillola in basso | 3.70-07 | Non provato | | Attendere il collaudo. |
| Barra delle info su una riga | 3.60-01 | Non approvato | Da assottigliare, e telefono a tutto schermo. | Sostituita da 3.70-01 e 3.70-03. |
| Immagine nello spazio libero | 3.60-02 | Non approvato | All'apertura si restringe con un'animazione. | Sostituita da 3.70-02. |
| Barra nascosta, immagine a tutto schermo | 3.60-03 | OK | Tutto OK. | Archiviata. |
| Elenco cartelle del tablet: nomi lunghi a capo | 3.54-01 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: barra più sottile e apertura senza animazione (`3.70-01`, `3.70-02`); tutto schermo sul telefono in orizzontale (`3.70-03`); schermo largo e tablet in verticale (`3.70-04` - `3.70-07`).
- **Rimasti fuori, da decidere se servono**: la vista 'Cartelle di sistema' tiene il FAB anche su tablet e in orizzontale; nella pillola di casa il tocco lungo su 'Mostra nascoste' non apre il pannello.
- **Concluso**: barra nascosta e immagine a tutto schermo (`3.60-03`), nomi lunghi nell'elenco del tablet (`3.54-01`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
