package io.github.roccobot.aiv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * L'icona, il nome e da dove viene.
 *
 * ⚠️ **Sta in DUE posti, e non è un doppione**: in cima alla schermata delle cartelle,
 * dove è il volto dell'app all'apertura, e in fondo alle impostazioni, dove ogni app
 * mette il proprio 'chi siamo'. Sono due usi diversi della stessa cosa, quindi un
 * composable solo con due misure e con il collegamento facoltativo: là c'è, qui no,
 * perché una schermata iniziale che porta fuori dall'app al primo tocco sbagliato non
 * è quello che serve.
 * ⚠️ Nessuno di questi testi è una stringa di risorsa, ed è deliberato: sono un nome,
 * una firma e un dominio. Tradurre 'by Roccobot' sarebbe un errore, non una cortesia, e
 * una risorsa inviterebbe a farlo.
 */
@Composable
fun Identity(
    iconSize: Dp,
    modifier: Modifier = Modifier,
    link: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(iconSize)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Astonishing Image Viewer",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "(AIV) by Roccobot 天",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!link) return@Column
        // ⚠️ Non `primary`: l'accento come TESTO non si legge, e la ragione col numero
        // sta accanto a `LINK_LIGHT` in `Theme.kt`. Si legge QUI e non dentro il
        // costruttore del testo, che non è un contesto componibile.
        val ink = accentInk()
        Text(
            text = buildAnnotatedString {
                withLink(LinkAnnotation.Url("https://roccobot.me")) {
                    withStyle(
                        SpanStyle(color = ink, textDecoration = TextDecoration.Underline)
                    ) { append("roccobot.me") }
                }
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Quanto si ingrandisce il livello di primo piano perché questa anteprima mostri la
 * stessa cosa che mostra il launcher.
 *
 * ⚠️⚠️ **I due fattori sono uno per ciascuna cosa che il launcher fa, e non sono
 * intercambiabili.** `108 / 72`, cioè esattamente 1.5, perché di un'icona adattiva si
 * vedono **solo i 72dp centrali** della tela da 108: l'anello esterno esiste per la
 * maschera e la parallasse del launcher, e chi rende la tela intera sta mostrando un
 * margine che sul telefono nessuno vede. Il **1.3** è l'altra metà di una coppia:
 * HyperOS ingrandisce il primo piano di circa un terzo, quindi `ic_launcher_foreground`
 * porta un glifo rimpicciolito di 1.3 per venire giusto sul telefono, e qui si moltiplica
 * per lo stesso 1.3 per rivedere il disegno com'è. Chi ne cambia uno deve cambiare anche
 * l'altro.
 */
private const val LAUNCHER_ZOOM = 1.5f * 1.3f

/**
 * L'icona del launcher, disegnata grande.
 *
 * ⚠️⚠️ **L'ingrandimento si fa al DISEGNO e non alla misura, e fino alla 0.27 questa
 * differenza costava tutto l'ingrandimento.** `Modifier.size` **negozia** col genitore:
 * `SizeNode.measure` chiama `constrain(vincoli in ingresso, misura chiesta)` quando
 * `enforceIncoming` è vero, e per `size` è vero (per `requiredSize` no), verificato nel
 * bytecode di `foundation-layout` e non a memoria. Il `Box` qui sotto passa ai figli i
 * propri vincoli col solo minimo azzerato, quindi il massimo resta la misura della
 * piastrella: l'immagine chiedeva 187.2dp, ne otteneva 96, e l'anteprima mostrava la
 * **tela intera** invece dei 72dp centrali, cioè un glifo 1.95 volte più piccolo di
 * quello del launcher. È esattamente quello che l'utente aveva visto e segnalato.
 * ⚠️ Un `Modifier.scale` è una trasformazione di disegno: nessun genitore la può
 * limitare, e il ritaglio del `Box` continua a valere.
 */
@Composable
private fun AppIcon(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 24))
            .background(colorResource(R.color.launcher_background)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().scale(LAUNCHER_ZOOM)
        )
    }
}
