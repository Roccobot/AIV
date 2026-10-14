# Feedback AIV

Versione **5.92**: una vibrazione sola, quella piena, su ogni pressione lunga (la tua scelta B1).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.92 è pubblicata: [v5.92](https://github.com/Roccobot/AIV/releases/tag/v5.92), con i due APK
[AIV-5.92.apk](https://github.com/Roccobot/AIV/releases/download/v5.92/AIV-5.92.apk) (AIV GitHub) e [AIV-Play-5.92.apk](https://github.com/Roccobot/AIV/releases/download/v5.92/AIV-Play-5.92.apk) (AIV Play), più [AIV-Play-5.92.aab](https://github.com/Roccobot/AIV/releases/download/v5.92/AIV-Play-5.92.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `6ebb8a7`, release dal commit `6ebb8a7` (SlimVer 5.92 / versionCode 376; APK GitHub 11.125.113 byte, digest `ff8fac3f`; APK Play 11.014.701 byte, digest `e68ccffc`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.92**: la vibrazione, una prova. Giro **5.91**: il titolino `Disposizione delle cartelle` è approvato e registrato.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale, coi conteggi in cima. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, salvo in un documento cumulativo, dove restano quelli del giro che prosegue, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 5.92 sono superate: banco di prova completo di AIV GitHub (734 prove), prove di AIV Play (43), compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata. La prova nuova `VibrazioneTest` conta le vibrazioni di un gesto, e l'ho vista fallire prima della correzione: due vibrazioni su una voce di menu, nessuna sul comando `Data`.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.92-01 | Non provato | | Attendere il collaudo. |

## 1. Una vibrazione sola, piena, su ogni pressione lunga

Vale per AIV GitHub e per AIV Play. Prova la pressione lunga in quattro punti e confrontale fra loro: una miniatura in una cartella (inizia la selezione), una cartella nella griglia delle cartelle, `Elimina` del Disegno nell'editor completo (tenuto premuto fa `Elimina tutto`), e `Data` nella finestra di `Rinomina`. Devono vibrare tutte allo stesso modo, una volta sola, con la vibrazione piena del sistema.

Letture mie: è la tua scelta B1. Fino alla 5.91 la maggior parte dei tocchi lunghi vibrava due volte (il colpetto leggero di AIV più quello pieno che Compose, la libreria grafica dell'app, aggiunge da sé), e l'inizio della selezione solo col colpetto leggero: è la differenza che avevi sentito. Adesso quella di Compose è spenta ovunque e resta quella di AIV, piena. L'intensità dipende anche dalle impostazioni di vibrazione di HyperOS. Se ti sembra troppo forte resta la via B3, piena solo all'inizio della selezione. La versione sale di un centesimo: cambia una funzione che esiste già.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La vibrazione della pressione lunga | 5.92 | Scelta B1 | Il tocco che apre la selezione era *leggerissimo, diverso da altre vibrazioni*. | Fatta nella 5.92 (`5.92-01`). |
| Il titolino della disposizione | 5.91-01 | Tutto OK | | Chiusa, etichetta approvata. |

## Prossimi passi

- **In collaudo, giro 5.92**: la vibrazione (`5.92-01`).
- **Fermi fino al tuo via**: la vista globale delle immagini senza cartelle, in tutte e due le varianti (un unico flusso, le più recenti in cima), poi le funzioni sui file di AIV Play rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale). Prima della prossima release si leggono le 11 dipendenze più nuove.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app, i 12 tester per i 14 giorni del test chiuso e le schermate del telefono per la scheda (risposta G1); icona e immagine in evidenza sono pronte in `docs/store/`, e i passi in ordine sono in fondo a `docs/Store.md`.
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
