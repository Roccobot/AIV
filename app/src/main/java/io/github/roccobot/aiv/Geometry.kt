package io.github.roccobot.aiv

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader.TileMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * ⚠️⚠️ **A CHE COSA SERVE QUESTO FILE: È IL SESTO MODULO DELL'EDITOR COMPLETO, E NON PASSA DALLO
 * SHADER.** Gli altri cinque dicono di che **colore** è un pixel, e per questo vivono in
 * `Adjust.kt`, dove il conto gira in AGSL su ogni pixel; questo dice **dove** va un pixel, cioè
 * sposta l'immagine invece di ricolorarla. Sono due mestieri diversi, e il secondo non si può
 * scrivere là dentro per una ragione misurata: il salvataggio lavora a **tessere**, e una tessera
 * legge la propria porzione di sorgente, mentre una deformazione fa leggere a un pixel di uscita
 * un punto che può stare dall'altra parte della fotografia. Con la geometria dentro lo shader,
 * ogni tessera avrebbe letto il pezzo sbagliato.
 *
 * ⚠️⚠️ **MA LA REGOLA DI FONDO RESTA SODDISFATTA, E QUESTO È IL PUNTO**: quella regola non dice
 * 'il conto vive in AGSL', dice che la **stessa matematica non si scrive due volte**, perché il
 * giorno che una copia cambia l'utente vedrebbe un'anteprima e salverebbe un'altra immagine. Qui
 * il conto è scritto una volta, in Kotlin, e i lettori sono due: l'anteprima del palco e il
 * salvataggio sul file pieno passano tutti e due da [Warp.draw], con la stessa griglia e la
 * stessa aritmetica.
 * - ⚠️ **E in più il banco lo può misurare**, che con l'AGSL non succede: qui il conto è Kotlin
 *   puro, quindi l'andata e il ritorno, la scala di copertura e la griglia si provano davvero.
 *
 * ⚠️⚠️ **LA GEOMETRIA VIENE PER ULTIMA NELLA CATENA, DOPO IL COLORE, E LA RAGIONE È IL
 * DETTAGLIO**: quel modulo guarda i pixel vicini per accentuare il disegno e togliere la grana, e
 * li deve guardare **com'erano nel file**. Deformando prima, leggerebbe pixel già interpolati, cioè
 * misurerebbe una nitidezza che il ricampionamento ha appena ammorbidito. Deformando dopo, ogni
 * modulo lavora sull'immagine che si aspetta e questo file riceve il risultato finito.
 */

/**
 * Il modulo **Geometria**: dove va a finire un pixel, cioè come l'immagine si raddrizza, si stira,
 * si corregge della prospettiva e della curvatura dell'obiettivo.
 *
 * ⚠️⚠️ **CINQUE COMANDI IN UNA VERSIONE SOLA, ED È LA SUA RISPOSTA `intera` A `d-geo-quanto`**
 * (giro della `2.28`): la domanda chiedeva se spezzarla in due giri, tenendo il raddrizzamento e
 * l'aspetto davanti e le tre correzioni dell'obiettivo dietro, e la risposta è di provarla come un
 * pannello finito.
 *
 * ⚠️⚠️ **I PRIMI DUE MUOVONO L'IMMAGINE, GLI ALTRI TRE LA DEFORMANO, e la differenza si vede nel
 * conto**: [straighten] e [aspect] sono una rotazione e una scala, cioè trasformazioni che tengono
 * dritte le righe dritte; [horizontal] e [vertical] sono un **keystone**, che stringe un lato e
 * allarga l'opposto, e [distortion] è radiale, cioè l'unica che curva le righe. Chi corregge una
 * facciata fotografata dal basso muove i due keystone; chi corregge un grandangolo muove l'ultimo.
 *
 * ⚠️ **Sono tutti frazioni da -1 a +1**, come i cursori degli altri moduli, e l'interfaccia li
 * mostra da -100 a +100: è il linguaggio di Lightroom, che è quello che lui conosce. Quanto valgano
 * al fondo della corsa lo dicono le costanti di [Warp], una per cursore.
 */
