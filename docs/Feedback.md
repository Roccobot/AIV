# Feedback AIV

Versione **4.64**: nel modulo Disegno il comando si chiama `Elimina tutto`, e le linee dei terzi di `Raddrizza` hanno l'aspetto delle guide del Disegno.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.64 è pubblicata: [v4.64](https://github.com/Roccobot/AIV/releases/tag/v4.64), con l'APK
[AIV-4.64.apk](https://github.com/Roccobot/AIV/releases/download/v4.64/AIV-4.64.apk).
Commit prodotto su `main`: `b3f6671`, release dal commit `b3f6671` (SlimVer 4.64 / versionCode 343; APK 8.558.441 byte, digest `4956dfa0`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.64**: due prove nuove. Giro **4.63**: una prova OK.

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

Le verifiche automatiche della 4.64 sono superate: banco di prova completo (646 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.64-01 | Non provato | | Attendere il collaudo. |
| 4.64-02 | Non provato | | Attendere il collaudo. |

## 1. `Elimina tutto`, e la pressione lunga sul Disegno

Nel modulo Disegno disegna due elementi. Il comando a destra, sopra il cursore dell'opacità, si chiamava `Azzera` e adesso si chiama `Elimina tutto`: toccalo, e il disegno si svuota. Poi disegna un elemento, sceglilo con un tocco e tieni premuta l'icona del Disegno nella fila dei moduli: il disegno si svuota e `Elimina` si spegne. Disegna un altro elemento: deve nascere senza i punti color accento, cioè non scelto. Fino alla 4.63 la pressione lunga lasciava la scelta su un elemento che non c'era più, e l'elemento disegnato dopo nasceva scelto.

## 2. Le linee dei terzi di `Raddrizza`

Nel modulo Geometria trascina il cursore `Raddrizza`: mentre il dito è giù compaiono le linee dei terzi, adesso nel colore d'accento, sottili e senza alone, come la guida che corre lungo il bordo quando un elemento del Disegno vi si appoggia. Prova su un'immagine chiara e su una scura. Fino alla 4.63 erano una linea bianca su un alone scuro.

## Decisioni da concordare

- **La seconda parte della G2** (nota E del giro della 4.60): due domande in chat, su `Copia` e su `Ruota`.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| `Elimina tutto`, e la pressione lunga sul Disegno | 4.64-01 | Non provato | | Attendere il collaudo. |
| Le linee dei terzi di `Raddrizza` | 4.64-02 | Non provato | | Attendere il collaudo. |
| La luminosità dal vivo sull'elemento scelto | 4.63-01 | OK | Chiesto se `Azzera` serva, e in Altro le linee dei terzi come le guide del Disegno | Fatte nella 4.64 (`4.64-01`, `4.64-02`). |

## Prossimi passi

- **In collaudo**: `Elimina tutto` e la pressione lunga sul Disegno (`4.64-01`); le linee dei terzi di `Raddrizza` (`4.64-02`).
- **Dopo**: la seconda parte della G2, cioè il menu a pressione lunga sull'elemento (`Sposta sopra`, `Copia`, `Duplica`, `Sposta sotto`, `Ruota`, `Elimina`) e le maniglie per ridimensionare; lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: la luminosità dal vivo sull'elemento scelto (`4.63-01`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
