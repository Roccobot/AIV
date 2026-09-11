package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RuntimeShader
import android.graphics.Shader.TileMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/*
 * ⚠️⚠️ **A CHE COSA SERVE QUESTO FILE: A TOGLIERE LE BANDE DA UNA SFUMATURA, E IL DIFETTO È
 * ARITMETICO** (sua segnalazione, 2026-09-09: *secondo me la sfumatura può essere ulteriormente
 * migliorata, vedo ancora del banding. Se per fare un gradiente di qualità superiore serve
 * gestire una profondità colore più alta, o più memoria, o più risorse, per me va bene*).
 *
 * **Il conto.** Lo schermo tiene 256 livelli per canale. Una sfumatura che parte da un quarto di
 * opacità e arriva a zero fa cambiare il colore finale di una frazione di quei livelli: con una
 * tinta che dista un centinaio di livelli dal fondo sono venticinque gradini, distribuiti su
 * mezzo migliaio di pixel di altezza. Cioè **ogni gradino è alto una ventina di pixel**, ed è una
 * striscia che si vede. Non dipende dai colori scelti, non si toglie con più tappe (fra una
 * tappa e l'altra l'interpolazione è già continua: a quantizzare è la destinazione) e non si
 * toglie con più bit dentro l'app, perché il taglio a otto bit avviene alla fine, sul vetro.
 *
 * ⚠️⚠️ **L'UNICO RIMEDIO È IL RUMORE, e si chiama dithering**: si somma al colore un rumore
 * piccolo **prima** dell'arrotondamento, così i pixel a cavallo di un gradino cadono un po' di
 * qua e un po' di là, e il bordo netto si scioglie in una transizione sfumata. È lo stesso
 * mestiere del retino di una stampa.
 *
 * ⚠️⚠️ **E DALLA `2.06` IL RUMORE C'È ANCHE SOTTO ANDROID 13, PER UNA SUA RISPOSTA** (giro della
 * `2.05`, domanda `d-dither-vecchi`: **`copri`**). Uno shader scritto a mano nasce con Android 13,
 * quindi fino alla `2.05` i telefoni più vecchi restavano con la sfumatura della `1.95`, cioè
 * quella in cui le bande lui le vedeva. La via che resta senza `RuntimeShader` è **precalcolare**
 * la rampa col rumore già dentro e stenderla come immagine: è lo stesso conto, fatto una volta per
 * misura invece che a ogni pixel di ogni fotogramma (vedi [rampMask]).
 * - ⚠️⚠️ **NESSUNO DEI DUE PUÒ GUARDARLA, E LA DOMANDA LO DICEVA PRIMA DI FARLA**: né lui né io
 *   abbiamo un telefono sotto Android 13, quindi questo ramo lo presidia **il banco** e non lo ha
 *   guardato nessuno. Per questo il banco lo prende dal verso che conta di più, cioè che il disegno
 *   arrivi col colore giusto: una maschera che Skia non modulasse darebbe una fascia nera, e
 *   quello è il modo in cui questa strada può fallire.
 *
 * ⚠️⚠️ **LA `1.95` LO CHIEDEVA AL PAINT, E LUI LE BANDE LE VEDE ANCORA**: `isDither` accende il
 * dither di Skia, che è rumore ordinato preso da una matrice piccola. Quello che questo file
 * aggiunge è un dither **esplicito**, scritto da noi e per pixel, di un livello pieno e a
 * distribuzione triangolare, che è la quantità con cui l'errore di arrotondamento smette di
 * dipendere dal colore invece di essere soltanto attenuato.
 * - ⚠️⚠️ **E PERCHÉ QUELLO DEL PAINT NON SIA BASTATO NON SI SA, ED È GIUSTO SCRIVERLO INVECE DI
 *   INVENTARE UNA CAUSA**: sul banco quel dither si vede **agire** (`BandeTest` misura 184 righe
 *   miste su 210 col solo paint, contro 0 senza niente), quindi la riga della `1.95` faceva
 *   qualcosa. Ma il banco disegna col processore e il telefono con la scheda grafica, e quale dei
 *   due percorsi porti davvero quel flag fino al gradiente non è scritto in nessun posto che si
 *   possa leggere da qui. La differenza del rimedio nuovo è che **non dipende da quella
 *   risposta**: il rumore lo scriviamo noi, quindi c'è comunque.
 * - ⚠️ **La riga del paint resta per il caso in cui non ci sia nessuna delle due strade**, cioè un
 *   pennello che non è una rampa: là non c'è niente da ditherare e quel mezzo livello non fa
 *   danno. ⚠️ **Non si somma mai a un rumore nostro**: due dither insieme danno più grana e
 *   niente in cambio, ed è misurato in testa a `frontWash`.
 * - ⚠️ **Il rumore si somma ai soli canali del COLORE e non all'opacità**, ed è misurato sul
 *   conto del miscelamento: il colore che finisce a schermo vale `sorgente + fondo * (1 -
 *   opacità)`, quindi un rumore aggiunto anche all'opacità si cancella da sé sui fondi chiari
 *   (dove `fondo` vale quasi uno) e la sfumatura tornerebbe a bande proprio nel tema chiaro.
 *   Sommandolo al solo colore, il rumore sul risultato è quello voluto su qualunque fondo.
 * - ⚠️ **E resta dentro l'opacità** (`clamp` fra zero e `a`): un colore premoltiplicato più
 *   luminoso della propria opacità non è un colore, e che cosa ne farebbe il miscelatore non è
 *   scritto da nessuna parte.
 *
 * ⚠️⚠️ **E LA `2.10` HA CERCATO LA CAUSA DI UN BANDING CHE LUI VEDE ANCORA: HA TROVATO UN DIFETTO
 * VERO, E NON LA CAUSA** (punto G del campo libero del giro accorpato: *il banding del gradiente è
 * tornato, visibile soprattutto nel tema scuro*). Le misure si scrivono qui perché la sessione
 * dopo non ripercorra le stesse strade, e perché due di loro **smentiscono** un sospetto che era
 * scritto nel brief.
 * - **Le due sorgenti del rumore non erano indipendenti**, ed è il difetto corretto: vedi il blocco
 *   su [DITHER_AGSL]. Ma l'effetto sulle bande è piccolo, ed è misurato: simulando una rampa che
 *   scende di un livello ogni trenta righe, la media del disegno quantizzato si stacca da quella
 *   vera di **0,017 livelli** col rumore di prima, contro **0,5** senza nessun rumore. Cioè il
 *   dither di prima già teneva la media giusta.
 * - **La rampa a dieci segmenti non c'entra**, e il sospetto era ragionevole perché fra una tappa
 *   e l'altra l'interpolazione è lineare e la pendenza cambia di colpo: il salto più grande vale
 *   1,15 livelli ogni cento pixel. Ma il profilo si discosta da una curva liscia che passa per gli
 *   **stessi** punti di **0,14 livelli** al massimo, cioè meno di un quanto.
 * - **Il rumore non porta strutture sue lungo la colonna**: la media di riga oscilla fra -0,007 e
 *   +0,006 livelli su una riga di mille pixel, quindi non aggiunge nessuna banda.
 * - ⚠️⚠️ **QUELLO CHE RESTA DA MISURARE È SUL SUO TELEFONO, E LA VOCE DI COLLAUDO GLI CHIEDE UNA
 *   SCHERMATA**: da un'immagine catturata si conta se i pixel di una riga sono tutti uguali (il
 *   rumore non arriva fino al vetro, e allora la causa è nel percorso di disegno) oppure misti (il
 *   rumore arriva, e le bande vengono da altro). Nessuno dei conti fatti qui può rispondere, perché
 *   il banco disegna col processore e il suo telefono con la scheda grafica.
 *
 * ⚠️ **Il rumore è ancorato allo SCHERMO e non alla sfumatura**, perché la coordinata che arriva
 * qui è quella del pixel: scorrendo, la tinta si muove e la grana sta ferma. È la cosa giusta da
 * vedere, e la contraria (una grana che scorre insieme alla tinta) si noterebbe come un velo che
 * si muove.
 *
 * ⚠️⚠️ **LE ALTRE SFUMATURE DELL'APP NON PASSANO DI QUI, E NON È UNA DIMENTICANZA: IL CONTO DICE
 * CHE LÀ NON C'È NIENTE DA TOGLIERE.** Le due in fondo allo schermo (`GroundFade`) vanno dal fondo
 * pieno al trasparente, cioè attraversano **tutti** i livelli in un centinaio di punti: un gradino
 * viene alto **un pixel**, che nessuno vede. Il gradiente dell'intestazione attraversa un quarto
 * dei livelli su quasi tutto lo schermo, e là un gradino viene alto **quasi quaranta pixel**. Lo
 * stesso disegno, due aritmetiche diverse: chi porta il rimedio anche là aggiunge grana dove non
 * serve.
 */

