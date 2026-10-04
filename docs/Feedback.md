# Feedback AIV

Versione **3.72**: le correzioni del giro 3.72, cioè doppio tocco, maniglia, conferme in orizzontale e intestazione di casa sul telefono.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 3.72 è pubblicata: [v3.72](https://github.com/Roccobot/AIV/releases/tag/v3.72), con l'APK
[AIV-3.72.apk](https://github.com/Roccobot/AIV/releases/download/v3.72/AIV-3.72.apk).
Commit prodotto su `main`: `d341538` (SlimVer 3.72 / versionCode 307).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **3.72**: quattro prove in collaudo; `3.72-01`, `3.72-02` e `3.72-03` sostituiscono `3.71-01`, `3.71-06` e `3.71-08`. Giro **3.71**: `3.71-02`, `3.71-03`, `3.71-04`, `3.71-05` e `3.71-07` OK.

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

Le verifiche automatiche della 3.72 sono superate: banco di prova completo (535 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.72-01 | Non provato | | Attendere il collaudo. |
| 3.72-02 | Non provato | | Attendere il collaudo. |
| 3.72-03 | Non provato | | Attendere il collaudo. |
| 3.72-04 | Non provato | | Attendere il collaudo. |

## 1. Il doppio tocco ingrandisce sul posto

Con AIV **3.72**, apri un'immagine alta con la barra delle info visibile e fai doppio tocco: l'immagine deve crescere restando centrata nello spazio libero dalla barra, senza scivolare verso un bordo. Un altro doppio tocco la riporta nella misura di riposo, sotto la barra, con lo stesso percorso al contrario.

La causa era un conto sbagliato: durante l'animazione lo spostamento dell'immagine si moltiplicava di nuovo a ogni fotogramma, e a fine corsa l'immagine restava appoggiata al bordo. Ingrandita oltre lo spazio libero, l'immagine passa ancora sotto la barra, come quando la ingrandisci con due dita: se è questo che ti dava fastidio, dimmelo e lo affrontiamo a parte.

## 2. Tablet in verticale: la maniglia di nuovo sopra

Sul **tablet in verticale** la maniglia è di nuovo **sopra** l'elenco delle cartelle, con le due righe tenui al centro e la stessa distanza di prima. Trascinala con la navigazione a gesti attiva: deve spostare l'elenco.

## 3. Le conferme in orizzontale non sono più tagliate

Sul **telefono in orizzontale**, tieni premuta una cartella e scegli di nasconderla: la finestra 'Vuoi nascondere...' deve essere più larga, con tutto il testo e il tasto 'Applica a tutte le cartelle allo stesso livello' interi, e un po' d'aria sotto di sé. Vale per tutte le finestre centrate, quindi se ne incontri altre in orizzontale guardale. In verticale le finestre restano come prima.

## 4. Intestazione di casa sul telefono in orizzontale

Sul **telefono in orizzontale**, nella schermata iniziale: accanto all'icona ci sono solo 'Astonishing' e 'Image Viewer', su due righe, piccoli e tenui, senza la firma.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Doppio tocco sul posto | 3.72-01 | Non provato | | Attendere il collaudo. |
| Tablet in verticale: maniglia sopra | 3.72-02 | Non provato | | Attendere il collaudo. |
| Conferme in orizzontale | 3.72-03 | Non provato | | Attendere il collaudo. |
| Intestazione di casa sul telefono | 3.72-04 | Non provato | | Attendere il collaudo. |
| Barra delle info sottile anche su due righe | 3.71-01 | Accettabile | Il doppio tocco fa un percorso strano. | Sostituita da 3.72-01. |
| Telefono in verticale: tutto schermo nel visualizzatore | 3.71-02 | OK | Tutto OK. | Archiviata. |
| Tablet in orizzontale: immagini alte e barra di stato | 3.71-03 | OK | Tutto OK. | Archiviata. |
| Schermo largo: pillola in basso | 3.71-04 | OK | Tutto OK. | Archiviata. |
| Schermata iniziale in orizzontale | 3.71-05 | OK | Tutto OK. | Archiviata. |
| Tablet in verticale: pillola compatta e maniglia | 3.71-06 | Accettabile | Maniglia di nuovo in alto, con la stessa spaziatura. | Sostituita da 3.72-02. |
| Testa della colonna | 3.71-07 | OK | Tutto OK. | Archiviata. |
| Conferme in orizzontale | 3.71-08 | Non approvato | Tasto del testo tagliato. | Sostituita da 3.72-03. |

## Prossimi passi

- **In collaudo**: doppio tocco (`3.72-01`), maniglia (`3.72-02`), conferme in orizzontale (`3.72-03`), intestazione di casa (`3.72-04`).
- **Rimasti fuori, da decidere se servono**: la vista 'Cartelle di sistema' tiene il FAB anche su tablet e in orizzontale; nella pillola di casa il tocco lungo su 'Mostra nascoste' non apre il pannello.
- **Concluso**: tutto schermo nel visualizzatore, barra di stato del tablet, pillola in basso, schermata iniziale, testa della colonna (`3.71-02` - `3.71-05`, `3.71-07`); etichetta dell'invito approvata.
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
