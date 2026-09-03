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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
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
 * L'icona, il nome e da dove viene.
 *
 * ⚠️ **Sta in DUE posti, e non è un doppione**: in cima alla schermata delle cartelle,
 * dove è il volto dell'app all'apertura, e in fondo alle impostazioni, dove ogni app
 * mette il proprio 'chi siamo'. Sono due usi diversi della stessa cosa, quindi un
 * composable solo con due misure.
 * ⚠️ Nessuno di questi testi è una stringa di risorsa, ed è deliberato: sono un nome e
 * una firma. Tradurre 'by Roccobot' sarebbe un errore, non una cortesia, e una risorsa
 * inviterebbe a farlo. ⚠️ L'unica stringa tradotta è la **descrizione** del tocco
 * sull'icona, che non è un nome ma una frase che qualcuno si fa leggere.
 * ⚠️⚠️ **L'ICONA PORTA ALLA PAGINA DI DOWNLOAD IN TUTTI E DUE I POSTI, dalla 1.37**
 * (istruzione dell'utente, 2026-09-02: *icona sia in schermata home che nelle impostazioni
 * deve portare alla pagina di download*), e prima erano due scelte diverse: portava al
 * **repository**, e sulla schermata iniziale non portava da nessuna parte, perché una
 * schermata iniziale che esce dall'app al primo tocco sbagliato non era quello che
 * serviva. Adesso il parametro `link` non c'è più: non aveva altro da governare.
 * ⚠️ **Un indirizzo NON si scrive dentro un commento con l'asterisco del corsivo attaccato**,
 * e questa riga è nata da quel difetto: `AIV/` più `*` fa `/*`, che in Kotlin **apre un
 * commento annidato**, quindi il `*/` in fondo chiudeva quello di dentro e il file non
 * compilava più, con un 'Unclosed comment' cinquanta righe più giù.
 * ⚠️⚠️ **E LA RIGA `roccobot.me` NON C'È PIÙ, dalla 1.37** (riscontro `firma-nuova`:
 * *impostazioni: la riga con roccobot.me va tolta*): dalla 1.36 'Roccobot' **dentro la
 * firma** è già un collegamento allo stesso indirizzo, quindi quella riga era il secondo
 * collegamento identico nella stessa colonna. Il dominio si raggiunge dalla firma.
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
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Astonishing Image Viewer",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Signature()
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
 * glifo occupava **0.4398** del lato della maschera in larghezza e **0.5759** in altezza.
 * I 72dp centrali senza alcun ingrandimento ne prevedono **0.4436** e **0.5819**, cioè
 * uno scarto dell'**1%**; l'ipotesi del 1.3 ne prevede 0.5767 e 0.7565, cioè uno scarto
 * del **24%**. Non è un dubbio fra due letture vicine: una delle due è fuori di un
 * quarto.
 * ⚠️⚠️ **QUEL `1.3` NON VIVE PIÙ DA NESSUNA PARTE, e fino alla 1.32 questa nota diceva
 * il contrario**: stava nella scala di `ic_launcher_foreground.xml`, dove valeva come
 * preferenza di misura dell'utente sul glifo di allora, e la nota qui avvisava di non
 * 'uniformare' i due posti. Col disegno nuovo del 2026-09-02 quel drawable è stato
 * riscritto e la sua scala ha un'altra ragione, dichiarata là: **2/3**, cioè `72/108`,
 * perché il master arrivava composto per una piastrella piena. Qui invece niente cambia,
 * e la ragione per cui questa costante vale 1.5 è la stessa di prima.
 */
private const val LAUNCHER_ZOOM = 1.5f

/**
 * La pagina di download dell'app, dove porta il tocco sull'icona.
 *
 * ⚠️ **E non il repository, dalla 1.37**: quello è il posto dove sta il codice, e chi tocca
 * l'icona dell'app dentro l'app cerca l'app, non i suoi sorgenti. La pagina offre l'apk
 * firmato, il peso, le note di rilascio, e da lì il collegamento al codice c'è comunque.
 */
private const val PAGINA = "https://roccobot.github.io/AIV/"

/**
 * Il dominio personale dell'utente, dove porta la firma.
 *
 * ⚠️ **Una costante e non due stringhe uguali**: nata nella 1.36, quando la firma e la riga
 * del dominio lo portavano tutte e due. Dalla 1.37 la riga non c'è più (vedi [Identity]) e il
 * lettore è uno, ma la costante resta: un indirizzo scritto dentro un `buildAnnotatedString`
 * non si trova cercando.
 */
