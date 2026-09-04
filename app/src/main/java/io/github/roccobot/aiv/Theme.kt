package io.github.roccobot.aiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive, and how it got here: in material3 1.4.0, the stable
 * release, MaterialExpressiveTheme and MotionScheme are declared INTERNAL and
 * the compiler refuses them outright. They are public only on the 1.5.0 line,
 * which is alpha, so material3 is pinned outside the Compose BOM.
 * The owner chose the alpha deliberately, this being an experimental project.
 * The cost is written down rather than forgotten: alpha APIs change between
 * releases, so a library bump can break this file. It is one file on purpose.
 *
 * ⚠️⚠️ **IL COLORE DINAMICO È USCITO NELLA 0.44, e non è una semplificazione: è la
 * conseguenza necessaria di aver scelto una tavolozza.** Fino a lì, su Android 12 e
 * successivi, l'app prendeva i colori dallo sfondo del telefono, quindi qualunque tinta
 * scritta qui era invisibile a quasi tutti. L'utente ha chiesto un fondo, un accento e
 * un colore del tastino precisi: o si tengono quelli, o si tiene il colore dinamico.
 * Chi volesse rimetterlo cancellerebbe la scelta, non aggiungerebbe un'opzione.
 */

/**
 * ⚠️ **IL FONDO CHIARO È QUASI BIANCO E LEGGERMENTE GIALLO** (richiesta dell'utente):
 * `#FCFBF8` sta un soffio sotto il bianco e ha il rosso e il verde più alti del blu, che
 * è quello che si legge come calore. ⚠️ Non è un beige: serve a togliere il taglio del
 * bianco puro senza che il fondo diventi un colore.
 * ⚠️⚠️ **LA SATURAZIONE È SCESA DUE VOLTE, e ogni volta è una misura e non un
 * aggiustamento a occhio**: si conta la distanza fra il canale più alto e il più basso,
 * su 255.
 *
 * | quando | fondo | punti | saturazione |
 * |---|---|---|---|
 * | fino alla `0.44` | `#FAF8EF` | 11 | 4.4% |
 * | `0.45` | `#FCFBF5` | 7 | 2.8% |
 * | `1.13` | `#FCFBF8` | 4 | 1.6% |
 *
 * Le due richieste sono la stessa richiesta fatta due volte (`0.44`: *solo leggermente meno
 * saturo, dev'essere solo un accenno di giallo/crema*; `1.13`: *ancora leggermente meno
 * saturazione del giallo*), ed è il motivo per cui i valori scartati restano scritti qui:
 * chi ritocca la terza volta deve sapere da dove si scende, o rifarà il primo passo.
 * ⚠️ **Su un fondo quasi bianco la differenza fra 'accenno' e 'crema' sta in tre punti**, e
 * il passo di questa scala è appunto tre.
 * ⚠️ **Tutta la famiglia chiara scende insieme**, superfici e contenitori compresi, e il
 * fattore è lo stesso per tutti (i punti di ciascuno moltiplicati per 4/7, cioè il rapporto
 * fra il fondo nuovo e quello di prima): abbassare il solo fondo lascerebbe le schede più
 * gialle della pagina, che è il modo in cui una tavolozza smette di sembrare una tavolozza.
 * ⚠️⚠️ **E scende con loro `window_background` di `colors.xml`, che è l'unico valore fuori
 * da questo file**: lo dipinge il sistema prima che la composizione esista, quindi se resta
 * indietro l'app si apre con un lampo del fondo vecchio.
 */
private val LIGHT_BACK = Color(0xFFFCFBF8)

/** La superficie chiara: un filo sopra il fondo, così una scheda si stacca da sola. */
private val LIGHT_SURFACE = Color(0xFFFFFEFC)

