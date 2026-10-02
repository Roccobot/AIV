# Feedback AIV

Versione **3.37**: giro di verifica del layout tablet nativo (**3.31-3.36**) e delle
correzioni dal collaudo **3.25-3.30**.
[Apri il documento interattivo](https://aiv-feedback.roccobot-b90.workers.dev/feedback.html) oppure
APK: la Release 3.37 non è ancora pubblicata (niente `publish` in questo giro). Collauda
con la build di tip `main` oppure attendi la Release. L'ultima Release pubblica resta
[v3.36](https://github.com/Roccobot/AIV/releases/tag/v3.36).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Il giro **3.25-3.30** è stato consegnato e travasato il 2 ottobre 2026: le prove concluse senza
residuo sono in archivio. Qui restano le verifiche delle correzioni e il collaudo tablet.
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

Le verifiche automatiche della 3.37 sono superate: 482 prove, zero fallimenti, controllo delle 28 traduzioni e compilazione. Il collaudo sul telefono e sul tablet resta tuo.
Le decisioni del giro 3.24 restano chiuse nella sezione Decisioni.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 3.37-01 | Non provato | | Attendere il collaudo. |
| 3.37-02 | Non provato | | Attendere il collaudo. |
| 3.37-03 | Non provato | | Attendere il collaudo. |
| 3.37-04 | Non provato | | Attendere il collaudo. |
| 3.37-05 | Non provato | | Attendere il collaudo. |
| 3.37-06 | Non provato | | Attendere il collaudo. |
| 3.37-07 | Non provato | | Attendere il collaudo. |
| 3.37-08 | Non provato | | Attendere il collaudo. |
| 3.37-09 | Non provato | | Attendere il collaudo. |
| 3.31-01 | Non provato | | Attendere il collaudo. |
| 3.32-01 | Non provato | | Attendere il collaudo. |
| 3.33-01 | Non provato | | Attendere il collaudo. |
| 3.34-01 | Non provato | | Attendere il collaudo. |
| 3.35-01 | Non provato | | Attendere il collaudo. |
| 3.36-01 | Non provato | | Attendere il collaudo. |

## 1. Intestazione in Modalità incluse (riprova)

Installa AIV 3.37 senza cancellare i dati. Apri la home in `Modalità incluse` (poche cartelle).
Scorri verso il basso finché l'intestazione si chiude, poi trascina verso l'alto anche sullo
spazio vuoto sotto l'elenco: l'intestazione deve riaprirsi. Ripeti in griglia e in lista.
In `Modalità escluse` l'elenco lungo deve continuare a comportarsi come prima.

## 2. Nascosta: sottocartella in elenco e dicitura

In `Modalità escluse`, da Cartelle di sistema nascondi una cartella X **con la sua unica
sottocartella**. Apri Impostazioni, Cartelle, Cartelle incluse/escluse: padre e figlia devono
comparire nell'elenco. In Cartelle di sistema le righe nascoste devono portare la dicitura
`nascosta`. Controlla anche dopo un riavvio dell'app.

## 3. Gettoni modalità e grassetto nei testi

In Impostazioni, Cartelle, Cartelle incluse/escluse: i gettoni devono essere gettoni **normali**
(stesso font degli altri gettoni dell'app), senza grassetto forzato. Nel paragrafo descrittivo
sopra, **Modalità incluse** e **Modalità escluse** devono apparire in grassetto, non tra apici.

## 4. Lista cartelle: ×, conferma e nome a capo

Nella stessa pagina, ogni riga (incluse o escluse) deve mostrare una **×** a destra al posto di
'Rimuovi' / 'Rimuovi autorizzazione'. Toccala: deve chiedere 'Vuoi davvero rimuovere la cartella
dalla lista?'. Il nome della cartella ha più spazio; se è camelCase senza spazi (es. VideoDownloader)
può spezzarsi su minuscola→maiuscola; se non sta in due righe, ellissi in fondo.

## 5. Selezione multipla: un solo rettangolo arrotondato

In Cartelle di sistema, seleziona almeno tre file consecutivi. La evidenziazione deve avere gli
angoli arrotondati **solo** sul primo e sull'ultimo della serie consecutiva, come un unico
rettangolo continuo (niente 'ondine' intermedie).

## 6. Aggiungi cartella: già in lista

In Modalità incluse, Impostazioni → Cartelle → Aggiungi cartella. Nello sfoglio, le cartelle già
autorizzate devono mostrare un indicatore 'Già in lista' e non devono poter essere ri-aggiunte.

## 7. Avviso area troppo grande: sparisce

Con Correggi/Rimuovi, forza l'avviso di area troppo grande. L'avviso deve sparire se lo tocchi,
se tocchi altro, oppure dopo al massimo **10 secondi**.

## 8. Lentino: soglia e pennello dentro il cerchio

Con Correggi/Rimuovi, aumenta la dimensione del pennello: il lentino deve sparire **prima** di
quanto faceva in 3.30. Finché è attivo, il pennello ingrandito non deve uscire dal tondo del
lentino.

## 9. Qualità Correggi/Rimuovi su orli e linee

Su foto o illustrazioni reali, rimuovi un piccolo difetto **a ridosso di una linea o di un orlo**
(come negli screenshot del giro 3.30). Valuta se l'orlo resta continuo e se non compaiono macchie.
Allega prima/dopo nei casi deboli. Limiti noti: senza indizi il calcolo non inventa dettagli.

## 10. Visualizzatore tablet (3.31)

Su un tablet (o emulatore largo ≥ 600 dp): apri un'immagine. Da 600 dp il layout deve seguire il
mockup (info a lato da 1024). Sotto 1024 l'info non deve rubare lo spazio principale. Confronta
con [tablet](https://roccobot.github.io/AIV/tablet.html).

## 11. Cartelle tablet a due colonne (3.32)

Larghezza ≥ 600: home cartelle a due colonne come da mockup. Griglia/lista/albero restano
usabili; il FAB e l'intestazione non devono spezzarsi.

## 12. Impostazioni tablet (3.33)

≥ 600: indice e pagina impostazioni sul tablet (indice a lato da 800/1024 secondo le soglie).
Naviga almeno tre sotto-pagine; Indietro e ricerca devono restare coerenti.

## 13. Ricerca e editor strumenti a lato (3.34)

≥ 600: ricerca duale e, nell'editor completo, strumenti a lato da 1024. Verifica che i moduli
restino raggiungibili e che il palco non risulti schiacciato.

## 14. Cestino, Cronologia, TREE e dialoghi (3.35)

≥ 600: Cestino (gallery + dettagli a lato), Cronologia (gruppi data), TREE (cartelle a lato),
dialoghi centrati ~520 dp e Copia/sposta più larghi da 600. Controlla che non torni il layout
telefono 'stirato'.

## 15. Dimensioni/filigrana e schede Guida (3.36)

Da 1024: Dimensioni e filigrana a lato. Guida e scelta editor come sheet più larghe e
scorrevoli. Su telefono il comportamento precedente deve restare intatto.

## Decisioni da concordare

Le tre decisioni del giro 3.24 sono già chiuse e applicate dove previsto. Restano nel
documento interattivo con gli stessi identificatori, così la bozza cloud e i JSON vecchi
continuano a importarsi. Non è obbligatorio rispondervi di nuovo.

- **Impostazioni**: [confronto prima/dopo](Settings-proposal.md). Decisione 3.24: Applica
  la proposta (applicata dalla 3.27).
- **Tablet**: [mockup interattivi](https://roccobot.github.io/AIV/tablet.html).
  Decisione 3.24: Approvo la direzione. Implementazione nativa 3.31-3.36 da collaudare sopra.
- **Diagnosi PNG**: Decisione 3.24: Le modifiche si vedono. Non richiedere di nuovo.

Queste scelte sono separate dagli esiti delle prove. Gli stili restano in attesa del tuo
via libera; sfogliatore Web e Play Store restano sospesi.

## Riscontri conclusi

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Filigrana | 3.03 | OK | Il 2026-09-30 l'utente conferma che funziona come previsto. | Chiusa. |
| Fluidifica | 3.03 | OK | Il 2026-09-30 l'utente dichiara lo strumento ufficialmente completato. | Chiusa. |
| Giro cartelle / PNG / PSD / Correggi base (3.13-3.24 OK) | 3.24 | OK | Giro 3.24. | Archiviata. |
| d-settings-order / d-tablet-layout / d-png-fullscreen | 3.24 | Chiuse | Scelte confermate anche nel giro 3.25-3.30. | Chiuse. |
| Prove 3.25-3.30 OK senza residuo | 3.30 | OK | Giro 2 ottobre 2026. | Archiviata. |
| Header Modalità incluse (3.13-02 / 3.25-01) | 3.30 | Non approvato | Riprovato; fix in 3.37. | Ricollegata a 3.37-01. |
| Nascondi figli (3.13-08 / 3.26) | 3.30 | Non approvato | Riprovato; fix/verifica in 3.37. | Ricollegata a 3.37-02. |
| Gettoni/grassetto (3.26-03) | 3.30 | Non approvato | Corretto in 3.37. | Ricollegata a 3.37-03. |
| Lista × / camelCase (campo libero B) | 3.30 | Richiesta | Fatto in 3.37. | Ricollegata a 3.37-04. |
| Selezione arrotondamenti (3.27-01) | 3.30 | OK+ritocco | Fatto in 3.37. | Ricollegata a 3.37-05. |
| Già in lista (3.27-02) | 3.30 | OK+ritocco | Fatto in 3.37. | Ricollegata a 3.37-06. |
| Toast 10 s (3.25-02) | 3.30 | Accettabile | Fatto in 3.37. | Ricollegata a 3.37-07. |
| Lentino soglia (3.26-06) | 3.30 | Accettabile | Ritocco in 3.37. | Ricollegata a 3.37-08. |
| Qualità Correggi/Rimuovi (3.24-04 / 3.30-01) | 3.30 | Non approvato | Ulteriore ritocco in 3.37. | Ricollegata a 3.37-09. |
| Layout tablet assente (3.30-02) | 3.30 | OK | Nativo 3.31-3.36. | Ricollegata a 3.31-3.36. |
