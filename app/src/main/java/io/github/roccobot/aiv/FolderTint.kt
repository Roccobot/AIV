package io.github.roccobot.aiv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Il colore dell'**intestazione** di una cartella: le sedici tinte e la finestra che le mostra.
 *
 * ⚠️⚠️ **NASCE NELLA `1.85` DA UNA SUA RICHIESTA** (riscontro del giro della `1.83`: *il tocco
 * lungo sulla cartella apre un selettore di colore che fa impostare il colore della sfumatura
 * dell'intestazione per cartella, piccolo tocco di personalizzazione: la scelta sarà solo tra 16
 * colori preimpostati, presentati come tondi da toccare in una griglia 4x4*).
 * ⚠️ **Vive fuori dalle impostazioni**, perché non è una preferenza dell'app ma un dato di una
 * cartella: si sceglie guardando quella cartella, e nel pannello sarebbe una riga che chiede *di
 * quale?*. È la stessa clausola con cui `folderView` sta solo nella sua scorciatoia.
 */

/**
 * Una tinta di cartella: **due colori, uno per tema**.
 *
 * ⚠️⚠️ **DALLA `1.89`, ED È SUA RICHIESTA** (*ognuno dei 16 colori dovrebbe essere in realtà una
 * COPPIA di colori: una per il tema chiaro e una per il tema scuro, fatti in modo che ci sia
 * sempre una differenza minima dal colore di fondo*). Fino alla `1.87` la tinta era un numero
 * solo, e la conseguenza si misura: la menta `C0FFE5` sul fondo chiaro dell'app aveva un
 * contrasto di **1,03**, cioè era invisibile, e il grigio-blu `4E6367` sul fondo scuro stava a
 * **2,09**.
 * ⚠️ **Non rovescia la nota di allora** (*una tinta scelta dall'utente non cambia col tema, o la
 * cartella che ha segnato di rosso sarebbe di un altro rosso la sera*): a non cambiare è la
 * **scelta**, che resta un indice nell'archivio. Quello che cambia col tema è come quel rosso si
 * scrive, esattamente come per ogni altro colore dell'app.
 */
data class FrontTint(
    /** Il colore sul tema chiaro. */
    val light: Color,
    /** Il colore sul tema scuro. */
    val dark: Color
)