/**
 * ⚠️ **IL FONDO SCURO È GRIGIO, NON VERDE** (richiesta dell'utente: grigio scuro
 * leggermente blu/verde): `#151B1A` ha il verde e il blu appena sopra il rosso, cioè la
 * quantità di tinta che si sente e non si nomina. Un verde scuro vero avrebbe il verde
 * molto più alto degli altri due, e sotto una fotografia si vedrebbe.
 */
private val DARK_BACK = Color(0xFF151B1A)

/** La superficie scura, un gradino sopra il fondo. */
private val DARK_SURFACE = Color(0xFF1E2523)

/**
 * L'accento, cioè il colore del tastino e di tutto quello che l'app evidenzia.
 *
 * ⚠️⚠️ **È ESATTAMENTE IL COLORE DELL'ICONA** (`launcher_background` chiaro), per volontà
 * dell'utente: l'app e la sua icona devono essere la stessa cosa. Chi lo cambia deve
 * cambiare anche l'icona, o si separano.
 * ⚠️ **Contrasto MISURATO contro il fondo chiaro: 2.43** (era 2.37 sul fondo della
 * `0.44`, più saturo), cioè sotto il 3:1 che si chiede a una grafica non testuale. Non è
 * un difetto da correggere di nascosto: il colore lo ha scelto l'utente, che ha visto il
 * numero e ha risposto *accento OK*. È scritto qui perché nessuno debba rimisurarlo, ed è
 * lo stesso baratto già accettato per l'icona, che misura 2.42. Chi un giorno volesse
 * rientrare nella soglia deve **scurire l'accento**, non schiarire il fondo.
 * ⚠️ Quello che sta SOPRA il tastino invece si legge benissimo: `#00382F` sull'accento
 * misura 5.19.
 */
private val ACCENT_LIGHT = Color(0xFF43B59E)
private val ON_ACCENT_LIGHT = Color(0xFF00382F)

/**
 * L'accento scurito, per il tema scuro.
 *
 * ⚠️ **È la stessa tinta dell'icona scura** (`launcher_background` di `values-night`),
 * quindi anche qui l'app e la sua icona restano la stessa cosa, ed è la lettura letterale
 * della richiesta ('scurito su scuro'). ⚠️ **Misure**: 3.07 contro il fondo scuro, cioè
 * appena sopra la soglia delle grafiche non testuali, e 5.69 per il bianco sopra di lui.
 * A differenza del chiaro, qui il conto torna.
 */
private val ACCENT_DARK = Color(0xFF00727B)

/**
 * L'accento quando deve essere **letto**, cioè scritto come testo.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ L'ACCENTO VERO, COME TESTO, NON SI LEGGE**: `#43B59E` su fondo
 * chiaro misura 2.43, e per un testo la soglia è 4.5. L'accento è quello che l'utente ha
 * chiesto e non si tocca; quello che si tocca è l'unico posto in cui l'accento finiva
 * sotto forma di parole, cioè il collegamento a `roccobot.me` in fondo alle impostazioni.
 * ⚠️ **Restano i colori dell'icona**, che è il punto della richiesta: il chiaro è la
 * stessa tinta scurita (**6.21** sul fondo), lo scuro è **esattamente** il glifo
 * dell'icona scura, `launcher_foreground` di `values-night` (**9.97** sul fondo).
 * ⚠️ Chi volesse un accento leggibile dappertutto deve scurire `ACCENT_LIGHT`, e allora
 * questa coppia sparisce da sé: è nata da una deroga, non da una preferenza.
 */
private val LINK_LIGHT = Color(0xFF0B6B5B)
private val LINK_DARK = Color(0xFF4FD9BE)

