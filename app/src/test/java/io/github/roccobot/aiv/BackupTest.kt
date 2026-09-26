package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.random.Random

/**
 * Il banco di prova del **backup**, nato nella `2.93` con la funzione.
 *
 * ⚠️⚠️ **NASCE CON LA FUNZIONE E NON DOPO UN DIFETTO**, ed è il caso proattivo di `AIV/CLAUDE.md`
 * § '🧪 Quando si scrive una prova, e quando no': un'importazione riscrive le preferenze, le
 * copertine, il logo e il cestino, e quasi tutto quello che può andare storto va storto in
 * silenzio. Un numero che torna di un altro tipo, una chiave dimenticata dall'elenco, una
 * copertina di troppo che resta, un file tagliato che si legge come intero: nessuno dà un errore,
 * e tutti arrivano a lui su un telefono che ha appena perso le sue impostazioni.
 *
 * ⚠️⚠️ **METÀ DEI CASI SONO LA SUA RICHIESTA SULLE VERSIONI** (2026-09-26: *una versione di AIV
 * più recente ... troverà 'vuote' alcune impostazioni nate dopo ... una versione di AIV più
 * datata ... ignorerà alcune voci di importazione non sapendo come trattarle*). Quei file li
 * scrive [sigilla], cioè il contenitore vero con dentro voci scritte a mano: è il solo modo di
 * avere oggi un backup scritto da una versione che non esiste ancora.
 *
 * ⚠️ **Che cosa NON vede**: il selettore di sistema che sceglie dove salvare, Drive, e quanto
 * tempo un telefono impiega a far nascere la chiave di una password. Quelli si guardano sul
 * telefono, e la voce di collaudo li chiede.
 *
 * ⚠️ **Vuole la grafica vera** (`@GraphicsMode(NATIVE)`): il logo si disegna prima di essere
 * adottato, e con la grafica di serie un PNG non si decodifica.
 * ⚠️ **Non dichiara `OmbraArchivio`**, per la ragione scritta su [CestinoSpazioTest]: il cestino
 * vive in `getExternalFilesDir`, e con quell'ombra quel metodo muore.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupTest {

    private val app: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * ⚠️ **Si parte da un telefono vuoto**: le preferenze, gli stili, le copertine, il logo e il
     * cestino vivono fuori da quello che il banco azzera fra un caso e l'altro, e l'archivio delle
     * preferenze è un oggetto di processo. Senza questa pulizia l'ordine dei casi deciderebbe
     * l'esito.
     * ⚠️ **I due archivi accanto al cestino si cancellano per nome** (`bin.tsv` e
     * `restored.tsv`): `Bin.empty` toglie le sole righe dei file che trova, e una riga rimasta
     * senza file passerebbe al caso dopo.
     */
    @Before
    fun pulito() {
        runBlocking {
            rewritePreferences(app) { it.clear() }
            Presets.reset(app)
            FolderCovers.files(app).forEach { it.delete() }
            Watermark.forget(app)
            Bin.empty(app)
            Backup.sweep(app)
        }
        Bin.dir(app).parentFile?.let { casa ->
            File(casa, "bin.tsv").delete()
            File(casa, "restored.tsv").delete()
        }
    }

    // ── Le preferenze ────────────────────────────────────────────────────────

    /**
     * **Caso 1: ogni preferenza torna com'era, col suo tipo.**
     *
     * ⚠️⚠️ **SI CAMBIANO TUTTE, E NON SOLO ALCUNE**: [storto] sposta ogni valore dal suo stato di
     * fabbrica (i booleani si rovesciano, i numeri crescono, le stringhe e gli insiemi si
     * allungano), quindi un valore che non viaggia si vede sempre. Il numero lungo esce da un
     * intero, perché è il caso in cui un `long` trattato da `int` perde qualcosa.
     * ⚠️ **Le due chiavi vecchie ci sono** (`veil` e `mark-air`): nessuno le scrive più, e un
     * backup che le perdesse rimetterebbe il valore di fabbrica a chi non ha mai salvato da allora.
     * ⚠️ **I promemoria di questo telefono non viaggiano**: all'arrivo il permesso è già stato
     * chiesto, alla partenza no, e dopo l'importazione deve restare chiesto.
     * ⚠️ **Controprovata** leggendo i numeri decimali come interi: la misura dello zoom non si legge
     * più e si salta, e la prova cade già sul resoconto, che dice di aver saltato qualcosa.
     */
    @Test
    fun `ogni preferenza torna com'era, col suo tipo`() {
        runBlocking {
            scriviTutto()
            rewritePreferences(app) { storto(it) }
            FolderAsk.forget(app)
        }
        val partenza = archivio()
        assertEquals(
            "la partenza deve portare tutte le chiavi del backup",
            PREF_KEYS.map { it.name }.toSet(),
            partenza.keys
        )
        val file = esporta(PREF_AREAS)

        runBlocking {
            rewritePreferences(app) { it.clear() }
            SettingsStore.save(app, Settings())
            FolderAsk.remember(app)
        }
        val esito = importa(file, PREF_AREAS)

        assertEquals(PREF_AREAS, esito.applied)
        assertFalse("un file di questa versione non salta niente", esito.skipped)
        assertEquals(partenza, archivio())
        assertTrue(
            "il promemoria del permesso resta quello di questo telefono",
            runBlocking { FolderAsk.flow(app).first() }
        )
    }

    /**
     * **Caso 2: ogni chiave che l'app scrive ha un'area, o una ragione per restare fuori.**
     *
     * ⚠️⚠️ **È LA PROVA CHE TIENE ONESTO UN ELENCO SCRITTO A MANO** (vedi [PREF_KEYS]): una chiave
     * nuova che qualcuno dimenticasse di aggiungere resterebbe fuori da ogni backup, e nessuno lo
     * vedrebbe fino al giorno in cui serve. Qui l'archivio si riempie con tutti i suoi scrittori, e
     * ogni chiave trovata deve essere nell'elenco col tipo giusto, o fra quelle di [PREF_OUTSIDE].
     * ⚠️ **E il rovescio**: ogni chiave dell'elenco deve essere scritta davvero da qualcuno, tranne
     * le due vecchie. Una chiave dell'elenco che nessuno scrive è un nome sbagliato, cioè un valore
     * che non viaggia.
     * ⚠️ **Controprovata** togliendo `gpu-thumbs` dall'elenco: la prova la nomina e cade.
     */
    @Test
    fun `ogni chiave dell'archivio ha un'area o una ragione`() {
        runBlocking { scriviTutto() }
        val scritte = runBlocking { storedPreferences(app) }.asMap().entries
            .associate { it.key.name to it.value }

        for ((nome, valore) in scritte) {
            if (nome in PREF_OUTSIDE) continue
            val chiave = PREF_KEYS.firstOrNull { it.name == nome }
            assertNotNull("la chiave '$nome' non ha un'area e non è fra quelle fuori", chiave)
            assertEquals("il tipo di '$nome'", chiave!!.type, PrefType.of(valore))
        }
        val vecchie = setOf("veil", "mark-air")
        val mai = PREF_KEYS.map { it.name }.filter { it !in scritte && it !in vecchie }
        assertTrue("chiavi dell'elenco che nessuno scrive: $mai", mai.isEmpty())
        assertEquals(
            "un nome compare una volta sola",
            PREF_KEYS.size,
            PREF_KEYS.map { it.name }.toSet().size
        )
        assertTrue(
            "una chiave non puo essere dentro e fuori",
            PREF_KEYS.none { it.name in PREF_OUTSIDE }
        )
    }

    /**
     * **Caso 3: le caselle scelgono che cosa si rimette.**
     *
     * Il file porta tutte le aree, e se ne importa una sola: le altre restano quelle del telefono
     * d'arrivo, anche se il file ne porta un'altra versione.
     * ⚠️ **Controprovata** importando tutte le aree del resoconto senza guardare le caselle: il
     * lato del FAB non resta quello d'arrivo, e la prova cade.
     */
    @Test
    fun `le caselle scelgono che cosa si rimette`() {
        runBlocking {
            SettingsStore.save(app, Settings(hand = Hand.LEFT, fitGrow = true))
        }
        val file = esporta(BackupArea.entries.toSet())
        runBlocking {
            SettingsStore.save(app, Settings(hand = Hand.RIGHT, fitGrow = false))
        }

        val esito = importa(file, setOf(BackupArea.VIEW))

        assertEquals(setOf(BackupArea.VIEW), esito.applied)
        val dopo = archivio()
        assertEquals("l'aspetto viene dal file", true, dopo["fit-grow"])
        assertEquals("il lato dei tasti resta quello d'arrivo", Hand.RIGHT.token, dopo["hand"])
    }

    /**
     * **Caso 4: una preferenza di fabbrica alla partenza torna di fabbrica all'arrivo.**
     *
     * ⚠️⚠️ **È LA METÀ DELLA SUA RICHIESTA CHE NON SI VEDE**: sul telefono di partenza la misura
     * massima dello zoom non è mai stata toccata, quindi nell'archivio non c'è; su quello d'arrivo
     * sì. Il file la dichiara assente, e l'importazione la toglie: senza quell'elenco, per chi
     * importa sarebbe identica a una preferenza nata dopo il backup, che invece non si tocca
     * (caso 5).
     * ⚠️ **Controprovata** togliendo la riga che rimuove le assenti: la misura d'arrivo resta, e la
     * prova cade.
     */
    @Test
    fun `una preferenza di fabbrica alla partenza torna di fabbrica`() {
        runBlocking {
            rewritePreferences(app) { it[booleanPreferencesKey("fit-grow")] = true }
        }
        val file = esporta(setOf(BackupArea.VIEW))
        runBlocking {
            rewritePreferences(app) { it[floatPreferencesKey("zoom-max")] = 7f }
        }

        importa(file, setOf(BackupArea.VIEW))

        val dopo = archivio()
        assertEquals(true, dopo["fit-grow"])
        assertNull("la misura dello zoom torna quella di fabbrica", dopo["zoom-max"])
    }

    /**
     * **Caso 5: una preferenza che il file non nomina resta com'era.**
     *
     * ⚠️⚠️ **È IL FILE DI UNA VERSIONE PIÙ VECCHIA**: conosceva una sola chiave di quell'area, e
     * delle altre non dice niente, né valore né assenza. Quelle sono nate dopo il backup, quindi
     * all'arrivo *resteranno default o ai valori impostati dall'utente*, che è la sua frase.
     * ⚠️ **Il file non porta nemmeno l'elenco delle assenti**, e non è una scorciatoia: un elenco
     * che manca vale vuoto, e anche quel ramo va misurato.
     * ⚠️ **Controprovata** rimettendo la regola di prima, che toglieva tutte le chiavi dell'area
     * prima di scrivere: la misura dello zoom sparisce, e la prova cade.
     */
    @Test
    fun `una preferenza che il file non nomina resta com'era`() {
        val file = sigilla { zip ->
            testo(zip, "manifest.json", resoconto("view"))
            testo(zip, "prefs/view.json", preferenze(chiave("fit-grow", "boolean", false)))
        }
        runBlocking {
            rewritePreferences(app) {
                it[booleanPreferencesKey("fit-grow")] = true
                it[floatPreferencesKey("zoom-max")] = 7f
            }
        }

        val esito = importa(file, setOf(BackupArea.VIEW))

        assertEquals(setOf(BackupArea.VIEW), esito.applied)
        assertFalse("un file di una versione vecchia non salta niente", esito.skipped)
        val dopo = archivio()
        assertEquals(false, dopo["fit-grow"])
        assertEquals("quello che il file non nomina resta", 7f, dopo["zoom-max"])
    }

    /**
     * **Caso 6: un file di una versione più nuova si importa per quello che si conosce.**
     *
     * ⚠️⚠️ **È L'ALTRA METÀ DELLA SUA RICHIESTA** (*ignorerà alcune voci di importazione non
     * sapendo come trattarle, ma teoricamente potrà importare tutto ciò che è importabile*). Il
     * file porta tutto quello che una versione futura potrebbe scrivere: un'area nuova, una voce
     * nuova, un campo `version` nel resoconto, una chiave nuova, una chiave nota con un tipo nuovo,
     * un valore che non si legge, una chiave di un'altra area, un logo e una copertina in un
     * formato nuovo.
     * ⚠️⚠️ **E QUELLO CHE NON SI SA LEGGERE NON CANCELLA NIENTE**: il logo di adesso resta, e resta
     * la copertina della cartella di cui il file porta un formato sconosciuto. La copertina di
     * un'altra cartella invece se ne va, perché le copertine si sostituiscono.
     * ⚠️ **Le controprove sono cinque**, una per regola: un'area sconosciuta che rifiuta il file,
     * il campo `version` guardato, una chiave sconosciuta che rifiuta il file, un logo sconosciuto
     * che cancella quello di adesso, e le copertine sostituite senza guardare le cartelle tenute.
     * Ognuna fa cadere la prova.
     */
    @Test
    fun `un file di una versione piu nuova si importa per quello che si conosce`() {
        val file = sigilla { zip ->
            testo(zip, "manifest.json", resoconto("view", "editor", "covers", "sogni") {
                it.put("version", 7).put("novita", JSONObject().put("x", 1))
            })
            testo(
                zip, "prefs/view.json",
                preferenze(
                    chiave("fit-grow", "boolean", true),
                    chiave("manopola-futura", "boolean", true),
                    chiave("zoom-max", "double", "3.5"),
                    chiave("folder-columns", "int", "tre"),
                    chiave("hand", "string", Hand.LEFT.token),
                    assenti = listOf("bg-type", "assente-futura")
                )
            )
            testo(zip, "prefs/editor.json", preferenze(chiave("mark-on", "boolean", true)))
            byte(zip, "watermark/mark.avif", ByteArray(64) { 7 })
            byte(zip, "covers/123-500.avif", ByteArray(64) { 9 })
            byte(zip, "sogni/sogno.bin", ByteArray(16))
        }
        runBlocking {
            rewritePreferences(app) {
                it[floatPreferencesKey("zoom-max")] = 5f
                it[intPreferencesKey("folder-columns")] = 4
                it[stringPreferencesKey("bg-type")] = BgType.SOLID.token
                it[stringPreferencesKey("hand")] = Hand.RIGHT.token
            }
            assertTrue(Watermark.adopt(app, offri("logo.png", png())))
        }
        val logo = Watermark.file(app)!!.readBytes()
        copertina("123-100.webp", "tenuta")
        copertina("999-100.webp", "sostituita")

        val esito = importa(file, BackupArea.entries.toSet())

        assertEquals(setOf(BackupArea.VIEW, BackupArea.EDITOR, BackupArea.COVERS), esito.wanted)
        assertEquals(esito.wanted, esito.applied)
        assertTrue("il file portava cose che questa versione non conosce", esito.skipped)
        val dopo = archivio()
        assertEquals(true, dopo["fit-grow"])
        assertNull("una chiave sconosciuta non entra", dopo["manopola-futura"])
        assertEquals("un tipo nuovo si salta", 5f, dopo["zoom-max"])
        assertEquals("un valore che non si legge si salta", 4, dopo["folder-columns"])
        assertEquals("una chiave di un'altra area si salta", Hand.RIGHT.token, dopo["hand"])
        assertNull("un'assente nota torna di fabbrica", dopo["bg-type"])
        assertEquals(true, dopo["mark-on"])
        assertArrayEquals("il logo di adesso resta", logo, Watermark.file(app)?.readBytes())
        val copertine = runBlocking { FolderCovers.files(app) }.map { it.name }.toSet()
        assertEquals("resta la sola copertina tenuta", setOf("123-100.webp"), copertine)
    }

    /**
     * **Caso 7: un nome che compare due volte fra le preferenze rifiuta il file, e non cambia
     * niente.**
     *
     * ⚠️ **È quello che nessuna versione scrive**: dei due valori non si saprebbe quale vale.
     * Quindi è il confine fra 'un'altra versione' e 'un file fatto a mano', e di là il file non si
     * importa affatto.
     * ⚠️ **Controprovata** togliendo il controllo dal ramo delle assenti: il file entra, e la prova
     * cade.
     */
    @Test
    fun `un nome doppio fra le preferenze rifiuta il file`() {
        val file = sigilla { zip ->
            testo(zip, "manifest.json", resoconto("view"))
            testo(
                zip, "prefs/view.json",
                preferenze(chiave("fit-grow", "boolean", true), assenti = listOf("fit-grow"))
            )
        }
        runBlocking { rewritePreferences(app) { it[booleanPreferencesKey("fit-grow")] = false } }

        assertEquals(Backup.Reason.BAD, rifiuto(file))
        assertEquals("niente e cambiato", false, archivio()["fit-grow"])
    }

    // ── Le altre aree ────────────────────────────────────────────────────────

    /**
     * **Caso 8: gli stili tornano com'erano.**
     *
     * Un nome cambiato e uno stile di casa tolto sono le due cose che l'archivio degli stili porta
     * oltre agli stili salvati, e sono quelle che un'installazione nuova perderebbe.
     * ⚠️ **Controprovata** saltando la scrittura degli stili all'arrivo: l'archivio resta quello di
     * fabbrica, e la prova cade.
     */
    @Test
    fun `gli stili tornano come erano`() {
        val casa = Presets.house(app)
        Presets.rename(app, casa[0], "Il mio primo")
        Presets.remove(app, casa[1])
        val partenza = Presets.export(app)
        val file = esporta(setOf(BackupArea.STYLES))
        Presets.reset(app)

        val esito = importa(file, setOf(BackupArea.STYLES))

        assertEquals(setOf(BackupArea.STYLES), esito.applied)
        assertEquals(partenza, Presets.export(app))
    }

    /**
     * **Caso 9: le copertine del file prendono il posto di quelle di adesso.**
     *
     * All'arrivo ci sono una copertina di un'altra cartella e una più vecchia della stessa: se ne
     * vanno tutte e due, e resta quella del file, coi suoi byte.
     * ⚠️ **Controprovata** senza la potatura delle vecchie: restano tutte e tre, e la prova cade.
     */
    @Test
    fun `le copertine prendono il posto di quelle di adesso`() {
        copertina("1-100.webp", "della partenza")
        val file = esporta(setOf(BackupArea.COVERS))
        runBlocking { FolderCovers.files(app) }.forEach { it.delete() }
        copertina("2-200.webp", "di un'altra cartella")
        copertina("1-50.webp", "piu vecchia")

        val esito = importa(file, setOf(BackupArea.COVERS))

        assertEquals(setOf(BackupArea.COVERS), esito.applied)
        val dopo = runBlocking { FolderCovers.files(app) }
        assertEquals(listOf("1-100.webp"), dopo.map { it.name })
        assertEquals("della partenza", dopo.single().readText())
    }

    /**
     * **Caso 10: il logo viaggia con le impostazioni dell'editor, nei due versi.**
     *
     * Con un logo alla partenza, all'arrivo c'è lo stesso logo; senza, all'arrivo non ce n'è più.
     * ⚠️ **La seconda metà è la sostituzione**, ed è la parte che si dimentica: un backup senza logo
     * dice che la partenza non ne aveva, e lasciare quello d'arrivo darebbe una firma che nessuno
     * dei due telefoni aveva scelto insieme a quelle impostazioni.
     * ⚠️ **Controprovata** senza la riga che toglie il logo: la seconda metà cade.
     */
    @Test
    fun `il logo viaggia con le impostazioni dell'editor`() {
        runBlocking { assertTrue(Watermark.adopt(app, offri("logo.png", png()))) }
        val logo = Watermark.file(app)!!.readBytes()
        val conLogo = esporta(setOf(BackupArea.EDITOR))
        Watermark.forget(app)
        val senzaLogo = esporta(setOf(BackupArea.EDITOR))

        importa(conLogo, setOf(BackupArea.EDITOR))
        assertArrayEquals("il logo arriva", logo, Watermark.file(app)?.readBytes())

        importa(senzaLogo, setOf(BackupArea.EDITOR))
        assertNull("un backup senza logo toglie quello di adesso", Watermark.file(app))
    }

    /**
     * **Caso 11: il cestino si aggiunge, e un doppione non entra due volte.**
     *
     * ⚠️⚠️ **SI AGGIUNGE E NON SI SOSTITUISCE**: sostituire vorrebbe dire cancellare per sempre dei
     * file, cioè la sola cosa che il cestino esiste per non fare. Quindi all'arrivo resta quello che
     * c'era, e il file del backup arriva accanto con la sua provenienza.
     * ⚠️ **Il doppione si misura importando due volte lo stesso file**, che è il gesto vero di chi
     * non si ricorda se l'ha già fatto. La cronologia dei ripristini segue la stessa regola.
     * ⚠️ **Controprovata** senza il controllo dei doppioni: alla seconda importazione il file entra
     * con un altro nome, e la prova cade.
     */
    @Test
    fun `il cestino si aggiunge e un doppione non entra due volte`() {
        val adesso = System.currentTimeMillis()
        val origine = "/storage/emulated/0/Pictures/A.jpg"
        nelCestino("A.jpg", ByteArray(100) { 1 }, Bin.Record("A.jpg", adesso, origine, Bin.KIND_SENT))
        runBlocking { History.add(app, adesso - 1_000, listOf("/storage/emulated/0/Pictures/R.jpg")) }
        val file = esporta(setOf(BackupArea.BIN))
        runBlocking { Bin.empty(app) }
        Bin.dir(app).parentFile?.let { File(it, "restored.tsv").delete() }
        nelCestino("B.jpg", ByteArray(50) { 2 }, Bin.Record("B.jpg", adesso, "/storage/emulated/0/B.jpg"))

        repeat(2) { assertEquals(setOf(BackupArea.BIN), importa(file, setOf(BackupArea.BIN)).applied) }

        val (righe, files) = runBlocking { Bin.snapshot(app) }
        assertEquals(setOf("A.jpg", "B.jpg"), files.map { it.name }.toSet())
        assertEquals(2, files.size)
        assertEquals(origine, righe.single { it.name == "A.jpg" }.origin)
        assertEquals(Bin.KIND_SENT, righe.single { it.name == "A.jpg" }.kind)
        val storia = runBlocking { History.snapshot(app) }
        assertEquals(listOf("/storage/emulated/0/Pictures/R.jpg"), storia.map { it.path })
    }

    // ── Il contenitore ───────────────────────────────────────────────────────

    /**
     * **Caso 12: col sigillo il file si apre senza password, e dice che cosa porta.**
     *
     * ⚠️ **Il resoconto nomina le sole aree scelte**, ed è quello che la finestra di conferma
     * mostra prima di importare.
     * ⚠️ **Controprovata** scrivendo nel resoconto tutte le aree: la prova cade.
     */
    @Test
    fun `col sigillo il file si apre senza password`() {
        val file = esporta(setOf(BackupArea.VIEW, BackupArea.TINTS))
        val head = Backup.head(ByteArrayInputStream(file))
        assertFalse(head.locked)

        val resoconto = Backup.manifest(ByteArrayInputStream(file), Backup.open(head, null))

        assertEquals(setOf(BackupArea.VIEW, BackupArea.TINTS), resoconto.areas)
        assertEquals(BuildConfig.VERSION_NAME, resoconto.app)
        assertEquals(0, resoconto.foreign)
    }

    /**
     * **Caso 13: la password sbagliata non apre il file, e quella giusta sì.**
     *
     * ⚠️ **Anche la password assente**: un file protetto aperto come sigillo è una password
     * sbagliata, e non un file rotto.
     * ⚠️ **Controprovata** cifrando senza la password nella chiave: il file si apre con qualunque
     * parola, e la prova cade.
     */
    @Test
    fun `la password sbagliata non apre il file`() {
        runBlocking { rewritePreferences(app) { it[booleanPreferencesKey("fit-grow")] = true } }
        val file = esporta(setOf(BackupArea.VIEW), password = "segreta")
        assertTrue(Backup.head(ByteArrayInputStream(file)).locked)

        assertEquals(Backup.Reason.WRONG, rifiuto(file, password = "sbagliata"))
        assertEquals(Backup.Reason.WRONG, rifiuto(file, password = null))
        assertNull(rifiuto(file, password = "segreta"))
        assertEquals(true, archivio()["fit-grow"])
    }

    /**
     * **Caso 14: un byte cambiato non si apre, dovunque sia.**
     *
     * ⚠️ **Tre posti, tre risposte**: un byte del secondo segmento dice 'rotto'; uno del primo
     * segmento col sigillo dice 'rotto' anche lui, perché senza password non c'è niente che si
     * possa sbagliare; uno dei byte riservati dell'intestazione dice 'non è un backup', perché
     * nessuna versione li scrive diversi da zero.
     * ⚠️ **Controprovata** decifrando i segmenti dopo il primo senza verificare il tag: il byte
     * cambiato arriva allo ZIP, che lo vede dal suo CRC e risponde 'non è un backup' invece di
     * 'rotto', e la prova cade.
     */
    @Test
    fun `un byte cambiato non si apre`() {
        val file = grande()
        assertTrue("servono almeno tre segmenti", file.size > Backup.HEAD + 2 * PASSO)

        assertEquals(Backup.Reason.BROKEN, rifiuto(ritocca(file, Backup.HEAD + PASSO + 100)))
        assertEquals(Backup.Reason.BROKEN, rifiuto(ritocca(file, Backup.HEAD + 100)))
        assertEquals(Backup.Reason.BAD, rifiuto(ritocca(file, 6)))
        assertNull("il file intatto si apre", rifiuto(file))
    }

    /**
     * **Caso 15: un file tagliato fra due segmenti non si apre, e nemmeno uno allungato.**
     *
     * ⚠️⚠️ **È IL TAGLIO PIÙ INSIDIOSO**: fatto proprio sul confine, lascia segmenti tutti validi, e
     * senza il segno dell'ultimo nel nonce il file si leggerebbe come intero. Si taglia su ogni
     * confine, e poi si prova il rovescio, cioè un byte in più in coda.
     * ⚠️ **Controprovata** senza il segno dell'ultimo: sul primo confine il contenitore si apre, il
     * taglio lo scopre lo ZIP, che risponde 'non è un backup' invece di 'tagliato', e la prova cade.
     */
    @Test
    fun `un file tagliato fra due segmenti non si apre`() {
        val file = grande()
        var confine = Backup.HEAD + PASSO
        var tagli = 0
        while (confine < file.size) {
            assertEquals("taglio a $confine", Backup.Reason.BROKEN, rifiuto(file.copyOf(confine)))
            confine += PASSO
            tagli++
        }
        assertTrue("servono almeno due confini", tagli >= 2)
        assertEquals(Backup.Reason.BROKEN, rifiuto(file + byteArrayOf(0)))
    }

    /**
     * **Caso 16: due segmenti scambiati non si aprono.**
     *
     * ⚠️ **Il contatore nel nonce** è quello che lo vede: un segmento al posto dell'altro è valido
     * da solo, e fuori posto no.
     * ⚠️ **Controprovata** col contatore fermo a zero: lo scambio si apre, lo ZIP se ne accorge dal
     * CRC, e la prova cade.
     * ⚠️⚠️ **PER RIMETTERE QUEL DIFETTO SUL BANCO SERVE UN CIFRARIO NUOVO PER OGNI SEGMENTO**: la JVM
     * rifiuta di cifrare due volte con la stessa chiave e lo stesso nonce, e senza quella riga la
     * prova cadrebbe già scrivendo il file, cioè per la ragione sbagliata. È successo alla prima
     * corsa, e con lei sono cadute per la stessa ragione le due prove qui sopra.
     */
    @Test
    fun `due segmenti scambiati non si aprono`() {
        val file = grande()
        val primo = file.copyOfRange(Backup.HEAD, Backup.HEAD + PASSO)
        val secondo = file.copyOfRange(Backup.HEAD + PASSO, Backup.HEAD + 2 * PASSO)
        val scambiato = file.copyOf()
        secondo.copyInto(scambiato, Backup.HEAD)
        primo.copyInto(scambiato, Backup.HEAD + PASSO)

        assertEquals(Backup.Reason.BROKEN, rifiuto(scambiato))
    }

    /**
     * **Caso 17: un contenitore più nuovo chiede di aggiornare l'app, e uno storto no.**
     *
     * ⚠️⚠️ **È L'UNICO CANCELLO DI VERSIONE DEL FORMATO**: quel byte sale solo quando cambia il modo
     * di aprire il file, e allora la risposta giusta è 'aggiorna l'app'. Tutto il resto dello
     * storto è 'non è un backup': una versione zero, un'intestazione che non comincia con `AIVB`,
     * e un file protetto con più iterazioni del tetto, che è il modo di tenere un telefono a
     * contare per ore.
     * ⚠️ **Controprovata due volte**: senza il tetto il file con troppe iterazioni si apre, e senza
     * il cancello della versione il file più nuovo diventa 'non è un backup'. Tutte e due le volte
     * la prova cade.
     */
    @Test
    fun `un contenitore piu nuovo chiede di aggiornare l'app`() {
        val file = esporta(setOf(BackupArea.VIEW))
        val protetto = esporta(setOf(BackupArea.VIEW), password = "segreta")

        assertEquals(Backup.Reason.NEWER, apertura(file.copyOf().also { it[4] = 2 }))
        assertEquals(Backup.Reason.BAD, apertura(file.copyOf().also { it[4] = 0 }))
        assertEquals(Backup.Reason.BAD, apertura(file.copyOf().also { it[0] = 'X'.code.toByte() }))
        val oltre = Backup.ROUNDS_MAX + 1
        val troppi = protetto.copyOf().also {
            it[8] = (oltre ushr 24).toByte()
            it[9] = (oltre ushr 16).toByte()
            it[10] = (oltre ushr 8).toByte()
            it[11] = oltre.toByte()
        }
        assertEquals(Backup.Reason.BAD, apertura(troppi))
        assertNull(apertura(file))
    }

    /**
     * **Caso 18: la chiave di una password è quella della norma.**
     *
     * ⚠️⚠️ **I VETTORI SONO QUELLI PUBBLICATI PER PBKDF2 CON HMAC-SHA256** (password `password`,
     * sale `salt`, trentadue byte), e sono la sola misura che non dipende da questo codice. In più
     * il conto si confronta con `SecretKeyFactory` su un sale a caso, che è una seconda
     * implementazione scritta da altri.
     * ⚠️ **Controprovata** col contatore del blocco a zero: i vettori cadono.
     */
    @Test
    fun `la chiave di una password e quella della norma`() {
        val vettori = mapOf(
            1 to "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            2 to "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43",
            4096 to "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a"
        )
        for ((giri, atteso) in vettori) {
            val chiave = Backup.pbkdf2("password".toByteArray(), "salt".toByteArray(), giri)
            assertEquals("$giri iterazioni", atteso, esadecimale(chiave))
        }
        val sale = Random(93).nextBytes(16)
        val altri = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec("una parola lunga".toCharArray(), sale, 1000, 256)).encoded
        assertArrayEquals(altri, Backup.pbkdf2("una parola lunga".toByteArray(), sale, 1000))
    }

    /**
     * **Caso 19: la stessa password scritta in due modi è una password sola.**
     *
     * ⚠️ **Una lettera accentata arriva da una tastiera come un carattere e da un'altra come
     * lettera più accento**, e chi le ha scritte le vede uguali. Senza la normalizzazione, il file
     * scritto da un telefono non si aprirebbe sull'altro.
     * ⚠️ **Controprovata** senza la normalizzazione: i due byte differiscono, e la prova cade.
     */
    @Test
    fun `la stessa password scritta in due modi e una password sola`() {
        assertArrayEquals(Backup.passwordBytes(UNITA), Backup.passwordBytes(SCOMPOSTA))
        val file = esporta(setOf(BackupArea.VIEW), password = UNITA)
        assertNull(rifiuto(file, password = SCOMPOSTA))
    }

    // ── Quello che un file fatto a mano non deve poter fare ─────────────────

    /**
     * **Caso 20: una voce gonfiata apposta non entra, e non lascia niente.**
     *
     * ⚠️⚠️ **QUATTRO MEGABYTE DI ZERI COMPRESSI PESANO QUALCHE KILOBYTE**: un file che li portasse
     * fra le voci del cestino riempirebbe il telefono prima di essere scoperto. AIV quelle voci le
     * scrive senza compressione, quindi quello che esce non può superare quello che è entrato.
     * ⚠️ **Dopo il rifiuto non resta niente**: né nel cestino, né nelle cartelle d'appoggio.
     * Le cartelle si confrontano prima e dopo, così la prova non dipende dai loro nomi.
     * ⚠️ **Controprovata** senza il contatore: il file entra, e la prova cade.
     */
    @Test
    fun `una voce gonfiata apposta non entra`() {
        val file = sigilla { zip ->
            testo(zip, "manifest.json", resoconto("bin"))
            testo(zip, "bin/index.tsv", "")
            byte(zip, "bin/files/zeri.jpg", ByteArray(4 * 1024 * 1024), comprimi = true)
        }
        assertTrue("il file deve restare piccolo", file.size < 256 * 1024)
        val prima = cartelle()

        assertEquals(Backup.Reason.BAD, rifiuto(file))

        assertTrue("il cestino resta vuoto", runBlocking { Bin.snapshot(app) }.second.isEmpty())
        assertEquals("nessun appoggio resta", prima, cartelle())
    }

    /**
     * **Caso 21: un file del cestino che dice di tornare dentro la casa dell'app non entra.**
     *
     * ⚠️ **'Ripristina' scrive dove l'origine dice**, e un file scritto apposta potrebbe mandare
     * un'immagine accanto agli stili e alle preferenze. Un backup di AIV non lo fa mai.
     * ⚠️ **Controprovata** senza quel controllo: il file entra, e la prova cade.
     */
    @Test
    fun `un'origine dentro la casa dell'app non entra`() {
        val dentro = File(app.filesDir, "presets.json").absolutePath
        val file = sigilla { zip ->
            testo(zip, "manifest.json", resoconto("bin"))
            testo(zip, "bin/index.tsv", Bin.text(listOf(Bin.Record("x.jpg", 1L, dentro, Bin.KIND_SENT))))
            byte(zip, "bin/files/x.jpg", ByteArray(10))
        }

        assertEquals(Backup.Reason.BAD, rifiuto(file))
    }

    /**
     * **Caso 22: un nome che esce dalla sua cartella non entra.**
     *
     * ⚠️ **È il nome di un file del cestino**, e non uno qualunque: là il nome diventa un percorso
     * della cartella d'appoggio, e con una salita finirebbe fuori.
     * ⚠️⚠️ **CONTROPROVATA SENZA IL CONTROLLO DEL NOME, E LA MISURA DICE UNA COSA IN PIÙ**: il nome
     * arriva al disco e l'importazione cade con un errore di scrittura, perché il percorso passa da
     * una cartella d'appoggio che non esiste ancora. Cioè sul banco c'è una seconda difesa, e per
     * caso; il controllo è quello che la rende inutile, e che dà la risposta giusta.
     */
    @Test
    fun `un nome che esce dalla sua cartella non entra`() {
        val file = sigilla { zip ->
            testo(zip, "manifest.json", resoconto("bin"))
            testo(zip, "bin/index.tsv", "")
            byte(zip, "bin/files/../fuori.jpg", ByteArray(10))
        }

        assertEquals(Backup.Reason.BAD, rifiuto(file))
    }

    // ── Gli attrezzi ─────────────────────────────────────────────────────────

    /**
     * Riempie l'archivio con tutti i suoi scrittori.
     *
     * ⚠️ **La cartella d'avvio c'è**, perché senza la sua chiave non si scrive affatto.
     */
    private suspend fun scriviTutto() {
        SettingsStore.save(app, Settings(startFolder = 42L))
        SettingsStore.clipboardOpened(app, "https://esempio.it/a.jpg", 1L)
        FolderAsk.remember(app)
        Hint.entries.forEach { it.remember(app) }
        Recents.remember(app, "https://esempio.it/b.jpg", "b.jpg")
        DownloadFolder.set(app, "content://albero/prova")
        DownloadLog.note(app, DownloadLog.mark("jpg", 10L), System.currentTimeMillis())
        FolderTints.set(app, 7L, 3)
    }

    /**
     * Sposta ogni preferenza del backup dal valore che ha, e aggiunge le due chiavi vecchie.
     *
     * ⚠️ **Il numero lungo esce dall'intero**, così un `long` letto da `int` perderebbe qualcosa.
     */
    private fun storto(p: MutablePreferences) {
        p[booleanPreferencesKey("veil")] = false
        p[intPreferencesKey("mark-air")] = 5
        for (k in PREF_KEYS) {
            when (k.type) {
                PrefType.BOOLEAN -> booleanPreferencesKey(k.name).let { p[it] = !(p[it] ?: false) }
                PrefType.INT -> intPreferencesKey(k.name).let { p[it] = (p[it] ?: 0) + 1 }
                PrefType.LONG -> longPreferencesKey(k.name).let { p[it] = (p[it] ?: 0L) + 5_000_000_000L }
                PrefType.FLOAT -> floatPreferencesKey(k.name).let { p[it] = (p[it] ?: 0f) + 1.25f }
                PrefType.STRING -> stringPreferencesKey(k.name).let { p[it] = (p[it] ?: "") + "-x" }
                PrefType.SET -> stringSetPreferencesKey(k.name).let { p[it] = (p[it] ?: emptySet()) + "x" }
            }
        }
    }

    /** Le preferenze che il backup porta, nome per valore. */
    private fun archivio(): Map<String, Any> {
        val tutte: Preferences = runBlocking { storedPreferences(app) }
        return tutte.asMap().entries
            .associate { it.key.name to it.value }
            .filterKeys { nome -> PREF_KEYS.any { it.name == nome } }
    }

    /** Un backup delle aree scelte, come lo scrive l'app. */
    private fun esporta(aree: Set<BackupArea>, password: String? = null): ByteArray = runBlocking {
        val out = ByteArrayOutputStream()
        Backup.write(app, out, aree, password, rounds = GIRI)
        out.toByteArray()
    }

    /** Un backup scritto a mano dentro il contenitore vero: vedi [Backup.pack]. */
    private fun sigilla(corpo: (ZipOutputStream) -> Unit): ByteArray = runBlocking {
        val out = ByteArrayOutputStream()
        Backup.pack(out, null, GIRI) { corpo(it) }
        out.toByteArray()
    }

    private fun importa(file: ByteArray, scelte: Set<BackupArea>, password: String? = null): Backup.Outcome =
        runBlocking {
            val opened = Backup.open(Backup.head(ByteArrayInputStream(file)), password)
            Backup.restore(app, ByteArrayInputStream(file), opened, scelte)
        }

    /** Perché quel file non si importa, o `null` se si importa. */
    private fun rifiuto(file: ByteArray, password: String? = null): Backup.Reason? = try {
        importa(file, BackupArea.entries.toSet(), password)
        null
    } catch (e: Backup.Failure) {
        e.reason
    }

    /** Perché quell'intestazione non si apre, o `null` se si apre. */
    private fun apertura(file: ByteArray): Backup.Reason? = try {
        Backup.head(ByteArrayInputStream(file))
        null
    } catch (e: Backup.Failure) {
        e.reason
    }

    /**
     * Un backup di più segmenti: un file del cestino di trecento kilobyte a caso.
     *
     * ⚠️ **A caso e non pieno di zeri**: i file del cestino non si comprimono, ma i byte a caso
     * tengono la misura anche se un giorno lo facessero.
     */
    private fun grande(): ByteArray {
        nelCestino(
            "grande.jpg",
            Random(2026).nextBytes(300 * 1024),
            Bin.Record("grande.jpg", System.currentTimeMillis(), "/storage/emulated/0/g.jpg", Bin.KIND_SENT)
        )
        return esporta(setOf(BackupArea.BIN))
    }

    /** Un file nel cestino con la sua riga, dalla porta che usa anche l'importazione. */
    private fun nelCestino(nome: String, contenuto: ByteArray, riga: Bin.Record) {
        val appoggio = File(app.cacheDir, "arrivo").also { it.mkdirs() }
        val file = File(appoggio, nome).also { it.writeBytes(contenuto) }
        assertEquals(0, runBlocking { Bin.adopt(app, listOf(file to riga)) })
    }

    /**
     * Una copertina scritta nella casa delle copertine.
     *
     * ⚠️ **La cartella è quella di `FolderCovers`**, e la prova la conferma: il file deve comparire
     * fra quelli che `FolderCovers.files` vede, o il nome della cartella è cambiato.
     */
    private fun copertina(nome: String, testo: String) {
        val casa = File(app.filesDir, "covers").also { it.mkdirs() }
        File(casa, nome).writeText(testo)
        assertTrue("copertina $nome", runBlocking { FolderCovers.files(app) }.any { it.name == nome })
    }

    /** Le cartelle dell'app e quelle accanto al cestino, per vedere se ne è nata una. */
    private fun cartelle(): Set<String> {
        val dentro = app.filesDir.listFiles().orEmpty().filter { it.isDirectory }.map { it.absolutePath }
        val accanto = Bin.dir(app).parentFile?.listFiles().orEmpty()
            .filter { it.isDirectory }.map { it.absolutePath }
        return (dentro + accanto).toSet()
    }

    private fun ritocca(file: ByteArray, dove: Int): ByteArray =
        file.copyOf().also { it[dove] = (it[dove].toInt() xor 0x5a).toByte() }

    private fun resoconto(vararg aree: String, extra: (JSONObject) -> Unit = {}): String =
        JSONObject()
            .put("format", "aiv-backup")
            .put("app", "9.99")
            .put("created", 1L)
            .put("areas", JSONArray(aree.toList()))
            .also(extra)
            .toString()

    private fun chiave(nome: String, tipo: String, valore: Any): JSONObject =
        JSONObject().put("name", nome).put("type", tipo).put("value", valore)

    private fun preferenze(vararg chiavi: JSONObject, assenti: List<String>? = null): String =
        JSONObject().put("keys", JSONArray(chiavi.toList())).also { o ->
            assenti?.let { o.put("absent", JSONArray(it)) }
        }.toString()

    private fun testo(zip: ZipOutputStream, nome: String, contenuto: String) =
        byte(zip, nome, contenuto.toByteArray(Charsets.UTF_8), comprimi = true)

    private fun byte(zip: ZipOutputStream, nome: String, contenuto: ByteArray, comprimi: Boolean = false) {
        zip.setLevel(if (comprimi) 9 else 0)
        zip.putNextEntry(ZipEntry(nome))
        zip.write(contenuto)
        zip.closeEntry()
    }

    private fun offri(nome: String, contenuto: ByteArray): Uri {
        val uri = Uri.parse("content://prova/$nome")
        shadowOf(app.contentResolver).registerInputStream(uri, ByteArrayInputStream(contenuto))
        return uri
    }

    private fun png(): ByteArray {
        val mappa = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        mappa.eraseColor(Color.BLACK)
        val out = ByteArrayOutputStream()
        mappa.compress(Bitmap.CompressFormat.PNG, 100, out)
        mappa.recycle()
        return out.toByteArray()
    }

    private fun esadecimale(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }
}

/**
 * Le iterazioni delle prove: poche, perché qui si misura il formato e non il costo.
 *
 * ⚠️ **Il numero finisce nell'intestazione**, quindi il file si apre con le stesse regole di uno
 * da seicentomila.
 */
private const val GIRI = 2

/** Quanto occupa un segmento pieno nel file: i byte in chiaro più il tag. */
private const val PASSO = Backup.SEGMENT + Backup.TAG

/**
 * La stessa parola scritta in due modi: con la lettera accentata in un carattere solo, e con la
 * lettera nuda seguita dall'accento che si combina.
 *
 * ⚠️ **Sono scritte coi codici e non coi caratteri**, perché a leggerle nel sorgente le due forme
 * sono identiche: scritte a mano, chi rilegge la prova non vedrebbe che cosa misura, e un editor
 * che normalizza il testo le farebbe diventare uguali senza dire niente.
 */
private const val UNITA = "città"
private const val SCOMPOSTA = "città"
