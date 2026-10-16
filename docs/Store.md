# AIV Play su Google Play: che cosa si carica e che cosa si risponde

> **Cos'è questo file.** Tutto quello che serve per pubblicare AIV Play su Google Play e che non è
> codice: il file da caricare, i testi della scheda (italiano e inglese, scelta D1 dell'utente,
> 2026-10-11), le risposte ai moduli della Play Console e i passi in ordine. Nasce con la `5.70`
> (scelte A1 e B1). Ogni testo qui è una **proposta da validare nel collaudo**, come le etichette
> dell'app; i passi nella Console li fa l'utente.
> ⚠️ **Le risposte sui dati sono affermazioni sul codice**: chi aggiunge una libreria o una
> richiesta di rete che manda qualcosa cambia qui le risposte e `publish/privacy.html`, nella stessa
> versione.

## 📦 Il file da caricare

- **`AIV-Play-x.xx.aab`**, allegato a ogni release dalla `5.70` accanto ai due APK. Google Play
  accetta solo il formato AAB (Android App Bundle) per un'app nuova; gli APK restano per chi
  installa da GitHub.
- **La firma**: l'AAB è firmato con la chiave che firma già AIV (scelta B1). Su Google Play quella
  è soltanto la **chiave di caricamento**: alla prima pubblicazione si sceglie che sia Google a
  generare e custodire la chiave con cui firma l'app che consegna (Play App Signing, l'opzione
  proposta di serie). Quindi AIV Play scaricata da Google Play e AIV GitHub hanno firme diverse,
  ed è giusto così: sono due app distinte (`io.github.roccobot.aiv.play` e
  `io.github.roccobot.aiv`).
- **Livello di Android**: AIV punta ad Android 16 (`targetSdk` 36), il requisito di Google Play
  per le app nuove dal 31 agosto 2026.

## 🔏 Informativa sulla privacy

- Indirizzo da dare alla Console: <https://roccobot.github.io/AIV/privacy.html>. Una pagina sola,
  italiano e inglese (scelta C1), che vale per le due varianti.

## 📝 La scheda dello Store

I limiti di Google: nome fino a 30 caratteri, descrizione breve fino a 80, descrizione completa
fino a 4.000. I conti dei testi qui sotto li fa `tools/store-check.py`.

### Nome

`AIV - Astonishing Image Viewer`, uguale nelle due lingue.

### Descrizione breve

- **Italiano**: Galleria veloce per foto e video: zoom preciso, file enormi, editor e filigrana.
- **English**: Fast gallery for photos and videos: precise zoom, huge files, editor, watermark.

### Descrizione completa, italiano

AIV (Astonishing Image Viewer) è una galleria per Android pensata per chi lavora con le immagini: apre in un istante anche i file più grandi e li mostra al pixel, con uno zoom che segue le dita.

Cartelle
- Le cartelle di foto e video, ognuna con la sua copertina e il suo colore
- Griglie regolabili, cartelle nascoste e cartelle incluse a scelta
- Rinomina dei file, con un modello per rinominarne molti insieme (da Android 11)
- Cestino di sistema, con ripristino (da Android 11)

Visualizzatore
- Zoom gestuale fluido, fino al singolo pixel, anche su immagini enormi
- JPEG, PNG, WebP, AVIF, HEIC, SVG, GIF e WebP animati, e i video
- Informazioni complete sul file, condivisione, conversione in un altro formato

Editor
- Ritaglio, rotazione, ridimensionamento e regolazioni
- Disegno, testo e filigrana, con stili da salvare e riusare
- Un editor completo con le regolazioni avanzate (da Android 13)
- Il salvataggio crea sempre una copia in Download: l'originale resta com'è

Rispetto
- Nessuna pubblicità, nessun account, nessuna raccolta di dati
- Le immagini restano sul telefono
- 28 lingue, tema chiaro e scuro

### Descrizione completa, English

AIV (Astonishing Image Viewer) is an Android gallery made for people who work with images: it opens even the largest files in an instant and shows them down to the pixel, with a zoom that follows your fingers.

Folders
- Your photo and video folders, each with its own cover and color
- Adjustable grids, hidden folders and included folders of your choice
- File renaming, with a pattern to rename many at once (Android 11 and later)
- System trash, with restore (Android 11 and later)

Viewer
- Smooth pinch zoom, down to the single pixel, even on huge images
- JPEG, PNG, WebP, AVIF, HEIC, SVG, animated GIF and WebP, and videos
- Full file information, sharing, conversion to another format