/**
 * ⚠️⚠️ **IL TASTINO NON PRENDE `primary`, PRENDE `primaryContainer`**, e saperlo è la
 * differenza fra una tavolozza applicata e una tavolozza scritta. Verificato sul bytecode
 * di material3 1.5.0-alpha26: `FloatingActionButtonDefaults.containerColor`
 * risolve il token `PrimaryContainer`. Lasciando quel ruolo al suo valore di serie, il
 * tastino sarebbe rimasto **viola** in mezzo a tutto il resto.
 * ⚠️ Qui `primaryContainer` vale **quanto** `primary`, e non è una svista: la richiesta
 * dice un accento solo, e un contenitore più tenue sarebbe un secondo accento.
 *
 * ⚠️ **Gli altri ruoli si dichiarano per la stessa ragione**: quelli che restano
 * impostati portano la tavolozza di serie, che è viola. `secondaryContainer` lo
 * consumano il tasto tondeggiante e la pastiglia selezionata, `outline` il bordo
 * dell'interruttore e del campo di testo, la famiglia `surfaceContainer` i menu a
 * tendina. `error` invece resta quello di serie apposta: il rosso di un errore non è un
 * colore del marchio, e il suo `#BA1A1A` misura 5.2 su questo fondo.
 */
private val LightScheme: ColorScheme = lightColorScheme(
    primary = ACCENT_LIGHT,
    onPrimary = ON_ACCENT_LIGHT,
    primaryContainer = ACCENT_LIGHT,
    onPrimaryContainer = ON_ACCENT_LIGHT,
    secondary = Color(0xFF4C635C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE8E0),
    onSecondaryContainer = Color(0xFF0B3A31),
    background = LIGHT_BACK,
    onBackground = Color(0xFF1A1C1B),
    surface = LIGHT_SURFACE,
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFEFEEEA),
    onSurfaceVariant = Color(0xFF5B6360),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFCFA),
    surfaceContainer = Color(0xFFF8F7F4),
    surfaceContainerHigh = Color(0xFFF3F1EE),
    surfaceContainerHighest = Color(0xFFEDECE8),
    outline = Color(0xFF79817D),
    outlineVariant = Color(0xFFDAD9D5)
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = ACCENT_DARK,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = ACCENT_DARK,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF9FBDB5),
    onSecondary = Color(0xFF06372F),
    secondaryContainer = Color(0xFF2E4A44),
    onSecondaryContainer = Color(0xFFC7E4DC),
    background = DARK_BACK,
    onBackground = Color(0xFFE4E9E7),
    surface = DARK_SURFACE,
    onSurface = Color(0xFFE4E9E7),
    surfaceVariant = Color(0xFF2A312F),
    onSurfaceVariant = Color(0xFFA9B3B0),
    surfaceContainerLowest = Color(0xFF0F1413),
    surfaceContainerLow = Color(0xFF1A201F),
    surfaceContainer = Color(0xFF1E2523),
    surfaceContainerHigh = Color(0xFF283130),
    surfaceContainerHighest = Color(0xFF333B39),
    outline = Color(0xFF8A9490),
    outlineVariant = Color(0xFF3C4442)
)

/**
 * Il colore di un collegamento, cioè dell'accento quando è fatto di parole.
 *
 * ⚠️ Si sceglie guardando il **fondo del tema in vigore** e non `isSystemInDarkTheme()`:
 * la domanda a cui deve rispondere è 'su che cosa sto scrivendo', e l'unica risposta che
 * resta vera se un domani il tema si potesse forzare è quella che guarda la tavolozza.
 */
@Composable
fun accentInk(): Color =
    if (MaterialTheme.colorScheme.background.isLight()) LINK_LIGHT else LINK_DARK

