# Feedback AIV

Versione **3.30**: giro cumulativo delle correzioni e dei ritocchi dalla **3.25** alla **3.30**.
[Apri il documento interattivo](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html) oppure
[scarica AIV 3.30](https://github.com/Roccobot/AIV/releases/download/v3.30/AIV-3.30.apk).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro precedente (fino alla 3.24) è stato consegnato e travasato il 1 ottobre 2026: le prove
concluse stanno in archivio. Qui restano solo le verifiche del prodotto attuale (3.25-3.30).
Filigrana e Fluidifica restano approvati. Gli esiti si aggiornano dopo la consegna del giro
completo; le risposte nella bozza privata non sono pubblicate automaticamente.

Nel documento interattivo scegli **Tutto OK**, **Accettabile** o **Non approvato**;
nessuna scelta significa **Non provato**. Un secondo clic sulla scelta la cancella.
Nei commenti delle verifiche, delle decisioni e delle osservazioni puoi usare Grassetto, Corsivo e Link, oppure Cmd+B/I/K (Ctrl+B/I/K sulle altre tastiere). I campi mostrano direttamente la formattazione, con più righe disponibili e altezza regolabile. Il testo viene conservato come Markdown; i link aprono nuove schede.

Puoi aggiungere commenti e allegati (PNG, JPG, WebP, GIF, SVG e ZIP), anche trascinandoli sulla voce. Gli stessi allegati sono disponibili in Qualsiasi altra cosa; gli ZIP restano interi e possono essere scaricati. Le voci con un esito, un commento o un allegato sono evidenziate: verde per Tutto OK, ambra per Accettabile, rosso per Non approvato. Commenti e allegati senza esito hanno evidenza neutra; le decisioni con una scelta o un commento sono evidenziate allo stesso modo. I contatori continuano a misurare soltanto gli esiti. Il salvataggio avviene nel cloud privato: accedi con GitHub per continuare da un altro dispositivo.
Attendi **Salvato nel cloud**; esporta il JSON per conservarne una copia con gli allegati.
I campi Telefono e Tablet mantengono separati modello e versione Android. `Invia` rende leggibile il giro senza avviare lavori. Puoi modificarlo e inviare di nuovo;
quando vuoi la presa in carico, chiedi in chat di leggere l'ultimo feedback. Il JSON
manuale resta disponibile come copia o alternativa. L'agente aggiorna gli esiti dopo il tuo giro completo. Le nuove release aggiungono prove
senza togliere quelle aperte; gli identificatori mantengono le risposte già salvate.

Le verifiche automatiche della 3.30 sono superate: 474 prove, zero fallimenti,
controllo delle 28 traduzioni e compilazione. Il layout tablet nativo Android **non è ancora
in questa build**: i mockup restano la guida approvata; sul telefono verifica solo le voci sotto.
Le decisioni del giro 3.24 (impostazioni, tablet, PNG) sono già chiuse: le trovi ancora nella
sezione Decisioni per consultazione; non serve ripeterle se la bozza cloud le conserva.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.25-01 | Non provato | | Attendere il collaudo. |
| 3.25-02 | Non provato | | Attendere il collaudo. |
| 3.25-03 | Non provato | | Attendere il collaudo. |
| 3.26-01 | Non provato | | Attendere il collaudo. |
| 3.26-02 | Non provato | | Attendere il collaudo. |
| 3.26-03 | Non provato | | Attendere il collaudo. |
| 3.26-04 | Non provato | | Attendere il collaudo. |
| 3.26-05 | Non provato | | Attendere il collaudo. |
| 3.26-06 | Non provato | | Attendere il collaudo. |
| 3.26-07 | Non provato | | Attendere il collaudo. |
| 3.27-01 | Non provato | | Attendere il collaudo. |
| 3.27-02 | Non provato | | Attendere il collaudo. |
| 3.27-03 | Non provato | | Attendere il collaudo. |
| 3.30-01 | Non provato | | Attendere il collaudo. |
| 3.30-02 | Non provato | | Attendere il collaudo. |

## 1. Intestazione in Modalità incluse

Installa AIV 3.30 senza cancellare i dati. Apri la home in `Modalità incluse` (poche cartelle).
Scorri verso il basso finché l'intestazione si chiude, poi trascina verso l'alto anche sullo
spazio vuoto sotto l'elenco: l'intestazione deve riaprirsi. Ripeti in griglia e in lista.
In `Modalità escluse` l'elenco lungo deve continuare a comportarsi come prima.

## 2. Avviso area troppo grande sopra la bottomsheet

Apri una copia di prova nell'editor completo, modulo Dettaglio, strumento Correggi/Rimuovi.
Dipingi una selezione molto ampia e premi Applica: deve comparire l'avviso che l'area è
troppo grande. L'avviso deve stare **sopra** la bottomsheet (toast sul palco), non solo
dentro il corpo della scheda. Selezione e immagine devono restare intatte.

## 3. Aree un po' più ampie accettate

Con Correggi/Rimuovi, ripeti con aree medie (più grandi di una macchia piccola, ma non
enorme): Applica deve accettarle quando restano sotto il nuovo limite. Solo le selezioni
davvero eccessive devono produrre l'avviso. Indica in commento se il limite ti sembra
ancora stretto o troppo largo.

## 4. Nascosta: cartella e sottocartelle in elenco e albero

In `Modalità escluse`, da Cartelle di sistema nascondi una cartella X **con le sue
sottocartelle**. Apri Impostazioni, Cartelle, Cartelle incluse/escluse: i figli presenti
devono comparire nell'elenco. In Cartelle di sistema le righe nascoste devono portare la
dicitura `nascosta`. Controlla anche dopo un riavvio dell'app.

## 5. Scheda Nascondi: Annulla, azione e tasti pieni

Apri di nuovo `Quali cartelle vuoi nascondere?` su una cartella con almeno due figli.
`Annulla` deve stare a sinistra; l'azione principale a destra. Le scelte intermedie
(`Solo X`, `Solo le sottocartelle`, `X e le sue sottocartelle`) devono essere tasti pieni
a larghezza utile, allineati a sinistra, non semplici righe di testo. Chiudi con Annulla
senza applicare se stai solo controllando la disposizione.

## 6. Gettoni Modalità incluse / escluse

In Impostazioni, Cartelle, Cartelle incluse/escluse: i gettoni devono leggere
`Modalità incluse` e `Modalità escluse` **in grassetto semibold**, senza apici o virgolette
attorno al nome. Alterna i due gettoni: elenchi e testi devono seguire la modalità scelta.

## 7. Applica a tutte e testo sulle nuove cartelle

In `Modalità escluse`, tieni premuta una sottocartella in griglia o lista quando il genitore
ha almeno un'altra directory. Deve esserci `Applica a tutte le cartelle allo stesso livello`
come tasto tonale a tutta larghezza, allineato a sinistra col testo principale. Nella seconda
conferma il testo deve parlare di **nuove cartelle create** dentro il genitore (non la sola
parola inglese create). Conferma o Annulla a scelta; se confermi, verifica che le sorelle
diventino voci separate nell'elenco delle nascoste.

## 8. Promemoria selezione pendente tappabile

Nell'editor, con Correggi/Rimuovi, lascia una selezione verde senza Applica. Passa a un altro
modulo o tenta Salva: deve comparire il promemoria di applicare o cancellare la selezione.
Tocca il toast/snackbar: deve riportarti a Correggi/Rimuovi con la selezione ancora lì.
Cancella o Applica per chiudere la prova.

## 9. Lentino: pennello dentro il cerchio

Con Correggi/Rimuovi scegli un pennello piccolo così compare il lentino. Mentre dipingi,
il cerchio del pennello ingrandito nella lente non deve mai uscire dal tondo del lentino.
Prova anche pennelli un po' più grandi al confine in cui il lentino è ancora attivo.

## 10. Autorizza e Nascondi come tasti pieni in fondo

In Cartelle di sistema, in Modalità incluse tieni premuta una cartella non ancora autorizzata:
nella bottomsheet `Autorizza cartella` deve essere un tasto pieno (tonale) nel piè di pagina
della scheda, non solo una riga di menu. In Modalità escluse ripeti con `Nascondi`: stesso
trattamento di tasto pieno in fondo. Annulla deve restare raggiungibile.

## 11. Selezione multipla in Cartelle di sistema

Apri Cartelle di sistema. Tieni premuto su un'immagine o un video: deve aprirsi la stessa
scheda di selezione della griglia (PickSheet), non il vecchio riquadro su un solo file.
Seleziona almeno due media; il FAB della casa deve sparire mentre la scheda è aperta.
Indietro oppure `Nessuno` devono azzerare la selezione. Cambia cartella: la selezione
deve perdersi.

## 12. Aggiungi cartella in Impostazioni

In Modalità incluse apri Impostazioni, Cartelle, Cartelle incluse/escluse. Deve esserci
`Aggiungi cartella`. Toccalo: si apre l'albero delle destinazioni; scegli una cartella e
autorizza. Deve comparire nell'elenco delle autorizzate. Se era vuota, resta in elenco ma
non in griglia/lista finché non ha media. Rimuovila dall'elenco per chiudere pulito se
era solo di prova.

## 13. Ordine delle impostazioni

Apri Impostazioni. Verifica: la copia del percorso della lista file sta sotto Cartelle,
Opzioni di visualizzazione, Lista. `Rinomina e download` sta in `Modifica e protezione
dei file` (non più solo sotto Editor). In fondo esiste la sezione `Gestione dell'app` con
miniature memorizzate, Esporta/importa e Ripristina gli avvisi. Cerca `rinomina`,
`percorso` e `miniature`: le voci devono restare raggiungibili.

## 14. Qualità Correggi/Rimuovi su vestiti e pattern

Su foto reali, correggi piccoli difetti su tessuti, orli di vestiti, linee spezzate e
pattern ripetuti. Valuta raccordo, continuità dell'orlo e assenza di macchie o ripetizioni
innaturali dopo aver ingrandito. Il calcolo è migliorato (raggio adattivo, area campione
più ampia, isofote/struttura). Non può inventare dettagli dove mancano indizi: per i casi
deboli allega originale e risultato. Questa è la prova critica del giro.

## 15. Layout tablet ancora assente in questa build

Questa 3.30 **non** include ancora il layout tablet nativo Android. Sul telefono le voci
sopra bastano. Se hai un tablet a disposizione, nota soltanto che l'app resta con il layout
telefono (mockup approvati: [tablet](https://roccobot.github.io/AIV/tablet.html)); non
aspettarti la doppia colonna cartelle/contenuto. Segnala in commento se vuoi dare priorità
all'implementazione nativa nel prossimo giro.

## Decisioni da concordare

Le tre decisioni del giro 3.24 sono già chiuse e applicate dove previsto. Restano nel
documento interattivo con gli stessi identificatori, così la bozza cloud e i JSON vecchi
continuano a importarsi. Non è obbligatorio rispondervi di nuovo.

- **Impostazioni**: [confronto prima/dopo](Settings-proposal.md). Decisione 3.24: Applica
  la proposta (applicata dalla 3.27). Verifica pratica: voce 3.27-03.
- **Tablet**: [mockup interattivi](https://roccobot.github.io/AIV/tablet.html).
  Decisione 3.24: Approvo la direzione. Implementazione nativa ancora da fare (voce 3.30-02).
- **Diagnosi PNG**: Decisione 3.24: Le modifiche si vedono. Non richiedere di nuovo.

Queste scelte sono separate dagli esiti delle prove. Gli stili restano in attesa del tuo
via libera; sfogliatore Web e Play Store restano sospesi.

## Riscontri conclusi

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Filigrana | 3.03 | OK | Il 2026-09-30 l'utente conferma che funziona come previsto. | Chiusa. |
| Fluidifica | 3.03 | OK | Il 2026-09-30 l'utente dichiara lo strumento ufficialmente completato. | Chiusa. |
| Giro cartelle 3.13 (prove 01, 03-07, 09-12) | 3.24 | OK / ritocchi | Giro 3.24: Tutto OK; ritocchi UI confluiti in 3.25-3.27. | Archiviata; ritocchi in questo giro. |
| PNG / PSD / documento (3.14) | 3.24 | OK | Giro 3.24. | Archiviata. |
| Correggi/Rimuovi base (3.24-01, 03, 06-08) | 3.24 | OK | Giro 3.24. | Archiviata; qualità in 3.30-01. |
| Header Modalità incluse (3.13-02) | 3.24 | Non approvato | Fix in 3.25. | Ricollegata a 3.25-01. |
| Nascondi figli in elenco (3.13-08) | 3.24 | Non approvato | Fix in 3.26. | Ricollegata a 3.26-01/02. |
| Qualità Correggi/Rimuovi (3.24-04) | 3.24 | Non approvato | Migliorata in 3.29/3.30. | Ricollegata a 3.30-01. |
| Lentino pennello (3.24-02) | 3.24 | Accettabile | Ritocco zoom in 3.26. | Ricollegata a 3.26-06. |
| d-settings-order | 3.24 | Applica la proposta | Applicata in 3.27. | Chiusa. |
| d-tablet-layout | 3.24 | Approvo la direzione | Mockup ok; nativo da fare. | Chiusa come direzione. |
| d-png-fullscreen | 3.24 | Le modifiche si vedono | Diagnosi chiusa. | Chiusa. |