data class Geometry(
    val straighten: Float = 0f,
    val aspect: Float = 0f,
    val horizontal: Float = 0f,
    val vertical: Float = 0f,
    val distortion: Float = 0f
) {

    /** Se questo modulo non sposta un pixel: vedi la nota sulla tolleranza in [Light.idle]. */
    val idle: Boolean
        get() = abs(straighten) < DEAD && abs(aspect) < DEAD && abs(horizontal) < DEAD &&
            abs(vertical) < DEAD && abs(distortion) < DEAD

    companion object {
        val NONE = Geometry()

        private const val DEAD = 0.0005f
    }
}

/**
 * Il conto della geometria già pronto per un'immagine di misura nota: gli angoli, le scale e la
 * **scala di copertura**, calcolati una volta invece che per ogni punto.
 *
 * ⚠️⚠️ **ESISTE PERCHÉ LA COPERTURA COSTA UN GIRO SUL CONTORNO, e quel giro non si fa per
 * vertice**: la griglia dell'anteprima ha più di mille punti e si ricostruisce a ogni fotogramma,
 * quindi un conto fatto là dentro sarebbe mille volte lo stesso numero. Qui si fa una volta e lo
 * leggono tutti.
 *
 * ⚠️ **Le coordinate di lavoro sono ISOTROPE**, cioè normalizzate sul **lato lungo** e non su
 * ciascun asse: una rotazione fatta in coordinate normalizzate per asse non è una rotazione, è
 * un'ellisse, e raddrizzare un orizzonte lo storcerebbe invece di raddrizzarlo.
 */