/**
 * Le sedici tinte, nell'ordine in cui compaiono nella griglia 4x4: la ruota intera, dal rosso
 * al rosa.
 *
 * ⚠️⚠️ **RIFATTE DA CAPO NELLA `1.91`, ED È SUA ISTRUZIONE** (riscontro del giro della `1.89`,
 * voce `tinte-coppie`: *crea tu una nuova palette di 16 coppie che coprano tutte le tonalità
 * possibili*, perché *al momento ci sono troppi verdi, verdini e azzurrini*). La critica ha un
 * numero, ed è la ragione per cui la tavolozza vecchia non si poteva ritoccare: **sette tinte su
 * sedici** cadevano in 55 gradi di ruota, fra il verde acqua e l'azzurro, perché otto erano sue e
 * la sua scelta partiva dai colori di casa. Le tonalità di allora, ordinate: 11, 33, 64, 80, 127,
 * 155, 156, 173, 176, 205, 210, 210, 261, 284, 309, 343.
 *
 * ⚠️⚠️ **LE TONALITÀ SONO A PASSO UNIFORME IN OkLCh E NON IN HSL**, cioè 22,5 gradi l'una
 * dall'altra su una ruota **percettiva**: lo stesso passo in HSL addensa i verdi e dirada i blu,
 * che è esattamente il difetto da cui questa tavolozza nasce.
 *
 * ⚠️⚠️ **I DUE BERSAGLI DI LUMINOSITÀ NON SONO SCELTI, SONO MISURATI SULLA TAVOLOZZA CHE LUI
 * AVEVA GIÀ APPROVATO**: 0,615 per il tema chiaro, che è la mediana delle sedici di allora, e
 * 0,760 per quello scuro, che è la fascia delle quattro sue pensate per il fondo scuro
 * (`38BFD3` a 0,742, `FFA726` a 0,797, `4FD9BE` a 0,804). Così cambia la **distribuzione** delle
 * tonalità e non il carattere della tavolozza, che a lui andava bene.
 * - ⚠️ **La croma ha un tetto**, 0,17, che è poco sopra la più satura delle sue (`6C5CE0` a
 *   0,193): senza, ogni tinta va al limite del gamut e la tavolozza viene fluo. Provato, e la
 *   prima stesura dava un rosso `FF3C39` e un magenta `FC01C3`.
 *
 * ⚠️ **La soglia di contrasto resta 3 a 1** dal peggiore dei tre fondi di quel tema
 * (`background`, `surface` e `surfaceVariant` di `Theme.kt`), che è quella dei componenti non
 * testuali: un filetto e una cornice sono grafica. Il conto risulta fra 3,01 e 3,50 sul tema
 * chiaro e fra 5,7 e 6,7 su quello scuro.
 * - ⚠️ **Sopra una copertina non c'è niente da garantire**, e va detto invece di prometterlo: là
 *   sotto c'è un'immagine qualunque, e nessun colore stacca da tutte le immagini.
 *
 * ⚠️⚠️ **E LE SEDICI SONO PIÙ DISTINGUIBILI DI PRIMA, che è la cosa che lui ha chiesto**: la
 * coppia più vicina passa da **0,012 a 0,042** di distanza percettiva in OkLab, cioè tre volte e
 * mezzo. Il caso peggiore di prima erano il grigio-blu `4E6367` e il verde `2E7D4F`, che sul
 * fondo chiaro si leggevano quasi uguali.
 * - ⚠️ **Il metro è la distanza percettiva e non lo scarto sui canali**: due colori possono
 *   distare 32 su 255 e confondersi lo stesso, ed è quello che succedeva a quella coppia.
 *
 * ⚠️⚠️ **CON LORO ESCE IL GRIGIO-BLU, cioè l'unico neutro**, e non è una dimenticanza: sedici
 * tonalità pure sono quello che ha chiesto, e un grigio non è una tonalità. Se una cartella senza
 * carattere ne avesse bisogno, la ruota scende a quindici: è la domanda `d-tinta-neutro`.
 * ⚠️⚠️ **E LE CARTELLE GIÀ TINTE CAMBIANO COLORE**, perché nell'archivio vive l'**indice** e non
 * il colore (vedi `FolderTints`). Non si può evitare rinumerando: i sedici colori sono altri
 * sedici, quindi non esiste una corrispondenza da tenere.
 */
val FRONT_TINTS: List<FrontTint> = listOf(
    FrontTint(Color(0xFFD7534A), Color(0xFFFE8B7F)), // rosso
    FrontTint(Color(0xFFCF6000), Color(0xFFFE904E)), // corallo
    FrontTint(Color(0xFFB57500), Color(0xFFEF9D05)), // arancio
    FrontTint(Color(0xFF9D8201), Color(0xFFD1AE00)), // ambra
    FrontTint(Color(0xFF7F8F00), Color(0xFFAABE1E)), // oro
    FrontTint(Color(0xFF469B2C), Color(0xFF74CA5D)), // lime
    FrontTint(Color(0xFF009B6A), Color(0xFF06D190)), // verde
    FrontTint(Color(0xFF04998D), Color(0xFF03CCBB)), // smeraldo
    FrontTint(Color(0xFF0A96A4), Color(0xFF03C8DB)), // acqua
    FrontTint(Color(0xFF0091BF), Color(0xFF05C1FD)), // ciano
    FrontTint(Color(0xFF2286E5), Color(0xFF73B5FF)), // cielo
    FrontTint(Color(0xFF6878E8), Color(0xFF9AABFE)), // azzurro
    FrontTint(Color(0xFF916ADC), Color(0xFFBB9DFF)), // indaco
    FrontTint(Color(0xFFAF5EC3), Color(0xFFDF8BF3)), // viola
    FrontTint(Color(0xFFC554A1), Color(0xFFF782CF)), // magenta
    FrontTint(Color(0xFFD35078), Color(0xFFFE85A5))  // rosa
)

