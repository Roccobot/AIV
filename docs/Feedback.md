# Feedback AIV

Versione **3.39**: collaudo del trio tablet (sfarfallio editor, FAB ~35%, rail + empty
state) e riprove storiche `3.13-02` / `3.13-08`.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
La Release 3.39 è pubblicata: [v3.39](https://github.com/Roccobot/AIV/releases/tag/v3.39), con l'APK
[AIV-3.39.apk](https://github.com/Roccobot/AIV/releases/download/v3.39/AIV-3.39.apk).
Commit su `main`: `b3600a5` (SlimVer 3.39 / versionCode 297).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.38** è stato consegnato e travasato il 2 ottobre 2026 (~18:34 Europe/Rome).
Le prove OK del 3.38 sono in archivio sotto. Le decisioni 3.24 restano chiuse e **non** sono
riproposte. Filigrana e Fluidifica restano approvati.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ per il pannello); tieni premuto Salva (dischetto)
per **Consegna e copie**. Su desktop: Altro in colonna laterale, con pulsante Consegna a
destra dell'allegato. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.39 sono superate: 486 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet resta tuo.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.39-01 | Non provato | | Attendere il collaudo. |
| 3.39-02 | Non provato | | Attendere il collaudo. |
| 3.39-03 | Non provato | | Attendere il collaudo. |
| 3.39-04 | Non provato | | Attendere il collaudo. |
| 3.13-02 | Non provato | | Riprova storica; attendere il collaudo. |
| 3.13-08 | Non provato | | Riprova storica; attendere il collaudo. |

## 1. Editor tablet in orizzontale (≥1024)

Su tablet in **orizzontale** (larghezza ≥ 1024 dp): apri un'immagine e l'editor completo.
Lo schermo **non** deve sfarfallare né restare inutilizzabile. Con gli strumenti a lato
(`editorBeside`) **non** deve forzare il blocco in verticale. Ripeti aprendo anche l'editor
semplice. In verticale il comportamento precedente resta. Residuo 3.38: lo sfarfallio era
ancora presente; qui si verifica il fix.

## 2. Menu FAB: larghezza su tablet

Apri il menu del FAB sulla home cartelle (o in una cartella) su tablet, in orizzontale e
in verticale. Il pannello deve occupare **sempre circa il 35%** della larghezza (il 45%
era troppo; in verticale in 3.38 arrivava circa al 70%).

## 3. Rail tablet: redesign per mockup

Su tablet (≥ 600), la colonna laterale deve seguire il mockup del collaudo 3.38:
niente testo né icona **Cartelle**; in cima solo **Cerca**, distanziata dal bordo, con
placeholder dinamico ('Cerca nelle cartelle' / 'Cerca in *X*' con il nome cartella in
grassetto); il campo è centrato in verticale sul **tondo** della lente; Cestino e
Impostazioni in basso con etichetta testuale; elenco cartelle di default verso il basso,
con maniglia per spostarlo su/giù se c'è spazio.

## 4. Senza cartella scelta: empty state mockup

Su tablet, senza cartella di avvio e senza aver ancora toccato una cartella: lo spazio
principale mostra il contenuto del mockup (stessa formattazione e stessi link della
versione telefono), più in basso, un po' distanziato, il testo **Tocca una cartella per
iniziare**.

## 5. Intestazione in Modalità incluse (riprova)

Installa AIV 3.39 senza cancellare i dati. Apri la home in `Modalità incluse` (poche cartelle).
Scorri verso il basso finché l'intestazione si chiude, poi trascina verso l'alto anche sullo
spazio vuoto sotto l'elenco: l'intestazione deve riaprirsi. Ripeti in griglia e in lista.
In `Modalità escluse` l'elenco lungo deve continuare a comportarsi come prima.

## 6. Nascosta: elenco, dicitura e avvisi (riprova)

In `Modalità escluse`, da Cartelle di sistema nascondi una cartella X **con la sua unica
sottocartella**. Apri Impostazioni, Cartelle, Cartelle incluse/escluse: padre e figlia devono
comparire nell'elenco. In Cartelle di sistema le righe nascoste devono portare la dicitura
`nascosta`. Controlla anche dopo un riavvio dell'app.

Apri di nuovo gli avvisi di nascondere/mostrare (anche in Modalità incluse, se applicabile):
`Annulla` a sinistra, azione principale a destra; se c'è una terza azione (es. 'Applica a
tutte le cartelle allo stesso livello'), allineata a sinistra e chiaramente tappabile.
Niente controlli che fluttuano in mezzo al nulla.

## Etichette testuali

### e-folders-tablet-pick · Empty state tablet
Tocca una cartella per iniziare.

### e-folders-rail-search · Cerca rail (senza cartella)
Cerca nelle cartelle

### e-folders-rail-search-in · Cerca rail (in cartella)
Cerca in %1$s

### e-folders-rail-move · Maniglia elenco cartelle
Sposta elenco cartelle

## Decisioni da concordare

Nessuna decisione aperta in questo giro. Le tre del 3.24 restano chiuse (impostazioni =
Applica la proposta; tablet = Approvo la direzione; PNG = Le modifiche si vedono) e sono
in archivio sotto.

## Riscontri conclusi

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Filigrana | 3.03 | OK | 2026-09-30 | Chiusa. |
| Fluidifica | 3.03 | OK | 2026-09-30 | Chiusa. |
| Decisioni 3.24 | 3.24 | Chiuse | Riconfermate; non riproposte. | Chiuse. |
| Prove 3.37 OK senza residuo | 3.37 | OK | 3.37-01..07 (con residui assorbiti). | Archiviata. |
| UX documento (note A 3.30 + chat) | 3.38 | In codice | Striscia, Altro, overlay, shell: non è prova di collaudo app. | Manutenzione DF. |
| Griglia tablet più aria | 3.38-05 | OK | Tutto OK. | Archiviata. |
| Cestino a tutta larghezza | 3.38-06 | OK | Tutto OK; chiude 3.35-01. | Archiviata. |
| Apici → grassetto Impostazioni | 3.38-07 | OK | Tutto OK; chiude residuo 3.26-03 / 3.37-03. | Archiviata. |
| Indicatore già inclusa | 3.38-08 | OK | Tutto OK. | Archiviata. |
| Lentino soglia più bassa | 3.38-09 | OK | Tutto OK; chiude 3.37-08. | Archiviata. |

## Prossimi passi

- **3.40 (candidato)**: flash colori HEIC in sfoglio (neutro → vivido a fine animazione); indagare decode/color profile; esempio in tools `.memo/files/aiv-heic-color-flash-example.heic` (originale `IMG_20261002_204315.HEIC`).
- **Differito**: inpaint `3.38-11` / `3.24-04` / `3.30-01` / `3.37-09`; più sotto-pagine Impostazioni `3.27-03`.
- **Da decidere**: quanto insistire sull'inpaint rispetto ad accettare il limite euristico.
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
