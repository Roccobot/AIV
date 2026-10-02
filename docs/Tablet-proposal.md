# Proposta delle interfacce AIV per tablet

[Apri i mockup interattivi](https://roccobot.github.io/AIV/tablet.html).
Proposte dell'agente del 1 ottobre 2026; decisione `d-tablet-layout`: Approvo la direzione.
Nella `3.27` le schermate Android non cambiavano ancora. Dalla `3.31` il Visualizzatore
porta il pannello informazioni a lato (soglie 600 / 800 / 1024 dp del mockup; chiuso di
serie sotto i 1.024). Dalla `3.32` Cartelle è a due colonne da 600 dp (elenco a lato,
contenuto a destra; esclusa la vista ad albero). Dalla `3.33` Impostazioni sono
indice e pagina affiancati da 600 dp. Dalla `3.34` Ricerca riusa elenco+risultati da 600 dp;
editor semplice e completo portano gli strumenti a lato da 1.024 dp (sotto restano in basso).
Dalla `3.35` Cestino ha dettagli a lato da 600 dp; Cronologia gruppi+percorsi; Cartelle di
sistema cartelle a lato e file a destra; dialoghi rinomina/converti/salva a 520 dp e
Copia/sposta a 800 dp su tablet. Dalla `3.36` Dimensioni e filigrana mettono anteprima
accanto ai parametri da 1.024 dp; Guida/scelta editor e bottomsheet sono finestre
scorrevoli a larghezza contenuta da 600 dp.
I colori derivano da `Theme.kt`; il font è Roboto, distribuito con la propria licenza.
Sono composizioni web dimostrative, non schermate Android misurate né una nuova identità grafica.

| Schermata | Uso dello spazio |
|---|---|
| Cartelle | Elenco a lato, contenuto della cartella a destra. |
| Griglia | Miniature leggibili, colonne adattabili e comandi sul bordo. |
| Cartelle di sistema | Gerarchia, percorso e file visibili insieme. |
| Ricerca | Campo e ambito in alto; risultati nella parte principale. |
| Visualizzatore e video | Media al centro, informazioni laterali richiudibili. |
| Editor semplice | Canvas e strumenti di ritaglio/rotazione affiancati. |
| Editor completo | Canvas e parametri del modulo attivo; nove moduli raggiungibili. |
| Impostazioni | Indice e pagina scelta affiancati; tutte le sottopagine consultabili. |
| Cestino | Anteprime e comandi di ripristino, con i dettagli leggibili. |
| Cronologia | Raggruppamento per data e percorsi nello stesso spazio. |
| Copia e sposta | Elenco delle destinazioni e percorso da confermare. |
| Rinomina, download, conversione e salvataggio | Finestre centrate con larghezza contenuta. |
| Informazioni | Scheda richiudibile per recuperare il canvas. |
| Dimensioni e filigrana | Anteprima accanto ai parametri. |
| Guida e scelta dell'editor | Finestra scorrevole, spiegazione e azione insieme. |

Ogni schermata comprende stati vuoto, caricamento, accesso da autorizzare ed errore.
Formato orizzontale: 1.024 × 768; verticale: 800 × 1.280; finestra ridotta: 600 × 900.
Il mockup scorre nel browser quando lo schermo è più stretto del formato scelto.
In verticale e nella finestra ridotta l'editor porta gli strumenti sotto il canvas.
La preferenza sinistra/destra sposta i pannelli, conservando l'ordine di annulla/ripeti.
La selezione di cartella, pagina e modulo sopravvive ai cambi di orientamento e tema.

## Cosa concordare

- La doppia vista cartelle/contenuto: utile per alternare cartelle senza ritornare all'elenco.
- La posizione delle informazioni nel visualizzatore e la soglia per richiuderle.
- La larghezza del pannello dell'editor e l'altezza dei controlli in verticale.
- Il riordino delle impostazioni: proposta distinta in [Settings-proposal.md](Settings-proposal.md).

Usa la sezione Decisioni del [feedback](https://roccobot.github.io/AIV/feedback.html).
Le operazioni sono dimostrative; shader, riproduzione video e file non vengono eseguiti nel browser.
Filigrana e Fluidifica restano funzioni già approvate: qui si valuta soltanto la disposizione.
