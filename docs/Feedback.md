# Feedback AIV

Versione **4.60**: nel modulo Disegno si sceglie un segno con un tocco, e lo si cambia, lo si sposta o lo si elimina; il tasto Tratteggio dice se è acceso.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.60 è pubblicata: [v4.60](https://github.com/Roccobot/AIV/releases/tag/v4.60), con l'APK
[AIV-4.60.apk](https://github.com/Roccobot/AIV/releases/download/v4.60/AIV-4.60.apk).
Commit prodotto su `main`: `13e5ea1`, release dal commit `13e5ea1` (SlimVer 4.60 / versionCode 339; APK 8.541.641 byte, digest `83263b20`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.60**: cinque prove nuove. Giro **4.50**: sei prove OK.

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

Le verifiche automatiche della 4.60 sono superate: banco di prova completo (635 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.60-01 | Non provato | | Attendere il collaudo. |
| 4.60-02 | Non provato | | Attendere il collaudo. |
| 4.60-03 | Non provato | | Attendere il collaudo. |
| 4.60-04 | Non provato | | Attendere il collaudo. |
| 4.60-05 | Non provato | | Attendere il collaudo. |

## 1. Un tocco sceglie un segno

Nel modulo `Disegno` disegna due o tre forme, poi tocca una di loro: il rettangolo e l'ellisse mostrano quattro punti color accento ai vertici del loro riquadro, la linea e la freccia due punti ai capi, la mano libera quattro punti ai vertici del suo riquadro. Una forma riempita si prende anche toccandola dentro, una vuota toccandone la linea (ho dato al dito 24 dp di margine). Tocca un punto vuoto: la scelta sparisce, e quel tocco non disegna niente.

## 2. I parametri cambiano il segno scelto

Con un segno scelto, il modulo mostra i suoi valori: il tondo del colore, la luminosità, l'opacità, lo spessore, il tratteggio e il riempimento. Cambiane uno: cambia il segno scelto, e gli altri restano come sono. Ogni cambio si annulla con `Annulla`; un cursore trascinato è un passo solo. Dopo aver tolto la scelta, il segno nuovo nasce con gli ultimi valori. Un tasto strumento (rettangolo, freccia...) toglie la scelta e cambia strumento.

## 3. Il segno scelto si sposta

Con un segno scelto, appoggia il dito sul segno e trascina: il segno ti segue. Partendo fuori dal segno si disegna come prima.

## 4. Elimina

La quinta colonna dei tasti, sotto gli strumenti, è `Elimina`: si accende con un segno scelto e lo toglie. `Annulla` lo riporta.

## 5. Il tasto Tratteggio acceso e spento

Come hai scelto (B2): acceso, il tasto `Tratteggio` mostra il tratteggio nel colore della traccia; spento, il tratteggio è grigio e sbiadito.

## Decisioni da concordare

Nessuna decisione aperta in questo giro.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Un tocco sceglie un segno | 4.60-01 | Non provato | | Attendere il collaudo. |
| I parametri cambiano il segno scelto | 4.60-02 | Non provato | | Attendere il collaudo. |
| Il segno scelto si sposta | 4.60-03 | Non provato | | Attendere il collaudo. |
| Elimina | 4.60-04 | Non provato | | Attendere il collaudo. |
| Il tasto Tratteggio acceso e spento | 4.60-05 | Non provato | | Attendere il collaudo. |
| La linea di Spessore alla misura vera | 4.50-01 | OK | | Concluso. |
| Il cursore della luminosità sopra i tondi, col pollice sotto il dito | 4.50-02 | OK | | Concluso. |
| I valori di fabbrica | 4.50-03 | OK | | Concluso. |
| L'anteprima di Spessore è una lineetta curva | 4.50-04 | OK | | Concluso. |
| Gli Stili sempre in fondo | 4.50-05 | OK | | Concluso. |
| I tondi sotto le finestre | 4.50-06 | OK | | Concluso. |

## Prossimi passi

- **In collaudo**: la scelta di un segno (`4.60-01`), i parametri sul segno scelto (`4.60-02`), lo spostamento (`4.60-03`), `Elimina` (`4.60-04`), il tasto Tratteggio (`4.60-05`).
- **Dopo**: la seconda parte della G2 (maniglie per ridimensionare e ruotare, sopra e sotto); lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: le sei prove della 4.50 (`4.50-01..06`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