Editor
- Crop, rotate, resize and adjust
- Drawing, text and watermark, with styles to save and reuse
- A full editor with advanced adjustments (Android 13 and later)
- Saving always creates a copy in Downloads: the original stays as it is

Respect
- No ads, no account, no data collection
- Your images stay on your phone
- 28 languages, light and dark theme

### Categoria e grafica

- **Categoria**: Fotografia.
- **Grafica richiesta da Google**: icona 512 x 512 in PNG, immagine in evidenza 1024 x 500, almeno
  due schermate del telefono. Con la sua risposta G1 a `d-store-grafica` (giro della `5.80`)
  icona e immagine in evidenza le compone `tools/store-graphics.py` in `docs/store/`, coi pezzi di
  `og-image.py` (l'icona intera, senza ritaglio, perché la maschera la applica Google), e lui le ha
  approvate così (H1 a `d-store-immagini`, giro della `5.90`); le schermate le fa lui dal telefono. Le schermate di `publish/schermate/` sono della paginetta e
  non hanno le misure di Google.

## 🛡️ Le risposte ai moduli della Console

### Sicurezza dei dati

- **L'app raccoglie o condivide dati utente?** No. AIV non manda dati a nessun servizio dello
  sviluppatore né di terzi: si collega solo all'indirizzo web che la persona apre.
- **Crittografia in transito ed eliminazione dei dati**: non si applicano, perché non c'è niente
  da trasmettere né da cancellare.

### Autorizzazioni per foto e video

AIV Play dichiara `READ_MEDIA_IMAGES` e `READ_MEDIA_VIDEO`, e Google chiede di motivarle. Il testo
da incollare, in inglese perché lo legge il revisore:

> AIV is a gallery app: its core purpose is to browse, view, organize and edit the photos and
> videos on the device. It needs broad access to show every folder of images and videos, with
> covers, grids, renaming and the system trash.

Dalla `6.27` (sua scelta R1) AIV Play dichiara e chiede anche `ACCESS_MEDIA_LOCATION`, che da
Android 10 serve per leggere le coordinate nell'EXIF delle foto: la riga 'Posizione' della scheda
Info le mostra sul telefono, e non escono dall'app. Per questo la risposta della Sicurezza dei dati
qui sopra resta 'no': l'app legge la posizione delle foto ma non la raccoglie né la condivide.

### Classificazione dei contenuti

- **Tipo di app**: tutte le altre (non è un gioco, non è un social).
- **Violenza, sesso, linguaggio, sostanze, gioco d'azzardo**: no a tutto.
- **Interazione fra utenti, contenuti condivisi fra utenti, posizione**: no.

### Pubblico di destinazione e annunci

- **Fasce d'età**: 18 anni e oltre, il parere dell'agente: una fascia che comprende i minori
  chiede gli obblighi in più delle app per famiglie, e AIV non è pensata per loro.
- **Annunci**: no.
- **Accesso all'app**: tutte le funzioni sono disponibili senza credenziali.

## 🧭 I passi dell'utente, in ordine

1. **Account sviluppatore** su <https://play.google.com/console>: account personale, 25 dollari
   una volta sola, verifica dell'identità con un documento.
2. **Crea l'app**: nome `AIV - Astonishing Image Viewer`, lingua predefinita italiano, app
   gratuita.
3. **Test chiuso**: crea la traccia, carica `AIV-Play-x.xx.aab` dell'ultima release, aggiungi i
   tester (un elenco di indirizzi email o un gruppo Google) e manda loro il link di adesione. Un
   account personale nuovo può chiedere la produzione solo dopo **12 tester iscritti per 14 giorni
   di fila** (<https://support.google.com/googleplay/android-developer/answer/14151465>).
4. **Compila la scheda e i moduli** con i testi di questo file.
5. **Chiedi l'accesso alla produzione** dalla Console, finiti i 14 giorni.
6. **Verifica degli sviluppatori per AIV GitHub**: dal 30 settembre 2026 in Brasile, Indonesia,
   Singapore e Thailandia, nel 2027 ovunque, un'app installata fuori da Play deve venire da uno
   sviluppatore registrato. Con l'account della Console la registrazione dei pacchetti passa da lì
   (<https://developer.android.com/developer-verification/guides/faq>): il pacchetto
   `io.github.roccobot.aiv` si registra con la sua chiave, prima del 2027.