/**
 * La tinta scelta per una cartella nel tema in vigore, o `null` per quella dell'app.
 *
 * ⚠️ **L'indice fuori elenco vale come 'nessuna scelta'**: l'elenco può accorciarsi, e un numero
 * vecchio nell'archivio non deve far cadere la schermata.
 * ⚠️⚠️ **IL TEMA È QUELLO DELL'APP E NON QUELLO DI SISTEMA**, cioè [LocalAivLight] e non la
 * configurazione: AIV ha una voce sua in 'Aspetto', quindi con 'Chiaro' scelto qui dentro su un
 * telefono in tema scuro i due valori divergono. È il difetto che gli è già arrivato due volte
 * (`AIV/CLAUDE.md` § '🌗 Il tema scelto DENTRO l'app non è quello di sistema'), e questa funzione
 * è composabile proprio per non poterlo rifare.
 */
@Composable
fun frontTintOf(index: Int?): Color? {
    val coppia = index?.let { FRONT_TINTS.getOrNull(it) } ?: return null
    return if (LocalAivLight.current) coppia.light else coppia.dark
}

/**
 * La finestra che fa scegliere la tinta di questa cartella.
 *
 * ⚠️ **Non è una modale vera**: non raccoglie nessun input scritto, quindi il tocco fuori la
 * chiude, che è il criterio di `AIV/CLAUDE.md` § '👆 Che cosa fa il tocco FUORI da una finestra'.
 * ⚠️ **Il tocco sceglie e chiude**, senza un tasto di conferma: la scelta è di un colore fra
 * sedici e si vede subito dietro la finestra, quindi una conferma sarebbe un tocco in più per
 * dire una cosa che l'occhio ha già detto.
 * ⚠️⚠️ **OGNI TONDO È UN BERSAGLIO SOLO con `Role.RadioButton`**, e la sua descrizione parlata è
 * il **codice esadecimale**: un colore non ha un nome che esista in ventotto lingue senza
 * inventarlo, e sedici nomi inventati sarebbero sedici stringhe che nessun traduttore può
 * verificare. Il codice è un dato, e chi usa un lettore di schermo lo riconosce fra i sedici.
 * ⚠️ **Dalla `1.89` il codice è quello della variante in vigore**, non tutti e due: la coppia si
 * vede, e a chi ascolta serve il colore che la cartella avrà adesso. Nessuna variante si ripete
 * dentro un tema, quindi un codice solo distingue ancora i sedici.
 *
 * @param current l'indice scelto per questa cartella, o `null`.
 * @param onPick l'indice nuovo, o `null` per tornare alla tinta dell'app.
 */
