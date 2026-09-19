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
 * - ⚠️ **Quindi i moduli sono cinque e non otto**, e coincidono esattamente con quelli che i
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
     * Con che cosa questo preset si nomina nell'archivio.
     *
     * ⚠️⚠️ **NASCE CON LA `2.50`, E SENZA DI LEI UN PRESET DI CASA NON SI POTREBBE RINOMINARE**
     * (sua richiesta, giro della `2.40`: *si possono riordinare, rinominare e cancellare a
     * piacere sia i predefiniti di fabbrica che quelli creati dall'utente*). Il nome è quello che
     * si vede e adesso si può cambiare, quindi non può più essere anche l'identità: l'archivio
     * tiene le rinomine, i cancellati e l'ordine, e li tiene **per chiave**.
     * ⚠️ **Per i propri la chiave È il nome**, perché là l'identità non ha un secondo posto in cui
     * vivere: due preset propri che si chiamano uguale sono indistinguibili nell'elenco, e per
     * questo salvarne uno col nome di un altro lo sostituisce.
     */
    val key: String = name,
    /**
     * Se è uno dei venti che l'app porta con sé.
     *
     * ⚠️ **Serve a due cose**: il gruppo in cui compare ('Stili AIV' o 'Stili salvati'), e dove
     * l'archivio scrive quello che di lui è cambiato (una rinomina fra le rinomine, una
     * cancellazione fra i cancellati, invece del preset intero).
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
     * ⚠️ **Dalla `2.50` è quello che fa il TOCCO**, e il tocco lungo fa l'altra cosa: vedi
     * [addTo].
     */
    fun applyTo(base: Look): Look = base.copy(
        light = look.light,
        chroma = look.chroma,
        mix = look.mix,
        detail = look.detail,
        effects = look.effects,
        tone = look.tone
    )

    /**
     * [base] con sopra i **soli moduli che questo preset dichiara**: il tocco lungo, dalla `2.50`.
     *
     * ⚠️⚠️ **È SUA RICHIESTA, E LE DUE RIGHE SONO SUE** (campo libero del giro della `2.40`:
     * *tocco sullo stile = modifica assoluta (azzera tutto, poi modifica); tocco prolungato =
     * modifica additiva (tocca i valori inclusi, non modifica gli altri)*). Il gesto lungo serve a
     * **comporre**: un preset di sole curve sopra uno di sola luce, senza che il secondo porti via
     * quello che il primo aveva messo.
     * ⚠️⚠️ **QUELLO CHE 'DICHIARA' LO DICE IL FORMATO, e non serve un secondo dato**: un modulo a
     * riposo non si scrive nel file (vedi [Presets]), quindi 'questo preset parla di luce?' si
     * risponde chiedendo se la sua luce è a riposo. Un elenco di moduli scritto accanto sarebbe la
     * stessa informazione in due posti, e il primo a divergere sarebbe quello che nessuno guarda.
     * ⚠️ **Un modulo a riposo non azzera niente**: additiva vuol dire che quello che il preset non
     * nomina resta com'è, e un preset che nomina un modulo lo **sostituisce** per intero, perché
     * dentro un modulo i cursori si leggono insieme.
     */
    fun addTo(base: Look): Look = base.copy(
        light = if (look.light.idle) base.light else look.light,
        chroma = if (look.chroma.idle) base.chroma else look.chroma,
        mix = if (look.mix.idle) base.mix else look.mix,
        detail = if (look.detail.idle) base.detail else look.detail,
        effects = if (look.effects.idle) base.effects else look.effects,
        tone = if (look.tone.idle) base.tone else look.tone
    )

    companion object {

        /** Quello che di [look] un preset porta con sé, cioè i cinque moduli di colore. */
        fun of(name: String, look: Look): Preset = Preset(
            name = name.trim(),
            look = Look(
                light = look.light, chroma = look.chroma, mix = look.mix,
                detail = look.detail, effects = look.effects, tone = look.tone
            )
        )

        /** Quanto può essere lungo un nome dato a mano: il resto si taglia. */
        const val NAME_MAX = 40
    }
}

