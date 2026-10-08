# Feedback AIV

Versione **4.63**: nel modulo Disegno la luminosità si vede sull'elemento scelto mentre il dito scorre.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.63 è pubblicata: [v4.63](https://github.com/Roccobot/AIV/releases/tag/v4.63), con l'APK
[AIV-4.63.apk](https://github.com/Roccobot/AIV/releases/download/v4.63/AIV-4.63.apk).
Commit prodotto su `main`: `cc1c250`, release dal commit `cc1c250` (SlimVer 4.63 / versionCode 342; APK 8.554.837 byte, digest `97c6427a`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.63**: una prova nuova. Giro **4.61** e **4.62**: sette prove OK.

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

Le verifiche automatiche della 4.63 sono superate: banco di prova completo (644 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.63-01 | Non provato | | Attendere il collaudo. |

## 1. La luminosità dal vivo sull'elemento scelto

Disegna un elemento e sceglilo con un tocco. Tieni premuto un tondo e, senza staccare il dito, scorri: l'elemento cambia luminosità sotto il dito, e allo stacco il cursore si chiude. Poi tieni premuto un tondo e alza il dito fermo: il cursore resta, e trascinandolo l'elemento cambia colore allo stesso modo. Nella 4.61 il gesto unico posava il valore solo allo stacco, anche se la voce `4.61-04` prometteva l'anteprima dal vivo: adesso una prova lo misura.

## Decisioni da concordare

- **La seconda parte della G2** (nota E del giro della 4.60): due domande in chat, su `Copia` e su `Ruota`.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La luminosità dal vivo sull'elemento scelto | 4.63-01 | Non provato | | Attendere il collaudo. |
| Il bordo pieno sul tasto acceso | 4.61-01 | OK | | Concluso. |
| Il velo d'aiuto del Disegno | 4.61-02 | OK | | Concluso. |
| Il tratteggio del tasto finisce con due trattini | 4.61-03 | OK | | Concluso. |
| Il cursore della luminosità senza velo | 4.61-04 | OK | Chiesta l'anteprima dal vivo sull'elemento scelto, nei due gesti | Fatta nella 4.63 (`4.63-01`). |
| Disegnando, l'elemento si appoggia al bordo | 4.62-01 | OK | | Concluso. |
| Spostando, l'elemento si appoggia al bordo, e oltre esce | 4.62-02 | OK | | Concluso. |
| La guida lungo il bordo | 4.62-03 | OK | | Concluso. |

## Prossimi passi

- **In collaudo**: la luminosità dal vivo sull'elemento scelto (`4.63-01`).
- **Dopo**: la seconda parte della G2, cioè il menu a pressione lunga sull'elemento (`Sposta sopra`, `Copia`, `Duplica`, `Sposta sotto`, `Ruota`, `Elimina`) e le maniglie per ridimensionare; lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: le sette prove di 4.61 e 4.62 (`4.61-01..04`, `4.62-01..03`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
