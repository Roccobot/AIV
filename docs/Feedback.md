# Feedback AIV

Versione **4.61**: nel modulo Disegno il tasto acceso ha un bordo pieno, arriva il velo d'aiuto del modulo, il tratteggio del tasto finisce con due trattini e il cursore della luminosità lascia vedere il colore che cambia.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.61 è pubblicata: [v4.61](https://github.com/Roccobot/AIV/releases/tag/v4.61), con l'APK
[AIV-4.61.apk](https://github.com/Roccobot/AIV/releases/download/v4.61/AIV-4.61.apk).
Commit prodotto su `main`: `745a150`, release dal commit `745a150` (SlimVer 4.61 / versionCode 340; APK 8.554.837 byte, digest `af41136e`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.61**: quattro prove nuove. Giro **4.60**: cinque prove OK.

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

Le verifiche automatiche della 4.61 sono superate: banco di prova completo (639 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.61-01 | Non provato | | Attendere il collaudo. |
| 4.61-02 | Non provato | | Attendere il collaudo. |
| 4.61-03 | Non provato | | Attendere il collaudo. |
| 4.61-04 | Non provato | | Attendere il collaudo. |

## 1. Il bordo pieno sul tasto acceso

Nel modulo `Disegno` lo strumento scelto (rettangolo, freccia...) e i tasti accesi (`Tratteggio`, quello fra `Traccia`, `Spessore` e `Riempimento` che regola il cursore) hanno un bordo pieno color accento di 2 dp, oltre allo sfondo verde. Il bordo è disegnato sopra il tasto, così le bande che vanno da bordo a bordo non lo interrompono.

## 2. Il velo d'aiuto del Disegno

Per rivederlo: Impostazioni, 'Ripristina gli avvisi', poi apri l'editor completo e passa le due slide dell'editor. Al primo `Disegno` compaiono il tondo rosso cerchiato d'arancione e, sopra, il tuo testo: 'Con il modulo Disegno puoi aggiungere all'immagine linee, frecce, ellissi, rettangoli arrotondati, testi semplici e riquadri 'pillola'. Il salvataggio appiattisce l'immagine: non è possibile riaprirla per modificare o spostare gli elementi. Premi a lungo su un colore per regolare la sua luminosità.' Un tocco lo chiude. Il testo nomina anche il testo e la pillola, che non ci sono ancora: l'ho lasciato com'è, così le 28 lingue si scrivono una volta sola. È anche nell'[artefatto dei veli](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

## 3. Il tratteggio del tasto finisce con due trattini

Spegni `Tratteggio` e guardane i due capi: la banda comincia e finisce con un trattino. Trattini e spazi si allungano o si accorciano insieme, col ritmo del tuo mockup, finché chiudono sui due bordi del tasto; su un telefono e su un tablet il tasto ha larghezze diverse, e il ritmo resta quello.

## 4. Il cursore della luminosità senza velo

Disegna un elemento, sceglilo, e tieni premuto un tondo: il cursore della luminosità si apre senza velo e senza sfocatura, con un'ombra intorno (la stessa dell'opzione 'Ombra' delle impostazioni). Mentre il dito scorre vedi l'elemento scelto e i tasti cambiare colore.

## Decisioni da concordare

- **La seconda parte della G2** (nota E del giro della 4.60): due domande in chat, su `Copia` e su `Ruota`.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il bordo pieno sul tasto acceso | 4.61-01 | Non provato | | Attendere il collaudo. |
| Il velo d'aiuto del Disegno | 4.61-02 | Non provato | | Attendere il collaudo. |
| Il tratteggio del tasto finisce con due trattini | 4.61-03 | Non provato | | Attendere il collaudo. |
| Il cursore della luminosità senza velo | 4.61-04 | Non provato | | Attendere il collaudo. |
| Un tocco sceglie un elemento | 4.60-01 | OK | | Concluso. |
| I parametri cambiano l'elemento scelto | 4.60-02 | OK | | Concluso. |
| L'elemento scelto si sposta | 4.60-03 | OK | | Concluso. |
| Elimina | 4.60-04 | OK | | Concluso. |
| Il tasto Tratteggio acceso e spento | 4.60-05 | OK | | Concluso. |

## Prossimi passi

- **In collaudo**: il bordo pieno (`4.61-01`), il velo d'aiuto del Disegno (`4.61-02`), il tratteggio del tasto (`4.61-03`), il cursore della luminosità senza velo (`4.61-04`).
- **Dopo**: la seconda parte della G2, cioè il menu a pressione lunga sull'elemento (`Sposta sopra`, `Copia`, `Duplica`, `Sposta sotto`, `Ruota`, `Elimina`) e le maniglie per ridimensionare; lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: le cinque prove della 4.60 (`4.60-01..05`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
