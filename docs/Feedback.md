# Feedback AIV

Versione **3.52**: collaudo di Correggi/Rimuovi su più scale (aree più grandi, più contesto).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 3.52 è pubblicata: [v3.52](https://github.com/Roccobot/AIV/releases/tag/v3.52), con l'APK
[AIV-3.52.apk](https://github.com/Roccobot/AIV/releases/download/v3.52/AIV-3.52.apk).
Commit prodotto su `main`: `9819ed9` (SlimVer 3.52 / versionCode 301).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. L'inpaint `3.40-05` era chiuso così com'era: la 3.52 lo riapre su tua richiesta del 3 ottobre (aree più grandi, più contesto), sempre senza timbro clone.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.52 sono superate: banco di prova completo, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono resta tuo.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.52-01 | Non provato | | Attendere il collaudo. |
| 3.52-02 | Non provato | | Attendere il collaudo. |

## 1. Correggi/Rimuovi: aree grandi su una foto vera

Con AIV **3.52**, apri una **foto del telefono ad alta risoluzione** (12 MP o più) nell'**editor completo**, modulo **Dettaglio**, e tocca **Correggi/Rimuovi**.

Porta **Dimensione pennello** al massimo e dipingi sopra un oggetto da togliere (un cartello, una persona lontana, una macchia grande), poi tocca **Applica**. Fino alla 3.42 qui compariva l'avviso di selezione troppo grande: adesso la correzione deve avvenire.

Guarda se il riempimento continua le linee e le trame intorno e se l'attesa ti sembra accettabile. Se puoi, allega il prima e il dopo. Limite noto: una trama morbida come le nuvole può diventare cielo liscio.

## 2. Correggi/Rimuovi: bordi netti e trame regolari

Sempre in **Correggi/Rimuovi**, con un pennello piccolo o medio, togli un difetto che sta **sopra un bordo netto** (un orizzonte, lo spigolo di un muro) e uno sopra una **trama regolare** (mattoni, piastrelle, una griglia).

Lungo il bordo non devono comparire puntini scuri né trattini chiari, e le fughe della trama devono restare allineate. Rifai anche la tua prova della diagonale: deve restare pulita come prima.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Riordino Impostazioni | 3.41-02 | OK | Tutto OK. | Archiviata. |
| Rail tablet | 3.42-01 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: Correggi/Rimuovi su più scale, `3.52-01` e `3.52-02`.
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
