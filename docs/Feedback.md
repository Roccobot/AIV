# Feedback AIV

Versione **4.64**: nel modulo Disegno il comando si chiama `Elimina tutto`, e le linee dei terzi di `Raddrizza` hanno l'aspetto delle guide del Disegno.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.64 è pubblicata: [v4.64](https://github.com/Roccobot/AIV/releases/tag/v4.64), con l'APK
[AIV-4.64.apk](https://github.com/Roccobot/AIV/releases/download/v4.64/AIV-4.64.apk).
Commit prodotto su `main`: `b3f6671`, release dal commit `b3f6671` (SlimVer 4.64 / versionCode 343; APK 8.558.441 byte, digest `4956dfa0`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.64**: due prove nuove, e dieci domande aperte da `4.64-03` a `4.64-12`: rispondi con la lettera nel commento, e `Tutto OK` senza commento accetta il parere. Giro **4.63**: una prova OK.

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

Le verifiche automatiche della 4.64 sono superate: banco di prova completo (646 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.64-01 | Non provato | | Attendere il collaudo. |
| 4.64-02 | Non provato | | Attendere il collaudo. |
| 4.64-03 | Non provato | | Rispondere nel commento. |
| 4.64-04 | Non provato | | Rispondere nel commento. |
| 4.64-05 | Non provato | | Rispondere nel commento. |
| 4.64-06 | Non provato | | Rispondere nel commento. |
| 4.64-07 | Non provato | | Rispondere nel commento. |
| 4.64-08 | Non provato | | Rispondere nel commento. |
| 4.64-09 | Non provato | | Rispondere nel commento. |
| 4.64-10 | Non provato | | Rispondere nel commento. |
| 4.64-11 | Non provato | | Rispondere nel commento. |
| 4.64-12 | Non provato | | Rispondere nel commento. |

## 1. `Elimina tutto`, e la pressione lunga sul Disegno

Nel modulo Disegno disegna due elementi. Il comando a destra, sopra il cursore dell'opacità, si chiamava `Azzera` e adesso si chiama `Elimina tutto`: toccalo, e il disegno si svuota. Poi disegna un elemento, sceglilo con un tocco e tieni premuta l'icona del Disegno nella fila dei moduli: il disegno si svuota e `Elimina` si spegne. Disegna un altro elemento: deve nascere senza i punti color accento, cioè non scelto. Fino alla 4.63 la pressione lunga lasciava la scelta su un elemento che non c'era più, e l'elemento disegnato dopo nasceva scelto.

## 2. Le linee dei terzi di `Raddrizza`

Nel modulo Geometria trascina il cursore `Raddrizza`: mentre il dito è giù compaiono le linee dei terzi, adesso nel colore d'accento, sottili e senza alone, come la guida che corre lungo il bordo quando un elemento del Disegno vi si appoggia. Prova su un'immagine chiara e su una scura. Fino alla 4.63 erano una linea bianca su un alone scuro.

## 3. Domanda · `Copia` accanto a `Duplica` (G2)

Nel menu a pressione lunga sull'elemento, cioè la seconda parte della G2 (nota E del giro della 4.60), ci sono sia `Duplica` sia `Copia`. Che cosa fa `Copia`? Aspetta dal giro della 4.60.

**C1**: `Duplica` posa subito la copia accanto all'originale; `Copia` la tiene da parte, e `Incolla` la posa in un'altra immagine aperta nell'editor completo.

**C2**: `Copia` prende lo stile dell'elemento (colori, luminosità, spessore, tratteggio, riempimento), e `Incolla stile` lo dà a un altro elemento.

Con C1, dove compare `Incolla`? **I1**: in un menu che appare tenendo premuto un punto vuoto dell'immagine. **I2**: in un tasto `Incolla` del modulo Disegno, accanto a `Elimina tutto`, che c'è solo quando hai copiato un elemento.

Parere: **C1**, perché `Copia` da solo dice che si copia l'elemento, e copiare lo stile si chiamerebbe `Copia stile`; in una sola immagine `Copia` più `Incolla` rifarebbe il lavoro di `Duplica`, quindi `Incolla` serve in un'altra. E **I2**, perché il tasto si vede senza doverlo cercare.

## 4. Domanda · come gira `Ruota` (G2)

Nello stesso menu c'è `Ruota`: come gira l'elemento? Aspetta dal giro della 4.60.

**R1**: un quarto di giro a ogni tocco.

**R2**: una maniglia libera, con scatti a 0, 45 e 90 gradi.

Parere: **R1**. Un rettangolo o un'ellisse girati di un quarto di giro restano allineati ai bordi dell'immagine; con R2 l'editor dovrebbe prima imparare a disegnarli inclinati, perché oggi li descrive con due angoli opposti.

## 5. Domanda · i venti stili di casa, con foschia, grana e vignettatura

Gli `Stili AIV` sono ancora senza foschia, grana e vignettatura. Con `d-preset-xmp` avevi scelto di rifarli da capo una volta entrati tutti gli effetti, e gli effetti sono completi dalla 2.67: manca solo il tuo via libera. Insieme arriva il file di uno stile solo, `.aivstyle` (`d-stile-singolo`): un comando sulla riga di uno stile, e un'importazione che lo aggiunge agli altri.

**S1**: si rifanno subito dopo la seconda parte della G2.

**S2**: si rifanno dopo i lavori già in fila: la G2, lo strumento Sfocatura e il testo (G3).

**S3**: restano in attesa.

Se nel frattempo hai cambiato i tuoi stili in Lightroom, allega qui i file XMP: servono solo in quel caso.

Parere: **S2**, perché i lavori del Disegno sono già cominciati e in fila, e gli stili non dipendono da loro.

## 6. Domanda · lo sfogliatore Web e il Play Store

Li hai congelati tu (*per ora aspetta* e *per ora*), e te li ripropongo perché sono fermi da più di dieci giri.

**W1**: restano congelati tutti e due.

**W2**: si sblocca lo sfogliatore Web.

**W3**: si sblocca il Play Store.

**W4**: si sbloccano tutti e due.

Parere: **W1**, perché davanti ci sono già la G2, la Sfocatura, il testo e gli stili.

## 7. Domanda · la verifica approfondita delle regole di Arda e AIV

Il 2026-10-01 un'altra sessione l'aveva cominciata e poi sospesa, in attesa di una tua conferma esplicita. Il brief non dice che cosa cercasse con precisione, e il lavoro preliminare è rimasto nel contenitore di quella sessione, che da qui non si raggiunge.

**V1**: si riprende da capo, con una stima prima di partire.

**V2**: resta sospesa.

**V3**: si abbandona, e la voce esce dal brief.

Parere: **V3**, perché da allora le regole dei due repo sono cambiate molto, e una verifica nuova si chiede quando serve, con un obiettivo preciso.

## 8. Domanda · lo snellimento dei file di regole

Il 2026-09-28 avevi chiesto di indagare se si possono snellire ancora i file di configurazione e renderli modulari, con le parti poco usate in file che si leggono solo quando servono. È un'indagine: si misura quanto pesa ogni file che si carica da sé e quali sezioni si usano davvero, poi si propone. Intanto il nucleo comune a tutti gli agenti, `Core.md`, è arrivato a 13.981 byte su un limite di 14.000.

**M1**: comincio dalla misura e ti do la stima del lavoro, senza toccare niente.

**M2**: resta in attesa.

Parere: **M1**, perché il nucleo comune è pieno, e la prossima regola che deve valere sempre non ci entra.

## 9. Domanda · le foto di Subito (Roccobot ABP)

Su Subito le foto non si vedono: vivono su `images.sbito.it`, e la protezione anti-typosquatting del tuo profilo AdGuard DNS, quella che blocca i domini che imitano un nome noto, lo scambia per un'imitazione di `subito.it`. Lo sblocco che hai fatto non vale ancora per il server che usa il Mac.

Mi servono tre dati dalla dashboard di AdGuard DNS: a quale server è collegato ogni dispositivo; se la regola di sblocco, quella che comincia con `@@`, compare fra le regole utente del server del Mac; se le query del Mac compaiono nel registro. Scrivili nel commento: qui `Tutto OK` da solo non basta.

Se la regola c'è e la risposta resta riscritta: **P1** spegni la protezione anti-typosquatting su quel server, che è la via documentata da AdGuard; **P2** la lasci accesa, e le foto di Subito restano rotte.

Parere: **P1**, sapendo che quel server smette di bloccare i domini che imitano siti noti.

## 10. Domanda · le icone dei badge di Arda a 64 px (B3)

Su 'I Grandi di Arda' le icone dei badge sono a 256 px e si vedono a circa 15. Ridotte a 64 px pesano 193 KB invece di 413, ma non restano identiche: alle misure normali circa il 5% dei pixel cambia di colore in modo misurabile, e in Modalità XL, dove 64 px non bastano, il 23%. Il risparmio vale una volta sola per ogni visitatore, e non rallenta il primo disegno della pagina.

**B3a**: si riducono a 64 px.

**B3b**: restano come sono.

Parere: **B3b**, perché il guadagno è piccolo e una volta sola, e in Modalità XL le icone peggiorano.

## 11. Domanda · il riflusso forzato su Arda e Terramare

Lighthouse segnalava un riflusso forzato, cioè il browser costretto a ricalcolare l'impaginazione a metà di uno script: 812 ms su Terramare e 233 ms su Arda, misurati prima della 2.95 e della 15.85. Da allora i tuoi report danno 99 su Terramare e 95-98 su Arda, e quel dato non è stato rimisurato.

**F1**: lo rimisuro e, se pesa ancora, ti propongo come toglierlo.

**F2**: si chiude così, coi punteggi di adesso.

Parere: **F2**: col tempo di blocco a 120 ms su Terramare e a 250 ms su Arda, difficilmente un residuo cambierebbe qualcosa che si veda.

## 12. Domanda · il tocco su un allegato, nel pannello di Altro

Sul telefono apri Altro tenendo premuto il FAB, allega un'immagine, chiudi la tastiera e tocca l'allegato. Se il campo ha ancora il cursore, il nome dell'allegato entra nel testo; se non ce l'ha, va negli appunti. È un dubbio rimasto dalla 4.32: non si sa quale dei due capiti a tastiera chiusa.

Scrivi nel commento che cosa è successo e, se non è quello che ti aspetti, che cosa vorresti.

## Decisioni da concordare

- **Tutte le domande aperte del brief** sono le prove da `4.64-03` a `4.64-12` (eccezione chiesta da Rocco il 2026-10-08): `Copia` e `Ruota` della G2, gli stili di casa, sfogliatore Web e Play Store, la verifica approfondita, lo snellimento dei file di regole, le foto di Subito, le icone dei badge di Arda, il riflusso forzato, il tocco su un allegato in Altro.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| `Elimina tutto`, e la pressione lunga sul Disegno | 4.64-01 | Non provato | | Attendere il collaudo. |
| Le linee dei terzi di `Raddrizza` | 4.64-02 | Non provato | | Attendere il collaudo. |
| La luminosità dal vivo sull'elemento scelto | 4.63-01 | OK | Chiesto se `Azzera` serva, e in Altro le linee dei terzi come le guide del Disegno | Fatte nella 4.64 (`4.64-01`, `4.64-02`). |

## Prossimi passi

- **In collaudo**: `Elimina tutto` e la pressione lunga sul Disegno (`4.64-01`); le linee dei terzi di `Raddrizza` (`4.64-02`).
- **Domande aperte**: dieci, da `4.64-03` a `4.64-12`, anche di altri progetti; si risponde con la lettera nel commento.
- **Dopo**: la seconda parte della G2, cioè il menu a pressione lunga sull'elemento (`Sposta sopra`, `Copia`, `Duplica`, `Sposta sotto`, `Ruota`, `Elimina`) e le maniglie per ridimensionare; lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: la luminosità dal vivo sull'elemento scelto (`4.63-01`).
