# Feedback AIV

Versione **3.13**, Cartelle incluse/escluse. Questo è il documento di feedback di Codex;
le altre voci del documento Claude conservano il loro stato.

[Scarica AIV 3.13](https://github.com/Roccobot/AIV/releases/download/v3.13/AIV-3.13.apk)

Per ogni voce rispondi **OK**, **Da correggere** o **Non provato**, aggiungendo un commento
quando serve. Puoi inviarmi le risposte numerate direttamente in chat, in un giro solo.
Filigrana e Fluidifica sono già approvati e non richiedono un altro collaudo.

## 1. Aggiornamento e pagina delle impostazioni

Installa l'aggiornamento senza cancellare i dati. Apri `Impostazioni`, `Cartelle`,
`Cartelle incluse/escluse`. Deve essere selezionata `Modalità escluse`; le cartelle che avevi
nascosto devono essere ancora nell'elenco. Cerca nelle impostazioni `incluse`, `escluse` e
il nome di una cartella in elenco: la pagina deve essere raggiungibile dalla ricerca.

## 2. Modalità incluse e valori iniziali

Seleziona `Modalità incluse`. La lista iniziale deve autorizzare Camera, Screenshots e Movies
se presenti; controlla anche Screenshots in DCIM, se il tuo telefono la usa. Le modalità
griglia e lista devono mostrare soltanto cartelle autorizzate con almeno un'immagine o video.
Il FAB deve essere privo di `Mostra nascoste`. `Cartelle di sistema` deve continuare a mostrare
le cartelle del disco, con i filtri di sistema che hai scelto. Cerca un'immagine che esiste
soltanto in una cartella non autorizzata: deve rimanere fuori dai risultati.

## 3. Autorizzazione di una cartella con immagini

In `Cartelle di sistema`, tieni premuta una cartella non autorizzata e scegli `Autorizza
cartella`. Deve aprirsi `Aggiungi alle cartelle visualizzate`, con la spiegazione delle modalità
griglia e lista. Conferma: la riga deve segnare `autorizzata`, la cartella deve apparire nei due
elenchi e nelle impostazioni.

## 4. Autorizzazione di una cartella vuota

Ripeti su una cartella vuota. Deve esserci l'avviso enfatizzato che sarà visibile quando
conterrà almeno un'immagine o video. Conferma: deve comparire nella lista delle autorizzate
ma non nelle modalità griglia e lista. Aggiungici un'immagine; dopo l'indicizzazione e
l'aggiornamento dell'elenco deve apparire anche lì.

## 5. Rimozione e indipendenza delle liste

Rimuovi un'autorizzazione dalla pagina `Cartelle incluse/escluse`: la cartella deve sparire
dalle modalità griglia e lista. Passa a `Modalità escluse`: deve tornare visibile se non è
anche nell'elenco delle escluse. Alterna le modalità e riavvia l'app: liste e modalità scelta
devono restare salvate. Una cartella preautorizzata che hai rimosso deve restare rimossa.

## 6. Esclusione normale e prestito

In `Modalità escluse`, tieni premuta una cartella in modalità griglia o lista e conferma
`Nascondi`. Deve sparire, insieme alle eventuali sottocartelle. Nel FAB, `Mostra nascoste` deve
farla tornare temporaneamente, con il segno e l'opacità ridotta; `Mostra` deve rimostrarla
permanentemente. Se passi a `Modalità incluse` durante il prestito, questo deve terminare.

## 7. Tutte le cartelle allo stesso livello

Prepara un genitore X con almeno due sottocartelle, di cui una può essere vuota. Premi a lungo
su una sottocartella visibile in modalità griglia o lista. Deve esserci `Applica a tutte le
cartelle allo stesso livello`, con la seconda conferma che nomina X e avvisa che le nuove
cartelle saranno inizialmente visibili. Conferma: le sottocartelle attuali devono essere voci
separate nella lista delle escluse. Rimuovine una: deve tornare soltanto quella. Crea una nuova
sottocartella di X con un'immagine: deve essere visibile dopo l'indicizzazione.

## 8. Nascondi dalla vista di sistema

In `Modalità escluse`, apri `Cartelle di sistema` e premi a lungo su X, poi `Nascondi`:

- Senza sottocartelle: conferma della sola X; una sottocartella creata dopo deve essere visibile.
- Con una sottocartella: `Quali cartelle vuoi nascondere?`, con `Solo X` e `X e la sua sottocartella`.
- Con almeno due: stesso titolo, con `X e le sue sottocartelle`, `Solo le sottocartelle di X`, `Solo X`.

Prova i tre esiti su cartelle preparate apposta. `Solo X` deve lasciare visibili tutti i figli,
anche quelli creati dopo; la lista delle escluse deve indicare `Solo questa cartella`.
`Solo le sottocartelle di X` deve registrare i figli attuali separatamente e lasciare X visibile.
L'esclusione di X con le sottocartelle deve coprire tutto il ramo.

## 9. Prestito di un'esclusione della sola cartella

Nascondi `Solo X`, lasciando visibile un suo figlio con immagini. Accendi `Mostra nascoste`:
X deve avere il segno e l'opacità ridotta, il figlio deve conservare l'aspetto normale.

## 10. Copia, sposta e navigazione completa

In `Modalità incluse`, seleziona un'immagine e scegli `Copia` o `Sposta`: le destinazioni
ordinarie devono contenere soltanto le cartelle autorizzate. `Sfoglia tutte le cartelle...`
deve permettere di raggiungere anche le altre. Ripeti in `Modalità escluse`: le cartelle
nascoste devono mancare, salvo durante il prestito. Il cestino deve restare escluso.

## 11. Esporta e importa

Prepara almeno un'autorizzazione, un'esclusione ricorsiva e un'esclusione `Solo questa cartella`.
Esporta il file di impostazioni con `Cartelle incluse/escluse` selezionato. Cambia modalità e
aggiungi voci diverse alle due liste, poi importa il file: la modalità deve tornare quella
esportata; le liste devono contenere sia le voci del telefono sia quelle del file, senza doppioni.
Le esclusioni della sola cartella devono conservare il loro significato.

Se hai un file salvato con AIV 3.03 o precedente, importa la sua area delle cartelle mentre
sei in `Modalità incluse`: deve selezionare `Modalità escluse` e aggiungere le vecchie nascoste,
conservando le autorizzazioni del telefono. Le cartelle preautorizzate rimosse non devono
ricomparire dopo un'importazione o un riavvio.

## 12. Scheda SD, leggibilità e riscontro libero

Se usi una scheda SD, verifica che autorizzare o escludere una cartella sulla scheda non
modifichi l'omonima nella memoria interna. Prova la pagina e i popup nei due temi e con testo
ingrandito: tutti i comandi devono essere leggibili, scorrere se necessario e chiudersi
correttamente con `Annulla` o toccando fuori. Segnala anche comportamenti inattesi fuori da
queste prove, indicando il gesto e il risultato atteso.

## Risposta

Versione installata: ...
Telefono e versione Android: ...

1. ...
2. ...
3. ...
4. ...
5. ...
6. ...
7. ...
8. ...
9. ...
10. ...
11. ...
12. ...
