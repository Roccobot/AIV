# Feedback AIV

Versione **4.95**: nel modulo Disegno il tratto e il riempimento di fabbrica sono rossi, e `Elimina` è un'icona, l'ultima a destra.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.95 è pubblicata: [v4.95](https://github.com/Roccobot/AIV/releases/tag/v4.95), con l'APK
[AIV-4.95.apk](https://github.com/Roccobot/AIV/releases/download/v4.95/AIV-4.95.apk).
Commit prodotto su `main`: `15d1735`, release dal commit `15d1735` (SlimVer 4.95 / versionCode 352; APK 11.015.461 byte, digest `65c2c41d`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.95**: due prove sul modulo Disegno. Giro **4.94**: le due prove OK, la domanda sul disturbo chiusa (era il gesto a tre dita), e tre note in Altro.

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

Le verifiche automatiche della 4.95 sono superate: banco di prova completo (680 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.95-01 | Non provato | | Attendere il collaudo. |
| 4.95-02 | Non provato | | Attendere il collaudo. |

## 1. Il tratto e il riempimento di fabbrica

Apri un'immagine nell'editor completo, così gli strumenti ripartono dai valori di fabbrica, poi apri il modulo Disegno e disegna una freccia: è rossa piena, quasi opaca, tratteggiata, e spessa come il cursore `Spessore` a due terzi della corsa. Disegna un rettangolo e un'ellisse: hanno lo stesso tratto, e dentro il riempimento rosso leggero. Tocca `Traccia`: il cursore è quasi in fondo a destra, al 95%; tocca `Spessore`: il cursore è a due terzi.

Letture mie: i valori valgono per tutti gli strumenti di forma, come hai scelto con A3; 'rossa' è il rosso del primo tondo così com'è, senza scurirlo; il riempimento è lo stesso rosso al 20%, l'opacità che aveva l'ambra.

## 2. Il tasto `Elimina`

Con il rettangolo scelto come strumento, la fila dei tasti sotto gli strumenti è spostata a destra: Tratteggio, Traccia, Spessore, Riempimento e, per ultimo, il cestino di `Elimina`, sotto `Pannello`. Disegna due rettangoli, scegline uno e tocca il cestino: sparisce quello scelto, l'altro resta. Tieni premuto il cestino: spariscono tutti. Passa a `Testo`: il cestino è l'ultimo tasto della fila, dopo `Testo`, e fa lo stesso con testi, pillole e pannelli. Accanto al nome del cursore non ci sono più `Elimina` ed `Elimina tutto`.

Letture mie: il cestino è acceso finché nel disegno c'è almeno un elemento, così la pressione lunga funziona anche senza un elemento scelto; un tocco senza un elemento scelto non fa niente. `Elimina` resta anche nel menu della pressione lunga su un elemento.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il tratto e il riempimento di fabbrica | 4.95-01 | Non provato | | Attendere il collaudo. |
| Il tasto `Elimina` | 4.95-02 | Non provato | | Attendere il collaudo. |
| Le file del modulo Disegno | 4.94-01 | OK | | Chiusa. |
| La striscia scelta | 4.94-02 | OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: il tratto e il riempimento di fabbrica (`4.95-01`) e il tasto `Elimina` (`4.95-02`).
- **Adesso**: gli stili, nella 4.96, dai tuoi diciannove XMP che ho già; poi uno o due rilasci di assestamento e la 5.00.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
