# Feedback AIV

Versione **4.43**: nel modulo Disegno i tasti Contorno, Riempimento e Tratteggio sono disegni.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.43 è pubblicata: [v4.43](https://github.com/Roccobot/AIV/releases/tag/v4.43), con l'APK
[AIV-4.43.apk](https://github.com/Roccobot/AIV/releases/download/v4.43/AIV-4.43.apk).
Commit prodotto su `main`: `7592a78`, release dal commit `7592a78` (SlimVer 4.43 / versionCode 331; APK 8.509.125 byte, digest `14347203`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.43**: una prova nuova. Giro **4.42**: `4.42-01` OK.

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

Le verifiche automatiche della 4.43 sono superate: banco di prova completo (607 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.43-01 | Non provato | | Attendere il collaudo. |

## 1. I tasti del Disegno sono disegni

Apri il modulo `Disegno`. Sopra la tavolozza i tre tasti non hanno più la parola:
- **Contorno** è una linea spessa del colore della linea: scegli un altro colore e cambia anche lei.
- **Riempimento** è un rettangolo arrotondato del colore del riempimento, senza contorno, sopra una scacchiera. L'opacità è alzata in proporzione e non scende mai sotto il 40%: il salmone al 15% di fabbrica si vede a circa il 49%, e un riempimento pieno resta pieno. Scegli `Nessuno`: resta la scacchiera vuota con la diagonale rossa, come il tondo `Nessuno` della tavolozza.
- **Tratteggio** è una linea spessa tratteggiata grigio scuro, e il tasto si accende e si spegne.

Il lettore di schermo dice ancora le tre parole, e la prima resta `Contorno`. Due scelte sono mie: attorno alla linea di Contorno c'è il filo sottile dei tondi della tavolozza, perché il nero sul tema scuro e il bianco sul tema chiaro si vedano; e la formula dell'opacità mostrata è 40% + 60% di quella vera. Se una delle due non ti va, scrivilo qui.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| I tasti del Disegno sono disegni | 4.43-01 | Non provato | | Attendere il collaudo. |
| I valori di fabbrica del Disegno | 4.42-01 | OK | Tutto OK, con due richieste: i colori attuali in chat per ritoccare la tavolozza, e i tre tasti grafici. | Tasti in `4.43-01`; i colori sono in chat, la tavolozza aspetta i tuoi valori. |
| La punta vera mentre tieni Spessore | 4.41-01 | OK | Tutto OK, con una richiesta sui valori di fabbrica, chiarita in chat. | Ripresa in `4.42-01`. |

## Prossimi passi

- **In collaudo**: i tasti del Disegno sono disegni (`4.43-01`).
- **Aspetta te**: i colori nuovi della tavolozza; entrano nella versione dopo la tua risposta.
- **Prossima versione**: Disegno G2 (4.50): un tocco seleziona un oggetto, il rettangolo mostra quattro vertici color accento e la freccia due punti, e i parametri cambiano l'oggetto scelto; poi spostare, eliminare, sopra e sotto, ridimensionare e ruotare con le maniglie.
- **Dopo**: Disegno G3 (4.60), il testo.
- **Concluso**: il modulo Disegno, prima fase (`4.40-01`-`4.40-03`), la punta vera di `Spessore`, la lente sulle forme piccole e i valori di fabbrica (`4.41-01`, `4.41-02`, `4.42-01`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
