# Feedback AIV

Versione **5.02**: nella pagina `Filigrana` lo stelo del centro è più corto e passa sotto il logo, `Posizione della filigrana` è più piccola e meno contrastata, e gli spazi intorno al riquadro sono ritoccati; nella riga `Editor di immagini`, `Imposta` e il nome dell'app sono allineati a destra.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.02 è pubblicata: [v5.02](https://github.com/Roccobot/AIV/releases/tag/v5.02), con l'APK
[AIV-5.02.apk](https://github.com/Roccobot/AIV/releases/download/v5.02/AIV-5.02.apk).
Commit prodotto su `main`: `02c065e`, release dal commit `02c065e` (SlimVer 5.02 / versionCode 359; APK 11.020.205 byte, digest `102355ff`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.02**: due prove e un'etichetta. Giro **5.01**: i ritocchi della pagina `Filigrana` e della riga dell'editor, fatti qui.
Fuori dal collaudo: nel codice ho corretto i commenti che dicevano il contrario delle regole già aggiornate, e gli accenti scritti con l'apostrofo nei due script di controllo delle icone; l'app non cambia.

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

Le verifiche automatiche della 5.02 sono superate: banco di prova completo (703 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.02-01 | Non provato | | Attendere il collaudo. |
| 5.02-02 | Non provato | | Attendere il collaudo. |

## 1. La pagina `Filigrana`: stelo, parola e spazi

Apri `Impostazioni` → `Editor e salvataggio` → `Filigrana`. Il riquadro è più vicino a `Rimuovi` | `Seleziona`, e fra la nota sotto il riquadro e `Opacità` c'è un po' più d'aria. In alto nel riquadro c'è `Posizione della filigrana`, più piccola e meno contrastata di prima. Scegli il centro: lo stelo sale dal tondo e si ferma prima del centro. Poi scegli un logo: al centro il logo copre lo stelo.

Letture mie: lo stelo si ferma 12 punti sotto il centro, nel mezzo dei 10-15 che chiedevi; il contrasto 3 dà un grigio più scuro del fondo sul bianco e sul grigio del tema chiaro, più chiaro sul nero e sul grigio del tema scuro; la parola è passata da 14 a 12 punti; sopra il riquadro ci sono 6 punti invece di 16, e sotto la nota 8 in più.

## 2. La riga `Editor di immagini`, allineata a destra

Nella pagina `Editor e salvataggio`, guarda la riga `Editor di immagini`: il testo di `Imposta` e il nome dell'app finiscono sulla stessa verticale, e `app:` è a destra, subito prima del nome. Sotto la riga c'è più spazio prima di `Copia di sicurezza`.

Letture mie: la verticale è quella del nome dell'app, come suggerivi, cioè il bordo della pagina; il tasto si è spostato verso destra della misura del suo margine interno, quindi il punto in cui il tocco funziona resta intero e arriva appena oltre il testo.

## Etichette testuali

### e-settings_mark_where · Parola dentro il riquadro dell'anteprima
Posizione della filigrana
<!-- La tua scelta del giro 5.01. Prima: 'Posizione'. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| La pagina `Filigrana` | 5.02-01 | Non provato | | Attendere il collaudo. |
| La riga `Editor di immagini` | 5.02-02 | Non provato | | Attendere il collaudo. |
| La pagina `Filigrana` | 5.01-01 | Accettabile | Stelo più corto sotto il logo, spazi, parola più piccola a contrasto 3 | Ritoccata nella 5.02-01. |
| La riga `Editor di immagini` | 5.01-02 | Accettabile | `Imposta` e nome allineati, `app:` a destra, più spazio sotto | Ritoccata nella 5.02-02. |
| La pagina `Filigrana` | 5.00-01 | Non approvato | Senza interruttore, nota sotto, stelo | Rifatta nella 5.01-01. |
| La riga `Editor di immagini` | 5.00-02 | Non approvato | `Imposta`, `app:`, un solo `Editor interno` | Rifatta nella 5.01-02. |

## Prossimi passi

- **In collaudo**: la pagina `Filigrana` (`5.02-01`), la riga `Editor di immagini` (`5.02-02`) e un'etichetta.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