/**
 * Il programma che gira **su ogni pixel**: legge la sfumatura e le somma il rumore.
 *
 * ⚠️⚠️ **LA SFUMATURA ARRIVA COME INGRESSO E NON SI RISCRIVE QUI**, ed è la cosa che tiene una
 * fonte sola: le tappe restano quelle del mockup, dichiarate in Kotlin da chi disegna, e questo
 * programma non sa nemmeno che forma abbiano. Riscriverle in AGSL vorrebbe dire due rampe da
 * tenere allineate, e la prima a cambiare sarebbe quella che nessuno guarda.
 * ⚠️ **Il rumore è la somma di DUE sorgenti indipendenti**, che dà una distribuzione triangolare:
 * con una sorgente sola (distribuzione piatta) l'errore di arrotondamento resta legato al colore,
 * cioè le bande si attenuano invece di sparire. È il risultato classico della teoria del dither,
 * e costa una riga in più.
 * ⚠️⚠️ **E FINO ALLA `2.09` NON ERANO INDIPENDENTI, CIOÈ IL CODICE FACEVA QUELLO CHE LA RIGA QUI
 * SOPRA DICE DI NON FARE**: la seconda sorgente era la **stessa funzione** valutata in `p + (37,
 * 17)`, e questa funzione dipende da `p` solo attraverso un prodotto scalare, quindi spostare `p`
 * di una costante equivale a spostare quello scalare di una costante. Misurato: valeva
 * `g2 = fract(g1 + 0,87)` con uno scarto massimo di 0,017, cioè un legame **deterministico**; la
 * distribuzione della somma veniva piatta invece che triangolare, e il picco 0,87 livelli invece
 * di 1. Dalla `2.10` la seconda sorgente ha **coefficienti propri** (i due scambiati), e la
 * distribuzione misurata è la triangolare vera con picco 1,000.
 * ⚠️ **La sorgente è un rumore a gradiente interlacciato** e non un seno moltiplicato per un
 * numero grande: il secondo, sui numeri a mezza precisione di uno shader, degenera a strisce
 * proprio dove serve uniforme.
 */
