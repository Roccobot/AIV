# Feedback AIV

Versione **4.97**: gli stili di casa sono dieci, nuovi, e col tocco lungo si sommano; il segmento del tasto `Spessore` è più lungo.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.97 è pubblicata: [v4.97](https://github.com/Roccobot/AIV/releases/tag/v4.97), con l'APK
[AIV-4.97.apk](https://github.com/Roccobot/AIV/releases/download/v4.97/AIV-4.97.apk).
Commit prodotto su `main`: `cbd6f3f`, release dal commit `cbd6f3f` (SlimVer 4.97 / versionCode 354; APK 11.018.413 byte, digest `43dcbc6b`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.97**: tre prove, i dieci stili di casa, il tocco lungo che somma e il segmento di `Spessore`. Giro **4.96**: tre prove OK, le due etichette confermate, gli stili da rifare.

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

Le verifiche automatiche della 4.97 sono superate: banco di prova completo (686 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.97-01 | Non provato | | Attendere il collaudo. |
| 4.97-02 | Non provato | | Attendere il collaudo. |
| 4.97-03 | Non provato | | Attendere il collaudo. |

## 1. I dieci stili di casa

Apri un'immagine nell'editor completo e il modulo Stili: gli stili di casa sono dieci, `Roccobot` per primo e poi, in ordine alfabetico, `Caldo`, `Cieli profondi`, `Contrasto`, `Freddo`, `Nitido`, `Ombre aperte`, `Pellicola`, `Tenue`, `Vivido`. `Bianco e nero` e gli altri di prima non ci sono più; i tuoi stili salvati restano sotto. Tocca ognuno e guarda se fa quello che il nome promette; `Roccobot` tocca un poco tutto: ombre aperte e luci trattenute, colore vivace, cieli più profondi, nitidezza, un filo di grana e di vignettatura, una curva a S leggera.

Letture mie: ognuno tranne `Roccobot` lavora su un asse solo e con valori moderati, perché due o tre insieme restino dentro le corse dei cursori; `Caldo` e `Freddo` si annullano a vicenda, di proposito. I nomi sono miei: se uno non ti torna, dimmi quale e come lo chiameresti.

## 2. Il tocco lungo somma

Tocca `Caldo`: l'immagine si scalda. Tieni premuto `Caldo`: si scalda il doppio, e nel modulo Colore la `Temperatura` è a +24. Tocca `Contrasto`, poi tieni premuto `Ombre aperte` e poi `Pellicola`: restano tutti e tre, il contrasto, le ombre aperte e la grana con i neri sollevati. Tocca `Roccobot` e tieni premuto `Vivido`: il colore sale sopra quello di `Roccobot`. Il tocco breve resta 'azzera e applica', e `Annulla` disfa ogni passo.

Letture mie: i cursori principali si sommano fino al tetto della loro corsa; i secondari (il filtro del bianco e nero, raggio e maschera della nitidezza, dimensione e luci della grana, sfumatura della vignettatura) prendono il valore dello stile solo se lo stile lo nomina, perché da soli non cambiano un pixel; il bianco e nero resta se lo era; due curve si applicano una dopo l'altra. Gli stili di casa non usano i cursori secondari.

## 3. Il segmento del tasto `Spessore`

Nel modulo Disegno scegli il rettangolo e tocca `Spessore`, poi porta il cursore al minimo: la linea del tasto arriva quasi ai lati, a 3 dp, senza toccarli. Porta il cursore al massimo: la linea è più spessa e le estremità tonde restano alla stessa distanza dai lati.

Letture mie: la distanza è quella del rettangolo dentro il tasto `Riempimento`.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| I dieci stili di casa | 4.97-01 | Non provato | | Attendere il collaudo. |
| Il tocco lungo somma | 4.97-02 | Non provato | | Attendere il collaudo. |
| Il segmento del tasto `Spessore` | 4.97-03 | Non provato | | Attendere il collaudo. |
| Gli Effetti negli stili di casa | 4.96-01 | Accettabile | Approccio diverso: dieci stili additivi | Rifatti nella 4.97 (`4.97-01`, `4.97-02`). |
| Esportare e importare uno stile solo | 4.96-02 | OK | | Chiusa. |
| Il tratto e il riempimento di fabbrica | 4.95-01 | OK | | Chiusa. |
| Il tasto `Elimina` | 4.95-02 | OK | | Chiusa. |

## Prossimi passi

- **In collaudo**: i dieci stili (`4.97-01`), il tocco lungo che somma (`4.97-02`) e il segmento di `Spessore` (`4.97-03`).
- **Poi**: uno o due rilasci di assestamento e la 5.00, che ti chiederò di confermare.
- **Escluso per ora**: scegliere e spostare un elemento con un solo trascinamento (nota D del giro 4.90), come hai deciso.
- **5.0x**: la scelta per il Play Store (AIV come gestore di file, o galleria col solo accesso a immagini e video), con lo snellimento dei file di regole.
