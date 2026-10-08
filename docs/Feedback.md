# Feedback AIV

Versione **4.62**: nel modulo Disegno gli elementi si appoggiano ai bordi dell'immagine, con la guida; restano da provare le quattro voci della 4.61.
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.62 è pubblicata: [v4.62](https://github.com/Roccobot/AIV/releases/tag/v4.62), con l'APK
[AIV-4.62.apk](https://github.com/Roccobot/AIV/releases/download/v4.62/AIV-4.62.apk).
Commit prodotto su `main`: `c593f42`, release dal commit `c593f42` (SlimVer 4.62 / versionCode 341; APK 8.556.921 byte, digest `331bbff7`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.62**: tre prove nuove, e le quattro della 4.61 ancora da provare. Giro **4.60**: cinque prove OK.

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

Le verifiche automatiche della 4.62 sono superate: banco di prova completo (643 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.61-01 | Non provato | | Attendere il collaudo. |
| 4.61-02 | Non provato | | Attendere il collaudo. |
| 4.61-03 | Non provato | | Attendere il collaudo. |
| 4.61-04 | Non provato | | Attendere il collaudo. |
| 4.62-01 | Non provato | | Attendere il collaudo. |
| 4.62-02 | Non provato | | Attendere il collaudo. |
| 4.62-03 | Non provato | | Attendere il collaudo. |

## 1. Il bordo pieno sul tasto acceso

Nel modulo `Disegno` lo strumento scelto (rettangolo, freccia...) e i tasti accesi (`Tratteggio`, quello fra `Traccia`, `Spessore` e `Riempimento` che regola il cursore) hanno un bordo pieno color accento di 2 dp, oltre allo sfondo verde. Il bordo è disegnato sopra il tasto, così le bande che vanno da bordo a bordo non lo interrompono.

## 2. Il velo d'aiuto del Disegno

Per rivederlo: Impostazioni, 'Ripristina gli avvisi', poi apri l'editor completo e passa le due slide dell'editor. Al primo `Disegno` compaiono il tondo rosso cerchiato d'arancione e, sopra, il tuo testo: 'Con il modulo Disegno puoi aggiungere all'immagine linee, frecce, ellissi, rettangoli arrotondati, testi semplici e riquadri 'pillola'. Il salvataggio appiattisce l'immagine: non è possibile riaprirla per modificare o spostare gli elementi. Premi a lungo su un colore per regolare la sua luminosità.' Un tocco lo chiude. Il testo nomina anche il testo e la pillola, che non ci sono ancora: l'ho lasciato com'è, così le 28 lingue si scrivono una volta sola. È anche nell'[artefatto dei veli](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).

## 3. Il tratteggio del tasto finisce con due trattini

Spegni `Tratteggio` e guardane i due capi: la banda comincia e finisce con un trattino. Trattini e spazi si allungano o si accorciano insieme, col ritmo del tuo mockup, finché chiudono sui due bordi del tasto; su un telefono e su un tablet il tasto ha larghezze diverse, e il ritmo resta quello.

## 4. Il cursore della luminosità senza velo

Disegna un elemento, sceglilo, e tieni premuto un tondo: il cursore della luminosità si apre senza velo e senza sfocatura, con un'ombra intorno (la stessa dell'opzione 'Ombra' delle impostazioni). Mentre il dito scorre vedi l'elemento scelto e i tasti cambiare colore.

## 5. Disegnando, l'elemento si appoggia al bordo

Disegna un rettangolo cominciando a pochi millimetri dall'angolo in alto a sinistra dell'immagine: il primo vertice va sull'angolo, con la traccia a filo dei due bordi, tutta visibile. Allo stesso modo il vertice che tiene il dito si appoggia al bordo quando gli arriva vicino, da dentro o da fuori; più lontano (oltre 12 dp, circa 2 mm) l'elemento va oltre il bordo e se ne vede solo la parte dentro. Vale per rettangolo, ellisse, linea e freccia, punta compresa; la mano libera segue il dito. Una linea agganciata all'orizzontale o alla verticale tiene la direzione e si appoggia solo lungo l'altro asse. Il primo tocco deve ancora cadere dentro l'immagine.

## 6. Spostando, l'elemento si appoggia al bordo, e oltre esce

Scegli un elemento e trascinalo verso un bordo: arrivato vicino, si posa a filo. Spinto più in là, esce dall'immagine.

## 7. La guida lungo il bordo

Mentre il dito tiene un elemento appoggiato a un bordo, una linea sottile color accento corre lungo quel bordo; allo stacco sparisce. È la stessa guida dell'orizzontale e della verticale.

## Decisioni da concordare

- **La seconda parte della G2** (nota E del giro della 4.60): due domande in chat, su `Copia` e su `Ruota`.

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il bordo pieno sul tasto acceso | 4.61-01 | Non provato | | Attendere il collaudo. |
| Il velo d'aiuto del Disegno | 4.61-02 | Non provato | | Attendere il collaudo. |
| Il tratteggio del tasto finisce con due trattini | 4.61-03 | Non provato | | Attendere il collaudo. |
| Il cursore della luminosità senza velo | 4.61-04 | Non provato | | Attendere il collaudo. |
| Disegnando, l'elemento si appoggia al bordo | 4.62-01 | Non provato | | Attendere il collaudo. |
| Spostando, l'elemento si appoggia al bordo, e oltre esce | 4.62-02 | Non provato | | Attendere il collaudo. |
| La guida lungo il bordo | 4.62-03 | Non provato | | Attendere il collaudo. |
| Un tocco sceglie un elemento | 4.60-01 | OK | | Concluso. |
| I parametri cambiano l'elemento scelto | 4.60-02 | OK | | Concluso. |
| L'elemento scelto si sposta | 4.60-03 | OK | | Concluso. |
| Elimina | 4.60-04 | OK | | Concluso. |
| Il tasto Tratteggio acceso e spento | 4.60-05 | OK | | Concluso. |

## Prossimi passi

- **In collaudo**: il bordo pieno (`4.61-01`), il velo d'aiuto del Disegno (`4.61-02`), il tratteggio del tasto (`4.61-03`), il cursore della luminosità senza velo (`4.61-04`); l'appoggio ai bordi disegnando (`4.62-01`) e spostando (`4.62-02`), e la sua guida (`4.62-03`).
- **Dopo**: la seconda parte della G2, cioè il menu a pressione lunga sull'elemento (`Sposta sopra`, `Copia`, `Duplica`, `Sposta sotto`, `Ruota`, `Elimina`) e le maniglie per ridimensionare; lo strumento Sfocatura; G3, il testo; la pillola.
- **Concluso**: le cinque prove della 4.60 (`4.60-01..05`).
- **Già nel brief**: stili Lightroom in attesa di via libera; sfogliatore Web e Play Store sospesi.
