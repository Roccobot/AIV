# Feedback AIV

Versione **4.40**: il modulo Disegno, prima fase, col riempimento che ha colore e opacità suoi.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.40 è pubblicata: [v4.40](https://github.com/Roccobot/AIV/releases/tag/v4.40), con l'APK
[AIV-4.40.apk](https://github.com/Roccobot/AIV/releases/download/v4.40/AIV-4.40.apk).
Commit prodotto su `main`: `3893687`, release dal commit `3893687` (SlimVer 4.40 / versionCode 328; APK 8.506.557 byte, digest `4fee9457`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.40**: tre prove nuove. Giro **4.38**: `4.38-01` e `4.38-02` tutte OK.

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

Le verifiche automatiche della 4.40 sono superate: banco di prova completo (601 prove), controllo delle traduzioni e delle icone, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.40-01 | Non provato | | Attendere il collaudo. |
| 4.40-02 | Non provato | | Attendere il collaudo. |
| 4.40-03 | Non provato | | Attendere il collaudo. |

## 1. Il modulo Disegno: rettangolo e freccia

Apri un'immagine, tocca `Modifica` e scegli l'editor completo. Scorri la fila dei moduli fino in fondo a destra e tocca `Disegno`, la matita con lo scarabocchio. Tocca la penna del rettangolo, poi trascina sull'immagine da un angolo all'angolo opposto: il rettangolo arrotondato segue il dito, e i due capi del trascinamento sono i due vertici. Tocca la penna della freccia e trascina dall'inizio alla punta. Con due dita, nel modulo, ingrandisci e sposti l'immagine come negli altri moduli. Le altre tre penne sono mano libera, linea ed ellisse. L'arrotondamento degli angoli è fisso (tua risposta B1) e lo spessore resta un cursore continuo (C1).

## 2. Il riempimento con colore e opacità suoi

Con la penna del rettangolo o dell'ellisse, sotto le penne tocca `Contorno` e scegli un colore, per esempio il rosso; poi tocca `Riempimento` e scegli il bianco: il cursore sotto diventa `Opacità`, che parte dal 50%. Disegna un rettangolo: nasce col bordo rosso e il riempimento bianco al 50%, come nel tuo esempio. Il primo cerchio della fila del riempimento, con la diagonale, è `Nessuno`, che è il valore di fabbrica. La freccia, la linea e la mano libera ignorano il riempimento, e per loro il gettone `Riempimento` è spento. `Tratteggio` vale per tutte le penne, e la punta della freccia resta piena.

## 3. Il disegno nel file salvato, e Annulla

Disegna due o tre segni, poi tocca `Annulla`: sparisce l'ultimo segno, e solo quello. `Azzera`, accanto a `Spessore` o `Opacità`, toglie tutto il disegno in un passo. Prova a girare l'immagine (modulo `Ritaglio`) o a raddrizzarla (`Geometria`) dopo aver disegnato: il disegno gira e si inclina con lei. Salva e riapri il file: il disegno è dentro l'immagine, a piena risoluzione, nello stesso punto in cui lo vedevi, e non si modifica più come forma. Il modulo esiste solo nell'editor completo, quindi da Android 13 in su.

## Decisioni da concordare

Nessuna decisione aperta in questo giro: B1 (angoli fissi) e C1 (spessore continuo) le hai date in chat. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il modulo Disegno: rettangolo e freccia | 4.40-01 | Non provato | | Attendere il collaudo. |
| Il riempimento con colore e opacità suoi | 4.40-02 | Non provato | | Attendere il collaudo. |
| Il disegno nel file salvato, e Annulla | 4.40-03 | Non provato | | Attendere il collaudo. |
| I pannelli nudi | 4.38-01 | OK | Tutto OK. | Archiviata. |
| Le frecce su e giù ancora un poco più spesse | 4.38-02 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: il modulo Disegno, prima fase (`4.40-01`-`4.40-03`).
- **Prossima versione**: Disegno G2 (4.50): un tocco seleziona un oggetto, il rettangolo mostra quattro vertici color accento e la freccia due punti, e i parametri cambiano l'oggetto scelto; poi spostare, eliminare, sopra e sotto, ridimensionare e ruotare con le maniglie.
- **Dopo**: Disegno G3 (4.60), il testo.
- **Concluso**: pannelli nudi e frecce più spesse (`4.38-01`, `4.38-02`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
