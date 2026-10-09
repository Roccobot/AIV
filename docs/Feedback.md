# Feedback AIV

Versione **4.93**: nel modulo Disegno il doppio tocco su un testo, una pillola o un pannello apre la finestra delle parole.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.93 è pubblicata: [v4.93](https://github.com/Roccobot/AIV/releases/tag/v4.93), con l'APK
[AIV-4.93.apk](https://github.com/Roccobot/AIV/releases/download/v4.93/AIV-4.93.apk).
Commit prodotto su `main`: `f0d1b55`, release dal commit `f0d1b55` (SlimVer 4.93 / versionCode 350; APK 11.015.461 byte, digest `f00df977`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.93**: una prova sul modulo Disegno. Giro **4.92**: tutte e dieci le prove OK, e l'etichetta del velo approvata.

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

Le verifiche automatiche della 4.93 sono superate: banco di prova completo (677 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.93-01 | Non provato | | Attendere il collaudo. |

## 1. Il doppio tocco su un testo, una pillola o un pannello

Nel modulo Disegno scrivi un testo, disegna una pillola e un pannello, tutti e tre con delle parole. Con qualunque strumento scelto, tocca due volte di seguito il testo: si apre la finestra delle parole con le sue, come col tasto `Testo`; cambiale e tocca `Applica`. Fai lo stesso sulla pillola e sul pannello. Tocca due volte un rettangolo o una freccia: non succede niente di nuovo.

Poi tocca una volta un elemento già scelto: alterna ancora `Trasforma` e `Ruota`, senza ritardo. Tocca un testo non scelto e, subito dopo, trascinalo: si sposta. Dopo un doppio tocco, chiudi la finestra con `Annulla` e tieni premuto l'elemento: il menu dice ancora la modalità di prima.

Letture mie: il primo tocco fa subito quello che fa un tocco singolo (sceglie l'elemento o alterna la modalità), e il secondo, se arriva in fretta sullo stesso elemento, annulla l'alternanza e apre la finestra; così il tocco singolo non diventa più lento e un trascinamento subito dopo un tocco resta un trascinamento. Il tempo per il secondo tocco è quello del doppio tocco di Android.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il doppio tocco su testo, pillola e pannello | 4.93-01 | Non provato | | Attendere il collaudo. |
| `Elimina` accanto a `Elimina tutto` | 4.92-01 | OK | | Chiusa. |
| La dimensione di un testo nuovo | 4.92-02 | OK | | Chiusa. |
| Il colore non cambia la dimensione | 4.92-03 | OK | | Chiusa. |
| L'allineamento | 4.92-04 | OK | | Chiusa. |
| La centratura sulle minuscole | 4.92-05 | OK | | Chiusa. |
| Pillola e pannello senza parole | 4.92-06 | OK | | Chiusa. |
| I raccordi dell'etichetta | 4.92-07 | OK | | Chiusa. |
| Le strisce e il cursore fermo | 4.92-08 | OK | | Chiusa. |
| Il tasto `Sfondo` | 4.92-09 | OK | | Chiusa. |
| Gli angoli del Pannello | 4.92-10 | OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: il doppio tocco su testo, pillola e pannello (`4.93-01`).
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **Dopo**: gli stili, nella 4.95, con uno o due rilasci di assestamento; poi la 5.00.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
