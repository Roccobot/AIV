# Feedback AIV

Versione **4.38**: i pannelli nudi, senza il bordo d'accento, e le frecce su e giù ancora un poco più spesse.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.38 è pubblicata: [v4.38](https://github.com/Roccobot/AIV/releases/tag/v4.38), con l'APK
[AIV-4.38.apk](https://github.com/Roccobot/AIV/releases/download/v4.38/AIV-4.38.apk).
Commit prodotto su `main`: `f372ae0`, release dal commit `f372ae0` (SlimVer 4.38 / versionCode 327; APK 8.466.477 byte, digest `6f05e54d`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Le decisioni 3.24 restano chiuse e **non** sono riproposte.
Lati editor e HEIC restano chiusi. Giro **4.38**: due prove nuove. Giro **4.37**: `4.37-01`-`4.37-05` tutte OK.

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

Le verifiche automatiche della 4.38 sono superate: banco di prova completo (592 prove), controllo delle icone e compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.38-01 | Non provato | | Attendere il collaudo. |
| 4.38-02 | Non provato | | Attendere il collaudo. |

## 1. I pannelli nudi

Apri un menu qualunque, le `Informazioni` di un'immagine e una conferma, come `Svuota il cestino`, in tutti e due i temi: nessun pannello ha più il bordo d'accento. Si staccano dallo sfondo col solo colore della superficie, sopra la sfocatura e il velo. È la variante A del [mockup](https://claude.ai/artifact/CueMxi6gDkJqYbMVymeWPT). Le misure non cambiano, perché il bordo era disegnato sopra il pannello e non occupava spazio. Se cambi idea, il bordo torna com'era con una riga di codice.

## 2. Le frecce su e giù ancora un poco più spesse

Scorri una cartella con un gesto veloce: la freccia su o giù ha altri 0,3dp di tratto, come hai chiesto in `4.37-01`. La 4.37 era passata da 2 a 2,33 unità su 24, cioè 0,3dp; adesso è a 2,66.

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse e **non** sono riproposte.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| I pannelli nudi | 4.38-01 | Non provato | | Attendere il collaudo. |
| Le frecce su e giù ancora un poco più spesse | 4.38-02 | Non provato | | Attendere il collaudo. |
| Le frecce su e giù un poco più spesse | 4.37-01 | OK | Quasi bene, un altro 0,3dp non guasterebbe. | Ripresa in `4.38-02`. |
| Le frasi dei veli d'aiuto | 4.37-02 | OK | Tutto OK. | Archiviata. |
| L'Opacità di fabbrica al 40% | 4.37-03 | OK | Tutto OK. | Archiviata. |
| Estensione senza salto | 4.37-04 | OK | Tutto OK. | Archiviata. |
| Le linee dei terzi in Raddrizza | 4.37-05 | OK | Tutto OK. | Archiviata. |

## Prossimi passi

- **In collaudo**: pannelli nudi, frecce più spesse (`4.38-01`, `4.38-02`).
- **Prossima versione**: il modulo Disegno, prima fase (4.40: mano libera, segmenti, frecce, rettangoli, ellissi, colore e tratto), ultimo a destra nella fila dei moduli, con l'icona di Material dagli spigoli esterni arrotondati e le tue risposte D1a, D2a, D3a e D4a.
- **Concluso**: frasi dei veli, `Opacità` al 40%, `Estensione` senza salto, terzi in `Raddrizza` (`4.37-02`-`4.37-05`).
- **Chiuso, non riaperto**: lati editor `3.40-02`; HEIC `3.40-03`.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