private const val HOME = "https://roccobot.me"

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
 * bytecode di `foundation-layout`. Il `Box` qui sotto passa ai figli i
 * propri vincoli col solo minimo azzerato, quindi il massimo resta la misura della
 * piastrella: l'immagine chiedeva 187.2dp, ne otteneva 96, e l'anteprima mostrava la
 * **tela intera** invece dei 72dp centrali, cioè un glifo 1.95 volte più piccolo di
 * quello del launcher. È esattamente quello che l'utente aveva visto e segnalato.
 * ⚠️ Un `Modifier.scale` è una trasformazione di disegno: nessun genitore la può
 * limitare, e il ritaglio del `Box` continua a valere.
 */
@Composable
private fun AppIcon(size: Dp) {
    val opener = LocalUriHandler.current
    val label = stringResource(R.string.identity_page)
    Box(
        modifier = Modifier
            .size(size)
            // ⚠️ Il ritaglio PRIMA del tocco, o l'area toccabile resterebbe il quadrato
            // intero e gli angoli fuori dalla forma risponderebbero comunque.
            .clip(RoundedCornerShape(percent = 24))
            .background(colorResource(R.color.launcher_background))
            .clickable(onClickLabel = label) { opener.openUri(PAGINA) },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            // ⚠️ Descritta perché è un comando, e un comando senza nome è un comando che
            // nessuno può usare al buio. ⚠️ Fino alla 1.36 la descrizione era condizionata,
            // perché sulla schermata iniziale l'icona non si toccava e là era decorazione:
            // adesso si tocca in tutti e due i posti, quindi la condizione non c'è più.
            contentDescription = label,
            modifier = Modifier.fillMaxSize().scale(LAUNCHER_ZOOM)
        )
    }
}

/**
 * La firma: `✦ made with love by Roccobot` col glifo personale dell'utente in coda.
 *
 * ⚠️⚠️ **LA RIGA È DETTATA DALL'UTENTE, dalla 1.36** (2026-09-02: *sotto 'Astonishing
 * Image Viewer' c'è scritto 'by Roccobot 天' -> diventa `✦ made with love by Roccobot [logo]`;
 * 'Roccobot': link a roccobot.me; [logo]: il mio glifo personale, dello stesso colore del
 * carattere, preso dal mio design system*). Fino alla `1.35` era `by Roccobot 天`, cioè la
 * stessa firma col carattere cinese al posto del suo glifo.
 * ⚠️ **RESTA NON TRADOTTA, come prima**: è una firma, non un testo dell'interfaccia, e la
 * ragione per esteso sta in testa a questo file. Tradurre 'made with love' sarebbe un errore
 * per la stessa ragione per cui non si traduce 'by Roccobot'.
 *
 * ⚠️⚠️ **IL GLIFO È UN `InlineTextContent` E NON UN'ICONA ACCANTO AL TESTO**, ed è quello
 * che lo fa stare **dentro** la riga: accanto, in una `Row`, si allineerebbe al riquadro del
 * testo e non alla sua linea di base, e su una firma di un corpo piccolo la differenza si vede.
 * Il segnaposto è misurato in `em`, quindi cresce col carattere di chi ha i testi grandi.
 * ⚠️ **Il colore lo passa chi disegna e non `LocalContentColor`**: qui la tinta deve essere
 * quella del testo (parole sue: *dello stesso colore del carattere*), e dentro il contenuto in
 * linea il colore d'ambiente è quello della schermata, non quello di questa riga.
 * ⚠️ **Il testo alternativo del segnaposto è il carattere cinese**: è quello che un lettore
 * di schermo legge dove il disegno non si vede, e la forma è quella.
 *
 * ⚠️⚠️ **IL COLLEGAMENTO ADESSO C'È ANCHE NELLA SCHERMATA INIZIALE, e prima NO**: la
 * regola di questo file era che là il blocco non porta fuori dall'app (vedi la nota in testa),
 * e l'utente ha chiesto il collegamento proprio su quella riga. Vale la pena saperlo perché la
 * nota sopra [Identity] descrive ancora il resto del blocco: l'icona porta al repository e il
 * dominio per esteso compare **solo** dove `link` è vero, cioè in fondo alle impostazioni.
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
                    painter = painterResource(R.drawable.ic_tian),
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
 * Quanto spazio si prende il glifo dentro la riga della firma.
 *
 * ⚠️ **In `em` e non in dp**: è un carattere in mezzo a delle lettere, quindi la sua misura
 * è quella del corpo del testo. In dp resterebbe fermo mentre le lettere intorno crescono con
 * le impostazioni di sistema.
 * ⚠️ **Più largo che alto**, perché il disegno lo è: 160 per 145 unità nel file del design
 * system, cioè un rapporto di 1,1. Un segnaposto quadrato lo schiaccerebbe.
 */
private val MARK_WIDE = 1.21.em
private val MARK_TALL = 1.1.em