/**
 * Dove vivono i preset, e che cosa di loro l'utente ha cambiato.
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
 *
 * ⚠️⚠️ **DALLA `2.50` IL FILE È UN OGGETTO E NON PIÙ UN ELENCO, E IL VECCHIO SI LEGGE LO STESSO**:
 * finché i preset propri erano l'unica cosa che l'utente poteva toccare, l'archivio era il loro
 * elenco; adesso può **riordinare, rinominare e cancellare anche quelli di casa** (sua richiesta,
 * giro della `2.40`), e quelle tre cose non stanno dentro un elenco di preset propri. Un file
 * scritto prima comincia con `[`, e allora è l'elenco di prima e basta: senza quella riga,
 * aggiornare l'app porterebbe via a chi ce li ha tutti i preset salvati.
 * - **Che cosa tiene**: `mine` (i propri, nell'ordine scelto), `names` (le rinomine di quelli di
 *   casa, per chiave), `gone` (le chiavi di casa cancellate) e `house` (l'ordine di quelli di
 *   casa).
 * - ⚠️ **Di un preset di casa non si scrive mai il contenuto**, nemmeno rinominato: i suoi valori
 *   vivono nel programma, e copiarli nell'archivio vorrebbe dire che una taratura corretta in una
 *   versione futura non arriverebbe a chi lo ha rinominato.
 */
object Presets {

    /** Quello che l'elenco mostra: prima i venti di casa, poi i propri. */
    fun all(context: Context): List<Preset> = house(context) + mine(context)

    /**
     * I preset di casa, nell'ordine scelto, coi nomi effettivi e senza i cancellati.
     *
     * ⚠️ **L'ordine si applica alla lista del programma e non la sostituisce**: una chiave
     * salvata che non esiste più (un preset tolto da una versione futura) si scarta, e una che
     * l'archivio non nomina resta al suo posto di fabbrica. È lo stesso criterio di `padOrderOf`
     * per i riquadri.
     */
    fun house(context: Context): List<Preset> {
        val book = read(context)
        val vivi = HOUSE.filterNot { book.gone.contains(it.key) }
        val posto = book.house.withIndex().associate { (i, k) -> k to i }
        return vivi
            .sortedBy { posto[it.key] ?: (book.house.size + vivi.indexOf(it)) }
            .map { p -> book.names[p.key]?.let { p.copy(name = it) } ?: p }
    }

    /** I soli preset salvati dall'utente, nell'ordine scelto. */
    fun mine(context: Context): List<Preset> = read(context).mine

    /**
     * Salva [look] col nome [name], e ridà l'elenco nuovo dei propri.
     *
     * ⚠️ **Un nome già usato SOSTITUISCE invece di aggiungere**, e il confronto non guarda le
     * maiuscole: due preset che si chiamano uguale sono indistinguibili nell'elenco, quindi
     * l'unica cosa che si potrebbe fare con il secondo è cercare di capire quale sia.
     * ⚠️ **Un nome nuovo va in CODA e non in cima, dalla `2.50`**: l'ordine adesso è una scelta
     * dell'utente, e infilare l'ultimo arrivato davanti a quelli che lui ha disposto vorrebbe dire
     * rifargli la fila a ogni salvataggio.
     */
    fun save(context: Context, name: String, look: Look): List<Preset> {
        val fresco = Preset.of(name.take(Preset.NAME_MAX), look)
        if (fresco.name.isEmpty()) return mine(context)
        val book = read(context)
        val vecchio = book.mine.indexOfFirst { it.name.equals(fresco.name, ignoreCase = true) }
        val nuovi = book.mine.toMutableList()
        if (vecchio >= 0) nuovi[vecchio] = fresco else nuovi.add(fresco)
        return write(context, book.copy(mine = nuovi)).mine
    }

