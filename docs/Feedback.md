# Feedback AIV

Versione **4.33**: le correzioni del giro 4.32. Lo zoom all'apertura con le info spente, il trascinamento unico col menu Start aperto, il glifo senza scatto, le etichette del menu Start, `Menu basso`, le sfumature nuove e il testo nuovo del velo d'aiuto.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.33 è pubblicata: [v4.33](https://github.com/Roccobot/AIV/releases/tag/v4.33), con l'APK
[AIV-4.33.apk](https://github.com/Roccobot/AIV/releases/download/v4.33/AIV-4.33.apk).
Commit prodotto su `main`: `55c9c88` (SlimVer 4.33 / versionCode 322).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.33**: otto prove nuove dal giro 4.32. Giro **4.32**: `4.30-02`, `4.30-04`, `4.30-06`, `4.31-01` e `4.31-02` OK; `4.30-01`, `4.30-03`, `4.30-05`, `4.30-07` e `4.31-03` rifatte nella 4.33.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ o Salva, il dischetto, per il pannello). Su
desktop: Altro in colonna laterale. Sotto il campo di Altro ci sono allegati e
formattazione, e sotto ancora i sei comandi: Azzera tutto, Copia il riepilogo (negli
appunti), Esporta e Importa (uno ZIP con risposte e allegati), Salva, Invia. **Etichette testuali** (se presenti) vanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano, e le risposte alle prove chiuse escono dalla bozza.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 4.33 sono superate: banco di prova completo (577 prove), controllo delle 28 traduzioni e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.33-01 | Non provato | | Attendere il collaudo. |
| 4.33-02 | Non provato | | Attendere il collaudo. |
| 4.33-03 | Non provato | | Attendere il collaudo. |
| 4.33-04 | Non provato | | Attendere il collaudo. |
| 4.33-05 | Non provato | | Attendere il collaudo. |
| 4.33-06 | Non provato | | Attendere il collaudo. |
| 4.33-07 | Non provato | | Attendere il collaudo. |
| 4.33-08 | Non provato | | Attendere il collaudo. |

## 1. Lo zoom all'apertura con le info spente

Spegni le info in alto (o in basso) e apri un'immagine dalla griglia di una cartella: deve comparire subito a tutto schermo, senza partire più piccola e crescere. La causa era un'altra da quella corretta nella 4.32: lo stato della barra delle info nasceva acceso anche con le info spente, quindi la prima misura lasciava spazio a una barra che non c'era, e lo spazio si richiudeva con l'animazione. Il banco adesso lo misura fotogramma per fotogramma, e la prova falliva col difetto.

## 2. Il trascinamento col menu Start aperto

Apri il menu Start, poi trascina subito su o giù sulla griglia (o sulle cartelle della schermata iniziale): con lo stesso gesto il menu si chiude e la griglia scorre. Prima il primo tocco chiudeva il menu e annullava il trascinamento.

## 3. Menu basso

In **Elemento interattivo principale** il gettone `Menu` si chiama `Menu basso`, in tutte le lingue. Sceglilo: la fila dei tasti è 8dp più in basso, dentro lo spazio dei gesti di sistema.

## 4. Le sfumature in basso

Nella schermata iniziale: la sfumatura ampia com'è, più quella corta, che arriva all'85% invece del pieno. In una cartella: solo la sfumatura ampia, più opaca in fondo (65%); scorrendo resta, al 60% della sua opacità; negli ultimi 2 cm prima della fine della griglia scende a zero seguendo il dito. I 2 cm sono la mia lettura di *negli ultimi centimetri*: dimmi se li vuoi diversi.

## 5. Il velo d'aiuto del primo avvio

Nelle impostazioni tocca `Ripristina gli avvisi` e torna alla schermata iniziale col menu Start: la frase è la tua, `Tocca il tasto nell'angolo per accedere al menu: contiene tutti i comandi principali; tienilo premuto per le opzioni di visualizzazione. Dalle impostazioni, se vuoi, puoi cambiare il lato del pulsante.`

## 6. Il glifo che torna sul tondo

Con la pillola a scomparsa, scorri una cartella e fermati: il marchio torna sul tondo senza fermarsi a metà e senza il pezzo del disco che compariva di colpo alla fine. La causa: durante la dissolvenza il marchio era disegnato in un livello grande quanto la sua scatola, e il disco, che sporge un poco sopra, veniva tagliato. Il banco lo misura sui pixel.

## 7. Le etichette del menu Start

La tua prova di usabilità: il menu Start ha la stessa disposizione, con icone più grandi (caselle da 64dp) e sotto ognuna un nome breve: `Pillola`/`Tondo`, `Griglia`, `Lista`, `Cartelle`, `Mostra`/`Nascondi`, `Cerca`, `URL`, `Crea`, `Cestino`, `Impostazioni`; nel cestino `Cronologia`, `Ripristina`, `Svuota`. La × resta senza nome, e col centro sul centro del tondo. Le parole lunghe di altre lingue si rimpiccioliscono per entrare nella casella. Dimmi se la prova funziona: allora riattivo il tocco lungo su `Mostra`/`Nascondi`.

## 8. Il commutatore con dieci icone

Nella modalità `Cartelle di sistema`, dentro una cartella, il menu Start ha `Crea` (Nuova cartella): lì il commutatore pillola/tondo non c'è, e le righe restano tre.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Lo zoom all'apertura con le info spente | 4.33-01 | Non provato | | Attendere il collaudo. |
| Il trascinamento col menu Start aperto | 4.33-02 | Non provato | | Attendere il collaudo. |
| Menu basso | 4.33-03 | Non provato | | Attendere il collaudo. |
| Le sfumature in basso | 4.33-04 | Non provato | | Attendere il collaudo. |
| Il velo d'aiuto del primo avvio | 4.33-05 | Non provato | | Attendere il collaudo. |
| Il glifo che torna sul tondo | 4.33-06 | Non provato | | Attendere il collaudo. |
| Le etichette del menu Start | 4.33-07 | Non provato | | Attendere il collaudo. |
| Il commutatore con dieci icone | 4.33-08 | Non provato | | Attendere il collaudo. |
| I nomi dei menu | 4.30-01 | OK | `Menu` diventa `Menu basso`. | Fatto nella 4.33 (`4.33-03`). |
| Il tondo unico | 4.30-02 | OK | Tutto OK. | Archiviata. |
| Il menu Start si chiude a ogni tocco fuori | 4.30-03 | Accettabile | Il trascinamento dev'essere un gesto solo. | Rifatto nella 4.33 (`4.33-02`). |
| Il glifo del FAB | 4.30-04 | OK | Domanda sul colore delle icone. | Risposta in chat. |
| Lo zoom all'apertura | 4.30-05 | Non approvato | Resta con le info spente. | Rifatto nella 4.33 (`4.33-01`). |
| La sfocatura sotto il vetro | 4.30-06 | OK | Tutto OK. | Archiviata. |
| Le sfumature in basso | 4.30-07 | Non approvato | Specifica nuova per home e cartelle. | Rifatto nella 4.33 (`4.33-04`). |
| Il punto di riferimento dei comandi | 4.31-01 | OK | Tutto OK. | Archiviata. |
| Il menu Start di fabbrica | 4.31-02 | OK | Tutto OK. | Archiviata. |
| Il velo d'aiuto del primo avvio | 4.31-03 | OK | Testo nuovo. | Fatto nella 4.33 (`4.33-05`). |

## Prossimi passi

- **In collaudo**: zoom con le info spente, trascinamento unico, Menu basso, sfumature, velo d'aiuto, glifo, etichette del menu Start, commutatore (`4.33-01`-`4.33-08`).
- **Da decidere in chat**: colore delle icone sul vetro personalizzato (`4.30-04`); dissolvenza al tocco di una miniatura (nota A).
- **In attesa di piano e stima**: il modulo Disegno dell'editor (nota G), da concordare prima di cominciare.
- **Concluso**: tondo unico, sfocatura sotto il vetro, punto di riferimento, menu Start di fabbrica (`4.30-02`, `4.30-06`, `4.31-01`, `4.31-02`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