/**
 * Il filo di una superficie **contornata**: quella che esiste per il suo bordo e non per il
 * suo riempimento.
 *
 * ⚠️⚠️ **NASCE PERCHÉ `outlineVariant` SI VEDEVA SOLO SUL TEMA CHIARO, e il numero da solo
 * diceva il contrario** (domanda dell'utente, 2026-09-03, sulla pastiglia del nome nella
 * rinomina: *sbaglio o ha un filetto di contorno solo nel tema chiaro?*). Non sbagliava, e la
 * misura che sembrava smentirlo è la trappola da tenere: `outlineVariant` contro l'interno
 * della pastiglia stacca **1,86 sullo scuro** e **1,41 sul chiaro**, cioè sullo scuro di più.
 * ⚠️⚠️ **A DECIDERE NON È QUEL RAPPORTO, È QUANTO LAVORO FA GIÀ IL RIEMPIMENTO**: sul chiaro
 * l'interno della pastiglia sta a 1,13 dalla superficie del dialogo, cioè non si vede, e il
 * filo è la sola cosa che disegna la forma; sullo scuro l'interno sta a 1,39, cioè legge già
 * come un incavo, e un filo a 1,33 dalla superficie non aggiunge niente all'occhio. Provato
 * **guardandolo**, reso a tre volte la densità, e non contando: la pastiglia scura col filo e
 * quella senza sono indistinguibili.
 * ⚠️ **La via scartata era `outline` in tutti e due i temi**, che è una tinta sola invece di
 * due: sullo scuro va benissimo (4,27 dalla superficie) ma sul chiaro salta a 3,55, cioè
 * diventa una cornice, e sul chiaro il filo l'utente lo aveva già approvato com'era. Questi
 * due sono il **punto medio** fra `outlineVariant` e `outline`, calcolato e non scelto a
 * occhio: portano il chiaro da 1,25 a **2,01** e lo scuro da 1,33 a **2,47**, cioè un filo
 * che si vede in tutti e due i temi e in nessuno dei due pesa.
 * ⚠️ **Si sceglie guardando il fondo del tema in vigore**, come [accentInk] e per la stessa
 * ragione: la domanda è 'su che cosa sto disegnando'.
 */
@Composable
fun hairline(): Color =
    if (MaterialTheme.colorScheme.background.isLight()) HAIRLINE_LIGHT else HAIRLINE_DARK

private val HAIRLINE_LIGHT = Color(0xFFAAADA9)
private val HAIRLINE_DARK = Color(0xFF636C69)

/**
 * Il fondo del palco dell'editor: il grigio su cui galleggia l'immagine da ritagliare.
 *
 * ⚠️⚠️ **NON È IL FONDO DELLA PAGINA, e la ragione è di lavoro e non di gusto** (istruzione
 * dell'utente, 2026-09-04: *il bianco non va bene comunque, perché se si ha a che fare con
 * numerosi screenshot (che hanno molto bianco) non si capisce a colpo d'occhio dove
 * finiscono*). Il palco è la sola superficie dell'app su cui appoggia un'immagine di cui non
 * si sa niente: un fondo quasi bianco confina con una schermata bianca senza che si veda il
 * confine, cioè non si sa più dove finisce il file e dove comincia l'app.
 * ⚠️ **I due numeri sono suoi**, un grigio al 40% sul chiaro e al 70% sullo scuro, letti come
 * quantità di nero. **Misure**: sul chiaro `#999999` stacca **2,53** dalla scheda dei comandi
 * e **2,85** dal bianco puro, dove il quasi-bianco di prima stava a **1,09** e **1,03**, cioè
 * non staccava affatto.
 * ⚠️⚠️ **SULLO SCURO IL GUADAGNO CONTRO LA SCHEDA È PICCOLO, e va detto invece di lasciarlo
 * scoprire**: `#4D4D4D` stacca **1,58** dalla scheda contro l'**1,31** di prima, perché la
 * scheda scura è anche lei un grigio. Contro un'immagine bianca invece stacca **8,45**, che è
 * il problema che questo colore deve risolvere. Chi volesse più stacco dalla scheda sullo
 * scuro deve **schiarire** questo grigio, non scurirlo.
 * ⚠️ **È un grigio NEUTRO e non tinto come gli altri neutri dell'app**: dietro un'immagine una
 * tinta si somma ai suoi colori e li fa leggere sbagliati. È la stessa ragione per cui il
 * fondo sotto la lente è nero pieno.
 */
@Composable
fun stageBack(): Color =
    if (MaterialTheme.colorScheme.background.isLight()) STAGE_BACK_LIGHT else STAGE_BACK_DARK

