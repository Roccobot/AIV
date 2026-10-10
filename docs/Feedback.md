# Feedback AIV

Versione **4.98**: il banco di prova non cade più a caso sul velo d'aiuto, e la spiegazione di `Filigrana` nelle impostazioni dice dov'è davvero il tasto.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.98 è pubblicata: [v4.98](https://github.com/Roccobot/AIV/releases/tag/v4.98), con l'APK
[AIV-4.98.apk](https://github.com/Roccobot/AIV/releases/download/v4.98/AIV-4.98.apk).
Commit prodotto su `main`: `7aa7124`, release dal commit `7aa7124` (SlimVer 4.98 / versionCode 355; APK 11.018.805 byte, digest `20aa2775`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.98**: nessuna prova da fare sul telefono, un'etichetta da confermare. Giro **4.97**: tre prove OK, gli stili sono chiusi.

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

Le verifiche automatiche della 4.98 sono superate: banco di prova completo (686 prove), controllo delle traduzioni, compilazione.

## Etichette testuali

### e-settings_mark_on_desc · Spiegazione di Filigrana nelle impostazioni dell'editor
Aggiunge la filigrana all'immagine al salvataggio. Puoi attivarla o disattivarla al volo con l'apposito tasto on/off nella barra in basso dell'editor, accanto a 'Ridimensiona'. Abilitare o disabilitare la filigrana da lì equivale esattamente a muovere questo interruttore. ⚠️ N.B. con l'aggiunta della filigrana, le operazioni senza perdita come la rotazione JPEG diventano una riscrittura completa.
<!-- Cambia solo la posizione del tasto: prima diceva 'in alto a destra, prima di Salva'. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| I dieci stili di casa | 4.97-01 | OK | | Chiusa. |
| Il tocco lungo somma | 4.97-02 | OK | | Chiusa. |
| Il segmento del tasto `Spessore` | 4.97-03 | OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: l'etichetta della spiegazione di `Filigrana`.
- **Fatto nella 4.98**: la prova del velo d'aiuto che fermava i rilasci; la causa era nel banco di prova, non nell'app.
- **Poi**: gli assestamenti e la 5.00, che ti chiederò di confermare.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
