# Feedback AIV

Versione **4.45**: Luminosità e il grigio nel modulo Disegno; il passaggio da un menu a una scheda non lampeggia più.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.45 è pubblicata: [v4.45](https://github.com/Roccobot/AIV/releases/tag/v4.45), con l'APK
[AIV-4.45.apk](https://github.com/Roccobot/AIV/releases/download/v4.45/AIV-4.45.apk).
Commit prodotto su `main`: `4aadde3`, release dal commit `4aadde3` (SlimVer 4.45 / versionCode 333; APK 8.510.961 byte, digest `79afd910`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.45**: tre prove nuove. Giro **4.44**: `4.44-01` e `4.44-02` tutte OK.

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

Le verifiche automatiche della 4.45 sono superate: banco di prova completo (615 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.45-01 | Non provato | | Attendere il collaudo. |
| 4.45-02 | Non provato | | Attendere il collaudo. |
| 4.45-03 | Non provato | | Attendere il collaudo. |

## 1. Dal menu a Info, senza lampeggio

Tieni premuta una miniatura e scegli `Info`: il velo e la sfocatura dietro restano fermi mentre il menu se ne va e la scheda entra. Prova anche altri passaggi da una superficie all'altra, per esempio da un menu a una conferma. Il velo è misurato dal banco; la sfocatura no, perché la fa il telefono: se vedi ancora un lampo, dimmi dove.

## 2. Luminosità

Apri il modulo `Disegno` e accendi **Luminosità**, il quarto tasto, che mostra il colore dal più scuro al più chiaro. Il cursore sotto diventa `Luminosità`: verso destra il colore schiarisce, verso sinistra scurisce, e si ferma prima del bianco e del nero; tinta e opacità restano quelle. Vale per il bersaglio scelto: con `Traccia` cambia la linea, con `Riempimento` il riempimento. Il tondo scelto resta scelto, e il tasto `Traccia` e la punta piena mostrano il colore nuovo; se scegli un altro tondo, la luminosità riparte dal suo colore. Due scelte sono mie: i limiti (15% e 85% di luminosità) e la ripartenza da zero col tondo nuovo. Se ne vuoi altri, scrivilo qui.

## 3. Il grigio

Fra il bianco e il nero c'è il grigio `#B3B3B3`: ho letto 'grigio 30%' come una tinta al 30% di nero, il modo in cui lo dice un grafico. Se intendevi un grigio scuro (`#4D4D4D`), scrivilo qui. Sugli schermi stretti i tondi si rimpiccioliscono un poco perché ci stiano tutti e dieci.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Dal menu a Info, senza lampeggio | 4.45-01 | Non provato | | Attendere il collaudo. |
| Luminosità | 4.45-02 | Non provato | | Attendere il collaudo. |
| Il grigio | 4.45-03 | Non provato | | Attendere il collaudo. |
| Traccia, i tasti e la tua tavolozza | 4.44-01 | OK | Tutto OK, con un colore in più: il grigio. | Ripresa in `4.45-03`. |
| Il velo dietro i pannelli | 4.44-02 | OK | Eccellente; un lampeggio dal menu a Info. | Ripresa in `4.45-01`. |

## Prossimi passi

- **In collaudo**: il passaggio senza lampeggio (`4.45-01`), Luminosità (`4.45-02`), il grigio (`4.45-03`).
- **Prossima versione**: Disegno G2 (4.50): un tocco seleziona un oggetto, il rettangolo mostra quattro vertici color accento e la freccia due punti, i parametri cambiano l'oggetto scelto, e si elimina.
- **Dopo**: G2, seconda metà (maniglie, sopra e sotto); lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: il modulo Disegno dalla prima fase ai tasti disegnati (`4.40-01`-`4.44-01`); il velo nuovo (`4.44-02`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
