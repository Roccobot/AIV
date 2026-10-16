package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * L'icona, il nome e la firma.
 *
 * Compare in due posti con due misure: in cima alla schermata delle cartelle, come volto dell'app,
 * e in fondo alle impostazioni, come 'chi siamo'. In tutti e due l'icona porta alla pagina di
 * download, e 'Roccobot' nella firma al dominio personale.
 * ⚠️ **Nome e firma non sono stringhe di risorsa, apposta**: tradurre 'made with love by Roccobot'
 * sarebbe un errore, e una risorsa inviterebbe a farlo. Si traduce solo la descrizione del tocco
 * sull'icona, che è una frase per il lettore di schermo.
 * ⚠️ **In un commento un indirizzo non si scrive con l'asterisco del corsivo attaccato**: `/` più
 * `*` apre un commento annidato in Kotlin, e il file non compila più.
 */
@Composable
fun Identity(
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(iconSize)
        // Lo spazio è [FRONT_GAP] di `Front.kt`, lo stesso dell'intestazione di una cartella: le
        // due fasce si somigliano.
        Spacer(Modifier.height(FRONT_GAP))
        Text(
            text = "Astonishing Image Viewer",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            // Il titolo della schermata iniziale, il primo punto in cui un lettore di schermo si
            // ferma: nella casa la testata non ne ha un altro.
            modifier = Modifier.heading()
        )
        Signature()
    }
}

/**
 * L'identità coricata: l'icona a sinistra, e accanto il nome su due righe, piccolo e tenue.
 *
 * Serve alla testa della colonna sul telefono in orizzontale (voce `3.70-05`), bassa e larga, dove
 * l'identità in colonna lasciava l'icona minuscola; sul tablet resta [Identity]. Nome al 70% e
 * senza firma, su richiesta dell'utente. L'a capo è scritto nella stringa: le due righe sono sue, e
 * una colonna più larga non deve unirle.
 */
@Composable
fun IdentityRow(
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(iconSize)
        Spacer(Modifier.width(FRONT_GAP * 2))
        Text(
            text = "Astonishing\nImage Viewer",
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(alpha = IDENTITY_ROW_INK),
            maxLines = 2,
            // ⚠️ Il titolo della schermata iniziale, come in [Identity].
            modifier = Modifier.heading()
        )
    }
}

/**
 * La testa della colonna delle cartelle nella schermata iniziale, sullo schermo largo.
 *
 * Sul telefono l'identità è coricata ([IdentityRow]) e la sua icona comincia a [RAIL_ROW_INSET] dal
 * bordo della colonna, allineata alle icone delle cartelle sotto (voce `3.72-04`). Sul tablet resta
 * [Identity], centrata.
 */
