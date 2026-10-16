package io.github.roccobot.aiv

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive: `MaterialExpressiveTheme` e `MotionScheme` sono pubbliche solo sulla
 * linea 1.5.0 di material3, che arriva dal BOM di prova di Compose (scelta dell'utente, 6.24;
 * il perché è in `gradle/libs.versions.toml`). Una versione di prova può cambiare queste API da
 * un aggiornamento all'altro, ed è per questo che il tema vive in un file solo.
 *
 * ⚠️ **Niente colore dinamico**: con lui, da Android 12, l'app prenderebbe i colori dallo sfondo
 * del telefono, e la tavolozza chiesta dall'utente (fondo, accento e FAB) non si vedrebbe.
 */

/**
 * Il fondo chiaro: quasi bianco e appena giallo (richiesta dell'utente). `#FCFBF8` ha il rosso e
 * il verde più alti del blu, che si legge come calore, senza diventare un beige.
 * La saturazione si misura come distanza fra il canale più alto e il più basso, su 255: qui 4
 * punti, cioè 1,6%. L'utente l'ha chiesta più bassa due volte (da 11 a 7, poi a 4): un nuovo
 * ritocco riparte da qui.
 * ⚠️ **Tutta la famiglia chiara si muove insieme**, superfici e contenitori compresi, con lo
 * stesso fattore: abbassare il solo fondo lascerebbe le schede più gialle della pagina.
 * ⚠️ **Con loro si muove `window_background` di `colors.xml`**, l'unico valore fuori da questo
 * file: lo dipinge il sistema prima che la composizione esista, e se resta indietro l'app si apre
 * con un lampo del fondo vecchio.
 */
private val LIGHT_BACK = Color(0xFFFCFBF8)

/** La superficie chiara: un filo sopra il fondo, così una scheda si stacca da sola. */
private val LIGHT_SURFACE = Color(0xFFFFFEFC)

/**
 * Il fondo scuro: un grigio appena blu e verde, non un verde (richiesta dell'utente). `#151B1A`
 * ha il verde e il blu appena sopra il rosso; un verde scuro vero si vedrebbe sotto un'immagine.
 */
private val DARK_BACK = Color(0xFF151B1A)

/** La superficie scura, un gradino sopra il fondo. */
private val DARK_SURFACE = Color(0xFF1E2523)

/**
 * L'accento, cioè il colore del FAB e di tutto quello che l'app evidenzia.
 *
 * ⚠️ **È esattamente il colore dell'icona** (`launcher_background` chiaro), per volontà
 * dell'utente: chi lo cambia cambia anche l'icona.
 * ⚠️ **Contrasto col fondo chiaro: 2,43**, sotto il 3:1 di una grafica non testuale. L'utente
 * ha visto il numero e ha confermato l'accento; per rientrare nella soglia si scurisce l'accento,
 * non si schiarisce il fondo. Sopra il FAB `#00382F` misura 5,19.
 */
private val ACCENT_LIGHT = Color(0xFF43B59E)
private val ON_ACCENT_LIGHT = Color(0xFF00382F)

/**
 * L'accento scurito, per il tema scuro.
 *
 * È la stessa tinta dell'icona scura (`launcher_background` di `values-night`). Misura 3,07 col
 * fondo scuro, appena sopra la soglia delle grafiche non testuali, e 5,69 col bianco sopra.
 */
private val ACCENT_DARK = Color(0xFF00727B)

/**
 * L'accento quando deve essere **letto**, cioè scritto come testo.
 *
 * ⚠️ **Esiste perché l'accento, come testo, non si legge**: `#43B59E` sul fondo chiaro misura
 * 2,43, e per un testo la soglia è 4,5. Restano i colori dell'icona: il chiaro è la stessa tinta
 * scurita (6,21 sul fondo), lo scuro è il glifo dell'icona scura, `launcher_foreground` di
 * `values-night` (9,97). Se un giorno `ACCENT_LIGHT` si scurisse, questa coppia non servirebbe
 * più.
 */
private val LINK_LIGHT = Color(0xFF0B6B5B)
private val LINK_DARK = Color(0xFF4FD9BE)