internal class WarpPlan(
    /** Metà del lato lungo del rettangolo, cioè l'unità in cui vivono i conti. */
    val half: Float,
    /** Il centro del rettangolo. */
    val cx: Float,
    val cy: Float,
    /** Metà larghezza e metà altezza in unità di [half]: il rettangolo da coprire. */
    val ax: Float,
    val ay: Float,
    val cosT: Float,
    val sinT: Float,
    /** Quanto l'aspetto stira in orizzontale; in verticale si stringe dello stesso fattore. */
    val stretch: Float,
    val slantX: Float,
    val slantY: Float,
    val bend: Float,
    /** Quanto si ingrandisce perché nessun bordo resti scoperto: vedi [Warp.cover]. */
    val cover: Float
) {

    /**
     * Dove finisce il punto ([x], [y]) del rettangolo, cioè la mappatura **diretta**.
     *
     * ⚠️ **L'ordine dei cinque passi è la specifica**: prima la curvatura dell'obiettivo, che è un
     * difetto del vetro e quindi la cosa più vicina al sensore; poi l'aspetto e la rotazione, che
     * mettono l'immagine dritta; poi il keystone, che è il punto di vista; per ultima la scala di
     * copertura, che non corregge niente e serve solo a non lasciare bordi vuoti.
     */
    fun map(x: Float, y: Float): FloatArray {
        var nx = (x - cx) / half
        var ny = (y - cy) / half

        // 1. La distorsione dell'obiettivo: un punto si allontana o si avvicina al centro in
        // proporzione al **quadrato** della sua distanza, che è il primo termine del modello
        // classico. Positivo = cuscinetto, negativo = barile.
        if (bend != 0f) {
            val f = 1f + bend * (nx * nx + ny * ny)
            nx *= f
            ny *= f
        }

        // 2. L'aspetto: si stira in orizzontale e si stringe in verticale dello stesso fattore,
        // così l'area resta quella e il cursore cambia le proporzioni invece della misura.
        if (stretch != 1f) {
            nx *= stretch
            ny /= stretch
        }

        // 3. Il raddrizzamento.
        if (sinT != 0f) {
            val rx = nx * cosT - ny * sinT
            val ry = nx * sinT + ny * cosT
            nx = rx
            ny = ry
        }

        // 4. I due keystone, uno per asse e in sequenza: un lato si allarga, quello opposto si
        // stringe di altrettanto, e la retta di mezzo resta dov'è. Il conto per esteso, e perché
        // la divisione unica di prima faceva scivolare l'immagine, vivono su [Warp.SLANT].
        // ⚠️ **Il divisore si tiene lontano dallo zero**: a denominatore nullo il punto andrebbe
        // all'infinito, e un vertice all'infinito farebbe sparire l'immagine invece di deformarla.
        if (slantY != 0f && ay > 0f) {
            val u = ny / ay
            val d = max(1f + slantY * u, EDGE)
            nx = nx * (1f - slantY * slantY) / d
            ny = ay * (u + slantY) / d
        }
        if (slantX != 0f && ax > 0f) {
            val v = nx / ax
            val d = max(1f + slantX * v, EDGE)
            ny = ny * (1f - slantX * slantX) / d
            nx = ax * (v + slantX) / d
        }

        return floatArrayOf(cx + nx * cover * half, cy + ny * cover * half)
    }

    /**
     * Da dove viene il punto ([x], [y]), cioè la mappatura **inversa**.
     *
     * ⚠️⚠️ **SERVE AL COLORE MIRATO, E SENZA DI LEI QUEL TASTO PRENDEREBBE UN ALTRO PIXEL**: il
     * dito tocca l'immagine **deformata**, e il colore da leggere vive nel file, cioè prima della
     * deformazione. I cinque passi si disfano nell'ordine contrario.
     *
     * ⚠️ **L'unico passo che non ha una formula chiusa è la distorsione**, e si inverte con quattro
     * giri di Newton: il raggio cercato è quello che, gonfiato dal modello, dà il raggio che si ha
     * in mano. Quattro giri portano l'errore sotto il milionesimo per ogni valore che i cursori
     * possono chiedere, e il banco lo misura.
     */
    fun back(x: Float, y: Float): FloatArray {
        var nx = (x - cx) / (half * cover)
        var ny = (y - cy) / (half * cover)

        /*
         * I due keystone si disfano in ordine contrario, e ognuno ha la sua formula chiusa: la
         * mappa di un asse è una Möbius, quindi l'inversa è la stessa espressione con il segno
         * cambiato e senza il fattore del lato. Il conto è su [Warp.SLANT].
         */
        if (slantX != 0f && ax > 0f) {
            val v = nx / ax
            val d = max(1f - slantX * v, EDGE)
            ny /= d
            nx = ax * (v - slantX) / d
        }
        if (slantY != 0f && ay > 0f) {
            val u = ny / ay
            val d = max(1f - slantY * u, EDGE)
            nx /= d
            ny = ay * (u - slantY) / d
        }

        if (sinT != 0f) {
            val rx = nx * cosT + ny * sinT
            val ry = -nx * sinT + ny * cosT
            nx = rx
            ny = ry
        }

        if (stretch != 1f) {
            nx /= stretch
            ny *= stretch
        }

        if (bend != 0f) {
            val got = sqrt(nx * nx + ny * ny)
            if (got > 1e-6f) {
                var r = got
                repeat(NEWTON) {
                    val f = r + bend * r * r * r - got
                    val d = 1f + 3f * bend * r * r
                    if (abs(d) > 1e-6f) r -= f / d
                }
                val k = r / got
                nx *= k
                ny *= k
            }
        }

        return floatArrayOf(cx + nx * half, cy + ny * half)
    }

    private companion object {
        /** Il divisore del keystone non scende sotto questo: vedi [map]. */
        const val EDGE = 0.05f

        /** Quanti giri di Newton per invertire la distorsione: vedi [back]. */
        const val NEWTON = 4
    }
}

