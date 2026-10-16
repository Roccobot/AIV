package io.github.roccobot.aiv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Uno **stile**: un nome e i sei moduli che lo stile governa (Luce, Colore, HSL, Dettaglio, Curve
 * ed Effetti, come dice `docs/Editor.md`).
 *
 * ⚠️ **Uno stile non contiene la posa, il ritaglio e la geometria**: dipendono da come è stata
 * scattata quell'immagine (dov'è il cielo, dove finisce il soggetto, quanto pende l'orizzonte),
 * mentre uno stile passa da un'immagine all'altra. Uno che raddrizza di tre gradi girerebbe anche
 * le immagini dritte.
 *
 * Il nome non si traduce, nemmeno quello dei dieci di casa: è un nome proprio, come quello di una
 * cartella.
 */
data class Preset(
    val name: String,
    val look: Look,
    /**
     * Con che cosa questo stile si nomina nell'archivio.
     *
     * ⚠️ **Il nome si può cambiare, quindi non è l'identità** (richiesta dell'utente, giro della
     * 2.40: anche quelli di casa si riordinano, si rinominano e si cancellano): l'archivio tiene
     * rinomine, cancellati e ordine per chiave.
     * Per gli stili propri la chiave è il nome: due stili propri con lo stesso nome sarebbero
     * indistinguibili nell'elenco, e per questo salvarne uno col nome di un altro lo sostituisce.
     */
    val key: String = name,
    /**
     * Se è uno dei dieci che l'app ha di fabbrica.
     *
     * Serve al gruppo in cui compare ('Stili AIV' o 'Stili salvati') e a dove l'archivio scrive
     * quello che è cambiato (una rinomina fra le rinomine, una cancellazione fra i cancellati,
     * mai lo stile intero).
     */
    val house: Boolean = false
) {

    /**
     * [base] con sopra questo stile: i sei moduli si sostituiscono, tutto il resto resta. È il
     * tocco; il tocco lungo somma ([addTo]).
     *
     * Sostituire rende lo stile prevedibile: dice dove si arriva, e due tocchi danno la stessa
     * immagine. Per tornare indietro c'è 'Annulla', perché il risultato è un [Look] come un altro.
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
     * [base] con sopra questo stile **sommato**: il tocco lungo, che serve a comporre più stili
     * (richiesta dell'utente, giro della 2.40; somma cursore per cursore, risposta `B1` alla voce
     * `4.96-01`).
     * - I cursori principali si sommano, entro la loro corsa: lo stesso stile toccato due volte
     *   conta due volte, e un cursore a riposo somma zero.
     * - ⚠️ **I cursori secondari prendono il valore dello stile, se lo stile lo nomina** (filtro del
     *   bianco e nero, raggio e maschera della nitidezza, dimensione e luci della grana, sfumatura
     *   della vignettatura): sono la forma di un altro cursore, e la somma di due forme non è una
     *   forma. Lettura della sessione, dichiarata nel DF.
     * - Il bianco e nero vale se lo è uno dei due: spegnerlo per somma sarebbe una cancellazione.
     * - Le curve si applicano una dopo l'altra, quella dello stile sopra quella che c'è ([then]).
     */
    fun addTo(base: Look): Look = base.copy(
        light = base.light.let { a ->
            val b = look.light
            a.copy(
                exposure = plus(a.exposure, b.exposure, -Light.EXPOSURE_RANGE, Light.EXPOSURE_RANGE),
                contrast = plus(a.contrast, b.contrast),
                highlights = plus(a.highlights, b.highlights),
                shadows = plus(a.shadows, b.shadows),
                whites = plus(a.whites, b.whites),
                blacks = plus(a.blacks, b.blacks)
            )
        },
        chroma = base.chroma.let { a ->
            val b = look.chroma
            a.copy(
                temp = plus(a.temp, b.temp),
                tint = plus(a.tint, b.tint),
                saturation = plus(a.saturation, b.saturation),
                vibrance = plus(a.vibrance, b.vibrance),
                mono = a.mono || b.mono,
                filter = over(a.filter, b.filter)
            )
        },
        mix = Mix(base.mix.bands.zip(look.mix.bands) { a, b ->
            Band(hue = plus(a.hue, b.hue), sat = plus(a.sat, b.sat), lum = plus(a.lum, b.lum))
        }),
        detail = base.detail.let { a ->
            val b = look.detail
            a.copy(
                sharpen = plus(a.sharpen, b.sharpen, 0f),
                radius = over(a.radius, b.radius),
                masking = over(a.masking, b.masking),
                noise = plus(a.noise, b.noise, 0f),
                noiseColor = plus(a.noiseColor, b.noiseColor, 0f)
            )
        },
        effects = base.effects.let { a ->
            val b = look.effects
            a.copy(
                haze = plus(a.haze, b.haze),
                vignette = plus(a.vignette, b.vignette),
                vignetteFeather = over(a.vignetteFeather, b.vignetteFeather),
                grain = plus(a.grain, b.grain, 0f),
                grainSize = over(a.grainSize, b.grainSize),
                grainLift = over(a.grainLift, b.grainLift)
            )
        },
        tone = base.tone.copy(
            all = then(base.tone.all, look.tone.all),
            red = then(base.tone.red, look.tone.red),
            green = then(base.tone.green, look.tone.green),
            blue = then(base.tone.blue, look.tone.blue)
        )
    )

    companion object {

        /** Lo stile [name] con i sei moduli di [look] e nient'altro. */
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
 * Dove vivono gli stili, e che cosa di loro l'utente ha cambiato.
 *
 * L'archivio è un file e non una preferenza: è un elenco di oggetti annidati (otto fasce e
 * quattro curve per ognuno), che in un `DataStore` sarebbe una stringa lunghissima sotto una
 * chiave sola. È in `filesDir`, come le copertine: da `cacheDir` il sistema può cancellare.
 *
 * ⚠️ **Si legge e si scrive a mano con `org.json`**: è nella piattaforma, e il file si apre con
 * qualunque editor. Una serializzazione automatica legherebbe la forma del file a quella delle
 * classi. Ogni campo che manca vale il suo valore di riposo, e questo rende il formato compatibile
 * in avanti e all'indietro.
 *
 * Il file è un oggetto con `mine` (gli stili propri, nell'ordine scelto), `names` (le rinomine di
 * quelli di casa, per chiave), `gone` (le chiavi di casa cancellate) e `house` (l'ordine di quelli
 * di casa). ⚠️ Un file che comincia con `[` è il formato di prima della 2.50, cioè il solo elenco
 * degli stili propri, e si legge ancora: senza, un aggiornamento cancellerebbe gli stili salvati.
 * ⚠️ **Di uno stile di casa non si scrive mai il contenuto**, nemmeno rinominato: i valori vivono
 * nel programma, così una taratura corretta in una versione futura arriva anche a chi lo ha
 * rinominato.
 */
object Presets {

    /** Quello che l'elenco mostra: prima i dieci di casa, poi i propri. */
    fun all(context: Context): List<Preset> = house(context) + mine(context)

    /**
     * Gli stili di casa, nell'ordine scelto, coi nomi effettivi e senza i cancellati.
     *
     * L'ordine si applica alla lista del programma: una chiave salvata che non esiste più si
     * scarta, e una che l'archivio non nomina resta al suo posto di fabbrica (come `padOrderOf`
     * per i riquadri).
     */
    fun house(context: Context): List<Preset> {
        val book = read(context)
        val vivi = HOUSE.filterNot { book.gone.contains(it.key) }
        val posto = book.house.withIndex().associate { (i, k) -> k to i }
        return vivi
            .sortedBy { posto[it.key] ?: (book.house.size + vivi.indexOf(it)) }
            .map { p -> book.names[p.key]?.let { p.copy(name = it) } ?: p }
    }

    /** I soli stili salvati dall'utente, nell'ordine scelto. */
    fun mine(context: Context): List<Preset> = read(context).mine

    /**
     * Salva [look] col nome [name], e ridà l'elenco nuovo dei propri.
     *
     * Un nome già usato sostituisce, senza guardare le maiuscole: due stili con lo stesso nome
     * sarebbero indistinguibili. Un nome nuovo va in coda: l'ordine è una scelta dell'utente.
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
     * ⚠️ **Uno stile di casa si nasconde**, perché i suoi valori vivono nel programma: si scrive
     * la sua chiave fra i cancellati, e 'Ripristina' cancella quell'elenco con tutto il resto.
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
     * ⚠️ **Per uno stile proprio il nome è anche la chiave**: se il nome nuovo è già di un altro
     * stile proprio i due si fonderebbero, quindi la rinomina si scarta. A sostituire è solo il
     * salvataggio, dove l'utente ha appena scritto valori nuovi.
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

    /** Sposta lo stile da [from] all'indice [to], dentro il suo gruppo. */
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
     * Cancella tutto quello che l'utente ha fatto: l'app torna ai dieci di casa, nell'ordine e
     * coi nomi di fabbrica. Si cancella il file: l'assenza è già il valore di fabbrica.
     */
    fun reset(context: Context) {
        runCatching { store(context).delete() }
    }

    /**
     * L'archivio come testo, per chi lo vuole salvare fuori dall'app.
     *
     * Si esporta l'archivio intero: dopo un'installazione nuova si vogliono ritrovare anche le
     * rinomine e l'ordine, non solo gli stili creati (lettura della richiesta dell'utente).
     * È lo stesso testo del disco: nessun secondo formato da tenere allineato.
     */
    fun export(context: Context): String = book(read(context)).toString(2)

    /**
     * Uno stile solo come testo, per il file `.aivstyle`.
     *
     * Risposta `dopo` a `d-stile-singolo`: un comando sulla riga di uno stile, e un'importazione
     * che lo aggiunge agli altri. Il testo è l'oggetto che l'archivio scrive per ogni stile
     * ([writeLook]), senza un secondo formato.
     * Uno stile di casa esce col nome che ha adesso e arriva come stile proprio: il file non può
     * promettere che l'altro telefono abbia la stessa versione dell'app.
     */
    fun exportOne(p: Preset): String = writeLook(p).toString(2)

    /** Che cosa ha rimesso [load]: l'archivio intero, o uno stile solo. */
    enum class Loaded { BOOK, ONE }

    /**
     * Rimette un archivio esportato, o aggiunge uno stile solo, e dice quale dei due ha fatto;
     * `null` se il testo non si legge.
     *
     * Lo stile solo si aggiunge (risposta `dopo` a `d-stile-singolo`): passa da [save], quindi un
     * nome già usato ne prende i valori, e uno nuovo va in coda. Lo riconosce [one], prima di
     * [parse].
     * L'archivio intero sostituisce quello che c'è: è un'istantanea dell'elenco, e mescolarlo
     * darebbe una terza cosa che nessuno dei due descrive. Chi vuole tenere tutti e due esporta
     * prima.
     * ⚠️ **Un testo che non si legge non tocca niente**: la scrittura arriva dopo la lettura.
     */
    fun load(context: Context, text: String): Loaded? {
        val uno = one(text)
        if (uno != null) {
            save(context, uno.name, uno.look)
            return Loaded.ONE
        }
        val letto = runCatching { parse(text) }.getOrNull() ?: return null
        write(context, letto)
        return Loaded.BOOK
    }

    /**
     * Lo stile che [text] contiene, se è il file di uno stile solo.
     *
     * Lo si riconosce da `name` senza `mine`: un archivio scrive sempre `mine`, anche vuoto
     * ([book]), e uno stile ha sempre un nome. Con un nome vuoto il testo passa a [parse], che lo
     * rifiuta.
     */
    private fun one(text: String): Preset? = runCatching {
        val o = JSONObject(text.trim())
        if (!o.has(NAME) || o.has(MINE)) return@runCatching null
        val nome = o.optString(NAME).trim().take(Preset.NAME_MAX)
        if (nome.isEmpty()) null else Preset.of(nome, readLook(o))
    }.getOrNull()

    /**
     * Aggiunge a quello che c'è un archivio esportato, e dice se ci è riuscito: per il file di
     * impostazioni.
     *
     * Risposta `fonde` a `d-backup-importa`: gli stili del file si aggiungono, e dove si
     * sovrappongono vince il file. Vale per il file di impostazioni; l'importazione della pagina
     * degli stili è [load], che sostituisce.
     * - Gli stili salvati: uno con lo stesso nome (senza guardare le maiuscole, come in [save])
     *   prende i valori del file e resta al suo posto; gli altri vanno in coda nell'ordine del file.
     * - ⚠️ **Di quelli di casa l'archivio tiene solo i cambiamenti dalla fabbrica** (un nome, un
     *   nascosto, un ordine): si sommano quelli di qui e quelli del file, e sulla stessa cosa vince
     *   il file. Un nome o un ordine che il file non scrive non vale sopra uno di qui; uno stile
     *   nascosto da una delle due parti resta nascosto.
     * - Su un telefono nuovo equivale a [load].
     * Un testo che non si legge non tocca niente, come in [load]; in più dice se la scrittura è
     * riuscita, perché il backup conta le aree entrate.
     */
    fun merge(context: Context, text: String): Boolean {
        val arrivo = runCatching { parse(text) }.getOrNull() ?: return false
        val qui = read(context)
        val mine = qui.mine.toMutableList()
        arrivo.mine.forEach { p ->
            val dove = mine.indexOfFirst { it.name.equals(p.name, ignoreCase = true) }
            if (dove >= 0) mine[dove] = p else mine.add(p)
        }
        val fuso = Book(
            mine = mine,
            names = qui.names + arrivo.names,
            gone = (qui.gone + arrivo.gone).distinct(),
            house = arrivo.house.ifEmpty { qui.house }
        )
        return write(context, fuso) === fuso
    }

    /**
     * Se [text] è un archivio che [load] saprebbe leggere, senza scrivere niente.
     *
     * Serve al backup, che prima controlla tutto e poi scrive tutto: con la sola [load], un
     * archivio storto si scoprirebbe con le altre parti già rimesse.
     */
    internal fun readable(text: String): Boolean = runCatching { parse(text) }.isSuccess

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
     * La forma si riconosce dal primo carattere: un `[` è l'elenco di prima, il resto passa da
     * `JSONObject`, che su un testo storto va in errore, e l'errore lo raccoglie chi chiama.
     */
    private fun parse(text: String): Book {
        val pulito = text.trim()
        if (pulito.startsWith("[")) return Book(mine = readMine(JSONArray(pulito)))
        val o = JSONObject(pulito)
        /*
         * ⚠️ **Il file di uno stile solo non è un archivio vuoto**: letto così, [load] lo
         * scriverebbe al posto di quello che c'è e cancellerebbe tutti gli stili propri. Vale
         * anche per il file di impostazioni, che chiede [readable].
         */
        require(!(o.has(NAME) && !o.has(MINE))) { "one style, not a book" }
        val names = mutableMapOf<String, String>()
        o.optJSONObject("names")?.let { n ->
            n.keys().forEach { k -> n.optString(k).takeIf { it.isNotEmpty() }?.let { names[k] = it } }
        }
        return Book(
            mine = readMine(o.optJSONArray(MINE) ?: JSONArray()),
            names = names,
            gone = strings(o.optJSONArray("gone")),
            house = strings(o.optJSONArray("house"))
        )
    }

    private fun book(b: Book): JSONObject {
        val o = JSONObject()
        o.put(MINE, JSONArray().apply { b.mine.forEach { put(writeLook(it)) } })
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
     * L'ordine degli stili di casa, completato con quelli che l'archivio non nomina.
     *
     * Serve prima di uno spostamento: un archivio può non avere ancora nessun ordine, e una riga
     * non si sposta dentro una lista vuota.
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
        val nome = o.optString(NAME).trim()
        if (nome.isEmpty()) null else Preset(nome, readLook(o))
    }

    private fun store(context: Context): File = File(context.filesDir, STORE)

    // ── Andata e ritorno ─────────────────────────────────────────────────────
    //
    // ⚠️ **Un campo a riposo non si scrive**: uno stile di sola Luce deve leggersi come tale, e
    // con i moduli scritti per intero il file direbbe che tocca anche il colore e le curve, a zero.

    private fun writeLook(p: Preset): JSONObject {
        val o = JSONObject()
        o.put(NAME, p.name)
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
        if (v > DIAL_REST || v < -DIAL_REST) put(key, v.toDouble())
    }

    private fun JSONObject.num(key: String): Float = optDouble(key, 0.0).toFloat()

    /**
     * Scrive una curva come un elenco piatto di numeri: `at, to, at, to, ...`.
     *
     * Piatto: un punto è sempre due numeri, e le coppie annidate non direbbero niente di più.
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

    /** Il nome del file degli stili, dentro `filesDir`. */
    private const val STORE = "presets.json"

    /** Il campo del nome di uno stile, che solo il file di uno stile solo ha in cima. */
    private const val NAME = "name"

    /** Il campo degli stili propri, che un archivio scrive sempre, anche vuoto. */
    private const val MINE = "mine"
}

// ── La somma del tocco lungo ─────────────────────────────────────────────────

/** Un cursore principale sommato, dentro la sua corsa: vedi [Preset.addTo]. */
private fun plus(a: Float, b: Float, low: Float = -1f, high: Float = 1f): Float = (a + b).coerceIn(low, high)

/** Un cursore secondario: quello dello stile se lo nomina, altrimenti quello che c'è. */
private fun over(a: Float, b: Float): Float = if (b != 0f) b else a

/**
 * La curva [second] applicata dopo [first]: il tocco lungo con le curve.
 *
 * Se una delle due è a riposo il risultato è l'altra, esatta, ed è il caso di quasi tutti gli
 * stili. Con tutte e due, la curva composta si campiona nei nodi delle due e su una griglia di
 * nove punti, perché due curve monotone composte non sono una curva della stessa famiglia: è
 * un'approssimazione, entro il tetto di [Curve.MAX_KNOTS].
 */
internal fun then(first: Curve, second: Curve): Curve {
    if (second.idle) return first
    if (first.idle) return second
    val griglia = (0..COMPOSE_GRID).map { it / COMPOSE_GRID.toFloat() }
    val tutti = (griglia + first.knots.map { it.at } + second.knots.map { it.at }).sorted()
    val punti = mutableListOf<Float>()
    tutti.forEach { x -> if (punti.isEmpty() || x - punti.last() >= COMPOSE_GAP) punti.add(x) }
    if (punti.last() < 1f) punti[punti.size - 1] = 1f
    val scelti = if (punti.size <= Curve.MAX_KNOTS) punti else griglia
    return Curve(scelti.map { x -> Knot(x, second.valueAt(first.valueAt(x))) })
}

/** I tratti della griglia su cui si campiona una composizione di curve: nove punti. */
private const val COMPOSE_GRID = 8

/** Due nodi più vicini di così, nella composizione, sono uno solo. */
private const val COMPOSE_GAP = 1f / 32f

// ── I dieci di casa ──────────────────────────────────────────────────────────

/**
 * Le sole fasce nominate: quelle che non compaiono sono a riposo.
 */
private fun mix(vararg bands: Pair<Int, Band>): Mix =
    Mix(List(Mix.COUNT) { i -> bands.firstOrNull { it.first == i }?.second ?: Band.NONE })

/** Una curva dai suoi punti, scritti come `x to y`. */
private fun curve(vararg knots: Pair<Float, Float>): Curve =
    Curve(knots.map { Knot(it.first, it.second) })

/** Uno stile di casa, con la sua chiave e i suoi sei moduli facoltativi. */
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
 * I dieci stili di fabbrica, scritti dalla sessione (risposta `A1` alla voce `4.96-01`: dieci nomi
 * fra cui 'Roccobot', onnicomprensivo, senza 'Bianco e nero', pensati per sommarsi).
 *
 * ⚠️ **Sono pensati per sommarsi col tocco lungo** ([Preset.addTo]): ognuno tranne 'Roccobot'
 * lavora su un asse solo, con valori moderati, così due o tre insieme restano dentro le corse e
 * non si cancellano. 'Caldo' e 'Freddo' sono l'eccezione voluta: uno disfa l'altro.
 * - 'Roccobot' tocca tutti e sei i moduli: luce aperta nelle ombre e trattenuta nelle luci, colore
 *   vivace senza forzare la saturazione, cieli più profondi e pelle più chiara nell'HSL, nitidezza
 *   con la maschera, un filo di foschia tolta, di grana e di vignettatura, una curva a S leggera.
 * - ⚠️ **Nessuno usa i cursori secondari**: col tocco lungo un secondario prende il valore dello
 *   stile, e sovrascriverebbe la forma scelta da un altro.
 * - Le curve sono tre ('Roccobot', 'Contrasto', 'Pellicola'), e sommate si applicano una dopo
 *   l'altra ([then]).
 *
 * L'ordine è dell'utente (giro della 2.40): 'Roccobot' primo, gli altri in ordine alfabetico, cioè
 * l'ordine in cui la lista è scritta.
 * - ⚠️ **L'ordine si scrive e lo presidia il banco**: un `sortedBy` dipenderebbe da come la
 *   piattaforma confronta due stringhe (maiuscole, accenti, lingua del telefono).
 * - ⚠️ **La chiave non segue il nome**: nell'archivio di chi aggiorna vivono le chiavi (rinomine,
 *   nascosti, ordine). 'Roccobot' tiene `combo`; le chiavi degli stili di casa tolti restano
 *   nell'archivio senza effetto, perché l'elenco applica solo quelle che esistono
 *   ([Presets.house]).
 */