    /**
     * Toglie [p] dall'elenco, e ridà il posto da cui è uscito, per chi lo vuole rimettere.
     *
     * ⚠️⚠️ **UN PRESET DI CASA NON SI CANCELLA DAVVERO: SI NASCONDE**, perché i suoi valori vivono
     * nel programma e non nell'archivio. Quello che si scrive è la sua chiave fra i cancellati, e
     * 'Ripristina' porta via quell'elenco insieme a tutto il resto.
     */
    fun remove(context: Context, p: Preset): Int {
        val book = read(context)
        if (p.house) {
            val dove = book.house.indexOf(p.key)
            write(context, book.copy(gone = book.gone + p.key))
            return dove
        }
        val dove = book.mine.indexOfFirst { it.name.equals(p.name, ignoreCase = true) }
        if (dove < 0) return -1
        write(context, book.copy(mine = book.mine.filterIndexed { i, _ -> i != dove }))
        return dove
    }

    /** Rimette [p] dov'era, cioè quello che fa l''Annulla' della notifica. */
    fun restore(context: Context, p: Preset, at: Int) {
        val book = read(context)
        if (p.house) {
            write(context, book.copy(gone = book.gone - p.key))
            return
        }
        val nuovi = book.mine.toMutableList()
        nuovi.add(at.coerceIn(0, nuovi.size), p)
        write(context, book.copy(mine = nuovi))
    }

    /**
     * Cambia il nome di [p], e ridà il preset com'è adesso.
     *
     * ⚠️ **Per un preset proprio il nome è anche la chiave**, quindi rinominarlo lo fa diventare
     * un altro: se quel nome è già di un altro dei suoi, i due si fonderebbero, e per questo il
     * doppione si scarta invece di sostituire (a sostituire è il salvataggio, dove l'utente ha
     * appena scritto dei valori nuovi).
     */
    fun rename(context: Context, p: Preset, name: String): Preset {
        val pulito = name.trim().take(Preset.NAME_MAX)
        if (pulito.isEmpty()) return p
        val book = read(context)
        if (p.house) {
            write(context, book.copy(names = book.names + (p.key to pulito)))
            return p.copy(name = pulito)
        }
        val preso = book.mine.any {
            !it.name.equals(p.name, ignoreCase = true) && it.name.equals(pulito, ignoreCase = true)
        }
        if (preso) return p
        val nuovo = p.copy(name = pulito, key = pulito)
        write(context, book.copy(mine = book.mine.map { if (it.name == p.name) nuovo else it }))
        return nuovo
    }

    /** Sposta il preset che sta in [from] all'indice [to], dentro il suo gruppo. */
    fun move(context: Context, p: Preset, from: Int, to: Int) {
        val book = read(context)
        if (p.house) {
            val chiavi = houseOrder(book).toMutableList()
            if (from !in chiavi.indices || to !in chiavi.indices) return
            chiavi.add(to, chiavi.removeAt(from))
            write(context, book.copy(house = chiavi))
            return
        }
        val nuovi = book.mine.toMutableList()
        if (from !in nuovi.indices || to !in nuovi.indices) return
        nuovi.add(to, nuovi.removeAt(from))
        write(context, book.copy(mine = nuovi))
    }

    /**
     * Butta via tutto quello che l'utente ha fatto: l'app torna ai venti di casa, nell'ordine e
     * coi nomi di fabbrica.
     *
     * ⚠️ **Si cancella il file invece di riscriverlo vuoto**: l'assenza è già il valore di
     * fabbrica, e un file vuoto vorrebbe dire due modi di dire la stessa cosa.
     */
    fun reset(context: Context) {
        runCatching { store(context).delete() }
    }

