# Feedback AIV

Versione **3.38**: correzioni dal collaudo **3.37** (tablet, Impostazioni, documento di
feedback, residui).
[Apri il documento interattivo](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html).
La Release 3.38 non è pubblicata finché non chiedi `publish`. L'ultima Release pubblica resta
[v3.36](https://github.com/Roccobot/AIV/releases/tag/v3.36) se non ne è uscita un'altra.

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.37** è stato consegnato e travasato il 2 ottobre 2026. Le decisioni 3.24 restano
chiuse e **non** sono riproposte. Filigrana e Fluidifica restano approvati.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti puoi usare Grassetto, Corsivo, Codice inline (`` ` `` / ⌘M) e Link, oppure
Cmd+B/I/M/K. Su mobile: striscia Compilate in alto, menu hamburger (login GitHub + Altro),
e in editing solo Salva. Su desktop: Altro flottante a sinistra.

I campi Telefono e Tablet restano al cambio versione; Altro e allegati liberi si azzerano.
`Invia` rende leggibile il giro senza avviare lavori.

Le verifiche automatiche della 3.38 sono superate: 486 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet resta tuo.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.38-01 | Non provato | | Attendere il collaudo. |
| 3.38-02 | Non provato | | Attendere il collaudo. |
| 3.38-03 | Non provato | | Attendere il collaudo. |
| 3.38-04 | Non provato | | Attendere il collaudo. |
| 3.38-05 | Non provato | | Attendere il collaudo. |
| 3.38-06 | Non provato | | Attendere il collaudo. |
| 3.38-07 | Non provato | | Attendere il collaudo. |
| 3.38-08 | Non provato | | Attendere il collaudo. |
| 3.38-09 | Non provato | | Attendere il collaudo. |
| 3.38-10 | Non provato | | Attendere il collaudo. |
| 3.38-11 | Non provato | | Attendere il collaudo. |

## 1. Editor tablet in orizzontale (≥1024)

Su tablet in **orizzontale** (larghezza ≥ 1024 dp): apri un'immagine e l'editor completo.
Lo schermo **non** deve sfarfallare né restare inutilizzabile. Ripeti aprendo anche l'editor
semplice. In verticale il comportamento precedente resta.

## 2. Menu FAB: larghezza su tablet

Apri il menu del FAB sulla home cartelle (o in una cartella) su tablet. Il pannello **non**
deve occupare tutta la larghezza: al massimo circa **il 45%**. Confronta col caso del giro
3.37 (`tablet2.jpg`).

## 3. Rail Cartelle: icona e spazio

Su tablet (≥ 600): nella colonna cartelle, in alto a sinistra non deve andare a capo la
parola 'Cartelle' ,  c'è un'**icona** cartella al suo posto, con più aria verso i tasti
cerca/cestino/impostazioni.

## 4. Senza cartella scelta: contenuto centrato

Su tablet, senza cartella di avvio e senza aver ancora toccato una cartella: lo spazio
principale **non** è vuoto. Deve mostrare centrati titolo/invito (come il senso
dell'intestazione mobile), non solo uno schermo bianco.

## 5. Griglia: più aria fra le miniature

Nella griglia file su tablet: più spazio fra le righe e un poco fra le immagini rispetto
alla 3.37 (miniature leggermente più piccole vanno bene).

## 6. Cestino a tutta larghezza

Su tablet ≥ 600: apri il Cestino. La griglia occupa **tutta** la larghezza; **niente**
colonna dettagli a lato.

## 7. Impostazioni: apici → grassetto (tutte le pagine)

Apri almeno tre sotto-pagine Impostazioni (non solo Cartelle): termini che erano tra
apici (es. Scarica, Esporta/Converti, Modifica, Ripristina) appaiono in **grassetto senza
apici**. L'eccezione **'FAB'** resta tra apici. Eventuali `ATTENZIONE` diventano
**Attenzione** (grassetto, sola iniziale).

## 8. Indicatore già inclusa

In Modalità incluse → Aggiungi cartella: le cartelle già in lista mostrano **`già inclusa`**
(tutto minuscolo), non 'Già in lista', e non si possono ri-aggiungere.

## 9. Lentino: soglia ancora più bassa

Con Correggi/Rimuovi, aumenta il pennello: il lentino sparisce **prima** di quanto faceva
in 3.37. Finché è attivo, il pennello ingrandito resta dentro il tondo.

## 10. Documento di feedback: UX mobile e Altro

Apri questo documento sul telefono: striscia fissa in alto con Compilate n/N e conteggi
semaforo (icone, non emoji); hamburger a destra apre login GitHub + Altro a tutto schermo.
Mentre scrivi in un campo, resta visibile solo Salva (gli altri controlli tornano al blur
o ~5 s dopo un salvataggio). Su desktop: Altro flottante a sinistra. I link nelle prove
sono cliccabili, non markdown letterale.

## 11. Qualità Correggi/Rimuovi (riprova)

Su un orlo o una linea: valuta se resta più continuo che in 3.37. Allega prima/dopo se
ancora debole. Limiti noti: senza indizi il calcolo non inventa dettagli.

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
| Prove 3.37 OK senza residuo | 3.37 | OK | 3.37-01..07 (con residui sotto). | Archiviata dove OK. |
| UX documento mobile (note A 3.30) | 3.38 | Da collaudare | Era differita per errore; implementata. | 3.38-10. |
| Apici Impostazioni globali | 3.38 | Da collaudare | Solo Cartelle in 3.37; sweep completo. | 3.38-07. |
| Tablet editor / FAB / rail / cestino | 3.38 | Da collaudare | Note B e 3.31/3.35 del 3.37. | 3.38-01..06. |