@Composable
fun TintDialog(current: Int?, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.lowered(onDismiss),
        properties = loweredWindow(onDismiss),
        title = { Text(stringResource(R.string.front_tint)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TINT_GAP)) {
                FRONT_TINTS.chunked(TINT_COLUMNS).forEachIndexed { riga, tinte ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TINT_GAP)) {
                        tinte.forEachIndexed { colonna, tinta ->
                            val quale = riga * TINT_COLUMNS + colonna
                            TintDot(
                                tint = tinta,
                                picked = quale == current,
                                onPick = { onPick(quale); onDismiss() }
                            )
                        }
                    }
                }
            }
        },
        /*
         * ⚠️ **Il ritorno alla tinta dell'app è un tasto e non un diciassettesimo tondo**: un
         * tondo del colore dell'app starebbe in fila con gli altri sedici e si leggerebbe come
         * una scelta in più, mentre è il **togliere** una scelta.
         */
        confirmButton = {
            TextButton(onClick = { onPick(null); onDismiss() }) {
                Text(stringResource(R.string.front_tint_none))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Un tondo del selettore, **tagliato in due in orizzontale**.
 *
 * ⚠️⚠️ **SOPRA IL TEMA CHIARO E SOTTO QUELLO SCURO, SEMPRE, in tutti e due i temi** (*decidi tu
 * quali saranno per il tema scuro e rappresentali insieme tagliando in due in orizzontale i 16
 * tondi*). Mettere sopra la variante **in vigore** sarebbe più utile a chi sceglie, e il prezzo
 * non varrebbe: lo stesso tondo si leggerebbe rovesciato passando da un tema all'altro, quindi
 * la metà di sopra smetterebbe di voler dire qualcosa.
 * ⚠️ **Undici coppie su sedici hanno le due metà diverse**, e le altre cinque no perché quella
 * tinta stacca già da tutti e due i fondi: il taglio si vede dove il colore ha davvero due
 * versioni, e non è una decorazione applicata a tutti.
 *
 * ⚠️ **La spunta è dentro il tondo e non un contorno in più**: su una tinta chiara un contorno
 * d'accento si confonde con la tinta stessa, e il segno dovrebbe dire 'questa' senza dipendere
 * dal colore che sta segnando.
 * ⚠️ **L'inchiostro della spunta si sceglie dalla LUMINANZA della tinta**, non dal tema: sedici
 * tondi vanno dal grigio scuro alla menta chiarissima, e un segno bianco sparirebbe sulla metà
 * chiara in tutti e due i temi.
 * ⚠️⚠️ **QUINDI LA SPUNTA È DUE MEZZE SPUNTE, dalla `1.89`, e non un inchiostro di compromesso**:
 * sul tondo della menta le due metà sono `009D5C` e `C0FFE5`, cioè una vuole il bianco e l'altra
 * il nero, e un colore solo scelto sulla media sparirebbe su una delle due. Il taglio delle due
 * metà cade dove cade quello del tondo **per costruzione**: il segno è centrato, quindi il suo
 * mezzo è il mezzo del tondo, e nessun numero scritto a mano tiene insieme le due misure.
 */
@Composable
private fun TintDot(tint: FrontTint, picked: Boolean, onPick: () -> Unit) {
    val viva = if (LocalAivLight.current) tint.light else tint.dark
    val etichetta = "#%06X".format(viva.toArgb() and 0xFFFFFF)
    Box(
        modifier = Modifier
            .size(TINT_DOT)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
            .semantics { contentDescription = etichetta },
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.matchParentSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(tint.light))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(tint.dark))
        }
        if (picked) {
            TintMark(tint.light, top = true)
            TintMark(tint.dark, top = false)
        }
    }
}

/** Metà della spunta, con l'inchiostro che si legge sulla metà di tondo che ha sotto. */
@Composable
private fun TintMark(sotto: Color, top: Boolean) {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = if (sotto.luminance() > TINT_DARK_INK) Color.Black else Color.White,
        modifier = Modifier
            .size(TINT_MARK)
            .drawWithContent {
                clipRect(
                    top = if (top) 0f else size.height / 2f,
                    bottom = if (top) size.height / 2f else size.height
                ) { this@drawWithContent.drawContent() }
            }
    )
}

/** Quanto è largo un tondo: il bersaglio minimo di un tocco, che è quello che serve qui. */
private val TINT_DOT = 48.dp

/** La spunta dentro il tondo. */
private val TINT_MARK = 24.dp

/** L'aria fra un tondo e l'altro, in tutte e due le direzioni. */
private val TINT_GAP = 8.dp

/** Quante colonne ha la griglia: **quattro**, cioè la 4x4 che ha chiesto. */
private const val TINT_COLUMNS = 4

/**
 * Sopra quanta luminanza la spunta si scrive in nero.
 *
 * ⚠️ **Mezzo e non la soglia di contrasto WCAG**: qui si sceglie fra due inchiostri opposti, e la
 * luminanza relativa di Compose è già percettiva. Con le sedici tinte in casa il taglio cade dove
 * ci si aspetta, cioè fra il ciano e la menta.
 */
private const val TINT_DARK_INK = 0.5f
