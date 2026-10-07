# Feedback AIV

Versione **4.49**: nel modulo Disegno Spessore prima di Riempimento, e il cursore della luminosità sopra i tondi.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.49 è pubblicata: [v4.49](https://github.com/Roccobot/AIV/releases/tag/v4.49), con l'APK
[AIV-4.49.apk](https://github.com/Roccobot/AIV/releases/download/v4.49/AIV-4.49.apk).
Commit prodotto su `main`: `ac6df7a`, release dal commit `ac6df7a` (SlimVer 4.49 / versionCode 337; APK 8.525.257 byte, digest `34c15703`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.49**: due prove nuove. Giro **4.48**: quattro prove OK, `4.47-03` ripresa in `4.49-02`.

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

Le verifiche automatiche della 4.49 sono superate: banco di prova completo (628 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.49-01 | Non provato | | Attendere il collaudo. |
| 4.49-02 | Non provato | | Attendere il collaudo. |

## 1. Spessore prima di Riempimento

Nel modulo `Disegno` i tasti sotto gli strumenti sono adesso `Tratteggio`, `Traccia`, `Spessore`, `Riempimento`, da sinistra, come hai chiesto su `4.47-01`.

## 2. Il cursore della luminosità sopra i tondi

Tieni premuto un tondo: il cursore della luminosità compare sopra la fila dei tondi, dove il dito non lo copre, ed è più corto della fila, staccato di 24 dp da ciascun lato. Il resto funziona come nella 4.47: scorrendo senza staccare il dito il colore si applica allo stacco, alzando il dito fermo il cursore resta e lo chiudi toccando fuori.

## Decisioni da concordare

Nessuna decisione aperta in questo giro.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Spessore prima di Riempimento | 4.49-01 | Non provato | | Attendere il collaudo. |
| Il cursore della luminosità sopra i tondi | 4.49-02 | Non provato | | Attendere il collaudo. |
| Il tasto Spessore | 4.47-01 | OK | Spessore a sinistra di Riempimento. | Fatto in `4.49-01`. |
| La linea trasparente | 4.47-02 | OK | | Concluso. |
| La luminosità sui tondi | 4.47-03 | Accettabile | Il cursore sopra i tondi, più corto e staccato dai lati. | Ripresa in `4.49-02`. |
| Linee e frecce dritte | 4.47-04 | OK | | Concluso. |
| L'ombra della selezione | 4.48-01 | OK | Meravigliosa. | Concluso. |

## Prossimi passi

- **In collaudo**: Spessore prima di Riempimento (`4.49-01`), il cursore della luminosità sopra i tondi (`4.49-02`).
- **Dopo**: G2 (selezione, vertici, parametri sull'oggetto, elimina; poi maniglie, sopra e sotto); lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: la linea trasparente, le linee dritte (`4.47-02`, `4.47-04`); l'ombra della selezione (`4.48-01`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
