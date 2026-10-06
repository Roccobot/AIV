# Feedback AIV

Versione **4.37**: le note del giro 4.36. Le frecce su e giù un poco più spesse, le due frasi dei veli d'aiuto che dicono 'il pulsante', l'`Opacità` di fabbrica al 40%, `Estensione` senza salto e le linee dei terzi in `Raddrizza`.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.37 è pubblicata: [v4.37](https://github.com/Roccobot/AIV/releases/tag/v4.37), con l'APK
[AIV-4.37.apk](https://github.com/Roccobot/AIV/releases/download/v4.37/AIV-4.37.apk).
Commit prodotto su `main`: `b369e61`, release dal commit `b369e61` (SlimVer 4.37 / versionCode 326; APK 8.466.453 byte, digest `1288d546`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.37**: cinque prove nuove dalle note del giro 4.36. Giro **4.36**: `4.36-01` e `4.36-03` OK, `4.36-02` accettabile e ripresa in `4.37-01`.

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

Le verifiche automatiche della 4.37 sono superate: banco di prova completo (590 prove), controllo delle 28 traduzioni, controllo delle icone e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.37-01 | Non provato | | Attendere il collaudo. |
| 4.37-02 | Non provato | | Attendere il collaudo. |
| 4.37-03 | Non provato | | Attendere il collaudo. |
| 4.37-04 | Non provato | | Attendere il collaudo. |
| 4.37-05 | Non provato | | Attendere il collaudo. |

## 1. Le frecce su e giù un poco più spesse

Scorri una cartella con un gesto veloce: la freccia su o giù ha la stessa forma e lo stesso vertice della 4.36, con il tratto più spesso di 0,3dp, come hai chiesto in `4.36-02`. Confrontala con le icone accanto.

## 2. Le frasi dei veli d'aiuto

In `Impostazioni` tocca `Ripristina gli avvisi`, poi torna alla schermata iniziale con il FAB o con una pillola: il velo dice `Scorciatoia: tieni premuto il pulsante per le opzioni di visualizzazione.` Entra nel cestino: il velo dice `Scorciatoia: tieni premuto il pulsante per svuotare il cestino.` Sono le tue frasi, e la stessa correzione è nelle altre lingue.

## 3. L'Opacità di fabbrica al 40%

Con l'aspetto `Traslucido`, in `Tema e dettagli grafici` tocca `Ripristina`: `Opacità` torna al 40% e non più al 20%. Gli altri valori della tua schermata (Raggio 16dp, Intensità 300%, Scostamento 35%, colori predefiniti) erano già quelli di fabbrica. Chi ha già l'app tiene i valori che ha finché non tocca `Ripristina`.

## 4. Estensione senza salto

Con `Consenti la modifica dell'estensione` acceso per la rinomina, in una cartella seleziona un file, tocca `Rinomina`, poi `Estensione`: la finestra deve comparire ferma, con la tastiera già aperta sul campo. Prima il campo non prendeva il fuoco, la tastiera di `Rinomina` si chiudeva e le due finestre si ridisponevano mentre spariva: la causa è ragionata e non misurata, perché sul banco non c'è una tastiera. Se il salto resta, dimmi se la tastiera si chiude e si riapre.

## 5. Le linee dei terzi in Raddrizza

Apri un'immagine nell'editor completo, modulo `Geometria`, e trascina `Raddrizza`: mentre il dito è sul cursore compaiono due linee verticali e due orizzontali, ai terzi dell'immagine, e spariscono quando lo lasci. Per restare visibili su qualunque immagine sono una linea chiara su un alone scuro e non un colore scelto dall'immagine: una linea dell'orizzonte attraversa cielo e terra, e un colore solo sparirebbe su metà della sua lunghezza. Se toccando la barra senza strisciare le linee restano un istante dopo il rilascio, è l'attesa del doppio tocco che azzera il cursore.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Le frecce su e giù un poco più spesse | 4.37-01 | Non provato | | Attendere il collaudo. |
| Le frasi dei veli d'aiuto | 4.37-02 | Non provato | | Attendere il collaudo. |
| L'Opacità di fabbrica al 40% | 4.37-03 | Non provato | | Attendere il collaudo. |
| Estensione senza salto | 4.37-04 | Non provato | | Attendere il collaudo. |
| Le linee dei terzi in Raddrizza | 4.37-05 | Non provato | | Attendere il collaudo. |
| La dissolvenza al posto dei cambi netti | 4.36-01 | OK | Funziona, va tutto molto meglio. | Archiviata. |
| I glifi su e giù più sottili | 4.36-02 | Accettabile | Troppo sottili: 0,3dp in più. | Ripresa in `4.37-01`. |
| I tasti della pillola aperta centrati | 4.36-03 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: frecce più spesse, frasi dei veli, `Opacità` al 40%, `Estensione` senza salto, terzi in `Raddrizza` (`4.37-01`-`4.37-05`).
- **Prossima versione**: il modulo Disegno, prima fase (4.40: mano libera, segmenti, frecce, rettangoli, ellissi, colore e tratto), ultimo a destra nella fila dei moduli, con l'icona di Material dagli spigoli esterni arrotondati e le tue risposte D1a, D2a, D3a e D4a.
- **Concluso**: dissolvenza ai cambi netti, tasti della pillola centrati (`4.36-01`, `4.36-03`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
