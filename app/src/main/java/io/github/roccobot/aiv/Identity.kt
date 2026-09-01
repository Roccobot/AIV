package io.github.roccobot.aiv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
 * una risorsa inviterebbe a farlo. ⚠️ L'unica stringa tradotta è la **descrizione** del
 * tocco sull'icona, che non è un nome ma una frase che qualcuno si fa leggere.
 * ⚠️⚠️ **Il collegamento è UNO SOLO PER USO, e i due non sono lo stesso**: il dominio
 * personale sta sotto la firma, il repository sta **sull'icona**. Sono governati dallo
 * stesso [link] perché rispondono alla stessa domanda, cioè se questo blocco può portare
 * fuori dall'app: sulla schermata iniziale no, e vale per tutti e due.
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
        AppIcon(iconSize, link)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Astonishing Image Viewer",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "by Roccobot 天",
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
 * ⚠️ **`108 / 72`, cioè esattamente 1.5, ed è pura geometria**: di un'icona adattiva si
 * vedono **solo i 72dp centrali** della tela da 108, perché l'anello esterno esiste per la
 * maschera e la parallasse. Chi rende la tela intera mostra un margine che sul telefono
 * nessuno vede.
 *
 * ⚠️⚠️ **C'ERA ANCHE UN 1.3, ED ERA SBAGLIATO: MISURATO SULLO SCREENSHOT DEL LAUNCHER IL
 * 2026-08-29 e non più supposto.** Si diceva che HyperOS ingrandisse il primo piano di
 * circa un terzo, quindi questa costante valeva `1.5 * 1.3 = 1.95` e l'anteprima veniva
 * col glifo un quarto più grande di quello vero (segnalazione dell'utente: *è rimasta
 * quella sbagliata, voglio che entrambe appaiano come nel launcher*).
 * ⚠️ **I numeri, perché nessuno debba rifare la misura**: sull'icona del launcher il
 * glifo occupa **0.4398** del lato della maschera in larghezza e **0.5759** in altezza.
 * I 72dp centrali senza alcun ingrandimento ne prevedono **0.4436** e **0.5819**, cioè
 * uno scarto dell'**1%**; l'ipotesi del 1.3 ne prevede 0.5767 e 0.7565, cioè uno scarto
 * del **24%**. Non è un dubbio fra due letture vicine: una delle due è fuori di un
 * quarto.
 * ⚠️⚠️ **MA IL DRAWABLE NON SI TOCCA, e la distinzione è tutta**: quel `1.3` vive anche
 * nella scala del disegno (`ic_launcher_foreground.xml`), dove significa un glifo più
 * piccolo del massimo teorico. Là è **il risultato che l'utente ha dichiarato perfetto**,
 * quindi resta; qui era un divario da colmare che non esiste, quindi esce. Chi
 * 'uniformasse' i due posti rimpicciolirebbe l'icona sul telefono per far tornare un
 * conto.
 */
private const val LAUNCHER_ZOOM = 1.5f

/** Il repository dell'app, dove porta il tocco sull'icona. */
private const val REPO = "https://github.com/Roccobot/AIV"

/**
 * L'icona del launcher, disegnata grande.
 *
 * ⚠️⚠️ **I DUE POSTI IN CUI COMPARE LA DISEGNANO IDENTICA, ed è una decisione presa due
 * volte.** La `0.47` aveva rimpicciolito del 30% il glifo del solo 'chi siamo', su
 * richiesta dell'utente, distinguendo l'**anteprima** della schermata iniziale dal
 * **logo** delle impostazioni; il giorno dopo lui ha guardato le due e ha stabilito il
 * contrario (2026-08-29: *l'icona del 'Chi siamo' dev'essere identica a quella che si
 * vede in alto all'avvio*). Quindi il parametro è uscito invece di restare a 1 in attesa
 * di qualcuno: un argomento che nessuno usa è codice morto travestito da flessibilità.
 * ⚠️ Chi volesse riaprire la questione sappia che è già stata decisa in tutti e due i
 * versi, e che quello che vince è **una icona sola**: l'app e la sua icona devono essere
 * la stessa cosa dovunque compaiano.
 * ⚠️ Resta vero quello che dice `LAUNCHER_ZOOM`: **un difetto visto in un'anteprima non è
 * mai un motivo per cambiare la scala o l'alzata del drawable**.
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
private fun AppIcon(size: Dp, link: Boolean) {
    val opener = LocalUriHandler.current
    val label = stringResource(R.string.identity_repo)
    Box(
        modifier = Modifier
            .size(size)
            // ⚠️ Il ritaglio PRIMA del tocco, o l'area toccabile resterebbe il quadrato
            // intero e gli angoli fuori dalla forma risponderebbero comunque.
            .clip(RoundedCornerShape(percent = 24))
            .background(colorResource(R.color.launcher_background))
            .then(
                if (!link) Modifier
                else Modifier.clickable(onClickLabel = label) { opener.openUri(REPO) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            // ⚠️ Descritta solo quando è toccabile: da ferma è decorazione, e il nome
            // dell'app sta scritto sotto in lettere. Da toccabile invece è un comando, e
            // un comando senza nome è un comando che nessuno può usare al buio.
            contentDescription = if (link) label else null,
            modifier = Modifier.fillMaxSize().scale(LAUNCHER_ZOOM)
        )
    }
}
