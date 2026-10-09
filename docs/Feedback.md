# Feedback AIV

Versione **4.94**: nel modulo Disegno le file hanno la stessa aria fra loro, e la striscia scelta ha gli angoli stondati.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.94 è pubblicata: [v4.94](https://github.com/Roccobot/AIV/releases/tag/v4.94), con l'APK
[AIV-4.94.apk](https://github.com/Roccobot/AIV/releases/download/v4.94/AIV-4.94.apk).
Commit prodotto su `main`: `416b934`, release dal commit `416b934` (SlimVer 4.94 / versionCode 351; APK 11.015.461 byte, digest `e58f5954`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.94**: due prove sul modulo Disegno e una domanda. Giro **4.93**: la prova del doppio tocco OK, e tre note in Altro.

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

Le verifiche automatiche della 4.94 sono superate: banco di prova completo (679 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.94-01 | Non provato | | Attendere il collaudo. |
| 4.94-02 | Non provato | | Attendere il collaudo. |

## 1. Le file del modulo Disegno

Apri il modulo Disegno e confronta la scheda col tuo mockup: fra i gettoni dei moduli, gli strumenti, i tasti, i tondi e la fila di `Dimensione` con `Elimina` ed `Elimina tutto` c'è la stessa aria; il nome del cursore resta vicino al cursore; sotto il cursore la fila delle strisce, e sotto le strisce un po' più d'aria prima della barra in fondo. Passa da uno strumento all'altro e accendi e spegni `Sfondo`: il cursore e le file dei tasti non si muovono. Poi passa agli altri moduli: la scheda è un po' più alta di prima, uguale per tutti.

Letture mie: l'aria fra due file è di 12 dp, presa dal tuo mockup, dove va da 11 a 15; la fila del nome e di `Elimina` è alta come i tasti; il Disegno è il modulo più alto, quindi la scheda cresce con lui (17 dp, misurati sul banco) e il palco si stringe di altrettanto.

## 2. La striscia scelta

Scrivi un testo, accendi `Sfondo` e scegli una striscia, poi la prima e l'ultima: il bordo scuro, il filo chiaro dentro e il colore hanno gli angoli stondati, uno dentro l'altro, come nel tuo disegno; sulla prima e sull'ultima striscia il bordo scuro segue anche lo stondamento della fila.

## Domande

### d-tasto-disturbo · Il disturbo sul tasto `Testo`

Nelle tue schermate il disturbo è un alone grigio granuloso: è l'onda che Android disegna sotto il dito quando si tocca un tasto. In `UI.png` ce n'è una su `Testo` e una su `Barrato`, distanti quanto due dita. Sul banco l'onda si spegne da sola, quindi penso che la schermata fatta con tre dita abbia toccato quei due tasti. Scegli `Testo`, aspetta un secondo e fai una schermata coi tasti di accensione e volume, poi guarda il tasto anche a occhio.

**T1**: coi tasti il disturbo non c'è: era il gesto della schermata, e non cambio niente.

**T2**: il disturbo c'è anche coi tasti, o lo vedo a occhio: lo indago con una registrazione dello schermo che mi mandi.

Parere: **T1**, perché le due onde sono su due file diverse e alla distanza di due dita, e sul banco nessun tasto resta segnato.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Le file del modulo Disegno | 4.94-01 | Non provato | | Attendere il collaudo. |
| La striscia scelta | 4.94-02 | Non provato | | Attendere il collaudo. |
| Il doppio tocco su testo, pillola e pannello | 4.93-01 | OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: le file del Disegno (`4.94-01`), la striscia scelta (`4.94-02`) e la domanda sul disturbo del tasto `Testo` (`d-tasto-disturbo`).
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **Dopo**: gli stili, nella 4.95, con uno o due rilasci di assestamento; poi la 5.00.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