    /**
     * L'archivio come testo, per chi lo vuole salvare fuori dall'app.
     *
     * ⚠️⚠️ **SI ESPORTA L'ARCHIVIO INTERO E NON I SOLI STILI PROPRI**, ed è la lettura della sua
     * richiesta fino in fondo (*da dove si potrà salvare un XML o JSON con i propri stili
     * personalizzati, o importarlo in seguito dopo una nuova installazione*): quello che si vuole
     * ritrovare dopo un'installazione nuova non sono solo gli stili creati, sono anche le rinomine
     * e l'ordine in cui li si era messi, cioè tutto quello che di quell'elenco era suo.
     * ⚠️ **È lo stesso testo che vive su disco**, quindi non c'è un secondo formato da tenere
     * allineato al primo, e un file esportato si può rimettere a mano.
     */
    fun export(context: Context): String = book(read(context)).toString(2)

    /**
     * Rimette un archivio esportato, e dice se ci è riuscito.
     *
     * ⚠️ **Sostituisce invece di fondere**, ed è la scelta che rende il gesto prevedibile: un
     * archivio importato è una fotografia di com'era l'elenco, e mescolarlo a quello che c'è
     * darebbe una terza cosa che nessuno dei due descrive. Chi vuole tenere tutti e due esporta
     * prima.
     * ⚠️ **Un testo che non si legge non tocca niente**: la scrittura arriva dopo la lettura, e
     * senza quell'ordine un file storto svuoterebbe l'archivio.
     */
    fun load(context: Context, text: String): Boolean {
        val letto = runCatching { parse(text) }.getOrNull() ?: return false
        write(context, letto)
        return true
    }

    // ── L'archivio ───────────────────────────────────────────────────────────

    /** Quello che il file tiene: i propri, e che cosa è stato fatto a quelli di casa. */
    private data class Book(
        val mine: List<Preset> = emptyList(),
        val names: Map<String, String> = emptyMap(),
        val gone: List<String> = emptyList(),
        val house: List<String> = emptyList()
    )

    private fun read(context: Context): Book {
        val file = store(context)
        if (!file.isFile) return Book()
        val testo = runCatching { file.readText() }.getOrNull() ?: return Book()
        return runCatching { parse(testo) }.getOrDefault(Book())
    }

    /**
     * Legge un archivio, nel formato di oggi o in quello di prima della `2.50`.
     *
     * ⚠️ **La forma si riconosce dal primo carattere e non si indovina**: un `[` è l'elenco di
     * prima, tutto il resto passa da `JSONObject`, che su un testo storto va in errore, e quel
     * caso lo prende chi chiama.
     */
    private fun parse(text: String): Book {
        val pulito = text.trim()
        if (pulito.startsWith("[")) return Book(mine = readMine(JSONArray(pulito)))
        val o = JSONObject(pulito)
        val names = mutableMapOf<String, String>()
        o.optJSONObject("names")?.let { n ->
            n.keys().forEach { k -> n.optString(k).takeIf { it.isNotEmpty() }?.let { names[k] = it } }
        }
        return Book(
            mine = readMine(o.optJSONArray("mine") ?: JSONArray()),
            names = names,
            gone = strings(o.optJSONArray("gone")),
            house = strings(o.optJSONArray("house"))
        )
    }

    private fun book(b: Book): JSONObject {
        val o = JSONObject()
        o.put("mine", JSONArray().apply { b.mine.forEach { put(writeLook(it)) } })
        if (b.names.isNotEmpty()) {
            o.put("names", JSONObject().apply { b.names.forEach { (k, v) -> put(k, v) } })
        }
        if (b.gone.isNotEmpty()) o.put("gone", JSONArray().apply { b.gone.forEach { put(it) } })
        if (b.house.isNotEmpty()) o.put("house", JSONArray().apply { b.house.forEach { put(it) } })
        return o
    }

    /** Scrive l'archivio e lo ridà, o ridà quello vecchio se il disco non collabora. */
    private fun write(context: Context, b: Book): Book {
        val fatto = runCatching { store(context).writeText(book(b).toString()) }.isSuccess
        return if (fatto) b else read(context)
    }

