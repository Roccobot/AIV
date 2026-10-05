# Feedback AIV

Versione **4.03**: il marchio centrato nel tasto tondo, le info col tema dello sfondo, i gettoni al maschile.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.03 è pubblicata: [v4.03](https://github.com/Roccobot/AIV/releases/tag/v4.03), con l'APK
[AIV-4.03.apk](https://github.com/Roccobot/AIV/releases/download/v4.03/AIV-4.03.apk).
Commit prodotto su `main`: `94a1ff0` (SlimVer 4.03 / versionCode 312).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.03**: tre prove nuove, dalle tue risposte al giro 4.02. Giro **4.02**: `4.02-01` e `4.02-02` OK, la `4.02-03` non approvata e rifatta nella `4.03-01`.

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

Le verifiche automatiche della 4.03 sono superate: banco di prova completo (547 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.03-01 | Non provato | | Attendere il collaudo. |
| 4.03-02 | Non provato | | Attendere il collaudo. |
| 4.03-03 | Non provato | | Attendere il collaudo. |

## 1. Il marchio nel tasto tondo, questa volta davvero

Avevi ragione: nella 4.02 sul telefono non era cambiato niente. Ho misurato la tua schermata, e il marchio era ancora quello del FAB quadrato, largo 24dp e spostato a destra. Il tasto tondo della pillola lo disegna un'altra parte del codice, che non diceva al marchio di essere nel tondo, e la prova della 4.02 controllava una copia invece del tasto vero. Con AIV **4.03**, pillola accesa su `A scomparsa`: il marchio è più piccolo dell'8% (22dp) e la **A** è centrata nel tondo su tutti e due gli assi, col disco solare spostato di conseguenza. La prova adesso guarda il tasto vero.

## 2. Le info col tema dello sfondo

Apri un'immagine con la barra delle info visibile, poi vai in Impostazioni e scegli `Tema dello sfondo` su `Scuro` con l'app in tema chiaro: la barra delle info diventa scura insieme allo sfondo. Con `Chiaro` diventa chiara anche se l'app è scura, e con `Automatico` segue il tema dell'app. Sul tablet vale anche il pannello laterale delle info, che ho letto come parte della stessa richiesta: dimmi se lo volevi com'era.

## 3. I gettoni al maschile

In Impostazioni, `Tema dello sfondo` mostra adesso `Automatico` · `Chiaro` · `Scuro`.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il marchio nel tasto tondo | 4.03-01 | Non provato | | Attendere il collaudo. |
| Le info col tema dello sfondo | 4.03-02 | Non provato | | Attendere il collaudo. |
| I gettoni al maschile | 4.03-03 | Non provato | | Attendere il collaudo. |
| Il marchio nel tasto tondo | 4.02-03 | Non approvato | Triangolo ancora troppo a destra. | Rifatta nella 4.03-01. |
| Interruttore della pillola, Tema e dettagli grafici | 4.02-01 | OK | Tutto OK, anche 4.02-02. | Archiviate. |

## Prossimi passi

- **In collaudo**: marchio nel tasto tondo, info col tema dello sfondo, gettoni al maschile (`4.03-01`-`4.03-03`).
- **Concluso**: interruttore della pillola e nome della pagina (`4.02-01`, `4.02-02`); i quattro ritocchi del DF sono online.
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