/**
 * Come una [Geometry] diventa un disegno: il piano, la griglia dei vertici, e le due strade che la
 * applicano.
 *
 * ⚠️⚠️ **LA DEFORMAZIONE SI DISEGNA COME UNA MAGLIA DI TRIANGOLI, E NON COME UNA MATRICE**: quattro
 * dei cinque comandi sarebbero una matrice 3x3, che Android sa applicare da sé, ma il quinto (la
 * distorsione) **curva le righe**, e nessuna matrice lo sa fare. Con una maglia fitta i cinque
 * comandi passano dalla stessa strada, e non esistono due meccanismi che possono divergere.
 * - ⚠️ **Il costo dichiarato è l'approssimazione**: dentro una cella la deformazione è lineare,
 *   quindi la curvatura si vede a tratti invece che continua. Con [CELLS] celle per lato una cella
 *   copre una manciata di pixel su uno schermo e qualche decina su un file grande, cioè lo scarto
 *   resta sotto il pixel. È la stessa via che Android usa per `drawBitmapMesh`.
 * - ⚠️ **Le coordinate della texture sono la griglia NON deformata**: il disegno prende il pixel
 *   che starebbe in quel punto e lo posa nel vertice deformato, che è esattamente la definizione
 *   della mappatura diretta.
 */
internal object Warp {

    /** Quanti gradi vale il raddrizzamento al fondo della corsa. */
    const val TILT = 15f

    /** Quanto l'aspetto stira al fondo della corsa. */
    const val STRETCH = 0.25f

    /**
     * Quanto pesa un keystone al fondo della corsa, cioè di quanto si allarga il lato che si apre.
     *
     * ⚠️⚠️ **IL PERNO È LA RETTA DI MEZZO, DALLA `2.30`, ED È IL SUO RISCONTRO** (giro della
     * `2.29`, voce `geo-dritto` non approvata: *dovrebbero avere come perno una retta che rimane
     * al centro, anziché un appoggio laterale*). Fino alla `2.29` i due keystone erano **una**
     * divisione prospettica sola (`nx` e `ny` divisi per `1 + sx*nx + sy*ny`), che è l'omografia da
     * manuale e manda rette in rette, ma **non è centrata**: a 0,35 il lato che si apre andava a
     * `1/0,65`, cioè +54%, e quello che si stringe a `1/1,35`, cioè -26%. Le due cose non si
     * compensano, quindi il trapezio scivolava tutto da una parte (misurato: il suo centro cadeva
     * a 0,4 di semialtezza dal centro dell'inquadratura) e l'immagine sembrava appoggiata a un
     * bordo invece di ruotare attorno a sé.
     *
     * ⚠️⚠️ **ADESSO OGNI ASSE È UNA MÖBIUS SUL PROPRIO LATO, E I DUE KEYSTONE SI APPLICANO IN
     * SEQUENZA.** Per il verticale, con `u = ny / ay` e `s` questo coefficiente:
     * `u -> (u + s) / (1 + s*u)` e `nx -> nx * (1 - s^2) / (1 + s*u)`. Tre proprietà, e sono
     * esattamente quello che chiedeva:
     * - **i bordi non si muovono** (`u = ±1` resta `±1`), quindi l'altezza è quella di prima e il
     *   trapezio è isoscele: il lato di sopra vale `1 + s` e quello di sotto `1 - s`, cioè il
     *   numero qui sotto si legge direttamente come un +35% e un -35%;
     * - **il centro dei quattro vertici resta il centro**, che è la forma esatta del difetto;
     * - **resta un'omografia**, quindi le righe dritte restano dritte. ⚠️ Deformare la sola
     *   coordinata trasversale (`nx /= 1 + s*ny`, lasciando `ny` dov'è) sarebbe stato più corto e
     *   **curva le verticali**: quella mappa manda una retta in un'iperbole, che in un comando di
     *   prospettiva è peggio del difetto che toglie.
     *
     * ⚠️ **La normalizzazione è sul lato VERO e non su quello lungo**: le coordinate di lavoro sono
     * isotrope (vedi [WarpPlan]), quindi su un'immagine larga il bordo di sopra sta a `ay` e non a
     * uno. Normalizzando sul lato lungo il perno tornerebbe a scappare, in proporzione a quanto
     * l'immagine è lontana dal quadrato.
     *
     * ⚠️ **Il prezzo è un filo di copertura in più**, ed è dichiarato: su un 4:3 con questo cursore
     * a fondo corsa la scala passa da 1,26 a 1,34, perché un trapezio centrato rientra da tutte e
     * due le parti invece che da una sola.
     *
     * ⚠️ **Il numero non è cambiato**, ed è la sua risposta `bene` a `d-geo-corsa` (giro della
     * `2.29`): quello che cambia è come si distribuisce, non quanto pesa.
     */
    const val SLANT = 0.35f

