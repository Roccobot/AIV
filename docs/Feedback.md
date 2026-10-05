# Feedback AIV

Versione **4.25**: il menu angolare col tondo a riposo e il commutatore, la pillola più corta, il traslucido di fabbrica con i due colori, e le due sfumature in basso.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.25 è pubblicata: [v4.25](https://github.com/Roccobot/AIV/releases/tag/v4.25), con l'APK
[AIV-4.25.apk](https://github.com/Roccobot/AIV/releases/download/v4.25/AIV-4.25.apk).
Commit prodotto su `main`: `32af491` (SlimVer 4.25 / versionCode 318).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.25**: sette prove, dalle tue note del giro 4.20 e dalle scelte A2, B3, C2. Giro **4.20**: `4.20-02`-`4.20-04` OK; `4.20-01` e `4.20-05` fatte nella 4.25.

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

Le verifiche automatiche della 4.25 sono superate: banco di prova completo (567 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.25-01 | Non provato | | Attendere il collaudo. |
| 4.25-02 | Non provato | | Attendere il collaudo. |
| 4.25-03 | Non provato | | Attendere il collaudo. |
| 4.25-04 | Non provato | | Attendere il collaudo. |
| 4.25-05 | Non provato | | Attendere il collaudo. |
| 4.25-06 | Non provato | | Attendere il collaudo. |
| 4.25-07 | Non provato | | Attendere il collaudo. |

## 1. Il menu angolare a riposo, e il commutatore

Con **Elemento interattivo principale** → `Menu angolare`, a riposo c'è un tasto tondo solo, col marchio. Scorri una cartella o la schermata iniziale: il tondo si allunga verso l'alto nella pillola verticale, con `in cima` sopra e `in fondo` al posto del marchio, e torna tondo quando il salto se ne va. Aperto nella schermata iniziale, la prima riga ha il commutatore e le due viste diverse da quella in cui sei: il commutatore mostra il riposo che il tocco mette (la capsula quando a riposo c'è il tondo, il tondo quando c'è la pillola). Toccalo: il menu si chiude e a riposo trovi la pillola verticale di due tasti, com'era nella 4.20; toccalo di nuovo per tornare al tondo. La scelta resta. Le tue due icone sono uniformate alle altre: la capsula alta 20 unità su 24, come gli altri glifi, e il tondo scalato dello stesso fattore, quindi largo quanto la capsula.

## 2. Il menu angolare si chiude a ogni tocco

Apri il menu angolare e tocca una voce qualunque, il commutatore o una vista compresi: il menu si richiude prima di agire, anche nelle cartelle (il 2×2).

## 3. La pillola più corta

Con la pillola (`A scomparsa` o `Estesa`) il tondo e la pillola stanno a 24dp dal bordo dello schermo, cioè 8dp dentro i bordi delle miniature, come nella variante A2 dell'anteprima. Nella schermata iniziale apri la pillola a scomparsa: con otto tasti arriva a 24dp da tutti e due i bordi, e la × è dove era il tondo. Vale anche nelle cartelle, nel menu angolare e nella pillola verticale del menu inferiore, e il menu inferiore fisso con pochi tasti li raccoglie a 24dp. Il FAB resta dov'era.

## 4. Il traslucido di fabbrica, e lo Scostamento

Da questa versione, su un telefono che non ha mai toccato le impostazioni, il metodo predefinito è la pillola a scomparsa col traslucido, con i tuoi valori: Raggio 16, Intensità 300%, Opacità 20%, Scostamento 35. Il cursore che si chiamava Colore si chiama **Opacità**, e Luminosità è diventato **Scostamento**, da 0 a 50 e senza segno: scurisce nel tema chiaro e schiarisce nel tema scuro. Tu hai già l'app e i tuoi valori della 4.20 restano: in **Tema e dettagli grafici**, sotto i cursori, tocca `Ripristina` per avere quelli di fabbrica. Poi passa dal tema chiaro a quello scuro e guarda le pillole.

## 5. I due colori del vetro

Sotto i cursori ci sono **Colore chiaro** e **Colore scuro**, ognuno con un tondo del suo colore. Toccane uno: si apre un selettore con tonalità, saturazione e luminanza, e il colore che ne risulta in cima. `Applica` lo salva, `Predefinito` torna all'accento che il vetro ha in quel tema, `Annulla` (o un tocco fuori) chiude senza cambiare niente. L'anteprima e le pillole usano il colore del tema in cui sei. `Ripristina` toglie anche i due colori.

## 6. Il bordo dell'anteprima

L'anteprima del traslucido ha un bordo d'accento di 5dp, dentro l'immagine e sopra le righe, che copre i bordi seghettati.

## 7. Le due sfumature in basso

Nella schermata iniziale e in cima a ogni cartella ci sono tutte e due le sfumature in basso, la più ampia e la più corta. Scorri una cartella: la più ampia se ne va insieme al titolo, la più corta resta. Arriva in fondo alla cartella: sparisce anche la più corta, così l'ultima riga si vede intera; risalendo torna. In una cartella che entra tutta nello schermo restano le due.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il menu angolare a riposo, e il commutatore | 4.25-01 | Non provato | | Attendere il collaudo. |
| Il menu angolare si chiude a ogni tocco | 4.25-02 | Non provato | | Attendere il collaudo. |
| La pillola più corta | 4.25-03 | Non provato | | Attendere il collaudo. |
| Il traslucido di fabbrica, e lo Scostamento | 4.25-04 | Non provato | | Attendere il collaudo. |
| I due colori del vetro | 4.25-05 | Non provato | | Attendere il collaudo. |
| Il bordo dell'anteprima | 4.25-06 | Non provato | | Attendere il collaudo. |
| Le due sfumature in basso | 4.25-07 | Non provato | | Attendere il collaudo. |
| Il menu angolare nella schermata iniziale | 4.20-01 | Accettabile | Molto valido, con quattro ritocchi. | Fatto nella 4.25 (`4.25-01`, `4.25-02`). |
| Il menu angolare nelle cartelle, e i salti | 4.20-02 | OK | Tutto OK. | Archiviata. |
| Le due viste a specchio | 4.20-03 | OK | Tutto OK. | Archiviata. |
| Il menu inferiore con pochi tasti | 4.20-04 | OK | Tutto OK. | Archiviata. |
| I quattro cursori del traslucido, con l'anteprima | 4.20-05 | OK | Tutto OK, con la proposta dei due colori. | Fatto nella 4.25 (`4.25-04`, `4.25-05`). |

## Prossimi passi

- **In collaudo**: menu angolare a riposo e commutatore, chiusura a ogni tocco, pillola più corta, traslucido di fabbrica, due colori, bordo dell'anteprima, sfumature (`4.25-01`-`4.25-07`).
- **Concluso**: menu angolare nelle cartelle, viste a specchio, menu inferiore con pochi tasti (`4.20-02`-`4.20-04`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
