package io.github.roccobot.aiv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Un **preset**: un nome e i cinque moduli di colore che quel nome porta con sé.
 *
 * ⚠️⚠️ **UN PRESET NON PORTA LA POSA, IL RITAGLIO E LA GEOMETRIA, E NON È UNA SEMPLIFICAZIONE**:
 * quei tre dipendono da **come è stata scattata quella fotografia** (da che parte sta il cielo,
 * dove finisce il soggetto, quanto pende l'orizzonte), mentre un preset è un aspetto che si porta
 * da un'immagine all'altra. Applicarne uno che raddrizza di tre gradi girerebbe anche le
 * fotografie dritte.
 * - ⚠️ **Quindi i moduli sono cinque e non sette**, e coincidono esattamente con quelli che i
 *   preset di Lightroom sanno dire: Luce, Colore, HSL, Dettaglio e Curve.
 *
 * ⚠️⚠️ **IL NOME NON SI TRADUCE, NEMMENO QUELLO DEI VENTI DI CASA**: è un nome proprio, come
 * quello di una cartella, e la cosa che lo distingue è che sia sempre lo stesso. Tradurlo
 * vorrebbe dire venti stringhe per ventotto lingue per dei nomi che chi li ha scritti riconosce
 * così come sono.
 */
data class Preset(
    val name: String,
    val look: Look,
    /**
     * Se è uno dei venti che l'app porta con sé.
     *
     * ⚠️ **Serve a una cosa sola, e non è la grafica**: un preset di casa non si cancella e non si
     * rinomina, perché non vive in nessun archivio da cui toglierlo. Chi non lo vuole lo ignora.
     */
    val house: Boolean = false
) {

    /**
     * [base] con sopra questo preset: i cinque moduli si sostituiscono, tutto il resto resta.
     *
     * ⚠️⚠️ **SOSTITUISCE INVECE DI SOMMARE, ED È LA SCELTA CHE RENDE UN PRESET PREVEDIBILE**: con
     * una somma, applicare due volte lo stesso preset darebbe due immagini diverse, e applicarne
     * uno sopra un altro darebbe qualcosa che nessuno dei due descrive. Così invece un preset
     * dice **dove si arriva**, e per tornare indietro c'è 'Annulla', perché quello che ne esce è
     * un [Look] come un altro.
     */
    fun applyTo(base: Look): Look = base.copy(
        light = look.light,
        chroma = look.chroma,
        mix = look.mix,
        detail = look.detail,
        tone = look.tone
    )

    companion object {

        /** Quello che di [look] un preset porta con sé, cioè i cinque moduli di colore. */
        fun of(name: String, look: Look): Preset = Preset(
            name = name.trim(),
            look = Look(
                light = look.light, chroma = look.chroma, mix = look.mix,
                detail = look.detail, tone = look.tone
            )
        )

        /** Quanto può essere lungo un nome dato a mano: il resto si taglia. */
        const val NAME_MAX = 40
    }
}

/**
 * Dove vivono i preset propri, e come si leggono i venti di casa.
 *
 * ⚠️⚠️ **L'ARCHIVIO È UN FILE E NON UNA PREFERENZA**, al contrario di quasi tutto il resto
 * dell'app: una preferenza tiene un valore, qui invece cresce un elenco di oggetti annidati (otto
 * fasce e quattro curve per ognuno), e scriverlo in un `DataStore` vorrebbe dire una stringa
 * lunghissima sotto una chiave sola, cioè un file travestito.
 * - ⚠️ **In `filesDir` e non in `cacheDir`**, per la stessa ragione delle copertine: quello che
 *   vive nella cache il sistema lo può buttare, e un preset salvato sparirebbe da solo.
 *
 * ⚠️⚠️ **SI LEGGE E SI SCRIVE A MANO CON `org.json`, SENZA NESSUNA LIBRERIA**: quella classe è
 * nella piattaforma da sempre, quindi non aggiunge un byte all'APK, e il formato che ne esce si
 * apre con qualunque editor. Una libreria di serializzazione automatica avrebbe legato la forma
 * del file alla forma delle classi, e il giorno che un modulo prende un campo i preset salvati
 * non si leggerebbero più.
 * - ⚠️ **Ogni campo che manca vale il suo valore di riposo**, ed è quello che rende il formato
 *   compatibile in avanti e all'indietro: un file scritto oggi si legge domani anche se domani i
 *   cursori sono sei, e un file scritto domani si legge oggi perdendo quello che oggi non esiste.
 */
object Presets {

    /** I preset dell'utente, dal più recente al più vecchio, e poi i venti di casa. */
    fun all(context: Context): List<Preset> = mine(context) + HOUSE

    /** I soli preset salvati dall'utente. */
    fun mine(context: Context): List<Preset> {
        val file = store(context)
        if (!file.isFile) return emptyList()
        val testo = runCatching { file.readText() }.getOrNull() ?: return emptyList()
        val righe = runCatching { JSONArray(testo) }.getOrNull() ?: return emptyList()
        return (0 until righe.length()).mapNotNull { i ->
            val o = righe.optJSONObject(i) ?: return@mapNotNull null
            val nome = o.optString("name").trim()
            if (nome.isEmpty()) null else Preset(nome, readLook(o))
        }
    }

    /**
     * Salva [look] col nome [name], e ridà l'elenco nuovo.
     *
     * ⚠️ **Un nome già usato SOSTITUISCE invece di aggiungere**, e il confronto non guarda le
     * maiuscole: due preset che si chiamano uguale sono indistinguibili nell'elenco, quindi
     * l'unica cosa che si potrebbe fare con il secondo è cercare di capire quale sia.
     */
    fun save(context: Context, name: String, look: Look): List<Preset> {
        val fresco = Preset.of(name.take(Preset.NAME_MAX), look)
        if (fresco.name.isEmpty()) return mine(context)
        val resto = mine(context).filterNot { it.name.equals(fresco.name, ignoreCase = true) }
        return write(context, listOf(fresco) + resto)
    }

    /** Toglie il preset [name] dai propri, e ridà l'elenco nuovo. */
    fun remove(context: Context, name: String): List<Preset> =
        write(context, mine(context).filterNot { it.name.equals(name, ignoreCase = true) })

    /** Scrive l'elenco e lo ridà, o ridà quello vecchio se il disco non collabora. */
    private fun write(context: Context, elenco: List<Preset>): List<Preset> {
        val righe = JSONArray()
        elenco.forEach { righe.put(writeLook(it)) }
        val fatto = runCatching { store(context).writeText(righe.toString()) }.isSuccess
        return if (fatto) elenco else mine(context)
    }

    private fun store(context: Context): File = File(context.filesDir, STORE)

    // ── Andata e ritorno ─────────────────────────────────────────────────────
    //
    // ⚠️⚠️ **UN CAMPO A RIPOSO NON SI SCRIVE**, e non è un risparmio di byte: un preset di sola
    // Luce deve leggersi come tale, e con i moduli scritti per intero il file direbbe che tocca
    // anche il colore e le curve, a zero. Chi lo apre vedrebbe un preset che fa tutto.

    private fun writeLook(p: Preset): JSONObject {
        val o = JSONObject()
        o.put("name", p.name)
        val l = p.look.light
        if (!l.idle) o.put("light", JSONObject().apply {
            num("exposure", l.exposure); num("contrast", l.contrast)
            num("highlights", l.highlights); num("shadows", l.shadows)
            num("whites", l.whites); num("blacks", l.blacks)
        })
        val c = p.look.chroma
        if (!c.idle || c.mono) o.put("chroma", JSONObject().apply {
            num("temp", c.temp); num("tint", c.tint)
            num("saturation", c.saturation); num("vibrance", c.vibrance)
            if (c.mono) put("mono", true)
            num("filter", c.filter)
        })
        if (!p.look.mix.idle) o.put("mix", JSONArray().apply {
            p.look.mix.bands.forEach { b ->
                put(JSONObject().apply { num("hue", b.hue); num("sat", b.sat); num("lum", b.lum) })
            }
        })
        val d = p.look.detail
        if (!d.idle) o.put("detail", JSONObject().apply {
            num("sharpen", d.sharpen); num("radius", d.radius); num("masking", d.masking)
            num("noise", d.noise); num("noiseColor", d.noiseColor)
        })
        if (!p.look.tone.idle) o.put("tone", JSONObject().apply {
            curveOut("all", p.look.tone.all); curveOut("red", p.look.tone.red)
            curveOut("green", p.look.tone.green); curveOut("blue", p.look.tone.blue)
        })
        return o
    }

    private fun readLook(o: JSONObject): Look {
        val l = o.optJSONObject("light")
        val c = o.optJSONObject("chroma")
        val m = o.optJSONArray("mix")
        val d = o.optJSONObject("detail")
        val t = o.optJSONObject("tone")
        return Look(
            light = if (l == null) Light.NONE else Light(
                exposure = l.num("exposure"), contrast = l.num("contrast"),
                highlights = l.num("highlights"), shadows = l.num("shadows"),
                whites = l.num("whites"), blacks = l.num("blacks")
            ),
            chroma = if (c == null) Chroma.NONE else Chroma(
                temp = c.num("temp"), tint = c.num("tint"),
                saturation = c.num("saturation"), vibrance = c.num("vibrance"),
                mono = c.optBoolean("mono", false), filter = c.num("filter")
            ),
            mix = if (m == null) Mix.NONE else Mix(List(Mix.COUNT) { i ->
                val b = m.optJSONObject(i) ?: return@List Band.NONE
                Band(hue = b.num("hue"), sat = b.num("sat"), lum = b.num("lum"))
            }),
            detail = if (d == null) Detail.NONE else Detail(
                sharpen = d.num("sharpen"), radius = d.num("radius"),
                masking = d.num("masking"), noise = d.num("noise"),
                noiseColor = d.num("noiseColor")
            ),
            tone = if (t == null) Tone.NONE else Tone(
                all = t.curveIn("all"), red = t.curveIn("red"),
                green = t.curveIn("green"), blue = t.curveIn("blue")
            )
        )
    }

    /** Scrive [v] solo se non è a riposo: vedi la nota qui sopra. */
    private fun JSONObject.num(key: String, v: Float) {
        if (v > DEAD || v < -DEAD) put(key, v.toDouble())
    }

    private fun JSONObject.num(key: String): Float = optDouble(key, 0.0).toFloat()

    /**
     * Scrive una curva come un elenco piatto di numeri: `at, to, at, to, ...`.
     *
     * ⚠️ **Piatto e non un elenco di coppie**: un punto è sempre due numeri, quindi la forma
     * annidata costerebbe due parentesi per punto senza dire niente di più.
     */
    private fun JSONObject.curveOut(key: String, c: Curve) {
        if (c.idle) return
        put(key, JSONArray().apply {
            c.knots.forEach { put(it.at.toDouble()); put(it.to.toDouble()) }
        })
    }

    private fun JSONObject.curveIn(key: String): Curve {
        val a = optJSONArray(key) ?: return Curve.NONE
        if (a.length() < 4 || a.length() % 2 != 0) return Curve.NONE
        val punti = (0 until a.length() step 2).map {
            Knot(a.optDouble(it, 0.0).toFloat(), a.optDouble(it + 1, 0.0).toFloat())
        }
        return Curve(punti)
    }

    /** Sotto questa soglia un valore non si scrive: è la stessa dei moduli. */
    private const val DEAD = 0.0005f

    /** Come si chiama il file dei preset propri, dentro `filesDir`. */
    private const val STORE = "presets.json"
}

// ── I venti di casa ──────────────────────────────────────────────────────────

/**
 * Una fascia per volta, invece di scrivere otto `Band` per ogni preset: quelle che non compaiono
 * sono a riposo.
 */
private fun mix(vararg bands: Pair<Int, Band>): Mix =
    Mix(List(Mix.COUNT) { i -> bands.firstOrNull { it.first == i }?.second ?: Band.NONE })

/** Una curva dai suoi punti, scritti come `x to y`. */
private fun curve(vararg knots: Pair<Float, Float>): Curve =
    Curve(knots.map { Knot(it.first, it.second) })

/** Un preset di casa, coi suoi cinque moduli facoltativi. */
private fun house(
    name: String,
    light: Light = Light.NONE,
    chroma: Chroma = Chroma.NONE,
    mix: Mix = Mix.NONE,
    detail: Detail = Detail.NONE,
    tone: Tone = Tone.NONE
): Preset = Preset(name, Look(light = light, chroma = chroma, mix = mix, detail = detail,
    tone = tone), house = true)

/**
 * I venti preset che l'app porta con sé.
 *
 * ⚠️⚠️ **QUATTORDICI SONO I SUOI, CONVERTITI DAI SUOI XMP DI LIGHTROOM, E SEI SONO SCRITTI IN
 * CASA** (sua istruzione, 2026-09-14: *aggiungi i miei, più uno creato ex novo da te per arrivare
 * alla cifra tonda di 20*). I suoi erano diciannove, e cinque non si sono potuti portare perché
 * fatti **soltanto** di cose che AIV non ha: due sono taratura dei primari della fotocamera, due
 * sono maschere locali, e uno è viraggio diviso con color grading a tre zone. Quindi i mancanti
 * sono sei invece di uno, ed è la ragione per cui i preset di casa non sono uno.
 *
 * ⚠️⚠️ **LA CONVERSIONE L'HA FATTA LA SESSIONE E NON C'È NESSUN LETTORE XMP NELL'APP**, ed è sua
 * istruzione (*per ora lasciamo stare l'importazione degli XMP*): quello che vive qui sono i
 * valori **già tradotti** nelle scale di AIV, e un lettore di file XMP sarebbe un meccanismo in
 * più da mantenere per un gesto che si fa una volta.
 * - **Come si sono tradotti**: l'esposizione è in stop e passa tale e quale; tutto il resto in
 *   Lightroom va da -100 a +100 e qui da -1 a +1, quindi si divide per cento; il raggio della
 *   maschera di contrasto è l'unico che cambia forma, perché là è un numero di pixel con l'uno
 *   come valore di serie e qui lo zero è quel valore di serie, quindi si prende il logaritmo in
 *   base due; e i punti di una curva là vanno da 0 a 255 e qui da 0 a 1.
 * - ⚠️ **Le otto fasce dell'HSL combaciano una a una**, e non è una fortuna: i centri di [Mix]
 *   sono gli otto di Lightroom, perché di là vengono.
 *
 * ⚠️ **Gli ultimi due dei sei sono riscritture dichiarate e non travasi**: 'Primari caldi' e
 * 'Primari verdi' rifanno nelle nostre fasce quello che i suoi 'Contrasto colore classico' e
 * 'Contrasto colore foliage' ottenevano dalla taratura dei primari, che è un'altra macchina. Il
 * risultato somiglia, la strada no, e per questo hanno un nome diverso dal suo.
 */
val HOUSE: List<Preset> = listOf(
    house("Combo",
        light = Light(exposure = -.2f, contrast = -.3f, highlights = -.3f, shadows = .2f, blacks = .4f),
        chroma = Chroma(saturation = .1f, vibrance = .1f),
        mix = mix(
            0 to Band(hue = .07f, sat = .1f, lum = .07f), 1 to Band(hue = -.02f, sat = .2f, lum = -.18f),
            2 to Band(hue = -.11f, sat = .03f, lum = .05f), 3 to Band(sat = .08f, lum = .05f),
            4 to Band(hue = -.05f, sat = -.09f, lum = .1f), 5 to Band(hue = -.09f, sat = -.25f, lum = .2f),
            6 to Band(hue = .04f), 7 to Band(hue = .01f)
        ),
        detail = Detail(sharpen = .85f, radius = .6781f, masking = .8f, noise = .25f, noiseColor = .12f),
        tone = Tone(all = curve(
            0f to .0706f, .149f to .1451f, .251f to .2196f,
            .502f to .502f, .8824f to .8706f, 1f to .9647f
        ))
    ),
    house("Rosso←",
        mix = mix(
            0 to Band(sat = .1f, lum = .07f), 1 to Band(hue = -.12f, sat = .2f, lum = -.18f),
            2 to Band(hue = -.11f, sat = .03f, lum = .05f), 3 to Band(sat = .08f, lum = .05f),
            4 to Band(hue = -.05f, sat = -.09f, lum = .1f), 5 to Band(hue = -.09f, sat = -.25f, lum = .2f),
            6 to Band(hue = .04f), 7 to Band(hue = .01f)
        )
    ),
    house("Rosso←←",
        mix = mix(
            0 to Band(hue = -.06f, lum = -.35f), 1 to Band(hue = -.14f, lum = -.2f),
            2 to Band(hue = -.11f, sat = .03f, lum = .05f), 3 to Band(sat = .08f, lum = .05f),
            4 to Band(hue = -.05f, sat = -.09f, lum = .1f), 5 to Band(hue = -.09f, sat = -.25f, lum = .2f),
            6 to Band(hue = .04f), 7 to Band(hue = .01f)
        )
    ),
    house("Color grading- caldo",
        mix = mix(
            0 to Band(hue = -.12f, sat = .05f, lum = .07f), 1 to Band(hue = -.08f, sat = .05f, lum = -.14f),
            2 to Band(hue = .08f, sat = .03f, lum = .05f), 3 to Band(hue = -.12f, sat = .08f, lum = .05f),
            4 to Band(hue = -.05f, sat = -.03f, lum = .13f), 5 to Band(hue = -.09f, sat = -.14f, lum = .08f),
            6 to Band(hue = .13f, sat = .13f), 7 to Band(hue = .01f)
        )
    ),
    house("Curva chiaroscuro",
        tone = Tone(all = curve(
            0f to .0706f, .149f to .1451f, .251f to .2196f,
            .502f to .502f, .8824f to .8706f, 1f to .9647f
        ))
    ),
    house("Curva onde",
        tone = Tone(all = curve(
            0f to .0706f, .1569f to .1882f, .2863f to .2667f,
            .498f to .5059f, .5608f to .5451f, .8824f to .8706f,
            1f to .9647f
        ))
    ),
    house("Dettagli fini",
        detail = Detail(sharpen = .7f, masking = .7f)
    ),
    house("Dettagli grossolani",
        detail = Detail(sharpen = .85f, radius = .6781f, masking = .75f, noise = .2f, noiseColor = .1f)
    ),
    house("Mix colori turchese",
        mix = mix(
            0 to Band(hue = .02f), 1 to Band(hue = -.03f),
            2 to Band(hue = -.33f), 3 to Band(hue = .02f),
            4 to Band(hue = -.23f), 5 to Band(hue = -.4f),
            6 to Band(hue = -.13f)
        )
    ),
    house("Neutro",
        mix = mix(
            0 to Band(sat = .05f), 1 to Band(hue = .07f, sat = -.49f, lum = .05f),
            2 to Band(hue = .2f, sat = .06f, lum = .05f), 3 to Band(hue = .06f, sat = .35f, lum = .05f),
            4 to Band(hue = .42f, sat = .1f, lum = -.05f), 5 to Band(sat = .25f)
        )
    ),
    house("Chiaro",
        mix = mix(
            0 to Band(hue = .38f, sat = -.06f, lum = -.21f), 1 to Band(sat = -.17f),
            2 to Band(hue = -.75f, sat = .18f, lum = .19f), 3 to Band(hue = -.78f, sat = -.08f),
            4 to Band(hue = -.38f, sat = -.08f, lum = .22f), 5 to Band(hue = -.22f, sat = .26f, lum = .39f),
            6 to Band(hue = .28f, sat = -.63f), 7 to Band(hue = .64f, sat = -.62f)
        )
    ),
    house("Standard",
        mix = mix(
            0 to Band(hue = .36f, sat = .05f), 1 to Band(hue = -.06f, sat = .49f, lum = .05f),
            2 to Band(hue = .03f, sat = .28f, lum = .05f), 3 to Band(hue = .4f, sat = .35f, lum = .05f),
            4 to Band(hue = -.12f, sat = .43f, lum = -.05f), 5 to Band(hue = -.32f, sat = .62f)
        )
    ),
    house("Slavato",
        mix = mix(
            0 to Band(hue = .36f, sat = -.13f), 1 to Band(sat = -.15f),
            2 to Band(hue = -.44f, sat = -.1f), 3 to Band(hue = 1f),
            4 to Band(hue = .1f, sat = -.11f, lum = -.32f), 5 to Band(hue = -.17f, sat = .19f, lum = -.23f),
            6 to Band(hue = -.1f, sat = -.63f), 7 to Band(hue = -.13f, sat = -.62f)
        )
    ),
    house("Blu Rosso",
        light = Light(
            exposure = -.15f, contrast = -.5f, highlights = -.6f, shadows = .5f, whites = .5f, 
            blacks = .2f
        ),
        chroma = Chroma(vibrance = -.08f),
        mix = mix(
            0 to Band(hue = .06f, sat = .01f), 1 to Band(hue = -.01f, sat = .08f, lum = .01f),
            2 to Band(hue = .01f, sat = .05f, lum = .01f), 3 to Band(hue = .07f, sat = .06f, lum = .01f),
            4 to Band(hue = -.02f, sat = .07f, lum = -.01f), 5 to Band(hue = -.05f, sat = .1f, lum = .1f)
        ),
        detail = Detail(sharpen = .5f, masking = .74f, noise = .12f, noiseColor = .14f),
        tone = Tone(all = curve(
            0f to .0706f, .149f to .1451f, .251f to .2196f,
            .502f to .502f, .8824f to .8706f, 1f to .9647f
        ))
    ),
    house("Bianco e nero",
        light = Light(contrast = .15f, blacks = -.1f),
        chroma = Chroma(mono = true, filter = .35f),
        mix = mix(
            3 to Band(lum = .15f), 5 to Band(lum = -.3f)
        )
    ),
    house("Ritratto",
        light = Light(highlights = -.25f, shadows = .2f, blacks = .08f),
        chroma = Chroma(temp = .05f, vibrance = .2f),
        mix = mix(
            0 to Band(sat = -.08f), 1 to Band(hue = .05f, sat = -.12f, lum = .12f),
            2 to Band(sat = -.1f)
        ),
        detail = Detail(sharpen = .35f, masking = .8f)
    ),
    house("Notturno",
        light = Light(exposure = .25f, highlights = -.2f, shadows = .45f, blacks = .15f),
        detail = Detail(sharpen = .3f, masking = .6f, noise = .55f, noiseColor = .45f)
    ),
    house("Pellicola",
        light = Light(contrast = .1f, whites = -.1f, blacks = .25f),
        chroma = Chroma(saturation = -.15f, vibrance = .12f),
        tone = Tone(all = curve(
            0f to .055f, .25f to .22f, .75f to .79f,
            1f to .96f
        ))
    ),
    house("Primari caldi",
        light = Light(contrast = .08f),
        mix = mix(
            0 to Band(hue = .28f, sat = -.5f), 1 to Band(hue = .15f, sat = -.2f),
            3 to Band(sat = .45f), 5 to Band(hue = -.3f, sat = .08f)
        )
    ),
    house("Primari verdi",
        light = Light(contrast = .08f),
        mix = mix(
            0 to Band(hue = .28f, sat = -.5f), 1 to Band(hue = .15f, sat = -.2f),
            2 to Band(hue = .4f, sat = .3f), 3 to Band(hue = .75f, sat = 1f),
            4 to Band(sat = .2f), 5 to Band(sat = .4f)
        )
    )
)