    /**
     * Quanto curva la distorsione al fondo della corsa, come coefficiente del raggio quadrato.
     *
     * ⚠️⚠️ **IL NUMERO È IL TETTO OLTRE IL QUALE IL DISEGNO SI RIPIEGA, E LO HA TROVATO IL BANCO**:
     * la mappa radiale vale `r * (1 + k * r^2)`, che è invertibile finché la sua derivata
     * `1 + 3 * k * r^2` resta positiva, cioè finché `k > -1 / (3 * r^2)`. Il raggio più grande è
     * quello dell'angolo, e in coordinate isotrope vale al massimo la radice di due (un'immagine
     * **quadrata**, dove i due lati sono tutti e due l'unità): là il tetto è `-0,167`, quindi il
     * `-0,25` della prima stesura stava oltre, e agli angoli l'inversa non esisteva affatto.
     * ⚠️ **A prendere il difetto è stata la prova dell'andata e ritorno**, non una lettura: il
     * palco disegnava senza dare nessun errore, e a sbagliare era il colore mirato, cioè un pixel
     * preso da un'altra parte della fotografia. Chi alza questo numero rifà quel difetto.
     * ⚠️ **Il margine è voluto**: a `0,12` la derivata all'angolo di un quadrato vale `0,28`,
     * mentre a `0,16` sarebbe un decimo, cioè un angolo che non si muove quasi più e un Newton che
     * ci mette molti più giri. Una correzione a barile di un grandangolo sta ben dentro questa
     * corsa.
     */
    const val BEND = 0.12f

    /**
     * Quante celle per lato ha la maglia.
     *
     * ⚠️ **Il numero governa l'errore della sola distorsione**, perché gli altri quattro comandi
     * sono lineari a tratti per costruzione: là la maglia è esatta ai vertici **e** in mezzo.
     */
    const val CELLS = 32

    /**
     * Quanti punti si campionano su ogni lato del contorno per la scala di copertura.
     *
     * ⚠️ **Non bastano i quattro angoli**: con la distorsione a barile il punto più rientrato del
     * contorno sta in mezzo a un lato, e guardando i soli angoli si otterrebbe una scala che lascia
     * scoperta proprio quella parte.
     *
     * ⚠️⚠️ **ERANO VENTIQUATTRO, E VENTIQUATTRO LASCIAVANO QUATTRO PUNTI SCOPERTI**: il minimo
     * della [cover] cade fra due campioni, quindi una griglia larga lo **sopravvaluta** e la scala
     * viene un filo corta. Col raddrizzamento a fondo corsa la prova ha contato quattro punti di
     * bordo che venivano da fuori dell'immagine, cioè quattro pixel di fondo.
     * ⚠️ **Il conto si fa una volta per piano e non per vertice**, che è la ragione per cui questo
     * numero si può alzare senza guardare il costo: la griglia della maglia ne ha più di mille, e
     * questa è una passata sola sul contorno.
     */
    const val EDGE_STEPS = 64

    /**
     * Il conto pronto per un rettangolo largo [w] e alto [h] col centro in ([cx], [cy]).
     *
     * ⚠️ **Il rettangolo si passa per intero e non come misura**: l'anteprima lo disegna dove la
     * vista lo mette, il salvataggio all'origine, e un centro implicito costringerebbe uno dei due
     * a spostare le coordinate prima e dopo.
     */
    fun plan(geo: Geometry, cx: Float, cy: Float, w: Float, h: Float): WarpPlan {
        val half = max(w, h) / 2f
        val safe = if (half > 0f) half else 1f
        val angle = geo.straighten * TILT * Math.PI.toFloat() / 180f
        val plain = WarpPlan(
            half = safe,
            cx = cx,
            cy = cy,
            ax = (w / 2f) / safe,
            ay = (h / 2f) / safe,
            cosT = cos(angle),
            sinT = sin(angle),
            stretch = 1f + geo.aspect * STRETCH,
            slantX = geo.horizontal * SLANT,
            slantY = geo.vertical * SLANT,
            bend = geo.distortion * BEND,
            cover = 1f
        )
        return WarpPlan(
            half = plain.half,
            cx = plain.cx,
            cy = plain.cy,
            ax = plain.ax,
            ay = plain.ay,
            cosT = plain.cosT,
            sinT = plain.sinT,
            stretch = plain.stretch,
            slantX = plain.slantX,
            slantY = plain.slantY,
            bend = plain.bend,
            cover = cover(plain)
        )
    }

