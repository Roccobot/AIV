# Feedback AIV

Versione **5.11**: corregge le due note del giro 5.10 su AIV Play, cioè il menu Start schiacciato e la riga dell'accesso parziale, con `Consenti tutte` che ora apre la pagina di AIV nelle impostazioni di Android.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 5.11 è pubblicata: [v5.11](https://github.com/Roccobot/AIV/releases/tag/v5.11), con i due APK
[AIV-5.11.apk](https://github.com/Roccobot/AIV/releases/download/v5.11/AIV-5.11.apk) (AIV GitHub) e [AIV-Play-5.11.apk](https://github.com/Roccobot/AIV/releases/download/v5.11/AIV-Play-5.11.apk) (AIV Play).
Commit prodotto su `main`: `890d20c`, release dal commit `890d20c` (SlimVer 5.11 / versionCode 362; APK GitHub 11.027.145 byte, digest `5bf72d31`; APK Play 10.963.677 byte, digest `c1b941d0`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **5.11**: due prove. Giro **5.10**: quattro prove confermate, l'accesso parziale da rifare, le cinque etichette confermate.

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

Le verifiche automatiche della 5.11 sono superate: banco di prova completo di AIV GitHub (705 prove), prove di AIV Play (15), controllo delle traduzioni, compilazione delle due varianti.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 5.11-01 | Non provato | | Attendere il collaudo. |
| 5.11-02 | Non provato | | Attendere il collaudo. |

## 1. AIV Play: il menu Start non è più schiacciato

Installa `AIV-Play-5.11.apk` sopra la 5.10. Sulla schermata iniziale apri il menu Start dalla pillola: il pannello è quadrato e su tre colonne, come in AIV GitHub, e ogni casella è grande quanto le altre. Poi rivedi il velo d'aiuto del primo avvio (nelle impostazioni, `Ripristina gli avvisi`, poi torna sulla schermata iniziale): la copia del menu che il velo mostra ha la stessa forma.

Letture mie: senza `Cestino` e `Cartelle` le caselle sono sette, quindi le righe sono tre e quella in cima ne ha una sola. Con il menu sull'angolo destro le righe sono: la pillola in cima, a destra; poi `Lista`, `Mostra`, `Impostazioni`; in fondo `Cerca`, `Apri URL` e la ×. `Impostazioni` resta sopra la × per la regola della parola più lunga. Il difetto c'era anche in AIV GitHub con la vista `Cartelle di sistema` e `Crea`, e la correzione vale per tutte e due.

## 2. AIV Play: la riga dell'accesso parziale

Togli il permesso ad AIV Play dalle impostazioni di Android (`App` → AIV → `Autorizzazioni` → `Foto e video`), torna nell'app, tocca `Concedi l'accesso` e scegli di selezionare solo alcune foto. Sotto il titolo compare `Vedi solo le immagini che hai scelto.` con `Consenti tutte`: la frase comincia dove cominciano le copertine, `Consenti tutte` finisce dove finiscono, e i due testi sono sulla stessa riga di scrittura; fra la riga e le copertine c'è lo stesso spazio che separa le copertine fra loro. Tocca `Consenti tutte`: si apre la pagina di AIV nelle impostazioni di Android, dove concedi `Foto e video` per intero; tornando nell'app la riga sparisce e compaiono tutte le cartelle.

Letture mie: con l'accesso parziale già dato, Android risponde a una nuova richiesta del permesso con la scelta delle foto, ed è quello che vedevi nella 5.10; la pagina dell'app nelle impostazioni è il posto in cui il permesso si concede per intero.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| AIV Play: menu Start | 5.11-01 | Non provato | | Attendere il collaudo. |
| AIV Play: riga dell'accesso parziale | 5.11-02 | Non provato | | Attendere il collaudo. |
| AIV GitHub invariata | 5.10-01 | Tutto OK | | Chiusa. |
| AIV Play: permesso | 5.10-02 | Tutto OK | | Chiusa. |
| AIV Play: accesso parziale | 5.10-03 | Non approvato | Consenti tutte è allineato male e oltretutto un tocco non rimanda alla vera autorizzazione: torna alla selezione 'limitata'. | Rifatta nella 5.11 (`5.11-02`). |
| AIV Play: funzioni assenti | 5.10-04 | Tutto OK | | Chiusa. |
| AIV Play: `Salva` in Download | 5.10-05 | Tutto OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: il menu Start e la riga dell'accesso parziale di AIV Play (`5.11-01`, `5.11-02`).
- **AIV Play, pezzo 2**: le funzioni sui file rifatte con MediaStore, una per rilascio, cominciando da `Elimina`.
- **AIV Play, pezzo 3**: account sviluppatore, scheda e prova chiusa su Google Play, che sono passi tuoi.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