private const val DITHER_AGSL = """
uniform shader ramp;

half grain(float2 p, float2 k) {
    return half(fract(52.9829189 * fract(dot(p, k))));
}

half4 main(float2 p) {
    half4 c = ramp.eval(p);
    half due = grain(p, float2(0.06711056, 0.00583715)) + grain(p, float2(0.00583715, 0.06711056));
    half noise = (due - 1.0) * half(1.0 / 255.0);
    return half4(clamp(c.rgb + noise, half3(0.0), half3(c.a)), c.a);
}
"""

/**
 * La stessa sfumatura di [brush], dipinta col rumore che le toglie le bande, oppure `null` dove
 * non si può fare.
 *
 * ⚠️⚠️ **`null` VUOL DIRE ANDROID 12 O PRIMA, e chi chiama deve avere la sua strada**: uno shader
 * scritto a mano vuole `RuntimeShader`, che nasce con Android 13. Sotto, il rumore arriva
 * precalcolato da [rampMask], che dalla `2.06` è la seconda strada: chi chiama prova questa e
 * ripiega su quella.
 * ⚠️ **Il secondo `null` è un pennello che non è uno shader**, cioè una tinta unita: una tinta
 * unita non ha nessuna rampa da quantizzare, quindi non c'è niente da ditherare e la strada di
 * prima è già quella giusta.
 * ⚠️ **La misura serve perché una sfumatura è ancorata alla sua scatola**: lo shader nasce per
 * quella misura, quindi va rifatto quando cambia. Chi chiama lo tiene già nella cache del
 * disegno, che si rifà esattamente là.
 *
 * @param brush la sfumatura da dipingere, con le sue tappe.
 * @param size quanto è grande il rettangolo che la porta.
 */
internal fun ditherShader(brush: Brush, size: Size): Shader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val ramp = (brush as? ShaderBrush)?.createShader(size) ?: return null
    return grainOver(ramp)
}