    /**
     * Di quanto si deve ingrandire l'immagine deformata perché il rettangolo resti coperto.
     *
     * ⚠️⚠️ **SENZA DI LEI UN RADDRIZZAMENTO LASCEREBBE QUATTRO CUNEI VUOTI AGLI ANGOLI**, che è
     * quello che si vede in ogni editor che non ha questo conto: si ruota di due gradi e agli
     * angoli compare il fondo. Qui la scala si **ricava** invece di essere un sesto cursore, e chi
     * raddrizza non deve poi ingrandire a mano per togliere i vuoti che ha appena fatto.
     *
     * ⚠️ **Si misura sul CONTORNO e non sui quattro angoli**: il punto più rientrato dipende dal
     * comando (con un keystone è un angolo, con la distorsione a barile è il mezzo di un lato), e i
     * soli angoli lascerebbero scoperta la seconda famiglia.
     *
     * ⚠️ **Regge perché la regione deformata contiene il centro e non si ripiega**: i cursori
     * arrivano fin dove le costanti qui sopra dicono, e in quell'intervallo la mappatura è
     * monotona lungo ogni raggio. Chi alzasse una di quelle costanti di molto guardi prima questa
     * riga.
     */
    fun cover(plan: WarpPlan): Float {
        var worst = Float.MAX_VALUE
        val w = plan.ax * plan.half
        val h = plan.ay * plan.half
        for (i in 0..EDGE_STEPS) {
            val t = i.toFloat() / EDGE_STEPS
            val xs = plan.cx - w + 2f * w * t
            val ys = plan.cy - h + 2f * h * t
            // I quattro lati del contorno: sopra, sotto, sinistra, destra.
            for (p in listOf(
                plan.map(xs, plan.cy - h),
                plan.map(xs, plan.cy + h),
                plan.map(plan.cx - w, ys),
                plan.map(plan.cx + w, ys)
            )) {
                val dx = if (w > 0f) abs(p[0] - plan.cx) / w else 0f
                val dy = if (h > 0f) abs(p[1] - plan.cy) / h else 0f
                worst = min(worst, max(dx, dy))
            }
        }
        if (worst <= 0f || worst >= 1f || worst == Float.MAX_VALUE) return 1f
        return 1f / worst
    }

    /**
     * I vertici della maglia: dove ogni punto della griglia **arriva**.
     *
     * ⚠️ **La griglia di partenza è uniforme sul rettangolo**, cioè è anche l'elenco delle
     * coordinate da cui il disegno legge: chi chiama passa la stessa griglia come texture, e le due
     * cose insieme sono la mappatura.
     */
    fun points(plan: WarpPlan, dest: RectF, cells: Int = CELLS): FloatArray {
        val out = FloatArray((cells + 1) * (cells + 1) * 2)
        var at = 0
        for (j in 0..cells) {
            val y = dest.top + dest.height() * j / cells
            for (i in 0..cells) {
                val x = dest.left + dest.width() * i / cells
                val p = plan.map(x, y)
                out[at++] = p[0]
                out[at++] = p[1]
            }
        }
        return out
    }

    /** La griglia non deformata, cioè da dove si legge ogni vertice. */
    fun grid(dest: RectF, cells: Int = CELLS): FloatArray {
        val out = FloatArray((cells + 1) * (cells + 1) * 2)
        var at = 0
        for (j in 0..cells) {
            val y = dest.top + dest.height() * j / cells
            for (i in 0..cells) {
                val x = dest.left + dest.width() * i / cells
                out[at++] = x
                out[at++] = y
            }
        }
        return out
    }

