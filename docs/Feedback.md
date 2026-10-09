# Feedback AIV

Versione **4.92**: nel modulo Disegno `Elimina` accanto a `Elimina tutto`, la dimensione di un testo nuovo, il colore che non la cambia più, l'allineamento, la centratura sulle minuscole, pillola e pannello senza parole, i raccordi dell'etichetta, le strisce, il tasto `Sfondo` e gli angoli del Pannello.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.92 è pubblicata: [v4.92](https://github.com/Roccobot/AIV/releases/tag/v4.92), con l'APK
[AIV-4.92.apk](https://github.com/Roccobot/AIV/releases/download/v4.92/AIV-4.92.apk).
Commit prodotto su `main`: `f1531c6`, release dal commit `f1531c6` (SlimVer 4.92 / versionCode 349; APK 10.999.077 byte, digest `2109c1b9`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.92**: dieci prove sul modulo Disegno e in fondo l'**etichetta testuale** del velo del Disegno, che nomina anche i pannelli. Campo vuoto vuol dire approvato. Giro **4.91**: quattro prove OK, il testo accettabile, `Elimina` non approvato; tutto rifatto qui.

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

Le verifiche automatiche della 4.92 sono superate: banco di prova completo (676 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.92-01 | Non provato | | Attendere il collaudo. |
| 4.92-02 | Non provato | | Attendere il collaudo. |
| 4.92-03 | Non provato | | Attendere il collaudo. |
| 4.92-04 | Non provato | | Attendere il collaudo. |
| 4.92-05 | Non provato | | Attendere il collaudo. |
| 4.92-06 | Non provato | | Attendere il collaudo. |
| 4.92-07 | Non provato | | Attendere il collaudo. |
| 4.92-08 | Non provato | | Attendere il collaudo. |
| 4.92-09 | Non provato | | Attendere il collaudo. |
| 4.92-10 | Non provato | | Attendere il collaudo. |

## 1. `Elimina` accanto a `Elimina tutto`

Nel modulo Disegno, sopra il cursore, a destra della sua etichetta, adesso ci sono due tasti di testo: `Elimina` ed `Elimina tutto`. `Elimina` è spento finché nessun elemento è scelto. Disegna una pillola e un rettangolo, scegli la pillola e tocca `Elimina`: la pillola sparisce, il rettangolo resta. Prova lo stesso con un testo, un pannello e una freccia. Nella fila dei tasti delle forme la quinta colonna è vuota, e il menu della pressione lunga ha ancora il suo `Elimina`.

## 2. La dimensione di un testo nuovo

Tocca `Testo`: senza un testo scelto il cursore `Dimensione` è spento. Tocca un punto dell'immagine e scrivi una parola: il testo nasce con la riga più lunga larga metà dell'immagine. Prova su un'immagine orizzontale e su una verticale, e con un testo di due righe, una lunga e una corta: è la riga lunga a misurare metà. Con il testo scelto il cursore si accende e cambia la dimensione. Lo stesso vale per le parole di una pillola o di un pannello posati da un tocco.

Letture mie: il cursore regola soltanto il testo scelto, perché un testo nuovo nasce con la sua dimensione; le parole di una pillola o di un pannello posati da un tocco prendono la stessa dimensione del testo.

## 3. Il colore non riporta il testo alla dimensione di prima

Scrivi un testo e lascialo scelto. Tira un suo angolo e ingrandiscilo, poi tocca un altro colore nella tavolozza: il testo prende il colore e resta grande. Prova anche con la luminosità, tenendo premuto un tondo, e con `Grassetto`.

## 4. L'allineamento: centro, sinistra, destra

Scrivi un testo di due righe di lunghezza diversa e tocca `Allineamento`: le righe vanno a sinistra; tocca ancora: a destra; ancora: tornano al centro. Lo stesso in una pillola e in un pannello.

## 5. La centratura sulle minuscole

Disegna una pillola larga e scrivi una parola tutta minuscola, per esempio `marea`: il corpo delle minuscole è al centro della pillola, con lo stesso spazio sopra e sotto. Prova la stessa parola in un pannello e in un testo con `Sfondo` acceso. Poi scrivi una parola tutta maiuscola, per esempio `CIAO`: le maiuscole sono al centro.

Letture mie: conta la maggioranza delle lettere del testo intero, minuscole da una parte e maiuscole e cifre dall'altra; tutte le righe si centrano sulla stessa fascia, così la distanza fra le righe resta uguale.

## 6. Pillola e pannello senza parole

Disegna una pillola: nella finestra delle parole `Applica` è già acceso, e toccandolo la pillola resta vuota. Scrivi qualcosa in una pillola, poi riapri la finestra con il tasto `Testo`, cancella tutto e tocca `Applica`: le parole spariscono e la pillola resta. Lo stesso col pannello. Con lo strumento `Testo`, invece, `Applica` aspetta ancora una lettera.

Letture mie: con `Pillola` o `Pannello`, un tocco su un punto vuoto seguito da `Applica` a campo vuoto non posa niente, perché un tocco misura la pillola sulle sue parole; per una pillola senza parole si trascina il dito.

## 7. I raccordi dell'etichetta

Scrivi un testo di tre righe di lunghezza diversa, per esempio `yolo`, `che dire`, `raga`, e tocca `Sfondo`. Dove una riga è più stretta della vicina, l'angolo fra le due strisce è un raccordo concavo; gli angoli che sporgono restano tondi; due righe lunghe uguali continuano dritte, senza tacche. Confronta col tuo mockup.

Letture mie: quando due righe sono quasi lunghe uguali, il tondo e il raccordo si stringono a metà della differenza, così non si incrociano.

## 8. Le strisce e il cursore fermo

Col rettangolo come strumento guarda dove sono il cursore e le due file di tasti. Passa a `Testo`, scrivi un testo e accendi `Sfondo`: sotto il cursore compare la fila delle strisce, rettangoli colorati affiancati senza spazio e più bassi dei tondi, e il cursore e le file dei tasti non si muovono. Passa da uno strumento all'altro: il cursore resta sempre nello stesso posto, e le due file non si avvicinano.

Letture mie: le strisce sono alte 20 dp, con i loro angoli esterni stondati; il posto della loro fila c'è con ogni strumento, vuoto quando non servono, ed è così che il cursore resta fermo.

## 9. Il tasto `Sfondo`

Guarda il tasto `Sfondo` nella fila del testo: è un rettangolino arrotondato con `Aa` in negativo, e non somiglia più a `Carattere`. Spento ha il colore dei tasti; acceso prende il colore della striscia scelta, e cambia mentre scegli un'altra striscia.

## 10. Gli angoli del Pannello

Disegna un pannello grande: gli angoli sono stondati dell'1% del suo lato lungo, più di prima.

## Etichette testuali

### e-hint_draw · Il velo del Disegno
Con il modulo Disegno puoi aggiungere all'immagine linee, frecce, ellissi, rettangoli arrotondati, testi semplici, riquadri 'pillola' e pannelli sfocati. Il salvataggio appiattisce l'immagine: non è possibile riaprirla per modificare o spostare gli elementi. Premi a lungo su un colore per regolare la sua luminosità.
<!-- Il velo che compare la prima volta che si apre il modulo Disegno. È cambiata la fine dell'elenco, con 'e pannelli sfocati' (tua risposta A1 a d-velo-pannello). La chiave del velo non cambia: chi l'ha già visto lo rivede da Impostazioni, 'Ripristina gli avvisi'. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| `Elimina` accanto a `Elimina tutto` | 4.92-01 | Non provato | | Attendere il collaudo. |
| La dimensione di un testo nuovo | 4.92-02 | Non provato | | Attendere il collaudo. |
| Il colore non cambia la dimensione | 4.92-03 | Non provato | | Attendere il collaudo. |
| L'allineamento | 4.92-04 | Non provato | | Attendere il collaudo. |
| La centratura sulle minuscole | 4.92-05 | Non provato | | Attendere il collaudo. |
| Pillola e pannello senza parole | 4.92-06 | Non provato | | Attendere il collaudo. |
| I raccordi dell'etichetta | 4.92-07 | Non provato | | Attendere il collaudo. |
| Le strisce e il cursore fermo | 4.92-08 | Non provato | | Attendere il collaudo. |
| Il tasto `Sfondo` | 4.92-09 | Non provato | | Attendere il collaudo. |
| Gli angoli del Pannello | 4.92-10 | Non provato | | Attendere il collaudo. |
| Il Pannello | 4.91-01 | OK | Angoli all'1% | Fatto nella 4.92 (prova `4.92-10`). |
| I colori della pillola | 4.91-02 | OK | | Chiusa. |
| Il testo con `Sfondo`, `Testo` e la dimensione | 4.91-03 | Accettabile | Dimensione dopo il colore; dimensione di fabbrica | Fatto nella 4.92 (prove `4.92-02` e `4.92-03`). |
| `Elimina` nella quinta colonna | 4.91-04 | Non approvato | `Elimina` accanto a `Elimina tutto` | Fatto nella 4.92 (prova `4.92-01`). |
| La centratura sull'immagine | 4.91-05 | OK | | Chiusa. |
| Gli strumenti più grandi | 4.91-06 | OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: le dieci prove della 4.92 e l'etichetta del velo del Disegno.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **Dopo**: gli stili, nella 4.95, con uno o due rilasci di assestamento; poi la 5.00.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