/**
 * Il programma compilato, con la sfumatura agganciata.
 *
 * ⚠️ **Vive in una funzione sua e non dentro [ditherShader]** perché il controllo di versione e
 * l'uso di una classe che nasce con Android 13 devono stare in due posti diversi, o l'analizzatore
 * statico non riconosce la guardia.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun grainOver(ramp: Shader): Shader =
    RuntimeShader(DITHER_AGSL).apply { setInputShader("ramp", ramp) }

/**
 * La stessa rampa col rumore già dentro, disegnata una volta come **maschera**: la strada dei
 * telefoni che [ditherShader] non può servire, cioè Android 12 e prima.
 *
 * ⚠️⚠️ **È UNA MASCHERA E NON UN'IMMAGINE A COLORI, e questo governa tutto il resto**: la rampa
 * dell'intestazione è una tinta **sola** con l'opacità che scende, quindi di lei basta l'opacità,
 * che è un byte per pixel invece di quattro. Il colore lo mette il paint di chi la posa, come per
 * qualunque maschera, e il conto della memoria scende a un quarto.
 * ⚠️⚠️ **E IL RUMORE VA SULL'OPACITÀ, che qui è l'unica strada e per fortuna è anche quella che
 * si taglia da sé la dose giusta.** Il colore che finisce a schermo vale `tinta * a + fondo * (1 -
 * a)`, quindi un livello di rumore sull'opacità ne muove `|tinta - fondo| / 255` sul risultato:
 * dove i due colori distano tanto (cioè dove i gradini sono grossi e le bande si vedono) il rumore
 * arriva forte, e dove distano poco (dove di bande quasi non ce n'è) arriva piano. Con
 * [GRAIN_STEPS] livelli e un distacco tipico di un centinaio, sul risultato viene circa **un
 * livello**, che è la dose del dither.
 * - ⚠️ **Nello shader il rumore va invece sul COLORE**, ed è scritto in testa a questo file: là il
 *   colore c'è, quindi si può perturbare quello e non dipendere dal fondo. Le due scelte non si
 *   contraddicono, sono quello che ognuna delle due strade ha in mano.
 *
 * ⚠️⚠️ **LA TESSERA È STRETTA E SI RIPETE, ED È LA RAGIONE PER CUI COSTA POCO**: le bande sono
 * **orizzontali**, quindi a romperle serve che il rumore cambi lungo la riga; che si ripeta ogni
 * [GRAIN_TILE] pixel non si vede, perché quello che si ripete vale un paio di livelli. Alta invece
 * dev'essere quanto il rettangolo, o la rampa non sarebbe più la sua.
 * ⚠️ **Il generatore è un LCG con seme fisso e non `Math.random`**: costa due moltiplicazioni per
 * pixel invece di due hash, e soprattutto dà la **stessa** immagine a ogni corsa, che è la sola
 * cosa che permetta al banco di misurarla.
 * ⚠️ **Il rumore è la somma di due estrazioni**, come nello shader e per la stessa ragione: una
 * sola (distribuzione piatta) attenua le bande invece di scioglierle.
 *
 * @param stops le tappe della rampa, come le riceve il pennello: posizione e quanto di [peak].
 * @param peak quanto copre la tinta nel suo punto più forte.
 * @param height quanto è alto in pixel il rettangolo da coprire.
 * @param top dove comincia quel rettangolo nello spazio del disegno: uno shader nasce ancorato
 *   all'origine del nodo, e la fascia comincia sopra di lei.
 * @return la maschera pronta da mettere in un paint colorato, o `null` se non c'è niente da
 *   coprire (altezza zero) o se la memoria non basta.
 */
internal fun rampMask(stops: List<Pair<Float, Float>>, peak: Float, height: Int, top: Float): Shader? {
    val mappa = rampBitmap(stops, peak, height) ?: return null
    return BitmapShader(mappa, TileMode.REPEAT, TileMode.CLAMP).apply {
        if (top != 0f) setLocalMatrix(Matrix().apply { setTranslate(0f, top) })
    }
}

/**
 * La tessera con dentro la rampa e il suo rumore, cioè quello che [rampMask] stende.
 *
 * ⚠️ **Vive in una funzione sua perché è la sola cosa che il banco può misurare per davvero**:
 * dei pixel resi non ci si può fidare per dire se il rumore c'è, perché nel disegno Skia ne
 * aggiunge già uno suo (misurato: senza nessun rumore nostro una riga porta comunque due toni
 * adiacenti). Nell'opacità della tessera invece il rumore o c'è o non c'è, e si conta.
 */
