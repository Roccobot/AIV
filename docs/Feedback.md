# Feedback AIV

Versione **4.48**: nel modulo Disegno il tasto Spessore, la linea trasparente, la luminosità sui tondi e le linee dritte (4.47); l'ombra della selezione rifatta (4.48).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.48 è pubblicata: [v4.48](https://github.com/Roccobot/AIV/releases/tag/v4.48), con l'APK
[AIV-4.48.apk](https://github.com/Roccobot/AIV/releases/download/v4.48/AIV-4.48.apk).
Commit prodotto su `main`: `dbe2e55`, release dal commit `dbe2e55` (SlimVer 4.48 / versionCode 336; APK 8.525.257 byte, digest `bddff268`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.47** e **4.48**: cinque prove nuove. Giro **4.46**: quattro prove OK, `4.46-04` ripresa in `4.48-01`.

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

Le verifiche automatiche della 4.48 sono superate: banco di prova completo (627 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.47-01 | Non provato | | Attendere il collaudo. |
| 4.47-02 | Non provato | | Attendere il collaudo. |
| 4.47-03 | Non provato | | Attendere il collaudo. |
| 4.47-04 | Non provato | | Attendere il collaudo. |
| 4.48-01 | Non provato | | Attendere il collaudo. |

## 1. Il tasto Spessore e le tre scelte

Nel modulo `Disegno` il quarto tasto è `Spessore`, al posto di `Luminosità`: una linea del colore della traccia, più spessa quanto più alto è lo spessore. `Traccia`, `Riempimento` e `Spessore` sono una scelta sola, come hai risposto (`S1`): con `Traccia` il cursore regola l'opacità della linea (nuova, piena di fabbrica), con `Riempimento` l'opacità del riempimento, con `Spessore` lo spessore. Con `Spessore` scelto i tondi cambiano il colore della linea: è una mia lettura, se li vuoi spenti scrivilo qui.

## 2. La linea trasparente, uniforme

Scegli `Traccia`, porta l'opacità a metà e disegna una freccia e qualche tratto a mano libera che ripassa su sé stesso. Dove la punta incrocia l'asta, e dove il tratto si sovrappone, il colore resta uguale al resto, senza zone più scure.

## 3. La luminosità tenendo premuto un tondo

Tieni premuto un tondo per circa mezzo secondo: sotto i colori compare un cursore della luminosità, con accanto il colore che ne risulta. Se senza staccare il dito scorri a destra o a sinistra, allo stacco il colore si applica e il cursore si chiude. Se invece alzi il dito fermo, il cursore resta: lo regoli e lo chiudi toccando fuori, e quel tocco non disegna. Il tondo resta del suo colore base, e un tocco successivo sul tondo torna al colore base. Vale per la traccia o per il riempimento, secondo il tasto scelto.

## 4. Linee e frecce dritte

Con `Linea` o `Freccia`, disegna quasi in orizzontale o quasi in verticale: entro 5 gradi dall'asse la linea si posa esattamente sull'asse, e finché il dito resta giù una linea sottile color accento attraversa l'immagine sull'asse scelto, e sparisce allo stacco. I 5 gradi sono una mia scelta: dimmi se l'aggancio scatta troppo presto o troppo tardi.

## 5. L'ombra della selezione, da bordo a bordo

Tieni premuta una miniatura per cominciare una selezione, in una cartella con poche immagini e in una con molte, nei due temi. Sopra la scheda in basso l'ombra adesso attraversa tutta la larghezza dello schermo e arriva fino alla scheda, senza il rettangolo che vedevi nella 4.46, e al massimo è la metà di prima (24,5% invece di 49%). Le spunte e la durata dei video restano sopra, piene.

## Decisioni da concordare

Nessuna decisione aperta in questo giro.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il tasto Spessore | 4.47-01 | Non provato | | Attendere il collaudo. |
| La linea trasparente | 4.47-02 | Non provato | | Attendere il collaudo. |
| La luminosità sui tondi | 4.47-03 | Non provato | | Attendere il collaudo. |
| Linee e frecce dritte | 4.47-04 | Non provato | | Attendere il collaudo. |
| L'ombra della selezione | 4.48-01 | Non provato | | Attendere il collaudo. |
| Dal menu a Info, senza lampeggio | 4.46-01 | OK | | Concluso. |
| La cartella d'origine | 4.46-02 | OK | La scelta della destinazione aveva sempre le miniature in alto? | Sì: la 4.46 non ha cambiato la disposizione, risposta in chat. |
| Gli avvisi centrati | 4.46-03 | OK | | Concluso. |
| L'ombra della selezione | 4.46-04 | Non approvato | Un rettangolo invece dell'ombra sotto la scheda; opacità da dimezzare. | Ripresa in `4.48-01`. |
| La freccia tratteggiata | 4.46-05 | OK | | Concluso. |

## Prossimi passi

- **In collaudo**: Spessore (`4.47-01`), la linea trasparente (`4.47-02`), la luminosità sui tondi (`4.47-03`), linee e frecce dritte (`4.47-04`), l'ombra della selezione (`4.48-01`).
- **Dopo**: G2 (selezione, vertici, parametri sull'oggetto, elimina; poi maniglie, sopra e sotto); lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: dal menu a Info, la cartella d'origine, gli avvisi centrati, la freccia tratteggiata (`4.46-01`, `-02`, `-03`, `-05`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
