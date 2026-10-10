# Feedback AIV

Versione **4.99**: la pagina `Filigrana` è rifatta sul tuo mockup, con l'anteprima che mostra la firma dal vivo; l'icona e le squadrette dei posti sono ritoccate.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.99 è pubblicata: [v4.99](https://github.com/Roccobot/AIV/releases/tag/v4.99), con l'APK
[AIV-4.99.apk](https://github.com/Roccobot/AIV/releases/download/v4.99/AIV-4.99.apk).
Commit prodotto su `main`: `85b38c4`, release dal commit `85b38c4` (SlimVer 4.99 / versionCode 356; APK 11.027.865 byte, digest `24595ed1`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.99**: tre prove sulla pagina `Filigrana` e sette etichette. Giro **4.98**: le note sulla pagina `Filigrana`, fatte qui.

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

Le verifiche automatiche della 4.99 sono superate: banco di prova completo (691 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.99-01 | Non provato | | Attendere il collaudo. |
| 4.99-02 | Non provato | | Attendere il collaudo. |
| 4.99-03 | Non provato | | Attendere il collaudo. |

## 1. La pagina `Filigrana`

Apri `Impostazioni` → `Editor e salvataggio` → `Filigrana`. In cima c'è `Attiva` con l'interruttore a destra, e sotto il tuo testo; non c'è più il secondo titolo `Filigrana` né il paragrafo che lo seguiva. Poi la riga del file con `Rimuovi` e `Seleziona`, `Posizione`, i tre numeri, e in fondo l'avviso sul senza perdita. I campi delle percentuali sono più bassi (44 dp invece di 56), e lo spazio fra un numero e l'altro è minore. Tieni premuto il tasto `Filigrana` nell'editor: si apre la stessa pagina, con lo stesso ordine.

Letture mie: `Seleziona` è una stringa della sola pagina, perché `Scegli` vale anche nella riga `Editor di immagini`, che non hai nominato; i cursori hanno la loro altezza di Material, perché sotto quella il dito non li prende bene.

## 2. L'anteprima della filigrana

Con un logo scelto, il riquadro grigio è più piccolo di un quarto, e accanto c'è la nota in corsivo `⚠️ Anteprima solo indicativa: verifica la resa nell'immagine reale.`. Il logo si vede intero e pieno, al doppio della misura. Prova un logo bianco: il riquadro diventa nero. Prova un logo scuro, col tema chiaro: il riquadro resta grigio. Sposta il posto e la distanza dal bordo: il logo segue.

Letture mie: l'opacità nell'anteprima è sempre al 100%, e la misura doppia arriva al massimo al 50% del lato lungo; il riquadro diventa nero o bianco solo quando il logo sul grigio non arriva al contrasto minimo 3:1 (quello del W3C per gli elementi grafici).

## 3. Le squadrette dei posti e l'icona `Filigrana`

Nel riquadro della `Posizione`, guarda le squadrette degli angoli non scelti: la piega ha lo stesso colore dei bracci, senza il punto più scuro. Nell'editor, il rettangolino in basso a sinistra dell'icona `Filigrana` è più piccolo (due terzi per lato).

## Etichette testuali

### e-settings_mark_on · Titolo dell'interruttore della filigrana
Attiva
<!-- Prima: 'Applica al salvataggio'. -->

### e-settings_mark_on_desc · Spiegazione della filigrana, sotto 'Attiva'
La filigrana è un elemento grafico (es. firma, logo) sovrapposto all'immagine secondo le tue impostazioni. Puoi attivarla o disattivarla da qui, o farlo al volo prima del salvataggio con l'apposito tasto on/off nella barra in basso dell'editor; tieni premuto il tasto per accedere rapidamente a questa schermata.
<!-- È il tuo testo del giro 4.98. -->

### e-settings_mark_clear · Tasto che toglie il logo
Rimuovi
<!-- Prima: 'Togli'. -->

### e-settings_mark_pick · Tasto che sceglie il logo
Seleziona
<!-- Nuova stringa della sola pagina: 'Scegli' resta nella riga dell'editor. -->

### e-settings_mark_lossy · Avviso in fondo alla pagina Filigrana
⚠️ Con l'aggiunta della filigrana, le operazioni senza perdita come la rotazione JPEG non sono applicabili: il salvataggio prevede una riscrittura completa.
<!-- Il tuo testo; prima era la coda della spiegazione. -->

### e-settings_mark_preview_note · Nota accanto all'anteprima
⚠️ Anteprima solo indicativa: verifica la resa nell'immagine reale.

### e-settings_editor · Voce che sceglie l'editor
Editor di immagini
<!-- Prima: 'Editor immagini'. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La pagina `Filigrana` | 4.99-01 | Non provato | | Attendere il collaudo. |
| L'anteprima della filigrana | 4.99-02 | Non provato | | Attendere il collaudo. |
| Le squadrette e l'icona | 4.99-03 | Non provato | | Attendere il collaudo. |
| La spiegazione di `Filigrana` | 4.98 | Riscritta | Il tuo testo, con la pagina rifatta | Nelle etichette di questo giro. |

## Prossimi passi

- **In collaudo**: la pagina `Filigrana` (`4.99-01`), la sua anteprima (`4.99-02`), le squadrette e l'icona (`4.99-03`), e sette etichette.
- **Poi**: la 5.00, che ti chiederò di confermare.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