/**
 * ⚠️ **Il FAB non prende `primary`, prende `primaryContainer`**: lo dicono i sorgenti di
 * material3 1.5.0-beta01 (`FloatingActionButtonDefaults.containerColor`). Col valore di serie il
 * FAB sarebbe viola. Qui `primaryContainer` vale quanto `primary`: l'accento è uno solo.
 *
 * Gli altri ruoli si dichiarano per la stessa ragione, o resterebbero viola: `secondaryContainer`
 * per il tasto arrotondato e la pastiglia scelta, `outline` per il bordo dell'interruttore e del
 * campo di testo, la famiglia `surfaceContainer` per i menu a tendina. `error` resta quello di
 * serie: il rosso di un errore non è un colore del marchio, e `#BA1A1A` misura 5,2 su questo
 * fondo.
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
 * ⚠️ Si sceglie guardando il fondo del tema in vigore, non `isSystemInDarkTheme()`: il tema
 * scelto dentro l'app può essere diverso da quello di sistema.
 */
@Composable
fun accentInk(): Color =
    if (MaterialTheme.colorScheme.background.isLight()) LINK_LIGHT else LINK_DARK

/**
 * Il filo di una superficie **contornata**: quella che esiste per il suo bordo e non per il
 * suo riempimento.
 *
 * ⚠️ **`outlineVariant` si vede solo sul tema chiaro, anche se il rapporto di contrasto dice il
 * contrario**: a decidere è quanto lavoro fa già il riempimento. Sul chiaro l'interno di una
 * pastiglia è a 1,13 dalla superficie del dialogo e il filo è la sola cosa che disegna la forma;
 * sullo scuro l'interno è a 1,39 e si legge già come un incavo, quindi un filo a 1,33 non si vede.
 * Questi due colori sono il punto medio fra `outlineVariant` e `outline`: 2,01 dalla superficie
 * sul chiaro e 2,47 sullo scuro, un filo che si vede in tutti e due i temi senza diventare una
 * cornice (`outline` sul chiaro arriverebbe a 3,55).
 * Si sceglie guardando il fondo del tema in vigore, come [accentInk].
 */
@Composable
fun hairline(): Color =
    if (MaterialTheme.colorScheme.background.isLight()) HAIRLINE_LIGHT else HAIRLINE_DARK

private val HAIRLINE_LIGHT = Color(0xFFAAADA9)
private val HAIRLINE_DARK = Color(0xFF636C69)

/**
 * Il fondo del palco dell'editor: il grigio su cui galleggia l'immagine da ritagliare.
 *
 * ⚠️ **Non è il fondo della pagina** (istruzione dell'utente): su un fondo quasi bianco una
 * schermata bianca non mostra dove finisce. I due grigi sono suoi, al 40% di nero sul chiaro e
 * al 70% sullo scuro: `#999999` stacca 2,53 dalla scheda dei comandi e 2,85 dal bianco puro;
 * `#4D4D4D` stacca solo 1,58 dalla scheda scura, che è anche lei grigia, ma 8,45 da un'immagine
 * bianca, che è il caso da risolvere. Per più stacco dalla scheda si schiarisce, non si scurisce.
 * ⚠️ **È un grigio neutro**: dietro un'immagine una tinta si somma ai suoi colori e li falsa.
 * Per la stessa ragione il fondo sotto la lente è nero pieno.
 */
@Composable
fun stageBack(): Color =
    if (MaterialTheme.colorScheme.background.isLight()) STAGE_BACK_LIGHT else STAGE_BACK_DARK

private val STAGE_BACK_LIGHT = Color(0xFF999999)
private val STAGE_BACK_DARK = Color(0xFF4D4D4D)

/**
 * Il colore delle maniglie del ritaglio nell'editor: l'accento dell'app.
 *
 * ⚠️ **Uguale nei due temi**: sotto le maniglie non c'è il fondo dell'app ma l'immagine scurita
 * del 55%, e [ACCENT_DARK] sparirebbe (1,20 sopra un'immagine bianca scurita).
 * Da solo misura 1,88, sotto il 3:1: per questo la maniglia ha anche un filo bianco intorno
 * (vedi `bracket` in `EditorScreen.kt`).
 */
val CROP_GRIP = ACCENT_LIGHT

/**
 * La stondatura di un riquadro dentro una finestra: un campo da riempire o una pastiglia.
 *
 * Un valore solo perché i riquadri di una stessa finestra abbiano la stessa stondatura (riscontro
 * dell'utente sulla rinomina): Material darebbe 4 a un campo contornato (`shapes.extraSmall`) e 8
 * a una pastiglia (`shapes.small`). Vale la misura delle pastiglie. Non è una regola generale
 * dell'app: è la forma dei riquadri delle finestre.
 */
val BOX_SHAPE = RoundedCornerShape(8.dp)

