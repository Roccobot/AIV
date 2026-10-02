# Feedback AIV

Versione **3.38**: correzioni dal collaudo **3.37** (tablet, Impostazioni, residui).
[Apri il documento interattivo](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
La Release 3.38 è pubblicata: [v3.38](https://github.com/Roccobot/AIV/releases/tag/v3.38), con l'APK
[AIV-3.38.apk](https://github.com/Roccobot/AIV/releases/download/v3.38/AIV-3.38.apk).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.38** è stato consegnato e travasato il 2 ottobre 2026 (~18:34 Europe/Rome).
Le prove OK del 3.38 sono in archivio sotto; restano aperte le Non approvato con i residui.
Le decisioni 3.24 restano chiuse e **non** sono riproposte. Filigrana e Fluidifica restano
approvati. Questa è **manutenzione** del documento post-collaudo, non una nuova versione
di collaudo 3.39 (niente prove `3.39-xx` finché non c'è Release 3.39).

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti: Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link (Cmd+B/I/M/K).
Su mobile: striscia con i soli chip semaforo centrati; in editing solo Salva; Altro prima
di Prossimi passi (tieni premuto il FAB ⇥ per il pannello); tieni premuto Salva (dischetto)
per **Consegna e copie**. Su desktop: Altro in colonna laterale, con pulsante Consegna a
destra dell'allegato. **Etichette testuali** (se presenti) stanno prima dell'archivio.
I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.38 sono superate: 486 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet del 3.38 è stato consegnato.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.38-01 | Non approvato | Sfarfallio ancora presente in landscape. | Fix critico in 3.39. |
| 3.38-02 | Non approvato | Verticale ~70%; vuole sempre ~35%. | FAB fisso ~35% in 3.39. |
| 3.38-03 | Non approvato | Redesign totale rail + mockup. | Rail mockup in 3.39. |
| 3.38-04 | Non approvato | Empty state = contenuto mockup + testo. | Con 3.38-03 in 3.39. |
| 3.38-11 | Non approvato | Nessun miglioramento visto. | Tentativo mirato o defer. |

## 1. Editor tablet in orizzontale (≥1024)

Su tablet in **orizzontale** (larghezza ≥ 1024 dp): apri un'immagine e l'editor completo.
Lo schermo **non** deve sfarfallare né restare inutilizzabile. Ripeti aprendo anche l'editor
semplice. In verticale il comportamento precedente resta. Residuo collaudo 3.38: il
sfarfallio è **ancora presente**.

## 2. Menu FAB: larghezza su tablet

Apri il menu del FAB sulla home cartelle (o in una cartella) su tablet, in orizzontale e
in verticale. Il pannello deve occupare **sempre circa il 35%** della larghezza (il 45%
era troppo; in verticale oggi arriva circa al 70%). In orizzontale il collaudo 3.38 era ok.

## 3. Rail tablet: redesign per mockup

Su tablet (≥ 600), ridisegna la colonna laterale come nel mockup del collaudo 3.38:
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

## 5. Qualità Correggi/Rimuovi (riprova)

Su un orlo o una linea: valuta se resta più continuo. Allega prima/dopo se ancora debole.
Residuo collaudo 3.38: nessun miglioramento visto. Limiti noti: senza indizi il calcolo
non inventa dettagli.

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

- **3.39: entra** (confermato): fix flicker editor `3.38-01` (critico); FAB sempre ~35% `3.38-02`; redesign rail+empty state mockup `3.38-03`+`04` (un filone).
- **Prossima release (3.39 se c'è tempo, altrimenti 3.40)**: flash colori HEIC in sfoglio (neutro → vivido a fine animazione); indagare decode/color profile; esempio HEIC in attesa di ri-allegato.
- **Differito**: inpaint `3.38-11`; più sotto-pagine `3.27-03`.
- **Da reinserire come prove nel DF dopo Release 3.39** (non collaudati nel giro 3.38; non sono lavoro prodotto 3.39 ora): `3.13-02` header Modalità incluse; `3.13-08` lista sistema + avvisi.
- **Da decidere**: quanto insistire sull'inpaint rispetto ad accettare il limite euristico.
