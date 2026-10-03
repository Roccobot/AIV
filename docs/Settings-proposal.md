# Proposta di riordino delle impostazioni

Confronto con AIV 3.13, letto il 1 ottobre 2026.
**Applicata nella `3.27`** (decisione `d-settings-order`: Applica la proposta).
La release 3.14 correggeva miniature e PSD e conservava la disposizione delle impostazioni.
Puoi scegliere nel [documento di feedback](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La regola applicata è in [Rules.md](../Rules.md), sezione 'Dove va un'impostazione, e chi la deve trovare'.

## Prima e dopo

| Voce | Prima | Proposta | Perché |
|---|---|---|---|
| Lista file: copia il percorso della cartella contenitore | Comandi e indicatori, nella pagina iniziale | Cartelle > Opzioni di visualizzazione > Lista | Descrive il comportamento della lista dei file, accanto alle altre sue opzioni. |
| Rinomina e download | Modifica e backup > Editor e salvataggio > Rinomina e download | Modifica e protezione dei file > Rinomina e download | Rinomina e download si usano anche senza aprire l'editor; la pagina rimane autonoma. |
| Gestisci le miniature memorizzate | Funzionalità avanzate | Gestione dell'app | È manutenzione, come esportazione/importazione e ripristino degli avvisi. |
| Esporta e importa; Ripristina gli avvisi | Azioni in fondo alla pagina | Gestione dell'app | Tre operazioni correlate raccolte sotto un titolo, senza aggiungere una pagina. |
| Modifica e backup | Titolo di sezione | Modifica e protezione dei file | La sezione comprende editor, rinomina, download e protezione degli originali. |

## Pagina iniziale attuale

- **Aspetto**: Tema dell'app; Effetto dietro menu e pannelli; Cartelle; Visualizzatore; Informazioni.
- **Navigazione**: Sfoglia solo le immagini; Sfoglia le immagini verso destra dalla meno recente.
- **Comandi e indicatori**: Etichette e pulsanti; Lista file: copia il percorso della cartella contenitore.
- **Modifica e backup**: Editor e salvataggio; Attiva il cestino; Pulizia automatica del cestino.
- **All'avvio**: Leggi il contenuto degli appunti all'avvio; Apri una cartella all'avvio.
- **Funzionalità avanzate**: Salva le miniature nella memoria grafica; Gestisci le miniature memorizzate.
- **Azioni finali**: Esporta e importa; Ripristina gli avvisi.

## Pagina iniziale proposta

- **Aspetto**: Tema dell'app; Effetto dietro menu e pannelli; Cartelle; Visualizzatore; Informazioni.
- **Navigazione**: Sfoglia solo le immagini; Sfoglia le immagini verso destra dalla meno recente.
- **Comandi e indicatori**: Etichette e pulsanti.
- **Modifica e protezione dei file**: Editor e salvataggio; Rinomina e download; Attiva il cestino; Pulizia automatica del cestino.
- **All'avvio**: Leggi il contenuto degli appunti all'avvio; Apri una cartella all'avvio.
- **Funzionalità avanzate**: Salva le miniature nella memoria grafica.
- **Gestione dell'app**: Gestisci le miniature memorizzate; Esporta e importa; Ripristina gli avvisi.

## Sottopagine, con la proposta applicata

| Pagina | Contenuto |
|---|---|
| Cartelle | Intestazione delle cartelle; Colore delle cartelle; Opzioni di visualizzazione; Cartelle incluse/escluse. |
| Cartelle > Opzioni di visualizzazione | Griglia: colonne, contatore sotto le cartelle, nome del file. Lista: contatore, dimensione del testo, copia del percorso della cartella contenitore. Cartelle di sistema: file nascosti, solo cartelle con immagini/video. |
| Cartelle > Cartelle incluse/escluse | Modalità, liste indipendenti e comandi di aggiunta/rimozione. |
| Visualizzatore | Sfondo; Tema dello sfondo; Adattamento e zoom; Riproduci i video direttamente. |
| Visualizzatore > Adattamento e zoom | Ingrandisci le immagini piccole; Zoom massimo; Rendering dei pixel; Zoom nel menu del visualizzatore. |
| Informazioni | Barra delle info e posizione; Contatore dei fotogrammi; Info sul file singolo; Dimensioni dei file selezionati. |
| Informazioni > Info sul file singolo | Elenco dei campi riordinabile. |
| Etichette e pulsanti | Posizione preferita dei pulsanti; Etichette sotto le icone; Indicatore dell'ultimo media visualizzato; Ordine dei pulsanti. |
| Etichette e pulsanti > Ordine dei pulsanti | Gruppi di pulsanti riordinabili. |
| Editor e salvataggio | Editor immagini; Copia di sicurezza prima di sovrascrivere; Formato e qualità; Stili dell'editor; Filigrana. |
| Editor e salvataggio > Stili dell'editor | Lista degli stili e relativi comandi. |
| Editor e salvataggio > Filigrana | Scelta della filigrana e impostazioni. |
| Rinomina e download | Consenti la rinomina al salvataggio; Scegli il percorso di download; Consenti la modifica dell'estensione, con scelte indipendenti Scarica/Rinomina. |
| Gestisci le miniature memorizzate | Stato e pulizia della cache. |
| Esporta e importa | Aree del backup, esportazione e importazione. |

Le tre voci di Navigazione/Comandi che rimangono direttamente accessibili sono brevi;
aggiungere una sottopagina per una sola voce aumenterebbe i tocchi senza aiutare la ricerca.
Il gruppo sulla protezione comprende quattro elementi, ma non costituisce una nuova famiglia:
è una sezione con due pagine autonome e due comandi collegati al cestino. Le liste riordinabili,
le cartelle e i backup conservano pagine proprie. La profondità massima rimane due livelli.
L'indicatore dell'ultimo media resta in Etichette e pulsanti, come hai chiesto.

## Prima di applicare la proposta

- Nuove stringhe: **2 titoli**, `Modifica e protezione dei file` e `Gestione dell'app`.
  Equivalenti inglesi: `Editing and file protection` e `App management`.
  Serviranno **54 testi** nei 27 cataloghi completi; i due cataloghi regionali manterranno
  i loro normali ripieghi. Le traduzioni andranno controllate prima della release.
- Conservare tutte le chiavi, i valori scelti, i valori iniziali e le aree di backup.
- Aggiornare i rimandi e la ricerca delle impostazioni: titoli e contenuto delle pagine devono
  continuare a trovare rinomina, download, percorso, miniature, avvisi e cartelle.
- Verificare navigazione avanti/indietro, ripristino della pagina dopo rotazione e importazione
  di un backup precedente. Gli ordinali delle pagine esistenti devono rimanere stabili.
- Verificare temi, testo ingrandito e riordino dei pulsanti su telefono. Il layout da tablet
  è una proposta distinta, consultabile nei [mockup](https://roccobot.github.io/AIV/tablet.html).