/**
 * Lo spessore del filo di un riquadro.
 *
 * È quello che Material disegna intorno a un campo a riposo, quindi i riquadri dell'app e i
 * campi hanno la stessa linea.
 * ⚠️ **Un campo col cursore dentro sale a 2, e questo numero non lo governa**: il valore è in
 * `OutlinedTextFieldDefaults.FocusedBorderThickness`, che `OutlinedTextField` non espone. Per
 * pareggiarlo servirebbe ricostruire il campo con `BasicTextField` e la sua `DecorationBox`.
 */
val BOX_EDGE = 1.dp

/**
 * Il separatore sfumato dell'app: un filo di `outlineVariant` pieno a metà e sfumato in linea
 * retta fino ai due capi.
 *
 * È uno solo per tutta l'app (nota dell'utente su `d-filo-uno`): lo usano `CropRule` nell'editor
 * completo e l'anteprima di `RenameDialog`, e l'aria intorno resta del chiamante. Lo spessore è
 * [BOX_EDGE], il filo di un riquadro.
 * Sfuma ai due capi: una linea piena da bordo a bordo dividerebbe la superficie in due, mentre
 * serve uno stacco.
 */
@Composable
fun FadedRule(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BOX_EDGE)
            .background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to ink,
                    1f to Color.Transparent
                )
            )
    )
}

/**
 * Se questo colore è chiaro, cioè se sopra ci si scrive in scuro.
 *
 * Pubblico perché la stessa domanda se la fa la Sfocatura dietro i pannelli, che deve sapere di
 * che tema è vestita l'app in quel momento: la formula della luminanza resta in un posto solo.
 */
fun Color.isLight(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f

/**
 * Quanto resta visibile un comando **spento**: il valore con cui Material segna il contenuto
 * inattivo.
 *
 * Un valore solo per tutta l'app, accanto agli altri valori del tema. Esiste come costante perché
 * i componenti di Material lo applicano da sé, mentre un `Text` o un glifo scritti a mano no.
 */
const val OFF_INK = 0.38f

/**
 * Se l'app va vestita di scuro, secondo la scelta dell'utente.
 *
 * La scelta arriva dall'archivio prima del tema: chi la legge la traduce qui nel `Boolean` da
 * passare ad [AivTheme], e per la scelta 'di sistema' serve `isSystemInDarkTheme()`, che è
 * composabile.
 */
@Composable
fun UiTheme.isDark(): Boolean = when (this) {
    UiTheme.SYSTEM -> isSystemInDarkTheme()
    UiTheme.LIGHT -> false
    UiTheme.DARK -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/**
 * Se l'app in questo momento è vestita di chiaro.
 *
 * ⚠️ **Serve a chi disegna da un nodo di modificatore** (la Sfocatura dietro i pannelli,
 * `Modifier.veiled`): un nodo legge solo i `CompositionLocal` pubblici, e la tavolozza di Material
 * è dietro uno interno alla libreria.
 * Dice il tema in vigore, non quello di sistema, e per questo lo fornisce [AivTheme]. Arriva anche
 * dentro le finestre di dialogo: un `Dialog` eredita i `CompositionLocal` dal punto in cui è
 * scritto.
 */
val LocalAivLight = staticCompositionLocalOf { true }

/**
 * L'accento del tema in vigore, per chi lo deve leggere **da un nodo**.
 *
 * Esiste per la stessa ragione di [LocalAivLight]: il bordo d'accento (`Modifier.edged`) si
 * disegna da un nodo di modificatore. Sono le due costanti che la tavolozza usa per `primary`,
 * quindi il bordo e il FAB non possono divergere.
 */
fun aivAccent(light: Boolean): Color = if (light) ACCENT_LIGHT else ACCENT_DARK

/**
 * L'inchiostro sopra l'accento, per il tema chiesto.
 *
 * ⚠️ **Va insieme ad [aivAccent], mai scelto a parte**: il FAB premuto prende l'accento
 * dell'altro tema, e con lui il suo inchiostro. L'inchiostro chiaro (`#00382F`) sull'accento
 * scuro (`#00727B`) misura 2,05, cioè illeggibile, senza nessun errore. Le coppie sono quelle della
 * tavolozza (`primaryContainer` e `onPrimaryContainer`).
 */
fun aivOnAccent(light: Boolean): Color = if (light) ON_ACCENT_LIGHT else Color.White

/**
 * Il fondo dell'app per il tema chiesto.
 *
 * Serve a chi vuole il fondo dell'altro tema, che da `MaterialTheme` non si raggiunge: i due
 * tasti dello scorrimento hanno il fondo scuro sul tema chiaro e chiaro sullo scuro (istruzione
 * dell'utente). Sono le due costanti che le tavolozze usano per `background`.
 */
fun aivGround(light: Boolean): Color = if (light) LIGHT_BACK else DARK_BACK

/**
 * La coppia dell'icona dell'app (fondo e glifo) per il tema chiesto.
 *
 * ⚠️ **`colorResource` legge la configurazione di sistema**, mentre il tema dell'app è una sua
 * impostazione (`Rules.md` § '🌗 Il tema scelto DENTRO l'app non è quello di sistema'). Le
 * risorse restano la fonte: si leggono da un contesto con `uiMode` forzato, così il FAB segue
 * l'icona anche se la coppia cambia. Il contesto si crea una volta per tema.
 */
@Composable
fun aivLauncher(light: Boolean): Pair<Color, Color> {
    val base = LocalContext.current
    return remember(base, light) {
        val conf = Configuration(base.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (light) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        }
        val res = base.createConfigurationContext(conf).resources
        Color(res.getColor(R.color.launcher_background, null)) to
            Color(res.getColor(R.color.launcher_foreground, null))
    }
}

/**
 * La tavolozza di un tema per un pezzo di schermata, dentro un'app che può essere nell'altro.
 *
 * Serve alle informazioni mostrate con l'immagine: con 'Tema dello sfondo' su `Scuro` anche loro
 * vanno su un fondo scuro (nota dell'utente). Cambiano solo la tavolozza e [LocalAivLight]:
 * caratteri, forme e movimento sono quelli dell'app.
 */
@Composable
fun AivPalette(light: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAivLight provides light) {
        MaterialTheme(colorScheme = if (light) LightScheme else DarkScheme, content = content)
    }
}

/** Segnala ogni pressione a [OutsideTouch], nel primo passaggio e senza consumarla. */
private fun Modifier.watchPresses(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) event.changes.firstOrNull()?.let { OutsideTouch.press(it.position) }
        }
    }
}

