# Feedback AIV

Versione **4.80**: nel modulo Disegno il menu della pressione lunga su un elemento, le maniglie per ridimensionarlo, la rotazione e lo strumento Sfocatura (giro cumulativo della 4.70 e della 4.80).
[il DF](https://aiv-feedback.roccobot-b90.workers.dev/feedback).
La Release 4.80 è pubblicata: [v4.80](https://github.com/Roccobot/AIV/releases/tag/v4.80), con l'APK
[AIV-4.80.apk](https://github.com/Roccobot/AIV/releases/download/v4.80/AIV-4.80.apk).
Commit prodotto su `main`: `341202c`, release dal commit `341202c` (SlimVer 4.80 / versionCode 345; APK 8.603.733 byte, digest `e6771deb`).
Tutti i veli d'aiuto, con le schermate: [Micro-onboarding di AIV](https://claude.ai/artifact/2HTm7ggPohapv6KMPEBLGE).
Per il testo (G3), il prossimo lavoro: scegli i caratteri, il nome dello stile della striscia e che cosa entra nell'APK nell'[artefatto dei caratteri](https://claude.ai/artifact/BnskaC7AgjgE5Dn23RVmoe), e incolla in chat la risposta che compone.

Questo è il documento condiviso da tutti gli agenti e le piattaforme.
La [guida di manutenzione](Feedback-maintenance.md) spiega come prenderlo in carico e aggiornarlo.
Giro **4.80**, cumulativo con la 4.70: quattro prove sugli elementi del Disegno e una sulla Sfocatura, e in fondo le **etichette testuali**: i testi dell'app nuovi o cambiati dalla 4.00, che non ti erano mai stati sottoposti, raggruppati per funzione, più una correzione che ti propongo. Campo vuoto vuol dire approvato. Giro **4.64**: due prove OK, e le dieci domande hanno tutte una risposta.

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

Le verifiche automatiche della 4.80 sono superate: banco di prova completo (651 prove), controllo delle traduzioni, compilazione.

| Voce | Stato | Commento dell'utente | Azione successiva |
|---|---|---|---|
| 4.70-01 | Non provato | | Attendere il collaudo. |
| 4.70-02 | Non provato | | Attendere il collaudo. |
| 4.70-03 | Non provato | | Attendere il collaudo. |
| 4.70-04 | Non provato | | Attendere il collaudo. |
| 4.80-01 | Non provato | | Attendere il collaudo. |

## 1. Il menu della pressione lunga su un elemento

Nel modulo Disegno disegna due rettangoli che si sovrappongono un poco. Tieni il dito fermo sul secondo: l'elemento risulta scelto, coi punti color accento, e si apre un menu di sei icone con la parola, come quello della pressione lunga sull'immagine nel visualizzatore ma senza la parte sopra. Nella prima riga `Sposta sopra`, `Copia`, `Duplica`; nella seconda `Sposta sotto`, `Ruota`, `Elimina`. Il secondo rettangolo è già in cima, quindi `Sposta sopra` è spento.

Tocca `Sposta sotto`: il rettangolo passa sotto il primo, e riaprendo il menu `Sposta sopra` è acceso e `Sposta sotto` spento. Tocca `Duplica`: compare una copia appena scostata in basso a destra, sopra l'originale, e risulta scelta. `Elimina` toglie l'elemento scelto, e `Annulla` lo rimette. Tenendo premuto un punto vuoto non si apre niente: dopo l'attesa il dito disegna come prima.

Letture mie, da confermare: l'ordine delle icone è quello della tua nota letto per righe; le parole sotto le icone seguono l'impostazione dei riquadri, come nel visualizzatore; la copia di `Duplica` è scostata del 3% del lato lungo dell'immagine; `Sposta sopra` e `Sposta sotto` scambiano l'elemento col vicino, un gradino per volta.

## 2. `Copia` e `Incolla` dello stile

Disegna un rettangolo blu, spesso e tratteggiato, e un'ellisse rossa senza tratteggio. Tieni premuto il rettangolo e tocca `Copia`. Tieni premuta l'ellisse: dove c'era `Copia` adesso c'è `Incolla`. Toccalo: l'ellisse prende colore, luminosità, opacità e spessore della linea, il tratteggio e il riempimento del rettangolo, e tiene forma, posizione e rotazione. Un `Annulla` toglie lo stile incollato.

Disegna una freccia e incollale lo stesso stile: prende la linea e non il riempimento, che una freccia non ha. Poi tieni premuto `Incolla`: compare l'avviso `Stile in memoria eliminato.`, e riaprendo il menu c'è di nuovo `Copia`.

Letture mie: lo stile resta in memoria finché l'editor è aperto, e si incolla su quanti elementi vuoi; fra rettangoli ed ellissi passa anche la Sfocatura (prova 5).

## 3. Le maniglie per ridimensionare

Scegli un rettangolo con un tocco: ha otto punti color accento, i quattro vertici e il mezzo di ogni lato. Trascina un vertice: si muovono i due lati che ci arrivano, e il vertice opposto resta fermo. Trascina il mezzo di un lato: si muove solo quel lato. Un punto si prende entro circa 16 dp; più in là il dito sposta l'elemento, come prima.

Prova anche l'ellisse e la mano libera, che si stira col suo riquadro e, trascinata oltre il lato opposto, si specchia. Su una linea e su una freccia i punti sono i due capi: trascinandone uno, vicino all'orizzontale o alla verticale si aggancia, con la guida, come quando la disegni.

Letture mie: i punti a metà dei lati sono un'aggiunta, per allungare lungo un asse solo; tirando un punto l'elemento non si appoggia ai bordi dell'immagine, come fa invece disegnando e spostando. Se lo vuoi anche lì, scrivilo.

## 4. `Ruota` e `Trasforma`

Scegli un rettangolo, tieni premuto e tocca `Ruota`: i punti diventano vuoti. Trascina un punto qualunque attorno all'elemento: gira attorno al suo centro, liberamente, e vicino a 0, 45 e 90 gradi, e ai loro multipli, si aggancia. Trascinando l'elemento lo sposti anche in questa modalità.

Riapri il menu: dove c'era `Ruota` adesso c'è `Trasforma`, che riporta i punti pieni. Tirando un vertice, il rettangolo girato si allarga lungo i suoi lati e non lungo quelli dell'immagine. Prova anche una freccia e una mano libera. Scegliendo un altro elemento si riparte da `Trasforma`.

Letture mie: l'aggancio ai multipli di 45 gradi scatta entro 5 gradi, come quello delle linee all'orizzontale; la modalità torna `Trasforma` quando cambia l'elemento scelto.

## 5. Lo strumento Sfocatura

Nel modulo Disegno, con il rettangolo come strumento, tocca il quinto tasto, `Sfocatura`. Si accende, e si spengono Tratteggio, Traccia, Spessore, Riempimento e i tondi dei colori; sotto, il cursore diventa `Sfocatura`. Disegna un rettangolo su una zona con dettagli, per esempio una scritta: dentro diventa sfocata, senza linea e senza riempimento. Sposta il cursore: va dallo 0,5 al 25% del lato maggiore dell'elemento.

Disegna sopra una freccia: resta nitida, perché la sfocatura è sotto tutti gli elementi. Cambia la Luce o il Colore, raddrizza l'immagine o ritagliala: l'area sfocata segue l'immagine e ne prende i colori. Ingrandisci con due dita sull'area: resta sfocata anche da vicino. Salva una copia, tenendo premuto `Salva`, e apri il file: l'area è sfocata come nell'editor.

Scegli il rettangolo sfocato e spegni il tasto: torna un rettangolo con la sua linea e il suo riempimento. Con linea, freccia e mano libera il tasto è spento.

Letture mie: il tasto è nella quinta colonna, dove dalla 4.60 alla 4.70 c'era `Elimina`, che adesso è nel menu della pressione lunga; di fabbrica la sfocatura vale il 10%; con la Sfocatura accesa i comandi della linea e del riempimento sono spenti, non nascosti.

Mentre disegni, sposti o ridimensioni un rettangolo sfocato, l'anteprima rifà la sfocatura a ogni movimento del dito: se l'elemento resta indietro rispetto al dito, scrivilo, e lo alleggerisco.

## Etichette testuali

### e-look_draw · Il modulo Disegno e i suoi strumenti
Disegno · Mano libera · Linea · Freccia · Rettangolo · Ellisse
<!-- chiavi: look_draw draw_free draw_line draw_arrow draw_rect draw_ellipse -->
<!-- Il nome del gettone del modulo e i cinque strumenti, che il lettore di schermo legge sui tasti. -->

### e-draw_outline · I tasti e i comandi del Disegno
Tratteggio · Traccia · Spessore · Riempimento · Sfocatura · Luminosità · Elimina tutto
<!-- chiavi: draw_dashed draw_outline draw_width draw_filled draw_blur draw_light draw_clear -->

### e-ink_red · I colori della tavolozza
Rosso · Ambra · Verde · Blu · Viola · Rosa · Bianco · Grigio · Nero
<!-- chiavi: ink_red ink_amber ink_green ink_blue ink_violet ink_pink ink_white ink_grey ink_black -->
<!-- I nomi che il lettore di schermo legge sui tondi. -->

### e-draw_raise · Il menu della pressione lunga su un elemento
Sposta sopra · Copia · Incolla · Sposta sotto · Ruota · Trasforma. Tenendo premuto `Incolla`, il lettore di schermo dice: Elimina lo stile in memoria. L'avviso: Stile in memoria eliminato.
<!-- chiavi: draw_raise draw_copy draw_paste draw_lower draw_rotate draw_transform draw_paste_clear draw_style_cleared -->

### e-hint_draw · Il velo d'aiuto del Disegno
Con il modulo Disegno puoi aggiungere all'immagine linee, frecce, ellissi, rettangoli arrotondati, testi semplici e riquadri 'pillola'. Il salvataggio appiattisce l'immagine: non è possibile riaprirla per modificare o spostare gli elementi. Premi a lungo su un colore per regolare la sua luminosità.

### e-settings_auto · I tre temi
Automatico · Chiaro · Scuro
<!-- chiavi: settings_auto settings_light settings_dark -->
<!-- Fino alla 4.00 erano al femminile: Automatica, Chiara, Scura. -->

### e-settings_main_control · L'elemento interattivo principale
Elemento interattivo principale. Scegli come vuoi interagire con l'app quando tieni lo smartphone in verticale: puoi scegliere tra pulsante fluttuante ('FAB') con menu, pillola con icone, menu e menu 'Start'. Le scelte: Tasto fluttuante · Pillola di icone · Menu basso · Menu 'Start'. L'opzione del menu basso: Fisso.
<!-- chiavi: settings_main_control settings_main_control_desc main_control_fab main_control_pill main_control_bar main_control_corner bar_fixed -->

### e-settings_button_look · L'aspetto dei pulsanti principali
Aspetto dei pulsanti principali. Riempimento ed effetti di trasparenza/sfocatura applicati al pulsante fluttuante, alla pillola di icone e ai menu. Le scelte: A scomparsa · Solido · Trasparente · Traslucido.
<!-- chiavi: settings_button_look settings_button_look_desc pill_slide pill_solid pill_translucent pill_glass -->
<!-- Fino alla 4.00 le scelte erano: A scorrimento, Tinta unita, Semitrasparente, Vetro satinato. -->

### e-glass_radius · I cursori del vetro
Raggio · Intensità · Opacità · Scostamento · Colore chiaro · Colore scuro · Predefinito · Ripristina
<!-- chiavi: glass_radius glass_intensity glass_tint glass_light glass_colour_light glass_colour_dark glass_default glass_reset -->

### e-start_pill · Il menu 'Start'
Il tasto a riposo: Pillola a riposo · Tondo a riposo. Nel menu: Pillola · Tondo · Griglia · Lista · Cartelle · Mostra · Nascondi · Apri URL · Crea · Apri · Ripristina · Svuota
<!-- chiavi: corner_rest_pill corner_rest_round start_pill start_round start_grid start_list start_tree start_show start_hide start_url start_new start_pick start_restore start_empty -->

### e-corner_hint · Il velo d'aiuto del menu 'Start'
Tocca il tasto nell'angolo per accedere al menu: contiene tutti i comandi principali; tienilo premuto per le opzioni di visualizzazione. Dalle impostazioni, se vuoi, puoi cambiare il lato del pulsante.

### e-bin_empty_hint · Le due scorciatoie del pulsante
Scorciatoia: tieni premuto il pulsante per svuotare il cestino. · Scorciatoia: tieni premuto il pulsante per le opzioni di visualizzazione.
<!-- chiavi: bin_empty_hint columns_hint -->
<!-- Fino alla 4.00 dicevano 'il tasto flottante' al posto di 'il pulsante'. -->

### e-settings_page_look · Il titolo della pagina dell'aspetto
Tema e dettagli grafici
<!-- Fino alla 4.00: Tema e pannelli. -->

### e-edit_no_file · Un file che non è sul telefono
Questo file non è sul telefono
<!-- Correzione proposta dall'agente: prima diceva `non sta sul telefono`, che usa 'stare' per dire dove una cosa si trova. -->

## Aggiornamenti recenti

| Funzione | Versione | Stato | Riscontro dell'utente | Azione successiva |
|---|---|---|---|---|
| Il menu della pressione lunga, `Copia` e `Incolla`, le maniglie e `Ruota` | 4.70-01, 4.70-02, 4.70-03, 4.70-04 | Non provato | | Attendere il collaudo. |
| Lo strumento Sfocatura | 4.80-01 | Non provato | | Attendere il collaudo. |
| `Elimina tutto`, e la pressione lunga sul Disegno | 4.64-01 | OK | | Concluso. |
| Le linee dei terzi di `Raddrizza` | 4.64-02 | OK | | Concluso. |
| Le dieci domande del brief | da 4.64-03 a 4.64-12 | Risposte | `Copia` copia lo stile e `Ruota` è una modalità libera; stili di casa dopo il Disegno; snellimento dei file di regole e Play Store dopo; nel DF solo le voci di AIV | Fatte nella 4.70 (prove `4.70-01`, `4.70-04`); il resto è nel brief. |

## Prossimi passi

- **In collaudo**: il menu della pressione lunga (`4.70-01`), `Copia` e `Incolla` dello stile (`4.70-02`), le maniglie (`4.70-03`), `Ruota` e `Trasforma` (`4.70-04`), lo strumento Sfocatura (`4.80-01`); le etichette testuali.
- **Da scegliere**: caratteri, nome dello stile della striscia e peso nell'APK, nell'artefatto dei caratteri, con la risposta in chat.
- **Dopo**: G3, il testo; la pillola; poi gli stili di casa con gli effetti e il file di uno stile solo; poi il Play Store, insieme allo snellimento dei file di regole.
- **Concluso**: `Elimina tutto` e la pressione lunga sul Disegno (`4.64-01`), le linee dei terzi di `Raddrizza` (`4.64-02`).
