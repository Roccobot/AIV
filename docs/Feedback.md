# Feedback AIV

Versione **4.44**: nel modulo Disegno `Traccia`, i tasti ridisegnati e la tua tavolozza; dietro i pannelli il velo nuovo.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.44 è pubblicata: [v4.44](https://github.com/Roccobot/AIV/releases/tag/v4.44), con l'APK
[AIV-4.44.apk](https://github.com/Roccobot/AIV/releases/download/v4.44/AIV-4.44.apk).
Commit prodotto su `main`: `1d7a809`, release dal commit `1d7a809` (SlimVer 4.44 / versionCode 332; APK 8.506.689 byte, digest `ccf42ad2`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.44**: due prove nuove. Giro **4.43**: `4.43-01` accettabile, ripresa in `4.44-01`.

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

Le verifiche automatiche della 4.44 sono superate: banco di prova completo (611 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.44-01 | Non provato | | Attendere il collaudo. |
| 4.44-02 | Non provato | | Attendere il collaudo. |

## 1. Traccia, i tasti e la tua tavolozza

Apri il modulo `Disegno`:
- sopra la tavolozza i tasti sono, da sinistra, **Tratteggio**, **Traccia** (il vecchio Contorno) e **Riempimento**, allineati con gli strumenti di disegno; la quarta colonna è il posto di Luminosità, che arriva con la 4.45;
- Traccia e Tratteggio sono una banda dello stesso spessore che arriva ai bordi del tasto, senza filo: piena del colore della linea, tratteggiata in grigio scuro;
- Riempimento è un rettangolo con la forma del tasto, staccato da un filetto, sopra la scacchiera;
- la tavolozza è la tua, con bianco e nero per ultimi, e all'apertura risultano scelti il rosso per la traccia e l'ambra per il riempimento (al 20%).

Il lettore di schermo dice `Traccia`, `Tratteggio` e `Riempimento`. Il colore `#CC6898` si chiama `Rosa`.

## 2. Il velo dietro i pannelli

Apri un menu, un dialogo o le info di un file, prima sul tema chiaro e poi sullo scuro. Il pannello compare veloce; dietro, lo sfondo si sfoca come di fabbrica e un velo sale in 800 ms, veloce all'inizio e sfumato alla fine: **nero al 30% sul tema chiaro, bianco al 30% sul tema scuro**. Quando il pannello se ne va, il velo sparisce con lui. Vale anche con i pulsanti in vetro, che non cambiano più velo e sfocatura.

Una lettura è mia: nella tua nota il velo bianco era scritto per il 'tema chiaro', e l'ho messo sul tema **scuro**. Se intendevi altro, scrivilo qui.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Traccia, i tasti e la tua tavolozza | 4.44-01 | Non provato | | Attendere il collaudo. |
| Il velo dietro i pannelli | 4.44-02 | Non provato | | Attendere il collaudo. |
| I tasti del Disegno sono disegni | 4.43-01 | Accettabile | Ordine, linee da bordo a bordo, Riempimento più grande, tavolozza nuova; Luminosità proposta. | Ripresa in `4.44-01`; Luminosità nella 4.45. |
| I valori di fabbrica del Disegno | 4.42-01 | OK | Tutto OK, con due richieste: i colori in chat e i tre tasti grafici. | Ripresa in `4.43-01` e `4.44-01`. |

## Prossimi passi

- **In collaudo**: Traccia, tasti e tavolozza (`4.44-01`); il velo dietro i pannelli (`4.44-02`).
- **Prossima versione**: 4.45, Luminosità: un cursore che schiarisce o scurisce il colore del tasto scelto, senza arrivare al bianco o al nero.
- **Dopo**: Disegno G2 (selezione, maniglie, sopra e sotto); lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: il modulo Disegno, prima fase (`4.40-01`-`4.40-03`), la punta vera di `Spessore`, la lente e i valori di fabbrica (`4.41-01`, `4.41-02`, `4.42-01`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
