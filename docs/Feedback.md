# Feedback AIV

Versione **4.96**: tre stili di casa hanno gli Effetti dei tuoi XMP, e uno stile si esporta e si importa da solo, col file `.aivstyle`.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.96 è pubblicata: [v4.96](https://github.com/Roccobot/AIV/releases/tag/v4.96), con l'APK
[AIV-4.96.apk](https://github.com/Roccobot/AIV/releases/download/v4.96/AIV-4.96.apk).
Commit prodotto su `main`: `5816198`, release dal commit `5816198` (SlimVer 4.96 / versionCode 353; APK 11.018.413 byte, digest `1b4a224f`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.96**: due prove sugli stili e due etichette. Le due prove della 4.95 sono ancora aperte, perché quel giro non è arrivato.

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

Le verifiche automatiche della 4.96 sono superate: banco di prova completo (684 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.95-01 | Non provato | | Attendere il collaudo. |
| 4.95-02 | Non provato | | Attendere il collaudo. |
| 4.96-01 | Non provato | | Attendere il collaudo. |
| 4.96-02 | Non provato | | Attendere il collaudo. |

## 1. Il tratto e il riempimento di fabbrica

Apri un'immagine nell'editor completo, così gli strumenti ripartono dai valori di fabbrica, poi apri il modulo Disegno e disegna una freccia: è rossa piena, quasi opaca, tratteggiata, e spessa come il cursore `Spessore` a due terzi della corsa. Disegna un rettangolo e un'ellisse: hanno lo stesso tratto, e dentro il riempimento rosso leggero. Tocca `Traccia`: il cursore è quasi in fondo a destra, al 95%; tocca `Spessore`: il cursore è a due terzi.

Letture mie: i valori valgono per tutti gli strumenti di forma, come hai scelto con A3; 'rossa' è il rosso del primo tondo così com'è, senza scurirlo; il riempimento è lo stesso rosso al 20%, l'opacità che aveva l'ambra.

## 2. Il tasto `Elimina`

Con il rettangolo scelto come strumento, la fila dei tasti sotto gli strumenti è spostata a destra: Tratteggio, Traccia, Spessore, Riempimento e, per ultimo, il cestino di `Elimina`, sotto `Pannello`. Disegna due rettangoli, scegline uno e tocca il cestino: sparisce quello scelto, l'altro resta. Tieni premuto il cestino: spariscono tutti. Passa a `Testo`: il cestino è l'ultimo tasto della fila, dopo `Testo`, e fa lo stesso con testi, pillole e pannelli. Accanto al nome del cursore non ci sono più `Elimina` ed `Elimina tutto`.

Letture mie: il cestino è acceso finché nel disegno c'è almeno un elemento, così la pressione lunga funziona anche senza un elemento scelto; un tocco senza un elemento scelto non fa niente. `Elimina` resta anche nel menu della pressione lunga su un elemento.

## 3. Gli Effetti negli stili di casa

Apri un'immagine nell'editor completo e il modulo Stili. Tocca `Roccobot`, poi apri il modulo Effetti: `Foschia` è a 5, `Vignettatura` a 3 con `Sfumatura` a 100, `Grana` a 2 con `Dimensione` a -32. Tocca `Dettagli fini`: negli Effetti c'è solo `Grana`, a 3, con `Dimensione` a -32. Tocca `T&O - Blu/Rosso`: `Foschia` a -6, `Vignettatura` a 1 con `Sfumatura` a 70, `Grana` a 1 con `Dimensione` a -56. Gli altri diciassette stili non toccano gli Effetti.

Letture mie: in Lightroom la vignettatura scurisce in negativo e qui in positivo, quindi il segno cambia; la dimensione della grana è un rapporto col 25 di serie di Lightroom, che faccio coincidere con la grana di serie di AIV; il punto medio della vignettatura diventa la `Sfumatura` (più basso in Lightroom, più dentro qui). Texture, Clarity, viraggio diviso e taratura dei primari restano fuori, perché AIV non ha quei moduli. 'Curva pellicola' è uno dei sei stili scritti da me e resta senza grana: aggiungerla cambierebbe uno stile che hai già provato.

## 4. Esportare e importare uno stile solo

Apri le impostazioni, `Stili dell'editor`: ogni riga ha tre icone, `Rinomina`, `Esporta` (il riquadro con la freccia) e il cestino, ultimo. Tocca `Esporta` sulla riga di `Roccobot`: il selettore propone `Roccobot.aivstyle`; salvalo, e compare `Stile esportato`. Rinomina `Roccobot` in qualcos'altro, poi tocca `Importa` in fondo alla pagina e scegli il file: compare `Stile importato`, e `Roccobot` arriva in fondo fra i tuoi stili salvati, mentre gli altri restano tutti. Importalo una seconda volta: non si raddoppia. Importa invece un file `.aivcollection`: sostituisce l'elenco come prima, e la notifica è `Stili importati`.

Letture mie: uno stile di casa esportato arriva dall'altra parte come stile tuo, col nome che ha adesso; lo stesso nome sostituisce, come quando salvi; il file di impostazioni non accetta un `.aivstyle` come archivio degli stili. Un nome con caratteri che un file non può avere (`T&O - Blu/Rosso`) esce con un trattino basso al loro posto.

## Etichette testuali

### e-settings_style_saved · Notifica dopo l'esportazione di uno stile solo
Stile esportato

### e-settings_style_loaded · Notifica dopo l'importazione di uno stile solo
Stile importato

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il tratto e il riempimento di fabbrica | 4.95-01 | Non provato | | Attendere il collaudo. |
| Il tasto `Elimina` | 4.95-02 | Non provato | | Attendere il collaudo. |
| Gli Effetti negli stili di casa | 4.96-01 | Non provato | | Attendere il collaudo. |
| Esportare e importare uno stile solo | 4.96-02 | Non provato | | Attendere il collaudo. |

## Prossimi passi

- **In collaudo**: il tratto e il riempimento di fabbrica (`4.95-01`), il tasto `Elimina` (`4.95-02`), gli Effetti negli stili (`4.96-01`) e lo stile solo (`4.96-02`).
- **Poi**: uno o due rilasci di assestamento e la 5.00, che ti chiederò di confermare.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
