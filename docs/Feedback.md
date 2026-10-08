# Feedback AIV

Versione **4.50**: nel modulo Disegno il cursore della luminosità copre i tondi e il pollice va sotto il dito, la linea di Spessore alla misura vera, i valori di fabbrica, gli Stili in fondo.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.50 è pubblicata: [v4.50](https://github.com/Roccobot/AIV/releases/tag/v4.50), con l'APK
[AIV-4.50.apk](https://github.com/Roccobot/AIV/releases/download/v4.50/AIV-4.50.apk).
Commit prodotto su `main`: `41ecd2b`, release dal commit `41ecd2b` (SlimVer 4.50 / versionCode 338; APK 8.525.257 byte, digest `0242419a`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.50**: sei prove nuove. Giro **4.49**: `4.49-01` OK con una richiesta, fatta in `4.50-01`; `4.49-02` ripresa in `4.50-02`.

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

Le verifiche automatiche della 4.50 sono superate: banco di prova completo (630 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.50-01 | Non provato | | Attendere il collaudo. |
| 4.50-02 | Non provato | | Attendere il collaudo. |
| 4.50-03 | Non provato | | Attendere il collaudo. |
| 4.50-04 | Non provato | | Attendere il collaudo. |
| 4.50-05 | Non provato | | Attendere il collaudo. |
| 4.50-06 | Non provato | | Attendere il collaudo. |

## 1. La linea di Spessore alla misura vera

Nel modulo `Disegno` la linea del tasto `Spessore` è spessa quanto il tratto che risulterà sullo schermo, cioè quanto la lineetta dell'anteprima mentre muovi il cursore. Segue anche lo zoom: ingrandendo l'immagine la linea si fa più spessa. Alle misure più piccole resta di 2 dp, e alle più grandi si ferma all'altezza del tasto.

## 2. Il cursore della luminosità sopra i tondi, col pollice sotto il dito

Tieni premuto un tondo: il cursore della luminosità si apre sopra la fila dei tondi e la copre, largo quanto lei, con il colore che ne risulta a sinistra. Senza staccare il dito, scorri: il pollice va dove è il dito, quindi da un tondo vicino al bordo arrivi a tutte e due le estremità. Allo stacco il colore si applica e il cursore si chiude; alzando il dito fermo il cursore resta, e lo chiudi toccando fuori.

## 3. I valori di fabbrica

Alla prima apertura del modulo `Disegno` trovi il rettangolo arrotondato con la linea tratteggiata, la traccia rossa con la luminosità al 25% della corsa, l'opacità al 50% e lo spessore al 40%; il riempimento resta l'ambra al 20%. 'Cursore al X%' l'ho letto come un posto sulla corsa: l'opacità va dal 10% al 100%, quindi a metà corsa vale il 55%. La luminosità di fabbrica vale per il rosso di partenza: un tocco su un tondo gli rende il suo colore, come prima.

## 4. L'anteprima di Spessore è una lineetta curva

Mentre tieni il cursore di `Spessore`, in basso a destra sull'immagine compare una lineetta curva alla misura vera del tratto, al posto del tondo pieno.

## 5. Gli Stili sempre in fondo

Il modulo `Stili` è sempre l'ultimo a destra, e il `Disegno` è subito prima. Vale anche se avevi riordinato la fila: nelle impostazioni, in `Ordine dei pulsanti`, riquadro `Editor completo: moduli`, un gettone degli Stili trascinato altrove torna in fondo.

## 6. I tondi sotto le finestre

Quando una finestra si apre sopra l'editor (per esempio `Vuoi scartare le modifiche?` uscendo con del lavoro in corso), i tondi dei colori svaniscono insieme all'arrivo della sfocatura, così non sbordano più dai bordi della finestra.

## Decisioni da concordare

- **Il tratteggio più evidente** (nota B del giro della 4.49): le proposte sono in chat.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La linea di Spessore alla misura vera | 4.50-01 | Non provato | | Attendere il collaudo. |
| Il cursore della luminosità sopra i tondi, col pollice sotto il dito | 4.50-02 | Non provato | | Attendere il collaudo. |
| I valori di fabbrica | 4.50-03 | Non provato | | Attendere il collaudo. |
| L'anteprima di Spessore è una lineetta curva | 4.50-04 | Non provato | | Attendere il collaudo. |
| Gli Stili sempre in fondo | 4.50-05 | Non provato | | Attendere il collaudo. |
| I tondi sotto le finestre | 4.50-06 | Non provato | | Attendere il collaudo. |
| Spessore prima di Riempimento | 4.49-01 | OK | La linea di Spessore alla misura vera del tratto. | Fatto in `4.50-01`. |
| Il cursore della luminosità sopra i tondi | 4.49-02 | Non approvato | Il cursore sopra i tondi come un livello che li copre, con l'anteprima a lato. | Ripresa in `4.50-02`. |
| L'ombra della selezione | 4.48-01 | OK | Meravigliosa. | Concluso. |

## Prossimi passi

- **In collaudo**: la linea di Spessore (`4.50-01`), il cursore della luminosità (`4.50-02`), i valori di fabbrica (`4.50-03`), l'anteprima curva (`4.50-04`), gli Stili in fondo (`4.50-05`), i tondi sotto le finestre (`4.50-06`).
- **Da scegliere in chat**: il tratteggio più evidente (nota B).
- **Dopo**: G2 (`4.60`: selezione, vertici, parametri sull'oggetto, elimina; poi maniglie, sopra e sotto); lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: Spessore prima di Riempimento (`4.49-01`); l'ombra della selezione (`4.48-01`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