    /**
     * I triangoli della maglia, due per cella.
     *
     * ⚠️ **Gli indici sono `Short` perché il disegno li vuole così**, e il tetto lo regge: con
     * [CELLS] celle per lato i vertici sono poco più di mille, cioè un trentesimo di quanto un
     * indice a sedici bit può contare.
     */
    fun indices(cells: Int = CELLS): ShortArray {
        val out = ShortArray(cells * cells * 6)
        var at = 0
        val row = cells + 1
        for (j in 0 until cells) {
            for (i in 0 until cells) {
                val a = (j * row + i).toShort()
                val b = (j * row + i + 1).toShort()
                val c = ((j + 1) * row + i).toShort()
                val d = ((j + 1) * row + i + 1).toShort()
                out[at++] = a
                out[at++] = b
                out[at++] = c
                out[at++] = b
                out[at++] = d
                out[at++] = c
            }
        }
        return out
    }

    /**
     * Disegna dentro [dest] quello che [paint] sa dipingere, deformato da [plan].
     *
     * ⚠️⚠️ **È LA FUNZIONE CHE HANNO IN COMUNE L'ANTEPRIMA E IL SALVATAGGIO, ED È TUTTO IL PUNTO DI
     * QUESTO FILE**: il palco le passa il pennello che porta lo shader del colore, il salvataggio
     * un pennello che porta il file già sviluppato, e la maglia è la stessa. Due disegni scritti in
     * due posti darebbero un'anteprima e un file che si somigliano finché nessuno tocca uno dei
     * due.
     *
     * ⚠️ **Le coordinate della texture sono in unità del PENNELLO**: chi chiama costruisce un
     * pennello che dipinge [dest] con l'immagine intera, quindi la griglia non deformata è già
     * l'elenco dei punti da cui leggere, e non serve una seconda conversione.
     */
    fun draw(canvas: Canvas, dest: RectF, plan: WarpPlan, paint: Paint, cells: Int = CELLS) {
        val verts = points(plan, dest, cells)
        val texs = grid(dest, cells)
        val order = indices(cells)
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES,
            verts.size,
            verts,
            0,
            texs,
            0,
            null,
            0,
            order,
            0,
            order.size,
            paint
        )
    }

    /**
     * [source] deformato da [geo], oppure `null` se la mappa di pixel non si è potuta costruire.
     *
     * ⚠️⚠️ **QUI NON SERVE NESSUNA SCHEDA GRAFICA, al contrario del conto del colore**: una maglia
     * di triangoli si disegna anche su una tela di memoria, quindi il file pieno si deforma in un
     * colpo solo e le tessere non c'entrano. È la seconda ragione per cui questo modulo non vive
     * nello shader.
     *
     * ⚠️ **L'immagine esce della stessa misura**: la deformazione lavora **dentro** il riquadro, e
     * la scala di copertura è quella che glielo garantisce. Un riquadro che cambiasse misura
     * vorrebbe dire un ritaglio deciso da noi invece che da lui.
     */
    fun render(source: Bitmap, geo: Geometry): Bitmap? {
        if (geo.idle) return null
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return null
        val out = try {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return null
        }
        val dest = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val plan = plan(geo, w / 2f, h / 2f, w.toFloat(), h.toFloat())
        val paint = Paint().apply {
            isAntiAlias = true
            /*
             * ⚠️ **Il filtro serve qui più che altrove**: i vertici cadono a coordinate che non
             * sono pixel interi quasi sempre, e senza filtro il ricampionamento arrotonderebbe al
             * pixel più vicino, cioè farebbe i gradini su ogni riga inclinata.
             */
            isFilterBitmap = true
            shader = BitmapShader(source, TileMode.CLAMP, TileMode.CLAMP)
        }
        return try {
            draw(Canvas(out), dest, plan, paint)
            out
        } catch (e: RuntimeException) {
            out.recycle()
            null
        }
    }
}
