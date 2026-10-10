# Feedback AIV

Versione **5.00**: la pagina `Filigrana` segue il tuo mockup del giro 4.99, e la riga `Editor di immagini` ha il tasto `Imposta app` accanto al titolo.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.00 è pubblicata: [v5.00](https://github.com/Roccobot/AIV/releases/tag/v5.00), con l'APK
[AIV-5.00.apk](https://github.com/Roccobot/AIV/releases/download/v5.00/AIV-5.00.apk).
Commit prodotto su `main`: `019e90c`, release dal commit `019e90c` (SlimVer 5.00 / versionCode 357; APK 11.024.109 byte, digest `528d567d`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.00**: due prove e cinque etichette. Giro **4.99**: le note sulla pagina `Filigrana` e sulla riga dell'editor, fatte qui.

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

Le verifiche automatiche della 5.00 sono superate: banco di prova completo (698 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.00-01 | Non provato | | Attendere il collaudo. |
| 5.00-02 | Non provato | | Attendere il collaudo. |

## 1. La pagina `Filigrana`, sul tuo mockup

Apri `Impostazioni` → `Editor e salvataggio` → `Filigrana`. L'interruttore è sulla riga di `Attiva`, e la spiegazione sotto è larga tutta la pagina. Il riquadro è più a sinistra: il bordo esterno della squadretta sinistra cade dove comincia il testo. `Posizione` è dentro il riquadro, in alto al centro: bianca sul riquadro nero, nera sul bianco, del colore del testo sul grigio. Accanto c'è la nota nuova, grigia, senza corsivo né `⚠️`, senza parole spezzate a fine riga. Con un carattere di sistema molto grande, o su uno schermo stretto, la nota va sotto il riquadro e nessuna parola si spezza.

Letture mie: il colore della nota è `#B1B1B1`, il grigio che ho misurato sul tuo mockup, perché `#fcfbf7` è il fondo stesso della pagina e la nota sparirebbe; il testo è salvato su una riga sola, e gli a capo li decide la larghezza, che nel tuo mockup dà la stessa colonna stretta.

## 2. La riga `Editor di immagini`

Nella stessa pagina `Editor e salvataggio`, il tasto `Imposta app` è a destra del titolo `Editor di immagini`. Sotto c'è la tua spiegazione, e sotto ancora l'app in vigore.

Lettura mia: la frase sull'editor interno (ritaglio e rotazione) non c'è più, perché il tuo testo sostituisce il paragrafo per intero; l'editor interno resta fra le scelte di `Imposta app`.

## Etichette testuali

### e-settings_mark_on_desc · Spiegazione della filigrana, sotto 'Attiva'
La filigrana è un elemento grafico (es. firma, logo) sovrapposto all'immagine. Puoi attivarla o disattivarla da qui, o farlo al volo prima del salvataggio con l'apposito tasto on/off nella barra in basso dell'editor; tieni premuto il tasto per accedere rapidamente a questa schermata.
<!-- Il tuo testo del giro 4.99. Prima: '...sovrapposto all'immagine secondo le tue impostazioni...'. -->

### e-settings_mark_lossy · Avviso in fondo alla pagina Filigrana
⚠️ Con l'aggiunta della filigrana, le operazioni senza perdita come la rotazione JPEG non sono applicabili: il salvataggio prevede una riscrittura del file.
<!-- Il tuo testo del giro 4.99. Prima: '...una riscrittura completa.'. -->

### e-settings_mark_preview_note · Nota accanto all'anteprima
Anteprima ingrandita a qualità bozza: verifica la filigrana nell'output.
<!-- Il tuo testo del giro 4.99, su una riga: gli a capo li decide la larghezza. Prima: '⚠️ Anteprima solo indicativa: verifica la resa nell'immagine reale.'. -->

### e-settings_editor_pick · Tasto accanto a 'Editor di immagini'
Imposta app
<!-- Il tuo testo del giro 4.99. Prima: 'Scegli'. -->

### e-settings_editor_desc · Spiegazione sotto 'Editor di immagini'
Scegli quale app deve aprire l'immagine con il comando Modifica (menu a pressione lunga del visualizzatore). Premi a lungo Modifica per cambiare app al volo senza riaprire le impostazioni.
<!-- Il tuo testo del giro 4.99. Prima: 'App associata al comando Modifica, nel menu del visualizzatore...'. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La pagina `Filigrana` | 5.00-01 | Non provato | | Attendere il collaudo. |
| La riga `Editor di immagini` | 5.00-02 | Non provato | | Attendere il collaudo. |
| La pagina `Filigrana` | 4.99-01 | Non approvato | Il mockup nuovo | Rifatta nella 5.00-01. |
| L'anteprima della filigrana | 4.99-02 | Tutto OK | | Chiusa. |
| Le squadrette e l'icona | 4.99-03 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: la pagina `Filigrana` (`5.00-01`), la riga `Editor di immagini` (`5.00-02`) e cinque etichette.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