private val STAGE_BACK_LIGHT = Color(0xFF999999)
private val STAGE_BACK_DARK = Color(0xFF4D4D4D)

/**
 * Il colore delle maniglie del ritaglio nell'editor: l'accento dell'app.
 *
 * ⚠️⚠️ **UGUALE NEI DUE TEMI, e non è una dimenticanza**: a differenza di [accentInk] e di
 * [hairline], qui la domanda 'su che cosa sto disegnando' ha una risposta che **non dipende
 * dal tema**, perché sotto le maniglie non c'è il fondo dell'app ma l'immagine velata, cioè
 * un colore qualunque scurito del 55%. Un accento scuro per il tema scuro sparirebbe proprio
 * là ([ACCENT_DARK] misura 1,20 sul velo di un'immagine bianca).
 * ⚠️ **Da solo non basta, e la misura lo dice**: 1,88 su quel velo, cioè sotto il 3:1. È la
 * ragione per cui la maniglia porta anche un filo bianco intorno, e il perché per esteso sta
 * su `bracket` in `EditorScreen.kt`, dove il disegno vive.
 */
val CROP_GRIP = ACCENT_LIGHT

/**
 * La stondatura di un riquadro dentro una finestra: un campo da riempire o una pastiglia.
 *
 * ⚠️⚠️ **STA QUI PERCHÉ PRIMA NON STAVA DA NESSUNA PARTE, ed è la ragione per cui i riquadri
 * di una stessa finestra divergevano** (riscontro della `1.45` sulla rinomina: *le stondature
 * devono essere tutte uguali*). Le pastiglie la prendevano da `shapes.small`, cioè 8, e i
 * campi da Material, che per un campo contornato usa `shapes.extraSmall`, cioè 4: nessuno dei
 * due numeri era scritto nel file che li disegnava, quindi non c'era un posto in cui
 * accorgersi che erano due.
 * ⚠️ **Vince la misura delle pastiglie e non quella dei campi**: è la direzione che indica il
 * riscontro (*il primo riquadro, solo contorno, è troppo poco arrotondato*), ed è la
 * stondatura che le pastiglie hanno già, quindi cambia solo ciò che è stato segnalato.
 * ⚠️ **Non è una regola generale dell'app** ma la forma dei riquadri delle finestre, ed è
 * quello che il riscontro chiedeva: *non regola generale, ma almeno in questo contesto*.
 */
val BOX_SHAPE = RoundedCornerShape(8.dp)

/**
 * Lo spessore del filo di un riquadro.
 *
 * ⚠️ **È quello che Material disegna intorno a un campo a riposo**, quindi i riquadri scritti
 * in casa e i campi portano la stessa linea senza che nessuno debba pareggiarli.
 * ⚠️⚠️ **UN CAMPO COL FUOCO DENTRO SALE A 2, E QUESTO NUMERO NON LO GOVERNA**: quel valore
 * vive in `OutlinedTextFieldDefaults.FocusedBorderThickness` e nessuna firma di
 * `OutlinedTextField` lo espone al chiamante. Per pareggiare anche quello bisognerebbe
 * smettere di usare quel componente e ricostruirlo con `BasicTextField` più la sua
 * `DecorationBox`, che gli spessori li prende come parametri: una trentina di righe per
 * campo. È dichiarato invece che nascosto, perché è l'unico caso in cui i quattro riquadri
 * non portano la stessa linea.
 */
val BOX_EDGE = 1.dp

/**
 * Se questo colore è chiaro, cioè se sopra ci si scrive in scuro.
 *
 * ⚠️ **Non è più privato dalla 1.38**: la stessa domanda se la fa il velo delle finestre
 * (`WindowVeil`), che deve sapere di che tema è vestita l'app **in quel momento** e non che
 * cosa dice il sistema. Due luminanze scritte in due file divergerebbero al primo ritocco
 * della formula.
 */