/**
 * Il tema dell'app.
 *
 * Il parametro `darkTheme` riceve la scelta dell'utente già risolta (vedi [isDark]); il valore di
 * serie è il sistema, per un'anteprima o una chiamata senza impostazioni.
 * ⚠️ **Il primo fotogramma il tema non lo governa**: lo sfondo della finestra lo dipinge il
 * sistema prima che Compose esista, da `window_background`, scelto dal tema del dispositivo. Chi
 * forza il chiaro su un telefono scuro vede un lampo scuro all'apertura; per evitarlo bisognerebbe
 * leggere la scelta prima di `setContentView`, fuori da Compose.
 */
@Composable
fun AivTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    // Il tema in vigore, per chi non può leggere la tavolozza di Material (vedi [LocalAivLight]).
    CompositionLocalProvider(LocalAivLight provides !darkTheme) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive()
        ) {
            // ⚠️ Questa `Surface` è la sola cosa che dà un colore al testo: il tema non tocca
            // `LocalContentColor`, che senza di lei resta nero fisso, e ogni `Text` e `Icon`
            // senza colore dichiarato sparirebbe nel tema scuro. Il rimedio è qui, per tutta
            // l'app. Il visualizzatore dipinge il proprio fondo sopra, quindi non cambia.
            Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
                /*
                 * ⚠️ **La Sfocatura dietro i pannelli si dipinge qui, una sola per tutta l'app**
                 * (voce `flash-menu-dialogo`; il perché è su `VeilStage`): passando da un menu a
                 * una finestra di dialogo cambia solo la sua opacità, senza il lampo di due
                 * finestre che cambiano velo in fotogrammi diversi.
                 * È dentro la `Surface`, per il colore del testo, e dopo il contenuto, perché in
                 * un `Box` l'ultimo figlio è sopra. La superficie che si è aperta vive in
                 * un'altra finestra, sopra di lei.
                 */
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Ogni pressione, per chi ascolta (il Menu 'Start' aperto), mai consumata:
                        // sempre nell'albero, il perché è su [OutsideTouch].
                        .watchPresses()
                ) {
                    content()
                    AppVeil(modifier = Modifier.matchParentSize())
                    /*
                     * ⚠️ **Dopo la sfocatura, quindi sopra tutto**: mentre un menu è aperto,
                     * impedisce al tocco di arrivare anche all'app, perché da Android 12 la
                     * finestra di un popup non è modale al tocco. Non dipinge niente e vale
                     * sempre.
                     * ⚠️ **A menu chiuso non è nell'albero**: un nodo sempre presente bloccherebbe
                     * l'intera app, perché la ricerca del tocco si ferma sul primo ramo che
                     * colpisce. Il perché per esteso è in testa a [MenuGuard].
                     */
                    MenuGuard(modifier = Modifier.matchParentSize())
                }
            }
        }
    }
}
