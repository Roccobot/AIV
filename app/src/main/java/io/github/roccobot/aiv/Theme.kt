package io.github.roccobot.aiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
 * `#FAF8EF` sta un soffio sotto il bianco e ha il rosso e il verde più alti del blu, che
 * è quello che si legge come calore. ⚠️ Non è un beige: la differenza dal bianco è di
 * cinque punti su 255, e serve a togliere il taglio del bianco puro senza che il fondo
 * diventi un colore.
 */
private val LIGHT_BACK = Color(0xFFFAF8EF)

/** La superficie chiara: un filo sopra il fondo, così una scheda si stacca da sola. */
private val LIGHT_SURFACE = Color(0xFFFFFEF8)

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
 * ⚠️ **Contrasto MISURATO contro il fondo chiaro: 2.37**, cioè sotto il 3:1 che si chiede
 * a una grafica non testuale. Non è un difetto da correggere di nascosto, perché il
 * colore lo ha scelto l'utente: è un numero scritto qui perché nessuno debba rimisurarlo,
 * ed è lo stesso baratto già accettato per l'icona, che misura 2.42. Chi un giorno
 * volesse rientrare nella soglia deve **scurire l'accento**, non schiarire il fondo.
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
 * chiaro misura 2.37, e per un testo la soglia è 4.5. L'accento è quello che l'utente ha
 * chiesto e non si tocca; quello che si tocca è l'unico posto in cui l'accento finiva
 * sotto forma di parole, cioè il collegamento a `roccobot.me` in fondo alle impostazioni.
 * ⚠️ **Restano i colori dell'icona**, che è il punto della richiesta: il chiaro è la
 * stessa tinta scurita (**6.04** sul fondo), lo scuro è **esattamente** il glifo
 * dell'icona scura, `launcher_foreground` di `values-night` (**9.97** sul fondo).
 * ⚠️ Chi volesse un accento leggibile dappertutto deve scurire `ACCENT_LIGHT`, e allora
 * questa coppia sparisce da sé: è nata da una deroga, non da una preferenza.
 */
private val LINK_LIGHT = Color(0xFF0B6B5B)
private val LINK_DARK = Color(0xFF4FD9BE)

/**
 * ⚠️⚠️ **IL TASTINO NON PRENDE `primary`, PRENDE `primaryContainer`**, e saperlo è la
 * differenza fra una tavolozza applicata e una tavolozza scritta. Verificato sul bytecode
 * di material3 1.5.0-alpha26 e non a memoria: `FloatingActionButtonDefaults.containerColor`
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
    surfaceVariant = Color(0xFFEDEBE1),
    onSurfaceVariant = Color(0xFF5B6360),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFAF2),
    surfaceContainer = Color(0xFFF6F4EA),
    surfaceContainerHigh = Color(0xFFF0EEE4),
    surfaceContainerHighest = Color(0xFFEAE8DE),
    outline = Color(0xFF79817D),
    outlineVariant = Color(0xFFD6D3C7)
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

private fun Color.isLight(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AivTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
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