fun Color.isLight(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f

/**
 * Se l'app va vestita di scuro, secondo la scelta dell'utente.
 *
 * ⚠️ Sta qui e non in `AivTheme` perché la scelta arriva **prima** del tema, dal
 * salvataggio: chi la legge deve poterla tradurre in un `Boolean` da passare al tema, e
 * quella traduzione ha bisogno di `isSystemInDarkTheme()`, che è componibile.
 */
@Composable
fun UiTheme.isDark(): Boolean = when (this) {
    UiTheme.SYSTEM -> isSystemInDarkTheme()
    UiTheme.LIGHT -> false
    UiTheme.DARK -> true
}

/**
 * ⚠️ **Il parametro `darkTheme` NON è più solo il sistema, dalla 0.45**: chi chiama gli
 * passa la scelta dell'utente già risolta (vedi [isDark]). Il valore di serie resta il
 * sistema, così un'anteprima o una chiamata senza impostazioni si comporta come prima.
 * ⚠️⚠️ **Quello che il tema NON può governare è il primo fotogramma**: lo sfondo della
 * finestra lo dipinge il sistema prima che Compose esista, e lo prende da
 * `window_background`, che ha una sola versione chiara e una scura scelte dal **tema del
 * dispositivo**. Chi forza il chiaro su un telefono scuro vedrà quindi un lampo scuro
 * all'apertura. Non è rimediabile da qui, e per rimediarlo servirebbe leggere la scelta
 * prima di `setContentView`, cioè fuori da Compose: dichiarato invece di nascosto.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/**
 * Se l'app in questo momento è vestita di **chiaro**.
 *
 * ⚠️⚠️ **SERVE AL VELO DELLE FINESTRE, e non si poteva ricavare da `MaterialTheme`**: il velo
 * si applica da un **nodo di modificatore** (vedi `Modifier.veiled`), e un nodo legge solo i
 * `CompositionLocal` pubblici, mentre la tavolozza di Material vive dietro uno interno alla
 * libreria. Questo è nostro, quindi si legge da dove serve.
 * ⚠️ **Dice il tema IN VIGORE e non quello di sistema**: chi ha forzato il chiaro con l'app
 * di sistema in scuro deve avere il velo del chiaro. Per questo lo fornisce [AivTheme], che è
 * il posto in cui quella scelta è già stata fatta.
 * ⚠️ **Arriva anche dentro i dialoghi**: un `Dialog` eredita i `CompositionLocal` da dove è
 * scritto, non dalla finestra in cui finisce.
 */
val LocalAivLight = staticCompositionLocalOf { true }

@Composable
fun AivTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    // ⚠️ Il tema in vigore, dichiarato per chi non può leggere la tavolozza di Material. Il
    // perché sta su [LocalAivLight].
    CompositionLocalProvider(LocalAivLight provides !darkTheme) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive()
        ) {
            // ⚠️⚠️ QUESTA `Surface` NON È DECORAZIONE, ED È LA SOLA COSA CHE DÀ UN
            // COLORE AL TESTO. Il tema porta la tavolozza ma NON tocca
            // `LocalContentColor`: quello lo imposta `Surface`, e senza di lei resta
            // al suo default, che è il NERO FISSO. Quindi ogni `Text` e ogni `Icon`
            // senza colore dichiarato usciva nero: in tema chiaro non si vede, in
            // tema scuro il testo spariva nel fondo. Segnalato dall'utente sul nome
            // dell'app nella schermata iniziale, ma il difetto era di tutta l'app, e
            // per questo il rimedio sta QUI e non su quella riga: rimediare al punto
            // dove si è visto avrebbe lasciato gli altri, uno per volta.
            // ⚠️ Sotto al visualizzatore non si vede, e va bene così: quello dipinge
            // già il proprio fondo a tutta schermata, quindi questa gli sta dietro
            // senza cambiargli niente.
            Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
                content()
            }
        }
    }
}
