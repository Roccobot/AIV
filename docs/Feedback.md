# Feedback AIV

Versione **5.91**: il titolino `Disposizione delle cartelle` nelle opzioni di visualizzazione, come l'hai riveduto nel giro 5.90.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.91 è pubblicata: [v5.91](https://github.com/Roccobot/AIV/releases/tag/v5.91), con i due APK
[AIV-5.91.apk](https://github.com/Roccobot/AIV/releases/download/v5.91/AIV-5.91.apk) (AIV GitHub) e [AIV-Play-5.91.apk](https://github.com/Roccobot/AIV/releases/download/v5.91/AIV-Play-5.91.apk) (AIV Play), più [AIV-Play-5.91.aab](https://github.com/Roccobot/AIV/releases/download/v5.91/AIV-Play-5.91.aab), il pacchetto da caricare su Google Play.
Commit prodotto su `main`: `9518165`, release dal commit `9518165` (SlimVer 5.91 / versionCode 375; APK GitHub 11.125.113 byte, digest `948e53b0`; APK Play 11.014.701 byte, digest `06a4778b`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.91**: il titolino riveduto, una prova e un'etichetta. Giro **5.90**: le due prove sono approvate, la spiegazione delle preferite è confermata, e l'icona e l'immagine in evidenza per Google Play sono approvate (H1).

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

Le verifiche automatiche della 5.91 sono superate: banco di prova completo di AIV GitHub (731 prove), prove di AIV Play (43), controllo delle traduzioni, compilazione delle due varianti e del pacchetto per Google Play, con la firma verificata.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.91-01 | Non provato | | Attendere il collaudo. |

## 1. Il titolino della disposizione

Vale per AIV GitHub e per AIV Play. In griglia tieni premuto il pulsante principale: sotto `Dimensione della griglia` il titolino adesso dice `Disposizione delle cartelle`, su una riga sola, sopra i gettoni `Alfabetica` e `Più recenti in alto`.

Letture mie: è la sola modifica di questa versione, quindi la 5.91 sale di un centesimo (la regola nuova su SlimVer). Nelle lingue che scrivono lungo il titolino può andare a capo su due righe: è il comportamento di tutti i titolini di quella finestra.

## Etichette testuali

### e-folder_order_short · Il titolino della disposizione, nelle opzioni di visualizzazione, riveduto da te
Disposizione delle cartelle

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il titolino della disposizione | 5.90 | Etichetta riveduta | `Disposizione delle cartelle` al posto di `Disposizione`. | Fatta nella 5.91 (`5.91-01`). |
| Le opzioni di visualizzazione | 5.90-02 | Tutto OK | | Chiusa. |
| La pagina delle cartelle preferite | 5.90-01 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo, giro 5.91**: il titolino (`5.91-01`) e la sua etichetta.
- **Tuoi, in parallelo**: l'account sviluppatore su Google Play, la creazione dell'app, i 12 tester per i 14 giorni del test chiuso e le schermate del telefono per la scheda (risposta G1); icona e immagine in evidenza sono pronte in `docs/store/`, e i passi in ordine sono in fondo a `docs/Store.md`.
- **AIV Play, dopo**: le altre funzioni sui file rifatte con MediaStore, una per rilascio (copia, sposta, duplica, nuova cartella, `Salva` sull'originale).
- **AIV GitHub, prima del 2027**: la registrazione nella verifica degli sviluppatori di Android, dall'account di Google Play.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
