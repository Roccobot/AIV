# Feedback AIV

Versione **4.46**: la cartella d'origine spenta, gli avvisi centrati, l'ombra della selezione, la freccia tratteggiata, e il lampeggio fra menu e Info.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.46 è pubblicata: [v4.46](https://github.com/Roccobot/AIV/releases/tag/v4.46), con l'APK
[AIV-4.46.apk](https://github.com/Roccobot/AIV/releases/download/v4.46/AIV-4.46.apk).
Commit prodotto su `main`: `93019ea`, release dal commit `93019ea` (SlimVer 4.46 / versionCode 334; APK 8.525.257 byte, digest `e051db5c`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.46**: cinque prove nuove. Giro **4.45**: `4.45-03` OK, `4.45-01` ripresa qui, `4.45-02` rifatta nella 4.47.

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

Le verifiche automatiche della 4.46 sono superate: banco di prova completo (620 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.46-01 | Non provato | | Attendere il collaudo. |
| 4.46-02 | Non provato | | Attendere il collaudo. |
| 4.46-03 | Non provato | | Attendere il collaudo. |
| 4.46-04 | Non provato | | Attendere il collaudo. |
| 4.46-05 | Non provato | | Attendere il collaudo. |

## 1. Dal menu a Info, senza lampeggio

Tieni premuta una miniatura e scegli `Info`. Adesso la finestra del menu che se ne va tiene la sfocatura piena finché la scheda è in scena, invece di farla calare durante i suoi 75 ms. La decisione è misurata dal banco, la sfocatura vera la fa il telefono: se vedi ancora un lampo, dimmi dove, e prova anche da un menu a una conferma.

## 2. La cartella d'origine non si può scegliere

Seleziona qualche immagine in una cartella e tocca `Copia` o `Sposta`. Nell'elenco delle destinazioni la cartella in cui sei è sbiadita e non risponde al tocco. Con `Sfoglia...`, nell'albero, ci puoi entrare (le sue sottocartelle sono destinazioni valide), ma il tasto in fondo che la sceglierebbe è spento: è una mia lettura della tua nota, se la vuoi chiusa anche lì scrivilo qui.

## 3. Gli avvisi centrati sui tasti in basso

Sposta o copia qualche immagine: l'avviso in basso, accanto al tasto tondo (o al FAB), ha il suo centro alla stessa altezza del centro del tasto. Quando sale sopra la scheda della selezione resta com'era.

## 4. L'ombra della selezione

Tieni premuta una miniatura per cominciare una selezione: sopra la scheda in basso la griglia scurisce gradualmente, come nel tuo mockup (dal 49% sul bordo a niente, in circa 120 dp), anche negli spazi fra le miniature. Le spunte e la durata dei video restano sopra, piene. Sul tema scuro l'ombra è bianca, come il velo: è una mia lettura, se la vuoi nera anche lì scrivilo qui.

## 5. La freccia tratteggiata

Nel modulo `Disegno` accendi `Tratteggio` e disegna qualche freccia di lunghezze diverse. Contro la punta l'asta è sempre piena, fino a uscire dall'angolo rientrante delle alette; poi comincia il tratteggio, e il pezzo che avanza cade alla coda. La linea tratteggiata resta com'era.

## Decisioni da concordare

Nessuna decisione aperta in questo giro: le domande sulla luminosità sono in chat. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Dal menu a Info, senza lampeggio | 4.46-01 | Non provato | | Attendere il collaudo. |
| La cartella d'origine | 4.46-02 | Non provato | | Attendere il collaudo. |
| Gli avvisi centrati | 4.46-03 | Non provato | | Attendere il collaudo. |
| L'ombra della selezione | 4.46-04 | Non provato | | Attendere il collaudo. |
| La freccia tratteggiata | 4.46-05 | Non provato | | Attendere il collaudo. |
| Dal menu a Info | 4.45-01 | Non approvato | Lampeggio attenuato ma ancora visibile. | Ripresa in `4.46-01`. |
| Luminosità | 4.45-02 | Non approvato | Niente tasto Luminosità, un tasto Spessore, la luminosità tenendo premuto un tondo. | Rifatta nella 4.47. |
| Il grigio | 4.45-03 | OK | | Concluso. |

## Prossimi passi

- **In collaudo**: il passaggio senza lampeggio (`4.46-01`), la cartella d'origine (`4.46-02`), gli avvisi (`4.46-03`), l'ombra (`4.46-04`), la freccia (`4.46-05`).
- **Prossima versione**: Disegno 4.47: il tasto Spessore al posto di Luminosità, la luminosità tenendo premuto un tondo, e l'aggancio di linee e frecce all'orizzontale e alla verticale con la guida.
- **Dopo**: G2 (selezione, vertici, parametri sull'oggetto, elimina; poi maniglie, sopra e sotto); lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: il grigio (`4.45-03`); il modulo Disegno fino ai tasti disegnati; il velo nuovo.
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