    /**
     * L'ordine dei preset di casa, completato con quello che l'archivio non nomina.
     *
     * ⚠️ **Serve prima di uno spostamento e non alla lettura**: un archivio nato prima della
     * `2.50` non ha nessun ordine, e muovere una riga dentro una lista vuota non vorrebbe dire
     * niente.
     */
    private fun houseOrder(b: Book): List<String> {
        val vive = HOUSE.map { it.key }.filterNot { b.gone.contains(it) }
        val scelte = b.house.filter { vive.contains(it) }
        return scelte + vive.filterNot { scelte.contains(it) }
    }

    private fun strings(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until a.length()).mapNotNull { a.optString(it).takeIf { s -> s.isNotEmpty() } }
    }

    private fun readMine(a: JSONArray): List<Preset> = (0 until a.length()).mapNotNull { i ->
        val o = a.optJSONObject(i) ?: return@mapNotNull null
        val nome = o.optString("name").trim()
        if (nome.isEmpty()) null else Preset(nome, readLook(o))
    }

    private fun store(context: Context): File = File(context.filesDir, STORE)

    // ── Andata e ritorno ─────────────────────────────────────────────────────
    //
    // ⚠️⚠️ **UN CAMPO A RIPOSO NON SI SCRIVE**, e non è un risparmio di byte: un preset di sola
    // Luce deve leggersi come tale, e con i moduli scritti per intero il file direbbe che tocca
    // anche il colore e le curve, a zero. Chi lo apre vedrebbe un preset che fa tutto, e dalla
    // `2.50` anche il tocco lungo leggerebbe il contrario del vero (vedi [Preset.addTo]).

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
        val e = p.look.effects
        if (!e.idle) o.put("effects", JSONObject().apply {
            num("haze", e.haze); num("vignette", e.vignette); num("grain", e.grain)
            num("vignetteFeather", e.vignetteFeather)
            num("grainSize", e.grainSize); num("grainLift", e.grainLift)
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
        val e = o.optJSONObject("effects")
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
            effects = if (e == null) Effects.NONE else Effects(
                haze = e.num("haze"), vignette = e.num("vignette"),
                vignetteFeather = e.num("vignetteFeather"), grain = e.num("grain"),
                grainSize = e.num("grainSize"), grainLift = e.num("grainLift")
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

    /** Come si chiama il file dei preset, dentro `filesDir`. */
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

/** Un preset di casa, con la sua chiave e i suoi sei moduli facoltativi. */
private fun house(
    key: String,
    name: String,
    light: Light = Light.NONE,
    chroma: Chroma = Chroma.NONE,
    mix: Mix = Mix.NONE,
    detail: Detail = Detail.NONE,
    effects: Effects = Effects.NONE,
    tone: Tone = Tone.NONE
): Preset = Preset(
    name = name,
    look = Look(
        light = light, chroma = chroma, mix = mix,
        detail = detail, effects = effects, tone = tone
    ),
    key = key,
    house = true
)

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
 *
 * ⚠️⚠️ **I NOMI E L'ORDINE SONO SUOI, DALLA `2.50`** (campo libero del giro della `2.40`, punto 4:
 * *'Combo' diventa 'Roccobot'. Gli altri vanno elencati in ordine alfabetico, dopo queste
 * rinomine*). Quindi 'Roccobot' è il primo e gli altri diciannove seguono in ordine alfabetico,
 * che è l'ordine in cui questa lista è scritta.
 * - ⚠️⚠️ **L'ORDINE SI SCRIVE E LO PRESIDIA IL BANCO, invece di ordinarlo a ogni lettura**: un
 *   `sortedBy` dipenderebbe da come la piattaforma confronta due stringhe (le maiuscole, gli
 *   accenti, la lingua del telefono), quindi la fila che l'utente vede cambierebbe col telefono.
 *   La prova misura l'invariante, cioè che dal secondo in poi siano in ordine, e un preset nuovo
 *   messo nel posto sbagliato la fa diventare rossa.
 * - ⚠️ **Le sue frecce diventano trattini**, ed è la regola dei caratteri applicata: aveva scritto
 *   `Rosso –` con un trattino lungo, che in questo progetto non si usa da nessuna parte, nemmeno
 *   in un nome proprio.
 * - ⚠️ **La chiave non segue il nome**, e per questo rinominare 'Pellicola' in 'Curva pellicola'
 *   non ha toccato la sua: quello che vive nell'archivio di chi aggiorna è la chiave, e cambiarla
 *   vorrebbe dire un preset che riappare dopo essere stato cancellato.
 */
val HOUSE: List<Preset> = listOf(
    house("combo", "Roccobot",
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
    house("bn", "Bianco e nero",
        light = Light(contrast = .15f, blacks = -.1f),
        chroma = Chroma(mono = true, filter = .35f),
        mix = mix(
            3 to Band(lum = .15f), 5 to Band(lum = -.3f)
        )
    ),
    house("grading-caldo", "Color grading (caldo)",
        mix = mix(
            0 to Band(hue = -.12f, sat = .05f, lum = .07f), 1 to Band(hue = -.08f, sat = .05f, lum = -.14f),
            2 to Band(hue = .08f, sat = .03f, lum = .05f), 3 to Band(hue = -.12f, sat = .08f, lum = .05f),
            4 to Band(hue = -.05f, sat = -.03f, lum = .13f), 5 to Band(hue = -.09f, sat = -.14f, lum = .08f),
            6 to Band(hue = .13f, sat = .13f), 7 to Band(hue = .01f)
        )
    ),
    house("curva-chiaroscuro", "Curva chiaroscuro",
        tone = Tone(all = curve(
            0f to .0706f, .149f to .1451f, .251f to .2196f,
            .502f to .502f, .8824f to .8706f, 1f to .9647f
        ))
    ),
    house("curva-onde", "Curva onde",
        tone = Tone(all = curve(
            0f to .0706f, .1569f to .1882f, .2863f to .2667f,
            .498f to .5059f, .5608f to .5451f, .8824f to .8706f,
            1f to .9647f
        ))
    ),
    house("pellicola", "Curva pellicola",
        light = Light(contrast = .1f, whites = -.1f, blacks = .25f),
        chroma = Chroma(saturation = -.15f, vibrance = .12f),
        tone = Tone(all = curve(
            0f to .055f, .25f to .22f, .75f to .79f,
            1f to .96f
        ))
    ),
    house("dettagli-fini", "Dettagli fini",
        detail = Detail(sharpen = .7f, masking = .7f)
    ),
    house("dettagli-grossi", "Dettagli grossolani",
        detail = Detail(sharpen = .85f, radius = .6781f, masking = .75f, noise = .2f, noiseColor = .1f)
    ),
    house("mix-turchese", "Mix colori turchese",
        mix = mix(
            0 to Band(hue = .02f), 1 to Band(hue = -.03f),
            2 to Band(hue = -.33f), 3 to Band(hue = .02f),
            4 to Band(hue = -.23f), 5 to Band(hue = -.4f),
            6 to Band(hue = -.13f)
        )
    ),
    house("notturno", "Notturno",
        light = Light(exposure = .25f, highlights = -.2f, shadows = .45f, blacks = .15f),
        detail = Detail(sharpen = .3f, masking = .6f, noise = .55f, noiseColor = .45f)
    ),
    house("primari-caldi", "Primari caldi",
        light = Light(contrast = .08f),
        mix = mix(
            0 to Band(hue = .28f, sat = -.5f), 1 to Band(hue = .15f, sat = -.2f),
            3 to Band(sat = .45f), 5 to Band(hue = -.3f, sat = .08f)
        )
    ),
    house("primari-verdi", "Primari verdi",
        light = Light(contrast = .08f),
        mix = mix(
            0 to Band(hue = .28f, sat = -.5f), 1 to Band(hue = .15f, sat = -.2f),
            2 to Band(hue = .4f, sat = .3f), 3 to Band(hue = .75f, sat = 1f),
            4 to Band(sat = .2f), 5 to Band(sat = .4f)
        )
    ),
    house("ritratto", "Ritratto",
        light = Light(highlights = -.25f, shadows = .2f, blacks = .08f),
        chroma = Chroma(temp = .05f, vibrance = .2f),
        mix = mix(
            0 to Band(sat = -.08f), 1 to Band(hue = .05f, sat = -.12f, lum = .12f),
            2 to Band(sat = -.1f)
        ),
        detail = Detail(sharpen = .35f, masking = .8f)
    ),
    house("rosso-1", "Rosso -",
        mix = mix(
            0 to Band(sat = .1f, lum = .07f), 1 to Band(hue = -.12f, sat = .2f, lum = -.18f),
            2 to Band(hue = -.11f, sat = .03f, lum = .05f), 3 to Band(sat = .08f, lum = .05f),
            4 to Band(hue = -.05f, sat = -.09f, lum = .1f), 5 to Band(hue = -.09f, sat = -.25f, lum = .2f),
            6 to Band(hue = .04f), 7 to Band(hue = .01f)
        )
    ),
    house("rosso-2", "Rosso - -",
        mix = mix(
            0 to Band(hue = -.06f, lum = -.35f), 1 to Band(hue = -.14f, lum = -.2f),
            2 to Band(hue = -.11f, sat = .03f, lum = .05f), 3 to Band(sat = .08f, lum = .05f),
            4 to Band(hue = -.05f, sat = -.09f, lum = .1f), 5 to Band(hue = -.09f, sat = -.25f, lum = .2f),
            6 to Band(hue = .04f), 7 to Band(hue = .01f)
        )
    ),
    house("to-blu-rosso", "T&O - Blu/Rosso",
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
    house("to-chiaro", "T&O - Chiaro",
        mix = mix(
            0 to Band(hue = .38f, sat = -.06f, lum = -.21f), 1 to Band(sat = -.17f),
            2 to Band(hue = -.75f, sat = .18f, lum = .19f), 3 to Band(hue = -.78f, sat = -.08f),
            4 to Band(hue = -.38f, sat = -.08f, lum = .22f), 5 to Band(hue = -.22f, sat = .26f, lum = .39f),
            6 to Band(hue = .28f, sat = -.63f), 7 to Band(hue = .64f, sat = -.62f)
        )
    ),
    house("to-neutro", "T&O - Neutro",
        mix = mix(
            0 to Band(sat = .05f), 1 to Band(hue = .07f, sat = -.49f, lum = .05f),
            2 to Band(hue = .2f, sat = .06f, lum = .05f), 3 to Band(hue = .06f, sat = .35f, lum = .05f),
            4 to Band(hue = .42f, sat = .1f, lum = -.05f), 5 to Band(sat = .25f)
        )
    ),
    house("to-slavato", "T&O - Slavato",
        mix = mix(
            0 to Band(hue = .36f, sat = -.13f), 1 to Band(sat = -.15f),
            2 to Band(hue = -.44f, sat = -.1f), 3 to Band(hue = 1f),
            4 to Band(hue = .1f, sat = -.11f, lum = -.32f), 5 to Band(hue = -.17f, sat = .19f, lum = -.23f),
            6 to Band(hue = -.1f, sat = -.63f), 7 to Band(hue = -.13f, sat = -.62f)
        )
    ),
    house("to-standard", "T&O - Standard",
        mix = mix(
            0 to Band(hue = .36f, sat = .05f), 1 to Band(hue = -.06f, sat = .49f, lum = .05f),
            2 to Band(hue = .03f, sat = .28f, lum = .05f), 3 to Band(hue = .4f, sat = .35f, lum = .05f),
            4 to Band(hue = -.12f, sat = .43f, lum = -.05f), 5 to Band(hue = -.32f, sat = .62f)
        )
    )
)
