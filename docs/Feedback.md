# Feedback AIV

Versione **4.20**: il menu angolare, le viste a specchio, il menu inferiore con pochi tasti sul lato preferito e i quattro cursori del traslucido.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.20 è pubblicata: [v4.20](https://github.com/Roccobot/AIV/releases/tag/v4.20), con l'APK
[AIV-4.20.apk](https://github.com/Roccobot/AIV/releases/download/v4.20/AIV-4.20.apk).
Commit prodotto su `main`: `bc6e638` (SlimVer 4.20 / versionCode 317).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.20**: cinque prove, dalle tue note del giro 4.15 e dalle decisioni sul menu angolare. Giro **4.15**: `4.15-01`-`4.15-06` OK.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 4.20 sono superate: banco di prova completo (563 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.20-01 | Non provato | | Attendere il collaudo. |
| 4.20-02 | Non provato | | Attendere il collaudo. |
| 4.20-03 | Non provato | | Attendere il collaudo. |
| 4.20-04 | Non provato | | Attendere il collaudo. |
| 4.20-05 | Non provato | | Attendere il collaudo. |

## 1. Il menu angolare nella schermata iniziale

Con AIV **4.20**, sul telefono in verticale, scegli **Elemento interattivo principale** → `Menu angolare`: non c'è una seconda fila, perché è sempre a scomparsa. A riposo c'è la pillola verticale del menu inferiore; tocca il marchio e dall'angolo cresce un pannello 3×3: in alto le tre viste, con quella in cui sei segnata da un disco; poi Mostra nascoste (spenta se non hai cartelle nascoste), Cerca e Apri un indirizzo; in basso Cestino, Impostazioni e la ×, che prende il posto del marchio. Il pannello si ferma sopra la linea dei gesti. Il paragrafo della voce adesso nomina anche il menu angolare.

## 2. Il menu angolare nelle cartelle, e i salti

In una cartella il pannello è un 2×2: Cerca e Cestino sopra, Impostazioni e la × sotto (nel cestino le sue tre voci). Scorrendo, la × e la casella sopra di lei diventano `in fondo` e `in cima`. Col lato preferito a sinistra ogni riga è a specchio, con la × nell'angolo di sinistra.

## 3. Le due viste a specchio

Col lato preferito a sinistra, nella pillola e nel menu inferiore anche le due viste sono a specchio, come hai chiesto: l'ordine è il rovescio esatto di quello di destra.

## 4. Il menu inferiore con pochi tasti

Col `Menu inferiore` in una cartella o nel cestino (tre voci, quattro con la × a scomparsa) i tasti si raccolgono sul lato preferito con la spaziatura della pillola, e il tasto d'angolo cade dove era il marchio della pillola verticale. Da cinque tasti in su, come nella schermata iniziale, restano distribuiti su tutta la larghezza.

## 5. I quattro cursori del traslucido, con l'anteprima

In **Tema e dettagli grafici** scegli `Traslucido`: sotto compaiono un'anteprima e quattro cursori, e l'anteprima cambia mentre muovi il dito. Come li ho letti, da correggere se intendevi altro: **Raggio** è quanto sfoca (13 di fabbrica, il valore di prima); **Intensità** è quanto si accendono i colori dietro il vetro (160%); **Colore** è quanto accento ha il vetro rispetto al tono del tema (100%); **Luminosità** schiarisce verso il bianco sopra lo zero e scurisce verso il nero sotto. Il valore si salva quando alzi il dito. Per ora il fondo dell'anteprima è a righe colorate: l'immagine fissa che mi mandi entra al prossimo giro. Quando avrai trovato lo stile, i cursori si nascondono con un interruttore nel codice e i valori restano.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il menu angolare nella schermata iniziale | 4.20-01 | Non provato | | Attendere il collaudo. |
| Il menu angolare nelle cartelle, e i salti | 4.20-02 | Non provato | | Attendere il collaudo. |
| Le due viste a specchio | 4.20-03 | Non provato | | Attendere il collaudo. |
| Il menu inferiore con pochi tasti | 4.20-04 | Non provato | | Attendere il collaudo. |
| I quattro cursori del traslucido, con l'anteprima | 4.20-05 | Non provato | | Attendere il collaudo. |
| Il menu inferiore a scomparsa | 4.15-01 | OK | Tutto OK. | Archiviata. |
| Il menu inferiore fisso, sopra la griglia | 4.15-02 | OK | Tutto OK. | Archiviata. |
| I salti nei due tasti dell'angolo | 4.15-03 | OK | Tutto OK. | Archiviata. |
| L'ordine col lato preferito a sinistra | 4.15-04 | OK | Tutto OK, con le due viste da mettere a specchio. | Fatto nella 4.20 (`4.20-03`). |
| La pillola estesa a sette voci | 4.15-05 | OK | Tutto OK. | Archiviata. |
| I testi delle due voci | 4.15-06 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: menu angolare nella home e nelle cartelle, viste a specchio, menu inferiore con pochi tasti, cursori del traslucido (`4.20-01`-`4.20-05`).
- **In attesa di te**: l'immagine fissa in WebP per l'anteprima del traslucido.
- **Poi**: i valori del traslucido che sceglierai diventano quelli di fabbrica, e i cursori si nascondono.
- **Concluso**: menu inferiore, salti, ordine a sinistra, pillola estesa a sette voci, testi (`4.15-01`-`4.15-06`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