@Composable
fun RailIdentity(phone: Boolean) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val icon = minOf(HEADER_ICON, maxHeight * RAIL_IDENTITY_SHARE)
        if (phone) {
            IdentityRow(
                iconSize = icon,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = RAIL_ROW_INSET)
            )
        } else {
            Identity(iconSize = icon, modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** Quanto dell'altezza della testa prende l'icona, prima del tetto della fascia piena. */
private const val RAIL_IDENTITY_SHARE = 0.4f

/** L'opacità del nome nell'identità coricata: il numero è dell'utente. */
private const val IDENTITY_ROW_INK = 0.7f

/**
 * Quanto si ingrandisce il livello di primo piano perché questa anteprima mostri quello che mostra
 * il launcher.
 *
 * ⚠️ **`108 / 72`, cioè 1,5, per geometria**: di un'icona adattiva si vedono solo i 72 dp centrali
 * della tela da 108; l'anello esterno serve alla maschera e alla parallasse. Misurato sullo
 * screenshot del launcher: il glifo occupa 0,4398 e 0,5759 del lato della maschera, e i 72 dp
 * centrali senza altri ingrandimenti prevedono 0,4436 e 0,5819 (scarto dell'1%). Un ingrandimento
 * di HyperOS in più, ipotizzato in passato, darebbe uno scarto del 24%. La scala di
 * `ic_launcher_foreground.xml` (2/3) ha un'altra ragione, scritta là.
 */
private const val LAUNCHER_ZOOM = 1.5f

/**
 * La pagina di download dell'app, dove porta il tocco sull'icona: chi tocca l'icona cerca l'app, e
 * da lì il collegamento al codice c'è comunque.
 */
private const val PAGINA = "https://roccobot.github.io/AIV/"

/**
 * Il dominio personale dell'utente, dove porta la firma. Una costante perché un indirizzo scritto
 * dentro un `buildAnnotatedString` non si trova cercando.
 */
private const val HOME = "https://roccobot.me"

/**
 * L'icona del launcher, disegnata grande, identica nei due posti in cui compare (scelta
 * dell'utente: l'app e la sua icona sono la stessa cosa dovunque). Un difetto visto in un'anteprima
 * non è un motivo per cambiare la scala o l'alzata del drawable (vedi [LAUNCHER_ZOOM]).
 *
 * ⚠️ **L'ingrandimento si fa al disegno e non alla misura**: `Modifier.size` negozia col genitore
 * (`SizeNode.measure` chiama `constrain` quando `enforceIncoming` è vero, e per `size` lo è,
 * verificato nel bytecode di `foundation-layout`). Il `Box` qui sotto passa ai figli il massimo
 * della piastrella, e una misura più grande sarebbe ridotta: l'anteprima mostrerebbe la tela
 * intera. Un `Modifier.scale` è una trasformazione di disegno che nessun genitore limita.
 *
 * ⚠️ **I colori seguono il tema dell'app e non quello di sistema**: `colorResource` legge la
 * configurazione di sistema, e con Android in chiaro e AIV in scuro l'icona resterebbe chiara (il
 * perché per esteso è su [aivLauncher]). Qui il tema è quello in vigore, al contrario del FAB, che
 * prende la coppia dell'altro: l'icona dice di che colore è l'app, il FAB ci si stacca sopra.
 */
@Composable
private fun AppIcon(size: Dp) {
    val opener = LocalUriHandler.current
    val label = stringResource(R.string.identity_page)
    val (fondo, glifo) = aivLauncher(LocalAivLight.current)
    Box(
        modifier = Modifier
            .size(size)
            // Il ritaglio prima del tocco, o gli angoli fuori dalla forma risponderebbero.
            .clip(RoundedCornerShape(percent = 24))
            .background(fondo)
            .clickable(onClickLabel = label) { opener.openUri(PAGINA) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            tint = glifo,
            // Descritta perché è un comando, in tutti e due i posti.
            contentDescription = label,
            modifier = Modifier.fillMaxSize().scale(LAUNCHER_ZOOM)
        )
    }
}

/**
 * La firma: `✦ made with love by Roccobot` col glifo personale dell'utente in coda, dettata da lui.
 * Non si traduce, per la ragione scritta in testa al file.
 *
 * ⚠️ **Il glifo è un `InlineTextContent` e non un'icona accanto al testo**: in una `Row` si
 * allineerebbe al riquadro del testo e non alla sua linea di base. Il segnaposto è in `em`, quindi
 * cresce coi testi grandi. Il colore lo passa chi disegna, perché dentro il contenuto in linea
 * `LocalContentColor` è quello della schermata e non della riga. Il testo alternativo è il
 * carattere cinese, che un lettore di schermo legge al posto del disegno.
 */
@Composable
private fun Signature() {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val glifo = "glifo"
    Text(
        text = buildAnnotatedString {
            append("✦ made with love by ")
            withLink(LinkAnnotation.Url(HOME)) {
                withStyle(SpanStyle(color = accentInk(), textDecoration = TextDecoration.Underline)) {
                    append("Roccobot")
                }
            }
            append(" ")
            appendInlineContent(glifo, "天")
        },
        inlineContent = mapOf(
            glifo to InlineTextContent(
                Placeholder(
                    width = MARK_WIDE,
                    height = MARK_TALL,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Icon(
                    imageVector = Glyphs.Tian,
                    contentDescription = null,
                    tint = ink
                )
            }
        ),
        style = MaterialTheme.typography.bodySmall,
        color = ink,
        textAlign = TextAlign.Center
    )
}

/**
 * Quanto spazio prende il glifo dentro la riga della firma: in `em`, come le lettere intorno, e più
 * largo che alto come il disegno (160 per 145 unità nel design system, rapporto 1,1).
 */
private val MARK_WIDE = 1.21.em
private val MARK_TALL = 1.1.em