internal fun rampBitmap(stops: List<Pair<Float, Float>>, peak: Float, height: Int): Bitmap? {
    if (height <= 0 || stops.isEmpty()) return null
    val mappa = try {
        Bitmap.createBitmap(GRAIN_TILE, height, Bitmap.Config.ALPHA_8)
    } catch (e: OutOfMemoryError) {
        return null
    }
    /*
     * ⚠️ **La riga si misura invece di darla per larga quanto l'immagine**: Skia può allineare
     * `rowBytes`, e un buffer più corto di `byteCount` fa fallire la copia.
     */
    val passo = mappa.rowBytes
    val byte = ByteArray(passo * height)
    var seme = GRAIN_SEED
    for (y in 0 until height) {
        val quanto = rampAt(stops, (y + 0.5f) / height) * peak * 255f
        val riga = y * passo
        for (x in 0 until GRAIN_TILE) {
            seme = seme * 1664525 + 1013904223
            val u1 = (seme ushr 8 and 0xFFFF) / 65535f - 0.5f
            seme = seme * 1664525 + 1013904223
            val u2 = (seme ushr 8 and 0xFFFF) / 65535f - 0.5f
            val con = quanto + (u1 + u2) * GRAIN_STEPS
            byte[riga + x] = con.roundToInt().coerceIn(0, 255).toByte()
        }
    }
    mappa.copyPixelsFromBuffer(ByteBuffer.wrap(byte))
    return mappa
}

/**
 * Quanto vale la rampa alla frazione `t`, interpolando fra le due tappe che la racchiudono.
 *
 * ⚠️ **Lineare, perché lineare è quello che fa il pennello**: `Brush.verticalGradient` interpola i
 * suoi colori dritto per dritto, e con una tinta sola che cambia soltanto opacità premoltiplicare
 * o no non cambia un numero. Quindi questa funzione ridà **la stessa** rampa, e le tappe restano
 * una fonte sola.
 */
private fun rampAt(stops: List<Pair<Float, Float>>, t: Float): Float {
    if (t <= stops.first().first) return stops.first().second
    for (i in 1 until stops.size) {
        val (fine, quantoFine) = stops[i]
        if (t > fine) continue
        val (inizio, quantoInizio) = stops[i - 1]
        val largo = fine - inizio
        if (largo <= 0f) return quantoFine
        val dove = (t - inizio) / largo
        return quantoInizio + (quantoFine - quantoInizio) * dove
    }
    return stops.last().second
}

/**
 * Quanto è larga la tessera del rumore, in pixel.
 *
 * ⚠️ **Il numero governa insieme la memoria e il periodo**: la maschera pesa `GRAIN_TILE` byte per
 * riga, quindi su una fascia alta un migliaio di pixel sono un paio di centinaia di kB, ed è la
 * ragione per cui questa strada si può percorrere. Più stretta costerebbe meno e ripeterebbe più
 * spesso; a 128 il periodo è già più largo di quello che un rumore di un paio di livelli può far
 * vedere.
 */
private const val GRAIN_TILE = 128

/**
 * Quanti livelli di opacità vale il rumore.
 *
 * ⚠️ **Due e non uno, e il conto è quello scritto su [rampMask]**: un livello di opacità ne muove
 * `|tinta - fondo| / 255` sul risultato, quindi con i distacchi di casa (un centinaio di livelli
 * fra la tinta di una cartella e il fondo dell'app) servono due livelli per arrivare al livello
 * pieno che il dither vuole. ⚠️ **E il tetto del danno è dichiarato**: anche nel caso estremo di
 * una tinta bianca su fondo nero, due livelli di opacità al 25% di copertura restano meno
 * dell'uno per cento del colore, cioè sotto la soglia in cui una grana si vede.
 */
private const val GRAIN_STEPS = 2f

/**
 * Il seme del generatore.
 *
 * ⚠️ **Fisso di proposito**: la maschera dev'essere la stessa a ogni corsa, o il banco misurerebbe
 * un'immagine diversa da quella che va sul telefono. Il valore non ha nessun significato, è un
 * numero dispari abbastanza grande da far partire l'LCG lontano da zero.
 */
private const val GRAIN_SEED = 0x5EED_1CE7