val HOUSE: List<Preset> = listOf(
    house("combo", "Roccobot",
        light = Light(contrast = .08f, highlights = -.2f, shadows = .15f, whites = .05f, blacks = -.05f),
        chroma = Chroma(saturation = .03f, vibrance = .15f),
        mix = mix(
            1 to Band(sat = -.05f, lum = .05f), 4 to Band(sat = .05f, lum = -.05f),
            5 to Band(sat = .08f, lum = -.08f)
        ),
        detail = Detail(sharpen = .35f, noise = .08f),
        effects = Effects(haze = .04f, vignette = .06f, grain = .03f),
        tone = Tone(all = curve(0f to 0f, .25f to .235f, .75f to .765f, 1f to 1f))
    ),
    house("caldo", "Caldo",
        chroma = Chroma(temp = .12f, tint = .02f)
    ),
    house("cieli", "Cieli profondi",
        light = Light(highlights = -.1f),
        mix = mix(4 to Band(sat = .1f, lum = -.1f), 5 to Band(sat = .2f, lum = -.2f))
    ),
    house("contrasto", "Contrasto",
        light = Light(contrast = .15f, whites = .05f, blacks = -.08f),
        tone = Tone(all = curve(0f to 0f, .25f to .22f, .75f to .78f, 1f to 1f))
    ),
    house("freddo", "Freddo",
        chroma = Chroma(temp = -.12f, tint = -.02f)
    ),
    house("nitido", "Nitido",
        detail = Detail(sharpen = .45f),
        effects = Effects(haze = .08f)
    ),
    house("ombre", "Ombre aperte",
        light = Light(highlights = -.15f, shadows = .35f, blacks = .12f)
    ),
    house("film", "Pellicola",
        chroma = Chroma(saturation = -.08f),
        effects = Effects(vignette = .12f, grain = .22f),
        tone = Tone(all = curve(0f to .06f, .5f to .5f, 1f to .96f))
    ),
    house("tenue", "Tenue",
        light = Light(contrast = -.1f),
        chroma = Chroma(saturation = -.25f, vibrance = -.1f)
    ),
    house("vivido", "Vivido",
        chroma = Chroma(saturation = .08f, vibrance = .3f)
    )
)
